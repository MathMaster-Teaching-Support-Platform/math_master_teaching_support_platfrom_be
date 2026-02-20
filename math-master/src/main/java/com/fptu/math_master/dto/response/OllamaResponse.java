package com.fptu.math_master.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OllamaResponse {

  private String model;

  @JsonProperty("created_at")
  private String createdAt;

  private Message message;

  private Boolean done;

  @JsonProperty("total_duration")
  private Long totalDuration;

  @JsonProperty("load_duration")
  private Long loadDuration;

  @JsonProperty("prompt_eval_count")
  private Integer promptEvalCount;

  @JsonProperty("prompt_eval_duration")
  private Long promptEvalDuration;

  @JsonProperty("eval_count")
  private Integer evalCount;

  @JsonProperty("eval_duration")
  private Long evalDuration;

  // For generate endpoint (non-chat)
  private String response;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Message {
    private String role;
    private String content;
  }

  public String getTextContent() {
    if (message != null && message.getContent() != null) {
      return message.getContent();
    }
    return response;
  }
}
