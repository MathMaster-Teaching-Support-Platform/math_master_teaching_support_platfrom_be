package com.fptu.math_master.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(
    name = "book_series",
    indexes = {
      @Index(name = "idx_book_series_school_grade", columnList = "school_grade_id"),
      @Index(name = "idx_book_series_subject", columnList = "subject_id")
    })
public class BookSeries extends BaseEntity {

  @Size(max = 255)
  @Nationalized
  @Column(name = "name", length = 255, nullable = false)
  private String name;

  @Column(name = "school_grade_id", nullable = false)
  private UUID schoolGradeId;

  @Column(name = "subject_id", nullable = false)
  private UUID subjectId;

  @Size(max = 50)
  @Column(name = "academic_year", length = 50)
  private String academicYear;
}

