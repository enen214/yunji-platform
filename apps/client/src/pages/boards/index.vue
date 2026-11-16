<script setup lang="ts">
import {onHide as onPageHide} from "@dcloudio/uni-app"
import {abandonLogin as cancelPageLogin} from "../../features/auth"
onPageHide(cancelPageLogin)

import LoginSheet from "../../components/LoginSheet.vue"

import {ref} from 'vue';import {onShow} from '@dcloudio/uni-app';import {request,navigate,type Row} from '../../api/platform'
import PageState from '../../components/PageState.vue'
const rows=ref<Row[]>([]),error=ref(''),busy=ref(false),icons=['▤','▣','♜','▱','◎','♧']
async function load(){busy.value=true;error.value='';try{rows.value=await request('GET','/api/v1/catalog/boards')}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
onShow(load)
</script><template><view class="page"><view class="board-intro">选择信息分类，浏览相关内容</view><PageState :busy="busy" :error="error" :empty="!rows.length&&!busy" empty-text="暂无已开放板块" @retry="load"/><view class="board-grid"><view v-for="(b,i) in rows" :key="b.id" class="board-tile" @click="navigate('board','?id='+b.id)"><view class="board-tile-top"><text class="board-icon">{{ icons[i%icons.length] }}</text></view><view class="board-tile-body"><text class="card-title">{{ b.name }}</text><text class="muted">{{ b.description||'浏览相关资讯与标签' }}</text><view class="meta-line"><text class="badge blue">{{ b.articleCount }} 条信息</text><text class="section-link">❯</text></view></view></view></view><text v-if="rows.length" class="end-note">- 已经到底了 -</text><LoginSheet/></view></template>
