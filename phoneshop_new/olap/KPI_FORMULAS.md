# KPI Formulas

Tài liệu này mô tả các KPI chính cho dashboard Power BI, dựa trên DWH mục tiêu:

- `Dim_Date`
- `Dim_Product`
- `Dim_Customer`
- `Fact_Sales`
- `Fact_Payments`
- `Fact_Inventory`
- `Fact_Reviews`

## 1. Sales KPIs

### Total Revenue
```text
SUM(Fact_Sales[quantity] * Fact_Sales[unit_price])
```

Nếu đã lưu sẵn cột doanh thu trong DWH/biểu thức measure:
```text
SUM(Fact_Sales[net_profit] + Fact_Sales[import_price] * Fact_Sales[quantity])
```

### Total Cost of Goods Sold
```text
SUM(Fact_Sales[quantity] * Fact_Sales[import_price])
```

### Gross Profit
```text
SUM(Fact_Sales[gross_profit])
```

### Net Profit
```text
SUM(Fact_Sales[net_profit])
```

### Gross Margin %
```text
DIVIDE([Gross Profit], [Total Revenue])
```

### Net Margin %
```text
DIVIDE([Net Profit], [Total Revenue])
```

### Units Sold
```text
SUM(Fact_Sales[quantity])
```

### Average Selling Price
```text
DIVIDE([Total Revenue], [Units Sold])
```

### Average Order Value
Vì model hiện tại là fact theo dòng hàng, có thể xấp xỉ theo `order_id`:
```text
DIVIDE([Total Revenue], DISTINCTCOUNT(Fact_Sales[order_id]))
```

## 2. Payment KPIs

### Total Payment Amount
```text
SUM(Fact_Payments[amount])
```

### Gateway Fee
```text
SUM(Fact_Payments[gateway_fee])
```

### Net Received
```text
SUM(Fact_Payments[net_received])
```

### Matched Payments
```text
CALCULATE(
    COUNTROWS(Fact_Payments),
    Fact_Payments[reconciliation_status] = "MATCHED"
)
```

### Mismatch Payments
```text
CALCULATE(
    COUNTROWS(Fact_Payments),
    Fact_Payments[reconciliation_status] <> "MATCHED"
)
```

### Reconciliation Match Rate %
```text
DIVIDE([Matched Payments], COUNTROWS(Fact_Payments))
```

### Payment Failure Rate %
```text
DIVIDE(
    CALCULATE(COUNTROWS(Fact_Payments), Fact_Payments[reconciliation_status] = "FAILED"),
    COUNTROWS(Fact_Payments)
)
```

## 3. Inventory KPIs

### Inventory Quantity On Hand
```text
SUM(Fact_Inventory[quantity_on_hand])
```

### Inventory Value
```text
SUM(Fact_Inventory[inventory_value])
```

### Average Inventory Value
```text
AVERAGE(Fact_Inventory[inventory_value])
```

### Inventory by Date
```text
SUM(Fact_Inventory[quantity_on_hand])
```

## 4. Review KPIs

### Total Reviews
```text
COUNTROWS(Fact_Reviews)
```

### Average Rating
```text
AVERAGE(Fact_Reviews[rating_stars])
```

### Verified Reviews
```text
CALCULATE(
    COUNTROWS(Fact_Reviews),
    Fact_Reviews[is_verified] = TRUE()
)
```

### Verified Review Rate %
```text
DIVIDE([Verified Reviews], [Total Reviews])
```

## 5. Customer KPIs

### Total Customers
```text
DISTINCTCOUNT(Dim_Customer[customer_key])
```

### Customer by Segment
```text
COUNTROWS(Dim_Customer)
```

### Revenue per Customer
```text
DIVIDE([Total Revenue], DISTINCTCOUNT(Fact_Sales[customer_key]))
```

## 6. Product KPIs

### Total Products
```text
DISTINCTCOUNT(Dim_Product[product_key])
```

### Top Product by Revenue
```text
RANKX(
    ALL(Dim_Product[product_name]),
    [Total Revenue],
    ,
    DESC
)
```

### Top Product by Quantity
```text
RANKX(
    ALL(Dim_Product[product_name]),
    [Units Sold],
    ,
    DESC
)
```

## 7. Suggested Power BI Measures

Nên tạo các measure sau để dùng lại trong report:

- `[Total Revenue]`
- `[Total Cost]`
- `[Gross Profit]`
- `[Net Profit]`
- `[Gross Margin %]`
- `[Net Margin %]`
- `[Units Sold]`
- `[Total Payments]`
- `[Gateway Fee]`
- `[Net Received]`
- `[Reconciliation Match Rate %]`
- `[Inventory Value]`
- `[Average Rating]`
- `[Verified Review Rate %]`

## 8. Notes

- `Fact_Sales` đang là fact theo dòng hàng, nên các KPI liên quan đơn hàng có thể phải `DISTINCTCOUNT(order_id)`.
- `Fact_Payments` là lớp đối soát, không nên trộn với sales fact.
- `Fact_Inventory` là snapshot theo ngày và theo variant/product key.
- `Fact_Reviews` dùng để phân tích mức độ hài lòng và độ tin cậy của review.

