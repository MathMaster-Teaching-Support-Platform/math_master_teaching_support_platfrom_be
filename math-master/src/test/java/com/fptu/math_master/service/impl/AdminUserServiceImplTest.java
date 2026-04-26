package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.dto.response.AdminUserListResponse;
import com.fptu.math_master.entity.Role;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.Status;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.EmailService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

@DisplayName("AdminUserServiceImpl - Tests")
@SuppressWarnings("unchecked")
class AdminUserServiceImplTest extends BaseUnitTest {

  @InjectMocks private AdminUserServiceImpl adminUserService;

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private EmailService emailService;

  private UUID sampleUserId;
  private User sampleUserWithRoles;

  @BeforeEach
  void setUp() {
    sampleUserId = UUID.fromString("0198a1b2-c3d4-7e8f-9a0b-1c2d3e4f5a6b");
    Role teacherRole = buildRole("TEACHER");
    sampleUserWithRoles =
        buildUser(
            sampleUserId,
            "tran.huu.loc",
            "Trần Hữu Lộc",
            "tran.huu.loc@student.fptu.edu.vn",
            Status.ACTIVE,
            Set.of(teacherRole),
            Instant.parse("2026-01-15T08:30:00Z"),
            Instant.parse("2026-04-01T12:00:00Z"));
  }

  private Role buildRole(String name) {
    Role role = Role.builder().name(name).build();
    role.setId(UUID.randomUUID());
    return role;
  }

  private User buildUser(
      UUID id,
      String userName,
      String fullName,
      String email,
      Status status,
      Set<Role> roles,
      Instant createdAt,
      Instant lastLogin) {
    User user =
        User.builder()
            .userName(userName)
            .fullName(fullName)
            .email(email)
            .status(status)
            .roles(roles)
            .build();
    user.setId(id);
    user.setCreatedAt(createdAt);
    user.setLastLogin(lastLogin);
    return user;
  }

  private void stubListUserCounts() {
    when(userRepository.countNonDeleted()).thenReturn(120L);
    when(userRepository.countByRoleName("ADMIN")).thenReturn(3L);
    when(userRepository.countByRoleName("TEACHER")).thenReturn(15L);
    when(userRepository.countStudentOnly()).thenReturn(98L);
    when(userRepository.countByStatus(Status.ACTIVE)).thenReturn(88L);
  }

  /**
   * Gọi listUsers và bắt {@link Specification} do {@code buildSpec} tạo ra — cần gọi {@link
   * Specification#toPredicate} để JaCoCo cover thân lambda buildSpec.
   */
  private Specification<User> captureListUsersSpecification(
      String role,
      String search,
      String status,
      String sortBy,
      String sortOrder,
      Instant createdFrom,
      Instant createdTo) {
    ArgumentCaptor<Specification<User>> captor = ArgumentCaptor.forClass(Specification.class);
    Pageable pageable = PageRequest.of(0, 10);
    when(userRepository.findAll(captor.capture(), eq(pageable))).thenReturn(new PageImpl<>(List.of()));
    stubListUserCounts();
    adminUserService.listUsers(role, search, status, sortBy, sortOrder, createdFrom, createdTo, pageable);
    return captor.getValue();
  }

  private void stubCriteriaAndForAnyPredicateArray(CriteriaBuilder cb) {
    when(cb.and(any(Predicate[].class))).thenAnswer(invocation -> mock(Predicate.class));
  }

  @Nested
  @DisplayName("listUsers()")
  class ListUsersTests {

    /**
     * Normal case: phân trang và thống kê khi không ép sort động.
     *
     * <p>Branch coverage: sortBy null → giữ nguyên pageable đầu vào.
     */
    @Test
    @DisplayName("Normal: trả về users, stats và pagination khi sortBy null")
    void it_should_return_users_stats_and_pagination_when_sort_by_is_null() {
      // ===== ARRANGE =====
      Pageable pageable = PageRequest.of(0, 10);
      Page<User> page = new PageImpl<>(List.of(sampleUserWithRoles), pageable, 1);
      when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
      stubListUserCounts();

      // ===== ACT =====
      AdminUserListResponse result =
          adminUserService.listUsers("all", null, "all", null, "desc", null, null, pageable);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result.getUsers()),
          () -> assertEquals(1, result.getUsers().size()),
          () -> assertEquals(sampleUserId, result.getUsers().getFirst().getId()),
          () -> assertEquals("tran.huu.loc", result.getUsers().getFirst().getUserName()),
          () -> assertEquals("Trần Hữu Lộc", result.getUsers().getFirst().getFullName()),
          () -> assertNotNull(result.getUsers().getFirst().getRoles()),
          () -> assertTrue(result.getUsers().getFirst().getRoles().contains("TEACHER")),
          () -> assertEquals(120L, result.getStats().getTotal()),
          () -> assertEquals(3L, result.getStats().getAdmins()),
          () -> assertEquals(15L, result.getStats().getTeachers()),
          () -> assertEquals(98L, result.getStats().getStudents()),
          () -> assertEquals(88L, result.getStats().getActive()),
          () -> assertEquals(0, result.getPagination().getPage()),
          () -> assertEquals(10, result.getPagination().getPageSize()),
          () -> assertEquals(1L, result.getPagination().getTotalItems()),
          () -> assertEquals(1, result.getPagination().getTotalPages()));

      // ===== VERIFY =====
      verify(userRepository, times(1)).findAll(any(Specification.class), eq(pageable));
      verify(userRepository, times(1)).countNonDeleted();
      verify(userRepository, times(1)).countByRoleName("ADMIN");
      verify(userRepository, times(1)).countByRoleName("TEACHER");
      verify(userRepository, times(1)).countStudentOnly();
      verify(userRepository, times(1)).countByStatus(Status.ACTIVE);
      verifyNoMoreInteractions(userRepository);
      verify(passwordEncoder, never()).encode(any());
      verifyNoMoreInteractions(passwordEncoder);
      verify(emailService, never()).sendDirectEmail(any(), any(), any());
      verifyNoMoreInteractions(emailService);
    }

    /**
     * Normal case: sort động theo email, hướng ASC.
     *
     * <p>Branch coverage: sortBy non-blank + sortOrder asc.
     */
    @Test
    @DisplayName("Normal: Pageable có Sort ASC theo email khi sortBy=email và sortOrder=asc")
    void it_should_apply_asc_sort_on_email_when_sort_by_is_email_and_order_is_asc() {
      // ===== ARRANGE =====
      Pageable input = PageRequest.of(1, 5);
      ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
      Page<User> page = new PageImpl<>(List.of(), input, 0);
      when(userRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(page);
      stubListUserCounts();

      // ===== ACT =====
      adminUserService.listUsers("all", null, "all", "email", "asc", null, null, input);

      // ===== ASSERT =====
      Pageable effective = pageableCaptor.getValue();
      assertAll(
          () -> assertEquals(1, effective.getPageNumber()),
          () -> assertEquals(5, effective.getPageSize()),
          () -> assertEquals(Sort.Direction.ASC, effective.getSort().getOrderFor("email").getDirection()));

      // ===== VERIFY =====
      verify(userRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
      verify(userRepository, times(1)).countNonDeleted();
      verify(userRepository, times(1)).countByRoleName("ADMIN");
      verify(userRepository, times(1)).countByRoleName("TEACHER");
      verify(userRepository, times(1)).countStudentOnly();
      verify(userRepository, times(1)).countByStatus(Status.ACTIVE);
      verifyNoMoreInteractions(userRepository);
      verifyNoMoreInteractions(passwordEncoder, emailService);
    }

    /**
     * Normal case: sortOrder khác asc (mặc định DESC).
     *
     * <p>Branch coverage: sortOrder không phải asc → Sort.Direction.DESC.
     */
    @Test
    @DisplayName("Normal: Sort DESC khi sortOrder không phải asc")
    void it_should_apply_desc_sort_when_sort_order_is_not_asc() {
      // ===== ARRANGE =====
      Pageable input = PageRequest.of(0, 20);
      ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
      Page<User> page = new PageImpl<>(List.of(), input, 0);
      when(userRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(page);
      stubListUserCounts();

      // ===== ACT =====
      adminUserService.listUsers("all", null, "all", "fullName", "invalid-order", null, null, input);

      // ===== ASSERT =====
      Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("fullName");
      assertNotNull(order);
      assertEquals(Sort.Direction.DESC, order.getDirection());

      // ===== VERIFY =====
      verify(userRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
      verify(userRepository, times(1)).countNonDeleted();
      verify(userRepository, times(1)).countByRoleName("ADMIN");
      verify(userRepository, times(1)).countByRoleName("TEACHER");
      verify(userRepository, times(1)).countStudentOnly();
      verify(userRepository, times(1)).countByStatus(Status.ACTIVE);
      verifyNoMoreInteractions(userRepository);
      verifyNoMoreInteractions(passwordEncoder, emailService);
    }

    /**
     * Normal case: sortBy không khớp alias → fallback createdAt (default switch).
     */
    @Test
    @DisplayName("Normal: fallback sort field createdAt khi sortBy không map được")
    void it_should_fallback_to_created_at_sort_when_sort_by_is_unknown() {
      // ===== ARRANGE =====
      Pageable input = PageRequest.of(0, 10);
      ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
      Page<User> page = new PageImpl<>(List.of(), input, 0);
      when(userRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(page);
      stubListUserCounts();

      // ===== ACT =====
      adminUserService.listUsers("all", null, "all", "unknownColumn", "asc", null, null, input);

      // ===== ASSERT =====
      assertNotNull(pageableCaptor.getValue().getSort().getOrderFor("createdAt"));

      // ===== VERIFY =====
      verify(userRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
      verify(userRepository, times(1)).countNonDeleted();
      verify(userRepository, times(1)).countByRoleName("ADMIN");
      verify(userRepository, times(1)).countByRoleName("TEACHER");
      verify(userRepository, times(1)).countStudentOnly();
      verify(userRepository, times(1)).countByStatus(Status.ACTIVE);
      verifyNoMoreInteractions(userRepository);
      verifyNoMoreInteractions(passwordEncoder, emailService);
    }

    /**
     * Normal case: sortBy blank → không thay Pageable.
     */
    @Test
    @DisplayName("Normal: giữ pageable gốc khi sortBy chỉ gồm khoảng trắng")
    void it_should_keep_original_pageable_when_sort_by_is_blank() {
      // ===== ARRANGE =====
      Pageable pageable = PageRequest.of(2, 15);
      Page<User> page = new PageImpl<>(List.of(), pageable, 0);
      when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
      stubListUserCounts();

      // ===== ACT =====
      adminUserService.listUsers("all", null, "all", "   ", "asc", null, null, pageable);

      // ===== VERIFY =====
      verify(userRepository, times(1)).findAll(any(Specification.class), eq(pageable));
      verify(userRepository, times(1)).countNonDeleted();
      verify(userRepository, times(1)).countByRoleName("ADMIN");
      verify(userRepository, times(1)).countByRoleName("TEACHER");
      verify(userRepository, times(1)).countStudentOnly();
      verify(userRepository, times(1)).countByStatus(Status.ACTIVE);
      verifyNoMoreInteractions(userRepository);
      verifyNoMoreInteractions(passwordEncoder, emailService);
    }

    /**
     * Normal case: roles null trên entity → UserResponse.roles null (nhánh toResponse).
     */
    @Test
    @DisplayName("Normal: roles null trong entity được map sang response null")
    void it_should_map_null_roles_when_user_has_no_roles_set() {
      // ===== ARRANGE =====
      User userNoRoles =
          buildUser(
              UUID.fromString("0198b2c3-d4e5-6f70-8a1b-2c3d4e5f6071"),
              "le.thi.mai",
              "Lê Thị Mai",
              "le.thi.mai@fptu.edu.vn",
              Status.INACTIVE,
              null,
              Instant.parse("2026-02-01T00:00:00Z"),
              null);
      Pageable pageable = PageRequest.of(0, 10);
      Page<User> page = new PageImpl<>(List.of(userNoRoles), pageable, 1);
      when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
      stubListUserCounts();

      // ===== ACT =====
      AdminUserListResponse result =
          adminUserService.listUsers("all", null, "all", null, null, null, null, pageable);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(1, result.getUsers().size()),
          () -> assertEquals("le.thi.mai@fptu.edu.vn", result.getUsers().getFirst().getEmail()),
          () -> assertEquals(null, result.getUsers().getFirst().getRoles()));

      // ===== VERIFY =====
      verify(userRepository, times(1)).findAll(any(Specification.class), eq(pageable));
      verify(userRepository, times(1)).countNonDeleted();
      verify(userRepository, times(1)).countByRoleName("ADMIN");
      verify(userRepository, times(1)).countByRoleName("TEACHER");
      verify(userRepository, times(1)).countStudentOnly();
      verify(userRepository, times(1)).countByStatus(Status.ACTIVE);
      verifyNoMoreInteractions(userRepository);
      verifyNoMoreInteractions(passwordEncoder, emailService);
    }
  }

  @Nested
  @DisplayName("mapSortField() — qua listUsers sort động")
  class MapSortFieldTests {

    private void assertSortPropertyEquals(String sortByInput, String expectedProperty) {
      Pageable input = PageRequest.of(0, 10);
      ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
      when(userRepository.findAll(any(Specification.class), pageableCaptor.capture()))
          .thenReturn(new PageImpl<>(List.of(), input, 0));
      stubListUserCounts();

      adminUserService.listUsers("all", null, "all", sortByInput, "asc", null, null, input);

      assertNotNull(pageableCaptor.getValue().getSort().getOrderFor(expectedProperty));

      verify(userRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
      verify(userRepository, times(1)).countNonDeleted();
      verify(userRepository, times(1)).countByRoleName("ADMIN");
      verify(userRepository, times(1)).countByRoleName("TEACHER");
      verify(userRepository, times(1)).countStudentOnly();
      verify(userRepository, times(1)).countByStatus(Status.ACTIVE);
      verifyNoMoreInteractions(userRepository);
      verifyNoMoreInteractions(passwordEncoder, emailService);
    }

    /**
     * Branch coverage: từng label {@code createdate} | {@code joindate} | {@code createdat} trong
     * switch mapSortField.
     */
    @ParameterizedTest(name = "sortBy={0} → createdAt")
    @ValueSource(strings = {"createDate", "joinDate", "createdAt"})
    @DisplayName("mapSortField: alias ngày tạo map sang createdAt")
    void it_should_map_join_or_create_date_alias_to_created_at_sort_field(String sortByAlias) {
      assertSortPropertyEquals(sortByAlias, "createdAt");
    }

    @Test
    @DisplayName("mapSortField: lastLogin (alias lastlogin)")
    void it_should_map_last_login_alias_to_last_login_sort_field() {
      assertSortPropertyEquals("lastLogin", "lastLogin");
    }

    @Test
    @DisplayName("mapSortField: status")
    void it_should_map_status_alias_to_status_sort_field() {
      assertSortPropertyEquals("status", "status");
    }

    @Test
    @DisplayName("mapSortField: userName (alias username)")
    void it_should_map_username_alias_to_user_name_sort_field() {
      assertSortPropertyEquals("username", "userName");
    }
  }

  @Nested
  @DisplayName("buildSpec() — thực thi Specification.toPredicate")
  class BuildSpecExecutionTests {

    @Test
    @DisplayName("Chỉ loại trừ DELETED khi role/search/status = all")
    void it_should_add_not_deleted_predicate_when_status_is_not_deleted_filter() {
      Specification<User> spec = captureListUsersSpecification("all", null, "all", null, null, null, null);
      CriteriaBuilder cb = mock(CriteriaBuilder.class);
      CriteriaQuery<?> query = mock(CriteriaQuery.class);
      Root<User> root = mock(Root.class);
      Path<Object> statusPath = mock(Path.class);
      Predicate notDel = mock(Predicate.class);
      when(root.get("status")).thenReturn(statusPath);
      when(cb.notEqual(statusPath, Status.DELETED)).thenReturn(notDel);
      stubCriteriaAndForAnyPredicateArray(cb);

      Predicate built = spec.toPredicate(root, query, cb);

      assertNotNull(built);
      verify(cb).notEqual(statusPath, Status.DELETED);
      verify(cb).and(any(Predicate[].class));
    }

    @Test
    @DisplayName("search non-blank → OR ba điều kiện LIKE trên fullName, email, userName")
    void it_should_add_or_like_predicates_when_search_is_non_blank() {
      Specification<User> spec =
          captureListUsersSpecification("all", "Vũ.Minh", "all", null, null, null, null);
      CriteriaBuilder cb = mock(CriteriaBuilder.class);
      CriteriaQuery<?> query = mock(CriteriaQuery.class);
      Root<User> root = mock(Root.class);
      Path<Object> statusPath = mock(Path.class);
      Expression<String> lowered = mock(Expression.class);
      Predicate likeA = mock(Predicate.class);
      Predicate likeB = mock(Predicate.class);
      Predicate likeC = mock(Predicate.class);
      Predicate orPred = mock(Predicate.class);
      when(root.get("status")).thenReturn(statusPath);
      when(root.get("fullName")).thenReturn(mock(Path.class));
      when(root.get("email")).thenReturn(mock(Path.class));
      when(root.get("userName")).thenReturn(mock(Path.class));
      when(cb.notEqual(statusPath, Status.DELETED)).thenReturn(mock(Predicate.class));
      when(cb.lower(any())).thenReturn(lowered);
      when(cb.like(eq(lowered), eq("%vũ.minh%"))).thenReturn(likeA, likeB, likeC);
      when(cb.or(likeA, likeB, likeC)).thenReturn(orPred);
      stubCriteriaAndForAnyPredicateArray(cb);

      assertNotNull(spec.toPredicate(root, query, cb));

      verify(cb).or(likeA, likeB, likeC);
    }

    @Test
    @DisplayName("role TEACHER → LEFT join roles + equal upper(name)")
    void it_should_left_join_roles_when_role_filter_is_teacher() {
      Specification<User> spec =
          captureListUsersSpecification("teacher", null, "all", null, null, null, null);
      CriteriaBuilder cb = mock(CriteriaBuilder.class);
      CriteriaQuery<?> query = mock(CriteriaQuery.class);
      Root<User> root = mock(Root.class);
      Path<Object> statusPath = mock(Path.class);
      Join<Object, Object> rolesJoin = mock(Join.class);
      Path<Object> roleNamePath = mock(Path.class);
      Expression<String> upperName = mock(Expression.class);
      when(root.get("status")).thenReturn(statusPath);
      when(cb.notEqual(statusPath, Status.DELETED)).thenReturn(mock(Predicate.class));
      when(root.join("roles", JoinType.LEFT)).thenReturn(rolesJoin);
      when(rolesJoin.get("name")).thenReturn(roleNamePath);
      when(cb.upper(any())).thenReturn(upperName);
      when(cb.equal(upperName, "TEACHER")).thenReturn(mock(Predicate.class));
      stubCriteriaAndForAnyPredicateArray(cb);

      assertNotNull(spec.toPredicate(root, query, cb));

      verify(root).join("roles", JoinType.LEFT);
    }

    @Test
    @DisplayName("role STUDENT_ONLY → INNER join + subquery NOT EXISTS teacher + distinct")
    void it_should_inner_join_and_subquery_when_role_is_student_only() {
      Specification<User> spec =
          captureListUsersSpecification("STUDENT_ONLY", null, "all", null, null, null, null);
      CriteriaBuilder cb = mock(CriteriaBuilder.class);
      @SuppressWarnings("rawtypes")
      CriteriaQuery query = mock(CriteriaQuery.class);
      Root<User> root = mock(Root.class);
      Path<Object> statusPath = mock(Path.class);
      Join<Object, Object> studentJoin = mock(Join.class);
      Path<Object> studentRoleName = mock(Path.class);
      Expression<String> upperStudent = mock(Expression.class);
      Expression<String> upperTeacher = mock(Expression.class);
      Subquery<Long> teacherSub = mock(Subquery.class);
      Root<User> subRoot = mock(Root.class);
      Join<Object, Object> subRoleJoin = mock(Join.class);
      Path<Object> subRoleName = mock(Path.class);
      Expression<Long> literalOne = mock(Expression.class);
      Path<Object> subIdPath = mock(Path.class);
      Path<Object> rootIdPath = mock(Path.class);
      Predicate eqIds = mock(Predicate.class);
      Predicate eqTeacher = mock(Predicate.class);
      Predicate existsPred = mock(Predicate.class);
      when(root.get("status")).thenReturn(statusPath);
      when(cb.notEqual(statusPath, Status.DELETED)).thenReturn(mock(Predicate.class));
      when(root.join("roles", JoinType.INNER)).thenReturn(studentJoin);
      when(studentJoin.get("name")).thenReturn(studentRoleName);
      when(cb.upper(any())).thenReturn(upperStudent, upperTeacher);
      when(cb.equal(upperStudent, "STUDENT")).thenReturn(mock(Predicate.class));
      when(query.subquery(Long.class)).thenReturn(teacherSub);
      when(teacherSub.from(User.class)).thenReturn(subRoot);
      when(subRoot.join("roles", JoinType.INNER)).thenReturn(subRoleJoin);
      when(cb.literal(1L)).thenReturn(literalOne);
      when(teacherSub.select(literalOne)).thenReturn(teacherSub);
      when(subRoot.get("id")).thenReturn(subIdPath);
      when(root.get("id")).thenReturn(rootIdPath);
      when(cb.equal(subIdPath, rootIdPath)).thenReturn(eqIds);
      when(subRoleJoin.get("name")).thenReturn(subRoleName);
      when(cb.equal(upperTeacher, "TEACHER")).thenReturn(eqTeacher);
      when(teacherSub.where(eqIds, eqTeacher)).thenReturn(teacherSub);
      when(cb.exists(teacherSub)).thenReturn(existsPred);
      when(cb.not(existsPred)).thenReturn(mock(Predicate.class));
      when(query.getResultType()).thenReturn((Class<?>) User.class);
      stubCriteriaAndForAnyPredicateArray(cb);

      assertNotNull(spec.toPredicate(root, query, cb));

      verify(query).distinct(true);
    }

    @Test
    @DisplayName("STUDENT_ONLY + resultType Long → không gọi distinct")
    void it_should_not_call_distinct_when_query_result_type_is_long() {
      Specification<User> spec =
          captureListUsersSpecification("STUDENT_ONLY", null, "all", null, null, null, null);
      CriteriaBuilder cb = mock(CriteriaBuilder.class);
      @SuppressWarnings("rawtypes")
      CriteriaQuery query = mock(CriteriaQuery.class);
      Root<User> root = mock(Root.class);
      Path<Object> statusPath = mock(Path.class);
      Join<Object, Object> studentJoin = mock(Join.class);
      Path<Object> studentRoleName = mock(Path.class);
      Expression<String> upperStudent = mock(Expression.class);
      Expression<String> upperTeacher = mock(Expression.class);
      Subquery<Long> teacherSub = mock(Subquery.class);
      Root<User> subRoot = mock(Root.class);
      Join<Object, Object> subRoleJoin = mock(Join.class);
      Path<Object> subRoleName = mock(Path.class);
      Expression<Long> literalOne = mock(Expression.class);
      Path<Object> subIdPath = mock(Path.class);
      Path<Object> rootIdPath = mock(Path.class);
      Predicate eqIds = mock(Predicate.class);
      Predicate eqTeacher = mock(Predicate.class);
      Predicate existsPred = mock(Predicate.class);
      when(root.get("status")).thenReturn(statusPath);
      when(cb.notEqual(statusPath, Status.DELETED)).thenReturn(mock(Predicate.class));
      when(root.join("roles", JoinType.INNER)).thenReturn(studentJoin);
      when(studentJoin.get("name")).thenReturn(studentRoleName);
      when(cb.upper(any())).thenReturn(upperStudent, upperTeacher);
      when(cb.equal(upperStudent, "STUDENT")).thenReturn(mock(Predicate.class));
      when(query.subquery(Long.class)).thenReturn(teacherSub);
      when(teacherSub.from(User.class)).thenReturn(subRoot);
      when(subRoot.join("roles", JoinType.INNER)).thenReturn(subRoleJoin);
      when(cb.literal(1L)).thenReturn(literalOne);
      when(teacherSub.select(literalOne)).thenReturn(teacherSub);
      when(subRoot.get("id")).thenReturn(subIdPath);
      when(root.get("id")).thenReturn(rootIdPath);
      when(cb.equal(subIdPath, rootIdPath)).thenReturn(eqIds);
      when(subRoleJoin.get("name")).thenReturn(subRoleName);
      when(cb.equal(upperTeacher, "TEACHER")).thenReturn(eqTeacher);
      when(teacherSub.where(eqIds, eqTeacher)).thenReturn(teacherSub);
      when(cb.exists(teacherSub)).thenReturn(existsPred);
      when(cb.not(existsPred)).thenReturn(mock(Predicate.class));
      when(query.getResultType()).thenReturn(Long.class);
      stubCriteriaAndForAnyPredicateArray(cb);

      assertNotNull(spec.toPredicate(root, query, cb));

      verify(query, never()).distinct(true);
    }

    @Test
    @DisplayName("status ACTIVE → thêm equal status; status lạ → catch, không thêm predicate")
    void it_should_add_status_predicate_or_ignore_unknown_status() {
      Specification<User> specActive =
          captureListUsersSpecification("all", null, "active", null, null, null, null);
      CriteriaBuilder cbA = mock(CriteriaBuilder.class);
      CriteriaQuery<?> qA = mock(CriteriaQuery.class);
      Root<User> rootA = mock(Root.class);
      Path<Object> statusPathA = mock(Path.class);
      when(rootA.get("status")).thenReturn(statusPathA);
      when(cbA.notEqual(statusPathA, Status.DELETED)).thenReturn(mock(Predicate.class));
      when(cbA.equal(statusPathA, Status.ACTIVE)).thenReturn(mock(Predicate.class));
      stubCriteriaAndForAnyPredicateArray(cbA);
      assertNotNull(specActive.toPredicate(rootA, qA, cbA));
      verify(cbA).equal(statusPathA, Status.ACTIVE);

      Specification<User> specUnknown =
          captureListUsersSpecification("all", null, "NOT_A_REAL_STATUS", null, null, null, null);
      CriteriaBuilder cbU = mock(CriteriaBuilder.class);
      CriteriaQuery<?> qU = mock(CriteriaQuery.class);
      Root<User> rootU = mock(Root.class);
      Path<Object> statusPathU = mock(Path.class);
      when(rootU.get("status")).thenReturn(statusPathU);
      when(cbU.notEqual(statusPathU, Status.DELETED)).thenReturn(mock(Predicate.class));
      stubCriteriaAndForAnyPredicateArray(cbU);
      assertNotNull(specUnknown.toPredicate(rootU, qU, cbU));
      verify(cbU, never()).equal(any(), any());
    }

    @Test
    @DisplayName("status DELETED → không notEqual DELETED; chỉ equal DELETED")
    void it_should_skip_not_deleted_predicate_when_filtering_deleted_status() {
      Specification<User> spec =
          captureListUsersSpecification("all", null, "deleted", null, null, null, null);
      CriteriaBuilder cb = mock(CriteriaBuilder.class);
      CriteriaQuery<?> query = mock(CriteriaQuery.class);
      Root<User> root = mock(Root.class);
      Path<Object> statusPath = mock(Path.class);
      when(root.get("status")).thenReturn(statusPath);
      when(cb.equal(statusPath, Status.DELETED)).thenReturn(mock(Predicate.class));
      stubCriteriaAndForAnyPredicateArray(cb);

      assertNotNull(spec.toPredicate(root, query, cb));

      verify(cb, never()).notEqual(statusPath, Status.DELETED);
      verify(cb).equal(statusPath, Status.DELETED);
    }

    @Test
    @DisplayName("createdFrom / createdTo → so sánh createdAt")
    void it_should_add_created_at_range_predicates_when_from_to_present() {
      Instant from = Instant.parse("2026-01-01T00:00:00Z");
      Instant to = Instant.parse("2026-12-31T23:59:59Z");
      Specification<User> spec =
          captureListUsersSpecification("all", null, "all", null, null, from, to);
      CriteriaBuilder cb = mock(CriteriaBuilder.class);
      CriteriaQuery<?> query = mock(CriteriaQuery.class);
      Root<User> root = mock(Root.class);
      Path<Object> statusPath = mock(Path.class);
      Path<Object> createdAtPath = mock(Path.class);
      when(root.get("status")).thenReturn(statusPath);
      when(root.get("createdAt")).thenReturn(createdAtPath);
      when(cb.notEqual(statusPath, Status.DELETED)).thenReturn(mock(Predicate.class));
      when(cb.greaterThanOrEqualTo(
              ArgumentMatchers.<Expression<? extends Instant>>any(), eq(from)))
          .thenReturn(mock(Predicate.class));
      when(cb.lessThanOrEqualTo(
              ArgumentMatchers.<Expression<? extends Instant>>any(), eq(to)))
          .thenReturn(mock(Predicate.class));
      stubCriteriaAndForAnyPredicateArray(cb);

      assertNotNull(spec.toPredicate(root, query, cb));

      verify(cb, times(1))
          .greaterThanOrEqualTo(ArgumentMatchers.<Expression<? extends Instant>>any(), eq(from));
      verify(cb, times(1))
          .lessThanOrEqualTo(ArgumentMatchers.<Expression<? extends Instant>>any(), eq(to));
    }
  }

  @Nested
  @DisplayName("resetPassword()")
  class ResetPasswordTests {

    /**
     * Abnormal case: user không tồn tại.
     *
     * <p>Branch coverage: Optional.empty → orElseThrow AppException.
     */
    @Test
    @DisplayName("Abnormal: USER_NOT_EXISTED khi không tìm thấy user")
    void it_should_throw_when_user_not_found_for_reset_password() {
      // ===== ARRANGE =====
      UUID missingId = UUID.fromString("0198c3d4-e5f6-7081-9a2b-3d4e5f607182");
      when(userRepository.findById(missingId)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(AppException.class, () -> adminUserService.resetPassword(missingId));
      assertEquals(ErrorCode.USER_NOT_EXISTED, ex.getErrorCode());

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(missingId);
      verifyNoMoreInteractions(userRepository);
      verify(passwordEncoder, never()).encode(any());
      verifyNoMoreInteractions(passwordEncoder);
      verify(emailService, never()).sendDirectEmail(any(), any(), any());
      verifyNoMoreInteractions(emailService);
    }

    /**
     * Normal case: đặt lại mật khẩu, lưu hash, gửi mail có fullName.
     */
    @Test
    @DisplayName("Normal: encode, save, email có tên đầy đủ khi fullName khác null")
    void it_should_encode_save_and_email_with_full_name_when_reset_succeeds() {
      // ===== ARRANGE =====
      when(userRepository.findById(sampleUserId)).thenReturn(Optional.of(sampleUserWithRoles));
      when(passwordEncoder.encode(any())).thenReturn("hashed-secret-value");

      // ===== ACT =====
      String tempPassword = adminUserService.resetPassword(sampleUserId);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(tempPassword),
          () -> assertEquals(12, tempPassword.length()));

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(sampleUserId);
      verify(passwordEncoder, times(1)).encode(tempPassword);
      verify(userRepository, times(1)).save(sampleUserWithRoles);
      verify(emailService, times(1))
          .sendDirectEmail(
              eq("tran.huu.loc@student.fptu.edu.vn"),
              eq("Mật khẩu của bạn đã được đặt lại - MathMaster"),
              argThat(
                  (String body) ->
                      body.contains("Trần Hữu Lộc")
                          && body.contains(tempPassword)
                          && body.contains("Mật khẩu tạm thời")));
      verifyNoMoreInteractions(userRepository, passwordEncoder, emailService);
    }

    /**
     * Normal case: fullName null → dùng userName trong nội dung HTML.
     */
    @Test
    @DisplayName("Normal: email dùng username khi fullName null")
    void it_should_use_username_in_email_body_when_full_name_is_null() {
      // ===== ARRANGE =====
      User user =
          buildUser(
              sampleUserId,
              "pham.minh.khang",
              null,
              "pham.minh.khang@student.fptu.edu.vn",
              Status.ACTIVE,
              new HashSet<>(),
              Instant.parse("2026-03-10T00:00:00Z"),
              null);
      when(userRepository.findById(sampleUserId)).thenReturn(Optional.of(user));
      when(passwordEncoder.encode(any())).thenReturn("hashed");

      // ===== ACT =====
      String tempPassword = adminUserService.resetPassword(sampleUserId);

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(sampleUserId);
      verify(passwordEncoder, times(1)).encode(tempPassword);
      verify(userRepository, times(1)).save(user);
      verify(emailService, times(1))
          .sendDirectEmail(
              eq("pham.minh.khang@student.fptu.edu.vn"),
              any(),
              argThat((String body) -> body.contains("pham.minh.khang") && body.contains(tempPassword)));
      verifyNoMoreInteractions(userRepository, passwordEncoder, emailService);
    }
  }

  @Nested
  @DisplayName("sendEmail()")
  class SendEmailTests {

    @Test
    @DisplayName("Abnormal: USER_NOT_EXISTED khi gửi mail cho user không tồn tại")
    void it_should_throw_when_user_not_found_for_send_email() {
      // ===== ARRANGE =====
      UUID missingId = UUID.fromString("0198d4e5-f607-8192-0a3c-4e5f60718293");
      when(userRepository.findById(missingId)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(
              AppException.class,
              () ->
                  adminUserService.sendEmail(
                      missingId, "Thông báo lịch thi", "Nội dung quan trọng"));
      assertEquals(ErrorCode.USER_NOT_EXISTED, ex.getErrorCode());

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(missingId);
      verifyNoMoreInteractions(userRepository);
      verify(emailService, never()).sendDirectEmail(any(), any(), any());
      verifyNoMoreInteractions(emailService);
      verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Normal: chuyển xuống dòng thành br và gửi HTML body")
    void it_should_wrap_body_with_br_for_newlines_when_send_email_succeeds() {
      // ===== ARRANGE =====
      when(userRepository.findById(sampleUserId)).thenReturn(Optional.of(sampleUserWithRoles));
      String subject = "Nhắc học bài Giải tích";
      String plain = "Dòng một\nDòng hai";

      // ===== ACT =====
      adminUserService.sendEmail(sampleUserId, subject, plain);

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(sampleUserId);
      verify(emailService, times(1))
          .sendDirectEmail(
              eq("tran.huu.loc@student.fptu.edu.vn"),
              eq(subject),
              argThat(
                  (String html) ->
                      html.contains("<br/>") && html.contains("Dòng một") && html.contains("Dòng hai")));
      verifyNoMoreInteractions(userRepository, passwordEncoder, emailService);
    }
  }

  @Nested
  @DisplayName("exportUsersToExcel()")
  class ExportUsersToExcelTests {

    @Test
    @DisplayName("Normal: set header, ghi workbook khi có danh sách user")
    void it_should_write_excel_bytes_when_users_exist() {
      // ===== ARRANGE =====
      when(userRepository.findAll(any(Specification.class)))
          .thenReturn(List.of(sampleUserWithRoles));
      MockHttpServletResponse response = new MockHttpServletResponse();

      // ===== ACT =====
      adminUserService.exportUsersToExcel("all", null, "all", response);

      // ===== ASSERT =====
      assertAll(
          () ->
              assertTrue(
                  response
                      .getContentType()
                      .contains("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
          () -> assertTrue(response.getContentAsByteArray().length > 200));

      // ===== VERIFY =====
      verify(userRepository, times(1)).findAll(any(Specification.class));
      verifyNoMoreInteractions(userRepository);
      verify(passwordEncoder, never()).encode(any());
      verifyNoMoreInteractions(passwordEncoder);
      verify(emailService, never()).sendDirectEmail(any(), any(), any());
      verifyNoMoreInteractions(emailService);
    }

    /**
     * Branch coverage: nhánh {@code != null ? ... : ""} trên từng cột dữ liệu — hàng toàn null/empty;
     * hàng thứ hai có đủ giá trị (và hai role để cover join tên).
     */
    @Test
    @DisplayName("Normal: Excel — ô rỗng khi field null; ô có giá trị khi field đầy đủ")
    void it_should_write_empty_or_filled_cells_depending_on_nullable_user_fields() throws Exception {
      // ===== ARRANGE =====
      User sparse = new User();
      sparse.setId(null);
      sparse.setUserName(null);
      sparse.setFullName(null);
      sparse.setEmail(null);
      sparse.setRoles(null);
      sparse.setStatus(null);
      sparse.setCreatedAt(null);
      sparse.setLastLogin(null);

      Role adminRole = buildRole("ADMIN");
      Role studentRole = buildRole("STUDENT");
      User fullRow =
          buildUser(
              UUID.fromString("0198e5f6-0718-8293-0a4d-5e6f708192a4"),
              "hoang.duc.anh",
              "Hoàng Đức Anh",
              "hoang.duc.anh@fptu.edu.vn",
              Status.INACTIVE,
              new HashSet<>(List.of(adminRole, studentRole)),
              Instant.parse("2026-06-20T14:00:00Z"),
              Instant.parse("2026-06-21T09:30:00Z"));

      when(userRepository.findAll(any(Specification.class))).thenReturn(List.of(sparse, fullRow));
      MockHttpServletResponse response = new MockHttpServletResponse();

      // ===== ACT =====
      adminUserService.exportUsersToExcel("all", null, "all", response);

      // ===== ASSERT =====
      try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))) {
        Sheet sheet = wb.getSheetAt(0);
        DataFormatter fmt = new DataFormatter();
        Row emptyRow = sheet.getRow(1);
        Row dataRow = sheet.getRow(2);
        assertAll(
            () -> assertEquals("", fmt.formatCellValue(emptyRow.getCell(0))),
            () -> assertEquals("", fmt.formatCellValue(emptyRow.getCell(1))),
            () -> assertEquals("", fmt.formatCellValue(emptyRow.getCell(2))),
            () -> assertEquals("", fmt.formatCellValue(emptyRow.getCell(3))),
            () -> assertEquals("", fmt.formatCellValue(emptyRow.getCell(4))),
            () -> assertEquals("", fmt.formatCellValue(emptyRow.getCell(5))),
            () -> assertEquals("", fmt.formatCellValue(emptyRow.getCell(6))),
            () -> assertEquals("", fmt.formatCellValue(emptyRow.getCell(7))),
            () ->
                assertEquals(
                    "0198e5f6-0718-8293-0a4d-5e6f708192a4", fmt.formatCellValue(dataRow.getCell(0))),
            () -> assertEquals("hoang.duc.anh", fmt.formatCellValue(dataRow.getCell(1))),
            () -> assertEquals("Hoàng Đức Anh", fmt.formatCellValue(dataRow.getCell(2))),
            () -> assertEquals("hoang.duc.anh@fptu.edu.vn", fmt.formatCellValue(dataRow.getCell(3))),
            () -> assertTrue(fmt.formatCellValue(dataRow.getCell(4)).contains("ADMIN")),
            () -> assertTrue(fmt.formatCellValue(dataRow.getCell(4)).contains("STUDENT")),
            () -> assertEquals("INACTIVE", fmt.formatCellValue(dataRow.getCell(5))),
            () -> assertTrue(fmt.formatCellValue(dataRow.getCell(6)).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")),
            () -> assertTrue(fmt.formatCellValue(dataRow.getCell(7)).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")));
      }

      // ===== VERIFY =====
      verify(userRepository, times(1)).findAll(any(Specification.class));
      verifyNoMoreInteractions(userRepository);
      verifyNoMoreInteractions(passwordEncoder, emailService);
    }

    /**
     * Abnormal case: IOException khi ghi stream → UNCATEGORIZED_EXCEPTION.
     */
    @Test
    @DisplayName("Abnormal: UNCATEGORIZED_EXCEPTION khi getOutputStream ném IOException")
    void it_should_throw_uncategorized_when_response_output_stream_throws_io_exception()
        throws IOException {
      // ===== ARRANGE =====
      when(userRepository.findAll(any(Specification.class))).thenReturn(List.of(sampleUserWithRoles));
      HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);
      when(response.getOutputStream()).thenThrow(new IOException("simulated-disk-failure"));

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(
              AppException.class,
              () -> adminUserService.exportUsersToExcel("all", null, "all", response));
      assertEquals(ErrorCode.UNCATEGORIZED_EXCEPTION, ex.getErrorCode());

      // ===== VERIFY =====
      verify(userRepository, times(1)).findAll(any(Specification.class));
      verifyNoMoreInteractions(userRepository);
      verifyNoMoreInteractions(passwordEncoder, emailService);
    }
  }
}
