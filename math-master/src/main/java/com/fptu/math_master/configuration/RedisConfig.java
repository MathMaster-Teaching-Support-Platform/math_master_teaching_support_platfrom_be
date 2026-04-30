package com.fptu.math_master.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import com.fptu.math_master.component.StreamConsumerListener;

import java.time.Duration;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Configuration
@EnableCaching
@RequiredArgsConstructor
@Slf4j
public class RedisConfig {

  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    // Configure ObjectMapper for proper JSON serialization
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // Use String serializer for keys
    StringRedisSerializer stringSerializer = new StringRedisSerializer();
    template.setKeySerializer(stringSerializer);
    template.setHashKeySerializer(stringSerializer);

    // Use JSON serializer for values
    GenericJackson2JsonRedisSerializer jsonSerializer =
        new GenericJackson2JsonRedisSerializer(objectMapper);
    template.setValueSerializer(jsonSerializer);
    template.setHashValueSerializer(jsonSerializer);

    template.afterPropertiesSet();
    return template;
  }

  @Bean
  public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    // Configure ObjectMapper for proper JSON serialization with Java 8 date/time support
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    GenericJackson2JsonRedisSerializer jsonSerializer =
        new GenericJackson2JsonRedisSerializer(objectMapper);

    RedisCacheConfiguration defaultConfig =
        RedisCacheConfiguration.defaultCacheConfig()
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
            .entryTtl(Duration.ofSeconds(30))
            .disableCachingNullValues();

    Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
    cacheConfigs.put("adminDashboardStats", defaultConfig.entryTtl(Duration.ofSeconds(30)));
    cacheConfigs.put("adminRevenueByMonth", defaultConfig.entryTtl(Duration.ofMinutes(10)));
    cacheConfigs.put("adminSystemStatus", defaultConfig.entryTtl(Duration.ofSeconds(15)));
    cacheConfigs.put("studentDashboardSummary", defaultConfig.entryTtl(Duration.ofSeconds(30)));
    cacheConfigs.put("studentDashboardUpcomingTasks", defaultConfig.entryTtl(Duration.ofSeconds(30)));
    cacheConfigs.put("studentDashboardRecentGrades", defaultConfig.entryTtl(Duration.ofSeconds(30)));
    cacheConfigs.put("studentDashboardLearningProgress", defaultConfig.entryTtl(Duration.ofSeconds(60)));
    cacheConfigs.put("studentDashboardWeeklyActivity", defaultConfig.entryTtl(Duration.ofSeconds(30)));
    cacheConfigs.put("studentDashboardStreak", defaultConfig.entryTtl(Duration.ofSeconds(30)));
    cacheConfigs.put("studentDashboardOverview", defaultConfig.entryTtl(Duration.ofSeconds(30)));

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaultConfig)
        .withInitialCacheConfigurations(cacheConfigs)
        .build();
  }

  @Bean
  public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer(
      RedisConnectionFactory connectionFactory,
      StreamConsumerListener streamConsumerListener,
      RedisTemplate<String, Object> redisTemplate) {

    String consumerName;
    try {
        consumerName = InetAddress.getLocalHost().getHostName() + "-" + UUID.randomUUID().toString().substring(0, 8);
    } catch (UnknownHostException e) {
        consumerName = "consumer-" + UUID.randomUUID().toString().substring(0, 8);
    }

    StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
            .pollTimeout(Duration.ofSeconds(1))
            .errorHandler(throwable -> {
                Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                if (cause.getMessage() != null && cause.getMessage().contains("Connection closed")) {
                    log.warn("Redis Stream: connection temporarily lost, will retry automatically: {}", cause.getMessage());
                } else {
                    log.error("Redis Stream processing error: ", throwable);
                }
            })
            .build();

    StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
        StreamMessageListenerContainer.create(connectionFactory, options);

    container.receive(
        Consumer.from("notif-group", consumerName),
        StreamOffset.create("notifications", ReadOffset.lastConsumed()),
        streamConsumerListener);

    container.start();
    return container;
  }
}
