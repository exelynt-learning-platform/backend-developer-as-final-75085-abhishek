package com.booking.dto.request;

import com.booking.enums.ReservationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReservationStatusRequest {

    @NotNull(message = "Status is required")
    private ReservationStatus status;
}
