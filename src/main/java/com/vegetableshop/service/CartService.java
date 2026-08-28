package com.vegetableshop.service;

import com.vegetableshop.dto.CartMutationResponse;
import com.vegetableshop.entity.Cart;
import com.vegetableshop.entity.CartItem;
import com.vegetableshop.entity.Product;
import com.vegetableshop.entity.User;
import com.vegetableshop.exception.CartOperationException;
import com.vegetableshop.exception.ProductNotFoundException;
import com.vegetableshop.repository.CartItemRepository;
import com.vegetableshop.repository.CartRepository;
import com.vegetableshop.repository.ProductRepository;
import com.vegetableshop.repository.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Profile("mysql")
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(
        CartRepository cartRepository,
        CartItemRepository cartItemRepository,
        ProductRepository productRepository,
        UserRepository userRepository
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Cart getOrCreateCart(String email) {
        return cartRepository.findByUserEmailIgnoreCase(email)
            .orElseGet(() -> createCart(email));
    }

    @Transactional
    public CartMutationResponse addProduct(String email, Long productId, int requestedQuantity) {
        requirePositiveQuantity(requestedQuantity);
        Product product = productRepository.findByIdAndStatusTrueAndCategoryStatusTrue(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));

        Cart cart = getOrCreateCart(email);
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
            .orElseGet(() -> newCartItem(cart, product));

        long newQuantity = item.getId() == null
            ? requestedQuantity
            : (long) item.getQuantity() + requestedQuantity;
        requireAvailableStock(product, newQuantity);

        item.setQuantity((int) newQuantity);
        cartItemRepository.saveAndFlush(item);
        return response(email, item, "Đã thêm sản phẩm vào giỏ hàng");
    }

    @Transactional
    public CartMutationResponse updateQuantity(String email, Long itemId, int quantity) {
        requirePositiveQuantity(quantity);
        CartItem item = findOwnedItem(email, itemId);
        Product product = item.getProduct();
        if (!product.isStatus() || !product.getCategory().isStatus()) {
            throw new CartOperationException("Sản phẩm này hiện không còn được bán");
        }
        requireAvailableStock(product, quantity);
        item.setQuantity(quantity);
        cartItemRepository.saveAndFlush(item);
        return response(email, item, "Đã cập nhật số lượng");
    }

    @Transactional
    public CartMutationResponse removeItem(String email, Long itemId) {
        CartItem item = findOwnedItem(email, itemId);
        cartItemRepository.delete(item);
        cartItemRepository.flush();
        return response(email, itemId, 0, 0, BigDecimal.ZERO, "Đã xóa sản phẩm khỏi giỏ hàng");
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotal(Cart cart) {
        return cart.getItems().stream()
            .map(CartItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public int countTotalQuantity(String email) {
        Long quantity = cartItemRepository.sumQuantityByUserEmail(email);
        return quantity == null ? 0 : quantity.intValue();
    }

    private Cart createCart(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
            .filter(User::isStatus)
            .orElseThrow(() -> new CartOperationException("Không tìm thấy tài khoản đang hoạt động"));
        Cart cart = new Cart();
        cart.setUser(user);
        return cartRepository.save(cart);
    }

    private CartItem newCartItem(Cart cart, Product product) {
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        return item;
    }

    private CartItem findOwnedItem(String email, Long itemId) {
        return cartItemRepository.findByIdAndCartUserEmailIgnoreCase(itemId, email)
            .orElseThrow(() -> new CartOperationException("Không tìm thấy sản phẩm trong giỏ hàng của bạn"));
    }

    private void requirePositiveQuantity(int quantity) {
        if (quantity < 1) {
            throw new CartOperationException("Số lượng phải lớn hơn 0");
        }
    }

    private void requireAvailableStock(Product product, long quantity) {
        if (quantity > product.getQuantity()) {
            throw new CartOperationException(
                "Sản phẩm " + product.getName() + " chỉ còn " + product.getQuantity() + " sản phẩm"
            );
        }
    }

    private CartMutationResponse response(String email, CartItem item, String message) {
        return response(
            email,
            item.getId(),
            item.getQuantity(),
            item.getProduct().getQuantity(),
            item.getSubtotal(),
            message
        );
    }

    private CartMutationResponse response(
        String email,
        Long itemId,
        int quantity,
        int stock,
        BigDecimal itemSubtotal,
        String message
    ) {
        Long totalQuantity = cartItemRepository.sumQuantityByUserEmail(email);
        BigDecimal cartTotal = cartItemRepository.sumSubtotalByUserEmail(email);
        long lineCount = cartItemRepository.countByCartUserEmailIgnoreCase(email);
        return new CartMutationResponse(
            itemId,
            quantity,
            stock,
            itemSubtotal,
            totalQuantity == null ? 0 : totalQuantity.intValue(),
            cartTotal == null ? BigDecimal.ZERO : cartTotal,
            lineCount == 0,
            message
        );
    }
}
