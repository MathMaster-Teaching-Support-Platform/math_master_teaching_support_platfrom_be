package com.fptu.math_master.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OllamaChatRequest {
    String model;
    List<Message> messages;
    Boolean stream;
    Options options;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Message {
        String role; // "system", "user", "assistant"
        String content;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Options {
        Double temperature;
        Integer numPredict;
        Integer topK;
        Double topP;
    }
}

