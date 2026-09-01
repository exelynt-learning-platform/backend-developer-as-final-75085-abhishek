package com.booking.repository;

import com.booking.entity.Reservation;
import com.booking.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * Find all reservations with optional filters (for ADMIN).
     */
    @Query("SELECT r FROM Reservation r WHERE " +
            "(:status IS NULL OR r.status = :status) AND " +
            "(:minPrice IS NULL OR r.totalPrice >= :minPrice) AND " +
            "(:maxPrice IS NULL OR r.totalPrice <= :maxPrice)")
    Page<Reservation> findAllWithFilters(
            @Param("status") ReservationStatus status,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);

    /**
     * Find reservations belonging to a specific user with optional filters (for USER).
     */
    @Query("SELECT r FROM Reservation r WHERE r.user.id = :userId AND " +
            "(:status IS NULL OR r.status = :status) AND " +
            "(:minPrice IS NULL OR r.totalPrice >= :minPrice) AND " +
            "(:maxPrice IS NULL OR r.totalPrice <= :maxPrice)")
    Page<Reservation> findByUserIdWithFilters(
            @Param("userId") Long userId,
            @Param("status") ReservationStatus status,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);
}
