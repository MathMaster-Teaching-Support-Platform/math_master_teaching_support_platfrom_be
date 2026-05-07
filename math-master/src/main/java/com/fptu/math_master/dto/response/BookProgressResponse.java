package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.BookStatus;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookProgressResponse {

  private UUID bookId;
  private BookStatus status;
  private boolean bookVerified;

  private int totalLessons;
  private int verifiedLessons;
  private int totalPages;
  private int verifiedPages;

  private List<LessonProgress> lessons;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LessonProgress {
    private UUID lessonId;
    private String lessonTitle;
    private Integer pageStart;
    private Integer pageEnd;
    private int totalPages;
    private int verifiedPages;
    private boolean lessonVerified;
  }
}
