# 网络攻防协会官网 · cust_ctf_assoc

高校网络攻防协会的官网。把散在聊天记录里的成绩、积分、技术文章收进一个站里 —— 学长毕业了，写的东西还留着。

单机应用：Spring Boot + Thymeleaf 服务端渲染 + MySQL，一台 2 核 4G 的云服务器就能跑。

---

## 现在能做什么

**访客（不用登录）**

- 首页：协会简介、最新公告、荣誉、积分前三（只显示昵称）
- 竞赛档案：列表、赛季筛选、比赛详情（参赛成员、个人/团队奖项、现场照片）
- 积分榜：总榜 / 年榜 / 季度榜 / 月榜；点昵称看总成绩、来源汇总和能力图。逐笔明细仅本人、队长、积分管理员和教师可见
- 技术空间：公开文章、标签筛选、关键词检索、评论展示
- 公告、荣誉墙、加入我们（招新报名表，填完不用注册账号）
- 邀请码注册

**成员（登录后）**

- 看全部已发布文章（含站内文章），收藏、评论（先审后发）
- 我的积分：每笔分怎么来的、归属哪个月、可发起申诉
- 我的资料：实名资料与逐字段隐私开关；公开页展示允许公开的字段和六类竞赛能力雷达
- 战队空间：创建战队、查看成员；队长按用户名添加/移出成员、转让队长、编辑简介、解散战队
- 积分兑换：查看可兑换资源，提交/取消兑换申请，跟踪审核状态
- 我的文章：写草稿、编辑、提交审核、查看驳回原因
- 自助修改密码

**管理员**（超管 / 内容 / 积分 / 宣传四类角色，按职能分权）

| 入口 | 能做什么 | 可用角色 |
|---|---|---|
| 比赛与奖项 | 增删改查比赛；记个人奖、团队奖；CSV 批量导入；关联现场照片 | 超管 / 积分 |
| 积分审核 | 登记挖洞与护网、审批入账、驳回；发布积分规则版本 | 超管 / 积分 |
| 积分申诉 | 处理申诉（成立即冲抵） | 超管 / 积分 |
| 积分兑换 | 配置兑换资源、上下架、审核兑换申请（通过即扣余额与库存） | 超管 / 积分 |
| 战队管理 | 查看全部战队（含已解散）、强制解散 | 仅超管 |
| 文章审核 | 审文章、下架；评论审核；敏感词库管理 | 超管 / 内容 |
| 媒体 | 上传、审核（选择 PUBLIC/MEMBER/OWNER）、下架、删除 | 超管 / 内容 |
| 公告与招新 | 发公告、加荣誉、下架；处理招新报名状态 | 超管 / 宣传 |
| 账号与邀请 | 创建成员、重置密码、发邀请码、分角色、禁用 | 仅超管 |
| 审计日志 | 查敏感操作（只读） | 仅超管 |

---

## 几条硬规矩（写进代码，不是写在文档里）

1. **积分流水只增不改。** 撤销不是改状态，是另记一条负数冲抵，原记录永不变。想改分，只能再加一笔。
2. **每笔流水都有幂等键。** 审核按钮点两次，库里仍然只有一条 —— 靠数据库唯一索引兜底，不靠代码自觉。
3. **未审核的媒体不进公共目录。** 上传先落 private 区；审核通过时先复制到 public、数据库提交成功后再删私有副本；下载统一走鉴权接口，可见范围 PUBLIC / MEMBER / OWNER 三档。
4. **涉及漏洞点的文章强制站内可见。** 作者选「公开」会被服务端覆盖 —— 这个站的备案主体是个人，漏洞细节公开出去没人兜。
5. **归属月锁死。** 每月 3 日关账：1、2 日补录仍可归上月，3 日起补录记当月；已公布的榜不会半夜自己变。
6. **所有积分来源走同一套链路。** 比赛奖、挖洞、护网都写 `points_request` + `points_ledger`，榜单和明细可追溯到原始记录。
7. **敏感后台操作全部留痕。** 角色变更、封号、积分审核/冲抵、文章发布/下架、媒体审核、招新状态、规则发布、战队与兑换操作都写 `audit_log`。
8. **首次登录/重置后强制改密。** 新建账号、邀请码注册以外的重置都带一次性临时密码，登录后只能进改密页。
9. **积分兑换不动流水。** 可用余额 = 流水合计 − 已通过的兑换成本；审核通过时原子扣库存，重复点击不会二次扣减。

---

## 技术栈

| 组件 | 版本 / 说明 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.2 |
| 模板 | Thymeleaf（服务端渲染，不做前后端分离） |
| 安全 | Spring Security 7.1.2 + BCrypt + 会话 Cookie + SessionRegistry 会话撤销 + 登录失败限流 + CSP |
| 数据 | Spring Data JPA（`ddl-auto=validate`）+ MySQL 8 |
| 迁移 | Flyway（表结构只由 `db/migration` 管） |
| 校验 | Bean Validation |
| Markdown | commonmark-java，`escapeHtml(true)` + URL 协议白名单 |
| 测试 | JUnit 5 + MockMvc + Testcontainers MySQL + Playwright |
| 构建 | Maven 3.9+ |

> Maven 3.6.1 跑不了当前 Spring Boot 4 构建链，请使用 Maven 3.9+。

---

## 本地跑起来

### 1. 环境

- JDK 21
- Maven 3.9+
- MySQL 8（默认连 `127.0.0.1:3307`）

```bash
java -version     # 21
mvn -v            # 3.9.x
```

### 2. 建库

```sql
CREATE DATABASE assoc_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'assoc_app'@'127.0.0.1' IDENTIFIED BY '你的密码';
GRANT ALL PRIVILEGES ON assoc_dev.* TO 'assoc_app'@'127.0.0.1';
FLUSH PRIVILEGES;
```

表不用手建，首次启动 Flyway 自动迁移。

### 3. 配环境变量

| 变量 | 默认 | 说明 |
|---|---|---|
| `DB_URL` | jdbc:mysql://127.0.0.1:3307/assoc_dev?... | 一般不用改 |
| `DB_USER` | assoc_app | |
| `DB_PASSWORD` | 空 | **必须设置** |
| `ADMIN_USERNAME` | Yorkchain | 首个超管的登录名 |
| `ADMIN_PASSWORD` | 无 | **必须设置**，至少 12 位、含三类字符；缺失或太弱会拒绝启动 |
| `STORAGE_ROOT` | D:/spring_study/uploads | 上传文件根目录 |
| `POINTS_BACKFILL_START` | 2015-01-01 | 积分补录允许的最早发生日期 |
| `DEMO_DATA_ENABLED` | false | 开发环境设为 true 时加载演示荣誉数据 |

```bash
export DB_PASSWORD='你的密码'
export ADMIN_PASSWORD='一次性强密码'
```

超管只在库里没有这个用户名时创建；首次登录会强制改密。

### 4. 跑

```bash
mvn spring-boot:run
# 打开 http://localhost:8080
```

### 5. 跑测试

```bash
mvn test                 # 62 个单元/页面测试（纯函数、策略、Markdown、敏感词、战队/兑换、权限辅助、页面渲染）
mvn verify               # 单元测试 + 30 个集成测试（Testcontainers 起 MySQL 8.0）
```

**集成测试（`*IT`）** 用 Testcontainers 启动一次性 MySQL，覆盖 V1-V10 空库迁移、四角色 URL 权限矩阵、积分封顶/申诉冲抵/幂等键、媒体两阶段审核与三档可见性、登录/强制改密/邀请码注册/CSP/招新限流、公开隐私和多会话撤销。

- CI 的 `integration-tests` 任务直接跑 `mvn verify -DskipUnitTests=true`，PR 必跑。
- 本机没有 Docker 时，可指向一个**专供测试的空库**（名字建议 `assoc_it`）跑同一套用例：

```bash
mvn verify -DskipUnitTests=true \
  "-Dit.datasource.url=jdbc:mysql://127.0.0.1:3309/assoc_it?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false" \
  -Dit.datasource.username=assoc_it -Dit.datasource.password=assoc_it_pw
```

没有 Docker 也没配外部库时，集成测试整类跳过，`mvn test` 不受影响。外部账号需要对测试库有建表权限；`FlywayMigrationIT` 还会创建并删除一个临时 probe 库，因此需要 `CREATE/DROP DATABASE` 权限。

**浏览器 E2E（Playwright）** 覆盖首页、登录/首次改密、成员投稿→管理员发布、积分榜与成员页：

```bash
cd e2e
npm ci
npx playwright install chromium      # 本机也可用 E2E_CHANNEL=msedge 免下载
npm test
```

E2E 默认由 Playwright 用 `target/cust-ctf-assoc-0.0.1-SNAPSHOT.jar` 拉起应用，需要先 `mvn -Dmaven.test.skip=true package`，并设置 `DB_URL / DB_USER / DB_PASSWORD / ADMIN_PASSWORD`。CI 的 `e2e` 任务自动完成这些步骤。

---

## 目录结构

```
src/main/java/cn/edu/cust/assoc
├─ controller    接请求、调 service、选视图
├─ service       业务规则 + 事务边界
├─ repository    Spring Data JPA 接口
├─ domain        JPA 实体、枚举、纯规则（积分算法、归属月、团队分账）
├─ dto           页面/接口传输对象（模板不直接接触实体）
├─ common        BizException、全局异常、URL/口令策略
├─ config        Security、超管初始化、演示数据
└─ security      从数据库加载用户、当前用户、强制改密过滤器

src/main/resources
├─ db/migration  V1~V10 迁移脚本
├─ templates     Thymeleaf 页面（公开页 + admin/ + me/）
├─ static        样式与确认弹窗脚本
└─ application.yml

src/test/java/cn/edu/cust/assoc
├─ support       IntegrationTestBase / TestDatabase / TestContainerConfig
├─ migration     FlywayMigrationIT（空库执行 V1-V10）
├─ security      四角色 URL 矩阵、认证流程、CSP 与招新限流
└─ service       积分封顶/冲抵/幂等、媒体两阶段审核与可见性

e2e/             Playwright 配置、浏览器用例与榜单数据准备

scripts/         deploy.sh / backup-db.sh
docs/            开发规范、本地开发、运维手册、部署清单
```

---

## 数据库迁移

| 版本 | 内容 |
|---|---|
| V1 | 账号域 8 张表 + 7 个角色种子 |
| V2 | 比赛档案 |
| V3 | 个人奖 / 团队奖 / 成员份额 |
| V4 | 媒体（private/public 两区、审核状态） |
| V5 | 积分规则 / 漏洞点 / 申请单 / 流水 / 申诉 |
| V6 | 技术文章 + 敏感词 |
| V7 | 公告 / 荣誉 / 招新报名 |
| V8 | 强制改密、审计日志、媒体关联、标签/评论/收藏、内容软删除、团队奖唯一约束、积分外键、规则种子、移除演示数据 |
| V9 | 战队空间增量（创建/入队时间、状态约束）、兑换资源与兑换申请 |
| V10 | 修复战队成员唯一约束、待审申诉/兑换并发约束、评论下架与旧账号强制改密 |

**已经执行过的迁移不要改**，要改结构就新建 V11。

---

## 积分怎么算

计分优先读取 `points_rule` 中「已发布、生效日不晚于发生日」的最高版本；没有对应规则时用代码里的默认表。历史流水不会因为新规则重算。

**竞赛**：省级 10/7/5、国赛 15/13/11、国际赛 20/18/16（一等/二等/三等），校级 0 分但留档。

**挖洞**：

```
基础分 = 10 - 2 × 等级序号（严重 1 / 高危 2 / 中危 3 / 低危 4）→ 8 / 6 / 4 / 2
得分 = max(1, 基础分 × 场景系数)，单洞 1，通杀 / 公益 各 1/2（不叠乘）
同一个漏洞点（平台 + 平台编号）累计封顶 10 分，超出部分不记
```

**护网**（唯一口径）：

```
1000 分以下        0 分
1000 ~ 4999        3 分
5000 ~ 9999        5 分起，每多 1000 加 1 分（5000=5 … 9999=9）
10000 起           10 分起，每多 5000 加 2 分（15000=12 …），封顶 20
```

**团队奖**：分给队员，合计必须**等于**这个奖的总分；同一比赛同一名次只能有一条团队奖；同一成员不能填两次。

---

## 账号与安全

- 账号来源：超管创建成员、邀请码自助注册；没有无邀请码的公开注册入口。
- 改密：本人可自助修改；忘记密码由超管重置（一次性临时密码，只显示一次）。
- 会话撤销：本人改密、禁用、改角色、重置密码后目标用户全部会话立即过期。
- 登录保护：同一 IP + 登录名连续失败 5 次后暂停 15 分钟；单机使用进程内计数。
- 四类管理员按 URL 和菜单双重分权，越权访问返回 403。
- Markdown 链接允许 http、https、mailto 和站内相对地址；外链图片只允许 https，模板禁止内联脚本，CSP 由应用与 Nginx 双重下发。
- 招新表单按真实 IP（Nginx 覆盖 X-Forwarded-For）和学号各限 5 次/小时；进程内计数，多实例部署需换 Redis。

---

## 部署

见 [docs/部署清单.md](docs/部署清单.md)（服务器初始化 → 装环境 → 部署 → Nginx/HTTPS → 备份 → 上线验收）。

日常运维见 [docs/运维手册.md](docs/运维手册.md)。

发布流程：本地 `mvn clean package` → 生成 `.sha256` → 上传 → `./deploy.sh`（按 app.jar 实际哈希校验 → 备份库 → 切符号链接 → 探活 → 失败自动回滚）。

**生产机上不跑 Maven**：2 核 4G 边编译边跑服务，构建自己就会因为内存不足失败。

---

## 已知限制

- 战队成员由队长按登录用户名直接添加，没有邀请确认流程；战队解散后名字仍被占用，不能复用。
- 积分兑换在待审核阶段不预留积分、不锁库存，以审核通过时的余额与库存为准，先到先得。
- 招新限流是**进程内计数**，重启清零；多实例部署需要换 Redis。
- 登录失败限流同样是进程内计数，多实例部署需要换 Redis。
- 敏感词是「规范化 + 包含匹配」，能挡零宽字符、全角、换行和制表符，挡不住拼音谐音，所以评论和文章必须配人工审核。
- 文章禁止复制只是前端提示，截图和看源码都能绕过。
- 社区、内部工具、镜像仓库和资料下载仍不在 1.0 范围内。

---

## 合规

个人名义维护，不代表任何学校或组织。站上不出现校名校徽；文章涉及真实漏洞点必须发在站内；页脚常驻「仅供学习交流，禁止用于任何未授权测试」。

## 许可证

见 [LICENSE](LICENSE)。
