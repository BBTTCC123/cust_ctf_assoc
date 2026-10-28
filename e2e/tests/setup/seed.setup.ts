import { test, expect } from '@playwright/test';
import mysql from 'mysql2/promise';

// E2E 榜单/成员页需要的固定样例数据。密码哈希对应 E2ePoints!2026，
// 这个账号只用来展示，不参与登录。所有语句都是幂等的，本地重复跑不会插重。
const POINTS_USER = 'e2e_points';
const POINTS_NICKNAME = 'E2E积分成员';
const POINTS_HASH = '$2a$10$PNaZ7v5sp3FOW63l/k3FOOkiNCsQ6JanrVnREaMQg7UQyzggoDXDG';
const PROOF_URL = 'https://example.com/e2e-points';
const IDEMPOTENCY_KEY = 'E2E:Ledger:1';

function databaseConfig() {
  const jdbcUrl = process.env.E2E_DB_URL || process.env.DB_URL || '';
  const match = jdbcUrl.match(/^jdbc:mysql:\/\/([^:/]+):(\d+)\/([^?]+)/);
  return {
    host: process.env.E2E_DB_HOST || match?.[1] || '127.0.0.1',
    port: Number(process.env.E2E_DB_PORT || match?.[2] || 3306),
    user: process.env.E2E_DB_USER || process.env.DB_USER || 'root',
    password: process.env.E2E_DB_PASSWORD ?? process.env.DB_PASSWORD ?? '',
    database: process.env.E2E_DB_NAME || match?.[3] || 'assoc_dev',
  };
}

test('准备 E2E 榜单数据', async () => {
  const connection = await mysql.createConnection(databaseConfig());
  try {
    await connection.execute(
      `INSERT INTO \`user\` (\`username\`, \`password_hash\`, \`nickname\`, \`status\`, \`must_change_password\`)
       SELECT ?, ?, ?, 'ACTIVE', 0
       WHERE NOT EXISTS (SELECT 1 FROM \`user\` WHERE \`username\` = ?)`,
      [POINTS_USER, POINTS_HASH, POINTS_NICKNAME, POINTS_USER]);

    await connection.execute(
      `INSERT INTO user_role (user_id, role_id)
       SELECT u.id, r.id FROM \`user\` u JOIN role r ON r.code = 'USER'
       WHERE u.username = ?
         AND NOT EXISTS (SELECT 1 FROM user_role ur WHERE ur.user_id = u.id AND ur.role_id = r.id)`,
      [POINTS_USER]);

    await connection.execute(
      `INSERT INTO points_request
         (user_id, source_type, occurred_on, points, state, proof_url, submitted_by, reviewed_by, reviewed_at)
       SELECT u.id, 'COMPETITION', CURDATE(), 12, 'APPROVED', ?, u.id, u.id, NOW()
       FROM \`user\` u
       WHERE u.username = ?
         AND NOT EXISTS (SELECT 1 FROM points_request pr WHERE pr.proof_url = ?)`,
      [PROOF_URL, POINTS_USER, PROOF_URL]);

    await connection.execute(
      `INSERT INTO points_ledger
         (user_id, request_id, entry_type, points, period_month, occurred_on, idempotency_key)
       SELECT pr.user_id, pr.id, 'EARN', 12, DATE_FORMAT(CURDATE(), '%Y-%m'), CURDATE(), ?
       FROM points_request pr
       WHERE pr.proof_url = ?
         AND NOT EXISTS (SELECT 1 FROM points_ledger WHERE idempotency_key = ?)`,
      [IDEMPOTENCY_KEY, PROOF_URL, IDEMPOTENCY_KEY]);

    const [rows] = await connection.execute(
      'SELECT COUNT(*) AS total FROM points_ledger WHERE idempotency_key = ?', [IDEMPOTENCY_KEY]);
    expect(Number(rows[0].total)).toBe(1);
  } finally {
    await connection.end();
  }
});
