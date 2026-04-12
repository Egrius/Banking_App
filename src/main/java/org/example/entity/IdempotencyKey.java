package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys", uniqueConstraints = {
        @UniqueConstraint(name = "key_uq", columnNames = {"key"})
})
@Getter
@Setter
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
                    generator = "idempotency_key_seq")
    @SequenceGenerator(name = "idempotency_key_seq", sequenceName = "idempotency_key_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "key", nullable = false)
    private UUID key;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id") //
    private BankTransaction transaction;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    protected IdempotencyKey(){}

    public static IdempotencyKey from(UUID key,  BankTransaction transaction, LocalDateTime createdAt) {
        IdempotencyKey newKey = new IdempotencyKey();
        newKey.setKey(key);
        newKey.setTransaction(transaction);
        newKey.setCreatedAt(createdAt);
        return newKey;
    }
}
