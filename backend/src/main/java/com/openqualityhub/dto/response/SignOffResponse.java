package com.openqualityhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignOffResponse {
    private String id;
    private String releaseId;
    private String userId;
    private String userName;
    private String role;
    private String decision;
    private String comment;
    private String decidedAt;
}
