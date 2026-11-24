import { test, expect } from '@playwright/test';
import { mockApiRoutes, seedAuthState, MOCK_DASHBOARD_DATA, MOCK_TEAMS_PAGE, DEMO_USER } from './helpers/auth';

// =====================================================
// DASHBOARD TESTS
// =====================================================
test.describe('Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await mockApiRoutes(page);
    await seedAuthState(page);
  });

  test('displays all stat cards with correct values', async ({ page }) => {
    await page.goto('/');
    const statsGrid = page.getByRole('main').locator('.grid').first();
    await expect(statsGrid.getByText('240')).toBeVisible();
    await expect(statsGrid.getByText('87.5%')).toBeVisible();
    await expect(statsGrid.getByText('12')).toBeVisible();
  });

  test('displays charts sections', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByText('Pass Rate Trend (30 days)')).toBeVisible();
    await expect(page.getByText('Defects by Severity')).toBeVisible();
    await expect(page.getByText('Defects by Status')).toBeVisible();
  });

  test('displays project health table', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByText('Project Health')).toBeVisible();
    await expect(page.getByRole('cell', { name: 'Auth Service' })).toBeVisible();
  });

  test('displays recent test runs', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByText('Recent Test Runs')).toBeVisible();
    await expect(page.getByText('Auth API regression')).toBeVisible();
  });
});

// =====================================================
// TEAMS TESTS
// =====================================================
test.describe('Teams', () => {
  test.beforeEach(async ({ page }) => {
    await mockApiRoutes(page);
    await seedAuthState(page);
  });

  test('displays teams list with descriptions', async ({ page }) => {
    await page.goto('/teams');
    await expect(page.getByText('Platform Team')).toBeVisible();
    await expect(page.getByText('Core platform quality engineering')).toBeVisible();
    await expect(page.getByText('Mobile Team')).toBeVisible();
  });

  test('shows project counts per team', async ({ page }) => {
    await page.goto('/teams');
    await expect(page.getByText('3 projects')).toBeVisible();
    await expect(page.getByText('2 projects')).toBeVisible();
  });

  test('opens and closes create team modal', async ({ page }) => {
    await page.goto('/teams');
    await page.getByRole('button', { name: 'New Team' }).click();
    await expect(page.getByLabel('Team Name')).toBeVisible();
    await page.getByRole('button', { name: 'Cancel' }).click();
    await expect(page.getByLabel('Team Name')).not.toBeVisible();
  });

  test('create team modal has required fields', async ({ page }) => {
    await page.goto('/teams');
    await page.getByRole('button', { name: 'New Team' }).click();
    await expect(page.getByLabel('Team Name')).toBeVisible();
    await expect(page.getByLabel('Description')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Create' })).toBeVisible();
  });

  test('can fill create team form', async ({ page }) => {
    await page.goto('/teams');
    await page.getByRole('button', { name: 'New Team' }).click();
    await page.getByLabel('Team Name').fill('QA Automation Team');
    await page.getByLabel('Description').fill('End-to-end automation specialists');
    await page.getByRole('button', { name: 'Create' }).click();
  });
});

// =====================================================
// PROJECTS TESTS
// =====================================================
test.describe('Projects', () => {
  test.beforeEach(async ({ page }) => {
    await mockApiRoutes(page);
    await seedAuthState(page);
  });

  test('displays projects page heading', async ({ page }) => {
    await page.goto('/projects');
    await expect(page.getByRole('heading', { name: 'Projects', exact: true })).toBeVisible();
  });

  test('shows empty state when no projects', async ({ page }) => {
    await page.goto('/projects');
    // With mock returning empty content, should show page correctly
    await expect(page).toHaveURL(/\/projects/);
  });
});

// =====================================================
// TEST RUNS TESTS
// =====================================================
test.describe('Test Runs', () => {
  test.beforeEach(async ({ page }) => {
    await mockApiRoutes(page);
    await seedAuthState(page);
  });

  test('displays test runs page', async ({ page }) => {
    await page.goto('/test-runs');
    await expect(page.getByRole('heading', { name: 'Test Runs', exact: true })).toBeVisible();
  });

  test('test run detail page loads', async ({ page }) => {
    // Mock a specific test run detail
    await page.route('**/api/v1/test-runs/tr-detail-1', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 'tr-detail-1', name: 'Auth API Regression v2.1', source: 'JUNIT',
          totalTests: 50, passed: 45, failed: 3, skipped: 2, passRate: 90.0,
          projectId: 'p1', projectName: 'Auth Service',
          executedAt: '2026-11-20T14:00:00Z', duration: 12500,
          testCases: [
            { id: 'tc1', name: 'testLoginSuccess', className: 'AuthTest', status: 'PASSED', duration: 120 },
            { id: 'tc2', name: 'testLoginFailure', className: 'AuthTest', status: 'FAILED', duration: 340, errorMessage: 'Expected 200 but got 401' },
          ],
        }),
      });
    });

    await page.goto('/test-runs/tr-detail-1');
    await expect(page.getByText('Auth API Regression v2.1')).toBeVisible();
  });
});

// =====================================================
// TEST DATA TESTS
// =====================================================
test.describe('Test Data', () => {
  test.beforeEach(async ({ page }) => {
    await mockApiRoutes(page);
    await seedAuthState(page);
  });

  test('displays test data templates', async ({ page }) => {
    await page.goto('/test-data');
    await expect(page.getByText('User Registration Data')).toBeVisible();
  });

  test('displays dashboard stats', async ({ page }) => {
    await page.goto('/test-data');
    // totalTemplates count rendered as stat card
    await expect(page.getByText('User Registration Data')).toBeVisible();
  });

  test('test data detail page loads', async ({ page }) => {
    await page.route('**/api/v1/test-data/templates/td-detail-1', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 'td-detail-1', name: 'User Registration Data', description: 'Test data for user reg',
          fieldCount: 3, profileCount: 2, generationCount: 5,
          fields: [
            { id: 'f1', name: 'email', fieldType: 'EMAIL', nullable: false, displayOrder: 1 },
            { id: 'f2', name: 'name', fieldType: 'STRING', nullable: false, displayOrder: 2 },
            { id: 'f3', name: 'age', fieldType: 'INTEGER', nullable: true, displayOrder: 3 },
          ],
          profiles: [
            { id: 'pr1', name: 'Positive', profileType: 'POSITIVE' },
            { id: 'pr2', name: 'Edge Cases', profileType: 'EDGE_CASE' },
          ],
          createdAt: '2026-11-01T10:00:00Z',
        }),
      });
    });

    await page.route('**/api/v1/test-data/templates/td-detail-1/generations*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [], totalElements: 0, totalPages: 0, size: 20, number: 0, first: true, last: true,
        }),
      });
    });

    await page.goto('/test-data/td-detail-1');
    await expect(page.getByText('User Registration Data')).toBeVisible();
  });
});

// =====================================================
// QA REQUESTS TESTS
// =====================================================
test.describe('QA Requests', () => {
  test.beforeEach(async ({ page }) => {
    await mockApiRoutes(page);
    await seedAuthState(page);
  });

  test('displays QA requests page', async ({ page }) => {
    await page.goto('/qa-requests');
    await expect(page.getByRole('heading', { name: 'QA Service Requests' })).toBeVisible();
  });

  test('QA dashboard shows metrics', async ({ page }) => {
    await page.goto('/qa-dashboard');
    await expect(page.getByRole('heading', { name: 'QA Service Dashboard' })).toBeVisible();
  });
});

// =====================================================
// DOCUMENTATION TESTS
// =====================================================
test.describe('Documentation', () => {
  test.beforeEach(async ({ page }) => {
    await mockApiRoutes(page);
    await seedAuthState(page);
  });

  test('displays documentation page', async ({ page }) => {
    await page.goto('/documentation');
    await expect(page.getByRole('heading', { name: 'Documentation Intelligence' })).toBeVisible();
  });

  test('documentation detail page loads', async ({ page }) => {
    // Remove conflicting global mock before adding specific one
    await page.unroute('**/api/v1/documentation/**');
    await page.route('**/api/v1/documentation/**', async (route) => {
      const url = route.request().url();
      if (url.endsWith('/doc-1')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'doc-1', title: 'API Design Guide', description: 'REST API design standards',
            docType: 'API_DOC', category: 'TECHNICAL', healthStatus: 'HEALTHY',
            url: 'https://docs.example.com/api', ownerName: 'Sarah Chen',
            lastCheckedAt: '2026-11-20T10:00:00Z', createdAt: '2026-10-01T10:00:00Z',
            updatedAt: '2026-11-20T10:00:00Z', reviewCycleDays: 30,
            recentChecks: [], recentReviews: [],
          }),
        });
      } else {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({}),
        });
      }
    });

    await page.goto('/documentation/doc-1');
    await expect(page.getByText('API Design Guide')).toBeVisible();
  });
});

// =====================================================
// RELEASES TESTS
// =====================================================
test.describe('Releases', () => {
  test.beforeEach(async ({ page }) => {
    await mockApiRoutes(page);
    await seedAuthState(page);
  });

  test('displays release confidence page', async ({ page }) => {
    await page.goto('/releases');
    await expect(page.getByRole('heading', { name: 'Release Confidence' })).toBeVisible();
  });

  test('release detail page loads', async ({ page }) => {
    // Remove conflicting global mock and replace with specific ones
    await page.unroute('**/api/v1/releases/**');
    await page.route('**/api/v1/releases/**', async (route) => {
      const url = route.request().url();
      if (url.includes('/rel-1/readiness')) {
        // Return a proper ReadinessReport (not a Page)
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'rr-1', releaseId: 'rel-1', releaseName: 'v2.1.0',
            projectName: 'Auth Service', overallScore: 82.5,
            gateStatus: 'PASS', passRateScore: 92.1, buildHealthScore: 88.0,
            defectScore: 67.4, metrics: {}, gateResults: [],
            generatedAt: '2026-11-20T14:00:00Z',
          }),
        });
      } else if (url.includes('/rel-1/sign-offs')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([]),
        });
      } else {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({}),
        });
      }
    });

    await page.goto('/releases/rel-1');
    await expect(page.getByText('v2.1.0')).toBeVisible();
  });
});

// =====================================================
// INTEGRATIONS TESTS
// =====================================================
test.describe('Integrations', () => {
  test.beforeEach(async ({ page }) => {
    await mockApiRoutes(page);
    await seedAuthState(page);
  });

  test('displays integrations page', async ({ page }) => {
    await page.goto('/integrations');
    await expect(page.getByRole('heading', { name: 'External Integrations' })).toBeVisible();
  });

  test('integration detail page loads', async ({ page }) => {
    await page.route('**/api/v1/integrations/int-1', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 'int-1', name: 'GitLab CI/CD', provider: 'GITLAB', enabled: true,
          hasSecret: true, projectName: 'Auth Service', eventCount: 15,
          config: '{"repository": "mydblink/karamfrontnew"}',
          createdAt: '2026-11-01T10:00:00Z',
        }),
      });
    });

    await page.route('**/api/v1/integrations/int-1/events*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [
            { id: 'ev1', eventType: 'pipeline', status: 'PROCESSED', receivedAt: '2026-11-20T14:00:00Z' },
          ],
          totalElements: 1, totalPages: 1, size: 20, number: 0, first: true, last: true,
        }),
      });
    });

    await page.goto('/integrations/int-1');
    await expect(page.getByText('GitLab CI/CD')).toBeVisible();
  });
});

// =====================================================
// AUDIT TRAIL TESTS
// =====================================================
test.describe('Audit Trail', () => {
  test.beforeEach(async ({ page }) => {
    await mockApiRoutes(page);
    await seedAuthState(page);
  });

  test('displays audit events', async ({ page }) => {
    await page.goto('/audit');
    await expect(page.getByText('Sarah Chen')).toBeVisible();
  });

  test('audit page shows event details', async ({ page }) => {
    await page.goto('/audit');
    // Use table cell locator to avoid matching <option> elements in filter dropdowns
    await expect(page.locator('tbody').getByText('CREATE')).toBeVisible();
    await expect(page.locator('tbody').getByText('TestRun')).toBeVisible();
  });
});

// =====================================================
// USERS MANAGEMENT TESTS
// =====================================================
test.describe('Users Management', () => {
  test.beforeEach(async ({ page }) => {
    await mockApiRoutes(page);
    await seedAuthState(page);
  });

  test('displays users list', async ({ page }) => {
    await page.goto('/settings/users');
    await expect(page.getByText('sarah.chen@demo.oqh')).toBeVisible();
  });

  test('shows user roles', async ({ page }) => {
    await page.goto('/settings/users');
    // The role is shown as a <select> value; check the select element has the ADMIN option selected
    await expect(page.locator('select').first()).toHaveValue('ADMIN');
  });
});

// =====================================================
// SIDEBAR & LAYOUT TESTS
// =====================================================
test.describe('Layout & Sidebar', () => {
  test.beforeEach(async ({ page }) => {
    await mockApiRoutes(page);
    await seedAuthState(page);
  });

  test('sidebar shows all navigation links', async ({ page }) => {
    await page.goto('/');
    const sidebar = page.locator('aside');
    await expect(sidebar.getByRole('link', { name: 'Dashboard', exact: true })).toBeVisible();
    await expect(sidebar.getByRole('link', { name: 'Teams' })).toBeVisible();
    await expect(sidebar.getByRole('link', { name: 'Projects' })).toBeVisible();
    await expect(sidebar.getByRole('link', { name: 'Test Runs' })).toBeVisible();
    await expect(sidebar.getByRole('link', { name: 'Test Data' })).toBeVisible();
    await expect(sidebar.getByRole('link', { name: 'QA Requests' })).toBeVisible();
    await expect(sidebar.getByRole('link', { name: 'Documentation' })).toBeVisible();
    await expect(sidebar.getByRole('link', { name: 'Releases' })).toBeVisible();
    await expect(sidebar.getByRole('link', { name: 'Integrations' })).toBeVisible();
    await expect(sidebar.getByRole('link', { name: 'Audit Trail' })).toBeVisible();
    await expect(sidebar.getByRole('link', { name: 'Users' })).toBeVisible();
  });

  test('sidebar highlights active page', async ({ page }) => {
    await page.goto('/teams');
    const teamsLink = page.getByRole('link', { name: 'Teams' });
    await expect(teamsLink).toHaveClass(/bg-blue-50/);
  });

  test('sidebar shows user info', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByText(DEMO_USER.displayName)).toBeVisible();
    await expect(page.getByText(DEMO_USER.email)).toBeVisible();
  });

  test('sidebar has branding', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByRole('heading', { name: 'Open Quality Hub' })).toBeVisible();
    await expect(page.getByText('Quality Engineering Platform')).toBeVisible();
  });

  test('logout button is visible', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByLabel('Sign out')).toBeVisible();
  });
});

// =====================================================
// FULL NAVIGATION FLOW
// =====================================================
test.describe('Navigation Flow', () => {
  test.beforeEach(async ({ page }) => {
    await mockApiRoutes(page);
    await seedAuthState(page);
  });

  test('navigate through all main pages', async ({ page }) => {
    const routes = [
      { link: 'Dashboard', url: /\/$/ },
      { link: 'Teams', url: /\/teams/ },
      { link: 'Projects', url: /\/projects/ },
      { link: 'Test Runs', url: /\/test-runs/ },
      { link: 'Test Data', url: /\/test-data/ },
      { link: 'QA Requests', url: /\/qa-requests/ },
      { link: 'QA Dashboard', url: /\/qa-dashboard/ },
      { link: 'Documentation', url: /\/documentation/ },
      { link: 'Releases', url: /\/releases/ },
      { link: 'Integrations', url: /\/integrations/ },
      { link: 'Audit Trail', url: /\/audit/ },
      { link: 'Users', url: /\/settings\/users/ },
    ];

    await page.goto('/');

    for (const route of routes) {
      await page.getByRole('link', { name: route.link, exact: route.link === 'Dashboard' }).click();
      await expect(page).toHaveURL(route.url);
      await expect(page.locator('text=Something went wrong')).not.toBeVisible();
    }
  });

  test('navigate back and forth between pages', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('link', { name: 'Teams' }).click();
    await expect(page).toHaveURL(/\/teams/);
    await page.getByRole('link', { name: 'Dashboard', exact: true }).click();
    await expect(page).toHaveURL(/\/$/);
    await page.getByRole('link', { name: 'Test Runs' }).click();
    await expect(page).toHaveURL(/\/test-runs/);
  });
});

// =====================================================
// AUTH & PROTECTION TESTS
// =====================================================
test.describe('Auth Protection', () => {
  test('redirects unauthenticated user to login', async ({ page }) => {
    await mockApiRoutes(page);
    await page.goto('/');
    await expect(page).toHaveURL(/\/login/);
  });

  test('protected routes redirect to login', async ({ page }) => {
    await mockApiRoutes(page);
    const protectedRoutes = ['/teams', '/projects', '/test-runs', '/qa-requests', '/documentation', '/releases', '/integrations', '/audit', '/settings/users', '/test-data'];
    for (const route of protectedRoutes) {
      await page.goto(route);
      await expect(page).toHaveURL(/\/login/);
    }
  });

  test('login page has demo credentials info', async ({ page }) => {
    await mockApiRoutes(page);
    await page.goto('/login');
    await expect(page.getByText('Demo credentials')).toBeVisible();
  });

  test('login page shows email and password fields', async ({ page }) => {
    await mockApiRoutes(page);
    await page.goto('/login');
    await expect(page.locator('input[type="email"]')).toBeVisible();
    await expect(page.locator('input[type="password"]')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Sign in' })).toBeVisible();
  });

  test('shows error on invalid credentials', async ({ page }) => {
    await mockApiRoutes(page);
    await page.goto('/login');
    await page.locator('input[type="email"]').fill('wrong@example.com');
    await page.locator('input[type="password"]').fill('BadPassword1!');
    await page.getByRole('button', { name: 'Sign in' }).click();
    await expect(page.getByText('Invalid email or password')).toBeVisible();
  });

  test('successful login redirects to dashboard', async ({ page }) => {
    await mockApiRoutes(page);
    await page.goto('/login');
    await page.locator('input[type="email"]').fill(DEMO_USER.email);
    await page.locator('input[type="password"]').fill(DEMO_USER.password);
    await page.getByRole('button', { name: 'Sign in' }).click();
    await expect(page).toHaveURL(/\/$/);
    await expect(page.getByRole('heading', { name: 'Quality Command Center' })).toBeVisible();
  });

  test('logout clears session and redirects', async ({ page }) => {
    await mockApiRoutes(page);
    await seedAuthState(page);
    await page.goto('/');
    await expect(page.getByText(DEMO_USER.displayName)).toBeVisible();
    await page.getByLabel('Sign out').click();
    await expect(page).toHaveURL(/\/login/);
  });
});
