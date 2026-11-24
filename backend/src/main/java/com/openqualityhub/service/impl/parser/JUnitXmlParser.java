package com.openqualityhub.service.impl.parser;

import com.openqualityhub.entity.TestCase;
import com.openqualityhub.entity.TestRun;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class JUnitXmlParser {

    public TestRun parse(InputStream inputStream) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(inputStream);
        doc.getDocumentElement().normalize();

        TestRun testRun = TestRun.builder().build();
        List<TestCase> testCases = new ArrayList<>();

        NodeList testSuites = doc.getElementsByTagName("testsuite");

        long totalDuration = 0;
        int totalTests = 0, passed = 0, failed = 0, skipped = 0;

        for (int i = 0; i < testSuites.getLength(); i++) {
            Element suite = (Element) testSuites.item(i);
            String suiteName = suite.getAttribute("name");

            NodeList testCaseNodes = suite.getElementsByTagName("testcase");
            for (int j = 0; j < testCaseNodes.getLength(); j++) {
                Element tc = (Element) testCaseNodes.item(j);

                String name = tc.getAttribute("name");
                String className = tc.getAttribute("classname");
                String timeStr = tc.getAttribute("time");
                long durationMs = 0;
                if (!timeStr.isEmpty()) {
                    durationMs = Math.round(Double.parseDouble(timeStr) * 1000);
                }

                TestCase.TestStatus status = TestCase.TestStatus.PASSED;
                String errorMessage = null;
                String stackTrace = null;

                NodeList failures = tc.getElementsByTagName("failure");
                NodeList errors = tc.getElementsByTagName("error");
                NodeList skippedNodes = tc.getElementsByTagName("skipped");

                if (failures.getLength() > 0) {
                    status = TestCase.TestStatus.FAILED;
                    Element failure = (Element) failures.item(0);
                    errorMessage = failure.getAttribute("message");
                    stackTrace = failure.getTextContent();
                } else if (errors.getLength() > 0) {
                    status = TestCase.TestStatus.ERROR;
                    Element error = (Element) errors.item(0);
                    errorMessage = error.getAttribute("message");
                    stackTrace = error.getTextContent();
                } else if (skippedNodes.getLength() > 0) {
                    status = TestCase.TestStatus.SKIPPED;
                }

                TestCase testCase = TestCase.builder()
                    .name(name)
                    .className(className)
                    .suiteName(suiteName)
                    .status(status)
                    .durationMs(durationMs)
                    .errorMessage(errorMessage)
                    .stackTrace(stackTrace)
                    .testRun(testRun)
                    .build();

                testCases.add(testCase);
                totalTests++;
                totalDuration += durationMs;

                switch (status) {
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

        return testRun;
    }
}
