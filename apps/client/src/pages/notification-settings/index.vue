<script setup lang="ts">
import {ref} from 'vue'
import {onShow,onHide} from '@dcloudio/uni-app'
import {request,navigate,token,type Row} from '../../api/platform'
import {abandonLogin as cancelLogin} from '../../features/auth'
import LoginSheet from '../../components/LoginSheet.vue'
import PageState from '../../components/PageState.vue'
const settings=ref<Row|null>(null),error=ref(''),busy=ref(false),loading=ref(false),notice=ref('')
const bound=ref(false),authorized=ref(false)
function sync(){const p=((settings.value?.permissions as Row[])||[])[0]||{};bound.value=!!p.wecomBound;authorized.value=!!p.instantAuthorized}
async function load(){if(!token()){settings.value=null;return}loading.value=true;error.value='';try{settings.value=await request('GET','/api/v1/me/reminders/settings');sync()}catch(e){error.value=(e as Error).message}finally{loading.value=false}}
async function save(){if(!settings.value||busy.value)return;busy.value=true;error.value='';notice.value='';try{settings.value=await request('PUT','/api/v1/me/reminders/settings',{enabled:settings.value.enabled,slot:settings.value.slot,expectedVersion:settings.value.lockVersion});sync();notice.value='提醒设置已保存'}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
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
<template><view class="page"><view class="hero"><text class="title">强提醒设置</text><text class="subtitle">一个账号设置一个日常处理时段，全部日常规则共用。</text></view><PageState :busy="loading" :error="error" @retry="load"/><text v-if="notice" role="status" class="success">{{ notice }}</text><template v-if="settings"><view class="card"><view class="row"><text class="card-title">接收更新提醒</text><switch :checked="settings.enabled" :disabled="busy" color="#50a770" @change="settings.enabled=!!($event as any).detail.value"/></view><text class="field-label">日常提醒时段 · {{ settings.timezone }}</text><view class="actions"><button role="button" v-for="slot in settings.slots" :key="slot" :disabled="busy" :class="settings.slot===slot?'':'quiet'" @click="settings.slot=slot">{{ slot }}</button></view><text class="muted">系统整理未读更新并创建企微任务，由工作人员在企微发送；所选时间是处理时段，不保证精确到达。关闭后暂停尚未发送的任务。</text></view><view class="card"><text class="card-title">企业微信联系关系</text><text class="badge">{{ bound?'已记录联系关系':'尚未添加企业微信，日常提醒不可用' }}</text><text class="muted">日常强提醒通过企业微信送达。未添加好友时日常提醒不生效，请先在企业微信添加云迹客服，再点下面的按钮確認。</text><button role="button" v-if="!bound" :disabled="busy" @click="bindWecom">我已添加企业微信好友</button></view><view class="card"><text class="card-title">及时提醒授权</text><text class="badge">{{ authorized?'本次授权可用':'尚未授权' }}</text><text class="muted">及时提醒走微信一次性订阅消息，每授权一次只能送达一条；不授权则无法开启及时提醒。</text><button role="button" :disabled="busy" @click="authorize">{{ authorized?'重新授权一次':'授权一次订阅消息' }}</button></view><view class="bottom-actions"><button role="button" class="quiet" :disabled="busy" @click="cancel">取消</button><button role="button" :disabled="busy" @click="save">保存设置</button></view></template><button role="button" v-else-if="!loading&&!error" @click="navigate('login')">登录后设置</button><LoginSheet/></view></template>
