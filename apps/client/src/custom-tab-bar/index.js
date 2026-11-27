/**
 * 微信小程序自定义底栏（原生组件，不走 Vue 编译）
 * uni 只把这个目录原样拷到产物根目录，所以必须用小程序原生四件套。
 * 悬浮玻璃材质见 index.wxss。
 */
Component({
  data: {
    selected: 0,
    step: 70,
    pillW: 62,
    list: [
      { pagePath: '/pages/index/index', text: '主页', icon: '/static/tabs/home.png', active: '/static/tabs/home-active.png' },
      { pagePath: '/pages/subscriptions/index', text: '信息盒子', icon: '/static/tabs/box.png', active: '/static/tabs/box-active.png' },
      { pagePath: '/pages/boards/index', text: '板块', icon: '/static/tabs/board.png', active: '/static/tabs/board-active.png' },
      { pagePath: '/pages/me/index', text: '我的', icon: '/static/tabs/my.png', active: '/static/tabs/my-active.png' },
    ]
  },

  attached() {
    this.layout()
    this.syncSelected()
  },

  pageLifetimes: {
    show() {
      this.syncSelected()
    }
  },

  methods: {
    /** 药丸的宽度和每格步长按实际屏宽算，写死 px 会在不同机型上错位 */
    layout() {
      try {
        const info = typeof wx.getWindowInfo === 'function' ? wx.getWindowInfo() : wx.getSystemInfoSync()
        const barWidth = info.windowWidth - 20 // 外层左右各 10px
        const step = barWidth / this.data.list.length
        this.setData({ step, pillW: Math.round(step - 10) })
      } catch (e) {
        this.setData({ step: 70, pillW: 62 })
      }
    },


    /** 按当前页面路由反推选中项；页面侧 onShow 也会调 syncTabBar 兜底 */
    syncSelected() {
      const pages = getCurrentPages()
      if (!pages || !pages.length) return
      const route = '/' + pages[pages.length - 1].route
      const index = this.data.list.findIndex((item) => item.pagePath === route)
      if (index >= 0 && index !== this.data.selected) this.setData({ selected: index })
    },

    onTap(e) {
      const index = Number(e.currentTarget.dataset.index)
      const item = this.data.list[index]
      if (!item || index === this.data.selected) return
      // 先给即时选中反馈，再切页；切完 syncSelected 会再校准一次
      this.setData({ selected: index })
      wx.switchTab({ url: item.pagePath })
    }
  }
})
