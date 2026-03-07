package org.example.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dao.UserDao;
import org.example.dto.user.*;
import org.example.entity.User;
import org.example.mapper.UserReadMapper;
import org.example.mapper.UserUpdateMapper;
import org.example.util.PasswordUtil;
import org.example.util.ValidatorUtil;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserDao userDao;
    private final UserReadMapper userReadMapper;
    private final UserUpdateMapper userUpdateMapper;

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
        ValidatorUtil.validate(loginDto);

        User foundUser = userDao.findByEmail(loginDto.email()).orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));
        if(!PasswordUtil.verify(loginDto.rawPassword(), foundUser.getPasswordHash())) {
            throw new IllegalArgumentException("Пароль неверен!");
        }
        return userReadMapper.map(foundUser);
    }

    public UserReadDto findById(Long userId) {
        return userReadMapper.map(userDao.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с id:" + userId + "не найден")));
    }

    public List<UserReadDto> findAll() {
        return userDao.findAll()
                .stream()
                .map(userReadMapper::map)
                .toList();
    }

    public UserReadDto updateUser(Long userId, UserUpdateDto updateDto) {
        User userToUpdate = userDao.findById(userId).orElseThrow(
                () -> new EntityNotFoundException("Не найден пользователь для обновления с id: " + userId));


        User updatedUser = userUpdateMapper.map(updateDto, userToUpdate);
        userDao.update(updatedUser);
        return userReadMapper.map(updatedUser);

    }

    public void changePassword(Long userId, PasswordChangeDto passwordChangeDto) {

        ValidatorUtil.validate(passwordChangeDto);

        User userToUpdate = userDao.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));

        if(PasswordUtil.verify(passwordChangeDto.oldPassword(), userToUpdate.getPasswordHash())) {
            userToUpdate.setPasswordHash(PasswordUtil.hash(passwordChangeDto.newPassword()));
            userDao.update(userToUpdate);
        } else {
            throw new IllegalArgumentException("Передан неправильный пароль");
        }
    }

    public void deleteUser(Long userId, String password) {
        User userToDelete = userDao.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Не найден пользователь для удаления с id: " + userId));

        if(!PasswordUtil.verify(password, userToDelete.getPasswordHash())) {
            log.warn("Попытка удаления пользователя {} с неверным паролем", userId);
            throw new IllegalArgumentException("Неверный пароль");
        }

        userDao.delete(userToDelete);
        log.info("Пользователь c id {} успешно удален", userId);
    }

    public List<UserReadDto> findUsersWithAccounts() {
        return null;
    }

}
