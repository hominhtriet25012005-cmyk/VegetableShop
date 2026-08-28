package com.vegetableshop.controller;

import com.vegetableshop.dto.RegisterRequest;
import com.vegetableshop.exception.DuplicateEmailException;
import com.vegetableshop.service.UserService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@Profile("mysql")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(@ModelAttribute("registerRequest") RegisterRequest registerRequest) {
        return "register";
    }

    @PostMapping("/register")
    public String register(
        @Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
        BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            userService.register(registerRequest);
        } catch (DuplicateEmailException exception) {
            bindingResult.rejectValue("email", "email.duplicate", exception.getMessage());
            return "register";
        }

        return "redirect:/login?registered";
    }
}
