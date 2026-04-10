package org.example.integration.config;

import org.example.dao.RoleDao;
import org.example.dao.UserDao;
import org.example.mapper.RoleReadMapper;
import org.example.mapper.UserReadMapper;
import org.example.service.RoleService;
import org.junit.jupiter.api.BeforeAll;

public class AbstractRoleServiceIntegrationTest extends AbstractIntegrationTest {

    protected RoleDao roleDao;
    protected RoleReadMapper roleReadMapper;
    protected UserDao userDao;
    protected UserReadMapper userReadMapper;

    protected RoleService roleService;

    @BeforeAll
    void initServices() {
        // DAO
        userDao = new UserDao();
        roleDao = new RoleDao();

        // Мапперы
        roleReadMapper = new RoleReadMapper();
        userReadMapper = new UserReadMapper(roleReadMapper);

        // Сервисы
        roleService = new RoleService(sessionFactory, roleDao, userDao, userReadMapper);

    }
}
