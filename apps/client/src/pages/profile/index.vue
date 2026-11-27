<script setup lang="ts">
import {onHide as onPageHide} from "@dcloudio/uni-app"
import {abandonLogin as cancelPageLogin} from "../../features/auth"
onPageHide(cancelPageLogin)

import LoginSheet from "../../components/LoginSheet.vue"

import {ref} from 'vue';import {onShow} from '@dcloudio/uni-app';import {request,navigate,token,setToken,uploadAvatar,downloadWithAuth,type Row} from '../../api/platform'
const profile=ref<Row|null>(null),nickname=ref(''),error=ref(''),busy=ref(false),avatar=ref('')
async function load(){if(!token()){profile.value=null;avatar.value='';return}profile.value=await request('GET','/api/v1/me/profile');nickname.value=profile.value!.nickname||'';avatar.value=profile.value!.avatarUrl?await downloadWithAuth(String(profile.value!.avatarUrl)).catch(()=>''):''}
async function pickAvatar(){
  if(busy.value)return
  let path=''
  try{const picked=await new Promise<any>((resolve,reject)=>uni.chooseImage({count:1,sizeType:['compressed'],sourceType:['album','camera'],success:resolve,fail:reject}));path=(picked?.tempFilePaths&&picked.tempFilePaths[0])||''}catch{return}
  if(!path)return
  busy.value=true;error.value=''
  try{profile.value=await uploadAvatar(path);avatar.value=profile.value!.avatarUrl?await downloadWithAuth(String(profile.value!.avatarUrl)).catch(()=>''):'';uni.showToast({title:'头像已更新'})}
  catch(e){error.value=(e as Error).message}finally{busy.value=false}
}
async function act(fn:()=>Promise<void>){if(busy.value)return;busy.value=true;error.value='';try{await fn();await load()}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
async function save(){await act(async()=>{await request('PUT','/api/v1/me/profile',{nickname:nickname.value});uni.showToast({title:'资料已保存'})})}
async function reload(){busy.value=true;error.value='';try{await load()}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
onShow(reload)
</script>
<template><view class="page"><view v-if="error" class="state-card" role="alert"><text class="error">{{ error }}</text><button role="button" class="quiet" @click="reload">重新加载</button></view><button role="button" v-if="!profile" @click="navigate('login')">登录</button><template v-else><view class="card profile-card"><view class="avatar" hover-class="yj-hover" @click="pickAvatar"><image v-if="avatar" :src="avatar" mode="aspectFill" style="width:52px;height:52px;border-radius:50%;display:block" /><text v-else>{{ (nickname||'云').slice(0,1) }}</text></view><view><text class="card-title">{{ nickname||'云迹用户' }}</text><text class="muted">个人资料</text></view></view><view class="card"><view class="field"><text class="field-label">昵称</text><input v-model="nickname" maxlength="100" /></view><view class="field"><text class="field-label">手机号</text><text class="muted">{{ profile.phoneMasked||'尚未绑定手机号' }}</text></view><button role="button" :disabled="busy" @click="save">保存资料</button></view><view class="actions"><button role="button" class="quiet" @click="act(async()=>{await request('POST','/api/v1/auth/customer/logout');setToken('');profile=null})">退出登录</button></view></template><LoginSheet/></view></template>
