"""Native isolated rehearsal of new production bootstrap, migration and restore tooling."""
import datetime
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import uuid
from urllib.error import HTTPError
from urllib.request import Request, urlopen
from database_ops import Mysql, migrate, bootstrap, backup, restore
from local_runtime import LocalRuntime, ROOT, FLAGS
from evidence import application_fingerprints


def main():
    runtime=LocalRuntime(33309,18089);runtime.redisport=16380
    report={'scope':'NATIVE_WINDOWS_PRODUCTION_CONFIG_AND_DATABASE_REHEARSAL','linuxRuntimeVerified':False,'startedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'sourceFingerprints':application_fingerprints(),'cases':[]}
    def check(name, fn):
        try:
            fn();report['cases'].append({'id':name,'status':'PASS'});print(name,'PASS',flush=True)
        except Exception as e:
            report['cases'].append({'id':name,'status':'FAIL','error':str(e)});raise
    def rejected(fn, fragment):
        try:fn()
        except Exception as e:
            assert fragment in str(e),str(e);return
        raise AssertionError('应拒绝此操作')
    def stop_api():
        if runtime.apiproc and runtime.apiproc.poll() is None:
            runtime.apiproc.terminate();runtime.apiproc.wait(timeout=20)
    def failed_start(profile='prod', env=None, args=None, reason=''):
        try:
            rejected(lambda:runtime.start_api(profile,env,args),'API')
            text=(runtime.work/'api.log').read_text(encoding='utf-8',errors='replace')
            assert reason in text, reason + ' missing from startup log'
        finally:stop_api()
    def assert_local_denied():
        for path,body in [('/api/local/login-codes',{'userId':91000000000000001,'appId':'local-client'}),('/api/v1/auth/customer/local',{'code':'synthetic','appId':'local-client'})]:
            try:
                urlopen(Request(runtime.url+path,json.dumps(body).encode(),{'Content-Type':'application/json','X-Local-Secret':runtime.local_secret},method='POST'),timeout=10)
                raise AssertionError('生产开放了本地入口')
            except HTTPError as e:assert e.code==404,e.read().decode()
    def canonical(db):
        cmd=db.command('mysqldump',False)+['--no-create-info','--skip-comments','--compact','--skip-add-locks','--skip-extended-insert','--order-by-primary','--hex-blob','--no-tablespaces','--set-gtid-purged=OFF',db.database]
        result=subprocess.run(cmd,capture_output=True,env=db.env(),timeout=60,creationflags=FLAGS)
        assert result.returncode==0,result.stderr.decode(errors='replace')
        return hashlib.sha256(result.stdout).hexdigest()
    try:
        runtime.start_database();runtime.start_redis()
        db=Mysql(port=runtime.dbport,database='yunji_production_rehearsal',user='root',password='',bindir=str(runtime.mysql))
        check('new-production-database-initialize-no-fixtures',lambda:migrate(db,True))
        assert db.sql('SELECT COUNT(*) FROM app_user')=='0'
        check('migration-checksum-repeatable',lambda:migrate(db))
        check('initialize-existing-database-refused',lambda:rejected(lambda:migrate(db,True),'已有库'))
        check('migration-concurrent-lock-refused',lambda:concurrent(db,rejected))
        first='V1__core_schema.sql';old=db.sql(f"SELECT sha256 FROM schema_history WHERE version='{first}'")
        db.sql(f"UPDATE schema_history SET sha256=REPEAT('0',64) WHERE version='{first}'")
        check('migration-checksum-tampering-refused',lambda:rejected(lambda:migrate(db),'已应用迁移被修改'))
        db.sql(f"UPDATE schema_history SET sha256='{old}' WHERE version='{first}'")
        runtime.database=db.database
        check('production-default-password-start-refused',lambda:failed_start(reason='Replace the official bootstrap admin password'))
        password=uuid.uuid4().hex+'A!'
        check('production-admin-bootstrap',lambda:bootstrap(db,password))
        assert db.sql("SELECT COUNT(*) FROM sys_user WHERE user_id<>1 AND status='0'")=='0'
        check('production-local-gateway-start-refused',lambda:failed_start(args=['--yunji.local-enabled=true'],reason='Production must disable local gateways'))
        check('production-short-token-start-refused',lambda:failed_start(env={'YUNJI_TOKEN_SECRET':'short'},reason='Production token secret too short'))
        check('mixed-production-local-profile-refused',lambda:failed_start(profile='prod,local',reason='Production and local profiles must not be combined'))
        check('production-valid-config-start',lambda:runtime.start_api('prod'))
        check('production-local-login-entry-404',assert_local_denied)
        stop_api()
        storage=runtime.work/'production-files';storage.mkdir();(storage/'synthetic-private-file').write_bytes('合成恢复文件'.encode())
        original=canonical(db)
        destination=runtime.work/'production-backup'
        check('new-backup-database-and-files-checksums',lambda:backup(db,storage,destination))
        restored=Mysql(port=runtime.dbport,database='yunji_restored_rehearsal',user='root',password='',bindir=str(runtime.mysql))
        restored_storage=runtime.work/'restored-files'
        check('restore-to-new-database-and-directory',lambda:restore(restored,destination,restored_storage))
        check('restored-database-content-digest-matches',lambda:equal(canonical(restored),original))
        check('restored-private-file-digest-matches',lambda:equal(hashlib.sha256((restored_storage/'synthetic-private-file').read_bytes()).hexdigest(),hashlib.sha256((storage/'synthetic-private-file').read_bytes()).hexdigest()))
        check('restore-existing-target-refused',lambda:rejected(lambda:restore(restored,destination,restored_storage),'禁止覆盖'))
        corrupt=runtime.work/'corrupt-backup';shutil.copytree(destination,corrupt);(corrupt/'database.sql').write_bytes(b'corrupt')
        check('restore-tampered-backup-refused',lambda:rejected(lambda:restore(Mysql(port=runtime.dbport,database='yunji_corrupt_rehearsal',user='root',password='',bindir=str(runtime.mysql)),corrupt,runtime.work/'never-created'),'校验失败'))
        runtime.database=restored.database
        check('restored-database-production-start',lambda:runtime.start_api('prod',{'YUNJI_STORAGE_ROOT':str(restored_storage)}))
        check('restored-production-local-login-entry-404',assert_local_denied)
    except Exception as e:
        report['fatal']=str(e)
        import traceback;traceback.print_exc()
    finally:
        report['completedAt']=datetime.datetime.now(datetime.timezone.utc).isoformat();report['sourceUnchanged']=report['sourceFingerprints']['backend']==application_fingerprints()['backend']
        report['logDirectory']=str(runtime.work);runtime.close()
        report['passed']=sum(c['status']=='PASS' for c in report['cases']);report['failed']=sum(c['status']=='FAIL' for c in report['cases'])+bool(report.get('fatal'))
        (ROOT/'reports/operations.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
        print(json.dumps({'passed':report['passed'],'failed':report['failed'],'logs':report['logDirectory']},ensure_ascii=False))
    return bool(report['failed'])


def equal(a,b):assert a==b,(a,b)
def concurrent(db,rejected):
    with db.lock():rejected(lambda:migrate(db),'并发执行')


if __name__=='__main__':raise SystemExit(main())
