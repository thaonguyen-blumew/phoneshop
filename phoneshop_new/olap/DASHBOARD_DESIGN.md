# PhoneShop OLAP Dashboard Design

Tài liệu này mô tả hướng thiết kế dashboard trực tiếp bằng Chart.js trên nền Data Warehouse, không dùng Power BI và không bắt buộc thiết kế theo API JSON riêng.

Luồng đề xuất:

```text
phoneshop_dwh
        |
        v
Spring Boot service query DWH
        |
        v
Thymeleaf model
        |
        v
Chart.js dashboard
```

Dashboard nên trả lời câu hỏi nghiệp vụ trước, rồi mới chọn biểu đồ.

---

## 1. Bộ lọc chung

Các trang dashboard nên dùng chung các bộ lọc sau:

- Ngày bắt đầu
- Ngày kết thúc
- Chế độ thời gian: ngày, tuần, tháng, quý, năm
- Brand
- Category
- Price tier
- Customer segment

Thứ tự ưu tiên báo cáo thời gian:

1. Theo ngày
2. Theo tuần
3. Theo tháng
4. Theo quý / năm
5. Theo mốc thời gian tùy chọn

Lý do:

- ngày: xem vận hành và biến động gần nhất
- tuần: phù hợp nhịp quản lý bán hàng
- tháng: phù hợp tổng kết kinh doanh
- khoảng tùy chọn: phục vụ bài toán so sánh chiến dịch / kỳ đặc biệt

---

## 2. Trang Tổng Quan Điều Hành

Mục tiêu: cho quản lý biết tình hình chung trong 30 giây.

Câu hỏi cần trả lời:

- Doanh thu hiện tại là bao nhiêu?
- Lợi nhuận gộp và lợi nhuận ròng là bao nhiêu?
- Biên lợi nhuận có tốt không?
- Bán được bao nhiêu sản phẩm?
- Thanh toán có khớp không?
- Tồn kho đang giữ bao nhiêu vốn?
- Rating trung bình có ổn không?

KPI cards:

- Total Revenue
- Gross Profit
- Net Profit
- Gross Margin %
- Net Margin %
- Units Sold
- Average Order Value
- Inventory Value
- Payment Match Rate %
- Average Rating

Biểu đồ:

- Line chart: doanh thu, lợi nhuận theo ngày / tuần / tháng
- Bar chart: top sản phẩm theo doanh thu
- Doughnut chart: trạng thái đối soát thanh toán
- Bar chart: doanh thu theo brand

Bảng cảnh báo:

- Sản phẩm tồn thấp
- Sản phẩm bán chậm
- Payment mismatch
- Sản phẩm rating thấp

---

## 3. Phân Tích Bán Hàng

Mục tiêu: hiểu bán được gì, bán cho ai, bán lúc nào, và hiệu quả ra sao.

Câu hỏi cần trả lời:

- Doanh thu tăng hay giảm theo thời gian?
- Brand nào đóng góp doanh thu lớn nhất?
- Category nào bán tốt nhất?
- Sản phẩm nào bán chạy?
- Sản phẩm nào bán chậm?
- Nhóm giá nào mang lại doanh thu / lợi nhuận tốt nhất?
- Khách hàng phân khúc nào mua nhiều nhất?

KPI:

- Total Revenue
- Units Sold
- Gross Profit
- Net Profit
- Average Selling Price
- Average Order Value
- Revenue per Customer

Biểu đồ:

- Line chart: revenue / profit trend
- Bar chart: revenue by brand
- Bar chart: revenue by category
- Horizontal bar: top products by revenue
- Horizontal bar: top products by quantity
- Horizontal bar: slow-moving products
- Stacked bar: revenue by price tier over time
- Bar chart: revenue by customer segment

Định nghĩa sản phẩm bán chậm:

```text
Sản phẩm bán chậm = sản phẩm có tồn kho > 0 nhưng số lượng bán thấp trong một khoảng thời gian xác định.
```

Gợi ý rule ban đầu:

```text
quantity_on_hand > 0
AND units_sold trong 30 ngày gần nhất <= 2
```

Nếu muốn chặt hơn:

```text
slow_moving_score = quantity_on_hand / GREATEST(units_sold_last_30_days, 1)
```

Sản phẩm có score càng cao thì càng dễ bị kẹt vốn.

---

## 4. Báo Cáo Tồn Kho / Nguyên Vật Liệu

Trong ngữ cảnh PhoneShop, "nguyên vật liệu" nên hiểu là hàng tồn kho, linh kiện/phụ kiện nhập về, hoặc vốn hàng hóa đang nằm trong kho. Nếu hệ thống chưa quản lý linh kiện sản xuất riêng, dashboard nên gọi là "Tồn kho & hàng hóa".

Câu hỏi cần trả lời:

- Tổng tồn kho hiện tại là bao nhiêu?
- Tổng giá trị tồn kho là bao nhiêu?
- Sản phẩm nào sắp hết hàng?
- Sản phẩm nào tồn nhiều nhưng bán chậm?
- Brand/category nào đang chiếm nhiều vốn tồn kho?
- Có mặt hàng nào nên nhập thêm không?
- Có mặt hàng nào nên giảm giá / đẩy bán không?

KPI:

- Inventory Quantity On Hand
- Inventory Value
- Low Stock Products
- Slow-Moving Products
- Average Inventory Value

Biểu đồ:

- Bar chart: inventory value by brand
- Bar chart: inventory quantity by category
- Table: low stock products
- Table: slow-moving products
- Scatter chart: stock quantity vs units sold

Bảng slow-moving nên có:

- Product name
- Brand
- Category
- Quantity on hand
- Units sold in selected period
- Inventory value
- Slow-moving score
- Suggested action

Suggested action có thể tính rule đơn giản:

```text
stock cao + bán thấp -> đẩy khuyến mãi
stock thấp + bán cao -> nhập thêm
stock thấp + bán thấp -> theo dõi
stock cao + bán cao -> duy trì tồn kho
```

---

## 5. Báo Cáo Thanh Toán / Đối Soát

Mục tiêu: đảm bảo doanh thu ghi nhận và tiền thực nhận không lệch.

Câu hỏi cần trả lời:

- Tổng tiền khách đã thanh toán là bao nhiêu?
- Phí cổng thanh toán là bao nhiêu?
- Thực nhận là bao nhiêu?
- Bao nhiêu giao dịch khớp?
- Bao nhiêu giao dịch mismatch / failed?
- Mismatch tập trung vào ngày nào?

KPI:

- Total Payment Amount
- Gateway Fee
- Net Received
- Matched Payments
- Mismatch Payments
- Reconciliation Match Rate %
- Payment Failure Rate %

Biểu đồ:

- Line chart: payment amount vs net received by time
- Doughnut chart: reconciliation status
- Bar chart: mismatch payments by date
- Table: payment mismatch details

---

## 6. Báo Cáo CSKH / Review

Mục tiêu: hiểu chất lượng trải nghiệm khách hàng và sản phẩm.

Câu hỏi cần trả lời:

- Rating trung bình là bao nhiêu?
- Sản phẩm nào bị đánh giá thấp?
- Brand nào có review tốt/xấu?
- Tỷ lệ review verified là bao nhiêu?
- Review có cải thiện theo thời gian không?
- Có nhóm sản phẩm nào bán tốt nhưng rating thấp không?

KPI:

- Total Reviews
- Average Rating
- Verified Reviews
- Verified Review Rate %
- Low Rating Products

Biểu đồ:

- Line chart: average rating over time
- Bar chart: average rating by brand
- Bar chart: review count by rating stars
- Horizontal bar: lowest rated products
- Table: products with high sales but low rating

Gợi ý cảnh báo:

```text
units_sold cao
AND average_rating < 3.5
```

Nhóm này cần ưu tiên kiểm tra chất lượng sản phẩm, mô tả sản phẩm, giao hàng, hoặc CSKH.

---

## 7. Phân Tích Khách Hàng

Mục tiêu: biết nhóm khách hàng nào có giá trị cao và hành vi mua ra sao.

Câu hỏi cần trả lời:

- Có bao nhiêu khách hàng?
- Segment nào mua nhiều nhất?
- Segment nào tạo doanh thu cao nhất?
- Doanh thu trung bình mỗi khách là bao nhiêu?
- Khách hàng ở khu vực nào đóng góp nhiều?

KPI:

- Total Customers
- Revenue per Customer
- Customers by Segment
- Revenue by Segment

Biểu đồ:

- Bar chart: revenue by customer segment
- Bar chart: customer count by segment
- Bar chart: revenue by province/city
- Table: top customers by revenue, nếu cần

---

## 8. Trang Dashboard Đề Xuất

Nên chia dashboard thành các tab hoặc các trang:

1. Tổng quan
2. Bán hàng
3. Tồn kho & hàng hóa
4. Thanh toán
5. CSKH & review
6. Khách hàng

Nếu cần làm bản đầu tiên nhanh, nên ưu tiên:

1. Tổng quan
2. Bán hàng
3. Tồn kho & hàng hóa
4. CSKH & review

Thanh toán có thể làm sau nếu phần đối soát đã có dữ liệu đủ tốt.

---

## 9. Truy Vấn OLAP Gợi Ý

### 9.1. Revenue trend

Theo ngày:

```sql
SELECT
    d.full_date AS period_label,
    SUM(s.quantity * s.unit_price) AS total_revenue,
    SUM(s.gross_profit) AS gross_profit,
    SUM(s.net_profit) AS net_profit,
    SUM(s.quantity) AS units_sold
FROM Fact_Sales s
JOIN Dim_Date d ON d.date_key = s.date_key
GROUP BY d.full_date
ORDER BY d.full_date;
```

Theo tuần:

```sql
SELECT
    CONCAT(d.year, '-W', LPAD(WEEK(d.full_date, 3), 2, '0')) AS period_label,
    SUM(s.quantity * s.unit_price) AS total_revenue,
    SUM(s.gross_profit) AS gross_profit,
    SUM(s.net_profit) AS net_profit,
    SUM(s.quantity) AS units_sold
FROM Fact_Sales s
JOIN Dim_Date d ON d.date_key = s.date_key
GROUP BY d.year, WEEK(d.full_date, 3)
ORDER BY d.year, WEEK(d.full_date, 3);
```

Theo tháng:

```sql
SELECT
    CONCAT(d.year, '-', LPAD(d.month, 2, '0')) AS period_label,
    SUM(s.quantity * s.unit_price) AS total_revenue,
    SUM(s.gross_profit) AS gross_profit,
    SUM(s.net_profit) AS net_profit,
    SUM(s.quantity) AS units_sold
FROM Fact_Sales s
JOIN Dim_Date d ON d.date_key = s.date_key
GROUP BY d.year, d.month
ORDER BY d.year, d.month;
```

### 9.2. Top products

```sql
SELECT
    p.product_name,
    p.brand,
    p.category_name,
    SUM(s.quantity) AS units_sold,
    SUM(s.quantity * s.unit_price) AS total_revenue,
    SUM(s.net_profit) AS net_profit
FROM Fact_Sales s
JOIN Dim_Product p ON p.product_key = s.product_key
GROUP BY p.product_key, p.product_name, p.brand, p.category_name
ORDER BY total_revenue DESC
LIMIT 10;
```

### 9.3. Slow-moving products

```sql
SELECT
    p.product_name,
    p.brand,
    p.category_name,
    COALESCE(inv.quantity_on_hand, 0) AS quantity_on_hand,
    COALESCE(sales.units_sold, 0) AS units_sold,
    COALESCE(inv.inventory_value, 0) AS inventory_value,
    COALESCE(inv.quantity_on_hand, 0) / GREATEST(COALESCE(sales.units_sold, 0), 1) AS slow_moving_score
FROM Dim_Product p
LEFT JOIN (
    SELECT
        product_key,
        SUM(quantity) AS units_sold
    FROM Fact_Sales
    GROUP BY product_key
) sales ON sales.product_key = p.product_key
LEFT JOIN (
    SELECT
        i.product_key,
        i.quantity_on_hand,
        i.inventory_value
    FROM Fact_Inventory i
    JOIN (
        SELECT product_key, MAX(date_key) AS latest_date_key
        FROM Fact_Inventory
        GROUP BY product_key
    ) latest
        ON latest.product_key = i.product_key
       AND latest.latest_date_key = i.date_key
) inv ON inv.product_key = p.product_key
WHERE COALESCE(inv.quantity_on_hand, 0) > 0
ORDER BY slow_moving_score DESC, inventory_value DESC
LIMIT 10;
```

### 9.4. Review warning

```sql
SELECT
    p.product_name,
    p.brand,
    COUNT(r.review_key) AS review_count,
    AVG(r.rating_stars) AS average_rating
FROM Fact_Reviews r
JOIN Dim_Product p ON p.product_key = r.product_key
GROUP BY p.product_key, p.product_name, p.brand
HAVING AVG(r.rating_stars) < 3.5
ORDER BY average_rating ASC, review_count DESC;
```

---

## 10. Nguyên Tắc Thiết Kế Chart.js

- Không vẽ quá nhiều chart trên một màn hình.
- KPI cards phải trả lời nhanh tình hình hiện tại.
- Chart chính nên là trend theo thời gian.
- Bảng cảnh báo chỉ hiển thị dữ liệu cần hành động.
- Mỗi trang dashboard nên có 1 câu hỏi chính.
- Biểu đồ phải đổi theo filter thời gian ngày / tuần / tháng / khoảng tùy chọn.
