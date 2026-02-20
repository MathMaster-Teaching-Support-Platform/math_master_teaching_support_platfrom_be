package com.fptu.math_master.configuration;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
public class SwaggerConfig {
  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Math Master API")
                .version("1.0")
                .description("Math Master Teaching Support Platform API"))
        .servers(
            List.of(
                new Server()
                    .url("https://nhducminhqt.name.vn")
                    .description("Production HTTPS Server"),
                new Server().url("http://localhost:8080").description("Local Development Server")));
  }
}
