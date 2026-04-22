package com.fptu.math_master.entity;

import com.fptu.math_master.util.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Standalone diagram cache: stores QuickLaTeX-rendered image URLs keyed by a SHA-256 hash of the
 * normalised LaTeX input. This cache is consulted when rendering LaTeX without an associated
 * Question entity (e.g. template preview, anonymous renders).
 */
@Entity
@Table(
    name = "diagram_cache",
    indexes = {
      @Index(name = "idx_diagram_cache_hash", columnList = "latex_hash", unique = true)
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagramCache {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  /** SHA-256 hex digest of the normalised LaTeX string (64 chars). */
  @Column(name = "latex_hash", nullable = false, length = 64, unique = true)
  private String latexHash;

  /** Original LaTeX content stored for debugging / cache inspection. */
  @Column(name = "latex_content", columnDefinition = "TEXT")
  private String latexContent;

  /** The QuickLaTeX image URL returned for this LaTeX. */
  @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
  private String imageUrl;

  @Column(name = "created_at", updatable = false, nullable = false)
  private Instant createdAt;

  @PrePersist
  public void prePersist() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }
}
