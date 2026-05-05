package com.fptu.math_master.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
/**
 * The exception to 'AppException'.
 */
public enum ErrorCode {
  UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
  INVALID_REQUEST(1000, "Invalid request", HttpStatus.BAD_REQUEST),
  INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
  USER_EXISTED(1002, "User existed", HttpStatus.BAD_REQUEST),
  USERNAME_INVALID(1003, "Username must be at least {min} characters", HttpStatus.BAD_REQUEST),
  INVALID_PASSWORD(1004, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),
  USER_NOT_EXISTED(1005, "User not existed", HttpStatus.NOT_FOUND),
  UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
  UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
  INVALID_DOB(1008, "Your age must be at least {min}", HttpStatus.BAD_REQUEST),
  ROLE_NOT_EXISTED(1009, "Role not existed", HttpStatus.NOT_FOUND),
  ROLE_ALREADY_EXISTS(1010, "Role already exists", HttpStatus.BAD_REQUEST),
  PERMISSION_NOT_EXISTED(1011, "Permission not existed", HttpStatus.NOT_FOUND),
  PERMISSION_ALREADY_EXISTS(1012, "Permission already exists", HttpStatus.BAD_REQUEST),
  EMAIL_ALREADY_EXISTS(1013, "Email already exists", HttpStatus.BAD_REQUEST),
  INCORRECT_PASSWORD(1014, "Current password is incorrect", HttpStatus.BAD_REQUEST),
  PASSWORD_MISMATCH(1015, "New password and confirm password do not match", HttpStatus.BAD_REQUEST),
  USER_ALREADY_BANNED(1016, "User is already banned", HttpStatus.BAD_REQUEST),
  USER_NOT_BANNED(1017, "User is not banned", HttpStatus.BAD_REQUEST),
  USER_ALREADY_DISABLED(1018, "User is already disabled", HttpStatus.BAD_REQUEST),
  USER_ALREADY_ENABLED(1019, "User is already enabled", HttpStatus.BAD_REQUEST),
  PROFILE_ALREADY_EXISTS(1020, "Teacher profile already exists", HttpStatus.BAD_REQUEST),
  PROFILE_NOT_FOUND(1021, "Teacher profile not found", HttpStatus.NOT_FOUND),
  PROFILE_ALREADY_APPROVED(1022, "Profile is already approved", HttpStatus.BAD_REQUEST),
  PROFILE_CANNOT_BE_MODIFIED(1023, "Approved profile cannot be modified", HttpStatus.BAD_REQUEST),
  INVALID_PROFILE_STATUS(1024, "Invalid profile status for this operation", HttpStatus.BAD_REQUEST),
  SCHOOL_NOT_FOUND(1025, "School not found", HttpStatus.NOT_FOUND),
  SCHOOL_ALREADY_EXISTS(1026, "School already exists", HttpStatus.BAD_REQUEST),
  WALLET_NOT_FOUND(1027, "Wallet not found", HttpStatus.NOT_FOUND),
  WALLET_ALREADY_EXISTS(1028, "Wallet already exists", HttpStatus.BAD_REQUEST),
  INSUFFICIENT_BALANCE(1029, "Insufficient balance", HttpStatus.BAD_REQUEST),
  TRANSACTION_NOT_FOUND(1030, "Transaction not found", HttpStatus.NOT_FOUND),
  PAYMENT_CREATION_FAILED(1031, "Payment creation failed", HttpStatus.INTERNAL_SERVER_ERROR),
  INVALID_WEBHOOK_SIGNATURE(1032, "Invalid webhook signature", HttpStatus.BAD_REQUEST),
  PAYMENT_ALREADY_PROCESSED(1033, "Payment already processed", HttpStatus.BAD_REQUEST),
  INVALID_AMOUNT(1034, "Invalid amount", HttpStatus.BAD_REQUEST),
  QUESTION_BANK_NOT_FOUND(1035, "Question bank not found", HttpStatus.NOT_FOUND),
  QUESTION_BANK_ACCESS_DENIED(
      1036, "You do not have permission to access this question bank", HttpStatus.FORBIDDEN),
  QUESTION_BANK_HAS_QUESTIONS_IN_USE(
      1037, "Question bank has questions being used in assessments", HttpStatus.BAD_REQUEST),
  QUESTION_BANK_REQUIRED(
      1038, "Question bank is required for exam matrix", HttpStatus.BAD_REQUEST),
  INVALID_SUBJECT(1039, "Invalid subject", HttpStatus.BAD_REQUEST),
  NOT_A_TEACHER(1040, "Only teachers can create question banks", HttpStatus.FORBIDDEN),
  ASSESSMENT_NOT_FOUND(1041, "Assessment not found", HttpStatus.NOT_FOUND),
  ASSESSMENT_ACCESS_DENIED(
      1042, "You do not have permission to access this assessment", HttpStatus.FORBIDDEN),
  ASSESSMENT_ALREADY_PUBLISHED(
      1043, "Assessment is already published and cannot be edited", HttpStatus.BAD_REQUEST),
  ASSESSMENT_HAS_SUBMISSIONS(
      1043,
      "Assessment has submissions and cannot be deleted or unpublished",
      HttpStatus.BAD_REQUEST),
  ASSESSMENT_NO_QUESTIONS(
      1044, "Assessment must have at least one question to publish", HttpStatus.BAD_REQUEST),
  ASSESSMENT_ZERO_TOTAL_POINTS(
      1044, "Total points across all questions must be greater than 0", HttpStatus.BAD_REQUEST),
  ASSESSMENT_IS_CLOSED(
      1093, "Assessment is closed and cannot be modified or unpublished", HttpStatus.BAD_REQUEST),
  MATRIX_CELL_FILL_INCOMPLETE(
      1094,
      "Not all matrix cells have their required question count filled in assessment_questions",
      HttpStatus.BAD_REQUEST),
  ASSESSMENT_INVALID_SCHEDULE(1045, "Start date must be before end date", HttpStatus.BAD_REQUEST),
  ASSESSMENT_START_DATE_PAST(1046, "Start date cannot be in the past", HttpStatus.BAD_REQUEST),
  ASSESSMENT_NOT_PUBLISHED(1047, "Assessment is not published", HttpStatus.BAD_REQUEST),
  ASSESSMENT_QUESTION_NOT_FOUND(1048, "Assessment question not found", HttpStatus.NOT_FOUND),
  EXAM_MATRIX_NOT_FOUND(1049, "Exam matrix not found", HttpStatus.NOT_FOUND),
  EXAM_MATRIX_ALREADY_EXISTS(1050, "Assessment already has an exam matrix", HttpStatus.BAD_REQUEST),
  ASSESSMENT_MUST_HAVE_LESSON(
      1051, "Assessment must be linked to a lesson to create matrix", HttpStatus.BAD_REQUEST),
  LESSON_HAS_NO_CHAPTERS(1052, "Lesson has no chapters", HttpStatus.BAD_REQUEST),
  EXAM_MATRIX_LOCKED(1053, "Exam matrix is locked and cannot be modified", HttpStatus.BAD_REQUEST),
  MATRIX_CELL_NOT_FOUND(1054, "Matrix cell not found", HttpStatus.NOT_FOUND),
  MATRIX_VALIDATION_FAILED(1055, "Matrix validation failed", HttpStatus.BAD_REQUEST),
  MATRIX_NOT_APPROVED(
      1056, "Matrix must be approved before assessment can be published", HttpStatus.BAD_REQUEST),
  INSUFFICIENT_QUESTIONS_AVAILABLE(
      1057, "Not enough questions available matching the criteria", HttpStatus.BAD_REQUEST),

  ASSESSMENT_NOT_AVAILABLE(
      1058, "Assessment is not available at this time", HttpStatus.BAD_REQUEST),
  ASSESSMENT_EXPIRED(1059, "Assessment has expired", HttpStatus.BAD_REQUEST),
  MAX_ATTEMPTS_REACHED(1060, "Maximum number of attempts reached", HttpStatus.BAD_REQUEST),
  QUIZ_ATTEMPT_NOT_FOUND(1061, "Quiz attempt not found", HttpStatus.NOT_FOUND),
  ATTEMPT_NOT_IN_PROGRESS(1062, "Attempt is not in progress", HttpStatus.BAD_REQUEST),
  ATTEMPT_ALREADY_SUBMITTED(1063, "Attempt has already been submitted", HttpStatus.BAD_REQUEST),
  ATTEMPT_ACCESS_DENIED(
      1064, "You do not have permission to access this attempt", HttpStatus.FORBIDDEN),
  TIME_LIMIT_EXCEEDED(1065, "Time limit has been exceeded", HttpStatus.BAD_REQUEST),
  SUBMISSION_NOT_FOUND(1066, "Submission not found", HttpStatus.NOT_FOUND),
  ASSESSMENT_NOT_SCHEDULED(1067, "Assessment is not currently scheduled", HttpStatus.BAD_REQUEST),
  ANSWER_NOT_FOUND(1068, "Answer not found", HttpStatus.NOT_FOUND),
  INVALID_ANSWER_FORMAT(1069, "Invalid answer format", HttpStatus.BAD_REQUEST),
  DRAFT_NOT_FOUND(1070, "Draft not found", HttpStatus.NOT_FOUND),
  QUESTION_NOT_FOUND(1071, "Question not found", HttpStatus.NOT_FOUND),
  SUBMISSION_NOT_GRADED(1072, "Submission has not been graded yet", HttpStatus.BAD_REQUEST),
  GRADES_NOT_RELEASED(1073, "Grades have not been released yet", HttpStatus.BAD_REQUEST),
  REGRADE_REQUEST_NOT_FOUND(1074, "Regrade request not found", HttpStatus.NOT_FOUND),
  QUESTION_TEMPLATE_NOT_FOUND(1075, "Question template not found", HttpStatus.NOT_FOUND),
  INVALID_TEMPLATE_SYNTAX(1076, "Invalid template syntax", HttpStatus.BAD_REQUEST),
  TEMPLATE_GENERATION_FAILED(
      1077, "Failed to generate question from template", HttpStatus.INTERNAL_SERVER_ERROR),
  MINDMAP_NOT_FOUND(1078, "Mindmap not found", HttpStatus.NOT_FOUND),
  MINDMAP_ACCESS_DENIED(
      1079, "You do not have permission to access this mindmap", HttpStatus.FORBIDDEN),
  MINDMAP_NODE_NOT_FOUND(1080, "Mindmap node not found", HttpStatus.NOT_FOUND),
  MINDMAP_GENERATION_FAILED(
      1081, "Failed to generate mindmap from AI", HttpStatus.INTERNAL_SERVER_ERROR),
  INVALID_MINDMAP_STRUCTURE(1082, "Invalid mindmap structure", HttpStatus.BAD_REQUEST),
  FINALIZE_EMPTY_QUESTIONS(1083, "questions list must not be empty", HttpStatus.BAD_REQUEST),
  QUESTION_TEXT_BLANK(
      1084, "questionText must not be blank for one or more questions", HttpStatus.BAD_REQUEST),
  MCQ_INVALID_OPTIONS(
      1085,
      "MCQ questions must have exactly 4 options (A, B, C, D) with no duplicates",
      HttpStatus.BAD_REQUEST),
  MCQ_INVALID_CORRECT_OPTION(
      1086, "correctAnswer for MCQ must be one of A, B, C, D", HttpStatus.BAD_REQUEST),
  CELL_EXCEEDS_TARGET(
      1087,
      "Adding these questions would exceed the cell's target question count",
      HttpStatus.BAD_REQUEST),
  TEMPLATE_NOT_USABLE(
      1088,
      "Template is in DRAFT status and cannot be used for finalisation",
      HttpStatus.BAD_REQUEST),
  TEMPLATE_ALREADY_PUBLISHED(1093, "Template is already published", HttpStatus.BAD_REQUEST),
  TEMPLATE_ALREADY_ARCHIVED(1094, "Template is already archived", HttpStatus.BAD_REQUEST),
  TEMPLATE_NOT_PUBLISHED(1095, "Template is not published and cannot be unpublished", HttpStatus.BAD_REQUEST),
  TEMPLATE_ACCESS_DENIED(
      1100, "You do not have permission to access this template", HttpStatus.FORBIDDEN),
  LESSON_NOT_FOUND(1089, "Lesson not found", HttpStatus.NOT_FOUND),
  CHAPTER_NOT_FOUND(1090, "Chapter not found", HttpStatus.NOT_FOUND),
  LESSON_GENERATION_FAILED(
      1091, "Failed to generate lesson content from AI", HttpStatus.INTERNAL_SERVER_ERROR),
  LESSON_ACCESS_DENIED(
      1092, "You do not have permission to access this lesson", HttpStatus.FORBIDDEN),
  EXAM_MATRIX_APPROVED(
      1095,
      "Exam matrix is already approved and cannot be modified. Reset the matrix first.",
      HttpStatus.BAD_REQUEST),
  CHAPTER_NOT_IN_LESSON(
      1096, "The specified chapter does not belong to this lesson", HttpStatus.BAD_REQUEST),
  CELL_QUESTION_COUNT_MISMATCH(
      1097,
      "Number of selected questions does not match the cell's required count",
      HttpStatus.BAD_REQUEST),
  CELL_QUESTION_DIMENSION_MISMATCH(
      1098,
      "One or more questions do not match the cell's difficulty or cognitive level",
      HttpStatus.BAD_REQUEST),
  ASSESSMENT_MATRIX_APPROVED_WHILE_PUBLISHED(
      1099,
      "Cannot approve matrix while assessment is already published or closed",
      HttpStatus.BAD_REQUEST),
  SUBMISSION_ALREADY_GRADED(1101, "Submission has already been graded", HttpStatus.BAD_REQUEST),
  ANSWER_SUBMISSION_MISMATCH(
      1102, "Answer does not belong to the specified submission", HttpStatus.BAD_REQUEST),
  GRADING_ACCESS_DENIED(
      1103, "You do not have permission to grade this submission", HttpStatus.FORBIDDEN),
  REGRADE_REQUEST_ALREADY_PENDING(
      1104, "A regrade request for this question is already pending", HttpStatus.BAD_REQUEST),
  REGRADE_REQUEST_NOT_PENDING(
      1105,
      "Regrade request is not in PENDING status and cannot be responded to",
      HttpStatus.BAD_REQUEST),

  ASSESSMENT_ALREADY_CLOSED(1106, "Assessment is already closed", HttpStatus.BAD_REQUEST),
  MATRIX_NOT_APPROVED_FOR_RESET(
      1107, "Only an APPROVED matrix can be reset to DRAFT", HttpStatus.BAD_REQUEST),
  QUESTION_ALREADY_IN_ASSESSMENT(
      1108, "Question is already part of this assessment", HttpStatus.BAD_REQUEST),
  QUESTION_NOT_IN_ASSESSMENT(
      1109, "Question does not belong to this assessment", HttpStatus.BAD_REQUEST),
  ASSESSMENT_QUESTION_EDIT_BLOCKED(
      1110,
      "Questions can only be managed on DRAFT non-matrix assessments",
      HttpStatus.BAD_REQUEST),
  SUBMISSION_ALREADY_INVALIDATED(
      1111, "Submission has already been invalidated", HttpStatus.BAD_REQUEST),
  SUBMISSION_INVALIDATION_BLOCKED(
      1112, "Only SUBMITTED or GRADED submissions can be invalidated", HttpStatus.BAD_REQUEST),
  SUBMISSION_RESULT_NOT_AVAILABLE(
      1113,
      "Your result is not available yet — grades have not been released",
      HttpStatus.FORBIDDEN),
  QUESTION_EDIT_BLOCKED(
      1114, "Questions used in a published assessment cannot be edited", HttpStatus.BAD_REQUEST),
  QUESTION_DELETE_BLOCKED(
      1115, "Questions used in an assessment cannot be deleted", HttpStatus.BAD_REQUEST),
  REGRADE_DEADLINE_PASSED(
      1116, "The regrade request window for this submission has closed", HttpStatus.BAD_REQUEST),
  ASSESSMENT_CLONE_BLOCKED(1117, "Cannot clone a deleted assessment", HttpStatus.BAD_REQUEST),
  CURRICULUM_NOT_FOUND(1118, "Curriculum not found", HttpStatus.NOT_FOUND),
  CURRICULUM_ALREADY_EXISTS(1119, "Curriculum already exists", HttpStatus.BAD_REQUEST),
  ASSESSMENT_LESSON_NOT_IN_MATRIX(
      1120,
      "One or more selected lessons are not available in the chosen exam matrix",
      HttpStatus.BAD_REQUEST),
  TEMPLATE_MAPPING_TEMPLATE_MISMATCH(
      1121,
      "Requested template does not match the matrix template mapping",
      HttpStatus.BAD_REQUEST),
  SUBJECT_NOT_FOUND(1122, "Subject not found", HttpStatus.NOT_FOUND),
  SUBJECT_ALREADY_EXISTS(1123, "Subject with this code already exists", HttpStatus.BAD_REQUEST),
  SUBJECT_HAS_CHAPTERS(
      1123_1,
      "Cannot deactivate subject while it still has chapters",
      HttpStatus.BAD_REQUEST),
  GRADE_SUBJECT_ALREADY_EXISTS(
      1124, "This subject is already linked to the given grade level", HttpStatus.BAD_REQUEST),
  GRADE_SUBJECT_NOT_FOUND(
      1125, "No mapping found between this subject and grade level", HttpStatus.NOT_FOUND),
  EXAM_MATRIX_ROW_NOT_FOUND(1126, "Exam matrix row not found", HttpStatus.NOT_FOUND),
  INVALID_FILE_FORMAT(2001, "Invalid file format", HttpStatus.BAD_REQUEST),
  MATRIX_ROW_QUESTION_TYPE_REQUIRED(
      1127, "questionTypeName is required when templateId is not provided", HttpStatus.BAD_REQUEST),
  SCHOOL_GRADE_NOT_FOUND(1128, "School grade not found", HttpStatus.NOT_FOUND),
    SCHOOL_GRADE_HAS_SUBJECTS(
            1128_1,
            "Cannot deactivate school grade while it still has subjects",
            HttpStatus.BAD_REQUEST),
  DOCUMENT_NOT_FOUND(1129, "Document not found", HttpStatus.NOT_FOUND),
  SCHOOL_GRADE_ALREADY_EXISTS(1129, "School grade already exists", HttpStatus.BAD_REQUEST),
  STUDENT_MULTIPLE_GRADES_NOT_ALLOWED(1132, "Students can only select one grade level", HttpStatus.BAD_REQUEST),
  ROADMAP_TOPIC_NOT_FOUND(1130, "Roadmap topic not found", HttpStatus.NOT_FOUND),
    INVALID_TOPIC_POINT_ORDER(
            1130_1,
            "Topic points must be positive and strictly increasing by order",
            HttpStatus.BAD_REQUEST),
  TEACHING_RESOURCE_NOT_FOUND(1131, "Teaching resource not found", HttpStatus.NOT_FOUND),
  TEACHING_RESOURCE_ACCESS_DENIED(
      1132, "You do not have permission to access this teaching resource", HttpStatus.FORBIDDEN),
  RESOURCE_FILE_TOO_LARGE(1133, "Resource file exceeds the allowed size of {limit}. (Uploaded: {actual})", HttpStatus.BAD_REQUEST),
  CHAT_SESSION_NOT_FOUND(1134, "Chat session not found", HttpStatus.NOT_FOUND),
  CHAT_SESSION_ACCESS_DENIED(
      1135, "You do not have permission to access this chat session", HttpStatus.FORBIDDEN),
  CHAT_SESSION_ARCHIVED(1136, "Chat session is archived", HttpStatus.BAD_REQUEST),
  CHAT_PROMPT_EMPTY(1137, "Prompt must not be empty", HttpStatus.BAD_REQUEST),
  CHAT_MESSAGE_NOT_FOUND(1138, "Chat message not found", HttpStatus.NOT_FOUND),
  CHAT_AI_CALL_FAILED(1139, "Failed to process chat with AI", HttpStatus.INTERNAL_SERVER_ERROR),
  ACCOUNT_NOT_ACTIVE(1140, "Account is not activated. Please check your email for a confirmation link.", HttpStatus.UNAUTHORIZED),
  LESSON_PLAN_NOT_FOUND(1141, "Lesson plan not found", HttpStatus.NOT_FOUND),
  LESSON_PLAN_ALREADY_EXISTS(1142, "You already have a lesson plan for this lesson", HttpStatus.BAD_REQUEST),
    LESSON_PLAN_ACCESS_DENIED(1143, "You do not have permission to access this lesson plan", HttpStatus.FORBIDDEN),
    QUESTION_REVIEW_STATUS_INVALID(
            1144,
            "Only AI_DRAFT questions can be approved",
            HttpStatus.BAD_REQUEST),
  COURSE_NOT_FOUND(1145, "Course not found", HttpStatus.NOT_FOUND),
  COURSE_ACCESS_DENIED(
      1146, "You do not have permission to access this course", HttpStatus.FORBIDDEN),
  COURSE_ALREADY_PUBLISHED(1147, "Course is already published", HttpStatus.BAD_REQUEST),
    COURSE_NOT_APPROVED(1147_1, "Course must be approved before publishing", HttpStatus.BAD_REQUEST),
    INVALID_COURSE_STATUS(1147_2, "Invalid course status transition", HttpStatus.BAD_REQUEST),
  ALREADY_ENROLLED(1148, "Student is already enrolled in this course", HttpStatus.BAD_REQUEST),
  ENROLLMENT_NOT_FOUND(1149, "Enrollment not found", HttpStatus.NOT_FOUND),
  ENROLLMENT_ACCESS_DENIED(
      1150, "You do not have permission to access this enrollment", HttpStatus.FORBIDDEN),
  COURSE_LESSON_NOT_FOUND(1151, "Course lesson not found", HttpStatus.NOT_FOUND),
  COURSE_NOT_PUBLISHED(1152, "Course is not published", HttpStatus.BAD_REQUEST),
  DUPLICATE_ORDER_INDEX(1153, "Duplicate order index detected in lesson reordering", HttpStatus.BAD_REQUEST),
  VIDEO_NOT_FOUND(1154, "Video not found for this lesson", HttpStatus.NOT_FOUND),
  QUESTION_TEMPLATE_NOT_IN_BANK(
          1155, "Question template does not belong to this question bank", HttpStatus.BAD_REQUEST),
  SUBSCRIPTION_PLAN_NOT_FOUND(1156, "Subscription plan not found", HttpStatus.NOT_FOUND),
  SUBSCRIPTION_PLAN_SLUG_EXISTS(1157, "A plan with this slug already exists", HttpStatus.BAD_REQUEST),
  SUBSCRIPTION_PLAN_HAS_ACTIVE_USERS(
      1158,
      "Cannot delete a plan that has active subscribers. Deactivate the plan instead.",
      HttpStatus.BAD_REQUEST),
  USER_SUBSCRIPTION_NOT_FOUND(1159, "User subscription not found", HttpStatus.NOT_FOUND),
  OCR_JOB_NOT_FOUND(1160, "OCR job not found", HttpStatus.NOT_FOUND),
  OCR_JOB_NOT_COMPLETED(1161, "OCR job not completed yet", HttpStatus.BAD_REQUEST),
  OCR_JOB_FAILED(1162, "OCR job failed", HttpStatus.INTERNAL_SERVER_ERROR),
  ASSESSMENT_ALREADY_IN_COURSE(1163, "Assessment is already added to this course", HttpStatus.BAD_REQUEST),
    COURSE_ASSESSMENT_NOT_FOUND(1164, "Course assessment not found", HttpStatus.NOT_FOUND),
    SUBSCRIPTION_PLAN_NOT_PURCHASABLE(1165, "Subscription plan is not purchasable", HttpStatus.BAD_REQUEST),
    NO_ACTIVE_SUBSCRIPTION(1166, "No active subscription found", HttpStatus.BAD_REQUEST),
        INSUFFICIENT_SUBSCRIPTION_TOKENS(1167, "Not enough subscription tokens", HttpStatus.BAD_REQUEST),
        GENERATED_SLIDE_NOT_FOUND(1168, "Generated slide file not found", HttpStatus.NOT_FOUND),
        GENERATED_SLIDE_ACCESS_DENIED(
                1169, "You do not have permission to access this generated slide", HttpStatus.FORBIDDEN),
        GENERATED_SLIDE_NOT_PUBLIC(1170, "Generated slide is not public", HttpStatus.BAD_REQUEST),
      ASSESSMENT_NOT_MATCH_COURSE_LESSONS(
          1171,
          "Assessment lessons do not match any lessons in this course",
          HttpStatus.BAD_REQUEST),
  // ─── Custom Course (provider = CUSTOM) ────────────────────────────────────
  CUSTOM_COURSE_SECTION_NOT_FOUND(1172, "Custom course section not found", HttpStatus.NOT_FOUND),
  CUSTOM_COURSE_SECTION_ACCESS_DENIED(
      1173,
      "You do not have permission to access this section",
      HttpStatus.FORBIDDEN),
  CUSTOM_COURSE_SECTION_HAS_LESSONS(
      1174,
      "Cannot delete a section that still has active lessons",
      HttpStatus.BAD_REQUEST),
  SECTION_REQUIRED_FOR_CUSTOM_COURSE(
      1175,
      "sectionId is required when adding a lesson to a CUSTOM course",
      HttpStatus.BAD_REQUEST),
  CUSTOM_TITLE_REQUIRED(
      1176,
      "customTitle is required when adding a lesson to a CUSTOM course",
      HttpStatus.BAD_REQUEST),
  LESSON_ID_REQUIRED_FOR_MINISTRY_COURSE(
      1177,
      "lessonId is required when adding a lesson to a MINISTRY course",
      HttpStatus.BAD_REQUEST),
  SUBJECT_REQUIRED_FOR_MINISTRY_COURSE(
      1178,
      "subjectId is required when creating a MINISTRY course",
      HttpStatus.BAD_REQUEST),
  GRADE_REQUIRED_FOR_MINISTRY_COURSE(
      1179,
      "schoolGradeId is required when creating a MINISTRY course",
      HttpStatus.BAD_REQUEST),
  SECTION_NOT_IN_COURSE(
      1180,
      "The specified section does not belong to this course",
      HttpStatus.BAD_REQUEST),
  OPERATION_NOT_SUPPORTED_FOR_PROVIDER(
      1181,
      "This operation is not supported for the course's provider type",
      HttpStatus.BAD_REQUEST),
  RATING_MIN_1(1182, "Rating must be at least 1", HttpStatus.BAD_REQUEST),
  RATING_MAX_5(1183, "Rating must be at most 5", HttpStatus.BAD_REQUEST),
  COMMENT_REQUIRED(1184, "Comment is required", HttpStatus.BAD_REQUEST),
  COURSE_NOT_ENROLLED(1185, "You must be enrolled in the course to leave a review", HttpStatus.FORBIDDEN),
  ALREADY_REVIEWED(1186, "You have already reviewed this course", HttpStatus.BAD_REQUEST),
  REVIEW_NOT_FOUND(1187, "Review not found", HttpStatus.NOT_FOUND),
    CHAPTER_HAS_LESSONS(1188, "Cannot delete chapter while it still has lessons", HttpStatus.BAD_REQUEST),

  // ─── Order Error Codes ────────────────────────────────────────────────────
  ORDER_NOT_FOUND(1188, "Order not found", HttpStatus.NOT_FOUND),
  ORDER_ACCESS_DENIED(1189, "You do not have permission to access this order", HttpStatus.FORBIDDEN),
  ORDER_ALREADY_PROCESSED(1190, "Order has already been processed", HttpStatus.BAD_REQUEST),
  ORDER_EXPIRED(1191, "Order has expired", HttpStatus.BAD_REQUEST),
  ORDER_CANNOT_BE_CANCELLED(1192, "Order cannot be cancelled", HttpStatus.BAD_REQUEST),

  // ─── Enrollment & Payment Error Codes ─────────────────────────────────────
  ENROLLMENT_IN_PROGRESS(1193, "Enrollment is already being processed", HttpStatus.CONFLICT),
  WALLET_CREATION_FAILED(1194, "Failed to create wallet", HttpStatus.INTERNAL_SERVER_ERROR),
  COURSE_HAS_PENDING_ORDERS(1195, "Course has pending orders and cannot be deleted", HttpStatus.BAD_REQUEST),
  COURSE_HAS_COMPLETED_ORDERS(1196, "Course has completed orders and cannot be deleted", HttpStatus.BAD_REQUEST),
  COURSE_MUST_HAVE_LESSONS(1197, "Course must have at least one lesson before submission", HttpStatus.BAD_REQUEST),
  COURSE_ALREADY_SUBMITTED(1198, "Course is already submitted for review", HttpStatus.BAD_REQUEST),
  INVALID_DISCOUNT_PRICE(1199, "Discounted price must be less than original price", HttpStatus.BAD_REQUEST),
  COURSE_RESUBMISSION_TOO_SOON(1200, "Please wait before resubmitting the course", HttpStatus.TOO_MANY_REQUESTS),

  // ─── Question Template & Matrix Error Codes ───────────────────────────────
  TAGS_REQUIRED(1201, "At least one tag is required", HttpStatus.BAD_REQUEST),
  TOO_MANY_TAGS(1202, "Maximum 5 tags allowed", HttpStatus.BAD_REQUEST),
  BANK_MISSING_CHAPTER(1203, "Question bank must be linked to a chapter", HttpStatus.BAD_REQUEST),
  BANK_GRADE_MISMATCH(1204, "Question bank grade does not match matrix grade", HttpStatus.BAD_REQUEST),
  BANK_SUBJECT_MISMATCH(1205, "Question bank subject does not match matrix subject", HttpStatus.BAD_REQUEST),

  // ─── Withdrawal Error Codes ───────────────────────────────────────────────
  WITHDRAWAL_REQUEST_NOT_FOUND(1207, "Withdrawal request not found", HttpStatus.NOT_FOUND),
  INVALID_WITHDRAWAL_STATUS(
      1208, "Withdrawal request status is invalid for this operation", HttpStatus.BAD_REQUEST),
  WITHDRAWAL_REQUEST_ALREADY_EXISTS(
      1209, "You already have a pending withdrawal request", HttpStatus.BAD_REQUEST),
  OTP_EXPIRED(1210, "OTP has expired", HttpStatus.BAD_REQUEST),
  INVALID_OTP(1211, "Invalid OTP code", HttpStatus.BAD_REQUEST),
  WITHDRAWAL_AMOUNT_TOO_SMALL(
      1212, "Withdrawal amount must be at least 10,000 VND", HttpStatus.BAD_REQUEST),
  TEMPLATE_PREVIEW_IMAGE_NOT_FOUND(
      1213, "This template does not have a preview image", HttpStatus.NOT_FOUND),

  // ─── Commission Proposal Error Codes ──────────────────────────────────────
  COMMISSION_PROPOSAL_NOT_FOUND(1214, "Commission proposal not found", HttpStatus.NOT_FOUND),
  COMMISSION_PROPOSAL_PENDING_EXISTS(
      1215, "You already have a pending commission proposal", HttpStatus.BAD_REQUEST),
  COMMISSION_PROPOSAL_INVALID_ACTION(
      1216, "Action must be APPROVED or REJECTED", HttpStatus.BAD_REQUEST),
  COMMISSION_PROPOSAL_ALREADY_REVIEWED(
      1217, "This proposal has already been reviewed", HttpStatus.BAD_REQUEST);


  ErrorCode(int code, String message, HttpStatusCode statusCode) {
    this.code = code;
    this.message = message;
    this.statusCode = statusCode;
  }

  private final int code;
  private final String message;
  private final HttpStatusCode statusCode;
}
