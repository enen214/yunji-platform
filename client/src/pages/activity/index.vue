<script setup lang="ts">
import {onHide as onPageHide} from "@dcloudio/uni-app"
import {abandonLogin as cancelPageLogin} from "../../features/auth"
onPageHide(cancelPageLogin)

import ActivityDownloads from '../../components/ActivityDownloads.vue'
import LoginSheet from "../../components/LoginSheet.vue"

import { ref } from 'vue';import { onLoad,onShow } from '@dcloudio/uni-app';import { request,navigate,token,type Row } from '../../api/platform'
const id=ref(''),activity=ref<Row|null>(null),fields=ref<Row[]>([]),answers=ref<Row>({}),registration=ref<Row|null>(null),error=ref(''),busy=ref(false)
async function load(){activity.value=await request('GET','/api/v1/activities/'+id.value);fields.value=JSON.parse(activity.value!.form.schemaJson).fields;if(token()){const regs=await request<Row[]>('GET','/api/v1/me/registrations');registration.value=regs.find(r=>r.activityId===id.value)||null}}
async function submit(){if(!token()){navigate('login');return}busy.value=true;error.value='';try{registration.value=await request('POST','/api/v1/me/activities/'+id.value+'/registration',{formVersionId:activity.value?.form.id,answers:answers.value});await load()}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
async function cancel(){busy.value=true;try{await request('DELETE','/api/v1/me/activities/'+id.value+'/registration');await load()}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
onLoad(q=>{id.value=String(q?.id||'')})
onShow(async()=>{if(!id.value)return;error.value='';try{await load()}catch(e){error.value=(e as Error).message}})
</script>
<template><view class="page"><text class="error">{{ error }}</text><template v-if="activity"><view class="hero"><text class="title">{{ activity.title }}</text><text class="subtitle">{{ activity.eventStart }} — {{ activity.eventEnd }}</text><text class="badge">剩余 {{ activity.remaining }} 名额</text></view><ActivityDownloads :activity-id="id"/><view v-if="registration&&registration.status!=='CANCELLED'" class="card"><text class="card-title">{{ registration.status==='CONFIRMED'?'报名已确认':'候补中' }}</text><text class="muted">{{ registration.status==='WAITLISTED'?'有人取消后会按报名顺序推进候补。':'请按活动安排参加。' }}</text><button role="button" class="danger" style="margin-top:18px" :disabled="busy" @click="cancel">取消报名</button></view><view v-else class="card"><text class="card-title">填写报名信息</text><view v-for="field in fields" :key="field.key" class="field"><text class="field-label">{{ field.label }} {{ field.required?'*':'' }}</text><picker v-if="field.type==='CHOICE'" :range="field.choices" @change="answers[field.key]=field.choices[Number($event.detail.value)]"><view class="picker">{{ answers[field.key]||'请选择' }}</view></picker><input v-else v-model="answers[field.key]" :type="field.type==='NUMBER'?'digit':'text'" :maxlength="field.maxLength" /></view><button role="button" :disabled="busy" @click="submit">{{ activity.remaining>0?'提交报名':activity.waitlistEnabled?'加入候补':'名额已满' }}</button></view><button role="button" v-if="activity.articleId" class="quiet" @click="navigate('article','?id='+activity.articleId)">查看活动通知</button></template><LoginSheet/></view></template>
