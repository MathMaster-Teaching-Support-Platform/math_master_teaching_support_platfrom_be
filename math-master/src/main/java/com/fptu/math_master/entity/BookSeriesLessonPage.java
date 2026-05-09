package com.fptu.math_master.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
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
    name = "book_series_lesson_pages",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_bslp_series_lesson",
          columnNames = {"book_series_id", "lesson_id"})
    },
    indexes = {
      @Index(name = "idx_bslp_series_order", columnList = "book_series_id, order_index"),
      @Index(name = "idx_bslp_book", columnList = "book_id"),
      @Index(name = "idx_bslp_lesson", columnList = "lesson_id")
    })
public class BookSeriesLessonPage extends BaseEntity {

  @Column(name = "book_series_id", nullable = false)
  private UUID bookSeriesId;

  @Column(name = "lesson_id", nullable = false)
  private UUID lessonId;

  @Column(name = "book_id", nullable = false)
  private UUID bookId;

  @Column(name = "page_start", nullable = false)
  private Integer pageStart;

  @Column(name = "page_end", nullable = false)
  private Integer pageEnd;

  @Column(name = "order_index", nullable = false)
  private Integer orderIndex;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "book_series_id", insertable = false, updatable = false)
  private BookSeries bookSeries;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "book_id", insertable = false, updatable = false)
  private Book book;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lesson_id", insertable = false, updatable = false)
  private Lesson lesson;
}

