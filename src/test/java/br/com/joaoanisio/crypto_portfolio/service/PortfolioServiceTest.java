package br.com.joaoanisio.crypto_portfolio.service;

import br.com.joaoanisio.crypto_portfolio.domain.Asset;
import br.com.joaoanisio.crypto_portfolio.domain.Transaction;
import br.com.joaoanisio.crypto_portfolio.domain.TransactionType;
import br.com.joaoanisio.crypto_portfolio.dto.PortfolioSummaryResponse;
import br.com.joaoanisio.crypto_portfolio.dto.PriceQuote;
import br.com.joaoanisio.crypto_portfolio.integration.CoinGeckoProperties;
import br.com.joaoanisio.crypto_portfolio.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioService")
class PortfolioServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PriceService priceService;

    /**
     * Properties e um record simples: usar a instancia real em vez de um mock
     * deixa o teste mais legivel e evita stub desnecessario.
     */
    @Spy
    private CoinGeckoProperties properties = new CoinGeckoProperties(
            "http://localhost", "", "brl",
            Duration.ofSeconds(3), Duration.ofSeconds(5), Duration.ofSeconds(60));

    @InjectMocks
    private PortfolioService portfolioService;

    private static final Asset BTC = Asset.builder()
            .id(UUID.randomUUID()).symbol("BTC").name("Bitcoin").coingeckoId("bitcoin").build();

    private static final Asset ETH = Asset.builder()
            .id(UUID.randomUUID()).symbol("ETH").name("Ethereum").coingeckoId("ethereum").build();

    private Transaction tx(Asset asset, TransactionType type, String qty, String price, String date) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .asset(asset)
                .type(type)
                .quantity(new BigDecimal(qty))
                .unitPrice(new BigDecimal(price))
                .executedAt(Instant.parse(date))
                .build();
    }

    @Test
    @DisplayName("retorna resumo zerado quando nao ha transacoes")
    void emptyPortfolio() {
        when(transactionRepository.findAllWithAssetChronological()).thenReturn(List.of());

        PortfolioSummaryResponse summary = portfolioService.getSummary();

        assertThat(summary.openPositions()).isZero();
        assertThat(summary.positions()).isEmpty();
        assertThat(summary.totalInvested()).isEqualByComparingTo("0");
        assertThat(summary.totalCurrentValue()).isEqualByComparingTo("0");
        assertThat(summary.currency()).isEqualTo("BRL");
    }

    @Test
    @DisplayName("consolida o cenario completo com compras, venda e dois ativos")
    void fullScenario() {
        when(transactionRepository.findAllWithAssetChronological()).thenReturn(List.of(
                tx(BTC, TransactionType.BUY, "0.5", "300000", "2026-01-10T12:00:00Z"),
                tx(BTC, TransactionType.BUY, "0.5", "400000", "2026-02-15T12:00:00Z"),
                tx(BTC, TransactionType.SELL, "0.25", "500000", "2026-03-20T12:00:00Z"),
                tx(ETH, TransactionType.BUY, "2", "15000", "2026-04-05T12:00:00Z")));

        when(priceService.getPrices(anyCollection())).thenReturn(Map.of(
                "bitcoin", new PriceQuote("bitcoin", "brl", new BigDecimal("400000"), Instant.now()),
                "ethereum", new PriceQuote("ethereum", "brl", new BigDecimal("20000"), Instant.now())));

        PortfolioSummaryResponse summary = portfolioService.getSummary();

        assertThat(summary.totalInvested()).isEqualByComparingTo("292500");
        assertThat(summary.realizedPnl()).isEqualByComparingTo("37500");
        assertThat(summary.openPositions()).isEqualTo(2);

        // 0.75 x 400.000 = 300.000  |  2 x 20.000 = 40.000
        assertThat(summary.totalCurrentValue()).isEqualByComparingTo("340000");
        assertThat(summary.unrealizedPnl()).isEqualByComparingTo("47500");
        assertThat(summary.totalPnl()).isEqualByComparingTo("85000");

        assertThat(summary.positions())
                .extracting(p -> p.symbol())
                .containsExactly("BTC", "ETH");   // ordenado por valor decrescente

        assertThat(summary.positions().get(0).averageCost()).isEqualByComparingTo("350000");
        assertThat(summary.positions().get(0).allocationPercent()).isEqualByComparingTo("88.24");
        assertThat(summary.positions().get(1).allocationPercent()).isEqualByComparingTo("11.76");
    }

    @Test
    @DisplayName("busca todas as cotacoes em uma unica chamada, nao uma por ativo")
    void fetchesPricesInSingleBatch() {
        when(transactionRepository.findAllWithAssetChronological()).thenReturn(List.of(
                tx(BTC, TransactionType.BUY, "1", "300000", "2026-01-10T12:00:00Z"),
                tx(ETH, TransactionType.BUY, "2", "15000", "2026-01-11T12:00:00Z")));

        when(priceService.getPrices(anyCollection())).thenReturn(Map.of(
                "bitcoin", new PriceQuote("bitcoin", "brl", new BigDecimal("400000"), Instant.now()),
                "ethereum", new PriceQuote("ethereum", "brl", new BigDecimal("20000"), Instant.now())));

        portfolioService.getSummary();

        verify(priceService, times(1)).getPrices(Set.of("bitcoin", "ethereum"));
    }

    @Test
    @DisplayName("exclui do resumo as posicoes totalmente vendidas")
    void excludesClosedPositions() {
        when(transactionRepository.findAllWithAssetChronological()).thenReturn(List.of(
                tx(BTC, TransactionType.BUY, "1", "300000", "2026-01-10T12:00:00Z"),
                tx(BTC, TransactionType.SELL, "1", "350000", "2026-02-10T12:00:00Z")));

        when(priceService.getPrices(anyCollection())).thenReturn(Map.of());

        PortfolioSummaryResponse summary = portfolioService.getSummary();

        assertThat(summary.openPositions()).isZero();
        assertThat(summary.totalInvested()).isEqualByComparingTo("0");
        // O lucro da venda permanece no historico mesmo sem posicao aberta
        assertThat(summary.realizedPnl()).isEqualByComparingTo("50000");
    }
}