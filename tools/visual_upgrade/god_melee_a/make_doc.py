"""Writes docs/visual_upgrade/god_melee_a.md from the two measurement files (so the numbers in the document are the numbers of the files) and the per-item notes below.

    python3 -B tools/visual_upgrade/god_melee_a/make_doc.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import common as C                                                     # noqa: E402

NOTES = {
    "staff_of_the_magi": ("Dungeons and Dragons: the master staff, a twisted violet shaft bound in gold, a floating crystal.",
                          "An armillary of three counter-turning rune rings (the outer one carries ten glowing runes) and six shards orbiting the crystal; three filigree tendrils and four gems round the claw cradle; "
                          "three gold sleeves engraved with a Latin lore line; two tassels of silk; a four-clawed butt; veins running up the violet strands."),
    "wizard_kings_vow": ("Black Clover: the Wizard King's rapier, a four-leaf clover guard, a basket ring, a mana gem pommel.",
                         "Six mana petals turning round the blade (the Magic Zone) and a gyroscope ring; a five-leaf crest on both faces of the blade root; clover wings on the guard; two more basket bars; "
                         "the cape as two silk ribbons from the pommel; gold wire wound on the grip; five petals round the pommel gem; the vow engraved down the blade."),
    "wand_of_the_wisdom_king": ("Tensura: the Great Sage as a wand, a crystal shaft with a floating core and satellites.",
                                "Three hexagonal gyroscope rings (six gems each) and a cloud of eight data shards turning round the core; a turning interface disc (two rings, six spokes, 24 ticks) behind it; "
                                "hexagonal collars with a thought-text; four crystal fins on the cradle; a floating gem under the butt; printed circuit traces on the shaft."),
    "hammer_of_thunderbolts": ("Dungeons and Dragons: a huge blue-grey hammer head with gold bands and a forked bolt, on an iron haft.",
                               "Three forked bolts circling the head and four storm orbs; five lightning rods crowning the head; 24 gold rivets; two gold end plates; a chain with a bolt charm from the lanyard; "
                               "four anvil flanges on the butt; thunder script on the head and the haft; veins in the steel."),
    "gungnir_the_unerring": ("Norse myth: Odin's spear, a rune-carved golden leaf head, a blue wrapped grip, a gold wire spiral.",
                             "Huginn and Muninn, two ravens (body, swept wings, tail, beak, a glowing eye) flying round the head; nine rune stones turning at the socket; four gold socket wings; "
                             "eight barbs on the head; two rune sleeves; blue and gold cord tassels; the head engraved with the spear's names."),
    "kusanagi_the_grass_cutter": ("Japanese myth: the sword of the serpent's tail, a curved blade with a wind hamon, a cloud guard, a jade pommel.",
                                  "Eight jade magatama (the eight heads) turning at the guard; three wind rings with beads round the blade; four cloud scrolls on the guard; a sacred rope with four paper streamers on the grip; "
                                  "a jade tassel; an inscription and wind marks engraved in the blade; wind veins."),
    "leviathans_crown_trident": ("Greek myth: a sea king's three-pronged crown of blue steel and coral, a pearl, a wave-etched shaft.",
                                 "Two wave rings and six bubbles turning round the prongs; a real crown of seven pearl-tipped points; shell fans; three hanging kelp ribbons; fish scales etched down the shaft; "
                                 "two sleeves with a sea lore line; the central prong engraved."),
    "calamity_wave_aegis": ("The Rising of the Shield Hero: a sea-blue heater shield with gold wave crests and a sapphire boss.",
                            "A ring of eight sapphire beads and three crescent wave blades turning round the boss; three more wave crests; a star of eight spikes round the boss; two scroll curls; nine sea-fan fins along the top; "
                            "waves and a lore line engraved in the face."),
    "hellblaze_the_demon_sin": ("The Seven Deadly Sins: a black jagged broadsword, lava veins, a horned demon guard.",
                                "Six tongues of black flame turning round the blade and a demon ring of eight thorns at the guard; the Sin's eye on both faces of the guard; a second pair of horns; a spiked ball on a chain; "
                                "fourteen extra teeth along the torn edge; a crown of flames at the pommel; the name engraved down the blade."),
    "minotaur_kings_labrys": ("DanMachi: a huge double-headed axe of dark iron, serrated rims, horn tips, ember cracks.",
                              "A halo of twelve spectral axe heads turning in front of the hub; a bull's brow and a nose ring on the hub; 26 rim teeth; three trophy cords with bone teeth; studded iron bands; a spiked butt; "
                              "a labyrinth engraved in both blades; a lore line on the haft."),
    "dragon_force_twin_fangs": ("Fairy Tail: two curved dragon-fang daggers with ember spines and red cord grips.",
                                "A halo of ten dragon scales turning round the pair and three flame tongues turning round each fang; claw guards (three claws each); dorsal spines along the blades; dragon eyes in the pommels; "
                                "red cord tassels; dragon scales and a lore line etched in the bone blades."),
    "dawnpiercer_crossbow": ("Original: a dark-wood crossbow with a sun-gold prod, a glowing sunstone and a gilt bolt.",
                             "A sun halo (two rings, sixteen rays) turning behind the prod; ribs of sun rays across both limbs (they flex with the draw); sights; three sun inlays and filigree on the stock; "
                             "a sunray fletching on the loaded bolt; a tassel; the draw states and the loaded state are rebuilt from the same parts."),
}


def table(rows, head):
    out = ["| " + " | ".join(head) + " |", "|" + "---|" * len(head)]
    for r in rows:
        out.append("| " + " | ".join(str(x) for x in r) + " |")
    return "\n".join(out)


def main():
    base = {r["id"]: r for r in json.load(open(os.path.join(C.DOCS, "god_melee_a_baseline.json")))["items"]}
    aft = json.load(open(os.path.join(C.DOCS, "god_melee_a_after.json")))
    items = aft["items"]
    summ = aft["summary"]
    rows1, rows2, rows3, rows4 = [], [], [], []
    for r in items:
        b = base[r["id"]]
        q = r["ratios_vs_baseline"]
        lb = b["mesh"]["layers"]
        la = r["mesh"]["layers"]
        near_b = 4 * (lb.get("0", 0) + lb.get("2", 0))
        n_orb = sum(v for k, v in la.items() if int(k) >= 3)
        near_a = 4 * (la.get("0", 0) + n_orb + 3 * la.get("2", 0) + n_orb)       # body, orbit parts, core glow + veins + halo (3 passes over the glow layer), the orbit glow
        rows1.append(["`%s`" % r["id"], b["kind"], b["mesh"]["quads_total"], r["mesh"]["quads_total"], q["mesh_quads"], b["mesh"]["unique_vertices"], r["mesh"]["unique_vertices"],
                      r["lod"]["quads_total"], "%.1f%%" % (100.0 * r["lod"]["quads_total"] / r["mesh"]["quads_total"])])
        rows2.append(["`%s`" % r["id"], "%dx%d" % tuple(b["skin"]["size"]), "%dx%d" % tuple(r["skin"]["size"]), b["skin"]["painted_texels"], r["skin"]["painted_texels"], q["skin_painted_texels"],
                      b["glow_map"]["painted_texels"], r["glow_map"]["painted_texels"], r["extra_maps"]["veins"]["painted_texels"]])
        rows3.append(["`%s`" % r["id"], "%d -> %d" % (b["passes"]["count"], r["passes"]["count"]), "%d -> %d" % (b["ambient"]["count"], r["ambient"]["count"]),
                      "%d -> %d" % (len(b["animation"]["idle"]), len(r["animation"]["idle"])), "no -> yes", "0 -> %d" % len(r["act_vfx"])])
        rows4.append(["`%s`" % r["id"], b["V"], r["V"], q["V"], r["component_ratios"]["mesh"], r["component_ratios"]["texture"], r["component_ratios"]["glow"], r["component_ratios"]["ambient"],
                      r["component_ratios"]["animation"], near_b, near_a])
    notes = []
    for r in items:
        lore, built = NOTES[r["id"]]
        notes.append("* **`%s`** (%s) %s *Built:* %s" % (r["id"], r["kind"], lore, built))
    md = f"""# Visual upgrade 1.102.0, God melee (a): twelve God grade hand weapons of wave 71

Owner request (verbatim): "Upgrade God Grade weapons/relics to look 5x better and Genesis grade to look 20x better." This document covers the **god_melee_a** slice: `staff_of_the_magi`, `wizard_kings_vow`,
`wand_of_the_wisdom_king`, `hammer_of_thunderbolts`, `gungnir_the_unerring`, `kusanagi_the_grass_cutter`, `leviathans_crown_trident`, `calamity_wave_aegis`, `hellblaze_the_demon_sin`,
`minotaur_kings_labrys`, `dragon_force_twin_fangs`, `dawnpiercer_crossbow`. Nothing here has been seen in a game (none can be run in the build environment); every number is a **proxy measured from the files**,
and "5x better looking" is not claimed as a fact about how the weapons look.

## 1. Short version

* Every item keeps its id, stats, acts, passives, engravings, Better Combat file, display preset, old mesh, old skins and old sprite (the inventory icon). The upgrade is **added next to them**: a new mesh
  `weapon_meshes/up/<id>.txt` with its own 1024 skin, glow map and veins map under `textures/entity/weapon/up/`, a low-detail mesh `<id>_lod.txt`, and an effect spec `visual_upgrade/<id>.json`.
  The client option `visual.upgrade` (config/nusmp-visual-upgrade-client.toml) puts every item back on its shipped look at once.
* Every item has: a mesh with new lore-shaped parts (all old parts stay), a **skin of at least 4x the painted texels** (measured {min(r["ratios_vs_baseline"]["skin_painted_texels"] for r in items):.1f}x to {max(r["ratios_vs_baseline"]["skin_painted_texels"] for r in items):.1f}x) with hammered / worn metal,
  ambient occlusion and a bevel line at every plate border, scratches and engraved lore text or patterns, **two emissive maps** (core glow and energy veins), **four passes** (skin, core glow, flowing veins, halo shell),
  **six ambient effect layers**, **five idle animations**, a **swing trail of its own pattern**, a **signature effect for each of its three acts**, and a **low-detail mesh** of at most 15% of the quads.
* **Not met, said plainly:** the composite visual score V did not reach 5x for any item (ratio {summ["V_ratio_min"]:.2f}x to {summ["V_ratio_max"]:.2f}x, mean {summ["V_ratio_mean"]:.2f}x), and the mesh did not grow to 5x its baseline quads.
  Section 7 says why (the shipped meshes already had 1000 to 3200 quads, not the 100 to 300 the brief assumed, and the performance guard of 2500 welded vertices forbids 5x of that).

## 2. How "better" is measured (the proxies)

Baseline = the shipped 1.101.0 files of each item, measured before anything was changed (`{os.path.join("docs", "visual_upgrade", "god_melee_a_baseline.json")}`, first commit of the slice); after = the new files
(`god_melee_a_after.json`). Both are written by `python3 -B tools/visual_upgrade/god_melee_a/measure.py baseline|after` and checked by `UpgradeGodMeleeATest`.

Score **V = 0.35 mesh + 0.20 texture + 0.15 passes + 0.20 ambient + 0.10 animation** (the weights of the brief's example). Each component is a plain count over one fixed reference, never capped:
mesh = quads in the mesh file / 600 (the God floor); texture = painted texels (alpha above 0) of the skin / 262144 (a full 512 x 512 skin); passes = material and glow passes drawn / 2 (the Genesis Demon-Slayer's
standard: skin and glow); ambient = ambient effect layers / 3 (the God minimum); animation = idle animations + 1 for a swing trail + the share of the item's acts that have a signature effect.

What a held God weapon had before, found in the code: 2 passes (the cutout skin and the additive glow, `WeaponRenderer.renderByItem`), 1 idle animation (the glow tint breathing, `VariantRules.pulse`), 1 ambient layer
(the grade shimmer of `RelicGradeFx`: one mote round a carrier of a God relic every 8 ticks), no swing trail, and acts that only used the generic shapes of `Wave71Acts` (`VfxShape.SLASH_WAVE` ...), shared by many weapons.

Welded vertices = distinct positions at two decimals (a vertex shared by the quads that meet there counts once); emitted vertices = 4 x quads (what `WeaponRenderer.emit` writes).

## 3. Mesh

{table(rows1, ["item", "class", "quads before", "quads after", "ratio", "welded before", "welded after (cap 2500)", "low-detail quads", "share"])}

## 4. Skin and glow maps

{table(rows2, ["item", "skin before", "skin after", "painted texels before", "painted texels after", "ratio", "glow map painted before", "glow map painted after", "veins map painted"])}

The old builders were run on a 1024 skin instead of 512 and with a denser packer (`ukit.pack_maxrects`, MaxRects instead of the shelf packer of `mesh_lib`), so every part also has a higher px per cm than before
(for example the Wizard King's Vow from 8.4 to 10.3). On top of the old painting every part gets `skinkit.detail_part` (dents and pits in metal, grain in wood, ambient occlusion and a light bevel line at the border of every
plate, scratches along the form), the lore text and patterns are engraved with `skinkit.Engraver` (a dark groove with a light rim and, where it glows, a glow line; letters are never smaller than 7 x 9 texels),
and the veins are the ridges of a noise field stretched along the part (`skinkit.paint_veins`, feature sizes in centimetres so they look alike at every resolution).

## 5. Passes, layers, animation

{table(rows3, ["item", "passes", "ambient layers", "idle animations", "swing trail", "act signatures"])}

## 6. Score

{table(rows4, ["item", "V before", "V after", "ratio", "mesh x", "texture x", "passes x", "ambient x", "animation x", "emitted vertices near, before", "emitted vertices near, after (worst case)"])}

Mean ratio {summ["V_ratio_mean"]:.2f}x, items at 5x or more: {summ["items_reaching_5x_V"]} of {summ["items"]}. The components that could grow did: texture (4x to {max(r["component_ratios"]["texture"] for r in items):.0f}x), passes 2x, ambient 6x, animation 7x;
the mesh component (35% of V) grew {min(r["component_ratios"]["mesh"] for r in items):.1f}x to {max(r["component_ratios"]["mesh"] for r in items):.1f}x.

## 7. Honest limits

1. **The 5x composite was not reached, and the mesh could not grow 5x.** The brief's rules contradict each other for these items: "mesh detail at least 5x its baseline quads" and "at most 2500 vertices at full detail". The shipped
   God meshes already had 982 to 3170 quads (688 to 1768 welded vertices), so 5x would be 5000 to 16000 quads (20000 to 64000 emitted vertices) in the hand of one player, 8 to 25 times the guard. The guard was kept (it is the owner's
   "do not undo the performance pass" rule), counted in **welded vertices** (every item is under 2500; the emitted count is 4 x quads and is reported too), so the mesh grew {min(r["ratios_vs_baseline"]["mesh_quads"] for r in items):.2f}x to {max(r["ratios_vs_baseline"]["mesh_quads"] for r in items):.2f}x.
   The score V then cannot reach 5x for an item whose baseline is mesh-heavy: V_before of the Staff of the Magi is {base['staff_of_the_magi']['V']:.2f} and {0.35 * base['staff_of_the_magi']['V_components']['mesh']:.2f} of it is the mesh.
2. **Nothing was seen in a game.** The previews in `docs/previews/visual_upgrade/god_melee_a/` are the project's software rasteriser (flat light, no bloom, orbiting parts frozen at phase 0, the additive passes drawn as the
   tool draws every glow); they show the geometry and the textures, not the look of the passes, the particles or the signature effects.
3. **Cost.** The upgraded meshes emit more vertices than the shipped ones (near the camera, worst case, table in section 6): the body, the orbiting parts, the core glow, the veins pass and the halo shell all walk the quads
   (the halo only on high effect quality, the orbiting glow not on low). All of it is render-thread work on lists built at load, with no allocation per frame (`UpgradeCoreTest` scans the hot methods), and from 12 blocks the
   low-detail mesh (at most 15% of the quads, no extra passes) is drawn, from 24 blocks without its glow. It has **not been profiled**; the numbers are a cost model, not a measurement.
4. **The ambient layers and swing trails are approximations.** Their positions are computed from the holder's body yaw and a hand offset, not from the animated arm; during a swing the particles are laid on an arc
   in front of the holder, not on the real blade. They are budgeted (distance, density option, the client particle allowance of the performance pass) but their look is untested.
5. **The inventory keeps the old sprite** (rule 7). The brief also asked for the 3D `gui` model to show the new mesh "where the item pipeline allows": `WeaponRenderer` deliberately draws the sprite model in the inventory,
   so this was **not done** (it would put a 3000-quad mesh in every slot).
6. No lang text was added (the config comments are the only new text), so there is no lang fragment.

## 8. What was built per item

{chr(10).join(notes)}

## 9. NEW / REPLACED / KEPT

| | what |
|---|---|
| **NEW** | per item: `weapon_meshes/up/<id>.txt`, `<id>_lod.txt` (the crossbow also `_pull1..3`, `_charged`), `textures/entity/weapon/up/<id>.png`, `_glow.png`, `_veins.png`, `_lod.png`, `_lod_glow.png`, `visual_upgrade/<id>.json` |
| **NEW** | generators `tools/visual_upgrade/god_melee_a/` (`ukit`, `skinkit`, `specs`, `items_*`, `build`, `measure`, `preview`, `make_doc`), measurement files `docs/visual_upgrade/god_melee_a_{{baseline,after}}.json`, previews `docs/previews/visual_upgrade/god_melee_a/` |
| **NEW** | client `client/WeaponUpgrade` (upgraded meshes, level of detail, orbiting parts, veins and halo passes, flicker), `client/upgrade/` (`UpgradeSpec`, `UpgradeSpecs`, `UpgradeConfig`, `UpgradeFx`, `UpgradeClient`), `vfx/client/layer/UpgradeSignatureLayer` |
| **NEW** | common `relics/upgrade/` (`UpgradeItems`, `UpgradeSignatures`, `UpgradeSelfTest`), config file `nusmp-visual-upgrade-client.toml`, self-test `visual.god_melee_a`, tests `UpgradeGodMeleeATest`, `UpgradeCoreTest`, `UpgradedMeshNormaliserTest` |
| **REPLACED** | nothing: the old meshes, skins, glow maps, sprites, item models and act effects are all still there and still used when `visual.upgrade` is off |
| **KEPT** | ids, registry names, stats, acts and their old effects, passives, engravings, Better Combat files, display presets, size classes; the Time Magic VFX (not touched) |

Additions to existing files (all one-liners or small and append-only): `client/WeaponRenderer` (`mesh()` asks `WeaponUpgrade` first and the old body is now `plainMesh()` + `read()`; the `orbit` directive and layers 3 and up in `parse`;
one field `Mesh.up`; the level-of-detail pick, the glow flicker factor and the `WeaponUpgrade.draw` call in `renderByItem`; `WeaponUpgrade.clear()` in the reload listener), `vfx/VfxShape` (appended `UPGRADE_SIGNATURE`),
`vfx/client/VfxManager` (one `register` line), `relics/RelicEngine` (one line after an act's body: `UpgradeSignatures.afterAct`), `NUSMP` (two lines: `UpgradeClient.init` in the client block, `UpgradeSelfTest.register` next to `PerfBoot.init`),
`tools/relics/new197_build.py` (one call at the end of `main`: the entry hook of the generators).

## 10. How it runs

* **Mesh choice.** `WeaponRenderer.mesh(id)` -> `WeaponUpgrade.mesh(id)`: option on, id (or the base id of a draw state) in `UpgradeItems`, its spec found -> the upgraded mesh (read by the same `read()` as every mesh, same placement line as the shipped one,
  same normaliser, and `UpgradedMeshNormaliserTest` proves the normaliser has nothing to do on any of them). Otherwise the shipped mesh. A broken file is logged once and the shipped look is used.
* **Level of detail.** `WeaponUpgrade.pick`: the distance of the item's pose from the camera (the length of the translation of the matrix): under the spec's 12 blocks the full mesh, to 24 the low-detail mesh, beyond it the low-detail mesh without glow.
* **Orbiting parts.** `orbit <layer> <pivot> <axis> <degrees per tick> <phase> <glow>` lines in the mesh file; the quads of that layer (3 and up) are kept apart from the normaliser, turned about the pivot each frame by one scratch quaternion.
* **Passes.** core glow (the existing pass, with a slow flutter `rune_flicker`), **veins** (`RenderType.eyes` on the veins map, the alpha of every vertex a wave of its model height and the game time), **halo shell** (`entityTranslucentEmissive` on the
  glow map, scaled 4.5% across, its alpha breathing). Quality: no halo on medium or low, no orbiting glow on low.
* **Ambient layers** (`UpgradeFx`): per client tick for at most 6 holders within 40 blocks, density by distance (full to 12 blocks, then 45%, beyond 24 only the ground layer) times `visual.ambientDensity` times the effect quality, every spawn taken from `PerfBudgets.clientParticles`.
  Kinds: `motes`, `ring`, `orbit`, `edge`, `arc`, `shaft`, `ground`, `drip`; particles are coloured dust or vanilla particles named in the spec.
* **Swing trail** (`UpgradeFx.swing`): while the holder swings, 3 to 6 particles per tick on the arc of the swing, patterned by the spec (`sigil`, `clover`, `circuit`, `bolt`, `thrust`, `wind`, `wave`, `flame`, `crescent`, `claw`, `ray`).
* **Act signatures.** `RelicEngine` calls `UpgradeSignatures.afterAct` after an act's body; it sends one `UPGRADE_SIGNATURE` effect (the caster, the point he looks at, the crc32 of "item:act" in the seed); `UpgradeSignatureLayer` draws the recipe
  of that act (rings, glyph discs, columns, bursts, spirals, beams, rain, forked bolts, orbiting marks, flashes) out of the effect system's primitives, inside its 400 vertex budget. The act's old effect still plays.
* **Rebuild.** `python3 -B tools/relics/new197_build.py` (the hook) or `python3 -B tools/visual_upgrade/god_melee_a/build.py [ids]`; then `measure.py after`, `preview.py`, `make_doc.py`. Deterministic.

## 11. Tests and checks

`UpgradeGodMeleeATest` (files, sizes, welded vertex cap, low-detail share, orbit lines, spec contents, acts equal the weapon's acts, the ratios of the after file are the numbers of the files and the 5x flag agrees with the number),
`UpgradeCoreTest` (list, signature id, spec parser, no allocation in the hot paths, the budget is taken before a particle is spawned, the shipped look is the fallback), `UpgradedMeshNormaliserTest`; the existing
`WeaponMeshesTest`, `BowDisplayGuardTest`, `Wave71AssetsTest` still pass. Self-test `visual.god_melee_a` (jar contents, specs parse, acts have signatures, the old files are still there, the option answers before the config loads).
Type-checked against the API stubs (the one remaining error, `MobEffectInstance is abstract`, is the known stub artefact in `RelicEngine`, line 227, not in a changed line).
"""
    with open(os.path.join(C.DOCS, "god_melee_a.md"), "w", encoding="utf-8", newline="\n") as fh:
        fh.write(md)
    print("wrote", os.path.join(C.DOCS, "god_melee_a.md"))


if __name__ == "__main__":
    main()
