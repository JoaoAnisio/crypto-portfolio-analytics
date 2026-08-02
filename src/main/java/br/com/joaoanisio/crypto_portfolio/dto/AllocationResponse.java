package br.com.joaoanisio.crypto_portfolio.dto;

import java.math.BigDecimal;

public record AllocationResponse(
        String symbol,
        String name,
        BigDecimal currentValue,
        BigDecimal percent
) {}