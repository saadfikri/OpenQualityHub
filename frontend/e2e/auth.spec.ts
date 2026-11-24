import { test, expect } from '@playwright/test';
import { mockApiRoutes, loginViaUI, DEMO_USER } from './helpers/auth';

test.describe('Authentication flow', () => {
  test.beforeEach(async ({ page }) => {
    await mockApiRoutes(page);
  });

  test('should display login page', async ({ page }) => {
    await page.goto('/');

    // Unauthenticated users get redirected to /login
    await expect(page).toHaveURL(/\/login/);

    // Verify key form elements are visible
    await expect(page.getByRole('heading', { name: 'Open Quality Hub' })).toBeVisible();
    await expect(page.getByText('Quality Engineering Platform')).toBeVisible();
    await expect(page.locator('input[type="email"]')).toBeVisible();
    await expect(page.locator('input[type="password"]')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Sign in' })).toBeVisible();
    await expect(page.getByText('Demo credentials')).toBeVisible();
  });

  test('should show error on invalid credentials', async ({ page }) => {
    await page.goto('/login');

    await page.locator('input[type="email"]').fill('wrong@example.com');
    await page.locator('input[type="password"]').fill('BadPassword1!');
    await page.getByRole('button', { name: 'Sign in' }).click();

    // The mock returns 401 for wrong credentials — expect error message
    await expect(page.getByText('Invalid email or password')).toBeVisible();
    await expect(page).toHaveURL(/\/login/);
  });

  test('should login successfully with valid credentials', async ({ page }) => {
    await loginViaUI(page);

    // Should redirect to dashboard (root route)
    await expect(page).toHaveURL(/\/$/);

    // Dashboard heading should be visible
    await expect(page.getByRole('heading', { name: 'Quality Command Center' })).toBeVisible();
  });

  test('should logout and redirect to login', async ({ page }) => {
    // Log in first
    await loginViaUI(page);
    await expect(page).toHaveURL(/\/$/);

    // User info should be visible in sidebar
    await expect(page.getByText(DEMO_USER.displayName)).toBeVisible();

    // Click the sign-out button (has aria-label="Sign out")
    await page.getByLabel('Sign out').click();

    // Should redirect back to login
    await expect(page).toHaveURL(/\/login/);
    await expect(page.getByRole('heading', { name: 'Open Quality Hub' })).toBeVisible();
  });
});
