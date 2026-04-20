package org.example.integration.service.business_logic;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.EntityTransaction;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.request.PageRequest;
import org.example.dto.response.PageResponse;
import org.example.dto.role.RoleReadDto;
import org.example.dto.user.*;
import org.example.entity.User;
import org.example.exception.security_exception.AccessDeniedException;
import org.example.exception.user.UserAlreadyExistsException;
import org.example.integration.service.config.AbstractUserServiceIntegrationTest;
import org.example.security.AuthContext;
import org.example.util.PasswordUtil;
import org.junit.jupiter.api.*;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class UserServiceBusinessLogicIT extends AbstractUserServiceIntegrationTest {

    @BeforeEach
    void cleanUp() {
        System.out.println("=== CLEANUP START ===");
        System.out.println("Executing cleanUp_userTable.sql");
        runSql("user_service/cleanUp_userTable.sql");

        System.out.println("Executing init_role_table.sql");
        runSql("role_service/init_role_table.sql");
        System.out.println("=== CLEANUP END ===");
    }

    private void createExistingUser(String firstName, String lastName, String email, String passwordHash) {
        EntityManager em = sessionFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            // Создание уже существующего пользователя
            AbstractUserServiceIntegrationTest.createUserForDB(em,  firstName,  lastName,  email,  passwordHash);

            tx.commit();
        } catch (Exception e) {
            if(tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    private UserReadDto createAndReturnUser(String firstName, String lastName, String email, String passwordHash) {

        EntityManager em = sessionFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            // Создание уже существующего пользователя
            User user = AbstractUserServiceIntegrationTest.createUserForDB(em,  firstName,  lastName,  email,  passwordHash);

            tx.commit();

            return new UserReadDto(
                    user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    user.getCreatedAt(),
                    user.getRoles().stream().map(r -> new RoleReadDto(r.getName())).toList()
            );

        } catch (Exception e) {
            if(tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

        @Nested
    class RegisterTests {

        @Test
        void registerUserShouldCreateNewUser_ifAllDataCorrect() {
            UserCreateDto userCreateDto = new UserCreateDto(
                    "TestName", "TestSurname", "12345", "testEmail@gmail.com"
            );

            UserReadDto userReadDto = userService.register(userCreateDto);

            try(EntityManager em = sessionFactory.createEntityManager();) {
                Optional<User> createdUserOpt = userDao.findByEmail(em, userReadDto.email());

                assertTrue(createdUserOpt.isPresent());
                User createdUser = createdUserOpt.get();

                assertEquals(userReadDto.firstName(), createdUser.getFirstName());
                assertEquals(userReadDto.lastName(), createdUser.getLastName());
                assertEquals(userReadDto.createdAt().truncatedTo(ChronoUnit.MILLIS),
                        createdUser.getCreatedAt().truncatedTo(ChronoUnit.MILLIS));

                assertTrue(createdUser.getRoles().stream().anyMatch(role -> role.getName().equals("USER")));
            }
        }

        @Test
        void registerUserShouldNotCreateNewUser_andThrowEntityNotFoundException_ifExists() {
            final String EXISTING_EMAIL = "testEmail@gmail.com";

            createExistingUser("TestName_canBeAnother", "TestLastname", EXISTING_EMAIL, PasswordUtil.hash("12345"));

            UserCreateDto alreadyExistedUsesCreateDto = new UserCreateDto(
                    "TestName", "TestSurname", "12345", "testEmail@gmail.com"
            );

            assertThatThrownBy(() -> userService.register(alreadyExistedUsesCreateDto))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining(EXISTING_EMAIL + " уже существует");

            try(EntityManager em = sessionFactory.createEntityManager();) {
                assertEquals(1, em.createQuery("SELECT u FROM User u WHERE u.email = :email")
                        .setParameter("email", EXISTING_EMAIL)
                        .getResultList()
                        .size());
            }
        }
    }

    @Nested
    class LoginTests {

        @Test
        void loginUser_shouldLoginSuccessfully_whenUserExists_andPasswordCorrect() {

            final String EXISTING_FIRST_NAME = "TestName_canBeAnother";
            final String EXISTING_LAST_NAME = "TestLastname";
            final String EXISTING_EMAIL = "testEmail@gmail.com";
            final String RAW_PASSWORD_FOR_EXISTING = "12345";

            createExistingUser(EXISTING_FIRST_NAME, EXISTING_LAST_NAME, EXISTING_EMAIL, PasswordUtil.hash(RAW_PASSWORD_FOR_EXISTING));

            try(EntityManager em = sessionFactory.createEntityManager();) {
                assertEquals(1, em.createQuery("SELECT u FROM User u WHERE u.email = :email")
                        .setParameter("email", EXISTING_EMAIL)
                        .getResultList()
                        .size());
            }

            UserLoginDto correctLoginDto = new UserLoginDto(EXISTING_EMAIL, RAW_PASSWORD_FOR_EXISTING);

            UserLoginReadDto loggedInUserReadDto = userService.login(correctLoginDto);

            assertEquals(EXISTING_FIRST_NAME, loggedInUserReadDto.userReadDto().firstName());
            assertEquals(EXISTING_LAST_NAME, loggedInUserReadDto.userReadDto().lastName());
            assertEquals(EXISTING_EMAIL, loggedInUserReadDto.userReadDto().email());
        }

        @Test
        void login_shouldThrowEntityNotFoundException_whenUserNotFound() {
            final String NOT_EXISTING_EMAIL = "testEmail@gmail.com";
            final String RAW_PASSWORD_FOR_NOT_EXISTING = "12345";

            final String ERROR_MESSAGE = "Пользователь не найден";

            UserLoginDto unknownLoginDto = new UserLoginDto(NOT_EXISTING_EMAIL, RAW_PASSWORD_FOR_NOT_EXISTING);

            try(EntityManager em = sessionFactory.createEntityManager();) {
                assertTrue(em.createQuery("SELECT u FROM User u WHERE u.email = :email")
                        .setParameter("email", NOT_EXISTING_EMAIL)
                        .getResultList().isEmpty());
            }

            System.out.println("\n --- ACTING ---\n");

            assertThatThrownBy(() -> userService.login(unknownLoginDto))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(ERROR_MESSAGE);

        }

        @Test
        void login_shouldThrowAccessDeniedException_whenPasswordIsIncorrect() {
            final String EXISTING_FIRST_NAME = "TestName_canBeAnother";
            final String EXISTING_LAST_NAME = "TestLastname";
            final String EXISTING_EMAIL = "testEmail@gmail.com";
            final String RAW_PASSWORD_FOR_EXISTING = "12345";

            final String INCORRECT_RAW_PASSWORD_FOR_EXISTING = "1234_incorrect";

            final String ERROR_MESSAGE = "Пароль неверен";

            createExistingUser(EXISTING_FIRST_NAME, EXISTING_LAST_NAME, EXISTING_EMAIL, PasswordUtil.hash(RAW_PASSWORD_FOR_EXISTING));

            try(EntityManager em = sessionFactory.createEntityManager();) {
                assertEquals(1, em.createQuery("SELECT u FROM User u WHERE u.email = :email")
                        .setParameter("email", EXISTING_EMAIL)
                        .getResultList()
                        .size());
            }

            UserLoginDto incorrectLoginDto = new UserLoginDto(EXISTING_EMAIL, INCORRECT_RAW_PASSWORD_FOR_EXISTING);

            assertThatThrownBy(() -> userService.login(incorrectLoginDto))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining(ERROR_MESSAGE);

        }
    }

    @Nested
    class FindAllTests {

        @BeforeEach
        void init() {
            createExistingUser("Name1", "Lastname1", "user1@gmail.com", "hashed");
            createExistingUser("Name2", "Lastname2", "user2@gmail.com", "hashed");
            createExistingUser("Name3", "Lastname3", "user3@gmail.com", "hashed");
            createExistingUser("Name4", "Lastname4", "user4@gmail.com", "hashed");
        }

        private final AuthContext adminAuthContext = new AuthContext(10L, "admin@gmail.com", List.of("ADMIN"));
        private final AuthContext userAuthContext =  new AuthContext(12L, "defaultUser@gmail.com", List.of("USER"));;

        @Test
        void findAll_shouldThrowAccessDeniedException_ifNotAdmin() {
            final String ERROR_MESSAGE = "Необходим доступ администратора";

            assertThatThrownBy(() -> userService.findAll(userAuthContext, PageRequest.of(0,2)))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining(ERROR_MESSAGE);
        }

        @Test
        void findAll_shouldReturnForAdmin_andPaginationShouldBeCorrect_ifPageRequestIsCorrect() {

            Integer PAGE_NUM = 0;
            Integer PAGE_SIZE = 2;

            PageRequest correctPageRequest_1 = PageRequest.of(PAGE_NUM, PAGE_SIZE);

            PageResponse<UserReadDto> pageResponse_1 = userService.findAll(adminAuthContext, correctPageRequest_1);

            assertNotNull(pageResponse_1);
            assertEquals(PAGE_NUM, pageResponse_1.getPageNumber());
            assertEquals(PAGE_SIZE, pageResponse_1.getPageSize());
            assertTrue(pageResponse_1.isHasNext());
            assertFalse(pageResponse_1.getContent().isEmpty());
            assertEquals(2, pageResponse_1.getContent().size());

            List<UserReadDto> content_1 = pageResponse_1.getContent();
            // Доделать проверку
            assertEquals("user1@gmail.com", content_1.get(0).email());
            assertEquals("user2@gmail.com", content_1.get(1).email());

            PAGE_NUM = 1;

            PageRequest correctPageRequest_2 = PageRequest.of(PAGE_NUM, PAGE_SIZE);

            PageResponse<UserReadDto> pageResponse_2 = userService.findAll(adminAuthContext, correctPageRequest_2);

            assertNotNull(pageResponse_2);
            assertEquals(PAGE_NUM, pageResponse_2.getPageNumber());
            assertEquals(PAGE_SIZE, pageResponse_2.getPageSize());
            assertFalse(pageResponse_2.isHasNext());
            assertFalse(pageResponse_2.getContent().isEmpty());
            assertEquals(2, pageResponse_2.getContent().size());

            List<UserReadDto> content_2 = pageResponse_2.getContent();

            assertEquals("user3@gmail.com", content_2.get(0).email());
            assertEquals("user4@gmail.com", content_2.get(1).email());
        }

        @Test
        void findAll_shouldReturnSecondPageCorrectly() {
            PageRequest pageRequest = PageRequest.of(1, 2);

            PageResponse<UserReadDto> response = userService.findAll(
                    new AuthContext(1L, "admin@test.com", List.of("ADMIN")),
                    pageRequest
            );

            assertNotNull(response);
            assertEquals(1, response.getPageNumber());
            assertEquals(2, response.getPageSize());
            assertEquals(4, response.getTotalElements());
            assertFalse(response.isHasNext());
            assertEquals(2, response.getContent().size());
            assertEquals("user3@gmail.com", response.getContent().get(0).email());
            assertEquals("user4@gmail.com", response.getContent().get(1).email());
        }
    }

    @Nested
    class UpdateUserTests {

        @Test
        void updateUser_shouldUpdateFieldsCorrectly() {
            UserReadDto createdUser = createAndReturnUser(
                    "OriginalFirstName", "OriginalLastName", "update@test.com", "password123"
            );

            UserUpdateDto updateDto = new UserUpdateDto("UpdatedFirstName", "UpdatedLastName");

            UserReadDto updatedUser = userService.updateUser(
                    createdUser.id(),
                    updateDto
            );

            assertEquals("UpdatedFirstName", updatedUser.firstName());
            assertEquals("UpdatedLastName", updatedUser.lastName());
            assertEquals(createdUser.email(), updatedUser.email());

            // Проверка в БД
            try (EntityManager em = sessionFactory.createEntityManager()) {
                User userFromDb = userDao.findById(em, createdUser.id()).orElseThrow();
                assertEquals("UpdatedFirstName", userFromDb.getFirstName());
                assertEquals("UpdatedLastName", userFromDb.getLastName());
            }
        }

        @Test
        void updateUser_shouldThrowEntityNotFoundException_whenUserNotFound() {
            UserUpdateDto updateDto = new UserUpdateDto("Name", "LastName");

            assertThatThrownBy(() -> userService.updateUser(
                    99999L,
                    updateDto)
            ).isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Не найден пользователь для обновления");
        }
    }

    @Nested
    class ChangePasswordTests {

        private final String OLD_PASSWORD = "oldPassword123";
        private final String NEW_PASSWORD = "newPassword456";

        @Test
        void changePassword_shouldUpdatePasswordSuccessfully() {
            UserReadDto createdUser = createAndReturnUser(
                    "User", "Test", "changepass@test.com", PasswordUtil.hash(OLD_PASSWORD)
            );

            PasswordChangeDto passwordChangeDto = new PasswordChangeDto(OLD_PASSWORD, NEW_PASSWORD, NEW_PASSWORD);

            userService.changePassword(
                    createdUser.id(),
                    passwordChangeDto
            );

            // Проверяем что новый пароль работает
            UserLoginDto loginDto = new UserLoginDto("changepass@test.com", NEW_PASSWORD);
            UserLoginReadDto loggedInUser = userService.login(loginDto);
            assertEquals(createdUser.id(), loggedInUser.userReadDto().id());
        }

        @Test
        void changePassword_shouldThrowAccessDeniedException_whenOldPasswordWrong() {
            UserReadDto createdUser = createAndReturnUser(
                    "User", "Test", "wrongpass@test.com", OLD_PASSWORD
            );

            PasswordChangeDto passwordChangeDto = new PasswordChangeDto("wrongPassword", NEW_PASSWORD, NEW_PASSWORD);

            assertThatThrownBy(() -> userService.changePassword(
                    createdUser.id(),
                    passwordChangeDto
            )).isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("неправильный пароль");
        }

        @Test
        void changePassword_shouldThrowEntityNotFoundException_whenUserNotFound() {
            PasswordChangeDto passwordChangeDto = new PasswordChangeDto(OLD_PASSWORD, NEW_PASSWORD, NEW_PASSWORD);

            assertThatThrownBy(() -> userService.changePassword(
                    99999L,
                    passwordChangeDto
            )).isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Пользователь не найден");
        }
    }

    @Nested
    class DeleteUserTests {

        private final String USER_PASSWORD = "deletePassword123";

        @Test
        void deleteUser_shouldRemoveUserFromDatabase_whenOwnerDeletesOwnProfile() {
            UserReadDto createdUser = createAndReturnUser(
                    "ToDelete", "OwnerDelete", "ownerdelete@test.com", PasswordUtil.hash(USER_PASSWORD)
            );

            // Проверяем что пользователь существует
            try (EntityManager em = sessionFactory.createEntityManager()) {
                assertTrue(userDao.findById(em, createdUser.id()).isPresent());
            }

            userService.deleteUser(
                    createdUser.id(),
                    USER_PASSWORD
            );

            // Проверяем что пользователь удален
            try (EntityManager em = sessionFactory.createEntityManager()) {
                assertFalse(userDao.findById(em, createdUser.id()).isPresent());
            }
        }

        @Test
        void deleteUser_shouldRemoveUserFromDatabase_whenAdminDeletesUser() {
            UserReadDto createdUser = createAndReturnUser(
                    "ToDelete", "AdminDelete", "admindelete@test.com", USER_PASSWORD
            );

            try (EntityManager em = sessionFactory.createEntityManager()) {
                assertTrue(userDao.findById(em, createdUser.id()).isPresent());
            }

            userService.deleteUser(
                    createdUser.id(),
                    "anyPassword"
            );

            try (EntityManager em = sessionFactory.createEntityManager()) {
                assertFalse(userDao.findById(em, createdUser.id()).isPresent());
            }
        }

        @Test
        void deleteUser_shouldThrowAccessDeniedException_whenOwnerProvidesWrongPassword() {
            UserReadDto createdUser = createAndReturnUser(
                    "ToDelete", "WrongPass", "wrongpassdelete@test.com", USER_PASSWORD
            );

            assertThatThrownBy(() -> userService.deleteUser(
                    createdUser.id(),
                    "wrongPassword"
            )).isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Неверный пароль");

            // Проверяем что пользователь не удален
            try (EntityManager em = sessionFactory.createEntityManager()) {
                assertTrue(userDao.findById(em, createdUser.id()).isPresent());
            }
        }

        @Test
        void deleteUser_shouldThrowEntityNotFoundException_whenUserNotFound() {
            assertThatThrownBy(() -> userService.deleteUser(
                    99999L,
                    "password"
            )).isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Не найден пользователь для удаления");
        }
    }
}
