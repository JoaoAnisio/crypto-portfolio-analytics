package br.com.joaoanisio.crypto_portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceQuote(
        String coingeckoId,
        String currency,
        BigDecimal price,
        Instant fetchedAt
) {}