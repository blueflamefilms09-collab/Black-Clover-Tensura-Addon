"""build: bakes the duel-disk models of the deck relics (slice `duel_disk`, 1.110.0).

    python3 -B tools/visual_upgrade/duel_disk/build.py

Writes (all additive; nothing existing is edited):
    assets/nusmp/visual_upgrade/models/duel_disk_<id>_worn[_lod].txt, duel_disk_<id>_hand[_lod].txt     the disk (worn on the left forearm, held)
    assets/nusmp/visual_upgrade/models/deck_of_many_things_cards_{worn,hand}.txt                        the floating cards of the Deck of Many Things, copied QUAD FOR QUAD
                                                                                                          from the existing models (only moved beside the disk in the worn one)
    assets/nusmp/textures/visual_upgrade/duel_disk_<id>[_lod][_glow|_star].png
    assets/nusmp/visual_upgrade/overrides_duel_disk.txt, fx/pact_of_the_deck.json
    tools/lang/duel_disk.json
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import disk as D  # noqa: E402

KIT = D.KIT
import fxtex  # noqa: E402
import metrics as MT  # noqa: E402
import spec as SP  # noqa: E402
import vu as V  # noqa: E402

ROOT = MT.ROOT
MODELS = MT.VU_MODELS
INV = 1.0 / D.BODY_SCALE

DECKS = {
    "deck_of_many_things": dict(
        grade="GOD", skin=512, pal=(0x1A1A22, 0xE6B94A, 0xFFD27A),
        shell=("obsidian", (28, 26, 40)), trim=("gold", (232, 190, 80)), strap=("leather", (52, 30, 72)),
        cardback=((46, 36, 66), (230, 190, 90)), marks=["star", "star", "star", "star", "star"], text="XXII FATE",
        stars=True, halo=True, orbit=False, gem_base=(255, 200, 90), zone_base=(14, 12, 24), spell_glow=0x8A6AE8,
        star=dict(style="swirl", dark=(4, 2, 12), mid=(130, 96, 34), hot=(255, 214, 120))),
    "pact_of_the_deck": dict(
        grade="SPECIAL", skin=256, pal=(0xF2F0E6, 0xC9A94B, 0xFFF3C0),
        shell=("silver", (236, 232, 214)), trim=("gold", (214, 176, 80)), strap=("leather", (150, 30, 36)),
        cardback=((160, 30, 38), (240, 226, 190)), marks=["spade", "heart", "star", "diamond", "club"], text="PACT 54",
        stars=False, halo=False, orbit=True, gem_base=(230, 60, 60), zone_base=(248, 244, 232), spell_glow=0xC9A94B,
        star=None),
}
CAPS = {"GOD": dict(mesh=2500, drawn=2500), "SPECIAL": dict(mesh=2600, drawn=2600)}


def make(rid, S, lod):
    vm = V.VU("duel_disk_" + rid + ("_lod" if lod else ""), lod=lod, skin=(64 if lod else S["skin"]), seed=11)
    vm.pal = S["pal"]
    D.build_disk(vm, S)
    vm.build()
    return vm


def centre(vm):
    pts = [p for q in vm.quads() if not q["flags"] & (V.HALO | V.STAR) for p in q["P"]]
    import numpy as np
    a = np.array(pts)
    return (a.min(axis=0) + a.max(axis=0)) / 2


def bake(rid, S):
    full, low = make(rid, S, False), make(rid, S, True)
    name = "duel_disk_" + rid
    full.write_textures(name)
    low.write_textures(name + "_lod")
    star_name = None
    if S["stars"]:
        sd = S["star"]
        fxtex.save_star(os.path.join(V.TEX_DIR, name + "_star.png"), sd["dark"], sd["mid"], sd["hot"], sd["style"], 29)
        star_name = name + "_star.png"
    c = centre(full)
    HS = 1.7                                           # held: the disk is drawn larger than on the arm so it is the main thing next to the floating cards
    off_hand = (-float(c[0]) * HS, -float(c[1]) * HS, -float(c[2]) * HS)
    for kind, kw in (("worn", dict(anchor="larm", scale=INV, offset=(0, 0, 0))), ("hand", dict(anchor="body", scale=HS, offset=off_hand))):
        full.write_model("%s_%s" % (name, kind), name, star_tex=star_name, lod_name="%s_%s_lod" % (name, kind), comment="%s %s" % (S["grade"], kind), **kw)
        low.write_model("%s_%s_lod" % (name, kind), name + "_lod", star_tex=star_name, comment="%s %s lod" % (S["grade"], kind), **kw)
    fs, ls = full.stats(), low.stats()
    print("%-22s %-8s quads %4d (glowmap %3d star %3d halo %3d) verts %4d drawn %4d | lod %3d verts (%.1f%%)" % (
        rid, S["grade"], fs["quads"], fs["glowmap"], fs["star"], fs["halo"], fs["vertices"], fs["drawn"], ls["vertices"], 100.0 * ls["vertices"] / fs["vertices"]))
    return fs, ls


# ---------------------------------------------------------------- the floating cards of the Deck of Many Things, copied quad for quad
KEEP = ("fate_cards", "fate_cards_far")


def extract_cards(src_name, dst_name, anchor, shift):
    """Copies the animation groups `fate_cards` and `fate_cards_far` (their quads, uv, normals, flags and animation numbers) of an existing model into a new one. Nothing is
    edited but the position (shift = dx, dy, dz in the file frame, applied to the quads and to the pivots) and the group numbers."""
    lines = open(os.path.join(MODELS, src_name + ".txt"), encoding="utf8").read().splitlines()
    head, groups, quads = [], {}, []
    for ln in lines:
        v = ln.split()
        if not v or v[0] == "#" or ln.startswith("#"):
            continue
        if v[0] in ("skin", "glowtex", "startex"):
            head.append(ln)
        elif v[0] == "grp":
            groups[int(v[1])] = v
        elif v[0] == "q":
            quads.append(v)
    new_id = {}
    out = ["# the floating cards of the Deck of Many Things (groups %s of %s, copied quad for quad, moved by %s); generated by tools/visual_upgrade/duel_disk, do not edit" % (", ".join(KEEP), src_name, shift),
           "model vu/" + dst_name, "anchor " + anchor] + head
    for gi in sorted(groups):
        g = groups[gi]
        if g[2] in KEEP:
            new_id[gi] = len(new_id) + 1
            px, py, pz = float(g[4]) + shift[0], float(g[5]) + shift[1], float(g[6]) + shift[2]
            out.append("grp %d %s %s %.3f %.3f %.3f %s %s %s" % (new_id[gi], g[2], g[3], px, py, pz, g[7], g[8], g[9]))
    n = 0
    for q in quads:
        gi = int(q[1])
        if gi not in new_id:
            continue
        nums = q[4:]
        o = []
        for c in range(4):
            r = nums[c * 8:(c + 1) * 8]
            r[0] = "%.3f" % (float(r[0]) + shift[0])
            r[1] = "%.3f" % (float(r[1]) + shift[1])
            r[2] = "%.3f" % (float(r[2]) + shift[2])
            o += r
        out.append("q %d %s %s %s" % (new_id[gi], q[2], q[3], " ".join(o)))
        n += 1
    with open(os.path.join(MODELS, dst_name + ".txt"), "w", newline="\n") as fh:
        fh.write("\n".join(out) + "\n")
    print("%-34s %d quads from %s" % (dst_name, n, src_name))
    return n


def main():
    for rid, S in DECKS.items():
        bake(rid, S)
    # the cards: worn = beside and above the disk (arm frame, file units = final / 0.85); hand = where they were
    shift_worn = ((-5.0 + 4.98) * INV, (-2.0 - 0.5) * INV, 0.0)
    extract_cards("deck_of_many_things_worn", "deck_of_many_things_cards_worn", "larm", shift_worn)
    extract_cards("deck_of_many_things", "deck_of_many_things_cards_hand", "body", (0.0, 0.0, 0.0))
    write_data()


CARDS_C = D.CARDS_CENTRE


def write_data():
    vu = MT.VU_DIR
    lines = ["# duel disk override table (slice duel_disk, 1.110.0): read after overrides.txt by client/relics/vu/VuModels, so these lines win for the relics named here.",
             "# generated by tools/visual_upgrade/duel_disk/build.py, do not edit. visual.upgrade=false ignores this file (the 1.109 look).",
             "hand deck_of_many_things vu/duel_disk_deck_of_many_things_hand vu/deck_of_many_things_cards_hand | size=0.62 style=palm",
             "worn deck_of_many_things vu/duel_disk_deck_of_many_things_worn@larm vu/deck_of_many_things_cards_worn@larm",
             "hand pact_of_the_deck vu/duel_disk_pact_of_the_deck_hand | size=0.50 style=palm",
             "worn pact_of_the_deck vu/duel_disk_pact_of_the_deck_worn@larm"]
    with open(os.path.join(vu, "overrides_duel_disk.txt"), "w", newline="\n") as fh:
        fh.write("\n".join(lines) + "\n")
    fx = {"id": "pact_of_the_deck", "grade": "SPECIAL", "ambient_sound": "nusmp:relic.ambient.pact_of_the_deck", "draw_ticks": 10, "layers": [
        SP.geo_layer("card fan", "ribbons", "card_fan"), SP.geo_layer("pact cards", "orbit_shards", "pact_cards"), SP.geo_layer("life points", "pulse_glow", "life_points"),
        SP.geo_layer("deck top card", "ribbons", "deck_top_card"),
        SP.part_layer("pact dust", "motes", "dust", 0xFFF3C0, count=1, every=6, radius=0.4, rise=-0.01),
        SP.part_layer("pact glints", "glints", "end_rod", None, count=1, every=10, radius=0.45, rise=0.01)]}
    with open(os.path.join(MT.VU_FX, "pact_of_the_deck.json"), "w", newline="\n") as fh:
        json.dump(fx, fh, indent=1)
        fh.write("\n")
    lang = {
        "duel_disk.option.name": "Duel disk look for deck relics",
        "duel_disk.option.tooltip": "Deck relics (Deck of Many Things, Pact of the Deck) are worn as a card duel disk on the left forearm and held as a disk with a card stack. Off: the older look. Shares the switch visual.upgrade.",
        "duel_disk.credit": "The duel disk is a third-party design used as a visual base only.",
    }
    os.makedirs(os.path.join(ROOT, "tools", "lang"), exist_ok=True)
    with open(os.path.join(ROOT, "tools", "lang", "duel_disk.json"), "w", newline="\n") as fh:
        json.dump(lang, fh, indent=2)
        fh.write("\n")


if __name__ == "__main__":
    main()
