<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { onShow, onHide } from '@dcloudio/uni-app'
import { loginState, cancelLogin, abandonLogin, completeLogin, currentLoginOwner } from '../features/auth'
import { nativeWechatLogin, authorizedPhoneCode, type PhoneAuthorizationEvent } from '../features/wechat-login'
import { setToken, navigate } from '../api/platform'
const owner=ref('')
const agreed=ref(true)
watch(()=>loginState.attempt,()=>{owner.value=currentLoginOwner()})
onMounted(()=>{owner.value=currentLoginOwner()})
onShow(()=>{owner.value=currentLoginOwner()})
function dismissOwned(){if(owner.value===loginState.owner)abandonLogin()}
onHide(dismissOwned)
let wechat=false
// #ifdef MP-WEIXIN
wechat=true
// #endif
function phoneLogin(event:PhoneAuthorizationEvent){
 if(loginState.busy||!loginState.visible)return
 try{void signIn(authorizedPhoneCode(event))}catch(e){loginState.error=(e as Error).message}
}
async function signIn(phoneCode:string) {
 if(loginState.busy)return
 loginState.busy=true;loginState.error=''
 const attempt=loginState.attempt
 try{
  const data=await nativeWechatLogin(phoneCode,()=>attempt===loginState.attempt)
  if(attempt!==loginState.attempt)return
  setToken(data.accessToken);await completeLogin()
 }catch(e){if(attempt===loginState.attempt)loginState.error=(e as Error).message||'登录失败，请重试'}finally{loginState.busy=false}
}
function toggleAgree(){agreed.value=!agreed.value}
function legal(key:string){cancelLogin();navigate('page','?key='+key)}
// A route change must never leave a detached mask intercepting the next page.
onUnmounted(dismissOwned)
// #ifndef MP-WEIXIN
function syncSheetFlag(open:boolean){if(typeof document!=='undefined')document.body.classList[open?'add':'remove']('yj-sheet-open')}
watch(()=>loginState.visible,visible=>syncSheetFlag(!!visible),{immediate:true})
onUnmounted(()=>syncSheetFlag(false))
// #endif
</script>
<template>
<view v-if="loginState.visible && owner===loginState.owner" class="sheet-mask login-mask" @click="cancelLogin">
  <view class="login-sheet" role="dialog" aria-label="登录云迹" @click.stop>
    <view class="sheet-grip"></view>
    <view class="ls-head">
      <text class="ls-title">请登录</text>
      <text class="ls-sub">登录后可使用完整功能</text>
    </view>
    <template v-if="wechat">
      <button role="button" :class="['ls-phone-btn', loginState.busy ? 'is-busy' : '']" :disabled="loginState.busy" open-type="getPhoneNumber" @getphonenumber="phoneLogin">
        <text class="ls-phone-icon"></text>
        <text>{{ loginState.busy ? '正在登录…' : '手机号一键登录' }}</text>
      </button>
    </template>
    <text v-else class="muted">请在微信小程序中登录。</text>
    <view class="ls-agree" @click="toggleAgree">
      <view class="ls-check" :class="{checked: agreed}">
        <view v-if="agreed" class="ls-check-dot"></view>
      </view>
      <text class="ls-agree-text">我已阅读并同意</text>
      <text class="ls-link" @click.stop="legal('user_agreement')">《用户协议》</text>
      <text class="ls-link" @click.stop="legal('privacy_policy')">《隐私政策》</text>
    </view>
    <text v-if="loginState.error" role="alert" class="ls-error">{{ loginState.error }}</text>
    <button role="button" class="ls-skip" :disabled="loginState.busy" @click="cancelLogin">暂不登录</button>
  </view>
</view>
</template>
<style lang="scss" scoped>
.sheet-mask {
  position: fixed; inset: 0; z-index: 1200;
  background: rgba(0,0,0,.45);
  display: flex; align-items: flex-end;
}
.login-sheet {
  width: 100%; box-sizing: border-box;
  background: #fff;
  border-radius: 24px 24px 0 0;
  padding: 12px 24px calc(24px + env(safe-area-inset-bottom));
  display: flex; flex-direction: column; align-items: center;
}
.sheet-grip {
  width: 40px; height: 4px; border-radius: 2px;
  background: #e6e9ed; margin-bottom: 18px;
}
.ls-head { text-align: center; margin-bottom: 22px; }
.ls-title { display: block; font-size: 20px; font-weight: 700; color: #1a1a1a; line-height: 1.4; }
.ls-sub { display: block; font-size: 13px; color: #737c88; margin-top: 4px; }
.ls-phone-btn {
  width: 100%; min-height: 50px; line-height: 50px;
  display: flex; align-items: center; justify-content: center; gap: 8px;
  background: #f2f4f3; color: #1a1a1a;
  border-radius: 999px; border: none;
  font-size: 16px; font-weight: 600;
  margin-bottom: 14px;
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
  margin: 8px 0 4px; gap: 2px;
}
.ls-check {
  width: 18px; height: 18px; border-radius: 50%;
  border: 1.5px solid #c4cbd2; box-sizing: border-box;
  display: flex; align-items: center; justify-content: center;
  margin-right: 6px; flex-shrink: 0;
}
.ls-check.checked { border-color: #50a770; }
.ls-check-dot {
  width: 10px; height: 10px; border-radius: 50%; background: #50a770;
}
.ls-agree-text { font-size: 12px; color: #737c88; }
.ls-link { font-size: 12px; color: #50a770; }
.ls-error { display: block; font-size: 13px; color: #bd5847; text-align: center; margin-top: 8px; }
.ls-error:empty { display: none; }
.ls-skip {
  margin-top: 14px; background: transparent; border: none; color: #1a1a1a;
  font-size: 15px; font-weight: 600; line-height: 1.4;
}
.ls-skip::after { border: none; }
</style>
