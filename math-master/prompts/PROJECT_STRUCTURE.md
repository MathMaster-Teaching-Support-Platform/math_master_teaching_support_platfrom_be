# Math Master — Project Structure

> **Auto-maintained:** Agent phải cập nhật file này mỗi khi thêm/xóa/rename file trong src.  
> **Cập nhật lần cuối:** 2026-04-17  
> **Hướng dẫn cập nhật:** Xem [AGENT_STRUCTURE_GUIDE.md](AGENT_STRUCTURE_GUIDE.md)

---

## Tech Stack

| Layer          | Công nghệ                                       |
| -------------- | ----------------------------------------------- |
| Language       | Java 21                                         |
| Framework      | Spring Boot 3.5.0                               |
| Build          | Maven (mvnw)                                    |
| Database       | PostgreSQL + Flyway migrations                  |
| Cache / Stream | Redis (Lettuce) + Redis Streams                 |
| Object Storage | MinIO (S3-compatible) + AWS SDK v2              |
| Realtime       | Centrifugo (WebSocket)                          |
| AI             | Gemini (Google) via HTTP                        |
| Auth           | Spring Security + OAuth2 Resource Server + JWT  |
| Docs           | Springdoc OpenAPI (Swagger)                     |
| Email          | Spring Mail + Thymeleaf templates               |
| Office Export  | Apache POI (PPTX), JLatexMath, PDFBox           |
| ORM            | Spring Data JPA + Hypersistence Utils           |
| Misc           | Lombok, GraalVM JS engine, OkHttp, Google OAuth |

---

## Root Files

```
pom.xml                   — Maven build config
docker-compose.yml        — Docker services (DB, Redis, MinIO, Centrifugo…)
Dockerfile                — App container build
nginx.conf                — Reverse proxy config
centrifugo-config.json    — Centrifugo realtime config
cors-config.xml           — MinIO CORS config
init-minio.sh             — MinIO bucket initialization
mvnw / mvnw.cmd           — Maven wrapper
```

---

## Source Code — `src/main/java/com/fptu/math_master/`

### Entry Point

```
MathMasterApplication.java
```

---

### controller/ — REST Controllers

```
AdminDashboardController.java
AdminRoadmapController.java
AdminSubscriptionController.java
AdminUserController.java
AssessmentController.java
AuthenticationController.java
CanonicalQuestionController.java
ChapterController.java
ChatSessionController.java
CourseController.java
CourseLessonController.java
CurriculumController.java
EnrollmentController.java
ExamMatrixController.java
GeminiController.java
GradingController.java
LatexRenderController.java
LessonContentController.java
LessonPlanController.java
LessonSlideController.java
LessonSlidePublicController.java
MindmapController.java
NotificationController.java
PaymentController.java
PermissionController.java
ProgressController.java
QuestionBankController.java
QuestionController.java
QuestionTemplateController.java
ResourceController.java
RoadmapTopicResourceController.java
RoleController.java
SchoolGradeController.java
StudentAssessmentController.java
StudentRoadmapController.java
SubjectController.java
SubscriptionController.java
TeacherProfileController.java
UserController.java
VideoUploadController.java
WalletController.java
WithdrawalController.java
AdminWithdrawalController.java
```

---

### entity/ — JPA Entities

```
BaseEntity.java                  — Abstract base (id, createdAt, updatedAt, audit)
AiReview.java
Answer.java
Assessment.java
AssessmentLesson.java
AssessmentQuestion.java
CanonicalQuestion.java
Chapter.java
ChatMessage.java
ChatSession.java
Course.java
CourseAssessment.java
CourseLesson.java
Curriculum.java
Enrollment.java
ExamMatrix.java
ExamMatrixBankMapping.java
ExamMatrixRow.java
ExamMatrixTemplateMapping.java
Grade.java
GradeAuditLog.java
GradeSubject.java
InvalidatedToken.java
LearningRoadmap.java
Lesson.java
LessonPlan.java
LessonProgress.java
LessonSlideGeneratedFile.java
MatrixUsageStats.java
Mindmap.java
MindmapNode.java
Notification.java
Permission.java
PointDistribution.java
Question.java
QuestionBank.java
QuestionTemplate.java
QuizAttempt.java
RegradeRequest.java
RoadmapEntryQuestionMapping.java
RoadmapFeedback.java
RoadmapTopic.java
Role.java
SchoolGrade.java
SlideTemplate.java
Subject.java
Submission.java
SubscriptionPlan.java
TeacherProfile.java
TeachingResource.java
TopicLearningMaterial.java
Transaction.java
User.java
WithdrawalRequest.java
UserSubscription.java
Wallet.java
```

---

### repository/ — Spring Data JPA Repositories

```
AiReviewRepository.java
AnswerRepository.java
AssessmentLessonRepository.java
AssessmentQuestionRepository.java
AssessmentRepository.java
CanonicalQuestionRepository.java
ChapterRepository.java
ChatMessageRepository.java
ChatSessionRepository.java
CourseAssessmentRepository.java
CourseLessonRepository.java
CourseRepository.java
CurriculumRepository.java
EnrollmentRepository.java
ExamMatrixBankMappingRepository.java
ExamMatrixRepository.java
ExamMatrixRowRepository.java
ExamMatrixTemplateMappingRepository.java
GradeAuditLogRepository.java
GradeRepository.java
GradeSubjectRepository.java
InvalidatedTokenRepository.java
LearningRoadmapRepository.java
LessonPlanRepository.java
LessonProgressRepository.java
LessonRepository.java
LessonSlideGeneratedFileRepository.java
MindmapNodeRepository.java
MindmapRepository.java
NotificationRepository.java
PermissionRepository.java
PointDistributionRepository.java
QuestionBankRepository.java
QuestionRepository.java
QuestionTemplateRepository.java
QuizAttemptRepository.java
RegradeRequestRepository.java
RoadmapEntryQuestionMappingRepository.java
RoadmapFeedbackRepository.java
RoadmapTopicRepository.java
RoleRepository.java
SchoolGradeRepository.java
SlideTemplateRepository.java
SubjectRepository.java
SubmissionRepository.java
SubscriptionPlanRepository.java
TeacherProfileRepository.java
TeachingResourceRepository.java
TopicLearningMaterialRepository.java
TransactionRepository.java
UserRepository.java
WithdrawalRequestRepository.java
UserSpecification.java             — JPA Specification for dynamic queries
UserSubscriptionRepository.java
WalletRepository.java
```

---

### service/ — Business Logic (interfaces)

```
AdminDashboardService.java
AdminUserService.java
AIEnhancementService.java
ApplicationInitLogic.java
AssessmentAutoSubmitService.java
AssessmentDraftService.java
AssessmentService.java
AuthenticationService.java
CanonicalQuestionService.java
CentrifugoService.java
ChapterService.java
ChatSessionService.java
CourseLessonService.java
CourseService.java
CurriculumService.java
EmailService.java
EnrollmentService.java
ExamMatrixService.java
ExcelImportService.java
GeminiService.java
GradingService.java
LatexRenderService.java
LearningRoadmapService.java
LessonContentService.java
LessonPlanService.java
LessonService.java
LessonSlideService.java
MindmapService.java
NotificationService.java
OcrService.java
PaymentService.java
PermissionService.java
ProgressService.java
QuestionBankService.java
QuestionSelectionService.java
QuestionService.java
QuestionTemplateService.java
ResourceService.java
RoadmapAdminService.java
RoadmapFeedbackService.java
RoadmapTopicResourceService.java
RoleService.java
SchoolGradeService.java
StudentAssessmentService.java
SubjectService.java
SubscriptionPlanService.java
TeacherProfileService.java
TemplateImportService.java
TemplateValidationService.java
UploadService.java
UserService.java
UserSubscriptionService.java
VideoUploadService.java
WalletService.java
WithdrawalService.java
```

### service/impl/ — Service Implementations

```
AdminDashboardServiceImpl.java
AdminUserServiceImpl.java
AIEnhancementServiceImpl.java
ApplicationInitLogicImpl.java
AssessmentAutoSubmitServiceImpl.java
AssessmentDraftServiceImpl.java
AssessmentServiceImpl.java
AuthenticationServiceImpl.java
CanonicalQuestionServiceImpl.java
CentrifugoServiceImpl.java
ChapterServiceImpl.java
ChatSessionServiceImpl.java
CourseLessonServiceImpl.java
CourseServiceImpl.java
CurriculumServiceImpl.java
EmailServiceImpl.java
EnrollmentServiceImpl.java
ExamMatrixPdfExportService.java      — Standalone (không có interface riêng)
ExamMatrixServiceImpl.java
ExcelImportServiceImpl.java
GeminiOcrServiceImpl.java            — OCR via Gemini AI
GeminiServiceImpl.java
GradingServiceImpl.java
LatexRenderServiceImpl.java
LearningRoadmapServiceImpl.java
LessonContentServiceImpl.java
LessonPlanServiceImpl.java
LessonServiceImpl.java
LessonSlideServiceImpl.java
MindmapServiceImpl.java
MinioUploadServiceImpl.java          — Implements UploadService
NotificationServiceImpl.java
PermissionServiceImpl.java
ProgressServiceImpl.java
QuestionBankServiceImpl.java
QuestionSelectionServiceImpl.java
QuestionServiceImpl.java
QuestionTemplateServiceImpl.java
ResourceServiceImpl.java
RoadmapAdminServiceImpl.java
RoadmapFeedbackServiceImpl.java
RoadmapTopicResourceServiceImpl.java
RoleServiceImpl.java
SchoolGradeServiceImpl.java
StudentAssessmentServiceImpl.java
SubjectServiceImpl.java
SubscriptionPlanServiceImpl.java
TeacherProfileServiceImpl.java
TemplateImportServiceImpl.java
TemplateValidationServiceImpl.java
UserServiceImpl.java
UserSubscriptionServiceImpl.java
VideoUploadServiceImpl.java
WithdrawalServiceImpl.java
```

### service/async/ — Async Job Processing (Redis Stream)

```
OcrJobConsumer.java
OcrJobProcessor.java
OcrJobProducer.java
OcrJobStatusService.java
```

---

### dto/ — Data Transfer Objects

#### dto/request/ — Inbound DTOs

```
AddAssessmentToCourseRequest.java
AddBankMappingRequest.java
AddQuestionToAssessmentRequest.java
AddTemplateMappingRequest.java
AdminSendEmailRequest.java
AIEnhancementRequest.java
AIGenerateTemplatesRequest.java
AnswerUpdateRequest.java
AssessmentRequest.java
AttachResourcesToRoadmapTopicRequest.java
AuthenticationRequest.java
BatchAddTemplateMappingsRequest.java
BatchUpsertMatrixRowCellsRequest.java
BuildExamMatrixRequest.java
BuildSimpleExamMatrixRequest.java
BulkApproveQuestionsRequest.java
BulkAssignQuestionsToBankRequest.java
CanonicalQuestionRequest.java
ChangePasswordRequest.java
CloneAssessmentRequest.java
CompleteGradingRequest.java
CompleteUploadRequest.java
CreateAdminRoadmapRequest.java
CreateChapterRequest.java
CreateChatSessionRequest.java
CreateCourseLessonRequest.java
CreateCourseRequest.java
CreateCurriculumRequest.java
CreateLessonPlanRequest.java
CreateLessonRequest.java
CreateQuestionRequest.java
CreateRoadmapEntryTestRequest.java
CreateRoadmapFeedbackRequest.java
CreateRoadmapTopicRequest.java
CreateSchoolGradeRequest.java
CreateSubjectRequest.java
CreateSubscriptionPlanRequest.java
DepositRequest.java
ExamMatrixRequest.java
FinalizePreviewRequest.java
FlagUpdateRequest.java
GeminiRequest.java
GenerateAssessmentByPercentageRequest.java
GenerateAssessmentQuestionsRequest.java
GenerateCanonicalQuestionsRequest.java
GenerateLessonContentRequest.java
GenerateMindmapRequest.java
GeneratePreviewRequest.java
GenerateTemplateQuestionsRequest.java
GoogleAuthRequest.java
GradeOverrideRequest.java
ImportQuestionsRequest.java
InitiateUploadRequest.java
IntrospectRequest.java
LatexRenderRequest.java
LessonSlideConfirmContentRequest.java
LessonSlideGenerateContentRequest.java
LessonSlideGeneratePptxFromJsonRequest.java
LessonSlideGeneratePptxRequest.java
LessonSlideJsonItemRequest.java
LinkGradeSubjectRequest.java
LogoutRequest.java
ManualAdjustmentRequest.java
ManualGradeRequest.java
MatrixCellRequest.java
MatrixRowRequest.java
MindmapNodeRequest.java
MindmapRequest.java
NotificationRequest.java
PayOSWebhookRequest.java
PermissionCreationRequest.java
PermissionUpdateRequest.java
PointsOverrideRequest.java
ProfileReviewRequest.java
PublishCourseRequest.java
QuestionBankRequest.java
QuestionTemplateBatchImportRequest.java
QuestionTemplateRequest.java
RefreshRequest.java
RegradeRequestCreationRequest.java
RegradeResponseRequest.java
RoadmapEntryQuestionMappingRequest.java
RoadmapEntryTestAnswerRequest.java
RoadmapEntryTestFlagRequest.java
RoleCreationRequest.java
RoleSelectionRequest.java
RoleUpdateRequest.java
SendChatMessageRequest.java
StartAssessmentRequest.java
SubmitAssessmentRequest.java
SubmitRoadmapEntryTestRequest.java
TeacherProfileRequest.java
TemplateImportRequest.java
TestTemplateRequest.java
UpdateAdminRoadmapRequest.java
UpdateChapterRequest.java
UpdateChatSessionRequest.java
UpdateCourseAssessmentRequest.java
UpdateCourseLessonRequest.java
UpdateCourseRequest.java
UpdateCurriculumRequest.java
UpdateLessonPlanRequest.java
UpdateLessonRequest.java
UpdateMatrixRowCellsRequest.java
UpdateQuestionRequest.java
UpdateQuestionTemplateRequest.java
UpdateRoadmapTopicRequest.java
UpdateSchoolGradeRequest.java
UpdateSubscriptionPlanRequest.java
UpdateTopicProgressRequest.java
UserCreationRequest.java
UserRegistrationRequest.java
UserSearchRequest.java
UserUpdateRequest.java
VerifyWithdrawalOtpRequest.java
WithdrawalRequestDto.java
RejectWithdrawalRequest.java
```

#### dto/response/ — Outbound DTOs

```
AdminDashboardStatsResponse.java
AdminQuickStatsResponse.java
AdminRevenueByMonthResponse.java
AdminSystemStatusResponse.java
AdminTransactionPageResponse.java
AdminTransactionResponse.java
AdminTransactionStatsResponse.java
AdminUserListResponse.java
AIEnhancedQuestionResponse.java
AIGeneratedTemplatesResponse.java
AnswerAckResponse.java
AnswerGradeResponse.java
ApiResponse.java                     — Generic wrapper: { code, message, result }
AssessmentGenerationResponse.java
AssessmentQuestionResponse.java
AssessmentResponse.java
AssessmentSummary.java
AttemptQuestionResponse.java
AttemptStartResponse.java
AuthenticationResponse.java
BankMappingResponse.java
BatchTemplateMappingsResponse.java
CanonicalQuestionResponse.java
ChapterResponse.java
ChatExchangeResponse.java
ChatMemoryInfoResponse.java
ChatMessageResponse.java
ChatSessionResponse.java
CognitiveLevelDistributionResponse.java
CourseAssessmentResponse.java
CourseLessonResponse.java
CourseResponse.java
CurriculumResponse.java
DraftSnapshotResponse.java
EnrollmentResponse.java
ExamMatrixResponse.java
ExamMatrixTableResponse.java
ExcelPreviewResponse.java
FinalizePreviewResponse.java
GeminiResponse.java
GeneratedQuestionSample.java
GeneratedQuestionsBatchResponse.java
GenerateLessonContentResponse.java
GradingAnalyticsResponse.java
GradingSubmissionResponse.java
ImportQuestionsResponse.java
InitiateUploadResponse.java
IntrospectResponse.java
LatexRenderResponse.java
LessonPlanResponse.java
LessonProgressItem.java
LessonResponse.java
LessonSlideGeneratedContentResponse.java
LessonSlideGeneratedFileResponse.java
LessonSlideJsonItemResponse.java
MatchingTemplatesResponse.java
MatrixCellResponse.java
MatrixChapterGroupResponse.java
MatrixRowResponse.java
MatrixValidationReport.java
MindmapDetailResponse.java
MindmapNodeResponse.java
MindmapResponse.java
MySubscriptionResponse.java
NotificationResponse.java
OcrComparisonResult.java
OcrJobResponse.java
PartUploadUrlResponse.java
PaymentLinkResponse.java
PayOSCreatePaymentResponse.java
PercentageBasedGenerationResponse.java
PermissionResponse.java
PreviewCandidatesResponse.java
QuestionBankResponse.java
QuestionBankTreeResponse.java
QuestionResponse.java
QuestionTemplateResponse.java
RegradeRequestResponse.java
RoadmapDetailResponse.java
RoadmapEntryTestActiveAttemptResponse.java
RoadmapEntryTestInfoResponse.java
RoadmapEntryTestProgressResponse.java
RoadmapEntryTestResultResponse.java
RoadmapEntryTestSnapshotResponse.java
RoadmapFeedbackResponse.java
RoadmapResourceOptionResponse.java
RoadmapStatsResponse.java
RoadmapSummaryResponse.java
RoadmapTopicCourseResponse.java
RoadmapTopicResponse.java
RoadmapUnlockedTopicResponse.java
RoleResponse.java
SchoolGradeResponse.java
SlideTemplateResponse.java
StudentAssessmentResponse.java
StudentInCourseResponse.java
StudentProgressResponse.java
SubjectResponse.java
SubscriptionOverallStatsResponse.java
SubscriptionPlanResponse.java
SuggestedQuestionsResponse.java
TeacherProfileResponse.java
TeachingResourceResponse.java
TemplateBatchImportResponse.java
TemplateImportResponse.java
TemplateMappingResponse.java
TemplateTestResponse.java
TemplateValidationResponse.java
TopicMaterialResponse.java
TransactionResponse.java
UserRegisterResponse.java
UserResponse.java
UserSubscriptionResponse.java
WalletResponse.java
WithdrawalRequestResponse.java
AdminWithdrawalRequestResponse.java
```

#### dto/ocr/ — OCR Job DTOs

```
OcrJob.java
OcrJobResult.java
```

---

### enums/ — Enum Definitions

```
AiReviewType.java
AssessmentMode.java
AssessmentSelectionStrategy.java
AssessmentStatus.java
AssessmentType.java
AttemptScoringPolicy.java
BillingCycle.java
ChapterProgressStatus.java
ChatMessageRole.java
ChatSessionStatus.java
CognitiveLevel.java
CurriculumCategory.java
DistributionType.java
EnrollmentStatus.java
Gender.java
LessonDifficulty.java
LessonSlideOutputFormat.java
LessonStatus.java
MatrixStatus.java
MindmapStatus.java
OcrJobStatus.java
ProfileStatus.java
QuestionDifficulty.java
QuestionGenerationMode.java
QuestionSourceType.java
QuestionStatus.java
QuestionType.java
RegradeRequestStatus.java
RoadmapGenerationType.java
RoadmapStatus.java
Status.java
SubmissionStatus.java
SubscriptionPlanStatus.java
TeachingResourceType.java
TemplateStatus.java
TemplateVariant.java
TopicStatus.java
TransactionStatus.java
TransactionType.java
WithdrawalStatus.java
UserSubscriptionStatus.java
```

---

### configuration/ — Spring Configs

```
ApplicationInitConfig.java
CentrifugoConfig.java
CustomJwtDecoder.java
GeminiConfig.java
JwtAuthenticationEntryPoint.java
MindmapPerformanceIndexConfig.java
MinioConfig.java
PayOSConfig.java
RedisConfig.java
RedisStreamConfig.java
SecurityConfig.java
SwaggerConfig.java
WebMvcConfig.java
```

#### configuration/properties/ — @ConfigurationProperties

```
CentrifugoProperties.java
GeminiProperties.java
InitProperties.java
MinioProperties.java
OcrAsyncProperties.java
PayOSProperties.java
```

---

### component/ — Spring Components (Redis Stream)

```
PendingTransactionCancelScheduler.java   — Auto-cancel PENDING txs after 15 min (runs every 60s)
WithdrawalOtpCancelScheduler.java        — Auto-cancel PENDING_VERIFY withdrawal requests when OTP expires (runs every 5 min)
StreamConsumerListener.java
StreamInitializer.java
StreamPublisher.java
StreamReclaimJob.java
```

---

### constant/ — Constants

```
PredefinedPermission.java
PredefinedRole.java
RoadmapConstant.java
```

---

### exception/ — Error Handling

```
AppException.java
ErrorCode.java                      — Centralized error code enum
GlobalExceptionHandler.java         — @ControllerAdvice
LatexCompileException.java
LatexRenderProxyException.java
LatexRenderTimeoutException.java
```

---

### util/ — Utilities

```
CSVParser.java
SecurityUtils.java
UuidV7Generator.java
```

---

## Resources — `src/main/resources/`

```
application.yaml              — Default config (dev)
application-prod.yaml         — Production config overrides
logback-spring.xml            — Logging config
db/migration/
    V1__Add_OCR_Fields_To_Teacher_Profile.sql
    V3__Add_Token_Fields_To_Subscription.sql
templates/email/
    email-confirmation.html
    teacher-approved.html
    teacher-rejected.html
```

---

## Test — `src/test/java/com/fptu/math_master/`

```
MathMasterApplicationTests.java
service/
    ExcelImportServiceTest.java
    impl/
        ExamMatrixPdfExportServiceTest.java
        ExamMatrixServiceBuildFullMatrixTest.java
```

### Test Resources — `src/test/resources/`

```
fixtures/exam-matrix/
    expected_full_thpt_matrix.json
    expected_minimal_matrix.json
```

---

## Scripts — `scripts/`

### PowerShell (`ps_scripts/`)

```
apply-format.ps1
deploy-on-local.ps1
install.ps1
manager.ps1
sort-annotations.ps1
test-chat-api.ps1
```

### Ubuntu (`ubuntu_scripts/`)

```
fr                        — Quick launcher
fr-manager.sh             — Main management script
install.sh
config/config.sh
lib/
    builder.sh, cleanup.sh, colors.sh, database-migrations.sh,
    environment-setup.sh, formatter.sh, git_utils.sh,
    health-checks.sh, logger.sh, status_checker.sh, ui.sh
```

### SQL Seeds (`sql/`)

```
seed_grade1_math_curriculum.sql … seed_grade12_math_curriculum.sql
seed_grade1_math_curriculum_part2.sql
seed_assessment_mindmap_slide_lessonplan_neon.sql
V1__create_subscription_tables.sql
V2__seed_default_subscription_plans.sql
V3__create_lesson_slide_generated_files.sql
```

---

## Domain Modules (Nhóm theo nghiệp vụ)

| Module            | Controller                                                     | Entity chính                                            | Service chính                                               |
| ----------------- | -------------------------------------------------------------- | ------------------------------------------------------- | ----------------------------------------------------------- |
| Auth              | AuthenticationController                                       | User, InvalidatedToken, Role, Permission                | AuthenticationService, UserService                          |
| User Management   | UserController, AdminUserController                            | User, TeacherProfile                                    | UserService, AdminUserService, TeacherProfileService        |
| Curriculum        | CurriculumController, SubjectController, SchoolGradeController | Curriculum, Subject, SchoolGrade, Grade, GradeSubject   | CurriculumService, SubjectService, SchoolGradeService       |
| Course            | CourseController, ChapterController, CourseLessonController    | Course, Chapter, CourseLesson, Lesson, Enrollment       | CourseService, ChapterService, CourseLessonService          |
| Lesson Content    | LessonContentController, LessonPlanController                  | Lesson, LessonPlan                                      | LessonContentService, LessonPlanService, LessonService      |
| Lesson Slide      | LessonSlideController, LessonSlidePublicController             | LessonSlideGeneratedFile, SlideTemplate                 | LessonSlideService                                          |
| Mindmap           | MindmapController                                              | Mindmap, MindmapNode                                    | MindmapService                                              |
| Assessment        | AssessmentController, StudentAssessmentController              | Assessment, AssessmentQuestion, Submission, Answer      | AssessmentService, StudentAssessmentService, GradingService |
| Question Bank     | QuestionBankController, QuestionController, QuestionTemplateC  | QuestionBank, Question, QuestionTemplate, CanonicalQ    | QuestionBankService, QuestionService, QuestionTemplateS     |
| Exam Matrix       | ExamMatrixController                                           | ExamMatrix, ExamMatrixRow, ExamMatrixBankMapping        | ExamMatrixService                                           |
| Learning Roadmap  | StudentRoadmapController, AdminRoadmapController               | LearningRoadmap, RoadmapTopic, TopicLearningMaterial    | LearningRoadmapService, RoadmapAdminService                 |
| Chat AI           | ChatSessionController, GeminiController                        | ChatSession, ChatMessage                                | ChatSessionService, GeminiService                           |
| Notification      | NotificationController                                         | Notification                                            | NotificationService, CentrifugoService                      |
| Payment           | PaymentController, WalletController, SubscriptionController    | Transaction, Wallet, SubscriptionPlan, UserSubscription | PaymentService, WalletService, SubscriptionPlanService      |
| Progress          | ProgressController                                             | LessonProgress                                          | ProgressService                                             |
| Resource / Upload | ResourceController, VideoUploadController                      | TeachingResource                                        | ResourceService, UploadService, VideoUploadService          |
| OCR (Async)       | — (via Redis Stream)                                           | — (Redis-based OcrJob)                                  | OcrService, OcrJobProducer/Consumer/Processor               |
| LaTeX             | LatexRenderController                                          | —                                                       | LatexRenderService                                          |
| Admin Dashboard   | AdminDashboardController, AdminSubscriptionController          | —                                                       | AdminDashboardService                                       |

---

## Quy ước đặt tên

- **Controller:** `<Feature>Controller.java`
- **Service interface:** `<Feature>Service.java`
- **Service impl:** `<Feature>ServiceImpl.java` (trong `service/impl/`)
- **Entity:** PascalCase, singular (`User.java`, `Course.java`)
- **Repository:** `<Entity>Repository.java`
- **Request DTO:** `<Action><Feature>Request.java` (trong `dto/request/`)
- **Response DTO:** `<Feature>Response.java` (trong `dto/response/`)
- **Enum:** PascalCase (trong `enums/`)
- **Config:** `<Feature>Config.java` (trong `configuration/`)
- **Properties:** `<Feature>Properties.java` (trong `configuration/properties/`)

---

## Package Path

```
com.fptu.math_master
├── component/
├── configuration/
│   └── properties/
├── constant/
├── controller/
├── dto/
│   ├── ocr/
│   ├── request/
│   └── response/
├── entity/
├── enums/
├── exception/
├── repository/
├── service/
│   ├── async/
│   └── impl/
└── util/
```
