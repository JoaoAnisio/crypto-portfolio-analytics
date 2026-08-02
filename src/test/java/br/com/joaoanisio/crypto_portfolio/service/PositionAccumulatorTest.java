package br.com.joaoanisio.crypto_portfolio.service;

import br.com.joaoanisio.crypto_portfolio.domain.Asset;
import br.com.joaoanisio.crypto_portfolio.domain.Transaction;
import br.com.joaoanisio.crypto_portfolio.domain.TransactionType;
import br.com.joaoanisio.crypto_portfolio.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PositionAccumulator")
class PositionAccumulatorTest {

    private Asset btc;
    private PositionAccumulator accumulator;

    @BeforeEach
    void setUp() {
        btc = Asset.builder()
                .id(UUID.randomUUID())
                .symbol("BTC")
                .name("Bitcoin")
                .coingeckoId("bitcoin")
                .build();
        accumulator = new PositionAccumulator(btc);
    }

    private Transaction transaction(TransactionType type, String quantity, String unitPrice) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .asset(btc)
                .type(type)
                .quantity(new BigDecimal(quantity))
                .unitPrice(new BigDecimal(unitPrice))
                .executedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("ao aplicar compras")
    class Buys {

        @Test
        @DisplayName("registra quantidade e custo da primeira compra")
        void singleBuy() {
            accumulator.apply(transaction(TransactionType.BUY, "0.5", "300000"));

            assertThat(accumulator.quantity()).isEqualByComparingTo("0.5");
            assertThat(accumulator.costBasis()).isEqualByComparingTo("150000");
            assertThat(accumulator.averageCost()).isEqualByComparingTo("300000");
            assertThat(accumulator.hasOpenPosition()).isTrue();
        }

        @Test
        @DisplayName("pondera o custo medio entre compras de precos diferentes")
        void weightedAverage() {
            accumulator.apply(transaction(TransactionType.BUY, "0.5", "300000"));
            accumulator.apply(transaction(TransactionType.BUY, "0.5", "400000"));

            assertThat(accumulator.quantity()).isEqualByComparingTo("1.0");
            assertThat(accumulator.costBasis()).isEqualByComparingTo("350000");
            assertThat(accumulator.averageCost()).isEqualByComparingTo("350000");
        }

        @Test
        @DisplayName("pondera pela quantidade, nao pela media simples dos precos")
        void weightsByQuantityNotByPrice() {
            accumulator.apply(transaction(TransactionType.BUY, "0.9", "100000"));
            accumulator.apply(transaction(TransactionType.BUY, "0.1", "200000"));

            // Media simples seria 150.000; a ponderada correta e 110.000
            assertThat(accumulator.averageCost()).isEqualByComparingTo("110000");
        }
    }

    @Nested
    @DisplayName("ao aplicar vendas")
    class Sells {

        @BeforeEach
        void buildPosition() {
            accumulator.apply(transaction(TransactionType.BUY, "0.5", "300000"));
            accumulator.apply(transaction(TransactionType.BUY, "0.5", "400000"));
        }

        @Test
        @DisplayName("apura o resultado realizado contra o custo medio")
        void realizesProfit() {
            accumulator.apply(transaction(TransactionType.SELL, "0.25", "500000"));

            // Recebeu 125.000, custo da parcela: 0.25 x 350.000 = 87.500
            assertThat(accumulator.realizedPnl()).isEqualByComparingTo("37500");
        }

        @Test
        @DisplayName("apura prejuizo quando vende abaixo do custo medio")
        void realizesLoss() {
            accumulator.apply(transaction(TransactionType.SELL, "0.25", "300000"));

            // Recebeu 75.000, custo da parcela: 87.500
            assertThat(accumulator.realizedPnl()).isEqualByComparingTo("-12500");
        }

        @Test
        @DisplayName("nao altera o custo medio da posicao remanescente")
        void keepsAverageCost() {
            accumulator.apply(transaction(TransactionType.SELL, "0.25", "500000"));

            assertThat(accumulator.quantity()).isEqualByComparingTo("0.75");
            assertThat(accumulator.costBasis()).isEqualByComparingTo("262500");
            assertThat(accumulator.averageCost()).isEqualByComparingTo("350000");
        }

        @Test
        @DisplayName("zera a posicao sem deixar residuo de arredondamento")
        void closesPositionCleanly() {
            accumulator.apply(transaction(TransactionType.SELL, "1.0", "350000"));

            assertThat(accumulator.quantity()).isEqualByComparingTo("0");
            assertThat(accumulator.costBasis()).isEqualByComparingTo("0");
            assertThat(accumulator.averageCost()).isEqualByComparingTo("0");
            assertThat(accumulator.hasOpenPosition()).isFalse();
            assertThat(accumulator.realizedPnl()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("acumula o resultado de vendas sucessivas")
        void accumulatesAcrossSells() {
            accumulator.apply(transaction(TransactionType.SELL, "0.25", "500000"));
            accumulator.apply(transaction(TransactionType.SELL, "0.25", "450000"));

            // 37.500 + (112.500 - 87.500) = 62.500
            assertThat(accumulator.realizedPnl()).isEqualByComparingTo("62500");
            assertThat(accumulator.quantity()).isEqualByComparingTo("0.5");
        }

        @Test
        @DisplayName("rejeita venda acima da posicao disponivel")
        void rejectsOversell() {
            assertThatThrownBy(() -> accumulator.apply(transaction(TransactionType.SELL, "2", "500000")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("BTC")
                    .hasMessageContaining("Histórico inconsistente");
        }
    }

    @Test
    @DisplayName("comeca zerado")
    void startsEmpty() {
        assertThat(accumulator.quantity()).isEqualByComparingTo("0");
        assertThat(accumulator.averageCost()).isEqualByComparingTo("0");
        assertThat(accumulator.hasOpenPosition()).isFalse();
    }
}