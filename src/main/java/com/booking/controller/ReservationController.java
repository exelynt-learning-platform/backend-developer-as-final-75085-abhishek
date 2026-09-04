package com.booking.controller;

import com.booking.dto.request.ReservationRequest;
import com.booking.dto.request.ReservationStatusRequest;
import com.booking.dto.response.ReservationResponse;
import com.booking.enums.ReservationStatus;
import com.booking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Manage reservations with ownership and filtering")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private final ReservationService reservationService;

    @GetMapping
    @Operation(
        summary = "List reservations",
        description = "ADMIN sees all reservations. USER sees only their own. " +
                      "Supports filtering by status, minPrice, maxPrice. Supports pagination and sorting."
    )
    public ResponseEntity<Page<ReservationResponse>> getReservations(
            @AuthenticationPrincipal UserDetails currentUser,
            @Parameter(description = "Filter by status: PENDING, CONFIRMED, CANCELLED")
            @RequestParam(required = false) ReservationStatus status,
            @Parameter(description = "Minimum total price filter")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum total price filter")
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(
                reservationService.getReservations(currentUser, status, minPrice, maxPrice, pageable));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get reservation by ID",
        description = "ADMIN can view any reservation. USER can only view their own."
    )
    public ResponseEntity<ReservationResponse> getReservationById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(reservationService.getReservationById(id, currentUser));
    }

    @PostMapping
    @Operation(
        summary = "Create a reservation",
        description = "Creates a reservation. The user identity is taken from the JWT token, not the request body."
    )
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody ReservationRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservationService.createReservation(request, currentUser));
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update reservation",
        description = "USER can only update their own reservation. ADMIN can update any."
    )
    public ResponseEntity<ReservationResponse> updateReservation(
            @PathVariable Long id,
            @Valid @RequestBody ReservationRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(reservationService.updateReservation(id, request, currentUser));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Update reservation status", description = "ADMIN only.")
    public ResponseEntity<ReservationResponse> updateReservationStatus(
            @PathVariable Long id,
            @Valid @RequestBody ReservationStatusRequest request) {
        return ResponseEntity.ok(reservationService.updateReservationStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete reservation",
        description = "ADMIN can delete any reservation. USER can cancel (delete) their own."
    )
    public ResponseEntity<Void> deleteReservation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        reservationService.deleteReservation(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
