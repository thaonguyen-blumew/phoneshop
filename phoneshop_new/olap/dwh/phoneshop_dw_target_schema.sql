CREATE DATABASE IF NOT EXISTS phoneshop_dwh
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE phoneshop_dwh;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS Fact_Reviews;
DROP TABLE IF EXISTS Fact_Inventory;
DROP TABLE IF EXISTS Fact_Payments;
DROP TABLE IF EXISTS Fact_Sales;
DROP TABLE IF EXISTS Dim_Customer;
DROP TABLE IF EXISTS Dim_Product;
DROP TABLE IF EXISTS Dim_Date;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE Dim_Date (
    date_key     INT         NOT NULL,
    full_date    DATE        NOT NULL,
    day          TINYINT     NOT NULL,
    month        TINYINT     NOT NULL,
    quarter      TINYINT     NOT NULL,
    year         SMALLINT    NOT NULL,
    is_weekend   BOOLEAN     NOT NULL,
    PRIMARY KEY (date_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE Dim_Product (
    product_key    BIGINT       NOT NULL,
    variant_id     BIGINT       NOT NULL,
    product_name   VARCHAR(255) NOT NULL,
    brand          VARCHAR(100),
    category_name  VARCHAR(100),
    storage        VARCHAR(20),
    color          VARCHAR(50),
    price_tier     VARCHAR(20),
    PRIMARY KEY (product_key),
    UNIQUE KEY uq_variant_id (variant_id),
    INDEX idx_brand (brand),
    INDEX idx_category_name (category_name),
    INDEX idx_color (color)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE Dim_Customer (
    customer_key     BIGINT       NOT NULL,
    user_id          BIGINT       NOT NULL,
    full_name        VARCHAR(255) NOT NULL,
    email            VARCHAR(255) NOT NULL,
    province_city    VARCHAR(100),
    customer_segment VARCHAR(30)  NOT NULL,
    PRIMARY KEY (customer_key),
    UNIQUE KEY uq_user_id (user_id),
    INDEX idx_segment (customer_segment),
    INDEX idx_city (province_city)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE Fact_Sales (
    sales_key       BIGINT AUTO_INCREMENT PRIMARY KEY,
    date_key        INT           NOT NULL,
    product_key     BIGINT        NOT NULL,
    customer_key    BIGINT        NOT NULL,
    quantity        INT           NOT NULL,
    unit_price      DECIMAL(19,2) NOT NULL,
    import_price    DECIMAL(19,2) NOT NULL,
    gross_profit    DECIMAL(19,2) NOT NULL,
    net_profit      DECIMAL(19,2) NOT NULL,
    INDEX idx_date (date_key),
    INDEX idx_product (product_key),
    INDEX idx_customer (customer_key),
    INDEX idx_sales_prod_date (product_key, date_key),
    CONSTRAINT fk_fact_sales_date
        FOREIGN KEY (date_key) REFERENCES Dim_Date(date_key),
    CONSTRAINT fk_fact_sales_product
        FOREIGN KEY (product_key) REFERENCES Dim_Product(product_key),
    CONSTRAINT fk_fact_sales_customer
        FOREIGN KEY (customer_key) REFERENCES Dim_Customer(customer_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE Fact_Payments (
    payment_key          BIGINT AUTO_INCREMENT PRIMARY KEY,
    date_key             INT           NOT NULL,
    payment_time         DATETIME      NOT NULL,
    customer_key         BIGINT        NOT NULL,
    order_id             BIGINT        NOT NULL,
    amount               DECIMAL(19,2) NOT NULL,
    gateway_fee          DECIMAL(19,2) NOT NULL,
    net_received         DECIMAL(19,2) NOT NULL,
    reconciliation_status VARCHAR(30)  NOT NULL,
    INDEX idx_date (date_key),
    INDEX idx_customer (customer_key),
    INDEX idx_order (order_id),
    INDEX idx_recon (reconciliation_status),
    CONSTRAINT fk_fact_payments_date
        FOREIGN KEY (date_key) REFERENCES Dim_Date(date_key),
    CONSTRAINT fk_fact_payments_customer
        FOREIGN KEY (customer_key) REFERENCES Dim_Customer(customer_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE Fact_Inventory (
    inventory_key      BIGINT AUTO_INCREMENT PRIMARY KEY,
    date_key           INT           NOT NULL,
    product_key        BIGINT        NOT NULL,
    quantity_on_hand   INT           NOT NULL,
    inventory_value    DECIMAL(19,2) NOT NULL,
    INDEX idx_date (date_key),
    INDEX idx_product (product_key),
    INDEX idx_prod_date (product_key, date_key),
    CONSTRAINT fk_fact_inventory_date
        FOREIGN KEY (date_key) REFERENCES Dim_Date(date_key),
    CONSTRAINT fk_fact_inventory_product
        FOREIGN KEY (product_key) REFERENCES Dim_Product(product_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE Fact_Reviews (
    review_key       BIGINT AUTO_INCREMENT PRIMARY KEY,
    date_key         INT          NOT NULL,
    product_key      BIGINT       NOT NULL,
    customer_key     BIGINT       NOT NULL,
    rating_stars     TINYINT      NOT NULL,
    is_verified      BOOLEAN      NOT NULL,
    INDEX idx_date (date_key),
    INDEX idx_product (product_key),
    INDEX idx_customer (customer_key),
    CONSTRAINT fk_fact_reviews_date
        FOREIGN KEY (date_key) REFERENCES Dim_Date(date_key),
    CONSTRAINT fk_fact_reviews_product
        FOREIGN KEY (product_key) REFERENCES Dim_Product(product_key),
    CONSTRAINT fk_fact_reviews_customer
        FOREIGN KEY (customer_key) REFERENCES Dim_Customer(customer_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
