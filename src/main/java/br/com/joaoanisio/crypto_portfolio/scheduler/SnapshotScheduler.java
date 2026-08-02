package br.com.joaoanisio.crypto_portfolio.scheduler;

import br.com.joaoanisio.crypto_portfolio.service.SnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "portfolio.snapshot.enabled", havingValue = "true", matchIfMissing = true)
public class SnapshotScheduler {

    private final SnapshotService snapshotService;

    /**
     * Captura diária do estado do portfólio.
     *
     * O try/catch é deliberado: uma falha na API externa não pode
     * derrubar o agendador nem poluir o log com stacktrace de erro
     * previsível. O job simplesmente tenta de novo no dia seguinte.
     */
    @Scheduled(cron = "${portfolio.snapshot.cron}", zone = "America/Sao_Paulo")
    public void captureDailySnapshot() {
        log.info("Iniciando captura agendada de snapshot");
        try {
            snapshotService.captureToday();
        } catch (Exception ex) {
            log.error("Falha na captura agendada de snapshot: {}", ex.getMessage());
        }
    }
}