package org.example.integration.config;

import org.example.dao.AccountDao;
import org.example.dao.UserDao;
import org.example.entity.Account;
import org.example.entity.User;
import org.example.entity.enums.AccountType;
import org.example.entity.enums.CurrencyCode;
import org.example.entity.enums.Status;
import org.example.mapper.AccountReadMapper;
import org.example.mapper.RoleReadMapper;
import org.example.mapper.UserReadMapper;
import org.example.service.AccountService;
import org.example.util.PasswordUtil;
import org.junit.jupiter.api.BeforeAll;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.math.BigDecimal;

public class AbstractAccountServiceIntegrationTest extends AbstractIntegrationTest {

    protected UserDao userDao;
    protected AccountDao accountDao;
    protected AccountReadMapper accountReadMapper;
    protected AccountService accountService;

    @BeforeAll
    void initServices() {
        userDao = new UserDao();
        accountDao = new AccountDao();
        accountReadMapper = new AccountReadMapper(new UserReadMapper(new RoleReadMapper())  );
        accountService = new AccountService(sessionFactory, userDao, accountDao, accountReadMapper);
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ДЛЯ ТЕСТОВ ==========

    protected User createUserInDB(String email, String password, String firstName, String lastName) {
        EntityManager em = sessionFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            User user = User.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .passwordHash(PasswordUtil.hash(password))
                    .build();

            em.persist(user);
            tx.commit();
            return user;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    protected Account createAccountInDB(Long userId, String accountNumber, CurrencyCode currencyCode,
                                        AccountType accountType, Status status, BigDecimal balance) {
        EntityManager em = sessionFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            User user = em.find(User.class, userId);
            if (user == null) {
                throw new RuntimeException("User not found with id: " + userId);
            }

            Account account = new Account(user, accountNumber, balance, currencyCode, accountType);

            if(status != Status.ACTIVE) account.setStatus(status);

            em.persist(account);
            tx.commit();
            return account;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    protected Account createAccountInDB(Long userId, AccountType accountType) {
        String accountNumber = generateAccountNumber(userId);
        return createAccountInDB(userId, accountNumber, CurrencyCode.US, accountType, Status.ACTIVE, BigDecimal.ZERO);
    }

    protected void deleteAllAccounts() {
        runSql("clean/cleanUp_accountsTable.sql");
    }

    protected void deleteAllUsers() {
       runSql("clean/cleanUp_userTable.sql");
    }

    protected String generateAccountNumber(Long userId) {
        return String.format("ACNT-%d-%d-%d",
                userId,
                System.currentTimeMillis(),
                (int)(Math.random() * 1000));
    }
}