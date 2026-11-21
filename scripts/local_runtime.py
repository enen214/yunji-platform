"""Disposable MySQL + business starter runtime shared by QA and manual workbench."""
from pathlib import Path
import os
import subprocess
import socket
import time
import uuid
import json
import hashlib
import base64
import shutil
from urllib.request import urlopen
from build_lock import backend_build_lock

ROOT = Path(__file__).resolve().parents[1]
FLAGS = getattr(subprocess, 'CREATE_NO_WINDOW', 0)

def backend_source_digest():
    digest = hashlib.sha256()
    for source in sorted(p for p in (ROOT/'backend').rglob('*') if p.is_file() and not any(x in p.parts for x in ['target','.git']) and p.name != '.flattened-pom.xml'):
        digest.update(source.relative_to(ROOT).as_posix().encode())
        digest.update(source.read_bytes())
    return digest.hexdigest()

def record_verified_build():
    target=ROOT/'backend/agri-admin/target'
    (target/'verified-source.sha256').write_text(backend_source_digest())
    (target/'verified-artifact.sha256').write_text(hashlib.sha256((target/'agri-admin.jar').read_bytes()).hexdigest())


class LocalRuntime:
    def __init__(self, dbport=33308, apiport=18088):
        self.dbport, self.apiport = dbport, apiport
        self.database = 'yunji_local'
        self.redisproc = None
        self.redisport = 16379
        self.redispassword = uuid.uuid4().hex
        self.local_secret = uuid.uuid4().hex
        self.mysql = Path(os.environ.get('YUNJI_TEST_MYSQL_BIN', r'D:\devenv\mysql\bin'))
        self.maven = Path(os.environ.get('YUNJI_TEST_MAVEN', r'D:\devenv\maven\apache-maven-3.9.16\bin\mvn.cmd'))
        settings=os.environ.get('YUNJI_TEST_MAVEN_SETTINGS')
        self.maven_options=['-s',settings] if settings else []
        self.java = Path(os.environ.get('YUNJI_TEST_JAVA', r'C:\Program Files\Java\jdk-21.0.10\bin\java.exe'))
        self.work = ROOT / '.runtime' / ('run-' + uuid.uuid4().hex[:10])
        self.dbproc = self.apiproc = None
        self.handles = []
        self.build_hash = None

    def preflight(self):
        for program in [self.mysql / 'mysqld.exe', self.mysql / 'mysql.exe', self.mysql / 'mysqladmin.exe', self.maven, self.java]:
            if not program.is_file():
                raise RuntimeError(f'缺少程序：{program}；见本地工具链说明')
        for port in [self.dbport, self.apiport, self.redisport]:
            with socket.socket() as sock:
                try:
                    sock.bind(('127.0.0.1', port))
                except OSError as error:
                    raise RuntimeError(f'端口 {port} 被占用；不改变已有进程') from error

    def _log(self, name):
        handle = (self.work / name).open('w', encoding='utf-8')
        self.handles.append(handle)
        return handle

    def build(self):
        with backend_build_lock(ROOT):
            return self._build_locked()

    def _build_locked(self):
        project = ROOT / 'backend'
        self.build_hash = backend_source_digest()
        jar = project / 'agri-admin/target/agri-admin.jar'
        marker = project / 'agri-admin/target/verified-source.sha256'
        artifact_marker = jar.parent/'verified-artifact.sha256'
        if jar.is_file() and marker.is_file() and marker.read_text() == self.build_hash and artifact_marker.is_file() and artifact_marker.read_text()==hashlib.sha256(jar.read_bytes()).hexdigest():
            return jar
        env = os.environ.copy()
        env['JAVA_HOME'] = str(self.java.parent.parent)
        result = subprocess.run([str(self.maven), *self.maven_options, '-B', '-q', '-pl', 'agri-admin', '-am', 'verify'], cwd=project, env=env,
                                stdout=self._log('maven.log'), stderr=subprocess.STDOUT,
                                creationflags=FLAGS)
        if result.returncode:
            raise RuntimeError(f'Maven verify 失败；查看 {self.work / "maven.log"}')
        record_verified_build()
        return jar

    def start_database(self):
        self.preflight()
        self.work.mkdir(parents=True)
        data = self.work / 'mysql'
        data.mkdir()
        err = self.work / 'mysql-error.log'
        common = [str(self.mysql / 'mysqld.exe'), '--no-defaults',
                  f'--basedir={self.mysql.parent.as_posix()}', f'--datadir={data.as_posix()}']
        result = subprocess.run(common + ['--initialize-insecure', f'--log-error={err.as_posix()}'],
                                cwd=self.work, capture_output=True, creationflags=FLAGS, timeout=60)
        if result.returncode:
            raise RuntimeError(f'临时 MySQL 初始化失败；查看 {err}')
        self.dbproc = subprocess.Popen(common + ['--console', '--bind-address=127.0.0.1',
                          f'--port={self.dbport}', '--mysqlx=0', '--skip-log-bin', '--innodb-buffer-pool-size=64M'],
                          cwd=self.work, stdout=self._log('mysql-console.log'), stderr=subprocess.STDOUT,
                          creationflags=FLAGS)
        deadline = time.monotonic() + 45
        while time.monotonic() < deadline:
            if self.dbproc.poll() is not None:
                raise RuntimeError(f'MySQL 提前退出；查看 {self.work}')
            result = subprocess.run(self._mysqladmin() + ['ping'], capture_output=True, creationflags=FLAGS, timeout=5)
            if result.returncode == 0:
                self.sql(f'CREATE DATABASE {self.database} CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;', database=False)
                return
            time.sleep(.2)
        raise RuntimeError(f'MySQL 启动超时；查看 {self.work}')

    def _mysqladmin(self):
        return [str(self.mysql / 'mysqladmin.exe'), '--no-defaults', '--host=127.0.0.1',
                f'--port={self.dbport}', '--user=root']

    def sql(self, statement, check=True, database=True):
        command = [str(self.mysql / 'mysql.exe'), '--no-defaults', '--host=127.0.0.1',
                   f'--port={self.dbport}', '--user=root', '--default-character-set=utf8mb4',
                   '--batch', '--skip-column-names']
        if database:
            command.append(self.database)
        result = subprocess.run(command, input=statement, encoding='utf-8', capture_output=True,
                                creationflags=FLAGS, timeout=45)
        if check and result.returncode:
            raise RuntimeError(result.stderr)
        return result

    def migrate(self):
        self.sql("CREATE TABLE IF NOT EXISTS schema_history(version VARCHAR(80) PRIMARY KEY,sha256 CHAR(64) NOT NULL,installed_at DATETIME DEFAULT CURRENT_TIMESTAMP)")
        files = [ROOT / 'database/framework-schema.sql'] + sorted((ROOT / 'database/migrations').glob('V*__*.sql'), key=lambda p: int(p.name.split('__')[0][1:]))
        for source in files:
            digest = hashlib.sha256(source.read_bytes()).hexdigest()
            old = self.sql("SELECT sha256 FROM schema_history WHERE version='%s'" % source.name).stdout.strip()
            if old:
                if old != digest: raise RuntimeError('已应用迁移被修改: ' + source.name)
                continue
            self.sql(source.read_text(encoding='utf-8'))
            self.sql("INSERT INTO schema_history(version,sha256) VALUES('%s','%s')" % (source.name,digest))

    def seed(self):
        for source in sorted((ROOT / 'database/fixtures').glob('*.sql')):
            self.sql(source.read_text(encoding='utf-8'))

    def start_redis(self):
        redis = Path(os.environ.get('YUNJI_REDIS_SERVER',r'D:\devenv\redis\redis-server.exe'))
        self.redisproc = subprocess.Popen([str(redis),'--bind','127.0.0.1','--port',str(self.redisport),'--requirepass',self.redispassword,'--save','','--appendonly','no'],cwd=self.work,stdout=self._log('redis.log'),stderr=subprocess.STDOUT,creationflags=FLAGS)
        deadline = time.monotonic()+15
        while time.monotonic()<deadline:
            if self.redisproc.poll() is not None: raise RuntimeError('Redis启动失败')
            try:
                with socket.create_connection(('127.0.0.1',self.redisport),timeout=1): return
            except OSError: time.sleep(.2)
        raise RuntimeError('Redis启动超时')

    def start_api(self, profile='local', extra_env=None, extra_args=None):
        built_jar = self.build()
        # Each instance reads an immutable artifact; later Maven repackaging cannot
        # replace the archive underneath an already starting/running JVM.
        jar = self.work / 'api-instance.jar'
        shutil.copyfile(built_jar, jar)
        # 临时实例里的独立只读用户；测试断言/合成数据由控制脚本用 root 执行。
        password = uuid.uuid4().hex
        self.sql(f"CREATE USER IF NOT EXISTS 'yunji_app'@'127.0.0.1' IDENTIFIED BY '{password}'; ALTER USER 'yunji_app'@'127.0.0.1' IDENTIFIED BY '{password}'; GRANT SELECT,INSERT,UPDATE,DELETE ON {self.database}.* TO 'yunji_app'@'127.0.0.1';")
        env = os.environ.copy()
        env.update({'YUNJI_DB_USERNAME': 'yunji_app', 'YUNJI_DB_PASSWORD': password,
                    'YUNJI_PUBLIC_TENANT': '000000', 'YUNJI_PROFILE':profile,
                    'YUNJI_REDIS_PORT':str(self.redisport),'YUNJI_REDIS_PASSWORD':self.redispassword,
                    'YUNJI_MONITOR_PASSWORD':uuid.uuid4().hex,'YUNJI_TOKEN_SECRET':uuid.uuid4().hex+uuid.uuid4().hex,
                    'YUNJI_LOCAL_SECRET':self.local_secret,'YUNJI_STORAGE_ROOT':str(self.work/'files'), 'YUNJI_API_PORT': str(self.apiport),
                    'YUNJI_IDENTITY_HMAC_KEY':uuid.uuid4().hex+uuid.uuid4().hex,
                    'YUNJI_IDENTITY_ENCRYPTION_KEY':base64.b64encode(os.urandom(32)).decode(),
                    'YUNJI_DB_URL': f'jdbc:mysql://127.0.0.1:{self.dbport}/{self.database}?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&characterEncoding=UTF-8'})
        env.update(extra_env or {})
        self.apiproc = subprocess.Popen([str(self.java), '-Dfile.encoding=UTF-8', '-Dstdout.encoding=UTF-8', '-Dstderr.encoding=UTF-8', '-jar', str(jar),
                          f'--server.port={self.apiport}', '--server.address=127.0.0.1'] + (extra_args or []),
                          env=env, cwd=self.work, stdout=self._log('api.log'), stderr=subprocess.STDOUT,
                          creationflags=FLAGS)
        # Readiness is emitted only after ApplicationRunner guards finish. An HTTP socket
        # can be opened before a failing production guard aborts startup.
        deadline = time.monotonic() + 90
        while time.monotonic() < deadline:
            if self.apiproc.poll() is not None:
                raise RuntimeError(f'API 提前退出；查看 {self.work / "api.log"}')
            try:
                with urlopen(self.url + '/api/v1/catalog/categories', timeout=2) as response:
                    if response.status == 200 and 'YUNJI_APPLICATION_READY' in (self.work/'api.log').read_text(encoding='utf-8',errors='replace'):
                        time.sleep(.5)
                        if self.apiproc.poll() is not None:
                            raise RuntimeError(f'API 启动门禁失败；查看 {self.work / "api.log"}')
                        return
            except OSError:
                time.sleep(.2)
        raise RuntimeError(f'API 启动超时；查看 {self.work / "api.log"}')

    @property
    def url(self):
        return f'http://127.0.0.1:{self.apiport}'

    def close(self):
        if self.apiproc and self.apiproc.poll() is None:
            self.apiproc.terminate()
            try:
                self.apiproc.wait(timeout=10)
            except subprocess.TimeoutExpired:
                self.apiproc.kill()
                self.apiproc.wait(timeout=10)
        if self.dbproc and self.dbproc.poll() is None:
            try:
                subprocess.run(self._mysqladmin() + ['shutdown'], capture_output=True, creationflags=FLAGS, timeout=15)
                self.dbproc.wait(timeout=10)
            except (subprocess.TimeoutExpired, OSError):
                self.dbproc.terminate()
                self.dbproc.wait(timeout=10)
        if self.redisproc and self.redisproc.poll() is None:
            self.redisproc.terminate()
            self.redisproc.wait(timeout=10)
        for handle in self.handles:
            handle.close()


def main():
    import argparse
    parser = argparse.ArgumentParser(description='启动隔离本地读取环境；Ctrl+C 停止自己创建的进程')
    parser.add_argument('--db-port', type=int, default=33308)
    parser.add_argument('--api-port', type=int, default=18088)
    args = parser.parse_args()
    runtime = LocalRuntime(args.db_port, args.api_port)
    try:
        runtime.start_database()
        runtime.migrate()
        runtime.seed()
        runtime.start_redis()
        runtime.start_api()
        print(json.dumps({'api': runtime.url, 'workdir': str(runtime.work), 'scope': 'DEVELOPMENT'}, ensure_ascii=False), flush=True)
        print('保持此窗口。另开窗口在 apps/admin 运行 npm run dev，在 apps/client 运行 npm run dev:h5；Ctrl+C 停止。', flush=True)
        while runtime.apiproc.poll() is None and runtime.dbproc.poll() is None:
            time.sleep(1)
    except KeyboardInterrupt:
        pass
    finally:
        runtime.close()


if __name__ == '__main__':
    main()
