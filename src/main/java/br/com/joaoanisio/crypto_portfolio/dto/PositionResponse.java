package br.com.joaoanisio.crypto_portfolio.dto;

import java.math.BigDecimal;

public record PositionResponse(
        String symbol,
        String name,
        BigDecimal quantity,
        BigDecimal averageCost,
        BigDecimal currentPrice,
        BigDecimal investedValue,
        BigDecimal currentValue,
        BigDecimal unrealizedPnl,
        BigDecimal unrealizedPnlPercent,
        BigDecimal allocationPercent
) {}