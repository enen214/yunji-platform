import { request } from '../api/platform'

export type PhoneAuthorizationEvent = { detail?: { code?: string; errMsg?: string } }

export function authorizedPhoneCode(event: PhoneAuthorizationEvent): string {
  const code = event.detail?.code
  const errMsg = event.detail?.errMsg || ''
  if (code && errMsg === 'getPhoneNumber:ok') return code
  // 技术原因只进控制台；界面文案必须短，且只能指向本页真实存在的操作（一键登录 / 暂不登录）。
  if (/deny|cancel/i.test(errMsg)) {
    console.warn('[wechat-login] 用户取消手机号授权:', errMsg)
    throw new Error('你取消了手机号授权，再点一次可重新授权')
  }
  if (/-10000|fail/i.test(errMsg)) {
    console.warn('[wechat-login] getPhoneNumber 未成功:', errMsg)
    throw new Error('微信授权未成功，请重试一次')
  }
  console.warn('[wechat-login] 未知授权结果:', errMsg)
  throw new Error('未获得手机号授权，请重试一次')
}

/** Obtain fresh, separate identity and phone codes. Never retry either code automatically. */
export async function nativeWechatLogin(phoneCode?: string, isCurrent: () => boolean = () => true) {
  // The runtime AppID is authoritative; no AppSecret is present in the client.
  const appId = uni.getAccountInfoSync().miniProgram.appId
  if (!/^wx[0-9a-f]{16}$/.test(appId)) throw new Error('请配置真实小程序 AppID 后再登录')
  const result = await new Promise<UniApp.LoginRes>((resolve, reject) => uni.login({
    provider: 'weixin',
    success: resolve,
    fail: (res) => {
      console.warn('[wechat-login] uni.login 失败:', res)
      reject(new Error('微信登录暂不可用，请重试'))
    }
  }))
  if (!isCurrent()) throw new Error('本次登录已取消')
  if (!result.code) throw new Error('未取得微信登录码，请重试')
  return request('POST', '/api/v1/auth/customer/wechat/native', {
    appId, code: result.code, ...(phoneCode ? { phoneCode } : {})
  })
}
