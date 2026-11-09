package com.sunshine.yunji.content.catalog.application;

import com.sunshine.yunji.content.shared.*;
import com.sunshine.yunji.content.identity.application.Actors;
import com.sunshine.yunji.content.article.application.ArticleAccess;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Labels have one immutable board. Parent nodes organize choices; matching is exact. */
@Service
public class LabelService {
    private final Db db; private final Actors actors;
    public LabelService(Db db,Actors actors){this.db=db;this.actors=actors;}
    public record Input(@NotBlank @Size(max=100) String name,@Positive long categoryId,Long parentId,
                        @NotBlank String status,int sortOrder,@Min(0) int expectedVersion){}
    public List<Map<String,Object>> list(boolean admin){
        if(admin)actors.staff().require("yunji:catalog:manage");
        return db.rows("SELECT l.*, (SELECT COUNT(*) FROM article_label_rel r JOIN article a ON a.id=r.article_id WHERE r.label_id=l.id AND"+ArticleAccess.VISIBLE+") AS article_count FROM content_label l JOIN content_category c ON c.id=l.category_id WHERE l.tenant_id='000000' AND l.deleted_at IS NULL AND c.deleted_at IS NULL"+(admin?"":" AND l.status='ENABLED' AND c.status='ENABLED'")+" ORDER BY l.sort_order,l.id LIMIT 1000");
    }
    @Transactional public Map<String,Object> save(Long id,Input b){
        var actor=actors.staff();actor.require("yunji:catalog:manage");
        BusinessException.require(Set.of("ENABLED","DISABLED").contains(b.status()),422,"标签状态错误");
        // One stable board row serializes empty-tree creation and concurrent reparenting.
        db.one("SELECT id FROM content_category WHERE id=? AND tenant_id='000000' AND deleted_at IS NULL AND status='ENABLED' FOR UPDATE",b.categoryId());
        long key=id==null?db.id():id;
        if(id!=null){var old=db.one("SELECT * FROM content_label WHERE id=? AND tenant_id='000000' AND deleted_at IS NULL FOR UPDATE",id);
            BusinessException.require(Db.number(old.get("lockVersion"))==b.expectedVersion(),409,"标签已变化，请刷新");
            BusinessException.require(Db.number(old.get("categoryId"))==b.categoryId(),422,"已建标签不能迁移板块，请在目标板块新建");}
        Set<Long> seen=new HashSet<>();seen.add(key);Long cursor=b.parentId();int depth=0;
        while(cursor!=null){BusinessException.require(depth++<8&&seen.add(cursor),422,"标签不能成环或超过八层");
            var parent=db.one("SELECT parent_id FROM content_label WHERE id=? AND category_id=? AND tenant_id='000000' AND deleted_at IS NULL AND status='ENABLED'",cursor,b.categoryId());
            cursor=parent.get("parentId")==null?null:Db.number(parent.get("parentId"));}
        if(id==null)db.update("INSERT INTO content_label(id,category_id,parent_id,name,status,sort_order) VALUES(?,?,?,?,?,?)",key,b.categoryId(),b.parentId(),b.name().strip(),b.status(),b.sortOrder());
        else db.update("UPDATE content_label SET parent_id=?,name=?,status=?,sort_order=?,lock_version=lock_version+1 WHERE id=?",b.parentId(),b.name().strip(),b.status(),b.sortOrder(),key);
        db.audit(actor.id(),"STAFF",id==null?"LABEL_CREATE":"LABEL_EDIT","LABEL",key,Map.of("categoryId",Long.toString(b.categoryId())));
        return db.one("SELECT * FROM content_label WHERE id=?",key);
    }
}
