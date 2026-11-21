const { chromium, expect: baseExpect } = require('@playwright/test');
const expect = baseExpect.configure({timeout:15000});
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');

(async () => {
 const root=path.resolve(__dirname,'..'), fixtures=JSON.parse(fs.readFileSync(process.env.YUNJI_BROWSER_FIXTURES,'utf8'));
 const report={scope:'LOCAL_CLOSURE_UI',startedAt:new Date().toISOString(),cases:[],errors:[]};let browser;
 async function check(id,fn){try{await fn();report.cases.push({id,status:'PASS'})}catch(e){report.cases.push({id,status:'FAIL',error:e.message});throw e}}
 async function api(method,route,body,token){const response=await fetch('http://127.0.0.1:18088'+route,{method,headers:{'Content-Type':'application/json',clientid:'e5cd7e4891bf95d1d19206ce24a7b32e','Idempotency-Key':crypto.randomUUID().replaceAll('-',''),...(token?{Authorization:'Bearer '+token}:{})},body:body?JSON.stringify(body):undefined});const result=await response.json();expect(response.status).toBe(200);expect(result.code).toBe(200);return result.data}
 try {
  browser=await chromium.launch({headless:true});const context=await browser.newContext({viewport:{width:390,height:844},acceptDownloads:true});const client=await context.newPage();client.on('pageerror',e=>report.errors.push(e.message));
  await check('client-login-direct-entry-can-return-to-feed',async()=>{
   await client.goto('http://127.0.0.1:5173/#/pages/login/index');await client.locator('.login-back').click();
   await expect(client.locator('[data-testid="feed-title"]')).toBeVisible();
  });
  await check('client-guest-subscriptions-login-return-does-not-loop',async()=>{
   await client.goto('http://127.0.0.1:5173/#/pages/subscriptions/index');await expect(client.locator('.guest-card')).toBeVisible();
   await client.locator('.guest-card:visible').getByText('登录',{exact:true}).click();await expect(client.getByRole('dialog',{name:'登录云迹'})).toBeVisible();
   await client.getByRole('button',{name:'关闭登录'}).click();await expect(client.locator('.guest-card')).toBeVisible();
  });
  await check('client-guest-notifications-login-return-does-not-loop',async()=>{
   await client.goto('http://127.0.0.1:5173/#/pages/notifications/index');await expect(client.locator('.guest-card')).toBeVisible();
   await client.locator('.guest-card:visible').getByText('登录',{exact:true}).click();await expect(client.getByRole('dialog',{name:'登录云迹'})).toBeVisible();
   await client.getByRole('button',{name:'关闭登录'}).click();await expect(client.locator('.guest-card')).toBeVisible();
  });
  await check('client-school-filter-removed-mobile',async()=>{
await client.goto('http://127.0.0.1:5173/#/pages/search/index');await client.getByText('筛选',{exact:true}).click();await expect(client.getByTestId('organization-filter')).toHaveCount(0);await expect(client.locator('.filter-sheet')).not.toContainText('学校与组织');await client.getByText('关闭',{exact:true}).click();
});
  await check('client-school-filter-removed-desktop',async()=>{
await client.setViewportSize({width:980,height:844});await client.reload();await client.getByText('筛选',{exact:true}).click();await expect(client.getByTestId('organization-filter')).toHaveCount(0);await expect(client.locator('.filter-sheet')).toContainText('普通信息');await client.getByText('关闭',{exact:true}).click();await client.setViewportSize({width:390,height:844});
});
  const token=(await api('POST','/auth/login',{username:'admin',password:'admin123',clientId:'e5cd7e4891bf95d1d19206ce24a7b32e',tenantId:'000000',grantType:'password'})).access_token;
  const categoryId=(await api('GET','/api/v1/catalog/categories',undefined,token))[0].id;
  await client.goto('http://127.0.0.1:5173/#/pages/login/index');await client.locator('input:visible').first().fill(fixtures.code);await client.locator('input:visible').first().blur();await client.getByText('已阅读并同意',{exact:true}).click();await client.getByText('登录合成用户',{exact:true}).click();await expect(client.locator('[data-testid="feed-title"]')).toBeVisible();
  async function page(name,query=''){await client.goto('http://127.0.0.1:5173/#/pages/'+name+'/index'+query);await client.reload();await client.waitForLoadState('networkidle')}
  async function publish(a){for(const action of ['SUBMIT','APPROVE','PUBLISH'])a=await api('POST','/api/v1/admin/articles/'+a.id+'/transitions',{action,versionId:a.latestVersionId,expectedVersion:a.lockVersion},token);return a}
  const automatic=[];client.on('request',r=>{if(r.url().endsWith('/api/v1/me/events')&&r.method()==='POST')automatic.push(r.postDataJSON())});
  await check('client-visible-exposure-offscreen-behavior',async()=>{
   await page('index');const cards=client.locator('.exposure-card');await expect.poll(()=>cards.count()).toBeGreaterThan(0);
   const card=cards.last(),id=await card.getAttribute('data-article-id');
   await expect.poll(()=>automatic.filter(e=>e.type==='VISIBLE_EXPOSURE').length).toBeGreaterThan(0);
   const box=await card.boundingBox();if(box.y>=844)expect(automatic.some(e=>e.type==='VISIBLE_EXPOSURE'&&e.objectId===id)).toBe(false);
   await card.scrollIntoViewIfNeeded();await expect.poll(()=>automatic.some(e=>e.type==='VISIBLE_EXPOSURE'&&e.objectId===id)).toBe(true);
  });
  await check('client-hidden-stay-time-excluded-h5-lifecycle',async()=>{
   await page('article','?id='+fixtures.articleId);automatic.length=0;
   await client.waitForTimeout(1800);
   await client.evaluate(()=>{Object.defineProperty(document,'hidden',{configurable:true,value:true});document.dispatchEvent(new Event('visibilitychange'))});
   await client.waitForTimeout(5000);
   await client.evaluate(()=>{Object.defineProperty(document,'hidden',{configurable:true,value:false});document.dispatchEvent(new Event('visibilitychange'))});
   await client.waitForTimeout(1500);expect(automatic.filter(e=>e.type==='VISIBLE_EXPOSURE')).toHaveLength(0);await client.getByRole('button',{name:'展开操作栏'}).click();await client.locator('.rail-button').filter({hasText:'强提醒'}).click();await client.getByText('再想想',{exact:true}).click();await page('notifications');
   report.stayObserved=automatic;await expect.poll(()=>automatic.filter(e=>e.type==='PAGE_STAY').length).toBeGreaterThan(0);
   const duration=automatic.find(e=>e.type==='PAGE_STAY').payload.durationMs;expect(duration).toBeGreaterThanOrEqual(3000);expect(duration).toBeLessThan(5000);
  });
  await check('client-reminder-edit-pause-resume-real-ui',async()=>{
await page('notification-edit','?articleId='+fixtures.articleId);await client.getByText('保存提醒',{exact:true}).click();await expect(client.getByText('保存提醒',{exact:true})).toHaveCount(0);const cards=client.locator('.reminder-card').filter({has:client.locator('.rule-actions')}).filter({hasText:'页面验收合成资讯'});await expect(cards).toHaveCount(1);if(await cards.first().getByText('移除强提醒',{exact:true}).count())await cards.first().getByText('移除强提醒',{exact:true}).click();await cards.first().getByText('恢复强提醒',{exact:true}).click();await expect(cards.first().getByText('移除强提醒',{exact:true})).toBeVisible();await cards.first().getByText('移除强提醒',{exact:true}).click();await expect(cards.first().getByText('恢复强提醒',{exact:true})).toBeVisible();
});
  await check('client-exact-version-conflict-refresh-real-ui',async()=>{
await page('article','?id='+fixtures.articleId);await expect(client.getByText('页面验收正文',{exact:true})).toBeVisible();await expect(client.getByText('已知悉 / 标记已读',{exact:true})).toHaveCount(0);const customerToken=await client.evaluate(()=>uni.getStorageSync('yunji.customer.token'));const old=(await api('GET','/api/v1/articles/'+fixtures.articleId)).versionId;
let a=await api('GET','/api/v1/admin/articles/'+fixtures.articleId,undefined,token);a=await api('POST','/api/v1/admin/articles/'+a.id+'/versions',{title:'页面验收合成资讯新版本',richText:'<p>新正文必须重新阅读</p>',contentType:'BASIC',categoryIds:[categoryId],expectedVersion:a.lockVersion},token);await publish(a);
const response=await fetch('http://127.0.0.1:18088/api/v1/me/read/'+fixtures.articleId,{method:'PUT',headers:{'Content-Type':'application/json',Authorization:'Bearer '+customerToken,'Idempotency-Key':crypto.randomUUID()},body:JSON.stringify({versionId:old})});expect(response.status).toBe(409);await page('article','?id='+fixtures.articleId);await expect(client.getByText('新正文必须重新阅读',{exact:true})).toBeVisible();
});
  await check('client-replacement-auto-follow-real-ui',async()=>{
   await client.getByRole('button',{name:'展开操作栏'}).click();const follow=client.locator('.rail-button').filter({hasText:'已关注'});if(await follow.count()){await follow.click();}
   await client.getByText('关注信息',{exact:true}).click();await expect(client.locator('.rail-button').filter({hasText:'已关注'})).toBeVisible();
   let target=await api('POST','/api/v1/admin/articles',{title:'新替换资讯',richText:'<p>替换后的正文</p>',contentType:'BASIC',categoryIds:[categoryId]},token);target=await publish(target);
   await api('POST','/api/v1/admin/articles/'+fixtures.articleId+'/replacement',{newArticleId:target.id},token);
   const hint=await api('GET','/api/v1/articles/'+fixtures.articleId+'/replacement',undefined,token);expect(hint.status).toBe('AVAILABLE');
   // The hash route is identical to the previous article check. Force a full
   // document reload so the client performs a fresh detail request after the
   // replacement transaction commits instead of reusing the mounted page.
   await page('article','?id='+fixtures.articleId);await client.reload();await client.waitForLoadState('networkidle');
   const detailResponse=await client.evaluate(async id=>{const r=await fetch('/api/v1/articles/'+id);return {status:r.status,body:await r.text()}},fixtures.articleId);report.replacementDetail=detailResponse;
   await expect.poll(()=>client.locator('body').innerText()).toContain('原资讯已撤下');await expect(client.getByText('新正文必须重新阅读',{exact:true})).toHaveCount(0);
   await page('subscriptions');await client.getByText('关注信息更替',{exact:true}).click();await expect(client.getByText('新资讯：新替换资讯',{exact:true}).first()).toBeVisible();await expect(client.getByText('确认转关注',{exact:true})).toHaveCount(0);
  });
  const desktop=await browser.newContext({acceptDownloads:true});const admin=await desktop.newPage();admin.on('pageerror',e=>report.errors.push(e.message));
  async function login(name,password){await admin.goto('http://127.0.0.1:5174/#/login');await admin.getByLabel('账号',{exact:true}).fill(name);await admin.getByLabel('密码',{exact:true}).fill(password);await admin.getByRole('button',{name:'登录',exact:true}).click();await expect(admin.getByRole('heading',{name:'资讯与审核',exact:true})).toBeVisible()}
  await check('creator-page-and-button-permissions-real-ui',async()=>{await login('test1','666666');await expect(admin.getByRole('button',{name:'新建资讯',exact:true})).toBeVisible();await expect(admin.getByRole('link',{name:'分类目录',exact:true})).toHaveCount(0);await admin.goto('http://127.0.0.1:5174/#/categories');await expect(admin.getByRole('alert')).toHaveText('当前账号没有此页面的操作权限。');await expect(admin.getByRole('button',{name:'新建',exact:true})).toHaveCount(0)});
  await check('reviewer-cannot-create-real-ui',async()=>{await login('test','666666');await expect(admin.getByRole('button',{name:'新建资讯',exact:true})).toHaveCount(0);await admin.locator('aside summary').filter({hasText:'活动与资料'}).click();await expect(admin.getByRole('link',{name:'材料评审',exact:true})).toBeVisible();await admin.getByRole('link',{name:'材料评审',exact:true}).click();await expect(admin.getByRole('alert').filter({hasText:'当前账号没有此页面的操作权限。'})).toHaveCount(0);await expect(admin.getByRole('heading',{name:'材料评审',exact:true})).toBeVisible()});
  await check('admin-audit-export-download-real-ui',async()=>{await login('admin','admin123');await admin.goto('http://127.0.0.1:5174/#/audit');const waiting=admin.waitForEvent('download');await admin.getByRole('button',{name:'导出当前列表',exact:true}).click();const file=await waiting;const saved=path.join(root,'reports/browser/audit-export.csv');await file.saveAs(saved);expect(fs.readFileSync(saved,'utf8')).toContain('HTTP_WRITE')});
  await check('admin-content-replacement-real-ui',async()=>{
   let from=await api('POST','/api/v1/admin/articles',{title:'后台替换原资讯',richText:'<p>原正文</p>',contentType:'BASIC',categoryIds:[categoryId]},token);from=await publish(from);
   let to=await api('POST','/api/v1/admin/articles',{title:'后台替换目标资讯',richText:'<p>新正文</p>',contentType:'BASIC',categoryIds:[categoryId]},token);to=await publish(to);
   await admin.goto('http://127.0.0.1:5174/#/articles');await admin.getByRole('button').filter({hasText:'后台替换原资讯'}).click();await admin.getByRole('button',{name:'替换资讯',exact:true}).click();
   await admin.getByLabel('替换目标',{exact:true}).selectOption(to.id);admin.once('dialog',d=>d.accept());await admin.getByRole('button',{name:'确认替换',exact:true}).click();
   await expect(admin.getByRole('button',{name:'确认替换',exact:true})).toHaveCount(0);expect((await api('GET','/api/v1/admin/articles/'+from.id,undefined,token)).status).toBe('OFFLINE');
  });
  await check('admin-activity-participant-export-real-ui',async()=>{await admin.goto('http://127.0.0.1:5174/#/activities');await admin.getByRole('row').filter({hasText:'页面验收合成活动'}).getByRole('button',{name:'查看',exact:true}).click();const waiting=admin.waitForEvent('download');await admin.getByRole('button',{name:'导出参与者名单',exact:true}).click();const file=await waiting;expect(file.suggestedFilename()).toBe('activity-registrations.csv')});
  await check('closure-no-runtime-errors',async()=>expect(report.errors).toEqual([]));
 }catch(e){report.fatal=e.message;report.bodyOnFailure=browser?.contexts().flatMap(c=>c.pages()).map(p=>({url:p.url(),body:p.locator('body').innerText().catch(()=>Promise.resolve(''))}));process.exitCode=1}
 finally{if(browser)await browser.close();report.completedAt=new Date().toISOString();report.passed=report.cases.filter(c=>c.status==='PASS').length;report.failed=report.cases.filter(c=>c.status==='FAIL').length;fs.writeFileSync(path.join(root,'reports/browser-closure.json'),JSON.stringify(report,null,2));console.log(JSON.stringify({passed:report.passed,failed:report.failed,fatal:report.fatal}))}
})();
