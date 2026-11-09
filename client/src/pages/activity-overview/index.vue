<script setup lang="ts">
import {onHide as onPageHide} from "@dcloudio/uni-app"
import {abandonLogin as cancelPageLogin} from "../../features/auth"
onPageHide(cancelPageLogin)

import LoginSheet from "../../components/LoginSheet.vue"

import {ref,computed} from 'vue';import {onShow} from '@dcloudio/uni-app';import {request,navigate,token,authenticated,type Row} from '../../api/platform'
const columns=ref<Row[]>([])
const rows=ref<Row[]>([]),favorites=ref<Row[]>([]),tab=ref('all'),error=ref(''),busy=ref(false),visible=computed(()=>tab.value==='all'?rows.value:rows.value.filter(a=>favorites.value.some(f=>f.id===a.id)))
async function load(){const results=await Promise.all([request<Row[]>('GET','/api/v1/activities'),token()?request<Row[]>('GET','/api/v1/me/activity-favorites'):Promise.resolve([]),request('GET','/api/v1/topics')]);rows.value=results[0];favorites.value=results[1];columns.value=results[2]}
async function favorite(id:string){await authenticated(async()=>{if(busy.value)return;busy.value=true;try{await request('PUT','/api/v1/me/activity-favorites/'+id,{enabled:!favorites.value.some(f=>f.id===id)});await load()}catch(e){error.value=(e as Error).message}finally{busy.value=false}})}
onShow(async()=>{try{await load()}catch(e){error.value=(e as Error).message}})
</script><template><view class="page"><view class="hero"><text class="title">活动总览</text><text class="subtitle">发现活动，收藏关注，查看详情后报名。</text></view><view class="secondary-services"><button role="button" class="quiet" @click="navigate('registrations')">我的报名</button></view><view v-if="columns.length" class="card"><text class="card-title">活动专题</text><view class="tag-grid"><button role="button" v-for="column in columns" :key="column.id" class="quiet" @click="navigate('activity-column','?id='+column.id)">{{ column.title }}</button></view></view><view class="segmented"><button role="button" :class="{active:tab==='all'}" @click="tab='all'">全部</button><button role="button" :class="{active:tab==='favorites'}" @click="tab='favorites'">收藏</button></view><text class="error">{{ error }}</text><view v-for="a in visible" :key="a.id" class="card"><view @click="navigate('activity','?id='+a.id)"><text class="card-title">{{ a.title }}</text></view><view class="actions"><button role="button" @click="navigate('activity','?id='+a.id)">查看详情 / 报名</button><button role="button" class="quiet" :disabled="busy" @click="favorite(a.id)">{{ favorites.some(f=>f.id===a.id)?'取消收藏':'收藏活动' }}</button></view></view><view v-if="!visible.length" class="empty">{{ tab==='favorites'?'暂无收藏活动':'暂无公开活动' }}</view><LoginSheet/></view></template>
