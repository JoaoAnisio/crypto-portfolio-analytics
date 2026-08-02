package br.com.joaoanisio.crypto_portfolio.dto;

import br.com.joaoanisio.crypto_portfolio.domain.PortfolioSnapshot;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SnapshotResponse(
        LocalDate date,
        String currency,
        BigDecimal totalInvested,
        BigDecimal totalCurrentValue,
        BigDecimal unrealizedPnl,
        BigDecimal realizedPnl,
        BigDecimal totalPnl,
        int openPositions
) {

    public static SnapshotResponse from(PortfolioSnapshot snapshot) {
        return new SnapshotResponse(
                snapshot.getSnapshotDate(),
                snapshot.getCurrency(),
                snapshot.getTotalInvested(),
                snapshot.getTotalCurrentValue(),
                snapshot.getUnrealizedPnl(),
                snapshot.getRealizedPnl(),
                snapshot.getUnrealizedPnl().add(snapshot.getRealizedPnl()),
                snapshot.getOpenPositions());
    }
}