from __future__ import annotations

import os
from collections import defaultdict

import pymysql

from product_normalization import canonical_color, split_base_name_and_color

DB_CONFIG = {
    "host": os.getenv("DB_HOST", "localhost"),
    "port": int(os.getenv("DB_PORT", "3306")),
    "user": os.getenv("DB_USER", "root"),
    "password": os.getenv("DB_PASSWORD", "12345"),
    "database": os.getenv("DB_NAME", "phoneshop_db"),
    "charset": "utf8mb4",
    "autocommit": False,
}


def connect():
    return pymysql.connect(**DB_CONFIG)


def has_column(cur, table_name, column_name):
    cur.execute(
        """
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = %s AND table_name = %s AND column_name = %s
        LIMIT 1
        """,
        (DB_CONFIG["database"], table_name, column_name),
    )
    return cur.fetchone() is not None


def load_catalog(cur):
    cur.execute(
        """
        SELECT product_id, name
        FROM products
        ORDER BY product_id
        """
    )
    products = cur.fetchall()

    cur.execute(
        """
        SELECT variant_id, product_id, color
        FROM product_variants
        ORDER BY variant_id
        """
    )
    variants = cur.fetchall()

    variants_by_product = defaultdict(list)
    for variant in variants:
        variants_by_product[variant[1]].append(
            {
                "variant_id": variant[0],
                "product_id": variant[1],
                "color": variant[2],
            }
        )
    return products, variants_by_product


def main():
    conn = connect()
    cur = conn.cursor()
    try:
        if not has_column(cur, "product_variants", "color"):
            print("product_variants.color is missing. Nothing to backfill.")
            return

        products, variants_by_product = load_catalog(cur)

        product_updates = 0
        variant_updates = 0

        for product_id, current_name in products:
            base_name, inferred_color = split_base_name_and_color(current_name)
            if not inferred_color:
                continue

            product_variants = variants_by_product.get(product_id, [])
            existing_colors = {
                canonical_color(v["color"])
                for v in product_variants
                if v["color"] is not None and str(v["color"]).strip()
            }
            existing_colors.discard(None)

            if existing_colors and existing_colors != {inferred_color}:
                continue

            if base_name and base_name != current_name:
                cur.execute(
                    """
                    UPDATE products
                    SET name = %s
                    WHERE product_id = %s
                    """,
                    (base_name, product_id),
                )
                product_updates += cur.rowcount

            for variant in product_variants:
                color_value = variant["color"]
                normalized_color = canonical_color(color_value)
                target_color = normalized_color if normalized_color else inferred_color
                if target_color is None:
                    continue
                if normalized_color == target_color and color_value is not None and str(color_value).strip():
                    continue
                cur.execute(
                    """
                    UPDATE product_variants
                    SET color = %s
                    WHERE variant_id = %s
                    """,
                    (target_color, variant["variant_id"]),
                )
                variant_updates += cur.rowcount

        conn.commit()
        print("Backfill done.")
        print(f"products updated: {product_updates}")
        print(f"variants updated: {variant_updates}")
    except Exception:
        conn.rollback()
        raise
    finally:
        cur.close()
        conn.close()


if __name__ == "__main__":
    main()
