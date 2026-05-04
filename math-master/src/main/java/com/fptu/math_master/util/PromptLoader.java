package com.fptu.math_master.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

/**
 * Loads versioned prompt templates from the classpath ({@code resources/prompts/})
 * and exposes both the body and a short content hash. The hash is stamped into
 * {@code Question.generationMetadata.promptVersion} so a regenerated batch can be
 * traced back to the exact prompt revision that produced it.
 *
 * <p>Caches the {@code (body, hash)} pair on first read; subsequent calls are
 * lock-free hash-map reads.
 */
@Component
public class PromptLoader {

  private final ConcurrentHashMap<String, Loaded> cache = new ConcurrentHashMap<>();

  /** Returns the prompt body for a given prompt name (without the {@code .md}). */
  public String body(String name) {
    return load(name).body;
  }

  /** Short SHA-256 hash (first 12 hex chars) of the prompt body. */
  public String version(String name) {
    return load(name).hash;
  }

  private Loaded load(String name) {
    return cache.computeIfAbsent(name, this::readFromClasspath);
  }

  private Loaded readFromClasspath(String name) {
    String path = "prompts/" + name + ".md";
    try {
      ClassPathResource resource = new ClassPathResource(path);
      String body = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
      return new Loaded(body, sha256Short(body));
    } catch (IOException e) {
      throw new IllegalStateException("Prompt not found on classpath: " + path, e);
    }
  }

  private static String sha256Short(String body) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(body.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest).substring(0, 12);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  private record Loaded(String body, String hash) {}
}
