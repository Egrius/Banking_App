package org.example.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.example.dao.AccountDao;
import org.example.dao.UserDao;
import org.example.dto.account.AccountCreateDto;
import org.example.dto.account.AccountReadDto;
import org.example.dto.user.UserReadDto;
import org.example.entity.Account;
import org.example.entity.User;
import org.example.entity.enums.AccountType;
import org.example.entity.enums.CurrencyCode;
import org.example.mapper.AccountReadMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
            AccountCreateDto correctCreateDto = new AccountCreateDto(1L, CurrencyCode.RUB, AccountType.CURRENT);

            UserReadDto expectedUserReadDto = new UserReadDto("Mock", "Mock", "mock@gmail.com", LocalDateTime.now(), Mockito.mock(List.class));

            AccountReadDto accountReadDto =   new AccountReadDto(
                    expectedUserReadDto, "ACNT-123",
                    BigDecimal.ZERO, correctCreateDto.currencyCode(),
                    correctCreateDto.accountType(), LocalDateTime.now(), null);

            User mockUser = mock(User.class);
            when(mockUser.getId()).thenReturn(1L);

            when(userDao.findById(mockEM, 1L))
                    .thenReturn(Optional.of(mockUser));

            when(accountDao.existsByUserIdAndType(mockEM, 1L, correctCreateDto.accountType()))
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

        }
    }
}