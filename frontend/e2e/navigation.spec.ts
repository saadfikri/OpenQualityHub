import { test, expect } from '@playwright/test';
import { mockApiRoutes, seedAuthState } from './helpers/auth';

test.describe('Navigation', () => {
  test('should navigate between pages using sidebar', async ({ page }) => {
    await mockApiRoutes(page);
    await seedAuthState(page);

    // Start at dashboard
    await page.goto('/');
    await expect(page.getByRole('heading', { name: 'Quality Command Center' })).toBeVisible();

    // Navigate to Teams
    await page.getByRole('link', { name: 'Teams' }).click();
    await expect(page).toHaveURL(/\/teams/);
    await expect(page.getByRole('heading', { name: 'Teams', exact: true })).toBeVisible();

    // Navigate to Projects
    await page.getByRole('link', { name: 'Projects' }).click();
    await expect(page).toHaveURL(/\/projects/);
    await expect(page.getByRole('heading', { name: 'Projects', exact: true })).toBeVisible();

    // Navigate to Test Runs
    await page.getByRole('link', { name: 'Test Runs' }).click();
    await expect(page).toHaveURL(/\/test-runs/);
    await expect(page.getByRole('heading', { name: 'Test Runs', exact: true })).toBeVisible();

    // Navigate to QA Requests
    await page.getByRole('link', { name: 'QA Requests' }).click();
    await expect(page).toHaveURL(/\/qa-requests/);
    await expect(page.getByRole('heading', { name: 'QA Service Requests' })).toBeVisible();

    // Navigate to QA Dashboard
    await page.getByRole('link', { name: 'QA Dashboard' }).click();
    await expect(page).toHaveURL(/\/qa-dashboard/);
    await expect(page.getByRole('heading', { name: 'QA Service Dashboard' })).toBeVisible();

    // Navigate to Documentation
    await page.getByRole('link', { name: 'Documentation' }).click();
    await expect(page).toHaveURL(/\/documentation/);
    await expect(page.getByRole('heading', { name: 'Documentation Intelligence' })).toBeVisible();

    // Navigate to Releases
    await page.getByRole('link', { name: 'Releases' }).click();
    await expect(page).toHaveURL(/\/releases/);
    await expect(page.getByRole('heading', { name: 'Release Confidence' })).toBeVisible();

    // Navigate to Integrations
    await page.getByRole('link', { name: 'Integrations' }).click();
    await expect(page).toHaveURL(/\/integrations/);
    await expect(page.getByRole('heading', { name: 'External Integrations' })).toBeVisible();

    // Navigate back to Dashboard (exact: true to distinguish from "QA Dashboard")
    await page.getByRole('link', { name: 'Dashboard', exact: true }).click();
    await expect(page).toHaveURL(/\/$/);
    await expect(page.getByRole('heading', { name: 'Quality Command Center' })).toBeVisible();
  });

  test('should protect routes when not authenticated', async ({ page }) => {
    await mockApiRoutes(page);
    // Do NOT seed auth state — user is not logged in

    // Try to access dashboard directly
    await page.goto('/');
    await expect(page).toHaveURL(/\/login/);

    // Try to access teams directly
    await page.goto('/teams');
    await expect(page).toHaveURL(/\/login/);

    // Try to access projects directly
    await page.goto('/projects');
    await expect(page).toHaveURL(/\/login/);

    // Try to access test runs directly
    await page.goto('/test-runs');
    await expect(page).toHaveURL(/\/login/);
  });
});
