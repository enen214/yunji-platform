package com.sunshine.yunji.content.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.security.*;
import java.util.*;
import java.util.regex.*;

/** JDBC boundary: all values bound, table names restricted by domain code. IDs never cross JSON as numbers. */
@Component
public class Db {
    public final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final SecureRandom random=new SecureRandom();
    public Db(JdbcTemplate jdbc,ObjectMapper mapper) {this.jdbc=jdbc;this.mapper=mapper;}
    public long id() {return random.nextLong() & Long.MAX_VALUE;}
    public static String hash(String value) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    public String token(){byte[] b=new byte[32];random.nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
    public String json(Object value){try{return mapper.writeValueAsString(value);}catch(Exception e){throw new BusinessException(422,"无法序列化请求");}}
    @SuppressWarnings("unchecked") public Map<String,Object> object(String value){try{return mapper.readValue(value,Map.class);}catch(Exception e){throw new IllegalStateException(e);}}
    public int update(String sql,Object...args){return jdbc.update(sql,args);}
    public long count(String sql,Object...args){Long n=jdbc.queryForObject(sql,Long.class,args);return n==null?0:n;}
    public List<Map<String,Object>> rows(String sql,Object...args){
        return jdbc.query(sql,(r,n)->{Map<String,Object> row=new LinkedHashMap<>();var meta=r.getMetaData();
            for(int i=1;i<=meta.getColumnCount();i++) {String key=meta.getColumnLabel(i);Object v=r.getObject(i);
                if(v instanceof Number&&(key.equals("id")||key.endsWith("_id")))v=v.toString();
                if(v instanceof java.sql.Timestamp t)v=t.toInstant().toString();
                if(v instanceof java.time.LocalDateTime t)v=t.toInstant(java.time.ZoneOffset.UTC).toString();
                if(v instanceof java.sql.Date d)v=d.toLocalDate().toString();
                if(v instanceof java.sql.Time t)v=t.toLocalTime().toString();
                Matcher m=Pattern.compile("_([a-z])").matcher(key);StringBuffer out=new StringBuffer();while(m.find())m.appendReplacement(out,m.group(1).toUpperCase());m.appendTail(out);
                row.put(out.toString(),v);
            }return row;},args);
    }
    public Map<String,Object> one(String sql,Object...args){var rows=rows(sql,args);BusinessException.require(!rows.isEmpty(),404,"对象不存在或不可访问");return rows.get(0);}
    public void audit(long actor,String realm,String action,String type,long id,Object data){update("INSERT INTO audit_event(id,actor_id,actor_client,action,object_type,object_id,after_json) VALUES(?,?,?,?,?,?,?)",id(),actor,realm,action,type,id,json(data));}
    public void event(String type,long id,String event,Object data){update("INSERT INTO outbox_event(id,aggregate_type,aggregate_id,event_type,payload) VALUES(?,?,?,?,?)",id(),type,id,event,json(data));}
    public static String text(String s,int max){BusinessException.require(s!=null&&!s.isBlank()&&s.length()<=max,422,"文本为空或超过长度限制");return s.strip();}
    public static long number(Object n){if(n instanceof Boolean b)return b?1:0;try{return Long.parseLong(String.valueOf(n));}catch(Exception e){throw new BusinessException(422,"ID或版本号格式错误");}}
}
