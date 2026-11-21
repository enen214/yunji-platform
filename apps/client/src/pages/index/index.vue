<script setup lang="ts">
import {onHide as onPageHide} from "@dcloudio/uni-app"
import {abandonLogin as cancelPageLogin} from "../../features/auth"
onPageHide(cancelPageLogin)

import LoginSheet from "../../components/LoginSheet.vue"

import {ref,computed,nextTick,getCurrentInstance} from 'vue'
import {onShow,onHide,onUnload,onReachBottom,onPageScroll} from '@dcloudio/uni-app'
import {navigate,event,request,token,type Row} from '../../api/platform'
import {content} from '../../features/content'
import {contentLabel,contentTypes,loadContentTypes} from '../../features/catalog'
import {visibleExposure,type NativeExposureScope} from '../../features/telemetry'
import {lifecycleText} from '../../features/format'
import {ACTIVITY_PAGES} from '../../features/activity-pages'
import type {ArticleCard,ArticlePage} from '../../types/api'
import type {HomeView} from '../../types/models'
import PageState from '../../components/PageState.vue'
const items=ref<ArticleCard[]>([]),home=ref<HomeView>({banners:[],recommendations:[],activityColumns:[]}),summary=ref<Row|null>(null)
const busy=ref(false),error=ref(''),cursor=ref(''),snapshot=ref(''),more=ref(true),sheet=ref(false),selected=ref<string[]>([]),draft=ref<string[]>([])
const options=computed(()=>contentTypes.value.map(t=>({name:t.name,values:[t.code]})))
const filterName=computed(()=>selected.value.length?selected.value.join('、'):'全部')
const feed=computed(()=>items.value)
// 规格 0-05：推荐资讯区分「重点推荐」（管理端强加）/「为你推荐」两档。
// 强加由后端 tier=3 排进 feed 最前，这里只做前端分组，不新增接口。
const recIds=computed(()=>{const s=new Set<string>();for(const r of home.value.recommendations||[])s.add(String(r.articleId));return s})
// 强加以 home.recommendations（后台 sort_order）为准：feed 命中时复用 feed 字段，
// 未命中（如 hot_until 已过期被 feed 过滤）用推荐位自身字段补齐，避免重点推荐区空着。
const hotPicks=computed<ArticleCard[]>(()=>{
 const byId=new Map(feed.value.map(i=>[String(i.id),i]))
 const now=Date.now()
 return (home.value.recommendations||[]).map(r=>{
  const hit=byId.get(String(r.articleId))
  if(hit)return hit
  // 兜底项按 effective_from/effective_to 现算生命周期，避免待生效条目丢掉「未开始」
  const s=r as {effectiveFrom?:string|null;effectiveTo?:string|null}
  const from=s.effectiveFrom?Date.parse(s.effectiveFrom):0,to=s.effectiveTo?Date.parse(s.effectiveTo):0
  const state:ArticleCard['lifecycleState']=from>now?'UPCOMING':(to&&to<=now?'EXPIRED':'ACTIVE')
  return {id:String(r.articleId),title:r.title,summary:r.summary,coverUrl:r.coverUrl,contentType:r.contentType,sourceName:r.sourceName,publishedAt:r.publishedAt,effectiveFrom:s.effectiveFrom??null,effectiveTo:s.effectiveTo??null,score:0,carrierType:'TEXT_IMAGE',abnormal:false,lifecycleState:state} as ArticleCard
 })
})
const hotColumns=computed(()=>[hotPicks.value.filter((_,i)=>i%2===0),hotPicks.value.filter((_,i)=>i%2===1)])
const recommended=computed(()=>feed.value.filter(i=>!recIds.value.has(String(i.id))))
// 强加（home_recommendation）自带的封面不会被信息流接口返回，这里按 articleId 兜底，
// 否则后台配好的封面在首页永远不显示。
const recCover=computed(()=>{const m:Record<string,string>={};for(const r of home.value.recommendations||[])if(r.coverUrl)m[String(r.articleId)]=r.coverUrl;return m})
const coverOf=(item:ArticleCard)=>item.coverUrl||recCover.value[String(item.id)]||''
// 无封面时按内容类型给配色，避免空白卡片；色板与全局绿色主色一致
const coverTint=(t:string)=>t==='ACTIVITY'?'linear-gradient(135deg,#8fd0a4,#4a9668)':t==='NOTICE'?'linear-gradient(135deg,#9dc3f0,#4a7fc1)':t==='POLICY'?'linear-gradient(135deg,#f0cf9d,#c19144)':t==='STUDY'?'linear-gradient(135deg,#c3a8e8,#7a5fb5)':'linear-gradient(135deg,#a8dcc0,#5f9e7a)'
const coverMark=(i:{title:string})=>(i.title||'云').replace(/^[\s（(【\[]+/,'').slice(0,1)
const columns=computed(()=>[recommended.value.filter((_,i)=>i%2===0),recommended.value.filter((_,i)=>i%2===1)])
const instance=getCurrentInstance(),exposure=visibleExposure(()=>(instance?.proxy as unknown as {$scope?:NativeExposureScope}|null)?.$scope)
let pageActive=false,loaded=false
async function load(reset=true){
 if(busy.value)return;busy.value=true;error.value=''
 try{
  const types=options.value.filter(o=>selected.value.includes(o.name)).flatMap(o=>o.values)
  let query='pageSize=20&hotOnly=true'+(types.length?'&contentType='+types.join(','):'')
  if(!reset&&cursor.value)query+='&cursor='+encodeURIComponent(cursor.value)+'&snapshotAt='+encodeURIComponent(snapshot.value)
  const result=await request<ArticlePage>('GET','/api/v1/articles?'+query)
  if(reset){items.value=[];exposure.reset()}
  items.value.push(...result.items);cursor.value=result.nextCursor||'';snapshot.value=result.snapshotAt;more.value=!!result.nextCursor;loaded=true
  await nextTick();exposure.observe()
 }catch(e){error.value=(e as Error).message}finally{busy.value=false}
}
async function initialize(){try{await loadContentTypes();home.value=await content.home();summary.value=token()?await request('GET','/api/v1/me/discovery-summary'):null;await load(true)}catch(e){error.value=(e as Error).message}}
function openFilter(){draft.value=[...selected.value];sheet.value=true}
function toggle(name:string){draft.value=draft.value.includes(name)?draft.value.filter(n=>n!==name):[...draft.value,name]}
function apply(){selected.value=[...draft.value];sheet.value=false;void load()}
function banner(b:Row){void event('BANNER_CLICK',b.id);if(b.linkType==='TOPIC')navigate('activity-column','?id='+b.linkValue);else if(b.linkType==='ARTICLE')navigate('article','?id='+b.linkValue);else if(b.linkType==='ACTIVITY')navigate(ACTIVITY_PAGES[String(b.linkValue)]||'activity-event','?id='+b.linkValue);else navigate('page','?key='+b.linkValue)}
onShow(()=>{pageActive=true;exposure.show();if(!loaded)void initialize();else{void load(true);void (async()=>{try{summary.value=token()?await request('GET','/api/v1/me/discovery-summary'):null}catch(e){error.value=(e as Error).message}})();void nextTick(()=>exposure.observe())}})
onPageScroll(()=>exposure.scroll())
onReachBottom(()=>{if(more.value)void load(false)})
onHide(()=>{pageActive=false;exposure.hide()});onUnload(()=>{pageActive=false;exposure.hide()})
// #ifdef H5
function visibility(){if(document.hidden)exposure.hide();else if(pageActive){exposure.show();exposure.observe()}}
document.addEventListener('visibilitychange',visibility);onUnload(()=>document.removeEventListener('visibilitychange',visibility))
// #endif
let statusBarHeight=0, capsulePadding=0
// #ifdef MP-WEIXIN
statusBarHeight=uni.getSystemInfoSync().statusBarHeight||0
capsulePadding=uni.getSystemInfoSync().windowWidth-uni.getMenuButtonBoundingClientRect().left+8
// #endif
</script>
<template><view class="page feed-page" :style="{paddingTop:statusBarHeight+'px'}">
<view class="feed-toolbar" :style="{paddingRight:capsulePadding+'px'}"><text class="feed-brand" data-testid="feed-title">云迹</text><view class="feed-search" role="button" aria-label="搜索资讯" @click="navigate('search')"><text class="search-symbol"></text><text class="search-placeholder">输入关键字或标签以搜索</text></view></view>
<view v-if="summary" class="update-strip" @click="navigate('subscriptions')"><text>▣</text><text>您的关注更新了 <text class="update-count">{{ summary.unreadCount }}</text> 条 · 今日新推送 <text class="update-count">{{ summary.sentToday }}</text> 条</text><text class="chevron">›</text></view>
<swiper v-if="home.banners.length" class="home-carousel" indicator-dots indicator-active-color="#50a770" autoplay circular><swiper-item v-for="b in home.banners" :key="b.id"><view class="feature-banner" @click="banner(b)"><image v-if="b.imageUrl" :src="b.imageUrl" class="banner-image" mode="aspectFill"/><text class="banner-tag">精选活动</text><view class="banner-caption"><text class="banner-title">{{ b.title }}</text><text class="banner-arrow">点击进入</text></view></view></swiper-item></swiper>
<view v-if="hotPicks.length" class="section-head"><text class="section-title">重点推荐</text></view>
<view v-if="hotPicks.length" class="feed-grid"><view v-for="(column,i) in hotColumns" :key="i" class="feed-column" :style="hotPicks.length<2?'width:100%':''"><view v-for="item in column" :key="item.id" class="feed-card exposure-card" :data-article-id="item.id" @click="event('SEARCH_CLICK',item.id);navigate('article','?id='+item.id)"><text v-if="lifecycleText(item.lifecycleState)" class="badge upcoming">{{ lifecycleText(item.lifecycleState) }}</text><image v-if="coverOf(item)" :src="coverOf(item)" class="feed-card-image" mode="aspectFill"/><view v-else class="feed-card-cover-ph" :style="{background:coverTint(item.contentType)}"><text class="ph-mark">{{ coverMark(item) }}</text></view><text class="feed-card-title">{{ item.title }}</text><text class="feed-card-summary">{{ item.summary||'查看完整资讯' }}</text><text class="feed-card-meta">{{ item.publishedAt?.slice(0,10) }}</text></view></view></view>
<view class="section-head"><text class="section-title">为你推荐</text><button role="button" class="feed-filter" :disabled="busy" @click="openFilter">{{ filterName }}⌄</button></view>
<PageState :busy="busy" :error="error" :empty="!feed.length&&!busy" empty-text="该分类下暂无资讯 · 试试其他分类" @retry="initialize"/>
<view class="feed-grid"><view v-for="(column,i) in columns" :key="i" class="feed-column"><view v-for="item in column" :key="item.id" class="feed-card exposure-card" :data-article-id="item.id" @click="event('SEARCH_CLICK',item.id);navigate('article','?id='+item.id)"><text class="badge" :class="{orange:['NOTICE','ACTIVITY'].includes(item.contentType)}">{{ contentLabel(item.contentType) }}</text><text v-if="lifecycleText(item.lifecycleState)" class="badge upcoming">{{ lifecycleText(item.lifecycleState) }}</text><image v-if="coverOf(item)" :src="coverOf(item)" class="feed-card-image" mode="aspectFill"/><view v-else class="feed-card-cover-ph" :style="{background:coverTint(item.contentType)}"><text class="ph-mark">{{ coverMark(item) }}</text></view><text class="feed-card-title">{{ item.title }}</text><text class="feed-card-summary">{{ item.summary||'查看完整资讯' }}</text><text class="feed-card-meta">{{ item.publishedAt?.slice(0,10) }}</text></view></view></view>
<button role="button" v-if="more&&!error" class="secondary load-more" :disabled="busy" @click="load(false)">加载更多资讯</button><text v-if="!more&&feed.length" class="end-note">- 已经到底了 -</text>
<view v-if="sheet" class="sheet-mask" @click="sheet=false"><view class="filter-sheet" role="dialog" aria-label="筛选资讯类型" @click.stop><view class="sheet-grip"></view><view class="row"><text class="section-title">筛选资讯类型</text><button role="button" class="quiet" @click="sheet=false">取消</button></view><view class="filter-options"><button role="button" :class="!draft.length?'':'quiet'" @click="draft=[]">全部</button><button role="button" v-for="o in options" :key="o.name" :class="draft.includes(o.name)?'':'quiet'" @click="toggle(o.name)">{{ o.name }}</button></view><view class="actions"><button role="button" class="quiet" @click="draft=[]">重置</button><button role="button" @click="apply">确定</button></view></view></view>
<LoginSheet/></view></template>
<style lang="scss" scoped>
.badge.upcoming{background:#e8eefc;color:#3b5bdb}
.feed-card-cover-ph{height:96px;border-radius:10px;display:flex;align-items:center;justify-content:center;margin-bottom:8px}
.ph-mark{font-size:30px;font-weight:700;color:#fff;opacity:.92;line-height:1}
</style>
