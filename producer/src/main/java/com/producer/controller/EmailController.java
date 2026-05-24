package com.producer.controller;

import com.producer.service.EmailProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class EmailController {

    private final EmailProducerService emailProducerService;

    @PostMapping("/disparar")
    public ResponseEntity<String> dispararEmails(@RequestBody Map<String, String> request) {
        String assunto = request.getOrDefault("assunto", "Notificação Importante");
        String corpo = request.getOrDefault("corpo", "Olá, esta é uma mensagem de teste.");

        emailProducerService.enviarEmailsParaTodosClientes(assunto, corpo);

        return ResponseEntity.ok("Emails enviados para processamento!");
    }
}