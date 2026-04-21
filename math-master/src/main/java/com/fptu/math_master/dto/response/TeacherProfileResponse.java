package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.ProfileStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeacherProfileResponse {
  private UUID id;
  private UUID userId;
  private String userName;
  private String fullName;
  private String schoolName;
  private String schoolAddress;
  private String schoolWebsite;
  private String position;
  private String verificationDocumentKey;
  private String description;
  private ProfileStatus status;
  private String adminComment;
  private UUID reviewedBy;
  private String reviewedByName;
  private LocalDateTime reviewedAt;
  private Instant createdAt;
  private Instant updatedAt;

  // discovery fields
  private String avatar;
  private long totalCourses;
  private int totalStudents;
  private int totalRatings;
  private BigDecimal averageRating;

  // social links
  private String websiteUrl;
  private String linkedinUrl;
  private String youtubeUrl;
  private String facebookUrl;
}
