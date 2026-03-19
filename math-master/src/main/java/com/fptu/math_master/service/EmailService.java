package com.fptu.math_master.service;

import java.util.Map;

public interface EmailService {
  void sendEmail(String to, String subject, String templateName, Map<String, Object> variables);
  
  void sendTeacherApprovalEmail(String to, String teacherName);
  
  void sendTeacherRejectionEmail(String to, String teacherName, String reason);
}
