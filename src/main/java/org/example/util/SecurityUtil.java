package org.example.util;

import org.example.entity.Role;
import org.example.security.AuthContext;

import java.nio.file.AccessDeniedException;
import java.util.Arrays;

public class SecurityUtil {

    public static void checkOwnerOrAdmin(AuthContext authContext, Long resourceOwnerId) {
        if (!authContext.isAdmin() && !authContext.getUserId().equals(resourceOwnerId)) {
            throw new AccessDeniedException(
                    String.format("Пользователь %d не имеет доступа к ресурсам пользователя %d",
                            authContext.getUserId(), resourceOwnerId)
            );
        }
    }

    public static void checkOwner(AuthContext authContext, Long resourceOwnerId) {
        if (!authContext.getUserId().equals(resourceOwnerId)) {
            throw new AccessDeniedException("Вы имеете доступ только к своим ресурсам");
        }
    }

    public static void checkAdmin(AuthContext authContext) {
        if (!authContext.isAdmin()) {
            throw new AccessDeniedException("Необходим доступ администратора");
        }
    }

    public static void checkAuthenticated(AuthContext authContext) {
       if(authContext == null) throw new AccessDeniedException("Пользователь не аутентифицирован");
    }

    // Проверка: любая из ролей
    public static void hasAnyRole(AuthContext authContext, Role... allowedRoles) {
        for (Role role : allowedRoles) {
            if (authContext.getRoles().contains(role.getName())) {
                return;
            }
        }
        throw new AccessDeniedException("Required roles: " + Arrays.toString(allowedRoles));
    }
}
