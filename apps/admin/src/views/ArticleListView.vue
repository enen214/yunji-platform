<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { get } from '../api/client'
interface Row { id: string; title: string; status?: string; publishedAt?: string }
const rows = ref<Row[]>([]); const error = ref('')
onMounted(async () => { try { rows.value = (await get<{ items: Row[] }>('/api/v1/articles')).items } catch (e) { error.value = e instanceof Error ? e.message : '加载失败' } })
</script>
<template><main class="page"><header><p class="eyebrow">CONTENT / LIST</p><h1>公开资讯预览</h1></header><p v-if="error" class="error">{{ error }}</p><table><thead><tr><th>ID</th><th>标题</th><th>状态</th><th>发布时间</th></tr></thead><tbody><tr v-for="row in rows" :key="row.id"><td>{{ row.id }}</td><td>{{ row.title }}</td><td>{{ row.status || 'PUBLISHED' }}</td><td>{{ row.publishedAt || '—' }}</td></tr></tbody></table></main></template>
<style scoped>.page{max-width:1120px;margin:0 auto;padding:40px 24px}.eyebrow{color:var(--primary);font-size:12px;font-weight:700;letter-spacing:.12em}h1{font-size:32px}.muted{color:var(--muted)}table{width:100%;margin-top:28px;border-collapse:collapse;background:var(--surface)}th,td{text-align:left;padding:12px;border-bottom:1px solid var(--line);font-size:14px}.error{color:#ad3a3a}</style>
