package com.producer.service;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.PublishRequest;
import com.producer.dto.EmailMessage;
import com.producer.entity.ClienteEntity;
import com.producer.repository.ClienteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailProducerService {

    private final AmazonSNS amazonSNS;
    private final ClienteRepository clienteRepository;
    private final ObjectMapper objectMapper;

    @Value("${aws.sns.topic.email}")
    private String emailTopicArn;

    public void enviarEmailsParaTodosClientes(String assunto, String corpo) {
        List<ClienteEntity> clientes = clienteRepository.findByAtivoTrue();

        log.info("Enviando emails para {} clientes", clientes.size());

        for (ClienteEntity cliente : clientes) {
            var messageId = UUID.randomUUID().toString();
            var tenantId = UUID.randomUUID().toString();

            try {
                EmailMessage mensagem = new EmailMessage(
                        messageId,
                        tenantId,
                        cliente.getEmail(),
                        assunto,
                        corpo,
                        cliente.getNome()
                );

                String mensagemJson = objectMapper.writeValueAsString(mensagem);

                PublishRequest publishRequest = new PublishRequest()
                        .withTopicArn(emailTopicArn)
                        .withMessage(mensagemJson)
                        .withSubject("Email para " + cliente.getNome());

                amazonSNS.publish(publishRequest);
                log.info("Mensagem enviada para o SNS: {}", cliente.getEmail());

            } catch (Exception e) {
                log.error("Erro ao enviar mensagem para {}: {}",
                        cliente.getEmail(), e.getMessage());
            }
        }
    }
}