<script setup lang="ts">
defineProps<{reviewCampaignIds:string[]}>()
</script>
<template><view v-if="reviewCampaignIds.length" class="card"><text class="card-title">申报环节</text><text class="muted">本活动含 {{ reviewCampaignIds.length }} 个申报项目</text><text class="muted">本期暂未开放申报</text></view></template>
