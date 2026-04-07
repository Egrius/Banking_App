package org.example.integration.service.business_logic;

import jakarta.persistence.EntityManager;
import org.example.dto.user.UserCreateDto;
import org.example.dto.user.UserReadDto;
import org.example.entity.User;
import org.example.integration.config.AbstractUserServiceIntegrationTest;
import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceBusinessLogicIT extends AbstractUserServiceIntegrationTest {

    @Test
    void registerUserShouldCreateNewUser_ifAllDataCorrect() {
        UserCreateDto userCreateDto = new UserCreateDto(
                "TestName", "TestSurname", "12345", "testEmail@gmail.com"
        );

        UserReadDto userReadDto = userService.register(userCreateDto);

        EntityManager em = sessionFactory.createEntityManager();

        Optional<User> createduserOpt = userDao.findByEmail(em, userReadDto.email());

        assertTrue(createduserOpt.isPresent());
        User createdUser = createduserOpt.get();

        assertEquals(userReadDto.firstName(), createdUser.getFirstName());
        assertEquals(userReadDto.lastName(), createdUser.getLastName());
        assertEquals(userReadDto.createdAt().truncatedTo(ChronoUnit.MILLIS),
                createdUser.getCreatedAt().truncatedTo(ChronoUnit.MILLIS));

    }
}
