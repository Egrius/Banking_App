package org.example.server.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.Request;
import org.example.dto.Response;
import org.example.dto.role.*;
import org.example.dto.user.UserReadDto;
import org.example.security.AuthContext;
import org.example.service.RoleService;
import org.example.util.JsonUtil;
import org.example.util.SecurityUtil;
import org.example.util.ValidatorUtil;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class RoleController implements BaseController {

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

        String action = parts[1];

        return switch (action) {
            case "createRole" -> handleCreateRole(request);
            case "findByIdRole" -> handleFindById(request, parts);
            case "findByNameRole" -> handleFindByName(request, parts);
            case "findAllRoles" -> handleFindAll(request);
            case "updateRole" -> handleUpdateRole(request);
            case "deleteRole" -> handleDeleteRole(request, parts);
            case "assignRole" -> handleAssignRole(request);
            case "assignManyRoles" -> handleAssignRoles(request);
            case "removeRole" -> handleRemoveRole(request);
            case "removeAllRoles" -> handleRemoveAllRoles(request);
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

    // TODO: доработать этот момент
    // role.findByName.?rolename=jkfjaljfl?something=kdjajda
    private Response handleFindByName(Request request, String[] parts) {

        String roleName = "";
        if(request.getHeaders().containsKey("rolename")) {
            roleName = request.getHeaders().get("rolename");
        } else if (parts.length == 3) {
           String[] parametersAndValues = parts[2].split("\\?");
           String param = parametersAndValues[0].split("\\=")[0];
           String value = parametersAndValues[0].split("\\=")[1];

           if(param.toLowerCase().equals("rolename")) {
               roleName = value;
           } else {
               throw new IllegalArgumentException("Не передан параметр имени роли в запрос для поиска роли. Правильный синтаксис: .?rolename=value либо передайте имя роли в заголовок rolename");
           }

        } else {
            throw new IllegalArgumentException("Не передан параметр имени роли в запрос для поиска роли. Правильный синтаксис: .?param1=value?param2=value либо передайте имя роли в заголовок rolename");
        }


        RoleReadDto role = roleService.findByName(roleName);
        return Response.success(role);
    }

    // role.findAll
    private Response handleFindAll(Request request) {
        return Response.success(roleService.findAll());
    }

    // role.update
    private Response handleUpdateRole(Request request) {

        RoleUpdateDto dto = (RoleUpdateDto) request.getPayload();

        ValidatorUtil.validate(dto);

        RoleReadDto updatedRole = roleService.updateRole(dto.roleId(), dto.newRoleName());
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
       AssignRoleDto dto = (AssignRoleDto) request.getPayload();

       ValidatorUtil.validate(dto);

        roleService.assignRoleToUser(dto.userId(), dto.roleId());
        return Response.success("Role assigned successfully");
    }

    // role.assignMany
    private Response handleAssignRoles(Request request) {
        AssignManyRolesDto dto = (AssignManyRolesDto) request.getPayload();

        ValidatorUtil.validate(dto);

        roleService.assignRolesToUser(dto.userId(), dto.roleIds());
        return Response.success("Roles assigned successfully");
    }

    // role.remove
    private Response handleRemoveRole(Request request) {
        RemoveRoleDto dto = (RemoveRoleDto) request.getPayload();

        roleService.removeRoleFromUser(dto.userId(), dto.roleId());
        return Response.success("Role removed successfully");
    }

    // role.removeAll
    private Response handleRemoveAllRoles(Request request) {
        RemoveAllRolesDto dto = (RemoveAllRolesDto) request.getPayload();

        roleService.removeAllRolesFromUser(dto.userId());
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
        HasRoleDto dto = (HasRoleDto) request.getPayload();

        ValidatorUtil.validate(dto);

        boolean hasRole = roleService.hasRole(dto.userId(), dto.roleName());
        return Response.success(Map.of("hasRole", hasRole));
    }

    // role.hasAnyRole
    private Response handleHasAnyRole(Request request) {
        HasAnyRoleDto dto = (HasAnyRoleDto) request.getPayload();

        ValidatorUtil.validate(dto);

        boolean hasAnyRole = roleService.hasAnyRole(dto.userId(), dto.roleNames());
        return Response.success(Map.of("hasAnyRole", hasAnyRole));
    }

    // role.hasAllRoles
    private Response handleHasAllRoles(Request request) {
        HasAllRolesDto dto = (HasAllRolesDto) request.getPayload();

        ValidatorUtil.validate(dto);

        boolean hasAllRoles = roleService.hasAllRoles(dto.userId(), dto.roleNames());
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