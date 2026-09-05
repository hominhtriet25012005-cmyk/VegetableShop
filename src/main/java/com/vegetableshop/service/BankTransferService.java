package com.vegetableshop.service;

import com.vegetableshop.config.BankTransferProperties;
import com.vegetableshop.entity.*;
import com.vegetableshop.repository.*;
import com.vegetableshop.exception.OrderOperationException;
import com.vegetableshop.exception.OrderNotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service @Profile("mysql")
public class BankTransferService {
    private final BankTransferProperties config;
    private final BankTransferPaymentRepository payments;
    private final OrderRepository orders;
    private final UserRepository users;
    private final VietQrService qr;
    public BankTransferService(BankTransferProperties config, BankTransferPaymentRepository payments,
                               OrderRepository orders, UserRepository users, VietQrService qr) {
        this.config=config; this.payments=payments; this.orders=orders; this.users=users; this.qr=qr;
    }
    public void requireReady() { config.requireReady(); }
    public void requireAdmin(String email) { admin(email); }
    @Transactional
    public BankTransferPayment create(Order order) {
        config.requireReady();
        BankTransferPayment p = new BankTransferPayment();
        p.setOrder(order); p.setBankBin(config.getBin()); p.setBankName(config.getBankName());
        p.setAccountNumber(config.getAccountNumber()); p.setAccountName(config.getAccountName());
        p.setReference(order.getOrderCode().replace("-", "")); p.setAmount(order.getTotalAmount());
        try { qr.payload(p); } catch (IllegalArgumentException | ArithmeticException ex) {
            throw new OrderOperationException("Số tiền chuyển khoản phải là số nguyên VND, lớn hơn 0 và dưới 500 triệu đồng.");
        }
        return payments.save(p);
    }
    private User active(String email) {
        return users.findByEmailIgnoreCase(email).filter(User::isStatus)
            .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Tài khoản không hoạt động"));
    }
    private void admin(String email) {
        if (active(email).getRole()!=Role.ADMIN) throw new org.springframework.security.access.AccessDeniedException("Chỉ Admin được xác nhận thanh toán");
    }
    @Transactional(readOnly=true)
    public BankTransferPayment view(Long orderId,String email) {
        User actor=active(email);
        BankTransferPayment p=payments.findByOrderId(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        if (actor.getRole()!=Role.ADMIN && !p.getOrder().getUser().getId().equals(actor.getId())) throw new OrderNotFoundException(orderId);
        return p;
    }
    @Transactional(readOnly=true)
    public Page<BankTransferPayment> list(String email,PaymentStatus status,int page) {
        admin(email);
        return payments.search(status, PageRequest.of(Math.max(0,page),20,Sort.by(Sort.Direction.DESC,"createdAt","id")));
    }
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void report(Long id,String email) {
        User user=active(email);
        Order order=orders.findByIdForUpdate(id).orElseThrow(() -> new OrderNotFoundException(id));
        if (!order.getUser().getId().equals(user.getId())) throw new OrderNotFoundException(id);
        BankTransferPayment p=payments.findByOrderId(id).orElseThrow(() -> new OrderNotFoundException(id));
        requirePayable(order);
        if (order.getPaymentStatus()==PaymentStatus.PAID || order.getPaymentStatus()==PaymentStatus.REPORTED) return;
        if (order.getPaymentStatus()!=PaymentStatus.UNPAID) throw new OrderOperationException("Trạng thái thanh toán không hợp lệ");
        p.setReportedAt(LocalDateTime.now());
        order.setPaymentStatus(PaymentStatus.REPORTED);
    }
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void confirm(Long id,String email,BigDecimal receivedAmount,String transactionCode) {
        admin(email);
        if (transactionCode==null || !transactionCode.trim().matches("[A-Za-z0-9._/-]{3,100}"))
            throw new OrderOperationException("Mã giao dịch ngân hàng phải có 3–100 ký tự chữ/số hoặc . _ / -");
        String code=transactionCode.trim().toUpperCase(java.util.Locale.ROOT);
        Order order=orders.findByIdForUpdate(id).orElseThrow(() -> new OrderNotFoundException(id));
        BankTransferPayment p=payments.findByOrderId(id).orElseThrow(() -> new OrderNotFoundException(id));
        requirePayable(order);
        if (receivedAmount==null || receivedAmount.compareTo(p.getAmount())!=0 || p.getAmount().compareTo(order.getTotalAmount())!=0)
            throw new OrderOperationException("Số tiền thực nhận chưa khớp đơn hàng. Không xác nhận tự động tiền thiếu/thừa.");
        if (order.getPaymentStatus()==PaymentStatus.PAID) {
            if (code.equals(order.getPaymentTransactionCode())) return;
            throw new OrderOperationException("Đơn đã được xác nhận bằng giao dịch khác; không ghi đè.");
        }
        if (order.getPaymentStatus()!=PaymentStatus.UNPAID && order.getPaymentStatus()!=PaymentStatus.REPORTED)
            throw new OrderOperationException("Trạng thái thanh toán không cho phép xác nhận");
        if (orders.existsByPaymentTransactionCode(code)) throw new OrderOperationException("Mã giao dịch này đã được dùng cho đơn khác");
        order.setPaymentTransactionCode(code); order.setPaidAt(LocalDateTime.now());
        order.setPaymentStatus(PaymentStatus.PAID); p.setConfirmedBy(email);
        orders.flush(); // Database unique protects concurrent confirmation of the same transaction.
    }
    private void requirePayable(Order order) {
        if (order.getPaymentMethod()!=PaymentMethod.BANK_TRANSFER || order.getStatus()==OrderStatus.CANCELLED)
            throw new OrderOperationException("Không thể thanh toán đơn đã hủy hoặc không dùng chuyển khoản QR");
    }
}
