export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface Team {
  id: string;
  name: string;
  description: string | null;
  projectCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface Project {
  id: string;
  name: string;
  description: string | null;
  status: 'ACTIVE' | 'ARCHIVED';
  teamId: string;
  teamName: string;
  testRunCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface TestRun {
  id: string;
  name: string;
  source: 'PLAYWRIGHT' | 'JUNIT' | 'WEBDRIVERIO' | 'CYPRESS' | 'TESTNG';
  environment: string | null;
  totalTests: number;
  passed: number;
  failed: number;
  skipped: number;
  durationMs: number | null;
  passRate: number;
  commitSha: string | null;
  branch: string | null;
  executedAt: string;
  projectId: string;
  projectName: string;
  createdAt: string;
}

export interface TestCase {
  id: string;
  name: string;
  className: string | null;
  suiteName: string | null;
  status: 'PASSED' | 'FAILED' | 'SKIPPED' | 'ERROR';
  durationMs: number | null;
  errorMessage: string | null;
  stackTrace: string | null;
}

export interface TestRunDetail extends TestRun {
  testCases: TestCase[];
}

export interface CreateTeamRequest {
  name: string;
  description?: string;
}

export interface UpdateTeamRequest {
  name?: string;
  description?: string;
}

export interface CreateProjectRequest {
  name: string;
  description?: string;
  teamId: string;
}

export interface UpdateProjectRequest {
  name?: string;
  description?: string;
  status?: 'ACTIVE' | 'ARCHIVED';
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  userId: string;
  email: string;
  displayName: string;
  organizationId: string;
  permissions: string[];
}

export interface DashboardData {
  summary: {
    totalTeams: number;
    totalProjects: number;
    totalTestRuns: number;
    totalTestCases: number;
    totalDefects: number;
    openDefects: number;
    averagePassRate: number;
    averageQualityScore: number;
    flakyTestCount: number;
  };
  passRateTrend: {
    date: string;
    passRate: number;
    totalTests: number;
    passed: number;
    failed: number;
  }[];
  projectHealth: {
    projectId: string;
    projectName: string;
    teamName: string;
    passRate: number | null;
    qualityScore: number | null;
    openDefects: number;
    totalTestRuns: number;
    status: string;
  }[];
  defectsByStatus: { label: string; count: number }[];
  defectsBySeverity: { label: string; count: number }[];
  recentTestRuns: {
    id: string;
    name: string;
    projectName: string;
    source: string;
    totalTests: number;
    passed: number;
    failed: number;
    skipped: number;
    passRate: number;
    executedAt: string;
  }[];
}

export interface QaServiceItem {
  id: string;
  name: string;
  description: string;
  category: string;
  estimatedHours: number | null;
  active: boolean;
  createdAt: string;
}

export interface QaServiceRequestItem {
  id: string;
  requestNumber: string;
  title: string;
  description: string;
  status: string;
  impact: string;
  urgency: string;
  priorityScore: number;
  priorityOverride: number | null;
  effectivePriority: number;
  overrideReason: string | null;
  qaServiceId: string;
  qaServiceName: string;
  qaServiceCategory: string;
  requestingTeamId: string;
  requestingTeamName: string;
  requestingUserId: string;
  requestingUserName: string;
  projectId: string | null;
  projectName: string | null;
  assignedTeamId: string | null;
  assignedTeamName: string | null;
  assignedUserId: string | null;
  assignedUserName: string | null;
  estimatedHours: number | null;
  actualHours: number | null;
  responseDeadline: string | null;
  resolutionDeadline: string | null;
  respondedAt: string | null;
  resolvedAt: string | null;
  createdAt: string;
  updatedAt: string;
  activities: QaRequestActivity[];
}

export interface QaRequestActivity {
  id: string;
  userId: string | null;
  userName: string | null;
  activityType: string;
  oldValue: string | null;
  newValue: string | null;
  comment: string | null;
  createdAt: string;
}

export interface QaRequestDashboard {
  totalRequests: number;
  openRequests: number;
  inProgressRequests: number;
  completedRequests: number;
  cancelledRequests: number;
  slaBreachCount: number;
  requestsByStatus: { status: string; count: number }[];
  requestsByPriority: { priority: number; count: number }[];
  requestsByService: { serviceName: string; category: string; count: number }[];
}

export interface DocumentationAssetItem {
  id: string;
  title: string;
  description: string | null;
  docType: string;
  url: string | null;
  repoPath: string | null;
  category: string;
  ownerId: string | null;
  ownerName: string | null;
  projectId: string | null;
  projectName: string | null;
  healthStatus: string;
  lastCheckedAt: string | null;
  lastReviewedAt: string | null;
  reviewCycleDays: number;
  createdAt: string;
  updatedAt: string;
  recentChecks: DocumentationCheck[];
  recentReviews: DocumentationReview[];
}

export interface DocumentationCheck {
  id: string;
  checkType: string;
  status: string;
  message: string | null;
  executedAt: string;
}

export interface DocumentationReview {
  id: string;
  reviewerId: string | null;
  reviewerName: string | null;
  status: string;
  dueDate: string | null;
  completedAt: string | null;
  comment: string | null;
  createdAt: string;
}

export interface DocumentationDashboard {
  totalAssets: number;
  healthyCount: number;
  warningCount: number;
  criticalCount: number;
  unknownCount: number;
  missingOwnerCount: number;
  pendingReviews: number;
  assetsByType: { type: string; count: number }[];
}

// Release Confidence
export interface GateResultItem {
  id: string;
  conditionId: string | null;
  metricKey: string;
  actualValue: number;
  expectedValue: number;
  operator: string;
  status: 'PASS' | 'FAIL';
  critical: boolean;
}

export interface MetricsSnapshot {
  passRate: number | null;
  totalTestRuns: number;
  openDefects: number;
  criticalDefects: number;
  highDefects: number;
  totalBuilds: number;
  successfulBuilds: number;
  buildSuccessRate: number | null;
}

export interface ReadinessReport {
  id: string;
  releaseId: string;
  releaseName: string;
  releaseVersion: string;
  overallScore: number;
  gateStatus: 'PASS' | 'WARN' | 'FAIL';
  summary: string;
  generatedAt: string;
  generatedByName: string | null;
  gateResults: GateResultItem[];
  metrics: MetricsSnapshot | null;
}

export interface SignOff {
  id: string;
  releaseId: string;
  userId: string;
  userName: string;
  role: string;
  decision: 'APPROVED' | 'REJECTED';
  comment: string | null;
  decidedAt: string;
}

export interface ReleaseReadinessSummary {
  releaseId: string;
  releaseName: string;
  releaseVersion: string;
  projectName: string;
  releaseStatus: string;
  targetDate: string | null;
  overallScore: number | null;
  gateStatus: string | null;
  approvedCount: number;
  rejectedCount: number;
  lastEvaluatedAt: string | null;
}

export interface ReleaseDashboard {
  activeReleases: ReleaseReadinessSummary[];
  totalActiveReleases: number;
  passingReleases: number;
  warningReleases: number;
  failingReleases: number;
}

// External Integrations
export interface IntegrationConfig {
  id: string;
  name: string;
  description: string | null;
  provider: string;
  enabled: boolean;
  hasSecret: boolean;
  projectId: string | null;
  projectName: string | null;
  eventCount: number;
  lastEventAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface WebhookEventItem {
  id: string;
  integrationId: string;
  eventType: string;
  provider: string;
  status: string;
  processingError: string | null;
  processedAt: string | null;
  receivedAt: string;
  payload: string | null;
}

export interface IntegrationDashboard {
  totalIntegrations: number;
  activeIntegrations: number;
  totalEventsProcessed: number;
  totalEventsFailed: number;
  byProvider: { provider: string; count: number }[];
  eventsByStatus: { status: string; count: number }[];
}

// User Management
export interface UserProfile {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  displayName: string;
  active: boolean;
  roleName: string;
  roleId: string;
  lastLoginAt: string | null;
  createdAt: string;
}

// Trend Analytics
export interface PassRateTrendPoint {
  date: string;
  passRate: number;
  totalTests: number;
}

export interface PassRateTrendData {
  dataPoints: PassRateTrendPoint[];
}

export interface DefectTrendPoint {
  date: string;
  opened: number;
  resolved: number;
}

export interface DefectTrendData {
  dataPoints: DefectTrendPoint[];
}

export interface TrendPoint {
  date: string;
  count: number;
}

export interface TrendPointData {
  dataPoints: TrendPoint[];
}

// Audit Trail
export interface AuditEventItem {
  id: string;
  entityType: string;
  entityId: string | null;
  action: string;
  details: string;
  userName: string | null;
  ipAddress: string | null;
  createdAt: string;
}

// Test Data Management
export interface TestDataTemplate {
  id: string;
  name: string;
  description: string | null;
  projectId: string | null;
  projectName: string | null;
  fieldCount: number;
  profileCount: number;
  generationCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface TestDataTemplateDetail extends TestDataTemplate {
  fields: DataFieldItem[];
  profiles: TestDataProfileItem[];
}

export interface DataFieldItem {
  id: string;
  name: string;
  fieldType: string;
  displayOrder: number;
  nullable: boolean;
  fakerGenerator: string | null;
  constraints: string | null;
}

export interface TestDataProfileItem {
  id: string;
  name: string;
  profileType: string;
  description: string | null;
  overrides: string | null;
  createdAt: string;
}

export interface TestDataGenerationItem {
  id: string;
  templateId: string;
  templateName: string;
  profileId: string | null;
  profileName: string | null;
  rowCount: number;
  status: string;
  exportFormat: string;
  errorMessage: string | null;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
}

export interface TestDataDashboard {
  totalTemplates: number;
  totalProfiles: number;
  totalGenerations: number;
  recentGenerations: TestDataGenerationItem[];
}

export interface CapacityOverview {
  teams: {
    teamId: string;
    teamName: string;
    totalHours: number;
    allocatedHours: number;
    availableHours: number;
    activeRequests: number;
    estimatedWorkload: number;
    utilizationLevel: string;
  }[];
  totalAvailableHours: number;
  totalAllocatedHours: number;
  totalActiveRequests: number;
}

// Contributors
export interface ContributorItem {
  id: string;
  externalId: string;
  provider: string;
  username: string;
  displayName: string | null;
  email: string | null;
  avatarUrl: string | null;
  totalCommits: number;
  totalPipelines: number;
  totalMerges: number;
  lastActivityAt: string | null;
  linkedUserId: string | null;
  linkedUserName: string | null;
  createdAt: string;
}

export interface ContributorActivityItem {
  id: string;
  contributorId: string;
  activityType: string;
  projectId: string | null;
  projectName: string | null;
  branch: string | null;
  commitSha: string | null;
  commitMessage: string | null;
  pipelineId: string | null;
  pipelineStatus: string | null;
  createdAt: string;
}

export interface ContributorDetail {
  contributor: ContributorItem;
  activityBreakdown: { activityType: string; count: number }[];
  projects: { projectId: string; projectName: string }[];
}
