<script setup lang="ts">
import {ref} from 'vue'
import {onShow,onHide,onReachBottom} from '@dcloudio/uni-app'
import {request,navigate,type Row} from '../../api/platform'
import {abandonLogin} from '../../features/auth'
import LoginSheet from '../../components/LoginSheet.vue'
import PageState from '../../components/PageState.vue'

type Tab='ARTICLE'|'KEYWORD'
const tab=ref<Tab>('ARTICLE'),contentType=ref(''),q=ref(''),rows=ref<Row[]>([]),cursor=ref(''),snapshot=ref(''),busy=ref(false),error=ref(''),loaded=ref(false)
const typeFilters=[{code:'',label:'全部'},{code:'BASIC',label:'基础信息'},{code:'NOTICE',label:'通知'},{code:'GUIDE',label:'攻略'}]
const typeText:Record<string,string>={BASIC:'基础信息',NOTICE:'通知',GUIDE:'攻略'}
async function trackedTargets():Promise<Set<string>>{
  const set=new Set<string>()
  try{
    for(let page=1;page<=10;page++){
      const p=await request('GET','/api/v1/me/reminders/rules?page='+page+'&size=100&targetType='+tab.value)
      const items=(p.items||[]) as Row[]
      for(const r of items)set.add(String(r.targetType)+':'+String(r.targetId))
      if(items.length<100||page*100>=Number(p.total||0))break
    }
  }catch{}
  return set
}
async function load(reset=true){
  if(busy.value)return
  busy.value=true;error.value=''
  try{
    let items:Row[]=[]
    if(tab.value==='ARTICLE'){
      const p=await request('GET','/api/v1/articles/search?q='+encodeURIComponent(q.value.trim())+(contentType.value?'&contentType='+contentType.value:'')+(reset?'':'&cursor='+encodeURIComponent(cursor.value)+'&snapshotAt='+encodeURIComponent(snapshot.value)))
      items=reset?p.items:[...rows.value,...p.items]
      cursor.value=p.nextCursor||'';snapshot.value=p.snapshotAt
    }else{
      items=await request<Row[]>('GET','/api/v1/catalog/keywords?q='+encodeURIComponent(q.value.trim()))
      cursor.value=''
    }
    const tracked=await trackedTargets()
    rows.value=items.filter(r=>!tracked.has(tab.value+':'+String(r.id)))
    loaded.value=true
  }catch(e){error.value=(e as Error).message}finally{busy.value=false}
}
function changeTab(value:Tab){if(busy.value)return;tab.value=value;q.value='';rows.value=[];cursor.value='';snapshot.value='';loaded.value=false;if(value==='KEYWORD')contentType.value='';void load()}
function changeType(value:string){if(busy.value||contentType.value===value)return;contentType.value=value;rows.value=[];cursor.value='';void load()}
function open(row:Row){navigate('notification-edit','?targetType='+tab.value+'&targetId='+row.id+'&mode=DAILY')}
onShow(()=>{void load()});onHide(abandonLogin)
onReachBottom(()=>{if(cursor.value)void load(false)})
</script>
<template>
<view class="page">
  <view class="hero"><text class="subtitle">选中对象后进入设置页，选择日常跟踪或下一次即时。</text></view>
  <view class="segmented">
    <button role="button" :class="{active:tab==='ARTICLE'}" :disabled="busy" hover-class="yj-hover" @click="changeTab('ARTICLE')">信息</button>
    <button role="button" :class="{active:tab==='KEYWORD'}" :disabled="busy" hover-class="yj-hover" @click="changeTab('KEYWORD')">关键词</button>
  </view>
  <view v-if="tab==='ARTICLE'" class="segmented">
    <button role="button" v-for="item in typeFilters" :key="item.label" :class="{active:contentType===item.code}" :disabled="busy" hover-class="yj-hover" @click="changeType(item.code)">{{ item.label }}</button>
  </view>
  <view class="search-row"><input v-model="q" maxlength="100" placeholder="搜索要提醒的信息或关键词" @confirm="load(true)"/><button role="button" :disabled="busy" hover-class="yj-hover" @click="load(true)">搜索</button></view>
  <PageState :busy="busy" :error="error" :empty="loaded&&!rows.length&&!busy" empty-text="暂无可选对象" @retry="load(true)"/>
  <view v-for="row in rows" :key="row.id" class="card">
    <view v-if="tab==='ARTICLE'" class="taxonomy-values">
      <text class="badge">{{ typeText[String(row.contentType)]||'资讯' }}</text>
      <text v-if="row.sourceName" class="badge blue">{{ row.sourceName }}</text>
    </view>
    <text class="card-title" @click="open(row)">{{ row.title||row.name }}</text>
    <view class="actions"><button role="button" :disabled="busy" hover-class="yj-hover" @click="open(row)">设强提醒</button></view>
  </view>
  <button v-if="cursor" role="button" class="quiet" :disabled="busy" hover-class="yj-hover" @click="load(false)">加载更多</button>
  <text v-if="loaded&&rows.length&&!cursor" class="end-note">没有更多了</text>
  <LoginSheet/>
</view>
</template>
