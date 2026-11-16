<script setup lang="ts">
import {onHide as onPageHide} from "@dcloudio/uni-app"
import {abandonLogin as cancelPageLogin} from "../../features/auth"
onPageHide(cancelPageLogin)

import LoginSheet from "../../components/LoginSheet.vue"

import {ref} from 'vue'
import {onShow} from '@dcloudio/uni-app'
import {request,navigate,token,type Row} from '../../api/platform'
import PageState from '../../components/PageState.vue'
const profile=ref<Row|null>(null),error=ref(''),busy=ref(false)
const entries=[{page:'favorites',title:'管理收藏',icon:'☆'},{page:'history',title:'浏览历史',icon:'◷'},{page:'feedback',title:'平台反馈',icon:'▢'},{page:'page',title:'关于我们',icon:'ⓘ',query:'?key=about'}]
async function load(){busy.value=true;error.value='';try{profile.value=token()?await request('GET','/api/v1/me/profile'):null}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
onShow(load)
</script>
<template><view class="page profile-page"><PageState :error="error" @retry="load"/><view class="card profile-card" @click="navigate(profile?'profile':'login')"><view class="avatar"><image v-if="profile?.avatarUrl" :src="profile.avatarUrl" mode="aspectFill" style="width:100%;height:100%;border-radius:50%"/><text v-else>{{ (profile?.nickname||'云').slice(0,1) }}</text></view><view class="profile-copy"><text class="card-title">{{ profile?.nickname||'点击登录云迹' }}</text><text class="muted">{{ profile?'关注重要资讯，不错过每一次更新':'登录后管理收藏、关注与提醒' }}</text></view><text class="chevron">›</text></view>
<view class="card profile-services"><text class="card-title">服务</text><view class="service-shortcuts"><view class="shortcut" @click="navigate('notifications')"><view class="shortcut-icon">♧</view><text>强提醒</text></view><view class="shortcut" @click="navigate('activity-overview')"><view class="shortcut-icon blue">▣</view><text>活动</text></view></view></view>
<view class="card menu-card"><view v-for="e in entries" :key="e.title" class="list-item row" @click="navigate(e.page,e.query||'')"><text class="menu-icon">{{ e.icon }}</text><text>{{ e.title }}</text><text class="chevron">›</text></view></view>
<view class="legal-footer"><text @click="navigate('page','?key=terms')">使用协议</text><text @click="navigate('page','?key=privacy')">隐私政策</text></view>
<LoginSheet/></view></template>
