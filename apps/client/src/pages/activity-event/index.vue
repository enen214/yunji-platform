<script setup lang="ts">
import {onHide as onPageHide} from "@dcloudio/uni-app"
import {abandonLogin as cancelPageLogin} from "../../features/auth"
onPageHide(cancelPageLogin)

import ActivityDownloads from '../../components/ActivityDownloads.vue'
import ActivitySignup from '../../components/activity/ActivitySignup.vue'
import ActivityApplyEntry from '../../components/activity/ActivityApplyEntry.vue'
import LoginSheet from "../../components/LoginSheet.vue"
import {dateText} from '../../features/format'

import { computed,ref } from 'vue';import { onLoad,onShow } from '@dcloudio/uni-app';import { request,navigate,token,type Row } from '../../api/platform'
const id=ref(''),activity=ref<Row|null>(null),reviewCampaignIds=ref<string[]>([]),error=ref(''),favorited=ref(false),favBusy=ref(false)
const statusText=computed(()=>{const a=activity.value;if(!a)return '';if(a.status==='CLOSED'||Date.parse(a.eventEnd)<Date.now())return '已结束';if(Date.parse(a.registrationEnd)<Date.now())return '已截止';return '报名中'})
const coverMark=computed(()=>{const m=(activity.value?.title||'').match(/[\u4e00-\u9fa5a-zA-Z]/);return m?m[0]:'活'})
async function load(){activity.value=await request('GET','/api/v1/activities/'+id.value);reviewCampaignIds.value=activity.value!.reviewCampaignIds||[];await loadFavorite()}
async function loadFavorite(){
 if(!token()){favorited.value=false;return}
 try{const rows=await request<Row[]>('GET','/api/v1/me/activity-favorites');favorited.value=rows.some(r=>String(r.id)===String(id.value))}
 catch(e){favorited.value=false;if((e as {status?:number}).status===401)error.value='登录已过期，请重新登录后再收藏'}
}
async function toggleFavorite(){
 if(!token()){navigate('login');return}
 if(favBusy.value)return;favBusy.value=true;error.value=''
 try{await request('PUT','/api/v1/me/activity-favorites/'+id.value,{enabled:!favorited.value});favorited.value=!favorited.value}catch(e){error.value=(e as Error).message}finally{favBusy.value=false}
}
onLoad(q=>{id.value=String(q?.id||'')})
async function reload(){if(!id.value)return;error.value='';try{await load()}catch(e){error.value=(e as Error).message}}
onShow(reload)
</script>
<template><view class="page"><view v-if="error" class="state-card" role="alert"><text class="error">{{ error }}</text><button role="button" class="quiet" @click="reload">重新加载</button></view><template v-if="activity"><view class="cover"><image v-if="activity.coverUrl" class="cover-image" :src="activity.coverUrl" mode="aspectFill"/><view v-else class="cover-fallback"><text class="cover-mark">{{ coverMark }}</text></view></view><view class="hero"><text class="title">{{ activity.title }}</text><text class="subtitle">{{ dateText(activity.eventStart) }} — {{ dateText(activity.eventEnd) }}</text><text class="badge" :class="{orange:statusText==='已截止',blue:statusText==='已结束'}">{{ statusText }}</text><button role="button" class="quiet" :disabled="favBusy" @click="toggleFavorite">{{ favorited?'已收藏':'收藏活动' }}</button></view><view v-if="activity.description" class="card"><text class="muted">{{ activity.description }}</text></view><ActivityDownloads :activity-id="id"/><ActivitySignup v-if="activity.form" :activity-id="id" :activity="activity" @changed="load"/><ActivityApplyEntry :review-campaign-ids="reviewCampaignIds"/><button role="button" v-if="activity.articleId" class="quiet" @click="navigate('article','?id='+activity.articleId)">查看活动通知</button></template><LoginSheet/></view></template>
<style lang="scss" scoped>
.cover{height:168px;border-radius:12px;overflow:hidden;margin:4px 0 10px;background:#e8f2ec}
.cover-image{width:100%;height:100%}
.cover-fallback{width:100%;height:100%;display:flex;align-items:center;justify-content:center;background:linear-gradient(135deg,#7cc394,#3f8a5c)}
.cover-mark{font-size:56px;font-weight:700;color:#fff;line-height:1}
</style>
