from __future__ import annotations

import os
from datetime import date, datetime, timedelta
from pathlib import Path

import numpy as np
import pandas as pd
import pymysql


# -----------------------------------------------------------------------------
# Config
# -----------------------------------------------------------------------------
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = int(os.getenv("DB_PORT", "3306"))
DB_USER = os.getenv("DB_USER", "root")
DB_PASSWORD = os.getenv("DB_PASSWORD", "12345")
DB_NAME = os.getenv("DB_NAME", "phoneshop_db")

INVENTORY_OUT = os.getenv("INVENTORY_OUT", "inventory_mock.xlsx")
STATEMENT_OUT = os.getenv("STATEMENT_OUT", "vnpay_statement_mock.xlsx")

RANDOM_SEED = int(os.getenv("RANDOM_SEED", "42"))
NUM_STATEMENT_ROWS = int(os.getenv("NUM_STATEMENT_ROWS", "30000"))
ERROR_RATE = float(os.getenv("ERROR_RATE", "0.05"))

rng = np.random.default_rng(RANDOM_SEED)

INVENTORY_DAYS = 365 * 2


# -----------------------------------------------------------------------------
# DB helpers
# -----------------------------------------------------------------------------
def connect_db():
    return pymysql.connect(
        host=DB_HOST,
        port=DB_PORT,
        user=DB_USER,
        password=DB_PASSWORD,
        database=DB_NAME,
        charset="utf8mb4",
        autocommit=True,
    )


def fetch_df(conn, sql: str, params=None) -> pd.DataFrame:
    with conn.cursor() as cur:
        cur.execute(sql, params or ())
        rows = cur.fetchall()
        columns = [desc[0] for desc in cur.description] if cur.description else []
    return pd.DataFrame(rows, columns=columns)


def get_columns(conn, table_name: str) -> list[str]:
    sql = """
        SELECT COLUMN_NAME
        FROM information_schema.columns
        WHERE table_schema = %s
          AND table_name = %s
        ORDER BY ordinal_position
    """
    with conn.cursor() as cur:
        cur.execute(sql, (DB_NAME, table_name))
        rows = cur.fetchall()
    return [row[0] for row in rows]


def read_existing_excel(path_str: str, expected_cols: list[str]) -> pd.DataFrame:
    path = Path(path_str)
    if not path.exists():
        return pd.DataFrame(columns=expected_cols)
    df = pd.read_excel(path)
    if df.empty:
        return pd.DataFrame(columns=expected_cols)
    for col in expected_cols:
        if col not in df.columns:
            df[col] = pd.NA
    return df[expected_cols].copy()


# -----------------------------------------------------------------------------
# Inventory mock
# -----------------------------------------------------------------------------
def build_inventory_mock(conn, existing: pd.DataFrame | None = None) -> pd.DataFrame:
    variants = fetch_df(
        conn,
        """
        SELECT variant_id
        FROM product_variants
        ORDER BY variant_id
        """,
    )

    if variants.empty:
        raise RuntimeError("No variants found in product_variants.")

    variant_ids = variants["variant_id"].astype(int).tolist()
    end_day = pd.Timestamp.today().normalize()

    if existing is None or existing.empty:
        start_day = end_day - pd.Timedelta(days=INVENTORY_DAYS - 1)
        all_days = pd.date_range(start=start_day, end=end_day, freq="D")
        base_df = pd.DataFrame(columns=["date_key", "variant_id", "quantity_on_hand"])
    else:
        base_df = existing.copy()
        base_df["date_key"] = pd.to_numeric(base_df["date_key"], errors="coerce").astype("Int64")
        base_df["variant_id"] = pd.to_numeric(base_df["variant_id"], errors="coerce").astype("Int64")
        last_date_key = int(base_df["date_key"].dropna().max())
        last_day = pd.to_datetime(str(last_date_key), format="%Y%m%d").normalize()
        start_day = last_day + pd.Timedelta(days=1)
        all_days = pd.date_range(start=start_day, end=end_day, freq="D")

    if all_days.empty:
        return base_df

    append_days = []
    for day in all_days:
        day_key = day.year * 10000 + day.month * 100 + day.day
        day_rng = np.random.default_rng(RANDOM_SEED + day_key)
        qty_col = day_rng.integers(5, 101, size=len(variant_ids), endpoint=False)
        append_days.append(
            pd.DataFrame(
                {
                    "date_key": np.repeat(day_key, len(variant_ids)),
                    "variant_id": np.array(variant_ids, dtype=np.int64),
                    "quantity_on_hand": qty_col.astype(int),
                }
            )
        )

    append_df = pd.concat(append_days, ignore_index=True)
    if base_df.empty:
        return append_df
    return pd.concat([base_df, append_df], ignore_index=True)


# -----------------------------------------------------------------------------
# Bank statement mock
# -----------------------------------------------------------------------------
def load_source_payments(conn) -> pd.DataFrame:
    order_cols = get_columns(conn, "orders")
    payment_cols = get_columns(conn, "payments")

    if not order_cols:
        raise RuntimeError("Table orders not found or has no columns.")
    if not payment_cols:
        raise RuntimeError("Table payments not found or has no columns.")

    txn_col = None
    for candidate in ["transaction_ref", "txn_ref"]:
        if candidate in payment_cols:
            txn_col = candidate
            break

    paid_at_col = "paid_at" if "paid_at" in payment_cols else None
    payment_status_col = "status" if "status" in payment_cols else None

    order_total_col = "total_amount" if "total_amount" in order_cols else None
    order_status_col = "status" if "status" in order_cols else None

    if txn_col is None:
        raise RuntimeError("Could not find txn ref column in payments table.")
    if order_total_col is None:
        raise RuntimeError("Could not find total_amount in orders table.")

    # Prefer real VNPAY successful payments from the current OLTP schema.
    base_sql = f"""
        SELECT
            p.{txn_col} AS txn_ref,
            p.order_id AS order_id,
            o.{order_total_col} AS order_total,
            p.{paid_at_col if paid_at_col else "created_at"} AS pay_date,
            p.{payment_status_col if payment_status_col else "status"} AS payment_status,
            o.{order_status_col if order_status_col else "status"} AS order_status
        FROM payments p
        JOIN orders o ON o.order_id = p.order_id
    """

    where_clauses = []
    params = []

    if "method" in payment_cols:
        where_clauses.append("p.method = %s")
        params.append("VN_PAY")
    if payment_status_col:
        where_clauses.append(f"p.{payment_status_col} = %s")
        params.append("SUCCESS")

    if where_clauses:
        base_sql += " WHERE " + " AND ".join(where_clauses)

    base_sql += " ORDER BY p.order_id ASC, p.payment_id ASC"

    df = fetch_df(conn, base_sql, params=params)

    # Fallback if the live data is sparse.
    if df.empty:
        fallback_sql = f"""
            SELECT
                COALESCE(p.{txn_col}, CONCAT('TX-', o.order_code, '-', p.payment_id)) AS txn_ref,
                p.order_id AS order_id,
                o.{order_total_col} AS order_total,
                COALESCE(p.{paid_at_col}, p.created_at, o.created_at) AS pay_date,
                p.{payment_status_col if payment_status_col else "status"} AS payment_status,
                o.{order_status_col if order_status_col else "status"} AS order_status
            FROM payments p
            JOIN orders o ON o.order_id = p.order_id
            ORDER BY p.order_id ASC, p.payment_id ASC
        """
        df = fetch_df(conn, fallback_sql)

    if df.empty:
        raise RuntimeError("No payment data found to build the bank statement mock.")

    df["txn_ref"] = df["txn_ref"].astype(str)
    df["order_id"] = pd.to_numeric(df["order_id"], errors="coerce").astype("Int64")
    df["order_total"] = pd.to_numeric(df["order_total"], errors="coerce").fillna(0.0)
    df["pay_date"] = pd.to_datetime(df["pay_date"], errors="coerce")

    # Use only rows that look successful on the system side.
    system_ok = df.copy()
    if "payment_status" in system_ok.columns:
        system_ok = system_ok[system_ok["payment_status"].astype(str).str.upper().eq("SUCCESS")]
    elif "order_status" in system_ok.columns:
        system_ok = system_ok[system_ok["order_status"].astype(str).str.upper().isin(["CONFIRMED", "PACKING", "SHIPPING", "DELIVERED"])]

    if system_ok.empty:
        system_ok = df.copy()

    return system_ok[["txn_ref", "order_id", "order_total", "pay_date"]].reset_index(drop=True)


def _build_statement_rows(source: pd.DataFrame, day_list: list[pd.Timestamp], rows_per_day: int) -> pd.DataFrame:
    if source.empty or not day_list or rows_per_day <= 0:
        return pd.DataFrame(columns=["txn_ref", "order_id", "bank_amount", "bank_status", "pay_date"])

    frames = []
    source = source.reset_index(drop=True)
    base_n = len(source)
    seq = 1

    for day in day_list:
        day_key = day.year * 10000 + day.month * 100 + day.day
        day_rng = np.random.default_rng(RANDOM_SEED + day_key)
        take_idx = day_rng.integers(0, base_n, size=rows_per_day)
        stmt = source.iloc[take_idx].copy().reset_index(drop=True)

        stmt["txn_ref"] = stmt["txn_ref"].astype(str) + f"-{day_key}-" + pd.Series(
            np.arange(seq, seq + rows_per_day), index=stmt.index
        ).astype(str).str.zfill(6)
        seq += rows_per_day

        stmt["bank_amount"] = pd.to_numeric(stmt["order_total"], errors="coerce").fillna(0.0).round(2)
        stmt["bank_status"] = "SUCCESS"

        hours = day_rng.integers(0, 24, size=rows_per_day)
        minutes = day_rng.integers(0, 60, size=rows_per_day)
        stmt["pay_date"] = day.floor("D") + pd.to_timedelta(hours, unit="h") + pd.to_timedelta(minutes, unit="m")

        error_n = int(round(rows_per_day * ERROR_RATE))
        if error_n > 0:
            error_idx = day_rng.choice(rows_per_day, size=error_n, replace=False)
            half = error_n // 2
            amount_err_idx = error_idx[:half]
            status_err_idx = error_idx[half:]

            if len(amount_err_idx) > 0:
                pct = day_rng.uniform(0.01, 0.08, size=len(amount_err_idx))
                sign = day_rng.choice([-1, 1], size=len(amount_err_idx))
                delta = np.maximum(stmt.loc[amount_err_idx, "bank_amount"].astype(float).abs() * pct, 1000.0)
                stmt.loc[amount_err_idx, "bank_amount"] = (
                    stmt.loc[amount_err_idx, "bank_amount"].astype(float) + sign * delta
                ).round(2)
                stmt.loc[amount_err_idx, "bank_status"] = "SUCCESS"

            if len(status_err_idx) > 0:
                stmt.loc[status_err_idx, "bank_status"] = "FAILED"

        frames.append(stmt[["txn_ref", "order_id", "bank_amount", "bank_status", "pay_date"]].copy())

    return pd.concat(frames, ignore_index=True)


def build_vnpay_statement_mock(conn, existing: pd.DataFrame | None = None) -> pd.DataFrame:
    source = load_source_payments(conn)

    if source.empty:
        raise RuntimeError("No source rows available for statement mock generation.")

    source = source.dropna(subset=["order_id"]).reset_index(drop=True)

    existing_df = pd.DataFrame(columns=["txn_ref", "order_id", "bank_amount", "bank_status", "pay_date"]) if existing is None else existing.copy()
    if not existing_df.empty:
        existing_df["pay_date"] = pd.to_datetime(existing_df["pay_date"], errors="coerce")
        existing_df["order_id"] = pd.to_numeric(existing_df["order_id"], errors="coerce").astype("Int64")
        existing_df["bank_amount"] = pd.to_numeric(existing_df["bank_amount"], errors="coerce").fillna(0.0)
        existing_df = existing_df[["txn_ref", "order_id", "bank_amount", "bank_status", "pay_date"]].copy()

    today = pd.Timestamp.today().normalize()
    if existing_df.empty:
        start_day = today - pd.Timedelta(days=365 * 2 - 1)
        day_list = list(pd.date_range(start=start_day, end=today, freq="D"))
        rows_per_day = max(1, int(round(NUM_STATEMENT_ROWS / max(len(day_list), 1))))
        new_df = _build_statement_rows(source, day_list, rows_per_day)
        if new_df.empty:
            return existing_df
        out = new_df
    else:
        last_day = existing_df["pay_date"].dropna().max().normalize()
        start_day = last_day + pd.Timedelta(days=1)
        if start_day > today:
            return existing_df

        day_list = list(pd.date_range(start=start_day, end=today, freq="D"))
        distinct_days = max(existing_df["pay_date"].dt.normalize().nunique(), 1)
        rows_per_day = max(1, int(round(len(existing_df) / distinct_days)))
        new_df = _build_statement_rows(source, day_list, rows_per_day)
        out = pd.concat([existing_df, new_df], ignore_index=True)

    out["order_id"] = out["order_id"].astype("Int64")
    out["bank_amount"] = pd.to_numeric(out["bank_amount"], errors="coerce").fillna(0.0).round(2)
    out["pay_date"] = pd.to_datetime(out["pay_date"], errors="coerce")
    return out[["txn_ref", "order_id", "bank_amount", "bank_status", "pay_date"]].copy()


def safe_to_excel(df: pd.DataFrame, path_str: str):
    path = Path(path_str)
    try:
        df.to_excel(path, index=False, engine="openpyxl")
        return path
    except PermissionError:
        fallback = path.with_name(f"{path.stem}_{pd.Timestamp.now():%Y%m%d_%H%M%S}{path.suffix}")
        df.to_excel(fallback, index=False, engine="openpyxl")
        print(f"Could not overwrite {path.name}; wrote {fallback.name} instead.")
        return fallback


# -----------------------------------------------------------------------------
# Main
# -----------------------------------------------------------------------------
def main():
    conn = connect_db()
    try:
        inventory_existing = read_existing_excel(INVENTORY_OUT, ["date_key", "variant_id", "quantity_on_hand"])
        statement_existing = read_existing_excel(STATEMENT_OUT, ["txn_ref", "order_id", "bank_amount", "bank_status", "pay_date"])

        inventory_df = build_inventory_mock(conn, inventory_existing)
        statement_df = build_vnpay_statement_mock(conn, statement_existing)

        safe_to_excel(inventory_df, INVENTORY_OUT)
        safe_to_excel(statement_df, STATEMENT_OUT)

        print(f"Wrote {len(inventory_df):,} rows to {INVENTORY_OUT}")
        print(f"Wrote {len(statement_df):,} rows to {STATEMENT_OUT}")
    finally:
        conn.close()


if __name__ == "__main__":
    main()
