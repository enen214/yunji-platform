"""Sequential reproducible product candidate regression. Does not push or publish."""
import datetime,json,os,subprocess,sys
from evidence import ROOT,application_fingerprints,source_hash
from local_runtime import FLAGS

def main():
 before=application_fingerprints();test_digest=source_hash('tests','scripts');rows=[]
 jobs=['build_acceptance','product_baseline_acceptance','product_browser_acceptance','workbench_demo_acceptance','operations_acceptance','install_snapshot','client_compat_acceptance','wechat_login_acceptance','provenance_audit']
 directory=ROOT/'reports/product-run';directory.mkdir(exist_ok=True)
 for name in jobs:
  print('START '+name,flush=True)
  with (directory/(name+'.log')).open('w',encoding='utf-8') as log:
   try:
    result=subprocess.run([sys.executable,str(ROOT/'scripts'/(name+'.py'))],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,env={**os.environ,'PYTHONIOENCODING':'utf-8'},creationflags=FLAGS,timeout=1800)
    code=result.returncode
   except subprocess.TimeoutExpired:code=124
  rows.append({'id':name,'status':'PASS' if code==0 else 'FAIL','exitCode':code,'log':'reports/product-run/'+name+'.log'})
  print(name,rows[-1]['status'],flush=True)
 report={'generatedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'sourceFingerprints':before,'testSourceSha256':test_digest,'sourceUnchanged':before==application_fingerprints() and test_digest==source_hash('tests','scripts'),'cases':rows,'passed':sum(r['status']=='PASS' for r in rows),'failed':sum(r['status']=='FAIL' for r in rows)}
 (ROOT/'reports/product-candidate-execution.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 print('Run python scripts/product_release_gate.py after updating the evidence ledger and acceptance record.',flush=True)
 return int(bool(report['failed']) or not report['sourceUnchanged'])

if __name__=='__main__':raise SystemExit(main())
