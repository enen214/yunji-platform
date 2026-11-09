<script setup lang="ts">
import {onHide as onPageHide} from "@dcloudio/uni-app"
import {abandonLogin as cancelPageLogin} from "../../features/auth"
onPageHide(cancelPageLogin)

import LoginSheet from "../../components/LoginSheet.vue"

import {ref} from 'vue';import {onShow} from '@dcloudio/uni-app';import {request,navigate,token,type Row} from '../../api/platform'
const status=ref<Row|null>(null),error=ref('');onShow(async()=>{status.value=null;if(token())try{status.value=await request('GET','/api/v1/me/reminders/settings')}catch(e){error.value=(e as Error).message}})
</script><template><view class="page"><view class="hero"><text class="title">通道与授权</text></view><text class="error">{{ error }}</text><button role="button" v-if="!status" @click="navigate('login')">登录后查看</button><template v-else><view class="card"><text class="card-title">微信订阅消息</text><text class="muted">即时提醒使用一次性订阅授权。拒绝授权不会开启即时跟踪，发送失败或结果未知不会当作成功。</text></view><view class="card"><text class="card-title">企业微信</text><text class="muted">需要建立并验证客户联系关系。日常任务由系统创建，工作人员在企微执行。</text></view><view class="card"><text class="card-title">当前通道状态</text><text class="badge">{{ status.localSimulation?'本地模拟验收':status.realProviderReady?'平台已接入':'平台待接入' }}</text><view v-for="(p,i) in status.permissions" :key="i"><text>{{ p.wecomBound?'企微关系已配置':'尚未验证企微关系' }}</text><text>即时可用 {{ p.available }} · 预留 {{ p.reserved }} · 已使用 {{ p.consumed }}</text></view><text v-if="!status.permissions.length" class="muted">尚无可发送授权或绑定</text></view></template><LoginSheet/></view></template>
