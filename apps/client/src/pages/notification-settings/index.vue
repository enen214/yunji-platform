<script setup lang="ts">
import {ref,computed} from 'vue'
import {onShow,onHide} from '@dcloudio/uni-app'
import {request,navigate,token,type Row} from '../../api/platform'
import {abandonLogin as cancelLogin} from '../../features/auth'
import LoginSheet from '../../components/LoginSheet.vue'
import PageState from '../../components/PageState.vue'
const settings=ref<Row|null>(null),error=ref(''),busy=ref(false),loading=ref(false),notice=ref('')
const bound=ref(false),authorized=ref(false)
const localSim = computed(()=>!!(settings.value?.localSimulation))
function sync(){const p=((settings.value?.permissions as Row[])||[])[0]||{};bound.value=!!p.wecomBound;authorized.value=!!p.instantAuthorized}
async function load(){if(!token()){settings.value=null;return}loading.value=true;error.value='';try{settings.value=await request('GET','/api/v1/me/reminders/settings');sync()}catch(e){error.value=(e as Error).message}finally{loading.value=false}}
async function save(){if(!settings.value||busy.value)return;busy.value=true;error.value='';notice.value='';try{settings.value=await request('PUT','/api/v1/me/reminders/settings',{enabled:settings.value.enabled,slot:settings.value.slot,expectedVersion:settings.value.lockVersion});sync();notice.value='提醒设置已保存'}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
async function requestWecom(){if(busy.value)return;busy.value=true;error.value='';notice.value='';try{settings.value=await request('POST','/api/v1/me/reminders/wecom-request',{});sync();notice.value='已提交添加申请，请在企业微信完成添加后回到本页确认绑定'}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
async function bindWecom(){if(busy.value)return;busy.value=true;error.value='';notice.value='';try{settings.value=await request('POST','/api/v1/me/reminders/wecom-link',{});sync();notice.value='已记录企业微信联系关系'}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
async function authorize(){if(busy.value)return;busy.value=true;error.value='';notice.value='';try{const template=String(settings.value?.instantTemplate||'');if(!template)throw new Error('订阅消息模板尚未配置，暂不能授权及时提醒')
// #ifdef MP-WEIXIN
const response=await new Promise<Row>((resolve,reject)=>uni.requestSubscribeMessage({tmplIds:[template],success:resolve,fail:reject}))
settings.value=await request('POST','/api/v1/me/reminders/instant-consent',{templateId:template,result:String(response[template]||'reject')});sync()
notice.value=authorized.value?'本次授权已生效':'未获得订阅授权，及时提醒仍不可用'
// #endif
// #ifndef MP-WEIXIN
throw new Error('请在微信小程序中授权及时提醒')
// #endif
}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
function cancel(){uni.navigateBack({fail:()=>navigate('notifications')})}
onShow(load);onHide(cancelLogin)
</script>
<template>
<view class="page">
  <view class="hero"><text class="subtitle">设置全局提醒时间，并完成企业微信绑定与及时提醒授权。</text></view>
  <PageState :busy="loading" :error="error" @retry="load"/>
  <text v-if="notice" role="status" class="success">{{ notice }}</text>
  <template v-if="settings">
    <view class="reminder-help">
      <view class="keyword-follow-row"><text class="card-title">定时提醒设置</text><text class="badge">{{ settings.enabled?'已开启':'已关闭' }}</text></view>
      <text>所有定时提醒将在你设置的时间推送。一个账号只能设置一个全局提醒时间。</text>
    </view>
    <view class="card">
      <view class="row"><text class="card-title">强提醒总开关</text><switch :checked="settings.enabled" :disabled="busy" color="#50a770" @change="settings.enabled=!!($event as any).detail.value"/></view>
      <text class="field-label">设置提醒时间</text>
      <view class="actions"><button role="button" v-for="item in settings.slots" :key="item" :disabled="busy" :class="settings.slot===item?'':'quiet'" hover-class="yj-hover" @click="settings.slot=item">{{ item }}</button></view>
      <text class="muted">修改后，所有定时提醒将按新时间推送。所选时间是处理时段，不保证精确到达；关闭总开关后暂停尚未发送的任务。</text>
    </view>
    <view class="card">
      <view class="row"><text class="card-title">企业微信绑定</text><text class="badge" :class="bound?'':'orange'">{{ bound?'已绑定':'未添加' }}</text></view>
      <view class="guide-step">
        <text class="step-number">1</text>
        <view>
          <text class="card-title">添加企业微信</text>
          <text class="step-description">点击下方按钮添加云迹小程序企业微信，启用强提醒功能，接收每日定时推送。</text>
          <button role="button" :disabled="busy" :class="bound?'quiet':''" hover-class="yj-hover" @click="requestWecom">{{ bound?'重新绑定企业微信':'立即添加企业微信' }}</button>
        </view>
      </view>
      <view class="guide-step">
        <text class="step-number">2</text>
        <view>
          <text class="card-title">等待添加确认</text>
          <text class="step-description">添加成功后，云迹小程序企业微信会话会提示好友添加完成。</text>
        </view>
      </view>
      <view class="guide-step">
        <text class="step-number">3</text>
        <view>
          <text class="card-title">返回小程序确认</text>
          <text class="step-description">{{ localSim?'点击「我已完成添加」，完成绑定后即可接收提醒。':'返回小程序后，绑定状态会自动同步，同步完成后即可接收提醒。' }}</text>
          <button v-if="localSim" role="button" :disabled="busy||bound" hover-class="yj-hover" @click="bindWecom">{{ bound?'已完成绑定':'我已完成添加，确认绑定' }}</button>
          <text v-else class="step-description">在企业微信中完成添加后，绑定状态会自动同步到本页，无需手动确认。</text>
        </view>
      </view>
      <text class="muted">日常强提醒通过企业微信送达，未绑定好友时日常提醒不生效。</text>
    </view>
    <view class="card">
      <view class="row"><text class="card-title">及时提醒授权</text><text class="badge" :class="authorized?'':'orange'">{{ authorized?'本次授权可用':'尚未授权' }}</text></view>
      <text class="muted">及时提醒走微信一次性订阅消息，每授权一次只能送达一条；不授权则无法开启及时提醒。</text>
      <button role="button" :disabled="busy" hover-class="yj-hover" @click="authorize">{{ authorized?'重新授权一次':'授权一次订阅消息' }}</button>
    </view>
    <view class="bottom-actions">
      <button role="button" class="quiet" :disabled="busy" hover-class="yj-hover" @click="cancel">取消</button>
      <button role="button" :disabled="busy" hover-class="yj-hover" @click="save">确认保存</button>
    </view>
  </template>
  <button v-else-if="!loading&&!error" role="button" hover-class="yj-hover" @click="navigate('login')">登录后设置</button>
  <LoginSheet/>
</view>
</template>
