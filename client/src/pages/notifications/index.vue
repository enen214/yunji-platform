<script setup lang="ts">
import {ref} from 'vue'
import {onShow,onHide} from '@dcloudio/uni-app'
import {request,navigate,token,type Row} from '../../api/platform'
import {abandonLogin} from '../../features/auth'
import {contentLabel} from '../../features/catalog'
import LoginSheet from '../../components/LoginSheet.vue'
import PageState from '../../components/PageState.vue'
const tab=ref('replacement')
const replacement=ref<Row[]>([]),instant=ref<Row[]>([]),keywords=ref<Row[]>([])
const counts=ref<Record<string,number>>({replacement:0,instant:0,keyword:0})
const error=ref(''),loading=ref(false),busy=ref(false),signedIn=ref(false)
async function load(){
  signedIn.value=!!token();if(!signedIn.value){replacement.value=[];instant.value=[];keywords.value=[];return}
  loading.value=true;error.value=''
  try{
    const r=await request('GET','/api/v1/me/reminders/changes?page=1&size=100&eventType=REPLACED')
    const i=await request('GET','/api/v1/me/reminders/rules?page=1&size=100&mode=INSTANT')
    const kw=await request('GET','/api/v1/me/reminders/keywords')
    replacement.value=r.items;instant.value=i.items
    counts.value.replacement=r.total;counts.value.instant=i.total
    keywords.value=(kw as Row[]).filter((k:Row)=>k.enabled)
    counts.value.keyword=keywords.value.length
  }catch(e){error.value=(e as Error).message}
  finally{loading.value=false}
}
function switchTab(v:string){if(loading.value||busy.value)return;tab.value=v}
async function dismissChange(c:Row){if(busy.value)return;busy.value=true;error.value='';try{await request('DELETE','/api/v1/me/reminders/changes/'+c.id);await load()}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
async function disable(rule:Row){if(busy.value)return;busy.value=true;error.value='';try{await request('PUT','/api/v1/me/reminders/rules/'+rule.id,{targetType:rule.targetType,targetId:rule.targetId,mode:rule.mode,enabled:false,expectedVersion:rule.lockVersion});await load()}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
async function disableKeyword(k:Row){if(busy.value||!k.trackingId)return;busy.value=true;error.value='';try{await request('PUT','/api/v1/me/reminders/rules/'+k.trackingId,{targetType:'KEYWORD',targetId:k.id,mode:'DAILY',enabled:false,expectedVersion:0});await load()}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
onShow(load);onHide(abandonLogin)
</script><template><view class="page"><view class="hero"><text class="title">强提醒</text></view><view v-if="!signedIn" class="card"><text>登录后管理强提醒</text><button role="button" @click="navigate('login')">登录</button></view><template v-else><view class="card notice-card"><text class="card-title">强提醒怎么用？</text><text class="muted">强提醒分三类：更替强提醒（你关注的信息换新时，每日汇总走企业微信）、关键词强提醒（你单独开启的关键词有新信息时提醒）、及时提醒（订阅消息，触发时立刻推送）。除你自己设置的内容外，强提醒不会额外打扰你。</text></view><view class="segmented"><button role="button" :class="{active:tab==='replacement'}" @click="switchTab('replacement')">更替强提醒 {{ counts.replacement }}</button><button role="button" :class="{active:tab==='instant'}" @click="switchTab('instant')">及时提醒 {{ counts.instant }}</button><button role="button" :class="{active:tab==='keyword'}" @click="switchTab('keyword')">关键词强提醒 {{ counts.keyword }}</button></view><template v-if="tab==='replacement'"><text v-if="replacement.length" class="stat-bar">更替强提醒 {{ replacement.length }} 条</text><PageState :busy="loading" :error="error" :empty="!replacement.length&&!loading" empty-text="暂无更替强提醒" @retry="load"/><view v-for="c in replacement" :key="c.id" class="card"><view class="row"><view class="tags"><text class="badge tag-warn">信息更替</text></view><text class="muted small">{{ String(c.createdAt).slice(0,10) }}</text></view><text class="card-title" @click="navigate('article','?id='+c.articleId)">{{ c.title }}</text><text class="warn-text">你关注的信息已有新版本，点击查看新资讯</text><view class="actions"><button role="button" class="danger" :disabled="busy" @click="dismissChange(c)">忽略</button></view></view></template><template v-else-if="tab==='instant'"><text v-if="instant.length" class="stat-bar">及时提醒累计 {{ instant.length }} 条</text><PageState :busy="loading" :error="error" :empty="!instant.length&&!loading" empty-text="暂无及时提醒" @retry="load"/><view v-for="rule in instant" :key="rule.id" class="card"><view class="row"><view class="tags"><text class="badge tag-warn">{{ contentLabel(rule.contentType) }}</text><text class="badge tag-info">{{ rule.available>0?'已授权待触发':'待重新授权' }}</text></view><text class="muted small">订阅消息 · 触发即刻推送</text></view><text class="card-title" @click="navigate('article','?id='+rule.targetId)">{{ rule.title }}</text><text class="warn-text">{{ rule.available>0?'触发时通过小程序订阅消息立即推送':'已按一次性订阅推送过，再推送需要重新授权' }}</text><view class="actions"><button role="button" class="danger" :disabled="busy" @click="disable(rule)">撤销</button></view></view></template><template v-else><text v-if="keywords.length" class="section-title">正在跟踪 {{ keywords.length }} 个关键词</text><PageState :busy="loading" :error="error" :empty="!keywords.length&&!loading" empty-text="暂无关键词强提醒" @retry="load"/><view v-for="k in keywords" :key="k.id" class="card"><view class="row"><view class="tags"><text class="badge tag-keyword">{{ k.name }}</text><text class="badge">关键词关注</text></view><text class="badge tag-warn">{{ k.newCount }} 条新信息</text></view><view v-if="k.latestTitle" class="latest"><text class="muted">{{ k.latestTitle }}</text><text v-if="k.latestDate" class="muted small">{{ String(k.latestDate).slice(0,10) }}</text></view><text v-else class="muted">暂无匹配资讯</text><view class="row"><text class="section-link" @click="navigate('keyword-updates','?keywordId='+k.id)">查看全部 ›</text><button role="button" class="danger" :disabled="busy||!k.trackingId" @click="disableKeyword(k)">取消关注强提醒</button></view></view></template><view class="bottom-actions"><button role="button" @click="navigate('notification-add')">＋ 新增强提醒</button><button role="button" class="quiet" @click="navigate('notification-settings')">强提醒设置</button></view></template><LoginSheet/></view></template>
