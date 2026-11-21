"""Build a real-AppID test mini-program without editing the tracked manifest or reading secrets."""
import argparse
import json
import os
from pathlib import Path
import re
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]

def main():
    parser = argparse.ArgumentParser(description='构建微信开发者工具专用测试包；不含 AppSecret')
    parser.add_argument('--app-id', default=os.environ.get('VITE_WECHAT_APP_ID', ''))
    args = parser.parse_args()
    app = ROOT / 'apps/client'
    app_id = args.app_id
    if not app_id:
        config = app / '.env.wechat.local'
        for line in config.read_text(encoding='utf-8-sig').splitlines() if config.exists() else []:
            if line.startswith('VITE_WECHAT_APP_ID='):
                app_id = line.split('=', 1)[1].strip()
    if not re.fullmatch(r'wx[0-9a-f]{16}', app_id):
        parser.error('请提供 --app-id 或在 apps/client/.env.wechat.local 配置 VITE_WECHAT_APP_ID')
    env = os.environ.copy()
    env['VITE_WECHAT_APP_ID'] = app_id
    # Dedicated output keeps verified standard artifacts unchanged.
    output = ROOT / '.runtime/wechat-devtools'
    env['UNI_OUTPUT_DIR'] = str(output)
    result = subprocess.run([shutil.which('npm.cmd') or shutil.which('npm'), 'run', 'build:mp-weixin', '--', '--mode', 'wechat'],
        cwd=app, env=env, creationflags=getattr(subprocess, 'CREATE_NO_WINDOW', 0))
    if result.returncode:
        return result.returncode
    project = output / 'project.config.json'
    config = json.loads(project.read_text(encoding='utf-8'))
    config['appid'] = app_id
    project.write_text(json.dumps(config, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print('微信开发者工具导入目录：' + str(output))
    print('请使用该 AppID 的开发者身份；本地模拟器可临时不校验合法域名。真机需可达的 HTTPS 后端。')
    return 0

if __name__ == '__main__':
    raise SystemExit(main())
