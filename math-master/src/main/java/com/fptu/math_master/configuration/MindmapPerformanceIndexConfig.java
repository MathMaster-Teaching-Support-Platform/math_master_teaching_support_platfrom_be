package com.fptu.math_master.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MindmapPerformanceIndexConfig implements ApplicationRunner {

  private final JdbcTemplate jdbcTemplate;

  @Override
  public void run(ApplicationArguments args) {
    long startedAt = System.currentTimeMillis();
    try {
      // Ensure critical indexes exist even if ddl-auto=update skips/does not backfill existing
      // schemas.
      jdbcTemplate.execute(
          "CREATE INDEX IF NOT EXISTS idx_mindmap_nodes_mindmap_order ON mindmap_nodes (mindmap_id, display_order)");
      jdbcTemplate.execute(
          "CREATE INDEX IF NOT EXISTS idx_mindmap_nodes_mindmap ON mindmap_nodes (mindmap_id)");
      jdbcTemplate.execute(
          "CREATE INDEX IF NOT EXISTS idx_mindmaps_teacher_deleted_created ON mindmaps (teacher_id, deleted_at, created_at DESC)");
      jdbcTemplate.execute(
          "CREATE INDEX IF NOT EXISTS idx_mindmaps_lesson_deleted_created ON mindmaps (lesson_id, deleted_at, created_at DESC)");
      jdbcTemplate.execute(
          "CREATE INDEX IF NOT EXISTS idx_mindmaps_teacher_lesson_deleted_created ON mindmaps (teacher_id, lesson_id, deleted_at, created_at DESC)");

      // Refresh planner statistics to avoid bad execution plans after index/data changes.
      jdbcTemplate.execute("ANALYZE mindmap_nodes");
      jdbcTemplate.execute("ANALYZE mindmaps");

      log.info(
          "Mindmap performance indexes ensured and analyzed in {} ms",
          System.currentTimeMillis() - startedAt);
    } catch (Exception ex) {
      log.warn("Unable to initialize mindmap performance indexes: {}", ex.getMessage());
    }
  }
}
