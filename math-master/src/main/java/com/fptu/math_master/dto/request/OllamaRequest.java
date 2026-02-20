package com.fptu.math_master.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OllamaRequest {

  private String model;

  private String prompt;

  private List<Message> messages;

  private Boolean stream;

  private Double temperature;

  @JsonProperty("max_tokens")
  private Integer maxTokens;

  private String system;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Message {
    private String role;
    private String content;
  }
}
