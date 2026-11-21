<script setup lang="ts">
import {onHide as onPageHide} from "@dcloudio/uni-app"
import {abandonLogin as cancelPageLogin} from "../../features/auth"
onPageHide(cancelPageLogin)

import LoginSheet from "../../components/LoginSheet.vue"

import {ref,computed} from 'vue'
import {onLoad,onShow,onUnload} from '@dcloudio/uni-app'
import {request,navigate,token,authenticated,type Row} from '../../api/platform'
import PageState from '../../components/PageState.vue'
const rows=ref<Row[]>([]),follows=ref<Row[]>([]),q=ref(''),label=ref(''),error=ref(''),busy=ref(false),saving=ref(false)
const visible=computed(()=>rows.value.filter(k=>String(k.name).toLowerCase().includes(q.value.trim().toLowerCase())))
let timer:ReturnType<typeof setTimeout>|undefined,serial=0
function enabled(id:string){return follows.value.some(f=>f.targetType==='KEYWORD'&&f.targetId===id)}
async function refreshFollows(){follows.value=token()?await request('GET','/api/v1/me/follows'):[]}
async function search(){const current=++serial;busy.value=true;error.value='';try{let result:Row[];if(label.value){const [c,d]=await Promise.all([request<Row[]>('GET','/api/v1/catalog/labels/'+label.value+'/curated-keywords'),request<Row[]>('GET','/api/v1/catalog/labels/'+label.value+'/keywords')]);result=[...new Map([...c,...d].map(k=>[k.id,k])).values()]}else result=await request('GET','/api/v1/catalog/keywords?q='+encodeURIComponent(q.value.trim()));if(current===serial)rows.value=result}catch(e){if(current===serial)error.value=(e as Error).message}finally{if(current===serial)busy.value=false}}
function input(){if(label.value)return;if(timer)clearTimeout(timer);timer=setTimeout(()=>{void search()},250)}
async function follow(id:string){await authenticated(async()=>{if(saving.value)return;saving.value=true;error.value='';try{await refreshFollows();const next=!enabled(id);await request('PUT','/api/v1/me/follows/KEYWORD/'+id,{enabled:next});await refreshFollows();uni.showToast({title:next?'已关注':'已取消关注'})}catch(e){error.value=(e as Error).message}finally{saving.value=false}})}
onLoad(o=>{label.value=String(o?.labelId||'');void search()});onShow(()=>{void refreshFollows().catch(e=>{error.value=(e as Error).message})});onUnload(()=>{serial++;if(timer)clearTimeout(timer)})
</script><template><view class="page"><view class="hero"><text class="title">关键词</text><text class="subtitle">关注后在信息盒子查看更新；外部提醒单独设置。</text></view><view class="search-row"><input v-model="q" maxlength="100" placeholder="只搜索关键词" @input="input" @confirm="search"/><button role="button" :disabled="busy" @click="search">查找</button></view><PageState :busy="busy" :error="error" :empty="!visible.length&&!busy" empty-text="没有匹配的关键词" @retry="search"/><view v-for="k in visible" :key="k.id" class="card keyword-follow-row"><text @click="navigate('keyword-updates','?id='+k.id)">{{ k.name }}</text><view class="actions"><button role="button" :disabled="saving" :class="enabled(k.id)?'':'quiet'" @click="follow(k.id)">{{ enabled(k.id)?'已关注':'关注' }}</button><button role="button" class="quiet" @click="navigate('keyword-updates','?id='+k.id)">查看资讯</button></view></view><LoginSheet/></view></template>
