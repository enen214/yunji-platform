<script setup lang="ts">
import {onMounted,ref} from 'vue'
import {request,authenticated,downloadLink,type Row} from '../api/platform'
const props=defineProps<{activityId:string}>(),rows=ref<Row[]>([]),history=ref<Record<string,Row[]>>({}),busy=ref(false),error=ref('')
async function load(){rows.value=await request('GET','/api/v1/activities/'+props.activityId+'/downloads')}
async function versions(id:string){try{history.value[id]=await request('GET','/api/v1/activities/'+props.activityId+'/downloads/'+id+'/versions')}catch(e){error.value=(e as Error).message}}
async function download(file:Row,version?:number){await authenticated(async()=>{if(busy.value)return;busy.value=true;error.value='';try{const link=await request<Row>('POST','/api/v1/activities/'+props.activityId+'/downloads/'+file.id+(version?'?version='+version:''));await downloadLink(link,file.name);await load()}catch(e){error.value=(e as Error).message}finally{busy.value=false}})}
onMounted(()=>load().catch(e=>error.value=(e as Error).message))
</script>
<template><view v-if="rows.length||error" class="card"><text class="card-title">活动下载</text><text role="alert" class="error">{{ error }}</text><view v-for="f in rows" :key="f.id" class="list-item"><text class="card-title">{{ f.name }}</text><text class="muted">{{ f.description }}</text><text class="small muted">当前 v{{ f.currentVersion }} · 下载 {{ f.downloadCount }} 次</text><view class="actions"><button role="button" :disabled="busy" @click="download(f)">下载当前版本</button><button role="button" v-if="f.showHistory" class="quiet" @click="versions(f.id)">历史版本</button></view><view v-for="v in history[f.id]||[]" :key="v.versionNo" class="row"><text>v{{ v.versionNo }} · {{ v.note }}</text><button role="button" class="quiet" :disabled="busy" @click="download(f,v.versionNo)">下载</button></view></view></view></template>
