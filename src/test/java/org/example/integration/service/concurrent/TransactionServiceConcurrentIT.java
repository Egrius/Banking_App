package org.example.integration.service.concurrent;

import jakarta.persistence.EntityManager;
import org.example.dto.transation.TransactionCreateDto;
import org.example.dto.transation.TransactionReadDto;
import org.example.entity.Account;
import org.example.integration.service.config.AbstractTransactionServiceIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.A;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionServiceConcurrentIT extends AbstractTransactionServiceIntegrationTest {

    private final Long ACCOUNT_A_ID = 1L;
    private final Long ACCOUNT_B_ID = 2L;
    private final Long ACCOUNT_C_ID = 3L;
    private final Long ACCOUNT_D_ID = 4L;

    private Account accountA;
    private Account accountB;
    private Account accountC;
    private Account accountD;

    @BeforeEach
    void setup() {
        // Очистить таблицы юзеров, транзакции, логов для транзакций.
        // Добавить тестовых пользователей с тестовым счетом.
        runSql("test_transfer_concurrent_data.sql");

        accountA = findAccountById(ACCOUNT_A_ID).orElseThrow();
        accountB = findAccountById(ACCOUNT_B_ID).orElseThrow();
        accountC = findAccountById(ACCOUNT_C_ID).orElseThrow();
        accountD = findAccountById(ACCOUNT_D_ID).orElseThrow();

        System.out.println("----- ПОСЛЕ setup() -----");
    }

    // Параллельные переводы с одного счёта
    @Nested
    class TransfersFromOneAccountTests {

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

        /*
        Три перевода с одного счёта (A→B 300, A→C 300, A→D 500) при балансе A = 1000
        Два успешны, третий падает (недостаточно средств)
         */
        @Test
        void transfer_shouldHandleThreeTransfers() throws ExecutionException, InterruptedException {

            List<TransactionCreateDto> createDtos = new ArrayList<>();
            ExecutorService executorService = Executors.newFixedThreadPool(3);

            BigDecimal MONEY_TO_TRANSFER_A_TO_B = BigDecimal.valueOf(400);

            UUID IDEMPOTENCY_KEY_FROM_A_TO_B = UUID.randomUUID();

            TransactionCreateDto dto_A_B = new TransactionCreateDto(
                    ACCOUNT_A_ID,
                    ACCOUNT_B_ID,
                    accountA.getCurrencyCode(),
                    MONEY_TO_TRANSFER_A_TO_B,
                    "",
                    IDEMPOTENCY_KEY_FROM_A_TO_B
            );
            createDtos.add(dto_A_B);

            BigDecimal MONEY_TO_TRANSFER_A_TO_C = BigDecimal.valueOf(400);

            UUID IDEMPOTENCY_KEY_FROM_A_TO_C = UUID.randomUUID();

            TransactionCreateDto dto_A_C = new TransactionCreateDto(
                    ACCOUNT_A_ID,
                    ACCOUNT_B_ID,
                    accountA.getCurrencyCode(),
                    MONEY_TO_TRANSFER_A_TO_C,
                    "",
                    IDEMPOTENCY_KEY_FROM_A_TO_C
            );
            createDtos.add(dto_A_C);

            BigDecimal MONEY_TO_TRANSFER_A_TO_D = BigDecimal.valueOf(400);

            UUID IDEMPOTENCY_KEY_FROM_A_TO_D = UUID.randomUUID();

            TransactionCreateDto dto_A_D = new TransactionCreateDto(
                    ACCOUNT_A_ID,
                    ACCOUNT_D_ID,
                    accountA.getCurrencyCode(),
                    MONEY_TO_TRANSFER_A_TO_D,
                    "",
                    IDEMPOTENCY_KEY_FROM_A_TO_D
            );
            createDtos.add(dto_A_D);

            CountDownLatch startLatch = new CountDownLatch(1);

            List<CompletableFuture<TransactionReadDto>> futures = new ArrayList<>();

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            List<CompletableFuture<TransactionReadDto>> completedOrderFutures = new ArrayList<>();

            for(TransactionCreateDto dto : createDtos) {
                futures.add(CompletableFuture.supplyAsync(() -> {
                                    try {
                                        startLatch.await();
                                        System.out.println("--- ПЕРЕВОД " + dto.fromAccountId() + " -> " + dto.toAccountId() + " : " + dto.amount());
                                        return transactionService.transfer(dto);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                }, executorService)
                                .thenApply(transactionReadDto -> {
                                    successCount.incrementAndGet();
                                    completedOrderFutures.add(CompletableFuture.completedFuture(transactionReadDto));
                                    return transactionReadDto;
                                })
                                .exceptionally(throwable -> {
                                    failCount.getAndIncrement();
                                    System.out.println("ОШИБКА: " + throwable.getMessage());
                                    return null;
                                })
                );
            }

            startLatch.countDown();

            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));

            allFutures.join();

            assertEquals(2, successCount.get());
            assertEquals(1, failCount.get());

            try (EntityManager em = sessionFactory.createEntityManager()) {

                Account aAfter = em.find(Account.class, ACCOUNT_A_ID);
                System.out.println("--- БАЛАНС А: " + aAfter.getBalance());
                assertEquals(0, new BigDecimal("200").compareTo(aAfter.getBalance()));

                assertNotEquals(completedOrderFutures.get(0).get().fromBalanceBefore(), completedOrderFutures.get(0).get().fromBalanceAfter());
                assertNotEquals(completedOrderFutures.get(1).get().fromBalanceBefore(), completedOrderFutures.get(1).get().fromBalanceAfter());
            }
        }
    }

    @Nested
    class DeadLockTransferTests {
        /*
        Одновременные переводы A→B 300 и B→A 300 при балансе A=1000, B=1000
        Оба успешны, балансы A=1000, B=1000 (нет дедлока)
         */
        @Test
        void transfer_shouldHandleSimultaneouslyTransfersFromEachOtherAccounts() {
            List<TransactionCreateDto> createDtos = new ArrayList<>();
            ExecutorService executorService = Executors.newFixedThreadPool(2);

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
            createDtos.add(dto1);

            BigDecimal MONEY_TO_TRANSFER_B_TO_A = BigDecimal.valueOf(300);

            UUID IDEMPOTENCY_KEY_FROM_B_TO_A = UUID.randomUUID();

            TransactionCreateDto dto2 = new TransactionCreateDto(
                    ACCOUNT_B_ID,
                    ACCOUNT_A_ID,
                    accountB.getCurrencyCode(),
                    MONEY_TO_TRANSFER_B_TO_A,
                    "",
                    IDEMPOTENCY_KEY_FROM_B_TO_A
            );

            createDtos.add(dto2);

            CountDownLatch startLatch = new CountDownLatch(1);

            List<CompletableFuture<TransactionReadDto>> futures = new ArrayList<>();

            for (TransactionCreateDto dto : createDtos) {
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        startLatch.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println("--- Начало перевода " + dto.fromAccountId() + " -> " + dto.toAccountId() + " : " + dto.amount());
                    return transactionService.transfer(dto);
                }, executorService));
            }

            startLatch.countDown();

            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

            System.out.println("-- DEADLOCK'A не произошло --");

            try (EntityManager em = sessionFactory.createEntityManager()) {

                Account aAfter = em.find(Account.class, ACCOUNT_A_ID);
                System.out.println("--- БАЛАНС А: " + aAfter.getBalance());
                assertEquals(0, new BigDecimal("1000").compareTo(aAfter.getBalance()));

                Account bAfter = em.find(Account.class, ACCOUNT_B_ID);
                System.out.println("--- БАЛАНС B: " + bAfter.getBalance());
                assertEquals(0, new BigDecimal("1000").compareTo(bAfter.getBalance()));

                assertTrue(aAfter.getVersion() > 0);
                assertTrue(bAfter.getVersion() > 0);
            }
        }

        /*
        Три встречных перевода: A→B, B→C, C→A (каждый по 300) при балансах A=1000, B=1000, C=1000
        Все успешны, балансы не изменились (циклический перевод)
         */
        @Test
        void transfer_shouldHandleThreeTSimultaneousTransfers() {

            try (EntityManager em = sessionFactory.createEntityManager()) {
                em.getTransaction().begin();
                Account accountA = em.find(Account.class, ACCOUNT_A_ID);
                accountA.setBalance(BigDecimal.valueOf(1000));
                Account accountB = em.find(Account.class, ACCOUNT_B_ID);
                accountB.setBalance(BigDecimal.valueOf(1000));
                Account accountC = em.find(Account.class, ACCOUNT_C_ID);
                accountB.setBalance(BigDecimal.valueOf(1000));
                em.getTransaction().commit();
            }

            List<TransactionCreateDto> createDtos = new ArrayList<>();
            ExecutorService executorService = Executors.newFixedThreadPool(2);

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
            createDtos.add(dto1);

            BigDecimal MONEY_TO_TRANSFER_B_TO_C = BigDecimal.valueOf(300);

            UUID IDEMPOTENCY_KEY_FROM_B_TO_C = UUID.randomUUID();

            TransactionCreateDto dto2 = new TransactionCreateDto(
                    ACCOUNT_B_ID,
                    ACCOUNT_C_ID,
                    accountB.getCurrencyCode(),
                    MONEY_TO_TRANSFER_B_TO_C,
                    "",
                    IDEMPOTENCY_KEY_FROM_B_TO_C
            );

            createDtos.add(dto2);

            BigDecimal MONEY_TO_TRANSFER_C_TO_A = BigDecimal.valueOf(300);

            UUID IDEMPOTENCY_KEY_FROM_C_TO_A = UUID.randomUUID();

            TransactionCreateDto dto3 = new TransactionCreateDto(
                    ACCOUNT_C_ID,
                    ACCOUNT_A_ID,
                    accountB.getCurrencyCode(),
                    MONEY_TO_TRANSFER_C_TO_A,
                    "",
                    IDEMPOTENCY_KEY_FROM_C_TO_A
            );

            createDtos.add(dto3);

            CountDownLatch startLatch = new CountDownLatch(1);

            List<CompletableFuture<TransactionReadDto>> futures = new ArrayList<>();

            for (TransactionCreateDto dto : createDtos) {
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        startLatch.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println("--- Начало перевода " + dto.fromAccountId() + " -> " + dto.toAccountId() + " : " + dto.amount());
                    return transactionService.transfer(dto);
                }, executorService));
            }

            startLatch.countDown();

            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

            try (EntityManager em = sessionFactory.createEntityManager()) {

                Account aAfter = em.find(Account.class, ACCOUNT_A_ID);
                System.out.println("--- БАЛАНС А: " + aAfter.getBalance());
                assertEquals(0, new BigDecimal("1000").compareTo(aAfter.getBalance()));

                Account bAfter = em.find(Account.class, ACCOUNT_B_ID);
                System.out.println("--- БАЛАНС B: " + bAfter.getBalance());
                assertEquals(0, new BigDecimal("1000").compareTo(bAfter.getBalance()));

                Account cAfter = em.find(Account.class, ACCOUNT_C_ID);
                System.out.println("--- БАЛАНС B: " + cAfter.getBalance());
                assertEquals(0, new BigDecimal("1000").compareTo(bAfter.getBalance()));

                assertTrue(aAfter.getVersion() > 0);
                assertTrue(bAfter.getVersion() > 0);
                assertTrue(cAfter.getVersion() > 0);
            }

        }
    }

}
