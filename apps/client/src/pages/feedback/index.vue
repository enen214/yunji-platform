<script setup lang="ts">
import {onHide as onPageHide} from "@dcloudio/uni-app"
import {abandonLogin as cancelPageLogin} from "../../features/auth"
onPageHide(cancelPageLogin)

import LoginSheet from "../../components/LoginSheet.vue"

import {ref} from 'vue';import {onShow} from '@dcloudio/uni-app';import {request,navigate,token} from '../../api/platform'
const feedbackType=ref('OTHER'),contact=ref(''),types=[['CONTENT','资讯内容'],['REMINDER','提醒推送'],['SERVICE','报名申报'],['OTHER','其他']]
const profile=ref(false),feedback=ref(''),error=ref(''),busy=ref(false),platform=uni
onShow(()=>{profile.value=!!token();if(!profile.value)feedback.value=''})
async function act(fn:()=>Promise<void>){if(busy.value)return;busy.value=true;error.value='';try{await fn()}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
</script>
<template><view class="page"><text class="error">{{ error }}</text><button role="button" v-if="!profile" @click="navigate('login')">登录后反馈</button><template v-else><view class="card"><text class="card-title">平台反馈</text><text class="muted" style="margin-bottom:12px">告诉我们哪里可以做得更好。</text><textarea v-model="feedback" placeholder="描述你的建议或遇到的问题" maxlength="1800" /><view class="actions"><button role="button" v-for="t in types" :key="t[0]" :class="feedbackType===t[0]?'':'quiet'" :disabled="busy" @click="feedbackType=t[0]">{{ t[1] }}</button></view><text class="field-label">联系方式（选填）</text><input v-model="contact" placeholder="仅用于本次反馈沟通" maxlength="100"/><button role="button" :disabled="busy||!feedback.trim()"  style="margin-top:12px" @click="act(async()=>{await request('POST','/api/v1/me/feedback',{description:feedback,type:feedbackType,contact:contact});feedback='';contact='';platform.showToast({title:'反馈已提交'})})">提交反馈</button></view></template><LoginSheet/></view></template>
