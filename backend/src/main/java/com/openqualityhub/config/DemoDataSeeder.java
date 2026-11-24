package com.openqualityhub.config;

import com.openqualityhub.entity.*;
import com.openqualityhub.repository.*;
import com.openqualityhub.service.impl.TestIdentityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    private final OrganizationRepository organizationRepository;
    private final TeamRepository teamRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ReleaseRepository releaseRepository;
    private final BuildRepository buildRepository;
    private final TestRunRepository testRunRepository;
    private final TestCaseRepository testCaseRepository;
    private final DefectRepository defectRepository;
    private final EnvironmentRepository environmentRepository;
    private final QualityGateRepository qualityGateRepository;
    private final QualityScoreRepository qualityScoreRepository;
    private final TestIdentityRepository testIdentityRepository;
    private final TestExecutionRepository testExecutionRepository;
    private final QaServiceRepository qaServiceRepository;
    private final QaServiceRequestRepository qaServiceRequestRepository;
    private final SlaConfigurationRepository slaConfigurationRepository;
    private final CapacityAllocationRepository capacityAllocationRepository;
    private final DocumentationAssetRepository documentationAssetRepository;
    private final DocumentationCheckRepository documentationCheckRepository;
    private final DocumentationReviewRepository documentationReviewRepository;
    private final ReleaseReadinessReportRepository releaseReadinessReportRepository;
    private final ReleaseSignOffRepository releaseSignOffRepository;
    private final IntegrationConfigRepository integrationConfigRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final PasswordEncoder passwordEncoder;

    private final Random random = new Random(42); // deterministic seed
    private Organization defaultOrg;

    @Override
    @Transactional
    public void run(String... args) {
        if (teamRepository.count() > 0) {
            log.info("Demo data already seeded, skipping");
            return;
        }

        log.info("Seeding demo data...");

        defaultOrg = organizationRepository.findById(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            .orElseThrow();

        seedEnvironments();
        List<User> users = seedUsers();
        List<Team> teams = seedTeams();
        List<Project> projects = seedProjects(teams);
        List<Release> releases = seedReleases(projects);
        seedBuildsAndTestRuns(projects, releases);
        seedDefects(projects, releases, users);
        seedQualityGates();
        seedQualityScores(projects);
        seedTestIdentities(projects);
        List<SlaConfiguration> slaConfigs = seedSlaConfigurations();
        List<QaService> qaServices = seedQaServices();
        seedQaServiceRequests(qaServices, teams, projects, users, slaConfigs);
        seedCapacityAllocations(teams, users);
        seedDocumentationAssets(projects, users);
        seedReleaseConfidence(releases, users);
        seedIntegrations(projects);

        log.info("Demo data seeded successfully: {} teams, {} projects, {} users",
            teams.size(), projects.size(), users.size());
    }

    private void seedEnvironments() {
        List.of(
            createEnv("Development", Environment.EnvironmentType.DEVELOPMENT, 0),
            createEnv("Integration", Environment.EnvironmentType.INTEGRATION, 1),
            createEnv("Staging", Environment.EnvironmentType.STAGING, 2),
            createEnv("Pre-Production", Environment.EnvironmentType.PRE_PRODUCTION, 3),
            createEnv("Production", Environment.EnvironmentType.PRODUCTION, 4)
        ).forEach(environmentRepository::save);
    }

    private Environment createEnv(String name, Environment.EnvironmentType type, int order) {
        return Environment.builder().organization(defaultOrg).name(name).envType(type).displayOrder(order).build();
    }

    private List<User> seedUsers() {
        Role adminRole = roleRepository.findByNameAndOrganizationId("ADMIN", defaultOrg.getId()).orElseThrow();
        Role qaManager = roleRepository.findByNameAndOrganizationId("QA_MANAGER", defaultOrg.getId()).orElseThrow();
        Role qaEngineer = roleRepository.findByNameAndOrganizationId("QA_ENGINEER", defaultOrg.getId()).orElseThrow();
        Role developer = roleRepository.findByNameAndOrganizationId("DEVELOPER", defaultOrg.getId()).orElseThrow();

        String hash = passwordEncoder.encode("Demo123!");

        List<User> users = List.of(
            buildUser("sarah.chen@demo.oqh", "Sarah", "Chen", hash, Set.of(adminRole)),
            buildUser("marcus.johnson@demo.oqh", "Marcus", "Johnson", hash, Set.of(qaManager)),
            buildUser("elena.rodriguez@demo.oqh", "Elena", "Rodriguez", hash, Set.of(qaEngineer)),
            buildUser("james.kim@demo.oqh", "James", "Kim", hash, Set.of(qaEngineer)),
            buildUser("priya.patel@demo.oqh", "Priya", "Patel", hash, Set.of(developer)),
            buildUser("alex.thompson@demo.oqh", "Alex", "Thompson", hash, Set.of(developer))
        );

        return userRepository.saveAll(users);
    }

    private User buildUser(String email, String first, String last, String hash, Set<Role> roles) {
        return User.builder()
            .organization(defaultOrg).email(email).passwordHash(hash)
            .firstName(first).lastName(last).displayName(first + " " + last)
            .roles(roles).build();
    }

    private List<Team> seedTeams() {
        List<Team> teams = List.of(
            Team.builder().organization(defaultOrg).name("Platform Engineering").description("Core platform services and infrastructure").build(),
            Team.builder().organization(defaultOrg).name("Web Applications").description("Customer-facing web applications").build(),
            Team.builder().organization(defaultOrg).name("Mobile").description("iOS and Android applications").build(),
            Team.builder().organization(defaultOrg).name("API Services").description("Backend API and microservices").build()
        );
        return teamRepository.saveAll(teams);
    }

    private List<Project> seedProjects(List<Team> teams) {
        List<Project> projects = new ArrayList<>();
        projects.add(Project.builder().name("Auth Service").description("Authentication and authorization service").team(teams.get(0)).build());
        projects.add(Project.builder().name("Payment Gateway").description("Payment processing integration").team(teams.get(0)).build());
        projects.add(Project.builder().name("Customer Portal").description("Main customer-facing web application").team(teams.get(1)).build());
        projects.add(Project.builder().name("Admin Dashboard").description("Internal administration dashboard").team(teams.get(1)).build());
        projects.add(Project.builder().name("iOS App").description("Native iOS application").team(teams.get(2)).build());
        projects.add(Project.builder().name("Android App").description("Native Android application").team(teams.get(2)).build());
        projects.add(Project.builder().name("REST API v2").description("Public REST API version 2").team(teams.get(3)).build());
        projects.add(Project.builder().name("GraphQL Gateway").description("GraphQL API gateway").team(teams.get(3)).build());
        return projectRepository.saveAll(projects);
    }

    private List<Release> seedReleases(List<Project> projects) {
        List<Release> releases = new ArrayList<>();
        for (Project project : projects) {
            releases.add(Release.builder()
                .project(project).releaseVersion("2.3.0").name("Q3 Release")
                .status(Release.ReleaseStatus.RELEASED).targetDate(LocalDate.now().minusDays(30))
                .actualDate(LocalDate.now().minusDays(28)).build());
            releases.add(Release.builder()
                .project(project).releaseVersion("2.4.0").name("Q4 Release")
                .status(Release.ReleaseStatus.QA_IN_PROGRESS).targetDate(LocalDate.now().plusDays(14)).build());
        }
        return releaseRepository.saveAll(releases);
    }

    private void seedBuildsAndTestRuns(List<Project> projects, List<Release> releases) {
        for (int i = 0; i < projects.size(); i++) {
            Project project = projects.get(i);
            Release currentRelease = releases.get(i * 2 + 1); // Q4 release
            int qualityTier = i % 3; // 0=high, 1=medium, 2=low quality

            for (int day = 29; day >= 0; day--) {
                Instant executedAt = Instant.now().minus(day, ChronoUnit.DAYS);

                Build build = Build.builder()
                    .project(project).release(currentRelease)
                    .buildNumber("build-" + (300 - day))
                    .commitSha(String.format("%040x", random.nextLong()).substring(0, 40))
                    .branch("main").status(Build.BuildStatus.SUCCESS)
                    .startedAt(executedAt).finishedAt(executedAt.plusSeconds(random.nextInt(300) + 60))
                    .build();
                build = buildRepository.save(build);

                int totalTests = 80 + random.nextInt(40);
                int basePassRate = switch (qualityTier) {
                    case 0 -> 92 + random.nextInt(7);
                    case 1 -> 78 + random.nextInt(12);
                    default -> 55 + random.nextInt(20);
                };
                int passed = (int) (totalTests * basePassRate / 100.0);
                int skipped = random.nextInt(5);
                int failed = totalTests - passed - skipped;
                if (failed < 0) { failed = 0; passed = totalTests - skipped; }

                TestRun testRun = TestRun.builder()
                    .project(project).build(build).release(currentRelease)
                    .name("CI Run #" + (300 - day))
                    .source(i % 2 == 0 ? TestRun.TestSource.JUNIT : TestRun.TestSource.PLAYWRIGHT)
                    .environment("staging")
                    .totalTests(totalTests).passed(passed).failed(failed).skipped(skipped)
                    .durationMs((long) (random.nextInt(120000) + 30000))
                    .executedAt(executedAt)
                    .build();
                testRun = testRunRepository.save(testRun);

                seedTestCases(testRun, totalTests, passed, failed, skipped);
            }
        }
    }

    private void seedTestCases(TestRun testRun, int total, int passed, int failed, int skipped) {
        String[] suites = {"Authentication", "Navigation", "Forms", "API Integration", "Data Validation", "Accessibility"};
        List<TestCase> cases = new ArrayList<>();

        for (int j = 0; j < total; j++) {
            TestCase.TestStatus status;
            if (j < passed) status = TestCase.TestStatus.PASSED;
            else if (j < passed + failed) status = TestCase.TestStatus.FAILED;
            else status = TestCase.TestStatus.SKIPPED;

            String suite = suites[j % suites.length];
            String errorMsg = status == TestCase.TestStatus.FAILED
                ? "Expected element to be visible but was not found"
                : null;

            cases.add(TestCase.builder()
                .testRun(testRun)
                .name("test_" + suite.toLowerCase().replace(" ", "_") + "_" + (j + 1))
                .className("com.oqh.tests." + suite.replace(" ", "") + "Test")
                .suiteName(suite)
                .status(status)
                .durationMs((long) (random.nextInt(5000) + 100))
                .errorMessage(errorMsg)
                .build());
        }
        testCaseRepository.saveAll(cases);
        testRun.setTestCases(cases);
    }

    private void seedDefects(List<Project> projects, List<Release> releases, List<User> users) {
        String[] titles = {
            "Login fails intermittently on Safari",
            "Payment timeout not handled gracefully",
            "Missing validation on email field",
            "Dashboard charts not rendering on mobile",
            "API returns 500 on invalid pagination params",
            "Memory leak in WebSocket connection handler",
            "CSRF token not refreshed after session timeout",
            "Search results not paginated correctly",
            "File upload fails for files larger than 10MB",
            "Dark mode colors have insufficient contrast"
        };
        Defect.Severity[] severities = Defect.Severity.values();
        Defect.DefectStatus[] statuses = {Defect.DefectStatus.OPEN, Defect.DefectStatus.IN_PROGRESS, Defect.DefectStatus.RESOLVED, Defect.DefectStatus.CLOSED};

        List<Defect> defects = new ArrayList<>();
        for (int i = 0; i < projects.size(); i++) {
            int numDefects = 3 + random.nextInt(5);
            for (int j = 0; j < numDefects; j++) {
                defects.add(Defect.builder()
                    .project(projects.get(i))
                    .release(releases.get(i * 2 + 1))
                    .title(titles[(i * 3 + j) % titles.length])
                    .description("Detailed reproduction steps for this defect.")
                    .severity(severities[random.nextInt(severities.length)])
                    .status(statuses[random.nextInt(statuses.length)])
                    .priority(random.nextInt(4) + 1)
                    .assignedTo(users.get(random.nextInt(users.size())))
                    .reportedBy(users.get(random.nextInt(users.size())))
                    .build());
            }
        }
        defectRepository.saveAll(defects);
    }

    private void seedQualityGates() {
        QualityGate gate = QualityGate.builder()
            .organization(defaultOrg)
            .name("Standard Release Gate")
            .description("Default quality gate for all releases")
            .defaultGate(true)
            .build();

        gate.getConditions().addAll(List.of(
            QualityGateCondition.builder().qualityGate(gate).metricKey("pass_rate").operator(QualityGateCondition.Operator.GTE).thresholdValue(new BigDecimal("85.00")).critical(true).build(),
            QualityGateCondition.builder().qualityGate(gate).metricKey("critical_defects").operator(QualityGateCondition.Operator.EQ).thresholdValue(BigDecimal.ZERO).critical(true).build(),
            QualityGateCondition.builder().qualityGate(gate).metricKey("high_defects").operator(QualityGateCondition.Operator.LTE).thresholdValue(new BigDecimal("3.00")).critical(false).build()
        ));

        qualityGateRepository.save(gate);
    }

    private void seedQualityScores(List<Project> projects) {
        List<QualityScore> scores = new ArrayList<>();
        for (Project project : projects) {
            BigDecimal reliability = randomScore(60, 99);
            BigDecimal defect = randomScore(50, 95);
            BigDecimal risk = randomScore(40, 90);
            BigDecimal automation = randomScore(55, 98);
            BigDecimal gate = randomScore(65, 100);

            BigDecimal overall = reliability.multiply(new BigDecimal("0.30"))
                .add(defect.multiply(new BigDecimal("0.25")))
                .add(risk.multiply(new BigDecimal("0.20")))
                .add(automation.multiply(new BigDecimal("0.15")))
                .add(gate.multiply(new BigDecimal("0.10")));

            scores.add(QualityScore.builder()
                .project(project)
                .overallScore(overall)
                .testReliabilityScore(reliability)
                .defectScore(defect)
                .riskCoverageScore(risk)
                .automationScore(automation)
                .gateComplianceScore(gate)
                .build());
        }
        qualityScoreRepository.saveAll(scores);
    }

    private void seedTestIdentities(List<Project> projects) {
        String[] suites = {"Authentication", "Navigation", "Forms", "API Integration", "Data Validation", "Accessibility"};
        int identitiesPerProject = 30;

        for (Project project : projects) {
            List<TestIdentity> identities = new ArrayList<>();
            for (int i = 0; i < identitiesPerProject; i++) {
                String suite = suites[i % suites.length];
                String testName = "test_" + suite.toLowerCase().replace(" ", "_") + "_" + (i + 1);
                String className = "com.oqh.tests." + suite.replace(" ", "") + "Test";
                String fingerprint = TestIdentityService.computeFingerprint(className, testName, suite);

                int execs = 20 + random.nextInt(10);
                int passCount = (int) (execs * (0.6 + random.nextDouble() * 0.39));
                int failCount = execs - passCount;
                boolean isFlaky = failCount > 2 && passCount > 2 && random.nextDouble() < 0.3;
                BigDecimal flakyScore = isFlaky
                    ? new BigDecimal(20 + random.nextInt(60)).setScale(2)
                    : new BigDecimal(random.nextInt(15)).setScale(2);

                TestIdentity.FailureClassification classification = isFlaky
                    ? TestIdentity.FailureClassification.TEST_INSTABILITY
                    : (failCount > 3 ? TestIdentity.FailureClassification.PRODUCT_DEFECT : TestIdentity.FailureClassification.UNKNOWN);

                identities.add(TestIdentity.builder()
                    .project(project)
                    .fingerprint(fingerprint)
                    .className(className)
                    .testName(testName)
                    .suiteName(suite)
                    .totalExecutions(execs)
                    .totalPassed(passCount)
                    .totalFailed(failCount)
                    .totalSkipped(0)
                    .flaky(isFlaky)
                    .flakyScore(flakyScore)
                    .failureClassification(classification)
                    .lastStatus(random.nextBoolean() ? "PASSED" : "FAILED")
                    .avgDurationMs((long) (random.nextInt(3000) + 200))
                    .build());
            }
            testIdentityRepository.saveAll(identities);
        }
    }

    private BigDecimal randomScore(int min, int max) {
        return new BigDecimal(min + random.nextInt(max - min)).setScale(2);
    }

    private List<SlaConfiguration> seedSlaConfigurations() {
        List<SlaConfiguration> slas = List.of(
            SlaConfiguration.builder().organization(defaultOrg).name("P1 - Critical").description("Critical priority SLA")
                .priorityLevel(1).responseTimeHours(1).resolutionTimeHours(4).businessHoursOnly(false).active(true).build(),
            SlaConfiguration.builder().organization(defaultOrg).name("P2 - High").description("High priority SLA")
                .priorityLevel(2).responseTimeHours(4).resolutionTimeHours(24).businessHoursOnly(true).active(true).build(),
            SlaConfiguration.builder().organization(defaultOrg).name("P3 - Medium").description("Medium priority SLA")
                .priorityLevel(3).responseTimeHours(8).resolutionTimeHours(72).businessHoursOnly(true).active(true).build(),
            SlaConfiguration.builder().organization(defaultOrg).name("P4 - Low").description("Low priority SLA")
                .priorityLevel(4).responseTimeHours(24).resolutionTimeHours(168).businessHoursOnly(true).active(true).build(),
            SlaConfiguration.builder().organization(defaultOrg).name("P5 - Planning").description("Planning backlog SLA")
                .priorityLevel(5).responseTimeHours(48).resolutionTimeHours(336).businessHoursOnly(true).active(true).build()
        );
        return slaConfigurationRepository.saveAll(slas);
    }

    private List<QaService> seedQaServices() {
        List<QaService> services = List.of(
            QaService.builder().organization(defaultOrg).name("Test Plan Review").description("Review and validate test plans for completeness and coverage")
                .category(QaService.ServiceCategory.TEST_PLANNING).estimatedHours(new BigDecimal("8")).active(true).build(),
            QaService.builder().organization(defaultOrg).name("Manual Test Execution").description("Execute manual test suites against a release candidate")
                .category(QaService.ServiceCategory.TEST_EXECUTION).estimatedHours(new BigDecimal("16")).active(true).build(),
            QaService.builder().organization(defaultOrg).name("Test Automation Development").description("Develop automated test scripts for new features")
                .category(QaService.ServiceCategory.TEST_AUTOMATION).estimatedHours(new BigDecimal("40")).active(true).build(),
            QaService.builder().organization(defaultOrg).name("Performance Test Suite").description("Execute load and performance testing")
                .category(QaService.ServiceCategory.PERFORMANCE_TESTING).estimatedHours(new BigDecimal("24")).active(true).build(),
            QaService.builder().organization(defaultOrg).name("Security Audit").description("Security vulnerability assessment and penetration testing")
                .category(QaService.ServiceCategory.SECURITY_TESTING).estimatedHours(new BigDecimal("32")).active(true).build(),
            QaService.builder().organization(defaultOrg).name("Accessibility Compliance Check").description("WCAG 2.1 AA compliance verification")
                .category(QaService.ServiceCategory.ACCESSIBILITY_TESTING).estimatedHours(new BigDecimal("16")).active(true).build(),
            QaService.builder().organization(defaultOrg).name("Regression Test Cycle").description("Full regression test execution before release")
                .category(QaService.ServiceCategory.REGRESSION_TESTING).estimatedHours(new BigDecimal("24")).active(true).build(),
            QaService.builder().organization(defaultOrg).name("Exploratory Testing Session").description("Time-boxed exploratory testing for a feature area")
                .category(QaService.ServiceCategory.EXPLORATORY_TESTING).estimatedHours(new BigDecimal("4")).active(true).build(),
            QaService.builder().organization(defaultOrg).name("Test Environment Setup").description("Configure and validate a test environment")
                .category(QaService.ServiceCategory.TEST_ENVIRONMENT_SETUP).estimatedHours(new BigDecimal("8")).active(true).build(),
            QaService.builder().organization(defaultOrg).name("QA Strategy Consultation").description("Strategic QA guidance for architecture or feature decisions")
                .category(QaService.ServiceCategory.CONSULTATION).estimatedHours(new BigDecimal("2")).active(true).build()
        );
        return qaServiceRepository.saveAll(services);
    }

    private void seedQaServiceRequests(List<QaService> services, List<Team> teams,
                                        List<Project> projects, List<User> users,
                                        List<SlaConfiguration> slaConfigs) {
        QaServiceRequest.RequestStatus[] statuses = QaServiceRequest.RequestStatus.values();
        QaServiceRequest.Impact[] impacts = QaServiceRequest.Impact.values();
        QaServiceRequest.Urgency[] urgencies = QaServiceRequest.Urgency.values();

        String[] titles = {
            "Regression testing for v2.4.0 release",
            "Performance baseline for new search feature",
            "Automate checkout flow end-to-end tests",
            "Security scan for payment integration",
            "Accessibility audit for dashboard redesign",
            "Test plan review for mobile app v3",
            "Environment setup for staging refresh",
            "Exploratory testing on new user onboarding",
            "Load test for Black Friday readiness",
            "QA strategy for microservices migration",
            "API contract testing for GraphQL gateway",
            "Cross-browser testing for portal redesign",
            "Data migration validation tests",
            "Test automation for CI/CD pipeline",
            "Manual smoke test for hotfix deployment",
            "Security penetration test for admin panel",
            "Performance profiling for memory leaks",
            "Accessibility remediation verification",
            "Regression suite optimization consultation",
            "Test environment capacity planning"
        };

        List<QaServiceRequest> requests = new ArrayList<>();
        for (int i = 0; i < titles.length; i++) {
            QaServiceRequest.Impact impact = impacts[random.nextInt(impacts.length)];
            QaServiceRequest.Urgency urgency = urgencies[random.nextInt(urgencies.length)];
            int priority = computeDemoPriority(impact, urgency);
            QaServiceRequest.RequestStatus status = statuses[random.nextInt(statuses.length)];
            QaService service = services.get(i % services.size());
            Team requestingTeam = teams.get(i % teams.size());
            User requestingUser = users.get(i % users.size());
            Project project = projects.get(i % projects.size());

            SlaConfiguration sla = slaConfigs.stream()
                .filter(s -> s.getPriorityLevel() == priority)
                .findFirst().orElse(slaConfigs.get(2));

            Instant createdAt = Instant.now().minus(random.nextInt(30), ChronoUnit.DAYS);

            QaServiceRequest req = QaServiceRequest.builder()
                .organization(defaultOrg)
                .requestNumber(String.format("QA-%04d", i + 1))
                .title(titles[i])
                .description("Detailed requirements for: " + titles[i])
                .qaService(service)
                .requestingTeam(requestingTeam)
                .requestingUser(requestingUser)
                .project(project)
                .status(status)
                .impact(impact)
                .urgency(urgency)
                .priorityScore(priority)
                .slaConfiguration(sla)
                .responseDeadline(createdAt.plus(sla.getResponseTimeHours(), ChronoUnit.HOURS))
                .resolutionDeadline(createdAt.plus(sla.getResolutionTimeHours(), ChronoUnit.HOURS))
                .estimatedHours(service.getEstimatedHours())
                .build();

            // Assign for non-initial statuses
            if (status != QaServiceRequest.RequestStatus.SUBMITTED) {
                req.setAssignedTeam(teams.get((i + 1) % teams.size()));
                req.setAssignedUser(users.get((i + 2) % users.size()));
                req.setRespondedAt(createdAt.plus(random.nextInt(8) + 1, ChronoUnit.HOURS));
            }
            if (status == QaServiceRequest.RequestStatus.COMPLETED || status == QaServiceRequest.RequestStatus.CLOSED) {
                req.setResolvedAt(createdAt.plus(random.nextInt(72) + 4, ChronoUnit.HOURS));
                req.setActualHours(service.getEstimatedHours().multiply(new BigDecimal("0.8").add(new BigDecimal(random.nextDouble() * 0.6))));
            }

            // Add activities inline before save
            req.getActivities().add(QaRequestActivity.builder()
                .request(req).user(requestingUser)
                .activityType(QaRequestActivity.ActivityType.STATUS_CHANGE)
                .newValue("SUBMITTED").comment("Request created")
                .build());
            if (req.getAssignedUser() != null) {
                req.getActivities().add(QaRequestActivity.builder()
                    .request(req).user(users.get(1))
                    .activityType(QaRequestActivity.ActivityType.ASSIGNMENT)
                    .newValue(req.getAssignedUser().getDisplayName())
                    .comment("Assigned by QA Manager")
                    .build());
            }

            requests.add(req);
        }
        qaServiceRequestRepository.saveAll(requests);

        // Advance the sequence past the seeded request numbers
        for (int i = 0; i < titles.length; i++) {
            qaServiceRequestRepository.getNextRequestNumber();
        }
    }

    private int computeDemoPriority(QaServiceRequest.Impact impact, QaServiceRequest.Urgency urgency) {
        int[][] matrix = {
            {1, 1, 2, 2},
            {1, 2, 2, 3},
            {2, 2, 3, 4},
            {2, 3, 4, 5}
        };
        return matrix[impact.ordinal()][urgency.ordinal()];
    }

    private void seedCapacityAllocations(List<Team> teams, List<User> users) {
        LocalDate thisMonday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        List<CapacityAllocation> allocations = new ArrayList<>();

        for (int week = 0; week < 4; week++) {
            LocalDate weekStart = thisMonday.plusWeeks(week);
            for (Team team : teams) {
                BigDecimal total = new BigDecimal(120 + random.nextInt(40));
                BigDecimal allocated = total.multiply(new BigDecimal("0.5").add(new BigDecimal(random.nextDouble() * 0.45)));
                allocations.add(CapacityAllocation.builder()
                    .team(team).weekStart(weekStart)
                    .totalHours(total).allocatedHours(allocated.setScale(1, java.math.RoundingMode.HALF_UP))
                    .notes(week == 0 ? "Current sprint capacity" : null)
                    .build());
            }
        }
        capacityAllocationRepository.saveAll(allocations);
    }

    private void seedDocumentationAssets(List<Project> projects, List<User> users) {
        String[][] docs = {
            {"REST API v2 Specification", "OpenAPI 3.0 specification for public REST API", "API_SPEC", "https://api.example.com/docs/openapi.yaml", null, "TECHNICAL", "90"},
            {"Customer Portal User Guide", "End-user guide for the customer portal", "USER_GUIDE", "https://docs.example.com/portal/guide", null, "USER_FACING", "60"},
            {"Auth Service Runbook", "Operational runbook for authentication service", "RUNBOOK", null, "services/auth/RUNBOOK.md", "TECHNICAL", "30"},
            {"Platform Architecture Overview", "High-level architecture and design decisions", "ARCHITECTURE", "https://wiki.example.com/architecture", null, "TECHNICAL", "180"},
            {"Release 2.4.0 Test Plan", "Comprehensive test plan for Q4 release", "TEST_PLAN", null, "releases/2.4.0/test-plan.md", "PROCESS", "14"},
            {"v2.3.0 Release Notes", "Release notes for Q3 release", "RELEASE_NOTES", "https://docs.example.com/releases/2.3.0", null, "USER_FACING", "365"},
            {"New Developer Onboarding", "Step-by-step guide for new team members", "ONBOARDING", "https://wiki.example.com/onboarding", null, "INTERNAL", "90"},
            {"Security Compliance Policy", "Security standards and compliance requirements", "POLICY", null, "policies/security-compliance.md", "COMPLIANCE", "180"},
            {"QA Best Practices Wiki", "Team wiki for QA methodologies and standards", "WIKI", "https://wiki.example.com/qa-practices", null, "PROCESS", "60"},
            {"GraphQL Schema Documentation", "GraphQL API schema and usage examples", "API_SPEC", "https://api.example.com/graphql/docs", null, "TECHNICAL", "45"},
            {"Mobile App Testing Guide", "Testing procedures for iOS and Android apps", "TEST_PLAN", null, "mobile/testing-guide.md", "PROCESS", "30"},
            {"CI/CD Pipeline Runbook", "Pipeline configuration and troubleshooting", "RUNBOOK", null, "devops/cicd-runbook.md", "TECHNICAL", "60"},
            {"GDPR Data Processing Agreement", "Data processing and privacy compliance docs", "POLICY", "https://legal.example.com/gdpr-dpa", null, "COMPLIANCE", "365"},
            {"Payment Gateway Integration", "Technical integration guide for payments", "USER_GUIDE", "https://docs.example.com/payments", null, "TECHNICAL", "90"},
            {"Accessibility Standards Guide", "WCAG 2.1 AA compliance documentation", "POLICY", "https://wiki.example.com/accessibility", null, "COMPLIANCE", "120"}
        };

        DocumentationAsset.HealthStatus[] statuses = {
            DocumentationAsset.HealthStatus.HEALTHY,
            DocumentationAsset.HealthStatus.HEALTHY,
            DocumentationAsset.HealthStatus.WARNING,
            DocumentationAsset.HealthStatus.HEALTHY,
            DocumentationAsset.HealthStatus.HEALTHY,
            DocumentationAsset.HealthStatus.CRITICAL,
            DocumentationAsset.HealthStatus.WARNING,
            DocumentationAsset.HealthStatus.HEALTHY,
            DocumentationAsset.HealthStatus.WARNING,
            DocumentationAsset.HealthStatus.UNKNOWN,
            DocumentationAsset.HealthStatus.HEALTHY,
            DocumentationAsset.HealthStatus.HEALTHY,
            DocumentationAsset.HealthStatus.CRITICAL,
            DocumentationAsset.HealthStatus.HEALTHY,
            DocumentationAsset.HealthStatus.WARNING
        };

        List<DocumentationAsset> assets = new ArrayList<>();
        for (int i = 0; i < docs.length; i++) {
            String[] d = docs[i];
            User owner = (i % 3 == 2) ? null : users.get(i % users.size()); // some without owner
            Project project = projects.get(i % projects.size());

            Instant lastReviewed = (statuses[i] == DocumentationAsset.HealthStatus.CRITICAL)
                ? Instant.now().minus(200 + random.nextInt(100), ChronoUnit.DAYS)
                : (statuses[i] == DocumentationAsset.HealthStatus.WARNING)
                    ? Instant.now().minus(50 + random.nextInt(30), ChronoUnit.DAYS)
                    : Instant.now().minus(random.nextInt(30), ChronoUnit.DAYS);

            DocumentationAsset asset = DocumentationAsset.builder()
                .organization(defaultOrg)
                .project(project)
                .title(d[0])
                .description(d[1])
                .docType(DocumentationAsset.DocType.valueOf(d[2]))
                .url(d[3])
                .repoPath(d[4])
                .category(DocumentationAsset.Category.valueOf(d[5]))
                .owner(owner)
                .healthStatus(statuses[i])
                .lastCheckedAt(Instant.now().minus(random.nextInt(7), ChronoUnit.DAYS))
                .lastReviewedAt(lastReviewed)
                .reviewCycleDays(Integer.parseInt(d[6]))
                .build();

            // Add health checks inline
            if (statuses[i] != DocumentationAsset.HealthStatus.UNKNOWN) {
                DocumentationCheck.CheckStatus linkStatus = (statuses[i] == DocumentationAsset.HealthStatus.CRITICAL && d[3] != null)
                    ? DocumentationCheck.CheckStatus.FAIL : DocumentationCheck.CheckStatus.PASS;
                if (d[3] != null) {
                    asset.getChecks().add(DocumentationCheck.builder()
                        .asset(asset)
                        .checkType(DocumentationCheck.CheckType.BROKEN_LINK)
                        .status(linkStatus)
                        .message(linkStatus == DocumentationCheck.CheckStatus.PASS ? "URL reachable" : "URL returned HTTP 404")
                        .build());
                }

                DocumentationCheck.CheckStatus ownerStatus = (owner == null)
                    ? DocumentationCheck.CheckStatus.FAIL : DocumentationCheck.CheckStatus.PASS;
                asset.getChecks().add(DocumentationCheck.builder()
                    .asset(asset)
                    .checkType(DocumentationCheck.CheckType.MISSING_OWNER)
                    .status(ownerStatus)
                    .message(owner != null ? "Owner assigned: " + owner.getDisplayName() : "No owner assigned")
                    .build());

                DocumentationCheck.CheckStatus reviewStatus = (statuses[i] == DocumentationAsset.HealthStatus.CRITICAL)
                    ? DocumentationCheck.CheckStatus.FAIL
                    : (statuses[i] == DocumentationAsset.HealthStatus.WARNING)
                        ? DocumentationCheck.CheckStatus.WARNING : DocumentationCheck.CheckStatus.PASS;
                asset.getChecks().add(DocumentationCheck.builder()
                    .asset(asset)
                    .checkType(DocumentationCheck.CheckType.STALE_REVIEW)
                    .status(reviewStatus)
                    .message("Review " + (reviewStatus == DocumentationCheck.CheckStatus.PASS ? "up to date" : "overdue"))
                    .build());
            }

            // Add reviews for some assets
            if (i % 2 == 0) {
                User reviewer = users.get((i + 1) % users.size());
                DocumentationReview.ReviewStatus rStatus = (i % 4 == 0)
                    ? DocumentationReview.ReviewStatus.APPROVED
                    : DocumentationReview.ReviewStatus.PENDING;
                DocumentationReview review = DocumentationReview.builder()
                    .asset(asset)
                    .reviewer(reviewer)
                    .status(rStatus)
                    .dueDate(LocalDate.now().plusDays(random.nextInt(30)))
                    .completedAt(rStatus == DocumentationReview.ReviewStatus.APPROVED ? Instant.now().minus(random.nextInt(10), ChronoUnit.DAYS) : null)
                    .comment(rStatus == DocumentationReview.ReviewStatus.APPROVED ? "Content verified and approved" : null)
                    .build();
                asset.getReviews().add(review);
            }

            assets.add(asset);
        }

        documentationAssetRepository.saveAll(assets);
        log.info("Seeded {} documentation assets with health checks and reviews", assets.size());
    }

    private void seedReleaseConfidence(List<Release> releases, List<User> users) {
        QualityGate gate = qualityGateRepository.findByOrganizationIdAndDefaultGateTrue(defaultOrg.getId())
            .orElse(null);
        if (gate == null) return;

        List<QualityGateCondition> conditions = gate.getConditions();

        // For each Q4 release (index 1, 3, 5, ... = QA_IN_PROGRESS), generate readiness reports
        for (int i = 0; i < releases.size(); i++) {
            Release release = releases.get(i);
            boolean isCurrentRelease = release.getStatus() == Release.ReleaseStatus.QA_IN_PROGRESS;
            boolean isReleasedRelease = release.getStatus() == Release.ReleaseStatus.RELEASED;

            if (!isCurrentRelease && !isReleasedRelease) continue;

            // Generate 2 readiness reports per release
            for (int rpt = 0; rpt < 2; rpt++) {
                BigDecimal passRate = isReleasedRelease
                    ? new BigDecimal("92." + random.nextInt(8))
                    : new BigDecimal(70 + random.nextInt(25) + "." + random.nextInt(10));
                long criticalDefects = isReleasedRelease ? 0 : random.nextInt(3);
                long highDefects = isReleasedRelease ? 0 : random.nextInt(5);
                BigDecimal buildSuccess = new BigDecimal(85 + random.nextInt(15) + "." + random.nextInt(10));

                BigDecimal defectScore = BigDecimal.valueOf(100);
                if (criticalDefects > 0) defectScore = BigDecimal.ZERO;
                else if (highDefects > 0) defectScore = BigDecimal.valueOf(Math.max(0, 100 - highDefects * 20));

                BigDecimal overallScore = passRate.multiply(new BigDecimal("0.40"))
                    .add(buildSuccess.multiply(new BigDecimal("0.30")))
                    .add(defectScore.multiply(new BigDecimal("0.30")))
                    .setScale(2, java.math.RoundingMode.HALF_UP);

                boolean hasCriticalFail = criticalDefects > 0;
                boolean hasHighFail = highDefects > 3;
                ReleaseReadinessReport.GateStatus gateStatus = hasCriticalFail
                    ? ReleaseReadinessReport.GateStatus.FAIL
                    : (hasHighFail ? ReleaseReadinessReport.GateStatus.WARN : ReleaseReadinessReport.GateStatus.PASS);

                // Force released to PASS
                if (isReleasedRelease && rpt == 1) {
                    gateStatus = ReleaseReadinessReport.GateStatus.PASS;
                    overallScore = new BigDecimal("92.50");
                }

                ReleaseReadinessReport report = ReleaseReadinessReport.builder()
                    .release(release)
                    .overallScore(overallScore)
                    .gateStatus(gateStatus)
                    .summary(String.format("Release %s: score %.1f%%, gate %s",
                        release.getReleaseVersion(), overallScore, gateStatus))
                    .metrics("{}")
                    .generatedAt(Instant.now().minus((2 - rpt) * 7L, ChronoUnit.DAYS))
                    .generatedBy(users.get(1)) // Marcus (QA Manager)
                    .build();

                // Add gate results for each condition
                for (QualityGateCondition condition : conditions) {
                    BigDecimal actual = switch (condition.getMetricKey()) {
                        case "pass_rate" -> passRate;
                        case "critical_defects" -> BigDecimal.valueOf(criticalDefects);
                        case "high_defects" -> BigDecimal.valueOf(highDefects);
                        default -> BigDecimal.ZERO;
                    };

                    boolean passes = evaluateDemoCondition(actual, condition.getThresholdValue(), condition.getOperator());

                    report.getGateResults().add(ReleaseGateResult.builder()
                        .report(report)
                        .condition(condition)
                        .metricKey(condition.getMetricKey())
                        .actualValue(actual)
                        .expectedValue(condition.getThresholdValue())
                        .operator(condition.getOperator())
                        .status(passes ? ReleaseGateResult.ResultStatus.PASS : ReleaseGateResult.ResultStatus.FAIL)
                        .critical(condition.isCritical())
                        .build());
                }

                releaseReadinessReportRepository.save(report);
            }

            // Add sign-offs for released releases
            if (isReleasedRelease) {
                for (int u = 0; u < Math.min(3, users.size()); u++) {
                    User user = users.get(u);
                    String roleName = user.getRoles().stream()
                        .map(Role::getName).findFirst().orElse("UNKNOWN");
                    releaseSignOffRepository.save(ReleaseSignOff.builder()
                        .release(release)
                        .user(user)
                        .role(roleName)
                        .decision(ReleaseSignOff.Decision.APPROVED)
                        .comment("Release looks good, approved")
                        .decidedAt(Instant.now().minus(random.nextInt(7), ChronoUnit.DAYS))
                        .build());
                }
            }
            // Add partial sign-offs for QA_IN_PROGRESS releases
            else if (isCurrentRelease) {
                releaseSignOffRepository.save(ReleaseSignOff.builder()
                    .release(release)
                    .user(users.get(1)) // QA Manager
                    .role("QA_MANAGER")
                    .decision(ReleaseSignOff.Decision.APPROVED)
                    .comment("QA testing complete, approved with caveats")
                    .decidedAt(Instant.now().minus(1, ChronoUnit.DAYS))
                    .build());
            }
        }

        log.info("Seeded release confidence data (readiness reports + sign-offs)");
    }

    private void seedIntegrations(List<Project> projects) {
        String[][] integrations = {
            {"GitHub CI/CD - Platform API", "GitHub Actions workflow integration for main API", "GITHUB"},
            {"GitHub CI/CD - Web Portal", "GitHub Actions for frontend builds", "GITHUB"},
            {"Jenkins - Nightly Regression", "Jenkins nightly regression test pipeline", "JENKINS"},
            {"GitLab - Mobile App Pipeline", "GitLab CI pipeline for mobile testing", "GITLAB"},
            {"Jira - Bug Tracking Sync", "Jira webhook for defect synchronization", "JIRA"},
            {"Generic - Selenium Grid", "Custom webhook from Selenium Grid test runner", "GENERIC"},
        };

        IntegrationConfig.Provider[] providers = {
            IntegrationConfig.Provider.GITHUB, IntegrationConfig.Provider.GITHUB,
            IntegrationConfig.Provider.JENKINS, IntegrationConfig.Provider.GITLAB,
            IntegrationConfig.Provider.JIRA, IntegrationConfig.Provider.GENERIC
        };

        String[] eventTypes = {"workflow_run", "push", "build_completed", "Pipeline Hook", "jira:issue_created", "test_complete"};
        WebhookEvent.EventStatus[] statuses = {
            WebhookEvent.EventStatus.PROCESSED, WebhookEvent.EventStatus.PROCESSED,
            WebhookEvent.EventStatus.PROCESSED, WebhookEvent.EventStatus.FAILED,
            WebhookEvent.EventStatus.PROCESSED, WebhookEvent.EventStatus.SKIPPED
        };

        for (int i = 0; i < integrations.length; i++) {
            Project project = projects.get(i % projects.size());

            IntegrationConfig config = IntegrationConfig.builder()
                .organization(defaultOrg)
                .project(project)
                .name(integrations[i][0])
                .description(integrations[i][1])
                .provider(providers[i])
                .webhookSecret("whsec_demo_" + i)
                .enabled(i != 3) // disable GitLab one
                .config("{}")
                .eventCount(5 + random.nextInt(50))
                .lastEventAt(Instant.now().minus(random.nextInt(48), ChronoUnit.HOURS))
                .build();

            config = integrationConfigRepository.save(config);

            // Create 3-5 webhook events per integration
            int eventCount = 3 + random.nextInt(3);
            for (int e = 0; e < eventCount; e++) {
                WebhookEvent.EventStatus status = (e == 0) ? statuses[i] : WebhookEvent.EventStatus.PROCESSED;
                WebhookEvent event = WebhookEvent.builder()
                    .integration(config)
                    .eventType(eventTypes[i])
                    .provider(providers[i])
                    .status(status)
                    .payload("{\"action\":\"completed\",\"demo\":true}")
                    .headers("{\"content-type\":\"application/json\"}")
                    .processedAt(status != WebhookEvent.EventStatus.RECEIVED ? Instant.now().minus(e, ChronoUnit.HOURS) : null)
                    .processingError(status == WebhookEvent.EventStatus.FAILED ? "Connection timeout to processing service" : null)
                    .build();
                webhookEventRepository.save(event);
            }
        }

        log.info("Seeded {} integrations with webhook events", integrations.length);
    }

    private boolean evaluateDemoCondition(BigDecimal actual, BigDecimal expected, QualityGateCondition.Operator operator) {
        int cmp = actual.compareTo(expected);
        return switch (operator) {
            case GTE -> cmp >= 0;
            case GT -> cmp > 0;
            case LTE -> cmp <= 0;
            case LT -> cmp < 0;
            case EQ -> cmp == 0;
        };
    }
}
