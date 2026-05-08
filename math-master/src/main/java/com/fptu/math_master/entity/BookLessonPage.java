package com.fptu.math_master.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Maps a Lesson (from the canonical curriculum hierarchy) to a page range
 * inside a specific Book's PDF. One row per (book, lesson). Defines which
 * pages get OCR'd and stored under that lesson_id in MongoDB.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(
    name = "book_lesson_pages",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_blp_book_lesson",
          columnNames = {"book_id", "lesson_id"})
    },
    indexes = {
      @Index(name = "idx_blp_book_order", columnList = "book_id, order_index"),
      @Index(name = "idx_blp_lesson", columnList = "lesson_id")
    })
public class BookLessonPage extends BaseEntity {

  @Column(name = "book_id", nullable = false)
  private UUID bookId;

  @Column(name = "lesson_id", nullable = false)
  private UUID lessonId;

  @Column(name = "page_start", nullable = false)
  private Integer pageStart;

  @Column(name = "page_end", nullable = false)
  private Integer pageEnd;

  @Column(name = "order_index", nullable = false)
  private Integer orderIndex;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "book_id", insertable = false, updatable = false)
  private Book book;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lesson_id", insertable = false, updatable = false)
  private Lesson lesson;
}
