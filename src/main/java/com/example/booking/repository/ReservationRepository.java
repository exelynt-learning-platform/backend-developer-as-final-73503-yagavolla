package com.example.booking.repository;

import com.example.booking.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.example.booking.enums.ReservationStatus;

import java.time.LocalDateTime;

public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {
	boolean existsByResourceIdAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThan(
			Long resourceId, ReservationStatus status, LocalDateTime endTime, LocalDateTime startTime);

	    boolean existsByResourceIdAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
		    Long resourceId, ReservationStatus status, LocalDateTime endTime, LocalDateTime startTime, Long id);
}