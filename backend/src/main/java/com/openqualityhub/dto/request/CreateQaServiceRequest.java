package com.openqualityhub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQaServiceRequest {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private String category;

    private BigDecimal estimatedHours;
}
