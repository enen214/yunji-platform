/** 未开始/已失效才提示；生效中一律返回空串，避免每张卡片多一行噪声。 */
export function lifecycleText(state?: string): string {
  if (state === 'UPCOMING') return '未开始'
  if (state === 'EXPIRED') return '已失效'
  return ''
}

export function dateText(value: unknown): string {
  if (value === null || value === undefined || value === '') return ''
  const raw = String(value)
  const at = new Date(raw)
  if (Number.isNaN(at.getTime())) return raw
  const pad = (n: number) => (n < 10 ? '0' + n : String(n))
  return (
    at.getFullYear() + '-' + pad(at.getMonth() + 1) + '-' + pad(at.getDate()) +
    ' ' + pad(at.getHours()) + ':' + pad(at.getMinutes())
  )
}
