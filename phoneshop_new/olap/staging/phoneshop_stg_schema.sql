CREATE DATABASE IF NOT EXISTS phoneshop_stg
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE phoneshop_stg;

DROP TABLE IF EXISTS stg_review_source;
DROP TABLE IF EXISTS stg_inventory_snapshot;
DROP TABLE IF EXISTS stg_payment_statement;
DROP TABLE IF EXISTS stg_payment_source;
DROP TABLE IF EXISTS stg_order_lines;
DROP TABLE IF EXISTS stg_order_header;
DROP TABLE IF EXISTS stg_customer_catalog;
DROP TABLE IF EXISTS stg_product_catalog;

CREATE TABLE stg_product_catalog (
    variant_id         BIGINT       NOT NULL,
    product_id         BIGINT       NOT NULL,
    raw_product_name   VARCHAR(255)  NOT NULL,
    product_name       VARCHAR(255)  NOT NULL,
    brand              VARCHAR(100),
    category_name      VARCHAR(100),
    parent_category    VARCHAR(100),
    storage_gb         INT,
    color              VARCHAR(50),
    price              DECIMAL(19,2),
    import_price       DECIMAL(19,2),
    stock_qty          INT,
    sku                VARCHAR(255),
    product_status     VARCHAR(20),
    price_tier         VARCHAR(20),
    source_created_at  DATETIME,
    PRIMARY KEY (variant_id),
    INDEX idx_product_id (product_id),
    INDEX idx_brand (brand),
    INDEX idx_category (category_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE stg_customer_catalog (
    customer_key       BIGINT       NOT NULL,
    user_id            BIGINT       NOT NULL,
    full_name          VARCHAR(255) NOT NULL,
    email              VARCHAR(255) NOT NULL,
    province_city      VARCHAR(100),
    customer_segment   VARCHAR(30)  NOT NULL,
    role_name          VARCHAR(50),
    registered_at      DATETIME,
    order_count        INT          NOT NULL DEFAULT 0,
    total_spend        DECIMAL(19,2) NOT NULL DEFAULT 0,
    PRIMARY KEY (customer_key),
    UNIQUE KEY uq_customer_user (user_id),
    INDEX idx_customer_segment (customer_segment),
    INDEX idx_province_city (province_city)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE stg_order_header (
    order_id           BIGINT       NOT NULL,
    order_code         VARCHAR(40)  NOT NULL,
    customer_key       BIGINT       NOT NULL,
    order_status       VARCHAR(20)  NOT NULL,
    payment_method     VARCHAR(20)  NOT NULL,
    total_amount       DECIMAL(19,2) NOT NULL,
    shipping_fee       DECIMAL(19,2) NOT NULL,
    discount_amount    DECIMAL(19,2) NOT NULL,
    applied_voucher    VARCHAR(50),
    shipping_name      VARCHAR(100) NOT NULL,
    shipping_phone     VARCHAR(20)  NOT NULL,
    shipping_address   VARCHAR(255) NOT NULL,
    shipping_ward      VARCHAR(100),
    shipping_district  VARCHAR(100),
    shipping_city      VARCHAR(100) NOT NULL,
    created_at         DATETIME,
    updated_at         DATETIME,
    confirmed_at       DATETIME,
    shipping_at        DATETIME,
    delivered_at       DATETIME,
    cancelled_at       DATETIME,
    PRIMARY KEY (order_id),
    UNIQUE KEY uq_order_code (order_code),
    INDEX idx_customer_key (customer_key),
    INDEX idx_status (order_status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE stg_order_lines (
    order_item_id      BIGINT       NOT NULL,
    order_id           BIGINT       NOT NULL,
    order_code         VARCHAR(40)  NOT NULL,
    customer_key       BIGINT       NOT NULL,
    variant_id         BIGINT       NOT NULL,
    quantity           INT          NOT NULL,
    unit_price         DECIMAL(19,2) NOT NULL,
    import_price       DECIMAL(19,2) NOT NULL,
    line_revenue       DECIMAL(19,2) NOT NULL,
    line_cogs          DECIMAL(19,2) NOT NULL,
    line_gross_profit  DECIMAL(19,2) NOT NULL,
    line_discount      DECIMAL(19,2) NOT NULL,
    line_shipping_fee  DECIMAL(19,2) NOT NULL,
    line_net_profit    DECIMAL(19,2) NOT NULL,
    order_status       VARCHAR(20)  NOT NULL,
    payment_method     VARCHAR(20)  NOT NULL,
    created_at         DATETIME,
    lead_time_days     DECIMAL(5,1),
    PRIMARY KEY (order_item_id),
    INDEX idx_order_id (order_id),
    INDEX idx_customer_key (customer_key),
    INDEX idx_variant_id (variant_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE stg_payment_source (
    payment_id             BIGINT       NOT NULL,
    order_id               BIGINT       NOT NULL,
    customer_key           BIGINT       NOT NULL,
    transaction_ref        VARCHAR(100),
    method                 VARCHAR(20)  NOT NULL,
    status                 VARCHAR(20)  NOT NULL,
    paid_at                DATETIME,
    created_at             DATETIME,
    updated_at             DATETIME,
    PRIMARY KEY (payment_id),
    UNIQUE KEY uq_transaction_ref (transaction_ref),
    INDEX idx_order_id (order_id),
    INDEX idx_customer_key (customer_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE stg_payment_statement (
    statement_key      BIGINT AUTO_INCREMENT PRIMARY KEY,
    txn_ref            VARCHAR(100) NOT NULL,
    order_id           BIGINT       NOT NULL,
    bank_amount        DECIMAL(19,2) NOT NULL,
    bank_status        VARCHAR(20)  NOT NULL,
    pay_date           DATETIME     NOT NULL,
    INDEX idx_txn_ref (txn_ref),
    INDEX idx_order_id (order_id),
    INDEX idx_pay_date (pay_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE stg_inventory_snapshot (
    date_key           INT          NOT NULL,
    variant_id         BIGINT       NOT NULL,
    quantity_on_hand   INT          NOT NULL,
    PRIMARY KEY (date_key, variant_id),
    INDEX idx_variant_id (variant_id),
    INDEX idx_date_key (date_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE stg_review_source (
    review_id          BIGINT       NOT NULL,
    customer_key       BIGINT       NOT NULL,
    product_key        BIGINT       NOT NULL,
    rating_stars       TINYINT      NOT NULL,
    review_created_at  DATETIME,
    is_verified        BOOLEAN      NOT NULL,
    comment            VARCHAR(1000),
    PRIMARY KEY (review_id),
    INDEX idx_customer_key (customer_key),
    INDEX idx_product_key (product_key),
    INDEX idx_created_at (review_created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
