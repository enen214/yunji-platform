package com.sunshine.yunji.content;

import com.sunshine.yunji.content.article.domain.*;
import com.sunshine.yunji.content.discovery.domain.RankingPolicy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DomainRulesTest {
    @Test void pinnedAlwaysRanksAboveOrdinaryWithinWeightBounds() {
        assertTrue(RankingPolicy.score(true,0,0,0)>RankingPolicy.score(false,5,10,5));
    }
    @Test void invalidWeightsCannotMakeScoreUnbounded() {
        assertThrows(IllegalArgumentException.class,()->RankingPolicy.score(false,6,0,0));
    }
    @Test void exposuresDoNotImplyRead() {
        assertTrue(RankingPolicy.inCoolingWindow(3,false,false));
        assertFalse(RankingPolicy.inCoolingWindow(2,false,false));
    }
}
