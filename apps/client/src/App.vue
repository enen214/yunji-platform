<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { initGlassTabPill } from './features/tabbar'
onLaunch(() => {
  // H5 的内置底栏没有选中态 class，这里补一块会滑动的高亮药丸
  initGlassTabPill()
})
</script>
<style lang="scss">@use './styles/app.scss';@use './styles/prototype.scss';</style>
