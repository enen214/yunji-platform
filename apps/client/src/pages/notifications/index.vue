<script setup lang="ts">
import {computed,ref} from 'vue'
import {onShow,onHide} from '@dcloudio/uni-app'
import {request,navigate,token,type Row} from '../../api/platform'
import {abandonLogin} from '../../features/auth'
import LoginSheet from '../../components/LoginSheet.vue'
import PageState from '../../components/PageState.vue'

type Tab='daily'|'instant'|'follow'
const tab=ref<Tab>('daily'),rules=ref<Row[]>([]),keywords=ref<Row[]>([]),counts=ref<Record<Tab,number>>({daily:0,instant:0,follow:0}),error=ref(''),notice=ref(''),loading=ref(false),busy=ref(false),signedIn=ref(false),page=ref(1),total=ref(0),instantAuthorized=ref(false),slot=ref(''),revealed=ref('')
const filters:Record<string,string>={daily:'mode=DAILY',instant:'mode=INSTANT',follow:''}
const typeText:Record<string,string>={BASIC:'基础信息',NOTICE:'通知',GUIDE:'攻略'}
const emptyText:Record<Tab,string>={
  daily:'暂无日常强提醒。点击下方「新增强提醒」，把关心的信息加入每日汇总。',
  instant:'暂无及时提醒。设置后，触发时通过微信订阅消息立即推送。',
  follow:'暂无关注强提醒。对关键词开启强提醒后，这里会按关键词汇总新信息。'
}
const isEmpty=computed(()=>tab.value==='follow'?!keywords.value.length:!rules.value.length)
const newTotal=computed(()=>keywords.value.reduce((sum,row)=>sum+Number(row.newCount||0),0))
function typeLabel(rule:Row){return rule.targetType==='KEYWORD'?'关键词':(typeText[String(rule.contentType)]||'资讯')}
function statusText(rule:Row){if(rule.mode==='INSTANT')return rule.enabled?(instantAuthorized.value?'已授权':'授权已用完'):'已关闭';return Number(rule.pendingCount||0)>0?'待触发':'暂无更新'}
function summaryText(){if(tab.value==='daily')return '日常强提醒 '+counts.value.daily+' 条规则 · 每天 '+(slot.value||'--:--')+' 汇总推送';if(tab.value==='instant')return '及时提醒 '+counts.value.instant+' 条规则 · 触发时立即推送'+(instantAuthorized.value?'':'（订阅授权已用完）');return '关注的关键词下有 '+newTotal.value+' 条新信息待查看'}
function showHelp(){uni.showModal({title:'强提醒怎么用？',content:'强提醒分两种：每日汇总（企业微信，按你设置的时间一次推送）和及时提醒（微信订阅消息，触发时立刻送达）。除你自己设置的内容外，强提醒不会额外打扰你。',showCancel:false,confirmText:'知道了'})}
async function loadMeta(raise:boolean){
  try{
    const [daily,instant,settings,keywordsResult]=await Promise.all([
      request('GET','/api/v1/me/reminders/rules?size=1&mode=DAILY'),
      request('GET','/api/v1/me/reminders/rules?size=1&mode=INSTANT'),
      request('GET','/api/v1/me/reminders/settings'),
      request<Row[]>('GET','/api/v1/me/reminders/keywords')
    ])
    counts.value.daily=Number(daily.total||0);counts.value.instant=Number(instant.total||0)
    keywords.value=keywordsResult||[];counts.value.follow=keywords.value.length
    slot.value=String(settings.slot||'')
    instantAuthorized.value=Boolean(((settings.permissions as Row[])||[])[0]?.instantAuthorized)
  }catch(e){if(raise&&!error.value)error.value=(e as Error).message}
}
async function load(){
  signedIn.value=!!token()
  if(!signedIn.value){rules.value=[];keywords.value=[];return}
  loading.value=true;error.value='';notice.value=''
  try{
    if(tab.value==='follow'){
      rules.value=[];total.value=0
      await loadMeta(true)
    }else{
      const result=await request('GET','/api/v1/me/reminders/rules?page='+page.value+'&size=20&'+filters[tab.value])
      rules.value=result.items||[];total.value=Number(result.total||0)
      void loadMeta(false)
    }
  }catch(e){error.value=(e as Error).message;if(!token())signedIn.value=false}finally{loading.value=false}
}
function switchTab(value:Tab){if(loading.value||busy.value)return;tab.value=value;page.value=1;rules.value=[];void load()}
function openTarget(rule:Row){navigate(rule.targetType==='ARTICLE'?'article':'keyword-updates','?id='+rule.targetId)}
function toInstant(rule:Row){navigate('notification-edit','?id='+rule.id+'&mode=INSTANT')}
let sx=0,sy=0,swiped=false
function touchStart(e:Row,rule:Row){swiped=false;if(revealed.value&&revealed.value!==String(rule.id))revealed.value='';const t=e.changedTouches?.[0];sx=t?.clientX||0;sy=t?.clientY||0}
function touchEnd(e:Row,rule:Row){if(tab.value!=='daily'||rule.targetType!=='ARTICLE')return;const t=e.changedTouches?.[0];const dx=(t?.clientX??sx)-sx,dy=(t?.clientY??sy)-sy;if(dx>=100&&Math.abs(dy)<=30&&dx>Math.abs(dy)*2.5){revealed.value=String(rule.id);swiped=true}}
function cardTap(){if(swiped){swiped=false;return}if(revealed.value)revealed.value=''}
async function markRead(rule:Row){if(busy.value)return;busy.value=true;error.value='';notice.value='';try{const d=await request<Row>('GET','/api/v1/articles/'+rule.targetId);const versionId=d?.versionId||d?.article?.currentVersionId;if(!versionId)throw new Error('无法获取资讯版本');await request('PUT','/api/v1/me/read/'+rule.targetId,{versionId});rule.read=true;notice.value='已标记已读';revealed.value=''}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
async function favorite(rule:Row){if(busy.value)return;busy.value=true;error.value='';notice.value='';try{await request('PUT','/api/v1/me/favorites/'+rule.targetId,{enabled:true});rule.favorited=true;notice.value='已收藏';revealed.value=''}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
async function removeRule(rule:Row){if(busy.value)return;busy.value=true;error.value='';notice.value='';try{await request('PUT','/api/v1/me/reminders/rules/'+rule.id,{targetType:rule.targetType,targetId:rule.targetId,mode:rule.mode,enabled:false,expectedVersion:rule.lockVersion});notice.value='已移除该强提醒';await load()}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
async function removeKeyword(row:Row){if(busy.value||!row.trackingId)return;busy.value=true;error.value='';notice.value='';try{const rule=await request('GET','/api/v1/me/reminders/rules/'+row.trackingId);await request('PUT','/api/v1/me/reminders/rules/'+row.trackingId,{targetType:'KEYWORD',targetId:row.id,mode:rule.mode,enabled:false,expectedVersion:rule.lockVersion});notice.value='已取消该关键词的强提醒';await load()}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
onShow(()=>{page.value=1;revealed.value='';swiped=false;void load()});onHide(abandonLogin)
</script>
<template>
<view class="page">
  <view v-if="!signedIn" class="card"><text>登录后管理强提醒</text><button role="button" hover-class="yj-hover" @click="navigate('login')">登录</button></view>
  <template v-else>
    <view class="reminder-help" hover-class="yj-hover" @click="showHelp">
      <view class="keyword-follow-row"><text class="card-title">强提醒怎么用？</text><text class="inline-link">查看说明 ›</text></view>
      <text>强提醒分两种：每日汇总（企业微信，按你设的时间一次推送）和及时提醒（订阅消息，触发时立刻到）。除你自己设置的内容外，强提醒不会额外打扰你。</text>
    </view>
    <view class="segmented reminder-tabs">
      <button role="button" :class="{active:tab==='daily'}" hover-class="yj-hover" @click="switchTab('daily')">日常强提醒 {{ counts.daily }}</button>
      <button role="button" :class="{active:tab==='instant'}" hover-class="yj-hover" @click="switchTab('instant')">及时提醒 {{ counts.instant }}</button>
      <button role="button" :class="{active:tab==='follow'}" hover-class="yj-hover" @click="switchTab('follow')">关注强提醒 {{ counts.follow }}</button>
    </view>
    <view class="reminder-summary"><text>{{ summaryText() }}</text></view>
    <view v-if="tab==='instant'&&!instantAuthorized" class="reminder-help">
      <view class="keyword-follow-row"><text class="card-title">订阅授权已用完</text><text class="inline-link" @click="navigate('notification-settings')">去重新授权 ›</text></view>
      <text>及时提醒按次授权，一次授权对应一次发送。要重新接收及时提醒，请重新授权。</text>
    </view>
    <text v-if="notice" class="muted">{{ notice }}</text>
    <PageState :busy="loading" :error="error" :empty="signedIn&&isEmpty&&!loading" :empty-text="emptyText[tab]" @retry="load"/>
    <template v-if="tab==='follow'">
      <view v-for="row in keywords" :key="row.id" class="card">
        <view class="keyword-follow-row">
          <text class="card-title" @click="navigate('keyword-updates','?id='+row.id)">{{ row.name }}</text>
          <view class="taxonomy-values">
            <text class="badge">{{ row.enabled?'已开启':'未开启' }}</text>
            <text v-if="Number(row.newCount)>0" class="badge orange">{{ row.newCount }} 条新信息</text>
          </view>
        </view>
        <text v-if="row.latestTitle" class="muted" @click="navigate('keyword-updates','?id='+row.id)">{{ row.latestTitle }}</text>
        <text v-if="row.latestDate" class="muted">{{ row.latestDate }}</text>
        <view class="actions">
          <button role="button" class="quiet" hover-class="yj-hover" @click="navigate('keyword-updates','?id='+row.id)">查看全部</button>
          <button v-if="row.trackingId" role="button" class="danger" :disabled="busy" hover-class="yj-hover" @click="removeKeyword(row)">取消关注强提醒</button>
          <button v-else role="button" hover-class="yj-hover" @click="navigate('notification-edit','?targetType=KEYWORD&targetId='+row.id)">开启强提醒</button>
        </view>
      </view>
    </template>
    <template v-else>
      <view v-for="rule in rules" :key="rule.id" class="card reminder-card swipe-card" @touchstart="touchStart($event,rule)" @touchend="touchEnd($event,rule)" @click="cardTap">
        <view v-if="revealed===String(rule.id)" class="swipe-pop" @click.stop>
          <button role="button" class="swipe-action" :disabled="busy" hover-class="yj-hover" @click.stop="markRead(rule)">已读</button>
          <button role="button" class="swipe-action primary" :disabled="busy" hover-class="yj-hover" @click.stop="favorite(rule)">收藏</button>
        </view>
        <view class="row">
          <view class="rule-copy">
            <text class="card-title" @click.stop="openTarget(rule)">{{ rule.title||'关注对象' }}</text>
            <view class="taxonomy-values">
              <text class="badge">{{ typeLabel(rule) }}</text>
              <text class="badge" :class="rule.mode==='INSTANT'&&!instantAuthorized?'orange':''">{{ statusText(rule) }}</text>
              <text v-if="rule.read" class="badge">已读</text>
              <text v-if="rule.favorited" class="badge">已收藏</text>
            </view>
            <text v-if="tab==='daily'" class="muted">日常汇总 · 每天 {{ rule.slot||slot }} 推送</text>
            <text v-else class="muted">触发时通过小程序订阅消息立即推送</text>
            <text v-if="tab==='daily'&&Number(rule.pendingCount)>0" class="muted">{{ rule.pendingCount }} 条更新待汇总</text>
          </view>
          <view class="rule-actions">
            <button role="button" class="danger" :disabled="busy" hover-class="yj-hover" @click="removeRule(rule)">{{ tab==='instant'?'撤销':'移除' }}</button>
            <button v-if="tab==='daily'" role="button" class="quiet" :disabled="busy" hover-class="yj-hover" @click="toInstant(rule)">设为及时</button>
          </view>
        </view>
      </view>
      <view v-if="total>20" class="actions">
        <button role="button" :disabled="page===1||loading" hover-class="yj-hover" @click="page--;load()">上一页</button>
        <text>{{ page }}</text>
        <button role="button" :disabled="page*20>=total||loading" hover-class="yj-hover" @click="page++;load()">下一页</button>
      </view>
    </template>
    <view class="bottom-actions">
      <button role="button" hover-class="yj-hover" @click="navigate('notification-add')">＋ 新增强提醒</button>
      <button role="button" class="quiet" hover-class="yj-hover" @click="navigate('notification-settings')">强提醒设置</button>
    </view>
  </template>
  <LoginSheet/>
</view>
</template>
