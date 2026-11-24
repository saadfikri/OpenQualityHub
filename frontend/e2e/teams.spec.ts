import { test, expect } from '@playwright/test';
import { mockApiRoutes, seedAuthState } from './helpers/auth';

test.describe('Teams management', () => {
  test.beforeEach(async ({ page }) => {
    await mockApiRoutes(page);
    await seedAuthState(page);
  });

  test('should display teams list', async ({ page }) => {
    await page.goto('/teams');

    // Page heading
    await expect(page.getByRole('heading', { name: 'Teams' })).toBeVisible();
    await expect(page.getByText('Manage your quality engineering teams')).toBeVisible();

    // Verify mock teams are rendered
    await expect(page.getByText('Platform Team')).toBeVisible();
    await expect(page.getByText('Core platform quality engineering')).toBeVisible();
    await expect(page.getByText('Mobile Team')).toBeVisible();
    await expect(page.getByText('Mobile application testing')).toBeVisible();

    // Verify project counts
    await expect(page.getByText('3 projects')).toBeVisible();
    await expect(page.getByText('2 projects')).toBeVisible();
  });

  test('should open create team modal', async ({ page }) => {
    await page.goto('/teams');

    // Wait for page to load
    await expect(page.getByRole('heading', { name: 'Teams' })).toBeVisible();

    // Click the "New Team" button
    await page.getByRole('button', { name: 'New Team' }).click();

    // Verify modal appears with correct title and form fields
    await expect(page.getByRole('heading', { name: 'New Team' })).toBeVisible();
    await expect(page.getByLabel('Team Name')).toBeVisible();
    await expect(page.getByLabel('Description')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Create' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Cancel' })).toBeVisible();
  });
});
