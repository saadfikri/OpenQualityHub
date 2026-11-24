package com.openqualityhub.dto.request;

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
public class UpdateIntegrationRequest {

    @Size(max = 200)
    private String name;
    @Size(max = 2000)
    private String description;
    @Size(max = 500)
    private String webhookSecret;
    private Boolean enabled;
    private UUID projectId;
    private String config;
}
