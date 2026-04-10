package org.example.unit.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.EntityTransaction;
import org.example.dao.AccountDao;
import org.example.dao.UserDao;
import org.example.dto.account.AccountCreateDto;
import org.example.dto.account.AccountReadDto;
import org.example.dto.account.AccountSummaryDto;
import org.example.dto.balance_audit.BalanceAuditReadDto;
import org.example.dto.request.PageRequest;
import org.example.dto.response.PageResponse;
import org.example.dto.role.RoleReadDto;
import org.example.dto.user.UserReadDto;
import org.example.entity.Account;
import org.example.entity.AccountBalanceAudit;
import org.example.entity.User;
import org.example.entity.enums.AccountType;
import org.example.entity.enums.CurrencyCode;
import org.example.entity.enums.Status;
import org.example.exception.account.AccountAlreadyExistsException;
import org.example.mapper.AccountReadMapper;
import org.example.security.AuthContext;
import org.example.service.AccountService;
import org.example.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceUnitTest {
/*
    @Mock
    private UserDao userDao;

    @Mock
    private AccountDao accountDao;

    @Mock
    private AccountReadMapper accountReadMapper;

    @Mock
    private EntityManagerFactory emf;

    @InjectMocks
    private AccountService accountService;

    private EntityManager mockEM = Mockito.mock(EntityManager.class);

    private EntityTransaction mockTx = Mockito.mock(EntityTransaction.class);

    @BeforeEach
    void getMockEntityManager() {
        when(emf.createEntityManager()).thenReturn(mockEM);
        when(mockEM.getTransaction()).thenReturn(mockTx);

    }

    @Nested
    class CreateAccountTests {

        @Test
        void createAccount_shouldCreate_ifDataIsCorrect() {

            final Long USER_ID = 1L;
            final List<String> USER_ROLES = List.of("USER");
            final String USER_EMAIL = "mock@gmail.com";
            final String ACCOUNT_NUMBER = "ACNT-123";

            AccountCreateDto correctCreateDto = new AccountCreateDto(USER_ID, CurrencyCode.RUB, AccountType.CURRENT);

            UserReadDto expectedUserReadDto = new UserReadDto(
                    1L,
                    "Mock", "Mock", USER_EMAIL,
                    LocalDateTime.now(),
                    USER_ROLES.stream().map(RoleReadDto::new).toList());

            AccountReadDto accountReadDto =   new AccountReadDto(
                    1L,
                    expectedUserReadDto, ACCOUNT_NUMBER,
                    BigDecimal.ZERO, correctCreateDto.currencyCode(),
                    correctCreateDto.accountType(), Status.ACTIVE,LocalDateTime.now(), null);
            


            User mockUser = mock(User.class);
            when(mockUser.getId()).thenReturn(USER_ID);

            when(userDao.findById(mockEM, USER_ID))
                    .thenReturn(Optional.of(mockUser));

            when(accountDao.existsByUserIdAndType(mockEM, USER_ID, correctCreateDto.accountType()))
                    .thenReturn(false);

            doNothing().when(accountDao).save(eq(mockEM), any(Account.class));

            when(accountReadMapper.map(any(Account.class))).thenReturn(accountReadDto);

            AccountReadDto actualResult = accountService.createAccount(correctCreateDto);

            assertNotNull(actualResult);
            assertEquals(actualResult.user().email(), accountReadDto.user().email());
            assertEquals(actualResult.accountNumber(), accountReadDto.accountNumber());
            assertEquals(actualResult.accountType(), accountReadDto.accountType());
            assertEquals(actualResult.balance(), accountReadDto.balance());
            assertEquals(actualResult.openingDate(), accountReadDto.openingDate());

            verify(mockTx).begin();
            verify(mockTx).commit();
            verify(mockEM).close();
        }

        @Test
        void createAccount_shouldThrowEntityNotFoundException_whenUserNotFound() {
            final Long WRONG_USER_ID = 999L;
            final List<String> USER_ROLES = List.of("USER");
            final String USER_EMAIL = "mock@gmail.com";
            final String EXPECTED_ERROR_MESSAGE = "Пользователь не найден";

            AccountCreateDto wrongCreateDto = new AccountCreateDto(WRONG_USER_ID, CurrencyCode.RUB, AccountType.CURRENT);

            when(userDao.findById(mockEM, WRONG_USER_ID))
                    .thenReturn(Optional.empty());

            when(mockTx.isActive()).thenReturn(true);

            assertThatThrownBy(() -> accountService.createAccount(wrongCreateDto))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(EXPECTED_ERROR_MESSAGE);

            verify(mockTx).begin();
            verify(mockTx).rollback();
            verify(mockEM).close();

            verify(accountDao, never()).existsByUserIdAndType(any(EntityManager.class), any(Long.class), any(AccountType.class));
            verify(accountDao, never()).save(any(EntityManager.class), any(Account.class));
            verify(mockTx, never()).commit();
            verify(accountReadMapper, never()).map(any(Account.class));

        }

        @Test
        void createAccount_shouldThrowAccountAlreadyExistsException_whenAccountOfTheSameTypeExists() {
            final Long USER_ID = 1L;
            final List<String> USER_ROLES = List.of("USER");
            final String USER_EMAIL = "mock@gmail.com";
            final String EXPECTED_ERROR_MESSAGE = "У пользователя уже есть счет такого типа";

            AccountCreateDto accountCreateDto = new AccountCreateDto(USER_ID, CurrencyCode.RUB, AccountType.CURRENT);

            User mockUser = Mockito.mock(User.class);
            when(mockUser.getId()).thenReturn(USER_ID);

            when(userDao.findById(mockEM, USER_ID))
                    .thenReturn(Optional.of(mockUser));


            when(accountDao.existsByUserIdAndType(mockEM, USER_ID, accountCreateDto.accountType()))
                    .thenReturn(true);

            when(mockTx.isActive()).thenReturn(true);

            assertThatThrownBy(() -> accountService.createAccount(accountCreateDto))
                    .isInstanceOf(AccountAlreadyExistsException.class)
                    .hasMessageContaining(EXPECTED_ERROR_MESSAGE);

            verify(mockTx).begin();
            verify(mockTx).rollback();
            verify(mockEM).close();


            verify(accountDao, never()).save(any(EntityManager.class), any(Account.class));
            verify(mockTx, never()).commit();
            verify(accountReadMapper, never()).map(any(Account.class));

        }
    }


    @Nested
    class GetUserAccountsTests {

        @Test
        void getUserAccounts_shouldReturnListOfAccountSummaries_whenUserHasAccounts() {
            final Long USER_ID = 1L;
            final List<String> USER_ROLES = List.of("USER");
            final String USER_EMAIL = "mock@gmail.com";

           

            Account account1 = mock(Account.class);
            Account account2 = mock(Account.class);
            List<Account> accounts = List.of(account1, account2);

            when(account1.getId()).thenReturn(1L);
            when(account1.getAccountNumber()).thenReturn("ACNT-111");
            when(account1.getCurrencyCode()).thenReturn(CurrencyCode.RUB);
            when(account1.getAccountType()).thenReturn(AccountType.CURRENT);

            when(account2.getId()).thenReturn(2L);
            when(account2.getAccountNumber()).thenReturn("ACNT-222");
            when(account2.getCurrencyCode()).thenReturn(CurrencyCode.US);
            when(account2.getAccountType()).thenReturn(AccountType.SAVINGS);

            when(accountDao.findUserAccountsByUserId(mockEM, USER_ID)).thenReturn(accounts);

            List<AccountSummaryDto> result = accountService.getUserAccounts(USER_ID);

            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(1L, result.get(0).id());
            assertEquals("ACNT-111", result.get(0).accountNumber());
            assertEquals(CurrencyCode.RUB, result.get(0).currencyCode());
            assertEquals(AccountType.CURRENT, result.get(0).accountType());

            verify(mockEM).close();
        }

        @Test
        void getUserAccounts_shouldReturnEmptyList_whenUserHasNoAccounts() {
            final Long USER_ID = 1L;
            final List<String> USER_ROLES = List.of("USER");
            final String USER_EMAIL = "mock@gmail.com";

           

            when(accountDao.findUserAccountsByUserId(mockEM, USER_ID)).thenReturn(List.of());

            List<AccountSummaryDto> result = accountService.getUserAccounts(USER_ID);

            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(mockEM).close();
        }
    }

    @Nested
    class CloseAccountTests {

        @Test
        void closeAccount_shouldCloseSuccessfully_whenAccountIsActive() {
            final Long USER_ID = 1L;
            final Long ACCOUNT_ID = 1L;
            final List<String> USER_ROLES = List.of("USER");
            final String USER_EMAIL = "mock@gmail.com";

           

            Account mockAccount = mock(Account.class);
            User mockUser = mock(User.class);

            when(mockUser.getId()).thenReturn(USER_ID);
            when(mockAccount.getUser()).thenReturn(mockUser);
            when(mockAccount.getStatus()).thenReturn(Status.ACTIVE);

            when(accountDao.findById(mockEM, ACCOUNT_ID)).thenReturn(Optional.of(mockAccount));

            accountService.closeAccount(ACCOUNT_ID);

            verify(mockAccount).setStatus(Status.CLOSED);
            verify(mockAccount).setClosingDate(any(LocalDateTime.class));
            verify(accountDao).save(mockEM, mockAccount);
            verify(mockTx).begin();
            verify(mockTx).commit();
            verify(mockEM).close();
        }

        @Test
        void closeAccount_shouldThrowEntityNotFoundException_whenAccountNotFound() {
            final Long USER_ID = 1L;
            final Long WRONG_ACCOUNT_ID = 999L;
            final List<String> USER_ROLES = List.of("USER");
            final String USER_EMAIL = "mock@gmail.com";
            final String EXPECTED_ERROR_MESSAGE = "Счет с id " + WRONG_ACCOUNT_ID + " не найден";

           

            when(accountDao.findById(mockEM, WRONG_ACCOUNT_ID)).thenReturn(Optional.empty());
            when(mockTx.isActive()).thenReturn(true);

            assertThatThrownBy(() -> accountService.closeAccount(WRONG_ACCOUNT_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(EXPECTED_ERROR_MESSAGE);

            verify(mockTx).begin();
            verify(mockTx).rollback();
            verify(mockEM).close();
            verify(accountDao, never()).save(any(EntityManager.class), any(Account.class));
        }

        @Test
        void closeAccount_shouldThrowIllegalStateException_whenAccountAlreadyClosed() {
            final Long USER_ID = 1L;
            final Long ACCOUNT_ID = 1L;
            final List<String> USER_ROLES = List.of("USER");
            final String USER_EMAIL = "mock@gmail.com";
            final String EXPECTED_ERROR_MESSAGE = "Счет уже закрыт";

           

            Account mockAccount = mock(Account.class);
            User mockUser = mock(User.class);

            when(mockUser.getId()).thenReturn(USER_ID);
            when(mockAccount.getUser()).thenReturn(mockUser);
            when(mockAccount.getStatus()).thenReturn(Status.CLOSED);

            when(accountDao.findById(mockEM, ACCOUNT_ID)).thenReturn(Optional.of(mockAccount));
            when(mockTx.isActive()).thenReturn(true);

            assertThatThrownBy(() -> accountService.closeAccount(ACCOUNT_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(EXPECTED_ERROR_MESSAGE);

            verify(mockTx).begin();
            verify(mockTx).rollback();
            verify(mockEM).close();
            verify(accountDao, never()).save(any(EntityManager.class), any(Account.class));
        }

        @Test
        void closeAccount_shouldThrowIllegalStateException_whenAccountBlocked() {
            final Long USER_ID = 1L;
            final Long ACCOUNT_ID = 1L;
            final List<String> USER_ROLES = List.of("USER");
            final String USER_EMAIL = "mock@gmail.com";
            final String EXPECTED_ERROR_MESSAGE = "Нельзя закрыть заблокированный счет";

           

            Account mockAccount = mock(Account.class);
            User mockUser = mock(User.class);

            when(mockUser.getId()).thenReturn(USER_ID);
            when(mockAccount.getUser()).thenReturn(mockUser);
            when(mockAccount.getStatus()).thenReturn(Status.BLOCKED);

            when(accountDao.findById(mockEM, ACCOUNT_ID)).thenReturn(Optional.of(mockAccount));
            when(mockTx.isActive()).thenReturn(true);

            assertThatThrownBy(() -> accountService.closeAccount(ACCOUNT_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(EXPECTED_ERROR_MESSAGE);

            verify(mockTx).begin();
            verify(mockTx).rollback();
            verify(mockEM).close();
            verify(accountDao, never()).save(any(EntityManager.class), any(Account.class));
        }
    }

    @Nested
    class BlockAccountTests {

        @Test
        void blockAccount_shouldBlockSuccessfully_whenAccountIsActive() {
            final Long USER_ID = 1L;
            final Long ACCOUNT_ID = 1L;
            final String REASON = "Suspicious activity";
            final List<String> USER_ROLES = List.of("ADMIN");
            final String USER_EMAIL = "admin@gmail.com";

           

            Account mockAccount = mock(Account.class);
            User mockUser = mock(User.class);

            when(mockUser.getId()).thenReturn(USER_ID);
            when(mockAccount.getUser()).thenReturn(mockUser);
            when(mockAccount.getStatus()).thenReturn(Status.ACTIVE);

            when(accountDao.findById(mockEM, ACCOUNT_ID)).thenReturn(Optional.of(mockAccount));

            accountService.blockAccount(ACCOUNT_ID, REASON);

            verify(mockAccount).setStatus(Status.BLOCKED);
            verify(accountDao).save(mockEM, mockAccount);
            verify(mockTx).begin();
            verify(mockTx).commit();
            verify(mockEM).close();
        }

        @Test
        void blockAccount_shouldThrowEntityNotFoundException_whenAccountNotFound() {
            final Long USER_ID = 1L;
            final Long WRONG_ACCOUNT_ID = 999L;
            final String REASON = "Suspicious activity";
            final List<String> USER_ROLES = List.of("ADMIN");
            final String USER_EMAIL = "admin@gmail.com";
            final String EXPECTED_ERROR_MESSAGE = "Счет с id " + WRONG_ACCOUNT_ID + " не найден";

           

            when(accountDao.findById(mockEM, WRONG_ACCOUNT_ID)).thenReturn(Optional.empty());
            when(mockTx.isActive()).thenReturn(true);

            assertThatThrownBy(() -> accountService.blockAccount(WRONG_ACCOUNT_ID, REASON))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(EXPECTED_ERROR_MESSAGE);

            verify(mockTx).begin();
            verify(mockTx).rollback();
            verify(mockEM).close();
            verify(accountDao, never()).save(any(EntityManager.class), any(Account.class));
        }

        @Test
        void blockAccount_shouldThrowIllegalStateException_whenAccountAlreadyBlocked() {
            final Long USER_ID = 1L;
            final Long ACCOUNT_ID = 1L;
            final String REASON = "Suspicious activity";
            final List<String> USER_ROLES = List.of("ADMIN");
            final String USER_EMAIL = "admin@gmail.com";
            final String EXPECTED_ERROR_MESSAGE = "Счет уже заблокирован";

           

            Account mockAccount = mock(Account.class);
            User mockUser = mock(User.class);

            when(mockUser.getId()).thenReturn(USER_ID);
            when(mockAccount.getUser()).thenReturn(mockUser);
            when(mockAccount.getStatus()).thenReturn(Status.BLOCKED);

            when(accountDao.findById(mockEM, ACCOUNT_ID)).thenReturn(Optional.of(mockAccount));
            when(mockTx.isActive()).thenReturn(true);

            assertThatThrownBy(() -> accountService.blockAccount(ACCOUNT_ID, REASON))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(EXPECTED_ERROR_MESSAGE);

            verify(mockTx).begin();
            verify(mockTx).rollback();
            verify(mockEM).close();
            verify(accountDao, never()).save(any(EntityManager.class), any(Account.class));
        }

        @Test
        void blockAccount_shouldThrowIllegalStateException_whenAccountClosed() {
            final Long USER_ID = 1L;
            final Long ACCOUNT_ID = 1L;
            final String REASON = "Suspicious activity";
            final List<String> USER_ROLES = List.of("ADMIN");
            final String USER_EMAIL = "admin@gmail.com";
            final String EXPECTED_ERROR_MESSAGE = "Нельзя заблокировать закрытый счет";

           

            Account mockAccount = mock(Account.class);
            User mockUser = mock(User.class);

            when(mockUser.getId()).thenReturn(USER_ID);
            when(mockAccount.getUser()).thenReturn(mockUser);
            when(mockAccount.getStatus()).thenReturn(Status.CLOSED);

            when(accountDao.findById(mockEM, ACCOUNT_ID)).thenReturn(Optional.of(mockAccount));
            when(mockTx.isActive()).thenReturn(true);

            assertThatThrownBy(() -> accountService.blockAccount(ACCOUNT_ID, REASON))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(EXPECTED_ERROR_MESSAGE);

            verify(mockTx).begin();
            verify(mockTx).rollback();
            verify(mockEM).close();
            verify(accountDao, never()).save(any(EntityManager.class), any(Account.class));
        }
    }

    @Nested
    class GetBalanceTests {

        @Test
        void getBalance_shouldReturnBalance_whenAccountFound() {
            final Long USER_ID = 1L;
            final Long ACCOUNT_ID = 1L;
            final List<String> USER_ROLES = List.of("USER");
            final String USER_EMAIL = "mock@gmail.com";
            final BigDecimal EXPECTED_BALANCE = BigDecimal.valueOf(1000.50);

           

            Account mockAccount = mock(Account.class);
            User mockUser = mock(User.class);

            when(mockUser.getId()).thenReturn(USER_ID);
            when(mockAccount.getUser()).thenReturn(mockUser);
            when(mockAccount.getBalance()).thenReturn(EXPECTED_BALANCE);

            when(accountDao.findById(mockEM, ACCOUNT_ID)).thenReturn(Optional.of(mockAccount));

            BigDecimal result = accountService.getBalance(ACCOUNT_ID);

            assertNotNull(result);
            assertEquals(EXPECTED_BALANCE, result);
            verify(mockEM).close();
        }

        @Test
        void getBalance_shouldThrowEntityNotFoundException_whenAccountNotFound() {
            final Long USER_ID = 1L;
            final Long WRONG_ACCOUNT_ID = 999L;
            final List<String> USER_ROLES = List.of("USER");
            final String USER_EMAIL = "mock@gmail.com";
            final String EXPECTED_ERROR_MESSAGE = "Счет с id " + WRONG_ACCOUNT_ID + " не найден";

           

            when(accountDao.findById(mockEM, WRONG_ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.getBalance(WRONG_ACCOUNT_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(EXPECTED_ERROR_MESSAGE);

            verify(mockEM).close();
        }
    }

    @Nested
    class GetBalanceAuditTests {

        @Test
        void getBalanceAudit_shouldReturnPageResponse_whenAuditsExist() {
            final Long USER_ID = 1L;
            final Long ACCOUNT_ID = 1L;
            final List<String> USER_ROLES = List.of("USER");
            final String USER_EMAIL = "mock@gmail.com";
            final PageRequest PAGE_REQUEST = PageRequest.of(0, 10);

           

            Account mockAccount = mock(Account.class);
            User mockUser = mock(User.class);

            when(mockUser.getId()).thenReturn(USER_ID);
            when(mockAccount.getUser()).thenReturn(mockUser);

            AccountBalanceAudit audit1 = mock(AccountBalanceAudit.class);
            AccountBalanceAudit audit2 = mock(AccountBalanceAudit.class);
            List<AccountBalanceAudit> audits = List.of(audit1, audit2);

            when(audit1.getBalanceBefore()).thenReturn(BigDecimal.ZERO);
            when(audit1.getBalanceAfter()).thenReturn(BigDecimal.valueOf(100));
            when(audit1.getChangeAmount()).thenReturn(BigDecimal.valueOf(100));
            when(audit1.getChangedAt()).thenReturn(LocalDateTime.now());
            when(audit1.getChangedByThread()).thenReturn("thread-1");

            when(audit2.getBalanceBefore()).thenReturn(BigDecimal.valueOf(100));
            when(audit2.getBalanceAfter()).thenReturn(BigDecimal.valueOf(250));
            when(audit2.getChangeAmount()).thenReturn(BigDecimal.valueOf(150));
            when(audit2.getChangedAt()).thenReturn(LocalDateTime.now());
            when(audit2.getChangedByThread()).thenReturn("thread-2");

            when(accountDao.findById(mockEM, ACCOUNT_ID)).thenReturn(Optional.of(mockAccount));
            when(accountDao.countAudits(mockEM, ACCOUNT_ID)).thenReturn(2L);
            when(accountDao.getAuditsPage(mockEM, ACCOUNT_ID, PAGE_REQUEST.getPageNumber(), PAGE_REQUEST.getPageSize()))
                    .thenReturn(audits);

            PageResponse<BalanceAuditReadDto> result = accountService.getBalanceAudit(ACCOUNT_ID, PAGE_REQUEST);

            assertNotNull(result);
            assertEquals(2, result.getContent().size());
            assertEquals(0, result.getPageNumber());
            assertEquals(10, result.getPageSize());
            assertEquals(2L, result.getTotalElements());

            verify(mockEM).close();
        }

        @Test
        void getBalanceAudit_shouldReturnEmptyPage_whenNoAuditsExist() {
            final Long USER_ID = 1L;
            final Long ACCOUNT_ID = 1L;
            final List<String> USER_ROLES = List.of("USER");
            final String USER_EMAIL = "mock@gmail.com";
            final PageRequest PAGE_REQUEST = PageRequest.of(0, 10);

           

            Account mockAccount = mock(Account.class);
            User mockUser = mock(User.class);

            when(mockUser.getId()).thenReturn(USER_ID);
            when(mockAccount.getUser()).thenReturn(mockUser);

            when(accountDao.findById(mockEM, ACCOUNT_ID)).thenReturn(Optional.of(mockAccount));
            when(accountDao.countAudits(mockEM, ACCOUNT_ID)).thenReturn(0L);
            when(accountDao.getAuditsPage(mockEM, ACCOUNT_ID, PAGE_REQUEST.getPageNumber(), PAGE_REQUEST.getPageSize()))
                    .thenReturn(List.of());

            PageResponse<BalanceAuditReadDto> result = accountService.getBalanceAudit(ACCOUNT_ID, PAGE_REQUEST);

            assertNotNull(result);
            assertTrue(result.getContent().isEmpty());
            assertEquals(0, result.getTotalElements());

            verify(mockEM).close();
        }

        @Test
        void getBalanceAudit_shouldThrowEntityNotFoundException_whenAccountNotFound() {
            final Long USER_ID = 1L;
            final Long WRONG_ACCOUNT_ID = 999L;
            final List<String> USER_ROLES = List.of("USER");
            final String USER_EMAIL = "mock@gmail.com";
            final PageRequest PAGE_REQUEST = PageRequest.of(0, 10);
            final String EXPECTED_ERROR_MESSAGE = "Счет не найден";

           

            when(accountDao.findById(mockEM, WRONG_ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.getBalanceAudit(WRONG_ACCOUNT_ID, PAGE_REQUEST))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(EXPECTED_ERROR_MESSAGE);

            verify(mockEM).close();
        }
    }

 */

}