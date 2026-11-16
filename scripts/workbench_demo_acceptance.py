"""Prove the default manual workbench has useful, isolated and repeat-safe demo content."""
import datetime
import json
import sys
from urllib.request import urlopen
from local_runtime import LocalRuntime, ROOT
from workbench_demo import seed
from workbench import Frontends
from evidence import application_fingerprints, source_hash


def main():
    runtime = LocalRuntime()
    frontends = Frontends(runtime.work)
    report = {'scope': 'LOCAL_WORKBENCH_DEMO', 'startedAt': datetime.datetime.now(datetime.timezone.utc).isoformat(), 'sourceFingerprints': application_fingerprints(), 'testSourceSha256': source_hash('tests', 'scripts'), 'cases': []}
    def check(name, fn):
        fn()
        report['cases'].append({'id': name, 'status': 'PASS'})
        print(name, 'PASS', flush=True)
    def get(path):
        with urlopen(runtime.url + path) as response:
            return json.load(response)['data']
    def require(value):
        if not value:
            raise AssertionError('演示验收条件未满足')
    try:
        runtime.start_database();runtime.migrate();runtime.seed();runtime.start_redis();runtime.start_api()
        values = seed(runtime)
        check('demo-six-articles-three-types', lambda: require(get('/api/v1/articles/search?includeExpired=false')['total'] == 6 and {get('/api/v1/articles/' + a)['article']['contentType'] for a in values['articleIds']} == {'NOTICE', 'GUIDE', 'BASIC'}))
        def taxonomy():
            detail = get('/api/v1/articles/' + values['articleIds'][0])
            require(len(detail['categories']) == 1 and len(detail['labels']) == 1 and len(detail['keywords']) == 1)
            require(detail['categories'][0]['id'] == get('/api/v1/catalog/categories')[0]['id'])
            require(detail['labels'][0]['name'] == '演示·办理指南' and detail['keywords'][0]['name'] == '演示·办理指南')
        check('demo-article-detail-exposes-board-label-keyword', taxonomy)
        check('demo-three-visible-topic-banners', lambda: require(len(get('/api/v1/home')['banners']) == 3 and all(b['linkType'] == 'TOPIC' for b in get('/api/v1/home')['banners'])))
        check('demo-topics-contain-visible-articles', lambda: require(all(get('/api/v1/topics/' + t)['items'] for t in values['topicIds'])))
        check('demo-repeat-does-not-duplicate', lambda: require(seed(runtime) == values and get('/api/v1/articles/search?includeExpired=false')['total'] == 6 and len(get('/api/v1/home')['banners']) == 3))
        check('demo-no-fabricated-notification-send', lambda: require(runtime.sql('SELECT COUNT(*) FROM reminder_batch').stdout.strip() == '0'))
        frontends.start()
        def images():
            for image in values['imagePaths']:
                with urlopen('http://127.0.0.1:5173' + image) as response:
                    require(response.headers['Content-Type'].startswith('image/png') and response.read().startswith(b'\x89PNG'))
        check('demo-images-served-without-external-network', images)
        def browser():
            import subprocess, shutil
            from local_runtime import FLAGS
            result = subprocess.run([shutil.which('node'), 'browser_workbench_demo.cjs'], cwd=ROOT/'tests', creationflags=FLAGS, capture_output=True, text=True, encoding='utf-8', errors='replace')
            log = runtime.work / 'demo-browser.log'
            log.write_text(result.stdout + result.stderr, encoding='utf-8')
            if result.returncode != 0:
                raise AssertionError(f'浏览器验收失败，详见 {log}:\n{(result.stdout + result.stderr)[-6000:]}')
        check('demo-default-home-carousel-and-detail-browser', browser)
    except Exception as error:
        report['fatal'] = str(error)
        print(str(error), file=sys.stderr)
    finally:
        frontends.close();runtime.close()
        report.update(completedAt=datetime.datetime.now(datetime.timezone.utc).isoformat(), sourceUnchanged=report['sourceFingerprints'] == application_fingerprints() and report['testSourceSha256'] == source_hash('tests', 'scripts'), logDirectory=str(runtime.work))
        report['passed'] = len(report['cases']);report['failed'] = int(bool(report.get('fatal')))
        (ROOT/'reports/workbench-demo.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    return report['failed']


if __name__ == '__main__':
    raise SystemExit(main())
