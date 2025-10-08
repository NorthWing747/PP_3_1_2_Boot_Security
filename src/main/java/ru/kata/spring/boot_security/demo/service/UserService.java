package ru.kata.spring.boot_security.demo.service;

import ru.kata.spring.boot_security.demo.model.User;

import java.util.List;

public interface UserService {

    List<User> getAll();
    User getById(Long id);
    void save(User user, List<Long> roleIds);
    void updateUser(User user, List<Long> roleIds);
    void delete(Long id);
}