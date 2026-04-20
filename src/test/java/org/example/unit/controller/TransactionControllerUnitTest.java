package org.example.unit.controller;

import org.example.dto.Request;
import org.example.dto.Response;
import org.example.dto.transation.TransactionCreateDto;
import org.example.dto.transation.TransactionReadDto;
import org.example.entity.enums.CurrencyCode;
import org.example.entity.enums.TransactionStatus;
import org.example.exception.security_exception.AccessDeniedException;
import org.example.security.AuthContext;
import org.example.server.controller.TransactionController;
import org.example.service.TransactionService;
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
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionControllerUnitTest {

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionController controller;

    private AuthContext ownerAuth;
    private AuthContext otherAuth;
    private final Long FROM_ACCOUNT_ID = 1L;
    private final Long OTHER_ACCOUNT_ID = 2L;
    private final Long TO_ACCOUNT_ID = 3L;

    @BeforeEach
    void setUp() {
        ownerAuth = new AuthContext(FROM_ACCOUNT_ID, "owner@test.com", List.of("USER"));
        otherAuth = new AuthContext(OTHER_ACCOUNT_ID, "other@test.com", List.of("USER"));
    }

    @Nested
    class TransferTests {

        private final String COMMAND = "transaction.transfer";

        @Test
        void transferShouldCallServiceAndReturnSuccess_whenOwnerTransfers() {
            UUID idempotencyKey = UUID.randomUUID();
            TransactionCreateDto dto = new TransactionCreateDto(FROM_ACCOUNT_ID, TO_ACCOUNT_ID,
                    CurrencyCode.US, BigDecimal.valueOf(100), "Test transfer", idempotencyKey);
            Request request = new Request(COMMAND);
            request.setAuthContext(ownerAuth);
            request.setPayload(dto);

            TransactionReadDto expected = new TransactionReadDto(1L, null, null, BigDecimal.valueOf(100), TransactionStatus.SUCCESS,
                    null, 0, null, null, null, null,null, null, null);
            when(transactionService.transfer(any(TransactionCreateDto.class))).thenReturn(expected);

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            verify(transactionService).transfer(dto);
        }

        @Test
        void transferShouldThrowAccessDenied_whenUserNotOwnerOfFromAccount() {
            UUID idempotencyKey = UUID.randomUUID();
            TransactionCreateDto dto = new TransactionCreateDto(FROM_ACCOUNT_ID, TO_ACCOUNT_ID,
                    CurrencyCode.US, BigDecimal.valueOf(100), "Test transfer", idempotencyKey);
            Request request = new Request(COMMAND);
            request.setAuthContext(otherAuth);
            request.setPayload(dto);

            assertThatThrownBy(() -> controller.handle(request))
                    .isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(transactionService);
        }
    }
}