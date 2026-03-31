package org.example.service;

import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dao.RoleDao;
import org.example.dao.UserDao;
import org.example.dto.role.RoleReadDto;
import org.example.dto.user.UserReadDto;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.mapper.UserReadMapper;
import org.example.security.AuthContext;
import org.example.util.SecurityUtil;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <h3>Сервис для управления ролями пользователей.</h3>
 * <p>
 * Все методы сервиса доступны только администраторам системы.
 * Обычные пользователи не имеют доступа к информации о ролях.
 * </p>
 * <p>
 * Роли используются для разграничения прав доступа:
 * <ul>
 *     <li>{@code ADMIN} — полный доступ ко всем функциям системы</li>
 *     <li>{@code USER} — базовый доступ (счета, карты, переводы)</li>
 * </ul>
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class RoleService {

    private final RoleDao roleDao;
    private final UserDao userDao;
    private final UserReadMapper userReadMapper;
    private final EntityManagerFactory entityManagerFactory;

    /**
     * <h3>Создание новой роли.</h3>
     * <p>
     * <b>Доступно только администратору.</b>
     * </p>
     * <p>Требования к имени роли:
     * <ul>
     *     <li>не может быть пустым</li>
     *     <li>длина не менее 3 символов</li>
     *     <li>должно быть уникальным (регистронезависимо)</li>
     * </ul>
     * </p>
     *
     * @param roleName     имя новой роли
     * @param authContext  контекст аутентификации
     * @throws IllegalArgumentException если имя роли не соответствует требованиям
     * @throws org.example.exception.security_exception.AccessDeniedException если текущий пользователь не администратор
     */
    public void createRole(String roleName, AuthContext authContext) {
        SecurityUtil.checkAdmin(authContext);

        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("Нельзя создать роль без имени");
        }
        if (roleName.length() < 3) {
            throw new IllegalArgumentException("Роль должна содержать больше 3-х символов");
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            if (roleDao.findByName(em, roleName.toUpperCase()).isPresent()) {
                throw new IllegalArgumentException("Роль с именем " + roleName.toUpperCase() + " уже существует");
            }

            Role role = new Role(roleName.toUpperCase());
            roleDao.save(em, role);

            tx.commit();
            log.info("Создана новая роль {} администратором {}", roleName.toUpperCase(), authContext.getUserId());

        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * <h3>Поиск роли по идентификатору.</h3>
     * <p>
     * <b>Доступно только администратору.</b>
     * </p>
     *
     * @param roleId       идентификатор роли
     * @param authContext  контекст аутентификации
     * @return DTO с информацией о роли
     * @throws EntityNotFoundException если роль не найдена
     * @throws org.example.exception.security_exception.AccessDeniedException если текущий пользователь не администратор
     */
    public RoleReadDto findById(Long roleId, AuthContext authContext) {
        SecurityUtil.checkAdmin(authContext);

        EntityManager em = entityManagerFactory.createEntityManager();

        try {
            Role role = roleDao.findById(em, roleId)
                    .orElseThrow(() -> new EntityNotFoundException("Роль с id " + roleId + " не найдена"));
            return new RoleReadDto(role.getName());
        } finally {
            em.close();
        }
    }

    /**
     * <h3>Поиск роли по имени.</h3>
     * <p>
     * <b>Доступно только администратору.</b>
     * Поиск выполняется без учета регистра.
     * </p>
     *
     * @param name         имя роли
     * @param authContext  контекст аутентификации
     * @return DTO с информацией о роли
     * @throws IllegalArgumentException если имя роли пустое
     * @throws EntityNotFoundException если роль не найдена
     * @throws org.example.exception.security_exception.AccessDeniedException если текущий пользователь не администратор
     */
    public RoleReadDto findByName(String name, AuthContext authContext) {
        SecurityUtil.checkAdmin(authContext);

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя роли не может быть пустым");
        }

        EntityManager em = entityManagerFactory.createEntityManager();

        try {
            Role role = roleDao.findByName(em, name.toUpperCase())
                    .orElseThrow(() -> new EntityNotFoundException("Роль с именем " + name.toUpperCase() + " не найдена"));
            return new RoleReadDto(role.getName());
        } finally {
            em.close();
        }
    }

    /**
     * Получение списка всех ролей.
     * <p>
     * <b>Доступно только администратору.</b>
     * </p>
     *
     * @param authContext контекст аутентификации
     * @return список всех ролей в системе
     * @throws org.example.exception.security_exception.AccessDeniedException если текущий пользователь не администратор
     */
    public List<RoleReadDto> findAll(AuthContext authContext) {
        SecurityUtil.checkAdmin(authContext);

        EntityManager em = entityManagerFactory.createEntityManager();

        try {
            return roleDao.findAll(em)
                    .stream()
                    .map(this::mapToReadDto)
                    .toList();
        } finally {
            em.close();
        }
    }

    /**
     * <h3>Обновление имени роли.</h3>
     * <p>
     * <b>Доступно только администратору.</b>
     * </p>
     *
     * @param roleId       идентификатор роли
     * @param newName      новое имя роли
     * @param authContext  контекст аутентификации
     * @return DTO с обновленной информацией о роли
     * @throws EntityNotFoundException если роль не найдена
     * @throws IllegalArgumentException если новое имя не соответствует требованиям или уже существует
     * @throws org.example.exception.security_exception.AccessDeniedException если текущий пользователь не администратор
     */
    public RoleReadDto updateRole(Long roleId, String newName, AuthContext authContext) {
        SecurityUtil.checkAdmin(authContext);

        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Имя роли не может быть пустым");
        }
        if (newName.length() < 3) {
            throw new IllegalArgumentException("Роль должна содержать больше 3-х символов");
        }

        String normalizedName = newName.toUpperCase();
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            Role role = roleDao.findById(em, roleId)
                    .orElseThrow(() -> new EntityNotFoundException("Роль с id " + roleId + " не найдена"));

            roleDao.findByName(em, normalizedName)
                    .filter(existing -> !existing.getId().equals(roleId))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Роль с именем " + normalizedName + " уже существует");
                    });

            role.setName(normalizedName);
            roleDao.update(em, role);

            tx.commit();
            log.info("Роль {} обновлена администратором {} на {}", roleId, authContext.getUserId(), normalizedName);
            return mapToReadDto(role);

        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * <h3>Удаление роли.</h3>
     * <p>
     * <b>Доступно только администратору.</b>
     * Роль не может быть удалена, если она назначена хотя бы одному пользователю.
     * </p>
     *
     * @param roleId       идентификатор роли
     * @param authContext  контекст аутентификации
     * @throws EntityNotFoundException если роль не найдена
     * @throws IllegalStateException если роль используется
     * @throws org.example.exception.security_exception.AccessDeniedException если текущий пользователь не администратор
     */
    public void deleteRole(Long roleId, AuthContext authContext) {
        SecurityUtil.checkAdmin(authContext);

        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            Role role = roleDao.findById(em, roleId)
                    .orElseThrow(() -> new EntityNotFoundException("Роль с id " + roleId + " не найдена"));

            if (roleDao.userWithThisRoleExist(em, roleId)) {
                throw new IllegalStateException("Нельзя удалить роль " + role.getName() + ", т.к она используется");
            }

            roleDao.delete(em, role);
            tx.commit();

            log.info("Роль {} удалена администратором {}", role.getName(), authContext.getUserId());

        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * <h3>Назначение роли пользователю.</h3>
     * <p>
     * <b>Доступно только администратору.</b>
     * </p>
     *
     * @param userId       идентификатор пользователя
     * @param roleId       идентификатор роли
     * @param authContext  контекст аутентификации
     * @throws EntityNotFoundException если пользователь или роль не найдены
     * @throws org.example.exception.security_exception.AccessDeniedException если текущий пользователь не администратор
     */
    public void assignRoleToUser(Long userId, Long roleId, AuthContext authContext) {
        SecurityUtil.checkAdmin(authContext);

        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            User user = userDao.findById(em, userId)
                    .orElseThrow(() -> new EntityNotFoundException("Пользователь с id " + userId + " не найден"));

            Role role = roleDao.findById(em, roleId)
                    .orElseThrow(() -> new EntityNotFoundException("Роль с id " + roleId + " не найдена"));

            boolean added = user.getRoles().add(role);
            if (added) {
                userDao.update(em, user);
                log.info("Роль {} назначена пользователю {} администратором {}",
                        role.getName(), userId, authContext.getUserId());
            } else {
                log.info("Роль {} уже назначена пользователю {}", role.getName(), userId);
            }

            tx.commit();

        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * <h3>Назначение нескольких ролей пользователю.</h3>
     * <p>
     * <b>Доступно только администратору.</b>
     * </p>
     *
     * @param userId       идентификатор пользователя
     * @param roleIds      набор идентификаторов ролей
     * @param authContext  контекст аутентификации
     * @throws EntityNotFoundException если пользователь или какая-либо роль не найдены
     * @throws org.example.exception.security_exception.AccessDeniedException если текущий пользователь не администратор
     */
    public void assignRolesToUser(Long userId, Set<Long> roleIds, AuthContext authContext) {
        SecurityUtil.checkAdmin(authContext);

        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            User user = userDao.findById(em, userId)
                    .orElseThrow(() -> new EntityNotFoundException("Пользователь с id " + userId + " не найден"));

            Set<Role> roles = roleIds.stream()
                    .map(roleId -> roleDao.findById(em, roleId)
                            .orElseThrow(() -> new EntityNotFoundException("Роль с id " + roleId + " не найдена")))
                    .collect(Collectors.toSet());

            roles.removeAll(user.getRoles());
            if (!roles.isEmpty()) {
                user.getRoles().addAll(roles);
                userDao.update(em, user);
                log.info("Роли {} назначены пользователю {} администратором {}",
                        roles.stream().map(Role::getName).collect(Collectors.toSet()),
                        userId, authContext.getUserId());
            }

            tx.commit();

        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * <h3>Удаление роли у пользователя.</h3>
     * <p>
     * <b>Доступно только администратору.</b>
     * </p>
     *
     * @param userId       идентификатор пользователя
     * @param roleId       идентификатор роли
     * @param authContext  контекст аутентификации
     * @throws EntityNotFoundException если пользователь или роль не найдены
     * @throws org.example.exception.security_exception.AccessDeniedException если текущий пользователь не администратор
     */
    public void removeRoleFromUser(Long userId, Long roleId, AuthContext authContext) {
        SecurityUtil.checkAdmin(authContext);

        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            User user = userDao.findById(em, userId)
                    .orElseThrow(() -> new EntityNotFoundException("Пользователь с id " + userId + " не найден"));

            Role role = roleDao.findById(em, roleId)
                    .orElseThrow(() -> new EntityNotFoundException("Роль с id " + roleId + " не найдена"));

            boolean removed = user.getRoles().remove(role);
            if (removed) {
                userDao.update(em, user);
                log.info("Роль {} удалена у пользователя {} администратором {}",
                        role.getName(), userId, authContext.getUserId());
            }

            tx.commit();

        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * <h3>Удаление всех ролей у пользователя.</h3>
     * <p>
     * <b>Доступно только администратору.</b>
     * </p>
     *
     * @param userId       идентификатор пользователя
     * @param authContext  контекст аутентификации
     * @throws EntityNotFoundException если пользователь не найден
     * @throws org.example.exception.security_exception.AccessDeniedException если текущий пользователь не администратор
     */
    public void removeAllRolesFromUser(Long userId, AuthContext authContext) {
        SecurityUtil.checkAdmin(authContext);

        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            User user = userDao.findById(em, userId)
                    .orElseThrow(() -> new EntityNotFoundException("Пользователь с id " + userId + " не найден"));

            if (!user.getRoles().isEmpty()) {
                user.getRoles().clear();
                userDao.update(em, user);
                log.info("Все роли удалены у пользователя {} администратором {}",
                        userId, authContext.getUserId());
            }

            tx.commit();

        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * <h3>Получение списка пользователей с определенной ролью.</h3>
     * <p>
     * <b>Доступно только администратору.</b>
     * </p>
     *
     * @param roleId       идентификатор роли
     * @param authContext  контекст аутентификации
     * @return список DTO пользователей, имеющих указанную роль
     * @throws EntityNotFoundException если роль не найдена
     * @throws org.example.exception.security_exception.AccessDeniedException если текущий пользователь не администратор
     */
    public List<UserReadDto> getUsersWithRole(Long roleId, AuthContext authContext) {
        SecurityUtil.checkAdmin(authContext);

        EntityManager em = entityManagerFactory.createEntityManager();

        try {
            roleDao.findById(em, roleId)
                    .orElseThrow(() -> new EntityNotFoundException("Роль с id " + roleId + " не найдена"));

            return roleDao.findUsersByRoleId(em, roleId).stream()
                    .map(userReadMapper::map)
                    .toList();
        } finally {
            em.close();
        }
    }

    /**
     * <h3>Получение всех ролей пользователя.</h3>
     * <p>
     * <b>Доступно только администратору.</b>
     * </p>
     *
     * @param userId       идентификатор пользователя
     * @param authContext  контекст аутентификации
     * @return набор DTO ролей пользователя
     * @throws EntityNotFoundException если пользователь не найден
     * @throws org.example.exception.security_exception.AccessDeniedException если текущий пользователь не администратор
     */
    public Set<RoleReadDto> getUserRoles(Long userId, AuthContext authContext) {
        SecurityUtil.checkAdmin(authContext);

        EntityManager em = entityManagerFactory.createEntityManager();

        try {
            User user = userDao.findById(em, userId)
                    .orElseThrow(() -> new EntityNotFoundException("Пользователь с id " + userId + " не найден"));

            return user.getRoles().stream()
                    .map(this::mapToReadDto)
                    .collect(Collectors.toSet());
        } finally {
            em.close();
        }
    }

    public boolean hasRole(Long userId, String roleName, AuthContext authContext) {
        SecurityUtil.checkAdmin(authContext);

        EntityManager em = entityManagerFactory.createEntityManager();

        try {
            User user = userDao.findById(em, userId)
                    .orElseThrow(() -> new EntityNotFoundException("Пользователь с id " + userId + " не найден"));

            return user.getRoles().stream()
                    .anyMatch(role -> role.getName().equals(roleName.toUpperCase()));
        } finally {
            em.close();
        }
    }

    public boolean hasAnyRole(Long userId, Set<String> roleNames, AuthContext authContext) {
        SecurityUtil.checkAdmin(authContext);

        EntityManager em = entityManagerFactory.createEntityManager();

        try {
            User user = userDao.findById(em, userId)
                    .orElseThrow(() -> new EntityNotFoundException("Пользователь с id " + userId + " не найден"));

            Set<String> upperCaseRoleNames = roleNames.stream()
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());

            return user.getRoles().stream()
                    .map(Role::getName)
                    .anyMatch(upperCaseRoleNames::contains);
        } finally {
            em.close();
        }
    }

    public boolean hasAllRoles(Long userId, Set<String> roleNames, AuthContext authContext) {
        SecurityUtil.checkAdmin(authContext);

        EntityManager em = entityManagerFactory.createEntityManager();

        try {
            User user = userDao.findById(em, userId)
                    .orElseThrow(() -> new EntityNotFoundException("Пользователь с id " + userId + " не найден"));

            Set<String> upperCaseRoleNames = roleNames.stream()
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());

            Set<String> userRoleNames = user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toSet());

            return userRoleNames.containsAll(upperCaseRoleNames);
        } finally {
            em.close();
        }
    }

    private RoleReadDto mapToReadDto(Role role) {
        return new RoleReadDto(role.getName());
    }
}