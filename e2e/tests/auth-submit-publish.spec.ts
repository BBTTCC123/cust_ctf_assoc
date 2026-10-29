import { test, expect, Page } from '@playwright/test';

const BASE_URL = process.env.E2E_BASE_URL || 'http://127.0.0.1:8080';
const ADMIN_USER = process.env.E2E_ADMIN_USERNAME || process.env.ADMIN_USERNAME || 'Yorkchain';
const ADMIN_BOOTSTRAP = process.env.E2E_ADMIN_PASSWORD || process.env.ADMIN_PASSWORD || '';
const ADMIN_NEW = process.env.E2E_ADMIN_NEW_PASSWORD || 'E2eAdmin!2026';

async function attemptLogin(page: Page, username: string, password: string): Promise<boolean> {
  await page.goto('/login');
  await page.locator('input[name="username"]').fill(username);
  await page.locator('input[name="password"]').fill(password);
  await page.getByRole('button', { name: '登录' }).click();
  await page.waitForLoadState();
  return !page.url().includes('/login?error');
}

/**
 * 超管可能处于三种状态：刚初始化（必须改密）、已经改过密、密码被改成了别的。
 * E2E 只负责前两种：先试 E2E_ADMIN_NEW_PASSWORD，失败再退回环境变量里的初始密码并完成强制改密。
 */
async function ensureAdmin(page: Page): Promise<void> {
  let loggedIn = await attemptLogin(page, ADMIN_USER, ADMIN_NEW);
  if (!loggedIn) {
    loggedIn = await attemptLogin(page, ADMIN_USER, ADMIN_BOOTSTRAP);
    expect(loggedIn, 'ADMIN_PASSWORD 无效，无法准备 E2E 超管').toBeTruthy();
  }

  if (page.url().includes('/password/change')) {
    await expect(page.getByRole('heading', { name: '修改密码' })).toBeVisible();
    await page.locator('input[name="currentPassword"]').fill(ADMIN_BOOTSTRAP);
    await page.locator('input[name="newPassword"]').fill(ADMIN_NEW);
    await page.locator('input[name="confirmPassword"]').fill(ADMIN_NEW);
    await page.getByRole('button', { name: '保存并重新登录' }).click();
    await expect(page).toHaveURL(/\/login/);
    expect(await attemptLogin(page, ADMIN_USER, ADMIN_NEW)).toBeTruthy();
  }
  await expect(page).toHaveURL(/\/$/);
}

test('错误密码在登录页给出明确提示', async ({ page }) => {
  await page.goto('/login');
  await page.locator('input[name="username"]').fill('not-a-real-user');
  await page.locator('input[name="password"]').fill('WrongPass!2026');
  await page.getByRole('button', { name: '登录' }).click();

  await expect(page.getByText('账号或密码不对')).toBeVisible();
});

test('成员首次登录改密并投稿，管理员发布后游客可见', async ({ browser }) => {
  const suffix = Date.now().toString(36) + Math.floor(Math.random() * 1000);
  const memberUsername = `e2e_m_${suffix}`.slice(0, 32);
  const initialPassword = 'MemberInit!2026';
  const memberPassword = 'MemberNew!2026';
  const articleTitle = `E2E 投稿 ${suffix}`;

  // 1. 超管（首次登录会被强制改密）
  const adminContext = await browser.newContext({ baseURL: BASE_URL });
  const adminPage = await adminContext.newPage();
  await ensureAdmin(adminPage);

  // 2. 超管创建成员：新账号首次登录必须改密
  await adminPage.goto('/admin/users');
  await adminPage.locator('input[name="username"]').fill(memberUsername);
  await adminPage.locator('input[name="nickname"]').fill('E2E 投稿成员');
  await adminPage.locator('input[name="password"]').fill(initialPassword);
  await adminPage.getByRole('button', { name: /创建/ }).click();
  await expect(adminPage.getByText(`成员 ${memberUsername} 已创建`)).toBeVisible();

  // 3. 成员登录 → 强制改密 → 用新密码重新登录
  const memberContext = await browser.newContext({ baseURL: BASE_URL });
  const memberPage = await memberContext.newPage();
  expect(await attemptLogin(memberPage, memberUsername, initialPassword)).toBeTruthy();
  await expect(memberPage).toHaveURL(/\/password\/change/);

  await memberPage.locator('input[name="currentPassword"]').fill(initialPassword);
  await memberPage.locator('input[name="newPassword"]').fill(memberPassword);
  await memberPage.locator('input[name="confirmPassword"]').fill(memberPassword);
  await memberPage.getByRole('button', { name: '保存并重新登录' }).click();
  await expect(memberPage).toHaveURL(/\/login/);
  await expect(memberPage.getByText('密码已修改，请用新密码重新登录')).toBeVisible();

  expect(await attemptLogin(memberPage, memberUsername, memberPassword)).toBeTruthy();
  await expect(memberPage).toHaveURL(/\/$/);

  // 4. 成员投稿（公开可见），进入待审
  await memberPage.goto('/me/articles/new');
  await memberPage.locator('input[name="title"]').fill(articleTitle);
  await memberPage.locator('textarea[name="contentMd"]').fill('这是 E2E 自动投稿的正文。');
  await memberPage.locator('select[name="visibility"]').selectOption('PUBLIC');
  await memberPage.getByRole('button', { name: '保存并提交审核' }).click();

  await expect(memberPage).toHaveURL(/\/me\/articles$/);
  await expect(memberPage.getByText('已提交审核')).toBeVisible();
  const myArticleRow = memberPage.locator('table tbody tr').filter({ hasText: articleTitle });
  await expect(myArticleRow).toHaveCount(1);
  await expect(myArticleRow).toContainText('待审核');

  // 5. 管理员在后台发布
  await adminPage.goto('/admin/articles');
  const pendingRow = adminPage.locator('table tbody tr').filter({ hasText: articleTitle });
  await expect(pendingRow).toHaveCount(1);
  await pendingRow.getByRole('button', { name: '发布' }).click();
  await expect(adminPage.locator('p.ok').filter({ hasText: '已发布' })).toBeVisible();
  // 发布后该行从「待审」表移到「已发布」表，操作按钮变成下架
  const publishedRow = adminPage.locator('table tbody tr').filter({ hasText: articleTitle });
  await expect(publishedRow).toHaveCount(1);
  await expect(publishedRow.getByRole('button', { name: '下架' })).toBeVisible();

  // 6. 游客在技术空间能看到并打开这篇文章
  const guestContext = await browser.newContext({ baseURL: BASE_URL });
  const guestPage = await guestContext.newPage();
  await guestPage.goto('/articles');
  const articleLink = guestPage.getByRole('link', { name: articleTitle });
  await expect(articleLink).toBeVisible();
  await articleLink.click();
  await expect(guestPage.getByRole('heading', { name: articleTitle, level: 1 })).toBeVisible();
  await expect(guestPage.getByText('这是 E2E 自动投稿的正文。')).toBeVisible();

  await guestContext.close();
  await memberContext.close();
  await adminContext.close();
});
