package com.fptu.math_master.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Nationalized;

import com.fptu.math_master.enums.BookStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(
    name = "books",
    indexes = {
      @Index(name = "idx_books_school_grade", columnList = "school_grade_id"),
      @Index(name = "idx_books_subject", columnList = "subject_id"),
      @Index(name = "idx_books_curriculum", columnList = "curriculum_id"),
      @Index(name = "idx_books_status", columnList = "status"),
      @Index(name = "idx_books_verified", columnList = "verified")
    })
public class Book extends BaseEntity {

  @Column(name = "school_grade_id", nullable = false)
  private UUID schoolGradeId;

  @Column(name = "subject_id", nullable = false)
  private UUID subjectId;

  @Column(name = "curriculum_id", nullable = false)
  private UUID curriculumId;

  @Size(max = 255)
  @Nationalized
  @Column(name = "title", length = 255, nullable = false)
  private String title;

  @Size(max = 255)
  @Nationalized
  @Column(name = "publisher", length = 255)
  private String publisher;

  @Size(max = 50)
  @Column(name = "academic_year", length = 50)
  private String academicYear;

  @Size(max = 500)
  @Column(name = "pdf_path", length = 500)
  private String pdfPath;

  @Size(max = 500)
  @Column(name = "thumbnail_path", length = 500)
  private String thumbnailPath;

  @Column(name = "total_pages")
  private Integer totalPages;

  @Column(name = "ocr_page_from")
  private Integer ocrPageFrom;

  @Column(name = "ocr_page_to")
  private Integer ocrPageTo;

  @Column(name = "status", length = 50, nullable = false)
  @Enumerated(EnumType.STRING)
  private BookStatus status;

  @Column(name = "ocr_error", columnDefinition = "TEXT")
  private String ocrError;

  @Column(name = "verified", nullable = false)
  private Boolean verified;

  @Column(name = "verified_at")
  private Instant verifiedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "school_grade_id", insertable = false, updatable = false)
  private SchoolGrade schoolGrade;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "subject_id", insertable = false, updatable = false)
  private Subject subject;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "curriculum_id", insertable = false, updatable = false)
  private Curriculum curriculum;

  @PrePersist
  public void prePersist() {
    super.prePersist();
    if (status == null) status = BookStatus.DRAFT;
    if (verified == null) verified = Boolean.FALSE;
  }
}
