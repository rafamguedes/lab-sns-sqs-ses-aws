package com.producer.config;

import com.producer.entity.ClienteEntity;
import com.producer.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final ClienteRepository clienteRepository;
    private final Random random = new Random();

    private static final String[] NOMES = {
            "João", "Maria", "Pedro", "Ana", "Carlos", "Fernanda", "Ricardo", "Juliana",
            "Marcos", "Patrícia", "Luiz", "Camila", "Rafael", "Beatriz", "Gabriel", "Larissa",
            "Felipe", "Amanda", "Bruno", "Carolina", "Diego", "Daniela", "Eduardo", "Fabiana",
            "Gustavo", "Helena", "Igor", "Isabela", "Leonardo", "Jéssica", "Thiago", "Natália",
            "Vinícius", "Priscila", "Wagner", "Roberta", "Alexandre", "Sandra", "Renato", "Tatiane"
    };

    private static final String[] SOBRENOMES = {
            "Silva", "Santos", "Oliveira", "Souza", "Costa", "Pereira", "Rodrigues",
            "Almeida", "Nascimento", "Lima", "Ferreira", "Gomes", "Carvalho", "Araújo",
            "Martins", "Ribeiro", "Alves", "Monteiro", "Barbosa", "Rocha", "Dias",
            "Moreira", "Cardoso", "Fernandes", "Cavalcanti", "Correia", "Melo", "Vieira",
            "Nunes", "Teixeira", "Machado", "Freitas", "Coelho", "Cunha", "Mendes",
            "Lopes", "Marques", "Pinto", "Borges", "Moraes"
    };

    private static final String[] DOMINIOS = {
            "gmail.com", "hotmail.com", "outlook.com", "yahoo.com",
            "empresa.com.br", "corporacao.com", "startup.com", "tech.com",
            "mail.com", "protonmail.com"
    };

    @Override
    public void run(String... args) {
        log.info("Iniciando geração de 10.000 clientes de teste...");
        long startTime = System.currentTimeMillis();

        // Gerar clientes em lotes para melhor performance
        int batchSize = 5;
        int totalClientes = 50;
        int clientesInseridos = 0;

        // Clientes fixos para teste rápido
        clienteRepository.save(new ClienteEntity(null, "João Silva", "cyberrminfo@gmail.com", true));
        clienteRepository.save(new ClienteEntity(null, "Maria Santos", "rafaelmguedes89@gmail.com", true));
        clienteRepository.save(new ClienteEntity(null, "Pedro Oliveira", "pedro@email.com", true));
        clienteRepository.save(new ClienteEntity(null, "Ana Costa", "ana@email.com", true));
        clienteRepository.save(new ClienteEntity(null, "Carlos Souza", "carlos@email.com", false));
        clientesInseridos += 5;

        // Gerar o restante dos clientes
        List<ClienteEntity> lote = new ArrayList<>();

        for (int i = clientesInseridos + 1; i <= totalClientes; i++) {
            ClienteEntity cliente = gerarClienteAleatorio(i);
            lote.add(cliente);

            if (lote.size() >= batchSize) {
                clienteRepository.saveAll(lote);
                clientesInseridos += lote.size();
                log.info("Progresso: {}/{} clientes inseridos ({}%)",
                        clientesInseridos, totalClientes,
                        (clientesInseridos * 100) / totalClientes);
                lote.clear();
            }
        }

        // Inserir lote final
        if (!lote.isEmpty()) {
            clienteRepository.saveAll(lote);
            clientesInseridos += lote.size();
        }

        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime) / 1000;

        log.info("✅ Geração concluída! {} clientes inseridos em {} segundos",
                clientesInseridos, duration);
        log.info("📊 Estatísticas:");
        log.info("   - Total: {}", clienteRepository.count());
    }

    private ClienteEntity gerarClienteAleatorio(int id) {
        String nome = NOMES[random.nextInt(NOMES.length)];
        String sobrenome = SOBRENOMES[random.nextInt(SOBRENOMES.length)];
        String nomeCompleto = nome + " " + sobrenome;

        // Evitar duplicação de nomes adicionando um número
        String email = gerarEmailUnico(nome, sobrenome, id);

        // 90% dos clientes ativos, 10% inativos
        boolean ativo = random.nextDouble() < 0.9;

        return new ClienteEntity(null, nomeCompleto, email, ativo);
    }

    private String gerarEmailUnico(String nome, String sobrenome, int id) {
        String dominio = DOMINIOS[random.nextInt(DOMINIOS.length)];

        // Diferentes formatos de email para mais realismo
        int formato = random.nextInt(4);
        String email;

        switch (formato) {
            case 0:
                email = nome.toLowerCase() + "." + sobrenome.toLowerCase() + id + "@" + dominio;
                break;
            case 1:
                email = nome.toLowerCase() + sobrenome.toLowerCase() + id + "@" + dominio;
                break;
            case 2:
                email = nome.toLowerCase().charAt(0) + sobrenome.toLowerCase() + id + "@" + dominio;
                break;
            default:
                email = nome.toLowerCase() + "." + sobrenome.toLowerCase().charAt(0) + id + "@" + dominio;
                break;
        }

        return email;
    }
}