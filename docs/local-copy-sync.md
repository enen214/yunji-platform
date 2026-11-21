# 桌面副本同步规矩

`/Users/rcj/Documents/GitHub/yunji-platform` 是唯一真源：所有代码改动、配置改动、契约变更只能落在仓库里，再由同步脚本单向下发到桌面运行副本 `/Users/rcj/Desktop/hd`。桌面副本只是 IDEA 的运行产物，允许与仓库存在下面四类白名单差异；除白名单之外，两侧必须逐字节一致（换行符统一 LF）。

## 白名单（桌面侧允许不同，且不会被同步脚本覆盖）

| 类别 | 路径 | 说明 |
| --- | --- | --- |
| 本地配置 | `agri-admin/src/main/resources/application-local.yml` | 桌面侧 `secret` 为明文（本地运行必需），其余内容必须与仓库一致；仓库侧为 `${YUNJI_WECHAT_SECRET:}` |
| 本地覆盖 | `config/application-local.yml` | 环境连接与开关覆盖，两地都必须是 `local-simulate: false` |
| 运行配置 | `.run/*.run.xml` | IDEA 运行配置，桌面侧带明文凭据，仓库侧只留空值模板 |
| 产物与编辑器状态 | `target/`、`.idea/`、`*.log`、`export-report.json`、`.DS_Store` | 编译产物、编辑器状态、日志，不参与比对与同步 |

## 同步命令

```bash
# 预览将要覆盖/删除的文件清单，不写盘
bash scripts/sync-local-copy.sh --dry-run

# 实际下发，并自动执行三项自检
bash scripts/sync-local-copy.sh
```

脚本行为：

- 真源 `backend/` → `/Users/rcj/Desktop/hd/`，真源 `contracts/` → `/Users/rcj/Desktop/hd/contracts/`
- 使用 `rsync -a --delete`，仓库删掉的文件在桌面也会消失
- 自动自检：忽略白名单与换行符后 `diff -rq` 无输出、桌面 `application-local.yml` 的 `secret` 非空、桌面侧 CRLF 源码文件数为 0；任一项不过即非零退出
- 脚本只写桌面，绝不写仓库

同步完成后如需验证运行，桌面目录执行 `mvn -o clean compile`（必须带 `clean`，否则 `target/` 里残留的旧 class 仍可能被 Spring 扫到）。

## 三条禁止事项

1. **禁止手改桌面副本当成果。** 在 `/Users/rcj/Desktop/hd` 里改出来的代码不算完成，下次同步会被覆盖丢失；所有成果必须写在仓库里。
2. **禁止拿桌面副本反向覆盖仓库。** 仓库是唯一真源，桌面侧的改动如需保留，必须人工判断后重新在仓库中实现。
3. **禁止把密钥写进仓库任何文件。** `YUNJI_WECHAT_SECRET`、`YUNJI_TOKEN_SECRET`、`YUNJI_IDENTITY_HMAC_KEY`、`YUNJI_DB_PASSWORD` 等凭据一律走本机环境变量或本机未纳入版本管理的文件，仓库内的运行配置模板只保留空值。

## IDEA 首次打开项目

- 运行配置请选 `DromaraApplication-local`，不要用 IDEA 自动生成的 `DromaraApplication`。自动生成的那条只包含 IDEA 能推断出的内容，不会读取 `.run` 文件，环境变量为空，会导致后端读不到 `YUNJI_WECHAT_SECRET`、登录接口返回 503。
- 桌面副本的 `.idea/runConfigurations/DromaraApplication-local.xml` 由同步脚本之外单独维护：它含有本地明文密钥，属于本机文件，不属于仓库内容，不要提交、也不要反向同步回仓库。
