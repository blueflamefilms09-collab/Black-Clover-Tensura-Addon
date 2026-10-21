"""Writes docs/visual_upgrade/djinn_dark_weapons.md from the measurements (docs/visual_upgrade/djinn_dark_weapons_{baseline,after}.json), the roster and the data tables, so the numbers in the
page cannot drift from the files:

    python3 -B tools/visual_upgrade/djinn_dark_weapons/make_doc.py
"""
import json
import os
import statistics
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import measure as M                                  # noqa: E402
import ditems as I                                   # noqa: E402

OUT = os.path.join(M.DOCS, "djinn_dark_weapons.md")


def load(name):
    return json.load(open(os.path.join(M.DOCS, name), encoding="utf-8"))


def main():
    base = {r["item"]: r for r in load("djinn_dark_weapons_baseline.json")["items"]}
    after = load("djinn_dark_weapons_after.json")
    items = after["items"]
    mesh_r = [r["ratios"]["mesh"] for r in items]
    tex_r = [r["ratios"]["texture"] for r in items]
    v_r = [r["V_after"] for r in items]
    vb_r = [r["V_after_brief_weights"] for r in items]
    welded = [r["mesh"]["vertices_welded"] for r in items]
    quads_before = [base[r["item"]]["mesh"]["quads_full_detail"] for r in items]
    quads_after = [r["mesh"]["quads_full_detail"] for r in items]
    lod = [r["mesh"]["lod_vertices"] // 4 for r in items]
    under5 = [r["item"].replace("djinn_weapon_", "").replace("_dark", "") for r in items if r["ratios"]["mesh"] < 5.0]
    rows = []
    for r in items:
        did = r["item"].replace("djinn_weapon_", "").replace("_dark", "")
        sp = I.SPECS[did]
        lk = sp["look"]
        spec = json.load(open(os.path.join(M.ASSETS, "visual_upgrade", r["item"] + ".json"), encoding="utf-8"))
        orb = spec["orbiters"][0]
        rows.append("| %s | %s | %s | %s / %s / %s / %s | %s (x%d) |" % (sp["name"], sp["kind"], sp["affinity"], lk["horns"].lower(), lk["wings"].lower(), lk["tail"].lower(), lk["halo"].lower(), sp["orbit"], orb["count"]))
    recipe_table = "\n".join(rows)
    size = 0
    for d in (os.path.join(M.ASSETS, "textures", "entity", "weapon", "up"), os.path.join(M.ASSETS, "weapon_meshes", "up"), os.path.join(M.ASSETS, "visual_upgrade")):
        for f in os.listdir(d):
            if f.startswith("djinn_weapon_"):
                size += os.path.getsize(os.path.join(d, f))
    mb = round(size / 1048576.0)
    import io
    buf = io.StringIO()
    old_stdout = sys.stdout
    sys.stdout = buf
    M.md_table()
    sys.stdout = old_stdout
    table = buf.getvalue()

    text = TEMPLATE.format(
        n=len(items), mesh_min=min(mesh_r), mesh_max=max(mesh_r), mesh_mean=statistics.mean(mesh_r), tex_min=min(tex_r), tex_max=max(tex_r), tex_mean=statistics.mean(tex_r),
        v_min=min(v_r), v_max=max(v_r), v_mean=statistics.mean(v_r), vb_min=min(vb_r), vb_max=max(vb_r), vb_mean=statistics.mean(vb_r),
        wmin=min(welded), wmax=max(welded), qb_min=min(quads_before), qb_max=max(quads_before), qa_min=min(quads_after), qa_max=max(quads_after), lod_min=min(lod), lod_max=max(lod),
        under5=(", ".join(under5) if under5 else "none"), table=table, mb=mb, recipe_table=recipe_table)
    open(OUT, "w", encoding="utf-8", newline="\n").write(text)
    print(OUT)


TEMPLATE = """# Visual upgrade, slice `djinn_dark_weapons` (release 1.108.0): the 36 dark Djinn weapons

Owner request: "Upgrade God Grade weapons/relics to look 5x better". "5x better looking" cannot be measured, so this page records **measured proxies** (before and after, read from the
shipped files) and says plainly what they do and do not show. **Nobody has seen these weapons in a game**: nothing here could be run (no Gradle, no game); the numbers come from the files, the
pictures in `docs/previews/visual_upgrade/djinn_dark_weapons/` from the project's software rasteriser, not from the game's renderer.

The slice: the 36 **dark** Djinn weapons `djinn_weapon_<name>_dark` (God grade, `WeaponGrades.djinn(kind, true)`; the light twins are Legend grade and are not touched). They were left out of the
two earlier God slices (`god_melee_a`: the twelve God weapons of Wave 71; `god_melee_b`: the rift pocket weapons and the Paradox) as "cut: the 36 dark Djinn weapons"
(`docs/visual_upgrade/god_melee_b.md`, section 4, note 4). A dark Djinn weapon is the Djinn Equip's loaned main-hand weapon (`DjinnWeaponItem`, loan rules in `docs/djinn_weapons.md`).

## 1. NEW / REPLACED / KEPT

| | What |
|---|---|
| **NEW** | for each of the 36: an upgraded mesh `weapon_meshes/up/<id>.txt`, its level-of-detail mesh `up/<id>_lod.txt`, the mesh of its one orbiting piece `up/<id>_orbit.txt`; skins `textures/entity/weapon/up/<id>.png` (1024 px), `_glow.png` (core glow, additive) and `_glow2.png` (energy veins, a separate emissive map); the effect table `assets/nusmp/visual_upgrade/<id>.json` (36 files) |
| **NEW** | generators `tools/visual_upgrade/djinn_dark_weapons/{{frame, dlib, dkit, skin_glyphs, ditems, build, preview, measure, make_doc}}.py`; the measurements `docs/visual_upgrade/djinn_dark_weapons_baseline.json` (taken before anything was changed, first commit of the slice) and `_after.json`; the before / after sheets `docs/previews/visual_upgrade/djinn_dark_weapons/<name>.png` |
| **NEW** | `vupgrade/DjinnDarkSelfTests` (self-test `visual.djinn_dark_weapons`), the pure `relics/upgrade/UpgradeParticleNames`, JUnit `DjinnDarkBudgetTest`, `DjinnDarkRatioTest`, `UpgradeParticleNamesTest`, `UpgradeFxPurgeScopeTest`; the lang fragment `tools/lang/djinn_dark_weapons.json` (one tooltip line) |
| **REPLACED** | nothing was removed. In the hand, on the ground, in frames and on mobs a dark Djinn weapon is drawn from the NEW mesh instead of the old one while the client option `visual.upgrade` is on (that is the only replacement, and it is a switch). The upgraded mesh keeps **every old quad** (position, normal, shape: a test checks it) and adds to it, so the silhouette, the size and the placement in the hand (`place` line copied) are the old ones |
| **KEPT** | the old meshes, skins, glow skins and 16 x 16 sprites (still the inventory icons), item models and display presets, item ids, stats, acts, passives, engravings (`DjinnWeaponRules`, `item/WeaponEngravings` rule 6 holds: no weapon was added or reworked in stats), the Djinn Equip, the loan, the Better Combat files; the 36 light weapons; the Time Magic VFX (`TimeMagicLayer`, `time_*` textures, `gen_time_vfx_textures.py`, the Time calls of `TimeBook`) were not read or edited |
| **REPLACED (follow-up)** | `client/relics/fx/UpgradeFxClient` and `relics/upgrade/UpgradeActFx` built vanilla coloured `DustParticleOptions`; they now spawn the mod's own custom particles (section 6) and their two exclusion lines are gone from `docs/rukh/scope.txt` |

Rule 6 (engravings) and rule 7 (3D model, old sprite as the icon, glow / effects) hold. Rule 4 (version bump) was **not** applied: the brief of this release forbids touching the version files.

## 2. Measured proxies (before and after)

The visual score is `V = sum(weight x after / max(1, before))` over five components, so `V_before = 1` by construction. The weights are those of `god_melee_b` (the numbers of the two slices
are comparable): **mesh 0.20, texture 0.20, passes 0.15, ambient layers 0.25, idle animations 0.20**; the brief's own example weights (mesh .35, texture .2, glow .15, ambient .2, animation .1)
are reported next to them. Components, as measured: **mesh** = quads at full detail (body + void layer, plus every instance of the orbiting piece); **texture** = painted texels (alpha > 0) of
the skin and of every emissive map; **passes** = draw passes (before: body, core glow; after: body, core glow, vein wave, halo shell, orbiting pieces); **ambient layers** = effect layers that run
while the weapon is held; **idle animations** = channels that move on their own.

**Baseline conventions, said plainly.** A dark Djinn weapon is a `DjinnWeaponItem`, not a relic definition, so it never had the grade shimmer of `client/relics/RelicGradeFx` (that looks at
relic definitions only): its baseline ambient layer count is **0** and the ratio divides by max(1, 0), so that component is the plain count of 8 layers. Its one idle animation was the glow pulse
(`VariantRules.pulse`). **Two of the five components (ambient 8.0, animation 7.0, together 3.4 of the V) are therefore cheap counts that start from 0 or 1, and V is not a measure of how the
weapon looks.** The two ratios that mean something are the mesh and the texture ones.

| measure | min | mean | max |
|---|---|---|---|
| mesh ratio (quads at full detail, orbiting pieces included) | {mesh_min:.2f} | {mesh_mean:.2f} | {mesh_max:.2f} |
| painted-texel ratio (all maps) | {tex_min:.1f} | {tex_mean:.1f} | {tex_max:.1f} |
| V (our weights) | {v_min:.2f} | {v_mean:.2f} | {v_max:.2f} |
| V (brief's example weights) | {vb_min:.2f} | {vb_mean:.2f} | {vb_max:.2f} |
| welded vertices at full detail (cap 2500) | {wmin} | | {wmax} |
| quads before | {qb_min} | | {qb_max} |
| quads after (full detail) | {qa_min} | | {qa_max} |
| level-of-detail quads | {lod_min} | | {lod_max} |

Items whose mesh ratio is under 5.0: {under5}.

{table}

(`welded after` is the welded vertex count of the body mesh alone; the orbiting pieces are added for the cap: `DjinnDarkBudgetTest` applies the cap to body + 6 pieces.)

## 3. What was built

**The pipeline** (`tools/visual_upgrade/djinn_dark_weapons/`). The old weapons are baked meshes (the Djinn armour set's weapon piece, converted by `tools/relics/djinn_weapons.py`), not part-structured
builders, so the upgrade is built on the baked file:

1. every old quad is kept (corners, normals); glow-only parts of the old weapon (energy arcs, flame licks) stay glow-only quads;
2. the old skin islands (clusters of quads whose UV boxes touch) are cut out, enlarged (2x where it fits, else 1.5x or 1x; colour bicubic, alpha nearest) and packed with the UV groups of the new
   parts into a **new 1024 px skin** by a MaxRects packer; the old quads get the new UVs, so old and new surfaces have a similar texel density;
3. new parts are built with the owner's mesh_lib method (real sections, lofts, sweeps, lathes; `tools/relics/mesh_lib.py`, `new197_lib.py`) on the **frame of the old weapon** (`frame.py`: the axis,
   the thickness along y, where the handle ends and the head begins), so a ring, a collar or a ribbon sits on the real weapon whatever its shape; a long tube is cut into parts of at most 44 units so
   no UV strip is longer than the skin allows; the detail factor scales segment, side and repeat counts so a build is sized to **5 times the old quads** (never above the cap);
4. painting: the mesh_lib materials, then on every surface, old and new, grain, an ambient-occlusion band and a bevel rim along every island edge and hairline scratches (`gb.detail_pass` of
   god_melee_b for the new parts, `dlib.detail_old` for the enlarged old islands), a slight cast of the Djinn's colour over the old islands, a core glow map, and a **second emissive map**
   (energy veins on the old islands, lit rune script on bands and rings, the Djinn's name engraved and lit on a plate);
5. writers: mesh, level-of-detail mesh (vertex clustering, at most 14% of the quads), the orbiting piece, the three skins and the data table.

**The effect set** is the one of `god_melee_b`, read from the table by the code that already exists (`client/GodMeleeBRender`, `client/vupgrade/godb/*`, no second engine): the core glow (now breathing
with a slow flicker), the **vein pass** (the second emissive map, brightness a wave running up the weapon), the **halo shell** (the level-of-detail glow quads scaled about the axis), six **orbiting
pieces** (revolving, spinning, bobbing; each Djinn its own piece), a **swing trail** of 3 to 4 ghost copies in the Djinn's glow colour, **seven particle layers** (idle motes, a ring turning round the
head, sparks along the blade, a pool of light at the feet, a flare at the hand, a light shaft, a rare flare), the **level of detail** (full mesh to 12 blocks, the level-of-detail mesh beyond, no additive pass
from 24 blocks, no particles beyond 32, half rate beyond 12). The particle layers name the token `fx`: the mod's own particles, themed and dyed by the layer's colour
(`particle.fx.FxRelic.dust`), never a vanilla particle. The acts keep the effect shapes they had (`DjinnFx`): they are not touched.

**What each Djinn got** is decided by its own row of `docs/djinn_roster_36.json`, so the weapon wears the heraldry of its armour: the *horns* style becomes a crown of spikes, crescent horns,
antlers, curled ram horns or three big spikes; the *wings* style blade wings, feather fans, flame wings, veils or membranes; the *tail* a whip, flames, tendrils or a blade hanging from the pommel;
the *halo* a ring, a spoked ring or a ring of runes behind the head; the *affinity* a pack of its own (lightning coil and forked bolts; heat embers and bands; water tide ribbons and pearls; wind
rings and gusts; life vine and leaves; strength heavy studded bands and spurs; sound resonance rings and bells; light rays and star studs). Every dark weapon also has the dark rig: four grip bands,
a crown of thorns at the pommel, an eye gem, a name plate with the Djinn's name engraved and lit, two tilted rings of lit rune script round the head (one if it has a halo), and (by a hash of its name)
a collar of thorns, a ring of gems and a hanging chain.

| Djinn | weapon kind | affinity | horns / wings / tail / halo | orbiting piece |
|---|---|---|---|---|
{recipe_table}

## 4. The follow-up closed: the God worn upgrade effects onto the custom particles

`client/relics/fx/UpgradeFxClient` (the ambient layers of the upgraded God relics) and `relics/upgrade/UpgradeActFx` (the act bursts) built vanilla coloured `DustParticleOptions`; that is why `docs/rukh/scope.txt` excluded
the two files from the purge scan since 1.106.0. Both now go through the custom particle facade: `"dust"` is `FxRelic.dust(color, size)` (a custom mote dyed with the layer's colour); a vanilla name in a table
(`minecraft:end_rod`, `flame`, ...) becomes the replacement of that particle in the theme of the layer's colour (`Fx.options(FxRelic.theme(color), FxOld.X)`); the decision which name gives which `FxOld` constant is the pure
class `relics/upgrade/UpgradeParticleNames` (an alias table for the names `FxOld` has no constant for: `note` is an enchant glyph, `nautilus` a bubble, `cherry_leaves` a heart, ...; an unknown vanilla name becomes the
end-rod slot). The tick builds a local `ParticleOptions` before `level.addParticle`, which is what the scan's call pattern wants. The two exclusion lines were removed from `docs/rukh/scope.txt`;
`RukhPurgeScopeTest` and `FxSourceScanTest` pass, and `UpgradeFxPurgeScopeTest` pins that the two files are in the scope and that the scan patterns find nothing in them; `UpgradeParticleNamesTest` checks that
every particle name in every shipped table resolves to a constant of the generated `FxOld`. The tables themselves (`assets/nusmp/visual_upgrade/*.json`) were not changed: they keep their vanilla names, now only
names for the facade. Not run in a game: how the custom particles of a theme look instead of the dust is unseen.

## 5. Decisions the brief did not settle (honest notes)

1. **The cap read as welded vertices.** As in `god_melee_b`, the brief's "2500 vertices of one item in hand" is read as the welded vertices of the full-detail mesh including its six orbiting pieces. Every weapon is
   at or under 2500 (`DjinnDarkBudgetTest`). The brief's "mesh >= 5x the baseline quads" is met for the items whose old mesh is small enough; the build sizes each mesh to 5.0x and never above the cap
   (items under 5.0: see section 2). Emitted vertices per pass are 4 x quads, as the renderer writes them.
2. **The old islands are enlarged, not repainted.** The old skins are the armour set's skin cut down to the weapon (28 000 to 80 000 painted texels in small scattered islands), so there is no source of a
   better painting of the old surfaces: they are enlarged and given grain / occlusion / bevel / the Djinn's colour. The new parts are painted fresh. The old surfaces will look softer than the new ones up close.
3. **The painted-texel target (4x)** is met by the new parts' skin area and the glow maps; texels are counted as `alpha > 0`, as in the other slices. Video memory: a 1024 px RGBA map is 4 MB, the three maps of one
   weapon 12 MB, uploaded when a weapon is first drawn.
4. **The baseline ambient layers are 0** (section 2), which makes the ambient component of V a plain count. Reported, not hidden.
5. **A held weapon's own particles are placed from the holder's body yaw** (as in `god_melee_b`): close, never exact.
6. **The light weapons** (Legend grade) are not part of this slice and were not touched; the 72 weapons share one renderer, only the 36 dark ones have a table.
7. **Sizes.** The 36 weapons add about {mb} MB of skins and meshes to the jar (png, optimised); the old files stay.

## 6. Where the code touches existing files (all additions, listed so a merge stays trivial)

* `NUSMP.java`: one line, `com.newuniverse.nusmp.vupgrade.DjinnDarkSelfTests.register()` (next to the other visual self-tests). Self-test prefix: `visual.djinn_dark_weapons`.
* `client/vupgrade/godb/UpgradeSpecs.java`: the particle token `fx` (the mod's own particle by the layer's colour) before the existing `dust` branch; `UpgradeConfig` (the comment of `visual.upgrade` names the dark Djinn weapons).
  `WeaponRenderer` and `GodMeleeBRender` are **not** edited: the renderer finds the tables by item id (`assets/nusmp/visual_upgrade/<item>.json`) through the hooks of `god_melee_b`.
* `relics/djinn/weapon/DjinnWeaponItem.java`: one tooltip line for the dark weapons (`translatableWithFallback`, key `tooltip.nusmp.djinn_weapon.dark_look`, the English text is the fallback and the lang fragment).
* `client/relics/fx/UpgradeFxClient.java`, `relics/upgrade/UpgradeActFx.java`: the follow-up of section 4; `relics/upgrade/UpgradeParticleNames.java` new.
* `docs/rukh/scope.txt`: the two exclusion lines removed.
* `tools/relics/djinn_weapons.py`: one line at the end of `main` that runs `tools/visual_upgrade/djinn_dark_weapons/build.py` after the old files are written (not in `--check`).
* Lang fragment: `tools/lang/djinn_dark_weapons.json`.

## 7. How to rebuild and check

```
python3 -B tools/visual_upgrade/djinn_dark_weapons/build.py [djinn id ...]      the models, skins, tables (deterministic: a second run changes no byte)
python3 -B tools/visual_upgrade/djinn_dark_weapons/preview.py [djinn id ...]    the before / after sheets
python3 -B tools/visual_upgrade/djinn_dark_weapons/measure.py after             docs/visual_upgrade/djinn_dark_weapons_after.json (and `table` for the markdown above)
python3 -B tools/visual_upgrade/djinn_dark_weapons/make_doc.py                  this page
```
JUnit: `DjinnDarkBudgetTest`, `DjinnDarkRatioTest`, `UpgradeParticleNamesTest`, `UpgradeFxPurgeScopeTest` (and the existing `RukhPurgeScopeTest`, `FxSourceScanTest`, `WeaponMeshesTest`, `WeaponClassesTest`,
`DjinnWeaponAssetsTest`, `DjinnWeaponRulesTest`, `GodMeleeBBudgetTest`, `GodMeleeBRatioTest` still pass). The new files sit in sub-folders (`weapon_meshes/up/`, `textures/entity/weapon/up/`) so no
existing listing sees them.

## 8. What could NOT be verified

* Nothing was run in the game. The Java compiles against the project's API stubs (no error in the new or edited files; a deliberate wrong call was flagged by the same check), the pure logic and the files are tested.
* How the vein wave, the halo shell, the orbiting pieces and the swing ghosts look and how fast they move is untuned; the direction the swing ghosts lag in follows the item's own axes and may be the wrong way round
  for some swings. The orbit radius is the widest extent of the weapon plus 5.5 units: for the wide weapons (fans, daggers) the pieces may look far off.
* The motifs are placed from a measured frame of each old mesh (`frame.py`), not by hand: the preview sheets show them sitting plausibly on all 36, but a ring may cut through a wing of the old mesh or a ribbon
  hang oddly for a weapon whose silhouette the frame measures badly (bows, claws, fans). The sheets are the software rasteriser's, with one light and no additive blending.
* The enlarged old islands next to the fresh new parts: the difference in sharpness is only seen in the game. The core glow of the old weapon is kept; the veins on the old islands are procedural.
* `visual.guiModel` (the tilted model in the inventory slot) was written for `god_melee_b` and never drawn for these weapons.
* Frame time: the cost model is by construction (one extra pass over at most 700 glow quads for the veins, at most 330 level-of-detail quads for the halo and the ghosts, 6 small pieces), not measured; `/nusmp perf` was not run.
"""

if __name__ == "__main__":
    main()
