package ru.kata.spring.boot_security.demo.controller;

import ru.kata.spring.boot_security.demo.entity.User;
import ru.kata.spring.boot_security.demo.service.RoleService;
import ru.kata.spring.boot_security.demo.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final RoleService roleService;
    public AdminController(UserService userService, RoleService roleService) {
        this.userService = userService;
        this.roleService = roleService;
    }

    // Главная страница админа
    @GetMapping
    public String adminPage(Model model, Principal principal) {
        // Получаем текущего аутентифицированного пользователя
        String username = principal.getName();
        User authUser = userService.findByUsername(username);

        model.addAttribute("authUser", authUser);
        model.addAttribute("users", userService.getAll());

        return "admin";
    }

    // Форма добавления пользователя
    @GetMapping("/addUser")
    public String showAddUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("allRoles", roleService.getAllRoles());
        return "new";
    }

    // Обработка добавления пользователя с ОДНОЙ ролью
    @PostMapping("/save")
    public String saveUser(
            @ModelAttribute("user") User user,
            @RequestParam(value = "roleIds", required = false) List<Long> roleIds ) {
        userService.save(user, roleIds);
        return "redirect:/admin";
    }

    // Форма редактирования пользователя
    @GetMapping("/editUser")
    public String showEditUserForm(@RequestParam Long id, Model model) {
        User user = userService.getById(id);
        model.addAttribute("user", user);
        model.addAttribute("allRoles", roleService.getAllRoles());
        return "editUser";
    }

    // Обработка редактирования пользователя с двумя ролями
    @PostMapping("/editUser")
    public String updateUser(@ModelAttribute("user") User user,
                             @RequestParam(value = "roleIds", required = false) List<Long> roleIds) {
        userService.updateUser(user, roleIds);
        return "redirect:/admin";
    }

    // Удаление пользователя
    @PostMapping("/deleteUser")
    public String deleteUser(@RequestParam Long id) {
        userService.delete(id);
        return "redirect:/admin";
    }
}