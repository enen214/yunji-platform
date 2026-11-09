import { requestLogin } from '../features/auth'
let base = (import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8088').replace(/\/$/, '')
// #ifdef H5
if (!import.meta.env.VITE_API_BASE_URL) base = ''
// #endif
export type Row = Record<string,any>
export function token(): string { return String(uni.getStorageSync('yunji.customer.token') || '') }
export function setToken(value: string) { uni.setStorageSync('yunji.customer.token',value) }
const uncertainOperations=new Map<string,string>()
export function operationKey():string{return Date.now().toString(36)+Math.random().toString(36).slice(2)}
export function request<T = any>(method: 'GET'|'POST'|'PUT'|'DELETE', path: string, data?: unknown, suppliedKey?: string): Promise<T> {
 const snapshot=data===undefined?undefined:JSON.parse(JSON.stringify(data)),authorization=token()
 const signature=JSON.stringify([authorization,method,path,snapshot]),replaySafe=method==='GET'||path.startsWith('/api/v1/me/')||path.startsWith('/api/v1/admin/')
 const key=suppliedKey||uncertainOperations.get(signature)||operationKey()
 if(method!=='GET'&&replaySafe)uncertainOperations.set(signature,key)
 const header: Record<string,string>={Accept:'application/json','Content-Type':'application/json'}
 if(authorization)header.Authorization='Bearer '+authorization
 if(method!=='GET')header['Idempotency-Key']=key
 return new Promise((resolve,reject)=>{
  function send(attempt:number){uni.request({url:base+path,method,data:snapshot,header,timeout:15000,success(res){
   const body=res.data as Row
   if(res.statusCode>=200&&res.statusCode<300&&body.code===200){uncertainOperations.delete(signature);resolve(body.data as T)}
   else{if(res.statusCode===401)setToken('');if(res.statusCode<500)uncertainOperations.delete(signature);reject(Object.assign(new Error(body.msg||'请求失败'),{status:res.statusCode,body}))}
  },fail(){if(replaySafe&&attempt===0){send(1);return}reject(new Error('连接失败，请检查网络后重试'))}})}
  send(0)
 })
}
export async function uploadFile(path: string): Promise<Row> { return new Promise((resolve,reject)=>uni.uploadFile({url:base+'/api/v1/me/files',filePath:path,name:'file',header:{Authorization:'Bearer '+token(),'Idempotency-Key':operationKey()},success(res){try{const body=JSON.parse(res.data);if(res.statusCode===200&&body.code===200)resolve(body.data);else reject(new Error(body.msg))}catch{reject(new Error('文件上传失败'))}},fail(){reject(new Error('文件上传失败'))}})) }
export async function chooseMaterial(): Promise<Row> {
 // #ifdef H5
 return new Promise((resolve,reject)=>{
  const input=document.createElement('input');input.type='file';input.accept='.pdf,.png,.jpg,.jpeg,.txt';input.style.display='none';document.body.appendChild(input)
  input.oncancel=()=>{input.remove();reject(new Error('已取消文件选择'))}
  input.onchange=async()=>{const file=input.files?.[0];input.remove();if(!file){reject(new Error('请选择文件'));return}try{const form=new FormData();form.append('file',file);const response=await fetch(base+'/api/v1/me/files',{method:'POST',headers:{Authorization:'Bearer '+token(),'Idempotency-Key':operationKey()},body:form});const body=await response.json();if(!response.ok||body.code!==200)throw new Error(body.msg||'上传失败');resolve({...body.data,localName:file.name})}catch(e){reject(e)}};input.click()
 })
 // #endif
 // #ifndef H5
 return new Promise((resolve,reject)=>uni.chooseMessageFile({count:1,type:'file',success:async r=>{try{const file=r.tempFiles[0];resolve({...await uploadFile(file.path),localName:file.name})}catch(e){reject(e)}},fail(){reject(new Error('已取消文件选择'))}}))
 // #endif
}
export async function downloadFile(id: string): Promise<void> {
 const t = await request<Row>('POST',`/api/v1/me/files/${id}/tickets`)
 // #ifdef H5
 const response=await fetch(base+t.path,{headers:{Authorization:'Bearer '+token()}})
 if(!response.ok)throw new Error('下载失败，请重新获取下载票据')
 const blob=await response.blob(),href=URL.createObjectURL(blob),link=document.createElement('a')
 const disposition=response.headers.get('Content-Disposition')||'',encoded=disposition.match(/filename\*=UTF-8''([^;]+)/i)
 let name='yunji-download';try{if(encoded)name=decodeURIComponent(encoded[1]);else name=disposition.match(/filename="([^"]+)"/)?.[1]||name}catch{}
 link.href=href;link.download=name;document.body.appendChild(link);link.click();link.remove();setTimeout(()=>URL.revokeObjectURL(href),1000)
 return
 // #endif
 // #ifndef H5
 return new Promise((resolve,reject)=>{
  uni.downloadFile({url:base+t.path,header:{Authorization:'Bearer '+token()},
   success(r){
    if(r.statusCode!==200){reject(new Error('下载失败'));return}
    uni.openDocument({filePath:r.tempFilePath,showMenu:true,success(){resolve()},fail(){reject(new Error('已获取文件，当前设备无法打开此格式'))}})
   },
   fail(){reject(new Error('下载失败'))}
  })
 })
 // #endif
}
// Cloud URLs are signed server-side; never receive or construct cloud fileIDs here.
export async function downloadLink(link:Row,name:string):Promise<void>{
 const url=link.url||(base+link.path);const header:Record<string,string>=link.url?{}:{Authorization:'Bearer '+token()}
 // #ifdef H5
 if(link.url){const response=await fetch(link.url);if(!response.ok)throw new Error('云文件暂不可下载');const href=URL.createObjectURL(await response.blob()),a=document.createElement('a');a.href=href;a.download=name;a.click();setTimeout(()=>URL.revokeObjectURL(href),1000);await request('POST','/api/v1/me/library-downloads/complete',{ticket:link.ticket});return}
 const r=await fetch(url,{headers:header});if(!r.ok)throw new Error('下载链接已失效，请重试');const href=URL.createObjectURL(await r.blob()),a=document.createElement('a');a.href=href;a.download=name;a.click();setTimeout(()=>URL.revokeObjectURL(href),1000);await request('POST','/api/v1/me/library-downloads/complete',{ticket:link.ticket});return
 // #endif
 // #ifndef H5
 return new Promise((resolve,reject)=>uni.downloadFile({url,header,async success(r){if(r.statusCode!==200){reject(new Error('下载失败，请重新获取链接'));return}try{await request('POST','/api/v1/me/library-downloads/complete',{ticket:link.ticket})}catch(e){reject(e);return}uni.openDocument({filePath:r.tempFilePath,showMenu:true,success(){resolve()},fail(){reject(new Error('文件已获取，当前设备无法预览此格式'))}})},fail(){reject(new Error('下载失败'))}}))
 // #endif
}
const privatePages=new Set(['profile','favorites','history','registrations','applications','notifications','notification-add','notification-edit','notification-settings','notification-status','notification-history','notification-legacy','follows','follow-items','tag-tree','feedback'])
export function navigate(page: string,query='') {
 if(page==='login'){requestLogin();return}
 if(privatePages.has(page)&&!token()){requestLogin(()=>navigate(page,query));return}
 const url='/pages/'+page+'/index'+query
 if(!query&&['index','subscriptions','boards','me'].includes(page))uni.switchTab({url});else uni.navigateTo({url})
}
export async function authenticated(action:()=>void|Promise<void>){if(token())await action();else requestLogin(action)}
export async function event(type: string,objectId?: string,payload: Row={}) {if(!token())return;try{await request('POST','/api/v1/me/events',{eventId:Date.now().toString(36)+Math.random().toString(36).slice(2),type,objectId,occurredAt:new Date().toISOString(),payload})}catch{}}
