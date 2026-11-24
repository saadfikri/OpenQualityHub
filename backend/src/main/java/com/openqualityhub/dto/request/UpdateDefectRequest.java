package com.openqualityhub.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDefectRequest {
    @Size(max = 500)
    private String title;
    @Size(max = 10000)
    private String description;
    private String severity;
    private String status;
    @Min(1) @Max(5)
    private Integer priority;
    @Size(max = 200)
    private String component;
    private UUID assignedToId;
    @Size(max = 2000)
    private String resolution;
    @Size(max = 200)
    private String externalId;
    @Size(max = 2000)
    private String externalUrl;
}
