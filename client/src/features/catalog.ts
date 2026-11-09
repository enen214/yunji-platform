import type { CatalogItem } from '../types/api'

/** Keep filter labels tied to the operator-maintained directory, including child organizations. */
export function catalogOptions(items: CatalogItem[]): { item: CatalogItem; label: string }[] {
  const byId = new Map(items.map(item => [item.id, item]))
  const children = new Map<string, CatalogItem[]>()
  for (const item of items) {
    const parent = item.parentId && byId.has(item.parentId) ? item.parentId : ''
    children.set(parent, [...(children.get(parent) || []), item])
  }
  const result: { item: CatalogItem; label: string }[] = [], seen = new Set<string>()
  function visit(item: CatalogItem, ancestors: string[]) {
    if (seen.has(item.id)) return
    seen.add(item.id)
    const path = [...ancestors, item.name]
    result.push({ item, label: path.join(' / ') })
    for (const child of children.get(item.id) || []) visit(child, path)
  }
  for (const root of children.get('') || []) visit(root, [])
  for (const item of items) visit(item, [])
  return result
}

import {ref} from 'vue'
import {request} from '../api/platform'
export type ContentType={code:string;name:string;renderer:'TEXT'|'NOTICE'|'LONG_IMAGE'}
export const contentTypes=ref<ContentType[]>([{code:'BASIC',name:'普通信息',renderer:'TEXT'},{code:'NOTICE',name:'通知',renderer:'NOTICE'},{code:'GUIDE',name:'攻略',renderer:'LONG_IMAGE'}])
let pending:Promise<void>|null=null
export function loadContentTypes(){if(!pending)pending=request<ContentType[]>('GET','/api/v1/content-types').then(rows=>{contentTypes.value=rows}).finally(()=>{pending=null});return pending}
export function contentLabel(type:string){return contentTypes.value.find(t=>t.code===type)?.name||'资讯'}
export function presentationType(type:string){const r=contentTypes.value.find(t=>t.code===type)?.renderer;return r==='NOTICE'?'NOTICE':r==='LONG_IMAGE'?'GUIDE':'BASIC'}
export const displayTypes=[{name:'普通信息',values:['BASIC']},{name:'通知',values:['NOTICE']},{name:'攻略',values:['GUIDE']}]
