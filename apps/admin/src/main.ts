import { createApp } from 'vue'
import { createRouter, createWebHashHistory } from 'vue-router'
import App from './App.vue'
import Login from './views/Login.vue'
import Articles from './views/Articles.vue'
import ReplacementCandidates from './views/ReplacementCandidates.vue'
import ContentTypes from './views/ContentTypes.vue'
import FileLibrary from './views/FileLibrary.vue'
import Workspace from './views/Workspace.vue'
import Customers from './views/Customers.vue'
import Analytics from './views/Analytics.vue'
import DeliveryCenter from './views/DeliveryCenter.vue'
import CatalogTree from './views/CatalogTree.vue'
import {workspaces} from './workspaces'
import SystemManagement from './views/SystemManagement.vue'
import { token } from './api/client'
import './styles/tokens.css'
const router = createRouter({ history: createWebHashHistory(), routes: [
 {path:'/login',component:Login}, {path:'/',redirect:'/articles'},
 {path:'/content-types',component:ContentTypes}, {path:'/file-library',component:FileLibrary}, {path:'/articles',component:Articles}, {path:'/catalog-tree',component:CatalogTree},
 {path:'/content/replacements',component:ReplacementCandidates},
 {path:'/customers',component:Customers},
 ...['dashboard','content','behavior','profile'].map(view=>({path:'/analytics/'+view,component:Analytics,props:{view}})),
 ...['rules','records','settings','instant','templates','statistics','monitor','wecom','wecom-config','wecom-operations','wecom-logs','wecom-statistics'].map(view=>({path:'/reminders/'+view,component:DeliveryCenter,props:{view}})),
 ...Object.keys(workspaces).filter(section=>!['system','organizations','packages','subjects','dimensions','exams','artifacts','files','notifications','analytics','columns'].includes(section)).flatMap(section=>[{path:'/'+section,component:Workspace,meta:{section}},{path:'/'+section+'/new',component:Workspace,meta:{section,mode:'new'}},{path:'/'+section+'/:id/edit',component:Workspace,meta:{section,mode:'edit'}},{path:'/'+section+'/:id',component:Workspace,meta:{section,mode:'detail'}}]),
 {path:'/:pathMatch(.*)*',component:{template:'<section><h1>页面不存在</h1><p>请从导航进入有效页面。</p><a href="#/articles">返回工作台</a></section>'}},
 {path:'/system',component:SystemManagement},
] })
router.beforeEach(to => to.path !== '/login' && !token() ? '/login' : undefined)
createApp(App).use(router).mount('#app')
