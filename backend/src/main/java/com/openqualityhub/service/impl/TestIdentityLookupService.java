package com.openqualityhub.service.impl;

import com.openqualityhub.entity.Project;
import com.openqualityhub.entity.TestCase;
import com.openqualityhub.entity.TestIdentity;
import com.openqualityhub.repository.TestIdentityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestIdentityLookupService {

    private final TestIdentityRepository testIdentityRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TestIdentity findOrCreate(Project project, String fingerprint, TestCase testCase) {
        return testIdentityRepository
            .findByProjectIdAndFingerprint(project.getId(), fingerprint)
            .orElseGet(() -> {
                try {
                    TestIdentity newIdentity = TestIdentity.builder()
                        .project(project)
                        .fingerprint(fingerprint)
                        .className(testCase.getClassName())
                        .testName(testCase.getName())
                        .suiteName(testCase.getSuiteName())
                        .build();
                    return testIdentityRepository.saveAndFlush(newIdentity);
                } catch (DataIntegrityViolationException e) {
                    log.debug("Concurrent TestIdentity creation for fingerprint {}, fetching existing", fingerprint);
                    return testIdentityRepository
                        .findByProjectIdAndFingerprint(project.getId(), fingerprint)
                        .orElseThrow(() -> new IllegalStateException(
                            "TestIdentity not found after constraint violation for fingerprint: " + fingerprint));
                }
            });
    }
}
