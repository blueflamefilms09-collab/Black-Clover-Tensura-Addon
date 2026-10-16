"""Contact sheets of the relics as held objects (no game, no GPU): docs/relic_hand/*.png

    python3 tools/relics/hand_contact_sheet.py [--out docs/relic_hand] [--only held|objects|slots]

  objects_<n>.png   every hand model on its own, two views, in the colours of the relic that holds it
  held_<name>.png   representative relics of each kind as the full rows of tools/relics/hand_preview.py (third person from the front and the side, first person, ground, item frame, sprite)
  all_<slot>.png    every relic of a slot as a small cell: the third person close view and its sprite

Uses the rasteriser of hand_preview.py (the same maths as client/relics/hand/HandItemRenderer). Judge the silhouette, the way it is held and the colours; the arm is a plain box and
the first person view is an approximation (docs/relic_hand_models.md says what is not replicated).
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import hand_preview as H  # noqa: E402
import relic_inventory as RI  # noqa: E402
from PIL import Image  # noqa: E402

ROOT = H.ROOT

HELD = {
    "necklaces": ["amulet_of_the_forest", "pendant_of_echoed_souls", "lovelorn_locket", "sunheart_medallion", "witchers_medallion", "eye_of_the_aboleth"],
    "head": ["gold_circlet", "dreamwalker_cowl", "captains_tricorn", "tiny_duck_crown", "mask_of_indifference", "helm_of_whispers"],
    "hands_feet": ["bracers_of_phasing", "glove_of_appreciation", "elemental_hand_wraps", "phantoms_bracelet", "ruby_boots_of_planestriding", "sandals_of_sneaking"],
    "back_belt_chest": ["cloak_of_levitation", "cape_of_nine_lives", "alchemists_belt", "armor_of_the_lionheart", "starstudded_leathers", "skitchs_smuggling_coat"],
    "stones_artefacts": ["the_space_stone_or_the_tesseract", "the_time_stone_or_the_eye_of_agamotto", "the_infinity_gauntlet", "hand_of_vecna", "eye_of_vecna", "deck_of_many_things"],
    "tools_music": ["spirit_lantern", "soup_spoon_of_improbability", "bewitching_lyre", "horn_of_leroy", "warrens_key", "instant_cooking_pot"],
    "small_things": ["ring_of_power", "ring_of_memories", "coin_of_fortune", "pocket_time", "monster_compass", "lucky_egg"],
    "bags_figures": ["bag_of_holding", "bag_of_kittens", "snugglable_doll", "crystal_dragon", "lumpy", "arcanists_notebook"],
    "consumables": ["holy_water", "fey_honey", "scroll_of_absorption", "genie_gin", "potion_of_vampirism", "mad_marbles"],
}

OBJECT_ORDER = None


def save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    print("wrote", os.path.relpath(path, ROOT), img.size)


def held_sheets(out, by):
    for name, ids in HELD.items():
        rows = [H.relic_row(by[i], (150, 190)) for i in ids]
        rows = [r for r in rows if r is not None]
        sheet = Image.new("RGB", (rows[0].width, sum(r.height for r in rows)))
        y = 0
        for r in rows:
            sheet.paste(r, (0, y))
            y += r.height
        save(sheet, os.path.join(out, "held_%s.png" % name))


def object_sheets(out):
    import gen_hand_all as A
    models, assign = A.collect()
    models.update(A.gen_hand_garments.MODELS)
    used = sorted({n.split("@")[0].rstrip("~") for ms in assign.values() for n in ms} & set(models))
    # the files that exist: every model of the catalog that is a hand model
    names = [n for n in used if os.path.exists(os.path.join(H.HAND, n + ".txt"))]
    for i in range(0, len(names), 12):
        save(H.model_grid(names[i:i + 12], cols=3, panel=(150, 170)), os.path.join(out, "objects_%02d.png" % (i // 12 + 1)))


def slot_sheets(out, rs):
    atlas = None
    for slot in ("NECK", "HEAD", "HANDS", "FEET", "BACK", "BELT", "CHEST", "CHARM", "CONSUMABLE"):
        ds = [r for r in rs if r["slot"] == slot and not r["adopted"]]
        cells = [c for c in (H.held_cell(d) for d in ds) if c is not None]
        if not cells:
            continue
        cols = 6
        w, h = cells[0].size
        rows = (len(cells) + cols - 1) // cols
        sheet = Image.new("RGB", (w * cols, h * rows), (30, 32, 40))
        for i, c in enumerate(cells):
            sheet.paste(c, ((i % cols) * w, (i // cols) * h))
        save(sheet, os.path.join(out, "all_%s.png" % slot.lower()))


def main(argv):
    out = os.path.join(ROOT, "docs", "relic_hand")
    if "--out" in argv:
        out = os.path.abspath(argv[argv.index("--out") + 1])
    only = argv[argv.index("--only") + 1] if "--only" in argv else None
    rs, _ = RI.relics()
    by = {r["id"]: r for r in rs}
    if only in (None, "held"):
        held_sheets(out, by)
    if only in (None, "objects"):
        object_sheets(out)
    if only in (None, "slots"):
        slot_sheets(out, rs)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
