<script setup lang="ts">
import {onHide as onPageHide} from "@dcloudio/uni-app"
import {abandonLogin as cancelPageLogin} from "../../features/auth"
onPageHide(cancelPageLogin)

import LoginSheet from "../../components/LoginSheet.vue"

import {ref,computed} from 'vue'
import {onShow} from '@dcloudio/uni-app'
import {request,navigate,token,type Row} from '../../api/platform'
import {followTree} from '../../features/follows'
import {followCancellation} from '../../features/followCancellation'
import PageState from '../../components/PageState.vue'
const rows=ref<Row[]>([]),error=ref(''),signedIn=ref(false),loading=ref(false),tab=ref('keywords'),expanded=ref<string[]>([])
const boards=computed(()=>followTree(rows.value,tab.value))
async function load(){loading.value=true;error.value='';signedIn.value=!!token();try{rows.value=signedIn.value?await request('GET','/api/v1/me/follows'):[];rows.value=[...rows.value,...retained.value.filter(r=>!rows.value.some(x=>x.id===r.id))]}catch(e){error.value=(e as Error).message}finally{loading.value=false}}
function toggle(type:string){expanded.value=expanded.value.includes(type)?expanded.value.filter(t=>t!==type):[...expanded.value,type]}
const {cancelled,retained,busy,remove,reset}=followCancellation(load,error)

function open(r:Row){navigate(r.targetType==='ARTICLE'?'article':r.targetType==='KEYWORD'?'keyword-updates':r.targetType==='LABEL'?'label':'board','?id='+r.targetId)}
onShow(()=>{reset();void load()})
</script><template><view class="page"><view class="hero"><text class="title">管理关注</text></view><button role="button" v-if="!signedIn" @click="navigate('login')">登录</button><template v-else><view class="segmented"><button role="button" :class="{active:tab==='keywords'}" @click="tab='keywords'">关键词</button><button role="button" :class="{active:tab==='info'}" @click="tab='info'">信息</button></view><PageState :busy="loading" :error="error" :empty="!boards.length&&!loading" empty-text="暂无此类关注" @retry="load"/>
<view v-for="board in boards" :key="board.key" class="card follow-board"><view class="group-header row" role="button" :aria-expanded="expanded.includes('board:'+board.key)" @click="toggle('board:'+board.key)"><text class="card-title">{{ board.name }}</text><text>{{ expanded.includes('board:'+board.key)?'⌃':'⌄' }}</text></view><template v-if="expanded.includes('board:'+board.key)"><view v-for="g in board.labels" :key="g.key" class="following-group follow-label"><view class="group-header" role="button" :aria-expanded="expanded.includes(g.key)" @click="toggle(g.key)"><view class="group-copy"><text class="card-title">{{ g.name }}</text></view><text class="group-count">{{ g.items.length }}</text><text>{{ expanded.includes(g.key)?'⌃':'⌄' }}</text></view><view v-if="expanded.includes(g.key)" class="group-preview"><view v-for="f in g.items.slice(0,4)" :key="f.id" class="list-item row"><text @click="open(f)">{{ f.name||'已归档对象' }}</text><button role="button" class="quiet compact-button" :disabled="busy" @click="remove(f)">{{ cancelled.includes(f.id)?'关注':'取消关注' }}</button></view><text v-if="g.items.length>4" class="group-all" @click="navigate('follow-items','?kind='+tab+'&group='+encodeURIComponent(g.key))">查看全部 {{ g.items.length }} 条 ›</text></view></view></template></view><text v-if="rows.length>=200" class="muted">当前接口最多返回最近 200 项关注；超过此规模请由接手团队扩展服务端分页。</text></template><LoginSheet/></view></template>
