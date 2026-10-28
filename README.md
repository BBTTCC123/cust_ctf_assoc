# 网络攻防协会官网 · cust_ctf_assoc

高校网络攻防协会的官网。把散在聊天记录里的成绩、积分、技术文章收进一个站里 —— 学长毕业了，写的东西还留着。

单机应用：Spring Boot + Thymeleaf 服务端渲染 + MySQL，一台 2 核 4G 的云服务器就能跑。

---

## 现在能做什么

**访客（不用登录）**

- 首页：协会简介、最新公告、荣誉、积分前三
- 竞赛档案：历次比赛列表
- 积分榜：总榜 / 年榜 / 季度榜 / 月榜
- 技术空间：只显示标注「公开」的文章
- 公告、荣誉墙、加入我们（招新报名表，填完不用注册账号）

**成员（登录后）**

- 看全部已发布文章（含站内文章）
- 我的积分：每笔分怎么来的、归属哪个月、可发起申诉

**管理员**（超管 / 内容 / 积分 / 宣传四类角色，按职能分权）

| 入口 | 能做什么 |
|---|---|
| 比赛与奖项 | 增删改查比赛；记个人奖、团队奖；CSV 批量导入历史数据 |
| 积分审核 | 登记挖洞、审批入账、驳回；看申诉（成立即冲抵） |
| 文章审核 | 写文章、发布、驳回 |
| 媒体 | 上传、审核（审核通过才从前台可见） |
| 公告与招新 | 发公告、加荣誉、看报名名单 |
| 账号 | 分配角色、禁用账号（**仅超管**） |

---

## 几条硬规矩（写进代码，不是写在文档里）

1. **积分流水只增不改。** 撤销不是改状态，是另记一条负数冲抵，原记录永不变。想改分，只能再加一笔。
2. **每笔流水都有幂等键。** 审核按钮点两次，库里仍然只有一条 —— 靠数据库唯一索引兜底，不靠代码自觉。
3. **未审核的媒体不进公共目录。** 上传先落 private 区，审核通过才搬进 public，且下载统一走鉴权接口。
4. **涉及漏洞点的文章强制站内可见。** 作者选「公开」会被服务端覆盖 —— 这个站的备案主体是个人，漏洞细节公开出去没人兜。
5. **归属月锁死。** 每月 3 日关账，关账后补录的记当月，已公布的榜不会半夜自己变。

---

## 技术栈

| 组件 | 版本 / 说明 |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.16 |
| 模板 | Thymeleaf（服务端渲染，不做前后端分离） |
| 安全 | Spring Security 6 + BCrypt + 会话 Cookie |
| 数据 | Spring Data JPA（`ddl-auto=validate`）+ MySQL 8 |
| 迁移 | Flyway（表结构只由 `db/migration` 管） |
| 校验 | Bean Validation |
| Markdown | commonmark-java，渲染时 `escapeHtml(true)` |
| 构建 | Maven 3.9+ |

> Maven 3.6.1 跑不了 Spring Boot 3（插件要求 3.6.3+），这是升级过的原因。

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
| `ADMIN_PASSWORD` | ChangeMe#2026 | **首次启动前必须改** |
| `STORAGE_ROOT` | D:/spring_study/uploads | 上传文件根目录 |

```bash
export DB_PASSWORD='你的密码'
export ADMIN_PASSWORD='你的超管密码'
```

超管只在库里没有这个用户名时创建；账号建好后再改环境变量不会重置密码。

### 4. 跑

```bash
mvn spring-boot:run
# 打开 http://localhost:8080
```

### 5. 跑测试

```bash
mvn test     # 24 个单元测试
```

---

## 目录结构

```
src/main/java/cn/edu/cust/assoc
├─ controller    接请求、调 service、选视图
├─ service       业务规则 + 事务边界
├─ repository    Spring Data JPA 接口
├─ domain        JPA 实体、枚举、纯规则（积分算法、归属月、团队分账）
├─ dto           表单对象
├─ config        Security、超管初始化
└─ security      从数据库加载用户

src/main/resources
├─ db/migration  V1~V7 迁移脚本
├─ templates     Thymeleaf 页面（公开页 + admin/）
├─ static/css    样式
└─ application.yml

scripts/         deploy.sh / backup-db.sh
docs/            开发规范、本地开发、运维手册、部署清单
```

---

## 数据库迁移

| 版本 | 内容 |
|---|---|
| V1 | 账号域 8 张表（user / role / user_role / member_profile / team / team_member / registration_invite / password_reset_token）+ 7 个角色种子 |
| V2 | 比赛档案 |
| V3 | 个人奖 / 团队奖 / 成员份额 |
| V4 | 媒体（private/public 两区、审核状态） |
| V5 | 积分规则 / 漏洞点 / 申请单 / 流水 / 申诉 |
| V6 | 技术文章 + 敏感词 |
| V7 | 公告 / 荣誉 / 招新报名 |

**已经执行过的迁移不要改**，要改结构就新建 V8、V9。

---

## 积分怎么算

**竞赛**：省级 10/7/5、国赛 15/13/11、国际赛 20/18/16（一等/二等/三等），校级 0 分但留档。

**挖洞**：

```
基础分 = 10 - 2 × 等级序号（严重 1 / 高危 2 / 中危 3 / 低危 4）→ 8 / 6 / 4 / 2
得分 = max(1, 基础分 × 场景系数)，单洞 1，通杀 / 公益 各 1/2（不叠乘）
同一个漏洞点累计封顶 10 分，超出部分不记
```

**护网**：1000 分以下 0 分；1000~5000 给 3 分；5000~10000 给 4~8 分；10000 给 10 分；之后每 5000 加 2 分，封顶 20。

**团队奖**：分给队员，合计必须**等于**这个奖的总分，多一分少一分都提交不了。

---

## 部署

见 [docs/部署清单.md](docs/部署清单.md)（服务器初始化 → 装环境 → 部署 → Nginx/HTTPS → 备份 → 上线验收）。

日常运维见 [docs/运维手册.md](docs/运维手册.md)。

发布流程：本地 `mvn clean package` → 生成 `.sha256` → 上传 → `./deploy.sh`（校验 → 备份库 → 切符号链接 → 探活 → 失败自动回滚）。

**生产机上不跑 Maven**：2 核 4G 边编译边跑服务，构建自己就会因为内存不足失败。

---

## 已知限制

- 评论、收藏、能力雷达图、积分兑换**没做**（1.0 砍掉了，不是忘了）
- 招新表单的限流是**进程内计数**，重启清零；多实例部署需要换 Redis
- 敏感词是「规范化 + 包含匹配」，能挡零宽字符和全角，挡不住拼音谐音，所以必须配人工审核
- 文章禁止复制只是前端提示，截图和看源码都能绕过

---

## 合规

个人名义维护，不代表任何学校或组织。站上不出现校名校徽；文章涉及真实漏洞点必须发在站内；页脚常驻「仅供学习交流，禁止用于任何未授权测试」。

## 许可证

见 [LICENSE](LICENSE)。
