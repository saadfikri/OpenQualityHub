package com.openqualityhub.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTestDataProfileRequest {

    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    private String description;

    private String overrides;
}
