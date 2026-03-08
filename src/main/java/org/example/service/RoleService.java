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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class RoleService {

    private final RoleDao roleDao;
    private final UserDao userDao;
    private final UserReadMapper userReadMapper;
    private final EntityManagerFactory entityManagerFactory;


    public void createRole(String roleName) {
        if (roleName != null && !roleName.isBlank()) {
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
                log.info("Создана новая роль {}", roleName.toUpperCase());

            } catch (Exception e) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                throw e;
            } finally {
                em.close();
            }
        } else {
            throw new IllegalArgumentException("Нельзя создать роль без имени");
        }
    }

    public RoleReadDto findById(Long roleId) {
        EntityManager em = entityManagerFactory.createEntityManager();

        try {
            Role role = roleDao.findById(em, roleId)
                    .orElseThrow(() -> new EntityNotFoundException("Роль с id " + roleId + " не найдена"));
            return new RoleReadDto(role.getName());
        } finally {
            em.close();
        }
    }

    public RoleReadDto findByName(String name) {
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

    public List<RoleReadDto> findAll() {
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

    public RoleReadDto updateRole(Long roleId, String newName) {
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
            log.info("Обновлена роль {} с новым именем {}", roleId, normalizedName);
            return mapToReadDto(role);

        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public void deleteRole(Long roleId) {
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

        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public void assignRoleToUser(Long userId, Long roleId) {
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
                log.info("Роль {} назначена пользователю {}", role.getName(), userId);
            } else {
                log.info("Роль {} уже назначена пользователю {}", role.getName(), userId);
            }

            tx.commit();

        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public void assignRolesToUser(Long userId, Set<Long> roleIds) {
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
                log.info("Роли {} назначены пользователю {}",
                        roles.stream().map(Role::getName).collect(Collectors.toSet()), userId);
            }

            tx.commit();

        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public void removeRoleFromUser(Long userId, Long roleId) {
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
                log.info("Роль {} удалена у пользователя {}", role.getName(), userId);
            }

            tx.commit();

        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public void removeAllRolesFromUser(Long userId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            User user = userDao.findById(em, userId)
                    .orElseThrow(() -> new EntityNotFoundException("Пользователь с id " + userId + " не найден"));

            if (!user.getRoles().isEmpty()) {
                user.getRoles().clear();
                userDao.update(em, user);
                log.info("Все роли удалены у пользователя {}", userId);
            }

            tx.commit();

        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public Set<RoleReadDto> getUserRoles(Long userId) {
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

    public boolean hasRole(Long userId, String roleName) {
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

    public boolean hasAnyRole(Long userId, Set<String> roleNames) {
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

    public boolean hasAllRoles(Long userId, Set<String> roleNames) {
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

    public List<UserReadDto> getUsersWithRole(Long roleId) {
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

    private RoleReadDto mapToReadDto(Role role) {
        return new RoleReadDto(role.getName());
    }
}
