package org.example.mapper;

import org.example.dto.user.UserReadDto;
import org.example.entity.User;

public class UserReadMapper implements BaseMapper<User, UserReadDto> {
    @Override
    public UserReadDto map(User object) { // Накинуть роли в виде дтошек
        return new UserReadDto(
                object.getFirstName(),
                object.getLastName(),
                object.getEmail(),
                object.getCreatedAt()
        );
    }
}
