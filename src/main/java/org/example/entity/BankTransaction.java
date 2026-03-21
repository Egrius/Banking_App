package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.ToString;
import org.example.entity.enums.Status;
import org.example.entity.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "transactions")
@Getter
@ToString(exclude = {"fromAccount", "toAccount"})
public class BankTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transaction_id_generator")
    @SequenceGenerator(name = "transaction_id_generator", sequenceName = "transactions_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id", nullable = false)
    private Account fromAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id", nullable = false)
    private Account toAccount;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "from_balance_before", nullable = false)
    private BigDecimal fromBalanceBefore;

    @Column(name = "from_balance_after", nullable = false)
    private BigDecimal fromBalanceAfter;

    @Column(name = "to_balance_before", nullable = false)
    private BigDecimal toBalanceBefore;

    @Column(name = "to_balance_after", nullable = false)
    private BigDecimal toBalanceAfter;

    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "description")
    private String description;

    protected BankTransaction() {}

    private BankTransaction(Builder builder) {
        this.fromAccount = builder.fromAccount;
        this.toAccount = builder.toAccount;
        this.amount = builder.amount;
        this.status = builder.status;
        this.failureReason = builder.failureReason;
        this.retryCount = builder.retryCount != null ? builder.retryCount : 0;
        this.fromBalanceBefore = builder.fromBalanceBefore;
        this.fromBalanceAfter = builder.fromBalanceAfter;
        this.toBalanceBefore = builder.toBalanceBefore;
        this.toBalanceAfter = builder.toBalanceAfter;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.processedAt = builder.processedAt;
        this.description = builder.description;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Account fromAccount;
        private Account toAccount;
        private BigDecimal amount;
        private TransactionStatus status;
        private String failureReason;
        private Integer retryCount;
        private BigDecimal fromBalanceBefore;
        private BigDecimal fromBalanceAfter;
        private BigDecimal toBalanceBefore;
        private BigDecimal toBalanceAfter;
        private LocalDateTime createdAt;
        private LocalDateTime processedAt;
        private String description;

        public Builder fromAccount(Account fromAccount) {
            this.fromAccount = fromAccount;
            return this;
        }

        public Builder toAccount(Account toAccount) {
            this.toAccount = toAccount;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder status(TransactionStatus status) {
            this.status = status;
            return this;
        }

        public Builder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        public Builder retryCount(Integer retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public Builder fromBalanceBefore(BigDecimal fromBalanceBefore) {
            this.fromBalanceBefore = fromBalanceBefore;
            return this;
        }

        public Builder fromBalanceAfter(BigDecimal fromBalanceAfter) {
            this.fromBalanceAfter = fromBalanceAfter;
            return this;
        }

        public Builder toBalanceBefore(BigDecimal toBalanceBefore) {
            this.toBalanceBefore = toBalanceBefore;
            return this;
        }

        public Builder toBalanceAfter(BigDecimal toBalanceAfter) {
            this.toBalanceAfter = toBalanceAfter;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder processedAt(LocalDateTime processedAt) {
            this.processedAt = processedAt;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public BankTransaction build() {
            return new BankTransaction(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BankTransaction that)) return false;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
