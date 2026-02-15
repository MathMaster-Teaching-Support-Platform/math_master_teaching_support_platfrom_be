package com.fptu.math_master.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.configuration.properties.CentrifugoProperties;
import com.fptu.math_master.service.CentrifugoService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CentrifugoServiceImpl implements CentrifugoService {

  CentrifugoProperties centrifugoProperties;
  OkHttpClient centrifugoHttpClient;
  ObjectMapper objectMapper;

  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

  @Override
  public String generateConnectionToken(UUID userId, UUID attemptId) {
    try {
      long exp = Instant.now().plusSeconds(3600).getEpochSecond();

      Map<String, Object> claims = new HashMap<>();
      claims.put("sub", userId.toString());
      claims.put("exp", exp);
      claims.put("info", Map.of("attemptId", attemptId.toString()));

      String payload = objectMapper.writeValueAsString(claims);
      String signature = hmacSha256(payload, centrifugoProperties.getTokenHmacSecret());

      return Base64.getUrlEncoder()
              .withoutPadding()
              .encodeToString(payload.getBytes(StandardCharsets.UTF_8))
          + "."
          + signature;

    } catch (Exception e) {
      log.error("Error generating connection token", e);
      throw new RuntimeException("Failed to generate connection token", e);
    }
  }

  @Override
  public void publishToChannel(String channel, Map<String, Object> data) {
    try {
      Map<String, Object> payload = new HashMap<>();
      payload.put("method", "publish");
      payload.put(
          "params",
          Map.of(
              "channel", channel,
              "data", data));

      String jsonPayload = objectMapper.writeValueAsString(payload);

      Request request =
          new Request.Builder()
              .url(centrifugoProperties.getApiUrl())
              .addHeader("Authorization", "apikey " + centrifugoProperties.getApiKey())
              .addHeader("Content-Type", "application/json")
              .post(RequestBody.create(jsonPayload, JSON))
              .build();

      try (Response response = centrifugoHttpClient.newCall(request).execute()) {
        if (!response.isSuccessful()) {
          log.error("Failed to publish to Centrifugo channel {}: {}", channel, response.code());
        }
      }

    } catch (Exception e) {
      log.error("Error publishing to Centrifugo channel: {}", channel, e);
    }
  }

  @Override
  public void publishAnswerAck(UUID attemptId, UUID questionId, Long sequenceNumber) {
    Map<String, Object> data = new HashMap<>();
    data.put("type", "ack");
    data.put("questionId", questionId.toString());
    data.put("serverTimestamp", Instant.now().toString());
    data.put("sequenceNumber", sequenceNumber);
    data.put("success", true);

    publishToChannel(getAttemptChannel(attemptId), data);
  }

  @Override
  public void publishFlagAck(UUID attemptId, UUID questionId, Boolean flagged) {
    Map<String, Object> data = new HashMap<>();
    data.put("type", "flag_ack");
    data.put("questionId", questionId.toString());
    data.put("flagged", flagged);
    data.put("serverTimestamp", Instant.now().toString());
    data.put("success", true);

    publishToChannel(getAttemptChannel(attemptId), data);
  }

  @Override
  public void publishSubmitted(UUID attemptId) {
    Map<String, Object> data = new HashMap<>();
    data.put("type", "submitted");
    data.put("attemptId", attemptId.toString());
    data.put("serverTimestamp", Instant.now().toString());

    publishToChannel(getAttemptChannel(attemptId), data);
  }

  @Override
  public String getAttemptChannel(UUID attemptId) {
    return "attempt:" + attemptId.toString();
  }

  private String hmacSha256(String data, String key) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    mac.init(secretKey);
    byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
  }
}
