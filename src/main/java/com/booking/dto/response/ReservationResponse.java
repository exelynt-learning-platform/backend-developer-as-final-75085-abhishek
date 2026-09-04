package com.booking.dto.response;

import com.booking.entity.Reservation;
import com.booking.enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {

    private Long id;
    private Long userId;
    private String username;
    private Long resourceId;
    private String resourceName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ReservationStatus status;
    private BigDecimal totalPrice;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReservationResponse from(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .userId(reservation.getUser().getId())
                .username(reservation.getUser().getUsername())
                .resourceId(reservation.getResource().getId())
                .resourceName(reservation.getResource().getName())
                .startTime(reservation.getStartTime())
                .endTime(reservation.getEndTime())
                .status(reservation.getStatus())
                .totalPrice(reservation.getTotalPrice())
                .notes(reservation.getNotes())
                .createdAt(reservation.getCreatedAt())
                .updatedAt(reservation.getUpdatedAt())
                .build();
    }
}
