package ru.kata.spring.boot_security.demo.service;

import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.model.User;
import ru.kata.spring.boot_security.demo.repository.RoleRepository;
import ru.kata.spring.boot_security.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.*;

@Service
@Transactional
public class UserServiceImpl implements UserService, UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 🔐 Реализация UserDetailsService для Spring Security
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return user;
    }

    // 🏗️ Инициализация тестовых данных
    @PostConstruct
    public void initTestUsers() {
        // Создаем роли
        Role roleUser = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));

        Role roleAdmin = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN")));

        // Создаем обычного пользователя
        if (!userRepository.existsByUsername("user")) {
            User user = new User("User", "Userov", 25, "user", passwordEncoder.encode("user"), "user@example.com");
            user.setRoles(new HashSet<>(Collections.singletonList(roleUser)));
            userRepository.save(user);
            System.out.println("Создан тестовый пользователь: user/user");
        }

        // Создаем администратора
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User("Admin", "Adminov", 30, "admin", passwordEncoder.encode("admin"), "admin@example.com");
            admin.setRoles(new HashSet<>(Collections.singletonList(roleAdmin)));
            userRepository.save(admin);
            System.out.println("Создан тестовый администратор: admin/admin");
        }

        // Исправляем существующих пользователей без ролей
        fixExistingUsersWithoutRoles();
    }

    // 🔧 Исправляем пользователей без ролей
    @PostConstruct
    public void fixExistingUsersWithoutRoles() {
        List<User> users = userRepository.findAll();
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));

        boolean fixed = false;
        for (User user : users) {
            if (user.getRoles() == null || user.getRoles().isEmpty()) {
                System.out.println("Назначаем роль пользователю: " + user.getUsername());
                Set<Role> roles = new HashSet<>();
                roles.add(userRole);
                user.setRoles(roles);
                userRepository.save(user);
                fixed = true;
            }
        }
        if (fixed) {
            System.out.println("Исправлены пользователи без ролей");
        }
    }

    // 📋 CRUD методы
    @Override
    public List<User> getAll() {
        return userRepository.findAll();
    }

    @Override
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Override
    public User save(User user) {
        // Проверяем обязательные поля
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new RuntimeException("Логин не может быть пустым");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new RuntimeException("Пароль не может быть пустым");
        }

        // Проверяем, существует ли пользователь с таким логином
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Пользователь с логином '" + user.getUsername() + "' уже существует");
        }

        // Кодируем пароль
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Назначаем роль по умолчанию
        Role defaultRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));

        Set<Role> roles = new HashSet<>();
        roles.add(defaultRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        System.out.println("Создан пользователь: " + savedUser.getUsername() + " с ролями: " + savedUser.getRoles());

        return savedUser;
    }

    @Override
    public User updateUser(User user) {
        User existingUser = getById(user.getId());
        existingUser.setName(user.getName());
        existingUser.setSurname(user.getSurname());
        existingUser.setAge(user.getAge());
        existingUser.setUsername(user.getUsername());
        existingUser.setEmail(user.getEmail());

        // ✅ ПРАВИЛЬНО: Обновляем пароль только если он не пустой
        if (user.getPassword() != null && !user.getPassword().trim().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
            System.out.println("Пароль обновлен для пользователя: " + user.getUsername());
        }

        // ✅ Сохраняем существующие роли (не перезаписываем)
        // existingUser.setRoles(existingUser.getRoles());

        User updatedUser = userRepository.save(existingUser);
        System.out.println("Обновлен пользователь: " + updatedUser.getUsername() + " с ролями: " + updatedUser.getRoles());

        return updatedUser;
    }

    @Override
    public void delete(Long id) {
        User user = getById(id);
        System.out.println("Удален пользователь: " + user.getUsername());
        userRepository.delete(user);
    }

    // 🔍 Диагностический метод для проверки ролей пользователей
    public void checkUserRoles() {
        List<User> users = userRepository.findAll();
        System.out.println("=== ДИАГНОСТИКА РОЛЕЙ ПОЛЬЗОВАТЕЛЕЙ ===");
        for (User user : users) {
            System.out.println("User: " + user.getUsername() +
                    ", Email: " + user.getEmail() +
                    ", Roles: " + (user.getRoles() != null ? user.getRoles() : "NULL"));
        }
        System.out.println("=== КОНЕЦ ДИАГНОСТИКИ ===");
    }

    // 🔧 Метод для принудительного назначения роли пользователю
    public void assignRoleToUser(Long userId, String roleName) {
        User user = getById(userId);
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Роль не найдена: " + roleName));

        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        }
        user.getRoles().add(role);
        userRepository.save(user);
        System.out.println("Назначена роль " + roleName + " пользователю: " + user.getUsername());
    }
}