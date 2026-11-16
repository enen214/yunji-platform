<script setup lang="ts">
import {onHide as onPageHide} from "@dcloudio/uni-app"
import {abandonLogin as cancelPageLogin} from "../../features/auth"
onPageHide(cancelPageLogin)

import LoginSheet from "../../components/LoginSheet.vue"

import {ref} from 'vue';import {onLoad,onShow} from '@dcloudio/uni-app';import {request,navigate,token,type Row} from '../../api/platform'
const requestedMode=ref('')
const id=ref(''),targetId=ref(''),targetType=ref('ARTICLE'),mode=ref('DAILY'),enabled=ref(true),version=ref(0),title=ref(''),settings=ref<Row|null>(null),error=ref(''),busy=ref(false),signedIn=ref(false)
onLoad(q=>{requestedMode.value=String(q?.mode||'');id.value=String(q?.id||'');targetId.value=String(q?.targetId||q?.articleId||'');targetType.value=String(q?.targetType||'ARTICLE')})
onShow(async()=>{signedIn.value=!!token();if(!signedIn.value)return;try{error.value='';settings.value=await request('GET','/api/v1/me/reminders/settings');if(!id.value&&targetId.value){const found=await request('GET','/api/v1/me/reminders/rules?size=1&targetType='+targetType.value+'&targetId='+targetId.value);if(found.items.length)id.value=found.items[0].id}if(id.value){const r=await request('GET','/api/v1/me/reminders/rules/'+id.value);targetId.value=r.targetId;targetType.value=r.targetType;mode.value=r.mode;enabled.value=!!r.enabled;version.value=r.lockVersion;title.value=r.title||''}else if(targetId.value&&targetType.value==='ARTICLE'){const d=await request('GET','/api/v1/articles/'+targetId.value);title.value=d.article.title}if(requestedMode.value==='INSTANT')mode.value='INSTANT'}catch(e){error.value=(e as Error).message}})
async function save(){if(busy.value)return;busy.value=true;error.value='';try{if(mode.value==='INSTANT'&&!settings.value?.localSimulation){
// #ifdef MP-WEIXIN
if(!settings.value?.realProviderReady||!settings.value?.instantTemplate)throw new Error('平台接入尚未完成，暂不能开启即时提醒');
const template=settings.value.instantTemplate;const response=await new Promise<Row>((resolve,reject)=>uni.requestSubscribeMessage({tmplIds:[template],success:resolve,fail:reject}));if(response[template]!=='accept')throw new Error('尚未获得本次订阅授权，提醒方式未改变');
// #endif
// #ifndef MP-WEIXIN
throw new Error('请在微信小程序中授权即时提醒');
// #endif
}
await request(id.value?'PUT':'POST','/api/v1/me/reminders/rules'+(id.value?'/'+id.value:''),{targetType:targetType.value,targetId:targetId.value,mode:mode.value,enabled:enabled.value,expectedVersion:version.value});uni.redirectTo({url:'/pages/notifications/index'})}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
</script><template><view class="page"><view class="hero"><text class="title">{{ id?'编辑跟踪':'设置强提醒' }}</text><text class="subtitle">{{ title||'从下一次符合条件的更新开始提醒。' }}</text></view><text class="error">{{ error }}</text><button role="button" v-if="!signedIn" @click="navigate('login')">登录</button><view v-else-if="!targetId" class="card"><text>请选择要跟踪的资讯或关键词</text><button role="button" @click="navigate('notification-add')">选择资讯与关键词</button><button role="button" class="quiet" @click="navigate('follows')">我的关注</button></view><view v-else class="card"><text class="field-label">提醒方式</text><picker :disabled="busy" :range="['日常跟踪（企微）','下一次即时（微信订阅）']" :value="mode==='INSTANT'?1:0" @change="mode=Number($event.detail.value)===1?'INSTANT':'DAILY'"><view class="picker">{{ mode==='DAILY'?'日常跟踪（企微）':'下一次即时（微信订阅）' }}</view></picker><text class="muted">{{ mode==='DAILY'?'有新变化才汇总；企微任务需工作人员执行发送。':'使用一次性订阅授权，确定发送成功后恢复日常跟踪。' }}</text><text v-if="settings?.localSimulation" class="muted">本地验收使用模拟授权，不会发送真实消息。</text><button role="button" :disabled="busy" @click="save">保存提醒</button><button role="button" class="quiet" @click="navigate('notifications')">返回提醒列表</button></view><LoginSheet/></view></template>
