package br.com.joaoanisio.crypto_portfolio.repository;

import br.com.joaoanisio.crypto_portfolio.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByAssetIdOrderByExecutedAtDesc(UUID assetId);

    // Carrega todas as transações já com o Asset associado, evitando o problema de N+1 queries na listagem
    @Query("SELECT t FROM Transaction t JOIN FETCH t.asset ORDER BY t.executedAt DESC")
    List<Transaction> findAllWithAsset();

    //Busca por id já trazendo o Asset associado, para permitir a montagem do DTO fora do contexto transacional
    @Query("SELECT t FROM Transaction t JOIN FETCH t.asset WHERE t.id = :id")
    Optional<Transaction> findByIdWithAsset(@Param("id") UUID id);
}
