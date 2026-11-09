<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { get, logout } from './api/client'
import {staffSession,canSection} from './permissions'
const route = useRoute(), router = useRouter(), session = staffSession, sessionError=ref('')
let sessionRequest=0
const groups=ref<{title:string;items:string[][]}[]>([])
function currentTitle(){return groups.value.flatMap(g=>g.items).find(i=>('/'+i[0]===route.path||i[0]===route.meta.section))?.[1]||'页面'}
function currentGroup(){return groups.value.find(g=>g.items.some(i=>('/'+i[0]===route.path||i[0]===route.meta.section)))?.title||'工作台'}
async function refreshSession(){const request=++sessionRequest;sessionError.value='';if(route.path==='/login')return;try{const value=await get('/api/v1/admin/session');if(request===sessionRequest){session.value=value;groups.value=await get('/api/v1/admin/navigation')}}catch(e){if(request!==sessionRequest)return;if((e as {status?:number}).status===401){session.value=null;await router.replace('/login')}else sessionError.value='暂时无法核验会话，请重试。'}}
watch(() => route.path, refreshSession, { immediate: true })
async function exit() { await logout(); session.value = null; router.push('/login') }
</script>
<template>
 <router-view v-if="route.path === '/login'" />
 <div v-else class="shell">
  <aside><div class="brand">云迹<span>运营工作台</span></div><nav><details v-for="group in groups.filter(g=>g.items.some(i=>canSection(i[0])))" :key="group.title" :open="currentGroup()===group.title"><summary>{{ group.title }}</summary><template v-for="item in group.items" :key="item[0]"><router-link v-if="canSection(item[0])" :to="'/' + item[0]">{{ item[1] }}</router-link></template></details></nav><button class="quiet" @click="exit">退出登录</button></aside>
  <main><header><nav class="breadcrumbs" aria-label="面包屑"><router-link to="/articles">云迹</router-link><span>/</span><span>{{ currentGroup() }}</span><span>/</span><router-link v-if="route.meta.mode" :to="'/'+String(route.meta.section)">{{ currentTitle() }}</router-link><span v-else aria-current="page">{{ currentTitle() }}</span><span v-if="route.meta.mode">/ {{ route.meta.mode==='new'?'新增':'编辑' }}</span></nav><span class="badge">{{ session?.superAdmin ? '管理员' : '工作人员' }} · {{ session?.userId }}</span></header><div v-if="sessionError" role="alert" class="error">{{ sessionError }}<button class="quiet" @click="refreshSession">重试会话核验</button></div><router-view v-if="session && canSection(String(route.meta.section||route.path.slice(1)))" :key="route.path" /><p v-else-if="session" role="alert" class="error">当前账号没有此页面的操作权限。</p></main>
 </div>
</template>
