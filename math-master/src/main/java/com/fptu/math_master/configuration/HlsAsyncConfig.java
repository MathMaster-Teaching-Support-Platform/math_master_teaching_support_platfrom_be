package com.fptu.math_master.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configures a dedicated thread pool for HLS transcoding jobs.
 *
 * <p>FFmpeg is a CPU-intensive, long-running process (can take 30–120 s per video).
 * Using a separate executor with a small pool size ensures that HLS jobs do not starve
 * the default Spring async executor used by notifications, OCR, etc.
 *
 * <p>The bean name {@code "hlsTaskExecutor"} must match the {@code @Async("hlsTaskExecutor")}
 * annotation in {@link com.fptu.math_master.service.impl.HlsTranscodingServiceImpl}.
 */
@Configuration
@Slf4j
public class HlsAsyncConfig {

  /**
   * Thread pool for HLS transcoding.
   * <ul>
   *   <li>Core pool: 2 threads (two concurrent FFmpeg processes at most by default)</li>
   *   <li>Max pool: 4 threads</li>
   *   <li>Queue: 50 pending jobs (teachers unlikely to upload > 50 videos simultaneously)</li>
   * </ul>
   */
  @Bean(name = "hlsTaskExecutor")
  public Executor hlsTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix("hls-worker-");
    executor.setWaitForTasksToCompleteOnShutdown(true);   // finish ongoing encodes on shutdown
    executor.setAwaitTerminationSeconds(300);              // wait up to 5 min for running FFmpegs
    executor.initialize();
    log.info("HLS task executor initialized (core=2, max=4, queue=50)");
    return executor;
  }
}
