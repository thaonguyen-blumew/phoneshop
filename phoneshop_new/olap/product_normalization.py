from __future__ import annotations

import math
import re

_COLOR_RULES = [
    ("Titan Tự nhiên", ["Titan Tự nhiên", "Titan Natural", "Titan Nat", "Natural Titanium", "Titanium Natural"]),
    ("Titan Đen", ["Titan Đen", "Titan Black", "TITAN-BLACK", "TITAN BLACK"]),
    ("Titan Trắng", ["Titan Trắng", "Titan White", "TITAN-WHITE", "TITAN WHITE"]),
    ("Tự nhiên", ["Tự nhiên", "Nat", "NAT", "Natural"]),
    ("Đen", ["Đen", "Black", "Onyx", "Midnight", "Graphite", "Space Gray", "SpaceGrey", "Space Gray"]),
    ("Trắng", ["Trắng", "White", "Silver", "Pearl"]),
    ("Xanh dương", ["Xanh dương", "Blue", "Navy"]),
    ("Xanh lá", ["Xanh lá", "Green"]),
    ("Hồng", ["Hồng", "Pink", "Lilac"]),
    ("Tím", ["Tím", "Purple"]),
    ("Vàng", ["Vàng", "Gold"]),
    ("Cam", ["Cam", "Orange"]),
    ("Trong suốt", ["Trong suốt", "Clear"]),
    ("Bạc", ["Bạc"]),
]


def _alias_pattern(alias: str) -> str:
    escaped = re.escape(re.sub(r"\s+", " ", alias.strip()))
    return escaped.replace(r"\ ", r"[\s\-]+")


def _build_rule_index() -> list[tuple[str, str]]:
    indexed: list[tuple[str, str]] = []
    for canonical, aliases in _COLOR_RULES:
        for alias in aliases:
            indexed.append((alias, canonical))
    indexed.sort(key=lambda item: len(item[0]), reverse=True)
    return indexed


_COLOR_INDEX = _build_rule_index()


def _as_text(value: object) -> str | None:
    if value is None:
        return None
    if isinstance(value, str):
        candidate = value.strip()
        return candidate or None
    if isinstance(value, float) and math.isnan(value):
        return None
    return None


def canonical_color(raw: str | None) -> str | None:
    candidate = _as_text(raw)
    if candidate is None:
        return None
    normalized_candidate = re.sub(r"\s+", " ", candidate).strip()
    for alias, canonical in _COLOR_INDEX:
        if re.fullmatch(_alias_pattern(alias), normalized_candidate, flags=re.IGNORECASE):
            return canonical
    return candidate


def split_base_name_and_color(product_name: str | None) -> tuple[str | None, str | None]:
    candidate = _as_text(product_name)
    if candidate is None:
        return None, None

    for alias, canonical in _COLOR_INDEX:
        pattern = rf"^(?P<base>.*?)(?:[\s\-]+)?{_alias_pattern(alias)}$"
        match = re.match(pattern, candidate, flags=re.IGNORECASE)
        if match:
            base = re.sub(r"[\s\-]+$", "", match.group("base")).strip()
            if base:
                return base, canonical
            return None, canonical
    return candidate, None


def variant_display_name(product_name: str | None, storage_gb: int | None, color: str | None) -> str:
    parts: list[str] = []
    if product_name:
        parts.append(product_name)
    if storage_gb is not None:
        parts.append(f"{storage_gb}GB")
    if color:
        parts.append(color)
    return " | ".join(parts) if parts else ""


def normalize_variant_color(
    product_name: str | None,
    variant_color: str | None,
) -> tuple[str | None, str | None]:
    base_name, product_color = split_base_name_and_color(product_name)
    normalized_variant_color = canonical_color(variant_color)
    if normalized_variant_color is None:
        normalized_variant_color = product_color
    return base_name or product_name, normalized_variant_color
