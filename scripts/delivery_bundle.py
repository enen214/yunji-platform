"""Create a reviewable development handover bundle; never label blocked artifacts production releases."""
import argparse,datetime,hashlib,json,os,shutil,zipfile
from pathlib import Path
from evidence import ROOT,fingerprints,application_fingerprints,artifact_fingerprints,SKIP

def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def write(path,value):path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def copy_source(base,destination):
 for directory,dirs,names in os.walk(base):
  dirs[:]=sorted(d for d in dirs if d not in SKIP)
  for name in sorted(names):
   p=Path(directory)/name
   if p.is_symlink():raise RuntimeError('来源树含链接，需核查: '+str(p.relative_to(ROOT)))
   if name.startswith('.env') or name=='.flattened-pom.xml' or name.endswith(('.log','.pyc','.bak','.zip')):continue
   target=destination/p.relative_to(ROOT);target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,target)

def main():
 parser=argparse.ArgumentParser();parser.add_argument('--destination',type=Path,default=ROOT.parent/'yunji-delivery');args=parser.parse_args()
 destination=args.destination.resolve()
 if destination==ROOT or destination.is_relative_to(ROOT):raise RuntimeError('交付目录必须在工程之外')
 stamp=datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ');folder=destination/('yunji-development-'+stamp);folder.mkdir(parents=True,exist_ok=False)
 source=folder/'source';source.mkdir()
 for name in ['backend','apps','database','scripts','deploy','tests','docs','contracts','.github']:
  copy_source(ROOT/name,source)
 for name in ['README.md','.gitignore','.dockerignore']:
  if (ROOT/name).is_file():shutil.copyfile(ROOT/name,source/name)
 reports=source/'reports';reports.mkdir()
 for name in ['release-gate.json','workbench-demo.json','wechat-login.json','browser-prototype.json','client-compat.json','docker-static.json','three-end-acceptance.json','reminder-rebuild.json','browser-reminder-rebuild.json','latest.json','browser.json','browser-extended.json','browser-closure.json','browser-execution.json','operations.json','build.json','feature-review.json','security.json','npm-admin-audit.json','npm-client-audit.json','provenance.json','contract-reconciliation.json']:
  if (ROOT/'reports'/name).is_file():shutil.copyfile(ROOT/'reports'/name,reports/name)
 if (ROOT/'reports/sbom').exists():shutil.copytree(ROOT/'reports/sbom',reports/'sbom')
 builds=json.loads((ROOT/'reports/build.json').read_text(encoding='utf-8'))
 if builds.get('failed')!=0 or not all(builds.get('sourceFingerprints',{}).get(k)==v for k,v in application_fingerprints().items()):raise RuntimeError('构建证据过时或失败；先执行 build_acceptance.py')
 if not builds.get('sourceUnchanged') or builds.get('artifactFingerprints')!=artifact_fingerprints():raise RuntimeError('构建产物已变化，请重新构建验收')
 artifacts=folder/'artifacts';(artifacts/'backend').mkdir(parents=True)
 shutil.copyfile(ROOT/'backend/agri-admin/target/agri-admin.jar',artifacts/'backend/agri-admin.jar')
 for name,path in [('admin','apps/admin/dist'),('client-h5','apps/client/dist/build/h5'),('client-mp-weixin','apps/client/dist/build/mp-weixin')]:shutil.copytree(ROOT/path,artifacts/name)
 gate=json.loads((ROOT/'reports/release-gate.json').read_text(encoding='utf-8'))
 (folder/'先读我.md').write_text('# 云迹本地基线验收交接包\n\n本包用于本地基线验收、代码审查和接收团队接手；本地验收状态以 reports/release-gate.json 和三端改造验收报告为准；当前不具备生产发布资格。\n\n源码在 source；构建产物在 artifacts；先读 source/README.md、source/docs/当前阶段与验收结果.md、source/docs/交付与剩余门禁.md。部署选择由新工程独立设计，禁止接入旧库和旧脚本。无旧数据迁移；合成数据仅用于 local。真实通知供应商、平台资源、Linux 验证和安全整改仍按门禁处理。\n\n运行 python scripts/delivery_bundle.py --destination <新目录> 可以重建本包。本包不含 .runtime、密钥、数据库备份、依赖缓存和旧样本。外部运行环境和 npm/Maven 依赖需要由接收团队安装。\n',encoding='utf-8')
 manifest={'scope':'LOCAL_BASELINE_HANDOVER_NOT_PRODUCTION_RELEASE','createdAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'sourceFingerprints':fingerprints(),'localStandaloneAcceptanceComplete':gate.get('localStandaloneAcceptanceComplete',False),'localFeatureAcceptanceComplete':gate.get('localFeatureAcceptanceComplete',False),'releaseEligible':gate['releaseEligible'],'excluded':['yangben','.runtime','node_modules','target except verified API jar','dist except verified frontends','.env','logs','private workbench files'],'files':[]}
 for p in sorted(folder.rglob('*')):
  if p.is_file():manifest['files'].append({'path':p.relative_to(folder).as_posix(),'bytes':p.stat().st_size,'sha256':sha(p)})
 write(folder/'manifest.json',manifest)
 archive=folder.with_suffix('.zip')
 with zipfile.ZipFile(archive,'x',zipfile.ZIP_DEFLATED) as z:
  for p in sorted(folder.rglob('*')):
   if p.is_file():z.write(p,p.relative_to(folder).as_posix())
 with zipfile.ZipFile(archive) as z:
  if z.testzip():raise RuntimeError('交付 ZIP 校验失败')
  for row in manifest['files']:
   if hashlib.sha256(z.read(row['path'])).hexdigest()!=row['sha256']:raise RuntimeError('交付文件校验失败')
 receipt={'createdAt':manifest['createdAt'],'scope':manifest['scope'],'folder':str(folder),'archive':str(archive),'archiveSha256':sha(archive),'files':len(manifest['files']),'zipIntegrityVerified':True,'releaseEligible':manifest['releaseEligible']}
 write(ROOT/'reports/delivery.json',receipt);print(json.dumps(receipt,ensure_ascii=False))

if __name__=='__main__':main()
