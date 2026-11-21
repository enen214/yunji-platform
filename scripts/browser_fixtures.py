"""Synthetic prerequisites for real UI tests; tested UI writes are not performed here."""
import datetime
import json
import sys
import uuid
from local_runtime import ROOT
from workbench import private_write

sys.path.insert(0,str(ROOT/'tests'))
from http_suite import Suite
from extended_suite import upload

def renew_code(runtime,path):
    s=Suite(runtime)
    values=json.loads(path.read_text(encoding='utf-8'))
    values['code']=s.call('POST','/api/local/login-codes',{'userId':'91000000000000001','appId':'local-client'},headers={'X-Local-Secret':runtime.local_secret})['code']
    private_write(path,values)


def create(runtime):
    s=Suite(runtime);s.login_staff('admin','admin123');s.login_customer('alice',91000000000000001)
    cat=s.call('GET','/api/v1/catalog/categories')[0]['id']
    draft={'title':'页面验收合成资讯','summary':'页面验收摘要','richText':'<p>页面验收正文</p>','contentType':'BASIC','sourceName':'合成来源','categoryIds':[cat],'organizationIds':['902'],'keywordIds':['911'],'hotWeight':1}
    article=s.call('POST','/api/v1/admin/articles',draft,actor='admin')
    for action in ['SUBMIT','APPROVE','PUBLISH']:
        article=s.call('POST',f"/api/v1/admin/articles/{article['id']}/transitions",{'action':action,'versionId':article['latestVersionId'],'expectedVersion':article['lockVersion']},actor='admin')
    now=datetime.datetime.now(datetime.timezone.utc)
    def at(seconds):return (now+datetime.timedelta(seconds=seconds)).isoformat()
    activity=s.call('POST','/api/v1/admin/activities',{'title':'页面验收合成活动','organizationId':'902','articleId':article['id'],'registrationStart':at(-60),'registrationEnd':at(3600),'eventStart':at(7200),'eventEnd':at(10800),'capacity':2,'waitlistEnabled':True,'status':'PUBLISHED','fields':[{'key':'name','label':'参与人姓名','type':'TEXT','required':True,'maxLength':100,'choices':[]}]},actor='admin')
    campaign=s.call('POST','/api/v1/admin/review-campaigns',{'title':'页面验收合成评审','organizationId':'902','articleId':article['id'],'openAt':at(-60),'closeAt':at(3600),'status':'PUBLISHED','requirements':[{'key':'proof','label':'证明材料','required':True,'mediaTypes':['text/plain'],'maxSize':100000}]},actor='admin')
    file=upload(s,'admin','browser-resource.txt',b'new synthetic browser resource')
    subject=s.call('POST','/api/v1/admin/resources/subjects',{'organizationId':'902','code':'BROWSER','name':'页面验收合成科目'},actor='admin')
    package=s.call('POST','/api/v1/admin/resources/packages',{'organizationId':'902','subjectId':subject['id'],'title':'页面验收合成资料包','type':'GENERAL','status':'PUBLISHED','fileIds':[file['id']]},actor='admin')
    s.call('POST','/api/v1/admin/operation/pages',{'pageKey':'terms','title':'合成验收使用协议','content':'合成验收协议正文，不是实际法律文本'},actor='admin')
    s.call('POST','/api/local/notifications/authorization',{'provider':'WECHAT_SUBSCRIBE','appId':'local-app','callbackKey':uuid.uuid4().hex,'amount':5,'outcome':'SENT'},actor='alice',headers={'X-Local-Secret':runtime.local_secret})
    s.call('POST','/api/local/reminders/authorization',{'receiptKey':uuid.uuid4().hex,'wecomBound':True,'amount':5,'outcome':'SUCCESS'},actor='alice',headers={'X-Local-Secret':runtime.local_secret})
    code=s.call('POST','/api/local/login-codes',{'userId':'91000000000000001','appId':'local-client'},headers={'X-Local-Secret':runtime.local_secret})['code']
    path=runtime.work/'browser-fixtures-private.json'
    private_write(path,{'code':code,'articleId':article['id'],'activityId':activity['id'],'campaignId':campaign['id'],'packageId':package['id'],'fileId':file['id']})
    return path
