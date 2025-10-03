package ru.kata.spring.boot_security.demo.service;

import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.model.User;
import ru.kata.spring.boot_security.demo.repository.RoleRepository;
import ru.kata.spring.boot_security.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        logger.info("UserServiceImpl инициализирован");
    }

    // 🔐 Реализация UserDetailsService для Spring Security
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Попытка загрузки пользователя по логину: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.warn("Пользователь не найден: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        logger.debug("Пользователь найден: {} (роли: {})", username, user.getRoles());
        return user;
    }

    // 🏗️ Инициализация тестовых данных
    @PostConstruct
    public void initTestUsers() {
        logger.info("Начало инициализации тестовых данных...");

        try {
            // Создаем роли
            Role roleUser = roleRepository.findByName("ROLE_USER")
                    .orElseGet(() -> {
                        logger.debug("Создание роли ROLE_USER");
                        return roleRepository.save(new Role("ROLE_USER"));
                    });

            Role roleAdmin = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> {
                        logger.debug("Создание роли ROLE_ADMIN");
                        return roleRepository.save(new Role("ROLE_ADMIN"));
                    });

            // Создаем обычного пользователя
            if (!userRepository.existsByUsername("user")) {
                User user = new User("User", "Userov", 25, "user", passwordEncoder.encode("user"), "user@example.com");
                user.setRoles(new HashSet<>(Collections.singletonList(roleUser)));
                userRepository.save(user);
                logger.info("Создан тестовый пользователь: user/user");
            } else {
                logger.debug("Тестовый пользователь 'user' уже существует");
            }

            // Создаем администратора
            if (!userRepository.existsByUsername("admin")) {
                User admin = new User("Admin", "Adminov", 30, "admin", passwordEncoder.encode("admin"), "admin@example.com");
                admin.setRoles(new HashSet<>(Collections.singletonList(roleAdmin)));
                userRepository.save(admin);
                logger.info("Создан тестовый администратор: admin/admin");
            } else {
                logger.debug("Тестовый администратор 'admin' уже существует");
            }

            // Исправляем существующих пользователей без ролей
            fixExistingUsersWithoutRoles();

            logger.info("Инициализация тестовых данных завершена успешно");

        } catch (Exception e) {
            logger.error("Ошибка при инициализации тестовых данных", e);
        }
    }

    // 🔧 Исправляем пользователей без ролей
    @PostConstruct
    public void fixExistingUsersWithoutRoles() {
        logger.debug("Проверка пользователей без ролей...");

        List<User> users = userRepository.findAll();
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    logger.warn("Роль ROLE_USER не найдена, создаем новую");
                    return roleRepository.save(new Role("ROLE_USER"));
                });

        boolean fixed = false;
        for (User user : users) {
            if (user.getRoles() == null || user.getRoles().isEmpty()) {
                logger.info("Назначаем роль ROLE_USER пользователю: {}", user.getUsername());
                Set<Role> roles = new HashSet<>();
                roles.add(userRole);
                user.setRoles(roles);
                userRepository.save(user);
                fixed = true;
            }
        }

        if (fixed) {
            logger.info("Исправлены пользователи без ролей");
        } else {
            logger.debug("Пользователи без ролей не найдены");
        }
    }

    // 📋 CRUD методы
    @Override
    public List<User> getAll() {
        logger.debug("Запрос всех пользователей");
        List<User> users = userRepository.findAll();
        logger.debug("Найдено пользователей: {}", users.size());
        return users;
    }

    @Override
    public User getById(Long id) {
        logger.debug("Запрос пользователя по ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Пользователь не найден с ID: {}", id);
                    return new RuntimeException("User not found with id: " + id);
                });

        logger.debug("Пользователь найден: {} (ID: {})", user.getUsername(), id);
        return user;
    }

    @Override
    public User save(User user) {
        logger.info("Попытка сохранения нового пользователя с логином: {}", user.getUsername());

        // Проверяем обязательные поля
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            logger.error("Попытка сохранения пользователя с пустым логином");
            throw new RuntimeException("Логин не может быть пустым");
        }

        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            logger.error("Попытка сохранения пользователя с пустым паролем: {}", user.getUsername());
            throw new RuntimeException("Пароль не может быть пустым");
        }

        // Проверяем, существует ли пользователь с таким логином
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            logger.warn("Попытка создания пользователя с существующим логином: {}", user.getUsername());
            throw new RuntimeException("Пользователь с логином '" + user.getUsername() + "' уже существует");
        }

        // Кодируем пароль
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Назначаем роль по умолчанию
        Role defaultRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    logger.warn("Роль ROLE_USER не найдена, создаем новую");
                    return roleRepository.save(new Role("ROLE_USER"));
                });

        Set<Role> roles = new HashSet<>();
        roles.add(defaultRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        logger.info("Пользователь успешно создан: {} (ID: {}) с ролями: {}",
                savedUser.getUsername(), savedUser.getId(), savedUser.getRoles());

        return savedUser;
    }

    @Override
    public User updateUser(User user) {
        logger.info("Попытка обновления пользователя: {} (ID: {})", user.getUsername(), user.getId());

        User existingUser = getById(user.getId());
        logger.debug("Текущие данные пользователя: имя={}, фамилия={}, возраст={}",
                existingUser.getName(), existingUser.getSurname(), existingUser.getAge());

        existingUser.setName(user.getName());
        existingUser.setSurname(user.getSurname());
        existingUser.setAge(user.getAge());
        existingUser.setUsername(user.getUsername());
        existingUser.setEmail(user.getEmail());

        // Обновляем пароль только если он не пустой
        if (user.getPassword() != null && !user.getPassword().trim().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
            logger.debug("Пароль обновлен для пользователя: {}", user.getUsername());
        } else {
            logger.debug("Пароль не изменен для пользователя: {}", user.getUsername());
        }

        User updatedUser = userRepository.save(existingUser);
        logger.info("Пользователь успешно обновлен: {} (ID: {}) с ролями: {}",
                updatedUser.getUsername(), updatedUser.getId(), updatedUser.getRoles());

        return updatedUser;
    }

    @Override
    public void delete(Long id) {
        logger.info("Попытка удаления пользователя с ID: {}", id);

        User user = getById(id);
        userRepository.delete(user);

        logger.info("Пользователь успешно удален: {} (ID: {})", user.getUsername(), id);
    }

    // 🔍 Диагностический метод для проверки ролей пользователей
    public void checkUserRoles() {
        logger.debug("=== НАЧАЛО ДИАГНОСТИКИ РОЛЕЙ ПОЛЬЗОВАТЕЛЕЙ ===");

        List<User> users = userRepository.findAll();
        for (User user : users) {
            logger.info("User: {}, Email: {}, Roles: {}",
                    user.getUsername(),
                    user.getEmail(),
                    user.getRoles() != null ? user.getRoles() : "NULL");
        }

        logger.debug("=== КОНЕЦ ДИАГНОСТИКИ РОЛЕЙ ПОЛЬЗОВАТЕЛЕЙ ===");
    }

    // 🔧 Метод для принудительного назначения роли пользователю
    public void assignRoleToUser(Long userId, String roleName) {
        logger.info("Попытка назначения роли {} пользователю с ID: {}", roleName, userId);

        User user = getById(userId);
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> {
                    logger.error("Роль не найдена: {}", roleName);
                    return new RuntimeException("Роль не найдена: " + roleName);
                });

        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
            logger.debug("Инициализирован пустой набор ролей для пользователя: {}", user.getUsername());
        }

        user.getRoles().add(role);
        userRepository.save(user);

        logger.info("Роль {} успешно назначена пользователю: {}", roleName, user.getUsername());
    }

    // 🔍 Метод для получения пользователя по email (дополнительный)
    public Optional<User> findByEmail(String email) {
        logger.debug("Поиск пользователя по email: {}", email);

        // Вам нужно добавить этот метод в UserRepository
        // Optional<User> user = userRepository.findByEmail(email);
        Optional<User> user = Optional.empty(); // заглушка

        if (user.isPresent()) {
            logger.debug("Пользователь найден по email {}: {}", email, user.get().getUsername());
        } else {
            logger.debug("Пользователь не найден по email: {}", email);
        }

        return user;
    }
}