package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.ToString;
import org.example.entity.enums.ActionType;
import org.example.entity.enums.AuditStatus;
import org.example.entity.enums.EntityType;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "audit_log")
@Getter
@ToString(exclude = {"user", "transaction"})
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_log_id_generator")
    @SequenceGenerator(name = "audit_log_id_generator",
            sequenceName = "audit_log_id_seq",
            allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "email")
    private String email;

    @Column(name = "user_ip", length = 45)
    private String userIp;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private ActionType actionType;  // LOGIN, TRANSFER, CREATE_TEMPLATE...

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", length = 50)
    private EntityType entityType;  // ACCOUNT, CARD, TRANSACTION, USER, TEMPLATE...

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;  // JSON с данными ДО

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;  // JSON с данными ПОСЛЕ

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AuditStatus status;  // SUCCESS, FAILED, ERROR

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "thread_name", length = 100)
    private String threadName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private BankTransaction transaction;  // ссылка на транзакцию (если есть)

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;  // сколько миллисекунд заняло действие

    @Version
    private Long version; // под вопросом

    protected AuditLog() {}

    private AuditLog(Builder builder) {
        this.user = builder.user;
        this.email = builder.email;
        this.userIp = builder.userIp;
        this.userAgent = builder.userAgent;
        this.actionType = builder.actionType;
        this.entityType = builder.entityType;
        this.entityId = builder.entityId;
        this.oldValue = builder.oldValue;
        this.newValue = builder.newValue;
        this.status = builder.status;
        this.errorMessage = builder.errorMessage;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.threadName = builder.threadName != null ? builder.threadName : Thread.currentThread().getName();
        this.transaction = builder.transaction;
        this.executionTimeMs = builder.executionTimeMs;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuditLog)) return false;
        AuditLog auditLog = (AuditLog) o;
        return Objects.equals(id, auditLog.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }


    public static class Builder {
        private User user;
        private String email;
        private String userIp;
        private String userAgent;
        private ActionType actionType;
        private EntityType entityType;
        private Long entityId;
        private String oldValue;
        private String newValue;
        private AuditStatus status;
        private String errorMessage;
        private LocalDateTime createdAt;
        private String threadName;
        private BankTransaction transaction;
        private Long executionTimeMs;

        public Builder user(User user) {
            this.user = user;
            // автоматически заполняем username из user, если он есть
            if (user != null && this.email == null) {
                this.email = user.getEmail();
            }
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder username(String email) {
            this.email = email;
            return this;
        }

        public Builder userIp(String userIp) {
            this.userIp = userIp;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder actionType(ActionType actionType) {
            this.actionType = actionType;
            return this;
        }

        public Builder entityType(EntityType entityType) {
            this.entityType = entityType;
            return this;
        }

        public Builder entityId(Long entityId) {
            this.entityId = entityId;
            return this;
        }

        public Builder oldValue(String oldValue) {
            this.oldValue = oldValue;
            return this;
        }

        public Builder newValue(String newValue) {
            this.newValue = newValue;
            return this;
        }

        public Builder status(AuditStatus status) {
            this.status = status;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder threadName(String threadName) {
            this.threadName = threadName;
            return this;
        }

        public Builder transaction(BankTransaction transaction) {
            this.transaction = transaction;
            return this;
        }

        public Builder executionTimeMs(Long executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
            return this;
        }

        public AuditLog build() {
            if (actionType == null) {
                throw new IllegalStateException("actionType must not be empty");
            }
            if (status == null) {
                throw new IllegalStateException("status must not be null");
            }
            return new AuditLog(this);
        }
    }

}