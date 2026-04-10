package org.example.integration.service.edge_cases.transactionService;

import jakarta.persistence.EntityManager;
import org.example.dao.AccountDao;
import org.example.dto.idempotency_key.IdempotencyKeyCreateDto;
import org.example.dto.idempotency_key.IdempotencyKeyReadDto;
import org.example.dto.transation.TransactionCreateDto;
import org.example.dto.transation.TransactionReadDto;
import org.example.entity.Account;
import org.example.entity.BankTransaction;
import org.example.entity.enums.TransactionStatus;
import org.example.integration.config.AbstractTransactionServiceIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionServiceSuccessIT extends AbstractTransactionServiceIntegrationTest {

    private final Long FROM_ACCOUNT_ID = 1L;
    private final Long TO_ACCOUNT_ID = 2L;

    private Account fromAccount;
    private Account toAccount;


    @BeforeEach
    void setup() {
        // Очистить таблицы юзеров, транзакции, логов для транзакций.
        // Добавить тестового пользователя с тестовым счетом.
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

        TransactionCreateDto transactionCreateDto = new TransactionCreateDto(
                FROM_ACCOUNT_ID,
                TO_ACCOUNT_ID,
                fromAccount.getCurrencyCode(),
                MONEY_TO_TRANSFER,
                "",
                UUID.randomUUID()
        );

        System.out.println("\n ----- ДО ПЕРЕВОДА ----- \n");

        TransactionReadDto transactionReadDto = transactionService.transfer(transactionCreateDto);


        System.out.println("\n ----- ПОСЛЕ ПЕРЕВОДА ----- \n");

        assertEquals(TransactionStatus.SUCCESS, transactionReadDto.status());
        assertNotEquals(transactionReadDto.fromBalanceBefore(), transactionReadDto.fromBalanceAfter());
        assertNotEquals(transactionReadDto.toBalanceBefore(), transactionReadDto.toBalanceAfter());

        // Проврека соответсвия в бд

        EntityManager em = sessionFactory.createEntityManager();

        BankTransaction bankTransaction = em.find(BankTransaction.class, transactionReadDto.id());

        assertEquals(FROM_ACCOUNT_ID, bankTransaction.getFromAccount().getId());
        assertEquals(TO_ACCOUNT_ID, bankTransaction.getToAccount().getId());
        assertEquals(transactionReadDto.status(), bankTransaction.getStatus());

        assertEquals(fromMoneyBefore.doubleValue() - MONEY_TO_TRANSFER.doubleValue(), bankTransaction.getFromAccount().getBalance().doubleValue());
        assertEquals(toMoneyBefore.doubleValue() + MONEY_TO_TRANSFER.doubleValue(), bankTransaction.getToAccount().getBalance().doubleValue());
        assertNotNull(bankTransaction.getProcessedAt());
    }
}
