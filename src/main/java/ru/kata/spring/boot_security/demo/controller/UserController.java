package ru.kata.spring.boot_security.demo.controller;

import ru.kata.spring.boot_security.demo.model.User;
import ru.kata.spring.boot_security.demo.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Главная страница (index.html)
    @GetMapping("/")
    public String home() {
        return "index";
    }

    // Список пользователей (users.html)
    @GetMapping("/users")
    public String getAllUsers(Model model) {
        model.addAttribute("users", userService.getAll());
        return "users";
    }

    // Форма добавления пользователя (addUser.html)
    @GetMapping("/addUser")
    public String showAddUserForm(Model model) {
        model.addAttribute("user", new User());
        return "addUser";
    }

    // Обработка добавления пользователя
    @PostMapping("/addUser")
    public String addUser(@ModelAttribute User user) {
        userService.save(user);
        return "redirect:/users";
    }

    // Форма редактирования пользователя (editUser.html)
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
        return "redirect:/users";
    }

    // Удаление пользователя
    @PostMapping("/deleteUser")
    public String deleteUser(@RequestParam Long id) {
        userService.delete(id);
        return "redirect:/users";
    }
}