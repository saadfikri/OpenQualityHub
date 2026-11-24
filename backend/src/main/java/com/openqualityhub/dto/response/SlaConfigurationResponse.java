package com.openqualityhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlaConfigurationResponse {
    private String id;
    private String name;
    private String description;
    private int priorityLevel;
    private int responseTimeHours;
    private int resolutionTimeHours;
    private boolean businessHoursOnly;
    private boolean active;
    private String createdAt;
    private String updatedAt;
}
