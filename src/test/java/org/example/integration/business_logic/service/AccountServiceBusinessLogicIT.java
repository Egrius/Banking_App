package org.example.integration.business_logic.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.account.AccountCreateDto;
import org.example.dto.account.AccountReadDto;
import org.example.dto.account.AccountSummaryDto;
import org.example.dto.balance_audit.BalanceAuditReadDto;
import org.example.dto.request.PageRequest;
import org.example.dto.response.PageResponse;
import org.example.entity.Account;
import org.example.entity.User;
import org.example.entity.enums.AccountType;
import org.example.entity.enums.CurrencyCode;
import org.example.entity.enums.Status;
import org.example.exception.account.AccountAlreadyExistsException;
import org.example.exception.security_exception.AccessDeniedException;
import org.example.integration.config.AbstractAccountServiceIntegrationTest;
import org.example.security.AuthContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class AccountServiceBusinessLogicIT extends AbstractAccountServiceIntegrationTest {

    @BeforeEach
    void cleanUp() {
        deleteAllAccounts();
        deleteAllUsers();
        runSql("role_service/init_role_table.sql");
    }

    @Nested
    class CreateAccountTests {

        @Test
        void createAccount_shouldCreateNewAccountSuccessfully() {
            User user = createUserInDB("accounttest@test.com", "password123", "Test", "User");
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of());

            AccountCreateDto createDto = new AccountCreateDto(user.getId(), CurrencyCode.US, AccountType.CURRENT);

            AccountReadDto createdAccount = accountService.createAccount(createDto);

            assertNotNull(createdAccount);
            assertNotNull(createdAccount.id());
            assertEquals(user.getId(), createdAccount.user().id());
            assertEquals(CurrencyCode.US, createdAccount.currencyCode());
            assertEquals(AccountType.CURRENT, createdAccount.accountType());
            assertEquals(Status.ACTIVE, createdAccount.status());
            assertEquals(BigDecimal.ZERO, createdAccount.balance());
            assertNotNull(createdAccount.accountNumber());
            assertNotNull(createdAccount.openingDate());

            // Проверка в БД
            Account accountFromDb = accountDao.findById(sessionFactory.createEntityManager(), createdAccount.id()).orElseThrow();
            assertEquals(createdAccount.accountNumber(), accountFromDb.getAccountNumber());
            assertEquals(createdAccount.balance().doubleValue(), accountFromDb.getBalance().doubleValue());
        }

        @Test
        void createAccount_shouldThrowEntityNotFoundException_whenUserNotFound() {
            AuthContext authContext = new AuthContext(999L, "admin@test.com", List.of("ADMIN"));
            AccountCreateDto createDto = new AccountCreateDto(999L, CurrencyCode.US, AccountType.CURRENT);

            assertThatThrownBy(() -> accountService.createAccount(createDto))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Пользователь не найден");
        }

        @Test
        void createAccount_shouldThrowAccountAlreadyExistsException_whenAccountTypeExists() {
            User user = createUserInDB("duplicate@test.com", "password123", "Test", "User");
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of());

            // Создаем первый счет типа CURRENT
            createAccountInDB(user.getId(), AccountType.CURRENT);

            AccountCreateDto duplicateDto = new AccountCreateDto(user.getId(), CurrencyCode.US, AccountType.CURRENT);

            assertThatThrownBy(() -> accountService.createAccount(duplicateDto))
                    .isInstanceOf(AccountAlreadyExistsException.class)
                    .hasMessageContaining("уже есть счет такого типа");
        }

        @Test
        void createAccount_shouldAllowDifferentAccountTypes() {
            User user = createUserInDB("differenttypes@test.com", "password123", "Test", "User");
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of());

            // Создаем CURRENT счет
            createAccountInDB(user.getId(), AccountType.CURRENT);

            // Пытаемся создать SAVINGS счет
            AccountCreateDto savingsDto = new AccountCreateDto(user.getId(), CurrencyCode.US, AccountType.SAVINGS);

            AccountReadDto savingsAccount = accountService.createAccount(savingsDto);

            assertNotNull(savingsAccount);
            assertEquals(AccountType.SAVINGS, savingsAccount.accountType());
        }
    }

    @Nested
    class GetAccountTests {

        @Test
        void getAccount_shouldReturnAccount_whenAccountExists() {
            User user = createUserInDB("getaccount@test.com", "password123", "Test", "User");
            Account account = createAccountInDB(user.getId(), AccountType.CURRENT);
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of());

            AccountReadDto foundAccount = accountService.getAccount(account.getId(), authContext);

            assertNotNull(foundAccount);
            assertEquals(account.getId(), foundAccount.id());
            assertEquals(account.getAccountNumber(), foundAccount.accountNumber());
            assertEquals(account.getCurrencyCode(), foundAccount.currencyCode());
            assertEquals(account.getAccountType(), foundAccount.accountType());
        }

        @Test
        void getAccount_shouldThrowEntityNotFoundException_whenAccountNotFound() {
            User user = createUserInDB("getaccount@test.com", "password123", "Test", "User");
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of());

            assertThatThrownBy(() -> accountService.getAccount(999L, authContext))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("не найден");
        }

        @Test
        void getAccount_shouldThrowAccessDeniedException_whenUserNotOwner() {
            User owner = createUserInDB("owner@test.com", "password123", "Test", "User");
            User otherUser = createUserInDB("other@test.com", "password123", "Other", "User");
            Account account = createAccountInDB(owner.getId(), AccountType.CURRENT);
            AuthContext authContext = new AuthContext(otherUser.getId(), otherUser.getEmail(), List.of());

            assertThatThrownBy(() -> accountService.getAccount(account.getId(), authContext))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Недостаточно прав");
        }
    }

    @Nested
    class GetUserAccountsTests {

        @Test
        void getUserAccounts_shouldReturnAllUserAccounts() {
            User user = createUserInDB("useraccounts@test.com", "password123", "Test", "User");

            Account currentAccount = createAccountInDB(user.getId(), AccountType.CURRENT);
            Account savingsAccount = createAccountInDB(user.getId(), AccountType.SAVINGS);

            List<AccountSummaryDto> userAccounts = accountService.getUserAccounts(user.getId());

            assertNotNull(userAccounts);
            assertEquals(2, userAccounts.size());

            AccountSummaryDto currentSummary = userAccounts.stream()
                    .filter(a -> a.accountType() == AccountType.CURRENT)
                    .findFirst()
                    .orElseThrow();

            assertEquals(currentAccount.getId(), currentSummary.id());
            assertEquals(currentAccount.getAccountNumber(), currentSummary.accountNumber());
            assertEquals(currentAccount.getCurrencyCode(), currentSummary.currencyCode());
        }

        @Test
        void getUserAccounts_shouldReturnEmptyList_whenUserHasNoAccounts() {
            User user = createUserInDB("noaccounts@test.com", "password123", "Test", "User");

            List<AccountSummaryDto> userAccounts = accountService.getUserAccounts(user.getId());

            assertNotNull(userAccounts);
            assertTrue(userAccounts.isEmpty());
        }
    }

    @Nested
    class CloseAccountTests {

        @Test
        void closeAccount_shouldCloseActiveAccountSuccessfully() {
            User user = createUserInDB("closeaccount@test.com", "password123", "Test", "User");
            Account account = createAccountInDB(user.getId(), AccountType.CURRENT);
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of());

            assertEquals(Status.ACTIVE, account.getStatus());
            assertNull(account.getClosingDate());

            accountService.closeAccount(account.getId(), authContext);

            EntityManager em = sessionFactory.createEntityManager();
            try {
                Account closedAccount = accountDao.findById(em, account.getId()).orElseThrow();
                assertEquals(Status.CLOSED, closedAccount.getStatus());
                assertNotNull(closedAccount.getClosingDate());
            } finally {
                em.close();
            }
        }

        @Test
        void closeAccount_shouldThrowIllegalStateException_whenAccountAlreadyClosed() {
            User user = createUserInDB("alreadyclosed@test.com", "password123", "Test", "User");
            Account account = createAccountInDB(user.getId(), AccountType.CURRENT);
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of());

            accountService.closeAccount(account.getId(), authContext);

            assertThatThrownBy(() -> accountService.closeAccount(account.getId(), authContext))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("уже закрыт");
        }

        @Test
        void closeAccount_shouldThrowIllegalStateException_whenAccountBlocked() {
            User user = createUserInDB("blockedclose@test.com", "password123", "Test", "User");
            Account account = createAccountInDB(user.getId(), "BLOCKED-ACC", CurrencyCode.US,
                    AccountType.CURRENT, Status.BLOCKED, BigDecimal.ZERO);
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of());

            assertThatThrownBy(() -> accountService.closeAccount(account.getId(), authContext))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Нельзя закрыть заблокированный счет");
        }

        @Test
        void closeAccount_shouldThrowEntityNotFoundException_whenAccountNotFound() {
            User user = createUserInDB("closeaccount@test.com", "password123", "Test", "User");
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of());

            assertThatThrownBy(() -> accountService.closeAccount(999L, authContext))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("не найден");
        }
    }

    @Nested
    class BlockAccountTests {

        @Test
        void blockAccount_shouldBlockActiveAccountSuccessfully() {
            User user = createUserInDB("blockaccount@test.com", "password123", "Test", "User");
            Account account = createAccountInDB(user.getId(), AccountType.CURRENT);
            AuthContext adminContext = new AuthContext(user.getId(), user.getEmail(), List.of("ADMIN"));

            assertEquals(Status.ACTIVE, account.getStatus());

            accountService.blockAccount(account.getId(), "Подозрительная активность", adminContext);

            EntityManager em = sessionFactory.createEntityManager();
            try {
                Account blockedAccount = accountDao.findById(em, account.getId()).orElseThrow();
                assertEquals(Status.BLOCKED, blockedAccount.getStatus());
            } finally {
                em.close();
            }
        }

        @Test
        void blockAccount_shouldThrowIllegalStateException_whenAccountAlreadyBlocked() {
            User user = createUserInDB("alreadyblocked@test.com", "password123", "Test", "User");
            Account account = createAccountInDB(user.getId(), "BLOCKED-ACC", CurrencyCode.US,
                    AccountType.CURRENT, Status.BLOCKED, BigDecimal.ZERO);
            AuthContext adminContext = new AuthContext(user.getId(), user.getEmail(), List.of("ADMIN"));

            assertThatThrownBy(() -> accountService.blockAccount(account.getId(), "Another reason", adminContext))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("уже заблокирован");
        }

        @Test
        void blockAccount_shouldThrowIllegalStateException_whenAccountClosed() {
            User user = createUserInDB("closedblock@test.com", "password123", "Test", "User");
            Account account = createAccountInDB(user.getId(), "CLOSED-ACC", CurrencyCode.US,
                    AccountType.CURRENT, Status.CLOSED, BigDecimal.ZERO);
            AuthContext adminContext = new AuthContext(user.getId(), user.getEmail(), List.of("ADMIN"));

            assertThatThrownBy(() -> accountService.blockAccount(account.getId(), "Try block closed", adminContext))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void blockAccount_shouldThrowEntityNotFoundException_whenAccountNotFound() {
            User user = createUserInDB("blockaccount@test.com", "password123", "Test", "User");
            AuthContext adminContext = new AuthContext(user.getId(), user.getEmail(), List.of("ADMIN"));

            assertThatThrownBy(() -> accountService.blockAccount(999L, "Reason", adminContext))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("не найден");
        }
    }

    @Nested
    class GetBalanceTests {

        @Test
        void getBalance_shouldReturnCorrectBalance() {
            User user = createUserInDB("getbalance@test.com", "password123", "Test", "User");
            Account account = createAccountInDB(user.getId(), "BAL-ACC", CurrencyCode.US,
                    AccountType.CURRENT, Status.ACTIVE, new BigDecimal("1500.50"));
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of());

            BigDecimal balance = accountService.getBalance(account.getId(), authContext);

            assertEquals(new BigDecimal("1500.50"), balance);
        }

        @Test
        void getBalance_shouldReturnZeroForNewAccount() {
            User user = createUserInDB("zerobalance@test.com", "password123", "Test", "User");
            Account account = createAccountInDB(user.getId(), AccountType.CURRENT);
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of());

            BigDecimal balance = accountService.getBalance(account.getId(), authContext);

            assertEquals(BigDecimal.ZERO.doubleValue(), balance.doubleValue());
        }

        @Test
        void getBalance_shouldThrowEntityNotFoundException_whenAccountNotFound() {
            User user = createUserInDB("getbalance@test.com", "password123", "Test", "User");
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of());

            assertThatThrownBy(() -> accountService.getBalance(999L, authContext))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("не найден");
        }
    }

    @Nested
    class GetBalanceAuditTests {

        @Test
        void getBalanceAudit_shouldReturnEmptyPage_whenNoAudits() {
            User user = createUserInDB("audittest@test.com", "password123", "Test", "User");
            Account account = createAccountInDB(user.getId(), AccountType.CURRENT);
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of());

            PageRequest pageRequest = PageRequest.of(0, 10);

            PageResponse<BalanceAuditReadDto> auditPage = accountService.getBalanceAudit(
                    account.getId(), pageRequest, authContext);

            assertNotNull(auditPage);
            assertEquals(0, auditPage.getPageNumber());
            assertEquals(10, auditPage.getPageSize());
            assertEquals(0, auditPage.getTotalElements());
            assertTrue(auditPage.getContent().isEmpty());
        }

        @Test
        void getBalanceAudit_shouldThrowEntityNotFoundException_whenAccountNotFound() {
            User user = createUserInDB("audittest@test.com", "password123", "Test", "User");
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of());
            PageRequest pageRequest = PageRequest.of(0, 10);

            assertThatThrownBy(() -> accountService.getBalanceAudit(999L, pageRequest, authContext))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Счет не найден");
        }
    }

    @Nested
    class AccountNumberGenerationTests {

        @Test
        void createAccount_shouldGenerateUniqueAccountNumbers() {
            User user = createUserInDB("uniquenumbers@test.com", "password123", "Test", "User");
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of());

            AccountCreateDto createDto1 = new AccountCreateDto(user.getId(), CurrencyCode.US, AccountType.CURRENT);
            AccountCreateDto createDto2 = new AccountCreateDto(user.getId(), CurrencyCode.EUR, AccountType.SAVINGS);

            AccountReadDto account1 = accountService.createAccount(createDto1);
            AccountReadDto account2 = accountService.createAccount(createDto2);

            assertNotNull(account1.accountNumber());
            assertNotNull(account2.accountNumber());
            assertNotEquals(account1.accountNumber(), account2.accountNumber());
        }

        @Test
        void createAccount_shouldGenerateAccountNumberInCorrectFormat() {
            User user = createUserInDB("format@test.com", "password123", "Test", "User");
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of());
            AccountCreateDto createDto = new AccountCreateDto(user.getId(), CurrencyCode.US, AccountType.CURRENT);

            AccountReadDto account = accountService.createAccount(createDto);

            String accountNumber = account.accountNumber();
            assertTrue(accountNumber.matches("ACNT-\\d+-\\d+-\\d+"),
                    "Account number should match format ACNT-{userId}-{timestamp}-{random}");
            assertTrue(accountNumber.contains(user.getId().toString()));
        }
    }

    @Nested
    class MultipleAccountsTests {

        @Test
        void createAccount_shouldAllowMultipleDifferentAccountsForSameUser() {
            User user = createUserInDB("multi@test.com", "password123", "Test", "User");
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of());

            AccountReadDto currentAccount = accountService.createAccount(
                    new AccountCreateDto(user.getId(), CurrencyCode.US, AccountType.CURRENT));
            AccountReadDto savingsAccount = accountService.createAccount(
                    new AccountCreateDto(user.getId(), CurrencyCode.US, AccountType.SAVINGS));
            AccountReadDto businessAccount = accountService.createAccount(
                    new AccountCreateDto(user.getId(), CurrencyCode.US, AccountType.CREDIT));

            assertNotNull(currentAccount);
            assertNotNull(savingsAccount);
            assertNotNull(businessAccount);

            List<AccountSummaryDto> userAccounts = accountService.getUserAccounts(user.getId());
            assertEquals(3, userAccounts.size());
        }
    }
}