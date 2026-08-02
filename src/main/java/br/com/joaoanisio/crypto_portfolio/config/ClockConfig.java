package br.com.joaoanisio.crypto_portfolio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    /**
     * Fixa o fuso de referência do sistema. Sem isso, a data do snapshot
     * dependeria do timezone da máquina onde a aplicação está rodando,
     * o que geraria resultados diferentes em desenvolvimento e em produção.
     */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }
}