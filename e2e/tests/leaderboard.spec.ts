import { test, expect } from '@playwright/test';

test('积分榜与成员页展示公开积分摘要', async ({ page }) => {
  await page.goto('/points');

  await expect(page.getByRole('heading', { name: '积分榜' })).toBeVisible();
  const memberLink = page.getByRole('link', { name: 'E2E积分成员' });
  await expect(memberLink).toBeVisible();

  await memberLink.click();
  await expect(page).toHaveURL(/\/members\/\d+$/);
  await expect(page.getByRole('heading', { name: 'E2E积分成员', level: 1 })).toBeVisible();
  await expect(page.locator('p.lead').filter({ hasText: '总分' })).toContainText('12');
  await expect(page.locator('.source-list')).toContainText('比赛奖');
  await expect(page.locator('.source-list')).toContainText('12');

  // 匿名访客只得到成绩摘要，不应看到逐笔明细或学院专业年级。
  await expect(page.getByRole('heading', { name: '明细' })).toHaveCount(0);
  await expect(page.getByText('学院：')).toHaveCount(0);
});
