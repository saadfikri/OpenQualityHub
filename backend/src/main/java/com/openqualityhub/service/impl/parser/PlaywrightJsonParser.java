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
public class PlaywrightJsonParser {

    private final ObjectMapper objectMapper;

    public TestRun parse(InputStream inputStream) throws Exception {
        JsonNode root = objectMapper.readTree(inputStream);

        TestRun testRun = TestRun.builder().build();
        List<TestCase> testCases = new ArrayList<>();

        int totalTests = 0, passed = 0, failed = 0, skipped = 0;
        long totalDuration = 0;

        JsonNode suites = root.path("suites");
        for (JsonNode suite : suites) {
            parseSuite(suite, suite.path("title").asText(), testRun, testCases);
        }

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
        JsonNode specs = suite.path("specs");
        for (JsonNode spec : specs) {
            String testName = spec.path("title").asText();

            JsonNode tests = spec.path("tests");
            for (JsonNode test : tests) {
                JsonNode results = test.path("results");
                for (JsonNode result : results) {
                    String statusStr = result.path("status").asText();
                    long duration = result.path("duration").asLong(0);

                    TestCase.TestStatus status = switch (statusStr) {
                        case "passed", "expected" -> TestCase.TestStatus.PASSED;
                        case "failed", "unexpected" -> TestCase.TestStatus.FAILED;
                        case "skipped" -> TestCase.TestStatus.SKIPPED;
                        case "timedOut" -> TestCase.TestStatus.ERROR;
                        default -> TestCase.TestStatus.FAILED;
                    };

                    String errorMessage = null;
                    String stackTrace = null;

                    if (result.has("error")) {
                        JsonNode error = result.path("error");
                        errorMessage = error.path("message").asText(null);
                        stackTrace = error.path("stack").asText(null);
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
            }
        }

        // Recurse into nested suites
        JsonNode childSuites = suite.path("suites");
        for (JsonNode childSuite : childSuites) {
            parseSuite(childSuite, suiteName + " > " + childSuite.path("title").asText(), testRun, testCases);
        }
    }
}
