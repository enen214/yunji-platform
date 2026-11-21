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
<template><view class="page"><view class="hero"><text class="title">{{ name }}</text><text class="subtitle">{{ description||'浏览目录下的资讯，点击标签查看关键词。' }}</text></view><view class="tag-grid"><button role="button" v-for="l in labels" :key="l.id" class="quiet" @click="navigate('label','?id='+l.id)">{{ l.name }}</button></view>
<view v-if="kind==='label'" class="card"><view class="row"><text class="card-title">关键词筛选</text><text v-if="selected.length" class="section-link" @click="!busy&&(selected=[],load())">清空勾选</text></view><view class="tag-grid"><button role="button" v-for="k in keywords.slice(0,8)" :key="k.id" :disabled="busy" :class="selected.includes(k.id)?'':'quiet'" @click="select(k.id)">{{ k.name }}</button></view><text v-if="!keywords.length" class="muted">此标签暂无公开关键词</text><button role="button" class="quiet" @click="navigate('keywords','?labelId='+id)">全部关键词 · 共 {{ keywords.length }} 个 →</button><text class="muted small">多选关键词匹配任一；结果仍限定在当前标签内。</text></view>
<view class="section-head"><text class="section-title">相关资讯</text><text class="muted">共 {{ total }} 条</text></view><PageState :busy="busy" :error="error" :empty="!items.length&&!busy" empty-text="没有匹配的信息" @retry="initialize"/><DiscoveryCards :items="items"/><button role="button" v-if="cursor" class="secondary" :disabled="busy" @click="load(false)">加载更多</button><LoginSheet/></view></template>
