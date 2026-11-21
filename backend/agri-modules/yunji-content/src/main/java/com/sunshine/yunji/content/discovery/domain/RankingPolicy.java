package com.sunshine.yunji.content.discovery.domain;

public final class RankingPolicy {
    private RankingPolicy() {}
    /** 排序只用重要度与热点；时效分已在 FeedSql.score 用半衰期表示，不保留独立权重维度（L10）。 */
    public static int score(boolean pinned, int content, int hot) {
        if (content < 0 || content > 5 || hot < 0 || hot > 10)
            throw new IllegalArgumentException("权重超出允许范围");
        return (pinned ? 100 : 0) + content + hot;
    }
    public static boolean inCoolingWindow(int visibleExposures, boolean read, boolean clicked) {
        if (visibleExposures < 0) throw new IllegalArgumentException("曝光次数不能为负数");
        return read || clicked || visibleExposures >= 3;
    }
}
