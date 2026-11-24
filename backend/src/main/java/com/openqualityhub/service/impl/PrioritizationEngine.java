package com.openqualityhub.service.impl;

import com.openqualityhub.entity.QaServiceRequest;
import org.springframework.stereotype.Component;

/**
 * ITIL-inspired priority matrix.
 * Computes a 1-5 priority score from impact x urgency.
 *
 * Matrix:
 *                IMMEDIATE  HIGH  MEDIUM  LOW
 * CRITICAL         1         1      2      2
 * HIGH             1         2      2      3
 * MEDIUM           2         2      3      4
 * LOW              2         3      4      5
 */
@Component
public class PrioritizationEngine {

    private static final int[][] PRIORITY_MATRIX = {
        // IMMEDIATE, HIGH, MEDIUM, LOW  (urgency columns)
        { 1, 1, 2, 2 },  // CRITICAL impact
        { 1, 2, 2, 3 },  // HIGH impact
        { 2, 2, 3, 4 },  // MEDIUM impact
        { 2, 3, 4, 5 },  // LOW impact
    };

    public int computePriority(QaServiceRequest.Impact impact, QaServiceRequest.Urgency urgency) {
        int impactIdx = impact.ordinal();
        int urgencyIdx = urgency.ordinal();
        return PRIORITY_MATRIX[impactIdx][urgencyIdx];
    }
}
