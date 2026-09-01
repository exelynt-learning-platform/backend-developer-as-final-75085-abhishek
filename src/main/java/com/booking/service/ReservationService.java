package com.booking.service;

import com.booking.dto.request.ReservationRequest;
import com.booking.dto.request.ReservationStatusRequest;
import com.booking.dto.response.ReservationResponse;
import com.booking.entity.Reservation;
import com.booking.entity.Resource;
import com.booking.entity.User;
import com.booking.enums.ReservationStatus;
import com.booking.enums.Role;
import com.booking.exception.ResourceNotFoundException;
import com.booking.exception.UnauthorizedException;
import com.booking.repository.ReservationRepository;
import com.booking.repository.ResourceRepository;
import com.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    /**
     * Get reservations — ADMIN sees all, USER sees only their own.
     */
    @Transactional(readOnly = true)
    public Page<ReservationResponse> getReservations(
            UserDetails currentUser,
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {

        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return reservationRepository
                    .findAllWithFilters(status, minPrice, maxPrice, pageable)
                    .map(ReservationResponse::from);
        } else {
            User user = findUserOrThrow(currentUser.getUsername());
            return reservationRepository
                    .findByUserIdWithFilters(user.getId(), status, minPrice, maxPrice, pageable)
                    .map(ReservationResponse::from);
        }
    }

    /**
     * Get a single reservation — ADMIN sees any, USER sees only their own.
     */
    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long id, UserDetails currentUser) {
        Reservation reservation = findReservationOrThrow(id);
        enforceOwnership(reservation, currentUser);
        return ReservationResponse.from(reservation);
    }

    /**
     * Create a reservation — userId is taken from the JWT, never from request body.
     */
    @Transactional
    public ReservationResponse createReservation(ReservationRequest request, UserDetails currentUser) {
        validateTimeRange(request);

        User user = findUserOrThrow(currentUser.getUsername());
        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource", request.getResourceId()));

        if (!resource.getAvailable()) {
            throw new IllegalArgumentException("Resource is not available for booking");
        }

        BigDecimal totalPrice = calculateTotalPrice(resource.getPricePerHour(),
                request.getStartTime(), request.getEndTime());

        Reservation reservation = Reservation.builder()
                .user(user)
                .resource(resource)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(ReservationStatus.PENDING)
                .totalPrice(totalPrice)
                .notes(request.getNotes())
                .build();

        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    /**
     * Update a reservation — USER can only update their own; ADMIN can update any.
     */
    @Transactional
    public ReservationResponse updateReservation(Long id, ReservationRequest request, UserDetails currentUser) {
        validateTimeRange(request);

        Reservation reservation = findReservationOrThrow(id);
        enforceOwnership(reservation, currentUser);

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot update a cancelled reservation");
        }

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource", request.getResourceId()));

        BigDecimal totalPrice = calculateTotalPrice(resource.getPricePerHour(),
                request.getStartTime(), request.getEndTime());

        reservation.setResource(resource);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setTotalPrice(totalPrice);
        reservation.setNotes(request.getNotes());

        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    /**
     * Update reservation status — ADMIN only (enforced at controller level).
     */
    @Transactional
    public ReservationResponse updateReservationStatus(Long id, ReservationStatusRequest request) {
        Reservation reservation = findReservationOrThrow(id);
        reservation.setStatus(request.getStatus());
        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    /**
     * Delete a reservation — ADMIN can delete any; USER can cancel (delete) their own.
     */
    @Transactional
    public void deleteReservation(Long id, UserDetails currentUser) {
        Reservation reservation = findReservationOrThrow(id);
        enforceOwnership(reservation, currentUser);
        reservationRepository.delete(reservation);
    }

    // ──────────────────────────── helpers ────────────────────────────

    private Reservation findReservationOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", id));
    }

    private User findUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private void enforceOwnership(Reservation reservation, UserDetails currentUser) {
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !reservation.getUser().getUsername().equals(currentUser.getUsername())) {
            throw new UnauthorizedException("You do not have permission to access this reservation");
        }
    }

    private void validateTimeRange(ReservationRequest request) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }
    }

    private BigDecimal calculateTotalPrice(BigDecimal pricePerHour,
                                           java.time.LocalDateTime startTime,
                                           java.time.LocalDateTime endTime) {
        long minutes = Duration.between(startTime, endTime).toMinutes();
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 4,
                java.math.RoundingMode.HALF_UP);
        return pricePerHour.multiply(hours).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
