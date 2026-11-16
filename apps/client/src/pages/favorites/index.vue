<script setup lang="ts">
import {ref} from 'vue'
import {onShow,onHide} from '@dcloudio/uni-app'
import {abandonLogin as cancelLogin} from '../../features/auth'
import PersonalList from '../../components/PersonalList.vue'
const list=ref<InstanceType<typeof PersonalList>|null>(null)
onShow(()=>{void list.value?.refresh()});onHide(cancelLogin)
</script>
<template><PersonalList ref="list" kind="favorites" title="管理收藏"/></template>
