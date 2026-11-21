"""Current-version access and resource editing against the running new backend."""
from http_suite import equal
from extended_suite import upload
import datetime,uuid
from urllib.request import Request,urlopen
from urllib.error import HTTPError

def run(s):
 now=datetime.datetime.now(datetime.timezone.utc)
 subject_body={'organizationId':'902','code':'LIFECYCLE','name':'合成生命周期科目'}
 subject=s.call('POST','/api/v1/admin/resources/subjects',subject_body,actor='admin')
 body={**subject_body,'name':'合成科目已编辑','status':'ENABLED','expectedVersion':0}
 edited=s.call('PUT',f"/api/v1/admin/resources/subjects/{subject['id']}",body,actor='admin')
 s.check('subject-edit-and-stale-denial',['RES-01','ADM-04'],lambda:s.call('PUT',f"/api/v1/admin/resources/subjects/{subject['id']}",body,actor='admin',status=409))
 dim=s.call('POST','/api/v1/admin/resources/dimensions',{'kind':'MAJOR','code':'SYNTH_LIFECYCLE','name':'合成专业'},actor='admin')
 s.check('dimension-edit',['RES-02'],lambda:s.call('PUT',f"/api/v1/admin/resources/dimensions/{dim['id']}",{'kind':'MAJOR','code':'SYNTH_LIFECYCLE','name':'合成专业修改','status':'ENABLED','expectedVersion':0},actor='admin'))
 s.check('dimension-withdraw',['RES-02'],lambda:s.call('DELETE',f"/api/v1/admin/resources/dimensions/{dim['id']}?expectedVersion=1",actor='admin'))
 s.check('dimension-hidden-public',['RES-02'],lambda:equal(any(x['id']==dim['id'] for x in s.call('GET','/api/v1/resources/dimensions')),False))
 exam_body={'organizationId':'902','subjectId':subject['id'],'startsAt':(now+datetime.timedelta(days=30)).isoformat(),'displayTimezone':'Asia/Shanghai','sourceReference':'合成考试来源'}
 exam=s.call('POST','/api/v1/admin/resources/exams',exam_body,actor='admin')
 s.check('exam-edit',['RES-03'],lambda:s.call('PUT',f"/api/v1/admin/resources/exams/{exam['id']}",{**exam_body,'sourceReference':'更新考试来源','status':'PUBLISHED','expectedVersion':0},actor='admin'))
 s.check('exam-withdraw',['RES-03'],lambda:s.call('DELETE',f"/api/v1/admin/resources/exams/{exam['id']}?expectedVersion=1",actor='admin'))
 s.check('exam-offline-hidden-public',['RES-03'],lambda:equal(any(x['id']==exam['id'] for x in s.call('GET','/api/v1/resources/exams')),False))
 first=upload(s,'admin','lifecycle-first.txt',b'synthetic first current version')
 second=upload(s,'admin','lifecycle-second.txt',b'synthetic second current version')
 pack_body={'organizationId':'902','subjectId':subject['id'],'title':'合成版本资料包','type':'GENERAL','status':'PUBLISHED','fileIds':[first['id']]}
 pack=s.call('POST','/api/v1/admin/resources/packages',pack_body,actor='admin')
 ticket=s.call('POST',f"/api/v1/me/files/{first['id']}/tickets",actor='bob')
 pack=s.call('PUT',f"/api/v1/admin/resources/packages/{pack['id']}",{**pack_body,'fileIds':[second['id']],'expectedVersion':0},actor='admin')
 s.check('package-current-files-only',['RES-04','RES-05'],lambda:equal([f['fileId'] for f in s.call('GET',f"/api/v1/resources/packages/{pack['id']}")['files']],[second['id']]))
 s.check('package-historical-metadata-retained',['RES-04'],lambda:equal(s.r.sql(f"SELECT COUNT(DISTINCT version_no) FROM resource_file WHERE package_id={pack['id']}").stdout.strip(),'2'))
 s.check('package-removed-file-new-ticket-denied',['RES-05'],lambda:s.call('POST',f"/api/v1/me/files/{first['id']}/tickets",actor='bob',status=404))
 def old_ticket():
  req=Request(s.r.url+ticket['path'],headers={'Authorization':'Bearer '+s.tokens['bob']})
  try:urlopen(req);raise AssertionError('obsolete ticket still accessible')
  except HTTPError as e:equal(e.code,404)
 s.check('package-removed-file-issued-ticket-denied',['RES-05'],old_ticket)
 s.check('package-stale-edit-denied',['RES-04'],lambda:s.call('PUT',f"/api/v1/admin/resources/packages/{pack['id']}",{**pack_body,'expectedVersion':0},actor='admin',status=409))
 s.check('package-invalid-file-atomic-rollback',['RES-04'],lambda:s.call('PUT',f"/api/v1/admin/resources/packages/{pack['id']}",{**pack_body,'fileIds':['999999999'],'expectedVersion':1},actor='admin',status=404))
 s.check('package-failed-edit-preserves-current',['RES-04'],lambda:equal(s.call('GET',f"/api/v1/resources/packages/{pack['id']}")['currentVersionNo'],2))
 s.check('package-withdraw',['RES-04'],lambda:s.call('DELETE',f"/api/v1/admin/resources/packages/{pack['id']}?expectedVersion=1",actor='admin'))
 s.check('package-offline-detail-hidden',['RES-04'],lambda:s.call('GET',f"/api/v1/resources/packages/{pack['id']}",status=404))
 s.check('package-offline-file-access-revoked',['RES-05'],lambda:s.call('POST',f"/api/v1/me/files/{second['id']}/tickets",actor='bob',status=404))
 artifact=s.call('POST','/api/v1/admin/resources/artifacts',{'name':'合成生命周期插件','platform':'WEB','status':'DRAFT'},actor='admin')
 s.check('artifact-meta-edit',['RES-06'],lambda:s.call('PUT',f"/api/v1/admin/resources/artifacts/{artifact['id']}",{'name':'合成插件修改','platform':'LINUX','status':'PUBLISHED','expectedVersion':0},actor='admin'))
 s.check('artifact-withdraw',['RES-06'],lambda:s.call('DELETE',f"/api/v1/admin/resources/artifacts/{artifact['id']}?expectedVersion=1",actor='admin'))
 s.check('artifact-offline-hidden',['RES-06'],lambda:s.call('GET',f"/api/v1/resources/artifacts/{artifact['id']}",status=404))
 s.check('resource-management-requires-role',['ADM-02'],lambda:s.call('DELETE',f"/api/v1/admin/resources/packages/{pack['id']}?expectedVersion=2",actor='test1',status=403))
 s.check('resource-audit-recorded',['ADM-04'],lambda:equal(s.r.sql(f"SELECT COUNT(*)>0 FROM audit_event WHERE action='EXAM_UPDATE' AND object_id={exam['id']}").stdout.strip(),'1'))
