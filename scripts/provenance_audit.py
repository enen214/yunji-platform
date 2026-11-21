"""Read-only fingerprint comparison. Allows company initial framework; never imports sample files."""
import datetime
import hashlib
import json
from pathlib import Path
import subprocess
from evidence import application_fingerprints

ROOT=Path(__file__).resolve().parents[1]
SAMPLE=ROOT.parent/'yangben'
COMMIT='b4d44eebdefbd4cb7df37922c67657fa1614846b'
SKIP={'.git','node_modules','target','dist','unpackage','__pycache__','.runtime','.idea'}
EXTS={'.java','.vue','.ts','.js','.sql','.py','.sh','.ps1','.css','.scss','.yml','.yaml'}


def digest(data):return hashlib.sha256(data).hexdigest()
def normalized(data):return b''.join(data.split())
def files(root):
    # Prune before traversal; don't walk dependency/cache trees or read credentials.
    import os
    for directory, dirs, names in os.walk(root):
        dirs[:]=[d for d in dirs if d not in SKIP]
        for name in names:
            p=Path(directory)/name
            if p.suffix in EXTS and not name.startswith('.env') and not name.endswith(('.bak','.log')):yield p


def main():
    base=SAMPLE/'cis4j'
    entries=subprocess.check_output(['git','-C',str(base),'ls-tree','-r',COMMIT],creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0)).decode('utf-8').splitlines()
    blobs=[]
    for line in entries:
        metadata,name=line.split('\t',1);kind=metadata.split()
        if kind[1]=='blob' and Path(name).suffix in EXTS:blobs.append((kind[2],name))
    # Batch Git objects locally. Their contents stay inside this process; report hashes/paths only.
    process=subprocess.run(['git','-C',str(base),'cat-file','--batch'],input=''.join(x+'\n' for x,_ in blobs).encode(),capture_output=True,check=True,creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
    data=process.stdout;position=0;allowed={}
    for _,name in blobs:
        end=data.index(b'\n',position);size=int(data[position:end].split()[-1]);position=end+1
        content=data[position:position+size];position+=size+1
        allowed.setdefault(digest(normalized(content)),[]).append(name)
    forbidden={};sample_count=0
    for p in files(SAMPLE):
        content=p.read_bytes();norm=normalized(content)
        if len(norm)<120:continue
        fingerprint=digest(norm)
        if fingerprint in allowed:continue
        forbidden.setdefault(fingerprint,[]).append(p.relative_to(SAMPLE).as_posix());sample_count+=1
    new_count=0;matches=[];foundation=[];independent=[]
    for folder in ['backend','apps','database','scripts','deploy','tests']:
        for p in files(ROOT/folder):
            if p.name.endswith(('.min.js','.d.ts')):continue
            content=p.read_bytes();norm=normalized(content)
            if len(norm)<120:continue
            fingerprint=digest(norm);name=p.relative_to(ROOT).as_posix();new_count+=1
            row={'path':name,'sha256':digest(content),'normalizedSha256':fingerprint}
            if fingerprint in allowed:
                row['companyFrameworkMatches']=allowed[fingerprint];foundation.append(row)
            elif fingerprint in forbidden:
                row['sampleMatches']=forbidden[fingerprint];matches.append(row)
            else:independent.append(row)
    schema=ROOT/'database/framework-schema.sql'
    report={'generatedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'scope':'EXACT_AND_WHITESPACE_NORMALIZED_SOURCE_FINGERPRINTS','companyInitialCommit':COMMIT,'sampleFilesCompared':sample_count,'newSourceFilesCompared':new_count,'forbiddenMatches':matches,'companyFoundationFiles':foundation,'independentlyChangedOrNewFiles':independent,'officialSchemaSha256':digest(schema.read_bytes()),'limitations':['Fingerprint comparison cannot detect rewritten copies or prove complete functional quality.','Company initial framework files are explicitly authorized; source/license review is separate.','Credentials and dependency/build/cache trees excluded.'],'passed':not matches}
    report['sourceFingerprints']=application_fingerprints()
    (ROOT/'reports/provenance.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
    (ROOT/'docs/decisions/源码来源核查.md').write_text(f"# 源码来源核查\n\n生成时间：{report['generatedAt']}。公司首次框架提交 {COMMIT} 明确允许使用。官方SQL的SHA256：{report['officialSchemaSha256']}。\n\n本轮比较 {sample_count} 个样本中非首次框架文件和 {new_count} 个新工程源文件；忽略空白后的完整文件匹配发现 {len(matches)} 个需要核查的非基座匹配。详细路径与哈希见 `reports/provenance.json`。\n\n此检查不导入/修改样本；排除依赖、构建缓存、密钥。它能发现完全复制或只改空白的文件，不能证明没有经改写的复制，不能代替代码审查、依赖许可审查或功能验收。新业务按独立需求和迁移实现，保留可追踪文件哈希。\n",encoding='utf-8')
    print(json.dumps({'sampleFiles':sample_count,'newFiles':new_count,'companyFrameworkFiles':len(foundation),'nonFoundationMatches':len(matches),'passed':report['passed']}))
    return bool(matches)


if __name__=='__main__':raise SystemExit(main())
