package com.sunshine.yunji.content.article.domain;

/** Public replacement guidance never contains withdrawn content or invisible target identifiers. */
public record ReplacementView(String articleId, Status status, Target target) {
    public enum Status { NONE, AVAILABLE, UNAVAILABLE }
    public record Target(String id, String title, String versionId) {}
}
