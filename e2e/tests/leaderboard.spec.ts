import { test, expect } from '@playwright/test';

test('积分榜与成员页展示公开积分', async ({ page }) => {
  await page.goto('/points');

  await expect(page.getByRole('heading', { name: '积分榜' })).toBeVisible();
  const memberLink = page.getByRole('link', { name: 'E2E积分成员' });
  await expect(memberLink).toBeVisible();

  await memberLink.click();
  await expect(page).toHaveURL(/\/members\/\d+$/);
  await expect(page.getByRole('heading', { name: 'E2E积分成员', level: 1 })).toBeVisible();
  await expect(page.locator('p.lead').filter({ hasText: '总分' })).toContainText('12');

  const ledgerRow = page.locator('table tbody tr').filter({ hasText: '比赛奖' });
  await expect(ledgerRow.first()).toContainText('12');
});
