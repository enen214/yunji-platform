"""CycloneDX inventory of the libraries actually embedded in the verified deployment JAR."""
import datetime,hashlib,io,json,uuid,zipfile,os
from pathlib import Path
from evidence import ROOT,source_hash

def main():
 jar=ROOT/'backend/agri-admin/target/agri-admin.jar';components=[];unknown=[]
 repo=Path.home()/'.m2/repository';candidates={}
 for directory,dirs,names in os.walk(repo):
  for name in names:
   if name.endswith('.jar'):candidates.setdefault(name,[]).append(Path(directory)/name)
 with zipfile.ZipFile(jar) as archive:
  for path in sorted(n for n in archive.namelist() if n.startswith('BOOT-INF/lib/') and n.endswith('.jar')):
   blob=archive.read(path);props=[]
   with zipfile.ZipFile(io.BytesIO(blob)) as lib:
    for name in lib.namelist():
     if name.startswith('META-INF/maven/') and name.endswith('/pom.properties'):
      fields=dict(line.split('=',1) for line in lib.read(name).decode('utf-8').splitlines() if '=' in line and not line.startswith('#'))
      if all(k in fields for k in ['groupId','artifactId','version']):props.append(fields)
   filename=path.rsplit('/',1)[1]
   matching=[p for p in props if filename==p['artifactId']+'-'+p['version']+'.jar']
   fields=matching[0] if len(matching)==1 else props[0] if len(props)==1 else None
   if not fields:
    digest=hashlib.sha256(blob).hexdigest()
    # Coordinate fallback is accepted only when the local Maven artifact bytes match exactly.
    names=[filename]
    if filename.startswith('com.sun.xml.bind-'):names.append(filename.removeprefix('com.sun.xml.bind-'))
    matches=[p for n in names for p in candidates.get(n,[]) if hashlib.sha256(p.read_bytes()).hexdigest()==digest]
    coordinates=set()
    for candidate in matches:
     parts=candidate.relative_to(repo).parts
     if len(parts)>=4:coordinates.add(('.'.join(parts[:-3]),parts[-3],parts[-2]))
    if len(coordinates)==1:
     group,name,version=coordinates.pop();fields={'groupId':group,'artifactId':name,'version':version}
    if not fields and filename.startswith('spring-boot-jarmode-tools-'):
     version=filename.removeprefix('spring-boot-jarmode-tools-').removesuffix('.jar')
     container=repo/'org/springframework/boot/spring-boot-loader-tools'/version/('spring-boot-loader-tools-'+version+'.jar')
     if container.is_file():
      with zipfile.ZipFile(container) as original:
       embedded=original.read('META-INF/jarmode/spring-boot-jarmode-tools.jar')
       if hashlib.sha256(embedded).hexdigest()==digest:fields={'groupId':'org.springframework.boot','artifactId':'spring-boot-jarmode-tools','version':version}
   if fields:
    group,name,version=fields['groupId'],fields['artifactId'],fields['version']
    purl=f'pkg:maven/{group}/{name}@{version}'
    components.append({'type':'library','group':group,'name':name,'version':version,'purl':purl,'bom-ref':purl,'hashes':[{'alg':'SHA-256','content':hashlib.sha256(blob).hexdigest()}]})
   else:unknown.append(filename)
 report={'bomFormat':'CycloneDX','specVersion':'1.6','serialNumber':'urn:uuid:'+str(uuid.uuid4()),'version':1,'metadata':{'timestamp':datetime.datetime.now(datetime.timezone.utc).isoformat(),'component':{'type':'application','name':'yunji-platform','version':'local-delivery-closure'},'properties':[{'name':'yunji:artifact-sha256','value':hashlib.sha256(jar.read_bytes()).hexdigest()},{'name':'yunji:source-sha256','value':source_hash('backend','database/migrations')},{'name':'yunji:scope','value':'BOOT-INF/lib deployed runtime libraries; no graph edges inferred'}]},'components':components}
 out=ROOT/'reports/sbom';out.mkdir(exist_ok=True)
 (out/'backend-bom.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 (out/'inventory.json').write_text(json.dumps({'runtimeLibraries':len(components),'unidentifiedLibraries':unknown,'artifactSha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'sourceSha256':source_hash('backend','database/migrations')},indent=2)+'\n',encoding='utf-8')
 print(json.dumps({'runtimeLibraries':len(components),'unidentifiedLibraries':unknown}));return bool(unknown)

if __name__=='__main__':raise SystemExit(main())
