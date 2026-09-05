package com.vegetableshop.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_transfer_payments")
public class BankTransferPayment extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true) private Order order;
    @Column(name = "bank_bin", nullable = false, length = 6) private String bankBin;
    @Column(name = "bank_name", nullable = false, length = 100) private String bankName;
    @Column(name = "account_number", nullable = false, length = 19) private String accountNumber;
    @Column(name = "account_name", nullable = false, length = 100) private String accountName;
    @Column(nullable = false, unique = true, length = 25) private String reference;
    @Column(nullable = false, precision = 15, scale = 2) private BigDecimal amount;
    @Column(name = "reported_at") private LocalDateTime reportedAt;
    @Column(name = "confirmed_by", length = 150) private String confirmedBy;
    public String getStatusVersion() {
        return order.getPaymentStatus()+"|"+order.getStatus();
    }
    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public void setOrder(Order v) { order = v; }
    public String getBankBin() { return bankBin; }
    public void setBankBin(String v) { bankBin = v; }
    public String getBankName() { return bankName; }
    public void setBankName(String v) { bankName = v; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String v) { accountNumber = v; }
    public String getAccountName() { return accountName; }
    public void setAccountName(String v) { accountName = v; }
    public String getReference() { return reference; }
    public void setReference(String v) { reference = v; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { amount = v; }
    public LocalDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(LocalDateTime v) { reportedAt = v; }
    public String getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(String v) { confirmedBy = v; }
    public boolean isAwaitingTransfer() { return order.getStatus() != OrderStatus.CANCELLED
        && (order.getPaymentStatus() == PaymentStatus.UNPAID || order.getPaymentStatus() == PaymentStatus.REPORTED); }
}
