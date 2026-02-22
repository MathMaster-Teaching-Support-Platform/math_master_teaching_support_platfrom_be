package com.fptu.math_master.entity;

import com.fptu.math_master.util.UuidV7Generator;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.Nationalized;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(
    name = "mindmap_nodes",
    indexes = {
      @Index(name = "idx_mindmap_nodes_mindmap", columnList = "mindmap_id"),
      @Index(name = "idx_mindmap_nodes_parent", columnList = "parent_id"),
      @Index(name = "idx_mindmap_nodes_order", columnList = "display_order")
    })
public class MindmapNode {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "mindmap_id", nullable = false)
  private UUID mindmapId;

  @Column(name = "parent_id")
  private UUID parentId;

  @Lob
  @Nationalized
  @Column(name = "content", nullable = false)
  private String content;

  @Size(max = 50)
  @Column(name = "color", length = 50)
  private String color;

  @Size(max = 100)
  @Column(name = "icon", length = 100)
  private String icon;

  @Column(name = "display_order")
  private Integer displayOrder;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "mindmap_id", insertable = false, updatable = false)
  private Mindmap mindmap;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id", insertable = false, updatable = false)
  private MindmapNode parent;

  @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<MindmapNode> children;

  @PrePersist
  public void prePersist() {
    if (displayOrder == null) displayOrder = 0;
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = Instant.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }
}
