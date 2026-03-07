package org.example.dto.user;

import org.example.dto.role.RoleReadDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record UserReadDto(
        String firstName,
        String lastName,
        String email,
        LocalDateTime createdAt,
        List<RoleReadDto> roles
) { }
