export const clientId = 'e5cd7e4891bf95d1d19206ce24a7b32e'
export const token = () => sessionStorage.getItem('yunji.staff.token') || ''
const base = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')
const uncertainOperations=new Map<string,string>()
export async function request<T = any>(method: string, path: string, body?: unknown, suppliedKey?: string): Promise<T> {
  const headers: Record<string,string> = { Accept: 'application/json', clientid: clientId }
  if (token()) headers.Authorization = `Bearer ${token()}`
  if (!(body instanceof FormData)) headers['Content-Type'] = 'application/json'
  const signature=JSON.stringify([token(),method,path,body]),safeReplay=path.startsWith('/api/v1/admin/')&&!(body instanceof FormData)
  const key=suppliedKey||uncertainOperations.get(signature)||crypto.randomUUID().replaceAll('-', '')
  if(method!=='GET'&&safeReplay)uncertainOperations.set(signature,key)
  if (method !== 'GET') headers['Idempotency-Key'] = key
  const init={method,headers,body:body===undefined?undefined:body instanceof FormData?body:JSON.stringify(body)}
  let res:Response
  try{res=await fetch(base+path,init)}catch(e){if(!safeReplay&&method!=='GET')throw e;res=await fetch(base+path,init)}
  const data = await res.json()
  if (!res.ok || data.code !== 200){if(res.status<500)uncertainOperations.delete(signature);throw Object.assign(new Error(data.msg || '请求失败'), { status: res.status })}
  uncertainOperations.delete(signature)
  return (data.data ?? data) as T
}
export const get = <T = any>(path: string, params: Record<string,string|number|undefined> = {}) => request<T>('GET', path + (Object.keys(params).length ? '?' + new URLSearchParams(Object.entries(params).filter(([,v]) => v !== undefined).map(([k,v]) => [k,String(v)])) : ''))
export async function login(username: string, password: string, code = '', uuid = '') { const data = await request('POST', '/auth/login', { username, password, code, uuid, tenantId: '000000', clientId, grantType: 'password' }); sessionStorage.setItem('yunji.staff.token', data.access_token) }
export async function logout() { try { await request('POST', '/auth/logout') } finally { sessionStorage.removeItem('yunji.staff.token') } }
export async function upload(file: File) { const data = new FormData(); data.append('file', file); return request('POST', '/api/v1/admin/files', data) }
export async function download(id: string) { const ticket = await request('POST', `/api/v1/admin/files/${id}/tickets`); const res = await fetch(base + ticket.path, { headers: { Authorization: `Bearer ${token()}`, clientid: clientId } }); if (!res.ok) throw new Error('下载失败'); const blob = await res.blob(); const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = 'download'; a.click(); URL.revokeObjectURL(a.href) }
