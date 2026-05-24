package com.consumer.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SnsMessageExtractor {

  private final ObjectMapper objectMapper;

  public SnsMessageExtractor(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public String extract(String messageBody) {
    try {
      if (messageBody.contains("\"Message\"")) {
        JsonNode jsonNode = objectMapper.readTree(messageBody);
        JsonNode messageNode = jsonNode.get("Message");
        if (messageNode != null) {
          return messageNode.asText();
        }
      }
    } catch (Exception e) {
      log.warn("Could not extract SNS wrapper, using raw body. Reason: {}", e.getMessage());
    }
    return messageBody;
  }
}
