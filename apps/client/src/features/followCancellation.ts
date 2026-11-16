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
    const choice=await uni.showModal({title:'取消关注',content:'是否同时关闭对应强提醒？',confirmText:'一起关闭',cancelText:'保留提醒'})
    const rules=choice.confirm&&row.targetType!=='LABEL'?await request<Row>('GET','/api/v1/me/reminders/rules?targetType='+row.targetType+'&targetId='+row.targetId+'&size=100'):null
    await request('PUT','/api/v1/me/follows/'+row.targetType+'/'+row.targetId,{enabled:false,stopReminders:!!choice.confirm})
    paused.set(row.id,(rules?.items||[]).filter((rule:Row)=>rule.enabled));cancelled.value.push(row.id);retained.value.push(row)
   }
   await load()
  }catch(e){error.value=(e as Error).message}finally{busy.value=false}
 }
 return {cancelled,retained,busy,remove,reset}
}
