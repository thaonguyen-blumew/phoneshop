package com.ecommerce.mobile.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.hibernate.Hibernate;

import com.ecommerce.mobile.entity.Product;
import com.ecommerce.mobile.entity.ProductImage;
import com.ecommerce.mobile.entity.ProductVariant;

public final class ProductDisplayHelper {

    private static final String FALLBACK_IMAGE = "/assets/img/about-hero.svg";

    private ProductDisplayHelper() {
    }

    /**
     * URL hiển thị trên thẻ sản phẩm (ưu tiên ảnh primary, sau đó ảnh đầu tiên).
     */
    public static String cardImageUrl(Product product) {
        String u = findPrimaryOrFirstImageUrl(product);
        return u != null ? u.trim() : FALLBACK_IMAGE;
    }

    private static String findPrimaryOrFirstImageUrl(Product product) {
        if (product == null || !Hibernate.isPropertyInitialized(product, "variants")) {
            return null;
        }
        if (product.getVariants() == null) {
            return null;
        }
        for (ProductVariant v : product.getVariants()) {
            if (v == null || !Hibernate.isPropertyInitialized(v, "images") || v.getImages() == null) {
                continue;
            }
            for (ProductImage img : v.getImages()) {
                if (Boolean.TRUE.equals(img.getIsPrimary()) && hasText(img.getUrl())) {
                    return img.getUrl();
                }
            }
        }
        for (ProductVariant v : product.getVariants()) {
            if (v.getImages() == null) {
                continue;
            }
            for (ProductImage img : v.getImages()) {
                if (hasText(img.getUrl())) {
                    return img.getUrl();
                }
            }
        }
        return null;
    }

    /**
     * Giá thấp nhất trong các biến thể (để hiển thị “từ … ₫”).
     */
    public static BigDecimal minPrice(Product product) {
        if (product == null || product.getVariants() == null || product.getVariants().isEmpty()) {
            return null;
        }
        BigDecimal min = null;
        for (ProductVariant v : product.getVariants()) {
            if (v.getPrice() == null) {
                continue;
            }
            if (min == null || v.getPrice().compareTo(min) < 0) {
                min = v.getPrice();
            }
        }
        return min;
    }

    /**
     * Ảnh gallery: gom URL duy nhất, giữ thứ tự gặp.
     */
    public static List<String> collectImageUrls(Product product) {
        Set<String> ordered = new LinkedHashSet<>();
        if (product != null && Hibernate.isPropertyInitialized(product, "variants") && product.getVariants() != null) {
            for (ProductVariant v : product.getVariants()) {
                if (v == null || !Hibernate.isPropertyInitialized(v, "images") || v.getImages() == null) {
                    continue;
                }
                for (ProductImage img : v.getImages()) {
                    if (hasText(img.getUrl())) {
                        ordered.add(img.getUrl().trim());
                    }
                }
            }
        }
        return new ArrayList<>(ordered);
    }

    public static String formatVnd(BigDecimal amount) {
        if (amount == null) {
            return "—";
        }
        return String.format(Locale.forLanguageTag("vi-VN"), "%,.0f ₫", amount);
    }

    public static boolean isAbsoluteUrl(String url) {
        if (!hasText(url)) {
            return false;
        }
        String u = url.trim().toLowerCase(Locale.ROOT);
        return u.startsWith("http://") || u.startsWith("https://") || u.startsWith("//");
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
