package org.example.unit.service;

import jakarta.persistence.*;
import org.example.dao.UserDao;
import org.example.dto.request.PageRequest;
import org.example.dto.response.PageResponse;
import org.example.dto.response.ValidationErrorResponse;
import org.example.dto.role.RoleReadDto;
import org.example.dto.user.*;
import org.example.entity.User;
import org.example.exception.CustomValidationException;
import org.example.exception.security_exception.AccessDeniedException;
import org.example.exception.user.UserAlreadyExistsException;
import org.example.mapper.UserReadMapper;
import org.example.mapper.UserUpdateMapper;
import org.example.security.AuthContext;
import org.example.service.UserService;
import org.example.util.PasswordUtil;
import org.example.util.ValidatorUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceUnitTest {

    @Mock
    private UserDao userDao;

    @Mock
    private UserReadMapper userReadMapper;

    @Mock
    private UserUpdateMapper userUpdateMapper;

    @Mock
    private EntityManagerFactory emf;

    @InjectMocks
    private UserService userService;

    private final String USER_ROLE_NAME = "USER";

    private EntityManager mockEm;
    private EntityTransaction mockTx;
    private AuthContext authContext;

    @BeforeEach
    void setUp() {
        mockEm = Mockito.mock(EntityManager.class);
        mockTx = Mockito.mock(EntityTransaction.class);
        authContext = Mockito.mock(AuthContext.class);

        when(emf.createEntityManager()).thenReturn(mockEm);
    }

    @Nested
    class RegisterTests {

        @Test
        void registerUser_shouldRegisterIfDtoCorrect() {
            UserCreateDto correctDto = new UserCreateDto("Test", "Test", "12345", "test@gmail.com");
            UserReadDto dtoToReturn = new UserReadDto(1L, "Test", "Test", "test@gmail.com", LocalDateTime.now(),
                    List.of(new RoleReadDto(USER_ROLE_NAME)));

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(mockEm.getTransaction()).thenReturn(mockTx);

            when(userDao.findByEmail(mockEm, correctDto.email())).thenReturn(Optional.empty());
            when(userReadMapper.map(any(User.class))).thenReturn(dtoToReturn);

            UserReadDto actualResult = userService.register(correctDto);

            assertNotNull(actualResult);
            assertEquals(dtoToReturn.firstName(), actualResult.firstName());
            assertEquals(dtoToReturn.lastName(), actualResult.lastName());
            assertEquals(dtoToReturn.email(), actualResult.email());

            verify(mockTx).begin();
            verify(mockTx).commit();
            verify(userDao).save(eq(mockEm), any(User.class));
            verify(mockEm).close();
        }

        @Test
        void registerUser_shouldThrowValidationExceptionWhenDtoInvalid() {
            UserCreateDto invalidDto = new UserCreateDto("", "Test", "12345", "test@gmail.com");

            try (MockedStatic<ValidatorUtil> validatorMock = Mockito.mockStatic(ValidatorUtil.class)) {
                validatorMock.when(() -> ValidatorUtil.validate(invalidDto))
                        .thenThrow(new CustomValidationException(new ValidationErrorResponse(List.of())));

                assertThatThrownBy(() -> userService.register(invalidDto))
                        .isInstanceOf(CustomValidationException.class);
            }

            verify(mockTx, never()).begin();
            verify(userDao, never()).save(any(), any());
            verify(emf, never()).createEntityManager();
        }

        @Test
        void registerUser_shouldThrowExceptionWhenEmailAlreadyExists() {
            UserCreateDto dto = new UserCreateDto("Test", "Test", "12345", "existing@gmail.com");
            User existingUser = User.builder().build();

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(mockEm.getTransaction()).thenReturn(mockTx);

            when(userDao.findByEmail(mockEm, dto.email())).thenReturn(Optional.of(existingUser));

            assertThatThrownBy(() -> userService.register(dto))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("уже существует");

            verify(mockTx).begin();
            verify(mockTx).rollback();
            verify(userDao, never()).save(any(), any());
            verify(mockEm).close();
        }

        @Test
        void registerUser_shouldRollbackTransactionOnError() {
            UserCreateDto dto = new UserCreateDto("Test", "Test", "12345", "test@gmail.com");

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(mockEm.getTransaction()).thenReturn(mockTx);

            when(userDao.findByEmail(mockEm, dto.email())).thenReturn(Optional.empty());
            doThrow(new RuntimeException("DB Error")).when(userDao).save(eq(mockEm), any(User.class));

            when(mockTx.isActive()).thenReturn(true);

            assertThatThrownBy(() -> userService.register(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("DB Error");

            verify(mockTx).begin();
            verify(mockTx).rollback();
            verify(mockEm).close();
        }
    }

    @Nested
    class LoginTests {

        private final UserLoginDto correctLoginDto = new UserLoginDto("test@gmail.com", "12345");

        @BeforeEach
        void setUp() {

            mockEm = Mockito.mock(EntityManager.class);
            when(emf.createEntityManager()).thenReturn(mockEm);
        }

        @Test
        void login_shouldReturnUser_whenDataCorrect() {
            User mockUser = User.builder()
                    .firstName("TestFirstName")
                    .lastName("TestLastName")
                    .email(correctLoginDto.email())
                    .passwordHash("hashed")
                    .build();

            UserReadDto mockUserReadDto = new UserReadDto(
                    1L,
                    mockUser.getFirstName(),
                    mockUser.getLastName(),
                    mockUser.getEmail(),
                    mockUser.getCreatedAt(),
                    List.of(new RoleReadDto(USER_ROLE_NAME))
            );

            when(userDao.findByEmail(mockEm, correctLoginDto.email()))
                    .thenReturn(Optional.of(mockUser));
            when(userReadMapper.map(mockUser)).thenReturn(mockUserReadDto);

            try (MockedStatic<PasswordUtil> passwordUtilMock = Mockito.mockStatic(PasswordUtil.class)) {
                passwordUtilMock.when(() -> PasswordUtil.verify(correctLoginDto.rawPassword(), mockUser.getPasswordHash()))
                        .thenReturn(true);

                UserLoginReadDto actualResult = userService.login(correctLoginDto);

                assertNotNull(actualResult);
                assertEquals(mockUserReadDto.firstName(), actualResult.userReadDto().firstName());
                assertEquals(mockUserReadDto.lastName(), actualResult.userReadDto().lastName());
                assertEquals(mockUserReadDto.email(), actualResult.userReadDto().email());
            }
            verify(mockEm).close();
        }

        @Test
        void login_shouldThrow_whenUserNotFound() {
            when(userDao.findByEmail(mockEm, correctLoginDto.email()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.login(correctLoginDto))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Пользователь не найден");

            verify(mockEm).close();
        }

        @Test
        void login_shouldThrow_whenPasswordIncorrect() {
            User mockUser = User.builder()
                    .email(correctLoginDto.email())
                    .passwordHash("hashed")
                    .build();

            when(userDao.findByEmail(mockEm, correctLoginDto.email()))
                    .thenReturn(Optional.of(mockUser));

            try (MockedStatic<PasswordUtil> passwordUtilMock = Mockito.mockStatic(PasswordUtil.class)) {
                passwordUtilMock.when(() -> PasswordUtil.verify(correctLoginDto.rawPassword(), mockUser.getPasswordHash()))
                        .thenReturn(false);

                assertThatThrownBy(() -> userService.login(correctLoginDto))
                        .isInstanceOf(AccessDeniedException.class)
                        .hasMessage("Пароль неверен!");
            }
            verify(mockEm).close();
        }
    }

    @Nested
    class FindUserTests {

        @BeforeEach
        void setUp() {
            mockEm = Mockito.mock(EntityManager.class);

            when(emf.createEntityManager()).thenReturn(mockEm);
        }

        @Test
        void findById_shouldReturnUser_whenIdFound() {
            final Long ID_TO_FIND = 5L;
            User mockUser = Mockito.mock(User.class);
            UserReadDto dtoToReturn = new UserReadDto(1L,"Test", "Test", "test@gmail.com", LocalDateTime.now(), List.of());

            when(userDao.findById(mockEm, ID_TO_FIND)).thenReturn(Optional.of(mockUser));
            when(userReadMapper.map(mockUser)).thenReturn(dtoToReturn);

            UserReadDto actualResult = userService.findById(ID_TO_FIND);

            assertEquals(dtoToReturn.firstName(), actualResult.firstName());
            assertEquals(dtoToReturn.lastName(), actualResult.lastName());
            assertEquals(dtoToReturn.email(), actualResult.email());

            verify(mockEm).close();
        }

        @Test
        void findById_shouldThrow_whenIdNotFound() {
            final Long NON_EXISTENT_ID = 999L;

            when(userDao.findById(mockEm, NON_EXISTENT_ID)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> userService.findById(NON_EXISTENT_ID));
            verify(mockEm).close();
        }

        @Test
        void findById_shouldCloseEntityManager_evenIfExceptionOccurs() {
            final Long ID_TO_FIND = 5L;

            when(userDao.findById(mockEm, ID_TO_FIND)).thenThrow(new RuntimeException("Database error"));

            assertThrows(RuntimeException.class, () -> userService.findById(ID_TO_FIND));
            verify(mockEm).close();
        }
    }

    @Nested
    class FindAllUsersTests {

        private final PageRequest pageRequest = PageRequest.of(0, 10);
        private final AuthContext adminContext = mock(AuthContext.class);

        @BeforeEach
        void setUp() {
            when(adminContext.isAdmin()).thenReturn(true);
        }

        @Test
        void findAll_shouldReturnPageResponse_whenUsersExist() {
            List<User> mockUsers = List.of(Mockito.mock(User.class), Mockito.mock(User.class));
            List<UserReadDto> expectedDtos = List.of(
                    new UserReadDto(1L,"John", "Doe", "john@example.com", LocalDateTime.now(), List.of()),
                    new UserReadDto(2L, "Jane", "Smith", "jane@example.com", LocalDateTime.now(), List.of())
            );

            PageResponse<User> pageResponse = new PageResponse<>(mockUsers, 0, 10, 2L);

            mockEm = Mockito.mock(EntityManager.class);

            when(emf.createEntityManager()).thenReturn(mockEm);

            when(userDao.findAllPageable(mockEm, pageRequest)).thenReturn(pageResponse);
            when(userReadMapper.map(mockUsers.get(0))).thenReturn(expectedDtos.get(0));
            when(userReadMapper.map(mockUsers.get(1))).thenReturn(expectedDtos.get(1));

            PageResponse<UserReadDto> result = userService.findAll(adminContext, pageRequest);

            assertNotNull(result);
            assertEquals(2, result.getContent().size());
            assertEquals("John", result.getContent().get(0).firstName());
            assertEquals("Jane", result.getContent().get(1).firstName());
            assertEquals(2L, result.getTotalElements());

            verify(mockEm).close();
        }

        @Test
        void findAll_shouldThrow_whenNotAdmin() {
            AuthContext nonAdminContext = mock(AuthContext.class);
            when(nonAdminContext.isAdmin()).thenReturn(false);

            assertThatThrownBy(() -> userService.findAll(nonAdminContext, pageRequest))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void findAll_shouldThrow_whenPageRequestIsNull() {
            assertThatThrownBy(() -> userService.findAll(adminContext, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Нельзя выполнить пустой запрос!");
        }

        @Test
        void findAll_shouldReturnEmptyPage_whenNoUsers() {
            PageResponse<User> emptyPage = new PageResponse<>(List.of(), 0, 10, 0L);

            mockEm = Mockito.mock(EntityManager.class);

            when(emf.createEntityManager()).thenReturn(mockEm);

            when(userDao.findAllPageable(mockEm, pageRequest)).thenReturn(emptyPage);

            PageResponse<UserReadDto> result = userService.findAll(adminContext, pageRequest);

            assertNotNull(result);
            assertTrue(result.getContent().isEmpty());
            assertEquals(0L, result.getTotalElements());

            verify(mockEm).close();
        }
    }

    @Nested
    class UpdateUserTests {

        private final Long USER_ID = 1L;
        private final AuthContext authContext = mock(AuthContext.class);
        private final UserUpdateDto updateDto = new UserUpdateDto("new_First", "new_Last");
        private final User mockUser = Mockito.mock(User.class);
        private final User updatedUserMock = Mockito.mock(User.class);
        private final UserReadDto expectedDto = new UserReadDto(1L,"new_First", "new_Last", "oldEmail", LocalDateTime.now(), List.of());

        @BeforeEach
        void setUp() {
            when(authContext.isAdmin()).thenReturn(false);
            when(authContext.getUserId()).thenReturn(USER_ID);

            mockEm = Mockito.mock(EntityManager.class);
            when(emf.createEntityManager()).thenReturn(mockEm);

            mockTx = Mockito.mock(EntityTransaction.class);
            when(mockEm.getTransaction()).thenReturn(mockTx);
        }

        @Test
        void updateUser_shouldUpdate_whenDataIsValid() {
            when(userDao.findById(mockEm, USER_ID)).thenReturn(Optional.of(mockUser));
            when(userUpdateMapper.map(updateDto, mockUser)).thenReturn(updatedUserMock);
            when(userReadMapper.map(updatedUserMock)).thenReturn(expectedDto);

            try (MockedStatic<ValidatorUtil> validatorMock = Mockito.mockStatic(ValidatorUtil.class)) {
                validatorMock.when(() -> ValidatorUtil.validate(updateDto))
                        .thenAnswer(invocation -> null);

                UserReadDto actualResult = userService.updateUser(USER_ID, updateDto);

                assertNotNull(actualResult);
                assertEquals(expectedDto.firstName(), actualResult.firstName());
                assertEquals(expectedDto.lastName(), actualResult.lastName());

                verify(mockTx).begin();
                verify(mockTx).commit();
                verify(userDao).update(mockEm, updatedUserMock);
                verify(mockEm).close();
            }
        }

        @Test
        void updateUser_shouldThrow_whenUserNotFound() {
            when(userDao.findById(mockEm, USER_ID)).thenReturn(Optional.empty());

            try (MockedStatic<ValidatorUtil> validatorMock = Mockito.mockStatic(ValidatorUtil.class)) {
                validatorMock.when(() -> ValidatorUtil.validate(updateDto))
                        .thenAnswer(invocation -> null);

                when(mockTx.isActive()).thenReturn(true);

                assertThatThrownBy(() -> userService.updateUser(USER_ID, updateDto))
                        .isInstanceOf(EntityNotFoundException.class)
                        .hasMessageContaining("Не найден пользователь");

                verify(mockTx, never()).commit();
                verify(mockTx).rollback();
                verify(mockEm).close();
            }
        }

        @Test
        void updateUser_shouldThrow_whenValidationFails() {
            try (MockedStatic<ValidatorUtil> validatorMock = Mockito.mockStatic(ValidatorUtil.class)) {
                validatorMock.when(() -> ValidatorUtil.validate(updateDto))
                        .thenThrow(new CustomValidationException(new ValidationErrorResponse(List.of())));

                assertThatThrownBy(() -> userService.updateUser(USER_ID, updateDto))
                        .isInstanceOf(CustomValidationException.class);
            }

            verify(mockTx, never()).begin();
            verify(userDao, never()).findById(any(), any());
        }
    }

    @Nested
    class ChangePasswordTests {

        private final Long USER_ID = 1L;
        private final AuthContext authContext = mock(AuthContext.class);
        private final PasswordChangeDto passwordChangeDto = new PasswordChangeDto("oldPass", "newPass", "newPass");
        private final User user = User.builder().build();

        @BeforeEach
        void setUp() {
            user.setId(USER_ID);
            user.setPasswordHash("hashedOld");
            when(authContext.getUserId()).thenReturn(USER_ID);

            mockEm = Mockito.mock(EntityManager.class);
            when(emf.createEntityManager()).thenReturn(mockEm);

            mockTx = Mockito.mock(EntityTransaction.class);
            when(mockEm.getTransaction()).thenReturn(mockTx);
        }

        @Test
        void changePassword_shouldChange_whenDataCorrect() {
            when(userDao.findById(mockEm, USER_ID)).thenReturn(Optional.of(user));

            try (MockedStatic<ValidatorUtil> validatorMock = Mockito.mockStatic(ValidatorUtil.class);
                 MockedStatic<PasswordUtil> passwordUtilMock = Mockito.mockStatic(PasswordUtil.class)) {

                validatorMock.when(() -> ValidatorUtil.validate(passwordChangeDto))
                        .thenAnswer(invocation -> null);

                passwordUtilMock.when(() -> PasswordUtil.verify("oldPass", "hashedOld"))
                        .thenReturn(true);
                passwordUtilMock.when(() -> PasswordUtil.hash("newPass"))
                        .thenReturn("hashedNew");

                userService.changePassword(USER_ID, passwordChangeDto);

                verify(mockTx).begin();
                verify(mockTx).commit();
                verify(userDao).update(mockEm, user);
                assertEquals("hashedNew", user.getPasswordHash());
                verify(mockEm).close();
            }
        }

        @Test
        void changePassword_shouldThrow_whenOldPasswordIncorrect() {
            when(userDao.findById(mockEm, USER_ID)).thenReturn(Optional.of(user));

            try (MockedStatic<ValidatorUtil> validatorMock = Mockito.mockStatic(ValidatorUtil.class);
                 MockedStatic<PasswordUtil> passwordUtilMock = Mockito.mockStatic(PasswordUtil.class)) {

                validatorMock.when(() -> ValidatorUtil.validate(passwordChangeDto))
                        .thenAnswer(invocation -> null);

                passwordUtilMock.when(() -> PasswordUtil.verify("oldPass", "hashedOld"))
                        .thenReturn(false);

                when(mockTx.isActive()).thenReturn(true);

                assertThatThrownBy(() -> userService.changePassword(USER_ID, passwordChangeDto))
                        .isInstanceOf(AccessDeniedException.class)
                        .hasMessage("Передан неправильный пароль");

                verify(mockTx).begin();
                verify(mockTx).rollback();
                verify(userDao, never()).update(any(), any());
                verify(mockEm).close();
            }
        }

        @Test
        void changePassword_shouldThrow_whenUserNotFound() {
            when(userDao.findById(mockEm, USER_ID)).thenReturn(Optional.empty());

            try (MockedStatic<ValidatorUtil> validatorMock = Mockito.mockStatic(ValidatorUtil.class)) {
                validatorMock.when(() -> ValidatorUtil.validate(passwordChangeDto))
                        .thenAnswer(invocation -> null);


                when(mockTx.isActive()).thenReturn(true);

                assertThatThrownBy(() -> userService.changePassword(USER_ID, passwordChangeDto))
                        .isInstanceOf(EntityNotFoundException.class)
                        .hasMessage("Пользователь не найден");

                verify(mockTx).begin();
                verify(mockTx).rollback();
                verify(userDao, never()).update(any(), any());
                verify(mockEm).close();
            }
        }

        @Test
        void changePassword_shouldHandleOptimisticLockException() {
            when(userDao.findById(mockEm, USER_ID)).thenReturn(Optional.of(user));

            try (MockedStatic<ValidatorUtil> validatorMock = Mockito.mockStatic(ValidatorUtil.class);
                 MockedStatic<PasswordUtil> passwordUtilMock = Mockito.mockStatic(PasswordUtil.class)) {

                validatorMock.when(() -> ValidatorUtil.validate(passwordChangeDto))
                        .thenAnswer(invocation -> null);

                passwordUtilMock.when(() -> PasswordUtil.verify("oldPass", "hashedOld"))
                        .thenReturn(true);
                passwordUtilMock.when(() -> PasswordUtil.hash("newPass"))
                        .thenReturn("hashedNew");

                when(mockTx.isActive()).thenReturn(true);

                doThrow(new OptimisticLockException()).when(userDao).update(mockEm, user);

                assertThatThrownBy(() -> userService.changePassword(USER_ID, passwordChangeDto))
                        .isInstanceOf(ConcurrentModificationException.class)
                        .hasMessage("Пользователь был изменен. Обновите данные и повторите попытку.");

                verify(mockTx).begin();
                verify(mockTx).rollback();
                verify(mockEm).close();
            }
        }
    }

    @Nested
    class DeleteUserTests {

        private final Long USER_ID = 1L;
        private final AuthContext authContext = mock(AuthContext.class);
        private final User user = User.builder().build();
        private final String CORRECT_PASSWORD = "correctPass";

        @BeforeEach
        void setUp() {
            user.setId(USER_ID);
            user.setPasswordHash("hashedPass");
            when(authContext.getUserId()).thenReturn(USER_ID);
            when(authContext.isAdmin()).thenReturn(false);

            mockEm = Mockito.mock(EntityManager.class);
            when(emf.createEntityManager()).thenReturn(mockEm);

            mockTx = Mockito.mock(EntityTransaction.class);
            when(mockEm.getTransaction()).thenReturn(mockTx);
        }

        @Test
        void deleteUser_shouldDelete_whenPasswordCorrect() {
            when(userDao.findById(mockEm, USER_ID)).thenReturn(Optional.of(user));

            try (MockedStatic<PasswordUtil> passwordUtilMock = Mockito.mockStatic(PasswordUtil.class)) {
                passwordUtilMock.when(() -> PasswordUtil.verify(CORRECT_PASSWORD, "hashedPass"))
                        .thenReturn(true);

                userService.deleteUser(USER_ID, CORRECT_PASSWORD);

                verify(mockTx).begin();
                verify(mockTx).commit();
                verify(userDao).delete(mockEm, user);
                verify(mockEm).close();
            }
        }

        @Test
        void deleteUser_shouldDelete_whenAdmin_noPasswordCheck() {
            when(authContext.isAdmin()).thenReturn(true);
            when(userDao.findById(mockEm, USER_ID)).thenReturn(Optional.of(user));

            userService.deleteUser(USER_ID, "");

            verify(mockTx).begin();
            verify(mockTx).commit();
            verify(userDao).delete(mockEm, user);
            verify(mockEm).close();
        }

        @Test
        void deleteUser_shouldThrow_whenPasswordIncorrect() {
            when(userDao.findById(mockEm, USER_ID)).thenReturn(Optional.of(user));

            try (MockedStatic<PasswordUtil> passwordUtilMock = Mockito.mockStatic(PasswordUtil.class)) {
                passwordUtilMock.when(() -> PasswordUtil.verify(CORRECT_PASSWORD, "hashedPass"))
                        .thenReturn(false);

                when(mockTx.isActive()).thenReturn(true);

                assertThatThrownBy(() -> userService.deleteUser(USER_ID, CORRECT_PASSWORD))
                        .isInstanceOf(AccessDeniedException.class)
                        .hasMessage("Неверный пароль");

                verify(mockTx).begin();
                verify(mockTx).rollback();
                verify(userDao, never()).delete(any(), any());
                verify(mockEm).close();
            }
        }

        @Test
        void deleteUser_shouldThrow_whenUserNotFound() {
            when(userDao.findById(mockEm, USER_ID)).thenReturn(Optional.empty());


            when(mockTx.isActive()).thenReturn(true);

            assertThatThrownBy(() -> userService.deleteUser(USER_ID, CORRECT_PASSWORD))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Не найден пользователь");

            verify(mockTx).begin();
            verify(mockTx).rollback();
            verify(userDao, never()).delete(any(), any());
            verify(mockEm).close();
        }
    }
}