"""Product baseline acceptance against disposable real MySQL/Redis/full foundation.
No provider credentials, production data, or old demo business code is used.
"""
from pathlib import Path
import sys,json,uuid,datetime,time,traceback,os
from urllib.request import Request,urlopen
from urllib.error import HTTPError
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tests'))
from local_runtime import LocalRuntime,backend_source_digest
from http_suite import Suite,equal,contains_no
from evidence import application_fingerprints,source_hash

def main():
 r=LocalRuntime();s=Suite(r);started=time.monotonic();fatal=None;before=application_fingerprints();test_digest=source_hash('tests','scripts')
 def sql(q):return r.sql(q).stdout.strip()
 def call(method,path,body=None,actor='admin',**kwargs):return s.call(method,path,body,actor,**kwargs)
 def check(name,fn):s.check(name,['PB-20261027'],fn)
 def upload(path,data=b'synthetic file',fields=None,actor='admin',status=200,key=None):
  boundary='PB'+uuid.uuid4().hex;body=b''
  for k,v in (fields or {}).items():body+=f'--{boundary}\r\nContent-Disposition: form-data; name="{k}"\r\n\r\n{v}\r\n'.encode()
  filename='input.json' if path.endswith('/import') else 'acceptance.txt'
  body+=f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{filename}"\r\nContent-Type: application/octet-stream\r\n\r\n'.encode()+data+f'\r\n--{boundary}--\r\n'.encode()
  req=Request(r.url+path,data=body,headers={'Content-Type':'multipart/form-data; boundary='+boundary,'clientid':s.client,'Authorization':'Bearer '+s.tokens[actor],'Idempotency-Key':key or uuid.uuid4().hex},method='POST')
  try:
   with urlopen(req,timeout=90) as resp:code=resp.status;result=json.load(resp)
  except HTTPError as e:code=e.code;result=json.load(e)
  equal(code,status)
  if status==200:equal(result['code'],200)
  return result.get('data')
 try:
  r.start_database();r.migrate();r.seed();check('clean-install-and-repeat-migrations',r.migrate)
  r.start_redis();r.start_api();s.login_staff('admin','admin123');s.login_staff('test1','666666');s.login_customer('alice',91000000000000001);s.login_customer('bob',91000000000000002)
  check('anonymous-admin-denied',lambda:s.call('GET','/api/v1/admin/navigation',status=401))
  check('customer-admin-denied',lambda:call('GET','/api/v1/admin/file-library',actor='alice',status=401))
  check('six-db-navigation-groups',lambda:equal(len(call('GET','/api/v1/admin/navigation')),6))
  category=call('GET','/api/v1/catalog/categories',actor=None)[0]
  label=call('POST','/api/v1/admin/catalog/labels',{'name':'验收标签','categoryId':category['id'],'status':'ENABLED','sortOrder':0})
  base={'title':'本期验收资讯','summary':'三级摘要','richText':'<p>二级正文</p><script>evil()</script>','originalText':'一级原文','contentType':'BASIC','categoryIds':[category['id']],'labelIds':[label['id']],'keywordNames':['验收关键词'],'hotDays':7,'contentWeight':3,'lifespan':'LONG','expectedVersion':0,'pushEnabled':True}
  def create(**kw):return call('POST','/api/v1/admin/articles',{**base,**kw},actor='test1')
  for field,value in [('labelIds',[]),('categoryIds',[]),('keywordNames',[]),('summary',''),('originalText',''),('richText','')]:
   check('create-reject-'+field,lambda f=field,v=value:call('POST','/api/v1/admin/articles',{**base,f:v},status=422))
  article=create();aid=article['id'];detail=call('GET','/api/v1/articles/'+aid,actor=None);vid=detail['versionId']
  check('save-publishes-immediately',lambda:equal(detail['article']['title'],base['title']))
  check('body-sanitized',lambda:contains_no(detail['richText'],'script'))
  check('detail-taxonomy-complete',lambda:equal([len(detail[x]) for x in ['categories','labels','keywords']],[1,1,1]))
  def feed(actor='alice'):return call('GET','/api/v1/articles?hotOnly=true',actor=actor)['items']
  def has(id,actor='alice'):return any(x['id']==id for x in feed(actor))
  check('hot-feed-includes-new',lambda:equal(has(aid),True))
  cold=create(title='非热点',hotDays=0,keywordNames=['非热点'])
  check('zero-hot-excluded',lambda:equal(has(cold['id']),False))
  check('zero-hot-searchable',lambda:equal(call('GET','/api/v1/articles/search?q=非热点',actor='alice')['items'][0]['id'],cold['id']))
  def event(kind,id=aid):return call('POST','/api/v1/me/events',{'eventId':uuid.uuid4().hex,'type':kind,'objectId':id,'occurredAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'payload':{'visibleRatio':0.5,'durationMs':1000} if kind=='VISIBLE_EXPOSURE' else {}},actor='alice')
  event('VISIBLE_EXPOSURE');event('VISIBLE_EXPOSURE')
  check('server-exposure-30s-dedup',lambda:equal(sql(f'SELECT visible_count FROM user_exposure_total WHERE article_id={aid}'), '1'))
  for _ in range(2):sql(f'UPDATE user_exposure_total SET last_visible_at=UTC_TIMESTAMP()-INTERVAL 31 SECOND WHERE article_id={aid}');event('VISIBLE_EXPOSURE')
  check('daily-three-cools-feed',lambda:equal(has(aid),False))
  sql(f'UPDATE user_exposure_total SET day_key=DATE(UTC_TIMESTAMP()+INTERVAL 8 HOUR)-INTERVAL 1 DAY WHERE article_id={aid}')
  check('day-reset-feed-reenters',lambda:equal(has(aid),True))
  sql(f'UPDATE user_exposure_total SET visible_count=9 WHERE article_id={aid}')
  check('lifetime-nine-implicit-read',lambda:equal(has(aid),False))
  call('PUT','/api/v1/me/follows/KEYWORD/911',{'enabled':True},actor='alice')
  check('follow-no-auto-reminder',lambda:equal(call('GET','/api/v1/me/reminders/rules',actor='alice')['total'],0))
  call('PUT','/api/v1/me/follows/ARTICLE/'+aid,{'enabled':True},actor='alice')
  check('daily-reminder-needs-binding',lambda:call('POST','/api/v1/me/reminders/daily',{'targetType':'ARTICLE','targetId':aid},actor='alice',status=409))
  call('POST','/api/local/reminders/authorization',{'receiptKey':uuid.uuid4().hex,'wecomBound':True,'amount':2,'outcome':'SUCCESS'},actor='alice',headers={'X-Local-Secret':r.local_secret})
  rule=call('POST','/api/v1/me/reminders/daily',{'targetType':'ARTICLE','targetId':aid},actor='alice')
  follow=next(f for f in call('GET','/api/v1/me/follows',actor='alice') if f['targetType']=='ARTICLE' and f['targetId']==aid)
  call('PUT','/api/v1/me/follows/ARTICLE/'+aid,{'enabled':False,'stopReminders':False},actor='alice')
  check('unfollow-can-keep-reminder',lambda:equal(call('GET','/api/v1/me/reminders/rules/'+rule['id'],actor='alice')['enabled'],True))
  call('POST','/api/v1/me/follows/undo',{'followId':follow['id'],'targetType':'ARTICLE','targetId':aid,'rules':[]},actor='alice')
  call('PUT','/api/v1/me/follows/ARTICLE/'+aid,{'enabled':False,'stopReminders':True},actor='alice')
  check('unfollow-can-pause-reminder',lambda:equal(call('GET','/api/v1/me/reminders/rules/'+rule['id'],actor='alice')['enabled'],False))
  undo={'followId':follow['id'],'targetType':'ARTICLE','targetId':aid,'rules':[{'id':rule['id'],'expectedVersion':rule['lockVersion']+1}]}
  check('undo-stale-rule-rejected',lambda:call('POST','/api/v1/me/follows/undo',{**undo,'rules':[{'id':rule['id'],'expectedVersion':999}]},actor='alice',status=409))
  check('undo-stale-follow-rollback',lambda:equal(any(f['targetType']=='ARTICLE' and f['targetId']==aid for f in call('GET','/api/v1/me/follows',actor='alice')),False))
  check('undo-owner-only',lambda:call('POST','/api/v1/me/follows/undo',undo,actor='bob',status=404))
  call('POST','/api/v1/me/follows/undo',undo,actor='alice')
  check('undo-restores-follow-and-reminder',lambda:equal((any(f['targetType']=='ARTICLE' and f['targetId']==aid for f in call('GET','/api/v1/me/follows',actor='alice')),call('GET','/api/v1/me/reminders/rules/'+rule['id'],actor='alice')['enabled']),(True,True)))
  event('PAGE_VIEW')
  check('detail-view-explicit-read',lambda:equal(sql(f'SELECT source FROM user_read_state WHERE article_id={aid}'),'EXPLICIT'))
  edited=call('PUT','/api/v1/admin/articles/'+aid,{**base,'title':'修正后的标题','expectedVersion':article['lockVersion']})
  check('edit-stable-content-id',lambda:equal(call('GET','/api/v1/articles/'+aid,actor=None)['versionId'],vid))
  check('edit-does-not-append-business-version',lambda:equal(sql(f'SELECT COUNT(*) FROM article_version WHERE article_id={aid}'),'1'))
  check('edit-does-not-reset-read',lambda:equal(has(aid),False))
  check('optimistic-conflict',lambda:call('PUT','/api/v1/admin/articles/'+aid,{**base,'expectedVersion':0},status=409))
  check('edit-does-not-republish',lambda:equal(sql(f"SELECT COUNT(*) FROM outbox_event WHERE aggregate_id={aid} AND event_type='ARTICLE_PUBLISHED'"),'1'))
  newer=create(title='重大变化新资讯')
  check('exact-taxonomy-candidate',lambda:equal(any(c['oldArticleId']==aid and c['newArticleId']==newer['id'] for c in call('GET','/api/v1/admin/articles/replacement-candidates')),True))
  call('POST','/api/v1/admin/articles/'+aid+'/replacement',{'newArticleId':newer['id']})
  check('replaced-old-still-readable',lambda:equal(call('GET','/api/v1/articles/'+aid,actor=None)['article']['lifecycleState'],'EXPIRED'))
  check('replacement-migrates-follow',lambda:equal(any(f['targetType']=='ARTICLE' and f['targetId']==newer['id'] for f in call('GET','/api/v1/me/follows',actor='alice')),True))
  boxes=call('GET','/api/v1/me/subscription-boxes',actor='alice')
  check('replacement-highest-box',lambda:equal([b['boxKind'] for b in boxes if any(i['id']==newer['id'] for i in b['items'])],['REPLACEMENT']))
  check('replacement-cycle-rejected',lambda:call('POST','/api/v1/admin/articles/'+newer['id']+'/replacement',{'newArticleId':aid},status=404))
  guide=create(title='长图攻略',contentType='GUIDE',richText='',originalText='',summary='',longImageUrl='https://example.invalid/guide.png')
  check('guide-image-only',lambda:equal('img' in call('GET','/api/v1/articles/'+guide['id'],actor=None)['richText'],True))
  check('notice-requires-dates',lambda:call('POST','/api/v1/admin/articles',{**base,'contentType':'NOTICE'},status=422))
  call('POST','/api/v1/admin/articles/'+cold['id']+'/state',{'action':'END','expectedVersion':cold['lockVersion']})
  check('ended-readable',lambda:call('GET','/api/v1/articles/'+cold['id'],actor=None))
  call('POST','/api/v1/admin/articles/'+guide['id']+'/state',{'action':'OFFLINE','expectedVersion':guide['lockVersion']})
  check('offline-not-readable',lambda:call('GET','/api/v1/articles/'+guide['id'],actor=None,status=404))
  row={'title':'JSON导入验收','contentType':'BASIC','categoryName':category['name'],'labelNames':['验收标签'],'keywordNames':['导入新词'],'levelOneContent':'一级','levelTwoContent':'<p>二级</p>','levelThreeContent':'三级','importance':3,'hotDays':7,'timeLimitType':'LONG'}
  key=uuid.uuid4().hex;payload={'content':json.dumps([row,{**row,'title':'失败行','keywordNames':['应回滚新词'],'importance':99}],ensure_ascii=False)}
  receipt=call('POST','/api/v1/admin/articles/import-text',payload,key=key)
  check('import-independent-row-transaction',lambda:equal((receipt['successCount'],receipt['failCount'],receipt['status']),(1,1,'COMPLETED')))
  check('failed-row-keyword-rollback',lambda:equal(sql("SELECT COUNT(*) FROM keyword WHERE display_name='应回滚新词'"),'0'))
  check('import-replay-same-batch',lambda:equal(call('POST','/api/v1/admin/articles/import-text',payload,key=key)['batchId'],receipt['batchId']))
  check('import-key-payload-conflict',lambda:call('POST','/api/v1/admin/articles/import-text',{'content':json.dumps([row])},key=key,status=409))
  check('import-receipt-owner',lambda:call('GET','/api/v1/admin/articles/import-batches/'+receipt['batchId'],actor='test1',status=404))
  check('import-duplicate-json-fields',lambda:call('POST','/api/v1/admin/articles/import-text',{'content':'[{"title":"a","title":"b"}]'},status=422))
  check('import-cap-501-preflight',lambda:call('POST','/api/v1/admin/articles/import-text',{'content':json.dumps([row]*501)},status=422))
  check('import-json-file-entry',lambda:equal(upload('/api/v1/admin/articles/import',json.dumps([row],ensure_ascii=False).encode())['successCount'],1))
  batch=call('POST','/api/v1/admin/articles/import-text',{'content':json.dumps([{**row,'title':'同批A'},{**row,'title':'同批B'}])})
  check('import-reused-keywords-receipt',lambda:equal(batch['duplicatedKeywords'],['导入新词']))
  check('import-no-internal-candidates',lambda:equal(sql(f"SELECT COUNT(*) FROM article_replacement_candidate c JOIN article a ON a.id=c.old_article_id JOIN article b ON b.id=c.new_article_id WHERE a.import_batch_id={batch['batchId']} AND b.import_batch_id={batch['batchId']}"),'0'))
  check('import-full-500',lambda:equal(call('POST','/api/v1/admin/articles/import-text',{'content':json.dumps([{**row,'title':f'满量验收{i}','keywordNames':['满量验收']} for i in range(500)])})['successCount'],500))
  now=datetime.datetime.now(datetime.timezone.utc)
  def iso(days):return (now+datetime.timedelta(days=days)).isoformat()
  activity=call('POST','/api/v1/admin/activities',{'title':'活动下载验收','registrationStart':iso(-1),'registrationEnd':iso(1),'eventStart':iso(2),'eventEnd':iso(3),'capacity':2,'waitlistEnabled':True,'status':'PUBLISHED','fields':[],'expectedVersion':0})
  f=upload('/api/v1/admin/file-library',fields={'name':'全局测试文件','description':'合成验收文件'})
  f=upload('/api/v1/admin/file-library/'+f['id']+'/versions',b'new synthetic version',fields={'name':f['name'],'description':f['description'],'expectedVersion':f['lockVersion'],'note':'v2'})
  check('global-file-history-retained',lambda:equal(len(f['versions']),2))
  call('PUT','/api/v1/admin/activities/'+activity['id']+'/functions',{'files':[{'fileId':f['id'],'showHistory':True,'sortOrder':0}],'reviewIds':[],'expectedVersion':activity['lockVersion']})
  path='/api/v1/activities/'+activity['id']+'/downloads/'+f['id']
  check('anonymous-download-denied',lambda:call('POST',path,actor=None,status=401))
  ticket=call('POST',path,actor='alice');call('POST',path+'?version=1',actor='alice')
  check('ticket-issue-does-not-count-download',lambda:equal(sql(f"SELECT COUNT(*) FROM activity_download WHERE file_id={f['id']}"),'0'))
  check('global-file-public-no-storage-id',lambda:contains_no(json.dumps(call('GET','/api/v1/activities/'+activity['id']+'/downloads',actor=None)),'cloud'))
  def raw_download(actor,status):
   req=Request(r.url+ticket['path'],headers={'Authorization':'Bearer '+s.tokens[actor]})
   try:
    with urlopen(req) as response:code=response.status;body=response.read()
   except HTTPError as e:code=e.code;body=e.read()
   equal(code,status)
   if status==200:equal(body,b'new synthetic version')
  check('completion-before-read-denied',lambda:call('POST','/api/v1/me/library-downloads/complete',{'ticket':ticket['ticket']},actor='alice',status=409))
  check('completion-other-user-denied',lambda:call('POST','/api/v1/me/library-downloads/complete',{'ticket':ticket['ticket']},actor='bob',status=404))
  check('download-ticket-other-user-denied',lambda:raw_download('bob',404))
  check('download-ticket-byte-content',lambda:raw_download('alice',200))
  check('server-read-alone-not-download-complete',lambda:equal(sql(f"SELECT COUNT(*) FROM activity_download WHERE file_id={f['id']}"),'0'))
  call('POST','/api/v1/me/library-downloads/complete',{'ticket':ticket['ticket']},actor='alice')
  call('POST','/api/v1/me/library-downloads/complete',{'ticket':ticket['ticket']},actor='alice')
  check('activity-file-day-download-dedup',lambda:equal(sql(f"SELECT COUNT(*) FROM activity_download WHERE file_id={f['id']}"),'1'))
  check('download-ticket-single-use',lambda:raw_download('alice',404))
  check('referenced-file-cannot-delete',lambda:call('DELETE','/api/v1/admin/file-library/'+f['id']+'?expectedVersion='+str(f['lockVersion']),status=409))
  call('PUT','/api/v1/admin/file-library/'+f['id'],{'name':f['name'],'description':f['description'],'enabled':False,'expectedVersion':f['lockVersion']})
  check('disabled-file-download-denied',lambda:call('POST',path,actor='alice',status=404))
  account=call('POST','/api/v1/admin/accounts',{'userName':'operator_local','nickName':'验收运营','password':'SyntheticOnly2026!','enabled':True,'expectedVersion':0})
  check('new-single-role-login',lambda:s.login_staff('operator_local','SyntheticOnly2026!'))
  check('new-operator-business-access',lambda:call('GET','/api/v1/admin/articles',actor='operator_local'))
  check('self-disable-denied',lambda:call('PUT','/api/v1/admin/accounts/'+account,{'userName':'operator_local','nickName':'验收运营','password':'','enabled':False,'expectedVersion':0},actor='operator_local',status=409))
  call('PUT','/api/v1/admin/accounts/'+account,{'userName':'operator_local','nickName':'验收运营','password':'','enabled':False,'expectedVersion':0})
  check('disabled-account-session-denied',lambda:call('GET','/api/v1/admin/navigation',actor='operator_local',status=401))
  check('product-statistics-query',lambda:equal(len(call('GET','/api/v1/admin/analytics/product')['activeDays']),30))
  check('single-role-customer-directory',lambda:equal(call('GET','/api/v1/admin/customers',actor='test1')['total']>=2,True))
  call('PUT','/api/v1/me/follows/LABEL/'+label['id'],{'enabled':True},actor='bob')
  check('discovery-summary-label-count',lambda:equal(call('GET','/api/v1/me/discovery-summary',actor='bob')['sentToday']>0,True))
  check('single-search-history-delete' ,lambda:(call('POST','/api/v1/me/search-history',{'query':'验收关键词'},actor='alice'),call('DELETE','/api/v1/me/search-history/item?query=验收关键词',actor='alice'),equal(call('GET','/api/v1/me/search-history',actor='alice'),[])))
  from product_boundary_suite import run as boundary_checks
  boundary_checks(s)
  from export_contracts import export
  export(r.url)
 except Exception as e:
  fatal=str(e);traceback.print_exc()
 finally:
  r.close()
  report={'scope':'LOCAL_REAL_MYSQL_REDIS_FOUNDATION','sourceDigest':backend_source_digest(),'generatedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'durationSeconds':round(time.monotonic()-started,2),'results':s.results,'fatal':fatal,'pass':sum(x['status']=='PASS' for x in s.results),'fail':sum(x['status']=='FAIL' for x in s.results),'externalNotVerified':['WECHAT_REAL','WECOM_REAL','WECHAT_CLOUD_REAL','LINUX_DOCKER','WECHAT_DEVTOOLS_DEVICE'],'runtime':str(r.work)}
  report.update(sourceFingerprints=before,sourceUnchanged=before==application_fingerprints(),testSourceSha256=test_digest,contractSha256=__import__('hashlib').sha256((ROOT/'contracts/openapi.json').read_bytes()).hexdigest())
  (ROOT/'reports/product-baseline-acceptance.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),'utf-8')
  print('RESULT',report['pass'],report['fail'],'fatal:',bool(fatal),flush=True)
  if fatal or report['fail']:sys.exit(1)
if __name__=='__main__':main()
