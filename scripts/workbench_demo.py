"""Opt-in synthetic editorial fixtures; reachable only through the disposable local workbench."""
import datetime
import json
import uuid
from urllib.parse import urlparse
from urllib.request import Request, urlopen


def seed(runtime):
    base = runtime.url
    if urlparse(base).hostname not in {'127.0.0.1', 'localhost'} or not runtime.local_secret:
        raise RuntimeError('演示数据仅允许写入隔离本地工作台')
    receipt = runtime.work / 'editorial-demo.json'
    if receipt.exists():
        return json.loads(receipt.read_text(encoding='utf-8'))

    def call(method, path, body=None, token=None, secret=False):
        headers = {'Content-Type': 'application/json', 'clientid': 'e5cd7e4891bf95d1d19206ce24a7b32e', 'Idempotency-Key': uuid.uuid4().hex}
        if token:
            headers['Authorization'] = 'Bearer ' + token
        if secret:
            headers['X-Local-Secret'] = runtime.local_secret
        with urlopen(Request(base + path, json.dumps(body).encode() if body is not None else None, headers, method=method), timeout=20) as response:
            result = json.load(response)
        if result['code'] != 200:
            raise RuntimeError(result.get('msg', '演示内容创建失败'))
        return result.get('data')

    # A secret-protected local endpoint proves this is not a production API.
    issued = call('POST', '/api/local/login-codes', {'userId': '91000000000000001', 'appId': 'local-client'}, secret=True)
    customer = call('POST', '/api/v1/auth/customer/local', {'appId': 'local-client', 'code': issued['code']})['accessToken']
    call('POST', '/api/v1/auth/customer/logout', {}, customer)
    staff = call('POST', '/auth/login', {'username': 'admin', 'password': 'admin123', 'tenantId': '000000', 'clientId': 'e5cd7e4891bf95d1d19206ce24a7b32e', 'grantType': 'password'})['access_token']
    try:
        categories = call('GET', '/api/v1/catalog/categories')
        labels = []
        keywords = []
        for i, name in enumerate(['办理指南', '时间安排', '功能体验']):
            label = call('POST', '/api/v1/admin/catalog/labels', {'name': '演示·' + name, 'categoryId': categories[i % len(categories)]['id'], 'status': 'ENABLED', 'sortOrder': i}, staff)
            keyword = call('POST', '/api/v1/admin/catalog/keywords', {'name': '演示·' + name, 'status': 'ENABLED', 'sortOrder': i}, staff)
            call('PUT', '/api/v1/admin/catalog/labels/' + label['id'] + '/keywords', {'keywordIds': [keyword['id']], 'expectedVersion': 0}, staff)
            labels.append(label)
            keywords.append(keyword)
        now = datetime.datetime.now(datetime.timezone.utc)
        articles = []
        specs = [
            ('GUIDE', '新用户浏览与关注指南', 'guide', {'fit': '首次试用云迹的验收人员', 'duration': '约5分钟', 'level': '入门', 'steps': [{'title': '浏览和筛选', 'description': '查看首页，尝试通知、攻略、普通信息筛选，再进入板块选择标签。'}, {'title': '关注与提醒', 'description': '登录合成用户后，关注资讯或关键词，尝试开启日常强提醒。'}, {'title': '核对保存结果', 'description': '刷新页面，在管理关注、信息盒子和强提醒中核对真实状态。'}], 'tips': ['全部为合成测试资料，不代表学校或机构正式安排。', '本地模拟消息不会真实发送。']}),
            ('NOTICE', '体验活动报名时间示例', 'notice', {}),
            ('BASIC', '云迹功能体验入口说明', 'welcome', {'contacts': [{'label': '演示咨询入口', 'value': '请联系项目验收负责人（无真实联系方式）'}]}),
            ('GUIDE', '如何查看资料与原始来源', 'guide', {'fit': '资料和内容验收人员', 'duration': '约3分钟', 'level': '入门', 'steps': [{'title': '阅读正文', 'description': '核对正文与摘要；展开原始来源查看独立原文。'}, {'title': '查看相关内容', 'description': '使用返回和底部导航继续浏览，避免把示例当真实政策。'}], 'tips': ['正式资料需运营人员上传并发布。']}),
            ('NOTICE', '即将生效的信息展示示例', 'notice', {}),
            ('BASIC', '信息盒子与关注管理说明', 'welcome', {'contacts': [{'label': '测试提示', 'value': '关注后查看信息盒子，左滑后确认已读。'}]}),
        ]
        for i, (kind, title, image, presentation) in enumerate(specs):
            group = i % 3
            if kind == 'BASIC':
                presentation['relatedArticleIds'] = [articles[0]['id']]
            body = {'title': '【验收演示】' + title, 'summary': '合成测试内容，用于体验页面、筛选、关注和提醒；不构成真实通知。', 'richText': '<p>这是一条明确标记的验收演示资讯。</p><p>请使用本条内容检查排版、详情模板、关注状态和返回流程。时间、对象和流程均为合成数据。</p>', 'originalText': '合成验收原文。该内容由新工程本地演示脚本创建，未使用旧demo业务数据。', 'sourceName': '云迹本地验收', 'contentType': kind, 'categoryIds': [categories[group % len(categories)]['id']], 'labelIds': [labels[group]['id']], 'keywordIds': [keywords[group]['id']], 'presentation': presentation}
            if kind == 'NOTICE':
                body['effectiveFrom'] = (now + datetime.timedelta(hours=12 if i == 4 else -1)).isoformat()
                body['effectiveTo'] = (now + datetime.timedelta(days=3 if i == 4 else 2)).isoformat()
            body.update({'hotDays': 7, 'longImageUrl': 'http://127.0.0.1:5173/static/testing/guide.png' if kind == 'GUIDE' else None})
            article = call('POST', '/api/v1/admin/articles', body, staff)
            articles.append(article)
            if i != 4:
                call('POST', '/api/v1/admin/operation/recommendations', {'articleId': article['id'], 'coverUrl': '/static/testing/' + image + '.png', 'status': 'PUBLISHED', 'sortOrder': i}, staff)
        topics = []
        banners = []
        for i, (title, image) in enumerate([('开始体验云迹', 'welcome'), ('办理攻略与资料', 'guide'), ('通知与时间安排', 'notice')]):
            # H5 uses loopback; a remote device uses its separately configured test asset origin.
            image_url = 'http://127.0.0.1:5173/static/testing/' + image + '.png'
            topic = call('POST', '/api/v1/admin/operation/topics', {'title': '【验收演示】' + title, 'subtitle': '合成专题 · 非正式发布', 'imageUrl': image_url, 'description': '<p>用于测试轮播到独立专题再到资讯详情的完整流程。</p>', 'articleIds': [articles[j]['id'] for j in [[0, 2, 5], [0, 3], [1, 4]][i]], 'status': 'PUBLISHED', 'sortOrder': i, 'expectedVersion': 0}, staff)
            banner = call('POST', '/api/v1/admin/operation/banners', {'title': '【验收演示】' + title, 'imageUrl': image_url, 'linkType': 'TOPIC', 'linkValue': topic['id'], 'status': 'PUBLISHED', 'sortOrder': i}, staff)
            topics.append(topic['id'])
            banners.append(banner['id'])
        result = {'scope': 'LOCAL_SYNTHETIC_EDITORIAL_DEMO', 'articleIds': [a['id'] for a in articles], 'topicIds': topics, 'bannerIds': banners, 'imagePaths': ['/static/testing/' + n + '.png' for n in ['welcome', 'guide', 'notice']]}
        receipt.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
        return result
    finally:
        call('POST', '/auth/logout', {}, staff)


if __name__ == '__main__':
    from types import SimpleNamespace
    from workbench import state
    work, data = state()
    seed(SimpleNamespace(url=data['api'], local_secret=data['localSecret'], work=work))
    print('演示内容已就绪：6篇资讯、3个专题、3张轮播；仅当前隔离测试库。')
