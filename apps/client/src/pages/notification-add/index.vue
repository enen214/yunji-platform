<script setup lang="ts">
import {onHide as onPageHide} from "@dcloudio/uni-app"
import {abandonLogin as cancelPageLogin} from "../../features/auth"
onPageHide(cancelPageLogin)

import LoginSheet from "../../components/LoginSheet.vue"

import {ref} from 'vue'
import {onShow,onReachBottom} from '@dcloudio/uni-app'
import {request,navigate,token,type Row} from '../../api/platform'
import PageState from '../../components/PageState.vue'
const tab=ref('ARTICLE'),q=ref(''),rows=ref<Row[]>([]),busy=ref(false),saving=ref(''),error=ref(''),cursor=ref(''),snapshot=ref(''),added=ref<string[]>([]),wecomBound=ref<'BOUND'|'UNBOUND'|'UNKNOWN'>('UNKNOWN')
async function syncBound(){try{const settings=await request('GET','/api/v1/me/reminders/settings');const permission=((settings.permissions as Row[])||[])[0];wecomBound.value=typeof permission?.wecomBound==='boolean'?(permission.wecomBound?'BOUND':'UNBOUND'):'UNKNOWN'}catch{}}
async function load(reset=true){if(busy.value)return;busy.value=true;error.value='';try{if(tab.value==='ARTICLE'){const p=await request('GET','/api/v1/articles/search?q='+encodeURIComponent(q.value.trim())+(reset?'':'&cursor='+encodeURIComponent(cursor.value)+'&snapshotAt='+encodeURIComponent(snapshot.value)));rows.value=reset?p.items:[...rows.value,...p.items];cursor.value=p.nextCursor||'';snapshot.value=p.snapshotAt}else{rows.value=await request('GET','/api/v1/catalog/keywords?q='+encodeURIComponent(q.value.trim()));cursor.value=''}}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
async function change(type:string){if(busy.value||saving.value)return;tab.value=type;q.value='';rows.value=[];added.value=[];await load()}
async function add(r:Row){if(saving.value||!token())return;saving.value=r.id;error.value='';try{if(wecomBound.value==='UNKNOWN')await syncBound();if(wecomBound.value==='UNBOUND'){const decision=await uni.showModal({title:'需要先绑定企业微信',content:'日常强提醒通过企业微信发送，绑定后回到本页即可开启',confirmText:'去绑定',cancelText:'稍后'});if(decision.confirm)navigate('notification-settings');return}const type=tab.value;await request('POST','/api/v1/me/reminders/daily',{targetType:type,targetId:r.id});added.value.push(r.id);uni.showToast({title:'已加入日常强提醒'})}catch(e){error.value=(e as Error).message}finally{saving.value=''}}
onShow(()=>{void load();if(token())void syncBound()});onReachBottom(()=>{if(cursor.value)void load(false)})
</script>
<template><view class="page"><view class="hero"><text class="title">新增强提醒</text><text class="subtitle">添加后立即保存日常跟踪，从后续更新开始提醒。</text></view><view class="segmented"><button role="button" :class="{active:tab==='ARTICLE'}" :disabled="busy||!!saving" @click="change('ARTICLE')">可选信息</button><button role="button" :class="{active:tab==='KEYWORD'}" :disabled="busy||!!saving" @click="change('KEYWORD')">可选关键词</button></view><view class="search-row"><input v-model="q" maxlength="100" placeholder="搜索跟踪对象" @confirm="load()"/><button role="button" :disabled="busy||!!saving" @click="load()">搜索</button></view><PageState :busy="busy" :error="error" :empty="!rows.length&&!busy" empty-text="暂无可选对象" @retry="load()"/><view v-for="r in rows" :key="r.id" class="card row"><text>{{ r.title||r.name }}</text><button role="button" class="quiet" :disabled="!!saving||added.includes(r.id)" @click="add(r)">{{ added.includes(r.id)?'已添加':saving===r.id?'添加中…':'＋ 添加' }}</button></view><button role="button" v-if="cursor" class="quiet" :disabled="busy" @click="load(false)">加载更多</button><text class="muted small">添加会同时关注该对象并开启日常强提醒；仅关注不会自动开启强提醒。</text><LoginSheet/></view></template>
