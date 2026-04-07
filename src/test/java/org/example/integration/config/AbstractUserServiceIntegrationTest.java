package org.example.integration.config;

import jakarta.persistence.EntityManager;
import org.example.dao.*;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.mapper.*;
import org.example.service.UserService;
import org.junit.jupiter.api.BeforeAll;

public abstract class AbstractUserServiceIntegrationTest extends AbstractIntegrationTest {

    protected UserDao userDao;
    protected RoleDao roleDao;

    protected RoleReadMapper roleReadMapper;
    protected UserReadMapper userReadMapper;
    protected  UserUpdateMapper userUpdateMapper;

    protected UserService userService;

    @BeforeAll
    void initServices() {
        // DAO
        userDao = new UserDao();
        roleDao = new RoleDao();

        // Мапперы
        roleReadMapper = new RoleReadMapper();
        userReadMapper = new UserReadMapper(roleReadMapper);
        userUpdateMapper = new UserUpdateMapper();

        // Сервисы
        userService = new UserService(sessionFactory, userDao, roleDao, userReadMapper, userUpdateMapper);

    }

    /*
    Хелпер-методы
     */

    public static Long createUserForDB(EntityManager em, String firstName, String lastName, String email, String passwordHash) {
        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .passwordHash(passwordHash)
                .build();

        Role userRole = em.createQuery("SELECT r FROM Role r WHERE r.name = 'USER'", Role.class)
                .getSingleResult();

        user.getRoles().add(userRole);

        em.persist(user);

        return user.getId();
    }
}
