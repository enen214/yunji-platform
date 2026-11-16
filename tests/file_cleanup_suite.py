"""Exercise cleanup recovery and filesystem boundaries using owned synthetic objects."""
from http_suite import equal
from extended_suite import upload
from concurrent.futures import ThreadPoolExecutor

def run(s):
 def deleted(name,data):
  f=upload(s,'admin',name,data);s.call('DELETE',f"/api/v1/admin/files/{f['id']}",actor='admin')
  s.r.sql(f"UPDATE file_object SET deleted_at=UTC_TIMESTAMP()-INTERVAL 8 DAY WHERE id={f['id']}")
  key=s.r.sql(f"SELECT object_key FROM file_object WHERE id={f['id']}").stdout.strip()
  return f,key,s.r.work/'files'/key
 cleanup=lambda:s.call('POST','/api/v1/admin/files/cleanup?limit=100',actor='admin')
 f,key,path=deleted('cleanup-failure.txt',b'synthetic cleanup failure')
 path.unlink();path.mkdir();(path/'synthetic-blocker').write_bytes(b'synthetic blocker')
 s.check('file-cleanup-io-failure-retained',['OPS-05'],lambda:equal(cleanup()['failed'],1))
 s.check('file-cleanup-io-failure-keeps-deleted',['OPS-05'],lambda:equal(s.r.sql(f"SELECT status FROM file_object WHERE id={f['id']}").stdout.strip(),'DELETED'))
 (path/'synthetic-blocker').unlink();path.rmdir();path.write_bytes(b'synthetic cleanup failure')
 s.check('file-cleanup-retries-after-storage-repair',['OPS-05'],lambda:equal(cleanup()['purged'],1))
 f,key,path=deleted('cleanup-missing.txt',b'synthetic cleanup missing');path.unlink()
 s.check('file-cleanup-already-missing-reconciles',['OPS-05'],lambda:equal(cleanup()['alreadyMissing'],1))
 f,key,path=deleted('cleanup-outside.txt',b'synthetic cleanup outside')
 sentinel=s.r.work/'outside-cleanup-sentinel';sentinel.write_bytes(b'must remain')
 s.r.sql(f"UPDATE file_object SET object_key='../outside-cleanup-sentinel' WHERE id={f['id']}")
 s.check('file-cleanup-invalid-object-key-rejected',['OPS-05'],lambda:equal(cleanup()['failed'],1))
 s.check('file-cleanup-outside-file-preserved',['OPS-05'],lambda:equal(sentinel.read_bytes(),b'must remain'))
 s.r.sql(f"UPDATE file_object SET object_key='{key}' WHERE id={f['id']}");cleanup()
 f,key,path=deleted('cleanup-parallel.txt',b'synthetic cleanup parallel')
 def parallel():
  with ThreadPoolExecutor(2) as pool:results=list(pool.map(lambda _:cleanup(),range(2)))
  equal(sum(r['removed'] for r in results),1);equal(sum(r['failed'] for r in results),0)
 s.check('file-cleanup-parallel-no-double-delete',['OPS-05'],parallel)
 s.check('file-cleanup-audit-persists',['ADM-04','OPS-05'],lambda:equal(s.r.sql(f"SELECT COUNT(*) FROM audit_event WHERE action='FILE_PHYSICAL_DELETE' AND object_id={f['id']}").stdout.strip(),'1'))
