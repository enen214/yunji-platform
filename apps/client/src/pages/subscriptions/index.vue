<script setup lang="ts">
import {ref,computed,nextTick,getCurrentInstance,watch} from 'vue'
import {onShow,onHide,onUnload,onPageScroll} from '@dcloudio/uni-app'
import {syncTabBar} from '../../features/tabbar'
import {request,navigate,token,type Row} from '../../api/platform'
import {content} from '../../features/content'
import {contentLabel} from '../../features/catalog'
import {visibleExposure,type NativeExposureScope} from '../../features/telemetry'
import LoginSheet from '../../components/LoginSheet.vue'
import PageState from '../../components/PageState.vue'
const boxes=ref<Row[]>([]),tab=ref('CATEGORY'),grouped=ref(true),expanded=ref<string[]>([]),error=ref(''),loading=ref(false),busy=ref(false),signedIn=ref(false),revealed=ref('')
const visible=computed(()=>boxes.value.filter(b=>b.boxKind===tab.value))
const flat=computed(()=>[...new Map(visible.value.flatMap(b=>b.items).map((item:Row)=>[item.id,item])).values()])
const instance=getCurrentInstance(),exposure=visibleExposure(()=>(instance?.proxy as unknown as {$scope?:NativeExposureScope})?.$scope)
async function load(){signedIn.value=!!token();if(!signedIn.value){boxes.value=[];return}loading.value=true;error.value='';try{boxes.value=await content.boxes();await nextTick();exposure.reset();exposure.observe()}catch(e){error.value=(e as Error).message}finally{loading.value=false}}
async function markRead(item:Row){if(busy.value)return;busy.value=true;error.value='';try{await request('PUT','/api/v1/me/read/'+item.id,{versionId:item.currentVersionId});await load();revealed.value=''}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
function expand(id:string){expanded.value=expanded.value.includes(id)?expanded.value.filter(x=>x!==id):[...expanded.value,id];void nextTick(()=>exposure.observe())}
watch([tab,grouped],()=>{uni.setStorageSync('boxGrouped',grouped.value?'1':'0');void nextTick(()=>exposure.observe())})
let startX=0,startY=0
function touchStart(e:Row){startX=e.changedTouches?.[0]?.clientX||0;startY=e.changedTouches?.[0]?.clientY||0}
function touchEnd(e:Row,item:Row){const x=e.changedTouches?.[0]?.clientX??startX,y=e.changedTouches?.[0]?.clientY??startY;if(Math.abs(x-startX)>60&&Math.abs(y-startY)<30)revealed.value=item.id}
onShow(()=>{syncTabBar(1);exposure.show();void load()});onPageScroll(()=>exposure.scroll());onHide(()=>exposure.hide());onUnload(()=>exposure.hide())
// 统计：今日推送/关注更新/待读提醒
const totalUnread=computed(()=>boxes.value.reduce((s,b)=>s+Number(b.unreadCount||0),0))
const totalItems=computed(()=>boxes.value.reduce((s,b)=>s+(b.items?.length||0),0))
const statCards=computed(()=>[{n:totalItems.value,t:'今日推送'},{n:totalUnread.value,t:'关注更新'},{n:flat.value.length,t:'待读提醒'}])
// 水滴滑块位置（按当前 tab 索引）
const tabKeys=['CATEGORY','FOLLOWING','REPLACEMENT']
const curTab=computed(()=>Math.max(0,tabKeys.indexOf(tab.value)))
const tabNames=['推送信息','关注更新','关注信息更替']
// 状态栏高度（custom 导航，页面内容需自行避让状态栏）
const statusBarHeight=uni.getSystemInfoSync().statusBarHeight||0
// 盒子类型标签 + 去掉 name 后缀（"招生考试 · 板块盒子" -> "招生考试"）
function targetName(t:string){return ({CATEGORY:'板块',LABEL:'标签',ARTICLE:'资讯',KEYWORD:'关键词'} as Record<string,string>)[t]||'主题'}
function shortName(n:string){return String(n||'').replace(' · 板块盒子','').replace(' · 关键词盒子','').replace(' · 资讯关注','')}
</script>
<template><view class="page tab-page v34-box" :style="{paddingTop:statusBarHeight+'px'}">
  <!-- 顶部标题栏（与板块页一致） -->
  <view class="v34-bdnav"><text class="v34-bd-tt">信息盒子</text><view class="v34-bd-srch" hover-class="yj-hover" @click="navigate('search')"><text class="v34-srch-ic"></text></view></view>
  <!-- 数字卡（顶部，玻璃小盒子） -->
  <view class="v34-statcard">
    <view class="v34-row3"><view v-for="s in statCards" :key="s.t" class="v34-stt"><text class="v34-stt-n">{{ s.n }}</text><text class="v34-stt-t">{{ s.t }}</text></view></view>
  </view>
  <!-- 两个小胶囊按钮：位于推送信息/关注更新/关注信息更替 tab 上方 -->
  <view class="v34-opbtns">
    <view class="v34-opbtn" hover-class="yj-hover" @click="navigate('follows')"><text class="v34-opbtn-ic">♡</text><text>管理关注</text></view>
    <view class="v34-opbtn" hover-class="yj-hover" @click="navigate('tag-tree')"><text class="v34-opbtn-ic">▦</text><text>选择推送标签</text></view>
  </view>
  <!-- 三胶囊 tab + 水滴滑块 -->
  <view class="v34-filterwrap">
    <view class="v34-gootabs" :class="'tab-'+curTab">
      <view class="v34-goo-ind" :style="{transform:'translateX('+(curTab*100)+'%)'}"></view>
      <view v-for="(t,i) in tabNames" :key="i" :class="['v34-gtab',{on:tabKeys[i]===tab}]" hover-class="yj-hover" @click="tab=tabKeys[i]">{{ t }}</view>
    </view>
  </view>
  <!-- 消息列表：按盒子（板块/关键词）分组 -->
  <view class="v34-listhd"><view class="v34-lt"><view class="v34-bar"></view><text class="v34-tt">消息列表</text></view><text class="v34-ct">{{ flat.length }} 条消息</text></view>
  <PageState :busy="loading" :error="error" :empty="!visible.length&&!loading" empty-text="当前没有未读更新" @retry="load"/>
  <view v-for="box in visible" :key="box.id" class="v34-boxgrp">
    <!-- 更替盒子：特殊卡片 -->
    <template v-if="box.boxKind==='REPLACEMENT'">
      <view class="v34-rp-hd"><text class="v34-pill orange">关注信息更替</text><text v-if="box.unreadCount" class="v34-rp-cnt">{{ box.unreadCount }} 条更新</text></view>
      <text class="v34-rp-name">{{ shortName(box.name) }}</text>
      <view v-if="box.replacement" class="v34-rp-body"><text class="v34-rp-title">新资讯：{{ box.replacement.target?.title || '' }}</text><view class="v34-rp-go" hover-class="yj-hover" @click="navigate('article','?id='+(box.replacement.articleId||''))">查看新资讯</view></view>
    </template>
    <!-- 普通盒子：头部 + 内部信息列表 -->
    <template v-else>
      <view class="v34-boxhd">
        <text class="v34-pill">{{ targetName(box.targetType) }}</text>
        <text class="v34-boxhd-nm">{{ shortName(box.name) }}</text>
        <text v-if="box.unreadCount" class="v34-boxhd-unread">{{ box.unreadCount }} 未读</text>
      </view>
      <view v-for="item in (expanded.includes(box.id)?box.items:box.items.slice(0,2))" :key="item.id" class="v34-msg" :data-article-id="item.id" @touchstart="touchStart" @touchend="touchEnd($event,item)" hover-class="yj-hover" @click="navigate('article','?id='+item.id)">
        <view class="v34-msg-tp"><text class="v34-pill" :class="{orange:['NOTICE','ACTIVITY'].includes(item.contentType)}">{{ contentLabel(item.contentType) }}</text></view>
        <text class="v34-msg-h">{{ item.title }}</text>
        <view v-if="revealed===item.id" class="v34-msg-read"><button role="button" :disabled="busy" hover-class="yj-hover" @click.stop="markRead(item)">确认标记已读</button></view>
        <view class="v34-msg-ft"><text class="v34-msg-tm">{{ item.publishedAt?.slice(0,10) }}</text><text class="v34-msg-go">›</text></view>
      </view>
      <view v-if="box.items.length>2" class="v34-box-more" hover-class="yj-hover" @click="expand(box.id)">{{ expanded.includes(box.id)?'收起':'展开全部' }}</view>
      <view v-if="!box.items.length" class="v34-box-empty">盒子推送已暂停，关注仍保留</view>
    </template>
  </view>
  <LoginSheet/>
</view></template>
<style lang="scss" scoped>
.v34-box{background:transparent;padding-left:0;padding-right:0;position:relative;isolation:isolate;padding-bottom:calc(88px + constant(safe-area-inset-bottom));padding-bottom:calc(88px + env(safe-area-inset-bottom));}
/* 内容块统一垫到玻璃之上 */
.v34-bdnav,.v34-statcard,.v34-opbtns,.v34-filterwrap,.v34-listhd,.v34-msg,.v34-box>PageState{position:relative;z-index:1;}
/* 顶部标题栏（与板块页一致） */
.v34-bdnav{height:44px;flex:none;display:flex;align-items:center;justify-content:center;background:transparent;position:relative;}
.v34-bd-tt{font-size:17px;font-weight:800;color:#1a1a1a;}
.v34-bd-srch{position:absolute;right:14px;top:9px;width:26px;height:26px;border-radius:50%;background:#edf7f0;display:flex;align-items:center;justify-content:center;}
.v34-srch-ic{width:10px;height:10px;border:1.5px solid #3e8559;border-radius:50%;position:relative;box-sizing:border-box;}
.v34-srch-ic::after{content:'';position:absolute;width:4px;height:1.5px;background:#3e8559;right:-3px;bottom:-2px;transform:rotate(45deg);}
/* 数字卡：玻璃小盒子（立体厚度 + 液态玻璃质感） */
.v34-statcard{position:relative;margin:8px 18px 0;padding:14px 16px 13px;z-index:6;
  border-radius:20px;
  border:1px solid rgba(255,255,255,.78);
  background:linear-gradient(180deg,rgba(255,255,255,.72),rgba(255,255,255,.54));
  -webkit-backdrop-filter:blur(18px) saturate(1.5);
  backdrop-filter:blur(18px) saturate(1.5);
  /* 内高光 + 立体厚度阴影（小盒子感） */
  box-shadow:
    inset 0 1px 0 rgba(255,255,255,.95),
    inset 0 0 0 1px rgba(46,125,79,.05),
    0 1px 2px rgba(46,125,79,.06),
    0 6px 16px -6px rgba(46,125,79,.18),
    0 14px 30px -12px rgba(46,125,79,.22);
}
.v34-row3{display:flex;}
.v34-stt{flex:1;text-align:center;position:relative;display:flex;flex-direction:column;align-items:center;gap:2px;}
.v34-stt+.v34-stt::before{content:'';position:absolute;left:0;top:6px;bottom:6px;width:1px;background:#e6e9ed;}
.v34-stt-n{font-size:22px;font-weight:900;color:#1a1a1a;line-height:1;}
.v34-stt-t{font-size:10.5px;color:#737c88;}
/* 三胶囊 tab + 水滴滑块 */
.v34-filterwrap{margin-top:12px;padding:0 16px;}
.v34-gootabs{position:relative;display:flex;gap:8px;padding:4px;background:#eaf4ee;border-radius:14px;}
.v34-goo-ind{position:absolute;top:4px;left:4px;height:calc(100% - 8px);width:calc((100% - 8px - 16px)/3);border-radius:12px;z-index:1;background:linear-gradient(135deg,#FFFFFF,#DFF2E6);box-shadow:inset 0 1px 0 rgba(255,255,255,1),inset 0 0 0 1px rgba(67,179,125,.22),0 3px 8px rgba(80,167,112,.18);transition:transform .5s cubic-bezier(.3,1.2,.4,1);}
.v34-gootabs:active .v34-goo-ind{animation:v34Goo .5s ease;}
.v34-gtab{position:relative;z-index:2;flex:1;text-align:center;padding:9px 0;font-size:12.5px;color:#5a655c;font-weight:600;border-radius:12px;transition:color .3s;}
.v34-gtab.on{color:#2a8c60;font-weight:800;}
@keyframes v34Goo{0%{transform:scale(1,1)}25%{transform:scale(1.5,.68)}55%{transform:scale(1.22,.85)}100%{transform:scale(1,1)}}
/* 两个小胶囊按钮：位于 tab 上方，正常文档流（随内容滚动） */
.v34-opbtns{display:flex;justify-content:center;gap:10px;padding:12px 16px 0;}
.v34-opbtn{pointer-events:auto;flex:0 1 auto;min-width:112px;height:38px;padding:0 18px;border-radius:999px;display:flex;align-items:center;justify-content:center;gap:6px;font-size:13px;color:#2a6b48;font-weight:600;
  border:1px solid rgba(255,255,255,.78);
  background:linear-gradient(180deg,rgba(255,255,255,.68),rgba(255,255,255,.48));
  -webkit-backdrop-filter:blur(16px) saturate(1.5);
  backdrop-filter:blur(16px) saturate(1.5);
  box-shadow:inset 0 1px 0 rgba(255,255,255,.9),inset 0 0 0 1px rgba(46,125,79,.06),0 4px 14px -4px rgba(46,125,79,.28);
}
.v34-opbtn:active{transform:scale(.96);}
.v34-opbtn-ic{font-size:14px;}
/* 消息列表 */
.v34-listhd{display:flex;align-items:center;justify-content:space-between;padding:14px 16px 9px;}
.v34-lt{display:flex;align-items:center;gap:8px;}
.v34-bar{width:4px;height:18px;border-radius:2px;background:linear-gradient(180deg,#50A770,#3D8B5F);}
.v34-tt{font-size:17px;font-weight:800;color:#1a1a1a;}
.v34-ct{font-size:11px;color:#347b4e;background:#edf7f0;padding:2px 9px;border-radius:10px;font-weight:600;}
.v34-msg{margin:0 16px 9px;background:#fff;border-radius:14px;box-shadow:0 1px 4px rgba(0,0,0,.04);padding:11px 13px 10px;position:relative;}
.v34-msg-tp{margin-bottom:6px;display:flex;align-items:center;gap:6px;}
.v34-msg-h{font-size:14px;font-weight:700;color:#1a1a1a;line-height:20px;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;}
.v34-msg-ft{margin-top:7px;display:flex;align-items:center;justify-content:space-between;}
.v34-msg-tm{font-size:11px;color:#9aa0a8;}
.v34-msg-go{width:18px;height:18px;border-radius:50%;background:#f5f7f9;display:flex;align-items:center;justify-content:center;color:#c0c4cc;font-size:12px;}
.v34-msg.unread::before{content:'';position:absolute;left:0;top:14px;bottom:14px;width:3px;border-radius:0 3px 3px 0;background:#50a770;}
.v34-msg-read{margin-top:8px;}
.v34-msg-item{display:flex;align-items:center;justify-content:space-between;padding:8px 0;border-bottom:1px solid #f0f0f0;font-size:13px;}
.v34-pill{display:inline-flex;align-items:center;padding:2px 8px;border-radius:999px;font-size:10px;line-height:16px;white-space:nowrap;color:#2e7d4f;background:#edf7f0;font-weight:600;}
.v34-pill.orange{color:#b67526;background:#fdf3e7;}
/* 盒子分组（按板块/关键词） */
.v34-boxgrp{margin:0 16px 10px;padding:10px 12px 6px;border-radius:14px;
  border:1px solid rgba(255,255,255,.6);
  background:linear-gradient(180deg,rgba(255,255,255,.72),rgba(255,255,255,.5));
  -webkit-backdrop-filter:blur(18px) saturate(1.5);
  backdrop-filter:blur(18px) saturate(1.5);
  box-shadow:inset 0 1px 0 rgba(255,255,255,.85),0 2px 10px -2px rgba(46,125,79,.10);
}
.v34-boxhd{display:flex;align-items:center;gap:6px;padding:2px 2px 8px;}
.v34-boxhd-nm{flex:1;min-width:0;font-size:14px;font-weight:700;color:#1a1a1a;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;}
.v34-boxhd-unread{flex:none;font-size:11px;color:#50A770;background:#e8f5e9;padding:2px 8px;border-radius:10px;}
.v34-boxgrp .v34-msg{margin:0 0 7px;}
.v34-box-more{padding:2px 0 6px;text-align:center;font-size:12px;color:#2a8c60;font-weight:600;}
.v34-box-empty{padding:4px 2px 8px;font-size:12px;color:#9aa0a8;}
/* 更替盒子卡片 */
.v34-rp-hd{display:flex;align-items:center;gap:8px;margin-bottom:6px;}
.v34-rp-cnt{font-size:11px;color:#50A770;background:#e8f5e9;padding:2px 8px;border-radius:10px;}
.v34-rp-name{font-size:15px;font-weight:600;color:#1a1a1a;line-height:22px;display:block;}
.v34-rp-body{margin-top:8px;padding:10px;background:rgba(247,251,248,.9);border-radius:8px;display:flex;flex-direction:column;gap:10px;}
.v34-rp-title{font-size:13px;color:#3d8b5f;line-height:20px;}
.v34-rp-go{align-self:flex-start;display:inline-flex;align-items:center;padding:6px 14px;border-radius:999px;background:#3e8559;color:#fff;font-size:12px;font-weight:600;}
/* 毛玻璃兜底：不支持模糊时补不透明底，避免深色封面透上来 */
@supports not ((backdrop-filter:blur(1px)) or (-webkit-backdrop-filter:blur(1px))){
  .v34-statcard{background:rgba(252,254,253,.96)}
  .v34-opbtn{background:rgba(252,254,253,.96)}
  .v34-boxgrp{background:rgba(252,254,253,.96)}
}
</style>
