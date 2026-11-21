"""Isolated empty-database verification for the October 13 reminder rebuild."""
import sys,json,datetime,traceback,hashlib
from local_runtime import LocalRuntime,ROOT
from evidence import fingerprints
sys.path.insert(0,str(ROOT/'tests'))
from http_suite import Suite
from reminder_rebuild_suite import run

r=LocalRuntime();report={'scope':'REMINDER_REBUILD_LOCAL','releaseEligible':False,'startedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'sourceFingerprints':fingerprints(),'cases':[]}
try:
    r.start_database()
    # Exercise an installed V20 database, then apply V21 without rewriting history.
    r.sql("CREATE TABLE schema_history(version VARCHAR(80) PRIMARY KEY,sha256 CHAR(64) NOT NULL,installed_at DATETIME DEFAULT CURRENT_TIMESTAMP)")
    sources=[ROOT/'database/framework-schema.sql']+sorted((ROOT/'database/migrations').glob('V*__*.sql'),key=lambda p:int(p.name.split('__')[0][1:]))
    for source in sources:
        if source.name.startswith('V') and int(source.name.split('__')[0][1:])>=21:continue
        r.sql(source.read_text(encoding='utf-8'))
        r.sql(f"INSERT INTO schema_history(version,sha256) VALUES('{source.name}','{hashlib.sha256(source.read_bytes()).hexdigest()}')")
    r.seed();before=r.sql('SELECT COUNT(*) FROM app_user').stdout
    r.migrate();r.migrate()
    assert r.sql('SELECT COUNT(*) FROM app_user').stdout==before
    report['upgradeFromV20']=True
    r.start_redis();r.start_api()
    s=Suite(r);report['cases']=s.results
    for name,password in [('admin','admin123'),('test1','666666'),('test','666666')]:s.login_staff(name,password)
    s.login_customer('alice',91000000000000001);s.login_customer('bob',91000000000000002)
    run(s)
except Exception as e:
    traceback.print_exc();report['fatal']=str(e)
finally:
    r.close();report['logDirectory']=str(r.work)
    report['completedAt']=datetime.datetime.now(datetime.timezone.utc).isoformat()
    report['passed']=sum(c['status']=='PASS' for c in report['cases']);report['failed']=sum(c['status']=='FAIL' for c in report['cases'])+bool(report.get('fatal'))
    report['sourceUnchanged']=report['sourceFingerprints']==fingerprints()
    (ROOT/'reports/reminder-rebuild.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:report[k] for k in ['passed','failed','logDirectory']}),flush=True)
sys.exit(1 if report['failed'] else 0)
