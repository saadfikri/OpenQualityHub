package com.openqualityhub.service.impl.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openqualityhub.entity.MaestroFlowResult;
import com.openqualityhub.entity.TestCase;
import com.openqualityhub.entity.TestRun;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses Maestro test output JSON into TestRun + MaestroFlowResults.
 *
 * Supports two Maestro output formats:
 * 1. Maestro CLI output: { "suites": [{ "flows": [...] }] }
 * 2. Maestro Cloud output: { "flows": [...], "device": {...}, "app": {...} }
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaestroJsonParser {

    private final ObjectMapper objectMapper;

    public ParseResult parse(InputStream inputStream) throws Exception {
        JsonNode root = objectMapper.readTree(inputStream);

        TestRun testRun = TestRun.builder()
            .source(TestRun.TestSource.MAESTRO)
            .build();
        List<TestCase> testCases = new ArrayList<>();
        List<MaestroFlowResult> flowResults = new ArrayList<>();

        // Extract top-level device/app info (Maestro Cloud format)
        String globalDeviceName = root.path("device").path("name").asText(null);
        String globalPlatform = root.path("device").path("platform").asText(null);
        String globalOsVersion = root.path("device").path("os_version").asText(null);
        String globalDeviceModel = root.path("device").path("model").asText(null);
        String globalLocale = root.path("device").path("locale").asText(null);
        String globalAppId = root.path("app").path("id").asText(null);
        String globalAppVersion = root.path("app").path("version").asText(null);

        int totalTests = 0, passed = 0, failed = 0, skipped = 0;
        long totalDuration = 0;

        // Format 1: suites → flows (Maestro CLI)
        JsonNode suites = root.path("suites");
        if (suites.isArray() && !suites.isEmpty()) {
            for (JsonNode suite : suites) {
                // Per-suite device info overrides global
                String deviceName = suite.path("device").path("name").asText(globalDeviceName);
                String platform = suite.path("device").path("platform").asText(globalPlatform);
                String osVersion = suite.path("device").path("os_version").asText(globalOsVersion);
                String deviceModel = suite.path("device").path("model").asText(globalDeviceModel);
                String locale = suite.path("device").path("locale").asText(globalLocale);
                String appId = suite.path("app").path("id").asText(globalAppId);
                String appVersion = suite.path("app").path("version").asText(globalAppVersion);

                JsonNode flows = suite.path("flows");
                for (JsonNode flow : flows) {
                    FlowData fd = parseFlow(flow, testRun, deviceName, platform, osVersion,
                        deviceModel, locale, appId, appVersion);
                    testCases.add(fd.testCase);
                    flowResults.add(fd.flowResult);

                    totalTests++;
                    totalDuration += fd.duration;
                    switch (fd.testCase.getStatus()) {
                        case PASSED -> passed++;
                        case FAILED, ERROR -> failed++;
                        case SKIPPED -> skipped++;
                    }
                }
            }
        }

        // Format 2: top-level flows array (Maestro Cloud)
        JsonNode topFlows = root.path("flows");
        if (topFlows.isArray() && !topFlows.isEmpty() && testCases.isEmpty()) {
            for (JsonNode flow : topFlows) {
                // Per-flow device info overrides global
                String deviceName = flow.path("device").path("name").asText(globalDeviceName);
                String platform = flow.path("device").path("platform").asText(globalPlatform);
                String osVersion = flow.path("device").path("os_version").asText(globalOsVersion);
                String deviceModel = flow.path("device").path("model").asText(globalDeviceModel);
                String locale = flow.path("device").path("locale").asText(globalLocale);
                String appId = flow.path("app").path("id").asText(globalAppId);
                String appVersion = flow.path("app").path("version").asText(globalAppVersion);

                FlowData fd = parseFlow(flow, testRun, deviceName, platform, osVersion,
                    deviceModel, locale, appId, appVersion);
                testCases.add(fd.testCase);
                flowResults.add(fd.flowResult);

                totalTests++;
                totalDuration += fd.duration;
                switch (fd.testCase.getStatus()) {
                    case PASSED -> passed++;
                    case FAILED, ERROR -> failed++;
                    case SKIPPED -> skipped++;
                }
            }
        }

        testRun.setTotalTests(totalTests);
        testRun.setPassed(passed);
        testRun.setFailed(failed);
        testRun.setSkipped(skipped);
        testRun.setDurationMs(totalDuration);
        testRun.setTestCases(testCases);

        return new ParseResult(testRun, flowResults);
    }

    private FlowData parseFlow(JsonNode flow, TestRun testRun,
                                String deviceName, String platform, String osVersion,
                                String deviceModel, String locale, String appId, String appVersion) {
        String flowName = flow.path("name").asText(flow.path("flowName").asText("Unknown Flow"));
        String flowFile = flow.path("file").asText(flow.path("flowFile").asText(null));
        String statusStr = flow.path("status").asText("error");
        long duration = flow.path("duration_ms").asLong(flow.path("duration").asLong(0));
        String errorMsg = flow.path("error").isTextual()
            ? flow.path("error").asText()
            : flow.path("error").path("message").asText(null);
        String screenshotUrl = flow.path("screenshot").asText(flow.path("screenshotUrl").asText(null));
        String videoUrl = flow.path("video").asText(flow.path("videoUrl").asText(null));
        int retryCount = flow.path("retries").asInt(flow.path("retryCount").asInt(0));

        TestCase.TestStatus testStatus = switch (statusStr.toLowerCase()) {
            case "passed", "pass", "success" -> TestCase.TestStatus.PASSED;
            case "failed", "fail", "failure" -> TestCase.TestStatus.FAILED;
            case "skipped", "skip" -> TestCase.TestStatus.SKIPPED;
            case "error", "cancelled", "timeout" -> TestCase.TestStatus.ERROR;
            default -> TestCase.TestStatus.FAILED;
        };

        MaestroFlowResult.FlowStatus flowStatus = switch (statusStr.toLowerCase()) {
            case "passed", "pass", "success" -> MaestroFlowResult.FlowStatus.PASSED;
            case "failed", "fail", "failure" -> MaestroFlowResult.FlowStatus.FAILED;
            case "skipped", "skip" -> MaestroFlowResult.FlowStatus.SKIPPED;
            case "cancelled" -> MaestroFlowResult.FlowStatus.CANCELLED;
            case "error", "timeout" -> MaestroFlowResult.FlowStatus.ERROR;
            default -> MaestroFlowResult.FlowStatus.ERROR;
        };

        TestCase testCase = TestCase.builder()
            .name(flowName)
            .suiteName("Maestro")
            .className(flowFile)
            .status(testStatus)
            .durationMs(duration)
            .errorMessage(errorMsg)
            .testRun(testRun)
            .build();

        MaestroFlowResult flowResult = MaestroFlowResult.builder()
            .testRun(testRun)
            .testCase(testCase)
            .flowName(flowName)
            .flowFile(flowFile)
            .deviceName(deviceName)
            .devicePlatform(parsePlatform(platform))
            .osVersion(osVersion)
            .deviceModel(deviceModel)
            .deviceLocale(locale)
            .appId(appId)
            .appVersion(appVersion)
            .screenshotUrl(screenshotUrl)
            .videoUrl(videoUrl)
            .durationMs(duration)
            .status(flowStatus)
            .errorMessage(errorMsg)
            .retryCount(retryCount)
            .build();

        return new FlowData(testCase, flowResult, duration);
    }

    private MaestroFlowResult.DevicePlatform parsePlatform(String platform) {
        if (platform == null) return null;
        return switch (platform.toLowerCase()) {
            case "ios", "iphone", "ipad" -> MaestroFlowResult.DevicePlatform.IOS;
            case "android" -> MaestroFlowResult.DevicePlatform.ANDROID;
            case "web" -> MaestroFlowResult.DevicePlatform.WEB;
            default -> null;
        };
    }

    public record ParseResult(TestRun testRun, List<MaestroFlowResult> flowResults) {}

    private record FlowData(TestCase testCase, MaestroFlowResult flowResult, long duration) {}
}
