from __future__ import annotations

import subprocess
import sys
from datetime import datetime
from pathlib import Path


BASE_DIR = Path(__file__).resolve().parent
PYTHON = sys.executable


def run_step(label: str, script_name: str) -> None:
    script_path = BASE_DIR / script_name
    if not script_path.exists():
        raise FileNotFoundError(f"Missing script: {script_path}")

    print(f"[{datetime.now().isoformat(timespec='seconds')}] START {label}")
    result = subprocess.run(
        [PYTHON, str(script_path)],
        cwd=str(BASE_DIR),
        check=False,
    )
    if result.returncode != 0:
        raise SystemExit(result.returncode)
    print(f"[{datetime.now().isoformat(timespec='seconds')}] DONE  {label}")


def main() -> None:
    run_step("generate_excel", "mock_excel.py")
    run_step("etl_target_pipeline", "etl_target_pipeline.py")
    print(f"[{datetime.now().isoformat(timespec='seconds')}] PIPELINE COMPLETE")


if __name__ == "__main__":
    main()
