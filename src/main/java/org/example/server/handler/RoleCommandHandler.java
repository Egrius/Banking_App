package org.example.server.handler;

import lombok.RequiredArgsConstructor;
import org.example.dto.Request;
import org.example.dto.Response;
import org.example.dto.role.RoleReadDto;
import org.example.dto.user.UserReadDto;
import org.example.security.AuthContext;
import org.example.service.RoleService;
import org.example.util.JsonUtil;
import org.example.util.SecurityUtil;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class RoleCommandHandler implements BaseRequestHandler {

    private final RoleService roleService;

    @Override
    public Response handle(Request request) {
        // Проверка админа для всех операций с ролями
        AuthContext authContext = JsonUtil.convert(request.getAuthContext(), AuthContext.class);
        SecurityUtil.checkAdmin(authContext);

        String[] parts = request.getCommand().split("\\.");

        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid command: " + request.getCommand());
        }

        String action = parts[2];

        return switch (action) {
            case "create" -> handleCreateRole(request);
            case "findById" -> handleFindById(request, parts);
            case "findByName" -> handleFindByName(request);
            case "findAll" -> handleFindAll(request);
            case "update" -> handleUpdateRole(request, parts);
            case "delete" -> handleDeleteRole(request, parts);
            case "assign" -> handleAssignRole(request);
            case "assignMany" -> handleAssignRoles(request);
            case "remove" -> handleRemoveRole(request);
            case "removeAll" -> handleRemoveAllRoles(request);
            case "getUsersWithRole" -> handleGetUsersWithRole(request, parts);
            case "getUserRoles" -> handleGetUserRoles(request, parts);
            case "hasRole" -> handleHasRole(request);
            case "hasAnyRole" -> handleHasAnyRole(request);
            case "hasAllRoles" -> handleHasAllRoles(request);
            default -> Response.error("Unknown role action: " + action, 404);
        };
    }

    // role.create
    private Response handleCreateRole(Request request) {
        Map<String, Object> payload = JsonUtil.convert(request.getPayload(), Map.class);
        String roleName = (String) payload.get("roleName");

        roleService.createRole(roleName);
        return Response.success("Role created successfully");
    }

    // role.findById.1
    private Response handleFindById(Request request, String[] parts) {
        Long roleId = extractId(parts, 3);
        RoleReadDto role = roleService.findById(roleId);
        return Response.success(role);
    }

    // role.findByName
    private Response handleFindByName(Request request) {
        Map<String, Object> payload = JsonUtil.convert(request.getPayload(), Map.class);
        String roleName = (String) payload.get("roleName");

        RoleReadDto role = roleService.findByName(roleName);
        return Response.success(role);
    }

    // role.findAll
    private Response handleFindAll(Request request) {
        return Response.success(roleService.findAll());
    }

    // role.update.1
    private Response handleUpdateRole(Request request, String[] parts) {
        Long roleId = extractId(parts, 3);
        Map<String, Object> payload = JsonUtil.convert(request.getPayload(), Map.class);
        String newName = (String) payload.get("newName");

        RoleReadDto updatedRole = roleService.updateRole(roleId, newName);
        return Response.success(updatedRole);
    }

    // role.delete.1
    private Response handleDeleteRole(Request request, String[] parts) {
        Long roleId = extractId(parts, 3);
        roleService.deleteRole(roleId);
        return Response.success("Role deleted successfully");
    }

    // role.assign
    private Response handleAssignRole(Request request) {
        Map<String, Object> payload = JsonUtil.convert(request.getPayload(), Map.class);
        Long userId = ((Number) payload.get("userId")).longValue();
        Long roleId = ((Number) payload.get("roleId")).longValue();

        roleService.assignRoleToUser(userId, roleId);
        return Response.success("Role assigned successfully");
    }

    // role.assignMany
    private Response handleAssignRoles(Request request) {
        Map<String, Object> payload = JsonUtil.convert(request.getPayload(), Map.class);
        Long userId = ((Number) payload.get("userId")).longValue();
        Set<Long> roleIds = (Set<Long>) payload.get("roleIds");

        roleService.assignRolesToUser(userId, roleIds);
        return Response.success("Roles assigned successfully");
    }

    // role.remove
    private Response handleRemoveRole(Request request) {
        Map<String, Object> payload = JsonUtil.convert(request.getPayload(), Map.class);
        Long userId = ((Number) payload.get("userId")).longValue();
        Long roleId = ((Number) payload.get("roleId")).longValue();

        roleService.removeRoleFromUser(userId, roleId);
        return Response.success("Role removed successfully");
    }

    // role.removeAll
    private Response handleRemoveAllRoles(Request request) {
        Map<String, Object> payload = JsonUtil.convert(request.getPayload(), Map.class);
        Long userId = ((Number) payload.get("userId")).longValue();

        roleService.removeAllRolesFromUser(userId);
        return Response.success("All roles removed successfully");
    }

    // role.getUsersWithRole.1
    private Response handleGetUsersWithRole(Request request, String[] parts) {
        Long roleId = extractId(parts, 3);
        List<UserReadDto> users = roleService.getUsersWithRole(roleId);
        return Response.success(users);
    }

    // role.getUserRoles.1
    private Response handleGetUserRoles(Request request, String[] parts) {
        Long userId = extractId(parts, 3);
        Set<RoleReadDto> roles = roleService.getUserRoles(userId);
        return Response.success(roles);
    }

    // role.hasRole
    private Response handleHasRole(Request request) {
        Map<String, Object> payload = JsonUtil.convert(request.getPayload(), Map.class);
        Long userId = ((Number) payload.get("userId")).longValue();
        String roleName = (String) payload.get("roleName");

        boolean hasRole = roleService.hasRole(userId, roleName);
        return Response.success(Map.of("hasRole", hasRole));
    }

    // role.hasAnyRole
    private Response handleHasAnyRole(Request request) {
        Map<String, Object> payload = JsonUtil.convert(request.getPayload(), Map.class);
        Long userId = ((Number) payload.get("userId")).longValue();
        Set<String> roleNames = (Set<String>) payload.get("roleNames");

        boolean hasAnyRole = roleService.hasAnyRole(userId, roleNames);
        return Response.success(Map.of("hasAnyRole", hasAnyRole));
    }

    // role.hasAllRoles
    private Response handleHasAllRoles(Request request) {
        Map<String, Object> payload = JsonUtil.convert(request.getPayload(), Map.class);
        Long userId = ((Number) payload.get("userId")).longValue();
        Set<String> roleNames = (Set<String>) payload.get("roleNames");

        boolean hasAllRoles = roleService.hasAllRoles(userId, roleNames);
        return Response.success(Map.of("hasAllRoles", hasAllRoles));
    }

    private Long extractId(String[] parts, int index) {
        if (parts.length <= index) {
            throw new IllegalArgumentException("Missing ID parameter");
        }
        try {
            return Long.parseLong(parts[index]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid ID format: " + parts[index]);
        }
    }
}