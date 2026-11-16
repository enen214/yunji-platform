"""Record isolated login regression against exact source, tests and MP artifact."""
import datetime
import json
import shutil
import subprocess
from evidence import ROOT, application_fingerprints, artifact_fingerprints, source_hash

def main():
    before = application_fingerprints()
    result = subprocess.run([shutil.which('node'), 'tests/wechat_login.cjs'], cwd=ROOT, capture_output=True,
        text=True, encoding='utf-8', timeout=60, creationflags=getattr(subprocess, 'CREATE_NO_WINDOW', 0))
    if not result.stdout:
        raise RuntimeError(result.stderr)
    report = json.loads(result.stdout)
    report.update(generatedAt=datetime.datetime.now(datetime.timezone.utc).isoformat(), sourceFingerprints=before,
        sourceUnchanged=before == application_fingerprints(), artifactFingerprints=artifact_fingerprints(), testSourceSha256=source_hash('tests','scripts'))
    (ROOT / 'reports/wechat-login.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(report, ensure_ascii=True))
    return result.returncode or int(not report['sourceUnchanged'])

if __name__ == '__main__':
    raise SystemExit(main())
