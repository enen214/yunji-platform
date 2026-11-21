const { chromium, expect: baseExpect } = require('@playwright/test');
const expect = baseExpect.configure({timeout:15000});
const fs = require('node:fs');
const path = require('node:path');

(async () => {
  const root = path.resolve(__dirname, '..');
  const out = path.join(root, 'reports', 'browser');
  fs.mkdirSync(out, { recursive: true });
  const report = { scope: 'LOCAL_BROWSER_CORE_FLOW', startedAt: new Date().toISOString(), cases: [], errors: [] };
  let browser;
  async function check(id, fn) {
    try { await fn(); report.cases.push({ id, status: 'PASS' }); }
    catch (e) { report.cases.push({ id, status: 'FAIL', error: e.message }); throw e; }
  }
  try {
    browser = await chromium.launch({ headless: true });
    const context = await browser.newContext();
    const admin = await context.newPage();
    admin.on('pageerror', e => report.errors.push({ page: 'admin', error: e.message }));
    await check('admin-password-login-ui', async () => {
      await admin.goto('http://127.0.0.1:5174/#/login');
      await admin.getByLabel('账号', { exact: true }).fill('admin');
      await admin.getByLabel('密码', { exact: true }).fill('admin123');
      await admin.getByRole('button', { name: '登录', exact: true }).click();
      await expect(admin.getByRole('heading', { name: '资讯与审核', exact: true })).toBeVisible();
    });
    const title = '浏览器闭环通知 ' + Date.now();
    await check('article-create-ui', async () => {
      await admin.getByRole('button', { name: '新建资讯', exact: true }).click();
      await admin.getByLabel('标题', { exact: true }).fill(title);
      await admin.getByLabel('摘要', { exact: true }).fill('通过真实页面创建并审核发布');
      await admin.getByLabel('正文', { exact: true }).fill('<p>浏览器验收正文</p>');
      await admin.getByLabel('来源名称', { exact: true }).fill('合成验收来源');
      await admin.getByLabel('热点 0-10', { exact: true }).fill('1');
      const category = admin.getByLabel('分类', { exact: true });
      const value = await category.locator('option:not([value=""])').first().getAttribute('value');
      await category.selectOption(value);
      await admin.getByRole('button', { name: '保存草稿', exact: true }).click();
      await expect(admin.getByRole('heading', { name: title, exact: true })).toBeVisible();
    });
    await check('article-review-publish-ui', async () => {
      await admin.getByRole('button', { name: '提交审核', exact: true }).click();
      await expect(admin.getByRole('button', { name: '通过', exact: true })).toBeVisible();
      await admin.getByRole('button', { name: '通过', exact: true }).click();
      await expect(admin.getByRole('button', { name: '发布此版本', exact: true })).toBeVisible();
      await admin.getByRole('button', { name: '发布此版本', exact: true }).click();
      await expect(admin.getByRole('button', { name: '下线', exact: true })).toBeVisible();
      await admin.screenshot({ path: path.join(out, 'admin-published.png'), fullPage: true });
    });
    const client = await context.newPage();
    client.on('pageerror', e => report.errors.push({ page: 'client', error: e.message }));
    await check('customer-list-ui', async () => {
      await client.goto('http://127.0.0.1:5173/');
      await expect(client.locator('.feed-card-title').filter({ hasText: title })).toBeVisible();
      await client.screenshot({ path: path.join(out, 'client-list.png'), fullPage: true });
    });
    await check('customer-detail-ui', async () => {
      await client.locator('.feed-card-title').filter({ hasText: title }).click();
      await expect(client.getByText('浏览器验收正文', { exact: true })).toBeVisible();
      await client.getByRole('button',{name:'展开操作栏'}).click();await expect(client.getByText('关注信息', { exact: true })).toBeVisible();
      await client.screenshot({ path: path.join(out, 'client-detail.png'), fullPage: true });
    });
    await check('no-browser-runtime-error', async () => { expect(report.errors).toEqual([]); });
  } catch (e) {
    report.fatal = e.message;
    process.exitCode = 1;
  } finally {
    if (browser) await browser.close();
    report.passed = report.cases.filter(c => c.status === 'PASS').length;
    report.failed = report.cases.filter(c => c.status === 'FAIL').length;
    fs.writeFileSync(path.join(root, 'reports', 'browser.json'), JSON.stringify(report, null, 2));
    console.log(JSON.stringify(report));
  }
})();
