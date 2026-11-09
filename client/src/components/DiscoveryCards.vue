<script setup lang="ts">
import {onMounted,onUnmounted,watch,nextTick,getCurrentInstance} from 'vue'
import {onShow,onHide,onPageScroll} from '@dcloudio/uni-app'
import {visibleExposure,type NativeExposureScope} from '../features/telemetry'
import { navigate } from '../api/platform'
import type { ArticleCard } from '../types/api'
const props=defineProps<{items:ArticleCard[]}>()
const instance=getCurrentInstance(),exposure=visibleExposure(()=>(instance?.proxy as unknown as {$scope?:NativeExposureScope})?.$scope)
function show(){exposure.show();void nextTick(()=>exposure.observe())}
onMounted(show);onShow(show);onHide(()=>exposure.hide());onUnmounted(()=>exposure.hide());onPageScroll(()=>exposure.scroll())
watch(()=>props.items,()=>nextTick(()=>exposure.observe()),{deep:true})
</script>
<template><view class="discovery-list"><view v-for="item in items" :key="item.id" class="card result-card exposure-card" :data-article-id="item.id" @click="navigate('article','?id='+item.id)"><image v-if="item.coverUrl" :src="item.coverUrl" mode="aspectFill" class="result-cover"/><view class="result-copy"><view class="meta-line"><text v-if="item.categoryName" class="badge tag-info">{{ item.categoryName }}</text><text v-if="item.contentType==='NOTICE'" class="muted small">{{ item.lifecycleState==='EXPIRED'?'已失效':item.lifecycleState==='UPCOMING'?'待生效':'生效中' }}</text></view><text class="card-title">{{ item.title }}</text><text class="muted">{{ item.summary }}</text><text class="small muted">{{ item.sourceName||'云迹' }} · {{ item.publishedAt?.slice(0,10) }}</text><text v-if="item.contentType==='NOTICE'&&(item.effectiveFrom||item.effectiveTo)" class="small muted">{{ item.effectiveFrom||'不限起始' }} — {{ item.effectiveTo||'不限结束' }}</text></view></view></view></template>
