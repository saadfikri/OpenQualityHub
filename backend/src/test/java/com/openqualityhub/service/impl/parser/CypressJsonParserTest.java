package com.openqualityhub.service.impl.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openqualityhub.entity.TestCase;
import com.openqualityhub.entity.TestRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class CypressJsonParserTest {

    private CypressJsonParser parser;

    @BeforeEach
    void setUp() {
        parser = new CypressJsonParser(new ObjectMapper());
    }

    private InputStream toStream(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void parse_passedTest() throws Exception {
        String json = """
                {
                  "results": [{
                    "suites": [{
                      "title": "Login Suite",
                      "tests": [{
                        "title": "should login successfully",
                        "pass": true,
                        "fail": false,
                        "pending": false,
                        "skipped": false,
                        "duration": 1500,
                        "err": {}
                      }],
                      "suites": []
                    }]
                  }]
                }
                """;

        TestRun result = parser.parse(toStream(json));

        assertEquals(1, result.getTotalTests());
        assertEquals(1, result.getPassed());
        assertEquals(0, result.getFailed());
        assertEquals(0, result.getSkipped());
        assertEquals(1500L, result.getDurationMs());
        assertEquals(1, result.getTestCases().size());

        TestCase tc = result.getTestCases().get(0);
        assertEquals("should login successfully", tc.getName());
        assertEquals("Login Suite", tc.getSuiteName());
        assertEquals(TestCase.TestStatus.PASSED, tc.getStatus());
        assertEquals(1500L, tc.getDurationMs());
        assertNull(tc.getErrorMessage());
        assertNull(tc.getStackTrace());
    }

    @Test
    void parse_failedTest_withError() throws Exception {
        String json = """
                {
                  "results": [{
                    "suites": [{
                      "title": "Auth Suite",
                      "tests": [{
                        "title": "should fail on bad creds",
                        "pass": false,
                        "fail": true,
                        "pending": false,
                        "skipped": false,
                        "duration": 200,
                        "err": {
                          "message": "AssertionError: expected 401",
                          "estack": "at Context.<anonymous> (test.js:10)"
                        }
                      }],
                      "suites": []
                    }]
                  }]
                }
                """;

        TestRun result = parser.parse(toStream(json));

        assertEquals(1, result.getTotalTests());
        assertEquals(0, result.getPassed());
        assertEquals(1, result.getFailed());

        TestCase tc = result.getTestCases().get(0);
        assertEquals(TestCase.TestStatus.FAILED, tc.getStatus());
        assertEquals("AssertionError: expected 401", tc.getErrorMessage());
        assertEquals("at Context.<anonymous> (test.js:10)", tc.getStackTrace());
    }

    @Test
    void parse_pendingTest_markedAsSkipped() throws Exception {
        String json = """
                {
                  "results": [{
                    "suites": [{
                      "title": "Suite",
                      "tests": [{
                        "title": "pending test",
                        "pass": false,
                        "fail": false,
                        "pending": true,
                        "skipped": false,
                        "duration": 0,
                        "err": {}
                      }],
                      "suites": []
                    }]
                  }]
                }
                """;

        TestRun result = parser.parse(toStream(json));

        assertEquals(1, result.getSkipped());
        assertEquals(TestCase.TestStatus.SKIPPED, result.getTestCases().get(0).getStatus());
    }

    @Test
    void parse_skippedTest_markedAsSkipped() throws Exception {
        String json = """
                {
                  "results": [{
                    "suites": [{
                      "title": "Suite",
                      "tests": [{
                        "title": "skipped test",
                        "pass": false,
                        "fail": false,
                        "pending": false,
                        "skipped": true,
                        "duration": 0,
                        "err": {}
                      }],
                      "suites": []
                    }]
                  }]
                }
                """;

        TestRun result = parser.parse(toStream(json));

        assertEquals(1, result.getSkipped());
        assertEquals(TestCase.TestStatus.SKIPPED, result.getTestCases().get(0).getStatus());
    }

    @Test
    void parse_neitherPassNorFail_defaultsToFailed() throws Exception {
        String json = """
                {
                  "results": [{
                    "suites": [{
                      "title": "Suite",
                      "tests": [{
                        "title": "ambiguous test",
                        "pass": false,
                        "fail": false,
                        "pending": false,
                        "skipped": false,
                        "duration": 50,
                        "err": {}
                      }],
                      "suites": []
                    }]
                  }]
                }
                """;

        TestRun result = parser.parse(toStream(json));

        assertEquals(1, result.getFailed());
        assertEquals(TestCase.TestStatus.FAILED, result.getTestCases().get(0).getStatus());
    }

    @Test
    void parse_nestedSuites_concatenatesSuiteNames() throws Exception {
        String json = """
                {
                  "results": [{
                    "suites": [{
                      "title": "Parent",
                      "tests": [],
                      "suites": [{
                        "title": "Child",
                        "tests": [{
                          "title": "nested test",
                          "pass": true,
                          "fail": false,
                          "pending": false,
                          "skipped": false,
                          "duration": 100,
                          "err": {}
                        }],
                        "suites": []
                      }]
                    }]
                  }]
                }
                """;

        TestRun result = parser.parse(toStream(json));

        assertEquals(1, result.getTotalTests());
        assertEquals("Parent > Child", result.getTestCases().get(0).getSuiteName());
    }

    @Test
    void parse_multipleTestsAcrossSuites() throws Exception {
        String json = """
                {
                  "results": [{
                    "suites": [{
                      "title": "Suite A",
                      "tests": [
                        {"title": "test1", "pass": true, "fail": false, "pending": false, "skipped": false, "duration": 100, "err": {}},
                        {"title": "test2", "pass": false, "fail": true, "pending": false, "skipped": false, "duration": 200, "err": {"message": "err"}}
                      ],
                      "suites": []
                    }, {
                      "title": "Suite B",
                      "tests": [
                        {"title": "test3", "pass": false, "fail": false, "pending": true, "skipped": false, "duration": 0, "err": {}}
                      ],
                      "suites": []
                    }]
                  }]
                }
                """;

        TestRun result = parser.parse(toStream(json));

        assertEquals(3, result.getTotalTests());
        assertEquals(1, result.getPassed());
        assertEquals(1, result.getFailed());
        assertEquals(1, result.getSkipped());
        assertEquals(300L, result.getDurationMs());
    }

    @Test
    void parse_emptyResults_returnsZeroCounts() throws Exception {
        String json = """
                {
                  "results": []
                }
                """;

        TestRun result = parser.parse(toStream(json));

        assertEquals(0, result.getTotalTests());
        assertEquals(0, result.getPassed());
        assertEquals(0, result.getFailed());
        assertEquals(0, result.getSkipped());
        assertTrue(result.getTestCases().isEmpty());
    }

    @Test
    void parse_emptySuites_returnsZeroCounts() throws Exception {
        String json = """
                {
                  "results": [{
                    "suites": []
                  }]
                }
                """;

        TestRun result = parser.parse(toStream(json));

        assertEquals(0, result.getTotalTests());
    }

    @Test
    void parse_errWithoutMessage_noErrorCaptured() throws Exception {
        String json = """
                {
                  "results": [{
                    "suites": [{
                      "title": "Suite",
                      "tests": [{
                        "title": "test",
                        "pass": true,
                        "fail": false,
                        "pending": false,
                        "skipped": false,
                        "duration": 50,
                        "err": {}
                      }],
                      "suites": []
                    }]
                  }]
                }
                """;

        TestRun result = parser.parse(toStream(json));

        assertNull(result.getTestCases().get(0).getErrorMessage());
    }

    @Test
    void parse_testRunLinkedToTestCases() throws Exception {
        String json = """
                {
                  "results": [{
                    "suites": [{
                      "title": "Suite",
                      "tests": [{
                        "title": "linked test",
                        "pass": true,
                        "fail": false,
                        "pending": false,
                        "skipped": false,
                        "duration": 10,
                        "err": {}
                      }],
                      "suites": []
                    }]
                  }]
                }
                """;

        TestRun result = parser.parse(toStream(json));

        assertNotNull(result.getTestCases().get(0).getTestRun());
        assertSame(result, result.getTestCases().get(0).getTestRun());
    }
}
