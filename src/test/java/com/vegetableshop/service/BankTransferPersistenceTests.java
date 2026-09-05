package com.vegetableshop.service;
import com.vegetableshop.config.BankTransferProperties;
import com.vegetableshop.dto.CheckoutRequest;
import com.vegetableshop.entity.*;
import com.vegetableshop.repository.*;
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
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql=false,properties={"spring.sql.init.mode=never","spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false"})
@ActiveProfiles("mysql")
@Import({OrderService.class,BankTransferService.class,BankTransferProperties.class,VietQrService.class})
class BankTransferPersistenceTests {
    @Autowired EntityManager em; @Autowired OrderService checkout; @Autowired BankTransferService bank;
    @Autowired OrderRepository orders; @Autowired BankTransferPaymentRepository payments;
    @Autowired BankTransferProperties config; @Autowired PlatformTransactionManager txm;
    @MockitoBean InventoryService inventory;
    record Fixture(String email,Long productId,Long cartId) {}
    Fixture seed() {
        config.setEnabled(true);config.setBin("970422");config.setAccountNumber("0000000000");config.setAccountName("TEST");config.setBankName("MB test");
        var u=new User();u.setEmail(UUID.randomUUID()+"@test.local");u.setFullName("Buyer test");em.persist(u);
        var c=new Category();c.setName(UUID.randomUUID().toString());em.persist(c);
        var p=new Product();p.setName("Product test");p.setCategory(c);p.setPrice(new BigDecimal("185000"));p.setQuantity(5);em.persist(p);
        var cart=new Cart();cart.setUser(u);var item=new CartItem();item.setProduct(p);item.setQuantity(1);cart.addItem(item);em.persist(cart);em.flush();
        return new Fixture(u.getEmail(),p.getId(),cart.getId());
    }
    CheckoutRequest request() {var r=new CheckoutRequest();r.setReceiverName("Test buyer");r.setReceiverPhone("0900000000");r.setShippingAddress("Test address");r.setPaymentMethod("BANK_TRANSFER");return r;}
    @Test void sequentialDuplicateUsesSameOrderAndSnapshot() {
        var f=seed();var r=request();var first=checkout.placeOrder(f.email(),r);em.flush();
        var second=checkout.placeOrder(f.email(),r);assertEquals(first.getId(),second.getId());
        assertEquals(4,em.find(Product.class,f.productId()).getQuantity());assertTrue(em.find(Cart.class,f.cartId()).getItems().isEmpty());
        var p=bank.view(first.getId(),f.email());assertEquals(new BigDecimal("185000"),p.getAmount());
        bank.report(first.getId(),f.email());assertEquals(PaymentStatus.REPORTED,orders.findById(first.getId()).orElseThrow().getPaymentStatus());
        assertTrue(payments.search(PaymentStatus.REPORTED,org.springframework.data.domain.PageRequest.of(0,100)).stream()
            .anyMatch(found -> found.getOrder().getId().equals(first.getId())));
    }
    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void concurrentCheckoutCreatesOneOrderAndDecrementsOnce() throws Exception {
        var tx=new TransactionTemplate(txm);var f=tx.execute(s->seed());var r=request();
        var gate=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {
            Callable<Long> action=()->{gate.await();return checkout.placeOrder(f.email(),r).getId();};
            var a=pool.submit(action);var b=pool.submit(action);gate.countDown();
            assertEquals(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));
            tx.executeWithoutResult(s->{assertEquals(4,em.find(Product.class,f.productId()).getQuantity());
                assertEquals(1,orders.findByUserEmailIgnoreCaseOrderByCreatedAtDesc(f.email()).size());});
        } finally {pool.shutdownNow();}
    }

    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void concurrentConfirmationIsIdempotentAndKeepsStock() throws Exception {
        var tx=new TransactionTemplate(txm);var f=tx.execute(s->seed());var id=checkout.placeOrder(f.email(),request()).getId();
        String admin=tx.execute(s->{var u=new User();u.setEmail(UUID.randomUUID()+"@test.local");u.setFullName("Test admin");u.setRole(Role.ADMIN);em.persist(u);return u.getEmail();});
        String code="TX"+UUID.randomUUID();var gate=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> action=()->{gate.await();bank.confirm(id,admin,new BigDecimal("185000"),code);return true;};
            var a=pool.submit(action);var b=pool.submit(action);gate.countDown();assertTrue(a.get(15,TimeUnit.SECONDS));assertTrue(b.get(15,TimeUnit.SECONDS));
            tx.executeWithoutResult(s->{assertEquals(4,em.find(Product.class,f.productId()).getQuantity());
                var o=orders.findById(id).orElseThrow();assertEquals(PaymentStatus.PAID,o.getPaymentStatus());assertNotNull(o.getPaidAt());
                assertEquals(admin,payments.findByOrderId(id).orElseThrow().getConfirmedBy());});
        } finally {pool.shutdownNow();}
    }
}
