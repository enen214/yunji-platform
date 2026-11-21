from http_suite import equal
import uuid,json,datetime,io,zipfile
from urllib.request import Request,urlopen
from urllib.error import HTTPError
from concurrent.futures import ThreadPoolExecutor

def upload(s,actor,name,data,status=200):
 boundary=uuid.uuid4().hex
 payload=(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{name}"\r\nContent-Type: application/octet-stream\r\n\r\n').encode()+data+f'\r\n--{boundary}--\r\n'.encode()
 path='/api/v1/admin/files' if actor=='admin' else '/api/v1/me/files'
 req=Request(s.r.url+path,data=payload,headers={'Content-Type':'multipart/form-data; boundary='+boundary,'Authorization':'Bearer '+s.tokens[actor],'Idempotency-Key':uuid.uuid4().hex,'clientid':s.client})
 try:
  with urlopen(req,timeout=20) as response:code=response.status;body=json.loads(response.read())
 except HTTPError as e:code=e.code;body=json.loads(e.read())
 equal(code,status)
 if status==200:equal(body['code'],200)
 return body.get('data')

def run(s):
 aid=s.article_id
 now=datetime.datetime.now(datetime.timezone.utc)
 def at(seconds):return (now+datetime.timedelta(seconds=seconds)).isoformat()
 cfg={'title':'合成容量活动','organizationId':'902','articleId':aid,'registrationStart':at(-60),'registrationEnd':at(3600),'eventStart':at(7200),'eventEnd':at(10800),'capacity':1,'waitlistEnabled':True,'status':'PUBLISHED','fields':[{'key':'name','label':'姓名','type':'TEXT','required':True,'maxLength':30,'choices':[]}],'expectedVersion':0}
 a=s.call('POST','/api/v1/admin/activities',cfg,actor='admin');act=a['id'];form=a['form']['id']
 s.check('activity-public',['ACT-01'],lambda:equal(s.call('GET','/api/v1/activities/'+act)['title'],cfg['title']))
 s.check('activity-required-answer',['ACT-02'],lambda:s.call('POST',f'/api/v1/me/activities/{act}/registration',{'formVersionId':form,'answers':{}},actor='alice',status=422))
 def parallel():
  with ThreadPoolExecutor(2) as pool:
   result=list(pool.map(lambda user:s.call('POST',f'/api/v1/me/activities/{act}/registration',{'formVersionId':form,'answers':{'name':user}},actor=user),['alice','bob']))
  equal(sorted(x['status'] for x in result),['CONFIRMED','WAITLISTED']);return result
 holder={}
 s.check('capacity-parallel-no-oversell',['ACT-02','ACT-03'],lambda:holder.update(results=parallel()))
 regs=s.call('GET','/api/v1/admin/activities/'+act,actor='admin')['registrations']
 confirmed=next(x for x in regs if x['status']=='CONFIRMED');waiting=next(x for x in regs if x['status']=='WAITLISTED')
 first='alice' if confirmed['userId']=='91000000000000001' else 'bob';second='bob' if first=='alice' else 'alice'
 s.check('duplicate-registration-no-capacity-loss',['ACT-03'],lambda:equal(s.call('POST',f'/api/v1/me/activities/{act}/registration',{'formVersionId':form,'answers':{'name':first}},actor=first)['id'],confirmed['id']))
 s.check('cancel-promotes-oldest',['ACT-04'],lambda:s.call('DELETE',f'/api/v1/me/activities/{act}/registration',actor=first))
 s.check('promoted-state',['ACT-03','ACT-04'],lambda:equal(s.call('GET','/api/v1/me/registrations',actor=second)[0]['status'],'CONFIRMED'))
 s.check('capacity-ledger-consistent',['ACT-03'],lambda:equal(s.r.sql(f'SELECT confirmed_count FROM activity WHERE id={act}').stdout.strip(),'1'))
 s.check('activity-admin-read',['ACT-05'],lambda:equal(len(s.call('GET','/api/v1/admin/activities/'+act,actor='admin')['registrations']),2))
 file=upload(s,'alice','material.txt','合成私有材料'.encode());fid=file['id']
 s.check('file-magic-reject',['OPS-05'],lambda:upload(s,'alice','pretend.pdf',b'not a PDF',status=422))
 s.check('private-file-cross-user',['REV-04','RES-05'],lambda:s.call('POST',f'/api/v1/me/files/{fid}/tickets',actor='bob',status=404))
 ticket=s.call('POST',f'/api/v1/me/files/{fid}/tickets',actor='alice')['ticket']
 def consume():
  req=Request(s.r.url+'/api/v1/files/download/'+ticket,headers={'Authorization':'Bearer '+s.tokens['alice']})
  equal(urlopen(req).read(),'合成私有材料'.encode())
 s.check('private-ticket-download',['RES-05'],consume)
 s.check('download-is-not-client-proof',['ANA-04'],lambda:equal(s.r.sql("SELECT COUNT(*) FROM download_event WHERE event_type='SERVER_READ'").stdout.strip(),'1'))
 def reused():
  req=Request(s.r.url+'/api/v1/files/download/'+ticket,headers={'Authorization':'Bearer '+s.tokens['alice']})
  try:urlopen(req);raise AssertionError('ticket reused')
  except HTTPError as e:equal(e.code,404)
 s.check('ticket-single-use',['RES-05'],reused)
 campaign_config={'title':'合成评审','organizationId':'902','articleId':aid,'openAt':at(-60),'closeAt':at(3600),'status':'PUBLISHED','requirements':[{'key':'material','label':'证明材料','required':True,'mediaTypes':['text/plain','application/pdf'],'maxSize':10000,'organizationId':'902'}]}
 campaign=s.call('POST','/api/v1/admin/review-campaigns',campaign_config,actor='admin');cid=campaign['id']
 s.check('review-campaign-admin-detail',['REV-01'],lambda:equal(s.call('GET',f'/api/v1/admin/review-campaigns/{cid}',actor='admin')['title'],'合成评审'))
 campaign=s.call('PUT',f'/api/v1/admin/review-campaigns/{cid}',{**campaign_config,'title':'合成评审更新','expectedVersion':campaign['lockVersion']},actor='admin')
 campaign_config['title']='合成评审更新'
 s.check('review-campaign-update-stale-denied',['REV-01'],lambda:s.call('PUT',f'/api/v1/admin/review-campaigns/{cid}',{**campaign_config,'expectedVersion':0},actor='admin',status=409))
 s.check('review-eligibility',['REV-02'],lambda:equal(s.call('GET',f'/api/v1/me/review-campaigns/{cid}/eligibility',actor='alice')['eligible'],True))
 s.check('review-eligibility-negative',['REV-02'],lambda:equal(s.call('GET',f'/api/v1/me/review-campaigns/{cid}/eligibility',actor='bob')['eligible'],False))
 app=s.call('POST',f'/api/v1/me/review-campaigns/{cid}/draft',{'files':{},'expectedVersion':0},actor='alice');appid=app['id']
 s.check('review-campaign-rules-frozen-after-application',['REV-01','REV-04'],lambda:s.call('PUT',f'/api/v1/admin/review-campaigns/{cid}',{**campaign_config,'requirements':[{**campaign_config['requirements'][0],'required':False}],'expectedVersion':campaign['lockVersion']},actor='admin',status=409))
 reordered={**campaign_config,'requirements':[{**campaign_config['requirements'][0],'mediaTypes':['application/pdf','text/plain']}],'expectedVersion':campaign['lockVersion']}
 s.check('review-campaign-rule-order-is-semantic-noop',['REV-01','REV-04'],lambda:equal(s.call('PUT',f'/api/v1/admin/review-campaigns/{cid}',reordered,actor='admin')['ruleVersion'],campaign['ruleVersion']))
 s.check('review-required-material',['REV-05'],lambda:s.call('POST',f'/api/v1/me/review-applications/{appid}/actions',{'action':'SUBMIT','submissionId':app['latestSubmissionId'],'expectedVersion':app['lockVersion']},actor='alice',status=422))
 app=s.call('POST',f'/api/v1/me/review-campaigns/{cid}/draft',{'files':{'material':fid},'expectedVersion':app['lockVersion']},actor='alice');sub=app['latestSubmissionId']
 app=s.call('POST',f'/api/v1/me/review-applications/{appid}/actions',{'action':'SUBMIT','submissionId':sub,'expectedVersion':app['lockVersion']},actor='alice')
 s.check('submitted-material-frozen',['REV-04','REV-05'],lambda:s.call('POST',f'/api/v1/me/review-campaigns/{cid}/draft',{'files':{'material':fid},'expectedVersion':app['lockVersion']},actor='alice',status=409))
 s.check('application-cross-user',['REV-03'],lambda:s.call('GET',f'/api/v1/me/review-applications/{appid}',actor='bob',status=404))
 app=s.call('POST',f'/api/v1/admin/review-applications/{appid}/decisions',{'action':'CORRECTION','submissionId':sub,'expectedVersion':app['lockVersion'],'comment':'需要补正'},actor='admin')
 app=s.call('POST',f'/api/v1/me/review-campaigns/{cid}/draft',{'files':{'material':fid},'expectedVersion':app['lockVersion']},actor='alice');sub2=app['latestSubmissionId']
 app=s.call('POST',f'/api/v1/me/review-applications/{appid}/actions',{'action':'SUBMIT','submissionId':sub2,'expectedVersion':app['lockVersion']},actor='alice')
 s.check('stale-review-version-denied',['REV-06'],lambda:s.call('POST',f'/api/v1/admin/review-applications/{appid}/decisions',{'action':'APPROVED','submissionId':sub,'expectedVersion':app['lockVersion'],'comment':'旧版本不能审批'},actor='admin',status=409))
 s.check('review-current-decision',['REV-06'],lambda:s.call('POST',f'/api/v1/admin/review-applications/{appid}/decisions',{'action':'APPROVED','submissionId':sub2,'expectedVersion':app['lockVersion'],'comment':'已核实合成材料'},actor='admin'))
 sf=upload(s,'admin','resource.txt',b'synthetic resource');subj=s.call('POST','/api/v1/admin/resources/subjects',{'organizationId':'902','code':'SYNTH','name':'合成科目'},actor='admin')
 pack=s.call('POST','/api/v1/admin/resources/packages',{'organizationId':'902','subjectId':subj['id'],'title':'合成资料包','type':'REVISION','status':'PUBLISHED','fileIds':[sf['id']]},actor='admin')
 s.check('resource-published-files',['RES-01','RES-04'],lambda:equal(s.call('GET','/api/v1/resources/packages/'+pack['id'])['files'][0]['fileId'],sf['id']))
 s.check('resource-policy-download',['RES-05'],lambda:s.call('POST',f"/api/v1/me/files/{sf['id']}/tickets",actor='bob'))
 s.check('referenced-file-delete-denied',['OPS-05'],lambda:s.call('DELETE',f"/api/v1/admin/files/{sf['id']}",actor='admin',status=409))
 orphan=upload(s,'admin','orphan-cleanup.txt',b'orphan cleanup');s.call('DELETE',f"/api/v1/admin/files/{orphan['id']}",actor='admin')
 s.check('file-cleanup-retention-window',['OPS-05'],lambda:equal(s.call('POST','/api/v1/admin/files/cleanup?dryRun=false&limit=10',actor='admin')['candidates'],0))
 s.r.sql(f"UPDATE file_object SET deleted_at=UTC_TIMESTAMP()-INTERVAL 8 DAY WHERE id={orphan['id']}")
 s.check('file-cleanup-dry-run',['OPS-05'],lambda:equal(s.call('POST','/api/v1/admin/files/cleanup?dryRun=true&limit=10',actor='admin')['candidates'],1))
 s.check('file-cleanup-physical-delete',['OPS-05'],lambda:equal(s.call('POST','/api/v1/admin/files/cleanup?dryRun=false&limit=10',actor='admin')['removed'],1))
 s.check('file-cleanup-staff-permission',['OPS-05','ADM-02'],lambda:s.call('POST','/api/v1/admin/files/cleanup?dryRun=true',actor='test1',status=403))
 s.check('file-cleanup-repeat-safe',['OPS-05'],lambda:equal(s.call('POST','/api/v1/admin/files/cleanup?dryRun=false&limit=10',actor='admin')['candidates'],0))
 s.check('study-dimension',['RES-02'],lambda:s.call('POST','/api/v1/admin/resources/dimensions',{'kind':'GRADE','code':'2026','name':'2026级'},actor='admin'))
 s.check('exam-timezone',['RES-03'],lambda:s.call('POST','/api/v1/admin/resources/exams',{'organizationId':'902','subjectId':subj['id'],'startsAt':at(86400),'displayTimezone':'Asia/Shanghai','sourceReference':'合成日程'},actor='admin'))
 s.check('exam-invalid-timezone',['RES-03'],lambda:s.call('POST','/api/v1/admin/resources/exams',{'organizationId':'902','subjectId':subj['id'],'startsAt':at(86400),'displayTimezone':'Invalid/Zone','sourceReference':'合成日程'},actor='admin',status=422))
 data=io.BytesIO()
 with zipfile.ZipFile(data,'w') as z:z.writestr('readme.txt','synthetic package')
 af=upload(s,'admin','artifact.zip',data.getvalue());artifact=s.call('POST','/api/v1/admin/resources/artifacts',{'name':'合成插件','platform':'WINDOWS','status':'PUBLISHED'},actor='admin')
 s.check('artifact-frozen-checksum',['RES-06'],lambda:equal(s.call('POST',f"/api/v1/admin/resources/artifacts/{artifact['id']}/versions",{'versionName':'1.0.0','fileId':af['id'],'releaseNotes':'合成版本'},actor='admin')['versions'][0]['sha256'],af['sha256']))
 s.check('artifact-duplicate-version',['RES-06'],lambda:s.call('POST',f"/api/v1/admin/resources/artifacts/{artifact['id']}/versions",{'versionName':'1.0.0','fileId':af['id'],'releaseNotes':'重复'},actor='admin',status=409))
