package com.fptu.math_master.service;

import java.util.Map;

public interface EmailService {
  void sendEmail(String to, String subject, String templateName, Map<String, Object> variables);

  void sendDirectEmail(String to, String subject, String htmlBody);

  void sendTeacherApprovalEmail(String to, String teacherName);

  void sendTeacherRejectionEmail(String to, String teacherName, String reason);

  void sendEmailConfirmation(String to, String userName, String confirmationUrl);
}
