package com.openqualityhub.service.impl;

import com.openqualityhub.dto.response.TestRunDetailResponse;
import com.openqualityhub.dto.response.TestRunResponse;
import com.openqualityhub.entity.Project;
import com.openqualityhub.entity.TestCase;
import com.openqualityhub.entity.TestRun;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.mapper.TestRunMapper;
import com.openqualityhub.repository.ProjectRepository;
import com.openqualityhub.repository.TestRunRepository;
import com.openqualityhub.service.TestRunService;
import com.openqualityhub.service.impl.parser.JUnitXmlParser;
import com.openqualityhub.service.impl.parser.PlaywrightJsonParser;
import com.openqualityhub.service.impl.parser.WebdriverIOJsonParser;
import com.openqualityhub.service.impl.parser.CypressJsonParser;
import com.openqualityhub.service.impl.parser.TestNGXmlParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TestRunServiceImpl implements TestRunService {

    private final TestRunRepository testRunRepository;
    private final ProjectRepository projectRepository;
    private final TestRunMapper testRunMapper;
    private final JUnitXmlParser junitParser;
    private final PlaywrightJsonParser playwrightParser;
    private final WebdriverIOJsonParser webdriverIOParser;
    private final CypressJsonParser cypressParser;
    private final TestNGXmlParser testNGParser;
    private final TestIdentityService testIdentityService;

    @Override
    @Transactional
    public TestRunResponse importJUnitResults(UUID projectId, String name, String environment, MultipartFile file) {
        Project project = projectRepository.findByIdAndDeletedFalse(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        try {
            TestRun testRun = junitParser.parse(file.getInputStream());
            testRun.setName(name);
            testRun.setEnvironment(environment);
            testRun.setProject(project);
            testRun.setSource(TestRun.TestSource.JUNIT);

            testRun = testRunRepository.save(testRun);
            testIdentityService.recordExecutions(testRun);
            log.info("Imported JUnit results: {} tests for project {} (run {})",
                testRun.getTotalTests(), project.getName(), testRun.getId());
            return testRunMapper.toResponse(testRun);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse JUnit XML: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public TestRunResponse importPlaywrightResults(UUID projectId, String name, String environment, MultipartFile file) {
        Project project = projectRepository.findByIdAndDeletedFalse(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        try {
            TestRun testRun = playwrightParser.parse(file.getInputStream());
            testRun.setName(name);
            testRun.setEnvironment(environment);
            testRun.setProject(project);
            testRun.setSource(TestRun.TestSource.PLAYWRIGHT);

            testRun = testRunRepository.save(testRun);
            testIdentityService.recordExecutions(testRun);
            log.info("Imported Playwright results: {} tests for project {} (run {})",
                testRun.getTotalTests(), project.getName(), testRun.getId());
            return testRunMapper.toResponse(testRun);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse Playwright JSON: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public TestRunResponse importWebdriverIOResults(UUID projectId, String name, String environment, MultipartFile file) {
        Project project = projectRepository.findByIdAndDeletedFalse(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        try {
            TestRun testRun = webdriverIOParser.parse(file.getInputStream());
            testRun.setName(name);
            testRun.setEnvironment(environment);
            testRun.setProject(project);
            testRun.setSource(TestRun.TestSource.WEBDRIVERIO);

            testRun = testRunRepository.save(testRun);
            testIdentityService.recordExecutions(testRun);
            log.info("Imported WebdriverIO results: {} tests for project {} (run {})",
                testRun.getTotalTests(), project.getName(), testRun.getId());
            return testRunMapper.toResponse(testRun);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse WebdriverIO JSON: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public TestRunResponse importCypressResults(UUID projectId, String name, String environment, MultipartFile file) {
        Project project = projectRepository.findByIdAndDeletedFalse(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        try {
            TestRun testRun = cypressParser.parse(file.getInputStream());
            testRun.setName(name);
            testRun.setEnvironment(environment);
            testRun.setProject(project);
            testRun.setSource(TestRun.TestSource.CYPRESS);

            testRun = testRunRepository.save(testRun);
            testIdentityService.recordExecutions(testRun);
            log.info("Imported Cypress results: {} tests for project {} (run {})",
                testRun.getTotalTests(), project.getName(), testRun.getId());
            return testRunMapper.toResponse(testRun);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse Cypress JSON: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public TestRunResponse importTestNGResults(UUID projectId, String name, String environment, MultipartFile file) {
        Project project = projectRepository.findByIdAndDeletedFalse(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        try {
            TestRun testRun = testNGParser.parse(file.getInputStream());
            testRun.setName(name);
            testRun.setEnvironment(environment);
            testRun.setProject(project);
            testRun.setSource(TestRun.TestSource.TESTNG);

            testRun = testRunRepository.save(testRun);
            testIdentityService.recordExecutions(testRun);
            log.info("Imported TestNG results: {} tests for project {} (run {})",
                testRun.getTotalTests(), project.getName(), testRun.getId());
            return testRunMapper.toResponse(testRun);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse TestNG XML: " + e.getMessage(), e);
        }
    }

    @Override
    public TestRunDetailResponse getById(UUID id) {
        TestRun testRun = testRunRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("TestRun", id));
        return testRunMapper.toDetailResponse(testRun);
    }

    @Override
    public Page<TestRunResponse> getByProjectId(UUID projectId, Pageable pageable) {
        if (!projectRepository.findByIdAndDeletedFalse(projectId).isPresent()) {
            throw new ResourceNotFoundException("Project", projectId);
        }
        return testRunRepository.findByProjectIdAndDeletedFalseOrderByExecutedAtDesc(projectId, pageable)
            .map(testRunMapper::toResponse);
    }

    @Override
    public Page<TestRunResponse> getAll(Pageable pageable) {
        return testRunRepository.findByDeletedFalse(pageable).map(testRunMapper::toResponse);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        TestRun testRun = testRunRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("TestRun", id));
        testRun.setDeleted(true);
        testRunRepository.save(testRun);
        log.info("Soft-deleted test run: {}", id);
    }
}
