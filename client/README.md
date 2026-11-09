# 云迹用户端

uni-app Vue 3、TypeScript。资讯、订阅、服务、个人资料等页面已实现，H5/小程序编译通过。核心资讯列表和详情通过浏览器回环；全部用户写操作、微信开发者工具与真机验收仍待完成。

```powershell
npm ci
npm run dev:h5
npm run typecheck
npm run build:h5
npm run build:mp-weixin
```

H5 开发端口 5173，通过 Vite 同源代理访问本地 18088 API。小程序需配置 VITE_API_BASE_URL、VITE_WECHAT_APP_ID、manifest.json 中实际 AppID 与合法 HTTPS 域名。前端不得配置 AppSecret。启动、验收结果、本地客户登录限制与外部平台待办见项目根 README 和 docs/当前阶段与验收结果.md。
