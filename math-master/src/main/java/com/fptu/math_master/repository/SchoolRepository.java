package com.fptu.math_master.repository;

import com.fptu.math_master.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SchoolRepository extends JpaRepository<School, Long> {

  Optional<School> findByName(String name);

  boolean existsByName(String name);
}
