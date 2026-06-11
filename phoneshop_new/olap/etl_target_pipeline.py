from __future__ import annotations

import os
from datetime import date, datetime, timedelta
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path

import numpy as np
import pandas as pd
from sqlalchemy import create_engine, text

from product_normalization import canonical_color, split_base_name_and_color


def money(value) -> Decimal:
    return Decimal(str(value)).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)


def price_tier_for(price: float | int | Decimal | None, category_name: str | None = None) -> str:
    if category_name and category_name.lower() in {"phụ kiện", "phu-kien", "accessory", "phu kien"}:
        return "Accessory"
    if price is None:
        return "Unknown"
    p = float(price)
    if p < 2_000_000:
        return "Budget"
    if p < 8_000_000:
        return "Midrange"
    if p < 15_000_000:
        return "UpperMid"
    if p < 25_000_000:
        return "Premium"
    return "Flagship"


def customer_segment_for(order_count: int, total_spend: float) -> str:
    if order_count >= 12 or total_spend >= 40_000_000:
        return "VIP"
    if order_count >= 6 or total_spend >= 15_000_000:
        return "Loyal"
    if order_count >= 2 or total_spend > 0:
        return "Regular"
    return "New"


def gateway_fee_for(method: str | None, amount: float | int | Decimal) -> Decimal:
    amount_dec = money(amount)
    if (method or "").upper() == "COD":
        return money(0)
    fee = max(amount_dec * Decimal("0.015"), Decimal("1000"))
    return money(fee)


def revenue_share(series: pd.Series) -> pd.Series:
    total = series.sum()
    if not total or pd.isna(total):
        return pd.Series([1 / len(series)] * len(series), index=series.index)
    return series / total


def reconciliation_status_for(bank_status: str, bank_amount: Decimal, expected_amount: Decimal, payment_status: str) -> str:
    bank_status = (bank_status or "").upper()
    payment_status = (payment_status or "").upper()
    if bank_status != "SUCCESS":
        return "FAILED"
    if abs(float(bank_amount) - float(expected_amount)) > 0.01:
        return "AMOUNT_MISMATCH"
    if payment_status != "SUCCESS":
        return "STATUS_MISMATCH"
    return "MATCHED"


def build_engine(prefix: str, default_db: str) -> str:
    host = os.getenv(f"{prefix}_HOST", os.getenv("DB_HOST", "localhost"))
    port = int(os.getenv(f"{prefix}_PORT", os.getenv("DB_PORT", "3306")))
    user = os.getenv(f"{prefix}_USER", os.getenv("DB_USER", "root"))
    password = os.getenv(f"{prefix}_PASSWORD", os.getenv("DB_PASSWORD", "12345"))
    db_name = os.getenv(f"{prefix}_NAME", default_db)
    return f"mysql+pymysql://{user}:{password}@{host}:{port}/{db_name}?charset=utf8mb4"


SRC_ENGINE = create_engine(build_engine("SRC_DB", "phoneshop_db"), future=True)
STG_ENGINE = create_engine(build_engine("STG_DB", "phoneshop_stg"), future=True)
DWH_ENGINE = create_engine(build_engine("DWH_DB", "phoneshop_dwh"), future=True)

INVENTORY_XLSX = Path(os.getenv("INVENTORY_OUT", "inventory_mock.xlsx"))
STATEMENT_XLSX = Path(os.getenv("STATEMENT_OUT", "vnpay_statement_mock.xlsx"))


def read_df(engine, sql: str, params=None) -> pd.DataFrame:
    return pd.read_sql_query(text(sql), engine, params=params)


def table_columns(engine, table: str) -> set[str]:
    df = read_df(
        engine,
        """
        SELECT column_name
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = :table_name
        ORDER BY ordinal_position
        """,
        params={"table_name": table},
    )
    if df.empty:
        return set()
    return set(df.iloc[:, 0].astype(str).tolist())


def select_table(engine, table: str, wanted: list[tuple[str, str | None]]) -> pd.DataFrame:
    available = table_columns(engine, table)
    parts = []
    for source_col, alias in wanted:
        out_col = alias or source_col
        if source_col in available:
            parts.append(f"`{source_col}` AS `{out_col}`")
        else:
            parts.append(f"NULL AS `{out_col}`")
    sql = f"SELECT {', '.join(parts)} FROM `{table}`"
    return read_df(engine, sql)


def truncate_tables(engine, tables: list[str]):
    with engine.begin() as conn:
        conn.execute(text("SET FOREIGN_KEY_CHECKS = 0"))
        for tbl in tables:
            conn.execute(text(f"TRUNCATE TABLE {tbl}"))
        conn.execute(text("SET FOREIGN_KEY_CHECKS = 1"))


def load_source_frames() -> dict[str, pd.DataFrame]:
    users = read_df(
        SRC_ENGINE,
        """
        SELECT u.user_id, u.dtype, u.email, u.full_name, u.phone, u.created_at,
               u.role_id, u.salary, u.hire_date, r.name_role
        FROM users u
        LEFT JOIN roles r ON r.role_id = u.role_id
        """,
    )
    addresses = read_df(
        SRC_ENGINE,
        """
        SELECT address_id, user_id, city, district, ward, street, is_default, created_at
        FROM address
        """,
    )
    categories = read_df(
        SRC_ENGINE,
        """
        SELECT category_id, name, parent_id
        FROM categories
        """,
    )
    products = read_df(
        SRC_ENGINE,
        """
        SELECT product_id, name, brand, description, status, created_at, category_id
        FROM products
        """,
    )
    variants = select_table(
        SRC_ENGINE,
        "product_variants",
        [
            ("variant_id", None),
            ("product_id", None),
            ("color", None),
            ("storage_gb", None),
            ("price", None),
            ("import_price", None),
            ("stock_qty", None),
            ("sku", None),
        ],
    )
    orders = select_table(
        SRC_ENGINE,
        "orders",
        [
            ("order_id", None),
            ("user_id", None),
            ("order_code", None),
            ("status", None),
            ("payment_method", None),
            ("total_amount", None),
            ("shipping_fee", None),
            ("discount_amount", None),
            ("applied_voucher", None),
            ("voucher_id", None),
            ("shipping_name", None),
            ("shipping_phone", None),
            ("shipping_address", None),
            ("shipping_ward", None),
            ("shipping_district", None),
            ("shipping_city", None),
            ("created_at", None),
            ("updated_at", None),
            ("confirmed_at", None),
            ("shipping_at", None),
            ("delivered_at", None),
            ("cancelled_at", None),
        ],
    )
    order_items = select_table(
        SRC_ENGINE,
        "order_items",
        [
            ("order_item_id", None),
            ("order_id", None),
            ("variant_id", None),
            ("quantity", None),
            ("unit_price", None),
            ("product_name", None),
            ("variant_name", None),
            ("subtotal", None),
        ],
    )
    payments = select_table(
        SRC_ENGINE,
        "payments",
        [
            ("payment_id", None),
            ("order_id", None),
            ("method", None),
            ("status", None),
            ("transaction_ref", None),
            ("paid_at", None),
            ("created_at", None),
            ("updated_at", None),
        ],
    )
    reviews = read_df(
        SRC_ENGINE,
        """
        SELECT review_id, user_id, product_id, rating, comment, created_at, updated_at
        FROM reviews
        """,
    )
    return {
        "users": users,
        "addresses": addresses,
        "categories": categories,
        "products": products,
        "variants": variants,
        "orders": orders,
        "order_items": order_items,
        "payments": payments,
        "reviews": reviews,
    }


def load_excel_frames() -> dict[str, pd.DataFrame]:
    inventory = pd.read_excel(INVENTORY_XLSX)
    statement = pd.read_excel(STATEMENT_XLSX)
    return {"inventory": inventory, "statement": statement}


def build_product_stage(source: dict[str, pd.DataFrame]) -> pd.DataFrame:
    products = source["products"].copy()
    variants = source["variants"].copy()
    categories = source["categories"].copy()

    cat_parent = categories[["category_id", "name"]].rename(
        columns={"category_id": "parent_id", "name": "parent_category"}
    )
    cat = categories[["category_id", "name", "parent_id"]].rename(
        columns={"name": "category_name"}
    )
    cat = cat.merge(cat_parent, on="parent_id", how="left")

    merged = variants.merge(products, on="product_id", how="left", suffixes=("_variant", "_product"))
    merged = merged.merge(cat[["category_id", "category_name", "parent_category"]], on="category_id", how="left")

    normalized_product_names = []
    normalized_colors = []
    for _, row in merged.iterrows():
        base_name, inferred_color = split_base_name_and_color(row["name"])
        normalized_product_names.append(base_name or row["name"])
        color = canonical_color(row["color"]) or inferred_color
        normalized_colors.append(color)

    merged["raw_product_name"] = merged["name"]
    merged["product_name"] = normalized_product_names
    merged["color"] = normalized_colors
    merged["storage"] = merged["storage_gb"].apply(lambda x: None if pd.isna(x) else str(int(x)))
    merged["price_tier"] = merged.apply(lambda r: price_tier_for(r["price"], r.get("category_name")), axis=1)
    merged["product_status"] = merged["status"]
    merged["source_created_at"] = merged["created_at"]

    cols = [
        "variant_id",
        "product_id",
        "raw_product_name",
        "product_name",
        "brand",
        "category_name",
        "parent_category",
        "storage_gb",
        "color",
        "price",
        "import_price",
        "stock_qty",
        "sku",
        "product_status",
        "price_tier",
        "source_created_at",
    ]
    out = merged[cols].copy()
    out["product_name"] = out["product_name"].fillna("")
    out["brand"] = out["brand"].fillna("")
    out["category_name"] = out["category_name"].fillna("")
    out["price_tier"] = out["price_tier"].fillna("Unknown")
    return out


def build_customer_stage(source: dict[str, pd.DataFrame], orders: pd.DataFrame) -> pd.DataFrame:
    users = source["users"].copy()
    addresses = source["addresses"].copy()
    customers = users[users["dtype"].astype(str).eq("CUSTOMER")].copy()

    default_addresses = (
        addresses.sort_values(["user_id", "is_default", "address_id"], ascending=[True, False, True])
        .drop_duplicates("user_id", keep="first")
        [["user_id", "city"]]
        .rename(columns={"city": "province_city"})
    )

    order_counts = orders.groupby("user_id").size().rename("order_count").reset_index()
    spend = orders.groupby("user_id")["total_amount"].sum().rename("total_spend").reset_index()

    out = customers.merge(default_addresses, on="user_id", how="left")
    out = out.merge(order_counts, on="user_id", how="left")
    out = out.merge(spend, on="user_id", how="left")
    out["order_count"] = out["order_count"].fillna(0).astype(int)
    out["total_spend"] = out["total_spend"].fillna(0).astype(float)
    out["customer_segment"] = out.apply(lambda r: customer_segment_for(int(r["order_count"]), float(r["total_spend"])), axis=1)
    out["customer_key"] = out["user_id"]
    out["role_name"] = out["name_role"].fillna(out["dtype"])
    out["registered_at"] = out["created_at"]
    cols = [
        "customer_key",
        "user_id",
        "full_name",
        "email",
        "province_city",
        "customer_segment",
        "role_name",
        "registered_at",
        "order_count",
        "total_spend",
    ]
    return out[cols].copy()


def build_order_stage(source: dict[str, pd.DataFrame]) -> tuple[pd.DataFrame, pd.DataFrame]:
    orders = source["orders"].copy()
    order_items = source["order_items"].copy()
    variants = source["variants"][["variant_id", "product_id", "import_price"]].copy()
    variants["import_price"] = pd.to_numeric(variants["import_price"], errors="coerce").fillna(0.0)

    merged = order_items.merge(orders, on="order_id", how="left", suffixes=("_item", "_order"))
    merged = merged.merge(variants, on="variant_id", how="left")

    order_totals = merged.groupby("order_id")["subtotal"].sum().rename("order_item_total").reset_index()
    merged = merged.merge(order_totals, on="order_id", how="left")
    merged["revenue_share"] = merged["subtotal"] / merged["order_item_total"].replace({0: np.nan})
    merged["revenue_share"] = merged["revenue_share"].fillna(1 / merged.groupby("order_id")["order_id"].transform("count"))

    merged["line_revenue"] = merged["quantity"] * merged["unit_price"]
    merged["line_cogs"] = merged["quantity"] * merged["import_price"].fillna(0)
    merged["line_gross_profit"] = merged["line_revenue"] - merged["line_cogs"]
    merged["line_discount"] = (merged["discount_amount"].fillna(0) * merged["revenue_share"]).round(2)
    merged["line_shipping_fee"] = (merged["shipping_fee"].fillna(0) * merged["revenue_share"]).round(2)
    merged["line_net_profit"] = merged["line_gross_profit"] - merged["line_discount"] - merged["line_shipping_fee"]

    merged["lead_time_days"] = np.where(
        merged["delivered_at"].notna(),
        (pd.to_datetime(merged["delivered_at"]) - pd.to_datetime(merged["created_at"])).dt.total_seconds() / 86400.0,
        np.where(
            merged["shipping_at"].notna(),
            (pd.to_datetime(merged["shipping_at"]) - pd.to_datetime(merged["created_at"])).dt.total_seconds() / 86400.0,
            np.nan,
        ),
    )
    merged["lead_time_days"] = merged["lead_time_days"].round(1)

    header_cols = [
        "order_id",
        "order_code",
        "user_id",
        "status",
        "payment_method",
        "total_amount",
        "shipping_fee",
        "discount_amount",
        "applied_voucher",
        "shipping_name",
        "shipping_phone",
        "shipping_address",
        "shipping_ward",
        "shipping_district",
        "shipping_city",
        "created_at",
        "updated_at",
        "confirmed_at",
        "shipping_at",
        "delivered_at",
        "cancelled_at",
    ]
    header = orders[header_cols].copy()
    header = header.rename(columns={"user_id": "customer_key", "status": "order_status"})

    line_cols = [
        "order_item_id",
        "order_id",
        "order_code",
        "user_id",
        "variant_id",
        "quantity",
        "unit_price",
        "import_price",
        "line_revenue",
        "line_cogs",
        "line_gross_profit",
        "line_discount",
        "line_shipping_fee",
        "line_net_profit",
        "status",
        "payment_method",
        "created_at",
        "lead_time_days",
    ]
    lines = merged[line_cols].copy()
    lines = lines.rename(columns={"user_id": "customer_key", "status": "order_status"})
    lines["import_price"] = pd.to_numeric(lines["import_price"], errors="coerce").fillna(0.0)
    for col in ["line_revenue", "line_cogs", "line_gross_profit", "line_discount", "line_shipping_fee", "line_net_profit", "unit_price"]:
        lines[col] = pd.to_numeric(lines[col], errors="coerce").fillna(0.0)
    return header, lines


def build_payment_statement_stage(source: dict[str, pd.DataFrame], excel: dict[str, pd.DataFrame]) -> tuple[pd.DataFrame, pd.DataFrame]:
    payments = source["payments"].copy()
    orders = source["orders"][["order_id", "user_id", "total_amount", "status", "payment_method", "created_at"]].copy()
    statement = excel["statement"].copy()

    payment_latest = (
        payments.sort_values(["order_id", "created_at", "payment_id"])
        .drop_duplicates("order_id", keep="last")
        .rename(
            columns={
                "status": "payment_status",
                "created_at": "payment_created_at",
                "updated_at": "payment_updated_at",
            }
        )
    )

    payment_source = payment_latest.merge(orders, on="order_id", how="left")
    payment_source = payment_source.rename(
        columns={
            "user_id": "customer_key",
            "status": "order_status",
            "payment_method": "order_payment_method",
            "created_at": "order_created_at",
            "total_amount": "order_total_amount",
        }
    )
    payment_source["transaction_ref"] = payment_source["transaction_ref"].fillna(
        payment_source["payment_id"].map(lambda x: f"TX-{x}")
    )
    payment_source["method"] = payment_source["method"].fillna(payment_source["order_payment_method"])
    payment_source["status"] = payment_source["payment_status"]
    payment_source["created_at"] = payment_source["payment_created_at"].fillna(payment_source["order_created_at"])
    payment_source["updated_at"] = payment_source["payment_updated_at"].fillna(payment_source["created_at"])
    payment_source["customer_key"] = pd.to_numeric(payment_source["customer_key"], errors="coerce").astype("Int64")
    payment_source["order_id"] = pd.to_numeric(payment_source["order_id"], errors="coerce").astype("Int64")
    payment_source_calc = payment_source.copy()
    payment_source_stage = payment_source[[
        "payment_id",
        "order_id",
        "customer_key",
        "transaction_ref",
        "method",
        "status",
        "paid_at",
        "created_at",
        "updated_at",
    ]].copy()

    statement = statement.rename(columns={"bank_amount": "bank_amount", "bank_status": "bank_status", "pay_date": "pay_date"})
    statement["pay_date"] = pd.to_datetime(statement["pay_date"], errors="coerce")
    statement["order_id"] = pd.to_numeric(statement["order_id"], errors="coerce").astype("Int64")
    payment_statement_stage = statement[["txn_ref", "order_id", "bank_amount", "bank_status", "pay_date"]].copy()
    payment_statement_stage["bank_amount"] = pd.to_numeric(payment_statement_stage["bank_amount"], errors="coerce").fillna(0.0)

    payment_stage = statement.merge(
        payment_source_calc[["order_id", "customer_key", "transaction_ref", "method", "status", "paid_at", "created_at", "updated_at", "order_total_amount"]],
        on="order_id",
        how="left",
        suffixes=("", "_oltp"),
    )
    payment_stage["order_id"] = payment_stage["order_id"].astype("Int64")
    payment_stage["customer_key"] = payment_stage["customer_key"].fillna(payment_stage["order_id"]).astype("Int64")
    payment_stage["amount"] = pd.to_numeric(
        payment_stage["order_total_amount"].fillna(payment_stage["bank_amount"]),
        errors="coerce",
    ).fillna(0.0)
    payment_stage["bank_amount"] = pd.to_numeric(payment_stage["bank_amount"], errors="coerce").fillna(0.0)
    payment_stage["gateway_fee"] = payment_stage.apply(lambda r: float(gateway_fee_for(r.get("method"), r["amount"])), axis=1)
    payment_stage["net_received"] = (payment_stage["amount"] - payment_stage["gateway_fee"]).round(2)
    payment_stage["reconciliation_status"] = payment_stage.apply(
        lambda r: reconciliation_status_for(
            r.get("bank_status"),
            money(r.get("bank_amount", 0)),
            money(r.get("amount", 0)),
            r.get("payment_status", r.get("status", "")),
        ),
        axis=1,
    )
    payment_stage["date_key"] = pd.to_datetime(payment_stage["pay_date"]).dt.year * 10000 + pd.to_datetime(payment_stage["pay_date"]).dt.month * 100 + pd.to_datetime(payment_stage["pay_date"]).dt.day
    cols = [
        "date_key",
        "customer_key",
        "order_id",
        "amount",
        "gateway_fee",
        "net_received",
        "reconciliation_status",
        "txn_ref",
        "bank_amount",
        "bank_status",
        "pay_date",
        "method",
        "status",
    ]
    return payment_source_stage, payment_statement_stage, payment_stage[cols].copy()


def build_inventory_stage(excel: dict[str, pd.DataFrame]) -> pd.DataFrame:
    inv = excel["inventory"].copy()
    inv["date_key"] = pd.to_numeric(inv["date_key"], errors="coerce").astype("Int64")
    inv["variant_id"] = pd.to_numeric(inv["variant_id"], errors="coerce").astype("Int64")
    inv["quantity_on_hand"] = pd.to_numeric(inv["quantity_on_hand"], errors="coerce").fillna(0).astype(int)
    return inv[["date_key", "variant_id", "quantity_on_hand"]].copy()


def build_review_stage(source: dict[str, pd.DataFrame], product_stage: pd.DataFrame) -> pd.DataFrame:
    reviews = source["reviews"].copy()
    orders = source["orders"][["order_id", "user_id", "status", "created_at"]].copy()
    order_items = source["order_items"][["order_id", "variant_id"]].copy()
    variants = source["variants"][["variant_id", "product_id"]].copy()

    canonical_variant = (
        variants.sort_values("variant_id").groupby("product_id")["variant_id"].first().rename("canonical_variant_id").reset_index()
    )

    order_products = order_items.merge(variants, on="variant_id", how="left").merge(orders, on="order_id", how="left")
    order_products = order_products[order_products["status"].astype(str).eq("DELIVERED")]
    order_products["created_at"] = pd.to_datetime(order_products["created_at"])

    review_rows = []
    for _, r in reviews.iterrows():
        customer_id = r["user_id"]
        product_id = r["product_id"]
        review_dt = pd.to_datetime(r["created_at"])
        delivered = order_products[
            (order_products["user_id"] == customer_id)
            & (order_products["product_id"] == product_id)
            & (order_products["created_at"] <= review_dt)
        ]
        is_verified = not delivered.empty
        canonical_variant_id = canonical_variant.loc[canonical_variant["product_id"] == product_id, "canonical_variant_id"]
        product_key = int(canonical_variant_id.iloc[0]) if not canonical_variant_id.empty else int(product_stage.iloc[0]["variant_id"])
        review_rows.append(
            {
                "review_id": r["review_id"],
                "customer_key": customer_id,
                "product_key": product_key,
                "rating_stars": int(r["rating"]),
                "review_created_at": review_dt,
                "is_verified": bool(is_verified),
                "comment": r.get("comment"),
            }
        )

    return pd.DataFrame(review_rows)


def build_dim_date(source: dict[str, pd.DataFrame], excel: dict[str, pd.DataFrame]) -> pd.DataFrame:
    dates = []
    for key in ["orders", "reviews"]:
        df = source[key]
        for col in ["created_at"]:
            if col in df.columns:
                dates.extend(pd.to_datetime(df[col], errors="coerce").dropna().tolist())
    if "pay_date" in excel["statement"].columns:
        dates.extend(pd.to_datetime(excel["statement"]["pay_date"], errors="coerce").dropna().tolist())
    if "date_key" in excel["inventory"].columns:
        inv_dates = pd.to_datetime(excel["inventory"]["date_key"].astype(str), format="%Y%m%d", errors="coerce")
        dates.extend(inv_dates.dropna().tolist())

    if not dates:
        dates = [pd.Timestamp("2023-01-01"), pd.Timestamp("2026-03-31")]

    start = min(dates).date()
    end = max(dates).date()
    out = []
    d = start
    while d <= end:
        out.append(
            {
                "date_key": d.year * 10000 + d.month * 100 + d.day,
                "full_date": d,
                "day": d.day,
                "month": d.month,
                "quarter": ((d.month - 1) // 3) + 1,
                "year": d.year,
                "is_weekend": d.weekday() >= 5,
            }
        )
        d += timedelta(days=1)
    return pd.DataFrame(out)


def write_table(engine, table: str, df: pd.DataFrame):
    if df.empty:
        return
    df.to_sql(table, engine, if_exists="append", index=False, method="multi", chunksize=1000)


def main():
    if not INVENTORY_XLSX.exists():
        raise FileNotFoundError(f"Missing {INVENTORY_XLSX}")
    if not STATEMENT_XLSX.exists():
        raise FileNotFoundError(f"Missing {STATEMENT_XLSX}")

    source = load_source_frames()
    excel = load_excel_frames()

    product_stage = build_product_stage(source)
    customer_stage = build_customer_stage(source, source["orders"])
    order_header_stage, order_line_stage = build_order_stage(source)
    payment_source_stage, payment_statement_stage, payment_stage = build_payment_statement_stage(source, excel)
    inventory_stage = build_inventory_stage(excel)
    review_stage = build_review_stage(source, product_stage)
    dim_date = build_dim_date(source, excel)

    truncate_tables(
        STG_ENGINE,
        [
            "stg_review_source",
            "stg_inventory_snapshot",
            "stg_payment_statement",
            "stg_payment_source",
            "stg_order_lines",
            "stg_order_header",
            "stg_customer_catalog",
            "stg_product_catalog",
        ],
    )

    write_table(STG_ENGINE, "stg_product_catalog", product_stage)
    write_table(STG_ENGINE, "stg_customer_catalog", customer_stage)
    write_table(STG_ENGINE, "stg_order_header", order_header_stage)
    write_table(STG_ENGINE, "stg_order_lines", order_line_stage)
    write_table(STG_ENGINE, "stg_payment_source", payment_source_stage)
    write_table(STG_ENGINE, "stg_payment_statement", payment_statement_stage)
    write_table(STG_ENGINE, "stg_inventory_snapshot", inventory_stage)
    write_table(STG_ENGINE, "stg_review_source", review_stage)

    dwh_product = product_stage[[
        "variant_id",
        "product_name",
        "brand",
        "category_name",
        "storage_gb",
        "color",
        "price_tier",
    ]].copy()
    dwh_product = dwh_product.rename(columns={"variant_id": "product_key", "storage_gb": "storage"})
    dwh_product["variant_id"] = dwh_product["product_key"]
    dwh_product = dwh_product[[
        "product_key",
        "variant_id",
        "product_name",
        "brand",
        "category_name",
        "storage",
        "color",
        "price_tier",
    ]]
    dwh_product["storage"] = dwh_product["storage"].apply(lambda x: None if pd.isna(x) else str(int(x)))

    dwh_customer = customer_stage[[
        "customer_key",
        "user_id",
        "full_name",
        "email",
        "province_city",
        "customer_segment",
    ]].copy()

    dwh_sales = order_line_stage[[
        "created_at",
        "variant_id",
        "customer_key",
        "quantity",
        "unit_price",
        "import_price",
        "line_gross_profit",
        "line_net_profit",
    ]].copy()
    dwh_sales = dwh_sales.rename(columns={"created_at": "sale_dt", "variant_id": "product_key", "line_gross_profit": "gross_profit", "line_net_profit": "net_profit"})
    dwh_sales["date_key"] = pd.to_datetime(dwh_sales["sale_dt"]).dt.year * 10000 + pd.to_datetime(dwh_sales["sale_dt"]).dt.month * 100 + pd.to_datetime(dwh_sales["sale_dt"]).dt.day
    dwh_sales = dwh_sales[["date_key", "product_key", "customer_key", "quantity", "unit_price", "import_price", "gross_profit", "net_profit"]]

    dwh_payments = payment_stage.copy()
    dwh_payments["date_key"] = pd.to_datetime(dwh_payments["pay_date"]).dt.year * 10000 + pd.to_datetime(dwh_payments["pay_date"]).dt.month * 100 + pd.to_datetime(dwh_payments["pay_date"]).dt.day
    dwh_payments = dwh_payments.rename(columns={"pay_date": "payment_time"})
    dwh_payments = dwh_payments[[
        "date_key",
        "payment_time",
        "customer_key",
        "order_id",
        "amount",
        "gateway_fee",
        "net_received",
        "reconciliation_status",
    ]]

    product_lookup = product_stage[["variant_id", "import_price"]].copy()
    dwh_inventory = inventory_stage.merge(product_lookup, on="variant_id", how="left")
    dwh_inventory["inventory_value"] = dwh_inventory["quantity_on_hand"] * dwh_inventory["import_price"].fillna(0)
    dwh_inventory = dwh_inventory.rename(columns={"variant_id": "product_key"})[
        ["date_key", "product_key", "quantity_on_hand", "inventory_value"]
    ]

    dwh_reviews = review_stage.copy()
    dwh_reviews["date_key"] = pd.to_datetime(dwh_reviews["review_created_at"]).dt.year * 10000 + pd.to_datetime(dwh_reviews["review_created_at"]).dt.month * 100 + pd.to_datetime(dwh_reviews["review_created_at"]).dt.day
    dwh_reviews = dwh_reviews[["date_key", "product_key", "customer_key", "rating_stars", "is_verified"]]

    truncate_tables(
        DWH_ENGINE,
        [
            "Fact_Reviews",
            "Fact_Inventory",
            "Fact_Payments",
            "Fact_Sales",
            "Dim_Customer",
            "Dim_Product",
            "Dim_Date",
        ],
    )

    write_table(DWH_ENGINE, "Dim_Date", dim_date)
    write_table(DWH_ENGINE, "Dim_Product", dwh_product)
    write_table(DWH_ENGINE, "Dim_Customer", dwh_customer)
    write_table(DWH_ENGINE, "Fact_Sales", dwh_sales)
    write_table(DWH_ENGINE, "Fact_Payments", dwh_payments)
    write_table(DWH_ENGINE, "Fact_Inventory", dwh_inventory)
    write_table(DWH_ENGINE, "Fact_Reviews", dwh_reviews)

    print("Pipeline complete.")
    print(f"Staging product rows: {len(product_stage):,}")
    print(f"Staging customer rows: {len(customer_stage):,}")
    print(f"Staging order lines: {len(order_line_stage):,}")
    print(f"Staging payment rows: {len(payment_stage):,}")
    print(f"Staging inventory rows: {len(inventory_stage):,}")
    print(f"Staging review rows: {len(review_stage):,}")
    print(f"DWH Dim_Date rows: {len(dim_date):,}")
    print(f"DWH Dim_Product rows: {len(dwh_product):,}")
    print(f"DWH Dim_Customer rows: {len(dwh_customer):,}")
    print(f"DWH Fact_Sales rows: {len(dwh_sales):,}")
    print(f"DWH Fact_Payments rows: {len(dwh_payments):,}")
    print(f"DWH Fact_Inventory rows: {len(dwh_inventory):,}")
    print(f"DWH Fact_Reviews rows: {len(dwh_reviews):,}")


if __name__ == "__main__":
    main()
