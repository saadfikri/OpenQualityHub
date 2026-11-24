package com.openqualityhub.service.impl.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openqualityhub.entity.TestCase;
import com.openqualityhub.entity.TestRun;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses Cypress mochawesome JSON reporter output.
 * Expected format: { "results": [ { "suites": [ { "title": "...", "tests": [...], "suites": [...] } ] } ] }
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CypressJsonParser {

    private final ObjectMapper objectMapper;

    public TestRun parse(InputStream inputStream) throws Exception {
        JsonNode root = objectMapper.readTree(inputStream);

        TestRun testRun = TestRun.builder().build();
        List<TestCase> testCases = new ArrayList<>();

        JsonNode results = root.path("results");
        for (JsonNode result : results) {
            JsonNode suites = result.path("suites");
            for (JsonNode suite : suites) {
                parseSuite(suite, suite.path("title").asText(), testRun, testCases);
            }
        }

        int totalTests = 0, passed = 0, failed = 0, skipped = 0;
        long totalDuration = 0;

        for (TestCase tc : testCases) {
            totalTests++;
            totalDuration += tc.getDurationMs() != null ? tc.getDurationMs() : 0;
            switch (tc.getStatus()) {
                case PASSED -> passed++;
                case FAILED, ERROR -> failed++;
                case SKIPPED -> skipped++;
            }
        }

        testRun.setTotalTests(totalTests);
        testRun.setPassed(passed);
        testRun.setFailed(failed);
        testRun.setSkipped(skipped);
        testRun.setDurationMs(totalDuration);
        testRun.setTestCases(testCases);

        return testRun;
    }

    private void parseSuite(JsonNode suite, String suiteName, TestRun testRun, List<TestCase> testCases) {
        JsonNode tests = suite.path("tests");
        for (JsonNode test : tests) {
            String testName = test.path("title").asText();
            boolean pass = test.path("pass").asBoolean(false);
            boolean fail = test.path("fail").asBoolean(false);
            boolean pending = test.path("pending").asBoolean(false);
            boolean isSkipped = test.path("skipped").asBoolean(false);
            long duration = test.path("duration").asLong(0);

            TestCase.TestStatus status;
            if (pending || isSkipped) {
                status = TestCase.TestStatus.SKIPPED;
            } else if (fail) {
                status = TestCase.TestStatus.FAILED;
            } else if (pass) {
                status = TestCase.TestStatus.PASSED;
            } else {
                status = TestCase.TestStatus.FAILED;
            }

            String errorMessage = null;
            String stackTrace = null;
            JsonNode err = test.path("err");
            if (err.isObject() && err.has("message")) {
                errorMessage = err.path("message").asText(null);
                stackTrace = err.path("estack").asText(null);
            }

            testCases.add(TestCase.builder()
                    .name(testName)
                    .suiteName(suiteName)
                    .status(status)
                    .durationMs(duration)
                    .errorMessage(errorMessage)
                    .stackTrace(stackTrace)
                    .testRun(testRun)
                    .build());
        }

        // Recurse into nested suites
        JsonNode childSuites = suite.path("suites");
        for (JsonNode child : childSuites) {
            parseSuite(child, suiteName + " > " + child.path("title").asText(), testRun, testCases);
        }
    }
}
