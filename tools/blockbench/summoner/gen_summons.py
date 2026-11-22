"""Writes the Hell Mode summon models (docs/summoner_creatures.md): geo, animation, sheet, glow mask and Blockbench source for each species.

    python3 -B tools/blockbench/summoner/gen_summons.py [--only id,id] [--preview]

A script fallback (rule 15): no Blender or Blockbench was available; nothing here was made in an app.
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import summon_lib as L  # noqa: E402
import species_a as A  # noqa: E402
import species_b as B  # noqa: E402
import species_c as Cc  # noqa: E402

BUILDERS = {"hayate": A.build_hayate, "cerorin": A.build_cerorin, "rocanel": A.build_rocanel, "mira": A.build_mira, "hatch": B.build_hatch, "tsubame": B.build_tsubame,
            "solarin": B.build_solarin, "genbu": B.build_genbu, "ellie": Cc.build_ellie, "okiyosan": Cc.build_okiyosan, "dradra": Cc.build_dradra, "merus": Cc.build_merus}


def main():
    only = None
    if "--only" in sys.argv:
        only = set(sys.argv[sys.argv.index("--only") + 1].split(","))
    out, stats = {}, []
    for sid, fn in BUILDERS.items():
        if only and sid not in only:
            continue
        pal, clips, top, width = fn()
        st, sheet, desc, geo, anims = L.export("summon_" + sid, pal, clips, top, width)
        stats.append(st)
        out[sid] = (sheet, geo, anims, st)
        print("%-9s bones=%3d cubes=%3d quads=%4d k=%.2f height=%.2f lit=%d" % (sid, st["bones"], st["cubes"], st["quads"], st["density"], st["height_blocks"], st["lit_texels"]))
    if not only:
        json.dump(stats, open(os.path.join(HERE, "summon_stats.json"), "w"), indent=1)
    if "--preview" in sys.argv:
        import preview
        preview.main(out)


if __name__ == "__main__":
    main()
