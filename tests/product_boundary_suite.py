"""New product semantics under concurrency, revocation, review and reminder failures."""
import datetime,json,uuid
from concurrent.futures import ThreadPoolExecutor
from urllib.parse import urlencode
from http_suite import equal
from extended_suite import upload

def run(s):
 def call(method,path,body=None,actor='admin',**kw):return s.call(method,path,body,actor=actor,**kw)
 def check(name,fn):s.check(name,['PB-BOUNDARY'],fn)
 def sql(q):return s.r.sql(q).stdout.strip()
 now=datetime.datetime.now(datetime.timezone.utc)
 at=lambda days:(now+datetime.timedelta(days=days)).isoformat()
 category=call('GET','/api/v1/catalog/categories',actor=None)[0]['id']
 label=call('POST','/api/v1/admin/catalog/labels',{'name':'专项边界标签','categoryId':category,'status':'ENABLED','sortOrder':0})
 base={'title':'边界快照','summary':'边界摘要','richText':'<p>边界正文</p>','originalText':'边界原文','contentType':'BASIC','categoryIds':[category],'labelIds':[label['id']],'keywordNames':['边界词'],'hotDays':7,'contentWeight':3,'lifespan':'LONG','expectedVersion':0,'pushEnabled':True}
 def create(**kw):return call('POST','/api/v1/admin/articles',{**base,**kw})
 # Compare SQL production ranking, not a duplicate of its formula.
 ordinary=create(title='边界排序普通',contentWeight=5)
 pinned=create(title='边界排序置顶',pinned=True,contentWeight=1)
 urgent=create(title='边界排序紧急',urgent=True,contentWeight=1)
 def feed():return call('GET','/api/v1/articles?q=边界排序',actor='bob')['items']
 check('ranking-emergency-before-pin-before-normal',lambda:equal([x['id'] for x in feed()],[urgent['id'],pinned['id'],ordinary['id']]))
 call('PUT','/api/v1/me/read/'+pinned['id'],{'versionId':call('GET','/api/v1/articles/'+pinned['id'],actor=None)['versionId']},actor='bob')
 check('pinned-exempts-explicit-read',lambda:equal(pinned['id'] in [x['id'] for x in feed()],True))
 frozen=[create(title='边界快照'+str(i)) for i in range(7)]
 first=call('GET','/api/v1/articles/search?'+urlencode({'q':'边界快照','pageSize':2}),actor='bob')
 unseen=next(a for a in frozen if a['id'] not in [x['id'] for x in first['items']])
 call('POST','/api/v1/admin/articles/'+unseen['id']+'/state',{'action':'OFFLINE','expectedVersion':unseen['lockVersion']})
 later=create(title='边界快照新发布')
 def collect():
  seen=[x['id'] for x in first['items']];cursor=first['nextCursor']
  while cursor:
   page=call('GET','/api/v1/articles/search?'+urlencode({'q':'边界快照','pageSize':2,'cursor':cursor,'snapshotAt':first['snapshotAt']}),actor='bob')
   seen.extend(x['id'] for x in page['items']);cursor=page['nextCursor']
  equal(len(seen),6);equal(len(set(seen)),6);equal(unseen['id'] in seen,False);equal(later['id'] in seen,False)
 check('search-snapshot-stable-paging-revokes-hidden',collect)
 check('snapshot-query-tampering-rejected',lambda:call('GET','/api/v1/articles/search?'+urlencode({'q':'不同查询','pageSize':2,'cursor':first['nextCursor'],'snapshotAt':first['snapshotAt']}),actor='bob',status=400))
 check('snapshot-user-binding-enforced',lambda:call('GET','/api/v1/articles/search?'+urlencode({'q':'边界快照','pageSize':2,'cursor':first['nextCursor'],'snapshotAt':first['snapshotAt']}),actor='alice',status=400))
 sql("UPDATE article_feed_snapshot SET expires_at=UTC_TIMESTAMP()-INTERVAL 1 SECOND")
 check('expired-snapshot-explicit-conflict',lambda:call('GET','/api/v1/articles/search?'+urlencode({'q':'边界快照','pageSize':2,'cursor':first['nextCursor'],'snapshotAt':first['snapshotAt']}),actor='bob',status=409))
 check('admin-page-can-find-older-than-200',lambda:equal(call('GET','/api/v1/admin/articles/page?'+urlencode({'q':'修正后的标题'}))['total'],1))
 check('admin-page-500-import-total',lambda:equal(call('GET','/api/v1/admin/articles/page?'+urlencode({'q':'满量验收','page':25,'size':20}))['total'],500))
 check('admin-page-last-full-page',lambda:equal(len(call('GET','/api/v1/admin/articles/page?'+urlencode({'q':'满量验收','page':25,'size':20}))['items']),20))
 check('admin-page-limits-enforced',lambda:call('GET','/api/v1/admin/articles/page?size=101',status=422))
 for path in ['/api/v1/resources/packages','/api/v1/resources/exams','/api/v1/resources/artifacts']:
  check('retired-route-'+path.rsplit('/',1)[-1],lambda p=path:call('GET',p,actor=None,status=404))
 check('retired-consent-recording-route',lambda:call('POST','/api/v1/me/consents',{'versionId':1,'type':'TERMS'},actor='alice',status=404))
 check('retired-account-close-route',lambda:call('DELETE','/api/v1/me/account',{'confirmation':'注销账号'},actor='alice',status=404))
 # Concurrent normalization creates one global keyword and two independent contents.
 def concurrent_words():
  with ThreadPoolExecutor(2) as pool:articles=list(pool.map(lambda name:create(title='并发词条 '+name,keywordNames=[name]),['ConcurrentPB','concurrentpb']))
  ids=[call('GET','/api/v1/articles/'+a['id'],actor=None)['keywords'][0]['id'] for a in articles];equal(len(set(ids)),1)
 check('concurrent-keyword-normalization-single-identity',concurrent_words)
 canonical=create(title='别名规范词',keywordNames=['专项规范词'])
 canonical_id=call('GET','/api/v1/articles/'+canonical['id'],actor=None)['keywords'][0]['id']
 call('POST','/api/v1/admin/catalog/keywords/'+canonical_id+'/aliases',{'alias':'专项同义词'})
 aliased=create(title='别名复用',keywordNames=['专项同义词'])
 check('keyword-alias-reuses-canonical-identity',lambda:equal(call('GET','/api/v1/articles/'+aliased['id'],actor=None)['keywords'][0]['id'],canonical_id))
 call('POST','/api/v1/admin/catalog/blacklist',{'keyword':'专项禁用词','reason':'合成验收禁止新建'})
 check('manual-create-respects-keyword-blacklist',lambda:call('POST','/api/v1/admin/articles',{**base,'keywordNames':['专项禁用词']},status=422))
 expiring=create(title='热点归档仍能读')
 sql(f"UPDATE article SET hot_until=DATE(UTC_TIMESTAMP()+INTERVAL 8 HOUR)-INTERVAL 1 DAY WHERE id={expiring['id']}")
 check('hot-soft-archive-bounded-execution',lambda:equal(call('POST','/api/v1/admin/articles/archive-hot',{})['archived']>=1,True))
 check('hot-archive-preserves-detail',lambda:equal(call('GET','/api/v1/articles/'+expiring['id'],actor=None)['article']['title'],'热点归档仍能读'))
 check('hot-archive-repeat-idempotent',lambda:equal(call('POST','/api/v1/admin/articles/archive-hot',{})['archived'],0))
 s.login_customer('closure',91000000000000003)
 chain=[create(title='链式更替'+x,keywordNames=['链式词']) for x in ['A','B','C']]
 call('PUT','/api/v1/me/follows/ARTICLE/'+chain[0]['id'],{'enabled':True},actor='closure')
 for old,new in zip(chain,chain[1:]):call('POST','/api/v1/admin/articles/'+old['id']+'/replacement',{'newArticleId':new['id']})
 check('replacement-chain-follows-final-target',lambda:equal(any(x['targetType']=='ARTICLE' and x['targetId']==chain[-1]['id'] for x in call('GET','/api/v1/me/follows',actor='closure')),True))
 check('replacement-chain-resolves-to-final-target',lambda:equal(call('GET','/api/v1/articles/'+chain[0]['id']+'/replacement',actor=None)['target']['id'],chain[-1]['id']))
 second=call('POST','/api/v1/admin/catalog/labels',{'name':'专项第二标签','categoryId':category,'status':'ENABLED','sortOrder':0})
 for lid in [label['id'],second['id']]:call('PUT','/api/v1/me/follows/LABEL/'+lid,{'enabled':True},actor='closure')
 both=create(title='双标签共享状态',labelIds=[label['id'],second['id']],keywordNames=['双标签词'])
 def occurrences():return [b for b in call('GET','/api/v1/me/subscription-boxes',actor='closure') if any(i['id']==both['id'] for i in b['items'])]
 check('multi-label-two-boxes-share-article',lambda:equal(len(occurrences()),2))
 def event(id,kind='VISIBLE_EXPOSURE'):
  return call('POST','/api/v1/me/events',{'eventId':uuid.uuid4().hex,'type':kind,'objectId':id,'occurredAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'payload':{'visibleRatio':0.5,'durationMs':1000} if kind=='VISIBLE_EXPOSURE' else {}},actor='closure')
 event(both['id']);event(both['id'])
 check('multi-label-shared-exposure-dedup',lambda:equal(sql(f"SELECT visible_count FROM user_exposure_total WHERE user_id=91000000000000003 AND article_id={both['id']}"),'1'))
 event(both['id'],'PAGE_VIEW');event(both['id'],'PAGE_VIEW')
 check('click-30s-dedup',lambda:equal(sql(f"SELECT COUNT(*) FROM behavior_event WHERE user_id=91000000000000003 AND object_id={both['id']} AND event_type='PAGE_VIEW'"),'1'))
 check('multi-label-read-removes-all-boxes',lambda:equal(len(occurrences()),0))
 # Registration concurrency and review backend remain useful as callable activity functions.
 activity=call('POST','/api/v1/admin/activities',{'title':'边界报名','registrationStart':at(-1),'registrationEnd':at(1),'eventStart':at(2),'eventEnd':at(3),'capacity':1,'waitlistEnabled':True,'status':'PUBLISHED','fields':[],'expectedVersion':0})
 def register():
  with ThreadPoolExecutor(2) as pool:rows=list(pool.map(lambda user:call('POST','/api/v1/me/activities/'+activity['id']+'/registration',{'formVersionId':activity['form']['id'],'answers':{}},actor=user),['alice','bob']))
  equal(sorted(r['status'] for r in rows),['CONFIRMED','WAITLISTED']);return rows
 registrations=register()
 check('activity-concurrent-no-oversell',lambda:equal(sql(f"SELECT confirmed_count FROM activity WHERE id={activity['id']}"),'1'))
 owner='alice' if registrations[0]['status']=='CONFIRMED' else 'bob'
 call('DELETE','/api/v1/me/activities/'+activity['id']+'/registration',actor=owner)
 check('activity-cancel-promotes-waitlist',lambda:equal(sql(f"SELECT COUNT(*) FROM activity_registration WHERE activity_id={activity['id']} AND status='CONFIRMED'"),'1'))
 config={'title':'边界评审','openAt':at(-1),'closeAt':at(1),'status':'PUBLISHED','requirements':[{'key':'proof','label':'证明','required':True,'mediaTypes':['text/plain'],'maxSize':10000}]}
 campaign=call('POST','/api/v1/admin/review-campaigns',config,actor='test1');cid=campaign['id']
 check('review-single-role-no-organization',lambda:equal(call('GET','/api/v1/me/review-campaigns/'+cid+'/eligibility',actor='bob')['eligible'],True))
 file=upload(s,'alice','proof.txt',b'synthetic proof')
 app=call('POST','/api/v1/me/review-campaigns/'+cid+'/draft',{'files':{},'expectedVersion':0},actor='alice');appid=app['id']
 check('review-missing-required-material',lambda:call('POST','/api/v1/me/review-applications/'+appid+'/actions',{'action':'SUBMIT','submissionId':app['latestSubmissionId'],'expectedVersion':app['lockVersion']},actor='alice',status=422))
 app=call('POST','/api/v1/me/review-campaigns/'+cid+'/draft',{'files':{'proof':file['id']},'expectedVersion':app['lockVersion']},actor='alice')
 app=call('POST','/api/v1/me/review-applications/'+appid+'/actions',{'action':'SUBMIT','submissionId':app['latestSubmissionId'],'expectedVersion':app['lockVersion']},actor='alice');sub=app['currentSubmissionId']
 check('review-owner-isolation',lambda:call('GET','/api/v1/me/review-applications/'+appid,actor='bob',status=404))
 check('review-submitted-material-frozen',lambda:call('POST','/api/v1/me/review-campaigns/'+cid+'/draft',{'files':{'proof':file['id']},'expectedVersion':app['lockVersion']},actor='alice',status=409))
 app=call('POST','/api/v1/admin/review-applications/'+appid+'/decisions',{'action':'CORRECTION','submissionId':sub,'expectedVersion':app['lockVersion'],'comment':'补正合成证明'},actor='test1')
 app=call('POST','/api/v1/me/review-campaigns/'+cid+'/draft',{'files':{'proof':file['id']},'expectedVersion':app['lockVersion']},actor='alice')
 app=call('POST','/api/v1/me/review-applications/'+appid+'/actions',{'action':'SUBMIT','submissionId':app['latestSubmissionId'],'expectedVersion':app['lockVersion']},actor='alice')
 check('review-stale-submission-rejected',lambda:call('POST','/api/v1/admin/review-applications/'+appid+'/decisions',{'action':'APPROVED','submissionId':sub,'expectedVersion':app['lockVersion'],'comment':'旧提交不允许'},actor='test1',status=409))
 app=call('POST','/api/v1/admin/review-applications/'+appid+'/decisions',{'action':'APPROVED','submissionId':app['currentSubmissionId'],'expectedVersion':app['lockVersion'],'comment':'核实新提交'},actor='test1')
 check('review-correction-history-retained',lambda:equal([d['decision'] for d in app['decisions']],['CORRECTION','APPROVED']))
 check('review-global-activity-reference',lambda:call('PUT','/api/v1/admin/activities/'+activity['id']+'/functions',{'files':[],'reviewIds':[cid],'expectedVersion':call('GET','/api/v1/admin/activities/'+activity['id'])['lockVersion']}))
 # Reminder changes come from creation/replacement, never typo edits. Simulated transport is explicit.
 word=create(title='提醒关键词基线',keywordNames=['专项提醒词'])
 keyword=call('GET','/api/v1/articles/'+word['id'],actor=None)['keywords'][0]['id']
 def auth(bound=True,amount=0,outcome='SUCCESS'):return call('POST','/api/local/reminders/authorization',{'receiptKey':uuid.uuid4().hex,'wecomBound':bound,'amount':amount,'outcome':outcome},actor='closure',headers={'X-Local-Secret':s.r.local_secret})
 def tick():return call('POST','/api/v1/admin/reminders/tick',{})
 def batches():return call('GET','/api/v1/me/reminders/batches',actor='closure')['items']
 auth(amount=5)
 rule=call('POST','/api/v1/me/reminders/rules',{'targetType':'KEYWORD','targetId':keyword,'mode':'DAILY','enabled':True,'expectedVersion':0},actor='closure')
 call('PUT','/api/v1/me/reminders/settings',{'enabled':True,'slot':call('GET','/api/v1/me/reminders/settings',actor='closure')['slots'][0],'expectedVersion':0},actor='closure')
 sql("UPDATE reminder_preference SET slot='00:00:00',last_batch_date=NULL WHERE user_id=91000000000000003")
 first_reminder=create(title='日常新增触发',keywordNames=['专项提醒词'])
 check('keyword-new-publication-captures-reminder',lambda:equal(sql(f"SELECT state FROM reminder_change WHERE user_id=91000000000000003 AND article_id={first_reminder['id']}"),'PENDING'))
 event(first_reminder['id'],'PAGE_VIEW');tick()
 check('read-cancels-pending-reminder',lambda:equal(sql(f"SELECT state FROM reminder_change WHERE user_id=91000000000000003 AND article_id={first_reminder['id']}"),'READ'))
 daily=create(title='日常自动规划',keywordNames=['专项提醒词']);tick();batch=next(b for b in batches() if b['channel']=='WECom')
 check('daily-task-created-before-staff-execution',lambda:equal(batch['state'],'AWAITING_STAFF'))
 count=len(batches());tick()
 check('daily-task-not-repeated-without-confirmation',lambda:equal(len(batches()),count))
 call('POST','/api/v1/admin/reminders/batches/'+batch['id']+'/reconcile',{'state':'SENT','evidence':'LOCAL 合成回执人工已执行确认'})
 check('staff-confirmation-final',lambda:equal(next(b for b in batches() if b['id']==batch['id'])['state'],'SENT'))
 rule=call('PUT','/api/v1/me/reminders/rules/'+rule['id'],{'targetType':'KEYWORD','targetId':keyword,'mode':'INSTANT','enabled':True,'expectedVersion':rule['lockVersion']},actor='closure')
 instant=create(title='一次性即时新增',keywordNames=['专项提醒词']);tick()
 check('instant-synthetic-send-and-downgrade',lambda:equal((sql(f"SELECT state FROM reminder_change WHERE user_id=91000000000000003 AND article_id={instant['id']}"),call('GET','/api/v1/me/reminders/rules/'+rule['id'],actor='closure')['mode']),('SENT','DAILY')))
 rule=call('GET','/api/v1/me/reminders/rules/'+rule['id'],actor='closure')
 auth(outcome='UNKNOWN');rule=call('PUT','/api/v1/me/reminders/rules/'+rule['id'],{'targetType':'KEYWORD','targetId':keyword,'mode':'INSTANT','enabled':True,'expectedVersion':rule['lockVersion']},actor='closure')
 uncertain=create(title='未知结果不重发',keywordNames=['专项提醒词']);tick();unknown=next(b for b in batches() if b['state']=='UNKNOWN');tick()
 check('unknown-transport-never-blind-retry',lambda:equal(next(b for b in batches() if b['id']==unknown['id'])['attempts'],1))
 check('monitor-surfaces-unknown-incident',lambda:equal(any(x['id']==unknown['id'] for x in call('GET','/api/v1/admin/reminders/health')['items']),True))
 call('POST','/api/v1/admin/reminders/batches/'+unknown['id']+'/reconcile',{'state':'FAILED','evidence':'LOCAL 平台明确未发送核实凭证'})
 auth(outcome='SUCCESS');call('POST','/api/v1/admin/reminders/batches/'+unknown['id']+'/retry',{'evidence':'LOCAL 配置已修复允许补提凭证'});tick()
 check('definite-failure-retry-after-proof',lambda:equal(next(b for b in batches() if b['id']==unknown['id'])['state'],'SENT'))
 sql("UPDATE reminder_preference SET last_batch_date=NULL WHERE user_id=91000000000000003")
 auth(bound=False);implicit=create(title='批次隐式已读抑制',keywordNames=['专项提醒词']);tick()
 waiting=next(b for b in batches() if b['state']=='WAITING_AUTH')
 for i in range(9):
  if i:sql(f"UPDATE user_exposure_total SET last_visible_at=UTC_TIMESTAMP()-INTERVAL 31 SECOND WHERE user_id=91000000000000003 AND article_id={implicit['id']}")
  event(implicit['id'])
 auth();sql(f"UPDATE reminder_batch SET next_attempt_at=UTC_TIMESTAMP()-INTERVAL 1 SECOND WHERE id={waiting['id']}");tick()
 check('implicit-read-cancels-already-batched-unsent',lambda:equal(next(b for b in batches() if b['id']==waiting['id'])['state'],'CANCELLED'))
 metrics=call('GET','/api/v1/admin/analytics/product-metrics')
 check('analytics-review-and-registration-source-counts',lambda:equal((next(x for x in metrics['reviews'] if x['id']==cid)['approved'],next(x for x in metrics['activities'] if x['id']==activity['id'])['confirmed']),(1,1)))
 check('reminder-statistics-source-trend-contract',lambda:equal(set(['taskCreated','confirmed','trend','failures','sources']).issubset(call('GET','/api/v1/admin/reminders/statistics')),True))
