package org.example.integration.concurrent.service;

import jakarta.persistence.EntityManager;
import org.example.dto.transation.TransactionCreateDto;
import org.example.dto.transation.TransactionReadDto;
import org.example.entity.Account;
import org.example.integration.config.AbstractTransactionServiceIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransactionServiceConcurrentIT extends AbstractTransactionServiceIntegrationTest {

    private final Long ACCOUNT_A_ID = 1L;
    private final Long ACCOUNT_B_ID = 2L;
    private final Long ACCOUNT_C_ID = 3L;

    private Account accountA;
    private Account accountB;
    private Account accountC;

    @BeforeEach
    void setup() {
        // Очистить таблицы юзеров, транзакции, логов для транзакций.
        // Добавить тестовых пользователей с тестовым счетом.
        runSql("test_transfer_concurrent_data.sql");

        accountA = findAccountById(ACCOUNT_A_ID).orElseThrow();
        accountB = findAccountById(ACCOUNT_B_ID).orElseThrow();
        accountC = findAccountById(ACCOUNT_C_ID).orElseThrow();

        System.out.println("----- ПОСЛЕ setup() -----");
    }

    // Параллельные переводы с одного счёта
    /*
    (A -> B 600, A -> C 600) при балансе A = 1000 - последний не должен выполнится
     */
    @Test
    void transfer_shouldNotTransferIfNotEnoughMoney() throws InterruptedException {

        BigDecimal MONEY_TO_TRANSFER_A_TO_B = BigDecimal.valueOf(600.0);

        UUID IDEMPOTENCY_KEY_FROM_A_TO_B = UUID.randomUUID();

        TransactionCreateDto dto1 = new TransactionCreateDto(
                ACCOUNT_A_ID,
                ACCOUNT_B_ID,
                accountA.getCurrencyCode(),
                MONEY_TO_TRANSFER_A_TO_B,
                "",
                IDEMPOTENCY_KEY_FROM_A_TO_B
        );

        BigDecimal MONEY_TO_TRANSFER_A_TO_C = BigDecimal.valueOf(600.0);

        UUID IDEMPOTENCY_KEY_FROM_A_TO_C = UUID.randomUUID();

        TransactionCreateDto dto2 = new TransactionCreateDto(
                ACCOUNT_A_ID,
                ACCOUNT_C_ID,
                accountA.getCurrencyCode(),
                MONEY_TO_TRANSFER_A_TO_C,
                "",
                IDEMPOTENCY_KEY_FROM_A_TO_C
        );

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<?>> futures = new ArrayList<>();

        futures.add(executorService.submit(() -> {

            try {
                startLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            transactionService.transfer(dto1);

        }));

        futures.add(executorService.submit(() -> {

            try {
                startLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            transactionService.transfer(dto2);

        }));

        startLatch.countDown();

        int successCount = 0;
        int failureCount = 0;

        for (Future<?> future : futures) {
            try {
                future.get(10, TimeUnit.SECONDS);
                successCount++;
            } catch (ExecutionException e) {
                if (e.getCause() instanceof IllegalStateException &&
                        e.getCause().getMessage().contains("недостаточно средств")) {
                    System.out.println("------- ПОПАЛИ В НЕДОСТАТОЧНО СРЕДСТВ, УВЕЛИЧИВАЕМ FAILURECOUNT");
                    failureCount++;
                } else {
                    System.out.println("------- ПОПАЛИ В ELSE");
                    throw new RuntimeException(e);
                }
            } catch (TimeoutException e) {
                throw new RuntimeException(e);
            }
        }

        assertEquals(1, successCount);
        assertEquals(1, failureCount);

        // Проверяем итоговый баланс A
        try (EntityManager em = sessionFactory.createEntityManager()) {
            Account aAfter = em.find(Account.class, ACCOUNT_A_ID);
            // 1000 - 600 = 400 (только один перевод успешен)
            assertEquals(0, new BigDecimal("400").compareTo(aAfter.getBalance()));
        }
    }

    /*
    Два перевода с одного счёта на разные счета
    (A -> B 300, A -> C 300) при балансе A = 1000
    Оба перевода успешны, итоговый баланс A = 400
     */
    @Test
    void transfer_bothTransactionsShouldBeSuccess_andBalanceChangedCorrectly() {
        BigDecimal MONEY_TO_TRANSFER_A_TO_B = BigDecimal.valueOf(300);

        UUID IDEMPOTENCY_KEY_FROM_A_TO_B = UUID.randomUUID();

        TransactionCreateDto dto1 = new TransactionCreateDto(
                ACCOUNT_A_ID,
                ACCOUNT_B_ID,
                accountA.getCurrencyCode(),
                MONEY_TO_TRANSFER_A_TO_B,
                "",
                IDEMPOTENCY_KEY_FROM_A_TO_B
        );

        BigDecimal MONEY_TO_TRANSFER_A_TO_C = BigDecimal.valueOf(300);

        UUID IDEMPOTENCY_KEY_FROM_A_TO_C = UUID.randomUUID();

        TransactionCreateDto dto2 = new TransactionCreateDto(
                ACCOUNT_A_ID,
                ACCOUNT_C_ID,
                accountA.getCurrencyCode(),
                MONEY_TO_TRANSFER_A_TO_C,
                "",
                IDEMPOTENCY_KEY_FROM_A_TO_C
        );

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<CompletableFuture<TransactionReadDto>> futures = new ArrayList<>();

        futures.add(CompletableFuture.supplyAsync(() -> {
            try {
                startLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return transactionService.transfer(dto1);
        }, executorService));

        futures.add(CompletableFuture.supplyAsync(() -> {
            try {
                startLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return transactionService.transfer(dto2);
        }, executorService));


        startLatch.countDown();

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<CompletableFuture<Void>> processedFutures = new ArrayList<>();

        for (CompletableFuture<TransactionReadDto> f : futures) {
            CompletableFuture<Void> processed = f
                    .thenAccept(dto -> successCount.getAndIncrement())
                    .exceptionally(throwable -> {
                        failureCount.getAndIncrement();
                        System.out.println("ОШИБКА: " + throwable.getMessage());
                        return null;
                    });
            processedFutures.add(processed);
        }

        CompletableFuture.allOf(processedFutures.toArray(new CompletableFuture[0])).join();

        assertEquals(2, successCount.get());
        assertEquals(0, failureCount.get());

        try (EntityManager em = sessionFactory.createEntityManager()) {

            Account aAfter = em.find(Account.class, ACCOUNT_A_ID);
            assertEquals(0, new BigDecimal("400").compareTo(aAfter.getBalance()));

            Account bAfter = em.find(Account.class, ACCOUNT_B_ID);
            assertEquals(0, new BigDecimal("800").compareTo(bAfter.getBalance()));

            Account cAfter = em.find(Account.class, ACCOUNT_C_ID);
            assertEquals(0, new BigDecimal("700").compareTo(cAfter.getBalance()));
        }
    }
}
