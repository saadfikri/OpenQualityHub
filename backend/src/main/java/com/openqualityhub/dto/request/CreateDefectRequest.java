package com.openqualityhub.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDefectRequest {
    @NotBlank
    private String title;
    private String description;
    @NotNull
    private String severity;
    @Min(1) @Max(5)
    private int priority;
    @NotNull
    private UUID projectId;
    private UUID releaseId;
    private String component;
    private UUID assignedToId;
    private String externalId;
    private String externalUrl;
}
