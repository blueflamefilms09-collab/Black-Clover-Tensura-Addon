# Saiyan hair and the 3D Saiyan tail (made in Blender)

The owner asked for "the different hair states from the actual Dragon Ball anime: Super Saiyan's hair goes golden, and then even golder for each other, slightly different like in the
anime", and for the tails to be made in Blender, "an actual tail design, not blocky", with the standing rule that nothing may look like a flat PNG pasted into the game.
So every hair and the tail are real shaped 3D meshes: tapered, flat-sectioned locks with thickness, modelled, unwrapped and baked in Blender, worn on the player's head and
lower back like the devil forms are.

## NEW / REPLACED / KEPT (rule 5)

| | What |
|---|---|
| NEW | Seven Blender hair meshes (`saiyan_hair_base`, `_ss`, `_ss2`, `_ss3`, `_ss4`, `_god`, `_ui`) with far copies, skins and glow maps; the hair table `saiyan/SaiyanHair.java` (all 16 forms); the painter `client/aura/SaiyanHairDraw.java`; the client setting `saiyan.hair`; the CI self-test `saiyan.hair.rules`; the tests `SaiyanHairTest`, `SaiyanHairAssetsTest`, `SaiyanHairSourceScanTest`; the builder `tools/blender/saiyan_hair.py`; the previews. Before this the forms drew **no hair at all** (it had been put on hold), so the hair is new, not a replacement. |
| REPLACED | The Super Saiyan 4 tail: the camera-facing ribbon of seven textured strips (`SaiyanAura.tail()`, `particle/saiyan_tail.png`) is replaced by `saiyan_tail`, a real tapered, furred, curved tail mesh in three swaying parts, **but the old ribbon is kept in the code and is drawn when the new setting `saiyan.tail3d` is off, or when the tail mesh is missing**. Also a test pin changed on purpose: `SaiyanLookSourceScanTest` said "hair is on hold: nothing of it is drawn"; it now pins that hair and tail go through `SaiyanHairDraw`. |
| KEPT | Everything else: the look table `SaiyanLook` (colours, eyes, flames, bolts, armour, fur shell, charge-up), `SaiyanAura`'s ribbon code and its texture, the parked hair maths and texture in `tools/saiyan_look_parked/`, the forms, the server (nothing changed there, no packet), the Time Magic VFX (untouched). The vanilla hair of Base is kept: Base has no hair mesh. |

## What it looks like

Each form wears one of seven hair shapes. The shape is shared where the anime's hair is the same; the form's own colour is multiplied into the shape's neutral skin and added as a
glow, so the forms differ in colour and glow strength as well as in shape (the previews: `docs/saiyan_hair/previews/`).

| Form id | Shape | Hair colour (tint) | Glow | Notes |
|---|---|---|---|---|
| `kaioken` | base | black `1A1418` | red `E03020`, 0.35 | the classic spiky Goku hair, shorter, tips drooping; not self-lit |
| `false_super_saiyan` | ss | dull yellow `D8C070` | `E0B040`, 0.25 | the Super Saiyan shape, paler and dimmer: unfinished |
| `super_saiyan` | ss | gold `FFD020` | `FFD020`, 0.55 | tall upright spikes with a central fringe and two side bangs |
| `super_saiyan_full_power` | ss | brighter gold `FFDC38` | `FFE060`, 0.65 | same shape, golder |
| `super_saiyan_2` | ss2 | brightest gold `FFEA50` | `FFF080`, 0.75 | more, longer, thinner, more jagged spikes; two long bangs |
| `super_saiyan_3` | ss3 | deeper orange-gold `FFB818` | `FFC820`, 0.70 | very long: heavy standing spikes and a mane of long locks down to the knees; no fringe over the brow |
| `super_saiyan_4` | ss4 | black-red `2A1218` | red `C02828`, 0.35 | wild black mane swept back and down, shaggy locks, a fur ruff on the nape; not self-lit |
| `super_full_power_super_saiyan_4` | ss4 | `2C1016` | `E03838`, 0.40 | same mane, redder glow |
| `super_saiyan_4_limit_breaker` | ss4 | `301018` | pink-red `FF4468`, 0.50 | same mane, the strongest glow |
| `super_saiyan_god` | god | red `E02828` | `FF3030`, 0.60 | the Super Saiyan crown, a little smoother and swept, tips curled up |
| `super_saiyan_blue` | god | blue `48B8FF` | `40B0FF`, 0.60 | same shape |
| `perfected_super_saiyan_blue` | god | light blue `68D0FF` | `80D8FF`, 0.70 | same shape, lighter |
| `super_saiyan_blue_kaioken` | god | blue `58A8FF` | red `FF4060`, 0.60 | blue hair with red-hot tips |
| `ultra_instinct_sign` | ui | silver `BCC8E0` | `B0C8FF`, 0.60 | many thin, long, curled spikes; the hair floats loosely (three times the sway) |
| `perfected_ultra_instinct` | ui | white-silver `DCE6F8` | `E8F0FF`, 0.70 | same shape |
| `true_ultra_instinct` | ui | white `F4F8FF` | `FFFFFF`, 0.80 | same shape, whitest |

* **When it appears.** With the charge-up on the player keeps their own hair while the energy gathers; at the flash (two ticks before the burst) the form's hair springs up
  over eight ticks (scale 0.4 to 1, a small spring), and a form change re-triggers it because the aura starts again. With the charge-up off it is there at once. It shrinks away over the last eight ticks of the form.
* **How it moves.** The hair follows the head exactly (turning, nodding, crouching) because it is placed on the player model's head part. It sways a little (about 0.03 rad
  for the crown); the hanging locks of Super Saiyan 3 / 4 sway more and stream back when the player runs; the Ultra Instinct hair floats three times as loosely. The tail is placed on the body part and
  its three segments swing one after the other about their own joints (0.10, 0.16, 0.24 rad), so it whips softly instead of waving as a plank.
* **The tail.** A tube of seven-sided rings tapering from the root to a pointed tuft, curved out and up behind the player, coated with short fur blades; it is worn by the forms that
  already had a tail in `SaiyanLook` (the three Super Saiyan 4 forms), tinted with each form's fur colour.
* **Helmet.** If the player wears a vanilla helmet the hair is not drawn (it would clip through it). The tail is not affected.
* **Not in first person.** Neither is drawn in first person (the player body layers are not drawn there anyway, and the code skips it explicitly).

## Settings (client only, looks only: `config/nusmp-saiyan-look-client.toml`, section `saiyan`)

| Key | Default | Effect |
|---|---|---|
| `saiyan.hair` | true | false = the old no-hair look |
| `saiyan.tail3d` | true | false = the old camera-facing ribbon tail |
| `saiyan.looks` (existing) | true | false switches the whole look layer off, hair and tail included |
| `saiyan.fullDetail` (existing) | true | false = the far copy of hair and tail for everybody (light version) |
| `saiyan.chargeUp` (existing) | true | false = hair is there at once, no swap at the flash |

Nothing on the server depends on any of them; no packet is added (the form code that already travels in the Saiyan aura packet picks the hair).

## Cost (rule 11), worst case

* **Server and network: none.** No tick work, no entity, no packet, no config. (`saiyan.hair.rules` in the CI smoke test only checks the table and the maths.)
* **Client, one player in full detail:** hair at most 1128 quad lines (the Super Saiyan 4 mane, body 711 plus glow copy 417; the other shapes 791 to 1084; cap 1200), tail 640 (cap 700),
  so at most **1768 quad lines** for a Super Saiyan 4 (hair + tail), in at most **three draw batches** (hair cutout, hair additive glow, tail cutout). Other forms have hair only (at most 1084).
* **Far copy** (24 to 48 blocks, or everywhere with `fullDetail` off): the decimated Blender copy, at most 366 (hair) + 410 (tail) = **776 quad lines**, no glow pass.
* **Culling and budget:** nothing beyond 48 blocks (`SaiyanHair.FAR_BLOCKS`); at most **4 players at full detail and 6 at the far copy per frame**, nobody else (`NoelleFormRules.budgeted`, reset every frame).
  Worst case per frame: 4 x 1768 + 6 x 776 = **about 11,700 quad lines** (about 47,000 vertices), well inside what the devil forms already draw (they are allowed 3000 quads per player at the same budget).
* **No allocation per frame:** the matrices, the rotation and the mesh drawers are fields made once; the meshes are read lazily on first use of a shape (the mesh files are 200 to 300 KB of text each, so the
  very first frame a shape is drawn reads one file) and read again after a resource reload. `PerfProbe` section `c.saiyan.hair` times it.
* **Memory/disk:** 16 mesh files (2.3 MB of text, none read until used), 16 skins of 512 x 512 (672 KB).

## How it is made

`tools/blender/saiyan_hair.py` (Blender 5.3 as a Python module, `pip install bpy pillow numpy`; the pipeline of `docs/heroweapon/blender_pipeline.md`, `tools/blender/bl_lib.py` and `bl_entity.py`):

1. A **dome** that covers the head layer (a squarish superellipse section so it covers the hat box; the lower edge follows a hairline: low at the nape and sideburns, high over the forehead).
2. **Locks**: each is a closed, flat-sectioned lozenge swept along a curve (4 or 5 rings), wide and thick at the root and tapering to a point, rooted into the dome. Seven recipes (rows of locks, lengths,
   widths, droop or curl, bangs, side locks, the long mane of Super Saiyan 3 / 4, the fur ruff of Super Saiyan 4) make the seven shapes. Seeded jitter, so a rebuild gives the same files.
3. The **tail**: a tapered seven-sided tube along a smoothed S-curve, split into three parts at its joints (parts `tail`, `wing_l`, `wing_r`), plus short fur blades along it and a tuft at the tip.
4. **Materials** are a neutral light grey with a brushed bump; the lock tips (far from the head) bake a glow. Cycles bakes the lit, bumped surface into a 512 px skin (1024 px bake) and the
   emission into a glow skin. The game multiplies the form's colour into the skin, so one skin serves every form of a shape.
5. `bl_entity.finish_entity` joins, unwraps, bakes, writes `entity_meshes/<id>.txt` and a Decimate LOD copy `<id>_lod.txt`, and the skins in `textures/entity/boss_mesh/`.
6. The previews are rendered in Blender (Cycles) on a blocky player proxy that is **not** part of the export.

The part names borrow the roles the mesh drawer routes (`BossMeshDraw`, `MeshPose`): in a hair mesh `head` is the dome and the standing locks and `tail` the long hanging locks (both worn on the
head); in the tail mesh `tail`, `wing_l`, `wing_r` are the root, middle and tip segment (worn on the body, each carried by the one before). This is why the existing mesh drawer could be reused without change.

Rebuild: `python3 -B tools/blender/saiyan_hair.py [--only ss,tail] [--trial DIR] [--quick] [--no-previews]` (about 12 minutes for all eight on four cores).

## Previews (rendered in Blender, in `docs/saiyan_hair/previews/`)

* `hair_<shape>.png` for `base`, `ss`, `ss2`, `ss3`, `ss4`, `god`, `ui`: front, side and back (each sheet shows the shape in the colour of the first form that wears it).
* `saiyan_tail.png`: front, side, back and three-quarter view of the tail on the proxy.
* `all_forms.png`: the front of all 16 forms in their own tint and glow (the glow is approximated with an emission, the game adds it as an additive pass).

## Sources and confidence

Fandom is blocked from the build session (403), so it was not used (not worked around). Used, with pauses between requests:

| Fact | Source |
|---|---|
| Super Saiyan has blonde hair; Super Saiyan 4 has wilder hair and red fur, black hair chosen for contrast with the fur; Super Saiyan Blue has blue hair | English Wikipedia, "Goku" and "Dragon Ball" (the "Super Saiyan" title redirects to it); fetched |
| Super Saiyan 3: the spiky hair grows to about the waist and the eyebrows vanish; Super Saiyan 4: long black hair and red body fur; the tail fur glows gold in Super Saiyan; Ultra Instinct Sign: hair stays black but floats weightlessly, eyes silver; Perfected Ultra Instinct: the hair is silver and floats | Japanese Wikipedia, "孫悟空 (ドラゴンボール)"; fetched |
| Hair colours per form, the "brighter then deeper" gold, the shape order | the owner's brief and the colours already in `SaiyanLook` (kept as the glow base); the shapes are modelled by eye |

Could not be fetched: the Japanese Wikipedia page "超サイヤ人" (rate limited four times), and all fandom pages (403). Two points differ from the sources and were kept as the owner asked:
Ultra Instinct Sign is **silver** here (the series keeps the hair black in the Sign), and Super Saiyan 3 hangs **to the knees** (the series' hair is closer to the waist).

Confidence: the colours and the ordering are high (the brief and the wiki agree). The shapes are good likenesses of "spiky gold hair", "long Super Saiyan 3 mane", "wild Super Saiyan 4 mane" but are modelled by eye,
not traced from the anime: medium. The Super Saiyan 3 brow-less look is **not done**: the player's skin brows cannot be removed, so the fringe is just kept short.

## Not seen in a game (honest caveats)

There is no Gradle or Minecraft in the build session, so **the Java was not compiled and nothing was seen in a running game**; the previews are Blender renders of the shipped meshes and skins on a proxy,
with the tint and glow approximated. Things to check in a game: the hair sits on the head correctly while crouching, swimming and looking up and down; the hair does not clip through the vanilla hat layer or the Saiyan scouter;
the light level of the self-lit hair at night; the exact brightness of the additive glow; the tail's root position against the Saiyan armour skirt; the first frame a shape is drawn (one mesh file is read).
