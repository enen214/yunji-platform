<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { onShow, onHide } from '@dcloudio/uni-app'
import { loginState, cancelLogin, abandonLogin, completeLogin, currentLoginOwner } from '../features/auth'
import { nativeWechatLogin, authorizedPhoneCode, type PhoneAuthorizationEvent } from '../features/wechat-login'
import { request, setToken, navigate } from '../api/platform'
const code=ref(''), consent=ref(false), local=import.meta.env.DEV
const owner=ref('')
watch(()=>loginState.attempt,()=>{if(!loginState.busy){code.value='';consent.value=false}})
onMounted(()=>{owner.value=currentLoginOwner()})
onShow(()=>{owner.value=currentLoginOwner()})
function dismissOwned(){if(owner.value===loginState.owner)abandonLogin()}
onHide(dismissOwned)
let wechat=false
// #ifdef MP-WEIXIN
wechat=true
// #endif
function requireConsent(){if(!consent.value)loginState.error='请先阅读并同意使用协议与隐私政策'}
function phoneLogin(event:PhoneAuthorizationEvent){
 if(loginState.busy||!loginState.visible||owner.value!==loginState.owner)return
 if(!consent.value){requireConsent();return}
 try{void signIn(true,authorizedPhoneCode(event))}catch(e){loginState.error=(e as Error).message}
}
async function signIn(native:boolean,phoneCode?:string) {
 if(loginState.busy)return
 if(!consent.value){loginState.error='请先阅读并同意使用协议与隐私政策';return}
 loginState.busy=true;loginState.error=''
 const attempt=loginState.attempt
 try{
  let data
  if(native)data=await nativeWechatLogin(phoneCode,()=>attempt===loginState.attempt)
  else data=await request('POST','/api/v1/auth/customer/local',{code:code.value,appId:'local-client'})
  if(attempt!==loginState.attempt)return
  setToken(data.accessToken);code.value='';await completeLogin()
 }catch(e){if(attempt===loginState.attempt)loginState.error=(e as Error).message||'登录失败，请重试'}finally{if(attempt===loginState.attempt)loginState.busy=false}
}
function legal(key:string){cancelLogin();navigate('page','?key='+key)}
// A route change must never leave a detached mask intercepting the next page.
onUnmounted(dismissOwned)
</script>
<template><view v-if="loginState.visible && owner===loginState.owner" class="sheet-mask login-mask" @click="cancelLogin"><view class="filter-sheet login-sheet" role="dialog" aria-label="登录云迹" @click.stop><view class="sheet-grip"></view><view class="row"><text class="section-title">请登录</text><button role="button" class="quiet icon-button" :disabled="loginState.busy" aria-label="关闭登录" @click="cancelLogin">×</button></view><text class="muted">登录云迹，保存关注与收藏，接收你关心的更新。</text><template v-if="wechat"><button role="button" class="login-primary" :disabled="loginState.busy" :open-type="consent?'getPhoneNumber':''" @click="requireConsent" @getphonenumber="phoneLogin">{{ loginState.busy?'正在登录…':'手机号一键登录' }}</button><button role="button" class="quiet" :disabled="loginState.busy" @click="signIn(true)">普通微信登录（不授权手机号）</button></template><text v-else-if="!local" class="muted">请在微信小程序中登录。</text><template v-if="local"><text class="field-label">本地验收登录码</text><input v-model="code" placeholder="粘贴工作台签发的单次 code"/><button role="button" class="login-primary" :disabled="loginState.busy||!code" @click="signIn(false)">登录合成用户</button></template><view class="consent-row"><checkbox :checked="consent" color="#50a770" :disabled="loginState.busy" @click="consent=!consent"/><view><text @click="consent=!consent">已阅读并同意</text><text class="inline-link" @click="legal('terms')">《使用协议》</text><text class="inline-link" @click="legal('privacy')">《隐私政策》</text></view></view><text v-if="loginState.error" role="alert" class="error">{{ loginState.error }}</text><button role="button" class="quiet dismiss-login" :disabled="loginState.busy" @click="cancelLogin">暂不登录</button></view></view></template>
