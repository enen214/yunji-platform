<script setup lang="ts">
import {onHide as onPageHide} from "@dcloudio/uni-app"
import {abandonLogin as cancelPageLogin} from "../../features/auth"
onPageHide(cancelPageLogin)

import LoginSheet from "../../components/LoginSheet.vue"

import {ref} from 'vue'
import {onLoad,onReachBottom} from '@dcloudio/uni-app'
import {request,navigate,authenticated,type Row} from '../../api/platform'
import type {ArticlePage,ArticleCard} from '../../types/api'
import DiscoveryCards from '../../components/DiscoveryCards.vue'
import PageState from '../../components/PageState.vue'
const id=ref(''),name=ref('关键词新消息'),items=ref<ArticleCard[]>([]),cursor=ref(''),snapshot=ref(''),total=ref(0),busy=ref(false),error=ref(''),loaded=ref(false)
async function load(reset=true){if(busy.value)return;busy.value=true;error.value='';try{const p=await request<ArticlePage>('GET','/api/v1/articles/search?keywordId='+id.value+(reset?'':'&cursor='+encodeURIComponent(cursor.value)+'&snapshotAt='+encodeURIComponent(snapshot.value)));items.value=reset?p.items:[...items.value,...p.items];cursor.value=p.nextCursor||'';snapshot.value=p.snapshotAt;total.value=p.total;loaded.value=true}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
async function remind(){await authenticated(async()=>{try{const p=await request('GET','/api/v1/me/reminders/rules?size=1&targetType=KEYWORD&targetId='+id.value);navigate('notification-edit',p.items.length?'?id='+p.items[0].id:'?targetType=KEYWORD&targetId='+id.value)}catch(e){error.value=(e as Error).message}})}
onLoad(async o=>{id.value=String(o?.id||'');try{const all=await request<Row[]>('GET','/api/v1/catalog/keywords');name.value=all.find(k=>k.id===id.value)?.name||name.value}catch{}await load()})
onReachBottom(()=>{if(cursor.value)void load(false)})
</script><template><view class="page"><view class="hero"><text class="title">{{ name }}</text><text class="subtitle">关键词关联的公开资讯 · {{ total }} 条</text></view><button role="button" class="secondary" @click="remind">设置关键词强提醒</button><PageState :busy="busy" :error="error" :empty="loaded&&!items.length" empty-text="该关键词暂无公开资讯" @retry="load()"/><DiscoveryCards :items="items"/><button role="button" v-if="cursor" class="quiet" :disabled="busy" @click="load(false)">加载更多</button><LoginSheet/></view></template>

