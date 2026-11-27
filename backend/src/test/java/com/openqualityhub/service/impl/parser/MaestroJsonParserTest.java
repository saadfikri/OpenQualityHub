package com.openqualityhub.service.impl.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openqualityhub.entity.MaestroFlowResult;
import com.openqualityhub.entity.TestCase;
import com.openqualityhub.entity.TestRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class MaestroJsonParserTest {

    private MaestroJsonParser parser;

    @BeforeEach
    void setUp() {
        parser = new MaestroJsonParser(new ObjectMapper());
    }

    @Test
    void parse_maestroCloudFormat_withTopLevelFlows() throws Exception {
        String json = """
            {
              "device": {"name": "iPhone 15", "platform": "ios", "os_version": "17.2", "model": "iPhone15,2", "locale": "en_US"},
              "app": {"id": "com.example.app", "version": "2.1.0"},
              "flows": [
                {"name": "Login Flow", "file": "login.yaml", "status": "passed", "duration_ms": 3200},
                {"name": "Checkout Flow", "file": "checkout.yaml", "status": "failed", "duration_ms": 5100, "error": {"message": "Element not found"}, "screenshot": "https://cdn.maestro.dev/ss/123.png"},
                {"name": "Settings Flow", "file": "settings.yaml", "status": "skipped", "duration_ms": 0}
              ]
            }
            """;

        MaestroJsonParser.ParseResult result = parser.parse(toStream(json));
        TestRun testRun = result.testRun();

        assertEquals(TestRun.TestSource.MAESTRO, testRun.getSource());
        assertEquals(3, testRun.getTotalTests());
        assertEquals(1, testRun.getPassed());
        assertEquals(1, testRun.getFailed());
        assertEquals(1, testRun.getSkipped());
        assertEquals(8300, testRun.getDurationMs());

        assertEquals(3, result.flowResults().size());

        MaestroFlowResult loginFlow = result.flowResults().get(0);
        assertEquals("Login Flow", loginFlow.getFlowName());
        assertEquals("login.yaml", loginFlow.getFlowFile());
        assertEquals(MaestroFlowResult.DevicePlatform.IOS, loginFlow.getDevicePlatform());
        assertEquals("iPhone 15", loginFlow.getDeviceName());
        assertEquals("17.2", loginFlow.getOsVersion());
        assertEquals("iPhone15,2", loginFlow.getDeviceModel());
        assertEquals("en_US", loginFlow.getDeviceLocale());
        assertEquals("com.example.app", loginFlow.getAppId());
        assertEquals("2.1.0", loginFlow.getAppVersion());
        assertEquals(MaestroFlowResult.FlowStatus.PASSED, loginFlow.getStatus());
        assertEquals(3200, loginFlow.getDurationMs());

        MaestroFlowResult checkoutFlow = result.flowResults().get(1);
        assertEquals("Checkout Flow", checkoutFlow.getFlowName());
        assertEquals(MaestroFlowResult.FlowStatus.FAILED, checkoutFlow.getStatus());
        assertEquals("Element not found", checkoutFlow.getErrorMessage());
        assertEquals("https://cdn.maestro.dev/ss/123.png", checkoutFlow.getScreenshotUrl());

        MaestroFlowResult settingsFlow = result.flowResults().get(2);
        assertEquals(MaestroFlowResult.FlowStatus.SKIPPED, settingsFlow.getStatus());
    }

    @Test
    void parse_maestroCLIFormat_withSuites() throws Exception {
        String json = """
            {
              "suites": [{
                "device": {"name": "Pixel 8", "platform": "android", "os_version": "14"},
                "app": {"id": "com.example.app", "version": "1.0.0"},
                "flows": [
                  {"name": "Onboarding", "file": "onboarding.yaml", "status": "passed", "duration_ms": 4000},
                  {"name": "Search", "file": "search.yaml", "status": "error", "duration_ms": 2000, "error": "Timeout"}
                ]
              }]
            }
            """;

        MaestroJsonParser.ParseResult result = parser.parse(toStream(json));
        TestRun testRun = result.testRun();

        assertEquals(2, testRun.getTotalTests());
        assertEquals(1, testRun.getPassed());
        assertEquals(1, testRun.getFailed()); // ERROR maps to FAILED in test case

        assertEquals(2, result.flowResults().size());
        assertEquals(MaestroFlowResult.DevicePlatform.ANDROID, result.flowResults().get(0).getDevicePlatform());
        assertEquals("Pixel 8", result.flowResults().get(0).getDeviceName());
        assertEquals("14", result.flowResults().get(0).getOsVersion());

        // Error flow
        assertEquals(MaestroFlowResult.FlowStatus.ERROR, result.flowResults().get(1).getStatus());
        assertEquals("Timeout", result.flowResults().get(1).getErrorMessage());
    }

    @Test
    void parse_emptyFlows_producesEmptyResult() throws Exception {
        String json = """
            { "flows": [] }
            """;

        MaestroJsonParser.ParseResult result = parser.parse(toStream(json));
        assertEquals(0, result.testRun().getTotalTests());
        assertTrue(result.flowResults().isEmpty());
    }

    @Test
    void parse_withRetryCount() throws Exception {
        String json = """
            {
              "flows": [
                {"name": "Flaky Flow", "status": "passed", "duration_ms": 1000, "retries": 3}
              ]
            }
            """;

        MaestroJsonParser.ParseResult result = parser.parse(toStream(json));
        assertEquals(3, result.flowResults().get(0).getRetryCount());
    }

    @Test
    void parse_withVideoUrl() throws Exception {
        String json = """
            {
              "flows": [
                {"name": "Video Flow", "status": "failed", "duration_ms": 500, "video": "https://cdn.maestro.dev/vid/abc.mp4", "error": "Assert failed"}
              ]
            }
            """;

        MaestroJsonParser.ParseResult result = parser.parse(toStream(json));
        assertEquals("https://cdn.maestro.dev/vid/abc.mp4", result.flowResults().get(0).getVideoUrl());
    }

    @Test
    void parse_cancelledStatus() throws Exception {
        String json = """
            {
              "flows": [
                {"name": "Cancelled Flow", "status": "cancelled", "duration_ms": 100}
              ]
            }
            """;

        MaestroJsonParser.ParseResult result = parser.parse(toStream(json));
        assertEquals(MaestroFlowResult.FlowStatus.CANCELLED, result.flowResults().get(0).getStatus());
        assertEquals(TestCase.TestStatus.ERROR, result.testRun().getTestCases().get(0).getStatus());
    }

    @Test
    void parse_webPlatform() throws Exception {
        String json = """
            {
              "device": {"platform": "web"},
              "flows": [
                {"name": "Web Flow", "status": "passed", "duration_ms": 2000}
              ]
            }
            """;

        MaestroJsonParser.ParseResult result = parser.parse(toStream(json));
        assertEquals(MaestroFlowResult.DevicePlatform.WEB, result.flowResults().get(0).getDevicePlatform());
    }

    @Test
    void parse_perFlowDeviceOverridesGlobal() throws Exception {
        String json = """
            {
              "device": {"name": "Global Device", "platform": "ios"},
              "flows": [
                {"name": "Flow A", "status": "passed", "duration_ms": 100, "device": {"name": "Local Device", "platform": "android"}}
              ]
            }
            """;

        MaestroJsonParser.ParseResult result = parser.parse(toStream(json));
        assertEquals("Local Device", result.flowResults().get(0).getDeviceName());
        assertEquals(MaestroFlowResult.DevicePlatform.ANDROID, result.flowResults().get(0).getDevicePlatform());
    }

    @Test
    void parse_testCasesLinkedToFlowResults() throws Exception {
        String json = """
            {
              "flows": [
                {"name": "My Flow", "file": "my_flow.yaml", "status": "passed", "duration_ms": 1500}
              ]
            }
            """;

        MaestroJsonParser.ParseResult result = parser.parse(toStream(json));

        TestCase tc = result.testRun().getTestCases().get(0);
        assertEquals("My Flow", tc.getName());
        assertEquals("Maestro", tc.getSuiteName());
        assertEquals("my_flow.yaml", tc.getClassName());
        assertEquals(TestCase.TestStatus.PASSED, tc.getStatus());
        assertEquals(1500, tc.getDurationMs());

        assertNotNull(result.flowResults().get(0).getTestCase());
        assertEquals("My Flow", result.flowResults().get(0).getTestCase().getName());
    }

    @Test
    void parse_alternateFieldNames() throws Exception {
        String json = """
            {
              "flows": [
                {"flowName": "Alt Name", "flowFile": "alt.yaml", "status": "passed", "duration": 800, "retryCount": 1, "screenshotUrl": "http://ss.png", "videoUrl": "http://vid.mp4"}
              ]
            }
            """;

        MaestroJsonParser.ParseResult result = parser.parse(toStream(json));
        MaestroFlowResult fr = result.flowResults().get(0);
        assertEquals("Alt Name", fr.getFlowName());
        assertEquals("alt.yaml", fr.getFlowFile());
        assertEquals(800, fr.getDurationMs());
        assertEquals(1, fr.getRetryCount());
        assertEquals("http://ss.png", fr.getScreenshotUrl());
        assertEquals("http://vid.mp4", fr.getVideoUrl());
    }

    private ByteArrayInputStream toStream(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
}
