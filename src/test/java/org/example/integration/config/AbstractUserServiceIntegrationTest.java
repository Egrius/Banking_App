package org.example.integration.config;

import org.example.dao.*;
import org.example.mapper.*;
import org.example.service.UserService;
import org.junit.jupiter.api.BeforeAll;

public abstract class AbstractUserServiceIntegrationTest extends AbstractIntegrationTest {

    protected UserDao userDao;

    protected RoleReadMapper roleReadMapper;
    protected UserReadMapper userReadMapper;
    protected  UserUpdateMapper userUpdateMapper;

    protected UserService userService;

    @BeforeAll
    void initServices() {
        // DAO
        userDao = new UserDao();

        // Мапперы
        roleReadMapper = new RoleReadMapper();
        userReadMapper = new UserReadMapper(roleReadMapper);
        userUpdateMapper = new UserUpdateMapper();

        // Сервисы
        userService = new UserService(sessionFactory, userDao, userReadMapper, userUpdateMapper);
    }

    /*
    Хелпер-методы
     */
}
