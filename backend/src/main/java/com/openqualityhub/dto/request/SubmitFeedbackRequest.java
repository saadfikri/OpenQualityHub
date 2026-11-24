package com.openqualityhub.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitFeedbackRequest {

    @NotBlank
    private String feedback; // ACCEPTED, REJECTED, PARTIALLY_ACCEPTED

    private String comment;
}
