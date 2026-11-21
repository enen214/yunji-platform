<!-- 已被 pages/activity-event 取代，仅作兜底，勿新增入口 -->
<script setup lang="ts">
import {onHide as onPageHide} from "@dcloudio/uni-app"
import {abandonLogin as cancelPageLogin} from "../../features/auth"
onPageHide(cancelPageLogin)

import ActivityDownloads from '../../components/ActivityDownloads.vue'
import ActivitySignup from '../../components/activity/ActivitySignup.vue'
import ActivityApplyEntry from '../../components/activity/ActivityApplyEntry.vue'
import LoginSheet from "../../components/LoginSheet.vue"
import {dateText} from '../../features/format'

import { ref } from 'vue';import { onLoad,onShow } from '@dcloudio/uni-app';import { request,navigate,type Row } from '../../api/platform'
const id=ref(''),activity=ref<Row|null>(null),reviewCampaignIds=ref<string[]>([]),error=ref('')
async function load(){activity.value=await request('GET','/api/v1/activities/'+id.value);reviewCampaignIds.value=activity.value!.reviewCampaignIds||[]}
onLoad(q=>{id.value=String(q?.id||'')})
onShow(async()=>{if(!id.value)return;error.value='';try{await load()}catch(e){error.value=(e as Error).message}})
</script>
<template><view class="page"><text class="error">{{ error }}</text><template v-if="activity"><view class="hero"><text class="title">{{ activity.title }}</text><text class="subtitle">{{ dateText(activity.eventStart) }} — {{ dateText(activity.eventEnd) }}</text><text class="badge">剩余 {{ activity.remaining }} 名额</text></view><ActivityDownloads :activity-id="id"/><ActivitySignup v-if="activity.form" :activity-id="id" :activity="activity" @changed="load"/><ActivityApplyEntry :review-campaign-ids="reviewCampaignIds"/><button role="button" v-if="activity.articleId" class="quiet" @click="navigate('article','?id='+activity.articleId)">查看活动通知</button></template><LoginSheet/></view></template>
