package com.booking.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ResourceRequest {

    @NotBlank(message = "Resource name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotBlank(message = "Resource type is required")
    @Size(max = 50, message = "Type must not exceed 50 characters")
    private String type;

    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    @NotNull(message = "Price per hour is required")
    @DecimalMin(value = "0.01", message = "Price per hour must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 integer and 2 fraction digits")
    private BigDecimal pricePerHour;

    private Boolean available = true;
}
