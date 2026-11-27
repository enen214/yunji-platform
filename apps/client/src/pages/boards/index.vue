<script setup lang="ts">
import {onHide as onPageHide} from "@dcloudio/uni-app"
import {abandonLogin as cancelPageLogin} from "../../features/auth"
onPageHide(cancelPageLogin)

import LoginSheet from "../../components/LoginSheet.vue"

import {ref,computed,nextTick} from 'vue';import {onShow} from '@dcloudio/uni-app';import {request,navigate,type Row} from '../../api/platform'
import {syncTabBar} from '../../features/tabbar'
import PageState from '../../components/PageState.vue'
import type {ArticleCard} from '../../types/api'
const rows=ref<Row[]>([]),error=ref(''),busy=ref(false)
// 绿色系配色（全绿，不用彩虹色）
const palette=[['#EAF4EE','#3E8559'],['#E7F5EC','#2E9E63'],['#E4F2EF','#2C8C7D'],['#E6EFEA','#2E7D4F'],['#EAF4EE','#3E8559'],['#E7F5EC','#2E9E63'],['#E4F2EF','#2C8C7D']]
const icons=['▤','♜','◈','▣','◎','♧','✧']
async function load(){busy.value=true;error.value='';try{rows.value=await request('GET','/api/v1/catalog/boards')}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
onShow(()=>{syncTabBar(2);load()})
// 状态栏高度（custom 导航，页面内容需自行避让状态栏）
const statusBarHeight=uni.getSystemInfoSync().statusBarHeight||0
// 拆两列：奇数左列、偶数右列
const leftCol=computed(()=>rows.value.filter((_,i)=>i%2===0))
const rightCol=computed(()=>rows.value.filter((_,i)=>i%2===1))
function tint(i:number){return palette[i%palette.length]}
function icon(i:number){return icons[i%icons.length]}

// —— 就地放大概览层 ——
const ov=ref<null | {b:Row, idx:number, labels:Row[], hot:ArticleCard[], busy:boolean, open:boolean}>(null)
// 面板当前 rect（top/left/width/height，单位 px），用于"原地放大"过渡
const rect=ref({top:0,left:0,width:0,height:0,opacity:0})
function queryRect(sel:string):Promise<{top:number,left:number,width:number,height:number}>{
  return new Promise((resolve)=>{
    const q=uni.createSelectorQuery()
    q.select(sel).boundingClientRect((r:any)=>{
      if(r && !Array.isArray(r)) resolve({top:r.top||0,left:r.left||0,width:r.width||0,height:r.height||0})
      else resolve({top:0,left:0,width:0,height:0})
    }).exec()
  })
}
async function openOverview(b:Row, idx:number, isLeft:boolean, posInCol:number){
  // 1. 先记录被点击卡片的实际位置/尺寸
  const sel = isLeft ? '#blc-'+posInCol : '#brc-'+posInCol
  const start = await queryRect(sel)
  // 2. 面板瞬移到卡片位置（尺寸同卡片），并隐藏内容
  rect.value={top:start.top,left:start.left,width:start.width,height:start.height,opacity:0}
  ov.value={b, idx, labels:[], hot:[], busy:true, open:false}
  // 3. 下一帧触发过渡到目标尺寸（放大）
  await nextTick()
  const sys=uni.getSystemInfoSync()
  const winW=sys.windowWidth
  const target={top:60,left:18,width:winW-36,height:Math.min(430, sys.windowHeight*0.62)}
  // 先等面板渲染出起点
  await new Promise(r=>setTimeout(r,20))
  rect.value={top:target.top,left:target.left,width:target.width,height:target.height,opacity:1}
  ov.value={...ov.value, open:true}
  // 4. 加载数据
  try{
    const [allLabels, hotPage] = await Promise.all([
      request<Row[]>('GET','/api/v1/catalog/labels'),
      request<{items:ArticleCard[]}>('GET','/api/v1/articles?hotOnly=true&categoryId='+b.id)
    ])
    const labels = allLabels.filter(l=>l.categoryId===b.id)
    ov.value = {b, idx, labels, hot:(hotPage.items||[]).slice(0,2), busy:false, open:true}
  }catch(e){
    ov.value = {b, idx, labels:[], hot:[], busy:false, open:true}
  }
}
async function closeOverview(){
  if(!ov.value) return
  ov.value = {...ov.value, open:false}
  // 收起：面板缩回卡片尺寸
  const sel = '' // 收起时直接缩小并淡出即可
  rect.value={...rect.value, opacity:0}
  await new Promise(r=>setTimeout(r,320))
  ov.value=null
}
</script>
<template><view class="page tab-page v34-boards" :style="{paddingTop:statusBarHeight+'px'}">
  <view class="v34-bdnav"><text class="v34-bd-tt">板块</text><view class="v34-bd-srch" hover-class="yj-hover" @click="navigate('search')"><text class="v34-srch-ic"></text></view></view>
  <view class="v34-bdintro"><text class="v34-bd-r">{{ rows.length }} 个板块</text></view>
  <PageState :busy="busy" :error="error" :empty="!rows.length&&!busy" empty-text="暂无已开放板块" @retry="load"/>
  <view v-if="rows.length" class="v34-twocol">
    <view class="v34-col"><view class="v34-col-reel">
      <view v-for="(b,i) in leftCol" :key="b.id" :id="'blc-'+i" class="v34-bcard" :style="{background:'#fff'}" hover-class="yj-hover" @click="openOverview(b, i*2, true, i)">
        <view class="v34-bhalo" :style="{background:tint(i*2)[1]}"></view><view class="v34-bic" :style="{background:tint(i*2)[0]}"><text class="v34-bic-t" :style="{color:tint(i*2)[1]}">{{ icon(i*2) }}</text></view><text class="v34-bcard-h">{{ b.name }}</text><text class="v34-bcard-p">{{ b.description||'浏览相关资讯与标签' }}</text>
        <view class="v34-bcard-bt"><view class="v34-bcard-cnt"><text class="v34-bcard-n">{{ b.articleCount }}</text><text class="v34-bcard-u">条信息</text></view><view class="v34-bcard-go" :style="{background:tint(i*2)[0],color:tint(i*2)[1]}">›</view></view>
      </view>
    </view></view>
    <view class="v34-col"><view class="v34-col-reel">
      <view v-for="(b,i) in rightCol" :key="b.id" :id="'brc-'+i" class="v34-bcard" :style="{background:'#fff'}" hover-class="yj-hover" @click="openOverview(b, i*2+1, false, i)">
        <view class="v34-bhalo" :style="{background:tint(i*2+1)[1]}"></view><view class="v34-bic" :style="{background:tint(i*2+1)[0]}"><text class="v34-bic-t" :style="{color:tint(i*2+1)[1]}">{{ icon(i*2+1) }}</text></view><text class="v34-bcard-h">{{ b.name }}</text><text class="v34-bcard-p">{{ b.description||'浏览相关资讯与标签' }}</text>
        <view class="v34-bcard-bt"><view class="v34-bcard-cnt"><text class="v34-bcard-n">{{ b.articleCount }}</text><text class="v34-bcard-u">条信息</text></view><view class="v34-bcard-go" :style="{background:tint(i*2+1)[0],color:tint(i*2+1)[1]}">›</view></view>
      </view>
    </view></view>
  </view>

  <!-- 就地放大概览层 -->
  <view v-if="ov" class="v34-ov-mask" hover-class="yj-hover" @click="closeOverview">
    <view class="v34-ov-main" :class="{open:ov.open}" :style="{top:rect.top+'px',left:rect.left+'px',width:rect.width+'px',height:rect.height+'px',opacity:rect.opacity}" hover-class="yj-hover" @click.stop>
      <view class="v34-ov-head">
        <view class="v34-ov-bic" :style="{background:tint(ov.idx)[0]}"><text class="v34-ov-bic-t" :style="{color:tint(ov.idx)[1]}">{{ icon(ov.idx) }}</text></view>
        <view class="v34-ov-head-r"><text class="v34-ov-title">{{ ov.b.name }}</text><text class="v34-ov-desc">{{ ov.b.description || '浏览相关资讯与标签' }}</text></view>
      </view>
      <view class="v34-ov-sec"><text class="v34-ov-sec-dot" :style="{background:tint(ov.idx)[1]}"></text><text class="v34-ov-sec-t">标签</text></view>
      <view v-if="ov.busy" class="v34-ov-muted">加载中…</view>
      <view v-else-if="ov.labels.length" class="v34-ov-tags"><view v-for="l in ov.labels" :key="l.id" class="v34-ov-tag" hover-class="yj-hover" @click="closeOverview();navigate('label','?id='+l.id)">{{ l.name }}</view></view>
      <view v-else class="v34-ov-muted">该板块暂无标签</view>
      <view class="v34-ov-sec"><text class="v34-ov-sec-dot" :style="{background:tint(ov.idx)[1]}"></text><text class="v34-ov-sec-t">热门资讯</text></view>
      <view v-if="ov.busy" class="v34-ov-muted">加载中…</view>
      <view v-else-if="ov.hot.length" class="v34-ov-hot">
        <view v-for="a in ov.hot" :key="a.id" class="v34-ov-item" hover-class="yj-hover" @click="closeOverview();navigate('article','?id='+a.id)"><text class="v34-ov-item-t">{{ a.title }}</text><text class="v34-ov-item-m">{{ a.publishedAt?.slice(0,10) }}</text></view>
      </view>
      <view v-else class="v34-ov-muted">该板块暂无热门资讯</view>
      <view class="v34-ov-btn" hover-class="yj-hover" @click="closeOverview();navigate('board','?id='+ov.b.id)">查看板块详情</view>
    </view>
  </view>

  <LoginSheet/>
</view></template>
<style lang="scss" scoped>
.v34-boards{background:transparent;padding-left:0;padding-right:0;position:relative;isolation:isolate;}
/* 内容块统一垫到玻璃之上 */
.v34-bdnav,.v34-bdintro,.v34-twocol{position:relative;z-index:1;}
.v34-bdnav{height:44px;flex:none;display:flex;align-items:center;justify-content:center;background:transparent;position:relative;}
.v34-bd-tt{font-size:17px;font-weight:800;color:#1a1a1a;}
.v34-bd-srch{position:absolute;right:14px;top:9px;width:26px;height:26px;border-radius:50%;background:#edf7f0;display:flex;align-items:center;justify-content:center;}
.v34-srch-ic{width:10px;height:10px;border:1.5px solid #3e8559;border-radius:50%;position:relative;box-sizing:border-box;}
.v34-srch-ic::after{content:'';position:absolute;width:4px;height:1.5px;background:#3e8559;right:-3px;bottom:-2px;transform:rotate(45deg);}
.v34-bdintro{margin:10px 16px 4px;display:flex;align-items:baseline;justify-content:flex-end;}
.v34-bd-r{font-size:11px;color:#9aa0a8;}
.v34-twocol{display:flex;gap:10px;padding:8px 16px 20px;}
.v34-col{flex:1;display:flex;}
.v34-col-reel{display:flex;flex-direction:column;gap:10px;width:100%;}
.v34-bcard{position:relative;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 1px 4px rgba(0,0,0,.04);padding:13px 13px 11px;display:flex;flex-direction:column;gap:7px;min-height:118px;}
.v34-bhalo{position:absolute;top:-18px;right:-18px;width:74px;height:74px;border-radius:50%;opacity:.16;}
.v34-bic{position:relative;width:38px;height:38px;border-radius:12px;display:flex;align-items:center;justify-content:center;box-shadow:0 2px 6px rgba(0,0,0,.06);}
.v34-bic-t{font-size:18px;}
.v34-bcard-h{position:relative;font-size:15px;font-weight:800;color:#1a1a1a;line-height:21px;}
.v34-bcard-p{position:relative;font-size:11px;color:#9aa0a8;line-height:16px;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;}
.v34-bcard-bt{position:relative;display:flex;align-items:center;justify-content:space-between;margin-top:auto;}
.v34-bcard-cnt{display:flex;align-items:baseline;gap:3px;}
.v34-bcard-n{font-size:15px;font-weight:900;color:#1a1a1a;}
.v34-bcard-u{font-size:10px;color:#9aa0a8;}
.v34-bcard-go{width:22px;height:22px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:12px;}

/* 就地放大概览层：面板从卡片原位过渡放大 */
.v34-ov-mask{position:fixed;inset:0;z-index:300;background:rgba(0,0,0,.35);}
.v34-ov-main{position:fixed;background:#fff;border-radius:16px;box-shadow:0 10px 40px rgba(0,0,0,.18);overflow-y:auto;padding:16px 16px 14px;box-sizing:border-box;transition:top .42s cubic-bezier(.2,1,.3,1),left .42s cubic-bezier(.2,1,.3,1),width .42s cubic-bezier(.2,1,.3,1),height .42s cubic-bezier(.2,1,.3,1),opacity .3s,border-radius .42s;}
.v34-ov-main.open{border-radius:22px;}
.v34-ov-head{display:flex;align-items:center;gap:12px;margin-bottom:14px;opacity:0;transition:opacity .3s .18s;}
.v34-ov-main.open .v34-ov-head{opacity:1;}
.v34-ov-bic{width:44px;height:44px;border-radius:13px;display:flex;align-items:center;justify-content:center;flex:none;}
.v34-ov-bic-t{font-size:21px;}
.v34-ov-head-r{display:flex;flex-direction:column;gap:3px;min-width:0;}
.v34-ov-title{font-size:18px;font-weight:800;color:#1a1a1a;}
.v34-ov-desc{font-size:12px;color:#8a919c;line-height:18px;}
.v34-ov-sec{display:flex;align-items:center;gap:6px;margin:14px 0 8px;}
.v34-ov-sec-dot{width:5px;height:5px;border-radius:50%;}
.v34-ov-sec-t{font-size:13px;font-weight:800;color:#1a1a1a;}
.v34-ov-tags{display:flex;flex-wrap:wrap;gap:7px;}
.v34-ov-tag{display:inline-flex;padding:5px 12px;border-radius:999px;background:#edf7f0;color:#2e7d4f;font-size:12px;font-weight:600;}
.v34-ov-hot{display:flex;flex-direction:column;gap:8px;}
.v34-ov-item{display:flex;flex-direction:column;gap:4px;padding:11px 13px;border-radius:12px;background:#f6f8f7;}
.v34-ov-item-t{font-size:13px;font-weight:600;color:#1a1a1a;line-height:19px;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;}
.v34-ov-item-m{font-size:10px;color:#9aa0a8;}
.v34-ov-muted{font-size:12px;color:#9aa0a8;padding:4px 0;}
.v34-ov-btn{margin-top:18px;height:44px;border-radius:12px;background:#3e8559;color:#fff;font-size:15px;font-weight:700;display:flex;align-items:center;justify-content:center;}
</style>
