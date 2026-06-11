-- ============================================================
-- FILE: add_stockout_variants.sql
-- MỤC ĐÍCH: Thêm một số biến thể sản phẩm với stock_qty = 0
--           vào bảng product_variants (OLTP: phoneshop_db)
--           để dashboard Tồn kho có dữ liệu "Hết hàng" thực tế
--
-- Cách chạy:
--   mysql -u root -p phoneshop_db < add_stockout_variants.sql
-- hoặc chạy từng block trong MySQL Workbench / DBeaver
-- ============================================================

USE phoneshop_db;

-- ============================================================
-- BƯỚC 1: Xem các variant đang tồn > 0 để chọn set một số về 0
-- ============================================================
-- (Chạy SELECT này trước để xác nhận variant_id)
/*
SELECT pv.variant_id, p.name AS product_name, p.brand,
       pv.color, pv.storage_gb, pv.stock_qty, pv.price
FROM product_variants pv
JOIN products p ON p.product_id = pv.product_id
WHERE pv.stock_qty > 0
ORDER BY p.brand, pv.stock_qty ASC
LIMIT 30;
*/

-- ============================================================
-- BƯỚC 2: Set stock_qty = 0 cho một số biến thể cụ thể
--
-- Chiến lược chọn:
--   - Lấy các SKU giá cao (Flagship) hoặc màu ít phổ biến
--   - Tạo tình huống thực tế: đã bán hết
-- ============================================================

-- Option A: Dùng subquery để tự động chọn 10 variant ngẫu nhiên
--           (giữ lại ít nhất 1 variant/product để website vẫn chạy)
UPDATE product_variants
SET stock_qty = 0
WHERE variant_id IN (
    -- Lấy 10 variant có stock thấp nhất (dễ bán hết nhất)
    SELECT variant_id FROM (
        SELECT pv.variant_id
        FROM product_variants pv
        JOIN products p ON p.product_id = pv.product_id
        WHERE pv.stock_qty > 0
          AND pv.stock_qty <= 15          -- đang có ít hàng
          -- Đảm bảo product vẫn còn variant khác
          AND (
              SELECT COUNT(*) FROM product_variants pv2
              WHERE pv2.product_id = pv.product_id
                AND pv2.stock_qty > 0
                AND pv2.variant_id != pv.variant_id
          ) >= 1
        ORDER BY pv.stock_qty ASC
        LIMIT 10
    ) AS sub
);

-- ============================================================
-- BƯỚC 3: Thêm biến thể mới với stock_qty = 0
--         (màu/storage đã ngừng kinh doanh)
-- ============================================================

-- Lấy product_id của một số sản phẩm phổ biến để thêm variant hết hàng
-- (Bỏ comment và chỉnh product_id phù hợp với data thực của bạn)

/*
-- Ví dụ: Thêm variant "Gold 512GB" đã hết hàng cho sản phẩm có product_id=1
INSERT INTO product_variants
    (product_id, color, storage_gb, price, import_price, stock_qty, sku)
SELECT
    product_id,
    'Gold'  AS color,
    512     AS storage_gb,
    price * 1.1 AS price,          -- giá cao hơn 10%
    import_price * 1.08,
    0       AS stock_qty,          -- HẾT HÀNG
    CONCAT(sku, '-GOLD-512-OOS')
FROM product_variants
WHERE variant_id = 1               -- thay bằng variant_id thực của bạn
LIMIT 1;
*/

-- ============================================================
-- BƯỚC 4: Tạo bảng tóm tắt sau khi cập nhật
-- ============================================================
SELECT
    p.brand,
    COUNT(pv.variant_id)                                    AS total_sku,
    SUM(CASE WHEN pv.stock_qty = 0  THEN 1 ELSE 0 END)     AS het_hang,
    SUM(CASE WHEN pv.stock_qty <= 5
              AND pv.stock_qty > 0 THEN 1 ELSE 0 END)      AS sap_het,
    SUM(CASE WHEN pv.stock_qty > 5  THEN 1 ELSE 0 END)     AS du_hang,
    SUM(pv.stock_qty)                                       AS tong_qty,
    ROUND(SUM(pv.stock_qty * pv.import_price))              AS gia_tri_ton
FROM product_variants pv
JOIN products p ON p.product_id = pv.product_id
GROUP BY p.brand
ORDER BY gia_tri_ton DESC;

-- ============================================================
-- BƯỚC 5: Kiểm tra danh sách hết hàng
-- ============================================================
SELECT
    pv.variant_id,
    p.name          AS product_name,
    p.brand,
    pv.color,
    pv.storage_gb,
    pv.stock_qty,
    pv.price
FROM product_variants pv
JOIN products p ON p.product_id = pv.product_id
WHERE pv.stock_qty = 0
ORDER BY p.brand, p.name;
