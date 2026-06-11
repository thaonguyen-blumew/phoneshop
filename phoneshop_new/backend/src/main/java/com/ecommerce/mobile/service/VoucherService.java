package com.ecommerce.mobile.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.mobile.entity.Voucher;
import com.ecommerce.mobile.enums.VoucherDiscountType;
import com.ecommerce.mobile.repository.VoucherRepository;

@Service
public class VoucherService {

    public record VoucherApplyResult(Voucher voucher, BigDecimal discountAmount) {
    }

    private final VoucherRepository voucherRepository;

    public VoucherService(VoucherRepository voucherRepository) {
        this.voucherRepository = voucherRepository;
    }

    @Transactional(readOnly = true)
    public VoucherApplyResult resolveVoucher(String voucherCode, BigDecimal subtotal) {
        if (voucherCode == null || voucherCode.isBlank()) {
            return null;
        }

        BigDecimal safeSubtotal = subtotal == null ? BigDecimal.ZERO : subtotal;
        String normalizedCode = voucherCode.trim().toUpperCase();

        Voucher voucher = voucherRepository.findByCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new RuntimeException("Voucher không tồn tại"));

        validateVoucher(voucher, safeSubtotal);
        BigDecimal discountAmount = calculateDiscount(voucher, safeSubtotal);
        return new VoucherApplyResult(voucher, discountAmount);
    }

    @Transactional
    public void consumeVoucher(Voucher voucher) {
        if (voucher == null || voucher.getRemainingQuantity() == null) {
            return;
        }
        if (voucher.getRemainingQuantity() < 1) {
            throw new RuntimeException("Voucher đã hết lượt sử dụng");
        }
        voucher.setRemainingQuantity(voucher.getRemainingQuantity() - 1);
        voucherRepository.save(voucher);
    }

    private void validateVoucher(Voucher voucher, BigDecimal subtotal) {
        if (!Boolean.TRUE.equals(voucher.getIsActive())) {
            throw new RuntimeException("Voucher hiện không khả dụng");
        }
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getStartAt() != null && now.isBefore(voucher.getStartAt())) {
            throw new RuntimeException("Voucher chưa đến thời gian sử dụng");
        }
        if (voucher.getEndAt() != null && now.isAfter(voucher.getEndAt())) {
            throw new RuntimeException("Voucher đã hết hạn");
        }
        if (voucher.getMinOrderAmount() != null && subtotal.compareTo(voucher.getMinOrderAmount()) < 0) {
            throw new RuntimeException("Đơn hàng chưa đạt giá trị tối thiểu để dùng voucher");
        }
        if (voucher.getRemainingQuantity() != null && voucher.getRemainingQuantity() < 1) {
            throw new RuntimeException("Voucher đã hết lượt sử dụng");
        }
    }

    private BigDecimal calculateDiscount(Voucher voucher, BigDecimal subtotal) {
        if (subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;
        if (voucher.getDiscountType() == VoucherDiscountType.PERCENT) {
            discount = subtotal
                    .multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (voucher.getMaxDiscountAmount() != null && discount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
                discount = voucher.getMaxDiscountAmount();
            }
        } else {
            discount = voucher.getDiscountValue();
        }

        if (discount == null || discount.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return discount.min(subtotal);
    }
}
