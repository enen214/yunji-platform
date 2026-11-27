<script setup lang="ts">
import {onHide as onPageHide} from "@dcloudio/uni-app"
import {abandonLogin as cancelPageLogin} from "../../features/auth"
onPageHide(cancelPageLogin)

import LoginSheet from "../../components/LoginSheet.vue"
import PageNav from "../../components/PageNav.vue"

import { ref,computed,watch } from 'vue'
import { onLoad,onShow,onReachBottom } from '@dcloudio/uni-app'
import { request,navigate,token,event,type Row } from '../../api/platform'
import type { ArticleCard,ArticlePage } from '../../types/api'
import {contentTypes,loadContentTypes} from '../../features/catalog'
import PageState from '../../components/PageState.vue'
import DiscoveryCards from '../../components/DiscoveryCards.vue'
const q=ref(''),categoryIds=ref<string[]>([]),typeCodes=ref<string[]>([]),labelIds=ref<string[]>([]),keywordIds=ref<string[]>([]),expired=ref(false),sheet=ref(false),searched=ref(false),busy=ref(false),error=ref(''),cursor=ref(''),snapshot=ref(''),total=ref(0),echoOnly=ref(false)
const focused=ref(false),removingKey=ref('')
const draft=ref({typeCodes:[] as string[],categoryIds:[] as string[],labelIds:[] as string[],keywordIds:[] as string[],expired:false})
function openFilter(){draft.value={typeCodes:[...typeCodes.value],categoryIds:[...categoryIds.value],labelIds:[...labelIds.value],keywordIds:[...keywordIds.value],expired:expired.value};sheet.value=true}
function apply(){typeCodes.value=[...draft.value.typeCodes];categoryIds.value=[...draft.value.categoryIds];labelIds.value=[...draft.value.labelIds];keywordIds.value=[...draft.value.keywordIds];expired.value=draft.value.expired;void search(true,false)}
// 「检索」不再是一个按钮：输入即搜（Apple Music 也是边打边出结果），键盘搜索键只是同一件事的手势。
// 代价是要防抖，且打字过程不写搜索历史，只有回车 / 点热词 / 点历史才记。
let inputTimer:ReturnType<typeof setTimeout>|null=null
function onInput(){echoOnly.value=false;if(inputTimer)clearTimeout(inputTimer);const text=q.value.trim();if(!text){searched.value=false;items.value=[];total.value=0;cursor.value='';return}inputTimer=setTimeout(()=>void search(true,false),420)}
function clearQuery(){if(inputTimer)clearTimeout(inputTimer);q.value='';echoOnly.value=false;searched.value=false;items.value=[];total.value=0;cursor.value='';error.value=''}
// 生效中的筛选条件做成可删的胶囊行：筛选入口从"两个并排按钮"收进搜索框右侧一个图标，
// 但条件一旦生效就必须看得见、点得掉，否则用户不知道结果为什么变少。
const filterCount=computed(()=>typeCodes.value.length+categoryIds.value.length+labelIds.value.length+keywordIds.value.length+(expired.value?1:0))
const filterChips=computed(()=>{const out:{key:string;label:string;kind:string;id?:string}[]=[]
 if(!filterCount.value)return out
 typeCodes.value.forEach(c=>out.push({key:'t:'+c,label:contentTypes.value.find(t=>t.code===c)?.name||c,kind:'type',id:c}))
 categoryIds.value.forEach(id=>out.push({key:'c:'+id,label:categories.value.find(c=>c.id===id)?.name||'板块',kind:'cat',id}))
 labelIds.value.forEach(id=>out.push({key:'l:'+id,label:labels.value.find(l=>l.id===id)?.name||'标签',kind:'label',id}))
 keywordIds.value.forEach(id=>out.push({key:'k:'+id,label:keywords.value.find(k=>k.id===id)?.name||'关键词',kind:'kw',id}))
 if(expired.value)out.push({key:'expired',label:'含过期',kind:'expired'})
 return out})
function dropFilter(f:{kind:string;id?:string}){
 if(f.kind==='type')typeCodes.value=typeCodes.value.filter(x=>x!==f.id)
 if(f.kind==='cat'){categoryIds.value=categoryIds.value.filter(x=>x!==f.id);labelIds.value=labelIds.value.filter(id=>labels.value.some(l=>l.id===id&&categoryIds.value.includes(l.categoryId)))}
 if(f.kind==='label')labelIds.value=labelIds.value.filter(x=>x!==f.id)
 if(f.kind==='kw')keywordIds.value=keywordIds.value.filter(x=>x!==f.id)
 if(f.kind==='expired')expired.value=false
 void search(true,false)}
function resetFilters(){typeCodes.value=[];categoryIds.value=[];labelIds.value=[];keywordIds.value=[];expired.value=false;void search(true,false)}
const categories=ref<Row[]>([]),labels=ref<Row[]>([]),keywords=ref<Row[]>([]),history=ref<Row[]>([]),hot=ref<Row[]>([]),popular=ref<Row[]>([]),items=ref<ArticleCard[]>([])
// 后端封面字段为空：按 id 固定分配本地配图，同一条资讯在哪都长一样
const popCovers=['/static/covers/cover-notice.jpg','/static/covers/cover-study.jpg','/static/covers/cover-remind.jpg','/static/covers/cover-campus.jpg']
function popCover(a:Row,i:number){return String(a.coverUrl||'')||popCovers[i%popCovers.length]}
const types=computed<{code:string;name:string}[]>(()=>[{code:'',name:'全部'},...contentTypes.value.map(t=>({code:t.code,name:t.name}))])
function toggleContentType(code:string){draft.value.typeCodes=draft.value.typeCodes.includes(code)?draft.value.typeCodes.filter(x=>x!==code):[...draft.value.typeCodes,code]}
function toggleBoard(id:string){draft.value.categoryIds=draft.value.categoryIds.includes(id)?draft.value.categoryIds.filter(x=>x!==id):[...draft.value.categoryIds,id];draft.value.labelIds=draft.value.labelIds.filter(id=>labels.value.some(l=>l.id===id&&draft.value.categoryIds.includes(l.categoryId)))}
const visibleLabels=computed(()=>labels.value.filter(l=>draft.value.categoryIds.includes(l.categoryId)))
// 已登录走后端历史，未登录或请求失败静默回退到本地，绝不把 401 抛给用户
async function loadHistory(){const local:Row[]=uni.getStorageSync('yunji.search.history')||[];if(!token()){history.value=local;return}try{const list=await request<Row[]>('GET','/api/v1/me/search-history');history.value=(list||[]).filter(h=>!!h?.query).map(h=>({query:String(h.query),searchedAt:h.searchedAt}))}catch{history.value=local}}
function recordHistory(query:string){history.value=[{query},...history.value.filter((h:Row)=>h.query!==query)].slice(0,20);uni.setStorageSync('yunji.search.history',history.value);if(token())void request('POST','/api/v1/me/search-history',{query}).catch(()=>{})}
function removeHistory(query:string){if(removingKey.value)return;removingKey.value=query;setTimeout(()=>{history.value=history.value.filter((h:Row)=>h.query!==query);uni.setStorageSync('yunji.search.history',history.value);removingKey.value='';if(token())void request('DELETE','/api/v1/me/search-history/item?query='+encodeURIComponent(query)).catch(()=>{})},160)}
function clear(){uni.showModal({title:'清空历史搜索',content:'确定要清空全部搜索历史吗？',confirmText:'清空',confirmColor:'#2E9A6B',success:(r)=>{if(!r.confirm)return;history.value=[];uni.removeStorageSync('yunji.search.history');if(token())void request('DELETE','/api/v1/me/search-history').catch(()=>{})}})}
function toggle(id:string){draft.value.labelIds=draft.value.labelIds.includes(id)?draft.value.labelIds.filter(x=>x!==id):[...draft.value.labelIds,id]}
function toggleKeyword(id:string){draft.value.keywordIds=draft.value.keywordIds.includes(id)?draft.value.keywordIds.filter(x=>x!==id):[...draft.value.keywordIds,id]}
async function search(reset=true,record=true){if(busy.value)return;busy.value=true;error.value='';sheet.value=false;try{
 if(reset){cursor.value='';snapshot.value='';items.value=[];searched.value=true;if(record&&q.value.trim())recordHistory(q.value.trim())}
 const textQuery=echoOnly.value?'':q.value.trim()
 let query='includeExpired='+expired.value+'&pageSize=20'+(textQuery?'&q='+encodeURIComponent(textQuery):'')
 for(const [key,value] of Object.entries({categoryIds:categoryIds.value.join(','),contentType:typeCodes.value.join(','),labelIds:labelIds.value.join(','),keywordIds:keywordIds.value.join(','),cursor:cursor.value,snapshotAt:snapshot.value}))if(value)query+='&'+key+'='+encodeURIComponent(value)
 const page=await request<ArticlePage>('GET','/api/v1/articles/search?'+query);items.value.push(...page.items);cursor.value=page.nextCursor||'';snapshot.value=page.snapshotAt;total.value=page.total
 if(reset)await event('FILTER',undefined,{q:textQuery,categoryIds:categoryIds.value,labels:labelIds.value,includeExpired:expired.value})
 }catch(e){error.value=(e as Error).message}finally{busy.value=false}}
async function missing(){if(!token()){navigate('login');return}uni.showModal({title:'缺失信息申报',editable:true,placeholderText:'请描述缺少的内容或提供来源',success:async r=>{if(r.confirm&&r.content)try{await request('POST','/api/v1/me/feedback',{description:'缺失信息申报：'+r.content});uni.showToast({title:'已提交'})}catch(e){error.value=(e as Error).message}}})}
async function init(options:Row={}){try{await loadContentTypes();popular.value=await request('GET','/api/v1/search/popular');if(options?.echo){q.value=decodeURIComponent(String(options.echo));echoOnly.value=true}else{q.value=String(options?.q||'')}categoryIds.value=options?.categoryId?[String(options.categoryId)]:[];if(options?.keywordId)keywordIds.value=[String(options.keywordId)];if(options?.labelId)labelIds.value=[String(options.labelId)];[categories.value,labels.value,keywords.value,hot.value]=await Promise.all([request('GET','/api/v1/catalog/categories'),request('GET','/api/v1/catalog/labels'),request('GET','/api/v1/catalog/keywords?limit=100'),request('GET','/api/v1/search/hot')]);await loadHistory();if(q.value||categoryIds.value.length||keywordIds.value.length||labelIds.value.length)await search()}catch(e){error.value=(e as Error).message}}
// tabBar 页 switchTab 不能带参数：带条件进搜索时先把 query 暂存，落地再取
function takeTabQuery():Row|null{const raw=uni.getStorageSync('yj:tab-query:search');if(!raw)return null;uni.removeStorageSync('yj:tab-query:search');const out:Row={};String(raw).replace(/^\?/,'').split('&').forEach(kv=>{const i=kv.indexOf('=');if(i>0)out[decodeURIComponent(kv.slice(0,i))]=decodeURIComponent(kv.slice(i+1))});return out}
onLoad(o=>init(o||{}))
// H5 的底栏是 fixed 的，抽屉被 uni-page 层叠上下文关住压不住它，开抽屉时把底栏淡出
// #ifdef H5
watch(sheet,v=>document.body.classList.toggle('yj-sheet-open',v))
// #endif
// 从详情页/信息盒子带条件进来：清空旧条件再按新条件搜
onShow(()=>{const preset=takeTabQuery();if(!preset)return;q.value='';categoryIds.value=[];keywordIds.value=[];labelIds.value=[];typeCodes.value=[];expired.value=false;echoOnly.value=false;void init(preset)})
onReachBottom(()=>{if(cursor.value)void search(false)})
</script>
<template>
<view class="page search-page tab-page">
  <view class="page-aurora"/>
  <PageNav title="搜索" :back="true"/>
  <view class="search-row">
    <view class="sbox" :class="{focused}">
      <view class="sicon"/>
      <input v-model="q" :placeholder="hot[0]?.name||'输入关键字或标签以搜索'" maxlength="100" confirm-type="search" @focus="focused=true" @blur="focused=false" @input="onInput" @confirm="search(true,true)"/>
      <view v-if="q" class="sclear" hover-class="yj-hover" @click="clearQuery"><text>×</text></view>
      <view class="sfilter" :class="{on:filterCount>0}" hover-class="yj-hover" @click="openFilter">
        <view class="slider s1"/><view class="slider s2"/>
      </view>
    </view>
  </view>
  <view v-if="filterCount" class="active-filters">
    <view v-for="f in filterChips" :key="f.key" class="afchip" hover-class="yj-hover" @click="dropFilter(f)"><text class="afchip-t">{{ f.label }}</text><text class="afchip-x">×</text></view>
    <view class="afchip afreset" hover-class="yj-hover" @click="resetFilters">清空</view>
  </view>
  <PageState :busy="busy" :error="error" @retry="search()"/>
  <template v-if="!searched">
    <view class="hist-head"><text class="hist-title">历史搜索</text><text class="hist-clear" @click="clear">清空</text></view>
    <view class="chipflow">
      <view v-for="h in history" :key="h.query" class="chip yj-glass yj-press" :class="{removing:removingKey===h.query}" hover-class="yj-hover" @click="q=h.query;search()">
        <text class="chip-text">{{ h.query }}</text>
        <text class="chip-x" :aria-label="'删除搜索记录'+h.query" @click.stop="removeHistory(h.query)">×</text>
      </view>
    </view>
    <text v-if="!history.length" class="emptyline">暂无搜索历史</text>

    <view class="section-head"><text class="section-title">热搜榜</text></view>
    <text class="hot-cap">近七天用户搜索过的公开关键词</text>
    <view class="hot-list">
      <view v-for="(h,i) in hot" :key="h.id" class="hot-row" hover-class="yj-hover" @click="q=h.name;search()">
        <text class="hot-no" :class="{top:i<3}">{{ i+1 }}</text>
        <text class="hot-name">{{ h.name }}</text>
      </view>
    </view>
    <view v-if="!hot.length" class="rec-block">
      <view class="chipflow">
        <view v-for="k in keywords.slice(0,12)" :key="k.id" class="kw-pill yj-glass yj-press" hover-class="yj-hover" @click="q=k.name;search()">{{ k.name }}</view>
      </view>
    </view>

    <view class="section-head"><text class="section-title">热门资讯</text></view>
    <view class="pop-list">
      <view v-for="(a,i) in popular" :key="a.id" class="pop-item" hover-class="yj-hover" @click="navigate('article','?id='+a.id)">
        <view class="pop-cover"><image class="pop-cover-img" :src="popCover(a,i)" mode="aspectFill"/></view>
        <view class="pop-copy"><text class="pop-title">{{ a.title }}</text><view v-if="a.sourceName||a.publishedAt" class="pop-meta"><text v-if="a.sourceName">{{ a.sourceName }}</text><text v-if="a.publishedAt">{{ String(a.publishedAt).slice(0,10) }}</text></view></view>
      </view>
    </view>
  </template>
  <template v-else>
    <view class="result-head"><text>找到 {{ total }} 条资讯</text><text class="muted">{{ expired?'含过期历史':'当前有效与未开始' }}</text></view>
    <DiscoveryCards :items="items"/>
    <view v-if="!items.length&&!busy" class="empty">暂无匹配内容<button role="button" class="quiet" hover-class="yj-hover" @click="missing">缺失信息申报</button></view>
    <button role="button" v-if="cursor" :disabled="busy" class="secondary" hover-class="yj-hover" @click="search(false)">加载更多</button>
    <view v-if="items.length" class="list-fade"/>
  </template>

  <view v-if="sheet" class="sheet-mask" hover-class="yj-hover" @click="sheet=false">
    <view class="filter-sheet" hover-class="yj-hover" @click.stop>
      <view class="row"><text class="section-title">筛选资讯</text><button role="button" class="quiet" hover-class="yj-hover" @click="sheet=false">关闭</button></view>
      <text class="field-label">信息类型</text>
      <view class="actions">
        <button role="button" class="fchip yj-glass yj-press" v-for="t in types" :key="t.code" :class="{on:t.code===''?!draft.typeCodes.length:draft.typeCodes.includes(t.code)}" hover-class="yj-hover" @click="t.code===''?draft.typeCodes=[]:toggleContentType(t.code)">{{ t.name }}</button>
      </view>
      <text class="field-label">板块（可多选）</text>
      <view class="tag-grid">
        <button role="button" class="fchip yj-glass yj-press" :class="{on:!draft.categoryIds.length}" hover-class="yj-hover" @click="draft.categoryIds=[];draft.labelIds=[]">全部板块</button>
        <button role="button" class="fchip yj-glass yj-press" v-for="c in categories" :key="c.id" :class="{on:draft.categoryIds.includes(c.id)}" hover-class="yj-hover" @click="toggleBoard(c.id)">{{ c.name }}</button>
      </view>
      <text class="field-label">标签（可多选）</text>
      <text v-if="!draft.categoryIds.length" class="muted">先选择板块，再选择该板块下的标签</text>
      <view v-for="board in categories.filter(c=>draft.categoryIds.includes(c.id))" :key="board.id">
        <text class="field-label">{{ board.name }}</text>
        <view class="tag-grid">
          <button role="button" class="fchip yj-glass yj-press" v-for="l in visibleLabels.filter(l=>l.categoryId===board.id)" :key="l.id" :class="{on:draft.labelIds.includes(l.id)}" hover-class="yj-hover" @click="toggle(l.id)">{{ l.name }}</button>
        </view>
      </view>
      <text class="field-label">关键词（可多选）</text>
      <view class="tag-grid">
        <button role="button" class="fchip yj-glass yj-press" v-for="k in keywords" :key="k.id" :class="{on:draft.keywordIds.includes(k.id)}" hover-class="yj-hover" @click="toggleKeyword(k.id)">{{ k.name }}</button>
      </view>
      <view class="row"><text>启用过期历史信息搜索</text><switch :checked="draft.expired" @change="draft.expired=($event as any).detail.value"/></view>
      <view class="filter-actions"><button role="button" class="quiet" hover-class="yj-hover" @click="draft={typeCodes:[],categoryIds:[],labelIds:[],keywordIds:[],expired:false}">重置筛选</button><button role="button" :disabled="busy" hover-class="yj-hover" @click="apply">应用筛选</button></view>
    </view>
  </view>
  <LoginSheet/>
</view>
</template>
