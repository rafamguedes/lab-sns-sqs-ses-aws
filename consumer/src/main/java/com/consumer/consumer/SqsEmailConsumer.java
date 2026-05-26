package com.consumer.consumer;

import com.consumer.service.EmailProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

@Component
@Slf4j
@RequiredArgsConstructor
public class SqsEmailConsumer {

  private final SqsClient sqsClient;
  private final EmailProcessingService emailProcessingService;

  @Value("${aws.sqs.queue.email}")
  private String queueUrl;

  private volatile boolean running = true;

  @PostConstruct
  public void start() {
    // Menos pollers — back-pressure controla o ritmo, não quantidade de threads
    for (int i = 0; i < 5; i++) {
      Thread.ofVirtual().name("sqs-poller-" + i).start(this::runPoller);
    }
  }

  private void runPoller() {
    while (running) {
      try {
        if (!emailProcessingService.hasCapacity(10)) {
          LockSupport.parkNanos(Duration.ofMillis(200).toNanos());
          continue;
        }
        poll();
      } catch (Exception e) {
        log.error("Poller error", e);
      }
    }
  }

  private void poll() {
    var request =
        ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(10)
            .waitTimeSeconds(20)
            .visibilityTimeout(300)
            .messageSystemAttributeNames(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT)
            .build();

    List<Message> messages = sqsClient.receiveMessage(request).messages();

    if (messages.isEmpty()) return;

    log.info("Received {} messages", messages.size());
    messages.forEach(emailProcessingService::processAsync);
  }

  @PreDestroy
  public void stop() {
    running = false;
  }
}
