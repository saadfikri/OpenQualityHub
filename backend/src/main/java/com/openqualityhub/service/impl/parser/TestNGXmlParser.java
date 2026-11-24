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

/**
 * Parses TestNG testng-results.xml format.
 * Expected: <testng-results> → <suite> → <test> → <class> → <test-method>
 */
@Slf4j
@Component
public class TestNGXmlParser {

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

        int totalTests = 0, passed = 0, failed = 0, skipped = 0;
        long totalDuration = 0;

        NodeList testMethods = doc.getElementsByTagName("test-method");
        for (int i = 0; i < testMethods.getLength(); i++) {
            Element method = (Element) testMethods.item(i);

            // Skip config methods (setUp/tearDown)
            String isConfig = method.getAttribute("is-config");
            if ("true".equals(isConfig)) continue;

            String name = method.getAttribute("name");
            String signature = method.getAttribute("signature");
            String statusStr = method.getAttribute("status").toUpperCase();
            String durationStr = method.getAttribute("duration-ms");

            long durationMs = 0;
            if (!durationStr.isEmpty()) {
                durationMs = Long.parseLong(durationStr);
            }

            // Get class name from parent <class> element
            String className = null;
            Node parent = method.getParentNode();
            if (parent instanceof Element classElement && "class".equals(classElement.getTagName())) {
                className = classElement.getAttribute("name");
            }

            // Get suite name from ancestor <test> element
            String suiteName = null;
            Node ancestor = parent;
            while (ancestor != null) {
                if (ancestor instanceof Element el && "test".equals(el.getTagName())) {
                    suiteName = el.getAttribute("name");
                    break;
                }
                ancestor = ancestor.getParentNode();
            }

            TestCase.TestStatus status = switch (statusStr) {
                case "PASS" -> TestCase.TestStatus.PASSED;
                case "FAIL" -> TestCase.TestStatus.FAILED;
                case "SKIP" -> TestCase.TestStatus.SKIPPED;
                default -> TestCase.TestStatus.FAILED;
            };

            String errorMessage = null;
            String stackTrace = null;
            NodeList exceptions = method.getElementsByTagName("exception");
            if (exceptions.getLength() > 0) {
                Element exception = (Element) exceptions.item(0);
                NodeList messages = exception.getElementsByTagName("message");
                if (messages.getLength() > 0) {
                    errorMessage = messages.item(0).getTextContent().trim();
                }
                NodeList stacks = exception.getElementsByTagName("full-stacktrace");
                if (stacks.getLength() > 0) {
                    stackTrace = stacks.item(0).getTextContent().trim();
                }
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

        testRun.setTotalTests(totalTests);
        testRun.setPassed(passed);
        testRun.setFailed(failed);
        testRun.setSkipped(skipped);
        testRun.setDurationMs(totalDuration);
        testRun.setTestCases(testCases);

        return testRun;
    }
}
