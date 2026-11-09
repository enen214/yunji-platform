<script setup lang="ts">
import {onHide as onPageHide} from "@dcloudio/uni-app"
import {abandonLogin as cancelPageLogin} from "../../features/auth"
onPageHide(cancelPageLogin)

import LoginSheet from "../../components/LoginSheet.vue"

import {ref} from 'vue';import {onShow} from '@dcloudio/uni-app';import {request,navigate,token,setToken,type Row} from '../../api/platform'
const profile=ref<Row|null>(null),orgs=ref<Row[]>([]),nickname=ref(''),orgIndex=ref(-1),error=ref(''),busy=ref(false)
async function load(){if(!token()){profile.value=null;return}profile.value=await request('GET','/api/v1/me/profile');nickname.value=profile.value!.nickname||'';orgIndex.value=orgs.value.findIndex(o=>o.id===profile.value!.organizationId)}
async function act(fn:()=>Promise<void>){if(busy.value)return;busy.value=true;error.value='';try{await fn();await load()}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
async function save(){await act(async()=>{await request('PUT','/api/v1/me/profile',{nickname:nickname.value,organizationId:profile.value?.organizationId||null});uni.showToast({title:'资料已保存'})})}
onShow(async()=>{busy.value=true;error.value='';try{await load()}catch(e){error.value=(e as Error).message}finally{busy.value=false}})
</script>
<template><view class="page"><text class="error">{{ error }}</text><button role="button" v-if="!profile" @click="navigate('login')">登录</button><template v-else><view class="card profile-card"><view class="avatar">{{ (nickname||'云').slice(0,1) }}</view><view><text class="card-title">{{ nickname||'云迹用户' }}</text><text class="muted">个人资料</text></view></view><view class="card"><view class="field"><text class="field-label">昵称</text><input v-model="nickname" maxlength="100" /></view><view class="field"><text class="field-label">手机号</text><text class="muted">{{ profile.phoneMasked||'尚未绑定手机号' }}</text></view><button role="button" :disabled="busy" @click="save">保存资料</button></view><view class="actions"><button role="button" class="quiet" @click="act(async()=>{await request('POST','/api/v1/auth/customer/logout');setToken('');profile=null})">退出登录</button></view></template><LoginSheet/></view></template>
