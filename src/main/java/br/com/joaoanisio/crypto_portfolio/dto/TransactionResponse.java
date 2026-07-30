package br.com.joaoanisio.crypto_portfolio.dto;

import br.com.joaoanisio.crypto_portfolio.domain.Transaction;
import br.com.joaoanisio.crypto_portfolio.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String symbol,
        String assetName,
        TransactionType type,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal totalValue,
        Instant executedAt,
        Instant createdAt
) {

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAsset().getSymbol(),
                transaction.getAsset().getName(),
                transaction.getType(),
                transaction.getQuantity(),
                transaction.getUnitPrice(),
                transaction.totalValue(),
                transaction.getExecutedAt(),
                transaction.getCreatedAt()
        );
    }
}
