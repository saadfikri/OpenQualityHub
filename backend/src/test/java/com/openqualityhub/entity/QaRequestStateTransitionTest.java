package com.openqualityhub.entity;

import com.openqualityhub.service.impl.PrioritizationEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.openqualityhub.entity.QaServiceRequest.RequestStatus.*;
import static org.junit.jupiter.api.Assertions.*;

class QaRequestStateTransitionTest {

    // ---- Valid transitions ----

    @Test
    void submitted_canTransitionTo_triaged() {
        assertTrue(SUBMITTED.canTransitionTo(TRIAGED));
    }

    @Test
    void submitted_canTransitionTo_cancelled() {
        assertTrue(SUBMITTED.canTransitionTo(CANCELLED));
    }

    @Test
    void triaged_canTransitionTo_assigned() {
        assertTrue(TRIAGED.canTransitionTo(ASSIGNED));
    }

    @Test
    void triaged_canTransitionTo_cancelled() {
        assertTrue(TRIAGED.canTransitionTo(CANCELLED));
    }

    @Test
    void assigned_canTransitionTo_inProgress() {
        assertTrue(ASSIGNED.canTransitionTo(IN_PROGRESS));
    }

    @Test
    void assigned_canTransitionTo_onHold() {
        assertTrue(ASSIGNED.canTransitionTo(ON_HOLD));
    }

    @Test
    void assigned_canTransitionTo_cancelled() {
        assertTrue(ASSIGNED.canTransitionTo(CANCELLED));
    }

    @Test
    void inProgress_canTransitionTo_onHold() {
        assertTrue(IN_PROGRESS.canTransitionTo(ON_HOLD));
    }

    @Test
    void inProgress_canTransitionTo_completed() {
        assertTrue(IN_PROGRESS.canTransitionTo(COMPLETED));
    }

    @Test
    void inProgress_canTransitionTo_cancelled() {
        assertTrue(IN_PROGRESS.canTransitionTo(CANCELLED));
    }

    @Test
    void onHold_canTransitionTo_inProgress() {
        assertTrue(ON_HOLD.canTransitionTo(IN_PROGRESS));
    }

    @Test
    void onHold_canTransitionTo_cancelled() {
        assertTrue(ON_HOLD.canTransitionTo(CANCELLED));
    }

    @Test
    void completed_canTransitionTo_closed() {
        assertTrue(COMPLETED.canTransitionTo(CLOSED));
    }

    @Test
    void completed_canTransitionTo_inProgress() {
        assertTrue(COMPLETED.canTransitionTo(IN_PROGRESS));
    }

    // ---- Invalid transitions ----

    @Test
    void submitted_cannotTransitionTo_inProgress() {
        assertFalse(SUBMITTED.canTransitionTo(IN_PROGRESS));
    }

    @Test
    void submitted_cannotTransitionTo_completed() {
        assertFalse(SUBMITTED.canTransitionTo(COMPLETED));
    }

    @Test
    void closed_cannotTransitionToAnything() {
        for (QaServiceRequest.RequestStatus status : QaServiceRequest.RequestStatus.values()) {
            assertFalse(CLOSED.canTransitionTo(status),
                "CLOSED should not transition to " + status);
        }
    }

    @Test
    void cancelled_cannotTransitionToAnything() {
        for (QaServiceRequest.RequestStatus status : QaServiceRequest.RequestStatus.values()) {
            assertFalse(CANCELLED.canTransitionTo(status),
                "CANCELLED should not transition to " + status);
        }
    }

    @Test
    void triaged_cannotTransitionTo_completed() {
        assertFalse(TRIAGED.canTransitionTo(COMPLETED));
    }

    @Test
    void assigned_cannotTransitionTo_submitted() {
        assertFalse(ASSIGNED.canTransitionTo(SUBMITTED));
    }

    // ---- Self transitions should not be valid ----

    @Test
    void noSelfTransitions() {
        for (QaServiceRequest.RequestStatus status : QaServiceRequest.RequestStatus.values()) {
            assertFalse(status.canTransitionTo(status),
                status + " should not self-transition");
        }
    }

    // ---- PrioritizationEngine matrix ----

    private final PrioritizationEngine engine = new PrioritizationEngine();

    @ParameterizedTest
    @CsvSource({
        "CRITICAL, IMMEDIATE, 1",
        "CRITICAL, HIGH, 1",
        "CRITICAL, MEDIUM, 2",
        "CRITICAL, LOW, 2",
        "HIGH, IMMEDIATE, 1",
        "HIGH, HIGH, 2",
        "HIGH, MEDIUM, 2",
        "HIGH, LOW, 3",
        "MEDIUM, IMMEDIATE, 2",
        "MEDIUM, HIGH, 2",
        "MEDIUM, MEDIUM, 3",
        "MEDIUM, LOW, 4",
        "LOW, IMMEDIATE, 2",
        "LOW, HIGH, 3",
        "LOW, MEDIUM, 4",
        "LOW, LOW, 5"
    })
    void prioritizationEngine_shouldMatchMatrix(String impact, String urgency, int expected) {
        int result = engine.computePriority(
            QaServiceRequest.Impact.valueOf(impact),
            QaServiceRequest.Urgency.valueOf(urgency)
        );
        assertEquals(expected, result,
            String.format("Impact=%s, Urgency=%s should yield priority %d", impact, urgency, expected));
    }
}
