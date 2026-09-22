package com.example.booking.service;

import com.example.booking.dto.ReservationResponse;
import com.example.booking.entity.Reservation;
import com.example.booking.entity.Resource;
import com.example.booking.entity.User;
import com.example.booking.enums.ReservationStatus;
import com.example.booking.enums.Role;
import com.example.booking.exception.ForbiddenException;
import com.example.booking.repository.ReservationRepository;
import com.example.booking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {
    @Mock
    private ReservationRepository reservations;

    @Mock
    private UserRepository users;

    @Mock
    private ResourceService resourceService;

    private ReservationService service;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        service = new ReservationService(reservations, users, resourceService);

        User owner = new User("alice", "alice@example.com", "encoded", Role.USER);
        Resource resource = new Resource("Room", "Meeting room", "ROOM", new BigDecimal("25.00"), true);
        resource.setId(10L);

        reservation = new Reservation();
        reservation.setId(1L);
        reservation.setUser(owner);
        reservation.setResource(resource);
        reservation.setStartTime(LocalDateTime.of(2030, 1, 1, 10, 0));
        reservation.setEndTime(LocalDateTime.of(2030, 1, 1, 11, 0));
        reservation.setPrice(new BigDecimal("25.00"));
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setCreatedAt(LocalDateTime.of(2029, 12, 1, 10, 0));

        when(reservations.findById(1L)).thenReturn(Optional.of(reservation));
    }

    @Test
    void userCanReadOwnReservation() {
        ReservationResponse result = service.findById(1L, authentication("alice", "ROLE_USER"));

        assertEquals("alice", result.getUsername());
        assertEquals(1L, result.getId());
    }

    @Test
    void userCannotReadAnotherUsersReservation() {
        assertThrows(ForbiddenException.class,
                () -> service.findById(1L, authentication("bob", "ROLE_USER")));
    }

    @Test
    void adminCanDeleteAnotherUsersReservation() {
        service.delete(1L, authentication("admin", "ROLE_ADMIN"));

        verify(reservations).delete(reservation);
    }

    private Authentication authentication(String username, String role) {
        return new UsernamePasswordAuthenticationToken(username, null,
                List.of(new SimpleGrantedAuthority(role)));
    }
}
