package com.openqualityhub.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateTestDataRequest {

    @NotNull(message = "Template ID is required")
    private UUID templateId;

    private UUID profileId;

    @Min(value = 1, message = "Row count must be at least 1")
    @Max(value = 10000, message = "Row count must not exceed 10000")
    private int rowCount;

    @Builder.Default
    private String exportFormat = "JSON";
}
