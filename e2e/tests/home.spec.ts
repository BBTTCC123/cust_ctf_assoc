import { test, expect } from '@playwright/test';

test('首页与公开导航可访问', async ({ page }) => {
  await page.goto('/');

  await expect(page).toHaveTitle(/网络攻防协会/);
  await expect(page.getByRole('heading', { name: /以攻促防/ })).toBeVisible();

  const nav = page.locator('header nav');
  await expect(nav.getByRole('link', { name: '竞赛档案', exact: true })).toBeVisible();
  await expect(nav.getByRole('link', { name: '积分榜', exact: true })).toBeVisible();
  await expect(nav.getByRole('link', { name: '登录', exact: true })).toBeVisible();

  await nav.getByRole('link', { name: '积分榜', exact: true }).click();
  await expect(page).toHaveURL(/\/points$/);
  await expect(page.getByRole('heading', { name: '积分榜' })).toBeVisible();
});
