package com.sunshine.yunji.content.article.domain;

public record ArticleDetail(ArticleCard article, String richText, String sourceUrl, String versionId, String interpretation, String originalText,
                            java.util.List<Topic> categories,java.util.List<Topic> labels,java.util.List<Topic> keywords,
                            ArticlePresentation presentation,java.util.List<Topic> related) {
    public record Topic(String id,String name) {}
}
