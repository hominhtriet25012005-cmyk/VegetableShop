package com.vegetableshop.controller;

import com.vegetableshop.dto.ChangePasswordRequest;
import com.vegetableshop.dto.ProfileUpdateRequest;
import com.vegetableshop.entity.User;
import com.vegetableshop.service.UserService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Profile("mysql")
public class AccountController {

    private final UserService userService;

    public AccountController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/account")
    public String profile(Authentication authentication, Model model) {
        User user = userService.findByEmail(authentication.getName());
        model.addAttribute("user", user);
        model.addAttribute("profileRequest", userService.createProfileRequest(authentication.getName()));
        return "account/profile";
    }

    @PostMapping("/account")
    public String updateProfile(
        Authentication authentication,
        @Valid @ModelAttribute("profileRequest") ProfileUpdateRequest profileRequest,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        User user = userService.findByEmail(authentication.getName());
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            return "account/profile";
        }
        userService.updateProfile(authentication.getName(), profileRequest);
        redirectAttributes.addFlashAttribute("profileUpdated", true);
        return "redirect:/account";
    }

    @GetMapping("/account/change-password")
    public String changePasswordForm(Authentication authentication, Model model) {
        User user = userService.findByEmail(authentication.getName());
        model.addAttribute("hasLocalPassword", user.getPassword() != null);
        model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        return "account/change-password";
    }

    @PostMapping("/account/change-password")
    public String changePassword(
        Authentication authentication,
        @Valid @ModelAttribute("changePasswordRequest") ChangePasswordRequest request,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        User user = userService.findByEmail(authentication.getName());
        model.addAttribute("hasLocalPassword", user.getPassword() != null);
        if (bindingResult.hasErrors()) {
            return "account/change-password";
        }

        try {
            userService.changePassword(
                authentication.getName(),
                request.getCurrentPassword(),
                request.getNewPassword()
            );
        } catch (IllegalArgumentException exception) {
            bindingResult.rejectValue("currentPassword", "password.invalid", exception.getMessage());
            return "account/change-password";
        }

        redirectAttributes.addFlashAttribute("passwordChanged", true);
        return "redirect:/account/change-password";
    }
}
