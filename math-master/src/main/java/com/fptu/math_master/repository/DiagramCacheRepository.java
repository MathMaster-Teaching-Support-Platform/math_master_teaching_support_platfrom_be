package com.fptu.math_master.repository;

import com.fptu.math_master.entity.DiagramCache;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiagramCacheRepository extends JpaRepository<DiagramCache, UUID> {

  Optional<DiagramCache> findByLatexHash(String latexHash);
}
