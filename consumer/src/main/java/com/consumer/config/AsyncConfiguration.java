package com.consumer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AsyncConfiguration {

  @Bean(destroyMethod = "shutdown")
  public ExecutorService emailConsumerExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
  }
}
