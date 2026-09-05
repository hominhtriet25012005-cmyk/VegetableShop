package com.vegetableshop.controller;

import com.vegetableshop.dto.ForgotPasswordRequest;
import com.vegetableshop.dto.RegisterRequest;
import com.vegetableshop.dto.ResetPasswordRequest;
import com.vegetableshop.exception.DuplicateEmailException;
import com.vegetableshop.exception.InvalidAccountActivationTokenException;
import com.vegetableshop.service.AccountActivationService;
import com.vegetableshop.service.AccountMailWorkflowService;
import com.vegetableshop.exception.InvalidPasswordResetTokenException;
import com.vegetableshop.service.PasswordResetService;
import com.vegetableshop.service.CaptchaChallengeService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Profile("mysql")
public class AuthController {

    private final AccountMailWorkflowService mailWorkflowService;
    private final AccountActivationService activationService;
    private final PasswordResetService passwordResetService;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrations;
    private final boolean demoResetLinkEnabled;
    private final CaptchaChallengeService captchaChallengeService;

    public AuthController(
        AccountMailWorkflowService mailWorkflowService,
        AccountActivationService activationService,
        PasswordResetService passwordResetService,
        ObjectProvider<ClientRegistrationRepository> clientRegistrations,
        CaptchaChallengeService captchaChallengeService,
        @Value("${app.password-reset.demo-link-enabled:false}") boolean demoResetLinkEnabled
    ) {
        this.mailWorkflowService = mailWorkflowService;
        this.activationService = activationService;
        this.passwordResetService = passwordResetService;
        this.clientRegistrations = clientRegistrations;
        this.captchaChallengeService = captchaChallengeService;
        this.demoResetLinkEnabled = demoResetLinkEnabled;
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("googleLoginEnabled", clientRegistrations.getIfAvailable() != null);
        model.addAttribute("mailEnabled", mailWorkflowService.isMailEnabled());
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(
        @ModelAttribute("registerRequest") RegisterRequest registerRequest,
        HttpSession session,
        Model model
    ) {
        model.addAttribute("captchaQuestion", captchaChallengeService.issue(session, "register"));
        return "register";
    }

    @PostMapping("/register")
    public String register(
        @Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
        BindingResult bindingResult,
        @RequestParam(defaultValue = "") String captchaAnswer,
        HttpSession session,
        Model model
    ) {
        boolean captchaValid = captchaChallengeService.verify(session, "register", captchaAnswer);
        if (!captchaValid) {
            bindingResult.reject("captcha.invalid", "Kết quả CAPTCHA không đúng hoặc đã hết hạn");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("captchaQuestion", captchaChallengeService.issue(session, "register"));
            return "register";
        }

        try {
            var result = mailWorkflowService.register(registerRequest);
            return result.activationRequired()
                ? "redirect:/login?activationSent"
                : "redirect:/login?registered";
        } catch (DuplicateEmailException exception) {
            bindingResult.rejectValue("email", "email.duplicate", exception.getMessage());
            model.addAttribute("captchaQuestion", captchaChallengeService.issue(session, "register"));
            return "register";
        }

    }

    @GetMapping("/forgot-password")
    public String forgotPasswordForm(
        @ModelAttribute("forgotPasswordRequest") ForgotPasswordRequest forgotPasswordRequest,
        HttpSession session,
        Model model
    ) {
        model.addAttribute("mailEnabled", mailWorkflowService.isMailEnabled());
        model.addAttribute("captchaQuestion", captchaChallengeService.issue(session, "forgot-password"));
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(
        @Valid @ModelAttribute("forgotPasswordRequest") ForgotPasswordRequest forgotPasswordRequest,
        BindingResult bindingResult,
        @RequestParam(defaultValue = "") String captchaAnswer,
        HttpSession session,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        boolean captchaValid = captchaChallengeService.verify(session, "forgot-password", captchaAnswer);
        if (!captchaValid) {
            bindingResult.reject("captcha.invalid", "Kết quả CAPTCHA không đúng hoặc đã hết hạn");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("mailEnabled", mailWorkflowService.isMailEnabled());
            model.addAttribute("captchaQuestion", captchaChallengeService.issue(session, "forgot-password"));
            return "forgot-password";
        }

        mailWorkflowService.requestPasswordReset(forgotPasswordRequest.getEmail()).ifPresent(token -> {
            if (demoResetLinkEnabled) {
                redirectAttributes.addFlashAttribute("demoResetPath", "/reset-password?token=" + token);
            }
        });
        redirectAttributes.addFlashAttribute("resetRequested", true);
        return "redirect:/forgot-password";
    }

    @GetMapping("/activate-account")
    public String activateAccount(
        @RequestParam(name = "token", defaultValue = "") String token,
        Model model
    ) {
        try {
            activationService.activate(token);
            model.addAttribute("activationSuccess", true);
        } catch (InvalidAccountActivationTokenException exception) {
            model.addAttribute("activationSuccess", false);
            model.addAttribute("activationError", exception.getMessage());
        }
        return "activation-result";
    }

    @GetMapping("/resend-activation")
    public String resendActivationForm(
        @ModelAttribute("activationRequest") ForgotPasswordRequest activationRequest,
        Model model
    ) {
        model.addAttribute("mailEnabled", mailWorkflowService.isMailEnabled());
        return "resend-activation";
    }

    @PostMapping("/resend-activation")
    public String resendActivation(
        @Valid @ModelAttribute("activationRequest") ForgotPasswordRequest activationRequest,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("mailEnabled", mailWorkflowService.isMailEnabled());
            return "resend-activation";
        }
        mailWorkflowService.resendActivation(activationRequest.getEmail());
        redirectAttributes.addFlashAttribute("activationRequested", true);
        return "redirect:/resend-activation";
    }

    @GetMapping("/reset-password")
    public String resetPasswordForm(
        @RequestParam(name = "token", defaultValue = "") String token,
        @ModelAttribute("resetPasswordRequest") ResetPasswordRequest resetPasswordRequest,
        Model model
    ) {
        resetPasswordRequest.setToken(token);
        model.addAttribute("invalidToken", !passwordResetService.isValid(token));
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(
        @Valid @ModelAttribute("resetPasswordRequest") ResetPasswordRequest resetPasswordRequest,
        BindingResult bindingResult,
        Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("invalidToken", !passwordResetService.isValid(resetPasswordRequest.getToken()));
            return "reset-password";
        }

        try {
            passwordResetService.resetPassword(
                resetPasswordRequest.getToken(),
                resetPasswordRequest.getNewPassword()
            );
        } catch (InvalidPasswordResetTokenException exception) {
            bindingResult.reject("token.invalid", exception.getMessage());
            model.addAttribute("invalidToken", true);
            return "reset-password";
        }
        return "redirect:/login?passwordReset";
    }
}
