package com.openqualityhub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateQaServiceRequestRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private UUID qaServiceId;

    @NotNull
    private UUID requestingTeamId;

    private UUID projectId;

    @NotNull
    private String impact;

    @NotNull
    private String urgency;

    private BigDecimal estimatedHours;
}
