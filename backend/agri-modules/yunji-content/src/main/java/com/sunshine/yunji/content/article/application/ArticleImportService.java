package com.sunshine.yunji.content.article.application;

import com.fasterxml.jackson.databind.*;
import com.sunshine.yunji.content.identity.application.Actors;
import com.sunshine.yunji.content.shared.*;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.time.format.*;
import java.util.*;

/** Bounded synchronous import with durable resumable receipts and one transaction per row. */
@Service
public class ArticleImportService {
    public static final int MAX_BYTES=5*1024*1024,MAX_ROWS=500;
    private final Db db;private final Actors actors;private final ArticleCommands commands;
    private final ObjectMapper mapper;private final TransactionTemplate tx;private final Validator validator;
    public record Failure(int index,String title,String reason) {}
    public record Result(String batchId,String status,int total,int processed,int successCount,int failCount,
                         List<Failure> failList,List<String> createdKeywords,List<String> duplicatedKeywords,String timezone) {}
    public ArticleImportService(Db db,Actors actors,ArticleCommands commands,ObjectMapper mapper,PlatformTransactionManager manager,Validator validator){
        this.db=db;this.actors=actors;this.commands=commands;this.mapper=mapper.copy().enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        this.validator=validator;this.tx=new TransactionTemplate(manager);this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    public Result run(String key,String content){
        long staff=actors.staff().id();
        BusinessException.require(key!=null&&key.matches("[A-Za-z0-9_-]{8,128}"),422,"需要8至128位幂等请求标识");
        BusinessException.require(content!=null&&!content.isBlank(),422,"导入内容不能为空");
        BusinessException.require(content.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=MAX_BYTES,413,"导入内容超过5MiB");
        JsonNode input;try{input=mapper.readTree(content);}catch(Exception e){throw new BusinessException(422,"JSON格式错误，请检查括号、逗号和重复字段");}
        BusinessException.require(input.isArray()&&!input.isEmpty(),422,"需要非空JSON数组");
        BusinessException.require(input.size()<=MAX_ROWS,422,"单批最多500条，尚未导入任何内容");
        String canonical=input.toString(),hash=Db.hash(canonical),lease=db.token();
        long batch=Objects.requireNonNull(tx.execute(status->{
            db.update("INSERT INTO article_import_batch(id,staff_id,request_key,payload_hash,payload,total) VALUES(?,?,?,?,?,?) ON DUPLICATE KEY UPDATE request_key=VALUES(request_key)",db.id(),staff,key,hash,canonical,input.size());
            var row=db.one("SELECT * FROM article_import_batch WHERE staff_id=? AND request_key=? FOR UPDATE",staff,key);
            BusinessException.require(hash.equals(row.get("payloadHash")),409,"同一请求标识对应了不同内容");
            long id=Db.number(row.get("id"));if(!"COMPLETED".equals(row.get("status"))){
                BusinessException.require(db.count("SELECT COUNT(*) FROM article_import_batch WHERE id=? AND lease_until>UTC_TIMESTAMP()",id)==0,409,"此批次正在处理，请稍后查询结果");
                db.update("UPDATE article_import_batch SET status='PROCESSING',lease_token=?,lease_until=UTC_TIMESTAMP()+INTERVAL 2 MINUTE WHERE id=?",lease,id);
            }return id;
        }));
        if("COMPLETED".equals(db.one("SELECT status FROM article_import_batch WHERE id=?",batch).get("status")))return result(batch);
        long deadline=System.nanoTime()+60_000_000_000L;
        try{
            for(int i=0;i<input.size()&&System.nanoTime()<deadline;i++){
                int index=i+1;JsonNode item=input.get(i);
                if(db.count("SELECT COUNT(*) FROM article_import_item WHERE batch_id=? AND row_index=?",batch,index)>0)continue;
                try{tx.executeWithoutResult(status->{
                    assertLease(batch,lease);
                    ArticleCommands.Draft draft=map(item);
                    var violations=validator.validate(draft);BusinessException.require(violations.isEmpty(),422,violations.stream().map(v->v.getPropertyPath()+":"+v.getMessage()).sorted().findFirst().orElse("参数错误"));
                    var article=commands.create(draft,batch,false);
                    db.update("INSERT INTO article_import_item(batch_id,row_index,title,article_id) VALUES(?,?,?,?)",batch,index,draft.title(),article.get("id"));
                });}catch(BusinessException e){
                    if(e.status==409&&e.getMessage().contains("批次锁"))throw e;
                    tx.executeWithoutResult(status->{assertLease(batch,lease);db.update("INSERT IGNORE INTO article_import_item(batch_id,row_index,title,error_message) VALUES(?,?,?,?)",batch,index,item.path("title").asText("").substring(0,Math.min(255,item.path("title").asText("").length())),e.getMessage());});
                }
            }
            tx.executeWithoutResult(status->{
                assertLease(batch,lease);
                if(db.count("SELECT COUNT(*) FROM article_import_item WHERE batch_id=?",batch)==input.size()){
                    for(var row:db.rows("SELECT article_id FROM article_import_item WHERE batch_id=? AND article_id IS NOT NULL ORDER BY row_index",batch))commands.detect(Db.number(row.get("articleId")),batch);
                    db.update("UPDATE article_import_batch SET status='COMPLETED',completed_at=UTC_TIMESTAMP(),lease_token=NULL,lease_until=NULL WHERE id=?",batch);
                }else db.update("UPDATE article_import_batch SET status='PARTIAL',lease_token=NULL,lease_until=NULL WHERE id=?",batch);
            });
        }catch(RuntimeException e){tx.executeWithoutResult(status->db.update("UPDATE article_import_batch SET status='PARTIAL',lease_token=NULL,lease_until=NULL WHERE id=? AND lease_token=?",batch,lease));throw e;}
        return result(batch);
    }
    private void assertLease(long id,String lease){BusinessException.require(db.count("SELECT COUNT(*) FROM article_import_batch WHERE id=? AND lease_token=? AND lease_until>UTC_TIMESTAMP()",id,lease)==1,409,"批次锁已过期，请重试原请求恢复处理");}
    private ArticleCommands.Draft map(JsonNode row){
        BusinessException.require(row.isObject(),422,"每条数据必须是JSON对象");
        var data=new LinkedHashMap<String,Object>();
        String category=required(row,"categoryName");var categories=db.rows("SELECT id FROM content_category WHERE name=? AND tenant_id='000000' AND status='ENABLED' AND deleted_at IS NULL",category);
        BusinessException.require(categories.size()==1,422,"板块不存在或名称不唯一："+category);long categoryId=Db.number(categories.get(0).get("id"));
        data.put("categoryIds",List.of(categoryId));var labels=new LinkedHashSet<Long>();
        for(String label:names(row,"labelNames")){
            var found=db.rows("SELECT id FROM content_label WHERE name=? AND category_id=? AND status='ENABLED' AND deleted_at IS NULL",label,categoryId);
            BusinessException.require(found.size()==1,422,"标签在板块下不存在或名称不唯一："+label);labels.add(Db.number(found.get(0).get("id")));
        }
        data.put("labelIds",labels);data.put("keywordNames",names(row,"keywordNames"));data.put("title",required(row,"title"));data.put("contentType",required(row,"contentType"));
        for(String field:List.of("sourceName","sourceUrl","hotDays","urgent","pinned","pushEnabled"))if(row.hasNonNull(field))data.put(field,mapper.convertValue(row.get(field),Object.class));
        for(var pair:List.of(new String[]{"levelOneContent","originalText"},new String[]{"levelTwoContent","richText"},new String[]{"levelThreeContent","summary"},new String[]{"importance","contentWeight"},new String[]{"timeLimitType","lifespan"},new String[]{"longImage","longImageUrl"}))if(row.hasNonNull(pair[0]))data.put(pair[1],mapper.convertValue(row.get(pair[0]),Object.class));
        for(String field:List.of("effectiveFrom","effectiveTo"))if(row.hasNonNull(field)&&!row.get(field).asText().isBlank()){
            try{data.put(field,LocalDateTime.parse(row.get(field).asText(),DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT)).atZone(com.sunshine.yunji.content.discovery.domain.FeedRanking.BUSINESS_ZONE).toInstant().toString());}catch(Exception e){throw new BusinessException(422,"时间应为yyyy-MM-dd HH:mm:ss，按Asia/Shanghai解释");}
        }
        try{return mapper.convertValue(data,ArticleCommands.Draft.class);}catch(IllegalArgumentException e){throw new BusinessException(422,"字段类型错误，请检查整数、布尔值和字符串");}
    }
    private String required(JsonNode row,String field){BusinessException.require(row.path(field).isTextual(),422,field+"必须是字符串");return Db.text(row.path(field).asText(),field.equals("title")?255:100);}
    private List<String> names(JsonNode row,String field){BusinessException.require(row.path(field).isArray()&&row.path(field).size()<=30,422,field+"需1至30个字符串");var values=new LinkedHashSet<String>();for(JsonNode value:row.get(field)){BusinessException.require(value.isTextual(),422,field+"含非字符串");if(!value.asText().isBlank())values.add(Db.text(value.asText(),100));}BusinessException.require(!values.isEmpty(),422,field+"不能为空");return List.copyOf(values);}
    public Result result(long id){
        long staff=actors.staff().id();var batch=db.one("SELECT * FROM article_import_batch WHERE id=? AND staff_id=?",id,staff);
        var rows=db.rows("SELECT * FROM article_import_item WHERE batch_id=? ORDER BY row_index",id);
        var failed=rows.stream().filter(r->r.get("errorMessage")!=null).map(r->new Failure((int)Db.number(r.get("rowIndex")),Objects.toString(r.get("title"),""),r.get("errorMessage").toString())).toList();
        // Names are traceable to this batch by creator time and successful article relations.
        var words=db.rows("SELECT DISTINCT k.display_name FROM keyword k JOIN article_keyword_rel rel ON rel.keyword_id=k.id JOIN article a ON a.id=rel.article_id JOIN article_import_batch b ON b.id=a.import_batch_id WHERE b.id=? AND k.import_batch_id=b.id",id).stream().map(r->r.get("displayName").toString()).toList();
        var reused=db.rows("SELECT DISTINCT k.display_name FROM keyword k JOIN article_keyword_rel rel ON rel.keyword_id=k.id JOIN article a ON a.id=rel.article_id WHERE a.import_batch_id=? AND (k.import_batch_id IS NULL OR k.import_batch_id<>?) ORDER BY k.display_name",id,id).stream().map(r->r.get("displayName").toString()).toList();
        return new Result(Long.toString(id),batch.get("status").toString(),(int)Db.number(batch.get("total")),rows.size(),rows.size()-failed.size(),failed.size(),failed,words,reused,"Asia/Shanghai");
    }
}
