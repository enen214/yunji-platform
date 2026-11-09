<script setup lang="ts">
import {ref,watch} from 'vue'
import {request,navigate,token,authenticated,type Row} from '../api/platform'
import type {ArticleCard,ArticlePage} from '../types/api'
import DiscoveryCards from './DiscoveryCards.vue'
import PageState from './PageState.vue'
import LoginSheet from './LoginSheet.vue'
const props=defineProps<{kind:'board'|'label';id:string}>()
const id=ref(''),name=ref(''),description=ref(''),labels=ref<Row[]>([]),keywords=ref<Row[]>([]),selected=ref<string[]>([]),following=ref(false)
const items=ref<ArticleCard[]>([]),cursor=ref(''),snapshot=ref(''),total=ref(0),busy=ref(false),saving=ref(false),error=ref('')
async function load(reset=true){if(busy.value)return;busy.value=true;error.value='';try{let query=(props.kind==='board'?'categoryId':'labelIds')+'='+id.value;if(selected.value.length)query+='&keywordIds='+selected.value.join(',');if(!reset)query+='&cursor='+encodeURIComponent(cursor.value)+'&snapshotAt='+encodeURIComponent(snapshot.value);const p=await request<ArticlePage>('GET','/api/v1/articles?hotOnly=true&'+query);items.value=reset?p.items:[...items.value,...p.items];cursor.value=p.nextCursor||'';snapshot.value=p.snapshotAt;total.value=p.total}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
async function syncFollow(){}
async function initialize(){try{const all=await request<Row[]>('GET','/api/v1/catalog/labels');labels.value=all.filter(l=>props.kind==='board'?l.categoryId===id.value:l.parentId===id.value);const catalog=props.kind==='board'?await request<Row[]>('GET','/api/v1/catalog/boards'):all;const entry=catalog.find(x=>x.id===id.value);name.value=entry?.name||'未找到目录';description.value=entry?.description||'';if(props.kind==='label'){const [c,d]=await Promise.all([request<Row[]>('GET','/api/v1/catalog/labels/'+id.value+'/curated-keywords'),request<Row[]>('GET','/api/v1/catalog/labels/'+id.value+'/keywords')]);keywords.value=[...new Map([...c,...d].map(k=>[k.id,k])).values()]}await syncFollow();await load()}catch(e){error.value=(e as Error).message}}
function select(key:string){if(busy.value)return;selected.value=selected.value.includes(key)?selected.value.filter(k=>k!==key):[...selected.value,key];void load()}
watch(()=>props.id,value=>{if(value){id.value=value;void initialize()}},{immediate:true})
defineExpose({refresh:syncFollow,more(){if(cursor.value)void load(false)}})
</script>
<template><view class="page"><view class="hero"><text class="title">{{ name }}</text><text class="subtitle">{{ description||'浏览目录下的资讯，点击标签查看关键词。' }}</text></view><view class="tag-grid"><button role="button" v-for="l in labels" :key="l.id" class="quiet" @click="navigate('label','?id='+l.id)">{{ l.name }}<text class="tag-count">{{ l.articleCount||0 }}</text></button></view>
<view v-if="kind==='label'" class="card"><view class="row"><text class="card-title">关键词</text><text v-if="selected.length" class="section-link" @click="!busy&&(selected=[],load())">清空勾选</text></view><text class="muted kw-hint">勾选关键词后，这里只显示与关键词相关的信息（可多选）</text><view class="kw-list"><view v-for="k in keywords" :key="k.id" class="kw-item" :class="{on:selected.includes(k.id)}" @click="select(k.id)"><view class="kw-check" :class="{on:selected.includes(k.id)}"><text v-if="selected.includes(k.id)" class="kw-check-mark">✓</text></view><text class="kw-name">{{ k.name }}</text></view></view><text v-if="!keywords.length" class="muted">此标签暂无公开关键词</text><view class="kw-search-bar" @click="navigate('keywords','?labelId='+id)"><view class="search-symbol"></view><text class="kw-search-text">全部关键词 · 共 {{ keywords.length }} 个</text><text class="kw-search-link">搜索 / 关注 ›</text></view></view>
<view class="section-head"><text class="section-title">相关资讯</text><text class="muted">共 {{ total }} 条</text></view><PageState :busy="busy" :error="error" :empty="!items.length&&!busy" empty-text="没有匹配的信息" @retry="initialize"/><DiscoveryCards :items="items"/><button role="button" v-if="cursor" class="secondary" :disabled="busy" @click="load(false)">加载更多</button><LoginSheet/></view></template>
<style scoped>
.kw-hint{margin:6px 0 10px}
.kw-list{display:flex;flex-wrap:wrap;gap:8px}
.kw-item{display:flex;align-items:center;gap:6px;padding:6px 10px;border:1px solid #dcdfe6;border-radius:999px;background:#fff}
.kw-item.on{background:#F2F9F4;border-color:#3EB26B}
.kw-check{width:18px;height:18px;border-radius:50%;border:1.5px solid #dcdfe6;display:flex;align-items:center;justify-content:center;flex-shrink:0}
.kw-check.on{background:#3EB26B;border-color:#3EB26B}
.kw-check-mark{font-size:11px;color:#fff;line-height:1}
.kw-name{font-size:13px;color:#1a1a1a}
.kw-search-bar{display:flex;align-items:center;gap:8px;margin-top:10px;padding:10px 12px;background:#eaf0eb;border-radius:8px}
.kw-search-text{flex:1;color:#5a7166;font-size:13px}
.kw-search-link{color:#3EB26B;font-size:13px;font-weight:600}
.tag-count{margin-left:6px;color:#8eb5a0;font-size:12px}
</style>
