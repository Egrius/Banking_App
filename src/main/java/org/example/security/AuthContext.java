package org.example.security;

import org.example.entity.Role;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthContext that)) return false;
        return Objects.equals(getUserId(), that.getUserId()) && Objects.equals(getEmail(), that.getEmail()) && Objects.equals(getRoles(), that.getRoles());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUserId(), getEmail(), getRoles());
    }
}