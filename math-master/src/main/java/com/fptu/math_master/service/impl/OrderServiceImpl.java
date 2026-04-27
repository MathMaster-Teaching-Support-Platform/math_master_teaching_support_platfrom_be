package com.fptu.math_master.service.impl;

import com.fptu.math_master.component.StreamPublisher;
import com.fptu.math_master.dto.request.NotificationRequest;
import com.fptu.math_master.dto.response.OrderResponse;
import com.fptu.math_master.entity.*;
import com.fptu.math_master.enums.EnrollmentStatus;
import com.fptu.math_master.enums.OrderStatus;
import com.fptu.math_master.enums.TransactionStatus;
import com.fptu.math_master.enums.TransactionType;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.*;
import com.fptu.math_master.service.OrderService;
import com.fptu.math_master.service.WalletService;
import com.fptu.math_master.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OrderServiceImpl implements OrderService {

    OrderRepository orderRepository;
    CourseRepository courseRepository;
    EnrollmentRepository enrollmentRepository;
    WalletRepository walletRepository;
    TransactionRepository transactionRepository;
    UserRepository userRepository;
    WalletService walletService;
    StreamPublisher streamPublisher;
    com.fptu.math_master.service.EmailService emailService;

    private static final Duration ORDER_EXPIRY_DURATION = Duration.ofMinutes(15);
    private static final BigDecimal INSTRUCTOR_SHARE = new BigDecimal("0.90");
    private static final BigDecimal PLATFORM_SHARE = new BigDecimal("0.10");

    @Override
    @Transactional
    public OrderResponse createOrder(UUID courseId) {
        UUID studentId = SecurityUtils.getCurrentUserId();

        // 1. Validate course
        Course course = courseRepository
                .findByIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

        if (!course.isPublished()) {
            throw new AppException(ErrorCode.COURSE_NOT_PUBLISHED);
        }

        // 2. Check if already enrolled
        Optional<Enrollment> existingEnrollment = enrollmentRepository
                .findByStudentIdAndCourseIdAndDeletedAtIsNull(studentId, courseId);

        if (existingEnrollment.isPresent() && existingEnrollment.get().getStatus() == EnrollmentStatus.ACTIVE) {
            throw new AppException(ErrorCode.ALREADY_ENROLLED);
        }

        // 3. Check for pending orders
        List<Order> pendingOrders = orderRepository.findByStudentIdAndCourseIdAndStatusInAndDeletedAtIsNull(
                studentId, courseId, Arrays.asList(OrderStatus.PENDING, OrderStatus.PROCESSING));

        if (!pendingOrders.isEmpty()) {
            // Return existing pending order
            Order existingOrder = pendingOrders.get(0);
            log.info("Returning existing pending order {} for student {} and course {}",
                    existingOrder.getOrderNumber(), studentId, courseId);
            return mapToResponse(existingOrder, course);
        }

        // 4. Calculate pricing
        BigDecimal originalPrice = course.getOriginalPrice() != null ? course.getOriginalPrice() : BigDecimal.ZERO;
        BigDecimal finalPrice = calculateFinalPrice(course);
        BigDecimal discountAmount = originalPrice.subtract(finalPrice);

        // 5. Calculate split
        BigDecimal instructorEarnings = finalPrice.multiply(INSTRUCTOR_SHARE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal platformCommission = finalPrice.subtract(instructorEarnings);

        // 6. Generate order number
        String orderNumber = generateOrderNumber();

        // 7. Create order
        Order order = Order.builder()
                .studentId(studentId)
                .courseId(courseId)
                .status(OrderStatus.PENDING)
                .orderNumber(orderNumber)
                .originalPrice(originalPrice)
                .discountAmount(discountAmount)
                .finalPrice(finalPrice)
                .instructorEarnings(instructorEarnings)
                .platformCommission(platformCommission)
                .expiresAt(Instant.now().plus(ORDER_EXPIRY_DURATION))
                .build();

        order = orderRepository.save(order);

        log.info("Created order {} for student {} and course {} with amount {}",
                orderNumber, studentId, courseId, finalPrice);

        return mapToResponse(order, course);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        UUID studentId = SecurityUtils.getCurrentUserId();

        Order order = orderRepository
                .findByIdAndStudentId(orderId, studentId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        Course course = courseRepository
                .findByIdAndDeletedAtIsNull(order.getCourseId())
                .orElse(null);

        return mapToResponse(order, course);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        UUID studentId = SecurityUtils.getCurrentUserId();

        Order order = orderRepository
                .findByOrderNumber(orderNumber)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getStudentId().equals(studentId)) {
            throw new AppException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        Course course = courseRepository
                .findByIdAndDeletedAtIsNull(order.getCourseId())
                .orElse(null);

        return mapToResponse(order, course);
    }

    @Override
    @Transactional
    public OrderResponse confirmOrder(UUID orderId) {
        UUID studentId = SecurityUtils.getCurrentUserId();

        // 1. Get and validate order with pessimistic lock to prevent double payment
        Order order = orderRepository
                .findByIdAndStudentIdWithLock(orderId, studentId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        validateOrderForConfirmation(order);

        // 1.5. Check if already enrolled (to prevent double checkout of multiple
        // pending orders)
        Optional<Enrollment> existingEnrollment = enrollmentRepository
                .findByStudentIdAndCourseIdAndDeletedAtIsNull(studentId, order.getCourseId());
        if (existingEnrollment.isPresent() && existingEnrollment.get().getStatus() == EnrollmentStatus.ACTIVE) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancellationReason("User is already enrolled in this course");
            orderRepository.save(order);
            throw new AppException(ErrorCode.ALREADY_ENROLLED);
        }

        // 2. Get course
        Course course = courseRepository
                .findByIdAndDeletedAtIsNull(order.getCourseId())
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

        if (!course.isPublished()) {
            throw new AppException(ErrorCode.COURSE_NOT_PUBLISHED);
        }

        // 3. Update order status to PROCESSING
        order.setStatus(OrderStatus.PROCESSING);
        orderRepository.saveAndFlush(order);

        try {
            // 4. Process payment for paid courses
            if (order.getFinalPrice().compareTo(BigDecimal.ZERO) > 0) {
                processPayment(order, course);
            }

            // 5. Create enrollment
            Enrollment enrollment = createEnrollment(order);
            order.setEnrollmentId(enrollment.getId());

            // 6. Update order status to COMPLETED
            order.setStatus(OrderStatus.COMPLETED);
            order.setConfirmedAt(Instant.now());
            orderRepository.save(order);

            // 7. Send notifications
            sendOrderConfirmationNotifications(order, course);

            log.info("Order {} confirmed successfully for student {}", order.getOrderNumber(), studentId);

            return mapToResponse(order, course);

        } catch (Exception e) {
            // Rollback handled by @Transactional
            // Only update to FAILED if it wasn't already successfully processed
            if (order.getStatus() != OrderStatus.COMPLETED) {
                order.setStatus(OrderStatus.FAILED);
                order.setCancellationReason("Payment failed: " + e.getMessage());
                orderRepository.save(order);
            }

            log.error("Failed to confirm order {}: {}", order.getOrderNumber(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(UUID orderId, String reason) {
        UUID studentId = SecurityUtils.getCurrentUserId();

        Order order = orderRepository
                .findByIdAndStudentId(orderId, studentId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new AppException(ErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        order.setCancellationReason(reason != null ? reason : "Cancelled by user");
        orderRepository.save(order);

        log.info("Order {} cancelled by student {}", order.getOrderNumber(), studentId);

        Course course = courseRepository.findByIdAndDeletedAtIsNull(order.getCourseId()).orElse(null);
        return mapToResponse(order, course);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(Pageable pageable) {
        UUID studentId = SecurityUtils.getCurrentUserId();

        Page<Order> orders = orderRepository.findByStudentIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                studentId, pageable);

        Set<UUID> courseIds = new HashSet<>();
        orders.forEach(o -> courseIds.add(o.getCourseId()));

        Map<UUID, Course> courseMap = new HashMap<>();
        if (!courseIds.isEmpty()) {
            courseRepository.findAllById(courseIds).forEach(c -> courseMap.put(c.getId(), c));
        }

        return orders.map(order -> mapToResponse(order, courseMap.get(order.getCourseId())));
    }

    @Override
    @Transactional
    public void cancelExpiredOrders() {
        List<Order> expiredOrders = orderRepository.findExpiredOrders(OrderStatus.PENDING, Instant.now());

        if (expiredOrders.isEmpty()) {
            log.debug("No expired orders to cancel");
            return;
        }

        List<UUID> orderIds = expiredOrders.stream().map(Order::getId).toList();

        orderRepository.bulkUpdateStatus(
                orderIds,
                OrderStatus.CANCELLED,
                Instant.now(),
                "Order expired after 15 minutes");

        log.info("Cancelled {} expired orders", expiredOrders.size());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPendingOrder(UUID studentId, UUID courseId) {
        List<Order> pendingOrders = orderRepository.findByStudentIdAndCourseIdAndStatusInAndDeletedAtIsNull(
                studentId, courseId, Arrays.asList(OrderStatus.PENDING, OrderStatus.PROCESSING));
        return !pendingOrders.isEmpty();
    }

    // ─── Private Helper Methods ──────────────────────────────────────────────

    private BigDecimal calculateFinalPrice(Course course) {
        BigDecimal originalPrice = course.getOriginalPrice() != null ? course.getOriginalPrice() : BigDecimal.ZERO;

        if (course.getDiscountedPrice() != null) {
            if (course.getDiscountExpiryDate() == null || course.getDiscountExpiryDate().isAfter(Instant.now())) {
                return course.getDiscountedPrice();
            }
        }

        return originalPrice;
    }

    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return "ORD-" + timestamp + "-" + random;
    }

    private void validateOrderForConfirmation(Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new AppException(ErrorCode.ORDER_ALREADY_PROCESSED);
        }

        if (order.getExpiresAt() != null && order.getExpiresAt().isBefore(Instant.now())) {
            throw new AppException(ErrorCode.ORDER_EXPIRED);
        }
    }

    private void processPayment(Order order, Course course) {
        UUID studentId = order.getStudentId();

        // 1. Get or create student wallet
        Wallet studentWallet = walletRepository.findByUserId(studentId)
                .orElseGet(() -> {
                    walletService.createWallet(studentId);
                    return walletRepository.findByUserId(studentId)
                            .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
                });

        // 2. Check balance
        if (studentWallet.getBalance().compareTo(order.getFinalPrice()) < 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        // 3. Create PENDING student transaction
        Transaction studentTx = Transaction.builder()
                .wallet(studentWallet)
                .order(order)
                .orderNumber(order.getOrderNumber())
                .amount(order.getFinalPrice())
                .type(TransactionType.COURSE_PURCHASE)
                .status(TransactionStatus.PENDING)
                .description("Purchase Course: " + course.getTitle())
                .transactionDate(Instant.now())
                .orderCode(System.currentTimeMillis())
                .instructorEarnings(order.getInstructorEarnings())
                .platformCommission(order.getPlatformCommission())
                .build();
        studentTx = transactionRepository.save(studentTx);

        // 4. Deduct from student wallet
        walletService.deductBalance(studentWallet.getId(), order.getFinalPrice());

        // 5. Update student transaction to SUCCESS
        studentTx.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(studentTx);

        // 6. Get or create instructor wallet
        Wallet instructorWallet = walletRepository.findByUserId(course.getTeacherId())
                .orElseGet(() -> {
                    walletService.createWallet(course.getTeacherId());
                    return walletRepository.findByUserId(course.getTeacherId())
                            .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
                });

        // 7. Create PENDING instructor transaction
        Transaction instructorTx = Transaction.builder()
                .wallet(instructorWallet)
                .order(order)
                .orderNumber(order.getOrderNumber())
                .amount(order.getInstructorEarnings())
                .type(TransactionType.INSTRUCTOR_REVENUE)
                .status(TransactionStatus.PENDING)
                .description("Revenue from Course: " + course.getTitle())
                .transactionDate(Instant.now())
                .orderCode(System.currentTimeMillis() + 1)
                .build();
        instructorTx = transactionRepository.save(instructorTx);

        // 8. Add to instructor wallet
        walletService.addBalance(instructorWallet.getId(), order.getInstructorEarnings());

        // 9. Update instructor transaction to SUCCESS
        instructorTx.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(instructorTx);

        if (order.getPlatformCommission().compareTo(BigDecimal.ZERO) > 0) {
            // 10. Get Admin wallet (Try to find 'admin@mathmaster.vn' first, otherwise
            // fallback to first admin)
            UUID adminId = userRepository.findByEmail("admin@mathmaster.com")
                    .map(u -> u.getId())
                    .orElseGet(() -> {
                        List<UUID> adminIds = userRepository.findUserIdsByRoleName("ADMIN");
                        if (adminIds.isEmpty()) {
                            throw new RuntimeException("System missing ADMIN user for commission distribution");
                        }
                        return adminIds.get(0);
                    });

            Wallet adminWallet = walletRepository.findByUserId(adminId)
                    .orElseGet(() -> {
                        walletService.createWallet(adminId);
                        return walletRepository.findByUserId(adminId)
                                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
                    });

            // 11. Create PENDING platform commission transaction
            Transaction adminTx = Transaction.builder()
                    .wallet(adminWallet)
                    .order(order)
                    .orderNumber(order.getOrderNumber())
                    .amount(order.getPlatformCommission())
                    .type(TransactionType.PLATFORM_COMMISSION)
                    .status(TransactionStatus.PENDING)
                    .description("Platform Commission from Course: " + course.getTitle())
                    .transactionDate(Instant.now())
                    .orderCode(System.currentTimeMillis() + 2)
                    .build();
            adminTx = transactionRepository.save(adminTx);

            // 12. Add to admin wallet
            walletService.addBalance(adminWallet.getId(), order.getPlatformCommission());

            // 13. Update admin transaction to SUCCESS
            adminTx.setStatus(TransactionStatus.SUCCESS);
            transactionRepository.save(adminTx);
        }

        log.info("Payment processed for order {}: Student paid {}, Instructor earned {}, Platform commission {}",
                order.getOrderNumber(), order.getFinalPrice(), order.getInstructorEarnings(),
                order.getPlatformCommission());
    }

    private Enrollment createEnrollment(Order order) {
        Enrollment enrollment = Enrollment.builder()
                .courseId(order.getCourseId())
                .studentId(order.getStudentId())
                .status(EnrollmentStatus.ACTIVE)
                .enrolledAt(Instant.now())
                .build();

        enrollment = enrollmentRepository.save(enrollment);

        log.info("Created enrollment {} for order {}", enrollment.getId(), order.getOrderNumber());

        return enrollment;
    }

    private void sendOrderConfirmationNotifications(Order order, Course course) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderId", order.getId().toString());
        metadata.put("orderNumber", order.getOrderNumber());
        metadata.put("courseId", course.getId().toString());
        metadata.put("amount", order.getFinalPrice().toString());
        metadata.put("event", "ORDER_COMPLETED");

        // Get user details
        User student = userRepository.findById(order.getStudentId()).orElse(null);
        User instructor = userRepository.findById(course.getTeacherId()).orElse(null);

        // Student notification
        try {
            NotificationRequest studentNotification = NotificationRequest.builder()
                    .id(UUID.randomUUID().toString())
                    .type("COURSE")
                    .title("Đăng ký khóa học thành công")
                    .content("Bạn đã đăng ký thành công khóa học '" + course.getTitle() + "'. Mã đơn hàng: "
                            + order.getOrderNumber())
                    .recipientId(order.getStudentId().toString())
                    .senderId("SYSTEM")
                    .timestamp(LocalDateTime.now())
                    .metadata(metadata)
                    .actionUrl("/student/courses/" + order.getEnrollmentId())
                    .build();
            streamPublisher.publish(studentNotification);
        } catch (Exception e) {
            log.error("Failed to send order confirmation notification to student {}", order.getStudentId(), e);
        }

        // Student email
        if (student != null && student.getEmail() != null) {
            try {
                String enrollmentUrl = "http://localhost:3000/student/courses/" + order.getEnrollmentId();
                String formattedAmount = String.format("%,.0f VND", order.getFinalPrice());
                emailService.sendOrderConfirmationEmail(
                        student.getEmail(),
                        student.getFullName(),
                        course.getTitle(),
                        order.getOrderNumber(),
                        formattedAmount,
                        enrollmentUrl);
                log.info("Order confirmation email sent to student {}", student.getEmail());
            } catch (Exception e) {
                log.error("Failed to send order confirmation email to student {}", student.getEmail(), e);
            }
        }

        // Teacher notification
        try {
            NotificationRequest teacherNotification = NotificationRequest.builder()
                    .id(UUID.randomUUID().toString())
                    .type("COURSE")
                    .title("Học viên mới đăng ký")
                    .content("Khóa học '" + course.getTitle() + "' vừa có một học viên mới đăng ký.")
                    .recipientId(course.getTeacherId().toString())
                    .senderId("SYSTEM")
                    .timestamp(LocalDateTime.now())
                    .metadata(metadata)
                    .actionUrl("/teacher/courses/" + course.getId())
                    .build();
            streamPublisher.publish(teacherNotification);
        } catch (Exception e) {
            log.error("Failed to send order confirmation notification to teacher {}", course.getTeacherId(), e);
        }

        // Teacher email
        if (instructor != null && instructor.getEmail() != null && student != null) {
            try {
                String courseUrl = "http://localhost:3000/teacher/courses/" + course.getId();
                emailService.sendNewEnrollmentEmail(
                        instructor.getEmail(),
                        instructor.getFullName(),
                        student.getFullName(),
                        course.getTitle(),
                        courseUrl);
                log.info("New enrollment email sent to instructor {}", instructor.getEmail());
            } catch (Exception e) {
                log.error("Failed to send new enrollment email to instructor {}", instructor.getEmail(), e);
            }
        }
    }

    private OrderResponse mapToResponse(Order order, Course course) {
        String studentName = userRepository.findById(order.getStudentId())
                .map(User::getFullName)
                .orElse(null);

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .studentId(order.getStudentId())
                .studentName(studentName)
                .courseId(order.getCourseId())
                .courseTitle(course != null ? course.getTitle() : null)
                .courseThumbnailUrl(course != null ? course.getThumbnailUrl() : null)
                .enrollmentId(order.getEnrollmentId())
                .status(order.getStatus())
                .originalPrice(order.getOriginalPrice())
                .discountAmount(order.getDiscountAmount())
                .finalPrice(order.getFinalPrice())
                .instructorEarnings(order.getInstructorEarnings())
                .platformCommission(order.getPlatformCommission())
                .expiresAt(order.getExpiresAt())
                .confirmedAt(order.getConfirmedAt())
                .cancelledAt(order.getCancelledAt())
                .cancellationReason(order.getCancellationReason())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}