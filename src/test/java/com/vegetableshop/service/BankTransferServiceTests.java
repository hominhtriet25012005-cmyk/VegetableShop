package com.vegetableshop.service;
import com.vegetableshop.config.BankTransferProperties;
import com.vegetableshop.entity.*;
import com.vegetableshop.repository.*;
import com.vegetableshop.exception.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class BankTransferServiceTests {
    BankTransferProperties config; BankTransferPaymentRepository payments; OrderRepository orders; UserRepository users;
    BankTransferService service;Order order;BankTransferPayment payment;User owner,admin;
    @BeforeEach void setup() {
        config=new BankTransferProperties();config.setEnabled(true);config.setBin("970422");config.setBankName("Test bank");config.setAccountNumber("0000000000");config.setAccountName("TEST RECIPIENT");
        payments=mock(BankTransferPaymentRepository.class);orders=mock(OrderRepository.class);users=mock(UserRepository.class);
        service=new BankTransferService(config,payments,orders,users,new VietQrService());
        owner=new User();owner.setId(1L);owner.setEmail("owner@test.local");owner.setRole(Role.USER);owner.setStatus(true);
        admin=new User();admin.setId(2L);admin.setEmail("admin@test.local");admin.setRole(Role.ADMIN);admin.setStatus(true);
        when(users.findByEmailIgnoreCase(owner.getEmail())).thenReturn(Optional.of(owner));when(users.findByEmailIgnoreCase(admin.getEmail())).thenReturn(Optional.of(admin));
        order=new Order();order.setId(3L);order.setUser(owner);order.setOrderCode("VS-20260904120000-AABBCCDD");order.setPaymentMethod(PaymentMethod.BANK_TRANSFER);order.setPaymentStatus(PaymentStatus.UNPAID);order.setStatus(OrderStatus.PENDING);order.setTotalAmount(new BigDecimal("185000"));
        payment=new BankTransferPayment();payment.setOrder(order);payment.setAmount(order.getTotalAmount());
        when(orders.findByIdForUpdate(3L)).thenReturn(Optional.of(order));when(payments.findByOrderId(3L)).thenReturn(Optional.of(payment));
    }
    @Test void snapshotIsServerAmountAndDoesNotChangeWithConfiguration() {
        when(payments.save(any())).thenAnswer(i->i.getArgument(0));var p=service.create(order);
        assertEquals(order.getTotalAmount(),p.getAmount());assertEquals("VS20260904120000AABBCCDD",p.getReference());
        config.setAccountNumber("1111111111");assertEquals("0000000000",p.getAccountNumber());
    }
    @Test void missingConfigurationDisablesTransfer() {config.setEnabled(false);assertThrows(OrderOperationException.class,()->service.create(order));}
    @Test void reportIsIdempotentAndDoesNotMarkPaid() {
        service.report(3L,owner.getEmail());var time=payment.getReportedAt();service.report(3L,owner.getEmail());
        assertEquals(PaymentStatus.REPORTED,order.getPaymentStatus());assertEquals(time,payment.getReportedAt());assertNull(order.getPaidAt());
    }
    @Test void ownerCannotConfirmAndOtherUserCannotViewOrReport() {
        assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.confirm(3L,owner.getEmail(),payment.getAmount(),"TX123"));
        User stranger=new User();stranger.setId(5L);stranger.setStatus(true);stranger.setRole(Role.USER);when(users.findByEmailIgnoreCase("other")).thenReturn(Optional.of(stranger));
        assertThrows(OrderNotFoundException.class,()->service.view(3L,"other"));assertThrows(OrderNotFoundException.class,()->service.report(3L,"other"));
    }
    @Test void amountMismatchAndReusedTransactionRejected() {
        assertThrows(OrderOperationException.class,()->service.confirm(3L,admin.getEmail(),new BigDecimal("184000"),"TX123"));
        when(orders.existsByPaymentTransactionCode("TX123")).thenReturn(true);
        assertThrows(OrderOperationException.class,()->service.confirm(3L,admin.getEmail(),payment.getAmount(),"TX123"));assertEquals(PaymentStatus.UNPAID,order.getPaymentStatus());
    }
    @Test void confirmRecordsAuditAndCannotBeOverwritten() {
        service.confirm(3L,admin.getEmail(),payment.getAmount(),"tx123");var at=order.getPaidAt();
        assertEquals(PaymentStatus.PAID,order.getPaymentStatus());assertEquals(admin.getEmail(),payment.getConfirmedBy());
        service.confirm(3L,admin.getEmail(),payment.getAmount(),"tx123");assertEquals(at,order.getPaidAt());verify(orders,times(1)).flush();
        assertThrows(OrderOperationException.class,()->service.confirm(3L,admin.getEmail(),payment.getAmount(),"TX999"));
    }
    @Test void cancelledAndCodOrdersCannotReceiveQrPayments() {
        order.setStatus(OrderStatus.CANCELLED);assertThrows(OrderOperationException.class,()->service.report(3L,owner.getEmail()));
        assertThrows(OrderOperationException.class,()->service.confirm(3L,admin.getEmail(),payment.getAmount(),"TX123"));
        order.setStatus(OrderStatus.PENDING);order.setPaymentMethod(PaymentMethod.COD);assertThrows(OrderOperationException.class,()->service.report(3L,owner.getEmail()));
    }
}
