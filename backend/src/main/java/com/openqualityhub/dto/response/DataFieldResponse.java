package com.openqualityhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataFieldResponse {

    private UUID id;
    private String name;
    private String fieldType;
    private int displayOrder;
    private boolean nullable;
    private String fakerGenerator;
    private String constraints;
}
