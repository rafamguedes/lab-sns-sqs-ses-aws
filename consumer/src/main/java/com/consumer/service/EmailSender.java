package com.consumer.service;

import com.consumer.dto.EmailMessageDTO;

public interface EmailSender {
  void send(EmailMessageDTO email);
}
