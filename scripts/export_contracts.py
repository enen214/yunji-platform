"""Export actual running OpenAPI; route matrix distinguishes enforced boundaries from inference."""
import argparse
import datetime
import json
from pathlib import Path
import re
from urllib.request import urlopen
import yaml

ROOT = Path(__file__).resolve().parents[1]
VERBS = {'get','post','put','patch','delete','head','options'}


def export(base):
    # Large explicit DTO schemas can take longer than the application readiness probe.
    with urlopen(base + '/v3/api-docs', timeout=120) as res:
        spec = json.load(res)
    directory = ROOT / 'contracts'; directory.mkdir(exist_ok=True)
    (directory / 'openapi.json').write_text(json.dumps(spec, ensure_ascii=False, indent=2), encoding='utf-8', newline='\n')
    (directory / 'openapi.yaml').write_text(yaml.safe_dump(spec, allow_unicode=True, sort_keys=False), encoding='utf-8', newline='\n')
    permissions = {}
    java = ROOT / 'backend/agri-modules/yunji-content/src/main/java'
    for source in java.rglob('*Service.java'):
        perms = sorted(set(re.findall(r'\.require\("([^\"]+)"\)', source.read_text(encoding='utf-8'))))
        if perms:
            permissions[source.stem] = perms
    rows = []
    for route, operations in sorted(spec['paths'].items()):
        for method, op in operations.items():
            if method not in VERBS: continue
            boundary = 'FRAMEWORK'
            if route.startswith('/api/local') or route == '/api/v1/auth/customer/local': boundary='LOCAL_SECRET_OR_LOCAL_SINGLE_USE_CODE'
            elif route == '/api/v1/auth/customer/wechat/native': boundary='REAL_WECHAT_CODE_AND_OPTIONAL_PHONE_AUTHORIZATION'
            elif route == '/api/v1/auth/customer/wechat': boundary='WECHAT_CODE_OR_LOCAL_SYNTHETIC_COMPATIBILITY'
            elif route.startswith('/api/v1/admin'): boundary='STAFF_AND_SERVICE_PERMISSION'
            elif route.startswith('/api/v1/me') or route == '/api/v1/auth/customer/logout': boundary='CUSTOMER'
            elif route.startswith('/api/v1/files/download'): boundary='PRINCIPAL_BOUND_SINGLE_USE_TICKET'
            elif route.startswith('/api/v1'): boundary='PUBLIC'
            rows.append({'method':method.upper(),'path':route,'operationId':op.get('operationId'),'boundary':boundary,'tags':op.get('tags',[])})
    (directory / 'endpoint-matrix.json').write_text(json.dumps({'generatedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'source':'RUNNING_NEW_APPLICATION','paths':len(spec['paths']),'operations':rows,'servicePermissionCandidates':permissions,'permissionNote':'Realm classifications from route contract; exact permissions must be checked in Actors/service methods and negative API tests. Service candidates are not route-level certified grants.'},ensure_ascii=False,indent=2),encoding='utf-8')
    lines=['# 实际运行接口清单','','来源：新工程完整基座 `/v3/api-docs`。JSON/YAML为实际运行导出。','','新业务Controller响应使用显式记录模型，内部查询Map在HTTP边界投影为DTO；统一定义见 view-models.json。路径域划分不等于逐接口权限证明，实际权限见服务代码和反向用例。计划差异决策见 route-decisions.json。','','|方法|路径|身份边界|','|---|---|---|']
    lines += [f"|{r['method']}|`{r['path']}`|{r['boundary']}|" for r in rows]
    (directory / 'README.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
    print(f'Actual OpenAPI exported: {len(spec["paths"])} paths, {len(rows)} operations')


if __name__ == '__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--api',default='http://127.0.0.1:18088');args=parser.parse_args();export(args.api)
