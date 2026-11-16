"""New MySQL lifecycle tooling. Uses explicit credentials and checksum migrations, no fixtures."""
import argparse
import datetime
import getpass
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
from contextlib import contextmanager

ROOT = Path(__file__).resolve().parents[1]
FLAGS = getattr(subprocess, 'CREATE_NO_WINDOW', 0)


class Mysql:
    def __init__(self, host=None, port=None, database=None, user=None, password=None, bindir=None):
        self.host = host or os.environ.get('YUNJI_MIGRATION_HOST', '127.0.0.1')
        self.port = port or int(os.environ.get('YUNJI_MIGRATION_PORT', '3306'))
        self.database = database or os.environ.get('YUNJI_MIGRATION_DATABASE', 'yunji')
        self.user = user or os.environ.get('YUNJI_MIGRATION_USER', '')
        self.password = password if password is not None else os.environ.get('YUNJI_MIGRATION_PASSWORD', '')
        self.bindir = bindir or os.environ.get('YUNJI_MYSQL_BIN', '')
        if not re.fullmatch(r'[A-Za-z][A-Za-z0-9_]{0,63}', self.database):
            raise ValueError('数据库名称非法')
        if not self.user:
            raise ValueError('缺少 YUNJI_MIGRATION_USER')

    def program(self, name):
        suffix = '.exe' if os.name == 'nt' else ''
        found = str(Path(self.bindir) / (name + suffix)) if self.bindir else shutil.which(name + suffix)
        if not found or not Path(found).is_file():
            raise RuntimeError(f'缺少 {name}；设置 YUNJI_MYSQL_BIN')
        return found

    def command(self, name='mysql', database=True):
        return [self.program(name), '--no-defaults', '--protocol=TCP', '--host=' + self.host,
                '--port=' + str(self.port), '--user=' + self.user, '--default-character-set=utf8mb4',
                ] + (['--connect-timeout=10'] if name=='mysql' else []) + ([self.database] if database else [])

    def env(self):
        env = os.environ.copy(); env['MYSQL_PWD'] = self.password
        return env

    def sql(self, statement, database=True):
        result = subprocess.run(self.command(database=False) +
                                ['--batch', '--skip-column-names'] + ([self.database] if database else []),
                                input=statement, encoding='utf-8', capture_output=True, env=self.env(),
                                timeout=180, creationflags=FLAGS)
        if result.returncode:
            raise RuntimeError(result.stderr.strip())
        return result.stdout.strip()

    @contextmanager
    def lock(self):
        command = self.command(database=False) + ['--batch', '--skip-column-names', '--unbuffered']
        proc = subprocess.Popen(command, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                stderr=subprocess.PIPE, text=True, encoding='utf-8', env=self.env(), creationflags=FLAGS)
        try:
            proc.stdin.write(f"SELECT GET_LOCK('yunji_migrate_{self.database}',0);\n"); proc.stdin.flush()
            if proc.stdout.readline().strip() != '1':
                raise RuntimeError('数据库正在迁移或连接失败；拒绝并发执行')
            yield
        finally:
            if proc.poll() is None:
                proc.stdin.close(); proc.wait(timeout=15)
            proc.stdout.close(); proc.stderr.close()


def sources():
    return [ROOT / 'database/framework-schema.sql'] + sorted(
        (ROOT / 'database/migrations').glob('V*__*.sql'), key=lambda p: int(p.name.split('__')[0][1:]))


def migrate(db, initialize=False):
    with db.lock():
        if initialize:
            exists = db.sql(f"SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='{db.database}'", False)
            if exists != '0':
                raise RuntimeError('初始化只允许不存在的新库；已有库请用 migrate')
            db.sql(f'CREATE DATABASE `{db.database}` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;', False)
        db.sql('CREATE TABLE IF NOT EXISTS schema_history(version VARCHAR(80) PRIMARY KEY,sha256 CHAR(64) NOT NULL,installed_at DATETIME DEFAULT CURRENT_TIMESTAMP)')
        applied = dict(line.split('\t') for line in db.sql('SELECT version,sha256 FROM schema_history').splitlines())
        files = sources()
        unknown = set(applied) - {p.name for p in files}
        if unknown:
            raise RuntimeError('目标数据库版本高于当前发布包；禁止降级迁移: ' + ','.join(sorted(unknown)))
        for source in files:
            digest = hashlib.sha256(source.read_bytes()).hexdigest()
            if source.name in applied:
                if applied[source.name] != digest:
                    raise RuntimeError('已应用迁移被修改: ' + source.name)
                continue
            db.sql(source.read_text(encoding='utf-8-sig'))
            db.sql(f"INSERT INTO schema_history(version,sha256) VALUES('{source.name}','{digest}')")
            print('APPLIED ' + source.name, flush=True)


def bootstrap(db, password):
    import bcrypt
    if len(password) < 16 or password.lower() in {'admin123', 'passwordpassword'}:
        raise RuntimeError('管理员初始口令至少 16 字符')
    encoded = bcrypt.hashpw(password.encode(), bcrypt.gensalt(rounds=12)).decode()
    with db.lock():
        if db.sql('SELECT COUNT(*) FROM sys_user WHERE user_id=1') != '1':
            raise RuntimeError('缺少框架管理员')
        db.sql(f"UPDATE sys_user SET password='{encoded}',status='0',update_time=UTC_TIMESTAMP() WHERE user_id=1; UPDATE sys_user SET status='1' WHERE user_id<>1; DELETE FROM customer_session; DELETE FROM local_login_code;")
        # Framework bootstrap users are disabled; synthetic business data must never be imported.
    print('管理员口令已替换，其余框架初始账号已停用。')


def backup(db, storage, destination):
    storage = Path(storage).resolve(); destination = Path(destination).resolve()
    if not storage.is_dir() or destination.exists() or destination.is_relative_to(storage) or storage.is_relative_to(destination):
        raise RuntimeError('备份要求独立、尚不存在的输出目录和现有私有文件目录')
    destination.mkdir(parents=True)
    command = db.command('mysqldump')[:-1] + ['--single-transaction', '--routines', '--triggers', '--hex-blob', '--order-by-primary', '--no-tablespaces', '--set-gtid-purged=OFF', db.database]
    with (destination / 'database.sql').open('wb') as out:
        result = subprocess.run(command, stdout=out, stderr=subprocess.PIPE, env=db.env(), timeout=300, creationflags=FLAGS)
    if result.returncode:
        raise RuntimeError(result.stderr.decode('utf-8', errors='replace'))
    shutil.copytree(storage, destination / 'files')
    files = {p.relative_to(destination).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
             for p in sorted(destination.rglob('*')) if p.is_file()}
    manifest = {'createdAt': datetime.datetime.now(datetime.timezone.utc).isoformat(),
                'database': db.database, 'consistency': 'REQUIRES_APPLICATION_STOPPED', 'sha256': files}
    (destination / 'manifest.json').write_text(json.dumps(manifest, indent=2), encoding='utf-8')
    print(str(destination))


def restore(db, source, storage):
    source = Path(source).resolve(); storage = Path(storage).resolve()
    manifest = json.loads((source / 'manifest.json').read_text(encoding='utf-8'))
    for name, digest in manifest['sha256'].items():
        file = (source / name).resolve()
        if not file.is_relative_to(source) or not file.is_file() or hashlib.sha256(file.read_bytes()).hexdigest() != digest:
            raise RuntimeError('备份校验失败: ' + name)
    if storage.exists():
        raise RuntimeError('恢复附件目标必须不存在，禁止覆盖')
    with db.lock():
        if db.sql(f"SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='{db.database}'", False) != '0':
            raise RuntimeError('恢复目标库必须不存在，禁止覆盖')
        db.sql(f'CREATE DATABASE `{db.database}` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;', False)
        db.sql((source / 'database.sql').read_text(encoding='utf-8'))
        shutil.copytree(source / 'files', storage)
    print('已恢复到新数据库和新附件目录；原库保持不动。')


def main():
    parser = argparse.ArgumentParser(description='生产库生命周期；不读取 demo/fixtures')
    sub = parser.add_subparsers(dest='command', required=True)
    sub.add_parser('initialize'); sub.add_parser('migrate'); sub.add_parser('bootstrap')
    b = sub.add_parser('backup'); b.add_argument('--storage', required=True); b.add_argument('--destination', required=True)
    b.add_argument('--application-stopped', action='store_true', required=True)
    r = sub.add_parser('restore'); r.add_argument('--source', required=True); r.add_argument('--storage', required=True)
    args = parser.parse_args(); db = Mysql()
    if args.command in ('initialize', 'migrate'):
        migrate(db, args.command == 'initialize')
    elif args.command == 'bootstrap':
        password = getpass.getpass('新管理员口令（不回显）: ')
        if password != getpass.getpass('再次输入: '):
            raise RuntimeError('口令不一致')
        bootstrap(db, password)
    elif args.command == 'backup':
        backup(db, args.storage, args.destination)
    else:
        restore(db, args.source, args.storage)


if __name__ == '__main__':
    main()
