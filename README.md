# 山海迹 · 旅游攻略分享平台

山海迹是一套面向毕业设计展示的本地旅游内容平台，由独立 C 端、独立管理端和 Spring Boot API 组成。项目覆盖目的地、图文攻略、旅行路线、专题、创作者、互动、审核、举报、运营推荐和统计看板，并包含可重复生成的本地素材及大规模演示数据。
## 注意
本项目源码为半成品，如需完整版源码可联系

V：Q1848148016

Q:1848148016

## 访问地址

| 应用 | 地址 |
| --- | --- |
| C 端用户网站 | http://localhost:5173 |
| 后台管理系统 | http://localhost:5174 |
| Spring Boot API | http://localhost:8080 |
| OpenAPI 文档 | http://localhost:8080/swagger-ui.html |

## 演示账号

统一密码：`123456`

| 角色 | 账号 |
| --- | --- |
| 系统管理员 | `admin` |
| 内容审核员 | `auditor01` |
| 旅游创作者 | `creator01` |
| 普通用户 | `user01` |

## 技术基线

- Java 21.0.3、Spring Boot 3.3.2、Maven 3.9.8、MyBatis Plus 3.5.7
- MySQL 8.0.46、JJWT 0.12.5、Springdoc 2.5.0、Apache POI 5.2.5
- Node.js 24.11.0、npm 11.6.1、Vue 3.3.4、Vite 6.4.3
- Element Plus 2.11.1、Vue Router 4.2.2、Pinia 2.0.30、Axios 1.18.1、ECharts 6.1.0

## 项目结构

```text
backend/        Spring Boot 后端
user-web/       面向普通用户的图文旅行网站
admin-web/      面向管理员与审核员的管理系统
sql/            建库、基础数据、演示数据和删除脚本
assets-source/  原始本地演示素材
scripts/        工具发现、素材和数据生成脚本
docs/           需求、设计、接口、运行与测试文档
uploads/        运行期上传目录
logs/           运行日志
```

## 数据库

默认数据库名为 `travel_share`。`init-database.bat` 按顺序导入 `sql/schema.sql`、`sql/base-data.sql` 和 `sql/demo-data.sql`。所有密码均保存为 BCrypt 摘要，演示内容为原创生成数据。

## 常见问题

- PowerShell 提示不能运行 `npm.ps1`：项目脚本使用 `npm.cmd`，无需修改系统执行策略。
- 终端找不到 Java 或 Maven：脚本会只读发现 IntelliJ IDEA 自带 JDK 与 Maven。
- 数据库连接失败：确认 `MySQL80` 服务已启动，并检查 `.env` 中的账号和密码。
- 页面刷新 404：Vite 开发服务器已配置 SPA fallback；生产环境需要同样回退到 `index.html`。

更多信息见 `docs/Windows运行说明.md`、`docs/系统功能说明.md`、`docs/接口说明.md` 与 `docs/二次开发说明.md`。

## 快速开始

1. 双击 `check-environment.bat` 检查环境。
2. 在项目根目录创建 `.env`，参考 `.env.example` 填写本机 MySQL 密码。
3. 双击 `init-database.bat` 导入数据库与演示数据。
4. 双击 `start-all.bat` 启动三个应用。

也可以分别运行 `start-backend.bat`、`start-user-web.bat` 与 `start-admin-web.bat`。完整构建使用 `build-all.bat`。
