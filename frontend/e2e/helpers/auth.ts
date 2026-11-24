import type { Page } from '@playwright/test';

/** Demo credentials used by the seed data */
export const DEMO_USER = {
  email: 'sarah.chen@demo.oqh',
  password: 'Demo123!',
  displayName: 'Sarah Chen',
  userId: '00000000-0000-0000-0000-000000000001',
  organizationId: '00000000-0000-0000-0000-000000000100',
};

/** Standard mock auth response returned by the login API */
export const MOCK_AUTH_RESPONSE = {
  accessToken: 'mock-access-token-for-e2e-tests',
  refreshToken: 'mock-refresh-token-for-e2e-tests',
  tokenType: 'Bearer',
  userId: DEMO_USER.userId,
  email: DEMO_USER.email,
  displayName: DEMO_USER.displayName,
  organizationId: DEMO_USER.organizationId,
  permissions: [
    'TEAM_READ', 'TEAM_WRITE', 'TEAM_DELETE',
    'PROJECT_READ', 'PROJECT_WRITE', 'PROJECT_DELETE',
    'TEST_RUN_READ', 'TEST_RUN_WRITE',
    'DEFECT_READ', 'DEFECT_WRITE',
    'QA_SERVICE_READ', 'QA_SERVICE_WRITE',
    'QA_REQUEST_READ', 'QA_REQUEST_WRITE',
    'DOCUMENTATION_READ', 'DOCUMENTATION_WRITE',
    'RELEASE_READINESS_READ', 'RELEASE_READINESS_WRITE', 'RELEASE_SIGNOFF',
    'INTEGRATION_READ', 'INTEGRATION_WRITE', 'INTEGRATION_DELETE',
    'TEST_DATA_READ', 'TEST_DATA_WRITE',
    'FAILURE_ANALYSIS_READ', 'FAILURE_ANALYSIS_WRITE',
    'USER_READ', 'USER_WRITE',
    'AUDIT_READ',
  ],
};

/** Mock dashboard payload used when verifying the dashboard loads */
export const MOCK_DASHBOARD_DATA = {
  summary: {
    totalTeams: 4,
    totalProjects: 8,
    totalTestRuns: 240,
    totalTestCases: 1200,
    totalDefects: 44,
    openDefects: 12,
    averagePassRate: 87.5,
    averageQualityScore: 78.3,
    flakyTestCount: 6,
  },
  passRateTrend: [
    { date: '2026-10-15', passRate: 85.2, totalTests: 120, passed: 102, failed: 18 },
    { date: '2026-10-16', passRate: 88.1, totalTests: 118, passed: 104, failed: 14 },
  ],
  projectHealth: [
    {
      projectId: 'p1',
      projectName: 'Auth Service',
      teamName: 'Platform Team',
      passRate: 92.1,
      qualityScore: 85.0,
      openDefects: 2,
      totalTestRuns: 45,
      status: 'ACTIVE',
    },
  ],
  defectsBySeverity: [
    { label: 'CRITICAL', count: 3 },
    { label: 'HIGH', count: 8 },
    { label: 'MEDIUM', count: 15 },
    { label: 'LOW', count: 18 },
  ],
  defectsByStatus: [
    { label: 'OPEN', count: 12 },
    { label: 'IN_PROGRESS', count: 8 },
    { label: 'RESOLVED', count: 18 },
    { label: 'CLOSED', count: 6 },
  ],
  recentTestRuns: [
    {
      id: 'tr1',
      name: 'Auth API regression',
      projectName: 'Auth Service',
      source: 'PLAYWRIGHT',
      totalTests: 50,
      passed: 47,
      failed: 3,
      skipped: 0,
      passRate: 94.0,
      executedAt: '2026-11-10T14:30:00Z',
    },
  ],
};

/** Mock teams list response */
export const MOCK_TEAMS_PAGE = {
  content: [
    {
      id: 't1',
      name: 'Platform Team',
      description: 'Core platform quality engineering',
      projectCount: 3,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    },
    {
      id: 't2',
      name: 'Mobile Team',
      description: 'Mobile application testing',
      projectCount: 2,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    },
  ],
  totalElements: 2,
  totalPages: 1,
  size: 20,
  number: 0,
  first: true,
  last: true,
};

/**
 * Intercept API routes to mock all backend calls required
 * for a fully functional frontend without a running backend.
 */
export async function mockApiRoutes(page: Page) {
  // Auth login endpoint
  await page.route('**/api/v1/auth/login', async (route) => {
    const request = route.request();
    const body = request.postDataJSON();

    if (body?.email === DEMO_USER.email && body?.password === DEMO_USER.password) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(MOCK_AUTH_RESPONSE),
      });
    } else {
      // Use 400 instead of 401 to avoid triggering the Axios 401 interceptor
      // which does a hard window.location redirect that resets the React app
      await route.fulfill({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({ message: 'Invalid email or password' }),
      });
    }
  });

  // Dashboard
  await page.route('**/api/v1/dashboard', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(MOCK_DASHBOARD_DATA),
    });
  });

  // Teams
  await page.route('**/api/v1/teams*', async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(MOCK_TEAMS_PAGE),
      });
    } else if (route.request().method() === 'POST') {
      const body = route.request().postDataJSON();
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 'new-team-id',
          name: body.name,
          description: body.description ?? null,
          projectCount: 0,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        }),
      });
    } else {
      await route.continue();
    }
  });

  // Projects (needed for navigation tests)
  await page.route('**/api/v1/projects*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: 20,
        number: 0,
        first: true,
        last: true,
      }),
    });
  });

  // Test runs
  await page.route('**/api/v1/test-runs*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: 20,
        number: 0,
        first: true,
        last: true,
      }),
    });
  });

  // QA requests (use ** to also match sub-paths like /dashboard)
  await page.route('**/api/v1/qa-requests/**', async (route) => {
    if (route.request().url().includes('/dashboard')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          totalRequests: 0,
          openRequests: 0,
          inProgressRequests: 0,
          completedRequests: 0,
          cancelledRequests: 0,
          slaBreachCount: 0,
          requestsByStatus: [],
          requestsByPriority: [],
          requestsByService: [],
        }),
      });
    } else {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [],
          totalElements: 0,
          totalPages: 0,
          size: 20,
          number: 0,
          first: true,
          last: true,
        }),
      });
    }
  });

  // QA requests list (no sub-path, matches query params via trailing *)
  await page.route('**/api/v1/qa-requests*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: 20,
        number: 0,
        first: true,
        last: true,
      }),
    });
  });

  // QA services
  await page.route('**/api/v1/qa-services*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: 20,
        number: 0,
        first: true,
        last: true,
      }),
    });
  });

  // Capacity (matches sub-paths like /overview)
  await page.route('**/api/v1/capacity/**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        teams: [],
        totalAvailableHours: 0,
        totalAllocatedHours: 0,
        totalActiveRequests: 0,
      }),
    });
  });

  // Documentation (sub-paths including /dashboard)
  await page.route('**/api/v1/documentation/**', async (route) => {
    if (route.request().url().includes('/dashboard')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          totalAssets: 0,
          healthyCount: 0,
          warningCount: 0,
          criticalCount: 0,
          unknownCount: 0,
          missingOwnerCount: 0,
          pendingReviews: 0,
          assetsByType: [],
        }),
      });
    } else {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [],
          totalElements: 0,
          totalPages: 0,
          size: 20,
          number: 0,
          first: true,
          last: true,
        }),
      });
    }
  });

  // Documentation list (no sub-path, matches query params via trailing *)
  await page.route('**/api/v1/documentation*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: 20,
        number: 0,
        first: true,
        last: true,
      }),
    });
  });

  // Releases (sub-paths including /dashboard/readiness)
  await page.route('**/api/v1/releases/**', async (route) => {
    if (route.request().url().includes('/dashboard')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          activeReleases: [],
          totalActiveReleases: 0,
          passingReleases: 0,
          warningReleases: 0,
          failingReleases: 0,
        }),
      });
    } else {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [],
          totalElements: 0,
          totalPages: 0,
          size: 20,
          number: 0,
          first: true,
          last: true,
        }),
      });
    }
  });

  // Releases list (no sub-path, matches query params via trailing *)
  await page.route('**/api/v1/releases*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: 20,
        number: 0,
        first: true,
        last: true,
      }),
    });
  });

  // Integrations (sub-paths including /dashboard)
  await page.route('**/api/v1/integrations/**', async (route) => {
    if (route.request().url().includes('/dashboard')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          totalIntegrations: 0,
          activeIntegrations: 0,
          totalEventsProcessed: 0,
          totalEventsFailed: 0,
          byProvider: [],
          eventsByStatus: [],
        }),
      });
    } else {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [],
          totalElements: 0,
          totalPages: 0,
          size: 20,
          number: 0,
          first: true,
          last: true,
        }),
      });
    }
  });

  // Integrations list (no sub-path, matches query params via trailing *)
  await page.route('**/api/v1/integrations*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: 20,
        number: 0,
        first: true,
        last: true,
      }),
    });
  });

  // Defects
  await page.route('**/api/v1/defects*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [
          {
            id: 'd1', title: 'Login fails on Safari', severity: 'HIGH', priority: 'P1',
            status: 'OPEN', projectId: 'p1', projectName: 'Auth Service',
            assignedTo: 'John Doe', createdAt: '2026-11-01T10:00:00Z',
          },
        ],
        totalElements: 1, totalPages: 1, size: 20, number: 0, first: true, last: true,
      }),
    });
  });

  // Test Data - templates
  await page.route('**/api/v1/test-data/templates**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [
          {
            id: 'td1', name: 'User Registration Data', description: 'Test data for user reg',
            fieldCount: 5, profileCount: 3, generationCount: 12,
            createdAt: '2026-11-01T10:00:00Z',
          },
        ],
        totalElements: 1, totalPages: 1, size: 20, number: 0, first: true, last: true,
      }),
    });
  });

  // Test Data - dashboard
  await page.route('**/api/v1/test-data/dashboard', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        totalTemplates: 5, totalProfiles: 15, totalGenerations: 42,
        totalRowsGenerated: 12500, recentGenerations: [],
      }),
    });
  });

  // Audit trail
  await page.route('**/api/v1/audit*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [
          {
            id: 'a1', action: 'CREATE', entityType: 'TestRun', entityId: 'tr1',
            userName: 'Sarah Chen', userEmail: 'sarah.chen@demo.oqh',
            timestamp: '2026-11-20T14:30:00Z',
          },
        ],
        totalElements: 1, totalPages: 1, size: 20, number: 0, first: true, last: true,
      }),
    });
  });

  // Users
  await page.route('**/api/v1/users*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [
          {
            id: 'u1', email: 'sarah.chen@demo.oqh', displayName: 'Sarah Chen',
            firstName: 'Sarah', lastName: 'Chen',
            active: true, roleName: 'ADMIN', roleId: 'role-admin',
            lastLoginAt: '2026-11-20T10:00:00Z', createdAt: '2026-01-01T00:00:00Z',
          },
        ],
        totalElements: 1, totalPages: 1, size: 20, number: 0, first: true, last: true,
      }),
    });
  });

  // Search
  await page.route('**/api/v1/search*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ results: [], totalResults: 0 }),
    });
  });

  // Notifications sub-paths (unread-count, mark-read, etc.) — MUST be before the list route
  await page.route('**/api/v1/notifications/**', async (route) => {
    if (route.request().url().includes('/unread-count')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(0),
      });
    } else {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '{}' });
    }
  });

  // Notifications list
  await page.route('**/api/v1/notifications*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [], totalElements: 0, totalPages: 0, size: 20, number: 0, first: true, last: true,
      }),
    });
  });

  // Trends (pass-rate, defects, test-volume, flaky)
  await page.route('**/api/v1/trends/**', async (route) => {
    const url = route.request().url();
    if (url.includes('/defects')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ dataPoints: [], period: 30 }),
      });
    } else {
      // pass-rate, test-volume, flaky all use same shape
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ dataPoints: [], period: 30 }),
      });
    }
  });

  // Export
  await page.route('**/api/v1/export/**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'text/csv',
      body: 'id,name\n1,test\n',
    });
  });

  // Failure analysis status
  await page.route('**/api/v1/failure-analysis/status', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ available: true }),
    });
  });

  // Failure analysis by test run (404 = no analysis yet)
  await page.route('**/api/v1/failure-analysis/test-run/**', async (route) => {
    await route.fulfill({
      status: 404,
      contentType: 'application/json',
      body: JSON.stringify({ message: 'Not found' }),
    });
  });

  // SLA configs
  await page.route('**/api/v1/sla*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [], totalElements: 0, totalPages: 0, size: 20, number: 0, first: true, last: true,
      }),
    });
  });

  // Flaky tests
  await page.route('**/api/v1/flaky-tests*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [], totalElements: 0, totalPages: 0, size: 20, number: 0, first: true, last: true,
      }),
    });
  });

}

/**
 * Log in via the UI by filling the login form and submitting.
 * Requires API routes to be mocked beforehand.
 */
export async function loginViaUI(page: Page, email?: string, password?: string) {
  await page.goto('/login');
  await page.locator('input[type="email"]').fill(email ?? DEMO_USER.email);
  await page.locator('input[type="password"]').fill(password ?? DEMO_USER.password);
  await page.getByRole('button', { name: 'Sign in' }).click();
}

/**
 * Set auth state directly in localStorage so the app considers the user logged in.
 * Faster than going through the login form for tests that don't test auth itself.
 */
export async function seedAuthState(page: Page) {
  await page.addInitScript((authData) => {
    window.localStorage.setItem('oqh_auth', JSON.stringify(authData));
  }, {
    accessToken: MOCK_AUTH_RESPONSE.accessToken,
    refreshToken: MOCK_AUTH_RESPONSE.refreshToken,
    userId: MOCK_AUTH_RESPONSE.userId,
    email: MOCK_AUTH_RESPONSE.email,
    displayName: MOCK_AUTH_RESPONSE.displayName,
    organizationId: MOCK_AUTH_RESPONSE.organizationId,
    permissions: MOCK_AUTH_RESPONSE.permissions,
  });
}
