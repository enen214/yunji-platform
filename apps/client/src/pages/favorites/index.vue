<script setup lang="ts">
import {ref} from 'vue'
import {onShow,onHide,onUnload} from '@dcloudio/uni-app'
import {abandonLogin as cancelLogin} from '../../features/auth'
import {request,navigate,token,type Row} from '../../api/platform'
import {dateText} from '../../features/format'
import PageState from '../../components/PageState.vue'
import LoginSheet from '../../components/LoginSheet.vue'
const items=ref<Row[]>([]),activities=ref<Row[]>([]),busy=ref(false),error=ref(''),signedIn=ref(false)
const localCancelled=ref<Set<string>>(new Set()),localCancelledActivities=ref<Set<string>>(new Set())
async function load(){
 signedIn.value=!!token()
 if(!signedIn.value){items.value=[];activities.value=[];localCancelled.value=new Set();localCancelledActivities.value=new Set();return}
 busy.value=true;error.value=''
 try{
  const [info,act]=await Promise.all([request<Row[]>('GET','/api/v1/me/favorites'),request<Row[]>('GET','/api/v1/me/activity-favorites')])
  items.value=info;activities.value=act
 }catch(e){error.value=(e as Error).message}finally{busy.value=false}
}
function visibleItems(){const c=localCancelled.value;const kept:Row[]=[];items.value.forEach(r=>kept.push(r));return kept.filter(r=>!c.has(String(r.id))).concat(items.value.filter(r=>c.has(String(r.id))))}
function visibleActivities(){const c=localCancelledActivities.value;const kept:Row[]=[];activities.value.forEach(r=>kept.push(r));return kept.filter(r=>!c.has(String(r.id))).concat(activities.value.filter(r=>c.has(String(r.id))))}
async function act(fn:()=>Promise<void>){if(busy.value)return;busy.value=true;error.value='';try{await fn()}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
function dropArticle(r:Row){const id=String(r.id);if(localCancelled.value.has(id)){return act(async()=>{await request('PUT','/api/v1/me/favorites/'+id,{enabled:true});localCancelled.value.delete(id)})}localCancelled.value.add(id);return act(async()=>{await request('PUT','/api/v1/me/favorites/'+id,{enabled:false})})}
function dropActivity(r:Row){const id=String(r.id);if(localCancelledActivities.value.has(id)){return act(async()=>{await request('PUT','/api/v1/me/activity-favorites/'+id,{enabled:true});localCancelledActivities.value.delete(id)})}localCancelledActivities.value.add(id);return act(async()=>{await request('PUT','/api/v1/me/activity-favorites/'+id,{enabled:false})})}
function clearArticles(){uni.showModal({title:'清空信息收藏',content:'仅清除当前账号的收藏记录，确认继续？',success:r=>{if(r.confirm)void act(async()=>{await request('DELETE','/api/v1/me/favorites');localCancelled.value.clear();await load()})}})}
function openArticle(r:Row){navigate('article','?id='+r.id)}
function openActivity(r:Row){navigate('activity-event','?id='+r.id)}
onShow(()=>{void load()})
onHide(cancelLogin)
onUnload(()=>{localCancelled.value=new Set();localCancelledActivities.value=new Set()})
</script>
<template><view class="page"><view class="hero"><text class="title">管理收藏</text><text v-if="signedIn" class="subtitle">信息 {{ visibleItems().length }} 条 · 活动 {{ visibleActivities().length }} 个</text></view><button role="button" v-if="!signedIn" @click="navigate('login')">登录后查看</button><template v-else><PageState :busy="busy" :error="error" @retry="load"/>
<view class="section-head"><text class="section-title">信息收藏</text><text class="muted">{{ visibleItems().length }} 条</text></view>
<view v-for="r in visibleItems()" :key="'article-'+r.id" class="card"><view @click="openArticle(r)"><text class="card-title">{{ r.title }}</text><text class="muted">{{ r.summary||r.createdAt }}</text></view><button role="button" class="quiet" :disabled="busy" @click="dropArticle(r)">{{ localCancelled.has(String(r.id))?'收藏':'取消收藏' }}</button></view>
<text v-if="!visibleItems().length" class="muted">还没有收藏的资讯</text>
<button role="button" v-if="visibleItems().length" class="danger" :disabled="busy" @click="clearArticles">清空信息收藏</button>
<view class="section-head"><text class="section-title">活动收藏</text><text class="muted">{{ visibleActivities().length }} 个</text></view>
<view v-for="r in visibleActivities()" :key="'activity-'+r.id" class="card"><view @click="openActivity(r)"><text class="card-title">{{ r.title }}</text><text class="muted">{{ dateText(r.eventStart) }} — {{ dateText(r.eventEnd) }}</text></view><button role="button" class="quiet" :disabled="busy" @click="dropActivity(r)">{{ localCancelledActivities.has(String(r.id))?'收藏':'取消收藏' }}</button></view>
<text v-if="!visibleActivities().length" class="muted">还没有收藏的活动</text>
</template><LoginSheet/></view></template>
