<script setup lang="ts">
import {onMounted,ref} from 'vue'
import {get,request} from '../api/client'
const props=defineProps<{activityId:string}>(),emit=defineEmits<{saved:[]}>()
type Row=Record<string,any>
const files=ref<Row[]>([]),reviews=ref<Row[]>([]),selected=ref<string[]>([]),reviewIds=ref<string[]>([]),history=ref<string[]>([]),busy=ref(false),error=ref(''),version=ref(0)
async function load(){const [f,r,a,c]=await Promise.all([get('/api/v1/admin/file-library'),get('/api/v1/admin/review-campaigns'),get('/api/v1/admin/activities/'+props.activityId),get('/api/v1/admin/activities/'+props.activityId+'/functions')]);files.value=f;reviews.value=r;version.value=a.lockVersion;selected.value=c.files.map((x:Row)=>x.fileId);history.value=c.files.filter((x:Row)=>x.showHistory).map((x:Row)=>x.fileId);reviewIds.value=c.reviewIds}
async function save(){busy.value=true;error.value='';try{await request('PUT','/api/v1/admin/activities/'+props.activityId+'/functions',{files:selected.value.map((id,i)=>({fileId:id,showHistory:history.value.includes(id),sortOrder:i})),reviewIds:reviewIds.value,expectedVersion:version.value});await load();emit('saved')}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
onMounted(()=>load().catch(e=>error.value=(e as Error).message))
</script>
<template><div class="panel"><h3>活动功能引用</h3><p class="muted">下载按下列顺序展示。评审项目仅配置引用，当前不在 C 端开放申报入口。</p><label>引用文件<select multiple v-model="selected"><option v-for="f in files" :key="f.id" :value="f.id" :disabled="!f.enabled&&!selected.includes(f.id)">{{ f.name }}{{ f.enabled?'':'（已停用）' }}</option></select></label><div v-for="(id,i) in selected" :key="id" class="actions"><span>{{ files.find(f=>f.id===id)?.name }}</span><label><input type="checkbox" :value="id" v-model="history"/>开放历史版本</label><button type="button" class="quiet" :disabled="i===0" @click="[selected[i-1],selected[i]]=[selected[i],selected[i-1]]">上移</button></div><label>引用全局评审项目<select multiple v-model="reviewIds"><option v-for="r in reviews" :key="r.id" :value="r.id">{{ r.title }}</option></select></label><p role="alert" class="error">{{ error }}</p><button :disabled="busy" @click="save">保存功能引用</button></div></template>
