"""Real HTTP / SQL checks for the reviewed prototype adaptations."""
from http_suite import equal


def run(s):
    s.login_staff('admin', 'admin123')
    s.login_customer('prototype', 91000000000000002)
    category = s.call('GET', '/api/v1/catalog/categories')[0]['id']
    labels = [s.call('POST', '/api/v1/admin/catalog/labels', {
        'name': '原型验收标签' + str(i), 'categoryId': category,
        'status': 'ENABLED', 'sortOrder': i, 'expectedVersion': 0
    }, actor='admin') for i in range(2)]
    keyword = s.call('POST', '/api/v1/admin/catalog/keywords', {
        'name': '原型验收关键词', 'displayName': '原型验收关键词',
        'status': 'ENABLED', 'expectedVersion': 0
    }, actor='admin')
    articles = []
    for index, kind in enumerate(['NOTICE', 'GUIDE', 'BASIC']):
        a = s.call('POST', '/api/v1/admin/articles', {
            'title': '原型组合筛选' + str(index), 'summary': '真实筛选摘要',
            'richText': '<p>真实正文，不是原文</p>', 'originalText': '独立的原文段落',
            'contentType': kind, 'categoryIds': [category],
            'labelIds': [labels[index % 2]['id']], 'keywordIds': [911 if index == 0 else keyword['id']]
        }, actor='admin')
        for action in ['SUBMIT', 'APPROVE', 'PUBLISH']:
            a = s.call('POST', '/api/v1/admin/articles/' + a['id'] + '/transitions', {
                'action': action, 'versionId': a['latestVersionId'], 'expectedVersion': a['lockVersion']
            }, actor='admin')
        articles.append(a)
    root = '/api/v1/articles/search?q=原型组合筛选'
    s.check('prototype-content-types-or', [], lambda: equal(s.call('GET', root + '&contentType=NOTICE,GUIDE')['total'], 2))
    s.check('prototype-content-types-single-compatible', [], lambda: equal(s.call('GET', root + '&contentType=BASIC')['total'], 1))
    s.check('prototype-content-types-dedupe', [], lambda: equal(s.call('GET', root + '&contentType=NOTICE,NOTICE')['total'], 1))
    for value in ['NOTICE,', ',NOTICE', 'NOTICE,BOGUS', '']:
        s.check('prototype-type-invalid-' + (value or 'empty'), [], lambda value=value: s.call('GET', root + '&contentType=' + value, status=422))
    s.check('prototype-keywords-or', [], lambda: equal(s.call('GET', root + '&keywordIds=911,' + keyword['id'])['total'], 3))
    s.check('prototype-keywords-intersect-label', [], lambda: equal(s.call('GET', root + '&labelIds=' + labels[0]['id'] + '&keywordIds=' + keyword['id'])['total'], 1))
    s.check('prototype-keywords-reject-negative', [], lambda: s.call('GET', root + '&keywordIds=-1', status=422))
    s.check('prototype-keywords-bounded', [], lambda: s.call('GET', root + '&keywordIds=' + ','.join(str(i+1) for i in range(31)), status=422))
    page = s.call('GET', root + '&contentType=NOTICE,GUIDE&pageSize=1')
    next_page = '&cursor=' + page['nextCursor'] + '&snapshotAt=' + page['snapshotAt']
    s.check('prototype-multitype-snapshot-no-repeat', [], lambda: equal(
        len({page['items'][0]['id'], s.call('GET', root + '&contentType=NOTICE,GUIDE&pageSize=1' + next_page)['items'][0]['id']}), 2))
    s.check('prototype-cursor-binds-keywords', [], lambda: s.call('GET', root + '&contentType=NOTICE,GUIDE&pageSize=1&keywordIds=911' + next_page, status=400))
    d = s.call('GET', '/api/v1/articles/' + articles[0]['id'])
    s.check('prototype-original-separate-from-body', [], lambda: equal(d['originalText'], '独立的原文段落'))
    s.check('prototype-detail-real-labels', [], lambda: equal([l['id'] for l in d['labels']], [labels[0]['id']]))
    s.check('prototype-detail-real-keywords', [], lambda: equal([k['id'] for k in d['keywords']], ['911']))
    s.call('PUT', '/api/v1/me/follows/KEYWORD/911', {'enabled': True}, actor='prototype')
    s.check('prototype-follow-groups-real-membership', [], lambda: equal(any(l['id'] == labels[0]['id'] for f in s.call('GET', '/api/v1/me/follows', actor='prototype') if f['targetType'] == 'KEYWORD' and f['targetId'] == '911' for l in f['labels']), True))
    batch = {'labelIds': [l['id'] for l in labels], 'enabled': True}
    s.check('prototype-bulk-labels-anonymous-denied', [], lambda: s.call('PUT', '/api/v1/me/follows/labels', batch, status=401))
    s.check('prototype-bulk-labels-empty-rejected', [], lambda: s.call('PUT', '/api/v1/me/follows/labels', {'labelIds': [], 'enabled': True}, actor='prototype', status=422))
    s.call('PUT', '/api/v1/me/follows/labels', batch, actor='prototype')
    s.check('prototype-bulk-labels-save', [], lambda: equal(len([f for f in s.call('GET', '/api/v1/me/follows', actor='prototype') if f['targetType'] == 'LABEL' and f['targetId'] in batch['labelIds']]), 2))
    s.call('PUT', '/api/v1/me/follows/labels', {**batch, 'enabled': False}, actor='prototype')
    s.check('prototype-bulk-labels-atomic-rollback', [], lambda: s.call('PUT', '/api/v1/me/follows/labels', {'labelIds': [labels[0]['id'], '999999999999'], 'enabled': True}, actor='prototype', status=404))
    s.check('prototype-bulk-labels-no-partial-save', [], lambda: equal(any(f['targetType'] == 'LABEL' and f['targetId'] == labels[0]['id'] for f in s.call('GET', '/api/v1/me/follows', actor='prototype')), False))
    r = s.call('POST', '/api/v1/me/reminders/rules', {'targetType': 'ARTICLE', 'targetId': articles[0]['id'], 'mode': 'DAILY', 'enabled': True, 'expectedVersion': 0}, actor='prototype')
    path = '/api/v1/me/reminders/rules?targetType=ARTICLE&targetId=' + articles[0]['id']
    s.check('prototype-reminder-object-filter-total', [], lambda: equal(s.call('GET', path, actor='prototype')['total'], 1))
    s.check('prototype-reminder-object-filter-exact', [], lambda: equal(s.call('GET', path, actor='prototype')['items'][0]['id'], r['id']))
    s.check('prototype-reminder-object-type-required', [], lambda: s.call('GET', '/api/v1/me/reminders/rules?targetId=' + articles[0]['id'], actor='prototype', status=422))
    s.check('prototype-reminder-object-filter-anonymous', [], lambda: s.call('GET', path, status=401))
    ticket = s.call('POST', '/api/v1/me/feedback', {'description': '原型反馈校验', 'type': 'REMINDER', 'contact': 'synthetic@example.invalid'}, actor='prototype')
    row = next(r for r in s.call('GET', '/api/v1/me/feedback', actor='prototype') if r['id'] == ticket['id'])
    s.check('prototype-feedback-type-persisted', [], lambda: equal(row['type'], 'REMINDER'))
    s.check('prototype-feedback-contact-explicit-unverified', [], lambda: equal('未验证' in row['description'] and 'synthetic@example.invalid' in row['description'], True))
    s.check('prototype-feedback-invalid-type', [], lambda: s.call('POST', '/api/v1/me/feedback', {'description': 'bad type', 'type': 'ROOT'}, actor='prototype', status=422))
