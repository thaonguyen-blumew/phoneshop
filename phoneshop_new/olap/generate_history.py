"""
generate_history.py
-------------------
Sinh dữ liệu quá khứ từ 01/11/2024 đến hôm nay cho:
  - inventory_mock.xlsx  (snapshot tồn kho HÀNG NGÀY)
  - vnpay_statement_mock.xlsx  (giao dịch ngân hàng)

Cách dùng:
    cd olap
    python generate_history.py

Yêu cầu: pip install openpyxl pandas numpy pymysql
"""
from __future__ import annotations

import os
import numpy as np
import pandas as pd
import pymysql
from datetime import date
from pathlib import Path

# ── Kết nối DB ────────────────────────────────────────────────────────────────
DB_HOST     = os.getenv("DB_HOST",     "localhost")
DB_PORT     = int(os.getenv("DB_PORT", "3306"))
DB_USER     = os.getenv("DB_USER",     "root")
DB_PASSWORD = os.getenv("DB_PASSWORD", "12345")
DB_NAME     = os.getenv("DB_NAME",     "phoneshop_db")

INVENTORY_OUT = os.getenv("INVENTORY_OUT", "inventory_mock.xlsx")
STATEMENT_OUT = os.getenv("STATEMENT_OUT", "vnpay_statement_mock.xlsx")

# ── Tham số ────────────────────────────────────────────────────────────────────
START_DATE   = date(2024, 11, 1)   # Bắt đầu từ 01/11/2024
RANDOM_SEED  = 42
ERROR_RATE   = 0.05                # 5% giao dịch lỗi/lệch
# Tỷ lệ sản phẩm hết hàng theo tháng (để chart thú vị hơn)
STOCKOUT_RATE = 0.08               # 8% SKU = 0 mỗi snapshot

rng = np.random.default_rng(RANDOM_SEED)


def connect_db():
    return pymysql.connect(
        host=DB_HOST, port=DB_PORT, user=DB_USER,
        password=DB_PASSWORD, database=DB_NAME,
        charset="utf8mb4", autocommit=True,
    )


def fetch_df(conn, sql: str, params=None) -> pd.DataFrame:
    with conn.cursor() as cur:
        cur.execute(sql, params or ())
        rows = cur.fetchall()
        cols = [d[0] for d in cur.description] if cur.description else []
    return pd.DataFrame(rows, columns=cols)


def safe_to_excel(df: pd.DataFrame, path_str: str):
    path = Path(path_str)
    try:
        df.to_excel(path, index=False, engine="openpyxl")
        print(f"  ✓ Ghi {len(df):,} dòng → {path.name}")
    except PermissionError:
        fb = path.with_name(f"{path.stem}_new{path.suffix}")
        df.to_excel(fb, index=False, engine="openpyxl")
        print(f"  ⚠ File đang mở, ghi vào {fb.name} thay thế")


# ── 1. INVENTORY MOCK ─────────────────────────────────────────────────────────
def build_inventory(conn) -> pd.DataFrame:
    """
    Sinh snapshot tồn kho HÀNG NGÀY từ START_DATE → hôm nay.
    Mỗi ngày × mỗi variant_id → 1 dòng.

    Logic qty_on_hand:
      - Qty dao động 5–100 (ngẫu nhiên có seed theo ngày)
      - ~8% SKU mỗi ngày bị = 0 (hết hàng)
      - Xu hướng giảm nhẹ cuối năm (mô phỏng bán được)
    """
    variants = fetch_df(conn, "SELECT variant_id FROM product_variants ORDER BY variant_id")
    if variants.empty:
        raise RuntimeError("Không tìm thấy variant nào trong product_variants!")

    variant_ids = variants["variant_id"].astype(int).tolist()
    n = len(variant_ids)

    today = pd.Timestamp.today().normalize()
    start = pd.Timestamp(START_DATE)
    all_days = pd.date_range(start=start, end=today, freq="D")

    print(f"  Sinh inventory: {len(all_days)} ngày × {n} SKU = {len(all_days)*n:,} dòng...")

    frames = []
    for i, day in enumerate(all_days):
        day_key = day.year * 10000 + day.month * 100 + day.day
        day_rng = np.random.default_rng(RANDOM_SEED + day_key)

        # Base qty 5–100, xu hướng giảm nhẹ theo thời gian (simulate bán hàng)
        progress = i / max(len(all_days) - 1, 1)          # 0.0 → 1.0
        base_max = int(100 - progress * 20)                # 100 → 80
        base_min = int(5  - progress * 0)                  # giữ min=5
        qty = day_rng.integers(base_min, base_max + 1, size=n).astype(int)

        # Thêm ~8% hết hàng (qty=0)
        stockout_mask = day_rng.random(n) < STOCKOUT_RATE
        qty[stockout_mask] = 0

        # Một số SKU tồn rất cao (mô phỏng phụ kiện)
        high_mask = day_rng.random(n) < 0.05
        qty[high_mask] = day_rng.integers(100, 300, size=int(high_mask.sum())).astype(int)

        frames.append(pd.DataFrame({
            "date_key":         np.repeat(day_key, n),
            "variant_id":       np.array(variant_ids, dtype=np.int64),
            "quantity_on_hand": qty,
        }))

    return pd.concat(frames, ignore_index=True)


# ── 2. VNPAY STATEMENT MOCK ───────────────────────────────────────────────────
def build_statement(conn) -> pd.DataFrame:
    """
    Sinh bank statement từ START_DATE → hôm nay.
    Dựa trên các đơn hàng thực trong OLTP.
    """
    # Lấy danh sách đơn thực (ưu tiên VNPAY success)
    src = fetch_df(conn, """
        SELECT
            COALESCE(p.transaction_ref, CONCAT('TX-', p.payment_id)) AS txn_ref,
            p.order_id,
            o.total_amount AS order_total,
            COALESCE(p.paid_at, p.created_at, o.created_at) AS pay_date
        FROM payments p
        JOIN orders o ON o.order_id = p.order_id
        ORDER BY p.order_id, p.payment_id
    """)

    if src.empty:
        print("  ⚠ Không có dữ liệu payments, dùng giả lập đơn giản...")
        src = fetch_df(conn, """
            SELECT CONCAT('TX-ORD-', order_id) AS txn_ref,
                   order_id, total_amount AS order_total, created_at AS pay_date
            FROM orders WHERE status = 'DELIVERED'
            ORDER BY order_id
        """)

    if src.empty:
        raise RuntimeError("Không có đơn hàng nào để sinh statement!")

    src["txn_ref"]    = src["txn_ref"].astype(str)
    src["order_id"]   = pd.to_numeric(src["order_id"],   errors="coerce").astype("Int64")
    src["order_total"]= pd.to_numeric(src["order_total"],errors="coerce").fillna(0.0)

    today = pd.Timestamp.today().normalize()
    start = pd.Timestamp(START_DATE)
    all_days = list(pd.date_range(start=start, end=today, freq="D"))

    # ~40 giao dịch/ngày trung bình
    rows_per_day = max(5, min(80, len(src) // 10))
    base_n = len(src)

    print(f"  Sinh statement: {len(all_days)} ngày × ~{rows_per_day} giao dịch/ngày...")

    frames = []
    seq = 1
    for day in all_days:
        day_key = day.year * 10000 + day.month * 100 + day.day
        day_rng = np.random.default_rng(RANDOM_SEED + day_key + 9999)

        # Lấy ngẫu nhiên các đơn làm giao dịch trong ngày
        take_idx = day_rng.integers(0, base_n, size=rows_per_day)
        stmt = src.iloc[take_idx].copy().reset_index(drop=True)

        # Tạo txn_ref unique
        stmt["txn_ref"] = (
            stmt["txn_ref"].astype(str) + f"-{day_key}-"
            + pd.Series(range(seq, seq + rows_per_day), dtype=str).str.zfill(6).values
        )
        seq += rows_per_day

        stmt["bank_amount"] = stmt["order_total"].round(2)
        stmt["bank_status"] = "SUCCESS"

        # Phân bổ giờ giao dịch ngẫu nhiên trong ngày
        hours   = day_rng.integers(0, 24, size=rows_per_day)
        minutes = day_rng.integers(0, 60, size=rows_per_day)
        stmt["pay_date"] = (
            day.floor("D")
            + pd.to_timedelta(hours,   unit="h")
            + pd.to_timedelta(minutes, unit="m")
        )

        # Thêm lỗi (AMOUNT_MISMATCH + FAILED)
        error_n = max(1, int(round(rows_per_day * ERROR_RATE)))
        err_idx = day_rng.choice(rows_per_day, size=error_n, replace=False)
        half = len(err_idx) // 2
        # Lệch số tiền
        for idx in err_idx[:half]:
            pct  = day_rng.uniform(0.01, 0.08)
            sign = day_rng.choice([-1, 1])
            stmt.loc[idx, "bank_amount"] = round(
                float(stmt.loc[idx, "bank_amount"]) + sign * max(float(stmt.loc[idx, "bank_amount"]) * pct, 1000), 2
            )
        # Failed
        for idx in err_idx[half:]:
            stmt.loc[idx, "bank_status"] = "FAILED"

        frames.append(stmt[["txn_ref", "order_id", "bank_amount", "bank_status", "pay_date"]].copy())

    out = pd.concat(frames, ignore_index=True)
    out["order_id"]    = out["order_id"].astype("Int64")
    out["bank_amount"] = pd.to_numeric(out["bank_amount"], errors="coerce").fillna(0.0).round(2)
    out["pay_date"]    = pd.to_datetime(out["pay_date"],   errors="coerce")
    return out[["txn_ref", "order_id", "bank_amount", "bank_status", "pay_date"]].copy()


# ── MAIN ──────────────────────────────────────────────────────────────────────
def main():
    print("=" * 60)
    print(f"Sinh dữ liệu lịch sử từ {START_DATE} → hôm nay")
    print("=" * 60)

    conn = connect_db()
    try:
        print("\n[1/2] Inventory snapshot...")
        inv_df = build_inventory(conn)
        safe_to_excel(inv_df, INVENTORY_OUT)

        print("\n[2/2] VNPAY Bank Statement...")
        stmt_df = build_statement(conn)
        safe_to_excel(stmt_df, STATEMENT_OUT)

    finally:
        conn.close()

    print("\n✅ Xong! Bước tiếp theo:")
    print("   python etl_target_pipeline.py")


if __name__ == "__main__":
    main()
