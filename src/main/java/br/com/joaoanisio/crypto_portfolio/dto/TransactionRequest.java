package br.com.joaoanisio.crypto_portfolio.dto;

import br.com.joaoanisio.crypto_portfolio.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionRequest(

        @NotBlank(message = "O símbolo do ativo é obrigatório")
        String symbol,

        @NotNull(message = "O tipo da transação é obrigatório (BUY ou SELL)")
        TransactionType type,

        @NotNull(message = "A quantidade é obrigatória")
        @DecimalMin(value = "0", inclusive = false, message = "A quantidade deve ser maior que zero")
        BigDecimal quantity,

        @NotNull(message = "O preço unitário é obrigatório")
        @DecimalMin(value = "0", message = "O preço unitário não pode ser negativo")
        BigDecimal unitPrice,

        @NotNull(message = "A data de execução é obrigatória")
        @PastOrPresent(message = "A data de execução não pode estar no futuro")
        Instant executedAt

) {}
