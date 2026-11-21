<script setup lang="ts">
import {onHide as onPageHide} from "@dcloudio/uni-app"
import {abandonLogin as cancelPageLogin} from "../../features/auth"
onPageHide(cancelPageLogin)
function goNotifications(){uni.navigateTo({url:'/pages/notifications/index'})}
function goHome(){uni.switchTab({url:'/pages/index/index'})}
</script><template><view class="page"><view class="hero"><text class="title">旧版提醒已升级</text></view><text class="muted">旧版提醒已升级，请在「提醒」中查看规则与推送记录。</text><view class="actions"><button role="button" @click="goNotifications">前往提醒</button><button role="button" class="quiet" @click="goHome">返回首页</button></view></view></template>
