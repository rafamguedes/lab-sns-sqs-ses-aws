package com.consumer.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailMessage implements Serializable {

  @NotBlank(message = "Recipient must not be blank")
  @Email(message = "Recipient must be a valid email address")
  @JsonProperty("destinatario")
  @JsonAlias({"recipient"})
  private String recipient;

  @NotBlank(message = "Subject must not be blank")
  @JsonProperty("assunto")
  @JsonAlias({"subject"})
  private String subject;

  @NotBlank(message = "Body must not be blank")
  @JsonProperty("corpo")
  @JsonAlias({"body"})
  private String body;

  @JsonProperty("nomeCliente")
  @JsonAlias({"customerName"})
  private String customerName;
}
