# Blocky feet relics and the display fix (3.9.0)

Owner request: fix how the head, chest and feet relics show on the body, fix the position of some Curios relics, and give the armour pieces a blocky look like the Saint Valkyrie Dress. Later clarified twice: first "the armour pieces get the blocky style, nothing else", then **"only the feet one"**: only the FEET armour relics (boots, greaves, sandals) get the blocky style; head and chest pieces keep their look (they still get the contact fit). Every relic keeps its own colours; capes stay as they were.

## What was found (read from the code; nothing could be run in the build session)

| # | Finding | Fix |
|---|---|---|
| 1 | Every body model was shrunk to 85 % about the **joint of its body part** (hip for boots and greaves, shoulder for gloves, neck for chests). A boot whose sole is at 12.6 px was drawn with the sole at 10.7 px: about 1.3 to 1.8 px **above the foot**, and the shrink also pulled models into the body skin. | `relicfx.contactFit` (default on): the shrink is about the model's own middle height, so soles stay on the feet, hems on the hips (`WornPlacer.fit(anchor, model, centreY, true)`, `WornMesh.centreY()`). Off = the 3.8 placement. |
| 2 | Hip charms (flask, tome, pouch, pouch pair) stand at x 3.6..7.6, y 8.5..17: **inside the arm** that hangs at the hip, so half of them was hidden. | Lowered 3.4 px with the contact fit (`WornPlacer.hipHung`, `HIP_DROP`): they hang beside the thigh. |
| 3 | The real cause of "invisible" on the owner's machine is **not found**: the log has no failure line, the geometry scan found no model fully inside its body part, and the 1.110/1.111 failsafe believes things are drawn. | A line in `latest.log` the first time each armour relic is drawn: `armour relic <id> (<slot>): N model piece(s), V vertices, contactFit .., detail ..`. `/multiverse relic debug` already prints the per-slot facts. If the relic is still invisible after this build, send the log lines. |
| 4 | Slot icons: every one of the 1316 relic item models is `builtin/entity` with an `<id>_icon` sprite model (checked: none missing except the shield blocking variants, which are not relics). | No change needed; the sprite path is the existing one. |

## The blocky models (`relic_worn_x3`)

`tools/relics/gen_worn_x3.py` turns every worn model of a **feet relic** (the FEET looks, and every `relic` entry that is in no Curios tag and has a piece on a leg: 49 models, only the leg-anchored pieces, so a shared decoration such as `deco_flames` stays as it was) into a blocky one: the model is voxelised on a 0.375..2 px grid (the finest that keeps the shell within 2.2 x the old quad count), hidden faces are dropped, equal faces are merged, and the big flat faces get a raised trim frame, a panel of the face's own colour and a thin glow line. Every quad keeps the **colour slot** (main / trim / glow) and **atlas tile** of the quad it came from, so each relic keeps its own colours. Quads: 6,799 -> 15,370 (x2.3 on average; the biggest model, `greaves_windwalker`, is 981 quads).

Not changed: head and chest relics (they keep their 3.8 models), capes, cloaks, mantles and the BACK looks; amulets, belts, gloves and charms (Curios); the visual-upgrade models with their own skin; the armour sets of the mod (robes, mode overlays). The old files stay in `relic_worn`. The earlier 257-model head + chest + feet run (preview `armour_before_after.png`) is not shipped.

At run time (`WornLibrary.resolveX3`): a feet relic (`RelicSlot.FEET`) wears the x3 version of each of its models that is listed in `relic_worn_x3/index.txt`; the classic models are its LOD form. `relicfx.modelDetail = classic` restores the 3.8 look at once.

## Cost (rule 11, worst case)

* Per model at most 6,400 vertices (`UpgradeCaps.X3_MODEL_VERTICES`, checked by `X3ModelsTest` for every file), per wearer at most 14,000 (`X3_ENSEMBLE_VERTICES`; a relic over it keeps its classic models).
* x3 is drawn only within 12 blocks, never at the low quality, and for **at most the first four wearers of a frame** (`X3Gate`); everyone else wears the classic LOD. So at most 4 x 14,000 = 56,000 vertices per frame for all x3 relics, however many players are near. Additive passes stop at 24 blocks (the existing upgraded-relic rule).
* No new packets, no server work, no per-frame allocation (a counter and a nanoTime read per wearer per frame). The log line is written once per armour relic (at most 256).
* Disk: `relic_worn_x3` is about 2.4 MB of text (compresses well in the jar).

## Settings (client)
`relicfx.contactFit` (true), `relicfx.modelDetail` ("x3" or "classic").

## Tests
`WornPlacerTest` (contact fit, hip charms), `X3ModelsTest` (every file parses, caps, anchors and groups kept, no cape, gate), self-tests `relic.display39` and `relic.x3`.

## Not seen in a game
The blocky look was judged on the offline rasteriser sheet `docs/previews/relic_x3/feet_before_after.png`. The in-game placement of boots, gloves and hip charms after the contact fit, and whether the invisible armour is fixed, need the owner's eyes.
