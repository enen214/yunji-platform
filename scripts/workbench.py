"""Local synthetic workbench. Owns only processes it starts; no demo assets are read."""
import argparse
import json
import os
from pathlib import Path
import shutil
import socket
import subprocess
import time
import uuid
from urllib.request import Request, urlopen
from local_runtime import LocalRuntime, ROOT, FLAGS

CURRENT = ROOT / '.runtime/current.json'
USERS = {'alice': '91000000000000001', 'bob': '91000000000000002', 'closure': '91000000000000003'}


class Frontends:
    def __init__(self, work):
        self.work, self.processes, self.logs = work, [], []

    def start(self):
        npm = shutil.which('npm.cmd') or shutil.which('npm')
        if not npm:
            raise RuntimeError('需要 Node.js/npm')
        for port in (5173, 5174):
            with socket.socket() as sock:
                sock.bind(('127.0.0.1', port))
        for app, command, port in [('admin', 'dev', 5174), ('client', 'dev:h5', 5173)]:
            log = (self.work / (app + '-vite.log')).open('w', encoding='utf-8')
            self.logs.append(log)
            proc = subprocess.Popen([npm, 'run', command, '--', '--port', str(port), '--strictPort'],
                                    cwd=ROOT / 'apps' / app, stdout=log, stderr=subprocess.STDOUT,
                                    creationflags=FLAGS, start_new_session=os.name != 'nt')
            self.processes.append(proc)
            deadline = time.monotonic() + 60
            while time.monotonic() < deadline:
                if proc.poll() is not None:
                    raise RuntimeError(f'{app} 启动失败，见 {log.name}')
                try:
                    with urlopen(f'http://127.0.0.1:{port}/', timeout=2):
                        break
                except OSError:
                    time.sleep(.2)
            else:
                raise RuntimeError(f'{app} 启动超时')

    def close(self):
        for proc in reversed(self.processes):
            if proc.poll() is None:
                if os.name == 'nt':
                    subprocess.run(['taskkill', '/PID', str(proc.pid), '/T', '/F'],
                                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, creationflags=FLAGS)
                else:
                    import signal
                    os.killpg(proc.pid, signal.SIGTERM)
                proc.wait(timeout=10)
        for log in self.logs:
            log.close()


def private_write(path, data):
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding='utf-8')
    if os.name == 'nt':
        sid = subprocess.check_output(['whoami', '/user', '/fo', 'csv', '/nh'], creationflags=FLAGS).decode().strip().split(',')[-1].strip('"')
        result = subprocess.run(['icacls', str(path), '/inheritance:r', '/grant:r', f'*{sid}:(F)'],
                                capture_output=True, creationflags=FLAGS)
        if result.returncode:
            path.unlink()
            raise RuntimeError('无法限制本地密钥文件的访问权限')
    else:
        path.chmod(0o600)


def state():
    current = json.loads(CURRENT.read_text(encoding='utf-8'))
    work = Path(current['work']).resolve()
    if not work.is_relative_to((ROOT / '.runtime').resolve()):
        raise RuntimeError('工作台记录路径错误')
    data = json.loads((work / 'workbench-private.json').read_text(encoding='utf-8'))
    with urlopen(data['api'] + '/api/v1/catalog/categories', timeout=3):
        pass
    return work, data


def api(data, path, body, token=None, secret=False):
    headers = {'Content-Type': 'application/json'}
    if secret:
        headers['X-Local-Secret'] = data['localSecret']
    if token:
        headers['Authorization'] = 'Bearer ' + token
    with urlopen(Request(data['api'] + path, json.dumps(body).encode(), headers, method='POST'), timeout=15) as res:
        result = json.load(res)
        if result['code'] != 200:
            raise RuntimeError(result.get('msg', '请求失败'))
        return result['data']


def main():
    parser = argparse.ArgumentParser(description='本地合成验收工作台，不连接旧库/真实平台')
    sub = parser.add_subparsers(dest='command', required=True)
    start = sub.add_parser('start')
    start.add_argument('--wechat-login-config', type=Path, help='显式加载私有微信登录凭据；提醒仍使用合成通道')
    start.add_argument('--no-demo-data', action='store_true', help='不加载首页演示资讯与轮播，保留空内容测试库')
    sub.add_parser('stop'); sub.add_parser('status')
    code = sub.add_parser('code'); code.add_argument('user', choices=USERS, default='alice', nargs='?')
    auth = sub.add_parser('authorize'); auth.add_argument('user', choices=USERS, default='alice', nargs='?')
    auth.add_argument('--quota', type=int, default=5)
    args = parser.parse_args()
    if args.command != 'start':
        work, data = state()
        if args.command == 'stop':
            (work / 'stop.request').touch()
            print('已请求工作台停止自身服务。')
        elif args.command == 'status':
            print(json.dumps({k: v for k, v in data.items() if k != 'localSecret'}, ensure_ascii=False, indent=2))
        else:
            issued = api(data, '/api/local/login-codes', {'userId': USERS[args.user], 'appId': 'local-client'}, secret=True)
            if args.command == 'code':
                print(json.dumps({'user': args.user, **issued}, ensure_ascii=False))
            else:
                if not 1 <= args.quota <= 10:
                    raise RuntimeError('每次合成额度限定为 1–10')
                login = api(data, '/api/v1/auth/customer/local', {'code': issued['code'], 'appId': 'local-client'})
                try:
                    result = api(data, '/api/local/notifications/authorization',
                                 {'provider': 'WECHAT_SUBSCRIBE', 'appId': 'local-app', 'callbackKey': uuid.uuid4().hex,
                                  'amount': args.quota, 'outcome': 'SENT'}, login['accessToken'], True)
                    result['changeReminders'] = api(data, '/api/local/reminders/authorization',
                        {'receiptKey': uuid.uuid4().hex, 'wecomBound': True, 'amount': args.quota, 'outcome': 'SUCCESS'}, login['accessToken'], True)
                    print(json.dumps(result, ensure_ascii=False))
                finally:
                    api(data, '/api/v1/auth/customer/logout', {}, login['accessToken'])
        return
    runtime = LocalRuntime()
    frontends = Frontends(runtime.work)
    wechat_env = {'YUNJI_WECHAT_APP_ID': '', 'YUNJI_WECHAT_SECRET': ''}
    if args.wechat_login_config:
        import re
        private = json.loads(args.wechat_login_config.read_text(encoding='utf-8-sig'))
        if set(private) != set(wechat_env) or not re.fullmatch(r'wx[0-9a-f]{16}', private.get('YUNJI_WECHAT_APP_ID', '')) or not re.fullmatch(r'[0-9a-fA-F]{32}', private.get('YUNJI_WECHAT_SECRET', '')):
            raise RuntimeError('微信私有配置字段或格式错误；只接受 AppID 和 AppSecret')
        wechat_env.update(private)
    try:
        runtime.start_database(); runtime.migrate(); runtime.seed(); runtime.start_redis(); runtime.start_api(extra_env=wechat_env,extra_args=['--yunji.reminder.scheduler-enabled=true'])
        if not args.no_demo_data:
            from workbench_demo import seed
            seed(runtime)
        frontends.start()
        private_write(runtime.work / 'workbench-private.json', {
            'api': runtime.url, 'admin': 'http://127.0.0.1:5174', 'client': 'http://127.0.0.1:5173',
            'scope': 'LOCAL_WITH_REAL_WECHAT_LOGIN' if args.wechat_login_config else 'LOCAL_SYNTHETIC_ONLY', 'localSecret': runtime.local_secret, 'pid': os.getpid()})
        CURRENT.write_text(json.dumps({'work': str(runtime.work)}), encoding='utf-8')
        print('工作台已启动：管理端 http://127.0.0.1:5174，客户 H5 http://127.0.0.1:5173', flush=True)
        if args.wechat_login_config:
            print('真实微信登录已配置；手机号授权成功调用可能消耗微信平台额度。提醒仍为本地合成通道。', flush=True)
        print('另一终端执行 python scripts/workbench.py code alice 获取两分钟单次登录码。Ctrl+C 停止。', flush=True)
        while not (runtime.work / 'stop.request').exists():
            if any(p.poll() is not None for p in [runtime.apiproc, runtime.dbproc, runtime.redisproc, *frontends.processes]):
                raise RuntimeError('工作台服务退出，查看该次运行日志')
            time.sleep(.5)
    except KeyboardInterrupt:
        pass
    finally:
        frontends.close(); runtime.close()
        if CURRENT.exists() and json.loads(CURRENT.read_text())['work'] == str(runtime.work):
            CURRENT.unlink()
        (runtime.work / 'workbench-private.json').unlink(missing_ok=True)


if __name__ == '__main__':
    main()
