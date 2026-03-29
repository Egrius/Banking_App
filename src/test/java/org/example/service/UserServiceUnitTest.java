package org.example.service;

import jakarta.persistence.*;
import org.example.dao.UserDao;
import org.example.dto.response.ValidationErrorResponse;
import org.example.dto.role.RoleReadDto;
import org.example.dto.user.*;
import org.example.entity.User;
import org.example.exception.CustomValidationException;
import org.example.mapper.UserReadMapper;
import org.example.mapper.UserUpdateMapper;
import org.example.util.PasswordUtil;
import org.example.util.ValidatorUtil;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

    private final EntityManager mockEm = Mockito.mock(EntityManager.class);
    private final EntityTransaction mockTx = Mockito.mock(EntityTransaction.class);

    @Nested
    class RegisterTests {

        @Test
        void registerUser_shouldRegisterIfDtoCorrect() {
            UserCreateDto correctDto = new UserCreateDto("Test", "Test", "12345", "test@gmail.com");
            UserReadDto dtoToReturn = new UserReadDto("Test", "Test","test@gmail.com", LocalDateTime.now(),
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
            assertEquals(dtoToReturn.createdAt(), actualResult.createdAt());
            assertFalse(actualResult.roles().isEmpty());

            verify(mockTx).begin();
            verify(mockTx).commit();
            verify(userDao).save(eq(mockEm), any(User.class));
            verify(mockEm).close();
        }

        @Test
        void registerUser_shouldThrowValidationExceptionWhenDtoInvalid() {
            UserCreateDto invalidDto = new UserCreateDto("", "Test", "12345", "test@gmail.com");

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(mockEm.getTransaction()).thenReturn(mockTx);

            assertThatThrownBy(() -> userService.register(invalidDto))
                    .isInstanceOf(CustomValidationException.class);

            verify(mockTx, never()).begin();
            verify(userDao, never()).save(any(), any());
            verify(mockEm).close();
        }

        @Test
        void registerUser_shouldThrowExceptionWhenEmailAlreadyExists() {
            UserCreateDto dto = new UserCreateDto("Test", "Test", "12345", "existing@gmail.com");
            User existingUser = User.builder().build();

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(mockEm.getTransaction()).thenReturn(mockTx);
            when(userDao.findByEmail(mockEm, dto.email())).thenReturn(Optional.of(existingUser));

            assertThatThrownBy(() -> userService.register(dto))
                    .isInstanceOf(IllegalArgumentException.class)
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
            when(mockTx.isActive()).thenReturn(true);

            doThrow(new RuntimeException("DB Error")).when(userDao).save(eq(mockEm), any(User.class));

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
        private final UserLoginDto correctLoginDto = new UserLoginDto(
                "test@gmail.com", "12345"
        );

        private final UserLoginDto incorrectLoginDto = new UserLoginDto(
                "incorrect_email", ""
        );

        @Test
        public void login_shouldReturnExistingUser_whenDataCorrect_andUserFound() {
            User mockUser = User.builder()
                            .firstName("TestFirstName")
                            .lastName("TestLastName")
                            .email(correctLoginDto.email())
                            .passwordHash("hashed")
                            .build();

            UserReadDto mockUserReadDto = new UserReadDto(
                    mockUser.getFirstName(),
                    mockUser.getLastName(),
                    mockUser.getEmail(),
                    mockUser.getCreatedAt(),
                    List.of(new RoleReadDto(USER_ROLE_NAME))
            );

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(userDao.findByEmail(mockEm, correctLoginDto.email()))
                    .thenReturn(Optional.of(mockUser));

           try(MockedStatic<PasswordUtil> passwordUtilMock = Mockito.mockStatic(PasswordUtil.class)) {
                passwordUtilMock.when(() -> PasswordUtil.verify(correctLoginDto.rawPassword(), mockUser.getPasswordHash()))
                        .thenReturn(true);

               when(userReadMapper.map(mockUser)).thenReturn(mockUserReadDto);

               UserReadDto actualResult = userService.login(correctLoginDto);

               assertNotNull(actualResult);
               assertEquals(actualResult.firstName(), mockUserReadDto.firstName());
               assertEquals(actualResult.lastName(), mockUserReadDto.lastName());
               assertEquals(actualResult.email(), mockUserReadDto.email());
               assertEquals(actualResult.createdAt(), mockUserReadDto.createdAt());
               assertFalse(actualResult.roles().isEmpty());

               passwordUtilMock.verify(() -> PasswordUtil.verify(correctLoginDto.rawPassword(), mockUser.getPasswordHash()));
           }
           verify(mockEm).close();
        }
        @Test
        public void login_shouldThrow_whenDataCorrect_andUserNotFound() {

            when(emf.createEntityManager()).thenReturn(mockEm);

            try(MockedStatic<PasswordUtil> passwordUtilMock = Mockito.mockStatic(PasswordUtil.class)) {

                when(userDao.findByEmail(mockEm, correctLoginDto.email()))
                        .thenReturn(Optional.empty());

                assertThatThrownBy(() -> userService.login(correctLoginDto))
                        .isInstanceOf(EntityNotFoundException.class);

                passwordUtilMock.verifyNoInteractions();
            }
            verifyNoInteractions(userReadMapper);
            verify(mockEm).close();
        }
    }

    @Nested
    class FindUserTests {
        @Test
        public void findById_shouldReturnUser_ifIdFound() {
            final Long ID_TO_FIND = 5L;

            User mockUser = Mockito.mock(User.class);
            UserReadDto dtoToReturn = new UserReadDto("Test_First_name",
                    "Test_Last_Name",
                    "testEmail@gmail.com",
                    LocalDateTime.now(),
                    List.of(Mockito.mock(RoleReadDto.class)));

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(userDao.findById(mockEm, ID_TO_FIND)).thenReturn(
                    Optional.of(mockUser)
            );


            when(userReadMapper.map(mockUser))
                    .thenReturn(dtoToReturn);

            UserReadDto actualResult = userService.findById(ID_TO_FIND);

            assertEquals(actualResult.firstName(), dtoToReturn.firstName());
            assertEquals(actualResult.lastName(), dtoToReturn.lastName());
            assertEquals(actualResult.email(), dtoToReturn.email());

            verify(mockEm).close();

        }

        @Test
        public void findById_shouldThrowEntityNotFoundException_ifIdNotFound() {
            final Long NON_EXISTENT_ID = 999L;

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(userDao.findById(mockEm, NON_EXISTENT_ID)).thenReturn(
                    Optional.empty()
            );

            assertThrows(EntityNotFoundException.class, () -> {
                userService.findById(NON_EXISTENT_ID);
            });

            verify(mockEm).close();
        }

        @Test
        public void findById_shouldCloseEntityManager_evenIfExceptionOccurs() {
            final Long ID_TO_FIND = 5L;

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(userDao.findById(mockEm, ID_TO_FIND)).thenThrow(
                    new RuntimeException("Database connection error")
            );

            assertThrows(RuntimeException.class, () -> {
                userService.findById(ID_TO_FIND);
            });

            verify(mockEm).close();
        }

        @Test
        public void findById_shouldVerifyMapperCalledWithCorrectUser() {
            final Long ID_TO_FIND = 5L;

            User mockUser = Mockito.mock(User.class);
            UserReadDto dtoToReturn = Mockito.mock(UserReadDto.class);

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(userDao.findById(mockEm, ID_TO_FIND)).thenReturn(
                    Optional.of(mockUser)
            );
            when(userReadMapper.map(mockUser)).thenReturn(dtoToReturn);

            userService.findById(ID_TO_FIND);

            verify(userReadMapper).map(mockUser);
            verify(mockEm).close();
        }

        @Test
        public void findById_shouldReturnCorrectUserDto() {
            final Long ID_TO_FIND = 5L;
            final String EXPECTED_FIRST_NAME = "John";
            final String EXPECTED_LAST_NAME = "Doe";
            final String EXPECTED_EMAIL = "john.doe@example.com";
            final LocalDateTime NOW = LocalDateTime.now();

            User mockUser = Mockito.mock(User.class);
            UserReadDto expectedDto = new UserReadDto(
                    EXPECTED_FIRST_NAME,
                    EXPECTED_LAST_NAME,
                    EXPECTED_EMAIL,
                    NOW,
                    List.of()
            );

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(userDao.findById(mockEm, ID_TO_FIND)).thenReturn(
                    Optional.of(mockUser)
            );
            when(userReadMapper.map(mockUser)).thenReturn(expectedDto);

            UserReadDto actualResult = userService.findById(ID_TO_FIND);

            assertAll(
                    () -> assertEquals(EXPECTED_FIRST_NAME, actualResult.firstName()),
                    () -> assertEquals(EXPECTED_LAST_NAME, actualResult.lastName()),
                    () -> assertEquals(EXPECTED_EMAIL, actualResult.email()),
                    () -> assertEquals(NOW, actualResult.createdAt()),
                    () -> assertNotNull(actualResult.roles())
            );
            verify(mockEm).close();
        }
    }

    @Nested
    class FindAllUsersTests {

        @Test
        public void findAll_shouldReturnListOfUserReadDto_whenUsersExist() {

            List<User> mockUsers = List.of(
                    Mockito.mock(User.class),
                    Mockito.mock(User.class),
                    Mockito.mock(User.class)
            );

            List<UserReadDto> expectedDtos = List.of(
                    new UserReadDto("John", "Doe", "john@example.com", LocalDateTime.now(), List.of()),
                    new UserReadDto("Jane", "Smith", "jane@example.com", LocalDateTime.now(), List.of()),
                    new UserReadDto("Bob", "Johnson", "bob@example.com", LocalDateTime.now(), List.of())
            );

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(userDao.findAll(mockEm)).thenReturn(mockUsers);

            when(userReadMapper.map(mockUsers.get(0))).thenReturn(expectedDtos.get(0));
            when(userReadMapper.map(mockUsers.get(1))).thenReturn(expectedDtos.get(1));
            when(userReadMapper.map(mockUsers.get(2))).thenReturn(expectedDtos.get(2));

            List<UserReadDto> actualResult = userService.findAll();

            assertAll(
                    () -> assertEquals(3, actualResult.size()),
                    () -> assertEquals(expectedDtos.get(0).firstName(), actualResult.get(0).firstName()),
                    () -> assertEquals(expectedDtos.get(1).firstName(), actualResult.get(1).firstName()),
                    () -> assertEquals(expectedDtos.get(2).firstName(), actualResult.get(2).firstName())
            );

            verify(mockEm).close();
            verify(userDao).findAll(mockEm);
            verify(userReadMapper, times(3)).map(any(User.class));
        }

        @Test
        public void findAll_shouldReturnEmptyList_whenNoUsersExist() {

            List<User> emptyUserList = List.of();

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(userDao.findAll(mockEm)).thenReturn(emptyUserList);

            List<UserReadDto> actualResult = userService.findAll();

            assertAll(
                    () -> assertNotNull(actualResult),
                    () -> assertTrue(actualResult.isEmpty()),
                    () -> assertEquals(0, actualResult.size())
            );

            verify(mockEm).close();
            verify(userDao).findAll(mockEm);
            verify(userReadMapper, never()).map(any(User.class));
        }

        @Test
        public void findAll_shouldCloseEntityManager_evenIfExceptionOccurs() {

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(userDao.findAll(mockEm)).thenThrow(new RuntimeException("Database error"));

            assertThrows(RuntimeException.class, () -> {
                userService.findAll();
            });

            verify(mockEm).close();
            verify(userDao).findAll(mockEm);
            verify(userReadMapper, never()).map(any(User.class));
        }

        @Test
        public void findAll_shouldVerifyMapperCalledForEachUser() {

            List<User> mockUsers = List.of(
                    Mockito.mock(User.class),
                    Mockito.mock(User.class)
            );

            UserReadDto mockDto1 = Mockito.mock(UserReadDto.class);
            UserReadDto mockDto2 = Mockito.mock(UserReadDto.class);

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(userDao.findAll(mockEm)).thenReturn(mockUsers);
            when(userReadMapper.map(mockUsers.get(0))).thenReturn(mockDto1);
            when(userReadMapper.map(mockUsers.get(1))).thenReturn(mockDto2);

            List<UserReadDto> result = userService.findAll();

            assertAll(
                    () -> assertEquals(2, result.size()),
                    () -> verify(userReadMapper, times(1)).map(mockUsers.get(0)),
                    () -> verify(userReadMapper, times(1)).map(mockUsers.get(1)),
                    () -> verify(userReadMapper, times(2)).map(any(User.class))
            );

            verify(mockEm).close();
        }

        @Test
        public void findAll_shouldPreserveOrderOfUsers() {

            User user1 = Mockito.mock(User.class);
            User user2 = Mockito.mock(User.class);
            User user3 = Mockito.mock(User.class);

            List<User> mockUsers = List.of(user1, user2, user3);

            UserReadDto dto1 = new UserReadDto("Alice", "Brown", "alice@example.com", LocalDateTime.now(), List.of());
            UserReadDto dto2 = new UserReadDto("Bob", "White", "bob@example.com", LocalDateTime.now(), List.of());
            UserReadDto dto3 = new UserReadDto("Charlie", "Black", "charlie@example.com", LocalDateTime.now(), List.of());

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(userDao.findAll(mockEm)).thenReturn(mockUsers);
            when(userReadMapper.map(user1)).thenReturn(dto1);
            when(userReadMapper.map(user2)).thenReturn(dto2);
            when(userReadMapper.map(user3)).thenReturn(dto3);

            List<UserReadDto> result = userService.findAll();

            assertAll(
                    () -> assertEquals(3, result.size()),
                    () -> assertEquals("Alice", result.get(0).firstName()),
                    () -> assertEquals("Bob", result.get(1).firstName()),
                    () -> assertEquals("Charlie", result.get(2).firstName())
            );

            verify(mockEm).close();
        }

        @Test
        public void findAll_shouldCreateEntityManagerExactlyOnce() {
            List<User> mockUsers = List.of(Mockito.mock(User.class));

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(userDao.findAll(mockEm)).thenReturn(mockUsers);
            when(userReadMapper.map(any(User.class))).thenReturn(Mockito.mock(UserReadDto.class));

            userService.findAll();

            verify(emf, times(1)).createEntityManager();
            verify(mockEm).close();
        }

        @Test
        public void findAll_shouldHandleLargeUserList() {

            int userCount = 1000;
            List<User> mockUsers = new ArrayList<>();

            for (int i = 0; i < userCount; i++) {
                User mockUser = Mockito.mock(User.class);
                mockUsers.add(mockUser);
                when(userReadMapper.map(mockUser)).thenReturn(Mockito.mock(UserReadDto.class));
            }

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(userDao.findAll(mockEm)).thenReturn(mockUsers);

            List<UserReadDto> result = userService.findAll();

            assertAll(
                    () -> assertEquals(userCount, result.size()),
                    () -> verify(userReadMapper, times(userCount)).map(any(User.class)),
                    () -> verify(mockEm).close()
            );
        }
    }

    @Nested
    class UpdateUserTests {

        private final User mockUserToUpdate = Mockito.mock(User.class);


        @Test
        void updateUser_shouldUpdateCorrect_ifDataIsValid() {
            UserUpdateDto updateDto = new UserUpdateDto("new_First", "new_Last");

            User updatedUserMock = Mockito.mock(User.class);

            UserReadDto expectedDto = new UserReadDto(
                    updateDto.firstNameUpdated(),
                    updateDto.lastNameUpdated(),
                    "oldEmail",
                    LocalDateTime.now(),
                    List.of(Mockito.mock(RoleReadDto.class))
            );

            final Long CORRECT_ID_TO_FIND = 1L;

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(mockEm.getTransaction()).thenReturn(mockTx);

            try(MockedStatic<ValidatorUtil> validatorMock = Mockito.mockStatic(ValidatorUtil.class)) {
                validatorMock.when(() -> ValidatorUtil.validate(updateDto))
                        .thenAnswer(invocation -> null);

                when(userDao.findById(mockEm, CORRECT_ID_TO_FIND)).thenReturn(Optional.of(mockUserToUpdate));
                when(userUpdateMapper.map(updateDto, mockUserToUpdate)).thenReturn(updatedUserMock);
                when(userReadMapper.map(updatedUserMock)).thenReturn(expectedDto);

                UserReadDto actualResult = userService.updateUser(CORRECT_ID_TO_FIND, updateDto);

                assertAll(
                        () -> assertEquals(expectedDto.firstName(), actualResult.firstName()),
                        () -> assertEquals(expectedDto.lastName(), actualResult.lastName()),
                        () -> assertEquals(expectedDto.email(), actualResult.email())
                );


                validatorMock.verify(() -> ValidatorUtil.validate(updateDto), times(1));

                verify(mockTx).begin();
                verify(mockTx).commit();
                verify(mockEm).close();
                verify(userDao).findById(mockEm, CORRECT_ID_TO_FIND);
                verify(userUpdateMapper).map(updateDto, mockUserToUpdate);
                verify(userReadMapper).map(updatedUserMock);
            }
        }

        @Test
        void updateUser_shouldThrowEntityNotFoundException_andRollback_whenUserNotFound() {

            UserUpdateDto updateDto = new UserUpdateDto("new_First", "new_Last");
            final Long NON_EXISTENT_ID = 999L;

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(mockEm.getTransaction()).thenReturn(mockTx);

            try (MockedStatic<ValidatorUtil> validatorMock = Mockito.mockStatic(ValidatorUtil.class)) {
                validatorMock.when(() -> ValidatorUtil.validate(updateDto))
                        .thenAnswer(invocation -> null);

                when(userDao.findById(mockEm, NON_EXISTENT_ID)).thenReturn(Optional.empty());
                when(mockTx.isActive()).thenReturn(true);

                assertThrows(EntityNotFoundException.class, () -> {
                    userService.updateUser(NON_EXISTENT_ID, updateDto);
                });

                verify(mockTx, never()).commit();
                verify(mockEm).close();
                verify(mockTx).rollback();
                verify(userDao).findById(mockEm, NON_EXISTENT_ID);
                verify(userUpdateMapper, never()).map(any(), any());
                verify(userReadMapper, never()).map(any());
            }
        }

        @Test
        void updateUser_shouldThrowValidationExceptionWhenDataIsInvalid() {
            UserUpdateDto updateDto = new UserUpdateDto("A", "B");
            final Long ID_TO_UPDATE = 1L;


            when(emf.createEntityManager()).thenReturn(mockEm);
            when(mockEm.getTransaction()).thenReturn(mockTx);

            try(MockedStatic<ValidatorUtil> mockValidator = Mockito.mockStatic(ValidatorUtil.class)) {
                mockValidator.when(() -> ValidatorUtil.validate(updateDto))
                        .thenThrow(new CustomValidationException(new ValidationErrorResponse(List.of())));

                assertThatThrownBy(() -> userService.updateUser(ID_TO_UPDATE, updateDto))
                        .isInstanceOf(CustomValidationException.class);
            }

            verify(mockTx, never()).begin();
            verify(userDao, never()).findById(any(EntityManager.class), any(Long.class));
            verify(userUpdateMapper, never()).map(any(UserUpdateDto.class), any(User.class));
            verify(userDao, never()).update(any(EntityManager.class), any(User.class));
        }
    }

    @Nested
    class ChangePasswordTests {

        private final Long USER_ID = 1L;
        private final String OLD_PASSWORD = "oldPassword123";
        private final String NEW_PASSWORD = "newPassword456";
        private final String HASHED_OLD_PASSWORD = "hashedOldPassword";
        private final String HASHED_NEW_PASSWORD = "hashedNewPassword";

        @Test
        void changePassword_shouldChangeIfDataIsCorrect_andUserFound() {

            PasswordChangeDto passwordChangeDto = new PasswordChangeDto(OLD_PASSWORD, NEW_PASSWORD, NEW_PASSWORD);
            User user = User.builder().build();
            user.setId(USER_ID);
            user.setPasswordHash(HASHED_OLD_PASSWORD);

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(mockEm.getTransaction()).thenReturn(mockTx);
            when(mockTx.isActive()).thenReturn(true);
            when(userDao.findById(mockEm, USER_ID)).thenReturn(Optional.of(user));

            try (MockedStatic<ValidatorUtil> mockValidator = Mockito.mockStatic(ValidatorUtil.class);
                 MockedStatic<PasswordUtil> mockPasswordUtil = Mockito.mockStatic(PasswordUtil.class)) {

                mockValidator.when(() -> ValidatorUtil.validate(passwordChangeDto))
                        .thenAnswer(invocation -> null);

                mockPasswordUtil.when(() -> PasswordUtil.verify(OLD_PASSWORD, HASHED_OLD_PASSWORD))
                        .thenReturn(true);
                mockPasswordUtil.when(() -> PasswordUtil.hash(NEW_PASSWORD))
                        .thenReturn(HASHED_NEW_PASSWORD);

                userService.changePassword(USER_ID, passwordChangeDto);

                mockValidator.verify(() -> ValidatorUtil.validate(passwordChangeDto), times(1));
                verify(userDao).findById(mockEm, USER_ID);
                verify(userDao).update(mockEm, user);
                verify(mockTx).begin();
                verify(mockTx).commit();
                assertEquals(HASHED_NEW_PASSWORD, user.getPasswordHash());
            }
        }

        @Test
        void changePassword_shouldThrowEntityNotFoundException_whenUserNotFound() {

            PasswordChangeDto passwordChangeDto = new PasswordChangeDto(OLD_PASSWORD, NEW_PASSWORD, NEW_PASSWORD);

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(mockEm.getTransaction()).thenReturn(mockTx);
            when(userDao.findById(mockEm, USER_ID)).thenReturn(Optional.empty());
            when(mockTx.isActive()).thenReturn(true);

            try (MockedStatic<ValidatorUtil> mockValidator = Mockito.mockStatic(ValidatorUtil.class)) {
                mockValidator.when(() -> ValidatorUtil.validate(passwordChangeDto))
                        .thenAnswer(invocation -> null);

                assertThatThrownBy(() -> userService.changePassword(USER_ID, passwordChangeDto))
                        .isInstanceOf(EntityNotFoundException.class)
                        .hasMessage("Пользователь не найден");

                verify(userDao).findById(mockEm, USER_ID);
                verify(mockTx).begin();
                verify(userDao, never()).update(any(EntityManager.class), any(User.class));
                verify(mockTx, never()).commit();
                verify(mockTx).rollback();
            }
        }

        @Test
        void changePassword_shouldThrowValidationException_whenDataIsInvalid() {

            PasswordChangeDto invalidDto = new PasswordChangeDto("", "", "");

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(mockEm.getTransaction()).thenReturn(mockTx);

            try (MockedStatic<ValidatorUtil> mockValidator = Mockito.mockStatic(ValidatorUtil.class)) {
                mockValidator.when(() -> ValidatorUtil.validate(invalidDto))
                        .thenThrow(new CustomValidationException(new ValidationErrorResponse(List.of())));

                assertThatThrownBy(() -> userService.changePassword(USER_ID, invalidDto))
                        .isInstanceOf(CustomValidationException.class);

                verify(userDao, never()).findById(any(EntityManager.class), any(Long.class));
                verify(userDao, never()).update(any(EntityManager.class), any(User.class));
                verify(mockTx, never()).begin();
                verify(mockTx, never()).commit();
            }
        }

        @Test
        void changePassword_shouldThrowIllegalArgumentException_whenOldPasswordIsIncorrect() {

            PasswordChangeDto passwordChangeDto = new PasswordChangeDto(OLD_PASSWORD, NEW_PASSWORD, NEW_PASSWORD);
            User user = User.builder().build();
            user.setId(USER_ID);
            user.setPasswordHash(HASHED_OLD_PASSWORD);

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(mockEm.getTransaction()).thenReturn(mockTx);
            when(mockTx.isActive()).thenReturn(true);
            when(userDao.findById(mockEm, USER_ID)).thenReturn(Optional.of(user));

            try (MockedStatic<ValidatorUtil> mockValidator = Mockito.mockStatic(ValidatorUtil.class);
                 MockedStatic<PasswordUtil> mockPasswordUtil = Mockito.mockStatic(PasswordUtil.class)) {

                mockValidator.when(() -> ValidatorUtil.validate(passwordChangeDto))
                        .thenAnswer(invocation -> null);

                mockPasswordUtil.when(() -> PasswordUtil.verify(OLD_PASSWORD, HASHED_OLD_PASSWORD))
                        .thenReturn(false);

                assertThatThrownBy(() -> userService.changePassword(USER_ID, passwordChangeDto))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("Передан неправильный пароль");

                mockPasswordUtil.verify(() -> PasswordUtil.verify(OLD_PASSWORD, HASHED_OLD_PASSWORD), times(1));
                verify(userDao).findById(mockEm, USER_ID);
                verify(userDao, never()).update(any(EntityManager.class), any(User.class));
                verify(mockTx).begin();
                verify(mockTx).rollback();
                verify(mockTx, never()).commit();
            }
        }

        @Test
        void changePassword_shouldHandleOptimisticLockException_andThrowConcurrentModificationException() {

            PasswordChangeDto passwordChangeDto = new PasswordChangeDto(OLD_PASSWORD, NEW_PASSWORD, NEW_PASSWORD);
            User user = User.builder().build();
            user.setId(USER_ID);
            user.setPasswordHash(HASHED_OLD_PASSWORD);

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(mockEm.getTransaction()).thenReturn(mockTx);
            when(mockTx.isActive()).thenReturn(true);
            when(userDao.findById(mockEm, USER_ID)).thenReturn(Optional.of(user));

            try (MockedStatic<ValidatorUtil> mockValidator = Mockito.mockStatic(ValidatorUtil.class);
                 MockedStatic<PasswordUtil> mockPasswordUtil = Mockito.mockStatic(PasswordUtil.class)) {

                mockValidator.when(() -> ValidatorUtil.validate(passwordChangeDto))
                        .thenAnswer(invocation -> null);

                mockPasswordUtil.when(() -> PasswordUtil.verify(OLD_PASSWORD, HASHED_OLD_PASSWORD))
                        .thenReturn(true);
                mockPasswordUtil.when(() -> PasswordUtil.hash(NEW_PASSWORD))
                        .thenReturn(HASHED_NEW_PASSWORD);

                doThrow(new OptimisticLockException()).when(userDao).update(mockEm, user);

                assertThatThrownBy(() -> userService.changePassword(USER_ID, passwordChangeDto))
                        .isInstanceOf(ConcurrentModificationException.class)
                        .hasMessage("Пользователь был изменен. Обновите данные и повторите попытку.");

                verify(userDao).findById(mockEm, USER_ID);
                verify(userDao).update(mockEm, user);
                verify(mockTx).begin();
                verify(mockTx).rollback();
                verify(mockTx, never()).commit();
            }
        }

        @Test
        void changePassword_shouldRollbackTransaction_onAnyException() {

            PasswordChangeDto passwordChangeDto = new PasswordChangeDto(OLD_PASSWORD, NEW_PASSWORD, NEW_PASSWORD);

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(mockEm.getTransaction()).thenReturn(mockTx);
            when(mockTx.isActive()).thenReturn(true);

            try (MockedStatic<ValidatorUtil> mockValidator = Mockito.mockStatic(ValidatorUtil.class)) {
                mockValidator.when(() -> ValidatorUtil.validate(passwordChangeDto))
                        .thenThrow(new RuntimeException("Unexpected error"));

                assertThatThrownBy(() -> userService.changePassword(USER_ID, passwordChangeDto))
                        .isInstanceOf(RuntimeException.class);

                verify(mockTx).rollback();
                verify(mockTx, never()).commit();
                verify(userDao, never()).findById(any(EntityManager.class), any(Long.class));
            }
        }
    }

    @Nested
    class DeleteUserTests {

        private final Long USER_ID = 1L;
        private final String CORRECT_PASSWORD = "correctPassword123";
        private final String INCORRECT_PASSWORD = "wrongPassword456";
        private final String HASHED_PASSWORD = "hashedCorrectPassword";

        @Test
        void deleteUser_shouldDelete_ifPasswordAndIdCorrect() {

            User user = User.builder().build();
            user.setId(USER_ID);
            user.setPasswordHash(HASHED_PASSWORD);

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(mockEm.getTransaction()).thenReturn(mockTx);
            when(mockTx.isActive()).thenReturn(true);
            when(userDao.findById(mockEm, USER_ID)).thenReturn(Optional.of(user));

            try (MockedStatic<PasswordUtil> mockPasswordUtil = Mockito.mockStatic(PasswordUtil.class)) {
                mockPasswordUtil.when(() -> PasswordUtil.verify(CORRECT_PASSWORD, HASHED_PASSWORD))
                        .thenReturn(true);

                userService.deleteUser(USER_ID, CORRECT_PASSWORD);

                verify(userDao).findById(mockEm, USER_ID);
                verify(userDao).delete(mockEm, user);
                verify(mockTx).begin();
                verify(mockTx).commit();
                verify(mockTx, never()).rollback();

                mockPasswordUtil.verify(() -> PasswordUtil.verify(CORRECT_PASSWORD, HASHED_PASSWORD), times(1));
            }
        }

        @Test
        void deleteUser_shouldThrow_andRollback_ifPasswordIncorrectAndIdCorrect() {

            User user = User.builder().build();
            user.setId(USER_ID);
            user.setPasswordHash(HASHED_PASSWORD);

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(mockEm.getTransaction()).thenReturn(mockTx);
            when(mockTx.isActive()).thenReturn(true);
            when(userDao.findById(mockEm, USER_ID)).thenReturn(Optional.of(user));

            try (MockedStatic<PasswordUtil> mockPasswordUtil = Mockito.mockStatic(PasswordUtil.class)) {
                mockPasswordUtil.when(() -> PasswordUtil.verify(INCORRECT_PASSWORD, HASHED_PASSWORD))
                        .thenReturn(false);

                assertThatThrownBy(() -> userService.deleteUser(USER_ID, INCORRECT_PASSWORD))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("Неверный пароль");

                verify(userDao).findById(mockEm, USER_ID);
                verify(userDao, never()).delete(any(EntityManager.class), any(User.class));
                verify(mockTx).begin();
                verify(mockTx).rollback();
                verify(mockTx, never()).commit();

                mockPasswordUtil.verify(() -> PasswordUtil.verify(INCORRECT_PASSWORD, HASHED_PASSWORD), times(1));
            }
        }

        @Test
        void deleteUser_shouldThrow_andRollback_ifPasswordCorrectAndIdIncorrect() {

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(mockEm.getTransaction()).thenReturn(mockTx);
            when(mockTx.isActive()).thenReturn(true);
            when(userDao.findById(mockEm, USER_ID)).thenReturn(Optional.empty());

            try (MockedStatic<PasswordUtil> mockPasswordUtil = Mockito.mockStatic(PasswordUtil.class)) {

                mockPasswordUtil.when(() -> PasswordUtil.verify(anyString(), anyString()))
                        .thenReturn(true);

                assertThatThrownBy(() -> userService.deleteUser(USER_ID, CORRECT_PASSWORD))
                        .isInstanceOf(EntityNotFoundException.class)
                        .hasMessage("Не найден пользователь для удаления с id: " + USER_ID);

                verify(userDao).findById(mockEm, USER_ID);
                verify(userDao, never()).delete(any(EntityManager.class), any(User.class));
                verify(mockTx).begin();
                verify(mockTx).rollback();
                verify(mockTx, never()).commit();

                mockPasswordUtil.verify(() -> PasswordUtil.verify(anyString(), anyString()), never());
            }
        }

        @Test
        void deleteUser_shouldHandleDatabaseException_andRollback() {

            User user = User.builder().build();
            user.setId(USER_ID);
            user.setPasswordHash(HASHED_PASSWORD);

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(mockEm.getTransaction()).thenReturn(mockTx);
            when(mockTx.isActive()).thenReturn(true);
            when(userDao.findById(mockEm, USER_ID)).thenReturn(Optional.of(user));

            try (MockedStatic<PasswordUtil> mockPasswordUtil = Mockito.mockStatic(PasswordUtil.class)) {
                mockPasswordUtil.when(() -> PasswordUtil.verify(CORRECT_PASSWORD, HASHED_PASSWORD))
                        .thenReturn(true);

                doThrow(new RuntimeException("Database error")).when(userDao).delete(mockEm, user);

                assertThatThrownBy(() -> userService.deleteUser(USER_ID, CORRECT_PASSWORD))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessage("Database error");

                verify(userDao).findById(mockEm, USER_ID);
                verify(userDao).delete(mockEm, user);
                verify(mockTx).begin();
                verify(mockTx).rollback();
                verify(mockTx, never()).commit();
            }
        }

        @Test
        void deleteUser_shouldHandleOptimisticLockException_andRollback() {

            User user = User.builder().build();
            user.setId(USER_ID);
            user.setPasswordHash(HASHED_PASSWORD);

            when(emf.createEntityManager()).thenReturn(mockEm);
            when(mockEm.getTransaction()).thenReturn(mockTx);
            when(mockTx.isActive()).thenReturn(true);
            when(userDao.findById(mockEm, USER_ID)).thenReturn(Optional.of(user));

            try (MockedStatic<PasswordUtil> mockPasswordUtil = Mockito.mockStatic(PasswordUtil.class)) {
                mockPasswordUtil.when(() -> PasswordUtil.verify(CORRECT_PASSWORD, HASHED_PASSWORD))
                        .thenReturn(true);

                doThrow(new OptimisticLockException()).when(userDao).delete(mockEm, user);

                assertThatThrownBy(() -> userService.deleteUser(USER_ID, CORRECT_PASSWORD))
                        .isInstanceOf(OptimisticLockException.class);

                verify(userDao).findById(mockEm, USER_ID);
                verify(userDao).delete(mockEm, user);
                verify(mockTx).begin();
                verify(mockTx).rollback();
                verify(mockTx, never()).commit();
            }
        }
    }
}