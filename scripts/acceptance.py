from pathlib import Path
import sys,json,traceback,datetime,hashlib
from local_runtime import LocalRuntime
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tests'))
from http_suite import Suite
from extended_suite import run as extended
from notification_ops_suite import run as notifications
from closure_suite import run as closure
from snapshot_suite import run as snapshots
from resource_lifecycle_suite import run as resource_lifecycle
from file_cleanup_suite import run as file_cleanup
from delivery_closure_suite import run as delivery_closure
from suboperations_suite import run as suboperations
from discovery_suite import run as discovery
from prototype_suite import run as prototype
from feedback_suite import run as feedback
from export_contracts import export
from evidence import fingerprints
r=LocalRuntime();report={'scope':'FULL_FOUNDATION_INTEGRATION','releaseEligible':False,'startedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'sourceFingerprints':fingerprints(),'cases':[]}
try:
 r.start_database();r.migrate();r.seed();r.start_redis();r.start_api()
 export(r.url)
 suite=Suite(r);report['cases']=suite.results;suite.run();extended(suite);file_cleanup(suite);notifications(suite);snapshots(suite);resource_lifecycle(suite);delivery_closure(suite);suboperations(suite);discovery(suite);prototype(suite);feedback(suite);closure(suite)
except Exception as e:
 traceback.print_exc();report['fatal']=str(e)
finally:
 report['logDirectory']=str(r.work);r.close()
 report['passed']=sum(x['status']=='PASS' for x in report['cases']);report['failed']=sum(x['status']=='FAIL' for x in report['cases'])+bool(report.get('fatal'))
 report['sourceUnchanged']=report['sourceFingerprints']==fingerprints();report['completedAt']=datetime.datetime.now(datetime.timezone.utc).isoformat()
 (ROOT/'reports/latest.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
 print(json.dumps({'passed':report['passed'],'failed':report['failed'],'logs':report['logDirectory']},ensure_ascii=False))
sys.exit(1 if report['failed'] else 0)
