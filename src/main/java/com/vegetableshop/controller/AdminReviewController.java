package com.vegetableshop.controller;

import com.vegetableshop.entity.ReviewStatus;
import com.vegetableshop.exception.ReviewOperationException;
import com.vegetableshop.service.ReviewService;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Profile("mysql")
public class AdminReviewController {
    private final ReviewService reviews;
    public AdminReviewController(ReviewService reviews) { this.reviews = reviews; }

    @GetMapping("/admin/reviews")
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(required = false) ReviewStatus status,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("reviewPage", reviews.searchAdmin(keyword, status, page));
        model.addAttribute("keyword", keyword); model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", ReviewStatus.values());
        return "admin/reviews";
    }

    @GetMapping("/admin/reviews/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("review", reviews.adminDetail(id));
        return "admin/review-detail";
    }

    @PostMapping("/admin/reviews/{id}/moderate")
    public String moderate(@PathVariable Long id, @RequestParam ReviewStatus status,
                           @RequestParam(defaultValue = "") String note,
                           Authentication authentication, RedirectAttributes redirect) {
        try {
            reviews.moderate(id, status, note, authentication.getName());
            redirect.addFlashAttribute("successMessage", "Đã cập nhật trạng thái đánh giá");
        } catch (ReviewOperationException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/reviews/" + id;
    }
}
