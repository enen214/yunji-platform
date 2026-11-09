<script setup lang="ts">
import {ref} from 'vue'
import {onLoad} from '@dcloudio/uni-app'
import {request,navigate,type Row} from '../../api/platform'
import PageState from '../../components/PageState.vue'
const id=ref(''),topic=ref<Row|null>(null),busy=ref(false),error=ref('')
async function load(){busy.value=true;error.value='';try{topic.value=await request('GET','/api/v1/topics/'+id.value)}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
onLoad(q=>{id.value=String(q?.id||'');if(id.value)void load();else error.value='未指定活动专题，请从首页轮播或活动列表进入。'})
</script>
<template><view class="page topic-page"><PageState :busy="busy" :error="error" @retry="load"/><template v-if="topic"><image v-if="topic.imageUrl" class="topic-cover" :src="topic.imageUrl" mode="aspectFill"/><view class="hero"><text class="title">{{ topic.title }}</text><text class="subtitle">{{ topic.subtitle }}</text></view><view v-if="topic.description" class="card"><rich-text class="body" :nodes="topic.description"/></view><view class="section-head"><text class="section-title">相关资讯</text><text class="muted">{{ topic.items.length }} 条</text></view><view v-for="a in topic.items" :key="a.id" class="card result-card" @click="navigate('article','?id='+a.id)"><view class="topic-thumb"><image v-if="a.coverUrl" :src="a.coverUrl" mode="aspectFill" class="topic-thumb-img"/><text v-else class="topic-thumb-ph">图</text></view><view class="result-copy"><text v-if="a.categoryName" class="badge tag-info">{{ a.categoryName }}</text><text class="card-title">{{ a.title }}</text></view></view><view v-if="!topic.items.length" class="empty">暂无已发布的关联资讯</view></template></view></template>
