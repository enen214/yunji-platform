"""Compare the research plan with the actual running contract without pretending compatibility."""
import datetime,json,re
from pathlib import Path
from evidence import ROOT,application_fingerprints
import hashlib

def main():
 plan=json.loads((ROOT/'docs/spec/work-items.json').read_text(encoding='utf-8'))
 spec=json.loads((ROOT/'contracts/openapi.json').read_text(encoding='utf-8'))
 actual={m.upper()+' '+p for p,ops in spec.get('paths',{}).items() for m in ops if m in {'get','post','put','patch','delete','head','options'}}
 decisions=json.loads((ROOT/'contracts/route-decisions.json').read_text(encoding='utf-8'))['decisions']
 lookup={(d['featureId'],d['planned']):d for d in decisions}
 assert len(lookup)==len(decisions),'Duplicate route decision'
 rows=[]
 for item in plan:
  routes=[];missing=[]
  for route in item.get('routes',[]):
   if route.startswith(('WORKER ','SCRIPT ')):routes.append({'planned':route,'status':'NON_HTTP_DELIVERY'})
   elif route in actual:routes.append({'planned':route,'status':'PRESENT'})
   else:
    decision=lookup.get((item['featureId'],route))
    if decision and all(target in actual for target in decision['actualTargets']):
     routes.append({'planned':route,'status':decision['decision'],'actualTargets':decision['actualTargets'],'semantics':decision['semantics']})
    else:routes.append({'planned':route,'status':'UNRESOLVED'});missing.append(route)
  rows.append({'featureId':item['featureId'],'feature':item['feature'],'scope':item['scope'],'external':item['external'],'routes':routes,'missingCount':len(missing)})
 report={'generatedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'source':'docs/spec/work-items.json + contracts/openapi.json + contracts/route-decisions.json','actualPaths':len(spec.get('paths',{})),'actualOperations':len(actual),'rows':rows,'missingHttpRoutes':sum(r['missingCount'] for r in rows),'decidedDifferences':len(decisions),'platformPendingRoutes':[{'featureId':r['featureId'],**x} for r in rows for x in r['routes'] if x['status']=='PLATFORM_PENDING'],'notes':['Planned routes are research identifiers, not compatibility obligations.','PRESENT means a route string exists; REPLACED/COMPOSED document explicit reviewed semantics, not legacy compatibility.','NON_HTTP_DELIVERY requires worker or script evidence.','PLATFORM_PENDING is a real blocked operation, not satisfied by local synthetic tests.']}
 report['sourceFingerprints']=application_fingerprints();report['openapiSha256']=hashlib.sha256((ROOT/'contracts/openapi.json').read_bytes()).hexdigest()
 (ROOT/'reports/contract-reconciliation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 lines=['# 计划接口与新契约核对','','新契约为准；REPLACED/COMPOSED 是明确的新功能路径决策，不保留旧代码或同义兼容路由。PRESENT 只表示路径存在，实际子操作仍按用例和功能审查核对。','','|功能|范围|计划路由|结果|新接口与语义|','|---|---|---|---|---|']
 for row in rows:
  for route in row['routes']:lines.append(f"|{row['featureId']} {row['feature']}|{row['scope']}|`{route['planned']}`|{route['status']}|{'；'.join(route.get('actualTargets',[]))} {route.get('semantics','')}|")
 (ROOT/'docs/spec/计划契约核对.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
 print(json.dumps({'actualPaths':report['actualPaths'],'actualOperations':report['actualOperations'],'missingHttpRoutes':report['missingHttpRoutes']},ensure_ascii=False))

if __name__=='__main__':main()
