package com.fptu.math_master.repository;

import com.fptu.math_master.entity.School;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchoolRepository extends JpaRepository<School, UUID> {

  Optional<School> findByName(String name);

  boolean existsByName(String name);
}
