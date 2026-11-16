"""Real MySQL snapshot under publish/revise/offline mutations, not mocked sorting tests."""
from urllib.parse import urlencode
from http_suite import equal


def run(s):
    # Separate deterministic synthetic fixtures, never borrowed demo content/schema.
    statements = []
    ids = [92000000000000000 + n for n in range(1000)]
    for n, aid in enumerate(ids):
        statements.append(f"INSERT INTO article(id,tenant_id,title,summary,source_name,content_type,status,visibility,created_by) VALUES({aid},'000000','快照验收{n}','快照验收','合成快照','BASIC','DRAFT','PUBLIC',1)")
        statements.append(f"INSERT INTO article_version(id,article_id,version_no,title,relation_snapshot) VALUES({aid+20000},{aid},1,'快照验收{n}',JSON_OBJECT())")
        statements.append(f"UPDATE article SET status='PUBLISHED',published_at=UTC_TIMESTAMP()-INTERVAL {1001-n} SECOND,current_version_id={aid+20000} WHERE id={aid}")
    s.r.sql(';'.join(statements) + ';')
    first = s.call('GET', '/api/v1/articles?' + urlencode({'q': '快照验收', 'pageSize': 37}))
    seen = [x['id'] for x in first['items']]
    snapshot = first['snapshotAt']; cursor = first['nextCursor']
    revised = ids[50]; hidden = ids[100]
    new = 92000000000099999
    s.r.sql(f"UPDATE article SET published_at=UTC_TIMESTAMP(),title='快照验收更新' WHERE id={revised}; UPDATE article SET status='OFFLINE' WHERE id={hidden}; INSERT INTO article(id,tenant_id,title,summary,source_name,content_type,status,visibility,created_by) VALUES({new},'000000','快照验收新发布','快照验收','合成','BASIC','DRAFT','PUBLIC',1); INSERT INTO article_version(id,article_id,version_no,title,relation_snapshot) VALUES({new+1},{new},1,'快照验收新发布',JSON_OBJECT()); UPDATE article SET status='PUBLISHED',published_at=UTC_TIMESTAMP(),current_version_id={new+1} WHERE id={new};")
    s.check('snapshot-query-change-rejected', ['DIS-01'], lambda: s.call('GET', '/api/v1/articles?' + urlencode({'q':'改动过滤','pageSize':37,'snapshotAt':snapshot,'cursor':cursor}), status=400))
    s.check('snapshot-invalid-cursor-rejected', ['DIS-01'], lambda: s.call('GET','/api/v1/articles?cursor=invalid',status=400))
    def collect():
        nonlocal cursor
        while cursor:
            page = s.call('GET', '/api/v1/articles?' + urlencode({'q':'快照验收','pageSize':37,'snapshotAt':snapshot,'cursor':cursor}))
            seen.extend(x['id'] for x in page['items']); cursor = page['nextCursor']
        equal(len(seen), 999); equal(len(set(seen)), 999)
        equal(seen, [str(x) for x in reversed(ids) if x != hidden])
        assert str(new) not in seen
    s.check('snapshot-1000-concurrent-publish-revise-offline', ['DIS-01','DIS-02','DIS-03'], collect)
    s.r.sql("UPDATE article_feed_snapshot SET expires_at=UTC_TIMESTAMP()-INTERVAL 1 SECOND")
    s.check('snapshot-expiration-refresh', ['DIS-01'], lambda: s.call('GET','/api/v1/articles?' + urlencode({'q':'快照验收','pageSize':37,'snapshotAt':snapshot,'cursor':first['nextCursor']}),status=409))
    s.r.sql("UPDATE article SET status='OFFLINE' WHERE id IN (" + ','.join(map(str,ids+[new])) + ')')
