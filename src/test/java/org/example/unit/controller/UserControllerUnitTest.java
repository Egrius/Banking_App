package org.example.unit.controller;

import org.example.dto.Request;
import org.example.dto.Response;
import org.example.dto.request.PageRequest;
import org.example.dto.user.*;
import org.example.exception.CustomValidationException;
import org.example.exception.security_exception.AccessDeniedException;
import org.example.security.AuthContext;
import org.example.server.controller.UserController;
import org.example.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerUnitTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController controller;

    private AuthContext adminAuth;
    private AuthContext userAuth;
    private final Long USER_ID = 1L;
    private final Long OTHER_USER_ID = 2L;

    @BeforeEach
    void setUp() {
        adminAuth = new AuthContext(999L, "admin@test.com", List.of("ADMIN"));
        userAuth = new AuthContext(USER_ID, "user@test.com", List.of("USER"));
    }

    @Nested
    class RegisterTests {

        private final String COMMAND = "user.register";

        @Test
        void registerShouldReturnStatusCode400_whenNullPayload() {
            Request request = new Request(COMMAND);
            request.setPayload(null);

            Response response = controller.handle(request);

            assertEquals(400, response.getStatusCode());
            assertTrue(response.getMessage().contains("Payload is null"));
            verifyNoInteractions(userService);
        }

        @Test
        void registerShouldCallServiceAndReturnSuccess_whenValidRequest() {
            UserCreateDto dto = new UserCreateDto("John", "Doe", "12345", "john@test.com");
            Request request = new Request(COMMAND);
            request.setPayload(dto);

            UserReadDto expected = new UserReadDto(1L, "John", "Doe", "john@test.com", LocalDateTime.now(), List.of());
            when(userService.register(any(UserCreateDto.class))).thenReturn(expected);

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            assertEquals(200, response.getStatusCode());
            verify(userService).register(dto);
        }
    }

    @Nested
    class LoginTests {

        private final String COMMAND = "user.login";

        @Test
        void loginShouldCallServiceAndReturnSuccess_whenValidRequest() {
            UserLoginDto dto = new UserLoginDto("user@test.com", "12345");
            Request request = new Request(COMMAND);
            String mockJwt = "mockJwt";
            request.setPayload(dto);

            UserLoginReadDto expected = new UserLoginReadDto(new UserReadDto(1L, "John", "Doe", "user@test.com", LocalDateTime.now(), List.of()),mockJwt);
            when(userService.login(any(UserLoginDto.class))).thenReturn(expected);

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            assertEquals(200, response.getStatusCode());
            verify(userService).login(dto);
        }
    }

    @Nested
    class FindByIdTests {

        private final String COMMAND = "user.findById.1";

        @Test
        void findByIdShouldReturnUser_whenAdminRequests() {
            Request request = new Request(COMMAND);
            request.setAuthContext(adminAuth);

            UserReadDto expected = new UserReadDto(USER_ID, "John", "Doe", "user@test.com", LocalDateTime.now(), List.of());
            when(userService.findById(USER_ID)).thenReturn(expected);

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            verify(userService).findById(USER_ID);
        }

        @Test
        void findByIdShouldReturnUser_whenOwnerRequests() {
            Request request = new Request(COMMAND);
            request.setAuthContext(userAuth);

            UserReadDto expected = new UserReadDto(USER_ID, "John", "Doe", "user@test.com", LocalDateTime.now(), List.of());
            when(userService.findById(USER_ID)).thenReturn(expected);

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            verify(userService).findById(USER_ID);
        }

        @Test
        void findByIdShouldThrowAccessDenied_whenNotAdminOrOwner() {
            AuthContext otherAuth = new AuthContext(OTHER_USER_ID, "other@test.com", List.of("USER"));
            Request request = new Request(COMMAND);
            request.setAuthContext(otherAuth);

            assertThatThrownBy(() -> controller.handle(request))
                    .isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(userService);
        }
    }

    @Nested
    class FindAllTests {

        private final String COMMAND = "user.findAll";

        @Test
        void findAllShouldReturnPageResponse_whenAdminRequests() {
            Request request = new Request(COMMAND);
            request.setAuthContext(adminAuth);

            when(userService.findAll(any(AuthContext.class), any(PageRequest.class)))
                    .thenReturn(mock(org.example.dto.response.PageResponse.class));

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            verify(userService).findAll(eq(adminAuth), any(PageRequest.class));
        }

        @Test
        void findAllShouldThrowAccessDenied_whenNotAdmin() {
            Request request = new Request(COMMAND);
            request.setAuthContext(userAuth);

            assertThatThrownBy(() -> controller.handle(request))
                    .isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(userService);
        }
    }

    @Nested
    class UpdateUserTests {

        private final String COMMAND = "user.updateUser.1";

        @Test
        void updateUserShouldCallService_whenOwnerRequests() {
            UserUpdateDto dto = new UserUpdateDto("UpdatedName", "UpdatedLast");
            Request request = new Request(COMMAND);
            request.setAuthContext(userAuth);
            request.setPayload(dto);

            UserReadDto expected = new UserReadDto(USER_ID, "UpdatedName", "UpdatedLast", "user@test.com", LocalDateTime.now(), List.of());
            when(userService.updateUser(eq(USER_ID), any(UserUpdateDto.class))).thenReturn(expected);

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            verify(userService).updateUser(USER_ID, dto);
        }

        @Test
        void updateUserShouldCallService_whenAdminRequests() {
            UserUpdateDto dto = new UserUpdateDto("UpdatedName", "UpdatedLast");
            Request request = new Request(COMMAND);
            request.setAuthContext(adminAuth);
            request.setPayload(dto);

            UserReadDto expected = new UserReadDto(USER_ID, "UpdatedName", "UpdatedLast", "user@test.com", LocalDateTime.now(), List.of());
            when(userService.updateUser(eq(USER_ID), any(UserUpdateDto.class))).thenReturn(expected);

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            verify(userService).updateUser(USER_ID, dto);
        }

        @Test
        void updateUserShouldThrowAccessDenied_whenNotAdminOrOwner() {
            AuthContext otherAuth = new AuthContext(OTHER_USER_ID, "other@test.com", List.of("USER"));
            UserUpdateDto dto = new UserUpdateDto("Name", "Last");
            Request request = new Request(COMMAND);
            request.setAuthContext(otherAuth);
            request.setPayload(dto);

            assertThatThrownBy(() -> controller.handle(request))
                    .isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(userService);
        }
    }

    @Nested
    class ChangePasswordTests {

        private final String COMMAND = "user.changePassword.1";

        @Test
        void changePasswordShouldCallService_whenValidRequest() {
            PasswordChangeDto dto = new PasswordChangeDto("oldPass", "newPass", "newPass");
            Request request = new Request(COMMAND);
            request.setAuthContext(userAuth);
            request.setPayload(dto);

            doNothing().when(userService).changePassword(eq(USER_ID), any(PasswordChangeDto.class));

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            assertEquals("Password changed successfully", response.getMessage());
            verify(userService).changePassword(USER_ID, dto);
        }

        @Test
        void changePasswordShouldThrowAccessDenied_whenNotAdminOrOwner() {
            AuthContext otherAuth = new AuthContext(OTHER_USER_ID, "other@test.com", List.of("USER"));
            PasswordChangeDto dto = new PasswordChangeDto("old_pwd", "new_pwd", "new_pwd");
            Request request = new Request(COMMAND);
            request.setAuthContext(otherAuth);
            request.setPayload(dto);

            assertThatThrownBy(() -> controller.handle(request))
                    .isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(userService);
        }

        @Test
        void changePassword_shouldThrowValidationException_whenNewPasswordMissMatch() {
            AuthContext otherAuth = new AuthContext(USER_ID, "other@test.com", List.of("USER"));
            PasswordChangeDto dto = new PasswordChangeDto("old_pwd", "new_pwd", "wrong_new_pwd");
            Request request = new Request(COMMAND);
            request.setAuthContext(otherAuth);
            request.setPayload(dto);

            CustomValidationException exception = assertThrows(CustomValidationException.class, () -> controller.handle(request));

            assertTrue(exception.getErrorResponse().getViolations().getFirst().message().contains("не совпадает"));
            verifyNoInteractions(userService);
        }
    }

    @Nested
    class DeleteUserTests {

        private final String COMMAND = "user.deleteUser.1";

        @Test
        void deleteUserShouldCallService_whenOwnerRequestsWithPassword() {
            Request request = new Request(COMMAND);
            request.setAuthContext(userAuth);
            request.setHeaders(Map.of("password", "secret123"));

            doNothing().when(userService).deleteUser(eq(USER_ID), eq("secret123"));

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            assertEquals("User deleted successfully", response.getMessage());
            verify(userService).deleteUser(USER_ID, "secret123");
        }

        @Test
        void deleteUserShouldThrowIllegalArgumentException_whenPasswordMissing() {
            Request request = new Request(COMMAND);
            request.setAuthContext(userAuth);
            request.setHeaders(Map.of()); // пустые заголовки

            assertThatThrownBy(() -> controller.handle(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Password is required");
            verifyNoInteractions(userService);
        }

        @Test
        void deleteUserShouldCallService_whenAdminRequests() {
            Request request = new Request(COMMAND);
            request.setAuthContext(adminAuth);
            request.setHeaders(Map.of("password", "anyPassword"));

            doNothing().when(userService).deleteUser(eq(USER_ID), eq("anyPassword"));

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            verify(userService).deleteUser(USER_ID, "anyPassword");
        }
    }
}