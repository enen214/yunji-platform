# 新部署方案

> **2026-10-27 产品基线候选**：本轮迁移至V26，分支`feature/product-baseline-rebuild-20261027`未自动推送。按[现行开发与交接](../docs/产品基线重制开发与交接-20261027.md)及安装快照说明操作。已有库仅增量migrate，不得导入schema/seed；真实提醒传输未接线、云存储未实测，本机Windows不代表Linux/Docker验收。下方旧分支、迁移上限及功能描述为历史。

> **2026-10-22 候选版交接更新**：当前分支 `feature/client-prototype-rebuild-20261021`，迁移最新为V23。按 [本轮升级与回退说明](../docs/C端反馈对齐设计与交接-20261022.md) 配套更新三端；下方旧日期的迁移上限/分支仅为历史。V1～V22禁止改写，已有V22新工程库增量执行V23。

来源判断与边界见 `../docs/decisions/部署环境反推与选型.md`。这里的配置与工具为新编写，无旧 demo 脚本。目标是 Linux LTS + JDK21 + MySQL8 + Redis7 + Nginx；当前机器只有 Windows 原生运行环境，Linux/systemd/Nginx 尚待目标机实测。没有真实平台资源时不启用通知发送，不伪装成生产已就绪。

## Docker 适配（2026-10-12）

用户已确认 Linux x86_64/amd64 与 Docker 可用。容器交付操作见 [docker/README.md](docker/README.md)，沿用同一套后端配置和数据库工具，不默认替换现有 MySQL/Redis。本机未装 Docker，目标机运行结论仍待接收团队验证。

## 拓扑和目录

Nginx 443 → 静态客户 H5 / 管理端 `/admin/` → 回环 API18088 → 私网 MySQL3306、Redis6379、私有文件目录。小程序由微信平台发布，接口访问同一个 HTTPS 域名。仅公开80/443；数据库、Redis、API不向公网开放。

`/opt/yunji/releases/<版本>` 保存不可变应用与静态产物；`/opt/yunji/current` 是活动版本链接。`/var/lib/yunji/files` 是持久附件，`/etc/yunji/api.env` 是稳定密钥配置，均独立于发布目录。运行用户 `yunji`，不得用 root 运行应用。

## 从新库上线

1. 安装目标软件并创建专用用户、目录、TLS证书；配置目录0700，配置文件0600，私有文件0700。填写新的域名和凭据，不使用样本域名/密码。配置 NTP、UTC数据库时间，显示时区 Asia/Shanghai。
2. `python -m pip install -r scripts/requirements.txt`。迁移账号拥有建库/DDL/DML权限，只交给发布工具；应用账号只给目标库 SELECT/INSERT/UPDATE/DELETE，不得授予全局权限。
3. 在受控运维终端导出 migration.env 中变量，执行 `python scripts/database_ops.py initialize`。不存在的数据库才可初始化；不会导入任何 fixtures。
4. 执行 `python scripts/database_ops.py bootstrap`，通过隐藏输入设置新口令，停用框架其他初始账号。然后按职责通过后台开通工作人员。口令不作为命令参数。
5. 填好 api.env，稳定保管身份 HMAC与加密密钥，不能每次重启换密钥。安装 service，`systemctl daemon-reload`，`systemctl start yunji-api`；检查日志和回环公开目录接口。
6. 管理端以 `npm run build -- --base=/admin/` 构建；客户 H5用 `npm run build:h5`。装入发布目录，部署 Nginx配置，先 `nginx -t` 再重载。API、登录、管理端和附件必须各做一次实际域名检查。
7. 验证外网无法访问 local入口、OpenAPI和私有磁盘路径；验证初始测试账号停用；通过真实管理页面验证验证码登录。平台未接入项保持禁用，并交接清单。

## 升级和回滚

先停写（Nginx维护页或停止应用），备份，再执行 `migrate`。校验和变化或目标库有当前包不认识的迁移时拒绝继续。MySQL DDL不是完整事务；中途失败必须保留日志并恢复备份到新库，不能盲目重跑已失败的半段迁移。

检查新发布包校验和，放入新release目录；迁移成功后原子切换current链接、启动应用并进行验收。失败先停新版本。只有明确证明旧程序兼容新schema，才切回旧程序；否则使用下述新库恢复并同时切回对应旧程序和附件快照，不删除/覆盖原库。不提供自动执行未知schema降级的脚本。

## 备份恢复

停止应用后执行：

```text
python scripts/database_ops.py backup --application-stopped --storage /var/lib/yunji/files --destination /backup/yunji/20261004
```

命令包含事务快照、SQL与全部附件 SHA256清单。备份包含身份信息，使用受限目录和磁盘加密，异地副本按项目保留策略管理。`--application-stopped` 是操作者确认，工具无法凭此证明服务实际停止。

恢复目标库名改为新的 `yunji_restore_...`，附件使用不存在的新路径：

```text
python scripts/database_ops.py restore --source /backup/yunji/20261004 --storage /var/lib/yunji/files-restored
```

先校验全部备份内容，再恢复到新库；拒绝覆盖现有库/附件。更改应用连接和附件路径后启动，验收登录、公开资讯、私有文件、资料包、活动和评审。备份与恢复必须匹配同一停写时点。运行环境、身份密钥和平台凭据另作加密备份。

## 可观测与未验证项

systemd崩溃重启与journald日志；监控API探活、磁盘容量、MySQL/Redis连通性、失败登录、迁移失败、通知UNKNOWN积压。单机方案不能承诺高可用。首次选型为4vCPU/8GB RAM、100GB SSD（非容量测试结论），上线前按附件规模、并发用户与恢复目标做压测和容量调整。

真实微信/企微发送适配与平台回调仍待实现和联调；当前生产通知任务会拒绝调用。Linux服务、TLS、Nginx配置、MySQL大小写行为与平台实际网络尚未在此Windows机器验证。不得把本目录存在视作生产部署通过。
