// Product baseline: real H5 and administrator actions against disposable MySQL/Redis.
const {chromium,expect:base}=require('@playwright/test');
const expect=base.configure({timeout:15000}),fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const root=path.resolve(__dirname,'..'),f=JSON.parse(fs.readFileSync(process.env.YUNJI_BROWSER_FIXTURES,'utf8'));
const report={scope:'PRODUCT_BASELINE_REAL_BROWSER',cases:[],errors:[]};
async function check(id,fn){await fn();report.cases.push({id,status:'PASS'});console.log(id,'PASS')}
async function api(method,route,body,token){const r=await fetch(f.api+route,{method,headers:{'Content-Type':'application/json',clientid:f.clientId,'Idempotency-Key':crypto.randomUUID(),...(token?{Authorization:'Bearer '+token}:{})},body:body===undefined?undefined:JSON.stringify(body)});const b=await r.json();expect(r.status,b.msg).toBe(200);return b.data}
(async()=>{let browser;
try{
 browser=await chromium.launch();const c=await browser.newPage({viewport:{width:390,height:844}}),a=await browser.newPage({viewport:{width:1440,height:960}});
 for(const p of [c,a])p.on('pageerror',e=>report.errors.push(e.message));
 const go=async(name,query='')=>{const url='http://127.0.0.1:5173/#/pages/'+name+'/index'+query;if(c.url()===url)await c.reload();else await c.goto(url);await c.waitForLoadState('networkidle')};
 await check('baseline-login-dismiss-and-replay',async()=>{
  await go('me');await c.getByText('管理收藏',{exact:true}).click();await c.getByRole('button',{name:'关闭登录'}).click();await expect(c.getByRole('dialog')).toHaveCount(0);
  await c.getByText('管理收藏',{exact:true}).click();await c.locator('.login-sheet input:visible').fill(f.code);await c.getByText('已阅读并同意',{exact:true}).click();await c.getByText('登录合成用户',{exact:true}).click();await expect(c).toHaveURL(/favorites/);
 });
 // uni storage serialization differs across H5 versions; read through its public API.
 const ct=await c.evaluate(()=>window.uni.getStorageSync('yunji.customer.token'));
 await check('baseline-home-hot-two-column-live-summary',async()=>{await go('index');await expect(c.locator('.feed-column')).toHaveCount(2);await expect(c.locator('.feed-card-title')).toHaveCount(6);await expect(c.locator('.update-strip')).toContainText('您的关注更新了');await c.screenshot({path:path.join(root,'reports/browser/product-baseline-home.png'),fullPage:true})});
 await check('baseline-search-delete-single-history',async()=>{
  await go('search');await c.locator('.search-row input').fill('演示');await c.getByRole('button',{name:'搜索',exact:true}).click();await expect(c.locator('.result-card')).toHaveCount(6);
  await go('search');await c.getByRole('button',{name:'删除搜索记录演示',exact:true}).click();await expect(c.getByRole('button',{name:'删除搜索记录演示',exact:true})).toHaveCount(0);
 });
 await check('baseline-filter-groups-by-board',async()=>{
  await c.getByRole('button',{name:'筛选',exact:true}).click();const sheet=c.locator('.filter-sheet');await sheet.getByRole('button',{name:f.category.name,exact:true}).click();await expect(sheet.getByRole('button',{name:f.label.name,exact:true})).toBeVisible();await expect(sheet).not.toContainText('学校与组织');await sheet.getByRole('button',{name:'关闭',exact:true}).click();
 });
 await check('baseline-follow-undo-retains-page-row',async()=>{
  await go('follows');await c.locator('.follow-board .group-header').first().click();await c.locator('.follow-label .group-header').first().click();
  await c.getByRole('button',{name:'取消关注',exact:true}).first().click();await c.getByText('保留提醒',{exact:true}).click();await expect(c.getByRole('button',{name:'撤销取消',exact:true})).toHaveCount(1);
  const undo=c.getByRole('button',{name:'撤销取消',exact:true});
  // uni-button is a custom H5 element. Its label changes before the preceding
  // refresh releases busy; Playwright's native disabled check cannot detect it.
  await expect.poll(()=>undo.evaluate(el=>!el.hasAttribute('disabled')&&!el.classList.contains('uni-button-disabled'))).toBe(true);
  const restored=c.waitForResponse(r=>r.url().endsWith('/api/v1/me/follows/undo')&&r.request().method()==='POST');await undo.click();expect((await restored).status()).toBe(200);await expect(c.getByRole('button',{name:'撤销取消',exact:true})).toHaveCount(0);expect((await api('GET','/api/v1/me/follows',undefined,ct)).some(x=>x.targetId===f.keyword.id)).toBe(true);
 });
 await check('baseline-box-three-tiers-inline-grouping',async()=>{await go('subscriptions');await expect(c.locator('.segmented')).toContainText('推送信息');await c.getByRole('button',{name:'关注更新',exact:true}).click();await expect(c.locator('.exposure-card')).not.toHaveCount(0);await c.getByRole('button',{name:'按关注分组',exact:true}).click();await expect(c.getByRole('button',{name:'改为平铺',exact:true})).toBeVisible()});
 await check('baseline-reminders-no-delivery-ledger',async()=>{await go('notifications');for(const name of ['更替强提醒','关键词强提醒','及时提醒'])await expect(c.locator('.segmented')).toContainText(name);await expect(c.locator('.page')).not.toContainText('投递记录');await expect(c.locator('.page')).not.toContainText('发送失败')});
 await check('baseline-admin-real-login-single-role-menu',async()=>{await a.goto('http://127.0.0.1:5174/#/login');await a.getByLabel('账号',{exact:true}).fill('test1');await a.getByLabel('密码',{exact:true}).fill('666666');await a.getByRole('button',{name:'登录',exact:true}).click();await expect(a).toHaveURL(/articles/);await expect(a.locator('aside details')).toHaveCount(6);await a.goto('http://127.0.0.1:5174/#/system');await expect(a.locator('h1')).toHaveText('管理员账号')});
 let created;
 await check('baseline-admin-create-saves-and-publishes',async()=>{
  await a.goto('http://127.0.0.1:5174/#/articles');await a.getByRole('button',{name:'新建资讯',exact:true}).click();const form=a.locator('form.dialog');await form.getByLabel('标题',{exact:true}).fill('浏览器基线新资讯');await form.getByLabel('三级信息 · 列表一句话',{exact:true}).fill('浏览器真实录入摘要');await form.getByLabel('二级信息 · 正文',{exact:true}).fill('<p>浏览器真实录入正文</p>');await form.getByLabel('一级信息 · 原文',{exact:true}).fill('浏览器原文');await form.getByLabel('板块',{exact:true}).selectOption(f.category.id);await form.getByLabel('标签 · 至少一个',{exact:true}).selectOption([f.label.id]);await form.getByLabel('新增关键词 · 逗号或换行分隔',{exact:true}).fill('浏览器关键词');await form.getByLabel('热点天数',{exact:true}).fill('7');
  const response=a.waitForResponse(r=>r.url().endsWith('/api/v1/admin/articles')&&r.request().method()==='POST');await form.getByRole('button',{name:'保存并发布',exact:true}).click();created=(await (await response).json()).data;await expect(a.locator('form.dialog')).toHaveCount(0);expect((await api('GET','/api/v1/articles/'+created.id)).article.title).toBe('浏览器基线新资讯');
 });
 await check('baseline-admin-import-text-partial-receipt',async()=>{
  await a.getByRole('button',{name:'批量导入',exact:true}).click();const modal=a.locator('.dialog');await modal.getByRole('button',{name:'粘贴JSON',exact:true}).click();await modal.locator('textarea').fill(JSON.stringify([{title:'浏览器批量导入',contentType:'BASIC',categoryName:f.category.name,labelNames:[f.label.name],keywordNames:['浏览器导入词'],levelOneContent:'原文',levelTwoContent:'<p>正文</p>',levelThreeContent:'摘要',hotDays:7},{title:'预期失败',contentType:'BASIC',categoryName:'不存在',labelNames:['不存在'],keywordNames:['测试']}]));await modal.getByRole('button',{name:'开始导入',exact:true}).click();await expect(modal.getByRole('status')).toContainText('成功 1 · 失败 1');await expect(modal).toContainText('板块不存在');await modal.getByRole('button',{name:'关闭',exact:true}).click();
 });
 await check('baseline-admin-global-file-upload-ui',async()=>{
  await a.goto('http://127.0.0.1:5174/#/file-library');await a.getByRole('button',{name:'上传新文件',exact:true}).click();const form=a.locator('form.dialog');await form.getByLabel('名称',{exact:true}).fill('浏览器全局文件');await form.locator('input[type=file]').setInputFiles({name:'browser.txt',mimeType:'text/plain',buffer:Buffer.from('browser synthetic download')});await form.getByRole('button',{name:'保存',exact:true}).click();await expect(a.locator('form.dialog')).toHaveCount(0);await expect(a.getByRole('row').filter({hasText:'浏览器全局文件'})).toBeVisible();
 });
 await check('baseline-activity-file-download-client-confirmed',async()=>{
  await go('activity','?id='+f.activity.id);await expect(c.getByText('活动下载',{exact:true})).toBeVisible();const done=c.waitForResponse(r=>r.url().endsWith('/api/v1/me/library-downloads/complete'));const download=c.waitForEvent('download');await c.getByRole('button',{name:'下载当前版本',exact:true}).click();await download;expect((await done).status()).toBe(200);await expect(c.getByText('当前 v1 · 下载 1 次',{exact:true})).toBeVisible();
 });
 await check('baseline-admin-analytics-and-user-timeline',async()=>{
  await a.goto('http://127.0.0.1:5174/#/analytics/content');const downloads=a.locator('.panel').filter({has:a.getByRole('heading',{name:'成功下载',exact:true})});await expect(downloads).toBeVisible();const row=downloads.getByRole('row').filter({hasText:f.file.name});await expect(row).toHaveCount(1);await expect(row.getByRole('cell').nth(2)).toHaveText('1');await a.goto('http://127.0.0.1:5174/#/customers');await a.getByRole('button',{name:'查看活跃明细',exact:true}).first().click();await expect(a.getByRole('heading').filter({hasText:'近期活动'})).toBeVisible();
 });
 await check('baseline-browser-no-runtime-errors',async()=>expect(report.errors).toEqual([]));
 await a.screenshot({path:path.join(root,'reports/browser/product-baseline-admin.png'),fullPage:true});
}catch(e){report.fatal=e.message;console.error(e);process.exitCode=1}finally{if(browser)await browser.close();report.passed=report.cases.length;report.failed=report.fatal?1:0;fs.writeFileSync(path.join(root,'reports/browser-product-baseline.json'),JSON.stringify(report,null,2))}
})()
