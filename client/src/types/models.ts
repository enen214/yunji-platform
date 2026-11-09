// Generated from contracts/view-models.json. IDs are strings; nullable database fields stay explicit.
export interface CodeView {
 code: string;
 expiresIn: number;
}
export interface CustomerSessionView {
 accessToken: string;
 expiresIn: number;
 userId: string;
 realm: 'CUSTOMER';
}
export interface ProfileView {
 id: string;
 nickname: string | null;
 avatarUrl: string | null;
 phoneMasked: string | null;
 organizationId: string | null;
 status: string;
}
export interface StaffSessionView {
 userId: string;
 departmentId: string;
 realm: 'STAFF';
 superAdmin: boolean;
 permissions: string[];
}
export interface ArticleVersionView {
 id: string;
 articleId: string;
 versionNo: number;
 title: string;
 summary: string | null;
 coverUrl: string | null;
 sourceName: string | null;
 sourceUrl: string | null;
 contentType: string;
 effectiveFrom: string | null;
 effectiveTo: string | null;
 richText: string | null;
 relationSnapshot: string;
 changeSummary: string | null;
 createdBy: string | null;
 createdAt: string;
 reviewState: string;
 submittedAt: string | null;
 decidedAt: string | null;
 pinned: boolean;
 contentWeight: number;
 hotWeight: number;
 freshnessWeight: number;
 interpretation: string | null;
 originalText: string | null;
 carrierType: 'TEXT_IMAGE' | 'LONG_IMAGE' | 'FILE';
}
export interface ArticleReviewView {
 id: string;
 articleId: string;
 versionId: string;
 action: string;
 comment: string | null;
 operatorId: string;
 operatorDeptId: string | null;
 createdAt: string;
 requestId: string | null;
}
export interface SubjectView {
 id: string;
 tenantId: string;
 organizationId: string;
 code: string;
 name: string;
 status: string;
 lockVersion: number;
}
export interface DimensionView {
 id: string;
 tenantId: string;
 kind: string;
 code: string;
 name: string;
 status: string;
 lockVersion: number;
}
export interface ExamView {
 id: string;
 tenantId: string;
 organizationId: string;
 subjectId: string;
 startsAt: string;
 displayTimezone: string;
 sourceReference: string;
 lockVersion: number;
 status: string;
 subjectName: string | null;
 secondsRemaining: number | null;
}
export interface ArtifactVersionView {
 id: string;
 artifactId: string;
 versionName: string;
 fileId: string;
 sha256: string;
 releaseNotes: string;
 publishedAt: string;
 status: string;
}
export interface ResourceFileView {
 packageId: string;
 fileId: string;
 versionNo: number;
 displayName: string;
 sortOrder: number;
 sha256: string | null;
 sizeBytes: number | null;
 mediaType: string | null;
}
export interface ActivityFormView {
 id: string;
 activityId: string | null;
 versionNo: number;
 schemaJson: string;
 createdAt: string | null;
}
export interface RegistrationView {
 id: string;
 activityId: string;
 userId: string;
 formVersionId: string;
 answerSnapshot: string;
 status: 'CONFIRMED' | 'WAITLISTED' | 'CANCELLED';
 queueSeq: number;
 submittedAt: string;
 cancelledAt: string | null;
 lockVersion: number;
 title: string | null;
 eventStart: string | null;
}
export interface ReviewRequirementView {
 campaignId: string;
 ruleVersion: number;
 requirementKey: string;
 label: string;
 required: boolean;
 fileTypeAllowlist: string;
 maxSize: number;
 eligibilityJson: string;
}
export interface ReviewSubmissionView {
 id: string;
 applicationId: string;
 versionNo: number;
 requirementSnapshot: string;
 eligibilitySnapshot: string;
 submittedAt: string | null;
}
export interface ReviewMaterialView {
 submissionId: string;
 requirementKey: string;
 fileId: string;
}
export interface ReviewDecisionView {
 id: string;
 applicationId: string;
 submissionId: string;
 decision: string;
 comment: string;
 reviewerId: string;
 decidedAt: string;
}
export interface BannerView {
 id: string;
 tenantId: string;
 title: string;
 imageUrl: string;
 linkType: string;
 linkValue: string | null;
 sortOrder: number;
 status: string;
 effectiveFrom: string | null;
 effectiveTo: string | null;
 createdBy: string | null;
 createdAt: string;
 updatedAt: string;
 deletedAt: string | null;
}
export interface RecommendationView {
 id: string;
 articleId: string;
 coverUrl: string | null;
 sortOrder: number;
 status: string;
 effectiveFrom: string | null;
 effectiveTo: string | null;
 createdBy: string | null;
 createdAt: string;
}
export interface ColumnView {
 id: string;
 tenantId: string;
 name: string;
 categoryId: string;
 sortOrder: number;
 status: string;
}
export interface ConsentPageView {
 id: string;
 tenantId: string;
 pageKey: string;
 title: string;
 content: string;
 versionNo: number;
 status: string;
 updatedBy: string | null;
 updatedAt: string;
}
export interface FeedbackView {
 id: string;
 tenantId: string;
 userId: string;
 type: string;
 description: string;
 status: string;
 handlerId: string | null;
 handledAt: string | null;
 createdAt: string;
}
export interface ReportView {
 id: string;
 userId: string;
 articleId: string;
 reportType: string;
 reason: string | null;
 status: string;
 handledBy: string | null;
 handledAt: string | null;
 createdAt: string;
}
export interface FollowView {
 id: string;
 userId: string;
 targetType: string;
 targetId: string;
 createdAt: string;
 deletedAt: string | null;
 name: string | null;
 hidden: boolean | null;
 pushEnabled: boolean;
 labels: FollowLabelView[];
}
export interface RuleView {
 id: string;
 userId: string;
 channel: string;
 enabled: boolean;
 quietStart: string | null;
 quietEnd: string | null;
 createdAt: string;
 updatedAt: string;
 tenantId: string;
 targetType: string;
 targetId: string;
 mode: string;
 timezone: string;
 scheduledAt: string | null;
 localSendTime: string | null;
 ruleHash: string;
 lockVersion: number;
 nextRunAt: string | null;
}
export interface DeliveryView {
 id: string;
 userId: string;
 articleId: string | null;
 channel: string;
 templateKey: string | null;
 status: string;
 providerMessageId: string | null;
 errorCode: string | null;
 sentAt: string | null;
 createdAt: string;
 ruleId: string | null;
 ruleVersion: number;
 dedupeKey: string | null;
 articleVersionId: string | null;
 attempts: number;
 nextAttemptAt: string;
 leaseUntil: string | null;
 quotaReserved: boolean;
 ruleSnapshot: string | null;
 contentSnapshot: string | null;
 operation: string | null;
 amount: number | null;
}
export interface AliasView {
 id: string;
 tenantId: string;
 normalizedAlias: string;
 keywordId: string;
}
export interface ResourceView {
 id: string;
 tenantId: string;
 organizationId: string;
 subjectId: string;
 title: string;
 resourceType: string;
 status: 'DRAFT' | 'PUBLISHED' | 'OFFLINE';
 articleId: string | null;
 lockVersion: number;
 currentVersionNo: number;
 subjectName: string | null;
 files: ResourceFileView[] | null;
}
export interface ArtifactView {
 id: string;
 tenantId: string;
 name: string;
 platform: string;
 status: 'DRAFT' | 'PUBLISHED' | 'OFFLINE';
 guideArticleId: string | null;
 lockVersion: number;
 versions: ArtifactVersionView[] | null;
}
export interface ArticleAdminView {
 id: string;
 tenantId: string;
 title: string;
 summary: string | null;
 coverUrl: string | null;
 sourceType: string;
 sourceName: string | null;
 sourceUrl: string | null;
 contentType: string;
 status: 'DRAFT' | 'PUBLISHED' | 'OFFLINE';
 visibility: string;
 currentVersionId: string | null;
 latestVersionId: string | null;
 effectiveFrom: string | null;
 effectiveTo: string | null;
 pinned: boolean;
 contentWeight: number;
 hotWeight: number;
 freshnessWeight: number;
 publishedAt: string | null;
 offlineAt: string | null;
 createdBy: string | null;
 createdDeptId: string | null;
 createdAt: string;
 updatedBy: string | null;
 updatedAt: string;
 versionNo: number;
 deletedAt: string | null;
 lockVersion: number;
 submittedVersionId: string | null;
 versions: ArticleVersionView[] | null;
 reviews: ArticleReviewView[] | null;
 carrierType: 'TEXT_IMAGE' | 'LONG_IMAGE' | 'FILE';
 abnormal: boolean;
}
export interface ActivityView {
 id: string;
 tenantId: string;
 articleId: string | null;
 title: string;
 organizationId: string;
 registrationStart: string;
 registrationEnd: string;
 eventStart: string;
 eventEnd: string;
 capacity: number;
 confirmedCount: number;
 waitlistEnabled: boolean;
 status: 'DRAFT' | 'PUBLISHED' | 'CLOSED';
 lockVersion: number;
 createdBy: string;
 createdAt: string;
 remaining: number | null;
 form: ActivityFormView | null;
 registrations: RegistrationView[] | null;
}
export interface CampaignView {
 id: string;
 tenantId: string;
 organizationId: string;
 articleId: string | null;
 title: string;
 openAt: string;
 closeAt: string;
 ruleVersion: number;
 status: 'DRAFT' | 'PUBLISHED' | 'CLOSED';
 createdBy: string;
 lockVersion: number;
 requirements: ReviewRequirementView[] | null;
}
export interface ApplicationView {
 id: string;
 campaignId: string;
 userId: string;
 status: 'DRAFT' | 'SUBMITTED' | 'WITHDRAWN' | 'APPROVED' | 'REJECTED' | 'CORRECTION';
 currentSubmissionId: string | null;
 latestSubmissionId: string | null;
 lockVersion: number;
 title: string | null;
 organizationId: string | null;
 submissions: ReviewSubmissionView[] | null;
 files: ReviewMaterialView[] | null;
 decisions: ReviewDecisionView[] | null;
}
export interface FileView {
 id: string;
 ownerRealm: string | null;
 ownerId: string | null;
 sha256: string;
 sizeBytes: number;
 mediaType: string;
 displayName: string;
 privacy: string;
 status: 'UPLOADED' | 'VERIFIED' | 'REJECTED' | 'DELETED' | 'PURGED';
 createdAt: string | null;
}
export interface TicketView {
 ticket: string;
 expiresIn: number;
 path: string;
}
export interface CleanupView {
 dryRun: boolean;
 retentionDays: number;
 candidates: number;
 removed: number;
 alreadyMissing: number;
 purged: number;
 failed: number;
 failedIds: string[];
}
export interface EligibilityView {
 eligible: boolean;
 failedRequirementKeys: string[];
}
export interface RequestView {
 id: string;
 status: string;
}
export interface ReplacementResultView {
 oldArticleId: string;
 newArticleId: string;
}
export interface ArchivedView {
 id: string;
 title: string;
 summary: string | null;
 currentVersionId: string;
 effectiveTo: string | null;
}
export interface FavoriteView {
 id: string;
 title: string;
 summary: string | null;
 currentVersionId: string;
 createdAt: string | null;
}
export interface HistoryView {
 id: string;
 title: string;
 lastBrowsedAt: string;
 browseCount: number;
}
export interface BoxItemView {
 id: string;
 title: string;
 currentVersionId: string;
 unread: number;
 contentType: string;
 publishedAt: string | null;
}
export interface ReplacementTargetView {
 id: string;
 title: string;
 versionId: string;
}
export interface ReplacementHintView {
 articleId: string;
 status: 'NONE' | 'AVAILABLE' | 'UNAVAILABLE';
 target: ReplacementTargetView | null;
}
export interface BoxView {
 id: string;
 userId: string;
 targetType: string;
 targetId: string;
 createdAt: string;
 deletedAt: string | null;
 name: string | null;
 hidden: boolean | null;
 items: BoxItemView[];
 unreadCount: number;
 replacement: ReplacementHintView | null;
 boxKind: 'CATEGORY' | 'FOLLOWING' | 'REPLACEMENT';
 pushEnabled: boolean;
}
export interface PageView {
 versionId: string;
 pageKey: string;
 versionNo: number;
 title: string;
 content: string;
 publishedAt: string;
}
export interface HomeView {
 banners: HomeBannerView[];
 recommendations: HomeRecommendationView[];
 activityColumns: ColumnView[];
}
export interface AuditView {
 id: string;
 actorId: string;
 actorClient: string;
 action: string;
 objectType: string;
 objectId: string;
 requestId: string | null;
 createdAt: string;
}
export interface MetricView {
 metricDate: string | null;
 metricKey: string;
 value: number;
}
export interface ContentMetricView {
 id: string;
 title: string;
 events: number;
 viewCount: number;
 viewerCount: number;
 favoriteCount: number;
}
export interface AnalyticsView {
 publicationCount: number;
 content: ContentMetricView[];
 daily: MetricView[];
}
export interface AggregateView {
 date: string;
 timezone: string;
 metrics: MetricView[];
}
export interface BindingView {
 provider: string;
 appId: string;
 bindingState: string;
}
export interface QuotaView {
 id: string;
 appId: string;
 channel: string;
 templateKey: string;
 available: number;
 reserved: number;
 consumed: number;
}
export interface NotificationStatusView {
 bindings: BindingView[];
 quotas: QuotaView[];
 platformStatus: string;
}
export interface TickView {
 planned: number;
 processed: number;
 platformStatus: string;
}
export interface DepartmentScopeView {
 departmentId: string;
}
export interface ReviewScopeView {
 organizationId: string;
}
export interface AccessUserView {
 id: string;
 userName: string;
 nickName: string;
 deptId: string;
 status: string;
 departmentScope: DepartmentScopeView[];
 reviewScope: ReviewScopeView[];
}
export interface CatalogEntryView {
 id: string | null;
 tenantId: string | null;
 parentId: string | null;
 code: string | null;
 name: string | null;
 description: string | null;
 sortOrder: number | null;
 status: string | null;
 createdBy: string | null;
 createdAt: string | null;
 updatedBy: string | null;
 updatedAt: string | null;
 deletedAt: string | null;
 lockVersion: number | null;
 type: string | null;
 shortName: string | null;
 logoUrl: string | null;
 displayName: string | null;
 normalizedName: string | null;
}
export interface HomeBannerView {
 id: string;
 title: string;
 imageUrl: string;
 linkType: string;
 linkValue: string;
}
export interface HomeRecommendationView {
 id: string;
 articleId: string;
 title: string;
 summary: string | null;
 coverUrl: string | null;
 contentType: string;
 sourceName: string | null;
 publishedAt: string | null;
}
export interface ModerationView {
 id: string | null;
 userId: string | null;
 articleId: string | null;
 reportType: string | null;
 reason: string | null;
 status: string | null;
 handledBy: string | null;
 handledAt: string | null;
 createdAt: string | null;
 tenantId: string | null;
 type: string | null;
 description: string | null;
 handlerId: string | null;
}
export interface AdminPageSummaryView {
 id: string;
 pageKey: string;
 title: string;
 versionNo: number;
 status: string;
}
export interface NotificationDeliverySummaryView {
 id: string;
 articleId: string | null;
 channel: string;
 status: string;
 errorCode: string | null;
 sentAt: string | null;
 createdAt: string;
 ruleId: string | null;
 articleVersionId: string | null;
 attempts: number;
}
export interface PublicActivityView {
 id: string;
 articleId: string | null;
 title: string;
 organizationId: string;
 registrationStart: string;
 registrationEnd: string;
 eventStart: string;
 eventEnd: string;
 capacity: number;
 confirmedCount: number;
 waitlistEnabled: boolean;
 status: 'DRAFT' | 'PUBLISHED' | 'CLOSED';
 createdAt: string;
 remaining: number | null;
 form: ActivityFormView | null;
}
export interface PublicCampaignView {
 id: string;
 organizationId: string;
 articleId: string | null;
 title: string;
 openAt: string;
 closeAt: string;
 ruleVersion: number;
 status: 'DRAFT' | 'PUBLISHED' | 'CLOSED';
 requirements: ReviewRequirementView[] | null;
}
export interface PublicResourceView {
 id: string;
 organizationId: string;
 subjectId: string;
 title: string;
 resourceType: string;
 status: 'DRAFT' | 'PUBLISHED' | 'OFFLINE';
 articleId: string | null;
 currentVersionNo: number;
 subjectName: string | null;
 files: ResourceFileView[] | null;
}
export interface PublicArtifactView {
 id: string;
 name: string;
 platform: string;
 status: 'DRAFT' | 'PUBLISHED' | 'OFFLINE';
 guideArticleId: string | null;
 versions: ArtifactVersionView[] | null;
}
export interface PublicSubjectView {
 id: string;
 organizationId: string;
 code: string;
 name: string;
 status: string;
}
export interface PublicDimensionView {
 id: string;
 kind: string;
 code: string;
 name: string;
 status: string;
}
export interface PublicExamView {
 id: string;
 organizationId: string;
 subjectId: string;
 startsAt: string;
 displayTimezone: string;
 sourceReference: string;
 status: string;
 subjectName: string | null;
 secondsRemaining: number | null;
}
export interface FollowLabelView {
 id: string;
 name: string;
 categoryId: string;
 categoryName: string;
}
