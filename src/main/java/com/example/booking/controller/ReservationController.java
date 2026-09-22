package com.example.booking.controller;

import com.example.booking.dto.ReservationAdminRequest;
import com.example.booking.dto.ReservationRequest;
import com.example.booking.dto.ReservationResponse;
import com.example.booking.enums.ReservationStatus;
import com.example.booking.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Set;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "startTime", "endTime", "price", "status");

    private final ReservationService service;
    public ReservationController(ReservationService service) { this.service = service; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse create(@Valid @RequestBody ReservationRequest request, Authentication authentication) { return service.create(request, authentication); }

    @GetMapping
    public Page<ReservationResponse> findAll(@RequestParam(required = false) ReservationStatus status,
                                             @RequestParam(required = false) BigDecimal minPrice,
                                             @RequestParam(required = false) BigDecimal maxPrice,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(defaultValue = "createdAt,desc") String sort,
                                             Authentication authentication) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page must be >= 0 and size must be between 1 and 100");
        }
        if (minPrice != null && minPrice.signum() < 0 || maxPrice != null && maxPrice.signum() < 0) {
            throw new IllegalArgumentException("price filters must be zero or greater");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("minPrice must not be greater than maxPrice");
        }
        String[] sortParts = sort.split(",", 2);
        if (sortParts[0].isBlank() || !ALLOWED_SORT_FIELDS.contains(sortParts[0])) {
            throw new IllegalArgumentException("unsupported sort field: " + sortParts[0]);
        }
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParts[0]));
        return service.findAll(authentication, status, minPrice, maxPrice, pageable);
    }

    @GetMapping("/{id}") public ReservationResponse findById(@PathVariable Long id, Authentication authentication) { return service.findById(id, authentication); }
    @PutMapping("/{id}") public ReservationResponse update(@PathVariable Long id, @Valid @RequestBody ReservationRequest request, Authentication authentication) { return service.update(id, request, authentication); }
    @PutMapping("/{id}/admin") @PreAuthorize("hasRole('ADMIN')")
    public ReservationResponse adminUpdate(@PathVariable Long id, @Valid @RequestBody ReservationAdminRequest request) { return service.adminUpdate(id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) { service.delete(id, authentication); }
}