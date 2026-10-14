#!/usr/bin/env python3
"""Builds the Multiverse of Anime x Tensura No Index compatibility data pack.

Source of truth: tools/compat/multiverse_x_tensura_no_index/ (README.md + pack.mcmeta + data/).
Outputs (both deterministic: sorted entries, fixed timestamps, no extra attributes):
  docs/compat/multiverse_x_tensura_no_index.zip                  the drop-in pack for a world's datapacks folder
  src/main/resources/resourcepacks/nusmp_tni_compat/             the same files (without README.md), shipped in the mod jar and switched on
                                                                 by compat/CompatPacks only when tensuranoindex is loaded
Usage:  python tools/compat/build_compat_pack.py            build both
        python tools/compat/build_compat_pack.py --check    validate only, write nothing (exit 1 on a problem)
Before building it validates every JSON file and that every id the pack refers to is in known_ids.json.
"""
import json
import shutil
import sys
import zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
SRC = HERE / "multiverse_x_tensura_no_index"
ZIP_OUT = ROOT / "docs" / "compat" / "multiverse_x_tensura_no_index.zip"
JAR_COPY = ROOT / "src" / "main" / "resources" / "resourcepacks" / "nusmp_tni_compat"
STAMP = (2024, 1, 1, 0, 0, 0)
OWN_NS = "nusmp_tni_compat"


def files():
    return sorted(p for p in SRC.rglob("*") if p.is_file())


def refs(node, out):
    """Collects every string that looks like a namespaced id, with the key it sat under."""
    if isinstance(node, dict):
        for k, v in node.items():
            if k in ("name", "id", "loot_table_id", "table", "modid", "type") and isinstance(v, str):
                out.append((k, v))
            refs(v, out)
    elif isinstance(node, list):
        for v in node:
            refs(v, out)


def validate():
    problems = []
    known = json.loads((HERE / "known_ids.json").read_text(encoding="utf-8"))
    items = {"tensuranoindex:" + i for i in known["tensuranoindex_items"]}
    ents = {"tensuranoindex:" + i for i in known["tensuranoindex_entities"]} | {"nusmp:" + i for i in known["nusmp_entities"]}
    tables = {"nusmp:" + t for t in known["nusmp_loot_tables"]}
    for p in files():
        rel = p.relative_to(SRC).as_posix()
        if not (p.suffix in (".json", ".mcmeta")):
            continue
        try:
            doc = json.loads(p.read_text(encoding="utf-8"))
        except ValueError as e:
            problems.append(f"{rel}: not valid JSON ({e})")
            continue
        found = []
        refs(doc, found)
        for key, val in found:
            if key == "name" and val.startswith("tensuranoindex:") and val not in items:
                problems.append(f"{rel}: unknown item {val}")
            elif key == "id" and not val.startswith("#") and val not in ents:
                problems.append(f"{rel}: unknown entity {val}")
            elif key == "loot_table_id" and val not in tables:
                problems.append(f"{rel}: loot table {val} is not one of ours")
            elif key == "table" and not val.startswith(OWN_NS + ":"):
                problems.append(f"{rel}: table {val} is outside the pack's own namespace")
        if rel.startswith("data/") and rel.split("/")[1] not in (OWN_NS, "neoforge", "tensura"):
            problems.append(f"{rel}: namespace {rel.split('/')[1]} is not one this pack may write to")
        if "/tags/" in rel and "entity_types" in rel:
            problems.append(f"{rel}: the 1.21 tag folder is entity_type (singular)")
    meta = json.loads((SRC / "pack.mcmeta").read_text(encoding="utf-8"))
    if meta["pack"]["pack_format"] != 48:
        problems.append("pack.mcmeta: pack_format must be 48")
    return problems


def build_zip():
    ZIP_OUT.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(ZIP_OUT, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        for p in files():
            info = zipfile.ZipInfo(p.relative_to(SRC).as_posix(), STAMP)
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o644 << 16
            info.create_system = 3
            z.writestr(info, p.read_bytes().replace(b"\r\n", b"\n"))


def build_jar_copy():
    if JAR_COPY.exists():
        shutil.rmtree(JAR_COPY)
    for p in files():
        rel = p.relative_to(SRC)
        if rel.name == "README.md":
            continue
        dst = JAR_COPY / rel
        dst.parent.mkdir(parents=True, exist_ok=True)
        dst.write_bytes(p.read_bytes().replace(b"\r\n", b"\n"))


def main():
    problems = validate()
    for line in problems:
        print("PROBLEM:", line)
    if problems:
        return 1
    if "--check" in sys.argv:
        print(f"ok: {len(files())} files, nothing written")
        return 0
    build_zip()
    build_jar_copy()
    print(f"wrote {ZIP_OUT.relative_to(ROOT)} and {JAR_COPY.relative_to(ROOT)} ({len(files())} files)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
