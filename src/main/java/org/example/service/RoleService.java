package org.example.service;

import org.example.dto.user.UserReadDto;

import java.util.List;
import java.util.Set;

public class RoleService {
    public RoleReadDto createRole(RoleCreateDto createDto) {
        return null;
    }

    public RoleReadDto findById(Long roleId) {
        return null;
    }

    public RoleReadDto findByName(String name) {
        return null;
    }

    public List<RoleReadDto> findAll() {
        return null;
    }

    public RoleReadDto updateRole(Long roleId, RoleUpdateDto updateDto) {
        return null;
    }

    public void deleteRole(Long roleId) {

    }

    public void assignRoleToUser(Long userId, Long roleId) {

    }

    public void assignRolesToUser(Long userId, Set<Long> roleIds) {

    }

    public void removeRoleFromUser(Long userId, Long roleId) {

    }

    public void removeAllRolesFromUser(Long userId) {

    }

    public Set<RoleReadDto> getUserRoles(Long userId) {
        return null;
    }

    public boolean hasRole(Long userId, String roleName) {
        return false;
    }

    public boolean hasAnyRole(Long userId, Set<String> roleNames) {
        return false;
    }

    public boolean hasAllRoles(Long userId, Set<String> roleNames) {
        return false;
    }

    public List<UserReadDto> getUsersWithRole(Long roleId) {
        return null;
    }

    public long countUsersInRole(Long roleId) {
        return 0;
    }

    public boolean existsByName(String name) {
        return false;
    }
}
