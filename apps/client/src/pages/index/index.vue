<script setup lang="ts">
import {onHide as onPageHide} from "@dcloudio/uni-app"
import {abandonLogin as cancelPageLogin} from "../../features/auth"
onPageHide(cancelPageLogin)

import LoginSheet from "../../components/LoginSheet.vue"

import {ref,computed,nextTick,getCurrentInstance} from 'vue'
import {onShow,onHide,onUnload,onReachBottom,onPageScroll} from '@dcloudio/uni-app'
import {syncTabBar} from '../../features/tabbar'
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
onShow(()=>{syncTabBar(0);pageActive=true;exposure.show();if(!loaded)void initialize();else{void load(true);void (async()=>{try{summary.value=token()?await request('GET','/api/v1/me/discovery-summary'):null}catch(e){error.value=(e as Error).message}})();void nextTick(()=>exposure.observe())}})
onPageScroll((e)=>{exposure.scroll();const t=(e as any)?.scrollTop??0;const threshold=statusBarHeight+150;const next=t>threshold;if(next!==scrolled.value)scrolled.value=next})
onReachBottom(()=>{if(more.value)void load(false)})
onHide(()=>{pageActive=false;exposure.hide()});onUnload(()=>{pageActive=false;exposure.hide()})
// #ifdef H5
function visibility(){if(document.hidden)exposure.hide();else if(pageActive){exposure.show();exposure.observe()}}
document.addEventListener('visibilitychange',visibility);onUnload(()=>document.removeEventListener('visibilitychange',visibility))
// #endif
let statusBarHeight=0, capsulePadding=0, capsuleTop=0
// #ifdef MP-WEIXIN
statusBarHeight=uni.getSystemInfoSync().statusBarHeight||0
const _mb=uni.getMenuButtonBoundingClientRect()
capsulePadding=uni.getSystemInfoSync().windowWidth-_mb.left+8
capsuleTop=_mb.top||(statusBarHeight+4)
// #endif
// #ifndef MP-WEIXIN
capsuleTop=44
capsulePadding=100
// #endif
// 搜索栏 top：与微信胶囊垂直对齐（往上调，贴近状态栏）
const searchTop=Math.max(4,capsuleTop-4)
// v29 融合主页：上滑后轮播收成毛玻璃头部
const scrolled=ref(false)
const curBanner=ref(0)
function onSwiper(e:any){curBanner.value=e.detail?.current??0}
function goSearch(){navigate('search')}
// 数字卡：今日推送/关注更新/待办提醒（待办提醒暂无后端数据源，暂显 0）
const statCards=computed(()=>[{n:summary.value?.sentToday??0,t:'今日推送'},{n:summary.value?.unreadCount??0,t:'关注更新'},{n:summary.value?.todoCount??0,t:'待办提醒'}])
const today=computed(()=>{const d=new Date();return {day:d.getDate(),label:`${d.getMonth()+1}月 · 周${'日一二三四五六'[d.getDay()]}`}})
</script>
<template><view class="page feed-page tab-page v29-home" :style="{paddingTop:scrolled?'0':statusBarHeight+'px'}">
<!-- 上滑态：整幅贯通毛玻璃头部 -->
<view v-if="scrolled" class="v29-frosttop" :style="{paddingTop:statusBarHeight+'px'}">
  <image v-if="home.banners[curBanner]&&home.banners[curBanner].imageUrl" :src="home.banners[curBanner].imageUrl" class="v29-frost-img" mode="aspectFill"/>
  <view v-else class="v29-frost-img v29-frost-img-ph" :style="{background:coverTint('ACTIVITY')}"></view>
  <view class="v29-frost-bg"></view>
  <view class="v29-searchbar v29-searchbar-frost" :style="{right:capsulePadding+'px',top:searchTop+'px'}" hover-class="yj-hover" @click="goSearch"><text class="v29-search-ic"></text><text class="v29-search-ph">搜索资讯 / 板块 / 关键词</text></view>
  <view class="v29-frost-brand"><text class="v29-frost-name">云迹</text><text class="v29-frost-en">YUNJI · 云岭学院</text></view>
</view>
<!-- 初始态：轮播出血到顶 + 搜索顶置 -->
<view v-else class="v29-heros">
  <swiper class="v29-carousel" :indicator-dots="false" autoplay circular @change="onSwiper">
    <swiper-item v-for="b in home.banners" :key="b.id">
      <view class="v29-banner" hover-class="yj-hover" @click="banner(b)">
        <image v-if="b.imageUrl" :src="b.imageUrl" class="v29-banner-img" mode="aspectFill"/>
        <view v-else class="v29-banner-ph" :style="{background:coverTint('ACTIVITY')}"></view>
        <view class="v29-banner-ov"></view>
        <view class="v29-banner-cap"><text class="v29-banner-sub">YUNJI · 今日聚焦</text><text class="v29-banner-title">{{ b.title }}</text></view>
      </view>
    </swiper-item>
  </swiper>
  <view class="v29-searchbar" :style="{right:capsulePadding+'px',top:searchTop+'px'}" hover-class="yj-hover" @click="goSearch"><text class="v29-search-ic"></text><text class="v29-search-ph">搜索资讯 / 板块 / 关键词</text><view class="v29-search-go">⌕</view></view>
  <view class="v29-pager"><text v-for="(b,i) in home.banners" :key="i" :class="['v29-dot',{on:i===curBanner}]"></text></view>
</view>
<!-- 数字卡压住轮播底边 -->
<view class="v29-statcard" :class="{frost:scrolled}">
  <view class="v29-row3"><view v-for="s in statCards" :key="s.t" class="v29-stt"><text class="v29-stt-n">{{ s.n }}</text><text class="v29-stt-t">{{ s.t }}</text></view></view>
  <view class="v29-drow2"><text class="v29-dnum">{{ today.day }}</text><text class="v29-dlabel">{{ today.label }}</text></view>
</view>
<!-- 热门资讯：横滑卡片 -->
<view v-if="hotPicks.length" class="v29-sec"><text class="v29-sec-t">热门资讯</text><text class="v29-sec-r" @click="openFilter">更多 ›</text></view>
<scroll-view v-if="hotPicks.length" class="v29-rcrow" scroll-x :show-scrollbar="false"><view v-for="item in hotPicks" :key="item.id" class="v29-rcard" :data-article-id="item.id" hover-class="yj-hover" @click="event('SEARCH_CLICK',item.id);navigate('article','?id='+item.id)"><image v-if="coverOf(item)" :src="coverOf(item)" class="v29-rcard-img" mode="aspectFill"/><view v-else class="v29-rcard-ph" :style="{background:coverTint(item.contentType)}"><text class="ph-mark">{{ coverMark(item) }}</text></view><view class="v29-rcard-pad"><text class="v29-rcard-t">{{ item.title }}</text><text class="v29-rcard-m"><text class="v29-pill">{{ contentLabel(item.contentType) }}</text>{{ item.sourceName || '云迹' }}</text></view></view></scroll-view>
<!-- 为你推荐 -->
<view class="v29-sec"><text class="v29-sec-t">为你推荐</text><text class="v29-sec-r" @click="openFilter">重点推荐 ›</text></view>
<PageState :busy="busy" :error="error" :empty="!feed.length&&!busy" empty-text="该分类下暂无资讯 · 试试其他分类" @retry="initialize"/>
<view class="feed-grid v29-mix"><view v-for="(column,i) in columns" :key="i" class="feed-column"><view v-for="item in column" :key="item.id" class="feed-card exposure-card" :data-article-id="item.id" hover-class="yj-hover" @click="event('SEARCH_CLICK',item.id);navigate('article','?id='+item.id)"><text class="badge" :class="{orange:['NOTICE','ACTIVITY'].includes(item.contentType)}">{{ contentLabel(item.contentType) }}</text><text v-if="lifecycleText(item.lifecycleState)" class="badge upcoming">{{ lifecycleText(item.lifecycleState) }}</text><image v-if="coverOf(item)" :src="coverOf(item)" class="feed-card-image" mode="aspectFill"/><view v-else class="feed-card-cover-ph" :style="{background:coverTint(item.contentType)}"><text class="ph-mark">{{ coverMark(item) }}</text></view><text class="feed-card-title">{{ item.title }}</text><text class="feed-card-summary">{{ item.summary||'查看完整资讯' }}</text><text class="feed-card-meta">{{ item.publishedAt?.slice(0,10) }}</text></view></view></view>
<button role="button" v-if="more&&!error" class="secondary load-more" :disabled="busy" hover-class="yj-hover" @click="load(false)">加载更多资讯</button><text v-if="!more&&feed.length" class="end-note">- 已经到底了 -</text>
<view v-if="sheet" class="sheet-mask" hover-class="yj-hover" @click="sheet=false"><view class="filter-sheet" role="dialog" aria-label="筛选资讯类型" hover-class="yj-hover" @click.stop><view class="sheet-grip"></view><view class="row"><text class="section-title">筛选资讯类型</text><button role="button" class="quiet" hover-class="yj-hover" @click="sheet=false">取消</button></view><view class="filter-options"><button role="button" :class="!draft.length?'':'quiet'" hover-class="yj-hover" @click="draft=[]">全部</button><button role="button" v-for="o in options" :key="o.name" :class="draft.includes(o.name)?'':'quiet'" hover-class="yj-hover" @click="toggle(o.name)">{{ o.name }}</button></view><view class="actions"><button role="button" class="quiet" hover-class="yj-hover" @click="draft=[]">重置</button><button role="button" hover-class="yj-hover" @click="apply">确定</button></view></view></view>
<LoginSheet/></view></template>
<style lang="scss" scoped>
.badge.upcoming{background:#e8eefc;color:#3b5bdb}
.feed-card-cover-ph{height:96px;border-radius:10px;display:flex;align-items:center;justify-content:center;margin-bottom:8px}
.ph-mark{font-size:30px;font-weight:700;color:#fff;opacity:.92;line-height:1}
/* ===== v29 融合主页 ===== */
.v29-home{background:transparent;padding-left:0;padding-right:0;}
/* 初始态：轮播出血到顶 */
.v29-heros{position:relative;height:440px;overflow:hidden;}
.v29-carousel{width:100%;height:440px;}
.v29-banner{position:relative;width:100%;height:440px;}
.v29-banner-img{width:100%;height:440px;}
.v29-banner-ph{width:100%;height:440px;}
.v29-banner-ov{position:absolute;inset:0;background:linear-gradient(180deg,rgba(0,0,0,.06) 0%,rgba(0,0,0,0) 28%,rgba(0,0,0,.18) 62%,rgba(0,0,0,.46) 100%);}
.v29-banner-cap{position:absolute;left:16px;right:16px;bottom:70px;z-index:4;}
.v29-banner-sub{display:block;color:rgba(255,255,255,.92);font-size:11.5px;font-weight:600;}
.v29-banner-title{display:block;color:#fff;font-size:25px;font-weight:900;letter-spacing:.5px;margin-top:6px;text-shadow:0 2px 10px rgba(0,0,0,.42);overflow:hidden;text-overflow:ellipsis;white-space:nowrap;}
/* 搜索顶置 */
.v29-searchbar{position:absolute;left:12px;right:100px;height:32px;border-radius:16px;background:rgba(255,255,255,.62);backdrop-filter:blur(18px) saturate(1.5);-webkit-backdrop-filter:blur(18px) saturate(1.5);display:flex;align-items:center;gap:7px;padding:0 4px 0 12px;box-shadow:inset 0 1px 0 rgba(255,255,255,.7),0 4px 14px rgba(0,0,0,.14);z-index:6;}
.v29-searchbar-frost{position:absolute;background:rgba(255,255,255,.92);box-shadow:0 2px 8px rgba(20,40,30,.08);}
.v29-search-ic{width:11px;height:11px;border:1.5px solid #81928a;border-radius:50%;position:relative;flex-shrink:0;box-sizing:border-box;}
.v29-search-ic::after{content:"";position:absolute;width:5px;height:1.5px;background:#81928a;right:-4px;bottom:-2px;transform:rotate(45deg);}
.v29-search-ph{flex:1;color:rgba(30,60,45,.72);font-size:12px;}
.v29-search-go{width:24px;height:24px;border-radius:12px;background:#2e7d4f;color:#fff;display:flex;align-items:center;justify-content:center;font-size:15px;line-height:1;}
.v29-pager{position:absolute;right:16px;bottom:16px;display:flex;gap:4px;z-index:6;}
.v29-dot{width:6px;height:6px;border-radius:3px;background:rgba(255,255,255,.4);}
.v29-dot.on{width:14px;background:#fff;}
/* 数字卡 */
.v29-statcard{position:relative;margin:-52px 20px 0;background:linear-gradient(180deg,rgba(255,255,255,.9),rgba(255,255,255,.74));border:1px solid var(--glass-border-dim);border-radius:18px;box-shadow:var(--glass-shadow-inner), 0 10px 24px rgba(46,125,79,.12);padding:13px 16px 12px;z-index:6;backdrop-filter:blur(18px) saturate(1.5);-webkit-backdrop-filter:blur(18px) saturate(1.5);}
.v29-statcard.frost{margin:-34px 20px 0;}
.v29-row3{display:flex;}
.v29-stt{flex:1;text-align:center;position:relative;display:flex;flex-direction:column;align-items:center;gap:2px;}
.v29-stt-n{font-size:22px;font-weight:900;color:#141414;line-height:1;}
.v29-stt-t{font-size:11px;color:#737c88;}
.v29-drow2{display:flex;align-items:center;margin-top:11px;padding-top:11px;border-top:1px dashed #e6e9ed;}
.v29-dnum{font-size:30px;font-weight:900;color:#141414;line-height:1;}
.v29-dlabel{margin-left:8px;font-size:12px;color:#737c88;}
/* 分区标题 */
.v29-sec{display:flex;align-items:baseline;justify-content:space-between;padding:18px 20px 10px;}
.v29-sec-t{font-size:17px;font-weight:700;color:#1a1a1a;}
.v29-sec-r{font-size:12px;color:#50a770;}
/* 热门横滑卡片 */
.v29-rcrow{display:flex;flex-wrap:nowrap;white-space:nowrap;padding:0 16px;box-sizing:border-box;}
.v29-rcard{display:inline-flex;flex-direction:row;width:262px;flex:none;background:linear-gradient(180deg,rgba(255,255,255,.9),rgba(255,255,255,.74));border:1px solid var(--glass-border-dim);border-radius:16px;overflow:hidden;box-shadow:var(--glass-shadow-inner), 0 3px 12px rgba(46,125,79,.07);margin-right:10px;align-items:stretch;}
.v29-rcard-img{width:96px;height:96px;flex-shrink:0;}
.v29-rcard-ph{width:96px;height:96px;flex-shrink:0;display:flex;align-items:center;justify-content:center;}
.v29-rcard-pad{padding:10px 12px;flex:1;min-width:0;display:flex;flex-direction:column;justify-content:center;gap:6px;white-space:normal;}
.v29-rcard-t{font-size:14px;font-weight:600;color:#1a1a1a;line-height:1.45;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;}
.v29-rcard-m{font-size:11px;color:#737c88;display:flex;align-items:center;gap:6px;}
.v29-pill{font-size:10px;color:#2e7d4f;background:#edf7f0;border-radius:4px;padding:2px 6px;font-weight:600;}
/* 上滑毛玻璃头部 */
.v29-frosttop{position:relative;height:150px;margin-left:-14px;margin-right:-14px;box-shadow:0 4px 14px rgba(20,40,30,.10);z-index:4;overflow:hidden;}
.v29-frost-img{position:absolute;inset:0;width:100%;height:100%;filter:blur(18px);transform:scale(1.4);opacity:.9;}
.v29-frost-img-ph{filter:none;transform:none;}
.v29-frost-bg{position:absolute;inset:0;background:rgba(255,255,255,.62);backdrop-filter:blur(18px);-webkit-backdrop-filter:blur(18px);}
.v29-frost-brand{position:absolute;left:16px;top:96px;z-index:3;display:flex;flex-direction:column;}
.v29-frost-name{font-size:14px;font-weight:900;color:#141414;letter-spacing:.5px;}
.v29-frost-en{font-size:9px;color:#737c88;letter-spacing:.5px;margin-top:2px;}
.v29-mix{align-items:flex-start;}
/* 毛玻璃兜底：不支持模糊时补不透明底，避免深色封面透上来 */
@supports not ((backdrop-filter:blur(1px)) or (-webkit-backdrop-filter:blur(1px))){
  .v29-searchbar{background:rgba(252,254,253,.96)}
  .v29-statcard{background:rgba(252,254,253,.96)}
  .v29-frost-bg{background:rgba(252,254,253,.96)}
}
</style>
