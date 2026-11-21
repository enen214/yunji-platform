<script setup lang="ts">
import {ref} from 'vue'
import {onShow,onHide} from '@dcloudio/uni-app'
import {abandonLogin as cancelLogin} from '../../features/auth'
import {request,navigate,token,authenticated,type Row} from '../../api/platform'
import PageState from '../../components/PageState.vue'
import LoginSheet from '../../components/LoginSheet.vue'
const boards=ref<Row[]>([]),labels=ref<Row[]>([]),follows=ref<Row[]>([]),expanded=ref<string[]>([]),error=ref(''),busy=ref(false),loading=ref(false)
function enabled(type:string,id:string){return follows.value.some(f=>f.targetType===type&&f.targetId===id)}
function children(id:string){return labels.value.filter(l=>l.categoryId===id)}
function selected(id:string){return children(id).filter(l=>enabled('LABEL',l.id)).length}
function labelPath(l:Row){const parts=[l.name],seen=new Set([l.id]);let parent=labels.value.find(x=>x.id===l.parentId);while(parent&&!seen.has(parent.id)){parts.unshift(parent.name);seen.add(parent.id);parent=labels.value.find(x=>x.id===parent!.parentId)}return parts.join(' / ')}
function expand(id:string){expanded.value=expanded.value.includes(id)?expanded.value.filter(x=>x!==id):[...expanded.value,id]}
async function refresh(){follows.value=token()?await request('GET','/api/v1/me/follows'):[]}
async function change(type:string,id:string){await authenticated(async()=>{if(busy.value)return;busy.value=true;error.value='';try{await request('PUT','/api/v1/me/follows/'+type+'/'+id,{enabled:!enabled(type,id)});await refresh()}catch(e){error.value=(e as Error).message}finally{busy.value=false}})}
async function all(id:string){await authenticated(async()=>{if(busy.value)return;const group=children(id);if(group.length>100){error.value='此板块标签超过单次操作上限，请逐项选择';return}busy.value=true;error.value='';try{await request('PUT','/api/v1/me/follows/labels',{labelIds:group.map(l=>l.id),enabled:selected(id)!==group.length});await refresh()}catch(e){error.value=(e as Error).message}finally{busy.value=false}})}
async function load(){loading.value=true;error.value='';try{[boards.value,labels.value]=await Promise.all([request('GET','/api/v1/catalog/boards'),request('GET','/api/v1/catalog/labels')]);await refresh()}catch(e){error.value=(e as Error).message}finally{loading.value=false}}
onShow(load);onHide(cancelLogin)
</script><template><view class="page"><view class="hero"><text class="title">选择推送标签</text><text class="subtitle">共 {{ labels.length }} 个标签 · 勾选后该标签下的新信息进入信息盒子；勾父标签不会自动勾子标签</text></view><view class="reminder-help">勾选只决定信息是否进入盒子，不等于开启强提醒；强提醒在信息详情页单独设置。</view><PageState :busy="loading" :error="error" @retry="load"/><view v-for="b in boards" :key="b.id" class="card"><view class="group-header row" @click="expand(b.id)"><view><text class="card-title">{{ b.name }}</text><text class="muted">已选 {{ selected(b.id) }} / {{ children(b.id).length }} 个标签</text></view><text>{{ expanded.includes(b.id)?'⌃':'⌄' }}</text></view><view class="actions"><button role="button" v-if="children(b.id).length" class="quiet compact-button" :disabled="busy" @click="all(b.id)">{{ selected(b.id)===children(b.id).length?'取消全选':'全选本板块' }}</button></view><view v-if="expanded.includes(b.id)"><view v-for="l in children(b.id)" :key="l.id" class="list-item row" role="button" @click="change('LABEL',l.id)"><view class="check-main"><text class="check-box">{{ enabled('LABEL',l.id)?'☑':'☐' }}</text><text>{{ labelPath(l) }}</text></view><text class="muted">{{ enabled('LABEL',l.id)?'已推送':'推送' }}</text></view><text v-if="!children(b.id).length" class="muted">此板块暂无标签</text></view></view><LoginSheet/></view></template>
<style lang="scss" scoped>
.check-main{flex:1;min-width:0;display:flex;align-items:center;justify-content:flex-start;gap:8px}
.check-box{flex:none}
</style>
