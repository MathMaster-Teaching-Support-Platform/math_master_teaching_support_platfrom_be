package com.fptu.math_master;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Base class cho tat ca Unit Tests trong MathMaster.
 *
 * <p>- MockitoExtension: tu dong inject @Mock, @InjectMocks, @Captor
 *
 * <p>- LENIENT: tranh UnnecessaryStubbingException khi co shared mock setup trong @BeforeEach
 * nhung khong phai tat ca test deu dung
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public abstract class BaseUnitTest {

  protected ObjectMapper mapper = new ObjectMapper();

  @BeforeEach
  void baseSetUp() {
    mapper.findAndRegisterModules();
  }
}
