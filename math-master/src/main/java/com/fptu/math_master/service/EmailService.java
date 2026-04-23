package com.fptu.math_master.service;

import java.util.Map;

public interface EmailService {
  void sendEmail(String to, String subject, String templateName, Map<String, Object> variables);

  void sendDirectEmail(String to, String subject, String htmlBody);

  void sendTeacherApprovalEmail(String to, String teacherName);

  void sendTeacherRejectionEmail(String to, String teacherName, String reason);

  void sendEmailConfirmation(String to, String userName, String confirmationUrl);

  void sendPasswordResetEmail(String to, String userName, String resetUrl);

  // ─── Order & Refund Email Methods ────────────────────────────────────────

  void sendOrderConfirmationEmail(String to, String studentName, String courseTitle, 
      String orderNumber, String amount, String enrollmentUrl);

  void sendNewEnrollmentEmail(String to, String instructorName, String studentName, 
      String courseTitle, String courseUrl);

  void sendRefundConfirmationEmail(String to, String studentName, String courseTitle, 
      String refundAmount, String reason);

  void sendRefundNotificationEmail(String to, String instructorName, String studentName, 
      String courseTitle, String deductionAmount);
}
