"""Change-driven reminders: behavior, authority, frozen batches, and recovery evidence."""
from http_suite import equal
from concurrent.futures import ThreadPoolExecutor
import uuid,time,datetime

def run(s):
    api='/api/v1/me/reminders'; admin='/api/v1/admin/reminders'
    def auth(user='alice',amount=5,outcome='SUCCESS',bound=True,key=None,status=200):
        return s.call('POST','/api/local/reminders/authorization',{'receiptKey':key or uuid.uuid4().hex,'wecomBound':bound,'amount':amount,'outcome':outcome},actor=user,headers={'X-Local-Secret':s.r.local_secret},status=status)
    def tick():return s.call('POST',admin+'/tick',{},actor='admin')
    def sql(q):return s.r.sql(q).stdout.strip()
    category=s.call('GET','/api/v1/catalog/categories')[0]['id']
    def article(title,keyword=911):
        a=s.call('POST','/api/v1/admin/articles',{'title':title,'summary':'纯合成验收','richText':'<p>合成正文</p>','contentType':'BASIC','categoryIds':[category],'organizationIds':[902],'keywordIds':[keyword],'expectedVersion':0},actor='test1')
        return publish(a)
    def publish(a):
        for action,actor in [('SUBMIT','test1'),('APPROVE','test'),('PUBLISH','test')]:
            a=s.call('POST',f"/api/v1/admin/articles/{a['id']}/transitions",{'action':action,'versionId':a['latestVersionId'],'expectedVersion':a['lockVersion'],'comment':'提醒验收'},actor=actor)
        return a
    def revision(a):
        a=s.call('GET','/api/v1/admin/articles/'+a['id'],actor='test1')
        a=s.call('POST',f"/api/v1/admin/articles/{a['id']}/versions",{'title':'提醒修订 '+uuid.uuid4().hex[:6],'summary':'合成更新','richText':'<p>新版本</p>','contentType':'BASIC','categoryIds':[category],'organizationIds':[902],'keywordIds':[911],'expectedVersion':a['lockVersion']},actor='test1')
        return publish(a)
    def rule(a,user='alice',mode='DAILY',kind='ARTICLE'):
        return s.call('POST',api+'/rules',{'targetType':kind,'targetId':a,'mode':mode,'enabled':True,'expectedVersion':0},actor=user)
    def due():s.r.sql("UPDATE reminder_preference SET slot='00:00:00',last_batch_date=NULL")
    def changes(user='alice'):return s.call('GET',api+'/changes',actor=user)['items']
    def batches(user='alice'):return s.call('GET',api+'/batches',actor=user)['items']
    s.check('reminder-anonymous-denied',['SR-01'],lambda:s.call('GET',api+'/settings',status=401))
    s.check('reminder-admin-permission',['SR-01'],lambda:s.call('GET',admin+'/settings',actor='test1',status=403))
    key=uuid.uuid4().hex;auth(key=key)
    s.check('reminder-authorization-receipt-idempotent',['SR-07'],lambda:equal(auth(key=key)['permissions'][0]['available'],5))
    s.check('reminder-authorization-receipt-conflict',['SR-07'],lambda:auth(key=key,amount=1,status=409))
    s.check('reminder-authorize-no-client-mint',['SR-07'],lambda:s.call('POST','/api/local/reminders/authorization',{'receiptKey':uuid.uuid4().hex,'wecomBound':True,'amount':5,'outcome':'SUCCESS'},actor='alice',status=404))
    a=article('跟踪基线');r=rule(a['id']);keyword=rule(911,kind='KEYWORD')
    s.check('reminder-no-retroactive-changes',['SR-03'],lambda:equal(len(changes()),0))
    s.check('reminder-rule-cross-user',['SR-01'],lambda:s.call('GET',api+'/rules/'+r['id'],actor='bob',status=404))
    settings=s.call('GET',api+'/settings',actor='alice')
    s.check('reminder-settings-save',['SR-06'],lambda:s.call('PUT',api+'/settings',{'enabled':True,'slot':settings['slots'][0],'expectedVersion':settings['lockVersion']},actor='alice'))
    s.check('reminder-settings-stale',['SR-06'],lambda:s.call('PUT',api+'/settings',{'enabled':True,'slot':settings['slots'][0],'expectedVersion':settings['lockVersion']},actor='alice',status=409))
    a=revision(a)
    s.check('reminder-revision-article-only',['SR-03'],lambda:equal(int(sql('SELECT COUNT(*) FROM reminder_match')),1))
    cid=changes()[0]['id']
    s.check('reminder-ignore-cross-user',['SR-04'],lambda:s.call('DELETE',api+'/changes/'+cid,actor='bob',status=404))
    s.check('reminder-read-suppresses-pending',['SR-04'],lambda:s.call('PUT','/api/v1/me/read/'+a['id'],{'versionId':a['currentVersionId']},actor='alice'))
    s.check('reminder-read-keeps-tracking',['SR-04'],lambda:equal(s.call('GET',api+'/rules/'+r['id'],actor='alice')['enabled'],True))
    due();tick()
    s.check('reminder-no-empty-daily',['SR-05'],lambda:equal(len(batches()),0))
    a=revision(a)
    s.check('reminder-old-read-does-not-suppress-new',['SR-04'],lambda:equal(len(changes()),1))
    due();tick();b=batches()[0]
    s.check('reminder-wecom-task-awaits-staff',['SR-07'],lambda:equal(b['state'],'AWAITING_STAFF'))
    s.check('reminder-wecom-does-not-consume-wechat-quota',['SR-07'],lambda:equal(sql('SELECT available FROM reminder_permission WHERE user_id=91000000000000001'),'5'))
    tick();due();tick()
    s.check('reminder-unexecuted-task-not-recreated',['SR-05'],lambda:equal(len(batches()),1))
    newer=article('批次冻结之后的新资讯')
    s.check('reminder-new-change-outside-frozen-batch',['SR-05'],lambda:equal(int(sql(f"SELECT COUNT(*) FROM reminder_batch_item WHERE batch_id={b['id']}")),1))
    s.check('reminder-reconcile-needs-proof',['SR-07'],lambda:s.call('POST',admin+'/batches/'+b['id']+'/reconcile',{'state':'SENT','evidence':'不够'},actor='admin',status=422))
    s.call('POST',admin+'/batches/'+b['id']+'/reconcile',{'state':'SENT','evidence':'LOCAL 合成平台执行回执确认发送'},actor='admin')
    s.check('reminder-completion-keeps-later-changes',['SR-05'],lambda:equal(sql(f"SELECT state FROM reminder_change WHERE article_id={newer['id']}"),'PENDING'))
    # Explicit dismissal only suppresses the current update, keeping keyword tracking.
    c=next(c for c in changes() if c['articleId']==newer['id']);s.call('DELETE',api+'/changes/'+c['id'],actor='alice')
    s.check('reminder-dismiss-keeps-rule',['SR-04'],lambda:equal(s.call('GET',api+'/rules/'+keyword['id'],actor='alice')['enabled'],True))
    instant=article('即时跟踪对象',keyword=911);ri=rule(instant['id'],mode='INSTANT');instant=revision(instant)
    tick();ib=next(x for x in batches() if x['channel']=='WECHAT_SUBSCRIBE')
    s.check('reminder-instant-local-send',['SR-07'],lambda:equal(ib['state'],'SENT'))
    s.check('reminder-instant-success-downgrades',['SR-05'],lambda:equal(s.call('GET',api+'/rules/'+ri['id'],actor='alice')['mode'],'DAILY'))
    s.check('reminder-instant-quota-once',['SR-07'],lambda:equal(sql('SELECT consumed FROM reminder_permission WHERE user_id=91000000000000001'),'1'))
    auth('bob',outcome='UNKNOWN');rb=rule(instant['id'],'bob','INSTANT');instant=revision(instant);tick()
    ub=batches('bob')[0]
    s.check('reminder-unknown-frozen',['SR-07'],lambda:equal(ub['state'],'UNKNOWN'))
    s.check('reminder-unknown-keeps-instant',['SR-05'],lambda:equal(s.call('GET',api+'/rules/'+rb['id'],actor='bob')['mode'],'INSTANT'))
    tick();s.check('reminder-unknown-no-retry',['SR-07'],lambda:equal(batches('bob')[0]['attempts'],1))
    s.call('POST',admin+'/batches/'+ub['id']+'/reconcile',{'state':'FAILED','evidence':'LOCAL 合成回执明确未发送'},actor='admin')
    s.check('reminder-failure-restores-reservation',['SR-07'],lambda:equal(sql('SELECT reserved FROM reminder_permission WHERE user_id=91000000000000002'),'0'))
    replacement=article('替换目标');s.call('POST',f"/api/v1/admin/articles/{instant['id']}/replacement",{'newArticleId':replacement['id']},actor='admin')
    s.check('reminder-replacement-migrates-rule',['SR-03'],lambda:equal(s.call('GET',api+'/rules/'+rb['id'],actor='bob')['targetId'],replacement['id']))
    s.check('reminder-replacement-emits-change',['SR-03'],lambda:equal(sql(f"SELECT COUNT(*) FROM reminder_change WHERE user_id=91000000000000002 AND article_id={replacement['id']} AND event_type='REPLACED'"),'1'))
    before=sql('SELECT COUNT(*) FROM reminder_batch');
    with ThreadPoolExecutor(max_workers=2) as pool:list(pool.map(lambda _:tick(),range(2)))
    s.check('reminder-concurrent-worker-single-membership',['SR-05'],lambda:equal(sql('SELECT COUNT(*)-COUNT(DISTINCT change_id) FROM reminder_batch_item'),'0'))
    # Administrative explicit curation is distinct from historical article-derived keywords.
    label=s.call('POST','/api/v1/admin/catalog/labels',{'name':'验收标签','categoryId':category,'parentId':None,'status':'ENABLED','sortOrder':0,'expectedVersion':0},actor='admin')
    s.check('curation-write',['CAT-06'],lambda:s.call('PUT',f"/api/v1/admin/catalog/labels/{label['id']}/keywords",{'keywordIds':[911],'expectedVersion':0},actor='admin'))
    s.check('curation-public-explicit-keyword',['CAT-06'],lambda:equal(s.call('GET',f"/api/v1/catalog/labels/{label['id']}/curated-keywords")[0]['id'],'911'))
    s.check('curation-stale-rejected',['CAT-06'],lambda:s.call('PUT',f"/api/v1/admin/catalog/labels/{label['id']}/keywords",{'keywordIds':[],'expectedVersion':0},actor='admin',status=409))
    s.check('reminder-statistics-not-sample',['SR-06'],lambda:equal(sum(r['total'] for r in s.call('GET',admin+'/statistics',actor='admin')['states']),int(sql('SELECT COUNT(*) FROM reminder_batch'))))

    # Lease expiry must freeze, never resend an uncertain external call.
    auth('bob',outcome='SUCCESS',amount=0)
    s.r.sql("UPDATE reminder_batch SET state='SENDING',lease_until=UTC_TIMESTAMP()-INTERVAL 5 MINUTE WHERE state='UNKNOWN'")
    tick()
    s.check('reminder-expired-lease-freezes',['SR-07'],lambda:equal(sql("SELECT COUNT(*) FROM reminder_batch WHERE state='SENDING' AND lease_until<UTC_TIMESTAMP()"),'0'))
    s.check('reminder-expired-lease-auditable',['SR-07'],lambda:equal(int(sql("SELECT COUNT(*) FROM reminder_batch WHERE state='UNKNOWN' AND error_code='LEASE_EXPIRED'"))>0,True))
    s.check('reminder-customer-cannot-tick',['SR-01'],lambda:s.call('POST',admin+'/tick',{},actor='alice',status=401))
    s.check('reminder-rules-page-bound',['SR-06'],lambda:s.call('GET',api+'/rules?size=101',actor='alice',status=422))
    control=s.call('GET',admin+'/settings',actor='admin')
    settings_body={**control,'expectedVersion':control['lockVersion'],'slots':['00:00','13:00','17:00']}
    s.check('reminder-control-persist',['SR-06'],lambda:s.call('PUT',admin+'/settings',settings_body,actor='admin'))
    s.check('reminder-control-stale',['SR-06'],lambda:s.call('PUT',admin+'/settings',settings_body,actor='admin',status=409))
    control=s.call('GET',admin+'/settings',actor='admin')
    s.check('reminder-control-duplicate-slots',['SR-06'],lambda:s.call('PUT',admin+'/settings',{**control,'expectedVersion':control['lockVersion'],'slots':['08:00','08:00','17:00']},actor='admin',status=422))
    # Many keyword matches on a first publication share one change and preserve explanations.
    k2=s.call('POST','/api/v1/admin/catalog/keywords',{'name':'专项验收词','status':'ENABLED','sortOrder':0},actor='admin')
    rule(k2['id'],kind='KEYWORD')
    d=s.call('POST','/api/v1/admin/articles',{'title':'双关键词去重','summary':'合成','richText':'<p>正文</p>','contentType':'BASIC','categoryIds':[category],'organizationIds':[902],'keywordIds':[911,k2['id']],'expectedVersion':0},actor='test1')
    d=publish(d)
    s.check('reminder-multiple-keywords-one-change',['SR-03'],lambda:equal(sql(f"SELECT COUNT(*) FROM reminder_change WHERE user_id=91000000000000001 AND article_id={d['id']}"),'1'))
    s.check('reminder-multiple-keywords-keep-sources',['SR-03'],lambda:equal(sql(f"SELECT COUNT(*) FROM reminder_match m JOIN reminder_change c ON c.id=m.change_id WHERE c.user_id=91000000000000001 AND c.article_id={d['id']}"),'2'))
    # A read racing a worker has one serial result; provider-submitted tasks are not claimed revoked.
    with ThreadPoolExecutor(max_workers=2) as pool:
        futures=[pool.submit(tick),pool.submit(s.call,'PUT','/api/v1/me/read/'+d['id'],{'versionId':d['currentVersionId']},actor='alice')]
        for f in futures:f.result()
    s.check('reminder-read-worker-race-consistent',['SR-04'],lambda:equal(sql(f"SELECT state FROM reminder_change WHERE user_id=91000000000000001 AND article_id={d['id']}") in ['READ','BATCHED'],True))

    s.check('reminder-wecom-channel-filter',['SR-06'],lambda:equal(all(b['channel']=='WECom' for b in s.call('GET',admin+'/batches?channel=WECom',actor='admin')['items']),True))
    unknown=sql("SELECT id FROM reminder_batch WHERE state='UNKNOWN' LIMIT 1")
    s.check('reminder-unknown-cannot-retry',['SR-07'],lambda:s.call('POST',admin+'/batches/'+unknown+'/retry',{'evidence':'LOCAL 未核实不能盲目补提'},actor='admin',status=404))
    auth('bob',amount=0,outcome='FAILURE')
    failed_article=article('明确失败补提对象');rf=rule(failed_article['id'],'bob','INSTANT');failed_article=revision(failed_article);tick()
    failed=next(b for b in batches('bob') if b['state']=='FAILED' and b['id']!=ub['id'])
    auth('bob',amount=0,outcome='SUCCESS')
    retry_key=uuid.uuid4().hex;retry_body={'evidence':'LOCAL 已核实平台明确拒绝并修复配置'}
    s.check('reminder-definite-failure-retry',['SR-07'],lambda:s.call('POST',admin+'/batches/'+failed['id']+'/retry',retry_body,actor='admin',key=retry_key))
    s.check('reminder-retry-request-dedup',['SR-07'],lambda:s.call('POST',admin+'/batches/'+failed['id']+'/retry',retry_body,actor='admin',key=retry_key))
    tick()
    s.check('reminder-retry-success-one-quota',['SR-07'],lambda:equal(sql(f"SELECT CONCAT(state,':',attempts,':',quota_reserved) FROM reminder_batch WHERE id={failed['id']}"),'SENT:2:0'))
    s.check('reminder-retry-success-downgrades',['SR-05'],lambda:equal(s.call('GET',api+'/rules/'+rf['id'],actor='bob')['mode'],'DAILY'))

    future=(datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(hours=2)).isoformat()
    scheduled=s.call('POST','/api/v1/admin/articles',{'title':'未来生效资讯','richText':'<p>未来正文</p>','contentType':'BASIC','categoryIds':[category],'keywordIds':[911],'effectiveFrom':future},actor='test1')
    scheduled=publish(scheduled);due();tick()
    s.check('reminder-future-effective-keeps-pending',['SR-03'],lambda:equal(sql(f"SELECT state FROM reminder_change WHERE user_id=91000000000000001 AND article_id={scheduled['id']}"),'PENDING'))
    s.r.sql(f"UPDATE article SET effective_from=UTC_TIMESTAMP()-INTERVAL 1 MINUTE WHERE id={scheduled['id']}");due();tick()
    s.check('reminder-effective-now-plans',['SR-03'],lambda:equal(sql(f"SELECT state FROM reminder_change WHERE user_id=91000000000000001 AND article_id={scheduled['id']}"),'BATCHED'))

    # Round-robin fairness across the bounded page even when the first users are paused.
    for uid in range(10001,10102):
        s.r.sql(f"INSERT INTO app_user(id,nickname) VALUES({uid},'合成公平性用户'); INSERT INTO reminder_preference(user_id,enabled,slot) VALUES({uid},0,'00:00'); INSERT INTO reminder_change(id,user_id,article_id,version_id,event_type) VALUES({uid},{uid},{scheduled['id']},{scheduled['currentVersionId']},'PUBLISHED')")
    s.r.sql('UPDATE reminder_control SET plan_after_user=0');tick();cursor1=int(sql('SELECT plan_after_user FROM reminder_control'));tick();cursor2=int(sql('SELECT plan_after_user FROM reminder_control'))
    s.check('reminder-planner-does-not-starve-later-users',['SR-05'],lambda:equal(cursor2>cursor1,True))
    s.r.sql("UPDATE reminder_change SET state='CANCELLED' WHERE user_id BETWEEN 10001 AND 10101")

    retired=article('撤下后仍可暂停');rt=rule(retired['id'])
    s.call('POST',f"/api/v1/admin/articles/{retired['id']}/transitions",{'action':'OFFLINE','versionId':retired['currentVersionId'],'expectedVersion':retired['lockVersion']},actor='admin')
    s.check('reminder-can-pause-offline-article',['SR-04'],lambda:s.call('PUT',api+'/rules/'+rt['id'],{'targetType':'ARTICLE','targetId':retired['id'],'mode':'DAILY','enabled':False,'expectedVersion':rt['lockVersion']},actor='alice'))

    # Operator pause is a versioned, audited stop; it never grants consent or resumes a rule.
    pa=article('运营暂停专项对象');pr=rule(pa['id']);endpoint=admin+'/rules/'+pr['id']+'/pause'
    body={'expectedVersion':pr['lockVersion'],'reason':'LOCAL 用户工单请求暂停该条提醒'}
    s.check('reminder-pause-anonymous-denied',['SR-01'],lambda:s.call('POST',endpoint,body,status=401))
    s.check('reminder-pause-staff-permission',['SR-01'],lambda:s.call('POST',endpoint,body,actor='test1',status=403))
    s.check('reminder-pause-customer-denied',['SR-01'],lambda:s.call('POST',endpoint,body,actor='alice',status=401))
    s.check('reminder-pause-missing-rule',['SR-06'],lambda:s.call('POST',admin+'/rules/1/pause',body,actor='admin',status=404))
    s.check('reminder-pause-reason-required',['SR-06'],lambda:s.call('POST',endpoint,{**body,'reason':'不足'},actor='admin',status=422))
    s.check('reminder-pause-whitespace-rejected',['SR-06'],lambda:s.call('POST',endpoint,{**body,'reason':' '*20},actor='admin',status=422))
    s.check('reminder-pause-stale-version',['SR-06'],lambda:s.call('POST',endpoint,{**body,'expectedVersion':pr['lockVersion']+1},actor='admin',status=409))
    batch_snapshot=sql('SELECT id,state FROM reminder_batch ORDER BY id');pause_key=uuid.uuid4().hex
    s.check('reminder-operator-pause',['SR-06'],lambda:equal(s.call('POST',endpoint,body,actor='admin',key=pause_key)['enabled'],False))
    s.check('reminder-pause-request-idempotent',['SR-06'],lambda:equal(s.call('POST',endpoint,body,actor='admin',key=pause_key)['enabled'],False))
    s.check('reminder-pause-once-audit',['SR-06'],lambda:equal(sql(f"SELECT COUNT(*) FROM audit_event WHERE action='REMINDER_OPERATOR_PAUSE' AND actor_client='STAFF' AND object_id={pr['id']}"),'1'))
    s.check('reminder-pause-does-not-recall-platform-batches',['SR-07'],lambda:equal(sql('SELECT id,state FROM reminder_batch ORDER BY id'),batch_snapshot))
    paused=s.call('GET',api+'/rules/'+pr['id'],actor='alice')
    s.check('reminder-pause-already-paused',['SR-06'],lambda:s.call('POST',endpoint,{**body,'expectedVersion':paused['lockVersion']},actor='admin',status=409))
    s.check('reminder-customer-resumes-after-operator-pause',['SR-06'],lambda:equal(s.call('PUT',api+'/rules/'+pr['id'],{'targetType':'ARTICLE','targetId':pa['id'],'mode':'DAILY','enabled':True,'expectedVersion':paused['lockVersion']},actor='alice')['enabled'],True))
    s.check('reminder-admin-rule-title',['SR-06'],lambda:equal(next(x for x in s.call('GET',admin+'/rules',actor='admin')['items'] if x['id']==pr['id'])['title'],'运营暂停专项对象'))
    # Two distinct synthetic users: multiple modes overlap, paused rules and total switches do not inflate active users.
    original=s.call('GET',admin+'/statistics',actor='admin')
    s.r.sql("INSERT INTO app_user(id,nickname) VALUES(10103,'统计合成一'),(10104,'统计合成二'); INSERT INTO reminder_preference(user_id,enabled,slot) VALUES(10103,1,'00:00'),(10104,0,'00:00')")
    for rid,uid,mode,enabled in [(20101,10103,'DAILY',1),(20102,10103,'INSTANT',1),(20103,10104,'DAILY',1),(20104,10104,'INSTANT',0)]:
        s.r.sql(f"INSERT INTO reminder_tracking(id,user_id,target_type,target_id,mode,enabled) VALUES({rid},{uid},'ARTICLE',{rid},'{mode}',{enabled})")
    counts=s.call('GET',admin+'/statistics',actor='admin')
    s.check('reminder-statistics-distinct-users-and-mode-overlap',['SR-06'],lambda:equal({k:counts[k]-original[k] for k in ['trackedUsers','enabledUsers','dailyUsers','instantUsers']},{'trackedUsers':2,'enabledUsers':1,'dailyUsers':1,'instantUsers':1}))
    s.r.sql("UPDATE app_user SET status='DISABLED' WHERE id=10103")
    s.check('reminder-statistics-disabled-user-excluded',['SR-06'],lambda:equal(s.call('GET',admin+'/statistics',actor='admin')['enabledUsers'],original['enabledUsers']))
    s.r.sql("UPDATE app_user SET status='ACTIVE',deleted_at=UTC_TIMESTAMP() WHERE id=10103")
    s.check('reminder-statistics-deleted-user-excluded',['SR-06'],lambda:equal(s.call('GET',admin+'/statistics',actor='admin')['enabledUsers'],original['enabledUsers']))

    # Prove the scheduler itself, without invoking tick: only this owned API process restarts.
    auto=article('自动调度验收');due()
    s.r.apiproc.terminate();s.r.apiproc.wait(timeout=20)
    s.r.start_api(extra_args=['--yunji.reminder.scheduler-enabled=true','--yunji.reminder.poll-ms=1000','--yunji.file-cleanup.enabled=false'])
    def scheduler():
        deadline=time.monotonic()+50
        while time.monotonic()<deadline:
            if sql(f"SELECT COUNT(*) FROM reminder_batch b JOIN reminder_batch_item i ON i.batch_id=b.id JOIN reminder_change c ON c.id=i.change_id WHERE c.article_id={auto['id']} AND b.state='AWAITING_STAFF'")=='1':return
            time.sleep(1)
        raise AssertionError('自动调度没有创建待人工执行任务')
    s.check('reminder-scheduler-runs-without-admin-click',['SR-05','SR-07'],scheduler)
