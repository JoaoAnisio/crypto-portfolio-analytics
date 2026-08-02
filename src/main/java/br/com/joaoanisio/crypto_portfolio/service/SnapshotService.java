package br.com.joaoanisio.crypto_portfolio.service;

import br.com.joaoanisio.crypto_portfolio.domain.PortfolioSnapshot;
import br.com.joaoanisio.crypto_portfolio.dto.PortfolioSummaryResponse;
import br.com.joaoanisio.crypto_portfolio.dto.SnapshotResponse;
import br.com.joaoanisio.crypto_portfolio.repository.PortfolioSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SnapshotService {

    private final PortfolioSnapshotRepository snapshotRepository;
    private final PortfolioService portfolioService;
    private final Clock clock;

    /**
     * Gera (ou atualiza) o snapshot do dia corrente.
     *
     * Idempotente por data: chamar múltiplas vezes no mesmo dia atualiza
     * o registro existente em vez de criar um novo.
     */
    @Transactional
    public SnapshotResponse captureToday() {
        LocalDate today = LocalDate.now(clock);
        PortfolioSummaryResponse summary = portfolioService.getSummary();

        PortfolioSnapshot snapshot = snapshotRepository.findBySnapshotDate(today)
                .orElseGet(() -> PortfolioSnapshot.builder().snapshotDate(today).build());

        snapshot.setCurrency(summary.currency());
        snapshot.setTotalInvested(summary.totalInvested());
        snapshot.setTotalCurrentValue(summary.totalCurrentValue());
        snapshot.setUnrealizedPnl(summary.unrealizedPnl());
        snapshot.setRealizedPnl(summary.realizedPnl());
        snapshot.setOpenPositions(summary.openPositions());

        PortfolioSnapshot saved = snapshotRepository.save(snapshot);

        log.info("Snapshot registrado | data={} | valorAtual={} | posicoes={}",
                today, saved.getTotalCurrentValue(), saved.getOpenPositions());

        return SnapshotResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<SnapshotResponse> findHistory(LocalDate from, LocalDate to) {
        List<PortfolioSnapshot> snapshots = (from != null && to != null)
                ? snapshotRepository.findBySnapshotDateBetweenOrderBySnapshotDateAsc(from, to)
                : snapshotRepository.findAllByOrderBySnapshotDateDesc();

        return snapshots.stream().map(SnapshotResponse::from).toList();
    }
}