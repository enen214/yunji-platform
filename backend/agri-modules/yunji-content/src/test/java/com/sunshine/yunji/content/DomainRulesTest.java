package com.sunshine.yunji.content;

import com.sunshine.yunji.content.article.domain.*;
import com.sunshine.yunji.content.discovery.domain.FeedRanking;
import com.sunshine.yunji.content.discovery.domain.FeedSql;
import com.sunshine.yunji.content.discovery.domain.RankingPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class DomainRulesTest {
    @Test void pinnedAlwaysRanksAboveOrdinaryWithinWeightBounds() {
        assertTrue(RankingPolicy.score(true,0,0)>RankingPolicy.score(false,5,10));
    }
    @Test void invalidWeightsCannotMakeScoreUnbounded() {
        assertThrows(IllegalArgumentException.class,()->RankingPolicy.score(false,6,0));
    }
    @Test void exposuresDoNotImplyRead() {
        assertTrue(RankingPolicy.inCoolingWindow(3,false,false));
        assertFalse(RankingPolicy.inCoolingWindow(2,false,false));
    }
    @ParameterizedTest(name="importance={0}, hotDays={1}, lifespan={2}, matches={3}, exposures={4}")
    @MethodSource("rankingMatrix")
    void feedRankingMatchesSqlExpression(int importance,int hotDays,String lifespan,int matches,int exposures) {
        Instant now=Instant.parse("2026-01-01T00:00:00Z");
        Instant published=now.minus(Duration.ofHours(10));
        Instant effective=now.minus(Duration.ofHours(4));
        assertEquals(feedSqlValue(importance,hotDays,lifespan,published,effective,now,matches,exposures,false),
            FeedRanking.score(importance,hotDays,lifespan,published,effective,now,matches,exposures),1e-9,"unpinned");
        assertEquals(feedSqlValue(importance,hotDays,lifespan,published,effective,now,matches,exposures,true),
            FeedRanking.score(importance,hotDays,lifespan,published,effective,now,matches,0),1e-9,"pinned exempts exposure");
    }
    @Test void sqlScoreKeepsPinnedAndNineExposureLayers() {
        String sql=FeedSql.score(true);
        assertTrue(sql.contains("CASE WHEN a.pinned=1 THEN 1 ELSE"));
        assertTrue(sql.contains("WHEN 8 THEN .3 ELSE 0 END"));
        assertTrue(sql.contains("WHEN 1 THEN 1.5 ELSE 2 END"));
    }
    private static Stream<Arguments> rankingMatrix() {
        int[] hotDays={0,1,5,10,30};
        String[] lifespans={"URGENT","SHORT","SCHEDULED","LONG"};
        return IntStream.rangeClosed(1,5).boxed().flatMap(importance ->
            IntStream.of(hotDays).boxed().flatMap(hot ->
                Stream.of(lifespans).flatMap(lifespan ->
                    IntStream.rangeClosed(0,2).boxed().flatMap(matches ->
                        IntStream.rangeClosed(0,9).mapToObj(exposures ->
                            Arguments.of(importance,hot,lifespan,matches,exposures))))));
    }
    private static double feedSqlValue(int importance,int hotDays,String lifespan,Instant published,Instant effective,
                                       Instant now,int matches,int exposures,boolean pinned) {
        int hot=Math.min(30,Math.max(0,hotDays));
        double half=switch(lifespan){case "URGENT"->6;case "SHORT"->24;case "SCHEDULED"->48;default->168;};
        Instant anchor="SCHEDULED".equals(lifespan)&&effective!=null?effective:published;
        double age=Math.max(0,Duration.between(anchor,now).getSeconds()/3600.0);
        double base=10*importance+5*hot+50*Math.pow(2,-age/half);
        double matched=matches==0?1:matches==1?1.5:2;
        double exposure=switch(exposures){case 0,1->1;case 2->.95;case 3->.9;case 4->.8;case 5->.7;case 6->.6;case 7->.45;case 8->.3;default->0;};
        return base*matched*(pinned?1:exposure);
    }
}
