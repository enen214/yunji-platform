<script setup lang="ts">
import { ref, onMounted } from 'vue'; import { useRouter } from 'vue-router'; import { login, get } from '../api/client'
const username = ref(''), password = ref(''), error = ref(''), busy = ref(false), router = useRouter()
const captcha=ref<any>({captchaEnabled:false}), code=ref('')
async function refresh(){captcha.value=await get('/auth/code')}
onMounted(()=>refresh().catch(e=>error.value=e.message))
async function submit() { busy.value = true; error.value = ''; try { await login(username.value,password.value,code.value,captcha.value.uuid); await router.push('/articles') } catch(e) { error.value = (e as Error).message;await refresh() } finally { busy.value = false } }
</script>
<template><div class="login-page"><form class="login-card" @submit.prevent="submit"><span class="eyebrow">YUNJI</span><h1>云迹工作台</h1><p class="muted">管理内容、活动与校园服务</p><label>账号<input v-model="username" autocomplete="username" required /></label><label>密码<input v-model="password" type="password" autocomplete="current-password" required /></label><label v-if="captcha.captchaEnabled!==false">验证码<div class="row"><input v-model="code" required /><img :src="'data:image/png;base64,'+captcha.img" alt="验证码" @click="refresh" /></div></label><p role="alert" class="error">{{ error }}</p><button :disabled="busy">{{ busy ? '登录中…' : '登录' }}</button></form></div></template>
