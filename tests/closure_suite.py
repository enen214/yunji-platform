from http_suite import equal
from extended_suite import upload
import uuid,datetime,hashlib,json,subprocess,shutil

def run(s):
 # Every check is an executable behavior or a database invariant, not a paper scenario.
 now=datetime.datetime.now(datetime.timezone.utc)
 def at(seconds):return (now+datetime.timedelta(seconds=seconds)).isoformat()
 code=s.call('POST','/api/local/login-codes',{'userId':'91000000000000002','appId':'local-client'},headers={'X-Local-Secret':s.r.local_secret})['code']
 def wechat():
  result=s.call('POST','/api/v1/auth/customer/wechat',{'code':code,'appId':'local-client'});equal(result['userId'],'91000000000000002');equal(result['realm'],'CUSTOMER')
 s.check('wechat-business-local-exchange',['AUTH-02'],wechat)
 s.check('wechat-code-replay-denied',['AUTH-02'],lambda:s.call('POST','/api/v1/auth/customer/wechat',{'code':code,'appId':'local-client'},status=404))
 s.check('wechat-native-never-uses-synthetic-code',['AUTH-02'],lambda:s.call('POST','/api/v1/auth/customer/wechat/native',{'code':'synthetic-code','appId':'local-client','phoneCode':'synthetic-phone'},status=503))
 s.check('wechat-native-empty-phone-code-rejected',['AUTH-02'],lambda:s.call('POST','/api/v1/auth/customer/wechat/native',{'code':'synthetic-code','appId':'local-client','phoneCode':''},status=422))
 s.check('wechat-native-oversized-code-rejected',['AUTH-02'],lambda:s.call('POST','/api/v1/auth/customer/wechat/native',{'code':'x'*101,'appId':'local-client'},status=422))
 s.check('identity-subject-encrypted',['AUTH-02'],lambda:equal(s.r.sql("SELECT encrypted_subject LIKE 'v1:%' FROM app_user_identity WHERE provider='LOCAL_WECHAT'").stdout.strip(),'1'))
 c=s.call('POST','/api/v1/admin/catalog/categories',{'name':'合成分类','code':'SYNTHETIC_CAT','status':'ENABLED','sortOrder':1},actor='admin')
 s.check('catalog-cycle-denied',['CAT-01'],lambda:s.call('PUT','/api/v1/admin/catalog/categories/'+c['id'],{'name':'合成分类','code':'SYNTHETIC_CAT','parentId':c['id'],'status':'ENABLED','sortOrder':1,'expectedVersion':0},actor='admin',status=422))
 s.check('category-unique-code',['CAT-01'],lambda:s.call('POST','/api/v1/admin/catalog/categories',{'name':'重复编码分类','code':'SYNTHETIC_CAT','status':'ENABLED','sortOrder':1},actor='admin',status=409))
 o=s.call('POST','/api/v1/admin/catalog/organizations',{'name':'合成其他组织','code':'SYNTH_OTHER','type':'COLLEGE','parentId':'901','status':'ENABLED','sortOrder':1},actor='admin')
 s.check('organization-created',['CAT-02'],lambda:equal(any(i['id']==o['id'] for i in s.call('GET','/api/v1/catalog/organizations')),True))
 k=s.call('POST','/api/v1/admin/catalog/keywords',{'name':'合成治理词','status':'ENABLED','sortOrder':1},actor='admin')
 s.check('keyword-search',['CAT-03'],lambda:equal(s.call('GET','/api/v1/catalog/keywords?q=合成治理词')[0]['id'],k['id']))
 s.call('POST',f"/api/v1/admin/catalog/keywords/{k['id']}/aliases",{'alias':'synthetic_alias'},actor='admin')
 s.check('keyword-alias-search',['CAT-05'],lambda:equal(s.call('GET','/api/v1/catalog/keywords?q=synthetic_alias')[0]['id'],k['id']))
 s.check('keyword-alias-unique',['CAT-05'],lambda:s.call('POST',f"/api/v1/admin/catalog/keywords/{k['id']}/aliases",{'alias':'synthetic_alias'},actor='admin',status=409))
 s.call('POST','/api/v1/admin/catalog/blacklist',{'keyword':'合成治理词','reason':'合成治理测试'},actor='admin')
 s.check('blacklist-hides-keyword',['OPS-06'],lambda:equal(s.call('GET','/api/v1/catalog/keywords?q=合成治理词'),[]))
 s.check('blacklist-rejects-recreate',['OPS-06'],lambda:s.call('POST','/api/v1/admin/catalog/keywords',{'name':'合成治理词','status':'ENABLED','sortOrder':1},actor='admin',status=422))
 cat=s.call('GET','/api/v1/catalog/categories')[0]['id']
 s.check('combined-filter-no-cross-org',['CAT-04','ART-08'],lambda:equal(s.call('GET',f"/api/v1/articles?categoryId={cat}&organizationId={o['id']}")['items'],[]))
 s.check('content-search-match',['ART-08'],lambda:equal(any(a['id']==s.article_id for a in s.call('GET','/api/v1/articles?q=合成通知')['items']),True))
 s.check('literal-like-escaping',['ART-08'],lambda:equal(s.call('GET','/api/v1/articles?q=%25')['items'],[]))
 def create(title,**kw):
  b={'title':title,'summary':'合成内容','richText':'<p>合成正文</p>','contentType':'BASIC','categoryIds':[cat],'organizationIds':['902'],'keywordIds':['911'],**kw}
  a=s.call('POST','/api/v1/admin/articles',b,actor='admin')
  for action in ['SUBMIT','APPROVE','PUBLISH']:a=s.call('POST',f"/api/v1/admin/articles/{a['id']}/transitions",{'action':action,'versionId':a['latestVersionId'],'expectedVersion':a['lockVersion'],'comment':'验收合成发布'},actor='admin')
  return a
 expired=create('合成过期公告',effectiveFrom=at(-120),effectiveTo=at(-60))
 s.check('expired-not-current-feed',['ART-10'],lambda:equal(any(a['id']==expired['id'] for a in s.call('GET','/api/v1/articles')['items']),False))
 s.check('expired-archive-view',['ART-10'],lambda:equal(any(a['id']==expired['id'] for a in s.call('GET','/api/v1/articles/archive')),True))
 guide=create('合成下载教程',contentType='GUIDE')
 s.check('guide-uses-article-model',['RES-07'],lambda:equal(s.call('GET','/api/v1/articles/'+guide['id'])['article']['contentType'],'GUIDE'))
 older=create('合成旧通知');newer=create('合成替换通知')
 s.check('content-replacement',['ART-09'],lambda:s.call('POST',f"/api/v1/admin/articles/{older['id']}/replacement",{'newArticleId':newer['id']},actor='admin'))
 s.check('replacement-old-offline',['ART-09'],lambda:s.call('GET','/api/v1/articles/'+older['id'],status=404))
 s.check('replacement-self-denied',['ART-09'],lambda:s.call('POST',f"/api/v1/admin/articles/{newer['id']}/replacement",{'newArticleId':newer['id']},actor='admin',status=422))
 report=s.call('POST','/api/v1/me/reports/'+s.article_id,{'type':'OTHER','reason':'合成举报'},actor='alice')
 s.check('report-handler',['ENG-06'],lambda:s.call('POST','/api/v1/admin/moderation/reports/'+report['id'],{'status':'RESOLVED'},actor='admin'))
 s.check('report-customer-cannot-handle',['ENG-06'],lambda:s.call('POST','/api/v1/admin/moderation/reports/'+report['id'],{'status':'DISMISSED'},actor='alice',status=401))
 feedback=s.call('POST','/api/v1/me/feedback',{'description':'合成建议'},actor='alice')
 s.check('feedback-handler',['ENG-07'],lambda:s.call('POST','/api/v1/admin/moderation/feedback/'+feedback['id'],{'status':'RESOLVED'},actor='admin'))
 s.check('feedback-owned-list',['ENG-07'],lambda:equal(any(f['id']==feedback['id'] for f in s.call('GET','/api/v1/me/feedback',actor='bob')),False))
 s.check('review-project-list',['REV-01'],lambda:equal(len(s.call('GET','/api/v1/review-campaigns')),1))
 s.check('review-private-without-token',['REV-01'],lambda:s.call('GET','/api/v1/admin/review-campaigns',status=401))
 s.check('creator-cannot-manage-catalog',['ADM-02'],lambda:s.call('POST','/api/v1/admin/catalog/keywords',{'name':'非法词','status':'ENABLED','sortOrder':1},actor='test1',status=403))
 s.check('creator-cannot-read-admin-article',['ADM-02'],lambda:s.call('GET','/api/v1/admin/articles/'+guide['id'],actor='test1',status=404))
 s.check('scope-management-admin-only',['ADM-01'],lambda:s.call('GET','/api/v1/admin/access/users',actor='test1',status=403))
 s.check('framework-user-list',['ADM-03'],lambda:s.call('GET','/system/user/list',actor='admin'))
 s.check('framework-menu-list',['ADM-03'],lambda:s.call('GET','/system/menu/list',actor='admin'))
 s.check('framework-dept-list',['ADM-03'],lambda:s.call('GET','/system/dept/list',actor='admin'))
 s.check('framework-clientid-mismatch',['ADM-03'],lambda:s.call('GET','/system/user/list',actor='admin',headers={'clientid':'wrong'},status=401))
 # Failure handling and refunds, no duplicate quota consumption after retries.
 def auth(outcome):return s.call('POST','/api/local/notifications/authorization',{'provider':'WECHAT_SUBSCRIBE','appId':'local-app','callbackKey':uuid.uuid4().hex,'amount':5,'outcome':outcome},actor='alice',headers={'X-Local-Secret':s.r.local_secret})
 auth('RETRYABLE_FAILURE');r=s.call('POST','/api/v1/me/notifications/rules',{'targetType':'ARTICLE','targetId':s.article_id,'mode':'AT_TIME','channel':'WECHAT_SUBSCRIBE','timezone':'Asia/Shanghai','scheduledAt':at(120),'enabled':True,'expectedVersion':0},actor='alice');s.r.sql(f"UPDATE notification_rule SET next_run_at=UTC_TIMESTAMP()-INTERVAL 1 SECOND WHERE id={r['id']}");s.call('POST','/api/v1/admin/notifications/tick',actor='admin')
 delivery=s.r.sql(f"SELECT id FROM notification_delivery WHERE rule_id={r['id']}").stdout.strip()
 s.check('retryable-not-settled',['NOT-10'],lambda:equal(s.r.sql(f"SELECT status FROM notification_delivery WHERE id={delivery}").stdout.strip(),'RETRYABLE_FAILURE'))
 auth('SENT');s.r.sql(f"UPDATE notification_delivery SET next_attempt_at=UTC_TIMESTAMP()-INTERVAL 1 SECOND WHERE id={delivery}")
 s.check('retry-success',['NOT-10'],lambda:s.call('POST','/api/v1/admin/notifications/tick',actor='admin'))
 s.check('retry-reserves-only-once',['NOT-09'],lambda:equal(s.r.sql(f"SELECT COUNT(*) FROM notification_quota_ledger WHERE delivery_id={delivery} AND operation='RESERVE'").stdout.strip(),'1'))
 s.login_customer('close',91000000000000003)
 s.check('close-explicit-confirmation',['AUTH-06'],lambda:s.call('DELETE','/api/v1/me/account',{'confirmation':'随便'},actor='close',status=422))
 s.check('close-account',['AUTH-06'],lambda:s.call('DELETE','/api/v1/me/account',{'confirmation':'注销账号'},actor='close'))
 s.check('closed-session-invalid',['AUTH-06'],lambda:s.call('GET','/api/v1/me/profile',actor='close',status=401))
 # Backup to a private local run directory, restore into a second disposable database.
 def backup_restore():
  backup=s.r.work/'backup';backup.mkdir();dump=backup/'database.sql';files=s.r.work/'files';shutil.copytree(files,backup/'files')
  cmd=[str(s.r.mysql/'mysqldump.exe'),'--no-defaults','--host=127.0.0.1',f'--port={s.r.dbport}','--user=root','--single-transaction','--skip-comments','--hex-blob',s.r.database]
  with dump.open('wb') as out:subprocess.run(cmd,stdout=out,stderr=subprocess.PIPE,check=True,creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
  s.r.sql('CREATE DATABASE yunji_restore CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci',database=False)
  cmd=[str(s.r.mysql/'mysql.exe'),'--no-defaults','--host=127.0.0.1',f'--port={s.r.dbport}','--user=root','--default-character-set=utf8mb4','yunji_restore']
  with dump.open('rb') as inp:subprocess.run(cmd,stdin=inp,stdout=subprocess.PIPE,stderr=subprocess.PIPE,check=True,creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
  tables=s.r.sql('SHOW TABLES').stdout.splitlines();counts=[]
  for t in tables:
   pair=s.r.sql(f'SELECT (SELECT COUNT(*) FROM {s.r.database}.`{t}`),(SELECT COUNT(*) FROM yunji_restore.`{t}`)').stdout.strip().split('\t');equal(pair[0],pair[1]);counts.append({'table':t,'rows':int(pair[0])})
  filehashes={f.name:hashlib.sha256(f.read_bytes()).hexdigest() for f in files.iterdir() if f.is_file()}
  for f,sha in filehashes.items():equal(hashlib.sha256((backup/'files'/f).read_bytes()).hexdigest(),sha)
  (s.r.work/'backup-manifest.json').write_text(json.dumps({'tables':counts,'files':filehashes,'databaseSha256':hashlib.sha256(dump.read_bytes()).hexdigest(),'restoreVerified':True},indent=2),encoding='utf-8')
 s.check('backup-restores-all-tables-and-files',['ADM-05'],backup_restore)
 s.check('no-password-or-token-in-request-logs',['ADM-04'],lambda:equal('"password":' in (s.r.work/'api.log').read_text(encoding='utf-8'),False))
