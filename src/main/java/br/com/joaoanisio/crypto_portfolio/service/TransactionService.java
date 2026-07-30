package br.com.joaoanisio.crypto_portfolio.service;

import br.com.joaoanisio.crypto_portfolio.domain.Asset;
import br.com.joaoanisio.crypto_portfolio.domain.Transaction;
import br.com.joaoanisio.crypto_portfolio.domain.TransactionType;
import br.com.joaoanisio.crypto_portfolio.dto.TransactionRequest;
import br.com.joaoanisio.crypto_portfolio.dto.TransactionResponse;
import br.com.joaoanisio.crypto_portfolio.exception.BusinessException;
import br.com.joaoanisio.crypto_portfolio.exception.ResourceNotFoundException;
import br.com.joaoanisio.crypto_portfolio.repository.AssetRepository;
import br.com.joaoanisio.crypto_portfolio.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AssetRepository assetRepository;

    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        Asset asset = assetRepository.findBySymbolIgnoreCase(request.symbol())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ativo não encontrado para o símbolo: " + request.symbol()));

        if (request.type() == TransactionType.SELL) {
            validateSufficientBalance(asset, request.quantity());
        }

        Transaction transaction = Transaction.builder()
                .asset(asset)
                .type(request.type())
                .quantity(request.quantity())
                .unitPrice(request.unitPrice())
                .executedAt(request.executedAt())
                .build();

        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> findAll() {
        return transactionRepository.findAllWithAsset().stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(UUID id) {
        return transactionRepository.findByIdWithAsset(id)
                .map(TransactionResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transação não encontrada para o id: " + id));
    }

    @Transactional
    public void delete(UUID id) {
        if (!transactionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Transação não encontrada para o id: " + id);
        }
        transactionRepository.deleteById(id);
    }

    //Impede venda de quantidade superior à posição atual do ativo.
    //O saldo é o somatório das compras menos as vendas já registradas.
    private void validateSufficientBalance(Asset asset, BigDecimal quantityToSell) {
        BigDecimal currentBalance = transactionRepository
                .findByAssetIdOrderByExecutedAtDesc(asset.getId()).stream()
                .map(t -> t.getType() == TransactionType.BUY
                        ? t.getQuantity()
                        : t.getQuantity().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (currentBalance.compareTo(quantityToSell) < 0) {
            throw new BusinessException(
                    "Saldo insuficiente de %s. Disponível: %s, solicitado: %s"
                            .formatted(asset.getSymbol(), currentBalance.toPlainString(),
                                    quantityToSell.toPlainString()));
        }
    }
}
