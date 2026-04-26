package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.component.StreamPublisher;
import com.fptu.math_master.dto.request.NotificationRequest;
import com.fptu.math_master.dto.response.OrderResponse;
import com.fptu.math_master.entity.Course;
import com.fptu.math_master.entity.Enrollment;
import com.fptu.math_master.entity.Order;
import com.fptu.math_master.entity.Transaction;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.entity.Wallet;
import com.fptu.math_master.enums.CourseStatus;
import com.fptu.math_master.enums.EnrollmentStatus;
import com.fptu.math_master.enums.OrderStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.CourseRepository;
import com.fptu.math_master.repository.EnrollmentRepository;
import com.fptu.math_master.repository.OrderRepository;
import com.fptu.math_master.repository.TransactionRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.repository.WalletRepository;
import com.fptu.math_master.service.EmailService;
import com.fptu.math_master.service.WalletService;
import com.fptu.math_master.util.SecurityUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@DisplayName("OrderServiceImpl - Tests")
class OrderServiceImplTest extends BaseUnitTest {

  private static final UUID STUDENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID STUDENT_2_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
  private static final UUID TEACHER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
  private static final UUID ADMIN_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
  private static final UUID COURSE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID ORDER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID ENROLLMENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID STUDENT_WALLET_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID INSTRUCTOR_WALLET_ID =
      UUID.fromString("55555555-5555-5555-5555-555555555555");
  private static final UUID ADMIN_WALLET_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

  @InjectMocks private OrderServiceImpl orderService;

  @Mock private OrderRepository orderRepository;
  @Mock private CourseRepository courseRepository;
  @Mock private EnrollmentRepository enrollmentRepository;
  @Mock private WalletRepository walletRepository;
  @Mock private TransactionRepository transactionRepository;
  @Mock private UserRepository userRepository;
  @Mock private WalletService walletService;
  @Mock private StreamPublisher streamPublisher;
  @Mock private EmailService emailService;

  @Nested
  @DisplayName("createOrder()")
  class CreateOrderTests {

    /**
     * Abnormal case: Ném exception khi course không tồn tại.
     *
     * <p>Input:
     * <ul>
     *   <li>courseId: COURSE_ID (không có trong repository)</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>findByIdAndDeletedAtIsNull -> empty (throw COURSE_NOT_FOUND)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code COURSE_NOT_FOUND}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_course_does_not_exist() {
      // ===== ARRANGE =====
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        AppException exception = assertThrows(AppException.class, () -> orderService.createOrder(COURSE_ID));
        assertEquals(ErrorCode.COURSE_NOT_FOUND, exception.getErrorCode());
      }

      // ===== VERIFY =====
      verify(courseRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_ID);
      verifyNoMoreInteractions(courseRepository);
      verifyNoMoreInteractions(orderRepository, enrollmentRepository, userRepository);
    }

    /**
     * Abnormal case: Ném exception khi course chưa publish.
     *
     * <p>Input:
     * <ul>
     *   <li>courseId: COURSE_ID (status DRAFT)</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>course.isPublished() == false (throw COURSE_NOT_PUBLISHED)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code COURSE_NOT_PUBLISHED}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_course_is_not_published() {
      // ===== ARRANGE =====
      Course draftCourse = buildCourse(CourseStatus.DRAFT, new BigDecimal("299000"), null, null);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(draftCourse));

      // ===== ACT & ASSERT =====
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        AppException exception = assertThrows(AppException.class, () -> orderService.createOrder(COURSE_ID));
        assertEquals(ErrorCode.COURSE_NOT_PUBLISHED, exception.getErrorCode());
      }

      // ===== VERIFY =====
      verify(courseRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_ID);
      verify(enrollmentRepository, never()).findByStudentIdAndCourseIdAndDeletedAtIsNull(any(), any());
    }

    /**
     * Abnormal case: Ném exception khi student đã enrolled ACTIVE.
     *
     * <p>Input:
     * <ul>
     *   <li>existing enrollment status: ACTIVE</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>existingEnrollment.isPresent() == true</li>
     *   <li>existingEnrollment.status == ACTIVE (throw ALREADY_ENROLLED)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code ALREADY_ENROLLED}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_student_is_already_enrolled() {
      // ===== ARRANGE =====
      Course publishedCourse = buildCourse(CourseStatus.PUBLISHED, new BigDecimal("299000"), null, null);
      Enrollment enrollment = new Enrollment();
      enrollment.setStatus(EnrollmentStatus.ACTIVE);

      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(publishedCourse));
      when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(STUDENT_ID, COURSE_ID))
          .thenReturn(Optional.of(enrollment));

      // ===== ACT & ASSERT =====
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        AppException exception = assertThrows(AppException.class, () -> orderService.createOrder(COURSE_ID));
        assertEquals(ErrorCode.ALREADY_ENROLLED, exception.getErrorCode());
      }

      // ===== VERIFY =====
      verify(enrollmentRepository, times(1))
          .findByStudentIdAndCourseIdAndDeletedAtIsNull(STUDENT_ID, COURSE_ID);
      verify(orderRepository, never()).save(any(Order.class));
    }

    /**
     * Normal case: Trả về pending order đã tồn tại.
     *
     * <p>Input:
     * <ul>
     *   <li>studentId: STUDENT_ID</li>
     *   <li>course có status PUBLISHED</li>
     *   <li>pendingOrders có 1 phần tử</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>existingEnrollment empty (không throw ALREADY_ENROLLED)</li>
     *   <li>pendingOrders.isEmpty() == false (return existing order)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Không tạo order mới; trả về order pending hiện tại</li>
     * </ul>
     */
    @Test
    void it_should_return_existing_pending_order_when_pending_order_exists() {
      // ===== ARRANGE =====
      Course publishedCourse = buildCourse(CourseStatus.PUBLISHED, new BigDecimal("299000"), null, null);
      Order existingOrder = buildOrder(OrderStatus.PENDING, new BigDecimal("269000"), Instant.now().plusSeconds(90));
      existingOrder.setOrderNumber("ORD-202604260001-0001");
      User student = buildUser(STUDENT_ID, "Nguyen Son Nam", "nam@student.fptu.edu.vn");

      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(publishedCourse));
      when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(STUDENT_ID, COURSE_ID))
          .thenReturn(Optional.empty());
      when(orderRepository.findByStudentIdAndCourseIdAndStatusInAndDeletedAtIsNull(
              eq(STUDENT_ID), eq(COURSE_ID), anyList()))
          .thenReturn(List.of(existingOrder));
      when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

      // ===== ACT =====
      OrderResponse result;
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        result = orderService.createOrder(COURSE_ID);
      }

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(existingOrder.getId(), result.getId()),
          () -> assertEquals("ORD-202604260001-0001", result.getOrderNumber()),
          () -> assertEquals(OrderStatus.PENDING, result.getStatus()),
          () -> assertEquals("Nguyen Son Nam", result.getStudentName()));

      // ===== VERIFY =====
      verify(orderRepository, never()).save(any(Order.class));
      verify(userRepository, times(1)).findById(STUDENT_ID);
    }

    /**
     * Normal case: Tạo order mới khi không có enrollment active và không có pending order.
     *
     * <p>Input:
     * <ul>
     *   <li>course có discountedPrice còn hạn</li>
     *   <li>student chưa có enrollment active</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>discountedPrice != null && discount chưa hết hạn (nhánh dùng giá giảm)</li>
     *   <li>pendingOrders.isEmpty() == true (nhánh tạo order mới)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Lưu order với split doanh thu đúng 90/10 và trả về response</li>
     * </ul>
     */
    @Test
    void it_should_create_new_order_when_no_pending_order_exists() {
      // ===== ARRANGE =====
      Course publishedCourse =
          buildCourse(
              CourseStatus.PUBLISHED,
              new BigDecimal("300000"),
              new BigDecimal("240000"),
              Instant.now().plusSeconds(3_600));
      User student = buildUser(STUDENT_ID, "Tran Thi Linh", "linh@student.fptu.edu.vn");
      Order savedOrder = buildOrder(OrderStatus.PENDING, new BigDecimal("240000"), Instant.now().plusSeconds(600));
      savedOrder.setOriginalPrice(new BigDecimal("300000"));
      savedOrder.setDiscountAmount(new BigDecimal("60000"));
      savedOrder.setInstructorEarnings(new BigDecimal("216000.00"));
      savedOrder.setPlatformCommission(new BigDecimal("24000.00"));

      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(publishedCourse));
      when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(STUDENT_ID, COURSE_ID))
          .thenReturn(Optional.empty());
      when(orderRepository.findByStudentIdAndCourseIdAndStatusInAndDeletedAtIsNull(
              eq(STUDENT_ID), eq(COURSE_ID), anyList()))
          .thenReturn(Collections.emptyList());
      when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
      when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

      // ===== ACT =====
      OrderResponse result;
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        result = orderService.createOrder(COURSE_ID);
      }

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(OrderStatus.PENDING, result.getStatus()),
          () -> assertEquals(new BigDecimal("300000"), result.getOriginalPrice()),
          () -> assertEquals(new BigDecimal("60000"), result.getDiscountAmount()),
          () -> assertEquals(new BigDecimal("240000"), result.getFinalPrice()),
          () -> assertEquals(new BigDecimal("216000.00"), result.getInstructorEarnings()),
          () -> assertEquals(new BigDecimal("24000.00"), result.getPlatformCommission()));

      // ===== VERIFY =====
      verify(orderRepository, times(1)).save(any(Order.class));
      verify(userRepository, times(1)).findById(STUDENT_ID);
      verifyNoMoreInteractions(walletRepository, walletService, transactionRepository);
    }

    /**
     * Normal case: Dùng originalPrice khi discountedPrice đã hết hạn.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>discountedPrice != null nhưng discountExpiryDate.isAfter(now) == false</li>
     *   <li>existingEnrollment present nhưng status != ACTIVE (không throw)</li>
     * </ul>
     */
    @Test
    void it_should_use_original_price_when_discount_has_expired() {
      // ===== ARRANGE =====
      Course publishedCourse =
          buildCourse(
              CourseStatus.PUBLISHED,
              new BigDecimal("350000"),
              new BigDecimal("299000"),
              Instant.now().minusSeconds(3_600));
      Enrollment inactiveEnrollment = new Enrollment();
      inactiveEnrollment.setStatus(EnrollmentStatus.DROPPED);
      Order savedOrder = buildOrder(OrderStatus.PENDING, new BigDecimal("350000"), Instant.now().plusSeconds(600));
      savedOrder.setOriginalPrice(new BigDecimal("350000"));
      savedOrder.setDiscountAmount(BigDecimal.ZERO);
      savedOrder.setInstructorEarnings(new BigDecimal("315000.00"));
      savedOrder.setPlatformCommission(new BigDecimal("35000.00"));
      User student = buildUser(STUDENT_ID, "Ngo Phuong Nhi", "phuong.nhi@student.fptu.edu.vn");

      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(publishedCourse));
      when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(STUDENT_ID, COURSE_ID))
          .thenReturn(Optional.of(inactiveEnrollment));
      when(orderRepository.findByStudentIdAndCourseIdAndStatusInAndDeletedAtIsNull(
              eq(STUDENT_ID), eq(COURSE_ID), anyList()))
          .thenReturn(Collections.emptyList());
      when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
      when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

      // ===== ACT =====
      OrderResponse result;
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        result = orderService.createOrder(COURSE_ID);
      }

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(new BigDecimal("350000"), result.getFinalPrice()),
          () -> assertEquals(BigDecimal.ZERO, result.getDiscountAmount()),
          () -> assertEquals(new BigDecimal("315000.00"), result.getInstructorEarnings()),
          () -> assertEquals(new BigDecimal("35000.00"), result.getPlatformCommission()));

      // ===== VERIFY =====
      verify(orderRepository, times(1)).save(any(Order.class));
      verify(userRepository, times(1)).findById(STUDENT_ID);
    }
  }

  @Nested
  @DisplayName("getOrder()")
  class GetOrderTests {

    /**
     * Abnormal case: Ném exception khi order không thuộc về student hiện tại.
     *
     * <p>Input:
     * <ul>
     *   <li>orderId: ORDER_ID</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>findByIdAndStudentId -> empty (throw ORDER_NOT_FOUND)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code ORDER_NOT_FOUND}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_order_not_found_for_student() {
      // ===== ARRANGE =====
      when(orderRepository.findByIdAndStudentId(ORDER_ID, STUDENT_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        AppException exception = assertThrows(AppException.class, () -> orderService.getOrder(ORDER_ID));
        assertEquals(ErrorCode.ORDER_NOT_FOUND, exception.getErrorCode());
      }

      // ===== VERIFY =====
      verify(orderRepository, times(1)).findByIdAndStudentId(ORDER_ID, STUDENT_ID);
      verifyNoMoreInteractions(orderRepository, courseRepository);
    }

    /**
     * Normal case: Trả về order thành công khi course có thể null.
     *
     * <p>Input:
     * <ul>
     *   <li>order tồn tại của student hiện tại</li>
     *   <li>course lookup trả về empty</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>courseRepository.findByIdAndDeletedAtIsNull -> empty (course null branch trong map)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Response có courseTitle null nhưng vẫn trả đủ thông tin order</li>
     * </ul>
     */
    @Test
    void it_should_return_order_with_null_course_info_when_course_not_found() {
      // ===== ARRANGE =====
      Order order = buildOrder(OrderStatus.PENDING, new BigDecimal("120000"), Instant.now().plusSeconds(600));
      User student = buildUser(STUDENT_ID, "Le Minh Tuan", "tuan@student.fptu.edu.vn");
      when(orderRepository.findByIdAndStudentId(ORDER_ID, STUDENT_ID)).thenReturn(Optional.of(order));
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.empty());
      when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

      // ===== ACT =====
      OrderResponse result;
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        result = orderService.getOrder(ORDER_ID);
      }

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(order.getId(), result.getId()),
          () -> assertNull(result.getCourseTitle()),
          () -> assertEquals("Le Minh Tuan", result.getStudentName()));

      // ===== VERIFY =====
      verify(orderRepository, times(1)).findByIdAndStudentId(ORDER_ID, STUDENT_ID);
      verify(courseRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_ID);
      verify(userRepository, times(1)).findById(STUDENT_ID);
    }
  }

  @Nested
  @DisplayName("getOrderByNumber()")
  class GetOrderByNumberTests {

    /**
     * Abnormal case: Ném exception khi order number không tồn tại.
     */
    @Test
    void it_should_throw_exception_when_order_number_not_found() {
      // ===== ARRANGE =====
      when(orderRepository.findByOrderNumber("ORD-MISSING")).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        AppException exception =
            assertThrows(AppException.class, () -> orderService.getOrderByNumber("ORD-MISSING"));
        assertEquals(ErrorCode.ORDER_NOT_FOUND, exception.getErrorCode());
      }

      // ===== VERIFY =====
      verify(orderRepository, times(1)).findByOrderNumber("ORD-MISSING");
      verifyNoMoreInteractions(orderRepository, courseRepository);
    }

    /**
     * Abnormal case: Ném exception khi order thuộc về student khác.
     */
    @Test
    void it_should_throw_exception_when_order_belongs_to_another_student() {
      // ===== ARRANGE =====
      Order foreignOrder = buildOrder(OrderStatus.PENDING, new BigDecimal("100000"), Instant.now().plusSeconds(600));
      foreignOrder.setStudentId(STUDENT_2_ID);
      when(orderRepository.findByOrderNumber("ORD-FOREIGN")).thenReturn(Optional.of(foreignOrder));

      // ===== ACT & ASSERT =====
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        AppException exception =
            assertThrows(AppException.class, () -> orderService.getOrderByNumber("ORD-FOREIGN"));
        assertEquals(ErrorCode.ORDER_ACCESS_DENIED, exception.getErrorCode());
      }

      // ===== VERIFY =====
      verify(orderRepository, times(1)).findByOrderNumber("ORD-FOREIGN");
      verify(courseRepository, never()).findByIdAndDeletedAtIsNull(any());
    }

    /**
     * Normal case: Trả về order theo order number khi owner hợp lệ.
     */
    @Test
    void it_should_return_order_when_order_number_exists_and_owner_matches() {
      // ===== ARRANGE =====
      Order order = buildOrder(OrderStatus.PENDING, new BigDecimal("110000"), Instant.now().plusSeconds(600));
      order.setOrderNumber("ORD-202604260909-8989");
      Course course = buildCourse(CourseStatus.PUBLISHED, new BigDecimal("110000"), null, null);
      User student = buildUser(STUDENT_ID, "Phan Quoc Bao", "bao@student.fptu.edu.vn");
      when(orderRepository.findByOrderNumber("ORD-202604260909-8989")).thenReturn(Optional.of(order));
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

      // ===== ACT =====
      OrderResponse result;
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        result = orderService.getOrderByNumber("ORD-202604260909-8989");
      }

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals("ORD-202604260909-8989", result.getOrderNumber()),
          () -> assertEquals("Giải tích nâng cao cho kỹ sư", result.getCourseTitle()),
          () -> assertEquals("Phan Quoc Bao", result.getStudentName()));

      // ===== VERIFY =====
      verify(orderRepository, times(1)).findByOrderNumber("ORD-202604260909-8989");
      verify(courseRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_ID);
      verify(userRepository, times(1)).findById(STUDENT_ID);
    }
  }

  @Nested
  @DisplayName("confirmOrder()")
  class ConfirmOrderTests {

    /**
     * Abnormal case: Ném exception khi order status khác PENDING.
     */
    @Test
    void it_should_throw_exception_when_order_is_already_processed() {
      // ===== ARRANGE =====
      Order processedOrder =
          buildOrder(OrderStatus.COMPLETED, new BigDecimal("100000"), Instant.now().plusSeconds(600));
      when(orderRepository.findByIdAndStudentId(ORDER_ID, STUDENT_ID))
          .thenReturn(Optional.of(processedOrder));

      // ===== ACT & ASSERT =====
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        AppException exception = assertThrows(AppException.class, () -> orderService.confirmOrder(ORDER_ID));
        assertEquals(ErrorCode.ORDER_ALREADY_PROCESSED, exception.getErrorCode());
      }

      // ===== VERIFY =====
      verify(courseRepository, never()).findByIdAndDeletedAtIsNull(any());
      verify(orderRepository, never()).saveAndFlush(any(Order.class));
    }

    /**
     * Abnormal case: Ném exception khi order đã hết hạn.
     */
    @Test
    void it_should_throw_exception_when_order_is_expired() {
      // ===== ARRANGE =====
      Order expiredOrder =
          buildOrder(OrderStatus.PENDING, new BigDecimal("100000"), Instant.now().minusSeconds(60));
      when(orderRepository.findByIdAndStudentId(ORDER_ID, STUDENT_ID)).thenReturn(Optional.of(expiredOrder));

      // ===== ACT & ASSERT =====
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        AppException exception = assertThrows(AppException.class, () -> orderService.confirmOrder(ORDER_ID));
        assertEquals(ErrorCode.ORDER_EXPIRED, exception.getErrorCode());
      }

      // ===== VERIFY =====
      verify(orderRepository, never()).saveAndFlush(any(Order.class));
      verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    /**
     * Normal case: Confirm order miễn phí (finalPrice = 0), không đi qua payment flow.
     */
    @Test
    void it_should_confirm_free_order_without_payment_processing() {
      // ===== ARRANGE =====
      Order freeOrder = buildOrder(OrderStatus.PENDING, BigDecimal.ZERO, Instant.now().plusSeconds(900));
      Course course = buildCourse(CourseStatus.PUBLISHED, BigDecimal.ZERO, null, null);
      Enrollment enrollment = new Enrollment();
      enrollment.setId(ENROLLMENT_ID);
      enrollment.setCourseId(COURSE_ID);
      enrollment.setStudentId(STUDENT_ID);
      enrollment.setStatus(EnrollmentStatus.ACTIVE);
      enrollment.setEnrolledAt(Instant.now());
      User student = buildUser(STUDENT_ID, "Pham Gia Han", "han@student.fptu.edu.vn");
      User teacher = buildUser(TEACHER_ID, "Doang Van Tri", "tri@fptu.edu.vn");

      when(orderRepository.findByIdAndStudentId(ORDER_ID, STUDENT_ID)).thenReturn(Optional.of(freeOrder));
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(enrollment);
      when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
      when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));

      // ===== ACT =====
      OrderResponse result;
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        result = orderService.confirmOrder(ORDER_ID);
      }

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(OrderStatus.COMPLETED, result.getStatus()),
          () -> assertEquals(ENROLLMENT_ID, result.getEnrollmentId()),
          () -> assertNotNull(result.getConfirmedAt()));

      // ===== VERIFY =====
      verify(walletService, never()).deductBalance(any(), any());
      verify(transactionRepository, never()).save(any(Transaction.class));
      verify(orderRepository, times(1)).saveAndFlush(any(Order.class));
      verify(orderRepository, times(1)).save(any(Order.class));
      verify(streamPublisher, times(2)).publish(any(NotificationRequest.class));
      verify(emailService, times(1))
          .sendOrderConfirmationEmail(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
      verify(emailService, times(1))
          .sendNewEnrollmentEmail(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    /**
     * Normal case: Confirm order trả phí với commission > 0, đi qua đầy đủ payment flow.
     */
    @Test
    void it_should_confirm_paid_order_and_distribute_wallet_balances() {
      // ===== ARRANGE =====
      Order paidOrder = buildOrder(OrderStatus.PENDING, new BigDecimal("200000"), Instant.now().plusSeconds(900));
      paidOrder.setInstructorEarnings(new BigDecimal("180000.00"));
      paidOrder.setPlatformCommission(new BigDecimal("20000.00"));
      Course course = buildCourse(CourseStatus.PUBLISHED, new BigDecimal("250000"), null, null);
      Enrollment enrollment = new Enrollment();
      enrollment.setId(ENROLLMENT_ID);
      enrollment.setCourseId(COURSE_ID);
      enrollment.setStudentId(STUDENT_ID);
      enrollment.setStatus(EnrollmentStatus.ACTIVE);
      Wallet studentWallet = buildWallet(STUDENT_WALLET_ID, STUDENT_ID, new BigDecimal("500000"));
      Wallet instructorWallet = buildWallet(INSTRUCTOR_WALLET_ID, TEACHER_ID, new BigDecimal("120000"));
      Wallet adminWallet = buildWallet(ADMIN_WALLET_ID, ADMIN_ID, new BigDecimal("90000"));
      User student = buildUser(STUDENT_ID, "Vu Hoang Anh", "anh@student.fptu.edu.vn");
      User teacher = buildUser(TEACHER_ID, "Nguyen Minh Quan", "quan@fptu.edu.vn");

      when(orderRepository.findByIdAndStudentId(ORDER_ID, STUDENT_ID)).thenReturn(Optional.of(paidOrder));
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(walletRepository.findByUserId(STUDENT_ID)).thenReturn(Optional.of(studentWallet));
      when(walletRepository.findByUserId(TEACHER_ID)).thenReturn(Optional.of(instructorWallet));
      when(userRepository.findUserIdsByRoleName("ADMIN")).thenReturn(List.of(ADMIN_ID));
      when(walletRepository.findByUserId(ADMIN_ID)).thenReturn(Optional.of(adminWallet));
      when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(enrollment);
      when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
      when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));

      // ===== ACT =====
      OrderResponse result;
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        result = orderService.confirmOrder(ORDER_ID);
      }

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(OrderStatus.COMPLETED, result.getStatus()),
          () -> assertEquals(ENROLLMENT_ID, result.getEnrollmentId()));

      // ===== VERIFY =====
      verify(walletService, times(1)).deductBalance(STUDENT_WALLET_ID, new BigDecimal("200000"));
      verify(walletService, times(1)).addBalance(INSTRUCTOR_WALLET_ID, new BigDecimal("180000.00"));
      verify(walletService, times(1)).addBalance(ADMIN_WALLET_ID, new BigDecimal("20000.00"));
      verify(transactionRepository, times(6)).save(any(Transaction.class));
    }

    /**
     * Abnormal case: Khi payment throw exception thì order chuyển sang FAILED và rethrow lỗi.
     */
    @Test
    void it_should_mark_order_failed_when_payment_processing_throws_exception() {
      // ===== ARRANGE =====
      Order paidOrder = buildOrder(OrderStatus.PENDING, new BigDecimal("150000"), Instant.now().plusSeconds(900));
      paidOrder.setInstructorEarnings(new BigDecimal("135000.00"));
      paidOrder.setPlatformCommission(new BigDecimal("15000.00"));
      Course course = buildCourse(CourseStatus.PUBLISHED, new BigDecimal("150000"), null, null);
      Wallet lowBalanceWallet = buildWallet(STUDENT_WALLET_ID, STUDENT_ID, new BigDecimal("1000"));

      when(orderRepository.findByIdAndStudentId(ORDER_ID, STUDENT_ID)).thenReturn(Optional.of(paidOrder));
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(walletRepository.findByUserId(STUDENT_ID)).thenReturn(Optional.of(lowBalanceWallet));
      when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

      // ===== ACT & ASSERT =====
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        AppException exception = assertThrows(AppException.class, () -> orderService.confirmOrder(ORDER_ID));
        assertEquals(ErrorCode.INSUFFICIENT_BALANCE, exception.getErrorCode());
      }

      // ===== VERIFY =====
      verify(orderRepository, times(1)).saveAndFlush(any(Order.class));
      verify(orderRepository, times(1)).save(any(Order.class));
      verify(enrollmentRepository, never()).save(any(Enrollment.class));
      assertEquals(OrderStatus.FAILED, paidOrder.getStatus());
      assertTrue(paidOrder.getCancellationReason().startsWith("Payment failed:"));
    }

    /**
     * Normal case: Confirm order trả phí nhưng commission bằng 0 và bỏ qua email khi thiếu user.
     */
    @Test
    void it_should_confirm_paid_order_without_platform_commission_and_skip_emails_when_users_missing() {
      // ===== ARRANGE =====
      Order paidOrder = buildOrder(OrderStatus.PENDING, new BigDecimal("100000"), null);
      paidOrder.setInstructorEarnings(new BigDecimal("100000.00"));
      paidOrder.setPlatformCommission(BigDecimal.ZERO);
      Course course = buildCourse(CourseStatus.PUBLISHED, new BigDecimal("100000"), null, null);
      Enrollment enrollment = new Enrollment();
      enrollment.setId(ENROLLMENT_ID);
      enrollment.setCourseId(COURSE_ID);
      enrollment.setStudentId(STUDENT_ID);
      enrollment.setStatus(EnrollmentStatus.ACTIVE);
      Wallet studentWallet = buildWallet(STUDENT_WALLET_ID, STUDENT_ID, new BigDecimal("200000"));
      Wallet instructorWallet = buildWallet(INSTRUCTOR_WALLET_ID, TEACHER_ID, new BigDecimal("150000"));

      when(orderRepository.findByIdAndStudentId(ORDER_ID, STUDENT_ID)).thenReturn(Optional.of(paidOrder));
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(walletRepository.findByUserId(STUDENT_ID)).thenReturn(Optional.of(studentWallet));
      when(walletRepository.findByUserId(TEACHER_ID)).thenReturn(Optional.of(instructorWallet));
      when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(enrollment);
      when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());
      when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.empty());
      doThrow(new RuntimeException("Notification gateway down"))
          .when(streamPublisher)
          .publish(any(NotificationRequest.class));

      // ===== ACT =====
      OrderResponse result;
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        result = orderService.confirmOrder(ORDER_ID);
      }

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(OrderStatus.COMPLETED, result.getStatus()),
          () -> assertEquals(ENROLLMENT_ID, result.getEnrollmentId()));

      // ===== VERIFY =====
      verify(walletService, times(1)).deductBalance(STUDENT_WALLET_ID, new BigDecimal("100000"));
      verify(walletService, times(1)).addBalance(INSTRUCTOR_WALLET_ID, new BigDecimal("100000.00"));
      verify(walletService, never()).addBalance(eq(ADMIN_WALLET_ID), any(BigDecimal.class));
      verify(emailService, never())
          .sendOrderConfirmationEmail(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
      verify(emailService, never())
          .sendNewEnrollmentEmail(anyString(), anyString(), anyString(), anyString(), anyString());
    }
  }

  @Nested
  @DisplayName("cancelOrder()")
  class CancelOrderTests {

    /**
     * Abnormal case: Ném exception khi trạng thái order không phải PENDING.
     */
    @Test
    void it_should_throw_exception_when_order_status_is_not_pending() {
      // ===== ARRANGE =====
      Order processingOrder =
          buildOrder(OrderStatus.PROCESSING, new BigDecimal("180000"), Instant.now().plusSeconds(120));
      when(orderRepository.findByIdAndStudentId(ORDER_ID, STUDENT_ID)).thenReturn(Optional.of(processingOrder));

      // ===== ACT & ASSERT =====
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        AppException exception =
            assertThrows(
                AppException.class, () -> orderService.cancelOrder(ORDER_ID, "Không tiếp tục mua nữa"));
        assertEquals(ErrorCode.ORDER_CANNOT_BE_CANCELLED, exception.getErrorCode());
      }

      // ===== VERIFY =====
      verify(orderRepository, never()).save(any(Order.class));
      verify(courseRepository, never()).findByIdAndDeletedAtIsNull(any());
    }

    /**
     * Normal case: Cancel order thành công và dùng default reason khi truyền null.
     */
    @Test
    void it_should_cancel_order_with_default_reason_when_reason_is_null() {
      // ===== ARRANGE =====
      Order pendingOrder = buildOrder(OrderStatus.PENDING, new BigDecimal("90000"), Instant.now().plusSeconds(120));
      Course course = buildCourse(CourseStatus.PUBLISHED, new BigDecimal("90000"), null, null);
      User student = buildUser(STUDENT_ID, "Huynh Hai Dang", "dang@student.fptu.edu.vn");

      when(orderRepository.findByIdAndStudentId(ORDER_ID, STUDENT_ID)).thenReturn(Optional.of(pendingOrder));
      when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

      // ===== ACT =====
      OrderResponse result;
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        result = orderService.cancelOrder(ORDER_ID, null);
      }

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(OrderStatus.CANCELLED, result.getStatus()),
          () -> assertEquals("Cancelled by user", result.getCancellationReason()),
          () -> assertNotNull(result.getCancelledAt()));

      // ===== VERIFY =====
      verify(orderRepository, times(1)).save(any(Order.class));
      verify(courseRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_ID);
    }

    /**
     * Normal case: Cancel order và lưu custom reason khi được cung cấp.
     */
    @Test
    void it_should_cancel_order_with_given_reason_when_reason_is_not_null() {
      // ===== ARRANGE =====
      Order pendingOrder = buildOrder(OrderStatus.PENDING, new BigDecimal("130000"), Instant.now().plusSeconds(120));
      Course course = buildCourse(CourseStatus.PUBLISHED, new BigDecimal("130000"), null, null);
      User student = buildUser(STUDENT_ID, "Le Hai Long", "long@student.fptu.edu.vn");

      when(orderRepository.findByIdAndStudentId(ORDER_ID, STUDENT_ID)).thenReturn(Optional.of(pendingOrder));
      when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

      // ===== ACT =====
      OrderResponse result;
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        result = orderService.cancelOrder(ORDER_ID, "Đổi lịch học");
      }

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(OrderStatus.CANCELLED, result.getStatus()),
          () -> assertEquals("Đổi lịch học", result.getCancellationReason()));

      // ===== VERIFY =====
      verify(orderRepository, times(1)).save(any(Order.class));
      verify(userRepository, times(1)).findById(STUDENT_ID);
    }
  }

  @Nested
  @DisplayName("getMyOrders()")
  class GetMyOrdersTests {

    /**
     * Normal case: Trả về page order và map đúng thông tin course/student.
     */
    @Test
    void it_should_return_order_page_with_course_mapping() {
      // ===== ARRANGE =====
      Pageable pageable = PageRequest.of(0, 10);
      Order order1 = buildOrder(OrderStatus.PENDING, new BigDecimal("100000"), Instant.now().plusSeconds(600));
      order1.setId(UUID.fromString("77777777-7777-7777-7777-777777777777"));
      Order order2 = buildOrder(OrderStatus.CANCELLED, new BigDecimal("200000"), Instant.now().plusSeconds(600));
      order2.setId(UUID.fromString("88888888-8888-8888-8888-888888888888"));
      order2.setCourseId(UUID.fromString("99999999-9999-9999-9999-999999999999"));

      Course course1 = buildCourse(CourseStatus.PUBLISHED, new BigDecimal("100000"), null, null);
      course1.setId(order1.getCourseId());
      course1.setTitle("Giải tích 2");
      Course course2 = buildCourse(CourseStatus.PUBLISHED, new BigDecimal("200000"), null, null);
      course2.setId(order2.getCourseId());
      course2.setTitle("Toán rời rạc");
      User student = buildUser(STUDENT_ID, "Pham Khanh Nhi", "nhi@student.fptu.edu.vn");

      when(orderRepository.findByStudentIdAndDeletedAtIsNullOrderByCreatedAtDesc(STUDENT_ID, pageable))
          .thenReturn(new PageImpl<>(List.of(order1, order2), pageable, 2));
      when(courseRepository.findAllById(anyCollection())).thenReturn(List.of(course1, course2));
      when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

      // ===== ACT =====
      Page<OrderResponse> result;
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        result = orderService.getMyOrders(pageable);
      }

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(2, result.getTotalElements()),
          () -> assertEquals("Giải tích 2", result.getContent().get(0).getCourseTitle()),
          () -> assertEquals("Toán rời rạc", result.getContent().get(1).getCourseTitle()));

      // ===== VERIFY =====
      verify(courseRepository, times(1)).findAllById(anyCollection());
      verify(userRepository, times(2)).findById(STUDENT_ID);
    }

    /**
     * Normal case: Trả về page rỗng và không query course khi không có order.
     */
    @Test
    void it_should_return_empty_page_without_course_lookup_when_no_orders() {
      // ===== ARRANGE =====
      Pageable pageable = PageRequest.of(0, 10);
      when(orderRepository.findByStudentIdAndDeletedAtIsNullOrderByCreatedAtDesc(STUDENT_ID, pageable))
          .thenReturn(Page.empty(pageable));

      // ===== ACT =====
      Page<OrderResponse> result;
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        result = orderService.getMyOrders(pageable);
      }

      // ===== ASSERT =====
      assertAll(() -> assertNotNull(result), () -> assertTrue(result.isEmpty()));

      // ===== VERIFY =====
      verify(courseRepository, never()).findAllById(anyCollection());
    }
  }

  @Nested
  @DisplayName("cancelExpiredOrders() and hasPendingOrder()")
  class HouseKeepingTests {

    /**
     * Normal case: Không làm gì khi không có expired order.
     */
    @Test
    void it_should_skip_bulk_update_when_no_expired_orders() {
      // ===== ARRANGE =====
      when(orderRepository.findExpiredOrders(eq(OrderStatus.PENDING), any(Instant.class)))
          .thenReturn(Collections.emptyList());

      // ===== ACT =====
      orderService.cancelExpiredOrders();

      // ===== ASSERT =====
      assertTrue(true);

      // ===== VERIFY =====
      verify(orderRepository, times(1)).findExpiredOrders(eq(OrderStatus.PENDING), any(Instant.class));
      verify(orderRepository, never()).bulkUpdateStatus(anyList(), any(), any(), anyString());
    }

    /**
     * Normal case: Bulk update status CANCELLED cho danh sách expired order.
     */
    @Test
    void it_should_bulk_cancel_expired_orders_when_expired_orders_exist() {
      // ===== ARRANGE =====
      Order expired1 = buildOrder(OrderStatus.PENDING, new BigDecimal("100000"), Instant.now().minusSeconds(60));
      Order expired2 = buildOrder(OrderStatus.PENDING, new BigDecimal("90000"), Instant.now().minusSeconds(30));
      expired2.setId(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"));
      when(orderRepository.findExpiredOrders(eq(OrderStatus.PENDING), any(Instant.class)))
          .thenReturn(List.of(expired1, expired2));

      // ===== ACT =====
      orderService.cancelExpiredOrders();

      // ===== ASSERT =====
      assertTrue(true);

      // ===== VERIFY =====
      verify(orderRepository, times(1)).bulkUpdateStatus(anyList(), eq(OrderStatus.CANCELLED), any(Instant.class), eq("Order expired after 15 minutes"));
    }

    /**
     * Normal case: hasPendingOrder trả về true khi repository có phần tử.
     */
    @Test
    void it_should_return_true_when_pending_order_exists() {
      // ===== ARRANGE =====
      when(orderRepository.findByStudentIdAndCourseIdAndStatusInAndDeletedAtIsNull(
              eq(STUDENT_ID), eq(COURSE_ID), anyList()))
          .thenReturn(List.of(buildOrder(OrderStatus.PENDING, new BigDecimal("100000"), Instant.now())));

      // ===== ACT =====
      boolean result = orderService.hasPendingOrder(STUDENT_ID, COURSE_ID);

      // ===== ASSERT =====
      assertTrue(result);

      // ===== VERIFY =====
      verify(orderRepository, times(1))
          .findByStudentIdAndCourseIdAndStatusInAndDeletedAtIsNull(eq(STUDENT_ID), eq(COURSE_ID), anyList());
    }

    /**
     * Normal case: hasPendingOrder trả về false khi repository không có phần tử.
     */
    @Test
    void it_should_return_false_when_pending_order_does_not_exist() {
      // ===== ARRANGE =====
      when(orderRepository.findByStudentIdAndCourseIdAndStatusInAndDeletedAtIsNull(
              eq(STUDENT_ID), eq(COURSE_ID), anyList()))
          .thenReturn(Collections.emptyList());

      // ===== ACT =====
      boolean result = orderService.hasPendingOrder(STUDENT_ID, COURSE_ID);

      // ===== ASSERT =====
      assertEquals(false, result);

      // ===== VERIFY =====
      verify(orderRepository, times(1))
          .findByStudentIdAndCourseIdAndStatusInAndDeletedAtIsNull(eq(STUDENT_ID), eq(COURSE_ID), anyList());
    }
  }

  private Course buildCourse(
      CourseStatus status, BigDecimal originalPrice, BigDecimal discountedPrice, Instant discountExpiry) {
    Course course = new Course();
    course.setId(COURSE_ID);
    course.setTitle("Giải tích nâng cao cho kỹ sư");
    course.setTeacherId(TEACHER_ID);
    course.setStatus(status);
    course.setPublished(status == CourseStatus.PUBLISHED);
    course.setOriginalPrice(originalPrice);
    course.setDiscountedPrice(discountedPrice);
    course.setDiscountExpiryDate(discountExpiry);
    course.setThumbnailUrl("https://cdn.mathmaster.vn/course/thumbnail.png");
    return course;
  }

  private Order buildOrder(OrderStatus status, BigDecimal finalPrice, Instant expiresAt) {
    Order order = new Order();
    order.setId(ORDER_ID);
    order.setOrderNumber("ORD-202604260101-1234");
    order.setStudentId(STUDENT_ID);
    order.setCourseId(COURSE_ID);
    order.setStatus(status);
    order.setOriginalPrice(finalPrice);
    order.setDiscountAmount(BigDecimal.ZERO);
    order.setFinalPrice(finalPrice);
    order.setInstructorEarnings(finalPrice.multiply(new BigDecimal("0.90")).setScale(2, RoundingMode.HALF_UP));
    order.setPlatformCommission(finalPrice.subtract(order.getInstructorEarnings()));
    order.setExpiresAt(expiresAt);
    order.setCreatedAt(Instant.now().minusSeconds(120));
    order.setUpdatedAt(Instant.now());
    return order;
  }

  private User buildUser(UUID userId, String fullName, String email) {
    User user = new User();
    user.setId(userId);
    user.setFullName(fullName);
    user.setEmail(email);
    return user;
  }

  private Wallet buildWallet(UUID walletId, UUID userId, BigDecimal balance) {
    Wallet wallet = new Wallet();
    wallet.setId(walletId);
    wallet.setUser(buildUser(userId, "Wallet Owner", "wallet.owner@fptu.edu.vn"));
    wallet.setBalance(balance);
    return wallet;
  }
}
