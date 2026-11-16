"""Bind the isolated mini-program regression to current source and built artifacts."""
import datetime,json,shutil,subprocess
from evidence import ROOT,application_fingerprints,artifact_fingerprints,source_hash

def main():
    before=application_fingerprints()
    artifacts=artifact_fingerprints()
    result=subprocess.run([shutil.which('node'),'tests/client_compat.cjs'],cwd=ROOT,capture_output=True,text=True,encoding='utf-8',timeout=60,creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
    report=json.loads(result.stdout)
    report.update(generatedAt=datetime.datetime.now(datetime.timezone.utc).isoformat(),sourceFingerprints=before,sourceUnchanged=before==application_fingerprints(),artifactFingerprints=artifacts,testSourceSha256=source_hash('tests','scripts'))
    (ROOT/'reports/client-compat.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,ensure_ascii=True))
    return result.returncode or int(not report['sourceUnchanged'])

if __name__=='__main__':raise SystemExit(main())
