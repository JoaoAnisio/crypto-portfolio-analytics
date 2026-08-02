package br.com.joaoanisio.crypto_portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PortfolioSummaryResponse(
        String currency,
        BigDecimal totalInvested,
        BigDecimal totalCurrentValue,
        BigDecimal unrealizedPnl,
        BigDecimal unrealizedPnlPercent,
        BigDecimal realizedPnl,
        BigDecimal totalPnl,
        int openPositions,
        Instant calculatedAt,
        List<PositionResponse> positions
) {}