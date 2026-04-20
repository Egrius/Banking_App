package org.example.integration.service.config;

import org.example.dao.AccountDao;
import org.example.dao.CardDao;
import org.example.entity.Account;
import org.example.entity.Card;
import org.example.entity.User;
import org.example.entity.enums.AccountType;
import org.example.entity.enums.CardStatus;
import org.example.entity.enums.CardType;
import org.example.entity.enums.CurrencyCode;
import org.example.integration.AbstractIntegrationTest;
import org.example.service.CardService;
import org.example.util.PasswordUtil;
import org.junit.jupiter.api.BeforeAll;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AbstractCardServiceIntegrationTest extends AbstractIntegrationTest {

    protected CardDao cardDao;
    protected AccountDao accountDao;
    protected CardService cardService;

    private static final DateTimeFormatter EXPIRY_FORMAT = DateTimeFormatter.ofPattern("MM/yy");

    @BeforeAll
    void initServices() {
        cardDao = new CardDao();
        accountDao = new AccountDao();
        cardService = new CardService(sessionFactory, cardDao, accountDao);
    }

    // хелпер-методы

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

    protected Account createAccountInDB(Long userId, String accountNumber, CurrencyCode currencyCode, AccountType accountType) {
        EntityManager em = sessionFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            User user = em.find(User.class, userId);
            if (user == null) {
                throw new RuntimeException("User not found with id: " + userId);
            }

            Account account = new Account(user, accountNumber, BigDecimal.ZERO, currencyCode, accountType);

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

    protected Card createCardInDB(Long accountId, String cardNumber, String cardholderName,
                                  CardType cardType, CardStatus status, String name) {
        EntityManager em = sessionFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            Account account = em.find(Account.class, accountId);
            if (account == null) {
                throw new RuntimeException("Account not found with id: " + accountId);
            }

            Card card = Card.builder()
                    .user(account.getUser())
                    .account(account)
                    .cardNumber(cardNumber)
                    .currencyCode(account.getCurrencyCode())
                    .cardholderName(cardholderName)
                    .expiryDate(LocalDateTime.now().plusYears(5))
                    .cardType(cardType)
                    .status(status)
                    .name(name)
                    .build();

            em.persist(card);
            tx.commit();
            return card;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }



    protected void deleteAllCards() {
        runSql("clean/cleanUp_cardTable.sql");
    }

    protected void deleteAllAccounts() {
        runSql("clean/cleanUp_userTable.sql");
    }

    protected void deleteAllUsers() {
        runSql("clean/cleanUp_accountsTable.sql");
    }

    protected String generateCardNumber() {
        long random = (long) (Math.random() * 10_000_000_000L);
        String last4 = String.format("%04d", random % 10000);
        return String.format("****-****-****-%s", last4);
    }

    protected String generateAccountNumber(Long userId) {
        return  String.format("ACNT-%d-%d-%d",
                userId,
                System.currentTimeMillis(),
                (int)(Math.random() * 1000));
    }
}