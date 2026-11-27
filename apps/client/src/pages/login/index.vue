<script setup lang="ts">
let statusBarHeight = 0
// #ifdef MP-WEIXIN
statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 0
// #endif
import { ref } from 'vue'
import { onBackPress, onHide, onUnload } from '@dcloudio/uni-app'
import { setToken, navigate } from '../../api/platform'
import { nativeWechatLogin, authorizedPhoneCode, type PhoneAuthorizationEvent } from '../../features/wechat-login'
let attempt=0
function abandon(){attempt++;busy.value=false}
onHide(abandon);onUnload(abandon)
const error=ref(''),busy=ref(false),agreed=ref(true)
let wechatAvailable=false
// #ifdef MP-WEIXIN
wechatAvailable=true
// #endif
function finish(){
 // #ifdef H5
 uni.reLaunch({url:'/pages/index/index'});return
 // #endif
 // #ifndef H5
 if(getCurrentPages().length>1)uni.navigateBack();else uni.reLaunch({url:'/pages/index/index'})
 // #endif
}
function goBack(){if(getCurrentPages().length>1)uni.navigateBack({fail:()=>uni.reLaunch({url:'/pages/index/index'})});else uni.reLaunch({url:'/pages/index/index'})}
onBackPress(e=>{if(e.from==='navigateBack')return false;goBack();return true})
function phoneLogin(event:PhoneAuthorizationEvent){
 if(busy.value)return
 try{void signIn(authorizedPhoneCode(event))}catch(e){error.value=(e as Error).message}
}
async function signIn(phoneCode:string){
 if(busy.value)return
 busy.value=true;error.value='';const current=++attempt
 try{
  const data=await nativeWechatLogin(phoneCode,()=>current===attempt)
  if(current!==attempt)return
  setToken(data.accessToken);finish()
 }catch(e){if(current===attempt)error.value=(e as Error).message||'登录失败，请重试'}finally{if(current===attempt)busy.value=false}
}
function toggleAgree(){agreed.value=!agreed.value}
function legal(key:string){navigate('page','?key='+key)}
</script>
<template>
<view class="page login-page" :style="{paddingTop: statusBarHeight + 'px'}">
  <view class="login-topbar">
    <button role="button" class="quiet login-back" @click="goBack">‹ <text>返回</text></button>
    <text class="login-heading">登录</text>
    <text class="login-placeholder"></text>
  </view>
  <view class="login-brand">
    <text class="title">登录云迹</text>
    <text class="subtitle">登录后管理订阅、报名和个人资料</text>
  </view>
  <view class="login-main">
    <template v-if="wechatAvailable">
      <button role="button" :class="['ls-phone-btn', busy ? 'is-busy' : '']" :disabled="busy" open-type="getPhoneNumber" @getphonenumber="phoneLogin">
        <text class="ls-phone-icon"></text>
        <text>{{ busy ? '正在登录…' : '手机号一键登录' }}</text>
      </button>
    </template>
    <text v-else class="muted">请使用微信小程序登录。</text>
    <view class="ls-agree" hover-class="yj-hover" @click="toggleAgree">
      <view class="ls-check" :class="{checked: agreed}">
        <view v-if="agreed" class="ls-check-dot"></view>
      </view>
      <text class="ls-agree-text">我已阅读并同意</text>
      <text class="ls-link" @click.stop="legal('user_agreement')">《用户协议》</text>
      <text class="ls-link" @click.stop="legal('privacy_policy')">《隐私政策》</text>
    </view>
    <text v-if="error" role="alert" class="ls-error">{{ error }}</text>
    <button role="button" class="login-skip" :disabled="busy" @click="abandon();goBack()">暂不登录</button>
  </view>
</view>
</template>
<style lang="scss" scoped>
.login-topbar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 6px 0;
}
.login-back { background: transparent; border: none; color: #1a1a1a; font-size: 15px; padding: 4px 8px; }
.login-back::after { border: none; }
.login-heading { font-size: 16px; font-weight: 700; color: #1a1a1a; }
.login-placeholder { width: 48px; }
.login-brand { padding: 32px 8px 24px; }
.login-brand .title { font-size: 26px; font-weight: 700; color: #1a1a1a; }
.login-brand .subtitle { font-size: 14px; color: #737c88; margin-top: 8px; display: block; }
.login-main { padding: 0 8px; }
.ls-phone-btn {
  width: 100%; min-height: 50px; line-height: 50px;
  display: flex; align-items: center; justify-content: center; gap: 8px;
  background: #f2f4f3; color: #1a1a1a;
  border-radius: 999px; border: none;
  font-size: 16px; font-weight: 600;
  margin-bottom: 16px;
}
.ls-phone-btn::after { border: none; }
.ls-phone-btn.is-busy { opacity: .6; }
.ls-phone-icon {
  width: 18px; height: 18px; flex-shrink: 0;
  border: 1.5px solid #1a1a1a; border-radius: 4px; position: relative;
}
.ls-phone-icon::before {
  content: ''; position: absolute; left: 50%; bottom: 1px; transform: translateX(-50%);
  width: 6px; height: 1.5px; background: #1a1a1a; border-radius: 1px;
}
.ls-agree {
  display: flex; align-items: center; justify-content: center; flex-wrap: wrap;
  margin: 8px 0; gap: 2px;
}
.ls-check {
  width: 18px; height: 18px; border-radius: 50%;
  border: 1.5px solid #c4cbd2; box-sizing: border-box;
  display: flex; align-items: center; justify-content: center;
  margin-right: 6px; flex-shrink: 0;
}
.ls-check.checked { border-color: #50a770; }
.ls-check-dot { width: 10px; height: 10px; border-radius: 50%; background: #50a770; }
.ls-agree-text { font-size: 12px; color: #737c88; }
.ls-link { font-size: 12px; color: #50a770; }
.ls-error { display: block; font-size: 13px; color: #bd5847; text-align: center; margin-top: 8px; }
.ls-error:empty { display: none; }
.login-skip { background: transparent; border: none; color: #1a1a1a; font-size: 15px; font-weight: 600; line-height: 1.4; margin-top: 14px; }
.login-skip::after { border: none; }
</style>