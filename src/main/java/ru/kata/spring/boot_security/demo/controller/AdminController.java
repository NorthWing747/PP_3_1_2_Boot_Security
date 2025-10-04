package ru.kata.spring.boot_security.demo.controller;

import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.model.User;
import ru.kata.spring.boot_security.demo.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    // 📋 Главная страница админки
    @GetMapping
    public String adminPage(Model model) {
        model.addAttribute("users", userService.getAll());
        return "admin";
    }

    // Форма добавления пользователя
    @GetMapping("/addUser")
    public String showAddUserForm(Model model) {
        model.addAttribute("user", new User());
        return "addUser";
    }

    // Обработка добавления пользователя с ролями
    @PostMapping("/addUser")
    public String addUser(@ModelAttribute User user,
                          @RequestParam(value = "selectedRoles", required = false) List<String> selectedRoles) {

        // Создаем временные объекты Role для выбранных ролей
        if (selectedRoles != null && !selectedRoles.isEmpty()) {
            Set<Role> roles = new HashSet<>();
            for (String roleName : selectedRoles) {
                Role role = new Role();
                role.setName(roleName);
                roles.add(role);
            }
            user.setRoles(roles);
        }

        userService.save(user);
        return "redirect:/admin";
    }

    // Форма редактирования пользователя
    @GetMapping("/editUser")
    public String showEditUserForm(@RequestParam Long id, Model model) {
        User user = userService.getById(id);
        model.addAttribute("user", user);
        return "editUser";
    }

    // Обработка редактирования пользователя
    @PostMapping("/editUser")
    public String updateUser(@ModelAttribute User user) {
        userService.updateUser(user);
        return "redirect:/admin";
    }

    // Удаление пользователя
    @PostMapping("/deleteUser")
    public String deleteUser(@RequestParam Long id) {
        userService.delete(id);
        return "redirect:/admin";
    }
}