package com.example.booking.controller;

import com.example.booking.dto.ReservationAdminRequest;
import com.example.booking.dto.ReservationRequest;
import com.example.booking.dto.ReservationResponse;
import com.example.booking.dto.ReservationQuery;
import com.example.booking.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/reservations")
public class ReservationController {
    private final ReservationService service;
    public ReservationController(ReservationService service) { this.service = service; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse create(@Valid @RequestBody ReservationRequest request, Authentication authentication) { return service.create(request, authentication); }

    @GetMapping
    public Page<ReservationResponse> findAll(@Valid ReservationQuery query,
                                            Authentication authentication) {
        return service.findAll(authentication, query);
    }

    @GetMapping("/{id}") public ReservationResponse findById(@PathVariable Long id, Authentication authentication) { return service.findById(id, authentication); }
    @PutMapping("/{id}") public ReservationResponse update(@PathVariable Long id, @Valid @RequestBody ReservationRequest request, Authentication authentication) { return service.update(id, request, authentication); }
    @PutMapping("/{id}/admin") @PreAuthorize("hasRole('ADMIN')")
    public ReservationResponse adminUpdate(@PathVariable Long id, @Valid @RequestBody ReservationAdminRequest request) { return service.adminUpdate(id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) { service.delete(id, authentication); }
}