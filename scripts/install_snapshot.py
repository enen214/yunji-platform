"""Generate and round-trip an empty installation from immutable migrations, never from user data."""
import datetime,hashlib,json,subprocess
from local_runtime import LocalRuntime,ROOT,FLAGS
from database_ops import Mysql,migrate,sources

def dump(db,options):
 result=subprocess.run(db.command('mysqldump',False)+['--skip-comments','--no-tablespaces','--set-gtid-purged=OFF','--skip-add-locks','--skip-disable-keys','--skip-add-drop-table',*options,db.database],env=db.env(),capture_output=True,creationflags=FLAGS,timeout=180)
 if result.returncode:raise RuntimeError(result.stderr.decode('utf-8',errors='replace'))
 return result.stdout.decode('utf-8').replace('\r\n','\n').rstrip()+'\n'

def main():
 r=LocalRuntime(33310,18090);r.redisport=16381;directory=ROOT/'database/install';report={'scope':'EMPTY_INSTALLATION_SCHEMA_SEED_ROUND_TRIP','cases':[]}
 def check(name,value):
  if not value:raise AssertionError(name)
  report['cases'].append({'id':name,'status':'PASS'});print(name,'PASS',flush=True)
 try:
  r.start_database();db=Mysql(port=r.dbport,database=r.database,user='root',password='',bindir=str(r.mysql));migrate(db)
  # Drop unused initial company user assignments. Never emit a usable shared password.
  db.sql("DELETE FROM sys_user_role WHERE user_id<>1; DELETE FROM sys_user_post WHERE user_id<>1; DELETE FROM sys_user WHERE user_id<>1; UPDATE sys_user SET password='!UNINITIALIZED!',status='1',phonenumber='',email='',avatar=NULL WHERE user_id=1;")
  business=['article','app_user','file_object','activity','review_campaign','reminder_tracking','global_file','behavior_event','customer_session','audit_event']
  check('empty-business-tables',all(db.sql('SELECT COUNT(*) FROM '+t)=='0' for t in business))
  check('only-disabled-bootstrap-admin',db.sql("SELECT CONCAT(COUNT(*),':',MIN(status)) FROM sys_user")=='1:1')
  schema=dump(db,['--no-data','--routines','--triggers']);seed=dump(db,['--no-create-info','--complete-insert','--skip-extended-insert','--order-by-primary','--hex-blob'])
  target=Mysql(port=r.dbport,database='yunji_install_roundtrip',user='root',password='',bindir=str(r.mysql))
  target.sql('CREATE DATABASE yunji_install_roundtrip CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci',False);target.sql(schema);target.sql(seed)
  check('schema-seed-data-roundtrip',dump(target,['--no-create-info','--complete-insert','--skip-extended-insert','--order-by-primary','--hex-blob'])==seed)
  check('schema-roundtrip',dump(target,['--no-data','--routines','--triggers'])==schema)
  migrate(target);check('incremental-migrator-recognizes-snapshot',True)
  check('no-synthetic-business-after-roundtrip',all(target.sql('SELECT COUNT(*) FROM '+t)=='0' for t in business))
  directory.mkdir(exist_ok=True);(directory/'schema.sql').write_bytes(schema.encode('utf-8'));(directory/'seed.sql').write_bytes(seed.encode('utf-8'))
  manifest={'baseline':'2026-10-27','generatedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'sourceMigrations':{p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in sources()},'files':{name:hashlib.sha256((directory/name).read_bytes()).hexdigest() for name in ['schema.sql','seed.sql']},'bootstrapRequired':True,'containsBusinessFixtures':False}
  (directory/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n','utf-8');report['manifest']=manifest
 except Exception as error:report['fatal']=str(error);print(str(error),flush=True)
 finally:
  r.close();report['passed']=len(report['cases']);report['failed']=int(bool(report.get('fatal')));report['generatedAt']=datetime.datetime.now(datetime.timezone.utc).isoformat();(ROOT/'reports/install-snapshot.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n','utf-8')
 return report['failed']
if __name__=='__main__':raise SystemExit(main())
