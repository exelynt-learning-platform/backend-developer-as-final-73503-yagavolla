package com.example.booking;

import com.example.booking.repository.ReservationRepository;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.security.JwtService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReservationRepository reservations;

    @Autowired
    private ResourceRepository resources;

        @Autowired
        private JwtService jwtService;

    @BeforeEach
    void cleanReservations() {
        reservations.deleteAll();
    }

    @Test
    void loginReturnsJwtAndRejectsBadPassword() throws Exception {
        String token = login("user", "User@123");

        org.junit.jupiter.api.Assertions.assertTrue(token.split("\\.").length == 3);
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenForMissingUserIsRejectedAsUnauthorized() throws Exception {
        var missingUser = org.springframework.security.core.userdetails.User.withUsername("missing")
                .password("encoded")
                .roles("USER")
                .build();
        String token = jwtService.generateToken(missingUser);

        mockMvc.perform(get("/api/resources")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanCreateUpdateAndDeleteResourceButUserCannot() throws Exception {
        String userToken = login("user", "User@123");
        String adminToken = login("admin", "Admin@123");
        String resourceJson = "{\"name\":\"Test Room\",\"description\":\"Quiet room\",\"type\":\"ROOM\",\"price\":25.50,\"available\":true}";

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resourceJson))
                .andExpect(status().isForbidden());

        String response = mockMvc.perform(post("/api/resources")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resourceJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.price", is(25.5)))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(put("/api/resources/{id}", id)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resourceJson.replace("Test Room", "Updated Room")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Room")));

        mockMvc.perform(delete("/api/resources/{id}", id)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void invalidResourcePayloadReturnsBadRequest() throws Exception {
        String adminToken = login("admin", "Admin@123");

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"type\":\"ROOM\",\"price\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("name")));
    }

    @Test
    void reservationOwnershipFilteringAndAdminVisibilityWork() throws Exception {
        String userToken = login("user", "User@123");
        String adminToken = login("admin", "Admin@123");
        long resourceId = firstResourceId();
        String reservation = reservationJson(resourceId, 10);

        String response = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservation))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.price", is(50.0)))
                .andReturn().getResponse().getContentAsString();
        long reservationId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].username", is("user")));

        mockMvc.perform(get("/api/reservations/{id}", reservationId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("user")));

        mockMvc.perform(delete("/api/reservations/{id}", reservationId)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void reservationValidationFilteringPaginationAndSortingWork() throws Exception {
        String adminToken = login("admin", "Admin@123");
        String userToken = login("user", "User@123");
        long resourceId = firstResourceId();

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(resourceId, 20)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", bearer(adminToken))
                        .param("status", "PENDING")
                        .param("minPrice", "40")
                        .param("maxPrice", "60")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "price,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", is(1)))
                .andExpect(jsonPath("$.number", is(0)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", bearer(adminToken))
                        .param("minPrice", "60")
                        .param("maxPrice", "40"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":1,\"startTime\":\"2030-01-01T12:00:00\",\"endTime\":\"2030-01-01T11:00:00\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reservationUpdateCrudAndMissingDataErrorsWork() throws Exception {
        String userToken = login("user", "User@123");
        String adminToken = login("admin", "Admin@123");
        long resourceId = firstResourceId();

        String response = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(resourceId, 13)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long reservationId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(put("/api/reservations/{id}", reservationId)
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(resourceId, 15)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startTime", containsString("15:00")));

        String adminUpdate = "{\"resourceId\":" + resourceId + ",\"startTime\":\"2030-01-01T17:00:00\",\"endTime\":\"2030-01-01T18:00:00\",\"price\":75,\"status\":\"CONFIRMED\"}";
        mockMvc.perform(put("/api/reservations/{id}/admin", reservationId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminUpdate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.price", is(75)));

        mockMvc.perform(put("/api/reservations/{id}", reservationId)
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(resourceId, 19)))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/resources/999999")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidReservationStatusAndSortFieldReturnBadRequest() throws Exception {
        String adminToken = login("admin", "Admin@123");

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", bearer(adminToken))
                        .param("status", "INVALID"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", bearer(adminToken))
                        .param("sort", "password,asc"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", bearer(adminToken))
                        .param("sort", "price,sideways"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userCannotUseAdminReservationUpdate() throws Exception {
        String userToken = login("user", "User@123");

        mockMvc.perform(put("/api/reservations/1/admin")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":1,\"startTime\":\"2030-01-01T10:00:00\",\"endTime\":\"2030-01-01T11:00:00\",\"price\":25,\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isForbidden());
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }

    private long firstResourceId() {
        return resources.findAll().stream().findFirst().orElseThrow().getId();
    }

    private String reservationJson(long resourceId, int startHour) {
        LocalDateTime start = LocalDateTime.of(2030, 1, 1, startHour, 0);
        LocalDateTime end = start.plusHours(1);
        return "{\"resourceId\":" + resourceId + ",\"startTime\":\"" + start + "\",\"endTime\":\"" + end + "\"}";
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
