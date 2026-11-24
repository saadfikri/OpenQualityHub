package com.openqualityhub.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDocumentationReviewRequest {

    @NotNull
    private UUID assetId;

    private UUID reviewerId;

    private LocalDate dueDate;
}
