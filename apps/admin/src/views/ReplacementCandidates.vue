<script setup lang="ts">
import {ref,onMounted} from 'vue'
import {get,request} from '../api/client'
const rows=ref<any[]>([]),busy=ref(false),error=ref('')
async function load(){rows.value=await get('/api/v1/admin/articles/replacement-candidates')}
async function resolve(row:any,ignore=false){if(busy.value)return;if(!ignore&&!confirm('确认更替？旧文结束但仍可读，关注与提醒迁往新文。'))return;busy.value=true;error.value='';try{await request('POST',ignore?'/api/v1/admin/articles/replacement-candidates/'+row.id+'/ignore':'/api/v1/admin/articles/'+row.oldArticleId+'/replacement',ignore?undefined:{newArticleId:row.newArticleId});await load()}catch(e){error.value=(e as Error).message}finally{busy.value=false}}
onMounted(()=>load().catch(e=>error.value=e.message))
</script><template><section><div class="title-row"><div><h1>更替判断列表</h1><p>板块、标签集合、关键词集合完全一致时提示候选；分类相同不等于内容确实更替，请人工判断。</p></div><button :disabled="busy" @click="load">刷新</button></div><p role="alert" class="error">{{ error }}</p><div class="panel"><table><thead><tr><th>旧资讯</th><th>新资讯</th><th>操作</th></tr></thead><tbody><tr v-for="r in rows" :key="r.id"><td>{{ r.oldTitle }}</td><td>{{ r.newTitle }}</td><td><button :disabled="busy" @click="resolve(r)">确认更替</button><button :disabled="busy" class="quiet" @click="resolve(r,true)">忽略</button></td></tr></tbody></table><p v-if="!rows.length">暂无待判断候选</p></div></section></template>
