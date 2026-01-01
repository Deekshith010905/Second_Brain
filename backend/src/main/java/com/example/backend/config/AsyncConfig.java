package com.example.backend.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {
  @Bean(name = "ingestExecutor")
  public Executor ingestExecutor() {
    ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
    ex.setCorePoolSize(1);
    ex.setMaxPoolSize(1);
    ex.setQueueCapacity(200);
    ex.setThreadNamePrefix("ingest-");
    ex.initialize();
    return ex;
  }
}
