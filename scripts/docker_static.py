"""Reproducible template checks. Does not run Docker or certify Linux deployment."""
import datetime,json
import yaml
from evidence import ROOT,source_hash

def main():
    report={'generatedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'scope':'STATIC_ONLY_NO_DOCKER_ENGINE','deploySha256':source_hash('deploy'),'cases':[],'dockerImageBuilt':False,'linuxRuntimeVerified':False}
    def check(name,fn):
        try:
            assert fn();report['cases'].append({'id':name,'status':'PASS'})
        except Exception as e:report['cases'].append({'id':name,'status':'FAIL','error':str(e)})
    services=yaml.safe_load((ROOT/'deploy/docker/compose.yaml').read_text(encoding='utf-8'))['services']
    check('compose-amd64',lambda:all(s['platform']=='linux/amd64' for s in services.values()))
    check('api-no-host-port',lambda:not services['api'].get('ports'))
    check('api-private-persistent-files',lambda:services['api']['read_only'] and any(v.endswith(':/var/lib/yunji/files') for v in services['api']['volumes']))
    check('web-loopback-port',lambda:all(p.startswith('127.0.0.1:') for p in services['web']['ports']))
    check('dockerfiles-resolve',lambda:all((ROOT/'deploy/docker'/s['build']['context']/s['build']['dockerfile']).resolve().is_file() for s in services.values()))
    check('actual-artifacts-present',lambda:all((ROOT/p).is_file() for p in ['backend/agri-admin/target/agri-admin.jar','apps/admin/dist/index.html','apps/client/dist/build/h5/index.html']))
    report['passed']=sum(c['status']=='PASS' for c in report['cases']);report['failed']=len(report['cases'])-report['passed']
    (ROOT/'reports/docker-static.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report));return bool(report['failed'])

if __name__=='__main__':raise SystemExit(main())
