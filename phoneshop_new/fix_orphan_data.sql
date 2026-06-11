-- ============================================================
-- BƯỚC 1: Kiểm tra variant_id nào bị mất (orphaned references)
-- ============================================================

-- Xem cart_items đang tham chiếu đến variant không tồn tại
SELECT ci.cart_item_id, ci.variant_id, ci.product_name
FROM cart_items ci
LEFT JOIN product_variants pv ON ci.variant_id = pv.variant_id
WHERE pv.variant_id IS NULL;

-- Xem order_items đang tham chiếu đến variant không tồn tại
SELECT oi.order_item_id, oi.variant_id, oi.product_name
FROM order_items oi
LEFT JOIN product_variants pv ON oi.variant_id = pv.variant_id
WHERE pv.variant_id IS NULL;

-- ============================================================
-- BƯỚC 2: Xóa cart_items mồ côi (không còn variant tương ứng)
-- ============================================================
DELETE FROM cart_items
WHERE variant_id NOT IN (SELECT variant_id FROM product_variants);

-- ============================================================
-- BƯỚC 3: Kiểm tra product_images mồ côi
-- ============================================================
SELECT pi.image_id, pi.variant_id
FROM product_images pi
LEFT JOIN product_variants pv ON pi.variant_id = pv.variant_id
WHERE pv.variant_id IS NULL;

-- Xóa product_images mồ côi
DELETE FROM product_images
WHERE variant_id NOT IN (SELECT variant_id FROM product_variants);

-- ============================================================
-- BƯỚC 4: Xác nhận không còn orphan
-- ============================================================
SELECT 'cart_items orphans' AS check_type, COUNT(*) AS count
FROM cart_items ci
LEFT JOIN product_variants pv ON ci.variant_id = pv.variant_id
WHERE pv.variant_id IS NULL
UNION ALL
SELECT 'order_items orphans', COUNT(*)
FROM order_items oi
LEFT JOIN product_variants pv ON oi.variant_id = pv.variant_id
WHERE pv.variant_id IS NULL
UNION ALL
SELECT 'product_images orphans', COUNT(*)
FROM product_images pi
LEFT JOIN product_variants pv ON pi.variant_id = pv.variant_id
WHERE pv.variant_id IS NULL;
