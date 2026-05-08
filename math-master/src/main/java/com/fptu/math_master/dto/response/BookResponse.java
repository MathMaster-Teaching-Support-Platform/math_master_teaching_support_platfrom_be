package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.BookStatus;
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
public class BookResponse {
  private UUID id;
  private UUID schoolGradeId;
  private String schoolGradeName;
  private UUID subjectId;
  private String subjectName;
  private UUID bookSeriesId;
  private String bookSeriesName;
  private UUID curriculumId;
  private String curriculumName;
  private String title;
  private String publisher;
  private String academicYear;
  private String pdfPath;
  private String thumbnailPath;
  private Integer totalPages;
  private Integer ocrPageFrom;
  private Integer ocrPageTo;
  private BookStatus status;
  private String ocrError;
  private boolean verified;
  private Instant verifiedAt;
  private long mappedLessonCount;
  private Instant createdAt;
  private Instant updatedAt;
}
