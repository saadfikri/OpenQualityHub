package com.openqualityhub.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCapacityAllocationRequest {

    @NotNull
    private UUID teamId;

    private UUID userId;

    @NotNull
    private LocalDate weekStart;

    @NotNull
    private BigDecimal totalHours;

    @NotNull
    private BigDecimal allocatedHours;

    private String notes;
}
