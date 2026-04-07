package org.example.integration.service.business_logic;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.EntityTransaction;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.request.PageRequest;
import org.example.dto.response.PageResponse;
import org.example.dto.user.UserCreateDto;
import org.example.dto.user.UserLoginDto;
import org.example.dto.user.UserReadDto;
import org.example.entity.User;
import org.example.exception.security_exception.AccessDeniedException;
import org.example.exception.user.UserAlreadyExistsException;
import org.example.integration.config.AbstractUserServiceIntegrationTest;
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
        EntityManager em1 = sessionFactory.createEntityManager();
        EntityTransaction tx = em1.getTransaction();
        try {
            tx.begin();

            // Создание уже существующего пользователя
            AbstractUserServiceIntegrationTest.createUserForDB(em1,  firstName,  lastName,  email,  passwordHash);

            tx.commit();
        } catch (Exception e) {
            if(tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em1.close();
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

            UserReadDto loggedInUserReadDto = userService.login(correctLoginDto);

            assertEquals(EXISTING_FIRST_NAME, loggedInUserReadDto.firstName());
            assertEquals(EXISTING_LAST_NAME, loggedInUserReadDto.lastName());
            assertEquals(EXISTING_EMAIL, loggedInUserReadDto.email());
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

            final Integer PAGE_NUM = 0;
            final Integer PAGE_SIZE = 5;

            PageRequest correctPageRequest = PageRequest.of(PAGE_NUM, PAGE_SIZE);

            PageResponse<UserReadDto> pageResponse = userService.findAll(adminAuthContext, correctPageRequest);

            assertNotNull(pageResponse);
            assertEquals(PAGE_NUM, pageResponse.getPageNumber());
            assertEquals(PAGE_SIZE, pageResponse.getPageSize());
            assertFalse(pageResponse.isHasNext());
            assertFalse(pageResponse.getContent().isEmpty());
            assertEquals(4, pageResponse.getContent().size());

            List<UserReadDto> content = pageResponse.getContent();
            // Доделать проверку
            content.forEach(dto -> {

            });
        }
    }
}
