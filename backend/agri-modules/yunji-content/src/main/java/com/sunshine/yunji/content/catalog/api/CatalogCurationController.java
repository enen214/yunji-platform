package com.sunshine.yunji.content.catalog.api;

import com.sunshine.yunji.content.shared.*;
import com.sunshine.yunji.content.identity.application.Actors;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import com.sunshine.yunji.content.shared.contract.ViewMapper;

/** Explicit editorial associations; the legacy article-derived discovery endpoint is unchanged. */
@RestController @Validated
public class CatalogCurationController {
    private final Db db;private final Actors actors;
    public CatalogCurationController(Db db,Actors actors){this.db=db;this.actors=actors;}
    public record Node(String id,String name,String parentId,String categoryId,String status,Integer lockVersion){}
    public record Link(String labelId,String keywordId,int sortOrder){}
    public record Tree(List<Node> categories,List<Node> labels,List<Node> keywords,List<Link> links){}
    public record Saved(String labelId,int lockVersion){}
    public record Keyword(String id,String name){}
    public record Selection(@NotNull @Size(max=100) List<@Positive Long> keywordIds,@Min(0) int expectedVersion){}
    @GetMapping("/api/v1/admin/catalog/curation") public ApiResponse<Tree> tree(){
        actors.staff().require("yunji:catalog:manage");return ApiResponse.ok(ViewMapper.of(Map.of(
            "categories",db.rows("SELECT id,name,status FROM content_category WHERE tenant_id='000000' AND deleted_at IS NULL ORDER BY sort_order,id"),
            "labels",db.rows("SELECT id,name,category_id,parent_id,status,lock_version FROM content_label WHERE tenant_id='000000' AND deleted_at IS NULL ORDER BY sort_order,id"),
            "keywords",db.rows("SELECT id,display_name name,status FROM keyword WHERE tenant_id='000000' AND deleted_at IS NULL ORDER BY sort_order,id"),
            "links",db.rows("SELECT label_id,keyword_id,sort_order FROM label_keyword_curation ORDER BY label_id,sort_order,keyword_id")),Tree.class));
    }
    @PutMapping("/api/v1/admin/catalog/labels/{id}/keywords") @Transactional
    public ApiResponse<Saved> save(@PathVariable @Positive long id,@Valid @RequestBody Selection b){
        var actor=actors.staff();actor.require("yunji:catalog:manage");
        var label=db.one("SELECT * FROM content_label WHERE id=? AND tenant_id='000000' AND deleted_at IS NULL FOR UPDATE",id);
        BusinessException.require(Db.number(label.get("lockVersion"))==b.expectedVersion(),409,"标签已更新，请刷新");
        BusinessException.require(new HashSet<>(b.keywordIds()).size()==b.keywordIds().size(),422,"关键词不能重复");
        for(long k:b.keywordIds())db.one("SELECT id FROM keyword WHERE id=? AND tenant_id='000000' AND status='ENABLED' AND deleted_at IS NULL",k);
        db.update("DELETE FROM label_keyword_curation WHERE label_id=?",id);int index=0;
        for(long k:b.keywordIds())db.update("INSERT INTO label_keyword_curation(label_id,keyword_id,sort_order) VALUES(?,?,?)",id,k,index++);
        db.update("UPDATE content_label SET lock_version=lock_version+1 WHERE id=?",id);
        db.audit(actor.id(),"STAFF","LABEL_KEYWORDS","LABEL",id,b);
        return ApiResponse.ok(new Saved(Long.toString(id),b.expectedVersion()+1));
    }
    @GetMapping("/api/v1/catalog/labels/{id}/curated-keywords") public ApiResponse<List<Keyword>> publicKeywords(@PathVariable @Positive long id){
        return ApiResponse.ok(ViewMapper.list(db.rows("SELECT k.id,k.display_name name FROM label_keyword_curation r JOIN keyword k ON k.id=r.keyword_id JOIN content_label l ON l.id=r.label_id JOIN content_category c ON c.id=l.category_id WHERE l.id=? AND l.tenant_id='000000' AND l.deleted_at IS NULL AND l.status='ENABLED' AND c.deleted_at IS NULL AND c.status='ENABLED' AND k.deleted_at IS NULL AND k.status='ENABLED' ORDER BY r.sort_order,k.id",id),Keyword.class));
    }
}
