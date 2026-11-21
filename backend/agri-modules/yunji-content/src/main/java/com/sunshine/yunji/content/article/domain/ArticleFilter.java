package com.sunshine.yunji.content.article.domain;
import java.util.List;
public record ArticleFilter(String contentType,List<Long> labelIds,Long keywordId,Boolean includeExpired,List<Long> keywordIds,List<Long> categoryIds) {
 public ArticleFilter(String type,List<Long> labels,Long keyword,Boolean expired){this(type,labels,keyword,expired,List.of(),List.of());}
 public ArticleFilter(String type,List<Long> labels,Long keyword,Boolean expired,List<Long> keywords){this(type,labels,keyword,expired,keywords,List.of());}
 public List<String> contentTypes(){return contentType==null?List.of():java.util.Arrays.stream(contentType.split(",",-1)).map(String::trim).distinct().sorted().toList();}
 public static ArticleFilter legacy(){return new ArticleFilter(null,List.of(),null,null);}
}
