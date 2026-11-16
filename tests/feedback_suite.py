"""Latest client feedback: real DB/API behavior, ownership and negative boundaries."""
from concurrent.futures import ThreadPoolExecutor
import uuid
from http_suite import equal


def run(s):
    s.login_staff('admin', 'admin123')
    s.login_customer('feedback-alice', 91000000000000001)
    s.login_customer('feedback-bob', 91000000000000002)
    actor = 'feedback-alice'
    categories = s.call('GET', '/api/v1/catalog/categories')[:2]
    label = s.call('POST', '/api/v1/admin/catalog/labels', {'name': '反馈验收标签', 'categoryId': categories[0]['id'], 'status': 'ENABLED', 'sortOrder': 0}, actor='admin')
    keyword = s.call('POST', '/api/v1/admin/catalog/keywords', {'name': '反馈验收关键词', 'status': 'ENABLED', 'sortOrder': 0}, actor='admin')
    s.call('PUT', '/api/v1/admin/catalog/labels/' + label['id'] + '/keywords', {'keywordIds': [keyword['id']], 'expectedVersion': 0}, actor='admin')

    def draft(kind='BASIC', cat=None, presentation=None):
        return {'title': '反馈验收' + kind, 'summary': '反馈验收摘要', 'richText': '<p>反馈验收正文</p>', 'contentType': kind,
                'categoryIds': [cat or categories[0]['id']], 'labelIds': [] if cat else [label['id']],
                'keywordIds': [keyword['id']], 'presentation': presentation}

    def publish(a):
        for action in ['SUBMIT', 'APPROVE', 'PUBLISH']:
            a = s.call('POST', '/api/v1/admin/articles/' + a['id'] + '/transitions', {'action': action, 'versionId': a['latestVersionId'], 'expectedVersion': a['lockVersion']}, actor='admin')
        return a

    presentation = {'fit': '新生', 'duration': '10分钟', 'level': '简单', 'steps': [{'title': '准备材料', 'description': '先核实官方要求'}], 'tips': ['注意截止日期'], 'contacts': [], 'relatedArticleIds': []}
    guide = publish(s.call('POST', '/api/v1/admin/articles', draft('GUIDE', presentation=presentation), actor='admin'))
    basic = publish(s.call('POST', '/api/v1/admin/articles', draft('BASIC', categories[1]['id'], {'contacts': [{'label': '服务窗口', 'value': '一楼服务台'}], 'relatedArticleIds': [guide['id']]}), actor='admin'))
    s.check('feedback-three-type-guide-fields-versioned', [], lambda: equal(s.call('GET', '/api/v1/articles/' + guide['id'])['presentation']['steps'], presentation['steps']))
    s.check('feedback-basic-contact-and-related', [], lambda: equal((s.call('GET', '/api/v1/articles/' + basic['id'])['presentation']['contacts'][0]['value'], s.call('GET', '/api/v1/articles/' + basic['id'])['related'][0]['id']), ('一楼服务台', guide['id'])))
    s.check('feedback-presentation-invalid-step-rejected', [], lambda: s.call('POST', '/api/v1/admin/articles', draft(presentation={'steps': [{'title': '', 'description': '无标题'}]}), actor='admin', status=422))
    s.check('feedback-presentation-related-bounded', [], lambda: s.call('POST', '/api/v1/admin/articles', draft(presentation={'relatedArticleIds': [guide['id']] * 11}), actor='admin', status=422))
    s.check('feedback-presentation-null-step-rejected', [], lambda: s.call('POST', '/api/v1/admin/articles', draft(presentation={'steps': [None]}), actor='admin', status=422))
    root = '/api/v1/articles/search?q=反馈验收&includeExpired=false'
    boards = ','.join(c['id'] for c in categories)
    s.check('feedback-board-multiselect-or', [], lambda: equal(s.call('GET', root + '&categoryIds=' + boards)['total'], 2))
    s.check('feedback-board-multiselect-label-intersection', [], lambda: equal(s.call('GET', root + '&categoryIds=' + boards + '&labelIds=' + label['id'])['total'], 1))
    s.check('feedback-board-single-legacy-compatible', [], lambda: equal(s.call('GET', root + '&categoryId=' + categories[1]['id'])['total'], 1))
    s.check('feedback-board-negative-denied', [], lambda: s.call('GET', root + '&categoryIds=-1', status=422))
    s.check('feedback-board-list-bounded', [], lambda: s.call('GET', root + '&categoryIds=' + ','.join(str(i + 1) for i in range(31)), status=422))
    page = s.call('GET', root + '&categoryIds=' + boards + '&pageSize=1')
    s.check('feedback-cursor-binds-board-selection', [], lambda: s.call('GET', root + '&categoryIds=' + categories[0]['id'] + '&pageSize=1&cursor=' + page['nextCursor'] + '&snapshotAt=' + page['snapshotAt'], status=400))

    api = '/api/v1/me/reminders'
    payload = {'targetType': 'ARTICLE', 'targetId': basic['id']}
    s.check('feedback-quick-reminder-anonymous-denied', [], lambda: s.call('POST', api + '/daily', payload, status=401))
    s.check('feedback-quick-reminder-staff-denied', [], lambda: s.call('POST', api + '/daily', payload, actor='admin', status=401))
    before = s.call('GET', api + '/changes', actor=actor)['total']
    with ThreadPoolExecutor(max_workers=2) as pool:
        rules = list(pool.map(lambda _: s.call('POST', api + '/daily', payload, actor=actor), range(2)))
    rule = rules[0]
    s.check('feedback-quick-reminder-concurrent-one-rule', [], lambda: equal(rules[0]['id'], rules[1]['id']))
    s.check('feedback-quick-reminder-daily', [], lambda: equal(rule['mode'], 'DAILY'))
    s.check('feedback-quick-reminder-auto-follow', [], lambda: equal(any(f['targetType'] == 'ARTICLE' and f['targetId'] == basic['id'] for f in s.call('GET', '/api/v1/me/follows', actor=actor)), True))
    s.check('feedback-quick-reminder-does-not-fabricate-update', [], lambda: equal(s.call('GET', api + '/changes', actor=actor)['total'], before))
    s.check('feedback-reminder-user-ownership', [], lambda: s.call('GET', api + '/rules/' + rule['id'], actor='feedback-bob', status=404))
    s.call('PUT', api + '/rules/' + rule['id'], {**rule, 'enabled': False, 'expectedVersion': rule['lockVersion']}, actor=actor)
    s.check('feedback-reminder-pause-retains-follow', [], lambda: equal(any(f['targetId'] == basic['id'] for f in s.call('GET', '/api/v1/me/follows', actor=actor)), True))
    rule = s.call('POST', api + '/daily', payload, actor=actor)
    s.check('feedback-reminder-restores-same-rule', [], lambda: equal((rule['id'], rule['enabled']), (rules[0]['id'], True)))
    s.call('PUT', '/api/v1/me/follows/ARTICLE/' + basic['id'], {'enabled': False}, actor=actor)
    s.check('feedback-unfollow-pauses-rule', [], lambda: equal(s.call('GET', api + '/rules/' + rule['id'], actor=actor)['enabled'], False))
    s.check('feedback-invalid-target-atomic-no-follow', [], lambda: s.call('POST', api + '/daily', {'targetType': 'ARTICLE', 'targetId': '999999999999'}, actor=actor, status=404))
    s.call('PUT', '/api/v1/me/follows/KEYWORD/' + keyword['id'], {'enabled': True}, actor=actor)
    s.check('feedback-keyword-follow-creates-daily', [], lambda: equal(s.call('GET', api + '/rules?targetType=KEYWORD&targetId=' + keyword['id'], actor=actor)['items'][0]['mode'], 'DAILY'))
    s.check('feedback-keyword-three-level-membership', [], lambda: equal(next(f for f in s.call('GET', '/api/v1/me/follows', actor=actor) if f['targetType'] == 'KEYWORD' and f['targetId'] == keyword['id'])['labels'][0]['categoryId'], categories[0]['id']))
    newer = publish(s.call('POST', '/api/v1/admin/articles', {**draft('NOTICE'), 'title': '反馈验收新增通知'}, actor='admin'))
    s.check('feedback-keyword-new-content-real-count', [], lambda: equal(next(k for k in s.call('GET', api + '/keywords', actor=actor) if k['id'] == keyword['id'])['latestArticleId'], newer['id']))
    s.check('feedback-daily-real-update-visible', [], lambda: equal(any(c['articleId'] == newer['id'] for c in s.call('GET', api + '/changes?mode=DAILY', actor=actor)['items']), True))
    boxes = s.call('GET', '/api/v1/me/subscription-boxes', actor=actor)
    item = next(i for b in boxes if b['targetType'] == 'KEYWORD' and b['targetId'] == keyword['id'] for i in b['items'] if i['id'] == newer['id'])
    s.check('feedback-box-type-and-publication-time', [], lambda: equal((item['contentType'], bool(item['publishedAt'])), ('NOTICE', True)))
    s.call('PUT', '/api/v1/me/follows/KEYWORD/' + keyword['id'], {'enabled': False}, actor=actor)
    s.check('feedback-keyword-unfollow-removes-summary', [], lambda: equal(any(k['id'] == keyword['id'] for k in s.call('GET', api + '/keywords', actor=actor)), False))
    s.check('feedback-daily-no-disabled-rule-updates', [], lambda: equal(any(c['articleId'] == newer['id'] for c in s.call('GET', api + '/changes?mode=DAILY', actor=actor)['items']), False))

    # Quick opt-in must not downgrade existing instant mode or mint extra consent.
    s.call('POST', '/api/local/reminders/authorization', {'receiptKey': uuid.uuid4().hex, 'wecomBound': True, 'amount': 1, 'outcome': 'SUCCESS'}, actor=actor, headers={'X-Local-Secret': s.r.local_secret})
    rule = s.call('GET', api + '/rules/' + rule['id'], actor=actor)
    rule = s.call('PUT', api + '/rules/' + rule['id'], {**rule, 'mode': 'INSTANT', 'enabled': True, 'expectedVersion': rule['lockVersion']}, actor=actor)
    s.check('feedback-quick-retains-instant-mode', [], lambda: equal(s.call('POST', api + '/daily', payload, actor=actor)['mode'], 'INSTANT'))
    s.check('feedback-quick-does-not-mint-consent', [], lambda: equal(s.call('GET', api + '/settings', actor=actor)['permissions'][0]['available'], 1))
    retired = publish(s.call('POST', '/api/v1/admin/articles', {**draft(), 'title': '反馈撤下测试'}, actor='admin'))
    retired_rule = s.call('POST', api + '/daily', {'targetType': 'ARTICLE', 'targetId': retired['id']}, actor=actor)
    s.call('POST', '/api/v1/admin/articles/' + retired['id'] + '/transitions', {'action': 'OFFLINE', 'versionId': retired['currentVersionId'], 'expectedVersion': retired['lockVersion']}, actor='admin')
    s.check('feedback-quick-existing-offline-target-denied', [], lambda: s.call('POST', api + '/daily', {'targetType': 'ARTICLE', 'targetId': retired['id']}, actor=actor, status=404))

    topics = '/api/v1/admin/operation/topics'
    topic_body = {'title': '反馈独立活动专题', 'subtitle': '独立于板块', 'description': '<p>专题介绍</p><script>bad()</script>', 'status': 'DRAFT', 'sortOrder': 0, 'expectedVersion': 0, 'articleIds': [basic['id'], guide['id']]}
    s.check('feedback-topic-admin-anonymous-denied', [], lambda: s.call('POST', topics, topic_body, status=401))
    s.check('feedback-topic-admin-customer-denied', [], lambda: s.call('POST', topics, topic_body, actor=actor, status=401))
    topic = s.call('POST', topics, topic_body, actor='admin')
    s.check('feedback-draft-topic-not-public', [], lambda: s.call('GET', '/api/v1/topics/' + topic['id'], status=404))
    topic = s.call('PUT', topics + '/' + topic['id'], {**topic_body, 'status': 'PUBLISHED'}, actor='admin')
    public = s.call('GET', '/api/v1/topics/' + topic['id'])
    s.check('feedback-topic-independent-ordered-articles', [], lambda: equal([i['id'] for i in public['items']], [basic['id'], guide['id']]))
    s.check('feedback-topic-description-sanitized', [], lambda: equal('<script>' in public['description'], False))
    s.check('feedback-topic-stale-edit-denied', [], lambda: s.call('PUT', topics + '/' + topic['id'], topic_body, actor='admin', status=409))
    s.check('feedback-topic-duplicate-articles-denied', [], lambda: s.call('POST', topics, {**topic_body, 'articleIds': [guide['id'], guide['id']]}, actor='admin', status=422))
    s.check('feedback-topic-null-article-denied', [], lambda: s.call('POST', topics, {**topic_body, 'articleIds': [None]}, actor='admin', status=422))
    s.check('feedback-topic-invalid-image-denied', [], lambda: s.call('POST', topics, {**topic_body, 'imageUrl': 'javascript:alert(1)'}, actor='admin', status=422))
    banner = s.call('POST', '/api/v1/admin/operation/banners', {'title': '反馈专题轮播', 'imageUrl': 'https://example.invalid/synthetic.png', 'linkType': 'TOPIC', 'linkValue': topic['id'], 'status': 'PUBLISHED', 'sortOrder': 0}, actor='admin')
    s.check('feedback-banner-links-real-topic', [], lambda: equal(any(b['id'] == banner['id'] for b in s.call('GET', '/api/v1/home')['banners']), True))
    s.call('DELETE', topics + '/' + topic['id'] + '?expectedVersion=' + str(topic['lockVersion']), actor='admin')
    s.check('feedback-topic-offline-hides-banner', [], lambda: equal(any(b['id'] == banner['id'] for b in s.call('GET', '/api/v1/home')['banners']), False))
    s.check('feedback-topic-offline-deeplink-denied', [], lambda: s.call('GET', '/api/v1/topics/' + topic['id'], status=404))
