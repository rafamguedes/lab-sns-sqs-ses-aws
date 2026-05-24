package com.consumer.service;

import com.consumer.dto.EmailMessage;
import com.consumer.utils.SnsMessageExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailConsumerService {

  private final SqsClient sqsClient;
  private final Validator validator;
  private final ObjectMapper objectMapper;
  private final EmailSesService emailSesService;
  private final SnsMessageExtractor snsMessageExtractor;

  @Value("${aws.sqs.queue.email}")
  private String emailQueueUrl;

  @Value("${aws.sqs.max-retries:3}")
  private int maxRetries;

  private final AtomicInteger cycleCount = new AtomicInteger(0);

  @Scheduled(fixedDelay = 5000)
  public void consumeMessages() {
    cyclesCount();

    try {
      var receiveRequest =
          ReceiveMessageRequest.builder()
              .queueUrl(emailQueueUrl)
              .maxNumberOfMessages(10)
              .waitTimeSeconds(5)
              .messageSystemAttributeNames(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT)
              .build();

      var messages = sqsClient.receiveMessage(receiveRequest).messages();

      messages.forEach(this::processMessage);

    } catch (Exception e) {
      log.error("Error consuming messages: {}", e.getMessage());
    }
  }

  private void processMessage(Message message) {
    try {
      var emailJson = snsMessageExtractor.extract(message.body());
      var emailMessage = objectMapper.readValue(emailJson, EmailMessage.class);

      var violations = validator.validate(emailMessage);
      if (!violations.isEmpty()) {
        var errors =
            violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
        throw new IllegalArgumentException("Invalid EmailMessage: " + errors);
      }

      emailSesService.sendSimpleEmail(emailMessage);

    } catch (Exception e) {
      log.error("Error processing message {}: {}", message.messageId(), e.getMessage());
      handleProcessingFailure(message, e);
      return;
    }

    try {
      sqsClient.deleteMessage(
          DeleteMessageRequest.builder()
              .queueUrl(emailQueueUrl)
              .receiptHandle(message.receiptHandle())
              .build());
    } catch (Exception e) {
      log.warn(
          "Email sent but failed to delete message {} from queue: {}",
          message.messageId(),
          e.getMessage());
    }
  }

  private void handleProcessingFailure(Message message, Exception error) {
    int attempts = getProcessingAttempts(message);

    if (attempts >= maxRetries) {
      log.warn(
          "Message {} failed after {} attempts. Releasing back to SQS for DLQ redrive. Error: {}",
          message.messageId(),
          attempts,
          error.getMessage());
    } else {
      log.info(
          "Message {} failed on attempt {}/{}. Will retry after visibility timeout.",
          message.messageId(),
          attempts,
          maxRetries);
    }
  }

  private int getProcessingAttempts(Message message) {
    try {
      var attributes = message.attributes();
      if (attributes == null || attributes.isEmpty()) {
        log.warn(
            "No attributes found on message {}. Defaulting attempts to 1.", message.messageId());
        return 1;
      }
      String receiveCount = attributes.get(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT);
      return receiveCount != null ? Integer.parseInt(receiveCount) : 1;
    } catch (NumberFormatException e) {
      log.warn(
          "Could not parse ApproximateReceiveCount for message {}: {}",
          message.messageId(),
          e.getMessage());
      return 1;
    }
  }

  private void cyclesCount() {
    int cycle = cycleCount.incrementAndGet();
    if (cycle % 60 == 0) {
      log.info("Email consumer active. Cycle #{}", cycle);
    }

    if (cycle >= 1_000_000) {
      cycleCount.set(0);
    }

    if (cycleCount.incrementAndGet() % 60 == 0) {
      log.info("Email consumer active. Cycle #{}", cycleCount.get());
    }
  }
}
