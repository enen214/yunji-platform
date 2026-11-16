"""Reviewable product candidate gate; never converts local evidence into production approval."""
import datetime,hashlib,json
from evidence import ROOT,application_fingerprints,artifact_fingerprints,source_hash
from database_ops import sources

def read(path):
 try:return json.loads((ROOT/path).read_text(encoding='utf-8'))
 except (OSError,ValueError):return {}

def digest(path):
 p=ROOT/path
 # Git stores text as LF; evidence must survive Windows checkout conversion.
 return hashlib.sha256(p.read_bytes().replace(b'\r\n',b'\n')).hexdigest() if p.is_file() else None

def main():
 current=application_fingerprints();tests=source_hash('tests','scripts');checks={};counts={}
 names=['build','product-baseline-acceptance','browser-product-baseline','workbench-demo','client-compat','wechat-login','operations']
 for name in names:
  r=read('reports/'+name+'.json');cases=r.get('cases',r.get('results',[]))
  passed=r.get('passed',r.get('pass',0));failed=r.get('failed',r.get('fail',0))
  counts[name]={'passed':passed,'failed':failed}
  scopes=['backend'] if name=='operations' else ['backend','client'] if name=='wechat-login' else ['client'] if name=='client-compat' else list(current)
  fresh=all(r.get('sourceFingerprints',{}).get(k)==current[k] for k in scopes)
  test_fresh=name in ['build','operations'] or r.get('testSourceSha256')==tests
  checks[name]=bool(cases) and passed==len(cases) and not failed and not r.get('fatal') and all(c['status']=='PASS' for c in cases) and fresh and test_fresh and r.get('sourceUnchanged',False)
 build=read('reports/build.json')
 try:checks['builtArtifactsCurrent']=build.get('artifactFingerprints')==artifact_fingerprints()
 except Exception:checks['builtArtifactsCurrent']=False
 for name in ['client-compat','wechat-login']:
  checks[name+'ArtifactCurrent']=read('reports/'+name+'.json').get('artifactFingerprints',{}).get('client-mp-weixin')==build.get('artifactFingerprints',{}).get('client-mp-weixin')
 api=read('reports/product-baseline-acceptance.json')
 spec=read('contracts/openapi.json');checks['contractExportCurrent']=bool(spec.get('paths')) and api.get('contractSha256')==digest('contracts/openapi.json')
 snapshot=read('reports/install-snapshot.json');manifest=read('database/install/manifest.json')
 checks['emptyInstallationRoundTrip']=snapshot.get('passed')==6 and snapshot.get('failed')==0 and snapshot.get('manifest')==manifest and bool(manifest) and manifest.get('bootstrapRequired') is True and manifest.get('containsBusinessFixtures') is False and manifest.get('sourceMigrations')=={p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in sources()} and all(digest('database/install/'+p)==h for p,h in manifest.get('files',{}).items())
 provenance=read('reports/provenance.json')
 checks['sourceProvenance']=provenance.get('passed') is True and not provenance.get('forbiddenMatches') and provenance.get('sourceFingerprints')==current
 ledger=read('docs/spec/product-baseline-20261027.json')
 checks['ruleLedgerReviewed']=len(ledger.get('rules',[]))==234 and len({r['id'] for r in ledger.get('rules',[])})==234 and all(r.get('status') not in [None,'pending'] and r.get('implementation') and r.get('verificationLimit') for r in ledger.get('rules',[]))
 docs=['docs/产品基线重制开发与交接-20261027.md','docs/产品基线本地验收记录-20261027.md','docs/产品基线二次开发教程-20261027.md','database/install/README.md','docs/批量导入格式与恢复指南-20261027.md']
 checks['handoffDocuments']=all((ROOT/p).is_file() for p in docs)
 execution=read('reports/product-candidate-execution.json')
 checks['candidateExecutionCurrent']=execution.get('sourceFingerprints')==current and execution.get('testSourceSha256')==tests and execution.get('sourceUnchanged') is True and execution.get('passed')==9 and execution.get('failed')==0 and len(execution.get('cases',[]))==9 and all(c['status']=='PASS' for c in execution.get('cases',[]))
 ready=all(checks.values())
 report={'baseline':'2026-10-27','generatedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'sourceFingerprints':current,'testSourceSha256':tests,'checks':checks,'caseCounts':counts,'pushCandidateReady':ready,'allRequirementsComplete':False,'releaseEligible':False,'productionBlockers':['Real reminder transport is not wired into ReminderWorker; local simulated delivery is not real WeCom task creation or WeChat sending.','Cloud storage adapter and upload consistency require real integration and duplicate local storage removal.','WeChat developer tools/device, target Linux amd64 Docker, current dependency security and receiving-team acceptance are not certified.'],'productGaps':['Per-activity bespoke pages need actual activity content and design.','Avatar editing/upload is not complete.','Snapshot load threshold, real warning-channel integration and target daily backup schedule require further work.','See the 234-rule ledger for engineering variances and unverified rules.'],'evidenceSha256':{name:digest('reports/'+name+'.json') for name in names+['install-snapshot','provenance']},'limit':'Candidate means finite local checks pass and disclosed gaps are reviewable; it does not mean all 234 rules, external delivery or production completion.'}
 (ROOT/'reports/product-release-gate.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 print(json.dumps({'pushCandidateReady':ready,'releaseEligible':False,'checks':checks},ensure_ascii=False,indent=2))
 return int(not ready)

if __name__=='__main__':raise SystemExit(main())
