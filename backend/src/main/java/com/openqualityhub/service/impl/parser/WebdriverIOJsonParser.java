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

@Slf4j
@Component
@RequiredArgsConstructor
public class WebdriverIOJsonParser {

    private final ObjectMapper objectMapper;

    public TestRun parse(InputStream inputStream) throws Exception {
        JsonNode root = objectMapper.readTree(inputStream);

        TestRun testRun = TestRun.builder().build();
        List<TestCase> testCases = new ArrayList<>();

        // Support both formats:
        // 1. Flat format: root has "suites" array directly
        // 2. Spec reporter format: root has "specs" array, each spec has "suites"
        if (root.has("specs") && root.path("specs").isArray()) {
            for (JsonNode spec : root.path("specs")) {
                JsonNode suites = spec.path("suites");
                for (JsonNode suite : suites) {
                    parseSuite(suite, suite.path("name").asText(), testRun, testCases);
                }
            }
        } else if (root.has("suites") && root.path("suites").isArray()) {
            for (JsonNode suite : root.path("suites")) {
                parseSuite(suite, suite.path("name").asText(), testRun, testCases);
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
            String testName = test.path("name").asText();
            String stateStr = test.path("state").asText();
            long duration = test.path("duration").asLong(0);

            TestCase.TestStatus status = switch (stateStr) {
                case "passed" -> TestCase.TestStatus.PASSED;
                case "failed" -> TestCase.TestStatus.FAILED;
                case "skipped", "pending" -> TestCase.TestStatus.SKIPPED;
                default -> TestCase.TestStatus.FAILED;
            };

            String errorMessage = null;
            String stackTrace = null;

            JsonNode errorNode = test.path("error");
            if (!errorNode.isMissingNode() && !errorNode.isNull()) {
                errorMessage = errorNode.path("message").asText(null);
                stackTrace = errorNode.path("stack").asText(null);
            }

            TestCase testCase = TestCase.builder()
                .name(testName)
                .suiteName(suiteName)
                .status(status)
                .durationMs(duration)
                .errorMessage(errorMessage)
                .stackTrace(stackTrace)
                .testRun(testRun)
                .build();

            testCases.add(testCase);
        }

        // Recurse into nested suites
        JsonNode childSuites = suite.path("suites");
        for (JsonNode childSuite : childSuites) {
            parseSuite(childSuite, suiteName + " > " + childSuite.path("name").asText(), testRun, testCases);
        }
    }
}
