package com.openqualityhub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataFieldRequest {

    @NotBlank(message = "Field name is required")
    @Size(max = 200, message = "Field name must not exceed 200 characters")
    private String name;

    @NotBlank(message = "Field type is required")
    private String fieldType;

    private int displayOrder;

    private boolean nullable;

    @Size(max = 200, message = "Faker generator must not exceed 200 characters")
    private String fakerGenerator;

    private String constraints;
}
