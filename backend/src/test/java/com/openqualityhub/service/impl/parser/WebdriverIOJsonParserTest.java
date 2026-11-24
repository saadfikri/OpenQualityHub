package com.openqualityhub.service.impl.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openqualityhub.entity.TestCase;
import com.openqualityhub.entity.TestRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WebdriverIOJsonParserTest {

    private WebdriverIOJsonParser parser;

    private static final String FLAT_FORMAT_JSON = """
            {
              "suites": [
                {
                  "name": "Login Suite",
                  "tests": [
                    { "name": "should login with valid credentials", "state": "passed", "duration": 1200 },
                    { "name": "should reject invalid password", "state": "passed", "duration": 800 },
                    { "name": "should show error on empty fields", "state": "failed", "duration": 350,
                      "error": { "message": "Element not found", "stack": "Error: Element not found\\n    at Context.<anonymous>" } }
                  ]
                }
              ]
            }
            """;

    private static final String SPEC_FORMAT_JSON = """
            {
              "specs": [
                {
                  "suites": [
                    {
                      "name": "Dashboard Spec",
                      "tests": [
                        { "name": "should display widgets", "state": "passed", "duration": 500 },
                        { "name": "should refresh data", "state": "failed", "duration": 1500,
                          "error": { "message": "Timeout", "stack": "TimeoutError: Timeout\\n    at async" } }
                      ]
                    }
                  ]
                },
                {
                  "suites": [
                    {
                      "name": "Settings Spec",
                      "tests": [
                        { "name": "should save preferences", "state": "passed", "duration": 700 }
                      ]
                    }
                  ]
                }
              ]
            }
            """;

    private static final String ALL_STATES_JSON = """
            {
              "suites": [
                {
                  "name": "State Suite",
                  "tests": [
                    { "name": "test passed", "state": "passed", "duration": 100 },
                    { "name": "test failed", "state": "failed", "duration": 200 },
                    { "name": "test skipped", "state": "skipped", "duration": 0 },
                    { "name": "test pending", "state": "pending", "duration": 0 },
                    { "name": "test unknown", "state": "something_else", "duration": 50 }
                  ]
                }
              ]
            }
            """;

    private static final String ERROR_DETAILS_JSON = """
            {
              "suites": [
                {
                  "name": "Error Suite",
                  "tests": [
                    { "name": "has error object", "state": "failed", "duration": 300,
                      "error": { "message": "Expected true to be false", "stack": "AssertionError: Expected true to be false\\n    at UserContext.<anonymous>" } },
                    { "name": "has null error", "state": "failed", "duration": 150,
                      "error": null },
                    { "name": "has no error field", "state": "passed", "duration": 400 }
                  ]
                }
              ]
            }
            """;

    private static final String NESTED_SUITES_JSON = """
            {
              "suites": [
                {
                  "name": "Parent",
                  "tests": [
                    { "name": "parent test", "state": "passed", "duration": 100 }
                  ],
                  "suites": [
                    {
                      "name": "Child",
                      "tests": [
                        { "name": "child test", "state": "passed", "duration": 200 }
                      ],
                      "suites": [
                        {
                          "name": "Grandchild",
                          "tests": [
                            { "name": "grandchild test", "state": "failed", "duration": 300,
                              "error": { "message": "deep failure", "stack": "Error: deep failure" } }
                          ]
                        }
                      ]
                    }
                  ]
                }
              ]
            }
            """;

    private static final String EMPTY_SUITES_JSON = """
            {
              "suites": []
            }
            """;

    private static final String EMPTY_SPECS_JSON = """
            {
              "specs": []
            }
            """;

    private static final String NO_SUITES_OR_SPECS_JSON = """
            {
              "version": "8.0.0"
            }
            """;

    @BeforeEach
    void setUp() {
        parser = new WebdriverIOJsonParser(new ObjectMapper());
    }

    @Test
    void parse_flatFormat_shouldExtractAllTestCasesAndCounts() throws Exception {
        TestRun result = parser.parse(toInputStream(FLAT_FORMAT_JSON));

        assertEquals(3, result.getTotalTests());
        assertEquals(2, result.getPassed());
        assertEquals(1, result.getFailed());
        assertEquals(0, result.getSkipped());
        assertEquals(3, result.getTestCases().size());
    }

    @Test
    void parse_flatFormat_shouldSetSuiteNameOnAllTests() throws Exception {
        TestRun result = parser.parse(toInputStream(FLAT_FORMAT_JSON));

        result.getTestCases().forEach(tc ->
            assertEquals("Login Suite", tc.getSuiteName())
        );
    }

    @Test
    void parse_specFormat_shouldAggregateTestsAcrossSpecs() throws Exception {
        TestRun result = parser.parse(toInputStream(SPEC_FORMAT_JSON));

        assertEquals(3, result.getTotalTests());
        assertEquals(2, result.getPassed());
        assertEquals(1, result.getFailed());
        assertEquals(0, result.getSkipped());
    }

    @Test
    void parse_specFormat_shouldPreserveSuiteNamesFromEachSpec() throws Exception {
        TestRun result = parser.parse(toInputStream(SPEC_FORMAT_JSON));

        List<String> suiteNames = result.getTestCases().stream()
            .map(TestCase::getSuiteName)
            .distinct()
            .sorted()
            .toList();

        assertEquals(2, suiteNames.size());
        assertTrue(suiteNames.contains("Dashboard Spec"));
        assertTrue(suiteNames.contains("Settings Spec"));
    }

    @Test
    void parse_allStates_shouldMapPassedCorrectly() throws Exception {
        TestRun result = parser.parse(toInputStream(ALL_STATES_JSON));

        assertEquals(1, result.getPassed());
    }

    @Test
    void parse_allStates_shouldMapFailedAndUnknownToFailed() throws Exception {
        TestRun result = parser.parse(toInputStream(ALL_STATES_JSON));

        // "failed" + "something_else" (unknown defaults to FAILED)
        assertEquals(2, result.getFailed());
    }

    @Test
    void parse_allStates_shouldMapSkippedAndPendingToSkipped() throws Exception {
        TestRun result = parser.parse(toInputStream(ALL_STATES_JSON));

        // "skipped" + "pending"
        assertEquals(2, result.getSkipped());
    }

    @Test
    void parse_allStates_shouldCountTotalCorrectly() throws Exception {
        TestRun result = parser.parse(toInputStream(ALL_STATES_JSON));

        assertEquals(5, result.getTotalTests());
    }

    @Test
    void parse_errorDetails_shouldExtractMessageAndStack() throws Exception {
        TestRun result = parser.parse(toInputStream(ERROR_DETAILS_JSON));

        TestCase withError = result.getTestCases().stream()
            .filter(tc -> "has error object".equals(tc.getName()))
            .findFirst()
            .orElseThrow();

        assertEquals("Expected true to be false", withError.getErrorMessage());
        assertTrue(withError.getStackTrace().contains("AssertionError"));
    }

    @Test
    void parse_errorDetails_shouldHandleNullErrorNode() throws Exception {
        TestRun result = parser.parse(toInputStream(ERROR_DETAILS_JSON));

        TestCase nullError = result.getTestCases().stream()
            .filter(tc -> "has null error".equals(tc.getName()))
            .findFirst()
            .orElseThrow();

        assertNull(nullError.getErrorMessage());
        assertNull(nullError.getStackTrace());
    }

    @Test
    void parse_errorDetails_shouldHandleMissingErrorField() throws Exception {
        TestRun result = parser.parse(toInputStream(ERROR_DETAILS_JSON));

        TestCase noError = result.getTestCases().stream()
            .filter(tc -> "has no error field".equals(tc.getName()))
            .findFirst()
            .orElseThrow();

        assertNull(noError.getErrorMessage());
        assertNull(noError.getStackTrace());
    }

    @Test
    void parse_nestedSuites_shouldConcatenateSuiteNames() throws Exception {
        TestRun result = parser.parse(toInputStream(NESTED_SUITES_JSON));

        assertEquals(3, result.getTotalTests());

        TestCase parentTest = findTestByName(result, "parent test");
        assertEquals("Parent", parentTest.getSuiteName());

        TestCase childTest = findTestByName(result, "child test");
        assertEquals("Parent > Child", childTest.getSuiteName());

        TestCase grandchildTest = findTestByName(result, "grandchild test");
        assertEquals("Parent > Child > Grandchild", grandchildTest.getSuiteName());
    }

    @Test
    void parse_nestedSuites_shouldAggregateCountsAcrossAllLevels() throws Exception {
        TestRun result = parser.parse(toInputStream(NESTED_SUITES_JSON));

        assertEquals(2, result.getPassed());
        assertEquals(1, result.getFailed());
    }

    @Test
    void parse_emptySuites_shouldReturnZeroCounts() throws Exception {
        TestRun result = parser.parse(toInputStream(EMPTY_SUITES_JSON));

        assertEquals(0, result.getTotalTests());
        assertEquals(0, result.getPassed());
        assertEquals(0, result.getFailed());
        assertEquals(0, result.getSkipped());
        assertTrue(result.getTestCases().isEmpty());
    }

    @Test
    void parse_emptySpecs_shouldReturnZeroCounts() throws Exception {
        TestRun result = parser.parse(toInputStream(EMPTY_SPECS_JSON));

        assertEquals(0, result.getTotalTests());
        assertTrue(result.getTestCases().isEmpty());
    }

    @Test
    void parse_noSuitesOrSpecs_shouldReturnZeroCounts() throws Exception {
        TestRun result = parser.parse(toInputStream(NO_SUITES_OR_SPECS_JSON));

        assertEquals(0, result.getTotalTests());
        assertEquals(0, result.getPassed());
        assertEquals(0, result.getFailed());
        assertEquals(0, result.getSkipped());
        assertEquals(0L, result.getDurationMs());
        assertTrue(result.getTestCases().isEmpty());
    }

    @Test
    void parse_flatFormat_shouldCalculateTotalDuration() throws Exception {
        TestRun result = parser.parse(toInputStream(FLAT_FORMAT_JSON));

        // 1200 + 800 + 350 = 2350
        assertEquals(2350L, result.getDurationMs());
    }

    @Test
    void parse_specFormat_shouldCalculateTotalDurationAcrossSpecs() throws Exception {
        TestRun result = parser.parse(toInputStream(SPEC_FORMAT_JSON));

        // 500 + 1500 + 700 = 2700
        assertEquals(2700L, result.getDurationMs());
    }

    @Test
    void parse_nestedSuites_shouldCalculateDurationAcrossAllLevels() throws Exception {
        TestRun result = parser.parse(toInputStream(NESTED_SUITES_JSON));

        // 100 + 200 + 300 = 600
        assertEquals(600L, result.getDurationMs());
    }

    @Test
    void parse_shouldSetTestRunReferenceOnEachTestCase() throws Exception {
        TestRun result = parser.parse(toInputStream(FLAT_FORMAT_JSON));

        result.getTestCases().forEach(tc ->
            assertSame(result, tc.getTestRun())
        );
    }

    @Test
    void parse_shouldPreserveIndividualTestDurations() throws Exception {
        TestRun result = parser.parse(toInputStream(FLAT_FORMAT_JSON));

        TestCase login = findTestByName(result, "should login with valid credentials");
        assertEquals(1200L, login.getDurationMs());

        TestCase reject = findTestByName(result, "should reject invalid password");
        assertEquals(800L, reject.getDurationMs());
    }

    private TestCase findTestByName(TestRun testRun, String name) {
        return testRun.getTestCases().stream()
            .filter(tc -> name.equals(tc.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Test case not found: " + name));
    }

    private InputStream toInputStream(String json) {
        return new ByteArrayInputStream(json.getBytes());
    }
}
