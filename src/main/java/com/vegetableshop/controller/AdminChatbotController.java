package com.vegetableshop.controller;

import com.vegetableshop.dto.AdminChatbotFaqRequest;
import com.vegetableshop.entity.ChatbotInteractionStatus;
import com.vegetableshop.entity.ChatbotResponseSource;
import com.vegetableshop.exception.AdminOperationException;
import com.vegetableshop.service.AdminChatbotService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@Profile("mysql")
public class AdminChatbotController {

    private final AdminChatbotService chatbotService;

    public AdminChatbotController(AdminChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @GetMapping("/admin/chatbot")
    public String dashboard(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(required = false) ChatbotResponseSource source,
        @RequestParam(required = false) ChatbotInteractionStatus status,
        @RequestParam(defaultValue = "0") int page,
        Model model
    ) {
        try {
            model.addAttribute("chatbotDashboard",
                chatbotService.dashboard(from, to, keyword, source, status, page));
        } catch (AdminOperationException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            model.addAttribute("chatbotDashboard",
                chatbotService.dashboard(null, null, keyword, source, status, 0));
        }
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedSource", source);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("sources", ChatbotResponseSource.values());
        model.addAttribute("interactionStatuses", ChatbotInteractionStatus.values());
        model.addAttribute("retentionDays", chatbotService.retentionDays());
        return "admin/chatbot-dashboard";
    }

    @GetMapping("/admin/chatbot/faqs")
    public String faqs(@RequestParam(defaultValue = "") String keyword, Model model) {
        model.addAttribute("faqs", chatbotService.faqs(keyword));
        model.addAttribute("keyword", keyword);
        return "admin/chatbot-faqs";
    }

    @GetMapping("/admin/chatbot/faqs/new")
    public String newFaq(Model model) {
        model.addAttribute("faqRequest", new AdminChatbotFaqRequest());
        model.addAttribute("faqId", null);
        return "admin/chatbot-faq-form";
    }

    @PostMapping("/admin/chatbot/faqs")
    public String createFaq(
        @Valid @ModelAttribute("faqRequest") AdminChatbotFaqRequest request,
        BindingResult result,
        Model model,
        Authentication authentication,
        RedirectAttributes redirect
    ) {
        if (result.hasErrors()) {
            model.addAttribute("faqId", null);
            return "admin/chatbot-faq-form";
        }
        try {
            chatbotService.createFaq(request, authentication.getName());
            redirect.addFlashAttribute("successMessage", "Đã thêm FAQ cho chatbot");
            return "redirect:/admin/chatbot/faqs";
        } catch (AdminOperationException exception) {
            result.reject("faq.failed", exception.getMessage());
            model.addAttribute("faqId", null);
            return "admin/chatbot-faq-form";
        }
    }

    @GetMapping("/admin/chatbot/faqs/{id}/edit")
    public String editFaq(@PathVariable Long id, Model model) {
        model.addAttribute("faqRequest", chatbotService.faqForm(id));
        model.addAttribute("faqId", id);
        return "admin/chatbot-faq-form";
    }

    @PostMapping("/admin/chatbot/faqs/{id}")
    public String updateFaq(
        @PathVariable Long id,
        @Valid @ModelAttribute("faqRequest") AdminChatbotFaqRequest request,
        BindingResult result,
        Model model,
        Authentication authentication,
        RedirectAttributes redirect
    ) {
        if (result.hasErrors()) {
            model.addAttribute("faqId", id);
            return "admin/chatbot-faq-form";
        }
        try {
            chatbotService.updateFaq(id, request, authentication.getName());
            redirect.addFlashAttribute("successMessage", "Đã cập nhật FAQ");
            return "redirect:/admin/chatbot/faqs";
        } catch (AdminOperationException | EntityNotFoundException exception) {
            result.reject("faq.failed", exception.getMessage());
            model.addAttribute("faqId", id);
            return "admin/chatbot-faq-form";
        }
    }

    @PostMapping("/admin/chatbot/faqs/{id}/toggle")
    public String toggleFaq(
        @PathVariable Long id,
        Authentication authentication,
        RedirectAttributes redirect
    ) {
        try {
            boolean active = chatbotService.toggleFaq(id, authentication.getName());
            redirect.addFlashAttribute("successMessage",
                active ? "Đã kích hoạt FAQ" : "Đã tạm ẩn FAQ");
        } catch (RuntimeException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/chatbot/faqs";
    }

    @PostMapping("/admin/chatbot/interactions/purge")
    public String purgeInteractions(RedirectAttributes redirect) {
        long deleted = chatbotService.purgeExpiredInteractions();
        redirect.addFlashAttribute("successMessage",
            "Đã xóa " + deleted + " lượt hỏi quá thời hạn lưu trữ");
        return "redirect:/admin/chatbot";
    }
}
