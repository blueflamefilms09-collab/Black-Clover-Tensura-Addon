# Beast Will look pass (3.76.0)

The owner's words: the Beast Wills "look ass". The 3.61.0 / 3.62.0 cube forms were pale (most palettes mixed halfway to white), every beast wore the same inflated box of a trunk with the same shoulder slab and the same chest gem, and the tone tiles were speckled. This pass improves the EXISTING generator (`tools/beastwill/remake/`) and re-exports all 27 wills. Before / after sheets: `docs/previews/beastwill_look/before_after_1.png`, `_2.png`, `_3.png` (nine wills each, front and back).

## What changed

**Colour and contrast**
- `forms_gen.py` `PAL`: all 27 palettes re-keyed. Body = the mid tone of the hide, back = its dark, belly = its light, plate = armour that differs in VALUE from the hide (dark metal on a bright beast, pale bone on a dark one), trim = the bright edge, glow = one saturated accent. The near-white wills (Dragon, Fenrir, Pegasus, Kitsune, Wind-Skimmer, Ridge-Wolf, Cu Sith) keep their colour but now have real mid tones and darks.
- `roles()`: shadows are darker, cooler and more saturated (`shadow`), lights go only a fifth of the way to white (`hilite`; it was 30 to 50 percent), the glow material is the saturated accent, not a white blob.
- `forms_lib.py`: the pattern is painted on a half-resolution grid and scaled up (`PIX = 2`: a few clean pixels per cube face instead of speckle) and its strength is halved (`PATTERN_AMP` .13, was .26); the five tone steps are a little further apart.
- `forms_cube.py` `_tone_k`: a fill light from behind, so the back of a form (what a third-person player sees of their own beast) is a shade, not the darkest tone.

**Silhouette**
- `torso()`: a dark under-layer that hugs the player's body, a broad chest block over the upper half, two pectoral plates tilted to a V, abdominal bands that narrow to the belt, dark flanks. The waist reads narrow and the shoulders wide.
- `arm()` / `leg_*()`: slimmer upper arm, a dark narrow elbow and knee, flared and gold-trimmed forearm guard and greave, a thigh guard.
- `pauldron()`: nine shoulder pieces instead of one (slab, spike, fur, round, scale, fin, feather, bone, sleek); `gorget()`: plate, fur ruff, scale collar or none; `emblem()`: gem, sun, fang, chevron, eye, leaf; the belt cloth is a long centre tabard between two short tassets.
- `STYLE` (new table in `forms_gen.py`) picks these per will and sizes the signature feature: Dragon horns x1.45, wings x1.18 and taller spines; Lion mane x1.5 with a ruff and fur shoulders; Fenrir / Ridge-Wolf / Cu Sith / Cerberus ruffs and bigger manes; Minotaur horns x1.4 and a 12 percent broader chest with boulder shoulders; Pegasus wings x1.2; the Kitsune fans THREE tails; the sea wills wear fins; Nidhoggr bone ribs; the Shadow Panther and the Kitsune are sleek (no slab, no collar).

## The setting
Client, `config/nusmp-beastwill-spirit-client.toml`: `beastwill.classicForms` (default false). true draws the forms exactly as they were before this pass. `flatSpirits` and `classicSpirits` still work as before.

How: before the re-export every form file was copied under the key `bwf_<stem>_v1` (221 models `bwf_<stem>_v1_arm_<piece>.geo.json`, 77 idle clips, 27 sheets and 27 glow maps). `BeastWillForms` builds its sets from `bwf_<stem>` or `bwf_<stem>_v1`, cached separately, so the switch works without a restart. The piece list of every stem is unchanged, so `BeastWillFormTable` serves both.

## NEW / REPLACED / KEPT
- NEW: `STYLE`, `pauldron`, the collar and emblem kinds, `shadow` / `hilite`, the three-tailed fox tail, the `classicForms` setting, the `_v1` files, this doc and the preview sheets.
- REPLACED (asked for): the look of the 27 forms in game (models, idle clips and sheets re-exported under the same names).
- KEPT: every stem, piece name, bone name and file name; `BeastWillFormTable.java` and `docs/beastwill_forms.txt` (regenerated, identical); the Acquire piece (the right forearm); the idle animation rules; the old look behind `classicForms`; everything on the server.

## Worst-case cost (rule 11)
Server: nothing. Client: the caps are unchanged and still tested (400 cubes, 2000 quads per will, 16 cubes for the Acquire piece). Biggest will after the pass: Typhon 384 cubes, 1976 quads (as before); then Dragon 336 / 1560, Scylla & Charybdis 287 / 1722, Apophis 275 / 1290, Nidhoggr 273 / 1278; the smallest is Elderwood 119 / 714. The crowd budget of `BeastWillForms` is untouched, so the worst frame is still about 27.7k quads. Disk: the `_v1` copies add about 8 MB of JSON and PNG before compression.

## Tests run (offline runner)
`BeastWillFormsTest` (4), `GeoAssetsTest` (4), `BeastWillRemakeTest`, `BeastWillLookTest`, `ServerSafetyTest`, `ConfigFileNamesTest`: no failure. `geo_check.py` read the exported Typhon and Kitsune back.

## Not seen in a game
The sheets come from the repo's software renderer (`forms_preview.py`), not from Minecraft: no world light, no skin under the armour. The heads are still the full cube heads of 3.61.0 (they must enclose the player's head); the body under them is what changed. The `classicForms` switch was not exercised in a client.

## Heads and silhouette (3.78.0)

The 3.76.0 pass left the heads as they were: a box with a small muzzle stuck on and two dots for eyes, which read as a cartoon face (the Kraken's smile, Scylla's "glasses", the bear-like Lion, Dragon and Wolf). This pass rebuilds every head in `tools/beastwill/remake/forms_heads.py` and tidies the outline.

**Heads.** One shared recipe, proportioned per beast: a muzzle of two tapering segments that projects forward and drops, a separate lower jaw hanging open with the dark of the mouth above it, upper and lower fangs, cheek wedges that turn the box into a wedge, slanted brow ridges over narrow glowing eyes (the will's glow colour, emissive) in dark sockets, and a sloped forehead.
- Wolves, Cerberus, Fenrir, Cu Sith, Kitsune: a long muzzle, dark nose, fangs, the cheek ruff. Panther and Lion: a short broad muzzle, broad nose, whisker pads, sabre fangs (the Lion also a chin tuft).
- Dragon, Flame Serpent, Nidhoggr, Typhon: a long wedge snout, nostril ridges, a row of teeth, a nose horn, a frill of plates behind the jaw, plated brows.
- Ammit: a long flat crocodile snout with interlocking teeth and the eyes riding on top of the skull. Apophis, Hydra, Jormungandr, Yamata: a flat wedge head, long fangs, nostrils, a forked tongue.
- Boar: a heavy snout, flat nose and tusks that curve up past it. Minotaur: the same weight with a ring through the nose. Nue: a baboon's muzzle with long canines and ridges.
- Wind-Skimmer: a hooked beak (cere, beak, ivory hook, lower beak), a raptor's brow, cheek feathers. Pegasus: a long noble head with a plate and gem on the forehead.
- Sphinx: a stern still face (painted brows, slit eyes, nose, a line of a mouth), the false beard and a rearing serpent on the crown. Scylla wears the same face with fangs and side fins instead.
- Kraken: no face at all: the lip of the mantle over a great slit eye on each side, short arms hanging where a face would be, the beak behind them. Hafgufa: a heavy overhanging lip, a dark maw, a thrust lower jaw with tusks, small slit eyes low on the sides, throat grooves. Elderwood: a bark mask with hollow glowing eyes, slab brows, a cracked mouth with light in it and a moss beard.
- Many-headed wills: Cerberus's two side heads are whole heads; the four or seven side heads of Hydra, Typhon and Yamata and Scylla's six dog heads are a light version of the same head (the muzzle, jaw, fangs, one brow bar, eyes) in about a third of the cubes.

**Silhouette.** Shorter, lower feet and paws; a hock joint standing out behind the heel of the digitigrade legs; a ridge down every back (fur for the maned wills, a dorsal fin for the sea wills, leaves for the Elderwood, scale plates for the rest; the wills that already had spines keep them; not the Hafgufa, whose island sits there); tails a little thicker. The trunk and the height are the player's own and were not changed: the form is worn on the player's body, so it cannot be taller or leaner than the body it wraps.

**Cost.** Faces that can never be seen (the back of a cube against the skull, the buried ends of a segment) are no longer written, so the heads cost fewer quads than before although they have more cubes. Worst will after the pass: Typhon and the Dragon, 352 cubes; most quads Typhon 1672 (was 384 cubes and 1976 quads). Caps unchanged (400 cubes, 2000 quads, Acquire piece 16 cubes: largest 16, the Kitsune).

**Kept.** All stems, piece names, bone names and file names, every animation clip, `BeastWillFormTable` and `docs/beastwill_forms.txt` (regenerated, no difference), and the classic set `bwf_<stem>_v1` (not touched; the client setting `beastwill.classicForms` still shows it).

**Preview.** `docs/previews/beastwill_look/heads_1.png`, `heads_2.png`, `heads_3.png`: every will, 3.76.0 against this pass, a front view and a three-quarter head close-up.

**Not seen in a game.** The pictures are the repo's software renderer; the exported files of five wills (Typhon, Dragon, Kraken, Scylla & Charybdis, Wind-Skimmer) were read back with `geo_check.py` and draw the same heads. How the open jaws and brows sit under the idle animation, in first person, and next to a real skin's hat layer was not checked.
