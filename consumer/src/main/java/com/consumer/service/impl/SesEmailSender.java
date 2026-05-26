package com.consumer.service.impl;

import com.consumer.dto.EmailMessageDTO;
import com.consumer.service.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class SesEmailSender implements EmailSender {

  private final SesClient sesClient;

  @Value("${aws.ses.from}")
  private String fromEmail;

  @Value("${aws.ses.from-name:Notification System}")
  private String fromName;

  @Override
  public void send(EmailMessageDTO email) {

    var request =
        SendEmailRequest.builder()
            .destination(Destination.builder().toAddresses(email.getRecipient()).build())
            .message(
                Message.builder()
                    .subject(Content.builder().charset("UTF-8").data(email.getSubject()).build())
                    .body(
                        Body.builder()
                            .text(Content.builder().charset("UTF-8").data(email.getBody()).build())
                            .build())
                    .build())
            .source(fromName + " <" + fromEmail + ">")
            .build();

    try {
      var result = sesClient.sendEmail(request);

      log.info(
          "Email sent successfully tenantId={} messageId={} sesMessageId={}",
          email.getTenantId(),
          email.getMessageId(),
          result.messageId());

    } catch (SesException e) {
      var errorCode = e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "UNKNOWN";

      log.error(
          "SES error tenantId={} messageId={} errorCode={}",
          email.getTenantId(),
          email.getMessageId(),
          errorCode,
          e);

      throw e;
    }
  }
}
