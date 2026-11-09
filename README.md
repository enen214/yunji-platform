# 云迹内容平台

本仓库包含「云迹内容平台」的三个子项目：

| 目录 | 说明 | 技术栈 |
| ---- | ---- | ------ |
| `backend` | 后端服务 | Java 17 + Spring Boot 3 |
| `admin` | 管理端前端 | Vue 3 + Vite |
| `client` | 用户端（小程序 / H5） | uni-app（Vue 3） |

## 本地运行

### 后端（backend）
- 用 IDEA 打开 `backend`，运行 `DromaraApplication`，激活 `local` 配置，端口 8088。
- 数据库、Redis 等连接信息通过**环境变量**注入，参考 `backend/config/application-local.yml`。

### 管理端（admin）
- `npm install` 后 `npm run dev`，端口 5174，已配置代理到后端 8088。

### 用户端（client）
- 用 HBuilderX 导入 `client` 目录，运行到微信开发者工具。

## 安全说明
- 数据库密码、Redis 密码、登录密钥等一律通过环境变量注入，**不写入仓库**。
- 本地运行请自行准备环境变量（参考 `backend/config/application-local.yml` 与各 `*.env.example` 文件），不要将真实密钥提交到仓库。
