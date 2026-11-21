"""Actual npm audits and Maven SBOM advisory inventory; unavailable sources never count as clean."""
import concurrent.futures,datetime,json,shutil,subprocess,time
from urllib.request import Request,urlopen
from evidence import ROOT,source_hash
from local_runtime import FLAGS

def fetch(url,body=None):
 req=Request(url,data=json.dumps(body).encode() if body is not None else None,headers={'Content-Type':'application/json','User-Agent':'yunji-dependency-inventory/1','Accept-Encoding':'identity','Connection':'close'})
 last=None
 for attempt in range(4):
  try:
   with urlopen(req,timeout=90) as response:
    # Read until EOF instead of assuming a proxy's Content-Length is accurate.
    data=response.read();return json.loads(data)
  except Exception as e:
   last=e
   if attempt<3:time.sleep(2**attempt)
 # Some local proxies truncate urllib responses. Curl is an independent public-advisory fallback.
 curl=shutil.which('curl.exe') or shutil.which('curl')
 if curl:
  command=[curl,'--http1.1','--silent','--show-error','--fail','--retry','2','--max-time','45','-H','Content-Type: application/json',url]
  if body is not None:command+=['--data-binary','@-']
  result=subprocess.run(command,input=json.dumps(body).encode() if body is not None else None,capture_output=True,creationflags=FLAGS,timeout=150)
  if result.returncode==0:return json.loads(result.stdout)
 raise last

def main():
 report={'generatedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'limitations':['Advisory databases do not prove exploitability or detect all vulnerabilities.','SBOM covers resolved Maven dependencies; company modules are not external packages.','npm includes build and runtime dependencies; affected-package count is not unique-advisory count.'],'npm':{},'maven':{},'errors':[]}
 npm=shutil.which('npm.cmd') or shutil.which('npm')
 for app in ['admin','client']:
  try:
   result=subprocess.run([npm,'audit','--json'],cwd=ROOT/'apps'/app,capture_output=True,timeout=90,creationflags=FLAGS)
   audit=json.loads(result.stdout.decode('utf-8'));(ROOT/f'reports/npm-{app}-audit.json').write_text(json.dumps(audit,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
   if 'metadata' not in audit:raise RuntimeError(str(audit.get('error','audit unavailable')))
   report['npm'][app]={'sourceSha256':source_hash('apps/'+app),'counts':audit['metadata']['vulnerabilities'],'report':f'reports/npm-{app}-audit.json'}
  except Exception as e:report['errors'].append({'scope':'npm-'+app,'error':str(e)})
 try:
  bom=json.loads((ROOT/'reports/sbom/backend-bom.json').read_text(encoding='utf-8'))
  inventory=json.loads((ROOT/'reports/sbom/inventory.json').read_text(encoding='utf-8'))
  if inventory['unidentifiedLibraries']:raise RuntimeError('Unidentified runtime libraries: '+', '.join(inventory['unidentifiedLibraries']))
  props={p['name']:p['value'] for p in bom.get('metadata',{}).get('properties',[])}
  if props.get('yunji:source-sha256')!=source_hash('backend','database/migrations'):raise RuntimeError('SBOM is stale; run sbom_inventory.py after build')
  packages=sorted({(c.get('group','')+':'+c['name'],c['version']) for c in bom['components'] if c.get('purl','').startswith('pkg:maven/') and c.get('group')!='com.sunshine'})
  rows=[];ids=set()
  for start in range(0,len(packages),10):
   batch=packages[start:start+10];data=fetch('https://api.osv.dev/v1/querybatch',{'queries':[{'package':{'ecosystem':'Maven','name':name},'version':version} for name,version in batch]})
   if len(data['results'])!=len(batch):raise RuntimeError('OSV batch result length mismatch')
   for package,result in zip(batch,data['results']):
    found=[v['id'] for v in result.get('vulns',[])];ids.update(found)
    rows.append({'package':package[0],'version':package[1],'advisoryIds':found})
  report['maven']={'sourceSha256':source_hash('backend','database/migrations'),'queriedPackages':len(rows),'affectedPackages':sum(bool(r['advisoryIds']) for r in rows),'uniqueAdvisories':len(ids),'packages':rows,'advisories':[],'advisoryDetailsIncomplete':False}
  details=[]
  with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
   futures={pool.submit(fetch,'https://api.osv.dev/v1/vulns/'+i):i for i in sorted(ids)}
   for future in concurrent.futures.as_completed(futures):
    try:
     v=future.result();details.append({'id':v['id'],'aliases':v.get('aliases',[]),'summary':v.get('summary'),'severity':v.get('severity',[]),'references':v.get('references',[]),'affected':v.get('affected',[])})
    except Exception as e:report['maven']['advisoryDetailsIncomplete']=True;report['errors'].append({'scope':'osv-advisory','id':futures[future],'error':str(e)})
  report['maven']['advisories']=sorted(details,key=lambda v:v['id'])
 except Exception as e:report['errors'].append({'scope':'maven-osv','error':str(e)})
 report['scanComplete']=not report['errors']
 report['releaseSecurityEligible']=report['scanComplete'] and not report['maven'].get('uniqueAdvisories',0) and all(not v['counts'].get('high',0) and not v['counts'].get('critical',0) for v in report['npm'].values())
 (ROOT/'reports/security.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 print(json.dumps({'scanComplete':report['scanComplete'],'npm':{k:v['counts'] for k,v in report['npm'].items()},'mavenPackages':report['maven'].get('queriedPackages'),'mavenAdvisories':report['maven'].get('uniqueAdvisories'),'errors':report['errors'],'releaseSecurityEligible':report['releaseSecurityEligible']},ensure_ascii=False))
 return 0 if report['scanComplete'] else 1

if __name__=='__main__':raise SystemExit(main())
