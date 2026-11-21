package com.sunshine.yunji.content;
import com.sunshine.yunji.content.discovery.domain.FeedRanking;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
class FeedRankingTest {
 private final Instant now=Instant.parse("2026-10-27T00:00:00Z");
 @Test void exactFreshBaseAndInterest(){assertEquals(110,FeedRanking.score(1,10,"LONG",now,null,now,0,0),.00001);assertEquals(165,FeedRanking.score(1,10,"LONG",now,null,now,1,0),.00001);assertEquals(220,FeedRanking.score(1,10,"LONG",now,null,now,5,0),.00001);}
 @Test void halfLives(){assertEquals(35,FeedRanking.score(1,0,"URGENT",now.minusSeconds(6*3600),null,now,0,0),.00001);assertEquals(35,FeedRanking.score(1,0,"LONG",now.minusSeconds(7*86400),null,now,0,0),.00001);}
 @Test void scheduledFutureIsNotDecayed(){assertEquals(60,FeedRanking.score(1,0,"SCHEDULED",now.minusSeconds(86400),now.plusSeconds(3600),now,0,0),.00001);}
 @Test void ninthExposureIsImplicitRead(){assertEquals(.3,FeedRanking.exposure(8));assertEquals(0,FeedRanking.exposure(9));assertEquals(0,FeedRanking.score(5,30,"LONG",now,null,now,10,9));}
 @Test void inputBounds(){assertThrows(IllegalArgumentException.class,()->FeedRanking.score(0,0,"LONG",now,null,now,0,0));assertThrows(IllegalArgumentException.class,()->FeedRanking.score(1,31,"LONG",now,null,now,0,0));}
}
