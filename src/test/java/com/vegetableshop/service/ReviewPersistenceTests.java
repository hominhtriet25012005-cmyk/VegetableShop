package com.vegetableshop.service;

import com.vegetableshop.dto.ReviewRequest;
import com.vegetableshop.entity.*;
import com.vegetableshop.repository.*;
import com.vegetableshop.exception.ReviewOperationException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql = false, properties = {"spring.sql.init.mode=never", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"})
@ActiveProfiles("mysql")
@Import(ReviewService.class)
class ReviewPersistenceTests {
    @Autowired EntityManager em;
    @Autowired ReviewService service;
    @Autowired ReviewRepository reviews;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean ReviewImageStorage storage;

    record Fixture(User user, User admin, Product product, OrderDetail detail) {}
    private Fixture fixture() {
        var user = new User(); user.setEmail(UUID.randomUUID() + "@example.com"); user.setFullName("Khách kiểm thử"); em.persist(user);
        var admin = new User(); admin.setEmail(UUID.randomUUID() + "@example.com"); admin.setFullName("Admin kiểm thử"); admin.setRole(Role.ADMIN); em.persist(admin);
        var category = new Category(); category.setName("Danh mục " + UUID.randomUUID()); em.persist(category);
        var product = new Product(); product.setName("Nấm kiểm thử"); product.setCategory(category); product.setPrice(BigDecimal.TEN); em.persist(product);
        return new Fixture(user, admin, product, purchase(user, product));
    }
    private OrderDetail purchase(User user, Product product) {
        var order = new Order(); order.setOrderCode(UUID.randomUUID().toString().substring(0, 20)); order.setUser(user);
        order.setReceiverName("Khách"); order.setReceiverPhone("0900000000"); order.setShippingAddress("Địa chỉ thử nghiệm");
        order.setStatus(OrderStatus.COMPLETED); order.setPaymentMethod(PaymentMethod.COD); order.setPaymentStatus(PaymentStatus.PAID);
        var detail = new OrderDetail(); detail.setProduct(product); detail.setProductName(product.getName()); detail.setPrice(BigDecimal.TEN);
        detail.setQuantity(1); detail.setSubtotal(BigDecimal.TEN); order.addDetail(detail); em.persist(order); em.flush(); return detail;
    }
    private ReviewRequest request(Long detail) { var r = new ReviewRequest(); r.setOrderDetailId(detail); r.setRating(5); r.setComment("Rất tươi"); return r; }

    @Test void pendingApprovalHiddenAndDeletedAreConsistentAcrossQueries() {
        var f = fixture();
        assertEquals(1, service.eligiblePurchases(f.user().getEmail(), f.product().getId()).size());
        var review = service.save(f.user().getEmail(), f.product().getId(), request(f.detail().getId()));
        assertTrue(service.eligiblePurchases(f.user().getEmail(), f.product().getId()).isEmpty());
        assertEquals(0, service.summarize(f.product().getId()).reviewCount());
        assertTrue(service.findByProduct(f.product().getId()).isEmpty());
        service.moderate(review.getId(), ReviewStatus.APPROVED, "", f.admin().getEmail()); em.flush(); em.clear();
        assertEquals(1, service.findByProduct(f.product().getId()).size());
        assertEquals(5, service.summarize(f.product().getId()).averageRating());
        assertEquals(100, service.summarize(f.product().getId()).percentFor(5));
        assertEquals(1, service.searchAdmin("Nấm", ReviewStatus.APPROVED, 0).getTotalElements());
        assertEquals(1, service.findOwnReviews(f.user().getEmail(), f.product().getId()).size());
        assertTrue(service.findOwnReviews(f.admin().getEmail(), f.product().getId()).isEmpty());
        assertNotNull(service.adminDetail(review.getId()).getUser().getFullName());
        service.moderate(review.getId(), ReviewStatus.HIDDEN, "Ảnh cần kiểm tra", f.admin().getEmail());
        assertEquals(0, service.summarize(f.product().getId()).reviewCount());
        service.moderate(review.getId(), ReviewStatus.DELETED, "Nội dung vi phạm", f.admin().getEmail());
        assertEquals(ReviewStatus.DELETED, reviews.findById(review.getId()).orElseThrow().getStatus());
        assertTrue(service.eligiblePurchases(f.user().getEmail(), f.product().getId()).isEmpty());
    }

    @Test void differentPurchaseCanReviewSameProductAndUniqueKeyRejectsDuplicate() {
        var f = fixture();
        service.save(f.user().getEmail(), f.product().getId(), request(f.detail().getId()));
        var second = purchase(f.user(), f.product());
        service.save(f.user().getEmail(), f.product().getId(), request(second.getId()));
        assertEquals(2, service.findOwnReviews(f.user().getEmail(), f.product().getId()).size());
        var duplicate = new Review(); duplicate.setUser(f.user()); duplicate.setProduct(f.product());
        duplicate.setOrderDetail(f.detail()); duplicate.setRating(4);
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> reviews.saveAndFlush(duplicate));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentSubmissionsForOnePurchaseOnlyCreateOneReview() throws Exception {
        var f = new TransactionTemplate(transactionManager).execute(tx -> fixture());
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> submit = () -> {
                start.await();
                try { service.save(f.user().getEmail(), f.product().getId(), request(f.detail().getId())); return true; }
                catch (ReviewOperationException expected) { return false; }
            };
            var a = executor.submit(submit); var b = executor.submit(submit); start.countDown();
            int successes = (a.get(15, TimeUnit.SECONDS) ? 1 : 0) + (b.get(15, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successes);
            assertEquals(1, service.findOwnReviews(f.user().getEmail(), f.product().getId()).size());
        }
    }
}
