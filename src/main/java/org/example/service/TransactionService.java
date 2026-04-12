package org.example.service;

/*
. Flow транзакций в банке (перевод денег)
Полный цикл перевода:

Клиент → Запрос на перевод
    ↓
1. Валидация входных данных (сумма > 0, счета существуют, валюты совпадают)
    ↓
2. Проверка прав (счет отправителя принадлежит текущему пользователю)
    ↓
3. Проверка статусов (счет не заблокирован, не закрыт)
    ↓
4. Проверка лимитов (дневной лимит, максимальная сумма)    <- ПРОДУМАТЬ
    ↓
5. Создание записи транзакции со статусом PENDING
    ↓
6. Блокировка счетов (пессимистичная блокировка)
    ↓
7. Проверка достаточности средств
    ↓
8. Списание с from_account / Зачисление на to_account
    ↓
9. Обновление статуса транзакции → SUCCESS / FAILED
    ↓
10. Запись в аудит баланса (AccountBalanceAudit)
    ↓
11. Коммит транзакции (или rollback)
    ↓
12. Ответ клиенту

2. Функции, которые должны быть в TransactionService
Основные (обязательные):

    transfer — перевод между счетами (ядро)

    getTransactionById — получение транзакции по ID

    getAccountTransactions — история транзакций счета (с пагинацией)

    getUserTransactions — история всех транзакций пользователя (с пагинацией)

Дополнительные (опционально):

    getPendingTransactions — для восстановления после падения (recovery)

    retryTransaction — повтор неудачной транзакции

    cancelTransaction — отмена транзакции (если статус PENDING)

    ИДЕМПОТЕНТНОСТЬ ДЛЯ ТРАНЗАКЦИИ! {
        Сделать табличку с ключами, ключ генерирует клиент,
        на сервере принимается ключ, проверяется, есть ли он уже в таблице, если да,
        то предложить повторить транзакцию. Если нет, то сгенерить новый и выполнить транзакцию.
        В ключе должна быть дата истечения срока действия
    }

 */

import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.example.dao.AccountDao;
import org.example.dao.TransactionDao;
import org.example.dto.idempotency_key.IdempotencyKeyCreateDto;
import org.example.dto.idempotency_key.IdempotencyKeyReadDto;
import org.example.dto.transation.TransactionCreateDto;
import org.example.dto.transation.TransactionReadDto;
import org.example.entity.Account;
import org.example.entity.AccountBalanceAudit;
import org.example.entity.BankTransaction;
import org.example.entity.enums.CurrencyCode;
import org.example.entity.enums.Status;
import org.example.entity.enums.TransactionStatus;
import org.example.interceptor.AuditLogTransactionInterceptor;
import org.example.mapper.TransactionReadMapper;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class TransactionService {

    private final EntityManagerFactory emf;
    private final AccountDao accountDao;
    private final TransactionDao transactionDao;
    private final TransactionReadMapper transactionReadMapper;
    private final IdempotencyService idempotencyService;
    private final AuditService auditService;

    public TransactionService(AccountDao accountDao, TransactionDao transactionDao, EntityManagerFactory emf, TransactionReadMapper transactionReadMapper, IdempotencyService idempotencyService, AuditService auditService) {
        this.accountDao = accountDao;
        this.transactionDao = transactionDao;
        this.emf = emf;
        this.transactionReadMapper = transactionReadMapper;
        this.idempotencyService = idempotencyService;
        this.auditService = auditService;

        EventListenerRegistry registry = emf.unwrap(SessionFactoryImplementor.class).getServiceRegistry()
                .getService(EventListenerRegistry.class);

        AuditLogTransactionInterceptor auditInterceptor = new AuditLogTransactionInterceptor(auditService);

        registry.appendListeners(EventType.POST_INSERT, auditInterceptor);
        registry.appendListeners(EventType.POST_UPDATE, auditInterceptor);
    }

    private enum TransferLimit {
        RUB(new BigDecimal("5"), new BigDecimal("700")),
        US(new BigDecimal("5"), new BigDecimal("400")),
        EUR(new BigDecimal("5"), new BigDecimal("400"));

        private final BigDecimal min;
        private final BigDecimal max;

        TransferLimit(BigDecimal min, BigDecimal max) {
            this.min = min;
            this.max = max;
        }

        public static TransferLimit fromCurrency(CurrencyCode currency) {
            return switch (currency) {
                case RUB -> RUB;
                case US -> US;
                case EUR -> EUR;
            };
        }

        public BigDecimal getMin() {
            return min;
        }

        public BigDecimal getMax() {
            return max;
        }
    }

    // ВОЗНИКАЕТ N+1 ПРИ МАППИНГЕ ТРАНЗАКЦИИ В СЛУЧАЕ ПОВТОРНОГО КЛЮЧА: ГРУЗЯТСЯ РОЛИ ДВУХ ЮЗЕРОВ
    public TransactionReadDto transfer(TransactionCreateDto transactionCreateDto) {

        // Проверка на идемпотентность (если повторно одно и то же, то чекнуть статус, если pending, то дать возможность повторить?)

        if (transactionCreateDto.idempotencyKey() == null) throw new IllegalArgumentException("Не передан ключ идемпотентности");

        Optional<IdempotencyKeyReadDto> idempotencyKeyReadDtoOptional = idempotencyService.getKey(transactionCreateDto.idempotencyKey());

        if(idempotencyKeyReadDtoOptional.isPresent()) {
            // ключ уже есть => повторная операция, посмотреть статус связанной транзакции
            IdempotencyKeyReadDto idempotencyKeyReadDto = idempotencyKeyReadDtoOptional.get();

            // Если ключ просроченный, то удалить запись и выкинуть исклчени
            if(idempotencyKeyReadDto.expiresAt().isBefore(LocalDateTime.now())) {
                // delete(key)
                throw new IllegalStateException("Ключ просроченный, повторите ещё раз с новым ключом");
            }

            // Айдишка транзакции из ключа
            Long transactionId = idempotencyKeyReadDto.transactionId();

            if(transactionId != null) {
                try (EntityManager em = emf.createEntityManager()) {

                    Optional<BankTransaction> transactionOptional = transactionDao.findByIdWithAccountsAndUsers(em, transactionId);

                    // Если транзакция из ключа существует.
                    // Если её нет в ключе, то для этого ключа будет создана новая?
                    // Так-то ключ уже в БД и если он не на что не указывает, то его нужно снести.
                    if(transactionOptional.isPresent()) {

                        TransactionStatus status = transactionOptional.get().getStatus();

                        switch (status) {
                            case PENDING -> {
                                // Повторный запрос на выполнение - вернуть в ожидание.
                                throw new IllegalStateException("Транзакция уже обрабатывается");
                            }
                            case SUCCESS -> {
                                // Успешно - возвращаем результат
                                return transactionReadMapper.map(transactionOptional.get());
                            }
                            case FAILED -> {
                                // Транзакция неуспещна, т.е ключ указывает на неудачную - это некорректное состояние, нужно повторить.
                                // Ключ по идее нужно удалить из бд
                                return transactionReadMapper.map(transactionOptional.get());
                            }
                        }
                    }
                }
            } else {
                idempotencyService.delete(idempotencyKeyReadDto.id());
                throw new IllegalStateException("В ключе отсутствует привязанная транзакция");
            }
        }

        Map<String, String> properties = new HashMap<>();
        properties.put(AvailableSettings.INTERCEPTOR, AuditLogTransactionInterceptor.class.getName());

        EntityManager em = emf.createEntityManager(properties);
        EntityTransaction tx = em.getTransaction();

        try {

            tx.begin();

            Long id1 = transactionCreateDto.fromAccountId();
            Long id2 = transactionCreateDto.toAccountId();

            Long firstId = id1 < id2 ? id1 : id2;
            Long secondId = id1 < id2 ? id2 : id1;

            Account first = accountDao.findByIdWithUserAndPessimisticWrite(em, firstId)
                    .orElseThrow(() -> new EntityNotFoundException("Аккаунт отправителя с id " + transactionCreateDto.fromAccountId() + " не найден"));

            Account second = accountDao.findByIdWithUserAndPessimisticWrite(em, secondId)
                    .orElseThrow(() -> new EntityNotFoundException("Аккаунт получателя с id " + transactionCreateDto.toAccountId() + " не найден"));

            Account fromAccount = first.getId().equals(id1) ? first : second;
            Account toAccount = fromAccount == first ? second : first;

            if(fromAccount.getStatus() == Status.BLOCKED || fromAccount.getStatus() == Status.CLOSED) {
                throw new IllegalStateException("Нельзя выполнить транзакцию со счёта со статусом " + fromAccount.getStatus());
            }

            if(toAccount.getStatus() == Status.BLOCKED || toAccount.getStatus() == Status.CLOSED) {
                throw new IllegalStateException("Нельзя перевести средства счёт со статусом " + toAccount.getStatus());
            }

            if(fromAccount.getCurrencyCode() != toAccount.getCurrencyCode()) {
                throw new IllegalStateException("Нельзя выполнить перевод между разновалютными счетами!");
            }

            // Проверка лимитов(дневной лимит + максимальная сумма) (пока не реализовано)

            // Проверка лимитов за одну транзакцию
            // после проверки валют (до проверки достаточности средств)
            TransferLimit limit = TransferLimit.fromCurrency(fromAccount.getCurrencyCode());
            if (transactionCreateDto.amount().compareTo(limit.getMin()) < 0) {
                throw new IllegalStateException(
                        String.format("Минимальная сумма перевода для %s: %s",
                                fromAccount.getCurrencyCode(), limit.getMin())
                );
            }

            if (transactionCreateDto.amount().compareTo(limit.getMax()) > 0) {
                throw new IllegalStateException(
                        String.format("Максимальная сумма перевода для %s: %s",
                                fromAccount.getCurrencyCode(), limit.getMax())
                );
            }

            // Проверка достаточности средств
            if(fromAccount.getBalance().compareTo(transactionCreateDto.amount()) < 0) {
                throw new IllegalStateException("На счёте недостаточно средств для выполнения перевода");
            }

            // Создание записи транзакции со статусом PENDING

            BigDecimal fromBalanceBefore = fromAccount.getBalance();
            BigDecimal toBalanceBefore = toAccount.getBalance();

            BankTransaction bankTransaction = BankTransaction.builder()
                    .fromAccount(fromAccount)
                    .toAccount(toAccount)
                    .amount(transactionCreateDto.amount())
                    .status(TransactionStatus.PENDING)
                    .fromBalanceBefore(fromBalanceBefore)
                    .toBalanceBefore(toBalanceBefore)
                    .createdAt(LocalDateTime.now())
                    .description(transactionCreateDto.description())
                    .build();

            em.persist(bankTransaction);
            em.flush();

            IdempotencyKeyCreateDto idempotencyKeyCreateDto = new IdempotencyKeyCreateDto(
                    transactionCreateDto.idempotencyKey(),
                    bankTransaction,
                    LocalDateTime.now()
            );
            
            idempotencyService.createKey(idempotencyKeyCreateDto, em);

            BigDecimal fromBalanceAfter =fromAccount.getBalance().subtract(transactionCreateDto.amount());
            BigDecimal toBalanceAfter = toAccount.getBalance().add(transactionCreateDto.amount());

            //  Списание с from_account / Зачисление на to_account
            fromAccount.setBalance(fromBalanceAfter);
            toAccount.setBalance(toBalanceAfter);

            // Обновление статуса транзакции -> SUCCESS / FAILED
            bankTransaction.setStatus(TransactionStatus.SUCCESS);
            bankTransaction.setProcessedAt(LocalDateTime.now());
            bankTransaction.setFromBalanceAfter(fromAccount.getBalance());
            bankTransaction.setToBalanceAfter(toAccount.getBalance());

            // Запись в аудит баланса (AccountBalanceAudit)
            AccountBalanceAudit fromAudit = new AccountBalanceAudit(fromAccount, fromBalanceBefore, fromBalanceAfter, Thread.currentThread().getName());
            AccountBalanceAudit toAudit = new AccountBalanceAudit(toAccount, toBalanceBefore, toBalanceAfter, Thread.currentThread().getName());

            em.persist(fromAudit);
            em.persist(toAudit);

            tx.commit();

            // TODO: сделать перехватчик на выталкивание контекста и делать запись в лог

            return transactionReadMapper.map(bankTransaction);

        } catch (Exception e) {
            if (tx.isActive()) {

                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
}