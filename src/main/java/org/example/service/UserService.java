package org.example.service;

import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dao.UserDao;
import org.example.dto.request.PageRequest;
import org.example.dto.response.PageResponse;
import org.example.dto.user.*;
import org.example.entity.User;
import org.example.exception.security_exception.AccessDeniedException;
import org.example.exception.user.UserAlreadyExistsException;
import org.example.mapper.UserReadMapper;
import org.example.mapper.UserUpdateMapper;
import org.example.security.AuthContext;
import org.example.util.PasswordUtil;
import org.example.util.SecurityUtil;
import org.example.util.ValidatorUtil;

import java.util.ConcurrentModificationException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserDao userDao;
    private final UserReadMapper userReadMapper;
    private final UserUpdateMapper userUpdateMapper;
    private final EntityManagerFactory emf;

    public UserReadDto register(UserCreateDto createDto) {

        ValidatorUtil.validate(createDto);

        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try{

            tx.begin();

            if (userDao.findByEmail(em, createDto.email()).isPresent()) {
                tx.rollback();
                throw new UserAlreadyExistsException("Пользователь с email " + createDto.email() + " уже существует");
            }

            User user = User.builder()
                    .firstName(createDto.firstName())
                    .lastName(createDto.lastName())
                    .email(createDto.email())
                    .passwordHash(PasswordUtil.hash(createDto.rawPassword()))
                    .build();

            userDao.save(em, user);
            tx.commit();

            log.info("Пользователь {} был сохранён в БД", user.getEmail());

            return userReadMapper.map(user);

        } catch (Exception e) {
            if(tx.isActive()) tx.rollback();
            throw e;

        } finally {
            em.close();
        }
    }

    public UserReadDto login(UserLoginDto loginDto) {

        ValidatorUtil.validate(loginDto);

        EntityManager em = emf.createEntityManager();

        try {

            User foundUser = userDao.findByEmail(em, loginDto.email()).orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));

            if(!PasswordUtil.verify(loginDto.rawPassword(), foundUser.getPasswordHash())) {
                throw new AccessDeniedException("Пароль неверен!");
            }
            return userReadMapper.map(foundUser);
        } finally {
            em.close();
        }
    }

    public UserReadDto findById(Long userId) {
        EntityManager em = emf.createEntityManager();

        try {
            User user = userDao.findById(em, userId)
                    .orElseThrow(() -> new EntityNotFoundException("Пользователь с id: " + userId + " не найден"));

            return userReadMapper.map(user);

        } finally {
            em.close();
        }
    }

    public PageResponse<UserReadDto> findAll(AuthContext authContext, PageRequest pageRequest) {

        SecurityUtil.checkAdmin(authContext); // бросает unchecked если не админ

        if(pageRequest == null) throw new IllegalStateException("Нельзя выполнить пустой запрос!");

        EntityManager em = emf.createEntityManager();

        try {

            PageResponse<User> pageResponse = userDao.findAllPageable(em, pageRequest);

            return new PageResponse<UserReadDto>(
                    pageResponse.getContent().stream().map(userReadMapper::map).toList(),
                    pageResponse.getPageNumber(),
                    pageResponse.getPageSize(),
                    pageResponse.getTotalElements()
            );
        } finally {
            em.close();
        }
    }

    public UserReadDto updateUser(Long userId, UserUpdateDto updateDto, AuthContext authContext) {

        SecurityUtil.checkAdminOrOwner(authContext, userId); // бросает unchecked если не прошла проверка

        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {

            ValidatorUtil.validate(updateDto);

            log.debug("Валидация пройдена для updateUser");


            tx.begin();

            User userToUpdate = userDao.findById(em, userId).orElseThrow(
                    () -> new EntityNotFoundException("Не найден пользователь для обновления с id: " + userId));

            User updatedUser = userUpdateMapper.map(updateDto, userToUpdate);
            userDao.update(em, updatedUser);
            tx.commit();

            return userReadMapper.map(updatedUser);
        } catch (Exception e) {
            if(tx.isActive()) tx.rollback();
            log.error("Ошибка при обновлении пользователя", e);
            throw e;
        } finally {
            em.close();
        }
    }

    public void changePassword(Long userId, PasswordChangeDto passwordChangeDto, AuthContext authContext) {

        SecurityUtil.checkAdminOrOwner(authContext, userId);

        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            ValidatorUtil.validate(passwordChangeDto);

            tx.begin();

            User userToUpdate = userDao.findById(em, userId)
                    .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));

            if (PasswordUtil.verify(passwordChangeDto.oldPassword(), userToUpdate.getPasswordHash())) {
                userToUpdate.setPasswordHash(PasswordUtil.hash(passwordChangeDto.newPassword()));

                try {
                    userDao.update(em, userToUpdate);
                    tx.commit();
                    log.info("Пароль изменен для пользователя {}", userId);

                } catch (OptimisticLockException e) {
                    log.error("Конфликт версий при обновлении пользователя {}", userId);
                    throw new ConcurrentModificationException(
                            "Пользователь был изменен. Обновите данные и повторите попытку.");
                }
            } else {
                throw new AccessDeniedException("Передан неправильный пароль");
            }

        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public void deleteUser(Long userId, String password, AuthContext authContext) {

        SecurityUtil.checkAdminOrOwner(authContext, userId);

        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            User userToDelete = userDao.findById(em, userId)
                    .orElseThrow(() -> new EntityNotFoundException("Не найден пользователь для удаления с id: " + userId));

            if (!authContext.isAdmin()) {
                if (!PasswordUtil.verify(password, userToDelete.getPasswordHash())) {
                    log.warn("Попытка удаления пользователя {} с неверным паролем", userId);
                    throw new AccessDeniedException("Неверный пароль");
                }
            }

            userDao.delete(em, userToDelete);
            tx.commit();

            log.info("Пользователь c id {} успешно удален", userId);

        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
}