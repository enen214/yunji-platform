# Linux amd64 容器适配

> **2026-10-27 产品基线候选**：本轮迁移至V26，分支`feature/product-baseline-rebuild-20261027`未自动推送。按[现行开发与交接](../../docs/产品基线重制开发与交接-20261027.md)及安装快照说明操作。已有库仅增量migrate，不得导入schema/seed；真实提醒传输未接线、云存储未实测，本机Windows不代表Linux/Docker验收。下方旧分支、迁移上限及功能描述为历史。

> **2026-10-22 候选版交接更新**：当前分支 `feature/client-prototype-rebuild-20261021`，迁移最新为V23。按 [本轮升级与回退说明](../../docs/C端反馈对齐设计与交接-20261022.md) 配套更新三端；下方旧日期的迁移上限/分支仅为历史。V1～V22禁止改写，已有V22新工程库增量执行V23。

2026-10-12 用户确认目标为 Linux x86_64/amd64，Docker 可用。本目录是新写的交付适配，不含 demo 部署代码。本地没有 Docker CLI，尚无镜像构建或 Linux 启动通过证据。

沿用现有 MySQL 8、Redis、应用变量和身份密钥。容器只打包 API 与静态前端，不擅自更换后端配置或接管生产数据库。

## 开发交付产物

从源码构建时先执行 `python scripts/build_acceptance.py`，确保完整后端 JAR、管理端 `/admin/` 构建、客户 H5 构建存在。微信小程序包单独交给微信开发工具，不装入 Web 镜像。

若接收的是 `delivery_bundle.py` 生成的交付 ZIP，`source` 中不含 target/dist：先将 `artifacts/backend/agri-admin.jar` 放入 `source/backend/agri-admin/target/`，将 `artifacts/admin/` 内容放入 `source/apps/admin/dist/`，将 `artifacts/client-h5/` 内容放入 `source/apps/client/dist/build/h5/`，再以 `source` 作为 Docker 构建根目录。对照 manifest 校验产物后操作，不能混用不同包的源码和产物。

在仓库根目录，填写环境变量（以下为示意，真实值由部署团队提供）：

```sh
export YUNJI_RELEASE=20261012-review
export YUNJI_API_ENV_FILE=/etc/yunji/api.env
export YUNJI_FILES_DIR=/var/lib/yunji/files
export YUNJI_WEB_PORT=8080
docker compose -f deploy/docker/compose.yaml config --quiet
docker compose -f deploy/docker/compose.yaml build
```

`api.env` 按 `deploy/api.env.example` 填写。数据库和 Redis 地址必须是容器可达的私网地址，容器内 `127.0.0.1` 不是宿主机。禁止放入 `YUNJI_LOCAL_SECRET` 或本地模拟配置。身份 HMAC/加密密钥持续保留，不能每次生成。

## 部署团队接收步骤

1. 检查 `uname -m` 为 x86_64，确认 Docker Compose v2；确认数据库、Redis访问地址、目录权限和容量。附件目录 UID/GID 为 10001:10001，权限 0700，配置文件 0600。
2. 按原 `deploy/README.md` 执行数据库初始化或停写备份后的增量迁移。本轮从 V20 增量增加 V21/V22；V1–V22 不得改写。不导入合成验收 fixtures。
3. 首次初始化管理员并停用基座初始测试账号；运行时数据库用户与迁移用户分离。
4. `docker compose -f deploy/docker/compose.yaml up -d`。查看 `logs --tail=100 api`，检查回环 `http://127.0.0.1:8080/api/v1/catalog/categories`、H5 首页与 `/admin/`，完成登录、资讯发布、附件下载的实际回环。
5. 确认回环 `/api/local/login-codes`、`/v3/api-docs` 返回 404。API 无宿主机公开端口，文件目录不经 Nginx 发布。
6. 生产团队配置 HTTPS、外部访问、微信域名等上线事项，并在目标环境填写接收验收记录。开发侧不代签生产验收。

升级使用新的镜像版本标签，保留旧镜像和同时间点数据库/附件备份。数据库回滚遵守原交接书，不执行未经验证的 schema 降级。禁止 `docker compose down -v` 作为常规升级操作。

此文件不代表真实微信/企微、Linux、TLS、容器镜像或生产负载验收完成。

## 2026-10-13 提醒重制说明

当前新增跟踪和批次域及自动调度；正式真实发送仍关闭，不以 Docker 启动成功证明企微/微信可用。执行迁移前停写、备份，新版 API 与两端应成套替换。使用 V22 库降回旧程序不能当作新提醒功能回滚成功；需要保留新库快照并按既有恢复流程评估，禁止直接删除新表。详见 `docs/强提醒与后台重制开发交接-20261013.md`。
