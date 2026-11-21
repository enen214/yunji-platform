"""Executable integration checks. HTTP calls target the full company foundation, never the old starter."""
import json, uuid, time, hashlib
from urllib.request import Request,urlopen
from urllib.error import HTTPError
from urllib.parse import quote
from concurrent.futures import ThreadPoolExecutor

class Suite:
 def __init__(self,runtime):
  self.r=runtime;self.results=[];self.tokens={};self.client='e5cd7e4891bf95d1d19206ce24a7b32e'
 def call(self,method,path,body=None,actor=None,status=200,key=None,headers=None):
  h={'Content-Type':'application/json','clientid':self.client}
  if actor:h['Authorization']='Bearer '+self.tokens.get(actor,actor)
  if method not in ['GET','HEAD']:h['Idempotency-Key']=key or uuid.uuid4().hex
  h.update(headers or {})
  req=Request(self.r.url+quote(path,safe='/?:=&%+'),data=None if body is None else json.dumps(body,ensure_ascii=False).encode('utf-8'),headers=h,method=method)
  try:
   with urlopen(req,timeout=20) as resp:code=resp.status;raw=resp.read()
  except HTTPError as e:code=e.code;raw=e.read()
  data=json.loads(raw) if raw else {}
  assert code==status,(method,path,'HTTP',code,'expected',status,data)
  if status==200:assert data.get('code')==200,(method,path,data)
  return data.get('data')
 def check(self,name,features,fn):
  started=time.monotonic()
  try:fn();result={'id':name,'featureIds':features,'status':'PASS'}
  except Exception as e:result={'id':name,'featureIds':features,'status':'FAIL','error':str(e)}
  result['durationMs']=round((time.monotonic()-started)*1000);self.results.append(result);print(name,result['status'],result.get('error',''),flush=True)
 def login_staff(self,name,password):
  token=self.call('POST','/auth/login',{'clientId':self.client,'grantType':'password','tenantId':'000000','username':name,'password':password})['access_token']
  self.tokens[name]=token
 def login_customer(self,label,user):
  code=self.call('POST','/api/local/login-codes',{'userId':user,'appId':'local-client'},headers={'X-Local-Secret':self.r.local_secret})['code']
  self.tokens[label]=self.call('POST','/api/v1/auth/customer/local',{'code':code,'appId':'local-client'})['accessToken']
  return code
 def run(self):
  self.check('framework-password-login',['AUTH-01'],lambda:self.login_staff('admin','admin123'))
  self.check('creator-password-login',['AUTH-01','ADM-02'],lambda:self.login_staff('test1','666666'))
  self.check('reviewer-password-login',['AUTH-01','ADM-01'],lambda:self.login_staff('test','666666'))
  code=self.login_customer('alice',91000000000000001);self.login_customer('bob',91000000000000002)
  self.check('customer-single-use-code',['AUTH-03'],lambda:self.call('POST','/api/v1/auth/customer/local',{'code':code,'appId':'local-client'},status=404))
  self.check('local-code-secret',['AUTH-03'],lambda:self.call('POST','/api/local/login-codes',{'userId':91000000000000001,'appId':'local-client'},status=404))
  self.check('anonymous-protected',['AUTH-04'],lambda:self.call('GET','/api/v1/me/profile',status=401))
  self.check('staff-cannot-use-customer-token',['ADM-01'],lambda:self.call('GET','/api/v1/admin/articles',actor='alice',status=401))
  self.check('customer-profile',['AUTH-04'],lambda:self.call('PUT','/api/v1/me/profile',{'nickname':'合成学生甲','organizationId':'902'},actor='alice'))
  self.check('profile-invalid-org',['AUTH-04'],lambda:self.call('PUT','/api/v1/me/profile',{'nickname':'学生','organizationId':'999999'},actor='alice',status=404))
  draft={'title':'合成通知V1','summary':'合成摘要','richText':'<p>正文</p><script>alert(1)</script>','sourceName':'合成来源','sourceUrl':'https://example.invalid/source','contentType':'BASIC','categoryIds':[101],'organizationIds':[902],'keywordIds':[911],'expectedVersion':0}
  # Determine category ID from fresh migration instead of assuming demo IDs.
  draft['categoryIds']=[self.call('GET','/api/v1/catalog/categories')[0]['id']]
  categories=self.call('GET','/api/v1/catalog/categories')
  self.check('article-requires-one-category',['CAT-01','ART-01'],lambda:self.call('POST','/api/v1/admin/articles',{**draft,'categoryIds':[]},actor='test1',status=422))
  self.check('article-rejects-multiple-categories',['CAT-01','ART-01'],lambda:self.call('POST','/api/v1/admin/articles',{**draft,'categoryIds':[categories[0]['id'],categories[1]['id']]},actor='test1',status=422))
  key=uuid.uuid4().hex
  article=self.call('POST','/api/v1/admin/articles',draft,actor='test1',key=key);aid=article['id'];v1=article['latestVersionId']
  self.check('article-create-dedup',['ART-01'],lambda:equal(self.call('POST','/api/v1/admin/articles',draft,actor='test1',key=key)['id'],aid))
  self.check('idempotency-body-conflict',['ART-01'],lambda:self.call('POST','/api/v1/admin/articles',{**draft,'title':'不同内容'},actor='test1',key=key,status=409))
  self.check('draft-not-public',['ART-06','ART-07'],lambda:self.call('GET','/api/v1/articles/'+aid,status=404))
  self.check('creator-cannot-publish',['ADM-02','ART-05'],lambda:self.call('POST',f'/api/v1/admin/articles/{aid}/transitions',{'action':'PUBLISH','versionId':v1,'expectedVersion':article['lockVersion']},actor='test1',status=403))
  self.check('optimistic-conflict',['ART-02'],lambda:self.call('POST',f'/api/v1/admin/articles/{aid}/versions',{**draft,'expectedVersion':0},actor='test1',status=409))
  def transition(action,actor,version=v1):
   nonlocal article
   article=self.call('POST',f'/api/v1/admin/articles/{aid}/transitions',{'action':action,'versionId':version,'expectedVersion':article['lockVersion'],'comment':'合成意见'},actor=actor)
  self.check('submit-version',['ART-03'],lambda:transition('SUBMIT','test1'))
  self.check('approve-exact-version',['ART-04'],lambda:transition('APPROVE','test'))
  self.check('publish-approved',['ART-05'],lambda:transition('PUBLISH','test'))
  self.check('public-source',['ART-07'],lambda:equal(self.call('GET','/api/v1/articles/'+aid)['sourceUrl'],draft['sourceUrl']))
  self.check('published-html-sanitized',['ART-07'],lambda:contains_no(self.call('GET','/api/v1/articles/'+aid)['richText'],'script'))
  self.check('keyword-follow',['SUB-01'],lambda:self.call('PUT','/api/v1/me/follows/KEYWORD/911',{'enabled':True},actor='alice'))
  self.check('article-follow',['SUB-02'],lambda:self.call('PUT','/api/v1/me/follows/ARTICLE/'+aid,{'enabled':True},actor='alice'))
  self.check('subscription-boxes',['SUB-03','SUB-04','SUB-06'],lambda:equal(len(self.call('GET','/api/v1/me/subscription-boxes',actor='alice')),2))
  follow=self.call('GET','/api/v1/me/follows',actor='alice')[0]['id']
  self.check('cross-user-box-denied',['SUB-05'],lambda:self.call('PUT',f'/api/v1/me/subscription-boxes/{follow}/hidden',{'hidden':True},actor='bob',status=404))
  self.check('hide-keeps-follow',['SUB-05'],lambda:self.call('PUT',f'/api/v1/me/subscription-boxes/{follow}/hidden',{'hidden':True},actor='alice'))
  self.check('favorite',['ENG-01','ENG-02'],lambda:self.call('PUT','/api/v1/me/favorites/'+aid,{'enabled':True},actor='alice'))
  self.check('browse',['ENG-03'],lambda:self.call('POST','/api/v1/me/history/'+aid,actor='alice'))
  self.check('read-exact-version',['ENG-04'],lambda:self.call('PUT','/api/v1/me/read/'+aid,{'versionId':v1},actor='alice'))
  article=self.call('POST',f'/api/v1/admin/articles/{aid}/versions',{**draft,'title':'合成通知V2','expectedVersion':article['lockVersion']},actor='test1');v2=article['latestVersionId']
  self.check('revision-keeps-public-v1',['ART-02'],lambda:equal(self.call('GET','/api/v1/articles/'+aid)['article']['title'],'合成通知V1'))
  self.check('revision-submit',['ART-03'],lambda:transition('SUBMIT','test1',v2))
  self.check('revision-keeps-public-during-review',['ART-02'],lambda:equal(self.call('GET','/api/v1/articles/'+aid)['article']['title'],'合成通知V1'))
  self.check('revision-approve',['ART-04'],lambda:transition('APPROVE','test',v2))
  self.check('revision-publish',['ART-05'],lambda:transition('PUBLISH','test',v2))
  self.check('read-old-version-rejected',['ENG-04'],lambda:self.call('PUT','/api/v1/me/read/'+aid,{'versionId':v1},actor='alice',status=409))
  self.check('revision-returns-unread',['SUB-06'],lambda:equal(self.call('GET','/api/v1/me/subscription-boxes',actor='alice')[0]['unreadCount'],1))
  self.article_id=aid;self.version_id=v2
  self.check('article-publish-outbox',['ART-05'],lambda:equal(self.r.sql(f"SELECT COUNT(*) FROM outbox_event WHERE aggregate_id={aid} AND event_type='ARTICLE_PUBLISHED'").stdout.strip(),'2'))
  self.check('article-immutable-revisions',['ART-02'],lambda:equal(self.r.sql(f"SELECT COUNT(*) FROM article_version WHERE article_id={aid}").stdout.strip(),'2'))
  self.check('migrations-rerun',['ADM-05'],lambda:self.r.migrate())
  return self.results

def equal(a,b):assert a==b,(a,b)
def contains_no(value,text):assert text not in value,value
