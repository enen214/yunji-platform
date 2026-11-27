/**
 * 底栏选中态同步。
 * - 小程序：自定义 tabBar 不会自己跟着路由变，页面 onShow 时把 selected 推给组件实例。
 * - H5：uni 内置 tabBar 没有"选中"class 可用，这里写入 data-active，让 CSS 把高亮药丸
 *   用 transform 滑到对应位置（Apple Music 的切换手感就靠这一下）。
 * 非 tabBar 环境会安静跳过。
 */
export function syncTabBar(index: number) {
  // #ifdef MP-WEIXIN
  try {
    const pages = getCurrentPages()
    const page: any = pages[pages.length - 1]
    const bar = page && typeof page.getTabBar === 'function' ? page.getTabBar() : null
    if (bar && typeof bar.setData === 'function') bar.setData({ selected: index })
  } catch {
    /* 拿不到底栏实例就算了，不能因为这个崩页面 */
  }
  // #endif

  // #ifdef H5
  const el = document.querySelector('.uni-tabbar')
  if (el) el.setAttribute('data-active', String(index))
  // #endif
}

/**
 * H5 专用：往内置底栏里插一块高亮药丸。
 * 底栏由框架渲染、时机不确定，所以先试一次，没成就短轮询兜底。
 */
export function initGlassTabPill() {
  // #ifdef H5
  let tries = 0
  const timer = window.setInterval(() => {
    const bar = document.querySelector('.uni-tabbar')
    if (bar) {
      if (!bar.querySelector('.yj-tb-pill')) {
        const pill = document.createElement('div')
        pill.className = 'yj-tb-pill'
        bar.insertBefore(pill, bar.firstChild)
        if (!bar.getAttribute('data-active')) bar.setAttribute('data-active', '0')
      }
      // 点按立即滑动，不等页面 onShow
      if (!(bar as any).__yjBound) {
        ;(bar as any).__yjBound = true
        bar.addEventListener(
          'click',
          (e) => {
            const item = (e.target as HTMLElement).closest('.uni-tabbar__item')
            if (!item) return
            const items = Array.from(bar.querySelectorAll('.uni-tabbar__item'))
            const i = items.indexOf(item)
            if (i >= 0) bar.setAttribute('data-active', String(i))
          },
          true
        )
      }
      window.clearInterval(timer)
      return
    }
    if (++tries > 60) window.clearInterval(timer)
  }, 120)
  // #endif
}
