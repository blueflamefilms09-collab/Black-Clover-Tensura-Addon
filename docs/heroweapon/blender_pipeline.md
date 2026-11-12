# Hero weapons made in Blender (3.80.0)

The owner asked for the hero weapons to be made in Blender. From 3.80.0 the in-hand models of the hero weapons (the 4 Legendary in 3.80.0, five Vassal in 3.81.0,
the 7 Seven Star and the last five Vassal in 3.82.0: all 21) are modelled, unwrapped and baked in Blender, then exported to the mod's own weapon mesh format.

## What Blender does

| Step | Tool |
|---|---|
| Modelling | Blender meshes (bmesh), modifiers (bevel, subdivision, solidify, decimate), text objects for the rune inlays |
| Materials | Principled BSDF shaders: metal, roughness, bump textures (brushed, hammered, leather, wraps), the Bevel node for rounded edges |
| Unwrapping | Smart UV Project, islands scaled evenly and packed into one skin |
| Baking | Cycles: the lit, bumped surface and every bake-only detail floating over it (blade cores, gold lines, runes, engraving) is rendered into the 512 px skin; the emission into the glow skin |

The mod's own tools still write the mesh file (`tools/relics/mesh_lib.py`: the `place`, `grip`, `orient`, `glow` and `qn` lines, and
`tools/relics/weapon_fixes.json`) and draw the software preview that shows the mesh the way the game draws it.

## Files

- `tools/blender/bl_lib.py` — the pipeline: scene and studio light, materials, modelling helpers, `finish()` (join, unwrap, bake, export).
- `tools/blender/legendary.py` — sword, shield, spear, bow (and the bow's three draw stages on one skin), and since 3.83.0 their curse looks (`--only sword_curse,shield_curse,spear_curse,bow_curse`): the same models in a `curse` mode.
- `tools/blender/seven_star.py` — hammer, claw, axe, whip, staff, gauntlet, projectile.
- `tools/blender/vassal_a.py` — katana, scythe, fan, harpoon, chain.
- `tools/blender/vassal_b.py` — carriage, book, mirror, instrument, sewing set.

## Running it

Blender is used as a Python module, no window is needed:

```
python -m venv bvenv
bvenv/bin/pip install bpy pillow numpy          # Blender 5.x as a module (about 430 MB)
bvenv/bin/python -B tools/blender/legendary.py --only sword --trial /tmp/out     # look first: mesh, skins and a preview go to /tmp/out
bvenv/bin/python -B tools/blender/legendary.py --only sword                      # write into the mod's assets
```

After writing: `python3 -B tools/weapon_grip/grip_table.py --write`, `python3 -B tools/safe_additive/scan_textures.py --fix`,
`python3 -B tools/shrink_pngs.py src/main/resources/assets/nusmp/textures/entity/weapon` and, for the projectile,
`python3 -B tools/relics/weapon_sizes.py --apply --ids vassal_projectile`.

A bake takes about one minute per weapon on four CPU cores. The glyphs of the rune inlays come from GNU FreeMono (its runic block);
only their baked shapes are in the skin.

## Rules the builders keep

- Each weapon keeps the length, the grip position and the `place` line of the model it replaces, so the hand, the display
  transforms, the size class and the grip table still fit. Icons, item models and ids are untouched.
- At most 2800 quad lines per mesh including the glow copies (the runtime cap is 3000). The extra detail costs nothing in game: it
  is in the skin.
- Thin bake-only details use materials without the Bevel node (it would average a thin plate's top with its underside) and sit
  within 0.3 of the surface they mark.

## Old models

- The models of before 3.79.0 stay in `weapon_meshes/classic` and `textures/entity/weapon/classic`; the client setting
  `classicHeroWeaponModels` (`nusmp-heroweapon-client.toml`, section `models`) draws them.
- The 3.79.0 code-built models are replaced by the Blender ones (asked for by the owner); their generators
  (`tools/heroweapon/art_hart1.py` .. `art_hart4.py`) stay in the repo; since 3.83.0 the curse looks are Blender-made too, and the old
  curse looks are in the classic folders as well.

## Cost

Client drawing only. No server, network or tick cost. Worst case per weapon: 2800 quads per frame while it is on screen, as before.

## Seen in a game, and not

Seen (3.83.4 to 3.83.7, the CI client screenshot run, `docs/previews/ingame/`): all 21 base looks in a real client under the game's own
lighting, in the player's hand in first person and in third person, in an armour stand's hand and on an item display. Each is bound and
summoned the way a player gets it (`/multiverse hero bind`, `/multiverse hero summon`). They draw textured and lit, none black, none missing.

Not seen in a game: the curse looks, the bow's draw stages, the glow in the dark, the old models behind `classicHeroWeaponModels`, and a side
view of the third-person hold (the run photographs the player from the front, where a weapon pointing at the camera looks short). The run
uses a software GL driver, so colours and brightness on a real graphics card may differ slightly.

To repeat the check: push a commit whose message contains `[clientshot]`; the run commits `hero_first_person.jpg`, `hero_third_person.jpg`
and `hero_log.txt` to the pushed branch.
