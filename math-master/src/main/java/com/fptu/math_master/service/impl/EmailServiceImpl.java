package com.fptu.math_master.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.fptu.math_master.service.EmailService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;

  @Override
  @Async
  public void sendEmail(String to, String subject, String templateName, Map<String, Object> variables) {
    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

      Context context = new Context();
      context.setVariables(variables);
      String htmlContent = templateEngine.process("email/" + templateName, context);

      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(htmlContent, true);
      helper.setFrom("MathMaster <noreply@mathmaster.vn>");

      mailSender.send(mimeMessage);
      log.info("Professional email sent successfully to: {}", to);
    } catch (MessagingException e) {
      log.error("Failed to send email to {}: {}", to, e.getMessage());
    }
  }

  @Override
  @Async
  public void sendDirectEmail(String to, String subject, String htmlBody) {
    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(htmlBody, true);
      helper.setFrom("MathMaster <noreply@mathmaster.vn>");
      mailSender.send(mimeMessage);
      log.info("Direct email sent successfully to: {}", to);
    } catch (MessagingException e) {
      log.error("Failed to send direct email to {}: {}", to, e.getMessage());
    }
  }

  @Override
  public void sendTeacherApprovalEmail(String to, String teacherName) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("name", teacherName);
    variables.put("dashboardUrl", "http://localhost:3000/teacher/dashboard");
    
    sendEmail(to, "Chúc mừng! Hồ sơ giảng viên của bạn đã được phê duyệt 🎉", "teacher-approved", variables);
  }

  @Override
  public void sendTeacherRejectionEmail(String to, String teacherName, String reason) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("name", teacherName);
    variables.put("reason", reason != null ? reason : "Hồ sơ chưa đạt yêu cầu xác minh.");
    variables.put("retryUrl", "http://localhost:3000/profile");

    sendEmail(to, "Thông báo về hồ sơ giảng viên của bạn - MathMaster", "teacher-rejected", variables);
  }

  @Override
  public void sendEmailConfirmation(String to, String userName, String confirmationUrl) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("userName", userName);
    variables.put("confirmationUrl", confirmationUrl);

    sendEmail(to, "Xác nhận tài khoản MathMaster của bạn", "email-confirmation", variables);
  }

  @Override
  public void sendPasswordResetEmail(String to, String userName, String resetUrl) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("userName", userName);
    variables.put("resetUrl", resetUrl);

    sendEmail(to, "Đặt lại mật khẩu MathMaster của bạn", "password-reset", variables);
  }

  // ─── Order & Refund Email Methods ────────────────────────────────────────

  @Override
  public void sendOrderConfirmationEmail(String to, String studentName, String courseTitle, 
      String orderNumber, String amount, String enrollmentUrl) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("studentName", studentName);
    variables.put("courseTitle", courseTitle);
    variables.put("orderNumber", orderNumber);
    variables.put("amount", amount);
    variables.put("enrollmentUrl", enrollmentUrl);
    variables.put("currentYear", java.time.Year.now().getValue());

    sendEmail(to, "Xác nhận đăng ký khóa học - " + courseTitle, "order-confirmation", variables);
  }

  @Override
  public void sendNewEnrollmentEmail(String to, String instructorName, String studentName, 
      String courseTitle, String courseUrl) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("instructorName", instructorName);
    variables.put("studentName", studentName);
    variables.put("courseTitle", courseTitle);
    variables.put("courseUrl", courseUrl);
    variables.put("currentYear", java.time.Year.now().getValue());

    sendEmail(to, "Học viên mới đăng ký khóa học - " + courseTitle, "new-enrollment", variables);
  }

  @Override
  public void sendRefundConfirmationEmail(String to, String studentName, String courseTitle, 
      String refundAmount, String reason) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("studentName", studentName);
    variables.put("courseTitle", courseTitle);
    variables.put("refundAmount", refundAmount);
    variables.put("reason", reason);
    variables.put("currentYear", java.time.Year.now().getValue());

    sendEmail(to, "Xác nhận hoàn tiền - " + courseTitle, "refund-confirmation", variables);
  }

  @Override
  public void sendRefundNotificationEmail(String to, String instructorName, String studentName, 
      String courseTitle, String deductionAmount) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("instructorName", instructorName);
    variables.put("studentName", studentName);
    variables.put("courseTitle", courseTitle);
    variables.put("deductionAmount", deductionAmount);
    variables.put("currentYear", java.time.Year.now().getValue());

    sendEmail(to, "Thông báo hoàn tiền - " + courseTitle, "refund-notification", variables);
  }
}
