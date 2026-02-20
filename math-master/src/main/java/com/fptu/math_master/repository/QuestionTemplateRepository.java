package com.fptu.math_master.repository;

import com.fptu.math_master.entity.QuestionTemplate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionTemplateRepository extends JpaRepository<QuestionTemplate, UUID> {}
