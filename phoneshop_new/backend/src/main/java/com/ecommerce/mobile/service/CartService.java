package com.ecommerce.mobile.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.mobile.entity.Cart;
import com.ecommerce.mobile.entity.CartItem;
import com.ecommerce.mobile.entity.Customer;
import com.ecommerce.mobile.entity.ProductVariant;
import com.ecommerce.mobile.repository.CartItemRepository;
import com.ecommerce.mobile.repository.CartRepository;
import com.ecommerce.mobile.repository.ProductVariantRepository;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CustomerService customerService;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductVariantRepository productVariantRepository,
                       CustomerService customerService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productVariantRepository = productVariantRepository;
        this.customerService = customerService;
    }

    @Transactional
    public Cart getCartByCustomerEmail(String email) {
        Customer customer = customerService.requireCustomerByEmail(email);
        return cartRepository.findDetailedByCustomerId(customer.getUserID())
                .orElseGet(() -> createEmptyCart(customer));
    }

    @Transactional
    public CartItem addToCart(String customerEmail, Long variantId, Integer quantity) {
        Customer customer = customerService.requireCustomerByEmail(customerEmail);
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể sản phẩm"));

        int addQuantity = quantity == null || quantity < 1 ? 1 : quantity;
        if (variant.getStockQty() != null && addQuantity > variant.getStockQty()) {
            throw new RuntimeException("Vượt quá số lượng tồn kho (" + variant.getStockQty() + ")");
        }

        Cart cart = ensureCart(customer);
        CartItem item = cartItemRepository.findByCartIdAndVariantId(cart.getCartId(), variantId)
                .orElseGet(() -> createNewItem(cart, variant));

        int newQuantity = item.getQuantity() == null ? addQuantity : item.getQuantity() + addQuantity;
        if (variant.getStockQty() != null && newQuantity > variant.getStockQty()) {
            throw new RuntimeException("Vượt quá số lượng tồn kho (" + variant.getStockQty() + ")");
        }

        item.setQuantity(newQuantity);
        item.setUnitPrice(variant.getPrice());
        item.setProductName(buildProductName(variant));
        item.recalculateSubtotal();
        return cartItemRepository.save(item);
    }

    @Transactional
    public CartItem updateItemQuantity(String customerEmail, Long cartItemId, Integer quantity) {
        Customer customer = customerService.requireCustomerByEmail(customerEmail);
        Cart cart = ensureCart(customer);

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy item trong giỏ"));
        if (!item.getCart().getCartId().equals(cart.getCartId())) {
            throw new RuntimeException("Item không thuộc giỏ hàng hiện tại");
        }

        int newQuantity = quantity == null ? 1 : quantity;
        if (newQuantity < 1) {
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
            return null;
        }

        ProductVariant variant = item.getVariant();
        if (variant.getStockQty() != null && newQuantity > variant.getStockQty()) {
            throw new RuntimeException("Vượt quá số lượng tồn kho (" + variant.getStockQty() + ")");
        }

        item.setQuantity(newQuantity);
        item.setUnitPrice(variant.getPrice());
        item.setProductName(buildProductName(variant));
        item.recalculateSubtotal();
        return cartItemRepository.save(item);
    }

    @Transactional
    public Cart removeItem(String customerEmail, Long cartItemId) {
        Customer customer = customerService.requireCustomerByEmail(customerEmail);
        Cart cart = ensureCart(customer);

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy item trong giỏ"));
        if (!item.getCart().getCartId().equals(cart.getCartId())) {
            throw new RuntimeException("Item không thuộc giỏ hàng hiện tại");
        }

        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        return cart;
    }

    @Transactional
    public void clearCart(String customerEmail) {
        Customer customer = customerService.requireCustomerByEmail(customerEmail);
        cartRepository.findDetailedByCustomerId(customer.getUserID())
                .ifPresent(cart -> cartItemRepository.deleteByCartId(cart.getCartId()));
    }

    @Transactional
    public void removeItems(String customerEmail, List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) return;
        Customer customer = customerService.requireCustomerByEmail(customerEmail);
        Cart cart = ensureCart(customer);
        List<CartItem> toRemove = cart.getItems().stream()
                .filter(item -> itemIds.contains(item.getCartItemId()))
                .toList();
        cart.getItems().removeAll(toRemove);
        cartItemRepository.deleteAll(toRemove);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotal(Cart cart) {
        if (cart == null || cart.getItems() == null) {
            return BigDecimal.ZERO;
        }
        return cart.getItems().stream()
                .filter(item -> item.getUnitPrice() != null && item.getQuantity() != null)
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Cart ensureCart(Customer customer) {
        return cartRepository.findDetailedByCustomerId(customer.getUserID())
                .orElseGet(() -> createEmptyCart(customer));
    }

    private Cart createEmptyCart(Customer customer) {
        Cart cart = new Cart();
        cart.setCustomer(customer);
        return cartRepository.save(cart);
    }

    private CartItem createNewItem(Cart cart, ProductVariant variant) {
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setVariant(variant);
        item.setQuantity(0);
        item.setUnitPrice(variant.getPrice());
        item.setProductName(buildProductName(variant));
        item.recalculateSubtotal();
        return item;
    }

    private String buildProductName(ProductVariant variant) {
        String productName = variant.getProduct() != null && variant.getProduct().getName() != null
                ? variant.getProduct().getName()
                : "Sản phẩm";
        String storage = variant.getStorageGb() != null ? " - " + variant.getStorageGb() + "GB" : "";
        return productName + storage;
    }

    @Transactional(readOnly = true)
    public List<CartItem> sortItemsByAddedAt(Cart cart) {
        if (cart == null || cart.getItems() == null) {
            return List.of();
        }
        return cart.getItems().stream()
                .sorted(Comparator.comparing(CartItem::getAddedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }
}
