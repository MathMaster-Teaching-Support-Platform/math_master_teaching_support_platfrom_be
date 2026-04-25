package com.fptu.math_master;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class cho tat ca Integration Tests.
 *
 * <p>- SpringBootTest: load full application context
 *
 * <p>- AutoConfigureMockMvc: inject MockMvc de test HTTP endpoints
 *
 * <p>- ActiveProfiles("test"): su dung application-test.yml
 *
 * <p>- Transactional: moi test chay trong transaction va rollback sau khi xong
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

  @Autowired protected MockMvc mockMvc;

  @Autowired protected ObjectMapper mapper;
}
