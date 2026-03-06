package org.example.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dao.RoleDao;
import org.example.dao.UserDao;
import org.example.dto.role.RoleReadDto;
import org.example.dto.user.UserReadDto;
import org.example.entity.Role;
import org.example.entity.User;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class RoleService {

    private final RoleDao roleDao;
    private final UserDao userDao;

    public void createRole(String roleName) {
        if(roleName != null && !roleName.isBlank()) {
            if(roleName.length() < 3) {
                throw new IllegalArgumentException("Роль должна содержать больше 3-х символов");
            }

            // Проверяем, существует ли уже такая роль
            if(roleDao.findByName(roleName.toUpperCase()).isPresent()) {
                throw new IllegalArgumentException("Роль с именем " + roleName.toUpperCase() + " уже существует");
            }

            Role role = new Role(roleName.toUpperCase());
            roleDao.save(role);

            log.info("Создана новая роль {}", roleName.toUpperCase());

        } else {
            throw new IllegalArgumentException("Нельзя создать роль без имени");
        }
    }

    public RoleReadDto findById(Long roleId) {
        Role role = roleDao.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Роль с id " + roleId + " не найдена"));
        return new RoleReadDto(role.getName());
    }

    public RoleReadDto findByName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя роли не может быть пустым");
        }

        Role role = roleDao.findByName(name.toUpperCase())
                .orElseThrow(() -> new EntityNotFoundException("Роль с именем " + name.toUpperCase() + " не найдена"));
        return new RoleReadDto(role.getName());
    }

    public List<RoleReadDto> findAll() {
        return roleDao.findAll()
                .stream()
                .map(this::mapToReadDto)
                .toList();

    }

    public RoleReadDto updateRole(Long roleId, String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Имя роли не может быть пустым");
        }

        if (newName.length() < 3) {
            throw new IllegalArgumentException("Роль должна содержать больше 3-х символов");
        }

        Role role = roleDao.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Роль с id " + roleId + " не найдена"));

        roleDao.findByName(newName.toUpperCase())
                .ifPresent(existingRole -> {
                    if (!existingRole.getId().equals(roleId)) {
                        throw new IllegalArgumentException("Роль с именем " + newName.toUpperCase() + " уже существует");
                    }
                });

        role.setName(newName.toUpperCase());
        roleDao.update(role);

        log.info("Обновлена роль {} с новым именем {}", roleId, newName.toUpperCase());
        return mapToReadDto(role);
    }

    public void deleteRole(Long roleId) {

    }

    public void assignRoleToUser(Long userId, Long roleId) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с id " + userId + " не найден"));

        Role role = roleDao.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Роль с id " + roleId + " не найдена"));

        if(user.getRoles().add(role)) {
            userDao.update(user);
            log.info("Роль {} назначена пользователю {}", role.getName(), userId);
        } else {
            log.info("Не получилось установить роль {} пользователю {}", role.getName(), userId);
        }

    }

    public void assignRolesToUser(Long userId, Set<Long> roleIds) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с id " + userId + " не найден"));

        Set<Role> roles = roleIds.stream()
                .map(roleId -> roleDao.findById(roleId)
                        .orElseThrow(() -> new EntityNotFoundException("Роль с id " + roleId + " не найдена")))
                .collect(Collectors.toSet());

        user.getRoles().addAll(roles);
        userDao.update(user);
        log.info("Роли {} назначены пользователю {}", roleIds, userId);
    }

    public void removeRoleFromUser(Long userId, Long roleId) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с id " + userId + " не найден"));

        Role role = roleDao.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Роль с id " + roleId + " не найдена"));

        if (user.getRoles().remove(role)) {
            userDao.update(user);
            log.info("Роль {} удалена у пользователя {}", role.getName(), userId);
        }
    }

    public void removeAllRolesFromUser(Long userId) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с id " + userId + " не найден"));

        if (!user.getRoles().isEmpty()) {
            user.getRoles().clear();
            userDao.update(user);
            log.info("Все роли удалены у пользователя {}", userId);
        }
    }

    public Set<RoleReadDto> getUserRoles(Long userId) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с id " + userId + " не найден"));

        return user.getRoles().stream()
                .map(this::mapToReadDto)
                .collect(Collectors.toSet());
    }

    public boolean hasRole(Long userId, String roleName) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с id " + userId + " не найден"));

        return user.getRoles().stream()
                .anyMatch(role -> role.getName().equals(roleName.toUpperCase()));
    }

    public boolean hasAnyRole(Long userId, Set<String> roleNames) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с id " + userId + " не найден"));

        Set<String> upperCaseRoleNames = user.getRoles().stream()
                .map(Role::getName)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        return upperCaseRoleNames.stream()
                .anyMatch(upperCaseRoleNames::contains);
    }

    public boolean hasAllRoles(Long userId, Set<String> roleNames) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с id " + userId + " не найден"));

        Set<String> upperCaseRoleNames = roleNames.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        Set<String> userRoleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return userRoleNames.containsAll(upperCaseRoleNames);
    }

    public List<UserReadDto> getUsersWithRole(Long roleId) {
        Role role = roleDao.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Роль с id " + roleId + " не найдена"));

        return roleDao.findUsersByRoleId(roleId).stream()
                .map(userReadMapper::map)
                .toList();
    }

    public long countUsersInRole(Long roleId) {
        return 0;
    }

    public boolean existsByName(String name) {
        return false;
    }

    private RoleReadDto mapToReadDto(Role role) {
        return new RoleReadDto(role.getName());
    }
}
