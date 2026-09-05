package com.vegetableshop.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class ReviewUploadAdvice {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String tooLarge(HttpServletRequest request, RedirectAttributes redirect) {
        redirect.addFlashAttribute("errorMessage", "Ảnh quá lớn: tối đa 5 ảnh JPG/PNG, 2 MB mỗi ảnh");
        String uri = request.getRequestURI().substring(request.getContextPath().length());
        if (uri.matches("/product/[0-9]+/reviews"))
            return "redirect:" + uri.substring(0, uri.length() - "/reviews".length()) + "#reviews";
        return "redirect:/shop";
    }
}
