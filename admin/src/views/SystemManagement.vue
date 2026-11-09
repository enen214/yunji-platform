<script setup lang="ts">
import {onMounted,ref} from 'vue'
import {get,request} from '../api/client'
type Account={userId:string;userName:string;nickName:string;status:string;yunjiLockVersion:number}
const rows=ref<Account[]>([]),selected=ref<Account|null>(null),open=ref(false),busy=ref(false),error=ref('')
const form=ref({userName:'',nickName:'',password:'',enabled:true,expectedVersion:0})
async function load(){rows.value=await get('/api/v1/admin/accounts')}
async function act(fn:()=>Promise<void>){busy.value=true;error.value='';try{await fn()}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
function edit(row:Account|null){selected.value=row;form.value={userName:row?.userName||'',nickName:row?.nickName||'',password:'',enabled:row?.status!=='1',expectedVersion:row?.yunjiLockVersion||0};open.value=true}
async function save(){await act(async()=>{await request(selected.value?'PUT':'POST','/api/v1/admin/accounts'+(selected.value?'/'+selected.value.userId:''),form.value);form.value.password='';open.value=false;await load()})}
onMounted(()=>act(load))
</script>
<template><section><div class="title-row"><div><h1>管理员账号</h1><p class="muted">运营账号统一业务权限。停用或重置密码使现有会话失效。</p></div><button :disabled="busy" @click="edit(null)">新建账号</button></div><p role="alert" class="error">{{ error }}</p><div class="panel scroll"><table><thead><tr><th>账号</th><th>名称</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="r in rows" :key="r.userId"><td>{{ r.userName }}</td><td>{{ r.nickName }}</td><td>{{ r.status==='0'?'启用':'停用' }}</td><td><button class="quiet" @click="edit(r)">编辑 / 重置密码</button></td></tr></tbody></table></div><div v-if="open" class="overlay"><form class="dialog" @submit.prevent="save"><h2>{{ selected?'编辑':'新建' }}管理员</h2><label>账号<input v-model="form.userName" required minlength="3" maxlength="30" autocomplete="off"/></label><label>名称<input v-model="form.nickName" required maxlength="30"/></label><label>{{ selected?'新密码（留空保留）':'初始密码' }}<input v-model="form.password" type="password" :required="!selected" minlength="12" maxlength="64" autocomplete="new-password"/></label><p class="muted">密码需包含字母和数字，至少12位。通过安全渠道交付。</p><label><input type="checkbox" v-model="form.enabled"/>启用账号</label><p role="alert" class="error">{{ error }}</p><div class="actions"><button :disabled="busy">保存</button><button type="button" class="quiet" :disabled="busy" @click="open=false;form.password=''">取消</button></div></form></div></section></template>
