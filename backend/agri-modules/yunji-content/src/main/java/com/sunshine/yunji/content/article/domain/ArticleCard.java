package com.sunshine.yunji.content.article.domain;

import java.time.Instant;

public record ArticleCard(String id, String title, String summary, String coverUrl,
    String contentType, String sourceName, Instant publishedAt, Instant effectiveTo, int score, Instant effectiveFrom, String carrierType, boolean abnormal, String lifecycleState, String categoryName) {}
