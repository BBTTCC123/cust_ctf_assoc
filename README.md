# 网络攻防协会官网

协会官网的单体应用：竞赛档案、奖项、积分榜、技术文章、内容审核。

目前完成到「竞赛档案 + 奖项登记」，积分、文章、审核还没写。

## 技术栈

| 组件 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.16 |
| Web | Spring MVC + Thymeleaf（服务端渲染） |
| 安全 | Spring Security 6 + BCrypt |
| 数据 | Spring Data JPA + MySQL 8 |
| 迁移 | Flyway（表结构只由迁移脚本改，JPA 只做 validate） |
| 校验 | Bean Validation |
| 构建 | Maven 3.9+ |

> Maven 3.6.1 跑不了 Spring Boot 3（插件要求 3.6.3+）。本机用的是 3.9.16，见下面「本地运行」。

## 本地运行

### 1. 准备

- JDK 21：`C:\Program Files\Java\jdk-21`
- Maven：`C:\Users\16254\Documents\deepseek-harness\default-workspace\learning-workspace\tools\maven`
- MySQL 8，监听 127.0.0.1:3307
- 端口 8080

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$mvn = 'C:\Users\16254\Documents\deepseek-harness\default-workspace\learning-workspace\tools\maven\bin\mvn.cmd'
& $mvn -v
```

输出里 `Java version` 应是 21。

### 2. 建库建账号

```sql
CREATE DATABASE IF NOT EXISTS assoc_dev
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'assoc_app'@'127.0.0.1' IDENTIFIED BY '你的密码';
GRANT ALL PRIVILEGES ON assoc_dev.* TO 'assoc_app'@'127.0.0.1';
FLUSH PRIVILEGES;
```

业务表不用手建，Flyway 启动时自动迁移。

### 3. 环境变量

| 变量 | 默认值 | 说明 |
|---|---|---|
| `DB_URL` | jdbc:mysql://127.0.0.1:3307/assoc_dev?... | 一般不用改 |
| `DB_USER` | assoc_app | |
| `DB_PASSWORD` | 空 | **必须设置** |
| `ADMIN_USERNAME` | Yorkchain | 超管登录名 |
| `ADMIN_PASSWORD` | ChangeMe#2026 | **首次启动前必须改** |

```powershell
$env:DB_PASSWORD = '你的数据库密码'
$env:ADMIN_PASSWORD = '你的超管密码'
```

超管只在库里没有这个用户名时才创建；账号建好后再改环境变量不会重置密码。

### 4. 跑起来

```powershell
& $mvn spring-boot:run
```

浏览器打开 http://localhost:8080 。

## 目录结构

```
src/main/java/cn/edu/cust/assoc
├─ controller    接请求、调 service、选视图
├─ service       业务规则与事务边界
├─ repository    Spring Data JPA 接口
├─ domain        JPA 实体 + 业务枚举 + 纯规则
├─ dto           表单对象
├─ config        Security、超管初始化
└─ security      从数据库加载用户

src/main/resources
├─ db/migration  Flyway 脚本（V1 账号、V2 比赛、V3 奖项）
├─ templates     Thymeleaf 页面
├─ static/css    样式
└─ application.yml
```

## 已实现

**公共页面**：`/` 首页、`/about` 关于、`/competitions` 竞赛档案、404、500。

**登录**：`/login` 表单登录、`POST /logout` 登出；用户来自数据库，密码 BCrypt；`status` 不是 ACTIVE 的账号登不上。

**权限**：`/admin/**` 只放行 SUPER_ADMIN / CONTENT_ADMIN / POINTS_ADMIN / PR_ADMIN 四种角色；导航栏按角色显示入口。

**后台**：`/admin` 总览；比赛档案增删改查（`/admin/competitions`）；奖项登记（`/admin/competitions/{id}/awards`）。

**奖项规则**：
- 个人奖分数由「级别 × 名次」自动算：省级 10/7/5、国赛 15/13/11、国际赛 20/18/16、校级 0。
- 团队奖要分给成员，**合计必须等于该奖总分**，多一分少一分都提交不了。

## 还没做

积分流水与四榜、技术文章、评论、媒体上传与审核、账号管理。侧边栏里「积分审核 / 文章审核 / 账号」目前是占位链接。

## 三条红线

1. 积分流水只增不改，撤销靠反向流水。
2. 每笔流水有幂等键，唯一约束兜底。
3. 未审核的媒体不进公共目录，访问必须过鉴权。

详见 [docs/开发规范.md](docs/开发规范.md)。目前这三条是约束，不是已实现的功能。

## 合规

个人名义维护，不代表任何学校或组织；站上不出现校名校徽；文章涉及真实漏洞点时必须发在站内。
