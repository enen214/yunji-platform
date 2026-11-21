<script setup lang="ts">
import {onMounted,onUnmounted,watch,nextTick,getCurrentInstance} from 'vue'
import {onShow,onHide,onPageScroll} from '@dcloudio/uni-app'
import {visibleExposure,type NativeExposureScope} from '../features/telemetry'
import { navigate } from '../api/platform'
import { contentLabel } from '../features/catalog'
import { lifecycleText } from '../features/format'
import type { ArticleCard } from '../types/api'
const props=defineProps<{items:ArticleCard[]}>()
// 无封面时按内容类型给配色，与首页封面占位一致
const coverTint=(t:string)=>t==='ACTIVITY'?'linear-gradient(135deg,#8fd0a4,#4a9668)':t==='NOTICE'?'linear-gradient(135deg,#9dc3f0,#4a7fc1)':t==='POLICY'?'linear-gradient(135deg,#f0cf9d,#c19144)':t==='STUDY'?'linear-gradient(135deg,#c3a8e8,#7a5fb5)':'linear-gradient(135deg,#a8dcc0,#5f9e7a)'
const instance=getCurrentInstance(),exposure=visibleExposure(()=>(instance?.proxy as unknown as {$scope?:NativeExposureScope})?.$scope)
function show(){exposure.show();void nextTick(()=>exposure.observe())}
onMounted(show);onShow(show);onHide(()=>exposure.hide());onUnmounted(()=>exposure.hide());onPageScroll(()=>exposure.scroll())
watch(()=>props.items,()=>nextTick(()=>exposure.observe()),{deep:true})
</script>
<template><view class="discovery-list"><view v-for="item in items" :key="item.id" class="card result-card exposure-card" :data-article-id="item.id" @click="navigate('article','?id='+item.id)"><image v-if="item.coverUrl" :src="item.coverUrl" mode="aspectFill" class="result-cover"/><view v-else class="result-cover-ph" :style="{background:coverTint(item.contentType)}"/><view class="result-copy"><view class="meta-line"><text class="badge">{{ contentLabel(item.contentType) }}</text><text v-if="item.replacedByArticleId" class="badge replaced" @click.stop="navigate('article','?id='+(item.replacedByArticleId||''))">已被更替</text><text v-if="lifecycleText(item.lifecycleState)" class="muted small">{{ lifecycleText(item.lifecycleState) }}</text></view><text class="card-title">{{ item.title }}</text><text class="muted">{{ item.summary }}</text><text class="small muted">{{ item.sourceName||'云迹' }} · {{ item.publishedAt?.slice(0,10) }}</text><text v-if="item.effectiveFrom||item.effectiveTo" class="small muted">{{ item.effectiveFrom||'不限起始' }} — {{ item.effectiveTo||'不限结束' }}</text></view></view></view></template>
<style lang="scss" scoped>
.result-cover-ph{width:86px;height:86px;border-radius:8px;flex:none;display:flex;align-items:center;justify-content:center}
.badge.replaced{background:#f6e3c3;color:#8a5a12}
</style>
