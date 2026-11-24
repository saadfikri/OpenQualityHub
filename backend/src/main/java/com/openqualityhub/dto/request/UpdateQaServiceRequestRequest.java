package com.openqualityhub.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateQaServiceRequestRequest {
    @Size(max = 500)
    private String title;
    @Size(max = 10000)
    private String description;
    private String status;
    private UUID assignedTeamId;
    private UUID assignedUserId;
    @DecimalMin("0.0")
    private BigDecimal estimatedHours;
    @DecimalMin("0.0")
    private BigDecimal actualHours;
    @Size(max = 5000)
    private String comment;
}
