package com.fptu.math_master.repository;

import com.fptu.math_master.entity.MindmapNode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MindmapNodeRepository extends JpaRepository<MindmapNode, UUID> {

  @Query("SELECT n FROM MindmapNode n WHERE n.mindmapId = :mindmapId ORDER BY n.displayOrder ASC")
  List<MindmapNode> findByMindmapIdOrderByDisplayOrder(@Param("mindmapId") UUID mindmapId);

  @Query(
      "SELECT n FROM MindmapNode n WHERE n.mindmapId = :mindmapId AND n.parentId IS NULL ORDER BY n.displayOrder ASC")
  List<MindmapNode> findRootNodesByMindmapId(@Param("mindmapId") UUID mindmapId);

  @Query("SELECT n FROM MindmapNode n WHERE n.parentId = :parentId ORDER BY n.displayOrder ASC")
  List<MindmapNode> findByParentIdOrderByDisplayOrder(@Param("parentId") UUID parentId);

  @Query("SELECT COUNT(n) FROM MindmapNode n WHERE n.mindmapId = :mindmapId")
  long countByMindmapId(@Param("mindmapId") UUID mindmapId);

  @Query(
      "SELECT n.mindmapId as mindmapId, COUNT(n) as count FROM MindmapNode n WHERE n.mindmapId IN :mindmapIds GROUP BY n.mindmapId")
  List<NodeCountProjection> countByMindmapIds(@Param("mindmapIds") List<UUID> mindmapIds);

  interface NodeCountProjection {
    UUID getMindmapId();

    Long getCount();
  }

  @Query("SELECT n FROM MindmapNode n WHERE n.id = :id AND n.mindmapId = :mindmapId")
  Optional<MindmapNode> findByIdAndMindmapId(
      @Param("id") UUID id, @Param("mindmapId") UUID mindmapId);
}
