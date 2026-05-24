package com.consumer.controller;

import com.consumer.service.DeadLetterQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/dlq")
@RequiredArgsConstructor
public class DLQController {

  private final DeadLetterQueueService dlqService;
  private final SqsClient sqsClient;

  @Value("${aws.sqs.queue.dlq}")
  private String dlqUrl;

  @GetMapping("/status")
  public ResponseEntity<Map<String, String>> getDLQStatus() {
    try {
      var attributes =
          sqsClient
              .getQueueAttributes(
                  GetQueueAttributesRequest.builder()
                      .queueUrl(dlqUrl)
                      .attributeNamesWithStrings("All")
                      .build())
              .attributesAsStrings();

      return ResponseEntity.ok(
          Map.of(
              "queueUrl", dlqUrl,
              "messageCount",
                  attributes.getOrDefault(
                      QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES.toString(), "0"),
              "delayedMessageCount",
                  attributes.getOrDefault(
                      QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED.toString(), "0"),
              "notVisibleCount",
                  attributes.getOrDefault(
                      QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE.toString(),
                      "0"),
              "retentionPeriod",
                  attributes.getOrDefault(
                      QueueAttributeName.MESSAGE_RETENTION_PERIOD.toString(), "N/A")));

    } catch (Exception e) {
      log.error("Error fetching DLQ status: {}", e.getMessage());
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }

  @PostMapping("/process")
  public ResponseEntity<String> processDLQ() {
    dlqService.processDLQMessages();
    return ResponseEntity.accepted().body("DLQ processing started");
  }

  @PostMapping("/reprocess")
  public ResponseEntity<String> reprocessAllMessages() {
    dlqService.reprocessAllMessages();
    return ResponseEntity.accepted().body("Reprocessing of all DLQ messages started");
  }

  @DeleteMapping("/purge")
  public ResponseEntity<String> purgeDLQ() {
    try {
      sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(dlqUrl).build());

      log.warn("DLQ purged manually via API");
      return ResponseEntity.ok("DLQ purged successfully");

    } catch (Exception e) {
      log.error("Error purging DLQ: {}", e.getMessage());
      return ResponseEntity.internalServerError().body("Error purging DLQ: " + e.getMessage());
    }
  }
}
