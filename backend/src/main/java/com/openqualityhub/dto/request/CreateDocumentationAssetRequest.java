package com.openqualityhub.dto.request;

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
public class CreateDocumentationAssetRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private String docType;

    private String url;

    private String repoPath;

    @NotNull
    private String category;

    private UUID projectId;

    private UUID ownerUserId;

    private Integer reviewCycleDays;
}
