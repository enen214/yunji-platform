"""Evidence-based local and release gates; external certification is never inferred."""
import datetime,hashlib,json
from evidence import ROOT,application_fingerprints,artifact_fingerprints,source_hash

def read(name):
 p=ROOT/name
 return json.loads(p.read_text(encoding='utf-8-sig')) if p.exists() else {}
def digest(name):return hashlib.sha256((ROOT/name).read_bytes()).hexdigest()
def passed(r):return bool(r.get('cases')) and r.get('failed')==0 and not r.get('fatal') and all(c['status']=='PASS' for c in r['cases'])

def main():
 current=application_fingerprints()
 def fresh(r,keys):return all(r.get('sourceFingerprints',{}).get(k)==current[k] for k in keys)
 api=read('reports/latest.json');execution=read('reports/browser-execution.json');build=read('reports/build.json');ops=read('reports/operations.json');security=read('reports/security.json');contracts=read('reports/contract-reconciliation.json');provenance=read('reports/provenance.json');review=read('reports/feature-review.json')
 browser={n:read('reports/'+n+'.json') for n in ['browser','browser-extended','browser-closure','browser-reminder-rebuild','browser-prototype']}
 browser_fresh=fresh(execution,['backend','admin','client']) and execution.get('sourceUnchanged',False) and len(execution.get('suites',[]))==5 and execution.get('testSourceSha256')==source_hash('tests','scripts') and all(s['exitCode']==0 for s in execution['suites']) and all(execution.get('reportSha256',{}).get(n)==digest('reports/'+n+'.json') for n in browser)
 try:artifacts_fresh=build.get('artifactFingerprints')==artifact_fingerprints()
 except Exception:artifacts_fresh=False
 review_fresh=fresh(review,['backend','admin','client']) and review.get('requirementsSha256')==digest('docs/spec/local-acceptance-requirements.json')
 three_end=read('reports/three-end-acceptance.json')
 three_end_fresh=fresh(three_end,['backend','admin','client']) and three_end.get('requirementsSha256')==digest('docs/spec/three-end-requirements.json') and all(three_end.get('evidenceSha256',{}).get(n)==digest('reports/'+n+'.json') for n in ['latest','browser','browser-extended','browser-closure','reminder-rebuild','browser-reminder-rebuild'])
 scan_fresh=security.get('maven',{}).get('sourceSha256')==current['backend'] and all(security.get('npm',{}).get(app,{}).get('sourceSha256')==source_hash('apps/'+app) for app in ['admin','client'])
 rebuild=read('reports/reminder-rebuild.json')
 compat=read('reports/client-compat.json')
 login=read('reports/wechat-login.json')
 demo=read('reports/workbench-demo.json')
 prototype_spec=read('docs/spec/client-prototype-requirements.json')
 prototype_cases={c['id']:c for report in [api,browser.get('browser-prototype',{})] for c in report.get('cases',[])}
 prototype_ready=bool(prototype_spec.get('items')) and all(item.get('caseIds') and all(prototype_cases.get(case,{}).get('status')=='PASS' for case in item['caseIds']) for item in prototype_spec.get('items',[]))
 checks={
  'workbenchDemoAcceptancePassed':passed(demo) and fresh(demo,['backend','admin','client']) and demo.get('sourceUnchanged',False) and demo.get('testSourceSha256')==source_hash('tests','scripts'),
  'wechatLoginLocalRegressionPassed':passed(login) and fresh(login,['backend','client']) and login.get('sourceUnchanged',False) and login.get('testSourceSha256')==source_hash('tests','scripts') and login.get('artifactFingerprints',{}).get('client-mp-weixin')==build.get('artifactFingerprints',{}).get('client-mp-weixin'),
  'clientPrototypeAcceptancePassed':prototype_ready and passed(browser.get('browser-prototype',{})) and browser_fresh,

  'clientCompatibilityRegressionPassed':passed(compat) and fresh(compat,['client']) and compat.get('sourceUnchanged',False) and compat.get('artifactFingerprints',{}).get('client-mp-weixin')==build.get('artifactFingerprints',{}).get('client-mp-weixin') and compat.get('testSourceSha256')==source_hash('tests','scripts'),
  'reminderRebuildLocalPassed':passed(rebuild) and fresh(rebuild,['backend']) and rebuild.get('sourceUnchanged',False) and rebuild.get('upgradeFromV20',False),
  'reminderRebuildBrowserPassed':passed(browser['browser-reminder-rebuild']) and browser_fresh,
  'localApiSuitePassed':passed(api) and fresh(api,['backend']) and api.get('sourceUnchanged',False),
  'localBrowserCoreFlowPassed':passed(browser['browser']) and browser_fresh,
  'localBrowserExtendedFlowPassed':passed(browser['browser-extended']) and browser_fresh,
  'localBrowserClosureFlowPassed':passed(browser['browser-closure']) and browser_fresh,
  'localOperationsRehearsalPassed':passed(ops) and fresh(ops,['backend']) and ops.get('sourceUnchanged',False),
  'buildChecksPassed':passed(build) and fresh(build,['backend','admin','client']) and build.get('sourceUnchanged',False) and artifacts_fresh,
  'contractDecisionsResolved':contracts.get('missingHttpRoutes')==0 and fresh(contracts,['backend']) and contracts.get('openapiSha256')==digest('contracts/openapi.json'),
  'versionedLocalScenariosPassed':bool(review.get('rows')) and not review.get('localScenariosFailed') and review_fresh,
  'sourceProvenancePassed':provenance.get('passed',False) and fresh(provenance,['backend','admin','client']),
  'threeEndReviewedLocalScenariosPassed':bool(three_end.get('rows')) and three_end.get('localScenariosPassed',False) and three_end_fresh,
 }
 local=all(checks.values());platform=read('reports/platform-acceptance.json');linux=read('reports/linux-acceptance.json');team=read('reports/team-acceptance.json')
 platform_ready=platform.get('status')=='PASS' and fresh(platform,['backend','admin','client'])
 linux_ready=linux.get('status')=='PASS' and fresh(linux,['backend','admin','client'])
 team_ready=team.get('status')=='ACCEPTED' and fresh(team,['backend','admin','client'])
 security_ready=scan_fresh and security.get('scanComplete',False) and security.get('releaseSecurityEligible',False)
 blockers=[]
 if not local:blockers.append('本地证据未满足：'+', '.join(k for k,v in checks.items() if not v))
 if not platform_ready:blockers.append('独立平台用途/模板/绑定证据尚缺；正式通知传输与回调、调度接线及真实登录/送达验收待完成。')
 if not linux_ready:blockers.append('目标 Linux amd64 的容器构建/启动、TLS、Nginx、文件权限、恢复与容量验收待目标机器。')
 if not security_ready:blockers.append('安全发布门禁未通过；见 reports/security.json 和 docs/依赖安全处置.md，未自动接受风险。')
 if not team_ready:blockers.append('接收团队尚未确认产品文案、最终角色矩阵和试验/正式发布边界。')
 gate={'generatedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'sourceFingerprints':current,'phase':'LOCAL_BASELINE_VERIFIED_EXTERNAL_GATES_PENDING' if local else 'LOCAL_ACCEPTANCE_IN_PROGRESS','localStandaloneAcceptanceComplete':local,'localFeatureAcceptanceComplete':local and platform_ready,'releaseEligible':local and platform_ready and linux_ready and security_ready and team_ready,**checks,'localBrowserCasesPassed':sum(r.get('passed',0) for r in browser.values()),'apiCasesPassed':api.get('passed',0),'apiCasesFailed':api.get('failed',0),'operationsCasesPassed':ops.get('passed',0),'sourceExecutionUnchanged':execution.get('sourceUnchanged',False),'securityScanComplete':scan_fresh and security.get('scanComplete',False),'securityReleaseEligible':security_ready,'platformVerified':platform_ready,'linuxVerified':linux_ready,'teamAccepted':team_ready,'baselineFeatures':review.get('baselineFeatures'),'platformPendingFeatures':review.get('platformPending',[]),'candidateFeatures':['AUTH-05','CAT-06'],'contractRoutesCompared':contracts.get('actualOperations'),'contractMissingPlannedHttpRoutes':contracts.get('missingHttpRoutes'),'localImplementationGaps':[k for k,v in checks.items() if not v],'blockers':blockers,'limits':['Local standalone gate covers the finite versioned local scenarios, not exhaustive combinations or production certification.','Real notification bindings, template mapping and dispatcher integration still require platform scope and evidence.'],'evidence':['reports/wechat-login.json','reports/browser-prototype.json','reports/client-compat.json','reports/reminder-rebuild.json','reports/browser-reminder-rebuild.json','reports/three-end-acceptance.json','reports/docker-static.json','reports/latest.json','reports/browser.json','reports/browser-extended.json','reports/browser-closure.json','reports/browser-execution.json','reports/operations.json','reports/build.json','reports/feature-review.json','reports/security.json','reports/provenance.json','reports/contract-reconciliation.json']}
 (ROOT/'reports/release-gate.json').write_text(json.dumps(gate,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 items=read('docs/spec/work-items.json');rows={r['featureId']:r for r in review.get('rows',[])}
 lines=['# 功能证据追踪','','完整子操作决定见 功能子操作验收审查.md；这是固定场景证据，不以关联案例宣称全部功能或真实平台完成。','','|编号|功能|场景状态|外部|API用例|','|---|---|---|---|---|']
 for item in items:
  r=rows.get(item['featureId'],{});item['completeFeatureAcceptance']=r.get('status','NO_REVIEW');item['executedCaseIds']=r.get('requiredApiCaseIds',[]);item['acceptance']=r.get('acceptanceLimit','未审核')
  lines.append('|'+ '|'.join([item['featureId'],item['feature'],item['completeFeatureAcceptance'],item['external'],', '.join(item['executedCaseIds']) or '—'])+'|')
 (ROOT/'docs/spec/work-items.json').write_text(json.dumps(items,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 (ROOT/'docs/spec/功能证据追踪.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
 print(json.dumps(gate,ensure_ascii=True,indent=2))

if __name__=='__main__':main()
