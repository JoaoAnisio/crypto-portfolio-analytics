package br.com.joaoanisio.crypto_portfolio.service;

import br.com.joaoanisio.crypto_portfolio.domain.Asset;
import br.com.joaoanisio.crypto_portfolio.domain.Transaction;
import br.com.joaoanisio.crypto_portfolio.dto.AllocationResponse;
import br.com.joaoanisio.crypto_portfolio.dto.PortfolioSummaryResponse;
import br.com.joaoanisio.crypto_portfolio.dto.PositionResponse;
import br.com.joaoanisio.crypto_portfolio.dto.PriceQuote;
import br.com.joaoanisio.crypto_portfolio.exception.ExternalServiceException;
import br.com.joaoanisio.crypto_portfolio.integration.CoinGeckoProperties;
import br.com.joaoanisio.crypto_portfolio.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private static final int MONEY_SCALE = 2;
    private static final int PRICE_SCALE = 8;
    private static final int PERCENT_SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final TransactionRepository transactionRepository;
    private final PriceService priceService;
    private final CoinGeckoProperties properties;

    // Posição valorizada de um ativo: o acumulador somado à cotação de mercado.
    private record ValuedPosition(PositionAccumulator position,
                                  BigDecimal currentPrice,
                                  BigDecimal currentValue) {}

    public PortfolioSummaryResponse getSummary() {
        List<Transaction> transactions = transactionRepository.findAllWithAssetChronological();
        String currency = properties.vsCurrency().toUpperCase();

        if (transactions.isEmpty()) {
            return emptySummary(currency);
        }

        Map<Asset, PositionAccumulator> accumulators = accumulate(transactions);

        List<PositionAccumulator> openPositions = accumulators.values().stream()
                .filter(PositionAccumulator::hasOpenPosition)
                .toList();

        List<ValuedPosition> valued = valuate(openPositions);

        BigDecimal totalCurrentValue = valued.stream()
                .map(ValuedPosition::currentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalInvested = openPositions.stream()
                .map(PositionAccumulator::costBasis)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal realizedPnl = accumulators.values().stream()
                .map(PositionAccumulator::realizedPnl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal unrealizedPnl = totalCurrentValue.subtract(totalInvested);

        List<PositionResponse> positions = valued.stream()
                .map(v -> toPositionResponse(v, totalCurrentValue))
                .toList();

        return new PortfolioSummaryResponse(
                currency,
                money(totalInvested),
                money(totalCurrentValue),
                money(unrealizedPnl),
                percent(unrealizedPnl, totalInvested),
                money(realizedPnl),
                money(unrealizedPnl.add(realizedPnl)),
                positions.size(),
                Instant.now(),
                positions);
    }

    public List<AllocationResponse> getAllocation() {
        return getSummary().positions().stream()
                .map(p -> new AllocationResponse(
                        p.symbol(), p.name(), p.currentValue(), p.allocationPercent()))
                .toList();
    }

    /**
     * Agrupa por ativo e aplica as transações em sequência.
     * Laço explícito, e não Stream: o custo médio é um cálculo dependente
     * de ordem, com estado mutável — forçar Stream aqui só prejudicaria a leitura.
     */
    private Map<Asset, PositionAccumulator> accumulate(List<Transaction> transactions) {
        Map<Asset, PositionAccumulator> accumulators = new LinkedHashMap<>();

        for (Transaction transaction : transactions) {
            accumulators
                    .computeIfAbsent(transaction.getAsset(), PositionAccumulator::new)
                    .apply(transaction);
        }
        return accumulators;
    }

    // Busca as cotações de todas as posições abertas em uma única chamada e combina cada uma com sua posição
    private List<ValuedPosition> valuate(List<PositionAccumulator> openPositions) {
        Set<String> coingeckoIds = openPositions.stream()
                .map(p -> p.asset().getCoingeckoId())
                .collect(Collectors.toSet());

        Map<String, PriceQuote> quotes = priceService.getPrices(coingeckoIds);

        return openPositions.stream()
                .map(position -> {
                    PriceQuote quote = quotes.get(position.asset().getCoingeckoId());
                    if (quote == null) {
                        throw new ExternalServiceException(
                                "Cotação indisponível para " + position.asset().getSymbol());
                    }
                    return new ValuedPosition(position, quote.price(),
                            quote.price().multiply(position.quantity()));
                })
                .sorted(Comparator.comparing(ValuedPosition::currentValue).reversed())
                .toList();
    }

    private PositionResponse toPositionResponse(ValuedPosition valued, BigDecimal totalCurrentValue) {
        PositionAccumulator position = valued.position();
        BigDecimal invested = position.costBasis();
        BigDecimal unrealizedPnl = valued.currentValue().subtract(invested);

        return new PositionResponse(
                position.asset().getSymbol(),
                position.asset().getName(),
                normalize(position.quantity()),
                position.averageCost().setScale(PRICE_SCALE, ROUNDING),
                valued.currentPrice().setScale(PRICE_SCALE, ROUNDING),
                money(invested),
                money(valued.currentValue()),
                money(unrealizedPnl),
                percent(unrealizedPnl, invested),
                percent(valued.currentValue(), totalCurrentValue));
    }

    private PortfolioSummaryResponse emptySummary(String currency) {
        BigDecimal zeroMoney = BigDecimal.ZERO.setScale(MONEY_SCALE);
        BigDecimal zeroPercent = BigDecimal.ZERO.setScale(PERCENT_SCALE);

        return new PortfolioSummaryResponse(currency, zeroMoney, zeroMoney, zeroMoney,
                zeroPercent, zeroMoney, zeroMoney, 0, Instant.now(), List.of());
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, ROUNDING);
    }

    private BigDecimal percent(BigDecimal part, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO.setScale(PERCENT_SCALE);
        }
        return part.multiply(HUNDRED).divide(total, PERCENT_SCALE, ROUNDING);
    }

    // Remove zeros à direita para exibição, protegendo contra notação científica:
    // "100".stripTrailingZeros() vira 1E+2, o que sairia errado no JSON.
    private BigDecimal normalize(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }
}