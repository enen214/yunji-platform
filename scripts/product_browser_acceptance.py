"""Real three-end product browser checks with private disposable fixtures."""
import datetime,json,os,shutil,subprocess,sys,uuid
from urllib.request import Request,urlopen
from local_runtime import LocalRuntime,ROOT,FLAGS
from workbench import Frontends,private_write
from workbench_demo import seed
from evidence import application_fingerprints,source_hash
sys.path.insert(0,str(ROOT/'tests'))
from http_suite import Suite

def main():
 r=LocalRuntime();frontends=Frontends(r.work);before=application_fingerprints();fatal=None
 (ROOT/'reports/browser-product-baseline.json').write_text(json.dumps({'cases':[],'passed':0,'failed':0}),'utf-8')
 try:
  r.start_database();r.migrate();r.seed();r.start_redis();r.start_api();values=seed(r)
  s=Suite(r);s.login_staff('admin','admin123');s.login_customer('alice',91000000000000001)
  def call(method,path,body=None,actor='admin',**kw):return s.call(method,path,body,actor,**kw)
  category=call('GET','/api/v1/catalog/categories',actor=None)[0]
  detail=call('GET','/api/v1/articles/'+values['articleIds'][0],actor=None);label=detail['labels'][0];keyword=detail['keywords'][0]
  call('PUT','/api/v1/me/follows/KEYWORD/'+keyword['id'],{'enabled':True},actor='alice')
  call('PUT','/api/v1/me/follows/LABEL/'+label['id'],{'enabled':True},actor='alice')
  now=datetime.datetime.now(datetime.timezone.utc)
  iso=lambda days:(now+datetime.timedelta(days=days)).isoformat()
  activity=call('POST','/api/v1/admin/activities',{'title':'浏览器活动下载','registrationStart':iso(-1),'registrationEnd':iso(1),'eventStart':iso(2),'eventEnd':iso(3),'capacity':5,'waitlistEnabled':True,'status':'PUBLISHED','fields':[],'expectedVersion':0})
  boundary='browser'+uuid.uuid4().hex
  body=(f'--{boundary}\r\nContent-Disposition: form-data; name="name"\r\n\r\n浏览器活动附件\r\n--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="acceptance.txt"\r\nContent-Type: text/plain\r\n\r\nsynthetic browser file\r\n--{boundary}--\r\n').encode()
  with urlopen(Request(r.url+'/api/v1/admin/file-library',body,{'Content-Type':'multipart/form-data; boundary='+boundary,'Authorization':'Bearer '+s.tokens['admin'],'clientid':s.client,'Idempotency-Key':uuid.uuid4().hex})) as res:file=json.load(res)['data']
  call('PUT','/api/v1/admin/activities/'+activity['id']+'/functions',{'files':[{'fileId':file['id'],'showHistory':True,'sortOrder':0}],'reviewIds':[],'expectedVersion':activity['lockVersion']})
  code=call('POST','/api/local/login-codes',{'userId':91000000000000001,'appId':'local-client'},actor=None,headers={'X-Local-Secret':r.local_secret})['code']
  fixture=r.work/'browser-private.json';private_write(fixture,{'api':r.url,'clientId':s.client,'code':code,'category':category,'label':label,'keyword':keyword,'activity':activity,'file':file})
  frontends.start();env=os.environ.copy();env['YUNJI_BROWSER_FIXTURES']=str(fixture)
  result=subprocess.run([shutil.which('node'),'browser_product_baseline.cjs'],cwd=ROOT/'tests',env=env,creationflags=FLAGS,capture_output=True,text=True,encoding='utf-8',errors='replace')
  (r.work/'product-browser.log').write_text(result.stdout+result.stderr,'utf-8')
  if result.returncode:
   browser_report=json.loads((ROOT/'reports/browser-product-baseline.json').read_text('utf-8'))
   raise RuntimeError(browser_report.get('fatal') or (result.stdout+result.stderr)[-6000:] or '浏览器产品基线验收失败')
 except Exception as e:
  fatal=str(e);print(fatal,flush=True)
 finally:
  frontends.close();r.close()
  p=ROOT/'reports/browser-product-baseline.json';report=json.loads(p.read_text('utf-8')) if p.exists() else {'cases':[]}
  report.update(sourceFingerprints=before,sourceUnchanged=before==application_fingerprints(),testSourceSha256=source_hash('tests','scripts'),generatedAt=datetime.datetime.now(datetime.timezone.utc).isoformat(),logDirectory=str(r.work))
  if fatal:report.update(fatal=fatal,failed=max(1,report.get('failed',0)))
  p.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n','utf-8')
 return bool(fatal)

if __name__=='__main__':raise SystemExit(main())
