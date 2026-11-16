"""Evaluate versioned acceptance requirements, never infer completeness from route/test linkage."""
import datetime,json,hashlib
from evidence import ROOT,application_fingerprints

def read(name):return json.loads((ROOT/name).read_text(encoding='utf-8'))

def main():
 requirements=read('docs/spec/local-acceptance-requirements.json')
 plan=read('docs/spec/work-items.json');api=read('reports/latest.json');operations=read('reports/operations.json')
 contracts=read('reports/contract-reconciliation.json');current=application_fingerprints()
 browser=[read('reports/'+f+'.json') for f in ['browser','browser-extended','browser-closure']]
 api_cases={c['id']:c for c in api.get('cases',[])};ui_cases={c['id']:c for r in browser for c in r.get('cases',[])}
 assert {x['featureId'] for x in requirements['features']}=={x['featureId'] for x in plan}
 assert len({x['featureId'] for x in requirements['features']})==len(requirements['features'])
 routes={r['featureId']:r for r in contracts['rows']};rows=[]
 for feature in requirements['features']:
  fid=feature['featureId'];missing=[]
  for kind,required,cases in [('API',feature['requiredApiCaseIds'],api_cases),('BROWSER',feature['requiredBrowserCaseIds'],ui_cases)]:
   missing += [kind+':'+case for case in required if cases.get(case,{}).get('status')!='PASS']
  if routes[fid]['missingCount']:missing.append('UNRESOLVED_CONTRACT')
  if feature['specialEvidence']=='operations' and (operations.get('failed')!=0 or not operations.get('cases')):missing.append('OPERATIONS')
  if feature['specialEvidence']=='reports' and (api.get('failed')!=0 or not all(r.get('cases') and not r.get('fatal') and r.get('failed')==0 for r in browser)):missing.append('REPORT_SUITE')
  if feature['scope']=='CANDIDATE_CONFIRM':status='NOT_IN_BASELINE'
  elif missing:status='LOCAL_SCENARIOS_FAILED'
  elif feature['external']=='PLATFORM_PENDING':status='LOCAL_SCENARIOS_PASSED_PLATFORM_PENDING'
  else:status='LOCAL_SCENARIOS_PASSED'
  rows.append({**feature,'status':status,'missingEvidence':missing,'routes':routes[fid]['routes']})
 report={'generatedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'requirementsSha256':hashlib.sha256((ROOT/'docs/spec/local-acceptance-requirements.json').read_bytes()).hexdigest(),'sourceFingerprints':current,'rows':rows,'baselineFeatures':sum(r['scope']!='CANDIDATE_CONFIRM' for r in rows),'localScenariosFailed':[r['featureId'] for r in rows if r['status']=='LOCAL_SCENARIOS_FAILED'],'platformPending':[r['featureId'] for r in rows if r['external']=='PLATFORM_PENDING'],'limitations':['This is a finite explicitly versioned local acceptance baseline, not exhaustive product or production certification.','H5 visibility transitions are simulated in the timing case; real mini-program lifecycle remains unverified.','Real notification bindings, quota evidence and production dispatcher wiring are not represented as completed.']}
 (ROOT/'reports/feature-review.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 lines=['# 功能子操作验收审查','','固定要求在 `local-acceptance-requirements.json`。每项列出实际案例、路由差异决定和外部条件；案例通过仅说明这些已定义场景。真实平台、目标 Linux、容量、产品/权限签字单独验收。','','|功能|本地场景|外部|缺失证据|决策|','|---|---|---|---|---|']
 for r in rows:lines.append('|'+ '|'.join([r['featureId'],r['status'],r['external'],', '.join(r['missingEvidence']) or '—',r['decision']])+'|')
 (ROOT/'docs/spec/功能子操作验收审查.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
 print(json.dumps({k:report[k] for k in ['baselineFeatures','localScenariosFailed','platformPending']},ensure_ascii=False))
 return bool(report['localScenariosFailed'])

if __name__=='__main__':raise SystemExit(main())
