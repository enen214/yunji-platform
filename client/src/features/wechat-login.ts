import { request } from '../api/platform'

export type PhoneAuthorizationEvent = { detail?: { code?: string; errMsg?: string } }

export function authorizedPhoneCode(event: PhoneAuthorizationEvent): string {
  const code = event.detail?.code
  if (!code || event.detail?.errMsg !== 'getPhoneNumber:ok') {
    throw new Error('未获得手机号授权，可重新授权或选择普通微信登录')
  }
  return code
}

/** Obtain fresh, separate identity and phone codes. Never retry either code automatically. */
export async function nativeWechatLogin(phoneCode?: string, isCurrent: () => boolean = () => true) {
  // The runtime AppID is authoritative; no AppSecret is present in the client.
  const appId = uni.getAccountInfoSync().miniProgram.appId
  if (!/^wx[0-9a-f]{16}$/.test(appId)) throw new Error('请配置真实小程序 AppID 后再登录')
  const result = await new Promise<UniApp.LoginRes>((resolve, reject) => uni.login({
    provider: 'weixin', success: resolve, fail: () => reject(new Error('微信登录暂不可用，请重试'))
  }))
  if (!isCurrent()) throw new Error('本次登录已取消')
  if (!result.code) throw new Error('未取得微信登录码，请重试')
  return request('POST', '/api/v1/auth/customer/wechat/native', {
    appId, code: result.code, ...(phoneCode ? { phoneCode } : {})
  })
}
