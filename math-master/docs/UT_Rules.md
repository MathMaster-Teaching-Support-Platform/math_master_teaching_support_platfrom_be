# Rules: Viết Unit Test cho MathMaster

> Tài liệu này là bộ quy tắc **bắt buộc** khi viết Unit Test (UT) cho project MathMaster.
> Mọi UT phải tuân thủ các quy tắc dưới đây trước khi merge.

---

## 1. Cấu trúc tổng quát của một Test Class

```java
@DisplayName("CourseServiceImpl - Tests")
class CourseServiceImplTest extends BaseUnitTest {

    // 1. System Under Test (SUT)
    @InjectMocks
    private CourseServiceImpl courseService;

    // 2. Dependencies (mocked)
    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    // 3. Shared test data
    private Course mockCourse;
    private User mockTeacher;

    // 4. Setup shared mock data
    @BeforeEach
    void setUp() {
        mockTeacher = buildMockUser(1L, "teacher@fptu.edu.vn", Role.TEACHER);
        mockCourse  = buildMockCourse(10L, "Toán Cao Cấp", mockTeacher);
    }

    // 5. Test methods (grouped by method under test)
    // ...
}
```

**Quy tắc cấu trúc:**

- Luôn `extend BaseUnitTest` — không được bỏ qua.
- Khai báo `@InjectMocks` cho SUT **trước**, `@Mock` cho dependencies **sau**.
- Đặt shared mock data trong `@BeforeEach setUp()`, không lặp lại trong từng test.
- Nhóm các test theo method đang được test, dùng `@Nested` nếu nhiều test cho cùng method.

---

## 2. Quy tắc đặt tên test

### 2.1 Format tên method

```
it_should_[expected_behavior]_when_[condition]
```

**Ví dụ:**

```java
@Test
void it_should_return_course_when_id_is_valid()

@Test
void it_should_throw_exception_when_course_not_found()

@Test
void it_should_return_empty_list_when_teacher_has_no_course()
```

> ❌ **Không dùng:** `test1()`, `testGetCourse()`, `getCourse_success()`

### 2.2 `@DisplayName` bắt buộc khi dùng `@Nested`

```java
@Nested
@DisplayName("getCourseById()")
class GetCourseByIdTests {

    @Test
    @DisplayName("Normal: trả về course khi ID hợp lệ")
    void it_should_return_course_when_id_is_valid() { ... }

    @Test
    @DisplayName("Abnormal: ném exception khi course không tồn tại")
    void it_should_throw_exception_when_course_not_found() { ... }
}
```

---

## 3. Cấu trúc bên trong một Test Method: AAA Pattern

Mỗi test **bắt buộc** theo đúng 3 giai đoạn, có comment phân cách rõ ràng:

```java
@Test
void it_should_return_course_when_id_is_valid() {
    // ===== ARRANGE =====
    Long courseId = 10L;
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(mockCourse));

    // ===== ACT =====
    CourseResponse result = courseService.getCourseById(courseId);

    // ===== ASSERT =====
    assertNotNull(result);
    assertEquals(mockCourse.getId(), result.getId());
    assertEquals(mockCourse.getTitle(), result.getTitle());

    // ===== VERIFY =====
    verify(courseRepository, times(1)).findById(courseId);
    verifyNoMoreInteractions(courseRepository);
}
```

> **VERIFY** không phải optional — luôn verify các dependency đã được gọi đúng số lần và đúng argument.

---

## 4. Quy tắc Mock Data

### 4.1 Dùng builder method, không inline

```java
// ✅ ĐÚNG — dùng private builder method
private Course buildMockCourse(Long id, String title, User teacher) {
    Course course = new Course();
    course.setId(id);
    course.setTitle(title);
    course.setTeacher(teacher);
    course.setStatus(CourseStatus.PUBLISHED);
    course.setCreatedAt(LocalDateTime.now());
    return course;
}

// ❌ SAI — tạo object inline trong từng test
Course course = new Course();
course.setId(1L);
// ... lặp lại 10 lần trong 10 test
```

### 4.2 Dùng realistic data, không dùng "test", "abc", "123"

```java
// ✅ ĐÚNG
course.setTitle("Giải Tích 1 - Học Kỳ 2");
user.setEmail("nguyen.son.nam@student.fptu.edu.vn");
course.setPrice(new BigDecimal("299000"));

// ❌ SAI
course.setTitle("test course");
user.setEmail("test@test.com");
course.setPrice(new BigDecimal("1"));
```

### 4.3 Quy tắc mock theo loại JOIN

**Inner Join (bắt buộc có data ở cả hai phía):**

```java
// Ví dụ: courseRepository.findCourseWithEnrollments() dùng INNER JOIN
// → Phải mock đủ: course tồn tại VÀ có enrollment

@Test
void it_should_return_courses_that_have_enrollments() {
    // ARRANGE — cover cả hai phía của INNER JOIN
    Course courseWithEnrollment = buildMockCourse(1L, "Toán 1", mockTeacher);
    Enrollment enrollment = buildMockEnrollment(100L, mockStudent, courseWithEnrollment);

    when(courseRepository.findCoursesWithEnrollments())
        .thenReturn(List.of(courseWithEnrollment));

    // ACT
    List<CourseResponse> result = courseService.getCoursesWithEnrollments();

    // ASSERT
    assertEquals(1, result.size());
    assertEquals(courseWithEnrollment.getId(), result.get(0).getId());
}
```

**Left Join — phải cover CẢ HAI nhánh:**

```java
// Nhánh 1: có data ở bảng phụ (join match)
@Test
void it_should_include_review_count_when_reviews_exist() {
    when(courseRepository.findCoursesWithReviewCount())
        .thenReturn(List.of(buildCourseProjection(10L, 5))); // 5 reviews

    List<CourseResponse> result = courseService.getCourseSummaries();

    assertEquals(5, result.get(0).getReviewCount());
}

// Nhánh 2: không có data ở bảng phụ (join không match → NULL)
@Test
void it_should_set_review_count_to_zero_when_no_reviews_exist() {
    when(courseRepository.findCoursesWithReviewCount())
        .thenReturn(List.of(buildCourseProjection(10L, 0))); // 0 reviews

    List<CourseResponse> result = courseService.getCourseSummaries();

    assertEquals(0, result.get(0).getReviewCount());
}
```

> **Nguyên tắc:** Inner Join → cover full data path. Left/Right Join → cover cả trường hợp join match và không match.

---

## 5. Quy tắc Verify

### 5.1 Luôn verify số lần gọi

```java
// Verify gọi đúng 1 lần
verify(courseRepository, times(1)).findById(10L);

// Verify không bao giờ được gọi (abnormal flow)
verify(courseRepository, never()).save(any());

// Verify không có interaction nào khác ngoài những gì đã verify
verifyNoMoreInteractions(courseRepository, userRepository);
```

### 5.2 Verify argument cụ thể với `argThat`

Khi argument là object phức tạp, dùng `argThat` + assert bên trong:

```java
verify(courseRepository, times(1)).save(
    argThat(savedCourse -> {
        assertEquals("Giải Tích 1", savedCourse.getTitle());
        assertEquals(CourseStatus.DRAFT, savedCourse.getStatus());
        assertNotNull(savedCourse.getCreatedAt());
        assertEquals(mockTeacher.getId(), savedCourse.getTeacher().getId());
        return true; // phải return true để Mockito chấp nhận
    })
);
```

### 5.3 Verify với ArgumentCaptor khi cần inspect sau

```java
@Captor
private ArgumentCaptor<Course> courseCaptor;

@Test
void it_should_save_course_with_correct_status() {
    // ACT
    courseService.publishCourse(10L);

    // CAPTURE & ASSERT
    verify(courseRepository).save(courseCaptor.capture());
    Course savedCourse = courseCaptor.getValue();

    assertEquals(CourseStatus.PUBLISHED, savedCourse.getStatus());
    assertNotNull(savedCourse.getPublishedAt());
}
```

---

## 6. Quy tắc Assert

### 6.1 Thứ tự assert: critical trước

Assert những gì quan trọng nhất ở trên cùng. Nếu fail, message sẽ rõ hơn:

```java
// ✅ ĐÚNG — check null trước, rồi mới check giá trị
assertNotNull(result);
assertEquals(10L, result.getId());
assertEquals("Toán Cao Cấp", result.getTitle());
assertEquals(CourseStatus.PUBLISHED, result.getStatus());

// ✅ ĐÚNG — dùng assertAll để không dừng lại khi một assert fail
assertAll(
    () -> assertNotNull(result),
    () -> assertEquals(10L, result.getId()),
    () -> assertEquals("Toán Cao Cấp", result.getTitle())
);
```

### 6.2 Assert Exception

```java
@Test
void it_should_throw_exception_when_course_not_found() {
    // ARRANGE
    Long nonExistentId = 999L;
    when(courseRepository.findById(nonExistentId)).thenReturn(Optional.empty());

    // ACT & ASSERT
    AppException exception = assertThrows(
        AppException.class,
        () -> courseService.getCourseById(nonExistentId)
    );

    // Verify exception message/code
    assertEquals(ErrorCode.COURSE_NOT_FOUND, exception.getErrorCode());

    // Verify repository vẫn được gọi (không short-circuit trước đó)
    verify(courseRepository, times(1)).findById(nonExistentId);
}
```

### 6.3 Không assert những gì không liên quan đến logic đang test

```java
// ❌ SAI — test getCourseById nhưng lại assert cả teacher info
assertEquals(mockTeacher.getEmail(), result.getTeacher().getEmail()); // không liên quan

// ✅ ĐÚNG — chỉ assert những gì getCourseById chịu trách nhiệm
assertEquals(mockCourse.getId(), result.getId());
assertEquals(mockCourse.getTitle(), result.getTitle());
```

---

## 7. Quy tắc về Branch Coverage

### 7.1 Mỗi nhánh `if/else` phải có ít nhất 1 test

```java
// Service code:
public CourseResponse getCourseById(Long id) {
    Course course = courseRepository.findById(id)
        .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND)); // branch: không tìm thấy

    if (course.getStatus() == CourseStatus.DRAFT) { // branch: DRAFT
        throw new AppException(ErrorCode.COURSE_NOT_PUBLISHED);
    }
    return mapper.toResponse(course); // branch: bình thường
}

// Tests cần có:
// ✅ Test 1: happy path — course tồn tại, status PUBLISHED
// ✅ Test 2: course không tồn tại → COURSE_NOT_FOUND
// ✅ Test 3: course status DRAFT → COURSE_NOT_PUBLISHED
```

### 7.2 Comment Branch mapping ở đầu test

```java
/**
 * Normal case: getCourseById thành công
 *
 * Branch coverage:
 * - findById → present (TRUE branch của Optional)
 * - status != DRAFT (FALSE branch của status check)
 */
@Test
void it_should_return_course_when_id_is_valid_and_published() { ... }

/**
 * Abnormal case: Course ở trạng thái DRAFT
 *
 * Branch coverage:
 * - findById → present (TRUE branch — course tồn tại)
 * - status == DRAFT (TRUE branch của status check → throw exception)
 */
@Test
void it_should_throw_exception_when_course_is_draft() { ... }
```

---

## 8. Quy tắc về Mock Open / Close

Khi một service mở resource (stream, connection, lock) → phải verify cả open lẫn close:

```java
@Test
void it_should_release_lock_after_processing() {
    // ARRANGE
    when(lockService.acquireLock("course-10")).thenReturn(true);

    // ACT
    courseService.processWithLock(10L);

    // VERIFY — phải verify cả acquire VÀ release
    verify(lockService, times(1)).acquireLock("course-10");
    verify(lockService, times(1)).releaseLock("course-10"); // ← bắt buộc
}

@Test
void it_should_release_lock_even_when_processing_throws_exception() {
    // ARRANGE
    when(lockService.acquireLock("course-10")).thenReturn(true);
    when(courseRepository.findById(10L)).thenThrow(new RuntimeException("DB error"));

    // ACT & ASSERT
    assertThrows(RuntimeException.class, () -> courseService.processWithLock(10L));

    // VERIFY — lock vẫn phải được release dù có exception
    verify(lockService, times(1)).releaseLock("course-10");
}
```

---

## 9. Template đầy đủ — Normal Case

```java
/**
 * Normal case: [Mô tả ngắn gọn hành vi mong đợi]
 *
 * Branch coverage:
 * - [liệt kê các nhánh được cover trong test này]
 */
@Test
void it_should_[expected_behavior]_when_[condition]() {
    // ===== ARRANGE =====
    // 1. Chuẩn bị input
    Long courseId = 10L;

    // 2. Mock dependencies
    when(courseRepository.findById(courseId))
        .thenReturn(Optional.of(mockCourse));

    // ===== ACT =====
    CourseResponse result = courseService.getCourseById(courseId);

    // ===== ASSERT =====
    assertNotNull(result);
    assertEquals(mockCourse.getId(), result.getId());
    assertEquals(mockCourse.getTitle(), result.getTitle());

    // ===== VERIFY =====
    verify(courseRepository, times(1)).findById(courseId);
    verifyNoMoreInteractions(courseRepository);
}
```

---

## 10. Template đầy đủ — Abnormal Case

```java
/**
 * Abnormal case: [Mô tả điều kiện gây lỗi]
 *
 * Branch coverage:
 * - [liệt kê nhánh lỗi được cover]
 * - FALSE branch của [điều kiện nào đó] được cover bởi test normal
 */
@Test
void it_should_throw_exception_when_[error_condition]() {
    // ===== ARRANGE =====
    Long nonExistentId = 999L;
    when(courseRepository.findById(nonExistentId))
        .thenReturn(Optional.empty());

    // ===== ACT & ASSERT =====
    AppException exception = assertThrows(
        AppException.class,
        () -> courseService.getCourseById(nonExistentId)
    );

    assertEquals(ErrorCode.COURSE_NOT_FOUND, exception.getErrorCode());

    // ===== VERIFY =====
    verify(courseRepository, times(1)).findById(nonExistentId);
    // Verify save() KHÔNG được gọi trong trường hợp lỗi
    verify(courseRepository, never()).save(any());
}
```

---

## 11. Checklist trước khi commit test

- [ ] Test class `extend BaseUnitTest`
- [ ] Tên method theo format `it_should_..._when_...`
- [ ] Comment branch coverage ở đầu mỗi method
- [ ] Đủ 3 giai đoạn: ARRANGE / ACT / ASSERT+VERIFY
- [ ] Verify số lần gọi với `times(N)` hoặc `never()`
- [ ] `verifyNoMoreInteractions()` với các dependency quan trọng
- [ ] Mock data dùng builder method, không inline
- [ ] Mỗi nhánh `if/else` có ít nhất 1 test cover
- [ ] Left/Right Join có test cho cả trường hợp join match và không match
- [ ] Mock open/close đều được verify (nếu có lock, stream, ...)
- [ ] Data realistic (không dùng "test", "abc", "123")

---

## 13. Repository Test — Khi nào dùng `@Sql` và `@DataJpaTest`

### 13.1 Phân biệt 3 loại test liên quan đến Repository

| Loại                    | Annotation                            | Dùng khi                        | DB           |
| ----------------------- | ------------------------------------- | ------------------------------- | ------------ |
| **Unit Test (Service)** | `extend BaseUnitTest` + `@Mock`       | Test logic service, mock repo   | Không cần    |
| **Repository Test**     | `@DataJpaTest` + `@Sql`               | Test query JPQL/Native SQL thật | H2 in-memory |
| **Integration Test**    | `extend BaseIntegrationTest` + `@Sql` | Test flow end-to-end qua HTTP   | H2 in-memory |

> **Nguyên tắc:** Chỉ dùng `@Sql` khi bạn **thực sự cần chạy query thật** — tức là đang test Repository hoặc Integration flow. Unit Test service thì mock repo, không cần `@Sql`.

---

### 13.2 Base class cho Repository Test

Tạo `src/test/java/com/fptu/math_master/BaseRepositoryTest.java`:

```java
package com.fptu.math_master;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Base class cho Repository Tests.
 *
 * - @DataJpaTest: chỉ load JPA layer (Entity, Repository, H2) — KHÔNG load full context
 * - @ActiveProfiles("test"): dùng application-test.yml với H2
 * - Mỗi test tự động rollback sau khi chạy xong (@Transactional mặc định)
 */
@ExtendWith(SpringExtension.class)
@DataJpaTest
@ActiveProfiles("test")
public abstract class BaseRepositoryTest {
    // Không cần thêm gì — @DataJpaTest đã xử lý hết
}
```

---

### 13.3 Cấu trúc thư mục SQL scripts

```
src/test/resources/
└── repository/
    └── course/
        ├── find_published_courses_by_teacher/
        │   ├── setup.sql          ← INSERT data trước khi test
        │   └── teardown.sql       ← DELETE data sau test (thường không cần vì auto-rollback)
        └── find_courses_with_enrollment/
            └── setup.sql
```

> **Convention đặt tên thư mục:** `[tên_method_đang_test]` viết thường, dấu gạch dưới.

---

### 13.4 Viết `setup.sql`

File SQL phải **tự chứa đủ data**, không phụ thuộc vào data ngoài:

```sql
-- setup.sql cho: CourseRepository.findPublishedCoursesByTeacher()

-- 1. Insert teacher (phụ thuộc: User phải tồn tại trước Course)
INSERT INTO users (id, email, full_name, role, status)
VALUES (1, 'pham.dang.khoi@fptu.edu.vn', 'Phạm Đăng Khôi', 'TEACHER', 'ACTIVE');

-- 2. Insert courses với trạng thái khác nhau (để test filter)
INSERT INTO courses (id, title, status, teacher_id, created_at)
VALUES (10, 'Giải Tích 1', 'PUBLISHED', 1, NOW()),
       (11, 'Đại Số Tuyến Tính', 'DRAFT', 1, NOW()),      -- không được trả về
       (12, 'Xác Suất Thống Kê', 'PUBLISHED', 1, NOW());

-- 3. Insert course của teacher KHÁC (để test isolation)
INSERT INTO users (id, email, full_name, role, status)
VALUES (2, 'nguyen.son.nam@fptu.edu.vn', 'Nguyễn Sơn Nam', 'TEACHER', 'ACTIVE');

INSERT INTO courses (id, title, status, teacher_id, created_at)
VALUES (20, 'Vật Lý Đại Cương', 'PUBLISHED', 2, NOW()); -- không được trả về
```

**Quy tắc viết SQL:**

- Comment rõ mục đích từng block INSERT
- Insert data "nhiễu" để chứng minh query filter đúng (teacher khác, status khác)
- Dùng ID cố định (không auto-increment) để dễ assert sau

---

### 13.5 Template Repository Test đầy đủ

```java
/**
 * Test: CourseRepository - findPublishedCoursesByTeacher()
 *
 * Query: SELECT c FROM Course c WHERE c.teacher.id = :teacherId AND c.status = 'PUBLISHED'
 * JOIN type: INNER JOIN (teacher phải tồn tại)
 */
@DataJpaTest
@ActiveProfiles("test")
class CourseRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private CourseRepository courseRepository;

    // ─────────────────────────────────────────────
    // findPublishedCoursesByTeacher()
    // ─────────────────────────────────────────────

    /**
     * Normal case: trả về đúng danh sách course PUBLISHED của teacher
     *
     * Branch coverage:
     * - Teacher có course PUBLISHED → trả về list
     * - Filter đúng: loại DRAFT, loại course của teacher khác
     */
    @Test
    @Sql("find_published_courses_by_teacher/setup.sql")
    void it_should_return_only_published_courses_of_given_teacher() {
        // ===== ACT =====
        List<Course> result = courseRepository.findPublishedCoursesByTeacher(1L);

        // ===== ASSERT =====
        assertNotNull(result);
        assertEquals(2, result.size()); // chỉ id=10 và id=12

        // Verify tất cả đều là PUBLISHED
        result.forEach(course ->
            assertEquals(CourseStatus.PUBLISHED, course.getStatus())
        );

        // Verify tất cả thuộc đúng teacher
        result.forEach(course ->
            assertEquals(1L, course.getTeacher().getId())
        );

        // Verify id cụ thể (dựa trên setup.sql)
        List<Long> ids = result.stream().map(Course::getId).toList();
        assertThat(ids).containsExactlyInAnyOrder(10L, 12L);
    }

    /**
     * Abnormal case: teacher không có course nào → trả về list rỗng
     *
     * Branch coverage: LEFT branch — teacherId không match bất kỳ course nào
     */
    @Test
    @Sql("find_published_courses_by_teacher/setup.sql")
    void it_should_return_empty_list_when_teacher_has_no_published_course() {
        // ===== ACT =====
        // Teacher id=999 không tồn tại trong setup.sql
        List<Course> result = courseRepository.findPublishedCoursesByTeacher(999L);

        // ===== ASSERT =====
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
```

---

### 13.6 `@Sql` — các options quan trọng

```java
// Mặc định: chạy setup.sql TRƯỚC khi test
@Sql("setup.sql")

// Chạy nhiều file theo thứ tự
@Sql({"insert_users.sql", "insert_courses.sql"})

// Chạy TRƯỚC và SAU test (thường không cần nếu dùng @Transactional)
@Sql(scripts = "setup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "teardown.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)

// Dùng ở class level — áp dụng cho TẤT CẢ test trong class
@Sql("class_level_setup.sql")
class CourseRepositoryTest { ... }
```

> **Lưu ý:** `@DataJpaTest` đã bật `@Transactional` → mỗi test **tự động rollback** sau khi chạy. Không cần `teardown.sql` trong hầu hết trường hợp.

---

### 13.7 Quy tắc coverage cho Repository query

**Với Native Query / JPQL có LEFT JOIN:**

```java
// Query: SELECT c, COUNT(e.id) FROM Course c LEFT JOIN c.enrollments e GROUP BY c.id

// ✅ Test 1: Course CÓ enrollment → count > 0
@Test
@Sql("with_enrollment/setup.sql")
void it_should_count_enrollments_correctly_when_enrollments_exist() { ... }

// ✅ Test 2: Course KHÔNG có enrollment → count = 0 (LEFT JOIN trả NULL → COALESCE → 0)
@Test
@Sql("without_enrollment/setup.sql")
void it_should_return_zero_count_when_no_enrollments() { ... }
```

**Với INNER JOIN:**

```java
// Query: SELECT c FROM Course c INNER JOIN c.teacher t WHERE t.status = 'ACTIVE'
// → Teacher INACTIVE thì course không được trả về

// ✅ Test 1: Teacher ACTIVE → course xuất hiện
// ✅ Test 2: Teacher INACTIVE → course KHÔNG xuất hiện (bị loại bởi INNER JOIN)
```

---

## 14. Những điều KHÔNG được làm

| ❌ Không làm                                 | ✅ Thay bằng                                                  |
| -------------------------------------------- | ------------------------------------------------------------- |
| `@SpringBootTest` trong unit test            | `extend BaseUnitTest` + `@ExtendWith(MockitoExtension.class)` |
| Dùng `@Sql` trong Unit Test (Mockito)        | `when(repo.findBy...).thenReturn(...)` — mock là đủ           |
| `@Sql` không có data "nhiễu"                 | Luôn insert data của entity/teacher khác để verify isolation  |
| Dùng auto-increment ID trong `setup.sql`     | Dùng ID cố định để assert chính xác                           |
| Viết `setup.sql` phụ thuộc data từ test khác | Mỗi `setup.sql` phải tự chứa đủ data độc lập                  |
| Mock `static` method không cần thiết         | Tách logic vào helper class có thể mock                       |
| Assert chỉ `assertNotNull(result)`           | Assert từng field quan trọng                                  |
| Không có `verify()` trong Unit Test          | Luôn verify ít nhất dependency chính                          |
| Tạo object với `new` inline trong test body  | Dùng `@BeforeEach` + builder method                           |
| Test 1 method cover quá nhiều branch         | Tách thành nhiều test nhỏ theo từng branch                    |
| Tên test là `test1()`, `testOk()`            | Đặt tên theo format quy định                                  |
