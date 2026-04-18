package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.CourseProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponse {
  private UUID id;
  private UUID teacherId;
  private String teacherName;
  private String teacherAvatar;
  private String teacherPosition;
  private CourseProvider provider;
  private UUID subjectId;
  private String subjectName;
  private UUID schoolGradeId;
  private Integer gradeLevel;
  private String title;
  private String description;
  private String thumbnailUrl;
  private boolean isPublished;
  private BigDecimal rating;
  private int ratingCount;
  private int studentsCount;
  private int lessonsCount;
  private Instant createdAt;
  private Instant updatedAt;
  private String whatYouWillLearn;
  private String requirements;
  private String targetAudience;
  private String subtitle;
  private String language;
  private BigDecimal totalVideoHours;
  private Integer articlesCount;
  private Integer resourcesCount;
  private int sectionsCount;
}
