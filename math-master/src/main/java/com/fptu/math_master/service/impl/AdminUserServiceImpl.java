package com.fptu.math_master.service.impl;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fptu.math_master.dto.response.AdminUserListResponse;
import com.fptu.math_master.dto.response.UserResponse;
import com.fptu.math_master.entity.Role;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.Status;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.AdminUserService;
import com.fptu.math_master.service.EmailService;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserServiceImpl implements AdminUserService {

  private static final String ALPHA_NUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$";
  private static final SecureRandom RANDOM = new SecureRandom();

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailService emailService;

  @Override
  @Transactional(readOnly = true)
  public AdminUserListResponse listUsers(String role, String search, String status,
      String sortBy, String sortOrder, Instant createdFrom, Instant createdTo, Pageable pageable) {

    // Build pageable with dynamic sorting if sortBy is provided
    Pageable effectivePageable = pageable;
    if (sortBy != null && !sortBy.isBlank()) {
      String field = mapSortField(sortBy);
      Sort.Direction dir = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
      effectivePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(dir, field));
    }

    Specification<User> spec = buildSpec(role, search, status, createdFrom, createdTo);
    Page<User> page = userRepository.findAll(spec, effectivePageable);

    List<UserResponse> users = page.getContent().stream()
        .map(this::toResponse)
        .collect(Collectors.toList());

    long total = userRepository.countNonDeleted();
    long admins = userRepository.countByRoleName("ADMIN");
    long teachers = userRepository.countByRoleName("TEACHER");
    long students = userRepository.countStudentOnly();
    long active = userRepository.countByStatus(Status.ACTIVE);

    return AdminUserListResponse.builder()
        .users(users)
        .stats(AdminUserListResponse.Stats.builder()
            .total(total)
            .admins(admins)
            .teachers(teachers)
            .students(students)
            .active(active)
            .build())
        .pagination(AdminUserListResponse.Pagination.builder()
            .page(effectivePageable.getPageNumber())
            .pageSize(effectivePageable.getPageSize())
            .totalItems(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .build())
        .build();
  }

  @Override
  @Transactional
  public String resetPassword(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    String tempPassword = generateTemporaryPassword(12);
    user.setPassword(passwordEncoder.encode(tempPassword));
    userRepository.save(user);

    String emailBody = "<p>Xin chào <b>" + (user.getFullName() != null ? user.getFullName() : user.getUserName()) + "</b>,</p>"
        + "<p>Mật khẩu của bạn đã được đặt lại bởi quản trị viên.</p>"
        + "<p>Mật khẩu tạm thời: <b>" + tempPassword + "</b></p>"
        + "<p>Vui lòng đổi mật khẩu sau khi đăng nhập.</p>"
        + "<p>Trân trọng,<br/>MathMaster Team</p>";

    emailService.sendDirectEmail(user.getEmail(), "Mật khẩu của bạn đã được đặt lại - MathMaster", emailBody);
    log.info("Password reset and email sent for userId={}", userId);

    return tempPassword;
  }

  @Override
  public void sendEmail(UUID userId, String subject, String body) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    String htmlBody = "<div style=\"font-family:sans-serif;\">" + body.replace("\n", "<br/>") + "</div>";
    emailService.sendDirectEmail(user.getEmail(), subject, htmlBody);
    log.info("Admin sent email to userId={}, subject={}", userId, subject);
  }

  @Override
  @Transactional(readOnly = true)
  public void exportUsersToExcel(String role, String search, String status, HttpServletResponse response) {
    Specification<User> spec = buildSpec(role, search, status, null, null);
    List<User> users = userRepository.findAll(spec);

    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setHeader("Content-Disposition", "attachment; filename=\"users.xlsx\"");

    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Users");

      // Header row
      Row header = sheet.createRow(0);
      CellStyle headerStyle = workbook.createCellStyle();
      Font font = workbook.createFont();
      font.setBold(true);
      headerStyle.setFont(font);
      headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
      headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      headerStyle.setBorderBottom(BorderStyle.THIN);

      String[] columns = {"ID", "Username", "Full Name", "Email", "Role", "Status", "Join Date", "Last Login"};
      for (int i = 0; i < columns.length; i++) {
        Cell cell = header.createCell(i);
        cell.setCellValue(columns[i]);
        cell.setCellStyle(headerStyle);
        sheet.setColumnWidth(i, 5000);
      }

      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
      int rowNum = 1;
      for (User user : users) {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(user.getId() != null ? user.getId().toString() : "");
        row.createCell(1).setCellValue(user.getUserName() != null ? user.getUserName() : "");
        row.createCell(2).setCellValue(user.getFullName() != null ? user.getFullName() : "");
        row.createCell(3).setCellValue(user.getEmail() != null ? user.getEmail() : "");
        row.createCell(4).setCellValue(user.getRoles() != null
            ? user.getRoles().stream().map(Role::getName).collect(Collectors.joining(", ")) : "");
        row.createCell(5).setCellValue(user.getStatus() != null ? user.getStatus().name() : "");
        row.createCell(6).setCellValue(user.getCreatedAt() != null ? formatter.format(user.getCreatedAt()) : "");
        row.createCell(7).setCellValue(user.getLastLogin() != null ? formatter.format(user.getLastLogin()) : "");
      }

      workbook.write(response.getOutputStream());
      response.getOutputStream().flush();
    } catch (IOException e) {
      log.error("Failed to export users to Excel: {}", e.getMessage());
      throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }
  }

  private Specification<User> buildSpec(String role, String search, String status, Instant createdFrom, Instant createdTo) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      // Exclude soft-deleted users (unless status filter explicitly requests DELETED)
      boolean filteringDeleted = status != null && "DELETED".equalsIgnoreCase(status);
      if (!filteringDeleted) {
        predicates.add(cb.notEqual(root.get("status"), Status.DELETED));
      }

      if (search != null && !search.isBlank()) {
        String kw = "%" + search.toLowerCase() + "%";
        predicates.add(cb.or(
            cb.like(cb.lower(root.get("fullName")), kw),
            cb.like(cb.lower(root.get("email")), kw),
            cb.like(cb.lower(root.get("userName")), kw)));
      }

      if (role != null && !role.isBlank() && !"all".equalsIgnoreCase(role)) {
        if ("STUDENT_ONLY".equalsIgnoreCase(role)) {
          // Has STUDENT role AND does NOT have TEACHER role
          Join<Object, Object> studentJoin = root.join("roles", JoinType.INNER);
          predicates.add(cb.equal(cb.upper(studentJoin.get("name")), "STUDENT"));
          // Subquery: exclude users who also have TEACHER role
          jakarta.persistence.criteria.Subquery<Long> teacherSub = query.subquery(Long.class);
          jakarta.persistence.criteria.Root<User> subRoot = teacherSub.from(User.class);
          Join<Object, Object> subRoleJoin = subRoot.join("roles", JoinType.INNER);
          teacherSub.select(cb.literal(1L))
              .where(
                  cb.equal(subRoot.get("id"), root.get("id")),
                  cb.equal(cb.upper(subRoleJoin.get("name")), "TEACHER"));
          predicates.add(cb.not(cb.exists(teacherSub)));
          if (query.getResultType() != Long.class) {
            query.distinct(true);
          }
        } else {
          Join<Object, Object> roles = root.join("roles", JoinType.LEFT);
          predicates.add(cb.equal(cb.upper(roles.get("name")), role.toUpperCase()));
        }
      }

      if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
        try {
          Status statusEnum = Status.valueOf(status.toUpperCase());
          predicates.add(cb.equal(root.get("status"), statusEnum));
        } catch (IllegalArgumentException e) {
          // ignore unknown status value
        }
      }

      if (createdFrom != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
      }
      if (createdTo != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdTo));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  private String mapSortField(String sortBy) {
    return switch (sortBy.toLowerCase()) {
      case "fullname", "name" -> "fullName";
      case "email" -> "email";
      case "createdate", "joindate", "createdat" -> "createdAt";
      case "lastlogin" -> "lastLogin";
      case "status" -> "status";
      case "username" -> "userName";
      default -> "createdAt";
    };
  }

  private String generateTemporaryPassword(int length) {
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append(ALPHA_NUM.charAt(RANDOM.nextInt(ALPHA_NUM.length())));
    }
    return sb.toString();
  }

  private UserResponse toResponse(User user) {
    return UserResponse.builder()
        .id(user.getId())
        .userName(user.getUserName())
        .fullName(user.getFullName())
        .email(user.getEmail())
        .avatar(user.getAvatar())
        .status(user.getStatus())
        .lastLogin(user.getLastLogin())
        .roles(user.getRoles() != null
            ? user.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet())
            : null)
        .createdDate(user.getCreatedAt())
        .build();
  }
}
