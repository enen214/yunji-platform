"""Bind execution evidence to source/configuration, excluding generated output and credentials."""
import hashlib
import os
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
SKIP={'target','node_modules','dist','.git','.runtime','__pycache__','unpackage','.idea'}

def source_hash(*folders):
 digest=hashlib.sha256()
 for folder in sorted(folders):
  base=ROOT/folder
  for directory,dirs,names in os.walk(base):
   dirs[:]=sorted(d for d in dirs if d not in SKIP)
   for name in sorted(names):
    if name.startswith('.env') or name in {'.flattened-pom.xml'} or name.endswith(('.log','.pyc')):continue
    p=Path(directory)/name
    digest.update(p.relative_to(ROOT).as_posix().encode('utf-8'));digest.update(b'\0');digest.update(p.read_bytes());digest.update(b'\0')
 return digest.hexdigest()

def fingerprints():
 return {'backend':source_hash('backend','database/migrations'),'admin':source_hash('apps/admin'),
         'client':source_hash('apps/client'),'tests':source_hash('tests','scripts'),'deploy':source_hash('deploy')}

def application_fingerprints():
 return {'backend':source_hash('backend','database/migrations'),'admin':source_hash('apps/admin'),'client':source_hash('apps/client')}

def artifact_fingerprints():
 result={}
 for name,relative in [('backend','backend/agri-admin/target/agri-admin.jar'),('admin','apps/admin/dist'),('client-h5','apps/client/dist/build/h5'),('client-mp-weixin','apps/client/dist/build/mp-weixin')]:
  base=ROOT/relative;digest=hashlib.sha256()
  paths=[base] if base.is_file() else sorted(p for p in base.rglob('*') if p.is_file())
  if not paths:raise RuntimeError('Missing build artifact: '+relative)
  for path in paths:
   digest.update(path.relative_to(ROOT).as_posix().encode());digest.update(b'\0');digest.update(path.read_bytes());digest.update(b'\0')
  result[name]=digest.hexdigest()
 return result
