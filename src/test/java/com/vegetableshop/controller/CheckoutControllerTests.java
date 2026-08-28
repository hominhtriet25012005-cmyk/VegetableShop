package com.vegetableshop.controller;

import com.vegetableshop.dto.CheckoutRequest;
import com.vegetableshop.entity.Cart;
import com.vegetableshop.entity.CartItem;
import com.vegetableshop.entity.Order;
import com.vegetableshop.entity.User;
import com.vegetableshop.exception.OrderOperationException;
import com.vegetableshop.service.CartService;
import com.vegetableshop.service.OrderService;
import com.vegetableshop.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.ui.ConcurrentModel;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutControllerTests {

    @Mock private CartService cartService;
    @Mock private UserService userService;
    @Mock private OrderService orderService;

    @Test
    void checkoutPrefillsCustomerAndUsesBackendCartTotal() {
        Cart cart = cartWithOneItem();
        User user = new User();
        user.setFullName("Nguyễn Văn An");
        user.setPhone("0901234567");
        user.setAddress("123 Nguyễn Trãi");
        when(cartService.getOrCreateCart("user@example.com")).thenReturn(cart);
        when(userService.findByEmail("user@example.com")).thenReturn(user);
        when(cartService.calculateTotal(cart)).thenReturn(new BigDecimal("50000"));
        CheckoutRequest request = new CheckoutRequest();
        ConcurrentModel model = new ConcurrentModel();

        String view = controller().checkout(authentication(), request, model, new RedirectAttributesModelMap());

        assertEquals("checkout", view);
        assertEquals("Nguyễn Văn An", request.getReceiverName());
        assertEquals("0901234567", request.getReceiverPhone());
        assertEquals("123 Nguyễn Trãi", request.getShippingAddress());
        assertEquals(new BigDecimal("50000"), model.getAttribute("cartTotal"));
    }

    @Test
    void emptyCartRedirectsBackWithFriendlyMessage() {
        when(cartService.getOrCreateCart("user@example.com")).thenReturn(new Cart());
        RedirectAttributesModelMap attributes = new RedirectAttributesModelMap();
        String view = controller().checkout(authentication(), new CheckoutRequest(), new ConcurrentModel(), attributes);
        assertEquals("redirect:/cart", view);
        assertEquals("Giỏ hàng của bạn đang trống", attributes.getFlashAttributes().get("errorMessage"));
    }

    @Test
    void validCheckoutRedirectsToOwnedOrderDetail() {
        CheckoutRequest request = new CheckoutRequest();
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(request, "checkoutRequest");
        Order order = new Order();
        order.setId(42L);
        when(orderService.placeOrder("user@example.com", request)).thenReturn(order);

        String view = controller().placeOrder(authentication(), request, result, new ConcurrentModel());

        assertEquals("redirect:/orders/42?success", view);
    }

    @Test
    void checkoutFailureReturnsFormAndPreservesTransactionMessage() {
        CheckoutRequest request = new CheckoutRequest();
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(request, "checkoutRequest");
        Cart cart = cartWithOneItem();
        when(orderService.placeOrder("user@example.com", request))
            .thenThrow(new OrderOperationException("Sản phẩm chỉ còn 1 sản phẩm"));
        when(cartService.getOrCreateCart("user@example.com")).thenReturn(cart);
        when(cartService.calculateTotal(cart)).thenReturn(BigDecimal.TEN);

        String view = controller().placeOrder(authentication(), request, result, new ConcurrentModel());

        assertEquals("checkout", view);
        assertEquals(1, result.getGlobalErrorCount());
    }

    @Test
    void invalidFormDoesNotCallOrderService() {
        CheckoutRequest request = new CheckoutRequest();
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(request, "checkoutRequest");
        result.rejectValue("receiverName", "blank", "Tên không hợp lệ");
        Cart cart = cartWithOneItem();
        when(cartService.getOrCreateCart("user@example.com")).thenReturn(cart);
        when(cartService.calculateTotal(cart)).thenReturn(BigDecimal.TEN);

        assertEquals("checkout", controller().placeOrder(authentication(), request, result, new ConcurrentModel()));
        verify(orderService, never()).placeOrder("user@example.com", request);
    }

    @Test
    void historyAndDetailUseAuthenticatedEmail() {
        Order order = new Order();
        when(orderService.findOrdersForUser("user@example.com")).thenReturn(List.of(order));
        when(orderService.findOwnedOrder(5L, "user@example.com")).thenReturn(order);
        ConcurrentModel historyModel = new ConcurrentModel();
        ConcurrentModel detailModel = new ConcurrentModel();

        assertEquals("my-orders", controller().myOrders(authentication(), historyModel));
        assertEquals("order-detail", controller().orderDetail(authentication(), 5L, detailModel));
        assertEquals(List.of(order), historyModel.getAttribute("orders"));
        assertSame(order, detailModel.getAttribute("order"));
    }

    private CheckoutController controller() {
        return new CheckoutController(cartService, userService, orderService);
    }

    private Cart cartWithOneItem() {
        Cart cart = new Cart();
        cart.addItem(new CartItem());
        return cart;
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return UsernamePasswordAuthenticationToken.authenticated("user@example.com", "ignored", List.of());
    }
}
