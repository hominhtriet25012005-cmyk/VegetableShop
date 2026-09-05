package com.vegetableshop.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "voucher_scopes", uniqueConstraints = @UniqueConstraint(
    name = "uk_voucher_scope", columnNames = {"voucher_id", "scope_type", "target_id"}))
public class VoucherScope {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;
    @Enumerated(EnumType.STRING) @Column(name = "scope_type", nullable = false, length = 20)
    private VoucherScopeType scopeType;
    @Column(name = "target_id") private Long targetId;
    protected VoucherScope() {}
    public VoucherScope(Voucher voucher, VoucherScopeType scopeType, Long targetId) {
        this.voucher = voucher; this.scopeType = scopeType; this.targetId = targetId;
    }
    public Long getId() { return id; }
    public Voucher getVoucher() { return voucher; }
    public VoucherScopeType getScopeType() { return scopeType; }
    public Long getTargetId() { return targetId; }
}
