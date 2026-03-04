package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dao.UserDao;
import org.example.dto.user.UserCreateDto;
import org.example.dto.user.UserReadDto;
import org.example.entity.User;
import org.example.mapper.UserReadMapper;
import org.example.util.PasswordUtil;
import org.example.util.ValidatorUtil;
import java.util.List;

@RequiredArgsConstructor
public class UserService {

    private final UserDao userDao;
    private final UserReadMapper userReadMapper;

    public UserReadDto register(UserCreateDto createDto) {

        ValidatorUtil.validate(createDto);

        User user = User.builder()
                .firstName(createDto.firstName())
                .lastName(createDto.lastName())
                .email(createDto.email())
                .passwordHash(PasswordUtil.hash(createDto.rawPassword()))
                .build();

        userDao.save(user);

        return userReadMapper.map(user);
    }

    public UserReadDto login(UserLoginDto loginDto) {
        return null;
    }

    public UserReadDto findById(Long userId) {
        return null;
    }

    public List<UserReadDto> findAll() {
        return null;
    }

    public UserReadDto findByUsername(String username) {
        return null;
    }

    public UserReadDto updateUser(Long userId, UserUpdateDto updateDto) {
        return null;
    }

    public void deleteUser(Long userId, String password) {

    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {

    }

    public boolean existsByUsername(String username) {
        return false;
    }

    public boolean existsByEmail(String email) {
        return false;
    }

    public long countUsers() {
        return 0;
    }

    public List<UserReadDto> findUsersWithAccounts() {
        return null;
    }

}
