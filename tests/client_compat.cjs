// Isolated MP branch execution, not a substitute for WeChat DevTools or device testing.
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm'),assert=require('node:assert/strict');
const root=path.resolve(__dirname,'..'),ts=require('../apps/client/node_modules/typescript'),postcss=require('../apps/client/node_modules/postcss');
const cases=[];
function check(id,fn){try{fn();cases.push({id,status:'PASS'})}catch(e){cases.push({id,status:'FAIL',error:e.message})}}
let source=fs.readFileSync(path.join(root,'apps/client/src/features/telemetry.ts'),'utf8');
source=source.replace(/\s*\/\/ #ifdef H5[\s\S]*?\/\/ #endif/g,'');
const js=ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText;
const events=[],timers=new Map(),observations=[];let next=0,enumerations=0,disconnected=0;
const scope=new Proxy({createIntersectionObserver(options){assert.equal(options.observeAll,true);const item={callback:null};observations.push(item);return {disconnect(){disconnected++},relativeToViewport(){return {observe(selector,callback){assert.equal(selector,'.exposure-card');item.callback=callback}}}}}},{ownKeys(){enumerations++;throw Error('Native scope must not be enumerated')}});
const context={exports:{},require(name){assert.equal(name,'../api/platform');return {event:(...args)=>events.push(args)}},setTimeout(fn,ms){assert.equal(ms,1000);timers.set(++next,fn);return next},clearTimeout(id){timers.delete(id)}};
vm.runInNewContext(js,context);
const tracker=context.exports.visibleExposure(()=>scope);
const result=(id,ratio)=>({dataset:{articleId:id},intersectionRatio:ratio});
const flush=()=>{for(const [id,fn] of [...timers]){timers.delete(id);fn()}};
check('mp-native-observer-without-proxy-enumeration',()=>{tracker.show();tracker.observe();assert.equal(observations.length,1);assert.equal(enumerations,0)});
check('mp-exposure-requires-real-scroll',()=>{observations.at(-1).callback(result('a',1));flush();assert.equal(events.length,0);tracker.scroll()});
check('mp-exposure-requires-continuous-visibility',()=>{observations.at(-1).callback(result('a',.6));observations.at(-1).callback(result('a',.4));flush();assert.equal(events.length,0)});
check('mp-exposure-deduplicated-per-list',()=>{observations.at(-1).callback(result('a',.6));flush();observations.at(-1).callback(result('a',1));flush();assert.equal(events.length,1);assert.equal(events[0][0],'VISIBLE_EXPOSURE')});
check('mp-hidden-page-cancels-pending-exposure',()=>{observations.at(-1).callback(result('b',1));tracker.hide();flush();assert.equal(events.length,1);assert.ok(disconnected>0)});
check('mp-late-observer-callback-ignored-after-reentry',()=>{const old=observations.at(-1);tracker.show();tracker.observe();old.callback(result('late',1));flush();assert.equal(events.length,1)});
check('mp-reset-allows-new-list-and-cancels-timers',()=>{observations.at(-1).callback(result('b',1));tracker.reset();flush();assert.equal(events.length,1);tracker.observe();tracker.scroll();observations.at(-1).callback(result('a',1));flush();assert.equal(events.length,2)});
check('mp-missing-native-scope-is-safe',()=>{const t=context.exports.visibleExposure(()=>undefined);t.show();t.observe();t.hide()});
const css=postcss.parse(fs.readFileSync(path.join(root,'apps/client/dist/build/mp-weixin/app.wxss'),'utf8'));
function declarations(selector){const output={};css.walkRules(r=>{if(r.selector.split(',').map(x=>x.trim()).includes(selector))r.walkDecls(d=>{output[d.prop]=d.value})});return output}
check('mp-controls-have-literal-colors-and-borders',()=>{for(const sel of ['.page button','button.quiet','button.secondary','.card','.segmented','.segmented button.active','.filter-sheet']){const d=declarations(sel);assert.ok(Object.keys(d).length,sel);assert.ok(d.background||d['border-color']||d.border,sel);assert.ok(!Object.values(d).some(v=>v.includes('var(--color-')),sel)}assert.match(declarations('.page button').border,/1px solid/);assert.match(declarations('button.quiet').border,/1px solid/);assert.equal(declarations('.segmented button.active').color,'#fff')});
check('mp-compiled-pages-contain-action-controls',()=>{for(const page of ['login','notifications','notification-edit','notification-settings','activity-overview','subscriptions']){const wxml=fs.readFileSync(path.join(root,'apps/client/dist/build/mp-weixin/pages',page,'index.wxml'),'utf8');assert.match(wxml,/<button\b/,page);assert.match(wxml,/bindtap|bind:tap/,page)}});
check('mp-bundle-uses-native-page-observer',()=>{const compiled=fs.readFileSync(path.join(root,'apps/client/dist/build/mp-weixin/features/telemetry.js'),'utf8');assert.match(compiled,/\.createIntersectionObserver\(/);assert.doesNotMatch(compiled,/\.index\.createIntersectionObserver\(/);assert.doesNotMatch(compiled,/document\./)});
process.stdout.write(JSON.stringify({scope:'MP_COMPILED_ARTIFACT_AND_ISOLATED_LIFECYCLE',cases,passed:cases.filter(c=>c.status==='PASS').length,failed:cases.filter(c=>c.status==='FAIL').length,limits:['No WeChat runtime, developer tools, phone, or real account was used.']}));
process.exitCode=cases.some(c=>c.status==='FAIL')?1:0;
