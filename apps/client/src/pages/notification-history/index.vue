<script setup lang="ts">
import {onHide as onPageHide} from "@dcloudio/uni-app"
import {abandonLogin as cancelPageLogin} from "../../features/auth"
onPageHide(cancelPageLogin)

import LoginSheet from "../../components/LoginSheet.vue"

import {ref} from 'vue';import {onShow} from '@dcloudio/uni-app';import {request,navigate,token,type Row} from '../../api/platform'
const rows=ref<Row[]>([]),page=ref(1),total=ref(0),error=ref(''),local=ref(false),signedIn=ref(false)
const names:Row={PENDING:'等待处理',WAITING_AUTH:'等待授权或绑定',SENDING:'正在提交',AWAITING_STAFF:'等待企微工作人员发送',PARTIAL:'部分执行',SENT:'已确认发送',ACCEPTED:'平台已接受',FAILED:'发送失败',UNKNOWN:'发送结果核实中',CANCELLED:'已取消'}
async function load(){signedIn.value=!!token();if(!signedIn.value)return;try{const d=await request('GET',`/api/v1/me/reminders/batches?page=${page.value}&size=20`);rows.value=d.items;total.value=d.total;local.value=d.localSimulation}catch(e){error.value=(e as Error).message}}
onShow(load)
</script><template><view class="page"><view class="hero"><text class="title">发送记录</text><text v-if="local" class="subtitle">本地模拟记录，不代表真实微信消息。</text></view><text class="error">{{ error }}</text><button role="button" v-if="!signedIn" @click="navigate('login')">登录后查看</button><view v-for="r in rows" :key="r.id" class="card"><text class="card-title">{{ r.channel==='WECom'?'日常更新':'即时提醒' }} · {{ r.itemCount }} 条</text><text>{{ names[r.state]||r.state }}</text><text class="muted">{{ r.createdAt }}</text></view><view v-if="signedIn&&!rows.length" class="empty">暂无发送记录</view><view v-if="total>20" class="actions"><button role="button" :disabled="page===1" @click="page--;load()">上一页</button><button role="button" :disabled="page*20>=total" @click="page++;load()">下一页</button></view><LoginSheet/></view></template>
