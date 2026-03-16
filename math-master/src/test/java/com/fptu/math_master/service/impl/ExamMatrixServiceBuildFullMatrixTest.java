package com.fptu.math_master.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fptu.math_master.dto.request.BuildExamMatrixRequest;
import com.fptu.math_master.dto.request.MatrixCellRequest;
import com.fptu.math_master.dto.request.MatrixRowRequest;
import com.fptu.math_master.dto.response.ExamMatrixTableResponse;
import com.fptu.math_master.dto.response.MatrixRowResponse;
import com.fptu.math_master.entity.Chapter;
import com.fptu.math_master.entity.Curriculum;
import com.fptu.math_master.entity.ExamMatrix;
import com.fptu.math_master.entity.ExamMatrixRow;
import com.fptu.math_master.entity.ExamMatrixTemplateMapping;
import com.fptu.math_master.entity.QuestionTemplate;
import com.fptu.math_master.entity.Subject;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.MatrixStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.AssessmentQuestionRepository;
import com.fptu.math_master.repository.AssessmentRepository;
import com.fptu.math_master.repository.ChapterRepository;
import com.fptu.math_master.repository.CurriculumRepository;
import com.fptu.math_master.repository.ExamMatrixRepository;
import com.fptu.math_master.repository.ExamMatrixRowRepository;
import com.fptu.math_master.repository.ExamMatrixTemplateMappingRepository;
import com.fptu.math_master.repository.QuestionRepository;
import com.fptu.math_master.repository.QuestionTemplateRepository;
import com.fptu.math_master.repository.SubjectRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.AIEnhancementService;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Unit tests for {@link ExamMatrixServiceImpl#buildMatrix(BuildExamMatrixRequest)}.
 *
 * <p>Verified cognitive-level distribution (full THPT 50-question):
 * <pre>
 * Chapter                          | NB | TH | VD | VDC | Total
 * -----------------------------------------------------------------
 * 1. Đạo Hàm và Ứng Dụng          |  4 |  4 |  1 |   1 |   10
 * 2. Hàm Số Mũ - Logarit           |  2 |  2 |  2 |   2 |    8
 * 3. Số Phức                        |  3 |  1 |  1 |   1 |    6
 * 4. Nguyên Hàm – Tích Phân        |  3 |  2 |  2 |   1 |    8
 * 5. Khối Đa Diện                   |  1 |  1 |  1 |   0 |    3
 * 6. Khối Tròn Xoay                 |  2 |  0 |  0 |   0 |    2
 * 7. Giải Tích Trong Không Gian     |  3 |  3 |  2 |   0 |    8
 * 8. Tổ Hợp – Xác Suất             |  2 |  1 |  0 |   0 |    3
 * 9. Hình Học Không Gian            |  0 |  1 |  1 |   0 |    2
 * -----------------------------------------------------------------
 * GRAND TOTAL                       | 20 | 15 | 10 |   5 |   50
 * </pre>
 * Points: 50 × 0.20 = 10.00 điểm
 */
@ExtendWith(MockitoExtension.class)
class ExamMatrixServiceBuildFullMatrixTest {

  // ── JSON Mapper ─────────────────────────────────────────────────────────

  private static ObjectMapper MAPPER;

  @BeforeAll
  static void initMapper() {
    MAPPER =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  // ── Mocks ───────────────────────────────────────────────────────────────

  @Mock private ExamMatrixRepository examMatrixRepository;
  @Mock private ExamMatrixTemplateMappingRepository templateMappingRepository;
  @Mock private ExamMatrixRowRepository examMatrixRowRepository;
  @Mock private QuestionTemplateRepository questionTemplateRepository;
  @Mock private QuestionRepository questionRepository;
  @Mock private AssessmentRepository assessmentRepository;
  @Mock private AssessmentQuestionRepository assessmentQuestionRepository;
  @Mock private UserRepository userRepository;
  @Mock private CurriculumRepository curriculumRepository;
  @Mock private ChapterRepository chapterRepository;
  @Mock private SubjectRepository subjectRepository;
  @Mock private AIEnhancementService aiEnhancementService;

  @InjectMocks private ExamMatrixServiceImpl examMatrixService;

  // ── Constants ───────────────────────────────────────────────────────────

  private static final UUID TEACHER_ID = UUID.randomUUID();
  private static final UUID CURRICULUM_ID = UUID.randomUUID();
  private static final UUID SUBJECT_ID = UUID.randomUUID();
  private static final UUID TEMPLATE_ID = UUID.randomUUID();

  private static final UUID CH_DAO_HAM = UUID.randomUUID();
  private static final UUID CH_HAM_MU_LOG = UUID.randomUUID();
  private static final UUID CH_SO_PHUC = UUID.randomUUID();
  private static final UUID CH_NGUYEN_HAM_TP = UUID.randomUUID();
  private static final UUID CH_KHOI_DA_DIEN = UUID.randomUUID();
  private static final UUID CH_KHOI_TRON_XOAY = UUID.randomUUID();
  private static final UUID CH_GIAI_TICH_KG = UUID.randomUUID();
  private static final UUID CH_TO_HOP_XS = UUID.randomUUID();
  private static final UUID CH_HINH_HOC_KG = UUID.randomUUID();

  private static final BigDecimal PTS = new BigDecimal("0.20");
  private static final String TEACHER_NAME = "Nguyễn Văn A";
  private static final String CURRICULUM_NAME = "Toán 12 – Đề Minh Họa THPT";
  private static final String SUBJECT_NAME = "Toán";

  // ── Captured saves ──────────────────────────────────────────────────────

  private List<ExamMatrixRow> savedRows;
  private List<ExamMatrixTemplateMapping> savedCells;
  private UUID matrixId;

  // ── Setup / Teardown ────────────────────────────────────────────────────

  @BeforeEach
  void setUp() {
    savedRows = new ArrayList<>();
    savedCells = new ArrayList<>();
    matrixId = UUID.randomUUID();

    // Security Context — teacher JWT
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject(TEACHER_ID.toString())
            .build();
    JwtAuthenticationToken auth =
        new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
    SecurityContext secCtx = mock(SecurityContext.class);
    lenient().when(secCtx.getAuthentication()).thenReturn(auth);
    SecurityContextHolder.setContext(secCtx);

    // Curriculum
    Curriculum curriculum = new Curriculum();
    curriculum.setId(CURRICULUM_ID);
    curriculum.setName(CURRICULUM_NAME);
    curriculum.setGrade(12);
    curriculum.setSubjectId(SUBJECT_ID);
    lenient()
        .when(curriculumRepository.findByIdAndNotDeleted(CURRICULUM_ID))
        .thenReturn(Optional.of(curriculum));

    // Subject
    Subject subject = new Subject();
    subject.setId(SUBJECT_ID);
    subject.setName(SUBJECT_NAME);
    lenient().when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(subject));

    // Teacher
    User teacher = new User();
    teacher.setId(TEACHER_ID);
    teacher.setFullName(TEACHER_NAME);
    lenient().when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));
    lenient().when(userRepository.findByIdWithRoles(TEACHER_ID)).thenReturn(Optional.of(teacher));

    // Question template
    QuestionTemplate template = new QuestionTemplate();
    template.setId(TEMPLATE_ID);
    template.setName("Mẫu Trắc Nghiệm");
    lenient()
        .when(questionTemplateRepository.findById(TEMPLATE_ID))
        .thenReturn(Optional.of(template));

    // Chapters
    stubChapter(CH_DAO_HAM, "ĐẠO HÀM VÀ ỨNG DỤNG", 1);
    stubChapter(CH_HAM_MU_LOG, "HÀM SỐ MŨ - LOGARIT", 2);
    stubChapter(CH_SO_PHUC, "SỐ PHỨC", 3);
    stubChapter(CH_NGUYEN_HAM_TP, "NGUYÊN HÀM – TÍCH PHÂN", 4);
    stubChapter(CH_KHOI_DA_DIEN, "KHỐI ĐA DIỆN", 5);
    stubChapter(CH_KHOI_TRON_XOAY, "KHỐI TRÒN XOAY", 6);
    stubChapter(CH_GIAI_TICH_KG, "GIẢI TÍCH TRONG KHÔNG GIAN", 7);
    stubChapter(CH_TO_HOP_XS, "TỔ HỢP – XÁC SUẤT", 8);
    stubChapter(CH_HINH_HOC_KG, "HÌNH HỌC KHÔNG GIAN", 9);

    // ExamMatrix save — assign id + timestamps
    lenient()
        .when(examMatrixRepository.save(any(ExamMatrix.class)))
        .thenAnswer(
            inv -> {
              ExamMatrix m = inv.getArgument(0);
              m.setId(matrixId);
              m.setCreatedAt(Instant.now());
              m.setUpdatedAt(Instant.now());
              return m;
            });

    // Row save — capture
    lenient()
        .when(examMatrixRowRepository.save(any(ExamMatrixRow.class)))
        .thenAnswer(
            inv -> {
              ExamMatrixRow r = inv.getArgument(0);
              r.setId(UUID.randomUUID());
              r.setCreatedAt(Instant.now());
              r.setUpdatedAt(Instant.now());
              savedRows.add(r);
              return r;
            });

    // Cell save — capture
    lenient()
        .when(templateMappingRepository.save(any(ExamMatrixTemplateMapping.class)))
        .thenAnswer(
            inv -> {
              ExamMatrixTemplateMapping c = inv.getArgument(0);
              c.setId(UUID.randomUUID());
              c.setCreatedAt(Instant.now());
              c.setUpdatedAt(Instant.now());
              savedCells.add(c);
              return c;
            });

    // buildTableResponse queries
    lenient()
        .when(examMatrixRowRepository.findByExamMatrixIdOrderByOrderIndex(matrixId))
        .thenAnswer(inv -> new ArrayList<>(savedRows));
    lenient()
        .when(templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
        .thenAnswer(inv -> new ArrayList<>(savedCells));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // JSON comparison helper
  // ═══════════════════════════════════════════════════════════════════════════

  private ExamMatrixTableResponse loadExpected(String resourcePath) throws Exception {
    try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
      assertThat(is).as("Expected JSON resource: %s", resourcePath).isNotNull();
      return MAPPER.readValue(is, ExamMatrixTableResponse.class);
    }
  }

  /**
   * Compares actual response against expected JSON, ignoring all UUID fields,
   * timestamps, and the CognitiveLevel enum (only the label string is compared).
   */
  private void assertMatchesExpected(
      ExamMatrixTableResponse actual, ExamMatrixTableResponse expected) {
    assertThat(actual)
        .usingRecursiveComparison()
        .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
        .ignoringFieldsMatchingRegexes(
            ".*[Ii]d$", // id, teacherId, curriculumId, subjectId, rowId, ...
            ".*At$", // createdAt, updatedAt
            ".*cognitiveLevel$" // CognitiveLevel enum — compare label instead
            )
        .isEqualTo(expected);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ABNORMAL TEST CASES
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Abnormal: user has NO teacher/admin role.
   * Expects: {@link ErrorCode#NOT_A_TEACHER}
   */
  @Test
  @DisplayName("buildMatrix — NOT_A_TEACHER when user has no teacher/admin role")
  void should_throw_NOT_A_TEACHER() {
    // Replace security context with a student role
    Jwt studentJwt =
        Jwt.withTokenValue("student-token")
            .header("alg", "RS256")
            .subject(TEACHER_ID.toString())
            .build();
    JwtAuthenticationToken studentAuth =
        new JwtAuthenticationToken(
            studentJwt, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    SecurityContext studentCtx = mock(SecurityContext.class);
    when(studentCtx.getAuthentication()).thenReturn(studentAuth);
    SecurityContextHolder.setContext(studentCtx);

    User student = new User();
    student.setId(TEACHER_ID);
    student.setRoles(java.util.Set.of());
    when(userRepository.findByIdWithRoles(TEACHER_ID)).thenReturn(Optional.of(student));

    BuildExamMatrixRequest request = minimalRequest();

    assertThatThrownBy(() -> examMatrixService.buildMatrix(request))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).getErrorCode())
        .isEqualTo(ErrorCode.NOT_A_TEACHER);

    verify(examMatrixRepository, never()).save(any());
    verify(examMatrixRowRepository, never()).save(any());
    verify(templateMappingRepository, never()).save(any());
  }

  /**
   * Abnormal: row has no templateId AND no questionTypeName.
   * Expects: {@link ErrorCode#MATRIX_ROW_QUESTION_TYPE_REQUIRED}
   */
  @Test
  @DisplayName("buildMatrix — MATRIX_ROW_QUESTION_TYPE_REQUIRED when row is missing both")
  void should_throw_MATRIX_ROW_QUESTION_TYPE_REQUIRED() {
    MatrixRowRequest badRow =
        MatrixRowRequest.builder()
            .chapterId(CH_DAO_HAM)
            .templateId(null)
            .questionTypeName(null)
            .referenceQuestions("1")
            .orderIndex(1)
            .cells(List.of(cell(CognitiveLevel.NHAN_BIET, 1)))
            .build();

    BuildExamMatrixRequest request =
        BuildExamMatrixRequest.builder()
            .name("Test")
            .curriculumId(CURRICULUM_ID)
            .gradeLevel(12)
            .totalQuestionsTarget(1)
            .totalPointsTarget(PTS)
            .rows(List.of(badRow))
            .build();

    assertThatThrownBy(() -> examMatrixService.buildMatrix(request))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).getErrorCode())
        .isEqualTo(ErrorCode.MATRIX_ROW_QUESTION_TYPE_REQUIRED);

    verify(examMatrixRepository).save(any(ExamMatrix.class));
    verify(examMatrixRowRepository, never()).save(any());
    verify(templateMappingRepository, never()).save(any());
  }

  /**
   * Abnormal: row references a non-existent template.
   * Expects: {@link ErrorCode#QUESTION_TEMPLATE_NOT_FOUND}
   */
  @Test
  @DisplayName("buildMatrix — QUESTION_TEMPLATE_NOT_FOUND for missing template")
  void should_throw_QUESTION_TEMPLATE_NOT_FOUND() {
    UUID fakeTemplateId = UUID.randomUUID();
    when(questionTemplateRepository.findById(fakeTemplateId)).thenReturn(Optional.empty());

    MatrixRowRequest rowWithBadTemplate =
        MatrixRowRequest.builder()
            .chapterId(CH_DAO_HAM)
            .templateId(fakeTemplateId)
            .questionTypeName("Dạng bài test")
            .referenceQuestions("1")
            .orderIndex(1)
            .cells(List.of(cell(CognitiveLevel.NHAN_BIET, 1)))
            .build();

    BuildExamMatrixRequest request =
        BuildExamMatrixRequest.builder()
            .name("Test")
            .curriculumId(CURRICULUM_ID)
            .gradeLevel(12)
            .totalQuestionsTarget(1)
            .totalPointsTarget(PTS)
            .rows(List.of(rowWithBadTemplate))
            .build();

    assertThatThrownBy(() -> examMatrixService.buildMatrix(request))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).getErrorCode())
        .isEqualTo(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND);

    verify(examMatrixRepository).save(any(ExamMatrix.class));
    verify(examMatrixRowRepository).save(any(ExamMatrixRow.class));
    verify(templateMappingRepository, never()).save(any());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // SUCCESS TEST CASES
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Success: minimal matrix — 1 chapter, 1 row, 1 cell (NB×3, 2đ/câu).
   * Compares against {@code expected_minimal_matrix.json}.
   */
  @Test
  @DisplayName("buildMatrix — minimal matrix matches expected_minimal_matrix.json")
  void should_build_minimal_matrix() throws Exception {
    ExamMatrixTableResponse expected =
        loadExpected("/fixtures/exam-matrix/expected_minimal_matrix.json");

    BuildExamMatrixRequest request =
        BuildExamMatrixRequest.builder()
            .name("Đề kiểm tra đơn giản")
            .description("Ma trận tối thiểu – 1 chương, 1 dòng, 1 ô")
            .curriculumId(CURRICULUM_ID)
            .gradeLevel(10)
            .isReusable(false)
            .totalQuestionsTarget(5)
            .totalPointsTarget(new BigDecimal("10.00"))
            .rows(
                List.of(
                    MatrixRowRequest.builder()
                        .chapterId(CH_DAO_HAM)
                        .templateId(TEMPLATE_ID)
                        .questionTypeName("Trắc Nghiệm")
                        .orderIndex(1)
                        .cells(
                            List.of(
                                MatrixCellRequest.builder()
                                    .cognitiveLevel(CognitiveLevel.NHAN_BIET)
                                    .questionCount(3)
                                    .pointsPerQuestion(new BigDecimal("2.00"))
                                    .build()))
                        .build()))
            .build();

    ExamMatrixTableResponse actual = examMatrixService.buildMatrix(request);

    assertMatchesExpected(actual, expected);

    verify(examMatrixRepository).save(any(ExamMatrix.class));
    verify(examMatrixRowRepository).save(any(ExamMatrixRow.class));
    verify(templateMappingRepository).save(any(ExamMatrixTemplateMapping.class));
    verify(questionTemplateRepository).findById(TEMPLATE_ID);
    verify(examMatrixRowRepository).findByExamMatrixIdOrderByOrderIndex(matrixId);
    verify(templateMappingRepository).findByExamMatrixIdOrderByCreatedAt(matrixId);
    verify(chapterRepository).findById(CH_DAO_HAM);
    verify(userRepository).findById(TEACHER_ID);
    verify(curriculumRepository).findByIdAndNotDeleted(CURRICULUM_ID);
    verify(subjectRepository).findById(SUBJECT_ID);
  }

  /**
   * Success: full THPT 50-question matrix — 9 chapters, 26 rows, 47 cells.
   * Compares against {@code expected_full_thpt_matrix.json}.
   */
  @Test
  @DisplayName("buildMatrix — full THPT 50-question matrix matches expected_full_thpt_matrix.json")
  void should_build_full_thpt_matrix() throws Exception {
    ExamMatrixTableResponse expected =
        loadExpected("/fixtures/exam-matrix/expected_full_thpt_matrix.json");

    ExamMatrixTableResponse actual =
        examMatrixService.buildMatrix(buildFullTHPTMatrixRequest());

    assertMatchesExpected(actual, expected);

    verify(examMatrixRepository).save(any(ExamMatrix.class));
    verify(examMatrixRowRepository, times(26)).save(any(ExamMatrixRow.class));
    verify(templateMappingRepository, times(47)).save(any(ExamMatrixTemplateMapping.class));
    verify(questionTemplateRepository, times(26)).findById(TEMPLATE_ID);
    verify(examMatrixRowRepository).findByExamMatrixIdOrderByOrderIndex(matrixId);
    verify(templateMappingRepository).findByExamMatrixIdOrderByCreatedAt(matrixId);
    verify(chapterRepository, times(9)).findById(any());
    verify(userRepository).findById(TEACHER_ID);
    verify(curriculumRepository).findByIdAndNotDeleted(CURRICULUM_ID);
    verify(subjectRepository).findById(SUBJECT_ID);
  }

  /**
   * Success: gradeLevel is null → auto-resolved from curriculum (grade=12).
   */
  @Test
  @DisplayName("buildMatrix — auto-resolves gradeLevel from curriculum when not provided")
  void should_auto_resolve_grade_level_from_curriculum() {
    BuildExamMatrixRequest request =
        BuildExamMatrixRequest.builder()
            .name("Ma Trận Tự Lấy Lớp")
            .curriculumId(CURRICULUM_ID)
            .gradeLevel(null)
            .totalQuestionsTarget(1)
            .totalPointsTarget(PTS)
            .rows(
                List.of(
                    row(
                        CH_DAO_HAM,
                        "Đơn điệu của HS",
                        "1",
                        new AtomicInteger(1),
                        cell(CognitiveLevel.NHAN_BIET, 1))))
            .build();

    ExamMatrixTableResponse actual = examMatrixService.buildMatrix(request);

    assertThat(actual.getGradeLevel()).isEqualTo(12);
    assertThat(actual.getStatus()).isEqualTo(MatrixStatus.DRAFT);

    // gradeLevel resolution calls curriculum repo, + 1 more in buildTableResponse
    verify(curriculumRepository, times(2)).findByIdAndNotDeleted(CURRICULUM_ID);
    verify(examMatrixRepository).save(any(ExamMatrix.class));
    verify(examMatrixRowRepository).save(any(ExamMatrixRow.class));
    verify(templateMappingRepository).save(any(ExamMatrixTemplateMapping.class));
  }

  /**
   * Success: rows without templateId — cells should NOT be persisted.
   */
  @Test
  @DisplayName("buildMatrix — rows without templateId persist no cells")
  void should_not_persist_cells_when_no_template() {
    MatrixRowRequest rowNoTemplate =
        MatrixRowRequest.builder()
            .chapterId(CH_DAO_HAM)
            .templateId(null)
            .questionTypeName("Dạng bài thủ công")
            .referenceQuestions("1,2")
            .orderIndex(1)
            .cells(
                List.of(
                    cell(CognitiveLevel.NHAN_BIET, 2),
                    cell(CognitiveLevel.THONG_HIEU, 1)))
            .build();

    BuildExamMatrixRequest request =
        BuildExamMatrixRequest.builder()
            .name("Ma Trận Không Template")
            .curriculumId(CURRICULUM_ID)
            .gradeLevel(12)
            .totalQuestionsTarget(3)
            .totalPointsTarget(new BigDecimal("0.60"))
            .rows(List.of(rowNoTemplate))
            .build();

    ExamMatrixTableResponse actual = examMatrixService.buildMatrix(request);

    assertThat(actual.getStatus()).isEqualTo(MatrixStatus.DRAFT);
    assertThat(actual.getChapters()).hasSize(1);

    MatrixRowResponse rowResp = actual.getChapters().getFirst().getRows().getFirst();
    assertThat(rowResp.getQuestionTypeName()).isEqualTo("Dạng bài thủ công");
    assertThat(rowResp.getCells()).isEmpty();
    assertThat(rowResp.getRowTotalQuestions()).isZero();

    verify(examMatrixRepository).save(any(ExamMatrix.class));
    verify(examMatrixRowRepository).save(any(ExamMatrixRow.class));
    verify(templateMappingRepository, never()).save(any(ExamMatrixTemplateMapping.class));
    verify(questionTemplateRepository, never()).findById(any());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // Request Builders
  // ═══════════════════════════════════════════════════════════════════════════

  private BuildExamMatrixRequest minimalRequest() {
    return BuildExamMatrixRequest.builder()
        .name("Test")
        .curriculumId(CURRICULUM_ID)
        .gradeLevel(12)
        .totalQuestionsTarget(1)
        .totalPointsTarget(PTS)
        .rows(
            List.of(
                row(
                    CH_DAO_HAM,
                    "Đơn điệu của HS",
                    "1",
                    new AtomicInteger(1),
                    cell(CognitiveLevel.NHAN_BIET, 1))))
        .build();
  }

  /**
   * Full 50-question THPT request: 9 chapters, 26 rows, 47 cells.
   * NB=20, TH=15, VD=10, VDC=5 = 50 total, 0.20đ each = 10.00đ.
   */
  private BuildExamMatrixRequest buildFullTHPTMatrixRequest() {
    AtomicInteger order = new AtomicInteger(1);
    List<MatrixRowRequest> rows = new ArrayList<>();

    // Ch1: ĐẠO HÀM — 5 rows, NB=4 TH=4 VD=1 VDC=1 = 10
    rows.add(row(CH_DAO_HAM, "Đơn điệu của HS", "3,30", order,
        cell(CognitiveLevel.NHAN_BIET, 1), cell(CognitiveLevel.THONG_HIEU, 1)));
    rows.add(row(CH_DAO_HAM, "Cực trị của HS", "4,5,39,46", order,
        cell(CognitiveLevel.NHAN_BIET, 1), cell(CognitiveLevel.THONG_HIEU, 1),
        cell(CognitiveLevel.VAN_DUNG, 1), cell(CognitiveLevel.VAN_DUNG_CAO, 1)));
    rows.add(row(CH_DAO_HAM, "Min, Max của hàm số", "31", order,
        cell(CognitiveLevel.THONG_HIEU, 1)));
    rows.add(row(CH_DAO_HAM, "Đường Tiệm Cận", "6", order,
        cell(CognitiveLevel.NHAN_BIET, 1)));
    rows.add(row(CH_DAO_HAM, "Khảo sát và vẽ đồ thị", "7,8", order,
        cell(CognitiveLevel.NHAN_BIET, 1), cell(CognitiveLevel.THONG_HIEU, 1)));

    // Ch2: HÀM MŨ-LOG — 4 rows, NB=2 TH=2 VD=2 VDC=2 = 8
    rows.add(row(CH_HAM_MU_LOG, "Lũy thừa – Mũ – Logarit", "9,11", order,
        cell(CognitiveLevel.NHAN_BIET, 1), cell(CognitiveLevel.THONG_HIEU, 1)));
    rows.add(row(CH_HAM_MU_LOG, "HS Mũ – Logarit", "10", order,
        cell(CognitiveLevel.THONG_HIEU, 1)));
    rows.add(row(CH_HAM_MU_LOG, "PT Mũ – Logarit", "12,13,47", order,
        cell(CognitiveLevel.NHAN_BIET, 1), cell(CognitiveLevel.VAN_DUNG, 1),
        cell(CognitiveLevel.VAN_DUNG_CAO, 1)));
    rows.add(row(CH_HAM_MU_LOG, "BPT Mũ – Logarit", "32,40", order,
        cell(CognitiveLevel.VAN_DUNG, 1), cell(CognitiveLevel.VAN_DUNG_CAO, 1)));

    // Ch3: SỐ PHỨC — 2 rows, NB=3 TH=1 VD=1 VDC=1 = 6
    rows.add(row(CH_SO_PHUC, "Định nghĩa và tính chất", "18,20,34,42,49", order,
        cell(CognitiveLevel.NHAN_BIET, 2), cell(CognitiveLevel.THONG_HIEU, 1),
        cell(CognitiveLevel.VAN_DUNG, 1), cell(CognitiveLevel.VAN_DUNG_CAO, 1)));
    rows.add(row(CH_SO_PHUC, "Phép Toán", "19", order,
        cell(CognitiveLevel.NHAN_BIET, 1)));

    // Ch4: NGUYÊN HÀM–TP — 3 rows, NB=3 TH=2 VD=2 VDC=1 = 8
    rows.add(row(CH_NGUYEN_HAM_TP, "Nguyên hàm", "14,15,33", order,
        cell(CognitiveLevel.NHAN_BIET, 2), cell(CognitiveLevel.THONG_HIEU, 1)));
    rows.add(row(CH_NGUYEN_HAM_TP, "Tích phân", "16,17,41,48", order,
        cell(CognitiveLevel.NHAN_BIET, 1), cell(CognitiveLevel.THONG_HIEU, 1),
        cell(CognitiveLevel.VAN_DUNG, 1), cell(CognitiveLevel.VAN_DUNG_CAO, 1)));
    rows.add(row(CH_NGUYEN_HAM_TP, "Ứng dụng TP tính diện tích", "44", order,
        cell(CognitiveLevel.VAN_DUNG, 1)));

    // Ch5: KHỐI ĐA DIỆN — 1 row, NB=1 TH=1 VD=1 = 3
    rows.add(row(CH_KHOI_DA_DIEN, "Thể tích khối đa diện", "21,22,43", order,
        cell(CognitiveLevel.NHAN_BIET, 1), cell(CognitiveLevel.THONG_HIEU, 1),
        cell(CognitiveLevel.VAN_DUNG, 1)));

    // Ch6: KHỐI TRÒN XOAY — 2 rows, NB=2 = 2
    rows.add(row(CH_KHOI_TRON_XOAY, "Khối nón", "23", order,
        cell(CognitiveLevel.NHAN_BIET, 1)));
    rows.add(row(CH_KHOI_TRON_XOAY, "Khối trụ", "24", order,
        cell(CognitiveLevel.NHAN_BIET, 1)));

    // Ch7: GIẢI TÍCH KG — 4 rows, NB=3 TH=3 VD=2 = 8
    rows.add(row(CH_GIAI_TICH_KG, "PP tọa độ", "25", order,
        cell(CognitiveLevel.NHAN_BIET, 1)));
    rows.add(row(CH_GIAI_TICH_KG, "PT mặt cầu", "26,37,50", order,
        cell(CognitiveLevel.NHAN_BIET, 1), cell(CognitiveLevel.THONG_HIEU, 1),
        cell(CognitiveLevel.VAN_DUNG, 1)));
    rows.add(row(CH_GIAI_TICH_KG, "PT mặt phẳng", "27", order,
        cell(CognitiveLevel.NHAN_BIET, 1)));
    rows.add(row(CH_GIAI_TICH_KG, "PT đường thẳng", "28,38,45", order,
        cell(CognitiveLevel.THONG_HIEU, 2), cell(CognitiveLevel.VAN_DUNG, 1)));

    // Ch8: TỔ HỢP–XS — 3 rows, NB=2 TH=1 = 3
    rows.add(row(CH_TO_HOP_XS, "Hoán vị - Chỉnh hợp – Tổ hợp", "1", order,
        cell(CognitiveLevel.NHAN_BIET, 1)));
    rows.add(row(CH_TO_HOP_XS, "Cấp số cộng (cấp số nhân)", "2", order,
        cell(CognitiveLevel.NHAN_BIET, 1)));
    rows.add(row(CH_TO_HOP_XS, "Xác suất", "29", order,
        cell(CognitiveLevel.THONG_HIEU, 1)));

    // Ch9: HÌNH HỌC KG — 2 rows, TH=1 VD=1 = 2
    rows.add(row(CH_HINH_HOC_KG, "Góc", "35", order,
        cell(CognitiveLevel.VAN_DUNG, 1)));
    rows.add(row(CH_HINH_HOC_KG, "Khoảng cách", "36", order,
        cell(CognitiveLevel.THONG_HIEU, 1)));

    return BuildExamMatrixRequest.builder()
        .name("Ma Trận Đề Minh Họa THPT 2025")
        .description("Ma trận đề thi thử THPT Quốc Gia môn Toán – 50 câu, 10 điểm")
        .curriculumId(CURRICULUM_ID)
        .gradeLevel(12)
        .isReusable(true)
        .totalQuestionsTarget(50)
        .totalPointsTarget(new BigDecimal("10.00"))
        .rows(rows)
        .build();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // Row / Cell / Stub helpers
  // ═══════════════════════════════════════════════════════════════════════════

  private MatrixRowRequest row(
      UUID chapterId,
      String questionTypeName,
      String referenceQuestions,
      AtomicInteger orderCounter,
      MatrixCellRequest... cells) {
    return MatrixRowRequest.builder()
        .chapterId(chapterId)
        .templateId(TEMPLATE_ID)
        .questionTypeName(questionTypeName)
        .referenceQuestions(referenceQuestions)
        .orderIndex(orderCounter.getAndIncrement())
        .cells(List.of(cells))
        .build();
  }

  private MatrixCellRequest cell(CognitiveLevel level, int count) {
    return MatrixCellRequest.builder()
        .cognitiveLevel(level)
        .questionCount(count)
        .pointsPerQuestion(PTS)
        .build();
  }

  private void stubChapter(UUID chapterId, String title, int orderIndex) {
    Chapter ch = new Chapter();
    ch.setId(chapterId);
    ch.setTitle(title);
    ch.setOrderIndex(orderIndex);
    lenient().when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(ch));
  }
}
