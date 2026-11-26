package com.openqualityhub.service.impl;

import com.openqualityhub.entity.*;
import com.openqualityhub.repository.TestExecutionRepository;
import com.openqualityhub.repository.TestIdentityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestIdentityService {

    private final TestIdentityRepository testIdentityRepository;
    private final TestExecutionRepository testExecutionRepository;
    private final TestIdentityLookupService testIdentityLookupService;

    @Transactional
    public void recordExecutions(TestRun testRun) {
        if (testRun.getTestCases() == null || testRun.getTestCases().isEmpty()) {
            return;
        }

        Project project = testRun.getProject();
        int recorded = 0;

        for (TestCase testCase : testRun.getTestCases()) {
            String fingerprint = computeFingerprint(
                testCase.getClassName(), testCase.getName(), testCase.getSuiteName()
            );

            TestIdentity identity = testIdentityLookupService.findOrCreate(project, fingerprint, testCase);

            identity.recordExecution(testCase.getStatus(), testCase.getDurationMs());
            testIdentityRepository.save(identity);

            TestExecution execution = TestExecution.builder()
                .testIdentity(identity)
                .testRun(testRun)
                .testCase(testCase)
                .status(testCase.getStatus().name())
                .durationMs(testCase.getDurationMs())
                .errorMessage(testCase.getErrorMessage())
                .environment(testRun.getEnvironment())
                .branch(testRun.getBranch())
                .commitSha(testRun.getCommitSha())
                .executedAt(testRun.getExecutedAt())
                .build();
            testExecutionRepository.save(execution);
            recorded++;
        }

        log.debug("Recorded {} test executions for run {}", recorded, testRun.getId());
    }

    public static String computeFingerprint(String className, String testName, String suiteName) {
        String raw = normalize(className) + "::" + normalize(testName) + "::" + normalize(suiteName);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 40);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim();
    }
}
