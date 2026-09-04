package com.booking.dto.response;

import com.booking.entity.Resource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceResponse {

    private Long id;
    private String name;
    private String description;
    private String type;
    private Integer capacity;
    private BigDecimal pricePerHour;
    private Boolean available;

    public static ResourceResponse from(Resource resource) {
        return ResourceResponse.builder()
                .id(resource.getId())
                .name(resource.getName())
                .description(resource.getDescription())
                .type(resource.getType())
                .capacity(resource.getCapacity())
                .pricePerHour(resource.getPricePerHour())
                .available(resource.getAvailable())
                .build();
    }
}
