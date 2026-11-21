import { request } from '../api/platform'
import type { ArticleDetail, ArticlePage, CatalogItem } from '../types/api'
import type { HomeView, FollowView, FavoriteView, BoxView, ReplacementHintView } from '../types/models'

export const content = {
 home:()=>request<HomeView>('GET','/api/v1/home'),
 categories:()=>request<CatalogItem[]>('GET','/api/v1/catalog/categories'),
 organizations:()=>request<CatalogItem[]>('GET','/api/v1/catalog/organizations'),
 page:(query:string)=>request<ArticlePage>('GET','/api/v1/articles?'+query),
 detail:(id:string)=>request<ArticleDetail>('GET','/api/v1/articles/'+id),
 replacement:(id:string)=>request<ReplacementHintView>('GET','/api/v1/articles/'+id+'/replacement'),
 follows:()=>request<FollowView[]>('GET','/api/v1/me/follows'),
 favorites:()=>request<FavoriteView[]>('GET','/api/v1/me/favorites'),
 boxes:()=>request<BoxView[]>('GET','/api/v1/me/subscription-boxes'),
}
