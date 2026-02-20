package com.fptu.math_master.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OllamaChatResponse {
  String model;

  @JsonProperty("created_at")
  String createdAt;

  Message message;

  @JsonProperty("done_reason")
  String doneReason;

  Boolean done;

  @JsonProperty("total_duration")
  Long totalDuration;

  @JsonProperty("load_duration")
  Long loadDuration;

  @JsonProperty("prompt_eval_count")
  Integer promptEvalCount;

  @JsonProperty("prompt_eval_duration")
  Long promptEvalDuration;

  @JsonProperty("eval_count")
  Integer evalCount;

  @JsonProperty("eval_duration")
  Long evalDuration;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class Message {
    String role;
    String content;
  }
}
