"""Real HTTP/SQL acceptance for V20 discovery; no demo data or services."""
from http_suite import equal
from datetime import datetime, timezone, timedelta


def run(s):
    # Previous suites deliberately close/revoke users; use fresh sessions before this suite.
    s.login_staff('admin', 'admin123')
    s.login_customer('discovery', 91000000000000002)
    boards = s.call('GET', '/api/v1/catalog/categories')
    base = {'name': '验收标签', 'categoryId': boards[0]['id'], 'status': 'ENABLED', 'sortOrder': 0, 'expectedVersion': 0}
    label = s.call('POST', '/api/v1/admin/catalog/labels', base, actor='admin')
    child = s.call('POST', '/api/v1/admin/catalog/labels', {**base, 'name': '验收子标签', 'parentId': label['id']}, actor='admin')
    s.check('label-reject-cycle', [], lambda: s.call('PUT', '/api/v1/admin/catalog/labels/' + label['id'], {**base, 'parentId': child['id']}, actor='admin', status=422))
    s.check('label-reject-cross-board-parent', [], lambda: s.call('POST', '/api/v1/admin/catalog/labels', {**base, 'categoryId': boards[1]['id'], 'parentId': label['id']}, actor='admin', status=404))
    s.check('labels-staff-token-boundary', [], lambda: s.call('POST', '/api/v1/admin/catalog/labels', base, actor='discovery', status=401))
    draft = {'title': 'V20 搜索验收通知', 'richText': '<p>通知正文</p>', 'contentType': 'NOTICE', 'categoryIds': [boards[0]['id']], 'labelIds': [label['id']], 'keywordIds': [911]}
    s.check('article-reject-cross-board-label', [], lambda: s.call('POST', '/api/v1/admin/articles', {**draft, 'categoryIds': [boards[1]['id']]}, actor='admin', status=422))
    articles = []
    for i in range(2):
        a = s.call('POST', '/api/v1/admin/articles', {**draft, 'title': draft['title'] + str(i)}, actor='admin')
        for action in ['SUBMIT', 'APPROVE', 'PUBLISH']:
            a = s.call('POST', '/api/v1/admin/articles/' + a['id'] + '/transitions', {'action': action, 'versionId': a['latestVersionId'], 'expectedVersion': a['lockVersion']}, actor='admin')
        articles.append(a)
    path = '/api/v1/articles/search?contentType=NOTICE&labelIds=' + label['id'] + '&pageSize=1'
    first = s.call('GET', path)
    s.check('search-filter-and-paginate', [], lambda: equal(first['total'], 2))
    cursor = '&cursor=' + first['nextCursor'] + '&snapshotAt=' + first['snapshotAt']
    second = s.call('GET', path + cursor)
    s.check('search-pages-do-not-repeat', [], lambda: equal(len({first['items'][0]['id'], second['items'][0]['id']}), 2))
    s.check('search-cursor-binds-filter', [], lambda: s.call('GET', path.replace('NOTICE', 'GUIDE') + cursor, status=400))
    s.check('search-reject-unknown-type', [], lambda: s.call('GET', '/api/v1/articles/search?contentType=UNSAFE', status=422))
    s.check('label-keywords-from-published-articles', [], lambda: equal(any(k['id'] == '911' for k in s.call('GET', '/api/v1/catalog/labels/' + label['id'] + '/keywords')), True))
    a = articles[0]
    s.call('PUT', '/api/v1/me/follows/LABEL/' + label['id'], {'enabled': True}, actor='discovery')
    s.check('label-box-unread-exact-membership', [], lambda: equal(next(b for b in s.call('GET', '/api/v1/me/subscription-boxes', actor='discovery') if b['targetType'] == 'LABEL')['unreadCount'], 2))
    s.call('PUT', '/api/v1/me/read/' + a['id'], {'versionId': a['currentVersionId']}, actor='discovery')
    s.check('label-read-decrements-box', [], lambda: equal(next(b for b in s.call('GET', '/api/v1/me/subscription-boxes', actor='discovery') if b['targetType'] == 'LABEL')['unreadCount'], 1))
    s.check('summary-auth-required', [], lambda: s.call('GET', '/api/v1/me/discovery-summary', status=401))
    s.check('summary-count-real', [], lambda: equal(s.call('GET', '/api/v1/me/discovery-summary', actor='discovery')['unreadCount'] >= 1, True))
    for i in range(23):
        s.call('POST', '/api/v1/me/search-history', {'query': 'private-term-' + str(i)}, actor='discovery')
    s.check('search-history-bounded', [], lambda: equal(len(s.call('GET', '/api/v1/me/search-history', actor='discovery')), 20))
    s.check('hot-never-publishes-private-query', [], lambda: equal(any(k['name'].startswith('private-term-') for k in s.call('GET', '/api/v1/search/hot')), False))
    s.call('DELETE', '/api/v1/me/search-history', actor='discovery')
    s.check('search-history-clear', [], lambda: equal(s.call('GET', '/api/v1/me/search-history', actor='discovery'), []))
    s.call('PUT', '/api/v1/me/favorites/' + a['id'], {'enabled': True}, actor='discovery')
    s.call('POST', '/api/v1/me/history/' + a['id'], actor='discovery')
    s.call('DELETE', '/api/v1/me/history/' + a['id'], actor='discovery')
    s.check('history-individual-delete', [], lambda: equal(any(x['id'] == a['id'] for x in s.call('GET', '/api/v1/me/history', actor='discovery')), False))
    s.call('DELETE', '/api/v1/me/favorites', actor='discovery')
    s.check('favorites-clear', [], lambda: equal(s.call('GET', '/api/v1/me/favorites', actor='discovery'), []))
    s.check('retired-official-channel-rejected', [], lambda: s.call('POST', '/api/v1/me/notifications/rules', {'targetType': 'ARTICLE', 'targetId': a['id'], 'mode': 'INSTANT', 'channel': 'WECHAT_OFFICIAL', 'timezone': 'Asia/Shanghai', 'enabled': True}, actor='discovery', status=422))
    s.check('customers-admin-paging', [], lambda: equal(len(s.call('GET', '/api/v1/admin/customers?size=1', actor='admin')['items']), 1))
    s.check('customers-reject-customer-token', [], lambda: s.call('GET', '/api/v1/admin/customers', actor='discovery', status=401))
    s.check('reminder-capabilities-honest', [], lambda: equal(s.call('GET', '/api/v1/admin/reminders/capabilities', actor='admin')['realProviderReady'], False))
    # These are foundation endpoints, not substitute business APIs.
    for endpoint in ['/system/post/list', '/system/dict/type/list', '/system/dict/data/list', '/system/config/list', '/system/notice/list', '/system/client/list', '/monitor/cache', '/monitor/online/list', '/monitor/logininfor/list']:
        s.check('foundation-read-' + endpoint.replace('/', '-'), [], lambda endpoint=endpoint: s.call('GET', endpoint, actor='admin'))
