package com.openqualityhub.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTestDataTemplateRequest {

    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    private String description;

    private UUID projectId;

    private List<@Valid DataFieldRequest> fields;
}
