package org.example.unit.controller;

import org.example.dto.Request;
import org.example.dto.Response;
import org.example.dto.account.AccountCreateDto;
import org.example.dto.account.AccountReadDto;
import org.example.dto.account.AccountSummaryDto;
import org.example.dto.balance_audit.BalanceAuditReadDto;
import org.example.dto.request.PageRequest;
import org.example.dto.response.PageResponse;
import org.example.dto.user.UserReadDto;
import org.example.entity.enums.AccountType;
import org.example.entity.enums.CurrencyCode;
import org.example.entity.enums.Status;
import org.example.exception.security_exception.AccessDeniedException;
import org.example.security.AuthContext;
import org.example.server.controller.AccountController;
import org.example.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountControllerUnitTest {

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController controller;

    private AuthContext adminAuth;
    private AuthContext userAuth;
    private final Long USER_ID = 1L;
    private final Long OTHER_USER_ID = 2L;
    private final Long ACCOUNT_ID = 100L;

    @BeforeEach
    void setUp() {
        adminAuth = new AuthContext(999L, "admin@test.com", List.of("ADMIN"));
        userAuth = new AuthContext(USER_ID, "user@test.com", List.of("USER"));
    }

    @Nested
    class CreateAccountTests {

        private final String COMMAND = "account.createAccount";

        @Test
        void createAccountShouldCallServiceAndReturnSuccess_whenAdminRequests() {
            AccountCreateDto dto = new AccountCreateDto(USER_ID, CurrencyCode.US, AccountType.CURRENT);
            Request request = new Request(COMMAND);
            request.setAuthContext(adminAuth);
            request.setPayload(dto);

            AccountReadDto expected = new AccountReadDto(ACCOUNT_ID,
                    new UserReadDto(USER_ID, "John", "Doe", "john@test.com", null, List.of()),
                    "ACNT-001",
                    BigDecimal.ZERO,
                    CurrencyCode.US,
                    AccountType.CURRENT,
                    Status.ACTIVE,
                    LocalDateTime.now(),
                    null);
            when(accountService.createAccount(any(AccountCreateDto.class))).thenReturn(expected);

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            assertEquals(200, response.getStatusCode());
            verify(accountService).createAccount(dto);
        }

        @Test
        void createAccountShouldCallServiceAndReturnSuccess_whenOwnerRequests() {
            AccountCreateDto dto = new AccountCreateDto(USER_ID, CurrencyCode.US, AccountType.CURRENT);
            Request request = new Request(COMMAND);
            request.setAuthContext(userAuth);
            request.setPayload(dto);

            AccountReadDto expected = new AccountReadDto(ACCOUNT_ID, new UserReadDto(USER_ID, "John", "Doe", "john@test.com", null, null),"ACNT-001", BigDecimal.ZERO,
                    CurrencyCode.US, AccountType.CURRENT, Status.ACTIVE,
                    LocalDateTime.now(), null);
            when(accountService.createAccount(any(AccountCreateDto.class))).thenReturn(expected);

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            verify(accountService).createAccount(dto);
        }

        @Test
        void createAccountShouldThrowAccessDenied_whenNotAdminOrOwner() {
            AccountCreateDto dto = new AccountCreateDto(OTHER_USER_ID, CurrencyCode.US, AccountType.CURRENT);
            Request request = new Request(COMMAND);
            request.setAuthContext(userAuth);
            request.setPayload(dto);

            assertThatThrownBy(() -> controller.handle(request))
                    .isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(accountService);
        }
    }

    @Nested
    class GetAccountTests {

        private final String COMMAND = "account.getAccount." + ACCOUNT_ID;

        @Test
        void getAccountShouldReturnAccount_whenAdminRequests() {
            Request request = new Request(COMMAND);
            request.setAuthContext(adminAuth);

            AccountReadDto expected = mock(AccountReadDto.class);
            when(accountService.getAccount(eq(ACCOUNT_ID), eq(adminAuth))).thenReturn(expected);

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            verify(accountService).getAccount(ACCOUNT_ID, adminAuth);
        }

        @Test
        void getAccountShouldReturnAccount_whenOwnerRequests() {
            Request request = new Request(COMMAND);
            request.setAuthContext(userAuth);

            AccountReadDto expected = mock(AccountReadDto.class);
            when(accountService.getAccount(eq(ACCOUNT_ID), eq(userAuth))).thenReturn(expected);

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            verify(accountService).getAccount(ACCOUNT_ID, userAuth);
        }
    }

    @Nested
    class GetUserAccountsTests {

        private final String COMMAND = "account.getUserAccounts." + USER_ID;

        @Test
        void getUserAccountsShouldReturnList_whenAdminRequests() {
            Request request = new Request(COMMAND);
            request.setAuthContext(adminAuth);

            List<AccountSummaryDto> expected = List.of(
                    new AccountSummaryDto(1L, "ACNT-001", CurrencyCode.US, AccountType.CURRENT),
                    new AccountSummaryDto(2L, "ACNT-002", CurrencyCode.EUR, AccountType.SAVINGS)
            );
            when(accountService.getUserAccounts(USER_ID)).thenReturn(expected);

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            verify(accountService).getUserAccounts(USER_ID);
        }

        @Test
        void getUserAccountsShouldReturnList_whenOwnerRequests() {
            Request request = new Request(COMMAND);
            request.setAuthContext(userAuth);

            List<AccountSummaryDto> expected = List.of(
                    new AccountSummaryDto(1L, "ACNT-001", CurrencyCode.US, AccountType.CURRENT)
            );
            when(accountService.getUserAccounts(USER_ID)).thenReturn(expected);

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            verify(accountService).getUserAccounts(USER_ID);
        }

        @Test
        void getUserAccountsShouldThrowAccessDenied_whenNotAdminOrOwner() {
            String command = "account.getUserAccounts." + OTHER_USER_ID;
            Request request = new Request(command);
            request.setAuthContext(userAuth);

            assertThatThrownBy(() -> controller.handle(request))
                    .isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(accountService);
        }
    }

    @Nested
    class CloseAccountTests {

        private final String COMMAND = "account.closeAccount." + ACCOUNT_ID;

        @Test
        void closeAccountShouldCallService_whenAdminRequests() {
            Request request = new Request(COMMAND);
            request.setAuthContext(adminAuth);

            doNothing().when(accountService).closeAccount(eq(ACCOUNT_ID), eq(adminAuth));

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            verify(accountService).closeAccount(ACCOUNT_ID, adminAuth);
        }

        @Test
        void closeAccountShouldCallService_whenOwnerRequests() {
            // Создаём аккаунт, принадлежащий userAuth
            String command = "account.closeAccount." + ACCOUNT_ID;
            Request request = new Request(command);
            request.setAuthContext(userAuth);

            doNothing().when(accountService).closeAccount(eq(ACCOUNT_ID), eq(userAuth));

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            verify(accountService).closeAccount(ACCOUNT_ID, userAuth);
        }
    }

    @Nested
    class BlockAccountTests {

        private final String COMMAND = "account.blockAccount." + ACCOUNT_ID;

        @Test
        void blockAccountShouldCallService_whenAdminRequests() {
            Request request = new Request(COMMAND);
            request.setAuthContext(adminAuth);
            request.setHeaders(Map.of("reason", "Fraud suspicion"));

            doNothing().when(accountService).blockAccount(eq(ACCOUNT_ID), eq("Fraud suspicion"), eq(adminAuth));

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            verify(accountService).blockAccount(ACCOUNT_ID, "Fraud suspicion", adminAuth);
        }

        @Test
        void blockAccountShouldWorkWithEmptyReason_whenReasonHeaderMissing() {
            Request request = new Request(COMMAND);
            request.setAuthContext(adminAuth);
            request.setHeaders(Map.of());

            doNothing().when(accountService).blockAccount(eq(ACCOUNT_ID), eq(""), eq(adminAuth));

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            verify(accountService).blockAccount(ACCOUNT_ID, "", adminAuth);
        }
    }

    @Nested
    class GetBalanceAuditTests {

        private final String COMMAND = "account.getBalanceAudit." + ACCOUNT_ID;

        @Test
        void getBalanceAuditShouldReturnPageResponse_whenValidRequest() {
            Request request = new Request(COMMAND);
            request.setAuthContext(adminAuth);
            request.setPayload(PageRequest.of(0, 10));

            PageResponse<BalanceAuditReadDto> expected = new PageResponse<>(List.of(), 0, 10, 0);
            when(accountService.getBalanceAudit(eq(ACCOUNT_ID), any(PageRequest.class), eq(adminAuth)))
                    .thenReturn(expected);

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            verify(accountService).getBalanceAudit(eq(ACCOUNT_ID), any(PageRequest.class), eq(adminAuth));
        }
    }

    @Nested
    class GetBalanceTests {

        private final String COMMAND = "account.getBalance." + ACCOUNT_ID;

        @Test
        void getBalanceShouldReturnBalance_whenValidRequest() {
            Request request = new Request(COMMAND);
            request.setAuthContext(adminAuth);

            BigDecimal expectedBalance = new BigDecimal("1500.50");
            when(accountService.getBalance(eq(ACCOUNT_ID), eq(adminAuth))).thenReturn(expectedBalance);

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            assertEquals(expectedBalance, response.getData());
            verify(accountService).getBalance(ACCOUNT_ID, adminAuth);
        }
    }
}