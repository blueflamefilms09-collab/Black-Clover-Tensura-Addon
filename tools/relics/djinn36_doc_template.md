# The art of the 24 new Djinn (1.93.0): Bael to Murmur

Twenty-four Djinn were added after Vassago (rules, traits, Extreme Magics and weapon acts: `docs/djinn_roster_36.md`). This page is their **visuals**: the full-equip
armour set of each (light and Dark Djinn), the in-hand 3D weapon of each (light and dark twin), the Seal, the skins, the ambient particles and the dust palette. Code
that draws them: `client/relics/djinn/DjinnRoster36` (+ `DjinnRoster36Fx`); art tools: `tools/relics/djinn36_*.py`; previews: `docs/previews/djinn36/`.

## NEW / REPLACED / KEPT (owner rule 5)

| Part | Status | What |
|---|---|---|
| Armour sets `djinn/u_<id>/`, `djinn/d_<id>/` for the 24, their skins, glow skins, runes, dark veins, dark busts | NEW | every set has its own helm, horns / ears / crest, chest emblem, pauldrons, forearm and greave accents, wings (4 pieces), tail (18 of 24), halo (light set), back piece, mantle (15 of 24), halo in the light set (20 of 24), weapon piece; the Dark Djinn set adds the eye crest, the ring of thorns and black rukh on the wings |
| Shared-rig fallback `djinn/<id>/` (+ `<id>_armor.png`, `_rune`, `_bust`) for the 24 | NEW | the first twelve's old rig, built for the new Djinn from their `Look` so the fallback chain (`DjinnLook.chain`) and `DjinnAssetsTest` hold; it is drawn only if a unique set is missing |
| 24 Seals (`djinn_seal_<id>`), 24 light and 24 dark weapons (mesh, skin, glow skin, model json, icon model, 16 x 16 icon) | NEW | the Seal has a glyph of its own Djinn on the round seal; every weapon is a real 3D mesh in the hand with a glow layer and keeps a flat inventory icon (owner rule 7) |
| Motion row, ambient particles and dust palette of the 24 | NEW | `DjinnRoster36` (name-keyed, generated from `tools/relics/djinn36_roster.py`); `DjinnLook.style` / `ambient` fall back to it for ordinals past Vassago |
| The first twelve's meshes, skins, weapons, seals, motion rows, particles | KEPT | not one file of theirs changed (checked with `git status`) |
| `tools/relics/djinn_weapons.py` | KEPT, extended | its tables `DJINN`, `TARGET`, `NAMES`, `ACTS` now list all 36 (additive block at the end); the first twelve's outputs are byte-identical (`--check`) |
| Anything REPLACED | none | nothing the mod already had was swapped out |

## How they plug in

The loaders are name-keyed (`DjinnLook.dir/skinPath/runePath/bustPath` build every path from `Djinn.id()`), so the art needs no code: the files are named exactly like the
first twelve's and `DjinnMeshes` finds them once the enum has the entry. The forge-in and shatter of the full equip are `DjinnArmorLayer.assembled` and `part`, unchanged:
each piece is anchored on the model part it was built for, staggered by its own delay, flies in while it fades up and grows, and shatters out over the last ticks. The
glow pass is the additive `eyes` layer of each piece, the body-art is `<id>_u_rune` / `<id>_d_rune`, the fresnel aura is tinted by the Djinn's own glow colour.

The rule of the tail (`DjinnLookTest`): a Djinn has tail segments exactly when its `Look.tail` is not NONE. Six Djinn have no tail (Bael, Gusion, Buer, Dantalion, Crocell,
Furcas): no tail piece is built for them and their motion row has 0 segments; `DjinnRoster36.styleOf` also enforces it from the enum if a Look changes later.

## The 24 full equips

| Djinn | Head | Wings | Piece behind the back | Weapon in hand |
|---|---|---|---|---|
@@ROWS@@

Every set also has: a chest emblem of its own on a cuirass or harness, shoulder pieces that match the head, forearm and greave accents of the family (`djinn36_lib.FAMILY36`), the
Half Equip pieces (the brow crest, the main-hand pauldron, vambrace, gauntlet) and a Dark Djinn variant (black metal and cloth, gold trim, the Djinn's accent in the crystals).

## Motion, particles, frame cost

`DjinnRoster36` gives each Djinn a motion row (wing beat, tail segments, halo spin, back-piece spin and breathing, weapon size, tail sway: all 24 rows differ), an ambient
recipe (two particle keys with rates, and where they are born; all 24 recipes differ from each other and from the twelve) and a **dust palette**: a coloured dust particle
(the Djinn's glow fading into its body colour; a Dark Djinn's saturated colour fading into black violet) that moves as its affinity moves (heat rises, water falls, wind
swirls, strength sinks, sound spreads in rings, light twinkles, lightning jitters). `Frame` is the quads drawn in a Full Equip (tail counted five times) in the light and
the dark set; the budget is 5200.

| Djinn | Weapon kind | Weapon length (blocks) | Ambient (main + second, born at) | Frame light / dark | Tail |
|---|---|---|---|---|---|
@@ROWS2@@

## Weapons

The kinds, names and acts are the rules table's (`djinn36_weapon_data.py` is a copy of it for the tools). Each mesh is derived from the armour set's own weapon piece by
`djinn_weapons.py` (the same code as the first twelve): scaled to its target length under the size rule (2.2 to 3.6 blocks; the short kinds, daggers, fans, pincers and
claws, are drawn at 2.3 so they meet the 2.2 floor of `DjinnWeaponAssetsTest`), oriented with the head at -x, `orient manual`, white glow so the painted glow skin shows
as painted, `place` on the sprite diagonal, the hand holding it where the armour layer holds it. The model json and the 16 x 16 icon come from the same tool. The Dark
twin is the dark set's weapon piece in black metal and gold. The effects of a weapon in hand are its glow layer (pulsing as the other weapons' do), the particles of its
Djinn and the dust palette; the crimson void layer of the Genesis Demon-Slayer is **not** used (`DjinnWeaponAssetsTest` allows only the body and glow layers), so Belial's
hollow blade, Kimaris' lance and Glasya-Labolas' fangs carry their dimension in the glow layer instead.

## Item ids (72)

@@IDS@@

Textures: `textures/item/<id>.png` for each; models `models/item/<id>.json` (Seal: generated item model; weapons: builtin/entity with the standard display) and
`models/item/<id>_icon.json` for the weapons; weapon meshes `weapon_meshes/<id>.txt`, skins `textures/entity/weapon/<id>.png` and `_glow.png`. Names:
`tools/lang/djinn36_art.json` (the same names the rules builder put in `en_us.json`).

## Edits to existing files (all additions)

* `client/relics/djinn/DjinnLook.java`: `style(int)` and `ambient(int)` ask `DjinnRoster36` for an ordinal past their tables instead of returning the fallback at once (two three-line blocks).
* `client/relics/djinn/DjinnAmbient.java`: one line at the end of `emit`: `DjinnRoster36Fx.emitPalette(...)`.
* `tools/relics/djinn_weapons.py`: an additive block (`DJINN +=`, `TARGET.update`, `NAMES.update`, `ACTS.update`) before `lang_fragment`.
* `tools/item_preview/preview_weapons.py`: reads `qn` lines (per-vertex normals), which it did not before, so the Djinn weapons can be previewed.

New files: `client/relics/djinn/DjinnRoster36.java` (pure tables, no Minecraft types), `DjinnRoster36Fx.java` (the dust), `tools/relics/djinn36_*.py`, `tools/lang/djinn36_art.json`,
the assets above, `docs/previews/djinn36/`.

## Tools

`python3 -B tools/relics/djinn36_build.py [--djinn bael,agares] [--only armor|base|skins|seals|weapons|lang]` writes everything (idempotent, about 8 s a Djinn);
`--preview DIR` renders a mannequin sheet per Djinn (half equip, full light front and back, the weapon, full dark front and back, half dark) and `--icons DIR` the icon
sheet; `tools/relics/djinn36_java.py` writes `DjinnRoster36.java` from the table; `tools/relics/djinn36_check.py` is the CI check (files, budget, tail rule, distinct rows,
Java in step, `djinn_weapons.py --check`); `python3 tools/item_preview/preview_weapons.py djinn_weapon_bael ...` previews a weapon as the renderer places it.
The armour is built with the first generation's `mesh_lib` (real cross-sections: lofts, sweeps, lathes) through the same atlas painter, so each piece has box-free UVs, a
painted skin per material and an emissive glow map; `geo_check.py` checks Bedrock geo models and does not apply to these baked mesh files.

## Not verified

Nothing was run in the game. The previews are the mesh library's software rasteriser posing the pieces like `DjinnArmorLayer` does: they show shapes and colours, not the
real lighting, the animation (wing beat, tail wave, forge-in) or the dust. The tests that need only files and pure classes were run with the real 36-entry enum
(`DjinnLookTest`, `DjinnUniqueAssetsTest`, `DjinnWeaponAssetsTest`, `DjinnAssetsTest`): all pass except `DjinnLookTest.theDarkAuraIsRedVioletAndStillTellsTheDjinnApart`,
because Baal (`0xBFE4FF`) and Andrealphus (`0xBFE3FF`) have glow colours one level apart, which `DjinnLook.darkAura` (22% of the glow) cannot tell apart; that is a colour
of the roster table, not of the art (a glow of `0xB8E0FF` for Andrealphus, or a looser test, resolves it).
