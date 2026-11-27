<script setup lang="ts">
// 自定义导航条：与页面柔光底同层，不再是一条独立的白色导航栏
// 注意：布尔型 prop 未传入时 Vue 会 cast 成 false（不是 undefined），
// 所以必须用 withDefaults 显式给 true，否则返回按钮永远不渲染。
const props = withDefaults(defineProps<{ title: string; back?: boolean }>(), { back: true })

let statusBarHeight = 0
// #ifdef MP-WEIXIN
statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 0
// #endif

function back() {
  const pages = getCurrentPages()
  if (pages.length > 1) uni.navigateBack()
  else uni.switchTab({ url: '/pages/index/index' })
}
</script>

<template>
  <view class="page-nav" :style="{ paddingTop: statusBarHeight + 'px' }">
    <view class="page-nav-row">
      <view v-if="props.back" class="page-nav-back" role="button" aria-label="返回" @click="back">
        <view class="nav-chevron" />
      </view>
      <text class="page-nav-title">{{ props.title }}</text>
    </view>
  </view>
</template>
