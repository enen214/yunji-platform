<script setup lang="ts">
import {onHide as onPageHide} from "@dcloudio/uni-app"
import {abandonLogin as cancelPageLogin} from "../../features/auth"
onPageHide(cancelPageLogin)

import LoginSheet from "../../components/LoginSheet.vue"

import {ref} from 'vue';import {onShow} from '@dcloudio/uni-app';import {request,navigate,token,setToken,type Row} from '../../api/platform'
const profile=ref<Row|null>(null),orgs=ref<Row[]>([]),nickname=ref(''),avatar=ref(''),avatarData=ref(''),orgIndex=ref(-1),error=ref(''),busy=ref(false)
// 微信小程序 <image> 不支持 dataURL，把 base64 转成临时文件路径再显示；H5 直接支持
function tempPath(ext:string):string{
  const wxApi=(globalThis as unknown as {wx?:{env?:{USER_DATA_PATH?:string}}}).wx
  return (wxApi?.env?.USER_DATA_PATH||'wxfile://usr')+'/avatar_'+Date.now()+'.'+ext
}
function displayImage(src:string):Promise<string>{
  return new Promise((resolve)=>{
    // #ifdef H5
    resolve(src)
    // #endif
    // #ifdef MP-WEIXIN
    if(!src||!src.startsWith('data:image/')){resolve(src||'');return}
    const m=src.match(/^data:image\/(\w+);base64,(.+)$/)
    if(!m){resolve('');return}
    const ext=m[1]==='jpeg'?'jpg':m[1]
    try{
      const buf=uni.base64ToArrayBuffer(m[2])
      const fp=tempPath(ext)
      uni.getFileSystemManager().writeFile({filePath:fp,data:buf,success:()=>resolve(fp),fail:()=>resolve('')})
    }catch(e){resolve('')}
    // #endif
  })
}
async function load(){if(!token()){profile.value=null;return}profile.value=await request('GET','/api/v1/me/profile');nickname.value=profile.value!.nickname||'';avatarData.value=profile.value!.avatarUrl||'';avatar.value=await displayImage(avatarData.value);orgIndex.value=orgs.value.findIndex(o=>o.id===profile.value!.organizationId)}
async function act(fn:()=>Promise<void>){if(busy.value)return;busy.value=true;error.value='';try{await fn();await load()}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
async function save(){await act(async()=>{await request('PUT','/api/v1/me/profile',{nickname:nickname.value,organizationId:profile.value?.organizationId||null,avatarUrl:avatarData.value||null});uni.showToast({title:'资料已保存'})})}
function pickAvatar(){uni.chooseImage({count:1,sizeType:['compressed'],sourceType:['album','camera'],success:async(res)=>{const path=res.tempFilePaths[0];if(!path)return;try{
  // #ifdef MP-WEIXIN
  avatar.value=path
  const readAs=(filePath:string)=>{const ext=(filePath.split('.').pop()||'jpg').toLowerCase();const mime=ext==='png'?'image/png':ext==='gif'?'image/gif':ext==='webp'?'image/webp':'image/jpeg';uni.getFileSystemManager().readFile({filePath,encoding:'base64',success:r=>{avatarData.value='data:'+mime+';base64,'+r.data},fail:()=>{error.value='头像读取失败'}})}
  uni.compressImage({src:path,quality:60,success:r=>readAs(r.tempFilePath),fail:()=>readAs(path)})
  // #endif
  // #ifdef H5
  const blob=await (await fetch(path)).blob();const dataUrl=await new Promise<string>((resolve,reject)=>{const fr=new FileReader();fr.onload=()=>resolve(fr.result as string);fr.onerror=()=>reject(new Error('头像读取失败'));fr.readAsDataURL(blob)});avatarData.value=dataUrl;avatar.value=dataUrl
  // #endif
}catch(e){error.value=(e as Error).message}}})}
onShow(async()=>{busy.value=true;error.value='';try{await load()}catch(e){error.value=(e as Error).message}finally{busy.value=false}})
</script>
<template><view class="page"><text class="error">{{ error }}</text><button role="button" v-if="!profile" @click="navigate('login')">登录</button><template v-else><view class="card profile-card"><view class="avatar" @click="pickAvatar"><image v-if="avatar" :src="avatar" mode="aspectFill" style="width:100%;height:100%;border-radius:50%"/><text v-else>{{ (nickname||'云').slice(0,1) }}</text></view><view><text class="card-title">{{ nickname||'云迹用户' }}</text><text class="muted">点击头像可更换</text></view></view><view class="card"><view class="field"><text class="field-label">昵称</text><input v-model="nickname" maxlength="100" /></view><view class="field"><text class="field-label">手机号</text><text class="muted">{{ profile.phoneMasked||'尚未绑定手机号' }}</text></view><button role="button" :disabled="busy" @click="save">保存资料</button></view><view class="actions"><button role="button" class="quiet" @click="act(async()=>{await request('POST','/api/v1/auth/customer/logout');setToken('');profile=null})">退出登录</button></view></template><LoginSheet/></view></template>
