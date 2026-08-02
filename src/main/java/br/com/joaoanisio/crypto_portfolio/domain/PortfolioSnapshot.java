package br.com.joaoanisio.crypto_portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "portfolio_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "snapshot_date", nullable = false, unique = true)
    private LocalDate snapshotDate;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "total_invested", nullable = false, precision = 20, scale = 2)
    private BigDecimal totalInvested;

    @Column(name = "total_current_value", nullable = false, precision = 20, scale = 2)
    private BigDecimal totalCurrentValue;

    @Column(name = "unrealized_pnl", nullable = false, precision = 20, scale = 2)
    private BigDecimal unrealizedPnl;

    @Column(name = "realized_pnl", nullable = false, precision = 20, scale = 2)
    private BigDecimal realizedPnl;

    @Column(name = "open_positions", nullable = false)
    private int openPositions;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        PortfolioSnapshot other = (PortfolioSnapshot) o;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}