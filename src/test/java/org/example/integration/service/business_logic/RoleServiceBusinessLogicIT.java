package org.example.integration.service.business_logic;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.EntityTransaction;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.role.RoleReadDto;
import org.example.dto.user.UserCreateDto;
import org.example.dto.user.UserReadDto;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.integration.config.AbstractRoleServiceIntegrationTest;
import org.example.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class RoleServiceBusinessLogicIT extends AbstractRoleServiceIntegrationTest {

    @BeforeEach
    void cleanUp() {
        runSql("clean/cleanUp_userTable.sql");
        runSql("role_service/init_role_table.sql");
    }

    private UserReadDto createTestUser(String firstName, String lastName, String email, String rawPassword) {
        UserCreateDto createDto = new UserCreateDto(firstName, lastName, rawPassword, email);

        EntityManager em = sessionFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            User user = User.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .passwordHash(PasswordUtil.hash(rawPassword))
                    .build();

            Role userRole = roleDao.findByName(em, "USER")
                    .orElseThrow(() -> new RuntimeException("Роль USER не найдена"));
            user.getRoles().add(userRole);

            userDao.save(em, user);
            tx.commit();

            return new UserReadDto(user.getId(), user.getFirstName(), user.getLastName(),
                    user.getEmail(), user.getCreatedAt(), user.getRoles().stream().map(r -> new RoleReadDto(r.getName())).toList());
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    @Nested
    class CreateRoleTests {

        @Test
        void createRole_shouldCreateNewRole_whenNameIsValid() {
            String roleName = "MANAGER";

            roleService.createRole(roleName); // AuthContext будет на слое выше

            EntityManager em = sessionFactory.createEntityManager();
            try {
                Role createdRole = roleDao.findByName(em, roleName).orElseThrow();
                assertEquals(roleName, createdRole.getName());
            } finally {
                em.close();
            }
        }

        @Test
        void createRole_shouldThrowException_whenRoleAlreadyExists() {
            String roleName = "ADMIN"; // уже существует из init_role_table.sql

            assertThatThrownBy(() -> roleService.createRole(roleName))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("уже существует");
        }

        @Test
        void createRole_shouldConvertNameToUpperCase() {
            String roleName = "moderator";

            roleService.createRole(roleName);

            EntityManager em = sessionFactory.createEntityManager();
            try {
                assertTrue(roleDao.findByName(em, "MODERATOR").isPresent());
                assertFalse(roleDao.findByName(em, "moderator").isPresent());
            } finally {
                em.close();
            }
        }
    }

    @Nested
    class FindRoleTests {

        @Test
        void findById_shouldReturnRole_whenRoleExists() {
            Long roleId = 200L;

            RoleReadDto role = roleService.findById(roleId);

            assertNotNull(role);
            assertEquals("ADMIN", role.roleName());
        }

        @Test
        void findById_shouldThrowEntityNotFoundException_whenRoleDoesNotExist() {
            assertThatThrownBy(() -> roleService.findById(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("не найдена");
        }

        @Test
        void findByName_shouldReturnRole_whenRoleExists() {
            RoleReadDto role = roleService.findByName("USER");

            assertNotNull(role);
            assertEquals("USER", role.roleName());
        }

        @Test
        void findByName_shouldBeCaseInsensitive() {
            RoleReadDto role = roleService.findByName("user");

            assertNotNull(role);
            assertEquals("USER", role.roleName());
        }

        @Test
        void findByName_shouldThrowEntityNotFoundException_whenRoleDoesNotExist() {
            assertThatThrownBy(() -> roleService.findByName("NONEXISTENT"))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("не найдена");
        }
    }

    @Nested
    class FindAllRolesTests {

        @Test
        void findAll_shouldReturnAllRoles() {
            List<RoleReadDto> roles = roleService.findAll();

            assertNotNull(roles);
            assertTrue(roles.size() >= 2); // минимум ADMIN и USER
            assertTrue(roles.stream().anyMatch(r -> r.roleName().equals("ADMIN")));
            assertTrue(roles.stream().anyMatch(r -> r.roleName().equals("USER")));
        }
    }

    @Nested
    class UpdateRoleTests {

        @Test
        void updateRole_shouldUpdateRoleNameSuccessfully() {
            // Создаем новую роль для обновления
            roleService.createRole("OLDROLE");

            EntityManager em = sessionFactory.createEntityManager();
            Long roleId;
            try {
                roleId = roleDao.findByName(em, "OLDROLE").orElseThrow().getId();
            } finally {
                em.close();
            }

            RoleReadDto updatedRole = roleService.updateRole(roleId, "NEWROLE");

            assertEquals("NEWROLE", updatedRole.roleName());

            // Проверяем в БД
            em = sessionFactory.createEntityManager();
            try {
                assertTrue(roleDao.findByName(em, "NEWROLE").isPresent());
                assertFalse(roleDao.findByName(em, "OLDROLE").isPresent());
            } finally {
                em.close();
            }
        }

        @Test
        void updateRole_shouldThrowException_whenNewNameAlreadyExists() {
            // Создаем роль для обновления
            roleService.createRole("ROLETOUPDATE");

            EntityManager em = sessionFactory.createEntityManager();
            Long roleId;
            try {
                roleId = roleDao.findByName(em, "ROLETOUPDATE").orElseThrow().getId();
            } finally {
                em.close();
            }

            // Пытаемся обновить на существующее имя
            assertThatThrownBy(() -> roleService.updateRole(roleId, "ADMIN"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("уже существует");
        }

        @Test
        void updateRole_shouldThrowEntityNotFoundException_whenRoleDoesNotExist() {
            assertThatThrownBy(() -> roleService.updateRole(999L, "NEWNAME"))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("не найдена");
        }
    }

    @Nested
    class DeleteRoleTests {

        @Test
        void deleteRole_shouldDeleteRoleSuccessfully_whenRoleNotUsed() {
            // Создаем новую роль для удаления
            roleService.createRole("TODELETE");

            EntityManager em = sessionFactory.createEntityManager();
            Long roleId;
            try {
                roleId = roleDao.findByName(em, "TODELETE").orElseThrow().getId();
            } finally {
                em.close();
            }

            roleService.deleteRole(roleId);

            // Проверяем что роль удалена
            em = sessionFactory.createEntityManager();
            try {
                assertFalse(roleDao.findByName(em, "TODELETE").isPresent());
            } finally {
                em.close();
            }
        }

        @Test
        void deleteRole_shouldThrowException_whenRoleIsUsedByUsers() {
            // Роль USER используется пользователями по умолчанию
            createTestUser("testUser", "testLast", "test@gmail.com", "hashed");

            EntityManager em = sessionFactory.createEntityManager();
            Long roleId;
            try {
                roleId = roleDao.findByName(em, "USER").orElseThrow().getId();
            } finally {
                em.close();
            }

            assertThatThrownBy(() -> roleService.deleteRole(roleId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("используется");
        }

        @Test
        void deleteRole_shouldThrowEntityNotFoundException_whenRoleDoesNotExist() {
            assertThatThrownBy(() -> roleService.deleteRole(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("не найдена");
        }
    }

    @Nested
    class AssignRoleTests {

        private UserReadDto testUser;

        @BeforeEach
        void setUp() {
            testUser = createTestUser("RoleTest", "User", "roletest@test.com", "password123");
        }

        @Test
        void assignRoleToUser_shouldAssignRoleSuccessfully() {
            EntityManager em = sessionFactory.createEntityManager();
            Long roleId;
            try {
                roleId = roleDao.findByName(em, "ADMIN").orElseThrow().getId();
            } finally {
                em.close();
            }

            // У пользователя только роль USER
            roleService.assignRoleToUser(testUser.id(), roleId);

            // Проверяем что роль назначена
            em = sessionFactory.createEntityManager();
            try {
                User user = userDao.findById(em, testUser.id()).orElseThrow();
                assertTrue(user.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN")));
                assertTrue(user.getRoles().stream().anyMatch(r -> r.getName().equals("USER")));
            } finally {
                em.close();
            }
        }

        @Test
        void assignRoleToUser_shouldDoNothing_whenRoleAlreadyAssigned() {
            EntityManager em = sessionFactory.createEntityManager();
            Long roleId;
            try {
                roleId = roleDao.findByName(em, "USER").orElseThrow().getId();
            } finally {
                em.close();
            }

            // Попытка назначить уже существующую роль
            roleService.assignRoleToUser(testUser.id(), roleId);

            // Проверяем что роль все еще одна
            em = sessionFactory.createEntityManager();
            try {
                User user = userDao.findById(em, testUser.id()).orElseThrow();
                long roleCount = user.getRoles().stream().filter(r -> r.getName().equals("USER")).count();
                assertEquals(1, roleCount);
            } finally {
                em.close();
            }
        }

        @Test
        void assignRoleToUser_shouldThrowEntityNotFoundException_whenUserNotFound() {
            assertThatThrownBy(() -> roleService.assignRoleToUser(999L, 1L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("не найден");
        }

        @Test
        void assignRoleToUser_shouldThrowEntityNotFoundException_whenRoleNotFound() {
            assertThatThrownBy(() -> roleService.assignRoleToUser(testUser.id(), 999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("не найдена");
        }
    }

    @Nested
    class AssignMultipleRolesTests {

        private UserReadDto testUser;

        @BeforeEach
        void setUp() {
            testUser = createTestUser("MultiRoleTest", "User", "multi@test.com", "password123");
        }

        @Test
        void assignRolesToUser_shouldAssignMultipleRolesSuccessfully() {
            EntityManager em = sessionFactory.createEntityManager();
            Set<Long> roleIds;
            try {
                Long adminId = roleDao.findByName(em, "ADMIN").orElseThrow().getId();
                Long userId = roleDao.findByName(em, "USER").orElseThrow().getId();
                roleIds = Set.of(adminId, userId);
            } finally {
                em.close();
            }

            roleService.assignRolesToUser(testUser.id(), roleIds);

            // Проверяем что обе роли назначены
            em = sessionFactory.createEntityManager();
            try {
                User user = userDao.findById(em, testUser.id()).orElseThrow();
                assertEquals(2, user.getRoles().size());
                assertTrue(user.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN")));
                assertTrue(user.getRoles().stream().anyMatch(r -> r.getName().equals("USER")));
            } finally {
                em.close();
            }
        }

        @Test
        void assignRolesToUser_shouldSkipAlreadyAssignedRoles() {
            EntityManager em = sessionFactory.createEntityManager();
            Set<Long> roleIds;
            try {
                Long userId = roleDao.findByName(em, "USER").orElseThrow().getId();
                Long adminId = roleDao.findByName(em, "ADMIN").orElseThrow().getId();
                roleIds = Set.of(userId, adminId);
            } finally {
                em.close();
            }

            // У пользователя уже есть роль USER
            roleService.assignRolesToUser(testUser.id(), roleIds);

            // Должна добавиться только ADMIN
            em = sessionFactory.createEntityManager();
            try {
                User user = userDao.findById(em, testUser.id()).orElseThrow();
                assertEquals(2, user.getRoles().size());
            } finally {
                em.close();
            }
        }
    }

    @Nested
    class RemoveRoleTests {

        private UserReadDto testUser;

        @BeforeEach
        void setUp() {
            testUser = createTestUser("RemoveRoleTest", "User", "removerole@test.com", "password123");

            // Назначаем дополнительную роль
            EntityManager em = sessionFactory.createEntityManager();
            try {
                Long adminId = roleDao.findByName(em, "ADMIN").orElseThrow().getId();
                roleService.assignRoleToUser(testUser.id(), adminId);
            } finally {
                em.close();
            }
        }

        @Test
        void removeRoleFromUser_shouldRemoveRoleSuccessfully() {
            EntityManager em = sessionFactory.createEntityManager();
            Long roleId;
            try {
                roleId = roleDao.findByName(em, "ADMIN").orElseThrow().getId();
            } finally {
                em.close();
            }

            roleService.removeRoleFromUser(testUser.id(), roleId);

            em = sessionFactory.createEntityManager();
            try {
                User user = userDao.findById(em, testUser.id()).orElseThrow();
                assertFalse(user.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN")));
                assertTrue(user.getRoles().stream().anyMatch(r -> r.getName().equals("USER")));
            } finally {
                em.close();
            }
        }

        @Test
        void removeRoleFromUser_shouldDoNothing_whenRoleNotAssigned() {
            EntityManager em = sessionFactory.createEntityManager();
            Long roleId;
            try {
                // Создаем новую роль
                roleService.createRole("TESTROLE");
                roleId = roleDao.findByName(em, "TESTROLE").orElseThrow().getId();
            } finally {
                em.close();
            }

            // У пользователя нет этой роли
            roleService.removeRoleFromUser(testUser.id(), roleId);

            // Проверяем что ничего не изменилось
            em = sessionFactory.createEntityManager();
            try {
                User user = userDao.findById(em, testUser.id()).orElseThrow();
                assertEquals(2, user.getRoles().size()); // USER и ADMIN
            } finally {
                em.close();
            }
        }
    }

    @Nested
    class RemoveAllRolesTests {

        @Test
        void removeAllRolesFromUser_shouldRemoveAllRolesSuccessfully() {
            UserReadDto testUser = createTestUser("NoRolesTest", "User", "noroles@test.com", "password123");

            // Назначаем дополнительные роли
            EntityManager em = sessionFactory.createEntityManager();
            try {
                Long adminId = roleDao.findByName(em, "ADMIN").orElseThrow().getId();
                roleService.assignRoleToUser(testUser.id(), adminId);
            } finally {
                em.close();
            }

            // Проверяем что есть роли
            em = sessionFactory.createEntityManager();
            try {
                User user = userDao.findById(em, testUser.id()).orElseThrow();
                assertTrue(user.getRoles().size() > 0);
            } finally {
                em.close();
            }

            roleService.removeAllRolesFromUser(testUser.id());

            // Проверяем что ролей нет
            em = sessionFactory.createEntityManager();
            try {
                User user = userDao.findById(em, testUser.id()).orElseThrow();
                assertEquals(0, user.getRoles().size());
            } finally {
                em.close();
            }
        }
    }

    @Nested
    class GetUsersWithRoleTests {

        @Test
        void getUsersWithRole_shouldReturnUsersHavingRole() {
            // Создаем пользователя с ролью ADMIN
            UserReadDto adminUser = createTestUser("Admin", "User", "adminuser@test.com", "password123");

            EntityManager em = sessionFactory.createEntityManager();
            Long adminRoleId;
            try {
                adminRoleId = roleDao.findByName(em, "ADMIN").orElseThrow().getId();
                roleService.assignRoleToUser(adminUser.id(), adminRoleId);
            } finally {
                em.close();
            }

            List<UserReadDto> usersWithAdminRole = roleService.getUsersWithRole(adminRoleId);

            assertNotNull(usersWithAdminRole);
            assertTrue(usersWithAdminRole.stream().anyMatch(u -> u.email().equals("adminuser@test.com")));
        }
    }

    @Nested
    class GetUserRolesTests {

        @Test
        void getUserRoles_shouldReturnAllUserRoles() {
            UserReadDto testUser = createTestUser("GetRolesTest", "User", "getroles@test.com", "password123");

            EntityManager em = sessionFactory.createEntityManager();
            Long adminRoleId;
            try {
                adminRoleId = roleDao.findByName(em, "ADMIN").orElseThrow().getId();
                roleService.assignRoleToUser(testUser.id(), adminRoleId);
            } finally {
                em.close();
            }

            Set<RoleReadDto> userRoles = roleService.getUserRoles(testUser.id());

            assertNotNull(userRoles);
            assertEquals(2, userRoles.size());
            assertTrue(userRoles.stream().anyMatch(r -> r.roleName().equals("USER")));
            assertTrue(userRoles.stream().anyMatch(r -> r.roleName().equals("ADMIN")));
        }
    }

    @Nested
    class HasRoleTests {

        private UserReadDto testUser;

        @BeforeEach
        void setUp() {
            testUser = createTestUser("HasRoleTest", "User", "hasrole@test.com", "password123");
        }

        @Test
        void hasRole_shouldReturnTrue_whenUserHasRole() {
            boolean hasUserRole = roleService.hasRole(testUser.id(), "USER");
            assertTrue(hasUserRole);
        }

        @Test
        void hasRole_shouldReturnFalse_whenUserDoesNotHaveRole() {
            boolean hasAdminRole = roleService.hasRole(testUser.id(), "ADMIN");
            assertFalse(hasAdminRole);
        }

        @Test
        void hasRole_shouldBeCaseInsensitive() {
            boolean hasUserRole = roleService.hasRole(testUser.id(), "user");
            assertTrue(hasUserRole);
        }
    }

    @Nested
    class HasAnyRoleTests {

        private UserReadDto testUser;

        @BeforeEach
        void setUp() {
            testUser = createTestUser("HasAnyRoleTest", "User", "hasany@test.com", "password123");
        }

        @Test
        void hasAnyRole_shouldReturnTrue_whenUserHasAtLeastOneRole() {
            boolean hasAny = roleService.hasAnyRole(testUser.id(), Set.of("ADMIN", "USER"));
            assertTrue(hasAny);
        }

        @Test
        void hasAnyRole_shouldReturnFalse_whenUserHasNoneOfTheRoles() {
            boolean hasAny = roleService.hasAnyRole(testUser.id(), Set.of("ADMIN", "MANAGER"));
            assertFalse(hasAny);
        }
    }

    @Nested
    class HasAllRolesTests {

        private UserReadDto testUser;

        @BeforeEach
        void setUp() {
            testUser = createTestUser("HasAllRolesTest", "User", "hasall@test.com", "password123");

            EntityManager em = sessionFactory.createEntityManager();
            try {
                Long adminId = roleDao.findByName(em, "ADMIN").orElseThrow().getId();
                roleService.assignRoleToUser(testUser.id(), adminId);
            } finally {
                em.close();
            }
        }

        @Test
        void hasAllRoles_shouldReturnTrue_whenUserHasAllRoles() {
            boolean hasAll = roleService.hasAllRoles(testUser.id(), Set.of("USER", "ADMIN"));
            assertTrue(hasAll);
        }

        @Test
        void hasAllRoles_shouldReturnFalse_whenUserMissingSomeRole() {
            boolean hasAll = roleService.hasAllRoles(testUser.id(), Set.of("USER", "ADMIN", "MANAGER"));
            assertFalse(hasAll);
        }
    }
}