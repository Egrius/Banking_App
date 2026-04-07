package org.example.mapper;

import lombok.RequiredArgsConstructor;
import org.example.dto.user.UserReadDto;
import org.example.entity.User;

import java.time.temporal.ChronoUnit;

@RequiredArgsConstructor
public class UserReadMapper implements BaseMapper<User, UserReadDto> {

    private final RoleReadMapper roleReadMapper;

    @Override
    public UserReadDto map(User object) {
        return new UserReadDto(
                object.getFirstName(),
                object.getLastName(),
                object.getEmail(),
                object.getCreatedAt(),
                object.getRoles().stream().map(roleReadMapper::map).toList()
        );
    }
}
