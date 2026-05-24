package com.consumer.controller;

import com.consumer.service.DeadLetterQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.sqs.SqsClient;

@Slf4j
@RestController
@RequestMapping("/api/dlq")
@RequiredArgsConstructor
public class DLQController {

  private final DeadLetterQueueService dlqService;
  private final SqsClient sqsClient;

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
}
