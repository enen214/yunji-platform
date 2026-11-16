<script setup lang="ts">
import {onHide as onPageHide} from "@dcloudio/uni-app"
import {abandonLogin as cancelPageLogin} from "../../features/auth"
onPageHide(cancelPageLogin)

import LoginSheet from "../../components/LoginSheet.vue"

import { ref } from 'vue';import { onLoad } from '@dcloudio/uni-app';import { request,token,navigate,type Row } from '../../api/platform'
const page=ref<Row|null>(null),key=ref(''),error=ref('')
async function accept(){if(!token()){navigate('login');return}try{await request('POST','/api/v1/me/consents',{versionId:page.value?.versionId,type:key.value==='terms'?'TERMS':'PRIVACY'});uni.showToast({title:'同意记录已保存'})}catch(e){error.value=(e as Error).message}}
onLoad(async q=>{key.value=String(q?.key||'about');if(key.value==='about_us')key.value='about';if(key.value==='about'){page.value={title:'关于云迹',content:'云迹提供资讯浏览、分类关注、更新提醒、活动参与和资料查找服务。\n\n资讯与活动以发布方说明为准。日常提醒由企业微信工作人员执行，即时提醒需要微信一次性订阅授权。\n\n使用过程中遇到问题，可从“我的”进入“平台反馈”。'};return}try{page.value=await request('GET','/api/v1/pages/'+key.value)}catch(e){error.value=(e as Error).message}})
</script>
<template><view class="page"><text class="error">{{ error }}</text><template v-if="page"><view class="hero"><text class="title">{{ page.title }}</text><text v-if="key!=='about'" class="muted">版本 {{ page.versionNo }} · {{ page.publishedAt }}</text></view><view class="card body"><text style="white-space:pre-wrap">{{ page.content }}</text></view><button role="button" v-if="['terms','privacy'].includes(key)" @click="accept">同意此版本</button></template><LoginSheet/></view></template>
