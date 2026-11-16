"""Explicit branch acceptance: withdrawal/rejection, paused rules and logout invalidation."""
from http_suite import equal
import datetime


def run(s):
    cat=s.call('GET','/api/v1/catalog/categories')[0]['id']
    for kind,feature in [('categories','CAT-01'),('organizations','CAT-02'),('keywords','CAT-03')]:
        body={'name':'子操作验收-'+kind,'code':'branch-'+kind,'type':'SCHOOL','status':'ENABLED','parentId':None,'sortOrder':0,'expectedVersion':0}
        entry=s.call('POST','/api/v1/admin/catalog/'+kind,body,actor='admin')
        path='/api/v1/admin/catalog/'+kind+'/'+entry['id']
        s.check(kind+'-edit-current-version',[feature],lambda:s.call('PUT',path,{**body,'name':body['name']+'更新','expectedVersion':entry['lockVersion']},actor='admin'))
        s.check(kind+'-stale-edit-denied',[feature],lambda:s.call('PUT',path,body,actor='admin',status=409))
        s.check(kind+'-soft-delete',[feature],lambda:s.call('DELETE',path,actor='admin'))
        def hidden():
            assert not any(e['id']==entry['id'] for e in s.call('GET','/api/v1/catalog/'+kind))
        s.check(kind+'-deleted-hidden-public',[feature],hidden)
    a=s.call('POST','/api/v1/admin/articles',{'title':'独立状态分支验收','richText':'<p>synthetic branch fixture</p>','contentType':'BASIC','categoryIds':[cat]},actor='admin')
    def transition(action,comment='合成状态操作',status=200,expected=None):
        nonlocal a
        result=s.call('POST',f"/api/v1/admin/articles/{a['id']}/transitions",{'action':action,'versionId':a['latestVersionId'],'expectedVersion':a['lockVersion'] if expected is None else expected,'comment':comment},actor='admin',status=status)
        if status==200:a=result
    s.check('publish-unapproved-state-denied',['ART-05'],lambda:transition('PUBLISH',status=409))
    transition('SUBMIT')
    s.check('article-withdraw-submitted',['ART-03'],lambda:transition('WITHDRAW'))
    s.check('article-withdraw-clears-review-pointer',['ART-03'],lambda:equal(a['submittedVersionId'],None))
    transition('SUBMIT')
    s.check('article-reject-requires-reason',['ART-04'],lambda:transition('REJECT',comment='',status=422))
    s.check('article-reject-submitted',['ART-04'],lambda:transition('REJECT'))
    s.check('article-rejected-version-cannot-resubmit',['ART-03','ART-04'],lambda:transition('SUBMIT',status=409))
    a=s.call('POST',f"/api/v1/admin/articles/{a['id']}/versions",{'title':'退回后新修订','richText':'<p>new immutable revision</p>','contentType':'BASIC','categoryIds':[cat],'expectedVersion':a['lockVersion']},actor='admin')
    for action in ['SUBMIT','APPROVE','PUBLISH']:transition(action)
    s.check('article-offline-published',['ART-05'],lambda:transition('OFFLINE'))
    s.check('article-offline-body-inaccessible',['ART-05','ART-07'],lambda:s.call('GET','/api/v1/articles/'+a['id'],status=404))
    s.check('article-stale-offline-conflict',['ART-05'],lambda:transition('OFFLINE',expected=0,status=409))
    target=s.call('GET','/api/v1/articles?pageSize=20')['items'][0]['id']
    body={'targetType':'ARTICLE','targetId':target,'mode':'INSTANT','channel':'WECHAT_SUBSCRIBE','timezone':'Asia/Shanghai','enabled':True,'expectedVersion':0}
    rule=s.call('POST','/api/v1/me/notifications/rules',body,actor='alice')
    def update(enabled,version=None,status=200):
        nonlocal rule
        result=s.call('PUT','/api/v1/me/notifications/rules/'+rule['id'],{**body,'enabled':enabled,'quietStart':'22:00:00','quietEnd':'07:00:00','expectedVersion':rule['lockVersion'] if version is None else version},actor='alice',status=status)
        if status==200:rule=result
    s.check('rule-pause-keeps-settings',['NOT-01','NOT-05'],lambda:update(False))
    s.check('rule-pause-persists-and-is-boolean',['NOT-01','NOT-05'],lambda:equal((rule['enabled'],datetime.time.fromisoformat(rule['quietStart'])),(False,datetime.time(22,0))))
    s.check('rule-stale-edit-denied',['NOT-01'],lambda:update(True,version=0,status=409))
    s.check('rule-resume-current-version',['NOT-01','NOT-05'],lambda:update(True))
    s.check('rule-edit-other-customer-denied',['NOT-01'],lambda:s.call('PUT','/api/v1/me/notifications/rules/'+rule['id'],body,actor='bob',status=404))
    s.check('rule-invalid-quiet-window-denied',['NOT-01'],lambda:s.call('PUT','/api/v1/me/notifications/rules/'+rule['id'],{**body,'expectedVersion':rule['lockVersion'],'quietStart':'22:00:00','quietEnd':None},actor='alice',status=422))
    s.login_customer('logoutCustomer',91000000000000002)
    s.check('customer-logout',['AUTH-02','AUTH-03'],lambda:s.call('POST','/api/v1/auth/customer/logout',actor='logoutCustomer'))
    s.check('customer-auth-durable-audit',['AUTH-02','AUTH-03','ADM-04'],lambda:equal(s.r.sql("SELECT COUNT(DISTINCT action)=2 FROM audit_event WHERE actor_id=91000000000000002 AND action IN('CUSTOMER_LOGIN','CUSTOMER_LOGOUT')").stdout.strip(),'1'))
    s.check('customer-logout-session-invalid',['AUTH-02','AUTH-03'],lambda:s.call('GET','/api/v1/me/profile',actor='logoutCustomer',status=401))
    # Own a distinct staff session to avoid invalidating the main scenario's credentials.
    token=s.call('POST','/auth/login',{'clientId':s.client,'grantType':'password','tenantId':'000000','username':'test1','password':'666666'})['access_token']
    s.tokens['logoutStaff']=token
    s.check('staff-logout',['AUTH-01'],lambda:s.call('POST','/auth/logout',actor='logoutStaff'))
    s.check('staff-logout-session-invalid',['AUTH-01'],lambda:s.call('GET','/api/v1/admin/session',actor='logoutStaff',status=401))
