const { chromium, expect: baseExpect } = require('@playwright/test');
const expect = baseExpect.configure({timeout:15000});
const fs = require('node:fs');
const path = require('node:path');

(async()=>{
 const root=path.resolve(__dirname,'..'),out=path.join(root,'reports','browser');
 const fixtures=JSON.parse(fs.readFileSync(process.env.YUNJI_BROWSER_FIXTURES,'utf8'));
 const report={scope:'LOCAL_REAL_ADMIN_AND_CUSTOMER_UI',startedAt:new Date().toISOString(),cases:[],errors:[]};
 let browser;
 async function check(id,fn){try{await fn();report.cases.push({id,status:'PASS'});console.log(id,'PASS')}catch(e){report.cases.push({id,status:'FAIL',error:e.message});throw e}}
 try{
  browser=await chromium.launch({headless:true});
  const mobile=await browser.newContext({viewport:{width:390,height:844},acceptDownloads:true});
  const client=await mobile.newPage();client.on('pageerror',e=>report.errors.push({page:'client',error:e.message}));report.networkFailures=[];client.on('requestfailed',r=>report.networkFailures.push({method:r.method(),path:new URL(r.url()).pathname,error:r.failure()?.errorText}));
  client.on('request',r=>{if(r.url().endsWith('/api/v1/auth/customer/local')){const body=r.postDataJSON();report.loginRequest={matchesIssuedCode:body.code===fixtures.code,codeLength:body.code?.length,expectedLength:fixtures.code.length,appId:body.appId}}});
  // These are independent deep-link checks. Hash-only goto can reuse a retained
  // uni-app page instead of exercising a new deep-link load; UI clicks below
  // still verify navigation within each scenario.
  async function customer(page,query=''){await client.goto(`http://127.0.0.1:5173/#/pages/${page}/index${query}`);await client.reload();await client.waitForLoadState('networkidle')}
  const click=async text=>{const target=client.getByText(text,{exact:true});await expect(target).not.toHaveClass(/uni-button-disabled/);await target.click()};
  async function enter(locator,value){await locator.fill(value);await locator.blur();await client.waitForTimeout(200)}
  await check('customer-local-code-login-real-ui',async()=>{
   await customer('login');const loginInput=client.locator('input:visible').first();await enter(loginInput,fixtures.code);await expect(loginInput).toHaveValue(fixtures.code);await click('已阅读并同意');await click('登录合成用户');
   await expect(client.locator('[data-testid="feed-title"]')).toBeVisible();
  });
  await check('customer-profile-edit-real-ui',async()=>{
   await customer('profile');await expect(client.getByText('保存资料',{exact:true})).toBeVisible();
   await enter(client.locator('input:visible').first(),'页面验收用户');await click('保存资料');await expect(client.getByText('资料已保存',{exact:true})).toBeVisible();
   await customer('profile');await expect(client.locator('input:visible').first()).toHaveValue('页面验收用户');
  });
  await check('customer-article-favorite-follow-read-real-ui',async()=>{
   await customer('article','?id='+fixtures.articleId);await expect(client.getByText('页面验收正文',{exact:true})).toBeVisible();
   await client.getByRole('button',{name:'展开操作栏'}).click();await click('收藏');await click('确认收藏');await expect(client.locator('.rail-button').filter({hasText:'已收藏'})).toBeVisible();
   await click('关注信息');await expect(client.locator('.rail-button').filter({hasText:'已关注'})).toBeVisible();await expect(client.getByText('已知悉 / 标记已读',{exact:true})).toHaveCount(0);
  });
  await check('customer-subscription-hide-unhide-real-ui',async()=>{
   await customer('subscriptions');await client.getByText('关注更新',{exact:true}).click();await expect(client.getByText('页面验收合成资讯',{exact:true}).first()).toBeVisible();
   await click('隐藏盒子');await expect(client.getByText('取消隐藏',{exact:true})).toHaveCount(0);
   await click('显示隐藏盒子');await click('取消隐藏');await expect(client.getByText('隐藏盒子',{exact:true})).toBeVisible();
  });
  await check('customer-favorites-and-history-real-ui',async()=>{
   await customer('favorites');await expect(client.getByText('页面验收合成资讯',{exact:true})).toBeVisible();
   await customer('history');await expect(client.getByText('页面验收合成资讯',{exact:true})).toBeVisible();await click('清空浏览历史');await click('确定');
   await expect(client.getByText('页面验收合成资讯',{exact:true})).toHaveCount(0);
  });
  await check('customer-activity-register-cancel-real-ui',async()=>{
   await customer('activity','?id='+fixtures.activityId);await enter(client.locator('input:visible'),'合成报名学生');await click('提交报名');
   await expect(client.getByText('报名已确认',{exact:true})).toBeVisible();await click('取消报名');
   await expect(client.getByText('填写报名信息',{exact:true})).toBeVisible();
  });
  await check('customer-review-upload-draft-submit-real-ui',async()=>{
   await customer('review','?id='+fixtures.campaignId);const chooser=client.waitForEvent('filechooser');await click('选择材料');
   await (await chooser).setFiles({name:'proof.txt',mimeType:'text/plain',buffer:Buffer.from('合成浏览器材料')});
   await expect(client.getByText('已选 proof.txt',{exact:true})).toBeVisible();await click('保存新草稿');
   await expect(client.getByText('提交申报',{exact:true})).toBeVisible();await click('提交申报');
   await expect(client.getByText('SUBMITTED',{exact:true})).toBeVisible();
  });
  await check('customer-resource-file-browser-download',async()=>{
   await customer('resource');await click('页面验收合成资料包');const download=client.waitForEvent('download');await click('下载文件');
   const file=await download;const saved=path.join(out,'resource-download.txt');await file.saveAs(saved);
   expect(fs.readFileSync(saved,'utf8')).toBe('new synthetic browser resource');
  });
  await check('customer-reminder-create-pause-real-ui',async()=>{
   await customer('notifications','?articleId='+fixtures.articleId);
   const keys=[];await client.route('**/api/v1/me/reminders/rules',async route=>{if(route.request().method()!=='POST'){await route.continue();return}keys.push(route.request().headers()['idempotency-key']);const response=await route.fetch();if(keys.length===1)await route.abort('failed');else await route.fulfill({response})});
   await click('保存提醒');await expect(client.getByText('移除强提醒',{exact:true})).toBeVisible();expect(keys.length).toBe(2);expect(keys[0]).toBe(keys[1]);await client.unroute('**/api/v1/me/reminders/rules');await expect(client.getByText('移除强提醒',{exact:true})).toHaveCount(1);await click('移除强提醒');
   await expect(client.getByText('恢复强提醒',{exact:true})).toBeVisible();
  });
  await check('customer-consent-real-ui',async()=>{await customer('page','?key=terms');await click('同意此版本');await expect(client.getByText('同意记录已保存',{exact:true})).toBeVisible()});
  await check('customer-feedback-real-ui',async()=>{await customer('feedback');await enter(client.locator('textarea:visible').first(),'合成浏览器反馈');await click('提交反馈');await expect(client.getByText('反馈已提交',{exact:true})).toBeVisible()});
  await check('customer-mobile-no-horizontal-overflow',async()=>{
   for(const page of ['index','subscriptions','services','resource','me']){await customer(page);expect(await client.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth+1)).toBe(true)}
   await client.screenshot({path:path.join(out,'client-mobile-me.png'),fullPage:true});
  });
  const desktop=await browser.newContext({acceptDownloads:true});const admin=await desktop.newPage();
  admin.on('pageerror',e=>report.errors.push({page:'admin',error:e.message}));
  await check('admin-login-extended-real-ui',async()=>{await admin.goto('http://127.0.0.1:5174/#/login');await admin.getByLabel('账号',{exact:true}).fill('admin');await admin.getByLabel('密码',{exact:true}).fill('admin123');await admin.getByRole('button',{name:'登录',exact:true}).click();await expect(admin.getByRole('heading',{name:'资讯与审核',exact:true})).toBeVisible()});
  const sections={categories:'板块管理',organizations:'学校与组织',keywords:'关键词治理',activities:'活动与报名','review-campaigns':'评审项目','review-applications':'材料评审',packages:'资料包',subjects:'科目',dimensions:'年级与专业',exams:'考试日程',artifacts:'插件发布',files:'文件治理',notifications:'提醒账本',banners:'首页轮播',recommendations:'首页推荐位',pages:'协议管理',columns:'活动栏目',reports:'内容举报',feedback:'用户反馈',analytics:'内容效果',audit:'操作审计',system:'系统管理'};
  for(const [section,title] of Object.entries(sections))await check('admin-'+section+'-page-real-api',async()=>{
   const response=admin.waitForResponse(r=>r.url().includes(section==='system'?'/system/user/list':section==='columns'?'/api/v1/home':section==='reports'?'/api/v1/admin/moderation/reports':section==='feedback'?'/api/v1/admin/moderation/feedback':section==='subjects'?'/api/v1/admin/resources/subjects':section==='dimensions'?'/api/v1/admin/resources/dimensions':section==='exams'?'/api/v1/admin/resources/exams':section==='pages'?'/api/v1/admin/operation/pages':section==='banners'?'/api/v1/admin/operation/banners':section==='recommendations'?'/api/v1/admin/operation/recommendations':'/api/v1/admin/')&&r.request().method()==='GET'&&r.url().includes('5174'));
   await admin.goto('http://127.0.0.1:5174/#/'+section);await expect(admin.getByRole('heading',{name:title,exact:true})).toBeVisible();expect((await response).status()).toBe(200);await expect(admin.locator('p.error').first()).toHaveText('');
  });
  await check('admin-package-edit-prefill-real-ui',async()=>{
   await admin.goto('http://127.0.0.1:5174/#/packages');await admin.getByRole('row').filter({hasText:'页面验收合成资料包'}).getByRole('button',{name:'编辑',exact:true}).click();
   await expect(admin.getByLabel('类型',{exact:true})).toHaveValue('GENERAL');
   await expect(admin.getByLabel('文件',{exact:true})).toHaveValues([fixtures.fileId]);await admin.getByLabel('标题',{exact:true}).fill('页面验收资料包编辑');await admin.getByRole('button',{name:'保存',exact:true}).click();await expect(admin.getByText('页面验收资料包编辑',{exact:true})).toBeVisible();
  });
  await check('admin-review-campaign-edit-preserves-material-rules',async()=>{
   await admin.goto('http://127.0.0.1:5174/#/review-campaigns');await admin.getByRole('row').filter({hasText:'页面验收合成评审'}).getByRole('button',{name:'编辑',exact:true}).click();
   await expect(admin.getByLabel('显示名称',{exact:true})).toHaveValue('证明材料');await admin.getByLabel('标题',{exact:true}).fill('页面验收合成评审更新');await admin.getByRole('button',{name:'保存',exact:true}).click();await expect(admin.getByText('页面验收合成评审更新',{exact:true})).toBeVisible();
  });
  await admin.goto('http://127.0.0.1:5174/#/system');
  const systemRow=text=>admin.getByRole('row').filter({hasText:text});
  async function tab(name){await admin.getByRole('button',{name,exact:true}).click();await expect(admin.getByRole('button',{name:'新建'+name,exact:true})).toBeVisible()}
  async function saved(kind,text){await admin.getByRole('button',{name:'保存'+kind,exact:true}).click();await expect(admin.getByRole('heading',{name:'新建'+kind,exact:true})).toHaveCount(0);await expect(systemRow(text)).toBeVisible()}
  await check('admin-system-department-create-real-ui',async()=>{
   await tab('部门');await admin.getByRole('button',{name:'新建部门',exact:true}).click();await admin.getByLabel('部门名称',{exact:true}).fill('页面合成部门');await admin.getByLabel('上级部门',{exact:true}).selectOption({label:'研发部门'});await saved('部门','页面合成部门');
  });
  await check('admin-system-menu-create-real-ui',async()=>{
   await tab('菜单');await admin.getByRole('button',{name:'新建菜单',exact:true}).click();await admin.getByLabel('菜单名称',{exact:true}).fill('页面合成权限');await admin.getByLabel('权限标识',{exact:true}).fill('yunji:synthetic:browser');await saved('菜单','页面合成权限');
  });
  await check('admin-system-role-create-edit-permissions-real-ui',async()=>{
   await tab('角色');await admin.getByRole('button',{name:'新建角色',exact:true}).click();await expect.poll(async()=>await admin.getByLabel('菜单与操作权限',{exact:true}).locator('option').count()).toBeGreaterThan(0);await admin.getByLabel('角色名称',{exact:true}).fill('页面合成角色');await admin.getByLabel('角色标识',{exact:true}).fill('browser_synthetic');await admin.getByLabel('菜单与操作权限',{exact:true}).selectOption({label:'页面合成权限 · yunji:synthetic:browser'});await saved('角色','页面合成角色');
   await systemRow('页面合成角色').getByRole('button',{name:'编辑',exact:true}).click();await expect(admin.getByLabel('菜单与操作权限',{exact:true}).locator('option:checked')).toHaveText(['页面合成权限 · yunji:synthetic:browser']);await admin.getByLabel('框架数据范围',{exact:true}).selectOption('2');await admin.getByLabel('可访问部门',{exact:true}).selectOption({label:'页面合成部门'});await admin.getByRole('button',{name:'保存角色',exact:true}).click();await expect(admin.getByRole('heading',{name:'编辑角色',exact:true})).toHaveCount(0);
  });
  await check('admin-system-user-create-edit-role-real-ui',async()=>{
   await tab('用户');await admin.getByRole('button',{name:'新建用户',exact:true}).click();await admin.getByLabel('工作人员账号',{exact:true}).fill('browser_synthetic');await admin.getByLabel('工作人员姓名',{exact:true}).fill('页面合成工作人员');await admin.getByLabel('初始口令',{exact:true}).fill('Synthetic!23456789');await admin.getByLabel('所属部门',{exact:true}).selectOption({label:'页面合成部门'});await admin.getByLabel('工作人员角色',{exact:true}).selectOption({label:'页面合成角色 · browser_synthetic'});await saved('用户','browser_synthetic');
   await systemRow('browser_synthetic').getByRole('button',{name:'编辑',exact:true}).click();await expect(admin.getByLabel('工作人员角色',{exact:true}).locator('option:checked')).toHaveText(['页面合成角色 · browser_synthetic']);await admin.getByLabel('工作人员姓名',{exact:true}).fill('页面合成工作人员更新');await admin.getByRole('button',{name:'保存用户',exact:true}).click();await expect(admin.getByRole('heading',{name:'编辑用户',exact:true})).toHaveCount(0);await expect(systemRow('页面合成工作人员更新')).toBeVisible();
  });
  await check('admin-system-user-business-scope-real-ui',async()=>{
   await systemRow('browser_synthetic').getByRole('button',{name:'业务授权',exact:true}).click();await admin.getByLabel('资讯部门范围',{exact:true}).selectOption({label:'页面合成部门'});await admin.getByLabel('评审组织范围',{exact:true}).selectOption({label:'合成学院'});await admin.getByRole('button',{name:'保存业务授权',exact:true}).click();await expect(admin.getByRole('button',{name:'保存业务授权',exact:true})).toHaveCount(0);await systemRow('browser_synthetic').getByRole('button',{name:'业务授权',exact:true}).click();await expect(admin.getByLabel('资讯部门范围',{exact:true}).locator('option:checked')).toHaveText(['页面合成部门']);await expect(admin.getByLabel('评审组织范围',{exact:true}).locator('option:checked')).toHaveText(['合成学院']);await admin.locator('form').getByRole('button',{name:'关闭',exact:true}).click();
  });
  await check('admin-system-password-reset-real-ui',async()=>{
   await systemRow('browser_synthetic').getByRole('button',{name:'重置口令',exact:true}).click();await expect(admin.getByLabel('新的临时口令',{exact:true})).toHaveAttribute('type','password');await admin.getByLabel('新的临时口令',{exact:true}).fill('ResetSynthetic!2345');await admin.getByRole('button',{name:'确认重置口令',exact:true}).click();await expect(admin.getByText('口令已重置',{exact:true})).toBeVisible();
  });
  await check('admin-system-delete-unreferenced-synthetic-records',async()=>{
   admin.on('dialog',d=>d.accept());
   for(const [kind,name] of [['用户','browser_synthetic'],['角色','页面合成角色'],['菜单','页面合成权限'],['部门','页面合成部门']]){await tab(kind);await expect(systemRow(name)).toBeVisible();await systemRow(name).getByRole('button',{name:'删除',exact:true}).click();await expect(systemRow(name)).toHaveCount(0)}
  });
  await check('admin-resource-dimension-create-edit-withdraw-real-ui',async()=>{
   await admin.goto('http://127.0.0.1:5174/#/dimensions');await admin.getByRole('button',{name:'新建',exact:true}).click();await admin.getByLabel('代码',{exact:true}).fill('BROWSER_DIM');await admin.getByLabel('名称',{exact:true}).fill('页面合成年级');await admin.getByRole('button',{name:'保存',exact:true}).click();await expect(systemRow('页面合成年级')).toBeVisible();
   await systemRow('页面合成年级').getByRole('button',{name:'编辑',exact:true}).click();await admin.getByLabel('名称',{exact:true}).fill('页面合成年级更新');await admin.getByRole('button',{name:'保存',exact:true}).click();await expect(systemRow('页面合成年级更新')).toBeVisible();await systemRow('页面合成年级更新').getByRole('button',{name:'撤下',exact:true}).click();await expect(admin.locator('p.error').first()).toHaveText('');
   await systemRow('页面合成年级更新').getByRole('button',{name:'编辑',exact:true}).click();await expect(admin.getByLabel('状态',{exact:true})).toHaveValue('DISABLED');await admin.getByRole('button',{name:'关闭',exact:true}).click();
  });
  await check('admin-resource-subject-create-edit-withdraw-real-ui',async()=>{
   await admin.goto('http://127.0.0.1:5174/#/subjects');await admin.getByRole('button',{name:'新建',exact:true}).click();await admin.getByLabel('学校与组织',{exact:true}).selectOption({label:'合成学院'});await admin.getByLabel('科目代码',{exact:true}).fill('BROWSER_EDIT');await admin.getByLabel('名称',{exact:true}).fill('页面合成科目维护');await admin.getByRole('button',{name:'保存',exact:true}).click();await expect(systemRow('页面合成科目维护')).toBeVisible();
   await systemRow('页面合成科目维护').getByRole('button',{name:'编辑',exact:true}).click();await admin.getByLabel('名称',{exact:true}).fill('页面合成科目维护更新');await admin.getByRole('button',{name:'保存',exact:true}).click();await expect(systemRow('页面合成科目维护更新')).toBeVisible();await systemRow('页面合成科目维护更新').getByRole('button',{name:'撤下',exact:true}).click();await systemRow('页面合成科目维护更新').getByRole('button',{name:'编辑',exact:true}).click();await expect(admin.getByLabel('状态',{exact:true})).toHaveValue('DISABLED');await admin.getByRole('button',{name:'关闭',exact:true}).click();
  });
  await check('admin-resource-exam-create-edit-withdraw-real-ui',async()=>{
   await admin.goto('http://127.0.0.1:5174/#/exams');await admin.getByRole('button',{name:'新建',exact:true}).click();await admin.getByLabel('学校与组织',{exact:true}).selectOption({label:'合成学院'});await admin.getByLabel('科目',{exact:true}).selectOption({label:'页面验收合成科目'});await admin.getByLabel('考试时间',{exact:true}).fill('2027-01-01T09:00');await admin.getByLabel('来源',{exact:true}).fill('页面合成考试来源');await admin.getByRole('button',{name:'保存',exact:true}).click();await expect(systemRow('页面合成考试来源')).toBeVisible();
   await systemRow('页面合成考试来源').getByRole('button',{name:'编辑',exact:true}).click();await expect(admin.getByLabel('考试时间',{exact:true})).toHaveValue('2027-01-01T09:00');await admin.getByLabel('来源',{exact:true}).fill('页面合成考试来源更新');await admin.getByRole('button',{name:'保存',exact:true}).click();await expect(systemRow('页面合成考试来源更新')).toBeVisible();await systemRow('页面合成考试来源更新').getByRole('button',{name:'撤下',exact:true}).click();await systemRow('页面合成考试来源更新').getByRole('button',{name:'编辑',exact:true}).click();await expect(admin.getByLabel('状态',{exact:true})).toHaveValue('OFFLINE');await admin.getByRole('button',{name:'关闭',exact:true}).click();
  });
  await check('admin-resource-artifact-create-edit-withdraw-real-ui',async()=>{
   await admin.goto('http://127.0.0.1:5174/#/artifacts');await admin.getByRole('button',{name:'新建',exact:true}).click();await admin.getByLabel('名称',{exact:true}).fill('页面合成插件维护');await admin.getByRole('button',{name:'保存',exact:true}).click();await expect(systemRow('页面合成插件维护')).toBeVisible();await systemRow('页面合成插件维护').getByRole('button',{name:'编辑',exact:true}).click();await admin.getByLabel('名称',{exact:true}).fill('页面合成插件维护更新');await admin.getByRole('button',{name:'保存',exact:true}).click();await expect(systemRow('页面合成插件维护更新')).toBeVisible();await systemRow('页面合成插件维护更新').getByRole('button',{name:'撤下',exact:true}).click();await expect(systemRow('页面合成插件维护更新')).toContainText('OFFLINE');await systemRow('页面合成插件维护更新').getByRole('button',{name:'编辑',exact:true}).click();await expect(admin.getByLabel('状态',{exact:true})).toHaveValue('OFFLINE');await admin.getByRole('button',{name:'关闭',exact:true}).click();
  });
  await check('admin-review-decision-real-ui',async()=>{
   await admin.goto('http://127.0.0.1:5174/#/review-applications');await admin.getByRole('row').filter({hasText:'页面验收合成评审'}).getByRole('button',{name:'查看',exact:true}).click();
   await admin.getByLabel('决定',{exact:true}).selectOption('APPROVED');await admin.getByLabel('意见',{exact:true}).fill('已核实合成材料');await admin.getByRole('button',{name:'提交评审决定',exact:true}).click();await expect(admin.getByText('APPROVED · 已核实合成材料',{exact:true})).toBeVisible();
  });
  await check('customer-review-decision-visible-real-ui',async()=>{await customer('review','?id='+fixtures.campaignId);await expect(client.getByText('APPROVED',{exact:true}).first()).toBeVisible();await expect(client.getByText('已核实合成材料',{exact:true})).toBeVisible()});
  await check('client-independent-search-history-and-filter',async()=>{
   await customer('search');await enter(client.locator('input:visible').first(),'页面验收');await click('搜索');await expect(client.getByText('页面验收合成资讯',{exact:true})).toBeVisible();
   await click('筛选');await client.locator('.filter-sheet').getByText('通知',{exact:true}).click();await click('应用筛选');await expect(client.locator('.empty')).toContainText('暂无匹配内容');await expect(client.getByText('缺失信息申报',{exact:true})).toBeVisible();
   await customer('me');await client.goto('http://127.0.0.1:5173/?history-reentry='+Date.now()+'#/pages/search/index');await client.waitForLoadState('networkidle');await expect(client.getByText('页面验收',{exact:true})).toBeVisible();await click('清空');await expect(client.getByText('暂无搜索历史',{exact:true})).toBeVisible();
  });
  await check('admin-label-create-independent-form',async()=>{
   await admin.goto('http://127.0.0.1:5174/#/labels');await admin.getByRole('button',{name:'新建',exact:true}).click();await expect(admin).toHaveURL(/labels\/new/);
   await admin.getByLabel('名称',{exact:true}).fill('页面验收标签');const category=admin.getByLabel('归属板块',{exact:true});await category.selectOption(await category.locator('option').filter({hasNotText:'请选择'}).last().getAttribute('value'));
   await admin.getByRole('button',{name:'保存',exact:true}).click();await expect(admin.getByRole('row').filter({hasText:'页面验收标签'})).toBeVisible();
  });
  await check('client-native-tabs-boards-label-tree',async()=>{
   await customer('boards');await expect(client.locator('.uni-tabbar__label').filter({hasText:'板块'})).toBeVisible();await expect(client.getByText('选择信息分类，浏览相关内容',{exact:true})).toBeVisible();
   await customer('tag-tree');await client.locator('.card').filter({hasText:'已选'}).last().locator('.group-header').click();await expect(client.getByText('页面验收标签',{exact:true})).toBeVisible();const row=client.locator('.list-item').filter({hasText:'页面验收标签'});await row.getByText('选择',{exact:true}).click();await expect(row.getByText('已选择',{exact:true})).toBeVisible();
   await customer('subscriptions');await expect(client.locator('.box-card').filter({hasText:'页面验收标签'})).toBeVisible();
   await customer('follows');await click('信息');await expect(client.locator('.group-header').filter({hasText:'页面验收标签'})).toHaveCount(0);
  });
  await check('client-activity-favorite-overview',async()=>{
   await customer('activity-overview');const card=client.locator('.card').filter({hasText:'页面验收合成活动'});await card.getByText('收藏活动',{exact:true}).click();await click('收藏');await expect(card).toBeVisible();await card.getByText('取消收藏',{exact:true}).click();await expect(client.getByText('暂无收藏活动',{exact:true})).toBeVisible();
  });
  await check('client-reminder-independent-subpages',async()=>{
   await customer('notification-settings');await click('查看通道与授权');await expect(client.getByText('当前通道状态',{exact:true})).toBeVisible();await expect(client.getByText('本地模拟验收',{exact:true})).toBeVisible();
   await customer('notification-history');await expect(client.getByText('发送记录',{exact:true}).last()).toBeVisible();
  });
  for(const [route,title] of Object.entries({'customers':'客户用户','analytics/dashboard':'数据看板','analytics/content':'内容统计','analytics/behavior':'行为统计','analytics/profile':'用户概况','reminders/rules':'用户提醒规则','reminders/records':'推送记录','reminders/settings':'每日提醒设置','reminders/templates':'推送模板','reminders/statistics':'强提醒统计','reminders/monitor':'通道监控','reminders/wecom':'企微任务','foundation/post':'岗位管理','foundation/dict':'字典类型','foundation/config':'参数配置','foundation/notice':'通知公告','foundation/cache':'缓存监控','foundation/online':'在线工作人员','foundation/login':'登录日志','content/notices':'通知管理','content/history':'通知历史','content/replacements':'信息更替','content/strategy':'内容推荐策略'}))await check('new-admin-'+route.replace('/','-'),async()=>{
   await admin.goto('http://127.0.0.1:5174/#/'+route);await expect(admin.getByRole('heading',{name:title,exact:true})).toBeVisible();await admin.waitForLoadState('networkidle');await expect(admin.locator('p.error').first()).toHaveText('');
  });
  await check('client-new-pages-mobile-layout',async()=>{
   for(const name of ['boards','search','tag-tree','favorites','history','profile','notification-settings','notification-status','activity-overview']){await customer(name);expect(await client.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth+1)).toBe(true)}
   await customer('boards');await client.screenshot({path:path.join(out,'client-boards-v20.png'),fullPage:true});await customer('search');await client.screenshot({path:path.join(out,'client-search-v20.png'),fullPage:true});await admin.screenshot({path:path.join(out,'admin-v20.png'),fullPage:true});
  });
  await check('customer-logout-relogin-navigation',async()=>{await customer('profile');await click('退出登录');await expect(client.getByText('登录',{exact:true})).toBeVisible();await click('登录');await expect(client.getByRole('dialog',{name:'登录云迹'})).toBeVisible()});
  await check('extended-no-browser-runtime-errors',async()=>expect(report.errors).toEqual([]));
 }catch(e){report.fatal=e.message;process.exitCode=1;if(browser)for(const [i,context] of browser.contexts().entries())for(const [j,page] of context.pages().entries()){await page.screenshot({path:path.join(out,`extended-failure-${i}-${j}.png`),fullPage:true}).catch(()=>{});report.failurePage={url:page.url(),text:(await page.locator('body').innerText().catch(()=>'' )).slice(0,500)}}}
 finally{if(browser)await browser.close();report.passed=report.cases.filter(c=>c.status==='PASS').length;report.failed=report.cases.filter(c=>c.status==='FAIL').length;fs.writeFileSync(path.join(root,'reports/browser-extended.json'),JSON.stringify(report,null,2));console.log(JSON.stringify({passed:report.passed,failed:report.failed,fatal:report.fatal}))}
})();
