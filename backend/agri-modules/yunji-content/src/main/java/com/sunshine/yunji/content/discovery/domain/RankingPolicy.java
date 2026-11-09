package com.sunshine.yunji.content.discovery.domain;

public final class RankingPolicy {
    private RankingPolicy() {}
    public static int score(boolean pinned, int content, int hot, int freshness) {
        if (content < 0 || content > 5 || hot < 0 || hot > 10 || freshness < 0 || freshness > 5)
            throw new IllegalArgumentException("权重超出允许范围");
        return (pinned ? 100 : 0) + content + hot + freshness * 2;
    }
    public static boolean inCoolingWindow(int visibleExposures, boolean read, boolean clicked) {
        if (visibleExposures < 0) throw new IllegalArgumentException("曝光次数不能为负数");
        return read || clicked || visibleExposures >= 3;
    }
}
