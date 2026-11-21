"""Check the reviewed October scope against executed cases; preserve external limits."""
import datetime
import hashlib
import json
from evidence import ROOT, application_fingerprints, source_hash


def read(path):
    return json.loads((ROOT / path).read_text(encoding='utf-8-sig'))


def main():
    spec_path = 'docs/spec/three-end-requirements.json'
    spec = read(spec_path)
    names = ['latest', 'browser', 'browser-extended', 'browser-closure', 'reminder-rebuild', 'browser-reminder-rebuild']
    reports = {name: read('reports/' + name + '.json') for name in names}
    cases = {c['id']: c for r in reports.values() for c in r.get('cases', [])}
    rows = []
    for item in spec['items']:
        missing = [key for key in item['caseIds'] if cases.get(key, {}).get('status') != 'PASS']
        rows.append({**item, 'missingEvidence': missing,
                     'localScenarioStatus': 'FAIL' if missing else 'PASS' if item['caseIds'] else 'DECISION_ONLY'})
    report = {
        'generatedAt': datetime.datetime.now(datetime.timezone.utc).isoformat(),
        'sourceFingerprints': application_fingerprints(),
        'testSourceSha256': source_hash('tests', 'scripts'),
        'requirementsSha256': hashlib.sha256((ROOT / spec_path).read_bytes()).hexdigest(),
        'evidenceSha256': {name: hashlib.sha256((ROOT / ('reports/' + name + '.json')).read_bytes()).hexdigest() for name in names},
        'localScenariosPassed': all(not row['missingEvidence'] for row in rows),
        'rows': rows,
        'limitations': ['Case evidence covers explicitly listed scenarios only.',
                       'Source freshness and complete suite success are checked by release-gate.json.',
                       'Decision-only and external-pending rows are not completed product functions.'],
    }
    (ROOT / 'reports/three-end-acceptance.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    lines = ['# 三端改造逐项验收对照', '', '由 `scripts/three_end_review.py` 根据版本化要求与实际执行报告生成。PASS 仅指所列本地场景；DECISION_ONLY 表示边界决策，不代表功能已实现。完整执行与源码一致性另见 release-gate.json。', '', '|原要求|专业处理与范围|本地场景|证据用例|', '|---|---|---|---|']
    for row in rows:
        lines.append('|'+ '|'.join([row['requirement'], row['decision'], row['localScenarioStatus'], ', '.join(row['caseIds']) or '仅记录决策'])+'|')
    (ROOT / 'docs/spec/三端改造逐项验收对照.md').write_text('\n'.join(lines)+'\n', encoding='utf-8')
    print(json.dumps({'rows': len(rows), 'localScenariosPassed': report['localScenariosPassed'], 'failed': [r['requirement'] for r in rows if r['missingEvidence']]}, ensure_ascii=False))
    return not report['localScenariosPassed']


if __name__ == '__main__':
    raise SystemExit(main())
