<script setup lang="ts">
import {ref,computed} from 'vue'
import {onShow,onHide} from '@dcloudio/uni-app'
import {abandonLogin as cancelLogin} from '../../features/auth'
import {request,navigate,token,authenticated,type Row} from '../../api/platform'
import PageState from '../../components/PageState.vue'
import LoginSheet from '../../components/LoginSheet.vue'
const boards=ref<Row[]>([]),labels=ref<Row[]>([]),follows=ref<Row[]>([]),expanded=ref<string[]>([]),error=ref(''),busy=ref(false),loading=ref(false)
const busyBoard=ref<string|null>(null)
const totalSelected=computed(()=>follows.value.filter(f=>f.targetType==='LABEL').length)
function enabled(type:string,id:string){return follows.value.some(f=>f.targetType===type&&f.targetId===id)}
function children(id:string){return labels.value.filter(l=>l.categoryId===id)}
function selected(id:string){return children(id).filter(l=>enabled('LABEL',l.id)).length}
function expand(id:string){expanded.value=expanded.value.includes(id)?expanded.value.filter(x=>x!==id):[...expanded.value,id]}
async function refresh(){follows.value=token()?await request('GET','/api/v1/me/follows'):[]}
async function change(type:string,id:string){await authenticated(async()=>{if(busy.value)return;busy.value=true;error.value='';try{await request('PUT','/api/v1/me/follows/'+type+'/'+id,{enabled:!enabled(type,id)});await refresh()}catch(e){error.value=(e as Error).message}finally{busy.value=false}})}
async function all(id:string){await authenticated(async()=>{if(busyBoard.value)return;const group=children(id);if(group.length>100){error.value='此板块标签超过单次操作上限，请逐项选择';return}busyBoard.value=id;error.value='';try{await request('PUT','/api/v1/me/follows/labels',{labelIds:group.map(l=>l.id),enabled:selected(id)!==group.length});await refresh()}catch(e){error.value=(e as Error).message}finally{busyBoard.value=null}})}
async function load(){loading.value=true;error.value='';try{[boards.value,labels.value]=await Promise.all([request('GET','/api/v1/catalog/boards'),request('GET','/api/v1/catalog/labels')]);await refresh()}catch(e){error.value=(e as Error).message}finally{loading.value=false}}
onShow(load);onHide(cancelLogin)
</script>
<template><view class="page"><view class="push-stat"><view class="push-left"><view class="push-bar"></view><text class="push-title">推送标签选择</text></view><text class="push-count">{{ totalSelected }}</text></view><text class="push-hint">共 {{ labels.length }} 个标签 · 关注父标签不会自动选择子标签</text><PageState :busy="loading" :error="error" @retry="load"/><view v-for="b in boards" :key="b.id" class="push-card"><view class="push-head" @click="expand(b.id)"><view class="push-head-copy"><text class="push-head-name">{{ b.name }}</text><text class="push-head-sub">{{ children(b.id).length }} 个标签</text></view><button v-if="children(b.id).length" role="button" class="push-all" :disabled="busyBoard===b.id" @click.stop="all(b.id)">{{ selected(b.id)===children(b.id).length?'一键取消':'一键关注' }}</button><text class="push-prog">{{ selected(b.id) }}/{{ children(b.id).length }}</text><text class="push-arrow">{{ expanded.includes(b.id)?'⌃':'⌄' }}</text></view><view v-if="expanded.includes(b.id)" class="push-tags"><view v-for="l in children(b.id)" :key="l.id" class="push-tag" :class="{on:enabled('LABEL',l.id)}" @click="change('LABEL',l.id)"><view class="push-tag-copy"><text class="push-tag-name">{{ l.name }}</text><text class="push-tag-hint">{{ enabled('LABEL',l.id)?'点击取消关注':'点击关注' }}</text></view><view class="push-check" :class="{on:enabled('LABEL',l.id)}"><text v-if="enabled('LABEL',l.id)" class="push-check-mark">✓</text></view></view><text v-if="!children(b.id).length" class="push-empty">此板块暂无标签</text></view></view><LoginSheet/></view></template>
<style scoped>
.push-stat{display:flex;align-items:center;justify-content:space-between;margin:8px 0 12px;background:linear-gradient(135deg,#3EA968,#4CB87D);border-radius:10px;padding:12px 14px;box-shadow:0 4px 12px rgba(62,178,107,.22)}
.push-left{display:flex;align-items:center;gap:9px}
.push-bar{width:3px;height:16px;border-radius:2px;background:#fff}
.push-title{font-size:16px;font-weight:600;color:#fff}
.push-count{min-width:44px;text-align:center;font-size:14px;font-weight:600;color:#fff;background:rgba(255,255,255,.22);border-radius:8px;padding:3px 10px}
.push-hint{display:block;font-size:12px;color:#737c88;line-height:1.6;margin:0 2px 10px}
.push-card{background:#fff;border-radius:12px;margin-bottom:10px;overflow:hidden;box-shadow:0 2px 10px rgba(0,0,0,.06)}
.push-head{display:flex;align-items:center;gap:8px;background:#2B3450;padding:11px 12px}
.push-head-copy{flex:1;min-width:0}
.push-head-name{display:block;font-size:15px;font-weight:600;color:#fff;line-height:21px}
.push-head-sub{display:block;font-size:12px;color:rgba(255,255,255,.55);margin-top:2px}
.push-all{flex-shrink:0;padding:5px 12px;border-radius:999px;font-size:12px;font-weight:600;color:#fff;background:#3EB26B;border:0;min-height:0;line-height:1.6}
.push-prog{font-size:13px;color:rgba(255,255,255,.85);flex-shrink:0}
.push-arrow{font-size:13px;color:rgba(255,255,255,.8);flex-shrink:0}
.push-tags{padding:8px 8px 6px}
.push-tag{display:flex;align-items:center;justify-content:space-between;padding:11px 10px;border-radius:8px;margin-bottom:4px;background:#fff}
.push-tag.on{background:#F2F9F4;border-left:3px solid #3EB26B}
.push-tag-copy{flex:1;min-width:0}
.push-tag-name{display:block;font-size:14px;color:#1a1a1a;line-height:20px}
.push-tag-hint{display:block;font-size:11px;color:#9aa0a8;margin-top:2px}
.push-check{width:22px;height:22px;border-radius:50%;display:flex;align-items:center;justify-content:center;flex-shrink:0;border:1.5px solid #dcdfe6;background:#fff;margin-left:8px}
.push-check.on{background:#3EB26B;border-color:#3EB26B}
.push-check-mark{font-size:11px;color:#fff;line-height:1}
.push-empty{display:block;font-size:12px;color:#737c88;padding:8px 10px}
</style>
