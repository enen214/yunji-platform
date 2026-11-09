package com.sunshine.yunji.content.notification.api;
import java.util.List;

/** Explicit public boundary: no SQL snapshots, lease tokens, or credentials. */
public final class ReminderViews {
    private ReminderViews(){}
    public record Permission(boolean wecomBound,int available,int reserved,int consumed){}
    public record Settings(List<String> slots,String timezone,boolean enabled,String slot,int lockVersion,
                           boolean localSimulation,boolean realProviderReady,String instantTemplate,String wecomState,List<Permission> permissions){}
    public record Control(boolean dailyEnabled,boolean instantEnabled,String timezone,List<String> slots,
                          String dailyTemplate,String instantTemplate,int lockVersion){}
    public record Tracking(String id,String userId,String targetType,String targetId,String title,String mode,
                           boolean enabled,int lockVersion,String createdAt,String contentType,String slot,int available,int pendingCount){}
    public record Rules(List<Tracking> items,long total,Integer page,Integer size){}
    public record Change(String id,String articleId,String versionId,String title,String eventType,String state,String createdAt){}
    public record Changes(List<Change> items,long total,int page,int size){}
    public record Batch(String id,String userId,String channel,String state,Integer attempts,String providerTaskId,
                        String errorCode,String createdAt,String completedAt,Long itemCount){}
    public record Batches(List<Batch> items,long total,int page,int size,boolean localSimulation){}
    public record Count(String channel,String state,long total){}
    public record Trend(String day,String channel,long created,long confirmed){}
    public record Failure(String channel,String errorCode,long total){}
    public record Source(String targetType,long total){}
    public record Statistics(List<Count> states,long pendingChanges,long activeRules,long trackedUsers,long enabledUsers,long dailyUsers,long instantUsers,boolean localSimulation,long taskCreated,long confirmed,List<Trend> trend,List<Failure> failures,List<Source> sources){}
    public record Tick(int planned,int processed,boolean localSimulation,boolean realProviderReady){}
}
