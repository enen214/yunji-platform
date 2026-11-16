// Isolated client behavior; never calls WeChat and does not certify native UI behavior.
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const ts=require('../apps/client/node_modules/typescript');
const cases=[];
async function check(id,fn){try{await fn();cases.push({id,status:'PASS'})}catch(e){cases.push({id,status:'FAIL',error:e.message})}}
function compile(source){return ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText}
async function main(){
 let calls=[],loginCalls=0,currentApp='wx0123456789abcdef',identityCode='identity-code';
 const helper={exports:{},require(){return {request:async(...args)=>{calls.push(args);return {accessToken:'synthetic-token'}}}},uni:{getAccountInfoSync:()=>({miniProgram:{appId:currentApp}}),login(options){loginCalls++;options.success({code:identityCode})}}};
 vm.runInNewContext(compile(fs.readFileSync('apps/client/src/features/wechat-login.ts','utf8')),helper);
 const {authorizedPhoneCode,nativeWechatLogin}=helper.exports;
 await check('native-phone-rejection-does-not-login',async()=>{for(const event of [{},{detail:{errMsg:'getPhoneNumber:fail user deny'}},{detail:{errMsg:'getPhoneNumber:ok'}}])assert.throws(()=>authorizedPhoneCode(event));assert.equal(loginCalls,0)});
 await check('native-codes-distinct-and-no-plaintext-phone',async()=>{const phone=authorizedPhoneCode({detail:{code:'phone-code',errMsg:'getPhoneNumber:ok'}});await nativeWechatLogin(phone);assert.equal(calls.length,1);assert.equal(calls[0][1],'/api/v1/auth/customer/wechat/native');assert.deepEqual(JSON.parse(JSON.stringify(calls[0][2])),{appId:currentApp,code:'identity-code',phoneCode:'phone-code'})});
 await check('ordinary-native-login-does-not-request-phone',async()=>{await nativeWechatLogin();assert.equal(calls.at(-1)[2].phoneCode,undefined)});
 await check('abandoned-native-login-does-not-call-backend',async()=>{const count=calls.length;await assert.rejects(nativeWechatLogin('phone',()=>false));assert.equal(calls.length,count)});
 await check('invalid-app-id-fails-before-login',async()=>{currentApp='touristappid';const before=loginCalls;await assert.rejects(nativeWechatLogin());assert.equal(loginCalls,before);currentApp='wx0123456789abcdef'});
 await check('empty-identity-code-fails-before-backend',async()=>{identityCode='';const before=calls.length;await assert.rejects(nativeWechatLogin());assert.equal(calls.length,before)});
 for(const file of ['apps/client/src/components/LoginSheet.vue','apps/client/src/pages/login/index.vue']){
  let source=fs.readFileSync(file,'utf8').match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1].replace(/import\.meta\.env\.DEV/g,'true');
  const sheet=file.includes('LoginSheet');let tokens=[],nativeCalls=0,resolveNative;const hooks=[];
  const loginState={busy:false,visible:true,attempt:1,owner:'current',error:''};
  const context={exports:{},getCurrentPages:()=>[],uni:{getSystemInfoSync:()=>({statusBarHeight:0}),reLaunch(){}},require(name){
   if(name==='vue')return {ref:value=>({value}),watch(){},onMounted:fn=>fn(),onUnmounted:fn=>hooks.push(fn)};
   if(name==='@dcloudio/uni-app')return {onShow:fn=>fn(),onHide:fn=>hooks.push(fn),onUnload:fn=>hooks.push(fn),onBackPress(){}};
   if(name.endsWith('/features/auth'))return {loginState,currentLoginOwner:()=> 'current',cancelLogin(){},abandonLogin(){loginState.attempt++;loginState.busy=false;loginState.visible=false},completeLogin:async()=>{}};
   if(name.endsWith('/features/wechat-login'))return {authorizedPhoneCode,nativeWechatLogin(){nativeCalls++;return new Promise(resolve=>{resolveNative=resolve})}};
   if(name.endsWith('/api/platform'))return {setToken:t=>tokens.push(t),request:async()=>({accessToken:'local-token'}),navigate(){}};
   throw Error(name);
  }};
  source+='\nexports.test={phoneLogin,signIn,consent};';vm.runInNewContext(compile(source),context);
  const page=context.exports.test;
  await check((sheet?'sheet':'page')+'-consent-and-duplicate-guard',async()=>{
   page.phoneLogin({detail:{errMsg:'getPhoneNumber:ok',code:'phone-code'}});assert.equal(nativeCalls,0);
   page.consent.value=true;
   page.phoneLogin({detail:{errMsg:'getPhoneNumber:fail user deny'}});assert.equal(nativeCalls,0);
   page.phoneLogin({detail:{errMsg:'getPhoneNumber:ok',code:'phone-code'}});
   page.phoneLogin({detail:{errMsg:'getPhoneNumber:ok',code:'phone-code'}});assert.equal(nativeCalls,1);
  });
  await check((sheet?'sheet':'page')+'-late-response-after-leave-is-ignored',async()=>{
   hooks[0]();resolveNative({accessToken:'stale-token'});await new Promise(resolve=>setImmediate(resolve));assert.equal(tokens.length,0);
  });
 }
 await check('compiled-native-buttons-have-phone-authorization-bindings',async()=>{
  for(const file of ['components/LoginSheet.wxml','pages/login/index.wxml']){
   const wxml=fs.readFileSync('apps/client/dist/build/mp-weixin/'+file,'utf8');assert.match(wxml,/open-type=/);assert.match(wxml,/bindgetphonenumber|bind:getphonenumber/);
  }
 });
 const report={scope:'ISOLATED_WECHAT_CLIENT_AND_COMPILED_CONTROLS',cases,passed:cases.filter(c=>c.status==='PASS').length,failed:cases.filter(c=>c.status==='FAIL').length,limits:['No native authorization UI or actual phone API call is executed.']};
 process.stdout.write(JSON.stringify(report));process.exitCode=report.failed?1:0;
}
main().catch(e=>{console.error(e);process.exitCode=1});
