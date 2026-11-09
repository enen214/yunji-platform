package com.sunshine.yunji.content.article.application;
import com.sunshine.yunji.content.shared.Db;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.HashSet;
import com.sunshine.yunji.content.article.domain.ReplacementView;
@Service
public class ArticleAccess {
 public static final String VISIBLE=" a.tenant_id='000000' AND a.deleted_at IS NULL AND a.status='PUBLISHED' AND a.published_at<=UTC_TIMESTAMP() AND (a.effective_from IS NULL OR a.effective_from<=UTC_TIMESTAMP()) AND (a.effective_to IS NULL OR a.effective_to>UTC_TIMESTAMP()) AND a.current_version_id IS NOT NULL ";
 public static final String READABLE=" a.tenant_id='000000' AND a.deleted_at IS NULL AND a.status='PUBLISHED' AND a.published_at<=UTC_TIMESTAMP() AND a.current_version_id IS NOT NULL ";
 public static final String CURRENT=READABLE+" AND (a.effective_to IS NULL OR a.effective_to>UTC_TIMESTAMP()) ";
 public Map<String,Object> readable(long id){return db.one("SELECT a.id,a.title,a.current_version_id FROM article a WHERE "+READABLE+" AND a.id=?",id);}
 private final Db db;public ArticleAccess(Db db){this.db=db;}
 public Map<String,Object> visible(long id){return db.one("SELECT a.id,a.title,a.current_version_id FROM article a WHERE "+VISIBLE+" AND a.id=?",id);}
 public ReplacementView replacement(long id){
  var source=db.rows("SELECT id FROM article WHERE id=? AND tenant_id='000000' AND deleted_at IS NULL AND current_version_id IS NOT NULL",id);
  if(source.isEmpty())throw new com.sunshine.yunji.content.shared.BusinessException(404,"对象不存在或不可访问");
  long cursor=id;var seen=new HashSet<Long>();
  for(int depth=0;depth<32;depth++){
   if(!seen.add(cursor))return new ReplacementView(Long.toString(id),ReplacementView.Status.UNAVAILABLE,null);
   var edges=db.rows("SELECT new_article_id FROM article_replacement WHERE old_article_id=? AND status='RESOLVED' ORDER BY resolved_at DESC,id DESC LIMIT 1",cursor);
   if(edges.isEmpty()){
    if(cursor==id)return new ReplacementView(Long.toString(id),ReplacementView.Status.NONE,null);
    var target=db.rows("SELECT a.id,a.title,a.current_version_id FROM article a WHERE "+VISIBLE+" AND a.id=?",cursor);
    if(target.isEmpty())return new ReplacementView(Long.toString(id),ReplacementView.Status.UNAVAILABLE,null);
    var t=target.get(0);return new ReplacementView(Long.toString(id),ReplacementView.Status.AVAILABLE,new ReplacementView.Target(t.get("id").toString(),t.get("title").toString(),t.get("currentVersionId").toString()));
   }
   cursor=Db.number(edges.get(0).get("newArticleId"));
  }
  return new ReplacementView(Long.toString(id),ReplacementView.Status.UNAVAILABLE,null);
 }
 public java.util.List<Map<String,Object>> archive(){return db.rows("SELECT a.id,a.title,a.summary,a.current_version_id,a.effective_to FROM article a WHERE a.tenant_id='000000' AND a.status='PUBLISHED' AND a.deleted_at IS NULL AND a.published_at<=UTC_TIMESTAMP() AND a.effective_to<=UTC_TIMESTAMP() AND a.current_version_id IS NOT NULL ORDER BY a.effective_to DESC,a.id DESC LIMIT 100");}
}
