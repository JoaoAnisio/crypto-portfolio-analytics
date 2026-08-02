package br.com.joaoanisio.crypto_portfolio.repository;

import br.com.joaoanisio.crypto_portfolio.domain.PortfolioSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, UUID> {

    Optional<PortfolioSnapshot> findBySnapshotDate(LocalDate snapshotDate);

    List<PortfolioSnapshot> findAllByOrderBySnapshotDateDesc();

    List<PortfolioSnapshot> findBySnapshotDateBetweenOrderBySnapshotDateAsc(LocalDate from, LocalDate to);
}