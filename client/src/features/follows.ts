import type {Row} from '../api/platform'
export interface FollowGroup {key:string;name:string;items:Row[]}
// A keyword can belong to several real labels. Removing it updates all groups.
export function followGroups(rows:Row[],kind:string):FollowGroup[]{
 const groups=new Map<string,FollowGroup>()
 for(const row of rows.filter(r=>kind==='keywords'?r.targetType==='KEYWORD':r.targetType==='ARTICLE')){
  const memberships=(row.labels||[]).map((l:Row)=>({key:'label:'+l.id,name:l.categoryName+' / '+l.name}))
  if(!memberships.length)memberships.push(row.targetType==='CATEGORY'?{key:'category:'+row.targetId,name:row.name}:{key:'unclassified:'+row.targetType,name:row.targetType==='KEYWORD'?'未归类关键词':row.targetType==='ARTICLE'?'未归类资讯':'其他目录关注'})
  for(const member of memberships){const group=groups.get(member.key)||{...member,items:[]};group.items.push(row);groups.set(member.key,group)}
 }
 return [...groups.values()]
}

export interface FollowBoard {key:string;name:string;labels:FollowGroup[]}
export function followTree(rows:Row[],kind:string):FollowBoard[]{
 const boards=new Map<string,FollowBoard>()
 for(const row of rows.filter(r=>kind==='keywords'?r.targetType==='KEYWORD':r.targetType==='ARTICLE')){
  const memberships=row.labels?.length?row.labels:[{id:'unclassified',name:'未归类',categoryId:'unclassified',categoryName:'未归类'}]
  for(const l of memberships){
   const key=String(l.categoryId)
   const board:FollowBoard=boards.get(key)||{key,name:l.categoryName,labels:[]}
   const groupKey=l.id==='unclassified'?'unclassified:'+row.targetType:'label:'+l.id
   let group=board.labels.find(g=>g.key===groupKey)
   if(!group){group={key:groupKey,name:l.name,items:[]};board.labels.push(group)}
   if(!group.items.some(r=>r.id===row.id))group.items.push(row)
   boards.set(key,board)
  }
 }
 return [...boards.values()]
}

export function followLabels(rows:Row[],kind:'keywords'|'info'='keywords'):FollowGroup[]{
 const labels=new Map<string,FollowGroup>()
 for(const row of rows.filter(r=>kind==='keywords'?r.targetType==='KEYWORD':r.targetType==='ARTICLE')){
  const memberships=row.labels?.length?row.labels:[{id:'unclassified',name:kind==='keywords'?'未归类关键词':'未归类资讯'}]
  for(const l of memberships){
   const key=l.id==='unclassified'?'unclassified:'+row.targetType:'label:'+l.id
   const group:FollowGroup=labels.get(key)||{key,name:l.name,items:[]}
   if(!group.items.some(r=>r.id===row.id))group.items.push(row)
   labels.set(key,group)
  }
 }
 return [...labels.values()]
}
