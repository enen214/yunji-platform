# 云迹重制工程

当前本地分支：`feature/product-baseline-rebuild-20261027`。这是产品基线重制候选，**本轮按用户2026-10-28授权推送研发分支，本地可推送检查与生产发布批准分别判断**。不迁旧业务数据，不引用yangben demo业务代码。公司cis4j授权基座保留。C端uni-app单源码构建H5/微信小程序，后台独立Web。

| 需要做什么 | 现行入口 |
|---|---|
| 理解本轮架构、功能、限制和交接 | [产品基线开发与交接](docs/产品基线重制开发与交接-20261027.md) |
| 核对所有234条需求 | [逐条台账](docs/spec/产品基线逐条台账-20261027.md)、[机器台账](docs/spec/product-baseline-20261027.json) |
| 查看最新真实验收与候选门禁 | [验收记录](docs/产品基线本地验收记录-20261027.md)、[门禁JSON](reports/product-release-gate.json) |
| 接手改代码 | [本轮二次开发教程](docs/产品基线二次开发教程-20261027.md)、[工程决策](docs/decisions/产品基线工程决策-20261027.md) |
| 空库安装、增量升级与回滚 | [安装说明](database/install/README.md)、[部署交接](docs/部署团队交接操作书.md) |
| 用户/运营逐步操作与接力 | [操作验收手册](docs/产品基线操作与接力验收手册-20261027.md) |
| 批量导入 | [JSON格式与恢复指南](docs/批量导入格式与恢复指南-20261027.md) |
| 审计过程与旧决定 | [本轮开发日志](docs/开发日志-20261027-产品基线重制.md)、[历史README](docs/README历史-20261022.md) |

在仓库根目录配置Python/Java21/Maven3.9/MySQL8/Redis/Node工具链并安装依赖后：

```powershell
python scripts/workbench.py start
# 另一个终端获取两分钟有效、仅可使用一次的本地客户登录码
python scripts/workbench.py code alice
```

本地C端 `http://127.0.0.1:5173`，管理端 `http://127.0.0.1:5174`。工作台默认附合成资讯/专题/轮播；正式initialize和安装快照不含演示数据。工作台Ctrl+C停止。完整工具安装见 [本地开发说明](docs/本地开发与验收使用说明.md)。

复跑：`python scripts/product_candidate_acceptance.py`，再 `python scripts/product_release_gate.py`。前者构建并顺序执行本轮API/浏览器/演示/运维恢复/安装快照/兼容/登录/来源检查；后者拒绝缺失或过期证据。旧版reports/release-gate.json是旧产品审查口径，不能代替本轮 product-release-gate.json。

已知未完成包括真实提醒传输接线、微信云存储端到端及本地副本整改、头像编辑、实际活动独立页、外部告警与目标每日备份定时配置；微信开发工具/真机、Linux amd64 Docker、安全发布和接收团队验收仍独立门禁。逐条台账中“实现入口存在”不是已验收完成。

仓库和分支提交以实际 `git rev-parse HEAD` 与远端SHA核对；本轮2026-10-28已获用户分支推送授权；不合并main。不要用旧分支、历史交付ZIP或旧验收数字替代当前源码。
