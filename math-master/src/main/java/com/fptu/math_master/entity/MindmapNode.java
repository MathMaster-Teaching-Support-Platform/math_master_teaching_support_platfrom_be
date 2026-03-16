package com.fptu.math_master.entity;

import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.Nationalized;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "mindmap_nodes",
    indexes = {
      @Index(name = "idx_mindmap_nodes_mindmap", columnList = "mindmap_id"),
      @Index(name = "idx_mindmap_nodes_parent", columnList = "parent_id"),
      @Index(name = "idx_mindmap_nodes_order", columnList = "display_order"),
      @Index(name = "idx_mindmap_nodes_mindmap_order", columnList = "mindmap_id, display_order")
    })
/**
 * The entity of 'MindmapNode'.
 */
public class MindmapNode extends BaseEntity {

  /**
   * mindmap_id
   */
  @Column(name = "mindmap_id", nullable = false)
  private UUID mindmapId;

  /**
   * parent_id
   */
  @Column(name = "parent_id")
  private UUID parentId;

  /**
   * content
   */
  @Nationalized
  @Column(name = "content", nullable = false)
  private String content;

  /**
   * color
   */
  @Size(max = 50)
  @Column(name = "color", length = 50)
  private String color;

  /**
   * icon
   */
  @Size(max = 100)
  @Column(name = "icon", length = 100)
  private String icon;

  /**
   * display_order
   */
  @Column(name = "display_order")
  private Integer displayOrder;

  /**
   * Relationships
   * - Many-to-One with Mindmap
   * - Many-to-One with MindmapNode (parent)
   * - One-to-Many with MindmapNode (children)
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "mindmap_id", insertable = false, updatable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Mindmap mindmap;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id", insertable = false, updatable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private MindmapNode parent;

  @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Set<MindmapNode> children;

  @PrePersist
  public void prePersist() {
    if (displayOrder == null) displayOrder = 0;
  }
}
