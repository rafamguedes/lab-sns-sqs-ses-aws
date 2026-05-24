package com.consumer.service;

import com.consumer.dto.EmailMessage;
import com.consumer.utils.SnsMessageExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeadLetterQueueService {

  private final SqsClient sqsClient;
  private final ObjectMapper objectMapper;
  private final SnsMessageExtractor snsMessageExtractor;

  @Value("${aws.sqs.queue.email}")
  private String emailQueueUrl;

  @Value("${aws.sqs.queue.dlq}")
  private String dlqUrl;

  public void processDLQMessages() {
    try {
      var receiveRequest =
          ReceiveMessageRequest.builder()
              .queueUrl(dlqUrl)
              .maxNumberOfMessages(10)
              .waitTimeSeconds(5)
              .visibilityTimeout(30)
              .build();

      List<Message> messages = sqsClient.receiveMessage(receiveRequest).messages();

      if (messages.isEmpty()) {
        log.info("No messages found in DLQ");
        return;
      }

      log.info("Found {} messages in DLQ", messages.size());
      messages.forEach(this::processDLQMessage);

    } catch (Exception e) {
      log.error("Error processing DLQ: {}", e.getMessage());
    }
  }

  private void processDLQMessage(Message message) {
    try {
      var emailJson = snsMessageExtractor.extract(message.body());
      var emailMessage = objectMapper.readValue(emailJson, EmailMessage.class);

      log.error("MESSAGE IN DLQ - id: {} failed after multiple attempts", message.messageId());
      log.warn("  Recipient: {}", emailMessage.getRecipient());
      log.warn("  Subject: {}", emailMessage.getSubject());
      log.warn("  Customer: {}", emailMessage.getCustomerName());

    } catch (Exception e) {
      log.error("Error processing DLQ message {}: {}", message.messageId(), e.getMessage());
    }
  }

  public void reprocessAllMessages() {
    int totalReprocessed = 0;
    int totalFailed = 0;
    int maxBatches = 100;
    int batches = 0;
    List<Message> messages;

    try {
      do {
        messages =
            sqsClient
                .receiveMessage(
                    ReceiveMessageRequest.builder()
                        .queueUrl(dlqUrl)
                        .maxNumberOfMessages(10)
                        .waitTimeSeconds(5)
                        .build())
                .messages();

        for (Message message : messages) {
          try {
            reprocessMessage(message);
            totalReprocessed++;
          } catch (Exception e) {
            totalFailed++;
            log.error(
                "Skipping message {} after reprocess failure: {}",
                message.messageId(),
                e.getMessage());
          }
        }

        batches++;
        if (batches >= maxBatches) {
          log.warn(
              "Reached max batch limit ({}). Stopping reprocess. Requeued: {}, Failed: {}",
              maxBatches,
              totalReprocessed,
              totalFailed);
          return;
        }

      } while (!messages.isEmpty());

      log.info("DLQ reprocess complete. Requeued: {}, Failed: {}", totalReprocessed, totalFailed);

    } catch (Exception e) {
      log.error(
          "DLQ reprocess interrupted after {} messages: {}", totalReprocessed, e.getMessage());
    }
  }

  private void reprocessMessage(Message message) {
    sqsClient.sendMessage(
        SendMessageRequest.builder().queueUrl(emailQueueUrl).messageBody(message.body()).build());

    sqsClient.deleteMessage(
        DeleteMessageRequest.builder()
            .queueUrl(dlqUrl)
            .receiptHandle(message.receiptHandle())
            .build());

    log.info("Message {} successfully requeued from DLQ", message.messageId());
  }
}
