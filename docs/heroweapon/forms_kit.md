# Forms quality kit (`tools/blender/forms_kit.py`) and the three exemplars

Why this exists: the first batch of weapon-form models was rejected ("they all look like a little kid made them"; the bar is the
Genesis Demon-Slayer). The reasons were visible: primitives (cones, spheres, flat plates), perfect symmetry, clean surfaces, flat colour.
`forms_kit.py` is a helper layer on top of `bl_lib.py` (which it never edits) that makes the *geometry* dense, asymmetric and seeded, and the
*materials* worn, baked and painted. `forms_exemplars.py` uses it for three forms that follow the convention of `form_models.md`.

Status: the kit and the three exemplars are **new** (nothing is replaced, nothing removed). The exemplars are a clear step up from the
first batch, but **they do not reach the Demon-Slayer yet** (see "Honest assessment" at the end). Use them as the floor, not the ceiling.

## Run it

```
python3 -B tools/blender/forms_exemplars.py                       # bake all three into the mod's asset folders + tools/blender/forms_manifest_exemplars.json
python3 -B tools/blender/forms_exemplars.py --only slime          # one form (phoenix | slime | dragon), bake ONE at a time (4 CPUs)
python3 -B tools/blender/forms_exemplars.py --only dragon --trial DIR   # write into DIR instead of the mod (nothing in the repo changes)
python3 -B tools/blender/forms_exemplars.py --only phoenix --quick      # no bake: quad estimate + parts table in ~1 s (look here first)
python3 -B tools/blender/forms_exemplars.py --only slime --beauty DIR   # a Cycles still of the build (studio rig) into DIR
python3 -B tools/relics/preview_mesh.py legendary_sword__phoenix_sword --focus 6,62     # software preview; --focus y0,y1 zooms on a stretch of the axis
```

Do **not** run the repo-wide `python3 tools/shrink_pngs.py` for this: the kit packs its own two PNGs (`pack_png`, palette), which the
repo shrinker then skips. (Running it repo-wide re-encodes ~180 unrelated textures.)

## What each helper does (`import forms_kit as K`)

Coordinates are the mod's: y = length (cm), x = width, z = thickness (+z faces the viewer). Everything is seeded (`K.rng(seed)`): the same
script gives the same mesh. `detail=True` on a helper means "bake into the skin only, never exported" (free in quads).

| Area | Helpers | Notes |
|---|---|---|
| Scene / driver | `begin`, `finish_form`, `beauty`, `report`, `write_manifest`, `quad_estimate`, `quad_table` | `finish_form` unwraps, bakes COMBINED (1024 px) and EMIT (glow) with Cycles, boxes down to the 512 px skin, packs the PNGs, writes the `qn` mesh with the base's `place` (`header_from`). `report` = quads + size class + apparent length. |
| Materials | `surface` | worn metal / leather / bone / gel / feather / membrane. Baked from nodes: edge wear (own wide bevel, `wear_radius`), AO dirt, scratch patches, mottle, painted bump (`hammer`, `leather`, `brushed`), grooves, veins (voronoi), per-strip feather barbs and colour ramps, transmission (gel), emission with flicker (the glow skin). Cost in game: zero (it is all in the 512 px skin). |
| Blades | `blade_edge`, `blade`, `strip`, `merge_rows` | Asymmetric edge polylines with notches, chips and a slanted/broken tip from noise; bevelled profile with an (off-centre) fuller. `merge_rows` pairs two independent edges, so left and right never mirror. |
| Plates / armour | `stepped_plate`, `plate`, `inset_poly`, `rivets`, `rivet_line`, `seam`, `engrave_lines`, `bar`, `scale_plate` | Layered, stepped guards with seams and rivets (`detail=True` bakes them; real rivets need r >= 0.6 cm to be seen at 512 px). |
| Wraps / cloth | `ribbon`, `helix_wrap`, `grip_wrapped` | Hanging tails with fray and notches, wound cord with metal bands. Part names decide the exported `grip` directive: name grip parts `grip*`. |
| Gems | `gem_setting` | Stone in a bezel with real, bent claws. |
| Ornament | `filigree`, `spiral`, `runes` (real text mesh, `detail=True` = glowing relief) | Scrollwork and runes as geometry. |
| Arrays | `feather`, `fan_feathers`, `leaf_rows`, `spike_array`, `scale_plate`, `tapered`, `shard`, `ember`, `shard_cloud` (capped by `max_count`) | Feather/leaf/petal/flame/spike/scale arrays with seeded variation of length, angle and curl; bounded fragment clouds. |
| Organic | `blob`, `drip`, `bone`, `wood_branch`, `vine`, `tube`, `displace`, `asym_mirror` | Slime/egg/shell lumps, drips, bone shafts with knobs, bark, vines with leaves. |
| Mechanical | `link`, `chain`, `gear`, `spring` | Chains (link count capped), gears with irregular teeth, helical springs. |
| Output | `pack_png` / `shrink_pngs(folder, only)` | Median denoise blend + palette (skin MEDIANCUT binary alpha, glow FASTOCTREE RGBA). Packs only the item's own two PNGs. |

## Quality checklist for the other builders

Run `--quick` first, read the parts table, then bake. A form is not ready until every line below is true **and** you have looked at the
close-up renders (`--focus`), not only the full-weapon view.

1. **Quads**: 3,000 to 5,000 quad lines including glow copies (`report` prints it). Under 2,500 will look like the rejected batch.
   A quad gets a glow copy if any glow pixel lies in its UV box, so keep emission to the parts that glow.
2. **Wear**: every metal/leather/bone material uses `surface(wear=, dirt=, scratches=, mottle=)`; edges must catch a lighter colour.
3. **Asymmetry**: no exact mirror twins. Different seeds and counts left/right (`asym_mirror`, `merge_rows`, separate `rng`), a chip or missing piece.
4. **Secondary detail**: rivets, seams, bands, claws, tails, gems in settings, runes, drips. At least four kinds on every weapon.
5. **Layering**: stepped plates, overlapping scales/feathers, a collar over a joint. Never a single extruded outline.
6. **Silhouette readable at hand size**: the weapon is seen at ~100 px tall. Big shapes (wing, feather fan, drips) carry the read; detail below ~0.5 cm is noise.
7. **In size class**: `report` says "in class" (apparent length in the class range of `weapon_classes.json`); keep the base's `place`. Never ship `small`.
8. **No blown-out glow**: emission strength about 0.3 to 0.9; pale colours clip to white in the 512 px skin.
9. **Skin + glow PNG <= 250 KB** after packing (`report` checks).
10. **Part names**: grip words only on the grip (the exporter derives `grip` from them); blade words on the blade.
11. **Pure bake-only details** (`detail=True`) sit within ~1 cm of the exported surface, otherwise the bake misses them.
12. **Tell the owner what is not done**: engravings (`item/WeaponEngravings.java`, rule 6) and Better Combat attributes (rule 13) are shared-file work for the coordinator.

## Measured (trial and final runs, 4 CPUs, Cycles 24 samples, bake 1024 px boxed to 512)

| Form (mesh id) | Base / class | Body quads | Glow copies | Total | Budget | Skin + glow PNG | Build |
|---|---|---|---|---|---|---|---|
| `legendary_sword__phoenix_sword` | legendary_sword / longsword | 3,930 | 951 | **4,881** | 5,000 ok | 156 + 14 = 170.5 KB | 119 s |
| `legendary_shield__slime_shield` | legendary_shield / shield | 3,471 | 320 | **3,791** | 5,000 ok | 143 + 5 = 148.7 KB | 123 s |
| `vassal_scythe__demon_dragon_scythe` | vassal_scythe / scythe | 3,670 | 493 | **4,163** | 5,000 ok | 135 + 18 = 150.4 KB | 108 s |

Apparent length (blocks, third-person display scale): longsword 1.712 (1.4 to 1.8), shield 1.033 (0.9 to 1.2), scythe 2.384 (2.0 to 2.6): all in class.
Heaviest parts: phoenix blade 838 / wings 550 / guard 420 / grip 332 / plumage 312 / flames 286; slime gel 982 / rim 600 / drips 432 / core cage 246;
dragon: wing membrane + ribs, shaft collars, bell. Total for the three: 12,835 quad lines.

Files: `tools/blender/forms_kit.py`, `tools/blender/forms_exemplars.py`, `tools/blender/forms_manifest_exemplars.json`, the three
`weapon_meshes/<id>.txt` and six PNGs under `textures/entity/weapon/`, previews in `docs/heroweapon/form_previews/exemplars_*.png`
(`profile`, `three_quarter`, `closeup`, `closeup_tq`, `back`, `edge` per weapon: `phoenix`, `slime`, `dragon`).

## Research (rule 10)

English fandom wikis (Black Clover, Tensura, Fairy Tail) were **unreachable** from this environment, and `WebSearch` returned nothing usable
for the weapon names; the Japanese wikis were not reachable either. No reference images could be fetched. The designs therefore follow the
owner's brief and the form names only (phoenix = layered feathers, ember glow, gold guard with wing motifs; slime = translucent gel with
bubbles, core, drips, reinforced rim; demon dragon scythe = wing/bone blade with membrane and teeth, ornate haft). Nothing about canon
shapes was verified: the coordinator should compare with the wikis when access exists.

## Cost statement (rule 11)

Worst case per form, static geometry only: no entities, no per-tick work, no extra packets (they are weapon meshes drawn in hand):
at most 5,000 quad lines drawn per weapon in hand (budget enforced by `report`), one 512 px skin + one 512 px glow texture. The kit adds
no Java. The clouds of embers/droplets are static geometry capped by `max_count` (12 / 12). **Conflict to resolve:** the runtime `MeshCap`
default is 3,000 quads (jar task); the owner's budget here is 5,000. Phoenix (4,881) and the scythe (4,163) exceed 3,000, so either the cap
must be raised for forms or the meshes are truncated in game. Not seen in a game.

## Not done / needs the coordinator

* Tensura engravings (`item/WeaponEngravings.java`) for the new forms: shared Java, not touched.
* Better Combat attributes (`weapon_attributes/`) for forms: not touched.
* Grip: the `grip` fraction written from part names differs from the bases (phoenix 0.174 vs 0.076 base, scythe 0.272 vs 0.132); the `grip_table.py --write`
  sync from the manifest should be reviewed against the hand position before release. The slime shield has no grip directive (like its base).
* Registration of the forms (manifest class sync, `form_models` pipeline) is the coordinator's step; the manifest is in `tools/blender/`.
* Not seen in a game; only the software preview and Cycles stills.

## Honest assessment versus the Demon-Slayer

Better than the first batch: real asymmetric silhouettes (chipped blade, torn wing membrane, dented drips), layered feathers with painted
barbs, a bone wing with ribs, claws and teeth, stepped guard plates, claw-set gems, cloth tails, worn metal, baked translucency, bounded
cost, and a repeatable kit. **Not at the Demon-Slayer level**:

* Texel density is about 3 px/cm at 512 px, so fine detail (small rivets, engraving, filigree) disappears; the palette pack also softens contrast.
  The Demon-Slayer's crispness needs either a larger skin (not allowed) or fewer, bigger, bolder details.
* Phoenix: the guard scrollwork is bake-only and barely readable; the blade reads as cracked embers rather than layered plates.
* Dragon scythe: the membrane's glowing vein network looks like lava/giraffe pattern more than a wing; the bell is a studded ball; the haft orb is a plain yellow ball.
* Slime shield: the gel is dark and the bubbles are soft discs; it lacks the rim damage and asymmetry the other two have.
* Part of the budget goes to glow copies (phoenix: 951 lines), which is a trade-off to watch.
