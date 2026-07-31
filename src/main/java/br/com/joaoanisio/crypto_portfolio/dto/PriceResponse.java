package br.com.joaoanisio.crypto_portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceResponse(
        String symbol,
        String name,
        String currency,
        BigDecimal price,
        Instant fetchedAt
) {}