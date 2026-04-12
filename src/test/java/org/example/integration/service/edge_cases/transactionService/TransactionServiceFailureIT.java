package org.example.integration.service.edge_cases.transactionService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.EntityTransaction;
import org.example.dto.transation.TransactionCreateDto;
import org.example.entity.Account;
import org.example.entity.enums.Status;
import org.example.integration.config.AbstractTransactionServiceIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionServiceFailureIT extends AbstractTransactionServiceIntegrationTest {

    private final Long FROM_ACCOUNT_ID = 1L;
    private final Long TO_ACCOUNT_ID = 2L;
    private final Long NON_EXISTENT_ACCOUNT_ID = 999L;

    private Account fromAccount;
    private Account toAccount;

    @BeforeEach
    void setup() {
        runSql("test_transfer_data.sql");
        fromAccount = findAccountById(FROM_ACCOUNT_ID).orElseThrow();
        toAccount = findAccountById(TO_ACCOUNT_ID).orElseThrow();
    }

    @Test
    void transfer_shouldThrowException_whenFromAccountNotFound() {
        TransactionCreateDto dto = new TransactionCreateDto(
                NON_EXISTENT_ACCOUNT_ID, TO_ACCOUNT_ID,
                fromAccount.getCurrencyCode(), BigDecimal.valueOf(100), "", UUID.randomUUID()
        );

        Exception exception = assertThrows(EntityNotFoundException.class,
                () -> transactionService.transfer(dto));
        assertTrue(exception.getMessage().contains("не найден"));
    }

    @Test
    void transfer_shouldThrowException_whenToAccountNotFound() {
        TransactionCreateDto dto = new TransactionCreateDto(
                FROM_ACCOUNT_ID, NON_EXISTENT_ACCOUNT_ID,
                fromAccount.getCurrencyCode(), BigDecimal.valueOf(100), "", UUID.randomUUID()
        );

        Exception exception = assertThrows(EntityNotFoundException.class,
                () -> transactionService.transfer(dto));
        assertTrue(exception.getMessage().contains("не найден"));
    }

    @Test
    void transfer_shouldThrowException_whenFromAccountBlocked() {
        // Блокируем счёт
        try (EntityManager em = sessionFactory.createEntityManager()) {
            em.getTransaction().begin();
            Account account = em.find(Account.class, FROM_ACCOUNT_ID);
            account.setStatus(Status.BLOCKED);
            em.merge(account);
            em.getTransaction().commit();
        }

        TransactionCreateDto dto = new TransactionCreateDto(
                FROM_ACCOUNT_ID, TO_ACCOUNT_ID,
                fromAccount.getCurrencyCode(), BigDecimal.valueOf(100), "", UUID.randomUUID()
        );

        Exception exception = assertThrows(IllegalStateException.class,
                () -> transactionService.transfer(dto));
        assertTrue(exception.getMessage().contains("Нельзя выполнить транзакцию со счёта со статусом"));
    }

    @Test
    void transfer_shouldThrowException_whenToAccountBlocked() {
        try (EntityManager em = sessionFactory.createEntityManager()) {
            em.getTransaction().begin();
            Account account = em.find(Account.class, TO_ACCOUNT_ID);
            account.setStatus(Status.BLOCKED);
            em.merge(account);
            em.getTransaction().commit();
        }

        TransactionCreateDto dto = new TransactionCreateDto(
                FROM_ACCOUNT_ID, TO_ACCOUNT_ID,
                fromAccount.getCurrencyCode(), BigDecimal.valueOf(100), "", UUID.randomUUID()
        );

        Exception exception = assertThrows(IllegalStateException.class,
                () -> transactionService.transfer(dto));
        assertTrue(exception.getMessage().contains("Нельзя перевести средства"));
    }

    @Test
    void transfer_shouldThrowException_whenFromAccountClosed() {
        try (EntityManager em = sessionFactory.createEntityManager()) {
            em.getTransaction().begin();
            Account account = em.find(Account.class, FROM_ACCOUNT_ID);
            account.setStatus(Status.CLOSED);
            em.merge(account);
            em.getTransaction().commit();
        }

        TransactionCreateDto dto = new TransactionCreateDto(
                FROM_ACCOUNT_ID, TO_ACCOUNT_ID,
                fromAccount.getCurrencyCode(), BigDecimal.valueOf(100), "", UUID.randomUUID()
        );

        Exception exception = assertThrows(IllegalStateException.class,
                () -> transactionService.transfer(dto));
        assertTrue(exception.getMessage().contains("Нельзя выполнить транзакцию со счёта со статусом"));
    }

    @Test
    void transfer_shouldThrowException_whenInsufficientFunds() {

        try (EntityManager em = sessionFactory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();

            tx.begin();
            em.createQuery("UPDATE Account a SET a.balance = 200 WHERE a.id = :id")
                    .setParameter("id", fromAccount.getId())
                    .executeUpdate();
            tx.commit();

            fromAccount = em.find(Account.class, fromAccount.getId());

        }

        BigDecimal amount = fromAccount.getBalance().add(BigDecimal.ONE);
        TransactionCreateDto dto = new TransactionCreateDto(
                FROM_ACCOUNT_ID, TO_ACCOUNT_ID,
                fromAccount.getCurrencyCode(), amount, "", UUID.randomUUID()
        );

        Exception exception = assertThrows(IllegalStateException.class,
                () -> transactionService.transfer(dto));
        assertTrue(exception.getMessage().contains("На счёте недостаточно средств для выполнения перевода"));

        // Балансы не изменились
        try (EntityManager em = sessionFactory.createEntityManager()) {
            Account from = em.find(Account.class, FROM_ACCOUNT_ID);
            Account to = em.find(Account.class, TO_ACCOUNT_ID);
            assertEquals(fromAccount.getBalance(), from.getBalance());
            assertEquals(toAccount.getBalance(), to.getBalance());
        }
    }

    @Test
    void transfer_shouldThrowException_whenAmountBelowMinimum() {
        BigDecimal amount = new BigDecimal("4"); // ниже минимума (5)
        TransactionCreateDto dto = new TransactionCreateDto(
                FROM_ACCOUNT_ID, TO_ACCOUNT_ID,
                fromAccount.getCurrencyCode(), amount, "", UUID.randomUUID()
        );

        Exception exception = assertThrows(IllegalStateException.class,
                () -> transactionService.transfer(dto));
        assertTrue(exception.getMessage().contains("Минимальная сумма"));
    }

    @Test
    void transfer_shouldThrowException_whenAmountAboveMaximum() {
        BigDecimal amount = new BigDecimal("800"); // выше максимума (700 для RUB)
        TransactionCreateDto dto = new TransactionCreateDto(
                FROM_ACCOUNT_ID, TO_ACCOUNT_ID,
                fromAccount.getCurrencyCode(), amount, "", UUID.randomUUID()
        );

        Exception exception = assertThrows(IllegalStateException.class,
                () -> transactionService.transfer(dto));
        assertTrue(exception.getMessage().contains("Максимальная сумма"));
    }

    @Test
    void transfer_shouldThrowException_whenCurrenciesMismatch() {
        // Предполагаем, что fromAccount имеет RUB, а toAccount – USD (меняем вручную)
        try (EntityManager em = sessionFactory.createEntityManager()) {
            em.getTransaction().begin();
            Account to = em.find(Account.class, TO_ACCOUNT_ID);
            to.setCurrencyCode(org.example.entity.enums.CurrencyCode.US);
            em.merge(to);
            em.getTransaction().commit();
        }

        TransactionCreateDto dto = new TransactionCreateDto(
                FROM_ACCOUNT_ID, TO_ACCOUNT_ID,
                fromAccount.getCurrencyCode(), BigDecimal.valueOf(100), "", UUID.randomUUID()
        );

        Exception exception = assertThrows(IllegalStateException.class,
                () -> transactionService.transfer(dto));
        assertTrue(exception.getMessage().contains("разновалютными"));
    }

    @Test
    void transfer_shouldThrowException_whenIdempotencyKeyNull() {
        TransactionCreateDto dto = new TransactionCreateDto(
                FROM_ACCOUNT_ID, TO_ACCOUNT_ID,
                fromAccount.getCurrencyCode(), BigDecimal.valueOf(100), "", null
        );

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> transactionService.transfer(dto));
        assertTrue(exception.getMessage().contains("Не передан ключ"));
    }

    @Test
    void transfer_shouldThrowException_whenAmountIsZero() {
        TransactionCreateDto dto = new TransactionCreateDto(
                FROM_ACCOUNT_ID, TO_ACCOUNT_ID,
                fromAccount.getCurrencyCode(), BigDecimal.ZERO, "", UUID.randomUUID()
        );

        // Валидация должна отловить
        Exception exception = assertThrows(Exception.class,
                () -> transactionService.transfer(dto));
        assertTrue(exception.getMessage().contains("Минимальная сумма перевода"));
    }

    @Test
    void transfer_shouldThrowException_whenAmountIsNegative() {
        TransactionCreateDto dto = new TransactionCreateDto(
                FROM_ACCOUNT_ID, TO_ACCOUNT_ID,
                fromAccount.getCurrencyCode(), new BigDecimal("-50"), "", UUID.randomUUID()
        );

        Exception exception = assertThrows(Exception.class,
                () -> transactionService.transfer(dto));
        assertTrue(exception.getMessage().contains("Минимальная сумма перевода"));
    }
}