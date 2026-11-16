# 空库安装快照

本目录 `schema.sql`、`seed.sql` 和 `manifest.json` 是由空的本地临时 MySQL 按框架 schema + V1–V26 有序迁移生成，并在第二个空库往返校验的安装快照。manifest 内含迁移及文件 SHA-256。**只用于全新空库**，不含业务资讯、用户或演示数据；执行 `seed.sql` 会写入禁用的框架管理员占位账号，必须运行 bootstrap 设置独立口令后才能开放。

生产运行数据库优先使用版本迁移器，升级时禁止导入这套 schema/seed：

```text
python scripts/database_ops.py initialize
python scripts/database_ops.py bootstrap
```

`initialize` 要求数据库不存在，会按框架 schema、V1–V26 串行迁移并验证已应用迁移校验和。`bootstrap` 交互式读取两遍不少于16字符的新口令，更新管理员并停用其余框架用户。应用账号不得持有迁移 DDL 权限。不要把密码放命令行参数、Git 或聊天中。

已有库升级：先停写并完成数据库+私有文件备份，再运行 `python scripts/database_ops.py migrate`。迁移是增量不可变脚本；MySQL DDL 不保证整体事务。失败时保留日志，按部署交接恢复到新库，不要手工删除 schema_history 或编辑已发布迁移。

快照手工导入只供 DBA 在已确认新建的空库上采用：导入 `schema.sql` 后导入 `seed.sql`，随后必须执行同一 bootstrap。若省略步骤，管理员仍禁用且口令为不可登录占位值。手工导入路径不自动执行应用侧校验，应优先使用迁移器。

重新生成/验证：`python scripts/install_snapshot.py`。生成过程仅访问隔离的临时库；不得从 `yangben`、甲方旧库、当前运行库或本地演示工作台导出。
