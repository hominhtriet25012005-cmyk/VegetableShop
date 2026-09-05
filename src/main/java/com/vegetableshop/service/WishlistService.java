package com.vegetableshop.service;

import com.vegetableshop.entity.Product;
import com.vegetableshop.entity.User;
import com.vegetableshop.entity.Wishlist;
import com.vegetableshop.exception.ProductNotFoundException;
import com.vegetableshop.exception.WishlistOperationException;
import com.vegetableshop.repository.ProductRepository;
import com.vegetableshop.repository.UserRepository;
import com.vegetableshop.repository.WishlistRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Profile("mysql")
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private PromotionPricingService promotionPricingService;

    public WishlistService(
        WishlistRepository wishlistRepository,
        UserRepository userRepository,
        ProductRepository productRepository
    ) {
        this.wishlistRepository = wishlistRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Wishlist> findActiveItems(String email) {
        List<Wishlist> items=wishlistRepository.findByUserEmailIgnoreCaseOrderByCreatedAtDesc(email).stream()
            .filter(item -> item.getProduct().isStatus() && item.getProduct().getCategory().isStatus())
            .toList();
        if(promotionPricingService!=null) promotionPricingService.decorate(items.stream().map(Wishlist::getProduct).toList());
        return items;
    }

    @Transactional(readOnly = true)
    public Set<Long> findActiveProductIds(String email) {
        if (email == null) {
            return Set.of();
        }
        return new LinkedHashSet<>(wishlistRepository.findActiveProductIdsByUserEmail(email));
    }

    @Transactional(readOnly = true)
    public boolean contains(String email, Long productId) {
        return email != null
            && wishlistRepository.existsByUserEmailIgnoreCaseAndProductId(email, productId);
    }

    @Transactional(readOnly = true)
    public long countActive(String email) {
        return email == null ? 0 : wishlistRepository.countActiveByUserEmail(email);
    }

    @Transactional
    public boolean add(String email, Long productId) {
        User user = findActiveUser(email);
        Product product = productRepository.findByIdAndStatusTrueAndCategoryStatusTrue(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));
        if (wishlistRepository.findByUserIdAndProductId(user.getId(), productId).isPresent()) {
            return false;
        }
        Wishlist wishlist = new Wishlist();
        wishlist.setUser(user);
        wishlist.setProduct(product);
        wishlistRepository.saveAndFlush(wishlist);
        return true;
    }

    @Transactional
    public boolean remove(String email, Long productId) {
        return wishlistRepository.findByUserEmailIgnoreCaseAndProductId(email, productId)
            .map(item -> {
                wishlistRepository.delete(item);
                wishlistRepository.flush();
                return true;
            })
            .orElse(false);
    }

    private User findActiveUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
            .filter(User::isStatus)
            .orElseThrow(() -> new WishlistOperationException("Không tìm thấy tài khoản đang hoạt động"));
    }
}
