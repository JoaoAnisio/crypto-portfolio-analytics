package br.com.joaoanisio.crypto_portfolio.service;

import br.com.joaoanisio.crypto_portfolio.domain.PortfolioSnapshot;
import br.com.joaoanisio.crypto_portfolio.dto.PortfolioSummaryResponse;
import br.com.joaoanisio.crypto_portfolio.dto.SnapshotResponse;
import br.com.joaoanisio.crypto_portfolio.repository.PortfolioSnapshotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SnapshotService")
class SnapshotServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate FIXED_DATE = LocalDate.of(2026, 3, 15);

    /** Relogio congelado: o teste roda igual hoje, amanha e a meia-noite. */
    private final Clock fixedClock =
            Clock.fixed(Instant.parse("2026-03-15T18:00:00Z"), ZONE);

    @Mock
    private PortfolioSnapshotRepository snapshotRepository;

    @Mock
    private PortfolioService portfolioService;

    private SnapshotService snapshotService;

    private SnapshotService service() {
        if (snapshotService == null) {
            snapshotService = new SnapshotService(snapshotRepository, portfolioService, fixedClock);
        }
        return snapshotService;
    }

    private PortfolioSummaryResponse summary() {
        return new PortfolioSummaryResponse(
                "BRL",
                new BigDecimal("292500.00"),
                new BigDecimal("340000.00"),
                new BigDecimal("47500.00"),
                new BigDecimal("16.24"),
                new BigDecimal("37500.00"),
                new BigDecimal("85000.00"),
                2,
                Instant.now(),
                List.of());
    }

    @Test
    @DisplayName("cria snapshot com a data do relogio injetado")
    void createsSnapshotForToday() {
        when(portfolioService.getSummary()).thenReturn(summary());
        when(snapshotRepository.findBySnapshotDate(FIXED_DATE)).thenReturn(Optional.empty());
        when(snapshotRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        SnapshotResponse response = service().captureToday();

        assertThat(response.date()).isEqualTo(FIXED_DATE);
        assertThat(response.totalCurrentValue()).isEqualByComparingTo("340000.00");
        assertThat(response.totalPnl()).isEqualByComparingTo("85000.00");

        ArgumentCaptor<PortfolioSnapshot> captor = ArgumentCaptor.forClass(PortfolioSnapshot.class);
        verify(snapshotRepository).save(captor.capture());
        assertThat(captor.getValue().getSnapshotDate()).isEqualTo(FIXED_DATE);
        assertThat(captor.getValue().getId()).isNull();  // entidade nova
    }

    @Test
    @DisplayName("atualiza o registro existente em vez de criar outro no mesmo dia")
    void isIdempotentWithinTheSameDay() {
        UUID existingId = UUID.randomUUID();
        PortfolioSnapshot existing = PortfolioSnapshot.builder()
                .id(existingId)
                .snapshotDate(FIXED_DATE)
                .currency("BRL")
                .totalInvested(new BigDecimal("100000.00"))
                .totalCurrentValue(new BigDecimal("100000.00"))
                .unrealizedPnl(BigDecimal.ZERO)
                .realizedPnl(BigDecimal.ZERO)
                .openPositions(1)
                .build();

        when(portfolioService.getSummary()).thenReturn(summary());
        when(snapshotRepository.findBySnapshotDate(FIXED_DATE)).thenReturn(Optional.of(existing));
        when(snapshotRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        service().captureToday();

        ArgumentCaptor<PortfolioSnapshot> captor = ArgumentCaptor.forClass(PortfolioSnapshot.class);
        verify(snapshotRepository).save(captor.capture());

        // Mesmo id: atualizou, nao criou
        assertThat(captor.getValue().getId()).isEqualTo(existingId);
        assertThat(captor.getValue().getTotalCurrentValue()).isEqualByComparingTo("340000.00");
    }

    @Test
    @DisplayName("nao persiste nada ao apenas consultar o historico")
    void historyIsReadOnly() {
        when(snapshotRepository.findAllByOrderBySnapshotDateDesc()).thenReturn(List.of());

        assertThat(service().findHistory(null, null)).isEmpty();
        verify(snapshotRepository, never()).save(any());
    }
}