// Playwright webServer 入口：用生产 jar 起应用，环境变量原样透传。
// 数据库、ADMIN_PASSWORD、STORAGE_ROOT 都由调用方（CI 或本地 shell）提供。
import { spawn } from 'node:child_process';
import path from 'node:path';

const projectRoot = path.resolve(import.meta.dirname, '..');
const jar = process.env.E2E_JAR
  || path.join(projectRoot, 'target', 'cust-ctf-assoc-0.0.1-SNAPSHOT.jar');
const java = process.env.JAVA_BIN || 'java';

const child = spawn(java, ['-jar', jar], {
  cwd: projectRoot,
  stdio: 'inherit',
  env: process.env,
});

for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => {
    if (!child.killed) child.kill(signal);
  });
}

child.on('exit', (code) => process.exit(code ?? 0));
