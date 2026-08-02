package br.com.joaoanisio.crypto_portfolio.service;

import br.com.joaoanisio.crypto_portfolio.domain.Asset;
import br.com.joaoanisio.crypto_portfolio.domain.Transaction;
import br.com.joaoanisio.crypto_portfolio.domain.TransactionType;
import br.com.joaoanisio.crypto_portfolio.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Acumula o estado de uma posição aplicando transações em ordem cronológica,
 * pelo método de custo médio ponderado.
 *
 * Regra de compra: soma quantidade e soma custo — o custo médio se dilui ou sobe.
 * Regra de venda:  o resultado realizado é a diferença entre o valor recebido
 *                  e o custo médio da parcela vendida. O custo médio NÃO muda.
 */
class PositionAccumulator {

    private static final int CALC_SCALE = 18;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final Asset asset;

    private BigDecimal quantity = BigDecimal.ZERO;
    private BigDecimal costBasis = BigDecimal.ZERO;
    private BigDecimal realizedPnl = BigDecimal.ZERO;

    PositionAccumulator(Asset asset) {
        this.asset = asset;
    }

    void apply(Transaction transaction) {
        if (transaction.getType() == TransactionType.BUY) {
            applyBuy(transaction);
        } else {
            applySell(transaction);
        }
    }

    private void applyBuy(Transaction transaction) {
        quantity = quantity.add(transaction.getQuantity());
        costBasis = costBasis.add(transaction.totalValue());
    }

    private void applySell(Transaction transaction) {
        BigDecimal soldQuantity = transaction.getQuantity();

        if (soldQuantity.compareTo(quantity) > 0) {
            throw new BusinessException(
                    "Histórico inconsistente para %s: venda de %s com posição de apenas %s"
                            .formatted(asset.getSymbol(),
                                    soldQuantity.toPlainString(),
                                    quantity.toPlainString()));
        }

        BigDecimal costOfSold = averageCost().multiply(soldQuantity);

        realizedPnl = realizedPnl.add(transaction.totalValue().subtract(costOfSold));
        quantity = quantity.subtract(soldQuantity);
        costBasis = costBasis.subtract(costOfSold);

        // Posição zerada: elimina resíduo de arredondamento do custo médio.
        if (quantity.signum() == 0) {
            costBasis = BigDecimal.ZERO;
        }
    }

    BigDecimal averageCost() {
        if (quantity.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return costBasis.divide(quantity, CALC_SCALE, ROUNDING);
    }

    boolean hasOpenPosition() {
        return quantity.signum() > 0;
    }

    Asset asset() {
        return asset;
    }

    BigDecimal quantity() {
        return quantity;
    }

    BigDecimal costBasis() {
        return costBasis;
    }

    BigDecimal realizedPnl() {
        return realizedPnl;
    }
}