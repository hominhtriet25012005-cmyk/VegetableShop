package com.vegetableshop.service;

import com.vegetableshop.dto.CheckoutRequest;
import com.vegetableshop.entity.Cart;
import com.vegetableshop.entity.CartItem;
import com.vegetableshop.entity.Order;
import com.vegetableshop.entity.OrderDetail;
import com.vegetableshop.entity.OrderStatus;
import com.vegetableshop.entity.PaymentMethod;
import com.vegetableshop.entity.PaymentStatus;
import com.vegetableshop.entity.Product;
import com.vegetableshop.entity.User;
import com.vegetableshop.exception.OrderNotFoundException;
import com.vegetableshop.exception.OrderOperationException;
import com.vegetableshop.repository.CartRepository;
import com.vegetableshop.repository.OrderRepository;
import com.vegetableshop.repository.ProductRepository;
import com.vegetableshop.repository.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Profile("mysql")
public class OrderService {

    private static final DateTimeFormatter CODE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public OrderService(
        CartRepository cartRepository,
        ProductRepository productRepository,
        UserRepository userRepository,
        OrderRepository orderRepository
    ) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Order placeOrder(String email, CheckoutRequest request) {
        User user = userRepository.findByEmailIgnoreCase(email)
            .filter(User::isStatus)
            .orElseThrow(() -> new OrderOperationException("Không tìm thấy tài khoản đang hoạt động"));
        Cart cart = cartRepository.findByUserEmailIgnoreCase(email)
            .orElseThrow(() -> new OrderOperationException("Giỏ hàng của bạn đang trống"));

        List<CartItem> cartItems = new ArrayList<>(cart.getItems());
        if (cartItems.isEmpty()) {
            throw new OrderOperationException("Giỏ hàng của bạn đang trống");
        }
        cartItems.sort(Comparator.comparing(item -> item.getProduct().getId()));

        Order order = new Order();
        order.setOrderCode(generateOrderCode());
        order.setUser(user);
        order.setReceiverName(request.getReceiverName().trim());
        order.setReceiverPhone(request.getReceiverPhone().trim());
        order.setShippingAddress(request.getShippingAddress().trim());
        order.setNote(normalizeOptional(request.getNote()));
        order.setPaymentMethod(PaymentMethod.COD);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setStatus(OrderStatus.PENDING);

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Long productId = cartItem.getProduct().getId();
            Product product = productRepository.findActiveByIdForUpdate(productId)
                .orElseThrow(() -> new OrderOperationException("Sản phẩm trong giỏ hiện không còn được bán"));
            int requestedQuantity = cartItem.getQuantity();
            if (requestedQuantity < 1) {
                throw new OrderOperationException("Số lượng đặt hàng không hợp lệ");
            }
            if (requestedQuantity > product.getQuantity()) {
                throw new OrderOperationException(
                    "Sản phẩm " + product.getName() + " chỉ còn " + product.getQuantity() + " sản phẩm"
                );
            }

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(requestedQuantity));
            OrderDetail detail = new OrderDetail();
            detail.setProduct(product);
            detail.setProductName(product.getName());
            detail.setPrice(product.getPrice());
            detail.setQuantity(requestedQuantity);
            detail.setSubtotal(subtotal);
            order.addDetail(detail);
            total = total.add(subtotal);

            product.setQuantity(product.getQuantity() - requestedQuantity);
        }

        order.setTotalAmount(total);
        Order savedOrder = orderRepository.save(order);
        cart.getItems().clear();
        return savedOrder;
    }

    @Transactional(readOnly = true)
    public List<Order> findOrdersForUser(String email) {
        return orderRepository.findByUserEmailIgnoreCaseOrderByCreatedAtDesc(email);
    }

    @Transactional(readOnly = true)
    public Order findOwnedOrder(Long id, String email) {
        return orderRepository.findByIdAndUserEmailIgnoreCase(id, email)
            .orElseThrow(() -> new OrderNotFoundException(id));
    }

    private String generateOrderCode() {
        String suffix = UUID.randomUUID().toString().replace("-", "")
            .substring(0, 8).toUpperCase(Locale.ROOT);
        return "VS-" + LocalDateTime.now().format(CODE_TIME) + "-" + suffix;
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
