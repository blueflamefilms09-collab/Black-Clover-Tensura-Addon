# Beast Will forms: 3D cube bodies (3.61.0)

The 3.60.0 spirits were flat painted sprites behind the player. In 3.61.0 each of the 27 Beast Wills is a real 3D model made of many small, angled, layered cubes
(Minecraft-detailed: stepped and angled horns, tails and tentacles, layered fur and feathers, plated armour with trim, glowing eyes and gems). Flat shapes stay
where they read best (wing membranes, cloth, fins) but every piece is built around the player's own body and never cuts into it.

## How it is built
- `tools/beastwill/remake/forms_gen.py` holds the 27 palettes and layouts, `forms_parts.py` / `forms_heads.py` the reusable parts, `forms_cube.py` the cube scene, `forms_lib.py` the
  tone-tile texture sheet, `forms_export.py` writes the files, `anim_check.py` poses a will at a moment of its idle loop, `geo_check.py` checks an export against the preview.
- Output per will (stem from `BeastWillLook.STEM`): `bwf_<stem>_arm_<piece>.geo.json` for head, body, armL, armR, legL, legR, tail, wings and acq (the Acquire piece),
  one shared sheet `textures/entity/bwf_<stem>.png` plus `_glow.png`, and `animations/entity/bwf_<stem>_arm_<piece>.animation.json` for pieces that move.
  `docs/beastwill_forms.txt` and `beastwill/BeastWillFormTable.java` list which pieces each stem has (generated together).
- `client/aura/BeastWillForms.java` draws them through the same `NoelleFormRig` engine as the Noelle armours (fly-in on assembly, shatter on end, silhouette pass, glow pass).

## Animation (idle loop, 4.8 s, whole cycles so it never jumps)
Breathing chest and cloth; swaying tail (three nested bones, so it whips from the tip); flapping wings; twitching ears; swaying manes; the extra heads of Cerberus, Hydra,
Typhon, Yamata no Orochi and Scylla & Charybdis sway on their own necks; Kraken's tentacles; the floating island of Hafgufa bobs; Fenrir's chains swing; the Flame Serpent's tail flame pulses.
All motion is a few degrees of rotation (tested to stay under 40 degrees, scale within 20 percent).

## Settings
- Client `beastwill.flatSpirits` (default off): draws the flat 3.60.0 sprites instead. `beastwill.classicSpirits` still draws the old animal spirits.

## NEW / REPLACED / KEPT
- NEW: 27 cube models (about 230 geo files), 27 sheets, 78 idle clips, `BeastWillForms`, `BeastWillFormTable`, the `flatSpirits` switch, `BeastWillFormsTest`.
- REPLACED (you asked): the look of the spirits in game. The 3.60.0 flat sprites are KEPT behind `flatSpirits`.
- KEPT: everything on the server side (wills, skills, saves, packets), the classic spirits, the glints of the multi-headed spirits.

## Worst-case cost
Server: nothing, this is client drawing only. Client per player per frame: nothing beyond 48 blocks or when invisible; within 24 blocks and the per-frame budget (4 players) the whole model with a glow pass,
the next 6 players the model without glow, the rest nothing. Biggest will (Typhon): 318 cubes = 1908 quads; worst frame (10 admitted players all in Typhon) is about 4 x 2 x 1908 + 6 x 1908 = 26.7k quads; a typical will (150 cubes, 900 quads) about 13k.
The Acquire piece is at most 16 cubes. Sets are built once per kind, no per-frame allocation. Animation is sampled from the clip, no extra packets.

## Not seen in a game
Previews come from a software renderer in `tools/`, not from Minecraft; animation direction and amplitude were checked in that preview only.
