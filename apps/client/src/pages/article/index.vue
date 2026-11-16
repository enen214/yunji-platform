<script setup lang="ts">
import {onHide as onPageHide} from "@dcloudio/uni-app"
import {abandonLogin as cancelPageLogin} from "../../features/auth"
onPageHide(cancelPageLogin)

import LoginSheet from "../../components/LoginSheet.vue"

import {ref,computed} from 'vue'
import {onLoad,onShow,onHide,onUnload,onShareAppMessage,onShareTimeline} from '@dcloudio/uni-app'
import {request,navigate,event,token,authenticated,type Row} from '../../api/platform'
import {content} from '../../features/content'
import {contentLabel,presentationType,loadContentTypes} from '../../features/catalog'
import type {ArticleDetail} from '../../types/api'
import type {ReplacementHintView} from '../../types/models'
import {visibleStay} from '../../features/telemetry'
import PageState from '../../components/PageState.vue'
const id=ref(''),detail=ref<ArticleDetail|null>(null),replacement=ref<ReplacementHintView|null>(null)
const statistics=ref<Row|null>(null)
const error=ref(''),loading=ref(false),busy=ref(false),favorite=ref(false),following=ref(false)
const followedKeywords=ref<string[]>([]),clock=ref(Date.now())
let clockTimer:ReturnType<typeof setInterval>|undefined
const type=computed(()=>presentationType(detail.value?.article.contentType||''))
const presentation=computed(()=>detail.value?.presentation||{})
const countdown=computed(()=>{const a=detail.value?.article;if(!a)return '';const from=a.effectiveFrom?Date.parse(a.effectiveFrom):0,to=a.effectiveTo?Date.parse(a.effectiveTo):0;const target=from>clock.value?from:to;if(!target)return '长期有效';const left=target-clock.value;if(left<=0)return '已失效';const h=Math.floor(left/3600000),m=Math.floor(left/60000)%60;return (from>clock.value?'距生效 ':'距截止 ')+(h>=24?Math.floor(h/24)+'天 ':'')+(h%24)+'小时 '+m+'分钟'})
const rail=ref(false),source=ref(false),panel=ref(''),rule=ref<Row|null>(null)
const lifecycle=computed(()=>{const a=detail.value?.article;if(!a)return '';if(a.effectiveTo&&Date.parse(a.effectiveTo)<=clock.value)return '已失效 · 历史信息';if(a.effectiveFrom&&Date.parse(a.effectiveFrom)>clock.value)return '待生效';return '生效中'})
const panelNames:Row={favorite:'收藏资讯',follow:'关注信息',keywords:'关注关键词',share:'分享资讯',reminder:'设置强提醒'}
let pageActive=false
const stay=visibleStay(()=>id.value)
async function personal(){
 if(!token()){favorite.value=false;following.value=false;followedKeywords.value=[];rule.value=null;return}
 const [follows,favorites,rules]=await Promise.all([content.follows(),content.favorites(),request('GET','/api/v1/me/reminders/rules?size=1&targetType=ARTICLE&targetId='+id.value)])
 followedKeywords.value=follows.filter(f=>f.targetType==='KEYWORD').map(f=>f.targetId);following.value=follows.some(f=>f.targetType==='ARTICLE'&&f.targetId===id.value);favorite.value=favorites.some(f=>f.id===id.value);rule.value=rules.items[0]||null
}
async function load(){
 loading.value=true;error.value='';stay.flush()
 try{
  await loadContentTypes()
  try{detail.value=await content.detail(id.value);replacement.value=await content.replacement(id.value);if(replacement.value?.status==='NONE')replacement.value=null}catch(e){if((e as Row).status!==404)throw e;detail.value=null;replacement.value=await content.replacement(id.value);if(replacement.value?.status==='NONE')throw e;return}
  await event('PAGE_VIEW',id.value);statistics.value=await request('GET','/api/v1/articles/'+id.value+'/statistics');if(pageActive)stay.show()
  if(token()){await request('POST','/api/v1/me/history/'+id.value);await personal()}
 }catch(e){error.value=(e as Error).message}finally{loading.value=false}
}
async function act(fn:()=>Promise<void>){await authenticated(async()=>{if(busy.value)return;busy.value=true;error.value='';try{await fn()}catch(e){error.value=(e as Error).message}finally{busy.value=false}})}
async function open(kind:string){if(kind==='share'){panel.value=kind;return}await authenticated(async()=>{try{await personal();panel.value=kind}catch(e){error.value=(e as Error).message}})}
async function toggleFavorite(){await act(async()=>{await request('PUT','/api/v1/me/favorites/'+id.value,{enabled:!favorite.value});favorite.value=!favorite.value;panel.value=''})}
async function unfollow(type:string,target:string){
 const decision=await uni.showModal({title:'取消关注',content:'是否同时关闭对应强提醒？确认：一起关闭；取消：保留提醒。',confirmText:'一起关闭',cancelText:'保留提醒'})
 await request('PUT','/api/v1/me/follows/'+type+'/'+target,{enabled:false,stopReminders:!!decision.confirm})
}
async function toggleFollow(){await act(async()=>{if(following.value)await unfollow('ARTICLE',id.value);else await request('PUT','/api/v1/me/follows/ARTICLE/'+id.value,{enabled:true});await personal()})}
async function followTopic(target:string){await act(async()=>{if(followedKeywords.value.includes(target))await unfollow('KEYWORD',target);else await request('PUT','/api/v1/me/follows/KEYWORD/'+target,{enabled:true});await personal()})}
async function report(){await authenticated(()=>{uni.showModal({title:'举报内容',editable:true,placeholderText:'请说明问题',success:async r=>{if(r.confirm&&r.content)await act(async()=>{await request('POST','/api/v1/me/reports/'+id.value,{type:'OTHER',reason:r.content});uni.showToast({title:'已提交举报'})})}})})}
async function enableReminder(){await act(async()=>{await request('POST','/api/v1/me/reminders/daily',{targetType:'ARTICLE',targetId:id.value});await personal();panel.value='';uni.showToast({title:'已开启强提醒并关注'})})}
async function pauseReminder(){await act(async()=>{if(!rule.value)return;await request('PUT','/api/v1/me/reminders/rules/'+rule.value.id,{...rule.value,enabled:false,expectedVersion:rule.value.lockVersion});await personal();panel.value=''})}
function sharePath(){return '/pages/article/index?id='+id.value}
async function copyShare(){let link=sharePath()
 // #ifdef H5
 link=window.location.origin+window.location.pathname+'#'+sharePath()
 // #endif
 uni.setClipboardData({data:link,success(){void event('SHARE',id.value);panel.value=''}})
}
function timeline(){
 // #ifdef MP-WEIXIN
 uni.showShareMenu({withShareTicket:true,menus:['shareAppMessage','shareTimeline']});uni.showToast({title:'请点右上角菜单分享到朋友圈',icon:'none'})
 // #endif
}
onShareAppMessage(()=>{void event('SHARE',id.value);return {title:detail.value?.article.title||'云迹资讯',path:sharePath()}})
onShareTimeline(()=>({title:detail.value?.article.title||'云迹资讯',query:'id='+id.value}))
let wechat=false
// #ifdef MP-WEIXIN
wechat=true
// #endif
onLoad(q=>{id.value=String(q?.id||'');void load()})
onShow(()=>{pageActive=true;clock.value=Date.now();if(clockTimer)clearInterval(clockTimer);clockTimer=setInterval(()=>clock.value=Date.now(),30000);if(detail.value){stay.show();void personal().catch(e=>{error.value=(e as Error).message})}})
onHide(()=>{if(clockTimer)clearInterval(clockTimer);pageActive=false;panel.value='';stay.flush()});onUnload(()=>{if(clockTimer)clearInterval(clockTimer);pageActive=false;stay.flush()})
// #ifdef H5
function visibility(){if(document.hidden)stay.hide();else if(pageActive&&detail.value)stay.show()}
document.addEventListener('visibilitychange',visibility);onUnload(()=>document.removeEventListener('visibilitychange',visibility))
// #endif
function openSource(){const url=detail.value?.sourceUrl;if(!url)return
 // #ifdef H5
 window.open(url,'_blank','noopener,noreferrer');return
 // #endif
 // #ifdef MP-WEIXIN
 uni.setClipboardData({data:String(url)})
 // #endif
}
function taxonomySearch(kind:'category'|'label'|'keyword',topic:{id:string}){const key=kind==='category'?'categoryId':kind==='label'?'labelId':'keywordId';navigate('search','?'+key+'='+encodeURIComponent(topic.id))}
</script>
<template><view class="page article-page" :class="{'rail-open':rail}">
<PageState :busy="loading" :error="error" @retry="load"/>
<view v-if="replacement" class="card notice-card"><text class="badge">内容已更新</text><text class="section-title">这条资讯已有新内容</text><template v-if="replacement.target"><text class="muted">新的公开资讯：{{ replacement.target.title }}</text><button role="button" @click="navigate('article','?id='+replacement.target.id)">查看新资讯 →</button><text class="muted small">关注与提醒设置已自动转移，可在“管理关注”中管理。</text></template><text v-else class="muted">替换资讯暂不可见，请稍后再查看。</text></view>
<template v-if="detail"><view class="article-main">
<view class="hero article-hero"><view class="meta-line"><text class="badge">{{ contentLabel(detail.article.contentType) }}</text><text class="muted">{{ detail.article.sourceName||'云迹' }}</text><text class="small">{{ detail.article.publishedAt?.slice(0,10) }}</text></view><text class="title">{{ detail.article.title }}</text><text v-if="detail.article.abnormal" class="error">内容已被举报，正在核查。</text></view>
<view v-if="detail.categories?.length||detail.labels?.length||detail.keywords?.length" class="card article-taxonomy" aria-label="资讯分类与关键词">
<view v-if="detail.categories?.length" class="taxonomy-row"><text class="taxonomy-name">板块</text><view class="taxonomy-values"><button v-for="item in detail.categories" :key="item.id" role="button" class="taxonomy-chip" @click="taxonomySearch('category',item)">{{ item.name }}</button></view></view>
<view v-if="detail.labels?.length" class="taxonomy-row"><text class="taxonomy-name">标签</text><view class="taxonomy-values"><button v-for="item in detail.labels" :key="item.id" role="button" class="taxonomy-chip" @click="taxonomySearch('label',item)">{{ item.name }}</button></view></view>
<view v-if="detail.keywords?.length" class="taxonomy-row"><text class="taxonomy-name">关键词</text><view class="taxonomy-values"><button v-for="item in detail.keywords" :key="item.id" role="button" class="taxonomy-chip" @click="taxonomySearch('keyword',item)">{{ item.name }}</button></view></view>
</view>
<view v-if="type==='NOTICE'" class="article-status"><view class="status-dot"/><text>{{ lifecycle }}</text><text class="muted">{{ countdown }}</text></view>
<view v-if="type==='NOTICE'" class="card notice-card"><text class="card-title">通知时间</text><text class="muted">开始：{{ detail.article.effectiveFrom||'发布即生效' }}</text><text class="muted">结束：{{ detail.article.effectiveTo||'长期有效' }}</text></view>
<view class="card article-content"><text class="card-title">正文</text><text v-if="detail.article.summary" class="muted article-summary">{{ detail.article.summary }}</text><rich-text class="body" :nodes="detail.richText"/><text v-if="!detail.richText" class="muted">暂无正文</text></view>

<template v-if="type==='BASIC'"><view v-if="presentation.contacts?.length" class="card"><text class="card-title">联系方式</text><view v-for="(contact,i) in presentation.contacts" :key="i" class="detail-kv"><text>{{ contact.label }}</text><text selectable>{{ contact.value }}</text></view></view><view v-if="detail.related.length" class="card"><text class="card-title">相关内容</text><view v-for="r in detail.related" :key="r.id" class="list-item row" @click="navigate('article','?id='+r.id)"><text>{{ r.name }}</text><text>›</text></view></view></template>
<view v-if="type!=='GUIDE'&&(detail.originalText||detail.sourceUrl)" class="card article-source"><view class="source-row" role="button" :aria-expanded="source" @click="source=!source"><text class="card-title">查看原始来源</text><text class="section-link">{{ source?'收起 ⌃':'展开 ⌄' }}</text></view><view v-if="source"><text class="muted">{{ detail.article.sourceName||'未提供发布单位' }} · {{ detail.article.publishedAt?.slice(0,10) }} · {{ contentLabel(detail.article.contentType) }}</text><text class="field-label">原文</text><scroll-view scroll-y class="source-scroll"><text class="source-text" selectable>{{ detail.originalText||'暂无原文' }}</text></scroll-view><template v-if="detail.sourceUrl"><text selectable class="muted small">{{ detail.sourceUrl }}</text><button role="button" class="quiet" @click="openSource">查看原文 / 复制链接</button></template></view></view>
<view v-if="statistics" class="card"><text class="small muted">浏览 {{ statistics.views }}</text></view><button role="button" class="quiet report-link" @click="report">举报内容</button>
</view><view v-if="rail" class="article-rail"><button role="button" class="rail-button" @click="open('favorite')"><text class="rail-symbol">{{ favorite?'★':'☆' }}</text><text>{{ favorite?'已收藏':'收藏' }}</text></button><button role="button" class="rail-button" @click="toggleFollow"><text class="rail-symbol">♡</text><text>{{ following?'已关注':'关注信息' }}</text></button><button role="button" class="rail-button" @click="open('keywords')"><text class="rail-symbol">#</text><text>关键词</text></button><button role="button" class="rail-button" @click="open('share')"><text class="rail-symbol">↗</text><text>分享</text></button><button role="button" class="rail-button" @click="open('reminder')"><text class="rail-symbol">♧</text><text>{{ rule?.enabled?'已设提醒':'强提醒' }}</text></button></view><button role="button" class="rail-toggle" :aria-label="rail?'收起操作栏':'展开操作栏'" @click="rail=!rail">{{ rail?'‹':'›' }}</button></template>
<view v-if="panel" class="sheet-mask modal-mask" @click="!busy&&(panel='')"><view class="filter-sheet" role="dialog" :aria-label="panelNames[panel]" @click.stop><view class="row"><text class="section-title">{{ panelNames[panel] }}</text><button role="button" class="icon-button" :disabled="busy" aria-label="关闭操作弹窗" @click="panel=''">×</button></view>
<template v-if="panel==='favorite'"><text class="muted">收藏后可在“我的 → 管理收藏”找到这条资讯。</text><button role="button" :disabled="busy" @click="toggleFavorite">{{ favorite?'取消收藏':'确认收藏' }}</button></template>
<template v-if="panel==='follow'"><text class="muted">关注后在信息盒子接收更替与新资讯。外部消息须另设强提醒。</text><button role="button" :disabled="busy" @click="toggleFollow">{{ following?'取消关注':'确认关注' }}</button></template>
<template v-if="panel==='keywords'"><text class="field-label">本条资讯的关键词</text><view class="tag-grid"><button role="button" v-for="k in detail?.keywords" :key="k.id" :class="followedKeywords.includes(k.id)?'':'quiet'" :disabled="busy" @click="followTopic(k.id)">{{ k.name }} {{ followedKeywords.includes(k.id)?'✓ 已关注':'＋ 关注' }}</button></view><text v-if="!detail?.keywords.length" class="muted">本条资讯暂无关联关键词</text></template>
<template v-if="panel==='share'"><view class="share-options"><button role="button" v-if="wechat" open-type="share">分享给微信好友</button><button role="button" v-if="wechat" class="secondary" @click="timeline">分享到朋友圈</button><button role="button" class="quiet" @click="copyShare">复制链接</button></view><text v-if="!wechat" class="muted">浏览器中可复制链接，在微信内可使用原生分享。</text></template>
<template v-if="panel==='reminder'"><text class="muted">{{ rule?.enabled?'此资讯已开启强提醒；关闭提醒后仍保留普通关注。':rule?.mode==='INSTANT'?'恢复此资讯原有的即时强提醒，需有效的一次性订阅授权。':'确认后自动关注此资讯，并开启日常强提醒；后续更新按所选时段汇总到企业微信。' }}</text><text v-if="rule?.enabled&&rule.mode==='INSTANT'" class="muted">当前为即时提醒，继续保留现有方式。</text><button role="button" v-if="!rule?.enabled" :disabled="busy" @click="enableReminder">确定</button><button role="button" v-else class="danger" :disabled="busy" @click="pauseReminder">取消强提醒</button><button role="button" class="quiet" :disabled="busy" @click="panel=''">{{ rule?.enabled?'知道了':'再想想' }}</button></template>
</view></view><LoginSheet/></view></template>
