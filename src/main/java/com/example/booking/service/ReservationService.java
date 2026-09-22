package com.example.booking.service;

import com.example.booking.dto.ReservationAdminRequest;
import com.example.booking.dto.ReservationRequest;
import com.example.booking.dto.ReservationResponse;
import com.example.booking.entity.Reservation;
import com.example.booking.entity.Resource;
import com.example.booking.entity.User;
import com.example.booking.enums.ReservationStatus;
import com.example.booking.exception.ConflictException;
import com.example.booking.exception.ForbiddenException;
import com.example.booking.exception.NotFoundException;
import com.example.booking.repository.ReservationRepository;
import com.example.booking.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
public class ReservationService {
    private final ReservationRepository reservations;
    private final UserRepository users;
    private final ResourceService resourceService;

    public ReservationService(ReservationRepository reservations, UserRepository users, ResourceService resourceService) {
        this.reservations = reservations;
        this.users = users;
        this.resourceService = resourceService;
    }

    public ReservationResponse create(ReservationRequest request, Authentication authentication) {
        User user = user(authentication.getName());
        Resource resource = resourceService.resource(request.getResourceId());
        validateWindow(request.getStartTime(), request.getEndTime());
        ensureAvailable(resource, request.getStartTime(), request.getEndTime(), null);

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setResource(resource);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPrice(resource.getPrice());
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setCreatedAt(LocalDateTime.now());
        return response(reservations.save(reservation));
    }

    public Page<ReservationResponse> findAll(Authentication authentication, ReservationStatus status,
                                              BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        boolean admin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Specification<Reservation> specification = (root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();
            if (!admin) predicates.add(builder.equal(root.get("user").get("username"), authentication.getName()));
            if (status != null) predicates.add(builder.equal(root.get("status"), status));
            if (minPrice != null) predicates.add(builder.greaterThanOrEqualTo(root.get("price"), minPrice));
            if (maxPrice != null) predicates.add(builder.lessThanOrEqualTo(root.get("price"), maxPrice));
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return reservations.findAll(specification, pageable).map(this::response);
    }

    public ReservationResponse findById(Long id, Authentication authentication) {
        Reservation reservation = reservation(id);
        ensureOwnerOrAdmin(reservation, authentication);
        return response(reservation);
    }

    public ReservationResponse update(Long id, ReservationRequest request, Authentication authentication) {
        Reservation reservation = reservation(id);
        ensureOwnerOrAdmin(reservation, authentication);
        Resource resource = resourceService.resource(request.getResourceId());
        validateWindow(request.getStartTime(), request.getEndTime());
        ensureAvailable(resource, request.getStartTime(), request.getEndTime(), id);
        reservation.setResource(resource);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPrice(resource.getPrice());
        return response(reservations.save(reservation));
    }

    public ReservationResponse adminUpdate(Long id, ReservationAdminRequest request) {
        Reservation reservation = reservation(id);
        Resource resource = resourceService.resource(request.getResourceId());
        validateWindow(request.getStartTime(), request.getEndTime());
        ensureAvailable(resource, request.getStartTime(), request.getEndTime(), id);
        reservation.setResource(resource);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPrice(request.getPrice());
        reservation.setStatus(request.getStatus());
        return response(reservations.save(reservation));
    }

    public void delete(Long id, Authentication authentication) {
        Reservation reservation = reservation(id);
        ensureOwnerOrAdmin(reservation, authentication);
        reservations.delete(reservation);
    }

    private void ensureAvailable(Resource resource, LocalDateTime start, LocalDateTime end, Long ignoredId) {
        if (!resource.isAvailable()) throw new ConflictException("Resource is not available");
        Specification<Reservation> overlap = (root, query, builder) -> builder.and(
                builder.equal(root.get("resource").get("id"), resource.getId()),
                builder.notEqual(root.get("status"), ReservationStatus.CANCELLED),
                builder.lessThan(root.get("startTime"), end),
                builder.greaterThan(root.get("endTime"), start));
        if (ignoredId != null) overlap = overlap.and((root, query, builder) -> builder.notEqual(root.get("id"), ignoredId));
        if (!reservations.findAll(overlap).isEmpty()) throw new ConflictException("Resource is already reserved for that period");
    }

    private void validateWindow(LocalDateTime start, LocalDateTime end) {
        if (end == null || start == null || !end.isAfter(start)) throw new ConflictException("End time must be after start time");
    }

    private User user(String username) { return users.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found")); }
    private Reservation reservation(Long id) { return reservations.findById(id).orElseThrow(() -> new NotFoundException("Reservation not found: " + id)); }

    private void ensureOwnerOrAdmin(Reservation reservation, Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!admin && !reservation.getUser().getUsername().equals(authentication.getName())) throw new ForbiddenException("You can access only your own reservations");
    }

    private ReservationResponse response(Reservation reservation) {
        ReservationResponse response = new ReservationResponse();
        response.setId(reservation.getId());
        response.setResourceId(reservation.getResource().getId());
        response.setResourceName(reservation.getResource().getName());
        response.setUsername(reservation.getUser().getUsername());
        response.setStartTime(reservation.getStartTime());
        response.setEndTime(reservation.getEndTime());
        response.setPrice(reservation.getPrice());
        response.setStatus(reservation.getStatus());
        response.setCreatedAt(reservation.getCreatedAt());
        return response;
    }
}