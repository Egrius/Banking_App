package org.example.service;

import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dao.AccountDao;
import org.example.dao.UserDao;
import org.example.dto.account.AccountCreateDto;
import org.example.dto.account.AccountReadDto;
import org.example.dto.account.AccountSummaryDto;
import org.example.dto.balance_audit.BalanceAuditReadDto;
import org.example.dto.request.PageRequest;
import org.example.dto.response.PageResponse;
import org.example.entity.Account;
import org.example.entity.AccountBalanceAudit;
import org.example.entity.User;
import org.example.entity.enums.Status;
import org.example.mapper.AccountReadMapper;
import org.example.util.ValidatorUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final UserDao userDao;
    private final AccountDao accountDao;
    private final AccountReadMapper accountReadMapper;
    private final EntityManagerFactory emf;

    public AccountReadDto createAccount(AccountCreateDto createDto) {

        ValidatorUtil.validate(createDto);

        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            User user = userDao.findById(em, createDto.userId())
                    .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));

            if(accountDao.existsByUserIdAndType(em, user.getId(), createDto.accountType())) {
                throw new IllegalStateException("У пользователя уже есть счет такого типа");
            }

            Account account = new Account(user,
                    generateAccountNumber(user.getId()),
                    BigDecimal.ZERO,
                    createDto.currencyCode(),
                    createDto.accountType()
            );

            accountDao.save(em, account);
            tx.commit();

            log.info("Создан новый счет: {} для пользователя: {}",
                    account.getAccountNumber(), user.getEmail());

            return accountReadMapper.map(account);
        } catch (Exception e) {
            if(tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public AccountReadDto getAccount(Long accountId) {

        EntityManager em = emf.createEntityManager();

        try {
            Account account = accountDao.findById(em, accountId)
                    .orElseThrow(() -> new EntityNotFoundException("Аккаунт с id " + accountId + " не найден"));

            return accountReadMapper.map(account);

        } finally {
            em.close();
        }
    }

    public List<AccountSummaryDto> getUserAccounts(Long userId){
        EntityManager em = emf.createEntityManager();

        try {
            List<Account> accounts = accountDao.findUserAccountsByUserId(em, userId);

            return accounts.stream()
                    .map(a -> new AccountSummaryDto(
                            a.getId(),
                            a.getAccountNumber(),
                            a.getCurrencyCode(),
                            a.getAccountType()
                    ))
                    .toList();

        } finally {
            em.close();
        }
    }

    // здесь может быть изоляция
    public void closeAccount(Long accountId) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            Account account = accountDao.findById(em, accountId)
                    .orElseThrow(() -> new EntityNotFoundException("Аккаунт с id " + accountId + " не найден"));

            if (account.getStatus() == Status.CLOSED) {
                throw new IllegalStateException("Аккаунт уже закрыт");
            }

            if (account.getStatus() == Status.BLOCKED) {
                throw new IllegalStateException("Нельзя закрыть заблокированный счет");
            }

            account.setStatus(Status.CLOSED);
            account.setClosingDate(LocalDateTime.now());

            accountDao.save(em, account);
            log.info("Аккаунт был закрыт");
            tx.commit();

        } catch(OptimisticLockException e) {
            tx.rollback();
            log.info("Аккаунт был кем-то обновлён, попробуйте ещё раз");
            throw e;
        } catch (Exception e) {
            if(tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    // здесь может быть изоляция
    // TODO в логи добавить причину плюс событие
    public void blockAccount(Long accountId, String reason){
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Account account = accountDao.findById(em, accountId)
                    .orElseThrow(() -> new EntityNotFoundException("Аккаунт с id " + accountId + " не найден"));

            if (account.getStatus() == Status.BLOCKED) {
                throw new IllegalStateException("Аккаунт уже заблокирован");
            }

            if (account.getStatus() == Status.CLOSED) {
                throw new IllegalStateException("Нельзя заблокировать закрытый счет");
            }

            account.setStatus(Status.BLOCKED);
            // TODO: сохранить reason (нужно поле в Account)

            accountDao.save(em, account);
            tx.commit();

            log.info("Аккаунт {} заблокирован. Причина: {}", account.getAccountNumber(), reason);

        } catch (OptimisticLockException e) {
            if (tx.isActive()) tx.rollback();
            log.info("Аккаунт был кем-то обновлён, попробуйте ещё раз");
            throw e;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;  // Не забываем пробросить!
        } finally {
            em.close();
        }
    }

    public PageResponse<BalanceAuditReadDto> getBalanceAudit(Long accountId, PageRequest pageRequest) {
        EntityManager em = emf.createEntityManager();

        try {
            if (!accountDao.existsById(em, accountId)) {
                throw new EntityNotFoundException("Аккаунт с id " + accountId + " не найден");
            }

            // Подсчет сколько всего, чтобы создать респонс
            Long auditsTotal = accountDao.countAudits(em, accountId);

            List<AccountBalanceAudit> audits = accountDao.getAuditsPage(em, accountId,pageRequest.getPageNumber(), pageRequest.getPageSize());
            return new PageResponse<BalanceAuditReadDto>(
                    audits.stream().map(a -> new BalanceAuditReadDto(
                        a.getBalanceBefore(), a.getBalanceAfter(), a.getChangeAmount(), a.getChangedAt(), a.getChangedByThread()
                    )).toList(),
                    pageRequest.getPageNumber(),
                    pageRequest.getPageSize(), auditsTotal);

        } finally {
            em.close();
        }
    }

    public BigDecimal getBalance(Long accountId) {
        EntityManager em = emf.createEntityManager();

        try {
            return accountDao.findById(em, accountId)
                    .map(Account::getBalance)
                    .orElseThrow(() -> new EntityNotFoundException("Счет с id" + accountId + "не найден"));

        } finally {
            em.close();
        }
    }

    private String generateAccountNumber(Long userId) {
        return String.format("ACNT-%d-%d-%d",
                userId,
                System.currentTimeMillis(),
                (int)(Math.random() * 1000));
    }
}