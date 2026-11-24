package com.openqualityhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QaServiceResponse {
    private String id;
    private String name;
    private String description;
    private String category;
    private BigDecimal estimatedHours;
    private boolean active;
    private String createdAt;
    private String updatedAt;
}
