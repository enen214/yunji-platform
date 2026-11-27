<script setup lang="ts">
import {onHide as onPageHide} from "@dcloudio/uni-app"
import {abandonLogin as cancelPageLogin} from "../../features/auth"

let statusBarHeight=0
// #ifdef MP-WEIXIN
statusBarHeight=uni.getSystemInfoSync().statusBarHeight||0
// #endif

onPageHide(cancelPageLogin)

import LoginSheet from "../../components/LoginSheet.vue"

import {ref} from 'vue'
import {onShow} from '@dcloudio/uni-app'
import {syncTabBar} from '../../features/tabbar'
import {request,navigate,token,downloadWithAuth,type Row} from '../../api/platform'
import PageState from '../../components/PageState.vue'
const profile=ref<Row|null>(null),error=ref(''),busy=ref(false),avatarLocal=ref(''),unread=ref(0)
const entries=[{page:'history',title:'浏览历史',icon:'◷',icClass:'ic-clock'},{page:'favorites',title:'收藏',icon:'☆',icClass:'ic-star'},{page:'profile',title:'个人信息',icon:'◎',icClass:'ic-user'},{page:'page',title:'关于我们',icon:'ⓘ',icClass:'ic-info',query:'?key=about'}]
async function load(){busy.value=true;error.value='';avatarLocal.value='';unread.value=0;try{profile.value=token()?await request('GET','/api/v1/me/profile'):null;if(profile.value?.avatarUrl){try{avatarLocal.value=await downloadWithAuth(String(profile.value.avatarUrl))}catch{avatarLocal.value=''}}if(token()){try{const summary=await request<Row>('GET','/api/v1/me/discovery-summary');unread.value=Number(summary?.unreadCount||0)}catch{unread.value=0}}}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
onShow(()=>{syncTabBar(3);load()})
</script>
<template><view class="page profile-page tab-page"><view class="profile-aurora"/><view class="nav-bar" :style="{paddingTop:statusBarHeight+'px'}"><view class="nav-row"><text class="nav-title">我的</text></view></view><PageState :error="error" @retry="load"/><view class="profile-hero" hover-class="yj-hover" @click="navigate(profile?'profile':'login')"><view class="avatar"><image v-if="avatarLocal" :src="avatarLocal" mode="aspectFill" style="width:100%;height:100%;border-radius:inherit"/><text v-else>{{ (profile?.nickname||'云').slice(0,1) }}</text></view><view class="profile-copy"><text class="card-title">{{ profile?.nickname||'点击登录云迹' }}</text><text class="muted">{{ profile?'关注重要资讯，不错过每一次更新':'登录后管理收藏、关注与提醒' }}</text></view><view class="profile-edit"><view class="ic-pen"/></view></view>
<view class="card svc-card"><text class="svc-label">服务</text><view class="svc-row"><view class="svc" hover-class="yj-hover" @click="navigate('notifications')"><view class="svc-icon"><view class="ic-bell"/><view v-if="unread>0" class="svc-dot"/></view><view class="svc-copy"><text class="svc-title">强提醒</text></view></view><view class="svc" hover-class="yj-hover" @click="navigate('activity-overview')"><view class="svc-icon blue"><view class="ic-cal"/></view><view class="svc-copy"><text class="svc-title">活动</text></view></view></view></view>
<view class="card menu-card"><view v-for="e in entries" :key="e.title" class="menu-row" hover-class="yj-hover" @click="navigate(e.page,e.query||'')"><view :class="['menu-icon', e.icClass]"/><text class="menu-name">{{ e.title }}</text><text class="chevron">›</text></view></view>
<view class="legal-footer"><text @click="navigate('page','?key=user_agreement')">使用协议</text><text @click="navigate('page','?key=privacy_policy')">隐私政策</text></view><text class="brand-mark">YUNJI</text>
<LoginSheet/></view></template>
