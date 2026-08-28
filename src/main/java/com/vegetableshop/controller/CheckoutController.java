package com.vegetableshop.controller;

import com.vegetableshop.dto.CheckoutRequest;
import com.vegetableshop.entity.Cart;
import com.vegetableshop.entity.Order;
import com.vegetableshop.entity.User;
import com.vegetableshop.exception.OrderOperationException;
import com.vegetableshop.service.CartService;
import com.vegetableshop.service.OrderService;
import com.vegetableshop.service.UserService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Profile("mysql")
public class CheckoutController {

    private final CartService cartService;
    private final UserService userService;
    private final OrderService orderService;

    public CheckoutController(CartService cartService, UserService userService, OrderService orderService) {
        this.cartService = cartService;
        this.userService = userService;
        this.orderService = orderService;
    }

    @GetMapping("/checkout")
    public String checkout(
        Authentication authentication,
        @ModelAttribute("checkoutRequest") CheckoutRequest request,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        Cart cart = cartService.getOrCreateCart(authentication.getName());
        if (cart.getItems().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Giỏ hàng của bạn đang trống");
            return "redirect:/cart";
        }
        prefillCustomer(request, userService.findByEmail(authentication.getName()));
        addCartToModel(cart, model);
        return "checkout";
    }

    @PostMapping("/checkout")
    public String placeOrder(
        Authentication authentication,
        @Valid @ModelAttribute("checkoutRequest") CheckoutRequest request,
        BindingResult bindingResult,
        Model model
    ) {
        if (bindingResult.hasErrors()) {
            return renderCheckout(authentication.getName(), model);
        }
        try {
            Order order = orderService.placeOrder(authentication.getName(), request);
            return "redirect:/orders/" + order.getId() + "?success";
        } catch (OrderOperationException exception) {
            bindingResult.reject("checkout.failed", exception.getMessage());
            return renderCheckout(authentication.getName(), model);
        }
    }

    @GetMapping("/my-orders")
    public String myOrders(Authentication authentication, Model model) {
        model.addAttribute("orders", orderService.findOrdersForUser(authentication.getName()));
        return "my-orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(Authentication authentication, @PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.findOwnedOrder(id, authentication.getName()));
        return "order-detail";
    }

    private String renderCheckout(String email, Model model) {
        Cart cart = cartService.getOrCreateCart(email);
        addCartToModel(cart, model);
        return "checkout";
    }

    private void addCartToModel(Cart cart, Model model) {
        model.addAttribute("cartItems", cart.getItems());
        model.addAttribute("cartTotal", cartService.calculateTotal(cart));
    }

    private void prefillCustomer(CheckoutRequest request, User user) {
        if (request.getReceiverName() == null) {
            request.setReceiverName(user.getFullName());
        }
        if (request.getReceiverPhone() == null) {
            request.setReceiverPhone(user.getPhone());
        }
        if (request.getShippingAddress() == null) {
            request.setShippingAddress(user.getAddress());
        }
    }
}
