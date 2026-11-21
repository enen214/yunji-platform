"""Replacement graph, exact reads, live grants and durable rejected-write evidence."""
from http_suite import equal
import uuid,datetime

def run(s):
 cat=s.call('GET','/api/v1/catalog/categories')[0]['id']
 def article(title):
  a=s.call('POST','/api/v1/admin/articles',{'title':title,'richText':'<p>private old body marker</p>','contentType':'BASIC','categoryIds':[cat],'organizationIds':['902'],'keywordIds':['911']},actor='admin')
  for action in ['SUBMIT','APPROVE','PUBLISH']:
   a=s.call('POST',f"/api/v1/admin/articles/{a['id']}/transitions",{'action':action,'versionId':a['latestVersionId'],'expectedVersion':a['lockVersion']},actor='admin')
  return a
 a,b,c=[article('closure replacement '+x) for x in ['A','B','C']]
 def replace(source,target):return s.call('POST',f"/api/v1/admin/articles/{source['id']}/replacement",{'newArticleId':target['id']},actor='admin')
 s.call('PUT','/api/v1/me/follows/ARTICLE/'+a['id'],{'enabled':True},actor='alice')
 s.call('POST','/api/v1/me/history/'+a['id'],actor='alice')
 s.check('replacement-public-none',['ART-09'],lambda:equal(s.call('GET',f"/api/v1/articles/{a['id']}/replacement")['status'],'NONE'))
 replace(a,b);replace(b,c)
 s.check('replacement-chain-final-target',['ART-09'],lambda:equal(s.call('GET',f"/api/v1/articles/{a['id']}/replacement")['target']['id'],c['id']))
 s.check('replacement-withdrawn-body-denied',['ART-09'],lambda:s.call('GET',f"/api/v1/articles/{a['id']}",status=404))
 s.check('replacement-follow-auto-migrated',['ART-09','SUB-02'],lambda:equal(s.r.sql(f"SELECT COUNT(*) FROM user_follow WHERE user_id=91000000000000001 AND target_type='ARTICLE' AND target_id={c['id']} AND deleted_at IS NULL").stdout.strip(),'1'))
 s.check('replacement-stale-confirmation',['ART-09'],lambda:s.call('POST',f"/api/v1/me/follows/articles/{a['id']}/replacement",{'targetId':b['id']},actor='alice',status=409))
 s.check('replacement-other-customer-denied',['ART-09','SUB-02'],lambda:s.call('POST',f"/api/v1/me/follows/articles/{a['id']}/replacement",{'targetId':c['id']},actor='bob',status=404))
 key=uuid.uuid4().hex
 s.check('replacement-explicit-confirmation',['ART-09','SUB-02'],lambda:s.call('POST',f"/api/v1/me/follows/articles/{a['id']}/replacement",{'targetId':c['id']},actor='alice',key=key))
 s.check('replacement-confirmation-replay',['ART-09'],lambda:s.call('POST',f"/api/v1/me/follows/articles/{a['id']}/replacement",{'targetId':c['id']},actor='alice',key=key))
 s.check('replacement-history-preserved',['ART-09','ENG-03'],lambda:equal(s.r.sql(f"SELECT COUNT(*) FROM user_browse_history WHERE article_id={a['id']} AND user_id=91000000000000001 AND deleted_at IS NULL").stdout.strip(),'1'))
 s.check('replacement-original-follow-ended',['ART-09','SUB-02'],lambda:equal(s.r.sql(f"SELECT COUNT(*) FROM user_follow WHERE target_id={a['id']} AND user_id=91000000000000001 AND deleted_at IS NULL").stdout.strip(),'0'))
 s.check('replacement-hidden-original-target',['ART-09'],lambda:s.call('POST',f"/api/v1/admin/articles/{c['id']}/replacement",{'newArticleId':a['id']},actor='admin',status=404))
 # Republish A (allowed content operation); then the graph must still reject C -> A.
 av=s.call('GET','/api/v1/admin/articles/'+a['id'],actor='admin')
 av=s.call('POST',f"/api/v1/admin/articles/{a['id']}/versions",{'title':'republished A','richText':'<p>A</p>','contentType':'BASIC','categoryIds':[cat],'organizationIds':['902'],'keywordIds':['911'],'expectedVersion':av['lockVersion']},actor='admin')
 for action in ['SUBMIT','APPROVE','PUBLISH']:
  av=s.call('POST',f"/api/v1/admin/articles/{a['id']}/transitions",{'action':action,'versionId':av['latestVersionId'],'expectedVersion':av['lockVersion']},actor='admin')
 s.check('replacement-cycle-denied',['ART-09'],lambda:s.call('POST',f"/api/v1/admin/articles/{c['id']}/replacement",{'newArticleId':a['id']},actor='admin',status=409))
 c=s.call('POST',f"/api/v1/admin/articles/{c['id']}/transitions",{'action':'OFFLINE','versionId':c['latestVersionId'],'expectedVersion':c['lockVersion']},actor='admin')
 def invisible():
  v=s.call('GET',f"/api/v1/articles/{b['id']}/replacement");equal(v['status'],'UNAVAILABLE');equal(v['target'],None);assert 'private old body marker' not in str(v)
 s.check('replacement-hidden-target-redacted',['ART-09'],invisible)
 s.check('replacement-unknown-source-denied',['ART-09'],lambda:s.call('GET','/api/v1/articles/7777777777/replacement',status=404))
 # Verify live role grants using an already-issued staff token.
 grants=s.r.sql("SELECT rm.role_id,rm.menu_id FROM sys_role_menu rm JOIN sys_user_role ur ON ur.role_id=rm.role_id JOIN sys_menu m ON m.menu_id=rm.menu_id WHERE ur.user_id=4 AND m.perms='yunji:article:write'").stdout.splitlines()
 s.r.sql("DELETE rm FROM sys_role_menu rm JOIN sys_user_role ur ON ur.role_id=rm.role_id JOIN sys_menu m ON m.menu_id=rm.menu_id WHERE ur.user_id=4 AND m.perms='yunji:article:write'")
 s.check('existing-session-grant-revoked',['ADM-01','ADM-02'],lambda:s.call('POST','/api/v1/admin/articles',{'title':'revoked','richText':'<p>x</p>','contentType':'BASIC'},actor='test1',status=403))
 for grant in grants:
  role,menu=grant.split("\t");s.r.sql(f"INSERT INTO sys_role_menu(role_id,menu_id) VALUES({role},{menu})")
 s.check('rejected-write-durable-audit',['ADM-04'],lambda:equal(s.r.sql("SELECT COUNT(*)>0 FROM audit_event WHERE action='HTTP_WRITE_REJECTED' AND actor_client='STAFF'").stdout.strip(),'1'))
 s.check('success-write-durable-audit',['ADM-04'],lambda:equal(s.r.sql("SELECT COUNT(*)>0 FROM audit_event WHERE action='HTTP_WRITE' AND actor_client='CUSTOMER'").stdout.strip(),'1'))
 now=datetime.datetime.now(datetime.timezone.utc).isoformat()
 s.check('stay-invalid-duration-denied',['ANA-02'],lambda:s.call('POST','/api/v1/me/events',{'eventId':uuid.uuid4().hex,'type':'PAGE_STAY','objectId':s.article_id,'occurredAt':now,'payload':{'durationMs':999}},actor='alice',status=422))
 s.check('stay-unpublished-target-denied',['ANA-02'],lambda:s.call('POST','/api/v1/me/events',{'eventId':uuid.uuid4().hex,'type':'PAGE_STAY','objectId':c['id'],'occurredAt':now,'payload':{'durationMs':1200}},actor='alice',status=404))
 s.check('export-anonymous-denied',['ACT-05','ADM-04'],lambda:s.call('POST','/api/v1/admin/exports',{'dataset':'audit'},status=401))
 s.check('export-customer-denied',['ACT-05','ADM-04'],lambda:s.call('POST','/api/v1/admin/exports',{'dataset':'audit'},actor='alice',status=401))
 s.check('export-creator-denied',['ACT-05','ADM-04'],lambda:s.call('POST','/api/v1/admin/exports',{'dataset':'audit'},actor='test1',status=403))
 def export_activity():
  activity=s.r.sql('SELECT activity_id FROM activity_registration ORDER BY activity_id LIMIT 1').stdout.strip()
  key=uuid.uuid4().hex
  result=s.call('POST','/api/v1/admin/exports',{'dataset':'activity-registrations','activityId':activity},actor='admin',key=key)
  assert result['rowCount']>0 and result['totalCount']>=result['rowCount'] and 'answerSnapshot' in result['csv']
  equal(s.call('POST','/api/v1/admin/exports',{'dataset':'activity-registrations','activityId':activity},actor='admin',key=key),result)
 s.check('activity-export-content-and-replay',['ACT-05','ADM-04'],export_activity)
 s.check('export-dataset-allowlist',['ADM-04'],lambda:s.call('POST','/api/v1/admin/exports',{'dataset':'app_user'},actor='admin',status=422))
 s.check('export-durable-audit',['ADM-04'],lambda:equal(s.r.sql("SELECT COUNT(*)>0 FROM audit_event WHERE action='DATA_EXPORT'").stdout.strip(),'1'))
 def totals():
  value=s.call('GET','/api/v1/admin/analytics/users',actor='admin')
  assert value['activeAccounts']>=2 and value['uniqueUsers30Days']>=value['uniqueUsersToday']
  assert not any(k in str(value) for k in ['userId','nickname','phone','token','subjectHash'])
 s.check('analytics-users-aggregate-only',['ANA-06'],totals)
 s.check('analytics-users-grant-denied',['ANA-06'],lambda:s.call('GET','/api/v1/admin/analytics/users',actor='test1',status=403))
 def public_fields():
  for path in ['/api/v1/activities','/api/v1/review-campaigns','/api/v1/resources/subjects','/api/v1/resources/packages']:
   rows=s.call('GET',path)
   for row in rows:
    assert 'tenantId' not in row and 'createdBy' not in row and 'registrations' not in row
    if 'id' in row:assert isinstance(row['id'],str)
 s.check('public-domain-boundaries-redact-staff-fields',['ACT-01','REV-01','RES-01'],public_fields)
