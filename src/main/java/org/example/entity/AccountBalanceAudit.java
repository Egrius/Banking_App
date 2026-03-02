package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

// TODO возможно добавить поле с причиной изменения

@Entity
@Table(name = "account_balance_audit")
@Getter
@ToString(exclude = {"account", "transaction"})
public class AccountBalanceAudit {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "audit_seq")
    @SequenceGenerator(name = "audit_seq", sequenceName = "audit_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "balance_before", nullable = false)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false)
    private BigDecimal balanceAfter;

    @Column(name = "change_amount", nullable = false)
    private BigDecimal changeAmount;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "changed_by_thread")
    private String changedByThread;

    protected AccountBalanceAudit() {}

    public AccountBalanceAudit(Account account, BigDecimal balanceBefore,
                               BigDecimal balanceAfter, String changedByThread) {
        this.account = account;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.changeAmount = balanceAfter.subtract(balanceBefore);
        this.changedAt = LocalDateTime.now();
        this.changedByThread = changedByThread;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccountBalanceAudit)) return false;
        AccountBalanceAudit that = (AccountBalanceAudit) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}