"""Run required builds without masking an earlier command failure; preserve logs and checksums."""
import datetime,json,os,shutil,subprocess
from evidence import ROOT,application_fingerprints,artifact_fingerprints
from local_runtime import LocalRuntime,FLAGS,record_verified_build
from build_lock import backend_build_lock

def main():
 report={'startedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'sourceFingerprints':application_fingerprints(),'cases':[]}
 directory=ROOT/'reports/build';directory.mkdir(exist_ok=True)
 runtime=LocalRuntime();npm=shutil.which('npm.cmd') or shutil.which('npm')
 env=os.environ.copy();env['JAVA_HOME']=str(runtime.java.parent.parent)
 jobs=[('backend-verify',ROOT/'backend',[str(runtime.maven),*runtime.maven_options,'-B','-q','-pl','agri-admin','-am','clean','verify'])]
 for app,scripts in [('admin',['typecheck','build']),('client',['typecheck','build:h5','build:mp-weixin'])]:
  for script in scripts:
   command=[npm,'run',script]+(['--','--base=/admin/'] if app=='admin' and script=='build' else [])
   jobs.append((app+'-'+script.replace(':','-'),ROOT/'apps'/app,command))
 try:
  for name,cwd,command in jobs:
   with (directory/(name+'.log')).open('w',encoding='utf-8') as log:
    if name=='backend-verify':
     with backend_build_lock(ROOT):
      result=subprocess.run(command,cwd=cwd,env=env,stdout=log,stderr=subprocess.STDOUT,creationflags=FLAGS,timeout=600)
      if result.returncode==0:record_verified_build()
    else:
     result=subprocess.run(command,cwd=cwd,env=env,stdout=log,stderr=subprocess.STDOUT,creationflags=FLAGS,timeout=600)
   report['cases'].append({'id':name,'status':'PASS' if result.returncode==0 else 'FAIL','exitCode':result.returncode,'log':'reports/build/'+name+'.log'})
   print(name,report['cases'][-1]['status'],flush=True)
  report['sourceUnchanged']=report['sourceFingerprints']==application_fingerprints()
  if all(c['status']=='PASS' for c in report['cases']):report['artifactFingerprints']=artifact_fingerprints()
 except Exception as e:report['fatal']=str(e)
 report['completedAt']=datetime.datetime.now(datetime.timezone.utc).isoformat()
 report['passed']=sum(c['status']=='PASS' for c in report['cases']);report['failed']=sum(c['status']=='FAIL' for c in report['cases'])+bool(report.get('fatal'))
 (ROOT/'reports/build.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 return 1 if report['failed'] or not report.get('sourceUnchanged') else 0

if __name__=='__main__':raise SystemExit(main())
