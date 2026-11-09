<script setup lang="ts">
let statusBarHeight = 0
// #ifdef MP-WEIXIN
statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 0
// #endif
import { ref } from 'vue';import { onBackPress, onHide, onUnload } from '@dcloudio/uni-app';import { request,setToken,navigate } from '../../api/platform'
import { nativeWechatLogin, authorizedPhoneCode, type PhoneAuthorizationEvent } from '../../features/wechat-login'
const consent=ref(false)
let attempt=0
function abandon(){attempt++;busy.value=false}
onHide(abandon);onUnload(abandon)
const code=ref(''),appId=ref('local-client'),error=ref(''),busy=ref(false),isLocal=import.meta.env.DEV
let wechatAvailable=false
// #ifdef MP-WEIXIN
wechatAvailable=true
// #endif
function finish(){
 // Direct H5 login is a legacy deep link; in-page intent replay uses LoginSheet.
 // #ifdef H5
 uni.reLaunch({url:'/pages/index/index'});return
 // #endif
 // #ifndef H5
 if(getCurrentPages().length>1)uni.navigateBack();else uni.reLaunch({url:'/pages/index/index'})
 // #endif
}
function goBack(){if(getCurrentPages().length>1)uni.navigateBack({fail:()=>navigate('index')});else navigate('index')}
onBackPress(e=>{if(e.from==='navigateBack')return false;goBack();return true})
function requireConsent(){if(!consent.value)error.value='请先阅读并同意使用协议与隐私政策'}
function phoneLogin(event:PhoneAuthorizationEvent){
 if(busy.value)return
 if(!consent.value){requireConsent();return}
 try{void signIn(true,authorizedPhoneCode(event))}catch(e){error.value=(e as Error).message}
}
async function signIn(native:boolean,phoneCode?:string){
 if(busy.value)return
 if(!consent.value){requireConsent();return}
 busy.value=true;error.value='';const current=++attempt
 try{
  const data=native?await nativeWechatLogin(phoneCode,()=>current===attempt):await request('POST','/api/v1/auth/customer/local',{code:code.value,appId:appId.value})
  if(current!==attempt)return
  setToken(data.accessToken);finish()
 }catch(e){if(current===attempt)error.value=(e as Error).message||'登录失败，请重试'}finally{if(current===attempt)busy.value=false}
}
</script>
<template><view class="page login-page" :style="{paddingTop: statusBarHeight + 'px'}"><view class="login-topbar"><button role="button" class="quiet login-back" @click="goBack">‹ <text>返回</text></button><text class="login-heading">登录</text><text class="login-placeholder"></text></view><view class="login-brand"><text class="eyebrow">WELCOME TO YUNJI</text><text class="title">登录云迹</text><text class="subtitle">登录后管理订阅、报名和个人资料，让资讯始终在手边。</text></view><view class="card login-card"><view class="meta-line" style="margin-bottom:17px"><text class="badge">安全登录</text><text class="muted">你的身份由平台保护</text></view><template v-if="wechatAvailable"><button role="button" :disabled="busy" :open-type="consent?'getPhoneNumber':''" @click="requireConsent" @getphonenumber="phoneLogin">{{busy?'正在登录…':'手机号一键登录'}}</button><button role="button" class="quiet" :disabled="busy" @click="signIn(true)">普通微信登录（不授权手机号）</button></template><text v-else-if="!isLocal" class="muted">请使用微信小程序登录。</text><template v-if="isLocal"><view class="section-head"><text class="section-title">本地验收登录</text><text class="section-link">仅开发环境</text></view><text class="muted">使用验收工作台签发的单次登录码，有效期两分钟。</text><view class="field"><text class="field-label">登录码</text><input v-model="code" placeholder="粘贴单次 code" /></view><button role="button" :disabled="busy||!code" @click="signIn(false)">登录合成用户</button></template><text class="error">{{ error }}</text></view><text class="muted" style="text-align:center;margin:20px 16px"><checkbox :checked="consent" color="#50a770" @click="consent=!consent"/><text @click="consent=!consent">已阅读并同意</text><text class="inline-link" @click="navigate('page','?key=terms')">《使用协议》</text><text class="inline-link" @click="navigate('page','?key=privacy')">《隐私政策》</text></text></view></template>
