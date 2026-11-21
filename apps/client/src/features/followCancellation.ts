import {ref} from 'vue'
import {request,type Row} from '../api/platform'

// Cancellation state is scoped to this page visit. Undo is one server transaction.
export function followCancellation(load:()=>Promise<void>,error:{value:string}) {
 const cancelled=ref<string[]>([]),retained=ref<Row[]>([]),busy=ref(false)
 const paused=new Map<string,Row[]>()
 function reset(){cancelled.value=[];retained.value=[];paused.clear()}
 async function remove(row:Row){
  if(busy.value)return
  busy.value=true;error.value=''
  try{
   if(cancelled.value.includes(row.id)){
    await request('POST','/api/v1/me/follows/undo',{followId:row.id,targetType:row.targetType,targetId:row.targetId,rules:(paused.get(row.id)||[]).map(rule=>({id:rule.id,expectedVersion:rule.lockVersion+1}))})
    cancelled.value=cancelled.value.filter(id=>id!==row.id);retained.value=retained.value.filter(item=>item.id!==row.id)
   }else{
    let stopReminders=false
    let snapshot:Row[]=[]
    if(row.targetType==='LABEL'){
     const choice=await uni.showModal({title:'取消关注',content:'取消后，该标签下的新信息不再进入信息盒子。',confirmText:'取消关注',cancelText:'再想想'})
     if(!choice.confirm){busy.value=false;return}
    }else{
     try{
      const r=await request<Row>('GET','/api/v1/me/reminders/rules?targetType='+row.targetType+'&targetId='+row.targetId+'&enabledOnly=true&size=100')
      snapshot=(r?.items||[]).filter((x:Row)=>x.enabled)
     }catch(e){snapshot=[]}
     if(snapshot.length>0){
      const choice=await uni.showModal({title:'取消关注',content:'是否同时关闭对应强提醒？',confirmText:'一起关闭',cancelText:'保留提醒'})
      stopReminders=choice.confirm
     }
    }
    await request('PUT','/api/v1/me/follows/'+row.targetType+'/'+row.targetId,{enabled:false,stopReminders})
    paused.set(row.id,stopReminders?snapshot:[])
    cancelled.value.push(row.id);retained.value.push(row)
   }
   await load()
  }catch(e){error.value=(e as Error).message}finally{busy.value=false}
 }
 return {cancelled,retained,busy,remove,reset}
}
