package com.sunshine.yunji.content.article.api;

import com.sunshine.yunji.content.identity.application.Actors;
import com.sunshine.yunji.content.shared.*;
import com.sunshine.yunji.content.shared.contract.ViewMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
public class ContentTypeController {
    private final Db db;private final Actors actors;
    public ContentTypeController(Db db,Actors actors){this.db=db;this.actors=actors;}
    public record Type(String code,String name,String renderer,boolean requireOriginal,boolean requireBody,
                       boolean requireSummary,boolean requireImage,boolean requireDates,boolean enabled,int lockVersion) {}
    @GetMapping("/api/v1/content-types") public ApiResponse<List<Type>> types(){return ApiResponse.ok(ViewMapper.list(db.rows("SELECT * FROM content_type_definition WHERE enabled=1 ORDER BY code"),Type.class));}
    @GetMapping("/api/v1/admin/content-types") public ApiResponse<List<Type>> admin(){actors.staff();return ApiResponse.ok(ViewMapper.list(db.rows("SELECT * FROM content_type_definition ORDER BY code"),Type.class));}
    @PutMapping("/api/v1/admin/content-types/{code}") @Transactional public ApiResponse<Void> save(@PathVariable String code,@RequestBody Type b){
        var actor=actors.staff();BusinessException.require(code.matches("[A-Z][A-Z0-9_]{0,19}")&&code.equals(b.code()),422,"类型编码需大写字母/数字/下划线");
        Db.text(b.name(),100);BusinessException.require(Set.of("TEXT","NOTICE","LONG_IMAGE").contains(b.renderer()),422,"请选择已支持的渲染方式");
        BusinessException.require(!b.renderer().equals("LONG_IMAGE")||b.requireImage()&&!b.requireBody()&&!b.requireOriginal(),422,"长图渲染必须要求图片，且不要求正文或原文");
        BusinessException.require(!b.renderer().equals("NOTICE")||b.requireDates(),422,"通知渲染必须要求起止时间");
        var rows=db.rows("SELECT lock_version FROM content_type_definition WHERE code=? FOR UPDATE",code);
        BusinessException.require((rows.isEmpty()?0:Db.number(rows.get(0).get("lockVersion")))==b.lockVersion(),409,"类型已变化，请刷新");
        db.update("INSERT INTO content_type_definition(code,name,renderer,require_original,require_body,require_summary,require_image,require_dates,enabled,lock_version) VALUES(?,?,?,?,?,?,?,?,?,1) ON DUPLICATE KEY UPDATE name=VALUES(name),renderer=VALUES(renderer),require_original=VALUES(require_original),require_body=VALUES(require_body),require_summary=VALUES(require_summary),require_image=VALUES(require_image),require_dates=VALUES(require_dates),enabled=VALUES(enabled),lock_version=lock_version+1",code,b.name(),b.renderer(),b.requireOriginal(),b.requireBody(),b.requireSummary(),b.requireImage(),b.requireDates(),b.enabled());
        db.audit(actor.id(),"STAFF","CONTENT_TYPE_SAVE","CONTENT_TYPE",0,b);return ApiResponse.ok(null);
    }
}
