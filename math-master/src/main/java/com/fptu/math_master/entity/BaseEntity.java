package com.fptu.math_master.entity;

import java.time.Instant;
import java.util.UUID;

import com.fptu.math_master.util.UuidV7Generator;

import jakarta.persistence.*;
import lombok.*;

/**
 * The 'BaseEntity' class providing common audit fields for all entities.
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEntity {

  /**
   * id
   */
  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  /**
   * created_at
   */
  @Column(name = "created_at", updatable = false, nullable = false)
  private Instant createdAt;

  /**
   * created_by
   */
  @Column(name = "created_by", updatable = false)
  private UUID createdBy;

  /**
   * updated_at
   */
  @Column(name = "updated_at")
  private Instant updatedAt;

  /**
   * updated_by
   */
  @Column(name = "updated_by")
  private UUID updatedBy;

  /**
   * deleted_at
   */
  @Column(name = "deleted_at")
  private Instant deletedAt;

  /**
   * deleted_by
   */
  @Column(name = "deleted_by")
  private UUID deletedBy;

  @PrePersist
  protected void prePersist() {
    Instant now = Instant.now();
    if (createdAt == null) createdAt = now;
    if (updatedAt == null) updatedAt = now;
  }

  @PreUpdate
  protected void preUpdate() {
    updatedAt = Instant.now();
  }
}
