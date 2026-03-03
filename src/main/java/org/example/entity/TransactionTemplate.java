package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.ToString;
import org.example.entity.enums.CurrencyCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "transaction_templates")
@Getter
@ToString(exclude = {"account", "defaultAccount"})
public class TransactionTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transaction_templ_id_generator")
    @SequenceGenerator(name = "transaction_templ_id_generator",
            sequenceName = "transaction_templs_id_seq",
            allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;  // Владелец шаблона (чей это шаблон)

    @Column(name = "beneficiary_name")
    private String beneficiaryName;

    @Column(name = "beneficiary_account_number")
    private String beneficiaryAccountNumber;

    @Column(name = "beneficiary_bank")
    private String beneficiaryBank;

    @Column(name = "beneficiary_bic")
    private String beneficiaryBic;

    @Column(name = "beneficiary_inn")
    private String beneficiaryInn;

    @Column(name = "amount")
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency_code")
    private CurrencyCode currencyCode;

    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_account_id")
    private Account defaultAccount; // Счёт списания по умолчанию (может быть null)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "usage_count")
    private Long usageCount;

    @Column(name = "name", nullable = false)
    private String name; // название шаблона ("Коммуналка", "Маме")

    @Version
    private Long version;

    protected TransactionTemplate() {}

    private TransactionTemplate(Builder builder) {
        this.account = builder.account;
        this.name = builder.name;
        this.beneficiaryName = builder.beneficiaryName;
        this.beneficiaryAccountNumber = builder.beneficiaryAccountNumber;
        this.beneficiaryBank = builder.beneficiaryBank;
        this.beneficiaryBic = builder.beneficiaryBic;
        this.beneficiaryInn = builder.beneficiaryInn;
        this.amount = builder.amount;
        this.currencyCode = builder.currencyCode;
        this.description = builder.description;
        this.defaultAccount = builder.defaultAccount;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.lastUsedAt = builder.lastUsedAt;
        this.usageCount = builder.usageCount != null ? builder.usageCount : 0L;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionTemplate)) return false;
        TransactionTemplate that = (TransactionTemplate) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public void incrementUsageCount() {
        this.usageCount++;
        this.lastUsedAt = LocalDateTime.now();
    }

    public boolean hasFixedAmount() {
        return amount != null;
    }

    public boolean hasDefaultAccount() {
        return defaultAccount != null;
    }

    public static class Builder {
        private Account account;
        private String name;
        private String beneficiaryName;
        private String beneficiaryAccountNumber;
        private String beneficiaryBank;
        private String beneficiaryBic;
        private String beneficiaryInn;
        private BigDecimal amount;
        private CurrencyCode currencyCode;
        private String description;
        private Account defaultAccount;
        private LocalDateTime createdAt;
        private LocalDateTime lastUsedAt;
        private Long usageCount;

        public Builder account(Account account) {
            this.account = account;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder beneficiaryName(String beneficiaryName) {
            this.beneficiaryName = beneficiaryName;
            return this;
        }

        public Builder beneficiaryAccountNumber(String beneficiaryAccountNumber) {
            this.beneficiaryAccountNumber = beneficiaryAccountNumber;
            return this;
        }

        public Builder beneficiaryBank(String beneficiaryBank) {
            this.beneficiaryBank = beneficiaryBank;
            return this;
        }

        public Builder beneficiaryBic(String beneficiaryBic) {
            this.beneficiaryBic = beneficiaryBic;
            return this;
        }

        public Builder beneficiaryInn(String beneficiaryInn) {
            this.beneficiaryInn = beneficiaryInn;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder currencyCode(CurrencyCode currencyCode) {
            this.currencyCode = currencyCode;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultAccount(Account defaultAccount) {
            this.defaultAccount = defaultAccount;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder lastUsedAt(LocalDateTime lastUsedAt) {
            this.lastUsedAt = lastUsedAt;
            return this;
        }

        public Builder usageCount(Long usageCount) {
            this.usageCount = usageCount;
            return this;
        }

        public TransactionTemplate build() {
            if (account == null) {
                throw new IllegalStateException("account не должен быть пустым");
            }
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalStateException("name не должен быть пустым");
            }
            return new TransactionTemplate(this);
        }
    }
}