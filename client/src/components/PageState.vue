<script setup lang="ts">
defineProps<{busy?:boolean;error?:string;empty?:boolean;emptyText?:string}>()
defineEmits<{retry:[]}>()
</script>
<template><view v-if="error" class="state-card" role="alert"><text class="error">{{ error }}</text><button role="button" class="quiet" @click="$emit('retry')">重新加载</button></view><view v-else-if="busy" class="state-loading" role="status"><view class="loading-dot"></view><text>正在加载…</text></view><view v-else-if="empty" class="empty"><text>{{ emptyText||'这里暂时没有内容' }}</text></view></template>
