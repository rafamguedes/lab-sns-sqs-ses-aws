package com.consumer.service;

import com.consumer.dto.EmailMessageDTO;
import com.consumer.utils.SnsMessageParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ses.model.SesException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailProcessingService {

  @Value("${aws.sqs.queue.email}")
  private String queueUrl;

  private final ExecutorService executorService;
  private final SnsMessageParser snsMessageParser;
  private final ObjectMapper objectMapper;
  private final Validator validator;
  private final EmailSender emailSender;
  private final SqsClient sqsClient;

  private final Semaphore semaphore = new Semaphore(100);

  public boolean hasCapacity(int needed) {
    return semaphore.availablePermits() >= needed;
  }

  public void processAsync(Message message) {
    semaphore.acquireUninterruptibly();

    executorService.submit(
        () -> {
          try {
            process(message);
          } finally {
            semaphore.release();
          }
        });
  }

  private void process(Message message) {
    try {
      String json = snsMessageParser.unwrapSnsMessage(message.body());
      EmailMessageDTO email = objectMapper.readValue(json, EmailMessageDTO.class);

      var violations = validator.validate(email);
      if (!violations.isEmpty()) {
        log.warn("Validation failed messageId={} violations={}", message.messageId(), violations);
        delete(message);
        return;
      }

      emailSender.send(email);
      delete(message);

    } catch (JsonProcessingException e) {
      log.error("Invalid JSON payload messageId={}", message.messageId(), e);
      delete(message);

    } catch (SesException e) {
      String errorCode = e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "UNKNOWN";
      log.error("SES error messageId={} errorCode={}", message.messageId(), errorCode, e);

    } catch (SdkException e) {
      log.error("AWS SDK error messageId={}", message.messageId(), e);

    } catch (Exception e) {
      log.error("Unexpected processing error messageId={}", message.messageId(), e);

    }
  }

  private void delete(Message message) {
    try {
      sqsClient.deleteMessage(
          DeleteMessageRequest.builder()
              .queueUrl(queueUrl)
              .receiptHandle(message.receiptHandle())
              .build());
    } catch (Exception e) {
      log.error("Delete failed messageId={}", message.messageId(), e);
    }
  }
}
