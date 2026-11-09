package com.sunshine.yunji.content.catalog.application;
import com.sunshine.yunji.content.shared.*;
import com.sunshine.yunji.content.identity.application.Actors;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.Normalizer;
import java.util.*;

@Service
public class CatalogCommands {
    private final Db db;private final Actors actors;
    public CatalogCommands(Db db,Actors actors){this.db=db;this.actors=actors;}
    public record Item(@NotBlank @Size(max=100) String name,@Size(max=64) String code,Long parentId,String type,@NotBlank String status,int sortOrder,int expectedVersion){}
    private String table(String kind){return switch(kind){case "categories"->"content_category";case "organizations"->"organization";case "keywords"->"keyword";default->throw new BusinessException(404,"目录不存在");};}
    public static String normalize(String text){return Normalizer.normalize(Db.text(text,100),Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);}
    public List<Map<String,Object>> list(String kind){var a=actors.staff();a.require("yunji:catalog:manage");return db.rows("SELECT * FROM "+table(kind)+" WHERE tenant_id='000000' ORDER BY sort_order,id LIMIT 500");}
    @Transactional public Map<String,Object> save(String kind,Long id,Item b){
        var actor=actors.staff();actor.require("yunji:catalog:manage");String table=table(kind);boolean create=id==null;long key=create?db.id():id;
        BusinessException.require(Set.of("ENABLED","DISABLED").contains(b.status),422,"目录状态错误");
        db.rows("SELECT id FROM "+table+" WHERE tenant_id='000000' ORDER BY id FOR UPDATE");
        if(!create){var current=db.one("SELECT * FROM "+table+" WHERE id=? AND tenant_id='000000' AND deleted_at IS NULL",key);BusinessException.require(Db.number(current.get("lockVersion"))==b.expectedVersion,409,"目录已变化");}
        long parent=b.parentId==null?0:b.parentId;
        if(!kind.equals("keywords")){
            String code=Db.text(b.code,64);
            if(kind.equals("organizations")){BusinessException.require(parent!=key,422,"不能以自身作为父级");Set<Long> seen=new HashSet<>();seen.add(key);long cursor=parent;
                while(cursor!=0){BusinessException.require(seen.add(cursor),422,"目录形成循环");cursor=Db.number(db.one("SELECT parent_id FROM organization WHERE id=? AND tenant_id='000000' AND deleted_at IS NULL",cursor).get("parentId"));}
                BusinessException.require(Set.of("SCHOOL","COLLEGE","DEPARTMENT").contains(b.type),422,"组织类型错误");
                if(create)db.update("INSERT INTO organization(id,parent_id,type,code,name,status,sort_order,created_by) VALUES(?,?,?,?,?,?,?,?)",key,parent,b.type,code,b.name,b.status,b.sortOrder,actor.id());
                else db.update("UPDATE organization SET parent_id=?,type=?,code=?,name=?,status=?,sort_order=?,lock_version=lock_version+1 WHERE id=?",parent,b.type,code,b.name,b.status,b.sortOrder,key);
            }else{
                if(create)db.update("INSERT INTO content_category(id,code,name,status,sort_order,created_by) VALUES(?,?,?,?,?,?)",key,code,b.name,b.status,b.sortOrder,actor.id());
                else db.update("UPDATE content_category SET code=?,name=?,status=?,sort_order=?,lock_version=lock_version+1 WHERE id=?",code,b.name,b.status,b.sortOrder,key);
            }
        }else{
            String normalized=normalize(b.name);BusinessException.require(db.count("SELECT COUNT(*) FROM keyword_blacklist WHERE tenant_id='000000' AND normalized_name=?",normalized)==0,422,"关键词已禁用");
            if(create)db.update("INSERT INTO keyword(id,display_name,normalized_name,status,sort_order,created_by) VALUES(?,?,?,?,?,?)",key,b.name,normalized,b.status,b.sortOrder,actor.id());
            else db.update("UPDATE keyword SET display_name=?,normalized_name=?,status=?,sort_order=?,lock_version=lock_version+1 WHERE id=?",b.name,normalized,b.status,b.sortOrder,key);
        }
        db.audit(actor.id(),"STAFF",create?"CATALOG_CREATE":"CATALOG_EDIT",kind,key,Map.of("name",b.name));return db.one("SELECT * FROM "+table+" WHERE id=?",key);
    }
    @Transactional public void delete(String kind,long id){
        var a=actors.staff();a.require("yunji:catalog:manage");String table=table(kind);db.one("SELECT id FROM "+table+" WHERE id=? AND tenant_id='000000' AND deleted_at IS NULL FOR UPDATE",id);
        if(!kind.equals("keywords"))BusinessException.require(db.count("SELECT COUNT(*) FROM "+table+" WHERE parent_id=? AND deleted_at IS NULL",id)==0,409,"请先处理子级目录");
        String relation=switch(kind){case "categories"->"article_category_rel";case "organizations"->"article_organization_rel";default->"article_keyword_rel";};String column=switch(kind){case "categories"->"category_id";case "organizations"->"organization_id";default->"keyword_id";};
        BusinessException.require(db.count("SELECT COUNT(*) FROM "+relation+" r JOIN article a ON a.id=r.article_id WHERE r."+column+"=? AND a.status='PUBLISHED'",id)==0,409,"目录仍被已发布内容引用");
        db.update("UPDATE "+table+" SET deleted_at=UTC_TIMESTAMP(),status='DISABLED',lock_version=lock_version+1 WHERE id=?",id);db.audit(a.id(),"STAFF","CATALOG_DELETE",kind,id,Map.of());
    }
    @Transactional public Map<String,Object> alias(long keyword,String alias){var a=actors.staff();a.require("yunji:catalog:manage");db.one("SELECT id FROM keyword WHERE id=? AND tenant_id='000000' AND status='ENABLED' AND deleted_at IS NULL",keyword);String name=normalize(alias);BusinessException.require(db.count("SELECT COUNT(*) FROM keyword WHERE normalized_name=? AND id<>?",name,keyword)==0,409,"别名与关键词冲突");long id=db.id();db.update("INSERT INTO keyword_alias(id,normalized_alias,keyword_id) VALUES(?,?,?)",id,name,keyword);db.audit(a.id(),"STAFF","KEYWORD_ALIAS","KEYWORD",keyword,Map.of("alias",name));return Map.of("id",Long.toString(id));}
    @Transactional public void blacklist(String text,String reason){var a=actors.staff();a.require("yunji:catalog:manage");String name=normalize(text);db.update("INSERT INTO keyword_blacklist(id,normalized_name,reason) VALUES(?,?,?)",db.id(),name,Db.text(reason,500));db.update("UPDATE keyword SET status='DISABLED',lock_version=lock_version+1 WHERE tenant_id='000000' AND normalized_name=?",name);db.audit(a.id(),"STAFF","KEYWORD_BLACKLIST","KEYWORD",0,Map.of("name",name));}
}
