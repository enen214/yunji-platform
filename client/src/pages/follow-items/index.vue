<script setup lang="ts">
import {onHide as onPageHide} from "@dcloudio/uni-app"
import {abandonLogin as cancelPageLogin} from "../../features/auth"
onPageHide(cancelPageLogin)

import LoginSheet from "../../components/LoginSheet.vue"

import {ref,computed} from 'vue'
import {onLoad,onShow} from '@dcloudio/uni-app'
import {request,navigate,token,type Row} from '../../api/platform'
import {followGroups} from '../../features/follows'
import {followCancellation} from '../../features/followCancellation'
import PageState from '../../components/PageState.vue'
const kind=ref('keywords'),group=ref(''),title=ref('更多关注'),q=ref(''),rows=ref<Row[]>([]),loading=ref(false),error=ref('')
const matched=computed(()=>followGroups(rows.value,kind.value).find(g=>g.key===group.value))
const visible=computed(()=>(matched.value?.items||[]).filter(r=>String(r.name||'').toLowerCase().includes(q.value.trim().toLowerCase())))
onLoad(o=>{kind.value=o?.kind==='info'?'info':'keywords';group.value=String(o?.group||'');title.value=kind.value==='keywords'?'更多关键词':'更多消息'})

async function load(){loading.value=true;error.value='';try{rows.value=token()?await request('GET','/api/v1/me/follows'):[];rows.value=[...rows.value,...retained.value.filter(r=>!rows.value.some(x=>x.id===r.id))]}catch(e){error.value=(e as Error).message}finally{loading.value=false}}
const {cancelled,retained,busy,remove,reset}=followCancellation(load,error)

function open(r:Row){navigate(r.targetType==='KEYWORD'?'keyword-updates':r.targetType==='ARTICLE'?'article':r.targetType==='LABEL'?'label':'board','?id='+r.targetId)}
onShow(()=>{reset();void load()})
</script><template><view class="page"><view class="hero"><text class="title">{{ matched?.name||title }}</text></view><view class="search-row"><input v-model="q" maxlength="100" placeholder="搜索已关注的名称"/><button role="button" class="quiet" @click="q=''">清空</button></view><PageState :busy="loading" :error="error" :empty="!visible.length&&!loading" empty-text="没有匹配的关注" @retry="load"/><view v-for="r in visible" :key="r.id" class="card row"><text @click="open(r)">{{ r.name||'已归档对象' }}</text><button role="button" class="quiet compact-button" :disabled="busy" @click="remove(r)">{{ cancelled.includes(r.id)?'撤销取消':'取消关注' }}</button></view><text v-if="rows.length>=200" class="muted">当前最多显示最近 200 项关注</text><LoginSheet/></view></template>

