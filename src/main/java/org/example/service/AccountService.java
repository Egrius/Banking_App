package org.example.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.EntityTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dao.AccountDao;
import org.example.dao.UserDao;
import org.example.dto.account.AccountCreateDto;
import org.example.dto.account.AccountReadDto;
import org.example.dto.account.AccountSummaryDto;
import org.example.entity.Account;
import org.example.entity.User;
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

    }

    public List<AccountSummaryDto> getUserAccounts(Long userId){

    }

    // здесь может быть изоляция
    public void closeAccount(Long accountId) {

    }

    // здесь может быть изоляция
    public void blockAccount(Long accountId, String reason){

    }

    // здесь может быть изоляция
    public List<BalanceAuditReadDto> getBalanceAudit() {

    }

    // здесь может быть изоляция
    public List<TransactionReadDto> getIncomingTransactions(Long accountId,
                                                            Pageable pageable) {
        return transactionDao.findByToAccountId(accountId, pageable)
                .map(transactionMapper::toDto);
    }


    public List<TransactionReadDto> getOutgoingTransactions(Long accountId,
                                                            Pageable pageable) {
        return transactionDao.findByFromAccountId(accountId, pageable)
                .map(transactionMapper::toDto);
    }

    public List<TransactionReadDto> getAllAccountTransactions(Long accountId,
                                                              Pageable pageable) {
        return transactionDao.findByAccountId(accountId, pageable)
                .map(transactionMapper::toDto);
    }


    public BigDecimal getBalance(Long accountId) {
        return accountDao.findById(accountId)
                .map(Account::getBalance)
                .orElseThrow(() -> new EntityNotFoundException("Счет не найден"));
    }

    // здесь может быть изоляция
    void updateBalance(Long accountId, BigDecimal newBalance) {

    }

    private String generateAccountNumber(Long userId) {
        return String.format("ACNT-%d-%d-%d",
                userId,
                System.currentTimeMillis(),
                (int)(Math.random() * 1000));
    }

}