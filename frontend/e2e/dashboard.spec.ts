import { test, expect } from '@playwright/test';
import { mockApiRoutes, seedAuthState } from './helpers/auth';

test.describe('Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await mockApiRoutes(page);
    await seedAuthState(page);
  });

  test('should display dashboard after login', async ({ page }) => {
    await page.goto('/');

    // Dashboard heading
    await expect(page.getByRole('heading', { name: 'Quality Command Center' })).toBeVisible();
    await expect(
      page.getByText('Real-time quality engineering metrics across all projects'),
    ).toBeVisible();
  });

  test('should show quality metrics cards', async ({ page }) => {
    await page.goto('/');

    const main = page.getByRole('main');

    // Wait for dashboard to load — the heading proves the API mock was consumed
    await expect(page.getByRole('heading', { name: 'Quality Command Center' })).toBeVisible();

    // Verify stat card labels inside the stats grid (first grid section in main)
    // Use the stats grid container to avoid ambiguity with table headers
    const statsGrid = main.locator('.grid').first();
    await expect(statsGrid.getByText('Teams')).toBeVisible();
    await expect(statsGrid.getByText('Projects')).toBeVisible();
    await expect(statsGrid.getByText('Test Runs')).toBeVisible();
    await expect(statsGrid.getByText('Avg Pass Rate')).toBeVisible();
    await expect(statsGrid.getByText('Open Defects')).toBeVisible();
    await expect(statsGrid.getByText('Flaky Tests')).toBeVisible();
    await expect(statsGrid.getByText('Quality Score')).toBeVisible();

    // Verify some actual values from the mock data (scoped to stats grid to avoid chart conflicts)
    await expect(statsGrid.getByText('240')).toBeVisible();   // totalTestRuns
    await expect(statsGrid.getByText('87.5%')).toBeVisible(); // averagePassRate
    await expect(statsGrid.getByText('12')).toBeVisible();    // openDefects

    // Verify chart sections are present
    await expect(main.getByText('Pass Rate Trend (30 days)')).toBeVisible();
    await expect(main.getByText('Test Volume (30 days)')).toBeVisible();
    await expect(main.getByText('Defects by Severity')).toBeVisible();
    await expect(main.getByText('Defects by Status')).toBeVisible();

    // Verify Project Health table
    await expect(main.getByText('Project Health')).toBeVisible();
    const projectHealthTable = main.locator('table');
    await expect(projectHealthTable.getByText('Auth Service')).toBeVisible();
    await expect(projectHealthTable.getByText('Platform Team')).toBeVisible();

    // Verify Recent Test Runs section
    await expect(main.getByText('Recent Test Runs')).toBeVisible();
    await expect(main.getByText('Auth API regression')).toBeVisible();
  });
});
