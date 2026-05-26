package com.consumer.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SnsMessageParser {

  private final ObjectMapper objectMapper;

  public String unwrapSnsMessage(String body) {
    try {
      JsonNode root = objectMapper.readTree(body);

      if (root.has("Type") && root.has("Message")) {
        return root.get("Message").asText();
      }

      return body;

    } catch (Exception e) {
      log.warn("Could not parse SNS wrapper", e);
      return body;
    }
  }
}
