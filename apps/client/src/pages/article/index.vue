<script setup lang="ts">
import {onHide as onPageHide} from "@dcloudio/uni-app"
import {abandonLogin as cancelPageLogin} from "../../features/auth"
onPageHide(cancelPageLogin)

import LoginSheet from "../../components/LoginSheet.vue"
import PageNav from "../../components/PageNav.vue"

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
const error=ref(''),loading=ref(false),busy=ref(false),favorite=ref(false),following=ref(false),wecomBound=ref<'BOUND'|'UNBOUND'|'UNKNOWN'>('UNKNOWN')
const followedKeywords=ref<string[]>([]),clock=ref(Date.now())
let clockTimer:ReturnType<typeof setInterval>|undefined
const type=computed(()=>presentationType(detail.value?.article.contentType||''))
const presentation=computed(()=>detail.value?.presentation||{})
// 小程序 rich-text 不吃页面里的 p 选择器，段距要写进节点内联样式，两端才一致
const bodyHtml=computed(()=>String(detail.value?.richText||'')
 .replace(/<p([^>]*)>/gi,(_m,attrs:string)=>'<p style="margin:0 0 18px"'+(attrs||'').replace(/\sstyle\s*=\s*"[^"]*"/i,'')+'>'))
// originalText 是富文本，原文区是纯文本展示，先把标签拆掉再排，否则会看到一堆 <p>
const originalPlain=computed(()=>String(detail.value?.originalText||'')
 .replace(/<br\s*\/?>/gi,'\n')
 .replace(/<\/?(p|div|li|h[1-6]|tr|blockquote)(\s[^>]*)?>/gi,'\n')
 .replace(/<[^>]+>/g,'')
 .replace(/&nbsp;/g,' ').replace(/&amp;/g,'&').replace(/&lt;/g,'<').replace(/&gt;/g,'>').replace(/&quot;/g,'"')
 .replace(/[ \t]+\n/g,'\n').replace(/\n{3,}/g,'\n\n').trim())
const countdown=computed(()=>{const a=detail.value?.article;if(!a)return '';const from=a.effectiveFrom?Date.parse(a.effectiveFrom):0,to=a.effectiveTo?Date.parse(a.effectiveTo):0;const target=from>clock.value?from:to;if(!target)return '长期有效';const left=target-clock.value;if(left<=0)return '已失效';const h=Math.floor(left/3600000),m=Math.floor(left/60000)%60;return (from>clock.value?'距生效 ':'距截止 ')+(h>=24?Math.floor(h/24)+'天 ':'')+(h%24)+'小时 '+m+'分钟'})
const source=ref(false),panel=ref(''),rule=ref<Row|null>(null)
// 板块 / 标签 / 关键词并成一行，点哪一个都能进搜索，不再分三行占高度
// 同名会重复（标签「校招」与关键词「校招」是两条），按名字去重，避免一排里出现两个一样的胶囊
const taxonomy=computed(()=>{
 const seen=new Set<string>()
 return [
  ...(detail.value?.categories||[]).map(item=>({kind:'category' as const,item})),
  ...(detail.value?.labels||[]).map(item=>({kind:'label' as const,item})),
  ...(detail.value?.keywords||[]).map(item=>({kind:'keyword' as const,item})),
 ].filter(t=>{const name=String(t.item.name||'').trim();if(!name||seen.has(name))return false;seen.add(name);return true})
})
const lifecycle=computed(()=>{const a=detail.value?.article;if(!a)return '';if(a.effectiveTo&&Date.parse(a.effectiveTo)<=clock.value)return '已失效 · 历史信息';if(a.effectiveFrom&&Date.parse(a.effectiveFrom)>clock.value)return '未开始';return '生效中'})
// 后端给的是 ISO（2026-11-01T00:00:00Z），直接摊在页面上不好看，统一成 2026-11-01 00:00
function fmtTime(value?:string|null){if(!value)return '';const d=new Date(value);if(Number.isNaN(d.getTime()))return String(value);const pad=(n:number)=>String(n).padStart(2,'0');return d.getFullYear()+'-'+pad(d.getMonth()+1)+'-'+pad(d.getDate())+' '+pad(d.getHours())+':'+pad(d.getMinutes())}
const panelNames:Row={favorite:'收藏资讯',follow:'关注信息',keywords:'关注关键词',share:'分享资讯',reminder:'设置强提醒'}
let pageActive=false
const stay=visibleStay(()=>id.value)
async function refreshBound(){try{const settings=await request('GET','/api/v1/me/reminders/settings');const permission=((settings.permissions as Row[])||[])[0];wecomBound.value=typeof permission?.wecomBound==='boolean'?(permission.wecomBound?'BOUND':'UNBOUND'):'UNKNOWN'}catch{}}
async function personal(){
 if(!token()){favorite.value=false;following.value=false;followedKeywords.value=[];rule.value=null;wecomBound.value='UNKNOWN';return}
 const [follows,favorites,rules]=await Promise.all([content.follows(),content.favorites(),request('GET','/api/v1/me/reminders/rules?size=1&targetType=ARTICLE&targetId='+id.value)])
 followedKeywords.value=follows.filter(f=>f.targetType==='KEYWORD').map(f=>f.targetId);following.value=follows.some(f=>f.targetType==='ARTICLE'&&f.targetId===id.value);favorite.value=favorites.some(f=>f.id===id.value);rule.value=rules.items[0]||null
 await refreshBound()
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
 let hasEnabled=false
 if(type==='ARTICLE'){hasEnabled=!!rule.value?.enabled}else{try{const rules=await request('GET','/api/v1/me/reminders/rules?size=1&targetType='+type+'&targetId='+target);hasEnabled=((rules.items as Row[])||[]).some((r:Row)=>r.enabled)}catch(e){hasEnabled=false}}
 let stopReminders=false
 if(hasEnabled){const decision=await uni.showModal({title:'取消关注',content:'是否同时关闭对应强提醒？确认：一起关闭；取消：保留提醒。',confirmText:'一起关闭',cancelText:'保留提醒'});stopReminders=!!decision.confirm}
 await request('PUT','/api/v1/me/follows/'+type+'/'+target,{enabled:false,stopReminders})
}
async function toggleFollow(){await act(async()=>{if(following.value)await unfollow('ARTICLE',id.value);else await request('PUT','/api/v1/me/follows/ARTICLE/'+id.value,{enabled:true});await personal()})}
async function followTopic(target:string){await act(async()=>{if(followedKeywords.value.includes(target))await unfollow('KEYWORD',target);else await request('PUT','/api/v1/me/follows/KEYWORD/'+target,{enabled:true});await personal()})}

// 详情页「提醒」先要一次性订阅授权；模板 ID 只从后端设置里取，不写死。
// #ifdef MP-WEIXIN
async function requestInstantConsent():Promise<boolean>{
 try{
  const settings=await request('GET','/api/v1/me/reminders/settings')
  const permission=((settings.permissions as Row[])||[])[0]
  const templateId=String(permission?.templateId||settings.instantTemplate||'')
  if(!templateId)return true
  const result=await new Promise<Row>((resolve,reject)=>uni.requestSubscribeMessage({tmplIds:[templateId],success:resolve,fail:reject}))
  const status=String(result?.[templateId]||'')
  if(status==='accept')return true
  if(status==='reject'||status==='ban'){uni.showToast({title:'未授权订阅消息，提醒无法送达',icon:'none'});return false}
  return true
 }catch{return true}
}
// #endif

async function enableReminder(){
 // #ifdef MP-WEIXIN
 if(!await requestInstantConsent())return
 // #endif
 if(wecomBound.value==='UNKNOWN')await refreshBound();if(wecomBound.value==='UNBOUND'){const decision=await uni.showModal({title:'需要先绑定企业微信',content:'日常强提醒通过企业微信发送，绑定后回到本页即可开启',confirmText:'去绑定',cancelText:'稍后'});if(decision.confirm)navigate('notification-settings');return}await act(async()=>{await request('POST','/api/v1/me/reminders/daily',{targetType:'ARTICLE',targetId:id.value});await personal();panel.value='';uni.showToast({title:'已开启强提醒并关注'})})}
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
function taxonomySearch(kind:'category'|'label'|'keyword',topic:{id:string,name?:string}){const key=kind==='category'?'categoryId':kind==='label'?'labelId':'keywordId';let q='?'+key+'='+encodeURIComponent(topic.id);if(topic.name)q+='&echo='+encodeURIComponent(topic.name);navigate('search',q)}
</script>
<template><view class="page article-page">
<view class="page-aurora"/>
<PageNav title="资讯详情"/>
<PageState :busy="loading" :error="error" @retry="load"/>
<view v-if="replacement" class="card notice-card"><text class="badge">内容已更新</text><text class="section-title">这条资讯已有新内容</text><template v-if="replacement.target"><text class="muted">新的公开资讯：{{ replacement.target.title }}</text><button role="button" @click="navigate('article','?id='+replacement.target.id)">查看新资讯 →</button><text class="muted small">关注与提醒设置已自动转移，可在“管理关注”中管理。</text></template><text v-else class="muted">替换资讯暂不可见，请稍后再查看。</text></view>
<template v-if="detail"><view class="article-main">
<view class="an-head">
<view class="an-kicker"><text class="k-type">{{ contentLabel(detail.article.contentType) }}</text><text class="k-sep">·</text><text>{{ detail.article.sourceName||'云迹' }}</text><text class="k-sep">·</text><text>{{ detail.article.publishedAt?.slice(0,10) }}</text></view>
<text class="an-title">{{ detail.article.title }}</text>
<text v-if="detail.article.abnormal" class="error an-warn">内容已被举报，正在核查。</text>
<text v-if="detail.article.summary" class="an-lede">{{ detail.article.summary }}</text>
</view>
<scroll-view v-if="taxonomy.length" scroll-x class="an-tags" :show-scrollbar="false" aria-label="资讯分类与关键词"><text v-for="t in taxonomy" :key="t.kind+t.item.id" :class="['an-tag',t.kind==='category'?'k':'']" @click="taxonomySearch(t.kind,t.item)">{{ t.item.name }}</text></scroll-view>
<view v-if="type==='NOTICE'" class="an-status"><view class="status-dot"/><text>{{ lifecycle }}</text><text class="muted">{{ countdown }}</text></view>
<view v-if="type==='NOTICE'" class="an-note"><view class="r"><text class="k">开始</text><text>{{ detail.article.effectiveFrom?fmtTime(detail.article.effectiveFrom):'发布即生效' }}</text></view><view class="r"><text class="k">结束</text><text>{{ detail.article.effectiveTo?fmtTime(detail.article.effectiveTo):'长期有效' }}</text></view></view>
<view class="an-body"><rich-text class="body" :nodes="bodyHtml"/><text v-if="!detail.richText" class="muted">暂无正文</text></view>
<template v-if="type==='BASIC'"><view v-if="presentation.contacts?.length" class="an-block"><text class="an-block-title">联系方式</text><view v-for="(contact,i) in presentation.contacts" :key="i" class="an-kv"><text>{{ contact.label }}</text><text selectable>{{ contact.value }}</text></view></view><view v-if="detail.related.length" class="an-block"><text class="an-block-title">相关内容</text><view v-for="r in detail.related" :key="r.id" class="an-link-row" @click="navigate('article','?id='+r.id)"><text class="lb">{{ r.name }}</text><text class="rt">›</text></view></view></template>
<view class="an-foot">
<view v-if="type!=='GUIDE'&&(detail.originalText||detail.sourceUrl)" class="an-src" :class="{open:source}"><view class="an-src-row" role="button" :aria-expanded="source" @click="source=!source"><text class="lb">查看原始来源</text><view class="rt"><text>{{ source?'收起':'展开' }}</text><view class="an-caret"/></view></view><view class="an-src-body" :class="{open:source}"><text class="an-src-meta">{{ detail.article.sourceName||'云迹' }} · {{ detail.article.publishedAt?.slice(0,10) }} · {{ contentLabel(detail.article.contentType) }}</text><scroll-view scroll-y class="an-src-scroll"><text class="an-src-text" selectable>{{ originalPlain||'暂无原文' }}</text></scroll-view><template v-if="detail.sourceUrl"><text selectable class="an-src-url">{{ detail.sourceUrl }}</text><button role="button" class="quiet" @click="openSource">打开原文链接</button></template></view></view>
<text class="an-sig">{{ statistics?('浏览 '+statistics.views+' · '):'' }}内容由云迹同步 · 如有出入以原文为准</text>
</view>
</view><view class="an-bar">
<button role="button" :class="['an-act',favorite?'fav':'']" @click="open('favorite')"><image class="sym" :src="favorite?'/static/article/star-on.png':'/static/article/star.png'"/><text>{{ favorite?'已收藏':'收藏' }}</text></button>
<button role="button" :class="['an-act',following?'on':'']" @click="toggleFollow"><image class="sym" :src="following?'/static/article/heart-on.png':'/static/article/heart.png'"/><text>{{ following?'已关注':'关注' }}</text></button>
<button role="button" class="an-act" @click="open('keywords')"><image class="sym" src="/static/article/hash.png"/><text>关键词</text></button>
<button role="button" :class="['an-act',rule?.enabled?'on':'']" @click="open('reminder')"><image class="sym" :src="rule?.enabled?'/static/article/bell-on.png':'/static/article/bell.png'"/><text>{{ rule?.enabled?'已提醒':'提醒' }}</text></button>
<button role="button" class="an-act" @click="open('share')"><image class="sym" src="/static/article/share.png"/><text>分享</text></button>
</view><view class="article-fade"/></template>
<view v-if="panel" class="sheet-mask modal-mask" @click="!busy&&(panel='')"><view class="filter-sheet" role="dialog" :aria-label="panelNames[panel]" @click.stop><view class="row"><text class="section-title">{{ panelNames[panel] }}</text><button role="button" class="icon-button" :disabled="busy" aria-label="关闭操作弹窗" @click="panel=''">×</button></view>
<template v-if="panel==='favorite'"><text class="muted">收藏后可在“我的 → 管理收藏”找到这条资讯。</text><button role="button" :disabled="busy" @click="toggleFavorite">{{ favorite?'取消收藏':'确认收藏' }}</button></template>
<template v-if="panel==='follow'"><text class="muted">关注后在信息盒子接收更替与新资讯。外部消息须另设强提醒。</text><button role="button" :disabled="busy" @click="toggleFollow">{{ following?'取消关注':'确认关注' }}</button></template>
<template v-if="panel==='keywords'"><text class="field-label">本条资讯的关键词</text><view class="tag-grid"><button role="button" v-for="k in detail?.keywords" :key="k.id" :class="followedKeywords.includes(k.id)?'':'quiet'" :disabled="busy" @click="followTopic(k.id)">{{ k.name }} {{ followedKeywords.includes(k.id)?'✓ 已关注':'＋ 关注' }}</button></view><text v-if="!detail?.keywords.length" class="muted">本条资讯暂无关联关键词</text></template>
<template v-if="panel==='share'"><view class="share-options"><button role="button" v-if="wechat" open-type="share">分享给微信好友</button><button role="button" v-if="wechat" class="secondary" @click="timeline">分享到朋友圈</button><button role="button" class="quiet" @click="copyShare">复制链接</button></view><text v-if="!wechat" class="muted">浏览器中可复制链接，在微信内可使用原生分享。</text></template>
<template v-if="panel==='reminder'"><text class="muted">{{ rule?.enabled?'此资讯已开启强提醒；关闭提醒后仍保留普通关注。':rule?.mode==='INSTANT'?'恢复此资讯原有的即时强提醒，需有效的一次性订阅授权。':'确认后自动关注此资讯，并开启日常强提醒；后续更新按所选时段汇总到企业微信。'+(wecomBound==='UNBOUND'?'需先绑定企业微信':'') }}</text><text v-if="rule?.enabled&&rule.mode==='INSTANT'" class="muted">当前为即时提醒，继续保留现有方式。</text><button role="button" v-if="!rule?.enabled" :disabled="busy" @click="enableReminder">确定</button><button role="button" v-else class="danger" :disabled="busy" @click="pauseReminder">取消强提醒</button><button role="button" class="quiet" :disabled="busy" @click="panel=''">{{ rule?.enabled?'知道了':'再想想' }}</button></template>
</view></view><LoginSheet/></view></template>
