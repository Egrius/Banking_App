package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.example.entity.enums.CardStatus;
import org.example.entity.enums.CardType;
import org.example.entity.enums.CurrencyCode;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "cards")
@Getter
@Setter
@ToString(exclude = {"user", "account"})
public class Card {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "cards_seq")
    @SequenceGenerator(name = "cards_seq", sequenceName = "cards_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "card_number", nullable = false, unique = true)
    private String cardNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency_code", nullable = false)
    private CurrencyCode currencyCode;

    @Column(name = "cardholder_name", nullable = false)
    private String cardholderName;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false)
    private CardType cardType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CardStatus status;

    @Column(name = "name")
    private String name;

    @Version
    private Long version;

    protected Card() {}

    private Card(Builder builder) {
        this.user = builder.user;
        this.account = builder.account;
        this.cardNumber = builder.cardNumber;
        this.currencyCode = builder.currencyCode;
        this.cardholderName = builder.cardholderName;
        this.expiryDate = builder.expiryDate;
        this.cardType = builder.cardType;
        this.status = builder.status != null ? builder.status : CardStatus.ACTIVE;
        this.name = builder.name;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Card)) return false;
        Card card = (Card) o;
        if (this.getId() != null && card.getId() != null) {
            return this.getId().equals(card.getId());
        }
        return Objects.equals(this.getCardNumber(), card.getCardNumber());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getCardNumber());
    }

    public static class Builder {
        private User user;
        private Account account;
        private String cardNumber;
        private CurrencyCode currencyCode;
        private String cardholderName;
        private LocalDateTime expiryDate;
        private CardType cardType;
        private CardStatus status;
        private String name;

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder account(Account account) {
            this.account = account;
            return this;
        }

        public Builder cardNumber(String cardNumber) {
            this.cardNumber = cardNumber;
            return this;
        }

        public Builder currencyCode(CurrencyCode currencyCode) {
            this.currencyCode = currencyCode;
            return this;
        }

        public Builder cardholderName(String cardholderName) {
            this.cardholderName = cardholderName;
            return this;
        }

        public Builder expiryDate(LocalDateTime expiryDate) {
            this.expiryDate = expiryDate;
            return this;
        }

        public Builder cardType(CardType cardType) {
            this.cardType = cardType;
            return this;
        }

        public Builder status(CardStatus status) {
            this.status = status;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Card build() {
            return new Card(this);
        }
    }
}