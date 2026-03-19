package com.fptu.math_master.configuration;

import com.fptu.math_master.configuration.properties.GeminiProperties;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GeminiConfig {

  GeminiProperties geminiProperties;

  @Bean(name = "geminiRestClient")
  public RestClient geminiRestClient() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    int timeoutMillis = (int) Duration.ofSeconds(geminiProperties.getTimeoutSeconds()).toMillis();
    factory.setConnectTimeout(timeoutMillis);
    factory.setReadTimeout(timeoutMillis);

    StringHttpMessageConverter stringConverter =
        new StringHttpMessageConverter(StandardCharsets.UTF_8);
    stringConverter.setSupportedMediaTypes(
        Arrays.asList(
            MediaType.TEXT_PLAIN,
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_OCTET_STREAM,
            MediaType.ALL));

    MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
    jsonConverter.setSupportedMediaTypes(
        Arrays.asList(
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_OCTET_STREAM,
            new MediaType("application", "*+json")));

    return RestClient.builder()
        .baseUrl(geminiProperties.getBaseUrl())
        .requestFactory(factory)
        .messageConverters(
            converters -> {
              converters.clear();
              converters.add(stringConverter);
              converters.add(jsonConverter);
            })
        .build();
  }
}
