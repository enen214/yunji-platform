"""Run the real browser against an isolated backend and both real development apps."""
import os
import shutil
import subprocess
import time
import json,datetime,hashlib
from urllib.request import urlopen
from local_runtime import LocalRuntime, ROOT, FLAGS
from workbench import Frontends
from browser_fixtures import create,renew_code
from evidence import application_fingerprints,source_hash


def main():
    runtime = LocalRuntime()
    frontends = Frontends(runtime.work)
    evidence={'startedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'sourceFingerprints':application_fingerprints()}
    evidence['testSourceSha256']=source_hash('tests','scripts')
    evidence['suites']=[]
    try:
        runtime.start_database()
        runtime.migrate()
        runtime.seed()
        runtime.start_redis()
        runtime.start_api()
        npm = shutil.which('npm.cmd') or shutil.which('npm')
        node = shutil.which('node')
        if not npm or not node:
            raise RuntimeError('Node.js/npm are required')
        frontends.start()
        core = subprocess.run([node, 'browser_suite.cjs'], cwd=ROOT / 'tests', creationflags=FLAGS)
        evidence['suites'].append({'name':'browser','exitCode':core.returncode})
        if core.returncode:return core.returncode
        path=create(runtime)
        env=os.environ.copy();env['YUNJI_BROWSER_FIXTURES']=str(path)
        result=subprocess.run([node,'browser_extended.cjs'],cwd=ROOT/'tests',env=env,creationflags=FLAGS)
        evidence['suites'].append({'name':'browser-extended','exitCode':result.returncode})
        if result.returncode:return result.returncode
        # Fresh issued login code; each browser scenario consumes its own single-use code.
        renew_code(runtime,path)
        result=subprocess.run([node,'browser_closure.cjs'],cwd=ROOT/'tests',env=env,creationflags=FLAGS)
        evidence['suites'].append({'name':'browser-closure','exitCode':result.returncode})
        if result.returncode:return result.returncode
        renew_code(runtime,path)
        result=subprocess.run([node,'browser_reminder_rebuild.cjs'],cwd=ROOT/'tests',env=env,creationflags=FLAGS)
        evidence['suites'].append({'name':'browser-reminder-rebuild','exitCode':result.returncode})
        if result.returncode:return result.returncode
        renew_code(runtime,path)
        result=subprocess.run([node,'browser_prototype.cjs'],cwd=ROOT/'tests',env=env,creationflags=FLAGS)
        evidence['suites'].append({'name':'browser-prototype','exitCode':result.returncode})
        path.unlink(missing_ok=True)
        return result.returncode
    except Exception as e:
        evidence['fatal']=str(e)
        raise
    finally:
        frontends.close()
        runtime.close()
        (runtime.work/'browser-fixtures-private.json').unlink(missing_ok=True)
        evidence.update(completedAt=datetime.datetime.now(datetime.timezone.utc).isoformat(),sourceUnchanged=evidence['sourceFingerprints']==application_fingerprints() and evidence['testSourceSha256']==source_hash('tests','scripts'),logDirectory=str(runtime.work))
        evidence['reportSha256']={name:hashlib.sha256((ROOT/'reports'/f'{name}.json').read_bytes()).hexdigest() for name in ['browser','browser-extended','browser-closure','browser-reminder-rebuild','browser-prototype'] if (ROOT/'reports'/f'{name}.json').exists()}
        (ROOT/'reports/browser-execution.json').write_text(json.dumps(evidence,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')


if __name__ == '__main__':
    raise SystemExit(main())
