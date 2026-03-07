package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.ToString;
import org.example.entity.enums.AccountType;
import org.example.entity.enums.CurrencyCode;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

@Entity
@Table(name = "accounts")
@Getter
@ToString(exclude = {"user"})
public class Account {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "accounts_seq")
    @SequenceGenerator(name = "accounts_seq", sequenceName = "accounts_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "balance", nullable = false)
    private BigDecimal balance;

    @Column(name = "currency_code", nullable = false)
    private CurrencyCode currencyCode;

    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Column(name = "opening_date", nullable = false)
    private LocalDateTime openingDate;

    @Column(name = "closing_date")
    private LocalDateTime closingDate;

    @OneToMany(mappedBy = "account",
            fetch = FetchType.LAZY,
            cascade = CascadeType.REMOVE,
            orphanRemoval = true)
    private Collection<TransactionTemplate> transactionTemplates = new ArrayList<>();

    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Collection<AccountBalanceAudit> balanceAudits = new ArrayList<>();

    @OneToMany(mappedBy = "fromAccount")
    private Collection<BankTransaction> outgoingTransactions = new ArrayList<>();

    @OneToMany(mappedBy = "toAccount")
    private Collection<BankTransaction> incomingTransactions = new ArrayList<>();

    @Version
    private Long version;

    protected Account() {}

    public Account(User user, String accountNumber, BigDecimal balance, CurrencyCode currencyCode,  AccountType accountType) {
        this.user = user;
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.currencyCode = currencyCode;
        this.accountType = accountType;
        this.openingDate = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account)) return false;
        Account account = (Account) o;
        return Objects.equals(getAccountNumber(), account.getAccountNumber());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAccountNumber());
    }
}
