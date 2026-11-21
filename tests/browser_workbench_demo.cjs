 const {chromium,expect}=require('@playwright/test');
const path=require('node:path');
(async()=>{
 const browser=await chromium.launch();
 try{
  const page=await browser.newPage({viewport:{width:390,height:844}}),errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto('http://127.0.0.1:5173/#/pages/index/index');
  await expect(page.locator('.feed-card-title')).toHaveCount(6);
  await expect(page.locator('.feature-banner')).toHaveCount(3);
  await expect.poll(()=>page.locator('.banner-image img').first().evaluate(el=>el.complete&&el.naturalWidth>0)).toBe(true);
  await page.screenshot({path:path.join(__dirname,'../reports/browser/workbench-demo-home.png'),fullPage:true});
  await page.locator('.feature-banner').first().click();await expect(page).toHaveURL(/activity-column/);await expect(page.locator('.topic-page .hero .title')).toContainText('验收演示');
  await page.locator('.topic-page .card-title').first().click();await expect(page).toHaveURL(/pages\/article\/index/);await expect(page.locator('.article-hero .title')).toContainText('验收演示');
  const articleId=new URLSearchParams(page.url().split('?')[1]).get('id');
  const detail=(await (await page.request.get('http://127.0.0.1:18088/api/v1/articles/'+articleId)).json()).data;
  await expect(page.locator('.taxonomy-row')).toHaveCount(3);
  await expect(page.locator('.taxonomy-row').nth(0)).toContainText(detail.categories[0].name);
  await expect(page.locator('.taxonomy-row').nth(1)).toContainText(detail.labels[0].name);
  await expect(page.locator('.taxonomy-row').nth(2)).toContainText(detail.keywords[0].name);
  await page.screenshot({path:path.join(__dirname,'../reports/browser/workbench-demo-detail.png'),fullPage:true});
  await page.locator('.taxonomy-row').nth(2).getByRole('button').click();
  await expect(page).toHaveURL(/pages\/search\/index\?keywordId=/);
  await expect(page.locator('.result-card .card-title').filter({hasText:detail.article.title})).toBeVisible();
  expect(errors).toEqual([]);
 }finally{await browser.close()}
})().catch(e=>{console.error(e);process.exitCode=1});
