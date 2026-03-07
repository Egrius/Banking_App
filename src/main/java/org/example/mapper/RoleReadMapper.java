package org.example.mapper;

import org.example.dto.role.RoleReadDto;
import org.example.entity.Role;

public class RoleReadMapper implements BaseMapper<Role, RoleReadDto> {
    @Override
    public RoleReadDto map(Role object) {
        return new RoleReadDto(object.getName());
    }
}
