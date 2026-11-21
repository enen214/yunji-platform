<script setup lang="ts">
import {ref} from 'vue'
import {onLoad} from '@dcloudio/uni-app'
import {request,navigate,type Row} from '../../api/platform'
import {contentLabel} from '../../features/catalog'
import PageState from '../../components/PageState.vue'
const id=ref(''),topic=ref<Row|null>(null),busy=ref(false),error=ref('')
async function load(){busy.value=true;error.value='';try{topic.value=await request('GET','/api/v1/topics/'+id.value)}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
onLoad(q=>{id.value=String(q?.id||'');if(id.value)void load();else error.value='未指定活动专题，请从首页轮播或活动列表进入。'})
</script>
<template><view class="page topic-page"><PageState :busy="busy" :error="error" @retry="load"/><template v-if="topic"><image v-if="topic.imageUrl" class="topic-cover" :src="topic.imageUrl" mode="aspectFill"/><view class="hero"><text class="title">{{ topic.title }}</text><text class="subtitle">{{ topic.subtitle }}</text></view><view v-if="topic.description" class="card"><rich-text class="body" :nodes="topic.description"/></view><view class="section-head"><text class="section-title">相关资讯</text><text class="muted">{{ topic.items.length }} 条</text></view><view v-for="a in topic.items" :key="a.id" class="card" @click="navigate('article','?id='+a.id)"><text class="badge">{{ contentLabel(a.contentType) }}</text><text class="card-title">{{ a.title }}</text><text class="muted">{{ a.summary }}</text><text class="box-time">{{ a.publishedAt?.slice(0,10) }}</text></view><view v-if="!topic.items.length" class="empty">暂无已发布的关联资讯</view></template></view></template>
