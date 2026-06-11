# PhoneShop BI CSDL Modeling

Tài liệu này mô tả mô hình CSDL phân tích mục tiêu của dự án BI + Data Warehouse.

Đây là model bám theo ERD bạn đã đưa:

- `Dim_Date`
- `Dim_Product`
- `Dim_Customer`
- `Fact_Sales`
- `Fact_Payments`
- `Fact_Inventory`
- `Fact_Reviews`

Mục tiêu của model:

- giữ nguyên OLTP hiện có
- không reset ID hay foreign key
- tách lớp giao dịch và lớp phân tích rõ ràng
- hỗ trợ Power BI / dashboard mà không cần join lòng vòng qua OLTP

---

## 1. Kiến trúc logic

```text
MySQL OLTP + Excel files
        |
        v
     Staging
        |
        v
  Data Warehouse
        |
        v
  Power BI / Dashboard
```

OLTP giữ dữ liệu vận hành.
Staging dùng để chuẩn hóa và map nguồn.
DWH dùng cho phân tích.

---

## 2. Phạm vi dữ liệu

### Nguồn OLTP

- `users`
- `roles`
- `categories`
- `products`
- `product_variants`
- `orders`
- `order_items`
- `payments`
- `shipments`
- `reviews`
- `address`

### Nguồn Excel

- 2 file Excel theo đề bài, đẩy vào staging trước khi vào DWH

---

## 3. Grain của từng bảng

- `Dim_Date`: 1 dòng cho 1 ngày
- `Dim_Product`: 1 dòng cho 1 variant sản phẩm
- `Dim_Customer`: 1 dòng cho 1 khách hàng
- `Fact_Sales`: 1 dòng cho 1 dòng bán hàng
- `Fact_Payments`: 1 dòng cho 1 payment transaction
- `Fact_Inventory`: 1 dòng cho 1 snapshot tồn kho theo ngày và sản phẩm
- `Fact_Reviews`: 1 dòng cho 1 review

---

## 4. Dimension tables

### 4.1. `Dim_Date`

Mục đích:
- lọc theo ngày, tháng, quý, năm
- phân tích xu hướng theo thời gian

Gợi ý cột:
- `date_key` PK
- `full_date`
- `day`
- `month`
- `quarter`
- `year`
- `day_of_week`
- `day_name`
- `is_weekend`

### 4.2. `Dim_Product`

Mục đích:
- phân tích theo sản phẩm, variant, màu, dung lượng, danh mục
- giữ đúng tinh thần: `product = base model`, `color = variant attribute`

Gợi ý cột:
- `product_key` PK
- `variant_id`
- `product_name`
- `brand`
- `category_name`
- `storage`
- `color`
- `price_tier`
- `current_price`
- `import_price`
- `current_stock`
- `sku`

Nguyên tắc:
- tên sản phẩm phải là base model
- màu nằm ở cột `color`
- nếu tên gốc chứa màu thì backfill / ETL phải tách ra trước khi vào DWH

### 4.3. `Dim_Customer`

Mục đích:
- phân tích theo khách hàng, khu vực, phân khúc

Gợi ý cột:
- `customer_key` PK
- `user_id`
- `full_name`
- `email`
- `province_city`
- `customer_segment`

`customer_segment` có thể suy ra từ hành vi mua hàng, tần suất, AOV, hoặc rule deterministic ở staging.

---

## 5. Fact tables

### 5.1. `Fact_Sales`

Mục đích:
- đo doanh thu
- đo lợi nhuận
- đo hiệu quả bán hàng theo sản phẩm và khách hàng

Gợi ý cột:
- `sales_key` PK
- `date_key` FK -> `Dim_Date`
- `product_key` FK -> `Dim_Product`
- `customer_key` FK -> `Dim_Customer`
- `quantity`
- `unit_price`
- `import_price`
- `gross_profit`
- `net_profit`

Business rule:
- đây là fact ở mức dòng bán hàng
- mỗi row phản ánh một giao dịch bán của một variant trong một thời điểm

### 5.2. `Fact_Payments`

Mục đích:
- theo dõi thanh toán
- theo dõi phí gateway
- đối soát tiền thực nhận

Gợi ý cột:
- `payment_key` PK
- `date_key` FK -> `Dim_Date`
- `customer_key` FK -> `Dim_Customer`
- `order_id`
- `amount`
- `gateway_fee`
- `net_received`
- `reconciliation_status`

Business rule:
- `order_id` là degenerate dimension
- `net_received = amount - gateway_fee`
- `reconciliation_status` dùng cho dashboard đối soát

### 5.3. `Fact_Inventory`

Mục đích:
- theo dõi tồn kho theo ngày
- tính giá trị tồn kho

Gợi ý cột:
- `inventory_key` PK
- `date_key` FK -> `Dim_Date`
- `product_key` FK -> `Dim_Product`
- `quantity_on_hand`
- `inventory_value`

Business rule:
- snapshot tồn kho theo thời điểm hoặc theo cuối ngày
- `inventory_value = quantity_on_hand * import_price`

### 5.4. `Fact_Reviews`

Mục đích:
- phân tích rating
- đo chất lượng sản phẩm / CSAT

Gợi ý cột:
- `review_key` PK
- `date_key` FK -> `Dim_Date`
- `product_key` FK -> `Dim_Product`
- `customer_key` FK -> `Dim_Customer`
- `rating_stars`
- `is_verified`

Business rule:
- review chỉ được xem là verified nếu khớp lịch sử mua hàng hợp lệ

---

## 6. Quan hệ giữa các bảng

- `Dim_Date[date_key]` -> `Fact_Sales[date_key]`
- `Dim_Date[date_key]` -> `Fact_Payments[date_key]`
- `Dim_Date[date_key]` -> `Fact_Inventory[date_key]`
- `Dim_Date[date_key]` -> `Fact_Reviews[date_key]`
- `Dim_Product[product_key]` -> `Fact_Sales[product_key]`
- `Dim_Product[product_key]` -> `Fact_Payments` nếu cần drill từ thanh toán sang sản phẩm qua order mapping
- `Dim_Product[product_key]` -> `Fact_Inventory[product_key]`
- `Dim_Product[product_key]` -> `Fact_Reviews[product_key]`
- `Dim_Customer[customer_key]` -> `Fact_Sales[customer_key]`
- `Dim_Customer[customer_key]` -> `Fact_Payments[customer_key]`
- `Dim_Customer[customer_key]` -> `Fact_Reviews[customer_key]`

Lưu ý:
- `Fact_Payments` có thể không cần FK trực tiếp sang `Dim_Product`
- nếu cần phân tích payment theo sản phẩm thì đi qua order mapping ở staging

---

## 7. Xử lý chuẩn hóa sản phẩm

Quy tắc chuẩn hóa:

- `products.name` lưu base model
- `product_variants.color` lưu màu variant
- `product_variants.storage_gb` lưu dung lượng
- không nhét màu vào tên sản phẩm trong DWH

Ví dụ:

- raw source: `iPhone 15 Pro Max Titan Đen`
- normalized product: `iPhone 15 Pro Max`
- normalized color: `Titan Đen`

---

## 8. Mapping nghiệp vụ sang BI

Model này hỗ trợ các KPI sau:

- doanh thu thuần
- lợi nhuận gộp
- lợi nhuận ròng
- tỷ lệ hoàn tất thanh toán
- tổng tiền thực nhận
- giá trị tồn kho
- số lượng tồn kho
- average rating
- tỷ lệ review verified
- top product / top brand / top category

---

## 9. Ghi chú triển khai

Trong repo hiện tại có thể vẫn còn một số file DWH cũ dùng mô hình giản lược.
Tài liệu này là bản model mục tiêu để thống nhất thiết kế BI.

Khi bắt đầu đồng bộ code, thứ tự hợp lý là:

1. staging
2. normalization
3. ETL
4. DWH schema
5. Power BI measure

