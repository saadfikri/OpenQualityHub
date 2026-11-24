package com.openqualityhub.service.impl.parser;

import com.openqualityhub.entity.TestCase;
import com.openqualityhub.entity.TestRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class TestNGXmlParserTest {

    private TestNGXmlParser parser;

    @BeforeEach
    void setUp() {
        parser = new TestNGXmlParser();
    }

    private InputStream toStream(String xml) {
        return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void parse_passedTest() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <testng-results>
                  <suite name="Default Suite">
                    <test name="Regression Tests">
                      <class name="com.example.LoginTest">
                        <test-method name="testLoginSuccess" status="PASS" duration-ms="1234" is-config="false" signature="testLoginSuccess()"/>
                      </class>
                    </test>
                  </suite>
                </testng-results>
                """;

        TestRun result = parser.parse(toStream(xml));

        assertEquals(1, result.getTotalTests());
        assertEquals(1, result.getPassed());
        assertEquals(0, result.getFailed());
        assertEquals(0, result.getSkipped());
        assertEquals(1234L, result.getDurationMs());

        TestCase tc = result.getTestCases().get(0);
        assertEquals("testLoginSuccess", tc.getName());
        assertEquals("com.example.LoginTest", tc.getClassName());
        assertEquals("Regression Tests", tc.getSuiteName());
        assertEquals(TestCase.TestStatus.PASSED, tc.getStatus());
        assertEquals(1234L, tc.getDurationMs());
        assertNull(tc.getErrorMessage());
        assertNull(tc.getStackTrace());
    }

    @Test
    void parse_failedTest_withException() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <testng-results>
                  <suite name="Suite">
                    <test name="Unit Tests">
                      <class name="com.example.CalcTest">
                        <test-method name="testDivision" status="FAIL" duration-ms="50" is-config="false" signature="testDivision()">
                          <exception class="java.lang.ArithmeticException">
                            <message><![CDATA[/ by zero]]></message>
                            <full-stacktrace><![CDATA[java.lang.ArithmeticException: / by zero
                at com.example.Calc.divide(Calc.java:10)]]></full-stacktrace>
                          </exception>
                        </test-method>
                      </class>
                    </test>
                  </suite>
                </testng-results>
                """;

        TestRun result = parser.parse(toStream(xml));

        assertEquals(1, result.getTotalTests());
        assertEquals(0, result.getPassed());
        assertEquals(1, result.getFailed());

        TestCase tc = result.getTestCases().get(0);
        assertEquals(TestCase.TestStatus.FAILED, tc.getStatus());
        assertEquals("/ by zero", tc.getErrorMessage());
        assertTrue(tc.getStackTrace().contains("ArithmeticException"));
    }

    @Test
    void parse_skippedTest() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <testng-results>
                  <suite name="Suite">
                    <test name="Tests">
                      <class name="com.example.SkipTest">
                        <test-method name="testSkipped" status="SKIP" duration-ms="0" is-config="false" signature="testSkipped()"/>
                      </class>
                    </test>
                  </suite>
                </testng-results>
                """;

        TestRun result = parser.parse(toStream(xml));

        assertEquals(1, result.getTotalTests());
        assertEquals(0, result.getPassed());
        assertEquals(0, result.getFailed());
        assertEquals(1, result.getSkipped());
        assertEquals(TestCase.TestStatus.SKIPPED, result.getTestCases().get(0).getStatus());
    }

    @Test
    void parse_configMethods_areSkipped() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <testng-results>
                  <suite name="Suite">
                    <test name="Tests">
                      <class name="com.example.SetupTest">
                        <test-method name="setUp" status="PASS" duration-ms="10" is-config="true" signature="setUp()"/>
                        <test-method name="tearDown" status="PASS" duration-ms="5" is-config="true" signature="tearDown()"/>
                        <test-method name="actualTest" status="PASS" duration-ms="100" is-config="false" signature="actualTest()"/>
                      </class>
                    </test>
                  </suite>
                </testng-results>
                """;

        TestRun result = parser.parse(toStream(xml));

        assertEquals(1, result.getTotalTests());
        assertEquals(1, result.getPassed());
        assertEquals("actualTest", result.getTestCases().get(0).getName());
    }

    @Test
    void parse_unknownStatus_defaultsToFailed() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <testng-results>
                  <suite name="Suite">
                    <test name="Tests">
                      <class name="com.example.UnknownTest">
                        <test-method name="testUnknown" status="UNKNOWN" duration-ms="10" is-config="false" signature="testUnknown()"/>
                      </class>
                    </test>
                  </suite>
                </testng-results>
                """;

        TestRun result = parser.parse(toStream(xml));

        assertEquals(1, result.getFailed());
        assertEquals(TestCase.TestStatus.FAILED, result.getTestCases().get(0).getStatus());
    }

    @Test
    void parse_multipleMethods_aggregatesCorrectly() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <testng-results>
                  <suite name="Suite">
                    <test name="Tests">
                      <class name="com.example.MultiTest">
                        <test-method name="test1" status="PASS" duration-ms="100" is-config="false" signature="test1()"/>
                        <test-method name="test2" status="FAIL" duration-ms="200" is-config="false" signature="test2()"/>
                        <test-method name="test3" status="SKIP" duration-ms="50" is-config="false" signature="test3()"/>
                        <test-method name="test4" status="PASS" duration-ms="150" is-config="false" signature="test4()"/>
                      </class>
                    </test>
                  </suite>
                </testng-results>
                """;

        TestRun result = parser.parse(toStream(xml));

        assertEquals(4, result.getTotalTests());
        assertEquals(2, result.getPassed());
        assertEquals(1, result.getFailed());
        assertEquals(1, result.getSkipped());
        assertEquals(500L, result.getDurationMs());
        assertEquals(4, result.getTestCases().size());
    }

    @Test
    void parse_emptyDurationMs_defaultsToZero() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <testng-results>
                  <suite name="Suite">
                    <test name="Tests">
                      <class name="com.example.Test">
                        <test-method name="test1" status="PASS" duration-ms="" is-config="false" signature="test1()"/>
                      </class>
                    </test>
                  </suite>
                </testng-results>
                """;

        TestRun result = parser.parse(toStream(xml));

        assertEquals(0L, result.getDurationMs());
    }

    @Test
    void parse_noException_errorFieldsNull() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <testng-results>
                  <suite name="Suite">
                    <test name="Tests">
                      <class name="com.example.Test">
                        <test-method name="testOk" status="PASS" duration-ms="10" is-config="false" signature="testOk()"/>
                      </class>
                    </test>
                  </suite>
                </testng-results>
                """;

        TestRun result = parser.parse(toStream(xml));

        assertNull(result.getTestCases().get(0).getErrorMessage());
        assertNull(result.getTestCases().get(0).getStackTrace());
    }

    @Test
    void parse_testRunLinkedToTestCases() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <testng-results>
                  <suite name="Suite">
                    <test name="Tests">
                      <class name="com.example.Test">
                        <test-method name="linked" status="PASS" duration-ms="5" is-config="false" signature="linked()"/>
                      </class>
                    </test>
                  </suite>
                </testng-results>
                """;

        TestRun result = parser.parse(toStream(xml));

        assertSame(result, result.getTestCases().get(0).getTestRun());
    }

    @Test
    void parse_noTestMethods_returnsEmptyRun() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <testng-results>
                  <suite name="Suite">
                    <test name="Tests">
                      <class name="com.example.Empty">
                      </class>
                    </test>
                  </suite>
                </testng-results>
                """;

        TestRun result = parser.parse(toStream(xml));

        assertEquals(0, result.getTotalTests());
        assertTrue(result.getTestCases().isEmpty());
    }

    @Test
    void parse_multipleClasses_resolveClassNames() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <testng-results>
                  <suite name="Suite">
                    <test name="Tests">
                      <class name="com.example.ClassA">
                        <test-method name="testA" status="PASS" duration-ms="10" is-config="false" signature="testA()"/>
                      </class>
                      <class name="com.example.ClassB">
                        <test-method name="testB" status="FAIL" duration-ms="20" is-config="false" signature="testB()"/>
                      </class>
                    </test>
                  </suite>
                </testng-results>
                """;

        TestRun result = parser.parse(toStream(xml));

        assertEquals(2, result.getTotalTests());
        assertEquals("com.example.ClassA", result.getTestCases().get(0).getClassName());
        assertEquals("com.example.ClassB", result.getTestCases().get(1).getClassName());
    }

    @Test
    void parse_suiteNameFromTestElement() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <testng-results>
                  <suite name="Master Suite">
                    <test name="Smoke Tests">
                      <class name="com.example.SmokeTest">
                        <test-method name="smoke1" status="PASS" duration-ms="10" is-config="false" signature="smoke1()"/>
                      </class>
                    </test>
                    <test name="Regression Tests">
                      <class name="com.example.RegTest">
                        <test-method name="reg1" status="PASS" duration-ms="20" is-config="false" signature="reg1()"/>
                      </class>
                    </test>
                  </suite>
                </testng-results>
                """;

        TestRun result = parser.parse(toStream(xml));

        assertEquals(2, result.getTotalTests());
        assertEquals("Smoke Tests", result.getTestCases().get(0).getSuiteName());
        assertEquals("Regression Tests", result.getTestCases().get(1).getSuiteName());
    }
}
