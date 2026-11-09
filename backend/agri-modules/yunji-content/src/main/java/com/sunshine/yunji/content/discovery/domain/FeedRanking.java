package com.sunshine.yunji.content.discovery.domain;

import java.time.*;

/** Product formula. Visibility and special priority tiers are evaluated separately. */
public final class FeedRanking {
    public static final ZoneId BUSINESS_ZONE=ZoneId.of("Asia/Shanghai");
    private static final double[] EXPOSURE={1,1,.95,.9,.8,.7,.6,.45,.3};
    private FeedRanking() {}
    public static double exposure(int count) {
        if(count<0)throw new IllegalArgumentException("negative exposure");
        return count>=9?0:EXPOSURE[count];
    }
    public static double score(int importance,int hotDays,String lifespan,Instant published,Instant effective,
                               Instant now,int matches,int exposures) {
        if(importance<1||importance>5||hotDays<0||hotDays>30||matches<0)throw new IllegalArgumentException("ranking inputs");
        double half=switch(lifespan){case "URGENT"->6;case "SHORT"->24;case "SCHEDULED"->48;case "LONG"->168;default->throw new IllegalArgumentException("lifespan");};
        Instant anchor="SCHEDULED".equals(lifespan)&&effective!=null?effective:published;
        double age=Math.max(0,Duration.between(anchor,now).toMillis()/3600000.0);
        return (importance*10+hotDays*5+50*Math.pow(2,-age/half))*(matches==0?1:matches==1?1.5:2)*exposure(exposures);
    }
}
