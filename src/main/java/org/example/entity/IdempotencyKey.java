package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys", uniqueConstraints = {
        @UniqueConstraint(name = "key_uq", columnNames = {"key", "transaction", "account", "user"})
})
@Getter
@Setter
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
                    generator = "idempotency_key_seq")
    @SequenceGenerator(name = "idempotency_key_seq", sequenceName = "idempotency_key_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "key")
    private UUID key;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private BankTransaction transaction;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    protected IdempotencyKey(){}
}
