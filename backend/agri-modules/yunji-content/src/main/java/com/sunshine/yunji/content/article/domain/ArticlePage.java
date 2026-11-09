package com.sunshine.yunji.content.article.domain;

import java.util.List;
public record ArticlePage(List<ArticleCard> items, long total, int pageNum, int pageSize, String snapshotAt, String nextCursor) {}
