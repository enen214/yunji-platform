package com.sunshine.yunji.content;

import com.sunshine.yunji.content.shared.contract.*;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ContractBoundaryTest {
 @Test void jdbcBooleanAndLargeIdentifiersKeepTheirJsonMeaning(){
  var activity=ViewMapper.of(Map.of("id","91000000000000001","waitlistEnabled",true,"confirmedCount",1,"objectKey","private/path"),Views.PublicActivityView.class);
  assertEquals("91000000000000001",activity.id());assertTrue(activity.waitlistEnabled());assertEquals(1,activity.confirmedCount());
 }
 @Test void publicHomeIncludesContentButRejectsUnlistedPrivateProperties(){
  var home=ViewMapper.of(Map.of("banners",java.util.List.of(),"activityColumns",java.util.List.of(),"recommendations",java.util.List.of(Map.of("id","1","articleId","2","title","Published title","summary","Summary","createdBy","private-staff"))),Views.HomeView.class);
  assertEquals("Published title",home.recommendations().get(0).title());assertEquals("Summary",home.recommendations().get(0).summary());
  assertFalse(java.util.Arrays.stream(home.recommendations().get(0).getClass().getRecordComponents()).anyMatch(c->c.getName().equals("createdBy")));
 }
}
