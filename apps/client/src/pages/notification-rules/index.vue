<script setup lang="ts">
import {ref} from 'vue'
import {onShow,onHide} from '@dcloudio/uni-app'
import {request,navigate,token,type Row} from '../../api/platform'
import {abandonLogin} from '../../features/auth'
import LoginSheet from '../../components/LoginSheet.vue'
import PageState from '../../components/PageState.vue'

type Tab='daily'|'instant'|'follow'
const tab=ref<Tab>('daily'),rules=ref<Row[]>([]),counts=ref<Record<Tab,number>>({daily:0,instant:0,follow:0}),error=ref(''),loading=ref(false),busy=ref(false),signedIn=ref(false),page=ref(1),total=ref(0),wecomBound=ref<'BOUND'|'UNBOUND'|'UNKNOWN'>('UNKNOWN')
const filters:Record<string,string>={daily:'mode=DAILY&targetType=ARTICLE',instant:'mode=INSTANT',follow:'mode=DAILY&targetType=KEYWORD'}
const typeText:Record<string,string>={BASIC:'基础信息',NOTICE:'通知',GUIDE:'攻略'}
const emptyText:Record<Tab,string>={daily:'暂未设置日常强提醒规则',instant:'暂未设置及时提醒规则',follow:'暂未设置关键词强提醒规则'}
function typeLabel(rule:Row){return rule.targetType==='KEYWORD'?'关键词':(typeText[String(rule.contentType)]||'资讯')}
async function syncBound(){try{const settings=await request('GET','/api/v1/me/reminders/settings');const permission=((settings.permissions as Row[])||[])[0];wecomBound.value=typeof permission?.wecomBound==='boolean'?(permission.wecomBound?'BOUND':'UNBOUND'):'UNKNOWN'}catch{}}
async function load(){
  signedIn.value=!!token()
  if(!signedIn.value){rules.value=[];return}
  loading.value=true;error.value=''
  try{
    const [daily,instant,follow]=await Promise.all([
      request('GET','/api/v1/me/reminders/rules?size=1&'+filters.daily),
      request('GET','/api/v1/me/reminders/rules?size=1&'+filters.instant),
      request('GET','/api/v1/me/reminders/rules?size=1&'+filters.follow)
    ])
    counts.value={daily:Number(daily.total||0),instant:Number(instant.total||0),follow:Number(follow.total||0)}
    const result=await request('GET','/api/v1/me/reminders/rules?page='+page.value+'&size=20&'+filters[tab.value])
    rules.value=result.items||[];total.value=Number(result.total||0)
  }catch(e){error.value=(e as Error).message}finally{loading.value=false}
}
function switchTab(value:Tab){if(loading.value||busy.value)return;tab.value=value;page.value=1;rules.value=[];void load()}
function openTarget(rule:Row){navigate(rule.targetType==='ARTICLE'?'article':'keyword-updates','?id='+rule.targetId)}
async function toggle(rule:Row){if(busy.value)return;busy.value=true;error.value='';try{if(!rule.enabled&&rule.mode==='DAILY'){if(wecomBound.value==='UNKNOWN')await syncBound();if(wecomBound.value==='UNBOUND'){const decision=await uni.showModal({title:'需要先绑定企业微信',content:'日常强提醒通过企业微信发送，绑定后回到本页即可开启',confirmText:'去绑定',cancelText:'稍后'});if(decision.confirm)navigate('notification-settings');return}}await request('PUT','/api/v1/me/reminders/rules/'+rule.id,{targetType:rule.targetType,targetId:rule.targetId,mode:rule.mode,enabled:!rule.enabled,expectedVersion:rule.lockVersion});await load()}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
onShow(()=>{void load();if(token())void syncBound()});onHide(abandonLogin)
</script>
<template>
<view class="page">
  <view class="hero"><text class="subtitle">按类型查看全部强提醒规则，可关闭、重新开启或修改提醒方式。</text></view>
  <view v-if="!signedIn" class="card"><text>登录后管理强提醒</text><button role="button" hover-class="yj-hover" @click="navigate('login')">登录</button></view>
  <template v-else>
    <view class="segmented reminder-tabs">
      <button role="button" :class="{active:tab==='daily'}" hover-class="yj-hover" @click="switchTab('daily')">日常强提醒 {{ counts.daily }}</button>
      <button role="button" :class="{active:tab==='instant'}" hover-class="yj-hover" @click="switchTab('instant')">及时提醒 {{ counts.instant }}</button>
      <button role="button" :class="{active:tab==='follow'}" hover-class="yj-hover" @click="switchTab('follow')">关注强提醒 {{ counts.follow }}</button>
    </view>
    <PageState :busy="loading" :error="error" :empty="!rules.length&&!loading" :empty-text="emptyText[tab]" @retry="load"/>
    <view v-for="rule in rules" :key="rule.id" class="card reminder-card">
      <view class="row">
        <view class="rule-copy">
          <text class="card-title" @click="openTarget(rule)">{{ rule.title||'关注对象' }}</text>
          <view class="taxonomy-values">
            <text class="badge">{{ typeLabel(rule) }}</text>
            <text class="badge" :class="rule.enabled?'':'orange'">{{ rule.enabled?'已开启':'已关闭' }}</text>
          </view>
          <text class="muted">{{ rule.mode==='INSTANT'?'及时提醒 · 触发时立即推送':'日常汇总 · 每天 '+(rule.slot||'--:--')+' 推送' }}</text>
        </view>
        <view class="rule-actions">
          <button role="button" class="quiet" :disabled="busy" hover-class="yj-hover" @click="toggle(rule)">{{ rule.enabled?'关闭':'重新开启' }}</button>
          <button role="button" class="secondary" hover-class="yj-hover" @click="navigate('notification-edit','?id='+rule.id)">修改方式</button>
        </view>
      </view>
    </view>
    <view v-if="total>20" class="actions">
      <button role="button" :disabled="page===1||loading" hover-class="yj-hover" @click="page--;load()">上一页</button>
      <text>{{ page }}</text>
      <button role="button" :disabled="page*20>=total||loading" hover-class="yj-hover" @click="page++;load()">下一页</button>
    </view>
    <view class="bottom-actions">
      <button role="button" hover-class="yj-hover" @click="navigate('notification-add')">新增强提醒</button>
      <button role="button" class="quiet" hover-class="yj-hover" @click="navigate('notifications')">返回强提醒首页</button>
    </view>
  </template>
  <LoginSheet/>
</view>
</template>
