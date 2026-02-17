package com.fptu.math_master;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MathMasterApplication {

  public static void main(String[] args) {
    SpringApplication.run(MathMasterApplication.class, args);
  }
}
