package org.example.security;

import org.example.entity.Role;

import java.util.Collections;
import java.util.List;

public class AuthContext {
    private final Long userId;
    private final String email;
    private final List<String> roles;

    public AuthContext(Long userId, String email, List<String> roles) {
        this.userId = userId;
        this.email = email;
        this.roles = Collections.unmodifiableList(roles);
    }

    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public List<String> getRoles() { return roles; }

    public boolean isAdmin() {
        return roles.contains("ADMIN");
    }

    public boolean canManageUser(Long targetUserId) {
        return isAdmin() || this.userId.equals(targetUserId);
    }
}