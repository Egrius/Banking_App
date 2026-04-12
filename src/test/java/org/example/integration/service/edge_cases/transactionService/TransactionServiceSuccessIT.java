package org.example.integration.service.edge_cases.transactionService;

import jakarta.persistence.EntityManager;
import org.example.dto.transation.TransactionCreateDto;
import org.example.dto.transation.TransactionReadDto;
import org.example.entity.*;
import org.example.entity.enums.ActionType;
import org.example.entity.enums.EntityType;
import org.example.entity.enums.TransactionStatus;
import org.example.integration.config.AbstractTransactionServiceIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionServiceSuccessIT extends AbstractTransactionServiceIntegrationTest {

    private final Long FROM_ACCOUNT_ID = 1L;
    private final Long TO_ACCOUNT_ID = 2L;

    private Account fromAccount;
    private Account toAccount;


    @BeforeEach
    void setup() {
        // Очистить таблицы юзеров, транзакции, логов для транзакций.
        // Добавить тестовых пользователей с тестовым счетом.
        runSql("test_transfer_data.sql");

        fromAccount = findAccountById(FROM_ACCOUNT_ID).orElseThrow();
        toAccount = findAccountById(TO_ACCOUNT_ID).orElseThrow();

        System.out.println("----- ПОСЛЕ setup() -----");
    }

    @Test
    void transfer_shouldCompleteCorrect_whenAccountsFound_andNoConflicts() {

        // Нужно проверить, что транзакция создана в бд со статусом Pending, но там нет задержек, поэтому это не сделать
        // Нужно убедиться что по итогу создана запись в транзакции со статусом успеха
        // Убедиться, что записан лог после выталкивания
        // Убедиться, что есть лог по балансу на счете
        // Убедиться, что ключ создан и что у него срок действия не истек. Потом в другом тесте нужно шаманить с ключом и повторным переводом

        BigDecimal MONEY_TO_TRANSFER = BigDecimal.valueOf(250.0);

        BigDecimal fromMoneyBefore = fromAccount.getBalance();
        BigDecimal toMoneyBefore = toAccount.getBalance();

        UUID IDEMPOTENCY_KEY_FROM_CLIENT = UUID.randomUUID();

        TransactionCreateDto transactionCreateDto = new TransactionCreateDto(
                FROM_ACCOUNT_ID,
                TO_ACCOUNT_ID,
                fromAccount.getCurrencyCode(),
                MONEY_TO_TRANSFER,
                "",
                IDEMPOTENCY_KEY_FROM_CLIENT
        );

        System.out.println("\n ----- ДО ПЕРЕВОДА ----- \n");

        TransactionReadDto transactionReadDto = transactionService.transfer(transactionCreateDto);

        System.out.println("\n ----- ПОСЛЕ ПЕРЕВОДА ----- \n");

        assertEquals(TransactionStatus.SUCCESS, transactionReadDto.status());
        assertNotEquals(transactionReadDto.fromBalanceBefore(), transactionReadDto.fromBalanceAfter());
        assertNotEquals(transactionReadDto.toBalanceBefore(), transactionReadDto.toBalanceAfter());

        // Проврека соответсвия в бд

        try(EntityManager em = sessionFactory.createEntityManager();) {
            BankTransaction bankTransaction = em.find(BankTransaction.class, transactionReadDto.id());

            assertEquals(FROM_ACCOUNT_ID, bankTransaction.getFromAccount().getId());
            assertEquals(TO_ACCOUNT_ID, bankTransaction.getToAccount().getId());
            assertEquals(transactionReadDto.status(), bankTransaction.getStatus());

            assertEquals(fromMoneyBefore.subtract(MONEY_TO_TRANSFER), bankTransaction.getFromAccount().getBalance());
            assertEquals(toMoneyBefore.add(MONEY_TO_TRANSFER), bankTransaction.getToAccount().getBalance());
            assertNotNull(bankTransaction.getProcessedAt());

            // Проверка создания ключа

            List<IdempotencyKey> keys = em.createQuery("SELECT ik FROM IdempotencyKey ik WHERE ik.transaction.id = :transactionId", IdempotencyKey.class)
                    .setParameter("transactionId", bankTransaction.getId())
                    .getResultList();

            assertEquals(1, keys.size());

            IdempotencyKey key = keys.getFirst();

            assertEquals(IDEMPOTENCY_KEY_FROM_CLIENT, key.getKey());
            assertFalse(key.getExpiresAt().isBefore(LocalDateTime.now()));

            // Проверка аудита для выталкивания

            List<AuditLog> transactionAudits = em.createQuery("SELECT aud FROM AuditLog aud WHERE aud.entityType = :entityType AND aud.entityId = :transactionId ORDER BY aud.id", AuditLog.class)
                    .setParameter("entityType", EntityType.TRANSACTION)
                    .setParameter("transactionId", bankTransaction.getId())
                    .getResultList();

            assertEquals(2, transactionAudits.size());
            assertEquals(ActionType.TRANSFER_INITIATE, transactionAudits.get(0).getActionType());
            assertEquals(ActionType.TRANSFER_COMPLETE, transactionAudits.get(1).getActionType());

            // Проверка аудита для обоих счетов

            fromAccount = em.find(Account.class, fromAccount.getId());
            toAccount = em.find(Account.class, toAccount.getId());

            BigDecimal fromMoneyAfter = fromAccount.getBalance();
            BigDecimal toMoneyAfter = toAccount.getBalance();

            List<AccountBalanceAudit> fromBalanceAudits = em.createQuery("SELECT aud FROM AccountBalanceAudit aud WHERE aud.account.id = :accountId", AccountBalanceAudit.class)
                    .setParameter("accountId", fromAccount.getId())
                    .getResultList();

            assertEquals(1, fromBalanceAudits.size());
            assertEquals(fromMoneyBefore, fromBalanceAudits.getFirst().getBalanceBefore());
            assertEquals(fromMoneyAfter, fromBalanceAudits.getFirst().getBalanceAfter());
            assertEquals(fromMoneyAfter.subtract(fromMoneyBefore), fromBalanceAudits.getFirst().getChangeAmount());

            List<AccountBalanceAudit> toBalanceAudits = em.createQuery("SELECT aud FROM AccountBalanceAudit aud WHERE aud.account.id = :accountId", AccountBalanceAudit.class)
                    .setParameter("accountId", toAccount.getId())
                    .getResultList();

            assertEquals(1, toBalanceAudits.size());
            assertEquals(toMoneyBefore, toBalanceAudits.getFirst().getBalanceBefore());
            assertEquals(toMoneyAfter, toBalanceAudits.getFirst().getBalanceAfter());
            assertEquals(toMoneyAfter.subtract(toMoneyBefore), toBalanceAudits.getFirst().getChangeAmount());
        }
    }

    @Test
    void transfer_shouldNotChangeMoneyAgainWithSameIdempotencyKey() {
        BigDecimal MONEY_TO_TRANSFER = BigDecimal.valueOf(250.0);

        BigDecimal fromMoneyBefore = fromAccount.getBalance();
        BigDecimal toMoneyBefore = toAccount.getBalance();

        UUID IDEMPOTENCY_KEY_FROM_CLIENT = UUID.randomUUID();

        TransactionCreateDto transactionCreateDto = new TransactionCreateDto(
                FROM_ACCOUNT_ID,
                TO_ACCOUNT_ID,
                fromAccount.getCurrencyCode(),
                MONEY_TO_TRANSFER,
                "",
                IDEMPOTENCY_KEY_FROM_CLIENT
        );

        System.out.println("\n ----- ^ ДО ПЕРЕВОДА ^ ----- \n");

        TransactionReadDto transactionReadDto = transactionService.transfer(transactionCreateDto);

        System.out.println("\n ----- ПОСЛЕ ПЕРЕВОДА ----- \n");

        assertEquals(TransactionStatus.SUCCESS, transactionReadDto.status());
        assertNotEquals(transactionReadDto.fromBalanceBefore(), transactionReadDto.fromBalanceAfter());
        assertNotEquals(transactionReadDto.toBalanceBefore(), transactionReadDto.toBalanceAfter());


        BigDecimal fromMoneyAfterFirst;
        BigDecimal toMoneyAfterFirst;

        try(EntityManager em = sessionFactory.createEntityManager();) {
            fromAccount = em.find(Account.class, fromAccount.getId());
            toAccount = em.find(Account.class, toAccount.getId());

            fromMoneyAfterFirst = fromAccount.getBalance();
            toMoneyAfterFirst = toAccount.getBalance();
        }


        // Повторный перевод

        System.out.println("\n ----- ^ ДО ПЕРЕВОДА (2) ^ ----- \n");

        // Ключ тот же!
        // Ключ еще не истёк по времени и указывает на успешную транзакцию
        TransactionReadDto transactionReadDto_2 = transactionService.transfer(transactionCreateDto);

        System.out.println("\n ----- ПОСЛЕ ПЕРЕВОДА (2) ----- \n");

        // Проверка, что получили ту же транзакцию и новая не создалась
        assertEquals(transactionReadDto.id(), transactionReadDto_2.id());

        try(EntityManager em = sessionFactory.createEntityManager();) {
            assertEquals(1, em.createQuery("SELECT t FROM BankTransaction t", BankTransaction.class).getResultList().size());

        }

        // Проверка, что деньги не поменялись
        BigDecimal fromMoneyAfterSecond;
        BigDecimal toMoneyAfterSecond;

        try(EntityManager em = sessionFactory.createEntityManager();) {
            fromAccount = em.find(Account.class, fromAccount.getId());
            toAccount = em.find(Account.class, toAccount.getId());

            fromMoneyAfterSecond = fromAccount.getBalance();
            toMoneyAfterSecond = toAccount.getBalance();
        }

        assertEquals(fromMoneyAfterFirst, fromMoneyAfterSecond);
        assertEquals(toMoneyAfterFirst, toMoneyAfterSecond);
    }

    @Test
    void transfer_shouldSetProcessedAt_whenSuccessful() {
        TransactionCreateDto dto = new TransactionCreateDto(
                FROM_ACCOUNT_ID, TO_ACCOUNT_ID, fromAccount.getCurrencyCode(),
                BigDecimal.valueOf(50), "", UUID.randomUUID()
        );

        LocalDateTime before = LocalDateTime.now();
        TransactionReadDto result = transactionService.transfer(dto);
        LocalDateTime after = LocalDateTime.now();

        try (EntityManager em = sessionFactory.createEntityManager()) {
            BankTransaction bt = em.find(BankTransaction.class, result.id());
            assertNotNull(bt.getProcessedAt());
            assertTrue(bt.getProcessedAt().isAfter(before) || bt.getProcessedAt().equals(before));
            assertTrue(bt.getProcessedAt().isBefore(after) || bt.getProcessedAt().equals(after));
        }
    } 

    @Test
    void transfer_shouldSaveDescription_whenDescriptionProvided() {
        String description = "Test transfer description with special chars: !@#$%";
        TransactionCreateDto dto = new TransactionCreateDto(
                FROM_ACCOUNT_ID, TO_ACCOUNT_ID, fromAccount.getCurrencyCode(),
                BigDecimal.valueOf(100), description, UUID.randomUUID()
        );

        TransactionReadDto result = transactionService.transfer(dto);

        try (EntityManager em = sessionFactory.createEntityManager()) {
            BankTransaction bt = em.find(BankTransaction.class, result.id());
            assertEquals(description, bt.getDescription());
        }
    }

}
