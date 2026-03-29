package org.example.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.EntityTransaction;
import org.example.dao.AccountDao;
import org.example.dao.UserDao;
import org.example.dto.account.AccountCreateDto;
import org.example.dto.account.AccountReadDto;
import org.example.dto.role.RoleReadDto;
import org.example.dto.user.UserReadDto;
import org.example.entity.Account;
import org.example.entity.User;
import org.example.entity.enums.AccountType;
import org.example.entity.enums.CurrencyCode;
import org.example.exception.account.AccountAlreadyExistsException;
import org.example.mapper.AccountReadMapper;
import org.example.security.AuthContext;
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
                    "Mock", "Mock", USER_EMAIL,
                    LocalDateTime.now(),
                    USER_ROLES.stream().map(RoleReadDto::new).toList());

            AccountReadDto accountReadDto =   new AccountReadDto(
                    expectedUserReadDto, ACCOUNT_NUMBER,
                    BigDecimal.ZERO, correctCreateDto.currencyCode(),
                    correctCreateDto.accountType(), LocalDateTime.now(), null);


            AuthContext correctAuthContext = new AuthContext(USER_ID, USER_EMAIL, USER_ROLES);


            User mockUser = mock(User.class);
            when(mockUser.getId()).thenReturn(USER_ID);

            when(userDao.findById(mockEM, USER_ID))
                    .thenReturn(Optional.of(mockUser));

            when(accountDao.existsByUserIdAndType(mockEM, USER_ID, correctCreateDto.accountType()))
                    .thenReturn(false);

            doNothing().when(accountDao).save(eq(mockEM), any(Account.class));

            when(accountReadMapper.map(any(Account.class))).thenReturn(accountReadDto);

            AccountReadDto actualResult = accountService.createAccount(correctCreateDto, correctAuthContext);

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
            AuthContext wrongAuthContext = new AuthContext(WRONG_USER_ID, USER_EMAIL, USER_ROLES);

            when(userDao.findById(mockEM, WRONG_USER_ID))
                    .thenReturn(Optional.empty());

            when(mockTx.isActive()).thenReturn(true);

            assertThatThrownBy(() -> accountService.createAccount(wrongCreateDto, wrongAuthContext))
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

            AuthContext authContext = new AuthContext(USER_ID, USER_EMAIL, USER_ROLES);

            AccountCreateDto accountCreateDto = new AccountCreateDto(USER_ID, CurrencyCode.RUB, AccountType.CURRENT);

            User mockUser = Mockito.mock(User.class);
            when(mockUser.getId()).thenReturn(USER_ID);

            try(MockedStatic<SecurityUtil> mockSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
                mockSecurityUtil.when(() -> SecurityUtil.checkAuthenticated(authContext)).thenAnswer((i) -> null);
                mockSecurityUtil.when(() -> SecurityUtil.checkAdminOrOwner(authContext, USER_ID)).thenAnswer((i) -> null);

                when(userDao.findById(mockEM, USER_ID))
                        .thenReturn(Optional.of(mockUser));


                when(accountDao.existsByUserIdAndType(mockEM, USER_ID, accountCreateDto.accountType()))
                        .thenReturn(true);

                when(mockTx.isActive()).thenReturn(true);

                assertThatThrownBy(() -> accountService.createAccount(accountCreateDto, authContext))
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
    }

    @Nested
    class GetAccountTests {

        @Test
        void getAccount_shouldReturnValidAccount_ifFoundAndUserHasRights() {
            final Long USER_ID = 1L;
            final String USER_FIRST_NAME = "Mock_Firstname";
            final String USER_LAST_NAME = "Mock_Lastname";
            final List<String> USER_ROLES = List.of("USER");
            final String USER_EMAIL = "mock@gmail.com";
            final Long EXISTING_ACCOUNT_ID = 1L;

            final String ACCOUNT_NUMBER = "ACNT-123";

            AuthContext authContext = new AuthContext(USER_ID, USER_EMAIL, USER_ROLES);

            Account mockAccount = Mockito.mock(Account.class);
            User mockUser = Mockito.mock(User.class);
            when(mockUser.getId()).thenReturn(USER_ID);

            AccountReadDto mockAccountReadDto = new AccountReadDto(
                    new UserReadDto(USER_FIRST_NAME, USER_LAST_NAME, USER_EMAIL, LocalDateTime.now(), USER_ROLES.stream().map(RoleReadDto::new).toList()),
                    ACCOUNT_NUMBER,
                    BigDecimal.ZERO,
                    CurrencyCode.RUB,
                    AccountType.CURRENT,
                    LocalDateTime.now(),
                    null
            );

            try(MockedStatic<SecurityUtil> mockSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
                mockSecurityUtil.when(() -> SecurityUtil.checkAuthenticated(authContext)).thenAnswer((i) -> null);
                mockSecurityUtil.when(() -> SecurityUtil.checkAdminOrOwner(authContext, USER_ID)).thenAnswer((i) -> null);

                when(accountDao.findById(mockEM, EXISTING_ACCOUNT_ID))
                        .thenReturn(Optional.of(mockAccount));

                when(mockAccount.getUser()).thenReturn(mockUser);

                when(accountReadMapper.map(mockAccount)).thenReturn(mockAccountReadDto);

                AccountReadDto actualResult = accountService.getAccount(EXISTING_ACCOUNT_ID, authContext);

                assertNotNull(actualResult);
                assertEquals(actualResult.accountNumber(), mockAccountReadDto.accountNumber());
                assertEquals(actualResult.accountType(), mockAccountReadDto.accountType());
                assertEquals(actualResult.openingDate(), mockAccountReadDto.openingDate());
                assertEquals(actualResult.user().email(), mockAccountReadDto.user().email());

                verify(mockEM).close();
            }
        }
    }

}