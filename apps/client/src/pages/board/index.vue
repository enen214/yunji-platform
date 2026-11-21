<script setup lang="ts">
import {onHide as onPageHide} from "@dcloudio/uni-app"
import {abandonLogin as cancelPageLogin} from "../../features/auth"
onPageHide(cancelPageLogin)

import {ref} from 'vue'
import {onLoad,onShow,onReachBottom} from '@dcloudio/uni-app'
import CatalogDetail from '../../components/CatalogDetail.vue'
const id=ref(''),catalog=ref<InstanceType<typeof CatalogDetail>|null>(null)
onLoad(q=>{id.value=String(q?.id||'')})
onShow(()=>{void catalog.value?.refresh().catch(()=>{})})
onReachBottom(()=>catalog.value?.more())
</script>
<template><CatalogDetail ref="catalog" kind="board" :id="id"/></template>
