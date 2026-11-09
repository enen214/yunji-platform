<script setup lang="ts">
import PageState from './PageState.vue'
import LoginSheet from "./LoginSheet.vue"

import {ref,onMounted} from 'vue';import {request,navigate,token,type Row} from '../api/platform'
const props=defineProps<{kind:'favorites'|'history'|'registrations'|'applications';title:string}>()
const rows=ref<Row[]>([]),error=ref(''),busy=ref(false),signedIn=ref(false)
const paths={favorites:'favorites',history:'history',registrations:'registrations',applications:'review-applications'}
async function load(){signedIn.value=!!token();if(!signedIn.value){rows.value=[];return}rows.value=await request('GET','/api/v1/me/'+paths[props.kind])}
async function act(fn:()=>Promise<void>){if(busy.value)return;busy.value=true;error.value='';try{await fn();await load()}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
function open(r:Row){navigate(props.kind==='registrations'?'activity':props.kind==='applications'?'review':'article','?id='+(r.activityId||r.campaignId||r.id))}
async function remove(r:Row){await act(async()=>{if(props.kind==='favorites')await request('PUT','/api/v1/me/favorites/'+r.id,{enabled:false});else await request('DELETE','/api/v1/me/history/'+r.id)})}
function clear(){uni.showModal({title:'清空'+props.title,content:'仅清除当前账号的记录，确认继续？',success:r=>{if(r.confirm)void act(async()=>{await request('DELETE','/api/v1/me/'+props.kind)})}})}
defineExpose({refresh:()=>act(load)})
onMounted(()=>act(load))
</script><template><view class="page"><view class="hero"><text class="title">{{ title }}</text><text v-if="signedIn" class="subtitle">共 {{ rows.length }} 条记录</text></view><PageState :busy="busy" :error="error" @retry="act(load)"/><button role="button" v-if="!signedIn" @click="navigate('login')">登录后查看</button><template v-else><view v-for="r in rows" :key="r.id" class="card"><view @click="open(r)"><text class="card-title">{{ r.title }}</text><text class="muted">{{ r.status||r.lastBrowsedAt||r.createdAt }}</text></view><button role="button" v-if="kind==='favorites'||kind==='history'" class="quiet" :disabled="busy" @click="remove(r)">{{ kind==='favorites'?'取消收藏':'删除记录' }}</button></view><view v-if="!rows.length" class="empty">这里还没有记录</view><button role="button" v-if="rows.length&&(kind==='favorites'||kind==='history')" class="danger" :disabled="busy" @click="clear">清空{{ title }}</button></template><LoginSheet/></view></template>
