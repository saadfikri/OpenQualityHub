package com.openqualityhub.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class CreateIntegrationRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    private String description;

    @NotBlank(message = "Provider is required")
    private String provider;

    @NotBlank(message = "Webhook secret is required for secure webhook processing")
    @Size(max = 500, message = "Webhook secret must not exceed 500 characters")
    private String webhookSecret;

    private UUID projectId;

    private String config;
}
