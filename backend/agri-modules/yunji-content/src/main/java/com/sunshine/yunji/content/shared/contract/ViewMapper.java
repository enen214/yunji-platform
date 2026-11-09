package com.sunshine.yunji.content.shared.contract;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import java.util.List;

/** Internal query maps never cross the HTTP boundary. Unlisted database fields are excluded. */
public final class ViewMapper {
 private static final ObjectMapper JSON=new ObjectMapper().findAndRegisterModules().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
 private ViewMapper(){}
 public static <T> T of(Object value,Class<T> type){try{return JSON.convertValue(value,type);}catch(IllegalArgumentException e){throw new IllegalArgumentException("DTO "+type.getSimpleName()+" mapping failed: "+e.getMessage(),e);}}
 public static <T> List<T> list(List<?> values,Class<T> type){return values.stream().map(v->of(v,type)).toList();}
}
