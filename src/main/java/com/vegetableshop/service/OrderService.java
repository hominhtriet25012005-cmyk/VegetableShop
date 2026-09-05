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
import com.vegetableshop.event.OrderPlacedMailEvent;
import com.vegetableshop.exception.OrderNotFoundException;
import com.vegetableshop.exception.OrderOperationException;
import com.vegetableshop.repository.CartRepository;
import com.vegetableshop.repository.OrderRepository;
import com.vegetableshop.repository.ProductRepository;
import com.vegetableshop.repository.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@Profile("mysql")
public class OrderService {

    private static final DateTimeFormatter CODE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final InventoryService inventoryService;
    private final BankTransferService bankTransferService;
    private final CheckoutPricingService checkoutPricingService;
    private final VoucherService voucherService;

    @org.springframework.beans.factory.annotation.Autowired
    public OrderService(
        CartRepository cartRepository,
        ProductRepository productRepository,
        UserRepository userRepository,
        OrderRepository orderRepository,
        ApplicationEventPublisher eventPublisher,
        InventoryService inventoryService,
        BankTransferService bankTransferService,
        Optional<CheckoutPricingService> checkoutPricingService,
        Optional<VoucherService> voucherService
    ) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.inventoryService = inventoryService;
        this.bankTransferService = bankTransferService;
        this.checkoutPricingService = checkoutPricingService.orElse(null);
        this.voucherService = voucherService.orElse(null);
    }

    /** Kept for focused unit tests from phases before discounts. */
    public OrderService(CartRepository cartRepository, ProductRepository productRepository,
                        UserRepository userRepository, OrderRepository orderRepository,
                        ApplicationEventPublisher eventPublisher, InventoryService inventoryService,
                        BankTransferService bankTransferService) {
        this(cartRepository, productRepository, userRepository, orderRepository, eventPublisher,
            inventoryService, bankTransferService, Optional.empty(), Optional.empty());
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Order placeOrder(String email, CheckoutRequest request) {
        User user = userRepository.findByEmailIgnoreCase(email)
            .filter(User::isStatus)
            .orElseThrow(() -> new OrderOperationException("Không tìm thấy tài khoản đang hoạt động"));
        if (request.getCheckoutToken()==null || !request.getCheckoutToken().matches("[a-fA-F0-9-]{36}"))
            throw new OrderOperationException("Phiên đặt hàng không hợp lệ. Vui lòng tải lại trang thanh toán.");
        var existing = orderRepository.findByCheckoutTokenAndUserEmailIgnoreCase(request.getCheckoutToken(), email);
        if (existing.isPresent()) return existing.get();
        if (!"COD".equals(request.getPaymentMethod()) && !"BANK_TRANSFER".equals(request.getPaymentMethod()))
            throw new OrderOperationException("Phương thức thanh toán không được hỗ trợ");
        if ("BANK_TRANSFER".equals(request.getPaymentMethod())) bankTransferService.requireReady();
        Cart cart = cartRepository.findForCheckout(email)
            .orElseThrow(() -> new OrderOperationException("Giỏ hàng của bạn đang trống"));
        existing = orderRepository.findByCheckoutTokenAndUserEmailIgnoreCase(request.getCheckoutToken(), email);
        if (existing.isPresent()) return existing.get();

        List<CartItem> cartItems = new ArrayList<>(cart.getItems());
        if (cartItems.isEmpty()) {
            throw new OrderOperationException("Giỏ hàng của bạn đang trống");
        }
        cartItems.sort(Comparator.comparing(item -> item.getProduct().getId()));

        Order order = new Order();
        order.setOrderCode(generateOrderCode());
        order.setCheckoutToken(request.getCheckoutToken());
        order.setUser(user);
        order.setReceiverName(request.getReceiverName().trim());
        order.setReceiverPhone(request.getReceiverPhone().trim());
        order.setShippingAddress(request.getShippingAddress().trim());
        order.setNote(normalizeOptional(request.getNote()));
        order.setPaymentMethod(PaymentMethod.valueOf(request.getPaymentMethod()));
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setStatus(OrderStatus.PENDING);

        java.util.Map<Product,Integer> quantities = new java.util.LinkedHashMap<>();
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

            quantities.put(product, requestedQuantity);
        }

        com.vegetableshop.dto.CheckoutPricingView pricing = checkoutPricingService == null
            ? regularPricing(quantities) : checkoutPricingService.quote(quantities, user, request.getVoucherCode(), true);
        order.setSubtotalAmount(pricing.subtotalAmount());
        order.setPromotionDiscountAmount(pricing.promotionDiscountAmount());
        order.setVoucherDiscountAmount(pricing.voucherDiscountAmount());
        order.setVoucher(pricing.voucher());
        order.setVoucherCode(pricing.voucher() == null ? null : pricing.voucher().getCode());
        order.setTotalAmount(pricing.totalAmount());

        for (var line : pricing.lines()) {
            Product product=line.product();
            OrderDetail detail = new OrderDetail();
            detail.setProduct(product);
            detail.setProductName(product.getName());
            detail.setPrice(line.originalUnitPrice());
            detail.setQuantity(line.quantity());
            detail.setDiscountAmount(line.promotionDiscountAmount().add(line.voucherDiscountAmount()));
            detail.setSubtotal(line.totalAmount());
            order.addDetail(detail);

            int quantityBefore = product.getQuantity();
            product.setQuantity(quantityBefore - line.quantity());
            inventoryService.recordSale(product, quantityBefore, line.quantity(),
                order.getOrderCode(), email);
        }
        Order savedOrder = orderRepository.save(order);
        if (voucherService != null) voucherService.record(pricing.voucher(), user, savedOrder, pricing.voucherDiscountAmount());
        if (order.getPaymentMethod()==PaymentMethod.BANK_TRANSFER) bankTransferService.create(savedOrder);
        cart.getItems().clear();
        eventPublisher.publishEvent(OrderPlacedMailEvent.from(savedOrder));
        return savedOrder;
    }

    private com.vegetableshop.dto.CheckoutPricingView regularPricing(java.util.Map<Product,Integer> quantities) {
        var lines=new java.util.ArrayList<com.vegetableshop.dto.CheckoutPricingView.Line>();BigDecimal total=BigDecimal.ZERO;
        for(var e:quantities.entrySet()){BigDecimal line=e.getKey().getPrice().multiply(BigDecimal.valueOf(e.getValue()));total=total.add(line);lines.add(new com.vegetableshop.dto.CheckoutPricingView.Line(e.getKey(),e.getValue(),e.getKey().getPrice(),e.getKey().getPrice(),BigDecimal.ZERO,BigDecimal.ZERO,line,null,false));}
        return new com.vegetableshop.dto.CheckoutPricingView(List.copyOf(lines),total,BigDecimal.ZERO,BigDecimal.ZERO,total,null,null);
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
