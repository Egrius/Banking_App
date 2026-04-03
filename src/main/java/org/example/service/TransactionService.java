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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dao.AccountDao;
import org.example.dao.TransactionDao;
import org.example.dto.idempotency_key.IdempotencyKeyCreateDto;
import org.example.dto.idempotency_key.IdempotencyKeyReadDto;
import org.example.dto.transation.TransactionCreateDto;
import org.example.dto.transation.TransactionReadDto;
import org.example.entity.Account;
import org.example.entity.BankTransaction;
import org.example.entity.IdempotencyKey;
import org.example.entity.enums.Status;
import org.example.entity.enums.TransactionStatus;
import org.example.mapper.TransactionReadMapper;
import org.example.security.AuthContext;
import org.example.util.SecurityUtil;
import org.example.util.ValidatorUtil;
import org.hibernate.Session;

import javax.swing.text.html.Option;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private final AccountDao accountDao;
    private final TransactionDao transactionDao;
    private final EntityManagerFactory emf;
    private final TransactionReadMapper transactionReadMapper;
    private final IdempotencyService idempotencyService;

    public TransactionReadDto transfer(TransactionCreateDto transactionCreateDto, AuthContext authContext) {
        //  Валидация входных данных
        ValidatorUtil.validate(transactionCreateDto);

        // Проверка прав
        SecurityUtil.checkOwner(authContext, transactionCreateDto.fromAccountId());

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

                    Optional<BankTransaction> transactionOptional = transactionDao.findById(em, transactionId);

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
            } else{
                idempotencyService.delete(idempotencyKeyReadDto.id());
                throw new IllegalStateException("В ключе отсутствует привязанная транзакция");
            }
        }

        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        // Session session = em.unwrap(Session.class);
        // org.hibernate.Transaction tx = session.beginTransaction();

        try {

            tx.begin();

            Long id1 = transactionCreateDto.fromAccountId();
            Long id2 = transactionCreateDto.toAccountId();

            Long firstId = id1 < id2 ? id1 : id2;
            Long secondId = id1 < id2 ? id2 : id1;

            // Проверка статусов счетов (получить + проверить)
            Account first  = Optional.of(em.find(Account.class, firstId, LockModeType.PESSIMISTIC_WRITE))
                    .orElseThrow(() -> new EntityNotFoundException("Аккаунт отправителя с id " + transactionCreateDto.fromAccountId() + " не найден"));

            Account second = Optional.of(em.find(Account.class, secondId, LockModeType.PESSIMISTIC_WRITE))
                    .orElseThrow(() -> new EntityNotFoundException("Аккаунт получателя с id " + transactionCreateDto.toAccountId() + " не найден"));

            Account fromAccount = first.getId().equals(id1) ? first : second;
            Account toAccount = fromAccount == first ? second : first;

            if(fromAccount.getStatus() == Status.BLOCKED || fromAccount.getStatus() == Status.CLOSED) {
                throw new IllegalStateException("Нельзя выполнить транзакцию со счёта со статусом " + fromAccount.getStatus());
            }

            if(toAccount.getStatus() == Status.BLOCKED || toAccount.getStatus() == Status.CLOSED) {
                throw new IllegalStateException("Нельзя перевести средства счёт со статусом " + toAccount.getStatus());
            }

            // Проверка лимитов(дневной лимит + максимальная сумма) (пока не реализовано)

            // Проверка достаточности средств
            if(fromAccount.getBalance().compareTo(transactionCreateDto.amount()) < 0) {
                throw new IllegalStateException("На счёте недостаточно средств для выполнения перевода");
            }

            // Создание записи транзакции со статусом PENDING
            // TODO расмотреть перевод различных валют (т.е нужно рассчитать сколько переводить со счета с зарегестрированной валютой)
            // TODO сущность с ключами идемпотентности и привязка к транзакции
            BankTransaction bankTransaction = BankTransaction.builder()
                    .fromAccount(fromAccount)
                    .toAccount(toAccount)
                    .amount(transactionCreateDto.amount())
                    .status(TransactionStatus.PENDING)
                    .fromBalanceBefore(fromAccount.getBalance())
                    .toBalanceBefore(toAccount.getBalance())
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

            //  Списание с from_account / Зачисление на to_account
            fromAccount.setBalance(fromAccount.getBalance().subtract(transactionCreateDto.amount()));
            toAccount.setBalance(toAccount.getBalance().add(transactionCreateDto.amount()));

            // Обновление статуса транзакции -> SUCCESS / FAILED
            bankTransaction.setStatus(TransactionStatus.SUCCESS);
            bankTransaction.setProcessedAt(LocalDateTime.now());
            bankTransaction.setFromBalanceAfter(fromAccount.getBalance());
            bankTransaction.setToBalanceAfter(toAccount.getBalance());

            tx.commit();

            // TODO: сделать перехватчик на выталкивание контекста и делать запись в лог
           // Запись в аудит баланса (AccountBalanceAudit)
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