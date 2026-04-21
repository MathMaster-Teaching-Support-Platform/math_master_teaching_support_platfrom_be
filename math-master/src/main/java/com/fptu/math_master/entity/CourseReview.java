package com.fptu.math_master.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.Nationalized;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "course_reviews",
    indexes = {
      @Index(name = "idx_course_reviews_course", columnList = "course_id"),
      @Index(name = "idx_course_reviews_student", columnList = "student_id"),
      @Index(name = "idx_course_reviews_rating", columnList = "rating")
    },
    uniqueConstraints = {
      @UniqueConstraint(name = "uq_course_reviews_student_course", columnNames = {"student_id", "course_id"})
    }
)
public class CourseReview extends BaseEntity {

  @Column(name = "course_id", nullable = false)
  private UUID courseId;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Min(1)
  @Max(5)
  @Column(name = "rating", nullable = false)
  private Integer rating;

  @Size(max = 2000)
  @Nationalized
  @Column(name = "comment", length = 2000)
  private String comment;

  @Size(max = 2000)
  @Nationalized
  @Column(name = "instructor_reply", length = 2000)
  private String instructorReply;

  @Column(name = "replied_at")
  private java.time.Instant repliedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", insertable = false, updatable = false)
  private Course course;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", insertable = false, updatable = false)
  private User student;
}
