package com.consumer.service;

import com.consumer.dto.EmailMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailSesService {

  private final SesClient sesClient;

  @Value("${aws.ses.from}")
  private String fromEmail;

  @Value("${aws.ses.from-name:Notification System}")
  private String fromName;

  public void sendSimpleEmail(EmailMessage emailMessage) {
    try {
      var request =
          SendEmailRequest.builder()
              .destination(Destination.builder().toAddresses(emailMessage.getRecipient()).build())
              .message(
                  Message.builder()
                      .body(
                          Body.builder()
                              .text(
                                  Content.builder()
                                      .charset("UTF-8")
                                      .data(emailMessage.getBody())
                                      .build())
                              .build())
                      .subject(
                          Content.builder()
                              .charset("UTF-8")
                              .data(emailMessage.getSubject())
                              .build())
                      .build())
              .source(fromName + " <" + fromEmail + ">")
              .build();

      var result = sesClient.sendEmail(request);

      log.info(
          "Email sent via SES to: {} - MessageId: {}",
          emailMessage.getRecipient(),
          result.messageId());

    } catch (SesException e) {
      String errorCode = e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "UNKNOWN";
      log.error(
          "SES error sending to {}: {} (code: {})",
          emailMessage.getRecipient(),
          e.getMessage(),
          errorCode);
      throw new RuntimeException("SES sending failure", e);

    } catch (Exception e) {
      log.error(
          "Unexpected error sending email to {}: {}", emailMessage.getRecipient(), e.getMessage());
      throw new RuntimeException("SES sending failure", e);
    }
  }
}
