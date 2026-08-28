package com.vegetableshop.controller;

import com.vegetableshop.entity.Cart;
import com.vegetableshop.exception.CartOperationException;
import com.vegetableshop.exception.ProductNotFoundException;
import com.vegetableshop.service.CartService;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Profile("mysql")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart")
    public String cart(Authentication authentication, Model model) {
        Cart cart = cartService.getOrCreateCart(authentication.getName());
        model.addAttribute("cart", cart);
        model.addAttribute("cartItems", cart.getItems());
        model.addAttribute("cartTotal", cartService.calculateTotal(cart));
        return "cart";
    }

    @PostMapping("/cart/items")
    public String addItem(
        Authentication authentication,
        @RequestParam Long productId,
        @RequestParam(defaultValue = "1") int quantity,
        RedirectAttributes redirectAttributes
    ) {
        try {
            cartService.addProduct(authentication.getName(), productId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sản phẩm vào giỏ hàng");
        } catch (CartOperationException | ProductNotFoundException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/items/{itemId}/quantity")
    public String updateQuantity(
        Authentication authentication,
        @PathVariable Long itemId,
        @RequestParam int quantity,
        RedirectAttributes redirectAttributes
    ) {
        try {
            cartService.updateQuantity(authentication.getName(), itemId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật số lượng");
        } catch (CartOperationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/items/{itemId}/delete")
    public String deleteItem(
        Authentication authentication,
        @PathVariable Long itemId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            cartService.removeItem(authentication.getName(), itemId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi giỏ hàng");
        } catch (CartOperationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/cart";
    }
}
