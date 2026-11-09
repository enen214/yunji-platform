import { event } from '../api/platform'

// Shared lifecycle rules for H5 and mini-program. Hidden time is never accumulated.
export function visibleStay(articleId:()=>string) {
 let started=0,elapsed=0
 return {
  show(){
   // #ifdef H5
   if(document.hidden)return
   // #endif
   if(!started)started=Date.now()
  },
  hide(){if(started){elapsed+=Date.now()-started;started=0}},
  flush(){this.hide();const durationMs=Math.min(elapsed,3600000);elapsed=0;if(durationMs>=1000)void event('PAGE_STAY',articleId(),{durationMs})}
 }
}

export interface NativeExposureScope {
 createIntersectionObserver(options:{observeAll:boolean;thresholds:number[]}): {
  disconnect():void
  relativeToViewport(): {observe(selector:string,callback:(result:{dataset?:{articleId?:string};intersectionRatio:number})=>void):void}
 }
}

export function visibleExposure(nativeScope:()=>NativeExposureScope|undefined) {
 let active=false,scrolled=false,observer:any=null,mutations:any=null,generation=0
 const pending=new Map<string,ReturnType<typeof setTimeout>>(),sent=new Set<string>()
 function cancel(){generation++;for(const timer of pending.values())clearTimeout(timer);pending.clear();observer?.disconnect();observer=null;mutations?.disconnect();mutations=null}
 function observeResult(id:string,ratio:number){
  if(!active||!scrolled||!id||sent.has(id))return
  if(ratio<0.5){const timer=pending.get(id);if(timer)clearTimeout(timer);pending.delete(id);return}
  if(pending.has(id))return
  const current=generation
  pending.set(id,setTimeout(()=>{pending.delete(id);if(active&&generation===current){sent.add(id);void event('VISIBLE_EXPOSURE',id,{visibleRatio:0.5,durationMs:1000})}},1000))
 }
 return {
  show(){active=true
   // #ifdef H5
   if(document.hidden)active=false
   // #endif
  },
  hide(){active=false;cancel()},
  reset(){sent.clear();scrolled=false;cancel()},
  scroll(){if(!scrolled){scrolled=true;this.observe()}},
  observe(){
   cancel();if(!active)return
   // #ifdef H5
   const observed=new Set<Element>(),current=generation
   observer=new IntersectionObserver(entries=>{if(generation!==current)return;entries.forEach(entry=>observeResult(String((entry.target as HTMLElement).dataset.articleId||''),entry.intersectionRatio))},{threshold:[0,0.5,1]})
   const attach=()=>{if(!active||generation!==current)return;document.querySelectorAll('.exposure-card').forEach(card=>{if(!observed.has(card)){observed.add(card);observer.observe(card)}})}
   mutations=new MutationObserver(attach);mutations.observe(document.body,{childList:true,subtree:true});attach()
   // #endif
   // #ifdef MP-WEIXIN
   // Resolve the native page only when mounted. Never enumerate or serialize Vue's proxy.
   const page=nativeScope()
   if(!page || typeof page.createIntersectionObserver!=='function')return
   observer=page.createIntersectionObserver({observeAll:true,thresholds:[0,0.5,1]})
   const epoch=generation
   observer.relativeToViewport().observe('.exposure-card',(result:{dataset?:{articleId?:string};intersectionRatio:number})=>{if(generation===epoch)observeResult(String(result.dataset?.articleId||''),result.intersectionRatio)})
   // #endif
  }
 }
}
