from http_suite import equal
import uuid,datetime,time

def run(s):
 aid=s.article_id
 at=lambda seconds:(datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(seconds=seconds)).isoformat()
 def authorize(user,outcome='SENT',amount=5,key=None):
  return s.call('POST','/api/local/notifications/authorization',{'provider':'WECHAT_SUBSCRIBE','appId':'local-app','callbackKey':key or uuid.uuid4().hex,'amount':amount,'outcome':outcome},actor=user,headers={'X-Local-Secret':s.r.local_secret})
 key=uuid.uuid4().hex;authorize('alice',key=key)
 s.check('quota-callback-dedup',['NOT-06','NOT-09'],lambda:equal(authorize('alice',key=key)['quotas'][0]['available'],5))
 s.check('quota-callback-conflict',['NOT-06'],lambda:s.call('POST','/api/local/notifications/authorization',{'provider':'WECHAT_SUBSCRIBE','appId':'local-app','callbackKey':key,'amount':1,'outcome':'SENT'},actor='alice',headers={'X-Local-Secret':s.r.local_secret},status=409))
 s.check('quota-cannot-mint-without-secret',['NOT-06'],lambda:s.call('POST','/api/local/notifications/authorization',{'provider':'WECHAT_SUBSCRIBE','appId':'local-app','callbackKey':uuid.uuid4().hex,'amount':10,'outcome':'SENT'},actor='alice',status=404))
 authorize('bob','UNKNOWN',amount=5)
 base={'targetType':'ARTICLE','targetId':aid,'mode':'INSTANT','channel':'WECHAT_SUBSCRIBE','timezone':'Asia/Shanghai','enabled':True,'expectedVersion':0}
 r1=s.call('POST','/api/v1/me/notifications/rules',base,actor='alice');r2=s.call('POST','/api/v1/me/notifications/rules',base,actor='bob')
 s.check('rule-cross-user-denied',['NOT-01'],lambda:s.call('DELETE','/api/v1/me/notifications/rules/'+r1['id'],actor='bob',status=404))
 article=s.call('GET','/api/v1/admin/articles/'+aid,actor='test1')
 draft={'title':'合成通知V3','summary':'通知触发版本','richText':'<p>新消息</p>','contentType':'BASIC','categoryIds':[s.call('GET','/api/v1/catalog/categories')[0]['id']],'organizationIds':[902],'keywordIds':[911],'expectedVersion':article['lockVersion']}
 article=s.call('POST',f'/api/v1/admin/articles/{aid}/versions',draft,actor='test1')
 for action,actor in [('SUBMIT','test1'),('APPROVE','test'),('PUBLISH','test')]:article=s.call('POST',f'/api/v1/admin/articles/{aid}/transitions',{'action':action,'versionId':article['latestVersionId'],'expectedVersion':article['lockVersion'],'comment':'合成通知'},actor=actor)
 s.check('instant-outbox-dispatch',['NOT-04'],lambda:s.call('POST','/api/v1/admin/notifications/tick',actor='admin'))
 alice=s.call('GET','/api/v1/me/notifications/deliveries',actor='alice');bob=s.call('GET','/api/v1/me/notifications/deliveries',actor='bob')
 s.check('notification-sent-local',['NOT-04'],lambda:equal(alice[0]['status'],'SENT'))
 s.check('notification-unknown-freeze',['NOT-10'],lambda:equal(bob[0]['status'],'UNKNOWN'))
 before=s.r.sql('SELECT SUM(consumed),SUM(reserved) FROM notification_quota').stdout.strip()
 s.call('POST','/api/v1/admin/notifications/tick',actor='admin')
 s.check('duplicate-worker-no-double-charge',['NOT-09'],lambda:equal(s.r.sql('SELECT SUM(consumed),SUM(reserved) FROM notification_quota').stdout.strip(),before))
 s.check('unknown-no-automatic-retry',['NOT-10'],lambda:equal(s.r.sql(f"SELECT attempts FROM notification_delivery WHERE id={bob[0]['id']}").stdout.strip(),'1'))
 s.check('unknown-reconcile-needs-evidence',['NOT-10'],lambda:s.call('POST',f"/api/v1/admin/notifications/{bob[0]['id']}/reconcile",{'outcome':'PERMANENT_FAILURE','evidence':''},actor='admin',status=422))
 s.check('unknown-invalid-evidence-keeps-reservation',['NOT-09','NOT-10'],lambda:equal(s.r.sql(f"SELECT status FROM notification_delivery WHERE id={bob[0]['id']}").stdout.strip(),'UNKNOWN'))
 s.check('unknown-evidence-reconcile',['NOT-10'],lambda:s.call('POST',f"/api/v1/admin/notifications/{bob[0]['id']}/reconcile",{'outcome':'PERMANENT_FAILURE','evidence':'合成测试供应商回执，确认未送达'},actor='admin'))
 s.check('unknown-evidence-audit-persisted',['ADM-04','NOT-10'],lambda:equal(s.r.sql(f"SELECT JSON_UNQUOTE(JSON_EXTRACT(after_json,'$.evidence')) FROM audit_event WHERE action='NOTIFICATION_RECONCILE' AND object_id={bob[0]['id']}").stdout.strip(),'合成测试供应商回执，确认未送达'))
 s.check('quota-release-consistent',['NOT-09'],lambda:equal(s.r.sql("SELECT reserved FROM notification_quota WHERE user_id=91000000000000002").stdout.strip(),'0'))
 timed=s.call('POST','/api/v1/me/notifications/rules',{**base,'mode':'AT_TIME','scheduledAt':at(10)},actor='alice');s.r.sql(f"UPDATE notification_rule SET next_run_at=UTC_TIMESTAMP()-INTERVAL 1 SECOND WHERE id={timed['id']}")
 s.check('time-reminder-dispatch',['NOT-02'],lambda:s.call('POST','/api/v1/admin/notifications/tick',actor='admin'))
 s.check('time-reminder-only-once',['NOT-02'],lambda:equal(s.r.sql(f"SELECT COUNT(*) FROM notification_delivery WHERE rule_id={timed['id']}").stdout.strip(),'1'))
 daily=s.call('POST','/api/v1/me/notifications/rules',{**base,'targetType':'KEYWORD','targetId':'911','mode':'DAILY','localSendTime':'09:00:00'},actor='alice');s.r.sql(f"UPDATE notification_rule SET next_run_at=UTC_TIMESTAMP()-INTERVAL 1 SECOND WHERE id={daily['id']}")
 s.check('daily-summary-snapshot',['NOT-03'],lambda:s.call('POST','/api/v1/admin/notifications/tick',actor='admin'))
 s.check('daily-next-run-advanced',['NOT-03'],lambda:equal(s.r.sql(f"SELECT next_run_at>UTC_TIMESTAMP() FROM notification_rule WHERE id={daily['id']}").stdout.strip(),'1'))
 s.check('cancel-reminder',['NOT-05'],lambda:s.call('DELETE','/api/v1/me/notifications/rules/'+daily['id'],actor='alice'))
 s.check('channel-bind-status',['NOT-07','NOT-08'],lambda:equal(s.call('GET','/api/v1/me/notifications/status',actor='alice')['platformStatus'],'LOCAL_FAKE'))
 # Operational content and protocol version evidence.
 page=s.call('POST','/api/v1/admin/operation/pages',{'pageKey':'terms','title':'使用协议','content':'合成使用协议V1'},actor='admin');pv=page['versionId']
 s.check('consent-version',['AUTH-06','OPS-03'],lambda:s.call('POST','/api/v1/me/consents',{'versionId':pv,'type':'TERMS'},actor='alice'))
 newer=s.call('POST','/api/v1/admin/operation/pages',{'pageKey':'terms','title':'使用协议','content':'合成使用协议V2'},actor='admin')
 s.check('consented-body-immutable',['OPS-03'],lambda:equal(s.r.sql(f"SELECT content FROM content_page_version WHERE id={pv}").stdout.strip(),'合成使用协议V1'))
 s.check('home-banner',['OPS-01'],lambda:s.call('POST','/api/v1/admin/operation/banners',{'title':'合成Banner','imageUrl':'https://example.invalid/banner.png','linkType':'ARTICLE','linkValue':aid,'sortOrder':1,'status':'PUBLISHED'},actor='admin'))
 s.check('home-recommendation',['OPS-02'],lambda:s.call('POST','/api/v1/admin/operation/recommendations',{'articleId':aid,'sortOrder':1,'status':'PUBLISHED'},actor='admin'))
 s.check('activity-column',['OPS-04'],lambda:s.call('POST','/api/v1/admin/operation/activity-columns',{'name':'合成活动栏目','categoryId':s.call('GET','/api/v1/catalog/categories')[0]['id'],'sortOrder':1,'status':'ENABLED'},actor='admin'))
 s.check('home-visible-projections',['OPS-01','OPS-02'],lambda:equal(len(s.call('GET','/api/v1/home')['banners']),1))
 event={'eventId':uuid.uuid4().hex,'type':'VISIBLE_EXPOSURE','objectId':aid,'occurredAt':at(0),'payload':{'visibleMs':1200}}
 s.check('exposure-event',['ANA-01'],lambda:s.call('POST','/api/v1/me/events',event,actor='alice'))
 s.check('exposure-event-dedup',['ANA-01'],lambda:s.call('POST','/api/v1/me/events',event,actor='alice'))
 s.check('exposure-does-not-mark-read',['ANA-01'],lambda:equal(s.r.sql(f"SELECT read_version_id={article['latestVersionId']} FROM user_read_state WHERE user_id=91000000000000001 AND article_id={aid}").stdout.strip(),'0'))
 for typ in ['PAGE_VIEW','PAGE_STAY','SEARCH_CLICK','FILTER','BANNER_CLICK','SHARE','DOWNLOAD_INTENT']:
  s.check('behavior-'+typ,['ENG-05'] if typ=='SHARE' else ['ANA-02','ANA-03'],lambda typ=typ:s.call('POST','/api/v1/me/events',{'eventId':uuid.uuid4().hex,'type':typ,'objectId':aid,'occurredAt':at(0),'payload':{'durationMs':1500} if typ=='PAGE_STAY' else {}},actor='alice'))
 day=datetime.datetime.now(datetime.timezone.utc).date().isoformat();s.call('POST','/api/v1/admin/analytics/aggregate',{'date':day},actor='admin');before=s.r.sql('SELECT metric_key,value FROM analytics_daily ORDER BY metric_key').stdout
 s.check('analytics-rebuild-idempotent',['ANA-05'],lambda:s.call('POST','/api/v1/admin/analytics/aggregate',{'date':day},actor='admin'))
 s.check('analytics-no-double-count',['ANA-05'],lambda:equal(s.r.sql('SELECT metric_key,value FROM analytics_daily ORDER BY metric_key').stdout,before))
 def privacy():
  result=s.call('GET','/api/v1/admin/analytics',actor='admin');text=str(result)
  assert 'nickname' not in text and 'phone' not in text and 'subjectHash' not in text,text
 s.check('analytics-no-identity-data',['ANA-06'],privacy)
 s.check('audit-readable',['ADM-04'],lambda:s.call('GET','/api/v1/admin/audit',actor='admin'))
