import os
from sqlalchemy import create_engine, text

def build_engine(prefix: str, default_db: str) -> str:
    host = os.getenv(f"{prefix}_HOST", os.getenv("DB_HOST", "localhost"))
    port = int(os.getenv(f"{prefix}_PORT", os.getenv("DB_PORT", "3306")))
    user = os.getenv(f"{prefix}_USER", os.getenv("DB_USER", "root"))
    password = os.getenv(f"{prefix}_PASSWORD", os.getenv("DB_PASSWORD", "12345"))
    db_name = os.getenv(f"{prefix}_NAME", default_db)
    return f"mysql+pymysql://{user}:{password}@{host}:{port}/{db_name}?charset=utf8mb4"

DWH_ENGINE = create_engine(build_engine("DWH_DB", "phoneshop_dwh"), future=True)

queries = [
    # Composite index for Fact_Inventory (product_key, date_key) to speed up latest stock lookups
    "ALTER TABLE Fact_Inventory ADD INDEX idx_prod_date (product_key, date_key);",
    # Composite index for Fact_Sales (product_key, date_key) to speed up sales analysis
    "ALTER TABLE Fact_Sales ADD INDEX idx_sales_prod_date (product_key, date_key);"
]

with DWH_ENGINE.begin() as conn:
    for q in queries:
        try:
            print(f"Executing: {q}")
            conn.execute(text(q))
            print("Successfully added index!")
        except Exception as e:
            print(f"Skipping or failed: {e}")
