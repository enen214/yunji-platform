import { reactive } from 'vue'

// Only the user's current intent is retained, in memory, for this login attempt.
export const loginState = reactive({ visible: false, busy: false, error: '', owner: '', attempt: 0 })
export function currentLoginOwner() {
  // H5 hash history may retain pages out of stack order after browser navigation.
  // Use the visible document route; native mini-programs keep their real page stack.
  // #ifdef H5
  return window.location.hash.slice(1) || '/pages/index/index'
  // #endif
  // #ifndef H5
  const pages=getCurrentPages(),page=pages[pages.length-1] as unknown as {route?:string;$page?:{id?:number;fullPath?:string}}
  return page ? `${page.route}:${page.$page?.id??page.$page?.fullPath??''}:${pages.length}` : ''
  // #endif
}
let continuation: (() => void | Promise<void>) | undefined
export function requestLogin(next?: () => void | Promise<void>) {
  if (loginState.busy) return
  continuation = next
  loginState.attempt++
  loginState.owner = currentLoginOwner()
  loginState.error = ''
  loginState.visible = true
}
export function cancelLogin() {
  if (loginState.busy) return
  abandonLogin()
}
export function abandonLogin() {
  loginState.attempt++
  loginState.busy = false
  continuation = undefined
  loginState.visible = false
  loginState.error = ''
}
export async function completeLogin() {
  const next = continuation
  continuation = undefined
  loginState.visible = false
  uni.$emit('customer-authenticated')
  if (next) await next()
  else {
    // #ifdef H5
    uni.reLaunch({url:currentLoginOwner()})
    return
    // #endif
    // #ifndef H5
    const page=getCurrentPages().slice(-1)[0] as unknown as {route:string;options?:Record<string,string>}
    if(page){const query=Object.entries(page.options||{}).map(([k,v])=>encodeURIComponent(k)+'='+encodeURIComponent(v)).join('&');uni.reLaunch({url:'/'+page.route+(query?'?'+query:'')})}
    // #endif
  }
}
