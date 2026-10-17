# The art kit of the arc characters

Version of the kit: the one in this commit (the lead sets the mod version). Written for the eight builders who make the thirty-odd characters of the arcs
(`com.newuniverse.nusmp.arc`, contract in `docs/labyrinth/mega_v2.md` section 6). The two reference characters are `tools/arc/char_yami_sukehiro.py` (an NPC) and
`tools/arc/char_heath_grice.py` (a boss with a second form); copy the one that is nearer to yours.

## 1. What this is and why

A character of the arcs is **one python file** `tools/arc/char_<id>.py` with a dict `SPEC` (and, only if the character needs one signature part, a function
`extra(kit, rig)`). `python3 -B tools/gen_arc_characters.py` turns every such file into a complete GeoLite model, ready for the generic renderers:

| written to | what |
|---|---|
| `src/main/resources/assets/nusmp/geo/entity/arc_<id>.geo.json` | the model (bones, cubes, uv) |
| `src/main/resources/assets/nusmp/animations/entity/arc_<id>.animation.json` | the clips: `idle walk run attack cast cast_big hurt transform death` and, when the spec asks, `spawn` and `chest_gem_reveal` |
| `src/main/resources/assets/nusmp/textures/entity/arc_<id>.png` | the texture (512 px wide, painted by the kit: cloth, leather, metal, hair, skin, face) |
| `src/main/resources/assets/nusmp/textures/entity/arc_<id>_glow.png` | the glow layer (drawn fully bright and additive: eyes, magic, emblems, a glowing blade) |
| `tools/render_preview/scenes.d/arc_<id>.json` | the preview scenes (section 10) |
| `tools/arc/manifest.json` | the facts of every model: height, hitbox, the numbers of `ArcLook`, clips, bones, glow bones, how a two-handed prop is held |

A spec with a `form2` section also gets `arc_<id>_form2.*` (the second form: the same spec with the overrides laid over it).

**Why a kit and not thirty hand-built models.** `tools/gen_zogratis_lucius.py` shows the quality bar (a hand-built model, about 900 lines for one character). Thirty of
those would never be finished, and no two would move alike. So the kit does what Lucius's generator did by hand, once, and parametrised: the paint (layered materials with a
hard-edged palette, per-face texel density, mirrored twins sharing one uv rectangle), the face (a 32 x 32 painted canvas), the hair (32 styles built from stepped caps, spikes
and plates), the clothes, and above all the clips: every character moves with the same names, the same timing contract (section 9) and the same rules, and no limb goes
through the body, because the kit solves the arms (inverse kinematics) instead of anybody keying forty bones by hand.

**The rules that apply to the files of this kit** (from the owner and the lead): add the new, keep the old (a character file is never edited by another builder; a prop,
hair style or accessory is appended, never replaced); the Time Magic VFX files are not touched; every file starts with a `Why:` docstring; comments say why; nothing
here is claimed to run in the game (section 15 lists what was and was not checked).

## 2. Quick start (a character in under an hour)

1. Copy the nearest reference: `tools/arc/char_npc_default.py` (the smallest NPC), `char_yami_sukehiro.py` (NPC with a weapon, cloak, accessory, bandages),
   `char_heath_grice.py` (boss: second form, wings, halo, floating prop, `extra`). Rename it `char_<id>.py` and change `"id"` to `<id>` (the file name decides the id).
2. Write the look in `SPEC` (section 3 has an annotated example, sections 4 and 5 every name and default). Start with the big things: build, height, skin, hair, the top,
   the legs, one prop; look; then add the details.
3. Build and look:

```
python3 -B tools/gen_arc_characters.py <id>                          # builds the files above (a few seconds)
python3 -B tools/gen_arc_characters.py --preview <id> front face idle attack     # builds, then renders those scenes
```

   The renderer writes PNGs to `build/render_preview` (or `$RENDER_PREVIEW_BUILD`). It needs JDK 21, python with numpy and Pillow (section 10). **Look at every clip**
   (`--preview <id>` with no names renders all of them, about a minute): the kit guarantees the rules of section 9, not that the pose is the one you imagine.
4. Fix what is ugly (a colour that is too dark in the light of the game, a hair style that hides the face, a prop that is too big: `props.<slot>.length`).
5. Run `python3 -B tools/gen_arc_characters.py --check` (rebuilds everything into a temporary folder and fails when a committed file differs or a character breaks
   a rule), and commit the generated files together with `tools/arc/char_<id>.py`. The lead takes the numbers of `ArcLook` from `tools/arc/manifest.json` (section 11).

Building takes about 1 to 3 seconds per model on an idle machine (the four models of this commit take 5 seconds together); the preview render is what takes time
(about 2 seconds per scene).

## 3. A complete, annotated spec

This file builds as written (it is checked by the selftest of the kit). Every key not named takes its default (section 5).

```python
"""tools/arc/char_example_knight.py: every file starts with a docstring whose first paragraph says WHY the character exists and where the look comes from."""
SPEC = {
    "id": "example_knight",              # snake_case; must equal the file name char_<id>.py; the model is arc_<id>
    "name": "Ser Example",               # the display name
    "role": "boss",                      # "npc" or "boss": an NPC's `cast` clip is a calm gesture (it is the talking clip), a boss's is magic
    "build": "broad",                    # slim average broad huge small lanky child (section 6)
    "height": 2.2,                       # blocks; leave out for the build's own height
    "skin": "tan",                       # a tone name, "#c89070", or a list of 4 colours dark to light
    "face": {
        "expression": "stern",           # picks eyes + brows + mouth; any of them can be set by hand next to it
        "eye_color": "#4a6aa8",
        "marks": ["scar_cheek_l", "stubble"],
    },
    "hair": {"style": "undercut", "color": "#3a2a1a"},
    "outfit": {
        "top": {"kind": "coat", "color": "#2c3a5a", "trim": "#c8a040", "length": "calf", "collar": "high",
                "emblem": {"kind": "crest", "where": "back", "size": 8}},     # a symbol painted on the back (or "chest")
        "inner": {"kind": "shirt", "color": "#e8e4dc"},
        "sleeves": {"kind": "long", "cuff": "#c8a040"},
        "legs": {"kind": "trousers", "color": "#1e1e28"},
        "boots": {"kind": "boots", "color": "#2a2220", "height": "knee"},
        "gloves": {"kind": "gauntlets", "color": "#6a6a74"},
        "belt": {"kind": "double", "color": "#3c281c", "buckle": "#c8a040", "pouches": True},
    },
    "cape": {"kind": "cloak", "color": "#7a1e24", "ragged": True, "emblem": {"kind": "wings", "size": 9}},
    "armor": {"pieces": ["pauldrons", "bracers", "gorget"], "style": "plate", "color": "#9aa4b4", "trim": "#c8a040"},
    "accessories": ["monocle", {"kind": "earrings", "color": "#c8a040", "side": "left"}],
    "props": {
        "right": {"kind": "greatsword", "color": "#c8d0e0", "color2": "#c8a040", "glow": "#ffcc66"},   # the blade glows in the glow layer
        "back": {"kind": "shield"},                                                                   # stowed on the back, not held
    },
    # wings, tail, halo, horns, ears, cloak ...: every part you do not name is simply not built
    "glow": {"color": "#ffcc66", "eyes": True, "props": True, "trim": True},   # ONE glow colour for eyes, props and emblems
    "clips": {"attack": "overhead", "cast": "magic", "idle": "guard", "extra": ["spawn"]},
    "form2": {                           # an optional second form: the overrides laid over everything above
        "name": "Ser Example, Unchained",
        "armor": {"style": "bone", "color": "#d8d0b4"},
        "cape": {"color": "#202030"},
        "glow": {"color": "#ff5040"},
    },
}


def extra(kit, rig):
    """An optional signature part. `kit` is gen_arc_kit (painters, colour helpers), `rig` the model under construction (rig.d = the measured body)."""
    d = rig.d
    gem = kit.metal(kit.shades("#ff5040", 5, 1.2), 31, glow=(255, 80, 64, 220), emboss=False)
    rig.box("body", (-1.5, d.sh_y - 5.0, -d.td / 2 - 0.6), (3.0, 3.0, 1.0), gem)       # a glowing gem on the chest
    rig.bone("charm", "body", (d.tw / 2, d.hip_y + 3.0, 0.0))                          # a bone of our own ...
    rig.box("charm", (d.tw / 2, d.hip_y + 1.0, -1.0), (1.5, 3.0, 2.0), gem)
    rig.animate("charm", sway=1.0)                                                      # ... that swings like cloth in every clip


def post(kit, rig):
    """Optional: touch up the finished clips (rig.A.clips is {name: {"loop", "animation_length", "bones": {bone: {"rotation": {"0.00": [x, y, z], ...}}}}})."""
    pass
```

The three parts of a character file:

* `SPEC`: the data. A typo is an error that names the path and lists the valid names (`outfit.top.kind: 'armour' is not valid; use one of: bare, tank, ...`).
* `extra(kit, rig)` (optional): called after the body, clothes, accessories and props are built and before the clips are made. `kit` is the module `gen_arc_kit`
  (painters, colour helpers, vocabulary); `rig` is the model under construction (section 12 lists what it offers). Add bones and cubes; bones named in `rig.animate(...)`
  move in every clip.
* `post(kit, rig)` (optional): called after the clips are made; `rig.A.clips[name]["bones"][bone]["rotation"]` is `{"0.00": [x, y, z], ...}` (degrees), so a character that
  needs one signature gesture (a pose held at the end of `cast_big`, a bow) can rewrite a track. Everything the kit generated stays valid; only touch what you must.


## 4. Every name

The vocabularies below are printed from the code of this commit; `--gallery <set>` draws any of them (section 10).

### 4.1 Hair, face, head

| field | names |
|---|---|
| `hair.style` (32) | `bald`, `crop`, `short`, `spiky`, `spiky_back`, `messy`, `slick`, `side_part`, `bob`, `long`, `long_wavy`, `ponytail`, `high_ponytail`, `twin_tails`, `twin_buns`, `bun`, `topknot`, `braid`, `twin_braids`, `mohawk`, `afro`, `hood`, `undercut`, `pompadour`, `wild_mane`, `bangs_long`, `curtains`, `shaggy`, `drills`, `spiky_long`, `samurai`, `wizard` |
| `face.eyes` (12) | `round`, `big`, `sharp`, `narrow`, `slit`, `tired`, `fierce`, `wide`, `closed`, `glow`, `empty`, `smile` |
| `face.brows` (9) | `none`, `thin`, `thick`, `angry`, `sad`, `raised`, `flat`, `heavy`, `arched` |
| `face.mouth` (10) | `neutral`, `smile`, `smirk`, `frown`, `grin`, `open`, `fangs`, `line`, `pout`, `none` |
| `face.nose` (4) | `small`, `none`, `sharp`, `wide` |
| `face.marks` (30) | `scar_diag`, `scar_diag_r`, `scar_eye_l`, `scar_eye_r`, `scar_cheek_l`, `scar_cheek_r`, `scar_brow_l`, `scar_brow_r`, `stubble`, `moustache`, `goatee`, `beard`, `blush`, `freckles`, `tear_l`, `tear_r`, `fang_marks`, `forehead_sigil`, `third_eye`, `cheek_lines`, `eye_bags`, `stitches`, `cracks`, `veins`, `war_paint`, `lipstick`, `eyeshadow`, `forehead_gem`, `light_lines`, `mole` |
| `face.mask.kind` (8) | `half`, `full`, `visor`, `skull`, `oni`, `veil`, `plague`, `fox` |

`face.expression` picks eyes, brows and mouth together (any of the three can still be set by hand):

| expression | eyes | brows | mouth |
|---|---|---|---|
| `calm` | `round` | `thin` | `neutral` |
| `stern` | `sharp` | `thick` | `line` |
| `angry` | `fierce` | `angry` | `frown` |
| `smirk` | `sharp` | `thin` | `smirk` |
| `cheerful` | `big` | `raised` | `smile` |
| `grin` | `round` | `thick` | `grin` |
| `sad` | `round` | `sad` | `frown` |
| `tired` | `tired` | `flat` | `line` |
| `sinister` | `narrow` | `angry` | `grin` |
| `mad` | `wide` | `angry` | `fangs` |
| `cold` | `narrow` | `flat` | `line` |
| `shy` | `big` | `sad` | `smile` |
| `dead` | `empty` | `none` | `line` |
| `demon` | `slit` | `angry` | `fangs` |

### 4.2 Body and clothes

| field | names |
|---|---|
| `build` (7) | `slim`, `average`, `broad`, `huge`, `small`, `lanky`, `child` |
| `skin` (14 tones, or a `#hex`, or a list of 4 colours dark to light) | `pale`, `fair`, `light`, `tan`, `brown`, `dark`, `olive`, `ashen`, `grey`, `red`, `blue`, `green`, `purple`, `undead` |
| `outfit.top.kind` (12) | `bare`, `tank`, `shirt`, `tunic`, `vest`, `coat`, `robe`, `uniform`, `dress`, `kimono`, `tabard`, `jumpsuit` |
| `outfit.top.length` (7) | `none`, `hip`, `thigh`, `knee`, `calf`, `ankle`, `floor` |
| `outfit.top.collar` (5) | `none`, `stand`, `high`, `fur`, `wide` |
| `outfit.top.material` (8) | `cloth`, `heavy`, `leather`, `plate`, `chain`, `fur`, `bone`, `scale` |
| `outfit.top.pattern` (6) | `plain`, `stripes`, `patches`, `runes`, `checks`, `dots` |
| `outfit.top.emblem.kind` (17) | `clover`, `bull`, `star`, `cross`, `skull`, `sun`, `moon`, `flame`, `wings`, `diamond`, `heart`, `spade`, `rune`, `crest`, `eye`, `tree`, `none` |
| `outfit.inner.kind` (4) | `none`, `bare`, `tank`, `shirt` |
| `outfit.sleeves.kind` (7) | `auto`, `none`, `short`, `long`, `wide`, `bell`, `torn` |
| `outfit.legs.kind` (8) | `trousers`, `shorts`, `skirt`, `bare`, `hakama`, `baggy`, `leggings`, `chaps` |
| `outfit.boots.kind` (7) | `boots`, `shoes`, `sandals`, `bare`, `geta`, `greaves`, `sabatons` |
| `outfit.gloves.kind` (5) | `none`, `gloves`, `fingerless`, `wraps`, `gauntlets` |
| `outfit.belt.kind` (4) | `plain`, `double`, `sash`, `obi` |
| `outfit.neck.kind` (8) | `none`, `scarf`, `belts`, `strap`, `cravat`, `choker`, `necklace`, `fur` |
| `outfit.bandages.parts` (13) | `chest`, `torso`, `arm_l`, `arm_r`, `forearm_l`, `forearm_r`, `leg_l`, `leg_r`, `shin_l`, `shin_r`, `neck`, `hand_l`, `hand_r` |
| `cape.kind` (5) | `cape`, `cloak`, `mantle`, `tattered`, `hooded_cloak` |
| `armor.pieces` (10) | `pauldrons`, `breastplate`, `bracers`, `gauntlets`, `greaves`, `sabatons`, `tassets`, `helm`, `gorget`, `spikes` |
| `armor.style` (5) | `plate`, `chain`, `leather`, `bone`, `scale` |

### 4.3 Accessories, creature parts, props

| field | names |
|---|---|
| `accessories[].kind` (28) | `glasses`, `monocle`, `eyepatch`, `headband`, `bandana`, `cap`, `beret`, `wizard_hat`, `witch_hat`, `crown`, `tiara`, `circlet`, `earrings`, `cigarette`, `pipe`, `pocket_watch`, `shoulder_fur`, `backpack`, `quiver`, `goggles`, `mask_band`, `flower`, `ribbon`, `veil`, `ear_cuffs`, `tassel`, `chain`, `satchel` |
| `wings.kind` (6) | `bat`, `feathered`, `angel`, `dragon`, `fairy`, `mechanical` |
| `tail.kind` (7) | `cat`, `wolf`, `fox`, `devil`, `lizard`, `dragon`, `rat` |
| `horns.kind` (7) | `devil`, `ram`, `oni`, `single`, `antlers`, `bull`, `small` |
| `ears.kind` (7) | `elf`, `cat`, `wolf`, `fox`, `rabbit`, `pointed`, `fin` |
| `halo.kind` (5) | `ring`, `gears`, `broken`, `thorns`, `flames` |
| `props.<slot>.kind` (16) | `sword`, `katana`, `dagger`, `greatsword`, `spear`, `trident`, `staff`, `wand`, `axe`, `hammer`, `mace`, `scythe`, `bow`, `grimoire`, `orb`, `shield` |
| `props` slots | `right`, `left`, `back`, `float` |

### 4.4 Clip styles

| field | names |
|---|---|
| `clips.attack` | `auto`, `slash`, `overhead`, `thrust`, `smash`, `punch`, `cast_jab`, `shoot`, `stab`, `sweep`, `none` |
| `clips.cast` | `auto`, `gesture`, `magic`, `pray`, `book` |
| `clips.idle` | `auto`, `relaxed`, `guard`, `proud`, `slouch`, `hover` |
| `clips.extra` | `chest_gem_reveal`, `spawn` |

### 4.5 Colour names

Every colour is `"#rrggbb"`, `"#rgb"`, `"rrggbb"`, an `(r, g, b)` tuple, or one of these names: `black`, `ink`, `charcoal`, `grey`, `gray`, `silver`, `white`, `cream`, `ivory`, `bone`, `beige`, `tan`, `brown`, `dark_brown`, `leather`, `red`, `crimson`, `maroon`, `orange`, `gold`, `yellow`, `olive`, `green`, `forest`, `teal`, `cyan`, `ice`, `blue`, `navy`, `indigo`, `purple`, `violet`, `magenta`, `pink`, `rose`, `steel`, `iron`, `bronze`, `copper`, `rust`.

## 5. Spec fields and their defaults

Every key not listed in a spec takes the value below. An unknown key is an error that lists the valid ones. `null` = not set (the part is not built, or the value is derived).

### 5.1 Top level

| key | default | meaning |
|---|---|---|
| `id` | `null` | the snake_case id; it must equal the file name `char_<id>.py` |
| `name` | `null` | the display name (default: the id, title-cased) |
| `role` | `"npc"` | `npc` or `boss`: decides the default cast style (an NPC gestures, a boss casts) |
| `build` | `"average"` | the body: see 4.2 |
| `height` | `null` | blocks; `null` = the build's own (slim 1.95, average 2.0, broad 2.1, huge 3.1, small 1.5, lanky 2.3, child 1.4) |
| `widen` | `1.0` | multiplies every body width (torso, arms, legs) |
| `skin` | `"light"` | see 4.2 |
| `face` | see below | 5.2 |
| `hair` | see below | 5.3 |
| `outfit` | see below | 5.4 |
| `cape` | `null` | 5.5 |
| `armor` | `null` | 5.6 |
| `accessories` | `[]` | 5.7: a list of names or dicts |
| `props` | `{}` | 5.8: slot -> prop |
| `wings` | `null` | 5.9 |
| `tail` | `null` | 5.9 |
| `halo` | `null` | 5.9 |
| `horns` | `null` | 5.9 |
| `ears` | `null` | 5.9 |
| `glow` | see below | 5.10 |
| `clips` | see below | 5.11 |
| `form2` | `null` | 5.12 |
| `notes` | `""` | free text for the next builder; not used |

### 5.2 `face`

`expression` fills `eyes`, `brows` and `mouth` that are `null`. `mask` is `null`, a kind name, or a dict `{kind, color, color2, glow, eye_glow}` (defaults `half`, `#d8d0b4`, `#2a2024`, none, none). `marks` is a list of names or `{kind: ...}` dicts. `eye_glow` makes the eyes shine in the glow layer.

| key | default |
|---|---|
| `expression` | `null` |
| `eyes` | `null` |
| `eye_color` | `"#4a6aa8"` |
| `eye_glow` | `null` |
| `sclera` | `null` |
| `brows` | `null` |
| `brow_color` | `null` |
| `mouth` | `null` |
| `lip_color` | `null` |
| `nose` | `"small"` |
| `lashes` | `false` |
| `marks` | `[]` |
| `eye_shift` | `0` |
| `brow_shift` | `0` |
| `mask` | `null` |

### 5.3 `hair`

`streak` colours a lock; `ribbon` ties a bow or band; `cover` is for the `hood` style.

| key | default |
|---|---|
| `style` | `"short"` |
| `color` | `"#2a2018"` |
| `streak` | `null` |
| `ribbon` | `null` |
| `cover` | `null` |

### 5.4 `outfit`

Sub-sections `top`, `inner`, `sleeves`, `legs`, `boots`, `gloves`, `neck` always exist; `belt` and `bandages` exist only when given (`true` takes the defaults).

**`outfit.top`**

| key | default |
|---|---|
| `kind` | `"shirt"` |
| `color` | `"#4a5a7a"` |
| `trim` | `null` |
| `length` | `null` |
| `open` | `null` |
| `collar` | `"none"` |
| `collar_color` | `null` |
| `buttons` | `false` |
| `pattern` | `"plain"` |
| `pattern_color` | `null` |
| `emblem` | `null` |
| `ragged` | `false` |
| `fold` | `false` |
| `hem_trim` | `false` |
| `open_top` | `7` |
| `open_bottom` | `3` |
| `material` | `null` |

**`outfit.inner`**

| key | default |
|---|---|
| `kind` | `null` |
| `color` | `"#ecebea"` |

**`outfit.sleeves`**

| key | default |
|---|---|
| `kind` | `"auto"` |
| `color` | `null` |
| `cuff` | `null` |

**`outfit.legs`**

| key | default |
|---|---|
| `kind` | `"trousers"` |
| `color` | `"#2c2c38"` |
| `chaps` | `"#c8b48c"` |
| `stripe` | `null` |

**`outfit.boots`**

| key | default |
|---|---|
| `kind` | `"boots"` |
| `color` | `"#2a2220"` |
| `height` | `"calf"` |
| `cuff` | `null` |

**`outfit.gloves`**

| key | default |
|---|---|
| `kind` | `"none"` |
| `color` | `"#2a2a30"` |
| `side` | `"both"` |

**`outfit.neck`**

| key | default |
|---|---|
| `kind` | `"none"` |
| `color` | `"#7a1e24"` |
| `gem` | `"#c83030"` |
| `glow` | `false` |

`outfit.top.emblem` is `null` or a dict with the defaults `{kind: "none", color: null (= the trim colour), where: "back", size: 6, aspect: 1.0, y: 3, dx: 0, glow: null, dark: null}`; `where` is `back` or `chest`.

**`outfit.belt`**: `kind` = `"plain"`, `color` = `"#3c281c"`, `buckle` = `"#c8a040"`, `pouches` = `false`, `sash` = `null`, `books` = `false`.

**`outfit.bandages`**: `parts` = [] (a list of body parts, see 4.2) and `color` = `"#d8d0b4"`.

### 5.5 `cape`

`kind` = `"cape"`, `color` = `"#3a2a3c"`, `trim` = `null`, `length` = `0.8`, `width` = `1.0`, `emblem` = `null`, `ragged` = `false`, `clasp` = `"#c8a040"`, `hood` = `false`, `lining` = `null`. `emblem` is a dict like the one of `outfit.top.emblem`. `ragged` cuts the hem into tatters, `hood` adds a hood, `length` is the fraction of the body (0.8 = to the calves), `width` scales the width, `lining` is the colour of the inside.

### 5.6 `armor`

`pieces` = `[]`, `color` = `"#8c98a8"`, `trim` = `"#c8a040"`, `style` = `"plate"`, `glow` = `null`, `emblem` = `null`. `glow` lights the trim; `emblem` puts an emblem name on the breastplate.

### 5.7 `accessories`

A list. An entry is a name (`"glasses"`) or a dict with `kind` = `null`, `color` = `null`, `color2` = `null`, `glow` = `null`, `side` = `null`. `side` picks the side of a one-sided item (an eye patch, an earring).

### 5.8 `props`

A dict of slot -> prop. An entry is a kind name (`"katana"`) or a dict with `kind` = `null`, `color` = `null`, `color2` = `null`, `glow` = `null`, `length` = `1.0`, `grip` = `null`, `sheathed` = `false`, `two_hand` = `null`. `length` scales the model; `grip` turns it in the fist (degrees, x y z); `sheathed` (only `sword`, `katana`, `dagger`) wears the blade in a scabbard at the hip instead of holding it; `two_hand: false` makes a two-handed prop be carried in one hand.

### 5.9 Creature parts

`wings`: `kind` = `"bat"`, `color` = `"#3a2a48"`, `color2` = `null`, `glow` = `null`, `span` = `1.0`, `folded` = `false`. (A bare kind name is enough: `"wings": "bat"`.)

`tail`: `kind` = `"devil"`, `color` = `"#6a2a3a"`, `color2` = `null`, `glow` = `null`, `length` = `1.0`. (A bare kind name is enough: `"wings": "bat"`.)

`horns`: `kind` = `"devil"`, `color` = `"#3a2e28"`, `color2` = `null`, `glow` = `null`, `size` = `1.0`. (A bare kind name is enough: `"wings": "bat"`.)

`ears`: `kind` = `"elf"`, `color` = `null`, `inner` = `null`, `size` = `1.0`. (A bare kind name is enough: `"wings": "bat"`.)

`halo`: `kind` = `"ring"`, `color` = `"#f2d070"`, `glow` = `"#ffe58a"`, `radius` = `1.0`, `behind` = `true`. (A bare kind name is enough: `"wings": "bat"`.)

### 5.10 `glow`

`color` is the character's one glow colour. The eyes glow in it (`eyes`), the props that have no glow of their own (`props`) and the emblems of the outfit and the cape (`trim`). A colour set on the part itself always wins; without `color` nothing is inherited.

| key | default |
|---|---|
| `color` | `null` |
| `eyes` | `true` |
| `props` | `true` |
| `trim` | `false` |

### 5.11 `clips`

| key | default |
|---|---|
| `attack` | `"auto"` |
| `cast` | `"auto"` |
| `idle` | `"auto"` |
| `extra` | `[]` |

`attack` = `auto` takes the swing of the main prop (a sword slashes, a spear thrusts, a hammer smashes ...) or a punch with bare hands; `cast` = `auto` takes `gesture` for an NPC, `magic` for a boss, `book` for a grimoire; `idle` = `auto` is `relaxed` (`guard` leans forward on bent legs, `proud` stands tall, `slouch` hangs, `hover` floats 5 px above the ground and the feet never touch it). `extra` asks for the optional clips.

### 5.12 `form2`

A second form: a dict with any of `name`, `skin`, `face`, `hair`, `outfit`, `cape`, `armor`, `accessories`, `props`, `wings`, `tail`, `halo`, `horns`, `ears`, `glow` laid over the first form's spec, section by section (dicts merge, anything else replaces). It may not change the build, the height or the id (the two models must stand in the same place). The driver builds a second model `arc_<id>_form2`.

## 6. Builds

| build | torso w x d | arm w x d | leg w x d | leg share | default height | hand | neck |
|---|---|---|---|---|---|---|---|
| `slim` | 7 x 4 | 3 x 3 | 3 x 3.5 | 0.54 | 1.95 blocks | 2.5 | 1.5 |
| `average` | 8 x 4.5 | 3.5 x 3.5 | 3.5 x 4 | 0.52 | 2 blocks | 2.5 | 1.0 |
| `broad` | 10 x 5.5 | 4.5 x 4.5 | 4.5 x 5 | 0.48 | 2.1 blocks | 3.0 | 0.5 |
| `huge` | 15 x 8 | 6.5 x 6.5 | 6.5 x 7 | 0.45 | 3.1 blocks | 4.0 | 0.0 |
| `small` | 6 x 3.5 | 2.5 x 2.5 | 2.5 x 3 | 0.5 | 1.5 blocks | 2.0 | 0.0 |
| `lanky` | 7 x 3.5 | 2.5 x 2.5 | 3 x 3.5 | 0.55 | 2.3 blocks | 2.5 | 2.0 |
| `child` | 6 x 3.5 | 2.5 x 2.5 | 2.5 x 3 | 0.46 | 1.4 blocks | 2.0 | 0.0 |

(Widths and depths in model pixels, 16 = one block. The head is always 8 px.)


## 7. The skeleton, the frame and the signs

One universal humanoid skeleton, so the same clips fit every model. The frame is the one of GeoLite and Blockbench: **pixels** (16 = one block), **y up**, the origin at
the feet, the model's **front on -z**, **+x is the model's left** (so the `_l` bones are at +x; on screen, facing the camera, the model's left is on the viewer's right).
Animation rotations are degrees **added** to the bone's own rotation, positions are pixels, scales multiply.

Signs of a rotation, the ones every recipe is written against: positive **x** swings a hanging limb **forward** (an arm raises forward) and negative x folds a knee
and leans the body forward; positive **z** swings a hanging limb towards **+x** (a right limb abducts with a negative z); positive **y** turns a part towards the model's
**right**.

| bone | parent | pivot | carries |
|---|---|---|---|
| `root` | - | the feet | moved by the grounding of the feet, the hover, the fall of the death clip |
| `body` | `root` | the hips | torso, clothes, belt, cape bones, wings, `prop_back`, `prop_hip_*` |
| `head` | `body` | the shoulder line | head box, neck, hair, mask, hat, horns, ears, halo, cigarette |
| `arm_l` `arm_r` | `body` | the shoulder joint | upper arm, sleeve, pauldron |
| `forearm_l` `forearm_r` | arm | the elbow | forearm, cuff, glove, bracer (a shield is strapped here) |
| `hand_l` `hand_r` | forearm | the wrist | the hand; its child bone `prop_l` / `prop_r` carries the held prop |
| `leg_l` `leg_r` | `root` | the hip | thigh, trouser leg |
| `shin_l` `shin_r` | leg | the knee | shin, boot |
| `skirt_l` `skirt_r` | `root` | the hip | the panels of a coat, robe, skirt or hakama that swing with the legs |

Bones that exist only when the spec asks for the part (all of them are given motion by the clips; section 9): `hair`, `hair_back`, `hair_tail_l/r`, `cape`, `cape_low`,
`ribbon`, `sash`, `tassel`, `watch`, `hat`, `cigarette`, `ear_l/r`, `horns`, `wing_l/r` (+ `wing_tip_l/r`), `tail1` ... `tail5`, `halo`, `prop_r`, `prop_l`, `prop_back`,
`prop_float`, `prop_hip_l/r`. The manifest lists the bones of every model.

`rig.d` is the measured body (`Dims`, in pixels): `top` (top of the head box), `head_y`, `head_top`, `sh_y` (the shoulder line), `sh_piv` (the shoulder joint), `hip_y`,
`leg_len`, `torso_h`, `tw` `td` (torso width and depth), `aw` `ad` (arm), `lw` `ld` (leg), `arm_x` (the arm's offset from the middle), `upper` `fore` `hand` (arm segments),
`elbow_y`, `wrist_y`, `knee_y`, `neck`, `build`. An `extra` part is placed from these, never from fixed numbers, so it fits every build.

## 8. Props

| kind | hand | held | swing (`attack`) | cast | notes |
|---|---|---|---|---|---|
| `sword` | right | one hand | slash | gesture | can be `sheathed` |
| `katana` | right | one hand | slash | gesture | curved; can be `sheathed`; Yami's |
| `dagger` | right | one hand | stab | gesture | can be `sheathed` |
| `greatsword` | right | two hands | overhead | gesture | 48 px long at a 32 px character |
| `spear` | right | two hands | thrust | gesture | |
| `trident` | right | two hands | thrust | gesture | |
| `staff` | right | one hand | cast_jab | magic | gem on top glows |
| `wand` | right | one hand | cast_jab | magic | |
| `axe` `hammer` `mace` | right | one hand | smash | gesture | |
| `scythe` | right | two hands | sweep | gesture | |
| `bow` | left | the bow in the left hand, the string hand free | shoot (draw to the cheek and release) | gesture | the string is part of the model, it does not move |
| `grimoire` | left | open in the hand | cast_jab | book | the pages glow |
| `orb` | right | on the palm | cast_jab | magic | the orb glows |
| `shield` | left | strapped to the forearm | slash (the other hand) | gesture | on the right slot it is mirrored |

`props.<slot>.glow` colours the glowing part of the prop (a blade's edge, a gem, pages, the orb). A shield is not swung: with a shield in one slot and a weapon in
the other, the weapon decides the clips and the shield arm rests raised behind the shield in every clip. `grip` (degrees x y z) turns a prop in the fist.

Slots: `right`, `left` (in the hand), `back` (stowed across the back), `float` (orbits slowly beside the character in every clip).

**Two-handed props.** The kit tries to hold the shaft with both hands: it fits one rest hold to the body (the shaft tilted across the chest and shifted until both arms
reach it with elbows that clear the torso) and every clip starts and ends on it. The arms of a blocky body are short, so a broad or huge build may not be able to: then
the prop is carried in one hand and the other arm hangs (`spear` and `trident` of a broad or huge build). After the clips are made the build checks them against the arm
rules (section 9.4) and, if a two-handed prop breaks one, builds the clips again with the prop in one hand. `props.<slot>.two_hand: false` asks for the one-handed carry
by hand. The manifest says which it is (`grip`: `two-handed` or `one-handed`, with a note).

**How to add a prop.** (1) In `tools/arc_kit/props.py` write `p_<name>(pb)` with the `PB` helpers: the prop is authored standing up, the grip at the origin, the long axis
along +y, the flat faces towards +-z; `pb.box(x, y, z, sx, sy, sz, paint, rot=None, pivot=None)`, `pb.steel(salt, edge)`, `pb.metal(color, salt, glow)`, `pb.leather(color,
salt)`, `pb.wrap(color, salt)`, `pb.curve(start, [(length, turn_degrees), ...], width, paint)` for a bend (a scythe blade, a bow limb), `pb.L(v)` for a length scaled by the
character and by `props.<slot>.length`, `pb.glow` (the glow colour or `None`), `pb.col` / `pb.col2` (the colours of the spec). (2) Add a `PropDef(build, hand, two, grip2, attack,
grip, cast, orient)` row to `PROPS`: `grip2` is where the other hand grips (prop-local), `orient = (local vector, mode)` says which side of the prop must face where (`edge`,
`lateral`, `forward`, `back`, `up`; see `anim.side_dir`). (3) Add a rest hold row to `clips.HOLD` (elevation, azimuth, reach fraction, phi, tilt for a right hand: where
the wrist is and where the long axis points; see `anim.ArmIK.target` and `anim.dir_from`). (4) Run `--selftest`: it builds every prop in every slot. A prop that only one
character uses can be added from the character file instead: `kit.register_prop("name", kit.PropDef(...))` and `kit.recipes.HOLD["name"] = (...)`.

## 9. The clips

### 9.1 The contract

The same clip names on every model, so the generic renderers need no per-character data. Times are what the Zogratis clock plays (`client.zogratis.ZogratisPose`).

| clip | length | loops | what it is |
|---|---|---|---|
| `idle` | 3.2 s | yes | breathing, a slow head sway; follow-through of hair, cloth, tails, wings; style `relaxed` / `guard` / `proud` / `slouch` / `hover` |
| `walk` | 1.1 s | yes | one cycle (two steps); the renderer paces it by the ground covered (`ArcLook.walkCycle` blocks per cycle, measured from the thigh swing of the clip) |
| `run` | 0.62 s | yes | the same, leaning, arms pumping, cloth blown back (`runCycle`) |
| `attack` | 0.9 s | no | the hit lands at 0.34 to 0.42 s (slash 0.38, thrust 0.36, overhead 0.40, smash 0.42, stab 0.34, sweep 0.42, cast_jab 0.40, shoot 0.46, punch 0.36). The clock starts the clip at `ATTACK_LEAD` = 0.2 s, so the wind-up before 0.2 s is not seen; the first and the last pose are the idle's |
| `cast` | 1.2 s | yes | what the character does while a spell winds up; for an NPC it is the **talking** clip (a calm gesture of one hand, a nod): `ArcNpcRenderer` plays it when a player stands within 3.2 blocks |
| `cast_big` | 2.4 s | no | the ultimate: it winds up to 0.8 x its length (1.92 s), the slam is at that moment, then it settles to the idle pose; the clock paces it by the cast progress and plays the rest after the release |
| `hurt` | 0.45 s | no | a flinch; ends on the idle pose |
| `transform` | 3.0 s | no | crouch, burst, rise, hold at the peak (1.75 s, `recipes.TRANSFORM_PEAK`), settle. With a `form2`, the first model plays 0 to 1.75 s and the second model 1.75 to 3.0 s, each over half of the transformation, so the swap happens at the peak pose, hidden by the flash |
| `death` | 2.8 s | no | the fall; it ends lying on the floor; the renderer fades the body |
| `spawn` | 2.0 s | no | optional (`clips.extra`): rises from a crouch; the boss renderer plays it once when the body first appears |
| `chest_gem_reveal` | 1.8 s | no | optional: the pose for a chest gem that is opened up; the boss renderer plays it while the boss is exposed |

Rules every generated clip obeys: every number is finite; a looping clip ends on its first pose; the lowest foot touches the floor in every grounded clip (the `root` is lowered or
lifted by what the legs leave over; a `hover` character floats 5 px above it in every clip and drops when it dies); `attack`, `hurt` and `cast_big` start and end on the idle's pose.

### 9.2 What the styles do

`clips.attack`: `slash` (an arc across the body), `overhead` (up behind the head and down), `thrust` (a lunge), `smash` (a heavy overhead blow), `stab` (a short jab),
`sweep` (a wide turn of the body), `cast_jab` (the weapon or the hand thrown forward), `shoot` (a bow: the string hand draws to the cheek and lets go), `punch` (bare hands),
`none`. `auto` takes the swing of the main prop, a punch without one. `clips.cast`: `gesture` (talking), `magic` (the weapon and the free hand raised), `book` (a grimoire is
held open, the free hand writes the air), `pray` (hands together, head bowed). `clips.idle`: see the table above.

### 9.3 How they are made (and why)

Nobody keys forty bones. A weapon swing is written as a short table of where the wrist goes and where the weapon points at each moment (elevation above the horizon, azimuth,
reach as a fraction of the arm, and the angle `phi` / lateral `tilt` of the weapon's long axis against the horizon; `recipes.py`). The body, legs and the other arm support it.
Everything is sampled 24 times a second, post-processed, thinned and written.

* **The arm** (`anim.ArmIK`) is solved in closed form: the elbow angle follows from the distance to the wanted wrist point, and the elbow sits somewhere on a circle around
  the line shoulder-wrist (the swivel). Every sample gets a table of swivel candidates (the natural elbow: behind the arm, a little low and out; clear of the torso; inside the
  joint limits) and a dynamic programme picks one candidate per sample, so the elbow follows the natural position and never flips from one side to the other. A target the arm
  cannot reach is brought to the nearest point it can; a target inside the torso is pushed out through the nearer face: the hand misses, the body stays whole. (An earlier
  iterative solver started from the previous sample drifted into other elbows and twisted wrists and never came back; a clip that ends on its first pose must really end there.)
* **The wrist** is solved analytically too: the hand turns the prop's long axis along the wanted direction and, where the prop has an `orient` rule, its edge up (or the string of
  a bow to the archer), with the Euler angles chosen so they stay continuous.
* **Both hands on a shaft** (section 8): the off hand is solved onto the point `grip2` of the prop. In the clips `idle walk run attack cast hurt` the shaft is pulled toward
  where both arms can reach it (fading out away from the rest hold, so every clip starts and ends on the idle's pose); in `cast_big`, `transform`, `spawn` and
  `chest_gem_reveal` the off hand moves freely and starts and ends on the shaft; in an `attack` the off hand lets go when the swing takes the shaft out of its reach and
  closes on it again as it returns.
* **Follow-through**: hair, capes, ribbons, tassels, tails, ears, wings and the halo hang from the body and lag behind its motion (a low-pass of the pitch, yaw and bob), are
  blown back by a run, and wobble; a floating prop (`float`) orbits; bones named in `rig.animate(...)` do the same.
* **Feet on the ground**: the root is moved so that the lowest foot touches the floor in every sample. **Clearance**: an arm hanging by forward kinematics is swung out until it
  is clear of the torso; a cape is tilted back until it is clear of the legs.

### 9.4 What the kit checks about its own clips

`tools/arc_kit/clipcheck.py` reads the written animation the way the game plays it (linear interpolation between keys) and checks, on every character the driver builds and on
the archetypes of the selftest: an arm never sinks more than **1.6 px** into the torso (a tenth of a block); the shoulder never turns more than **100 degrees** between two
samples of the 24 Hz grid (the elbow does not flip); a one-shot's arm ends within **12 degrees** of where it starts (the swivel steps are 8 degrees) and starts within 40 degrees of
the idle pose. The driver prints `PROBLEM <id> arm rule: ...` and fails if a finished model still breaks one (a two-handed prop falls back to one hand first). The selftest
adds: every number finite, loops close, the lowest foot within 0.8 px of the floor in every grounded clip, a one-shot ends where the idle starts.

## 10. Preview, gallery, selftest, check

```
python3 -B tools/gen_arc_characters.py                       # build every character (writes the assets, the scenes, the manifest)
python3 -B tools/gen_arc_characters.py heath_grice           # only the named ones (any number of ids)
python3 -B tools/gen_arc_characters.py --list                # id, role, build, name of every character
python3 -B tools/gen_arc_characters.py --validate [id ...]   # only read and check the specs (no files written)
python3 -B tools/gen_arc_characters.py --preview <id> [front back face idle walk run attack cast cast_big hurt transform death ...]
python3 -B tools/gen_arc_characters.py --check               # rebuild into a temporary folder; fail if a committed file differs (JSON by bytes, PNG by pixels) or a model has a problem
python3 -B tools/gen_arc_characters.py --gallery hair        # a labelled contact sheet of one vocabulary: hair eyes tops legs builds props accessories extras all   (--out DIR)
python3 -B tools/gen_arc_characters.py --selftest            # builds EVERY name of the vocabulary and checks the clips of eight archetypes (about 3 minutes; run it after touching the kit)
```

The build is deterministic (no randomness, no clock): the same spec gives the same bytes. `ARC_FAST=1` (or a comma list of clip names) restricts the clips for a quick look at a
model; nothing built that way may be committed (the driver warns, `--check` refuses).

**Scene names** (rendered by `python3 -B tools/render_preview/preview.py <name> ...`, written to `$RENDER_PREVIEW_BUILD`, default `build/render_preview`; the harness needs JDK
21 and python with numpy and Pillow): `arc_<id>_front` (front and front 3/4), `arc_<id>_back` (back and side), `arc_<id>_face` (the head, two views), and `arc_<id>_<clip>`
for every clip (a strip of frames: four for a loop, five for a one-shot, `attack` `cast_big` `transform` with a side view as well). A second form has the same scenes under
`arc_<id>_form2_*`. The reference characters' PNGs are in `docs/arc/preview/`.

## 11. The manifest, the numbers of `ArcLook`, the renderers

`tools/arc/manifest.json` has one entry per model (`arc_<id>` and `arc_<id>_form2`): `model`, `spec` (`GeoSpec.of("arc", "<id>")`), `form`, `role`, `name`, `build`, `files`,
`texture` (width, height), `cubes`, `faces_per_pass`, `height_blocks`, `height_with_hair_and_horns_blocks`, `bounds_blocks`, `recommended_hitbox_blocks` (`width`, `height`),
`arc_look`, `grip`, `notes`, `clips` (length and loop of each), `bones`, `glow_bones`, `problems` (empty).

`arc_look` is the argument list of `new ArcLook(model, height, walkCycle, runCycle, shadow, growXz, growUp, glow)` without the model: the character builder who writes the
`ArcCharacters` line copies `height`, `walkCycle`, `runCycle`, `shadow`, `growXz`, `growUp` and `glow` from there, and uses `recommended_hitbox_blocks` for the entity's size. The
renderer scales a boss by hitbox height over `look.height`, so a hitbox that is exactly the model's height is drawn at 1.

**The renderers** (`client/arc/`, nothing else of the game is touched):

* `ArcBossRenderer(EntityRendererProvider.Context, ArcLook)` extends `ZogratisRenderer<ArcBoss>`: model `GeoSpec.of("arc", look.model())` (a cleaned snake_case id; a bad id draws
  nothing), the pose clock `ZogratisPose` for every clip above, glow from the power curve (`ZogratisGlow`), a flash in the style colour over a transformation, the optional `spawn`
  and `chest_gem_reveal` clips, the second model from the middle of the transformation, a clean fade of the death. A missing model is logged once by the geo loader and nothing is
  drawn: never an exception.
* `ArcNpcRenderer(EntityRendererProvider.Context)`: reads `ArcNpcEntity.characterId()`, draws `arc_<id>`, falls back to `arc_npc_default` (a plain villager) when the model is missing,
  plays `idle` / `walk` (paced by the model's own stride) / the `cast` clip as the talking clip / `hurt` / `death`, and shows the name tag when the player looks at it or stands
  near. `ArcNpcRenderer.talkingRule` (a static, replaceable predicate) decides when an NPC talks; the default is "a player within 3.2 blocks and the NPC standing still".
* Pure, unit-tested pieces: `ArcModelKey` (model key resolution), `ArcRenderMath` (scale, stride, spawn time), `ArcNpcPose` (the clip an NPC plays).

**Lead integration line** (one line, next to the Zogratis registration, in `NUSMP.java` where `ZogratisClient::register` is added to the mod bus, client side only):
`modEventBus.addListener(com.newuniverse.nusmp.client.arc.ArcRenderers::register);`. It registers `ArcNpcRenderer` for `ArcEntities.NPC` and an `ArcBossRenderer` with the
entry's own `ArcLook` for every boss registered through `ArcEntities.boss(...)` (each guarded: a failing one is logged and skipped). No config, no command, no lang key.

## 12. Writing an `extra(kit, rig)`

`rig` is a `Rig`. `rig.box(bone, origin, size, paint, **kw)` adds a box to a bone (`origin` is its lowest corner in model pixels, `size` its extent; `kw`: `inflate`, `rotation`
`(x, y, z)` with `pivot`, `mirror`, `faces` (which faces are painted), `dens` (texels per pixel of a face: `{"front": 4}`; default 2), `key` (boxes with the same key share one uv
rectangle)). `rig.pair(bone_l, bone_r, origin, size, paint)` adds a box on `bone_l` and its mirrored twin (x -> -x) on `bone_r`; `rig.pair2(..., paint_l, paint_r)` gives the twin its own
painter (a scar on one arm). `rig.spike(bone, base, width, length, rx, rz, paint, fracs=(0.55, 0.45), taper=0.55, bend=(0, 0))` builds a tapering spike of stacked boxes (horns, thorns,
icicles); `rig.clump(bone, pivot, theta, alpha, width, length, paint)` a flat plate of hair, fur or feathers. `rig.bone(name, parent, pivot)` adds a bone; `rig.animate(bone, sway=1.0,
spin=0.0, spin_axis=1, pulse=0.0, phase=0.0, wind=1.0)` gives it motion in every clip (hangs like cloth, spins that many turns per second, breathes its scale); `rig.pose_point(bone, p, pose)`
is forward kinematics (the maths of the game's `GeoDraw`) for placing things against a posed model; `rig.pk.twin(rig.m, bone, cube)` mirrors a cube you already made.

A **painter** is `paint(face, i, j, fw, fh) -> (rgba or None, glow_rgba or None)`: `face` is `top bottom front back left right` (`left` is +x), `(i, j)` the texel, `(fw, fh)` the face
size in texels; colours are `(r, g, b, a)` 0 to 255, `None` = transparent. `kit.px(c, a=255)` makes a colour. The ones the kit exports: materials `fabric`, `leather`, `plate`, `chain_paint`,
`fur`, `bone_paint`, `bandage_paint`, `scale_paint`, `metal`, `solid`, `feather`, `clump`, `blade_paint`; head `hair_paint`, `skin_paint`; composition `over(base, top, faces)`, `band(top, base, j0, j1)`,
`band_px(...)`, `only(painter, faces)`, `canvas_paint(canvas, face, fallback)`, `canvas_plate(canvas)`, `decal_paint({face: canvas}, fallback)`; `kit.Canvas(w, h)` is a small pixel picture with
`rect ellipse line poly put at image`; `kit.emblem(kind, w, h, color, glow)` draws a symbol; `kit.mat(kind, color, salt)` is a material by name. Colours: `kit.rgb`, `kit.shades(base, n, contrast)` (n tones
dark to light: shadows go blue-violet, lights warm), `kit.mix`, `kit.lighten`, `kit.darken`, `kit.skin_palette`, `kit.cloth_palette`.

`kit.build_character(spec, extra=None, kit=None, file_id=None)` returns a finished `Rig` without writing anything (`kit.save(rig)` writes it): use it in a script to try a spec.

## 13. Why the kit is built the way it is

* **One spec file per character, a vocabulary of names** (`hair.style: "spiky"`): a builder who knows the series but not the pixel art can describe a character in an hour; a typo is
  a loud error listing the valid names; nothing is a free-form number except colours and sizes.
* **One universal skeleton**: every clip fits every model, so the renderers and the pose clock need no per-character data, and a boss can swap models in a transformation.
* **Clips generated from the body and the weapon**: forty characters with hand-keyed clips would not move alike and would clip. A swing is a table of wrist targets and a weapon
  direction; the arms are solved; the checks are run on the result.
* **Deterministic output and `--check`**: generated assets are committed, so the build must be reproducible or every rebuild is a diff nobody can review.
* **Copied from the Lucius generator** (read, not edited): the texture packer (per-face texel density, a key-shared uv rectangle for repeated plates, mirrored twins), the 32 x 32 face
  canvas, the hard-edged palettes with violet shadows, the hair clumps; the animation file format of `geo_builder`.
* **Glow in a second texture**: the same convention as every model of the mod (`arc_<id>_glow.png`, drawn full bright).
* **The renderers are generic** (`ArcBossRenderer`, `ArcNpcRenderer`): thirty characters must not mean thirty renderer classes; they reuse `ZogratisRenderer`, `ZogratisPose`, `ZogratisGlow`
  and `GeoDraw` so the behaviour of the Zogratis bosses (clock, glow curve, fade, culling) is the same.

## 14. Sources

Canon looks of the two reference characters (facts only, no text copied; the pages were not all readable, see section 15):

* Heath Grice: https://blackclover.fandom.com/wiki/Heath_Graice (spelled Graice there), https://anibase.net/en/character/lyBW8/Heath-Grice,
  https://myanimelist.net/character/156608, https://www.sportskeeda.com/anime/all-eye-midnight-sun-members-black-clover-ranked-based-strength. Used: a slim man with slanted eyes, short grey
  hair combed back, a scar from the right forehead across the face to the left cheek, a military-style black jumpsuit, two belts round the neck, a pocket watch he keeps checking, Ice
  magic; when he uses purified devil power: wings, a halo, light clothes and light lines on the face (the `form2`).
* Yami Sukehiro: https://blackclover.fandom.com/wiki/Yami_Sukehiro (as mirrored on https://myanimelist.net/character/127764), https://en.wikipedia.org/wiki/Yami_Sukehiro. Used: a tall muscular man
  (183 cm in the listing), a moustache and stubble, a black tattered cape with the Black Bull emblem and a strap round the neck over a white tank top, a belt of books, black trousers under
  light chaps, black boots, a cigarette, a katana. The mod's own Black Bull cloak (`docs/robe_anime.md`) is the look of the cloak.
* The design of the kit follows `tools/gen_zogratis_lucius.py`, `tools/geo_builder.py`, `tools/spirit_geo.py` (the packer idea), `client/geo/*` and `client/zogratis/*` of this repository.

## 15. Unverified, and what the kit cannot do yet

**Nothing here has run in the game.** What was run: the python kit (builds, the selftest, `--check`), the preview harness (a software rasteriser of the real `GeoDraw`; it is not the game:
the light and the colours in the game differ), the type check of the Java against the API stubs, and the pure JUnit tests through the local shim.

Unverified assumptions:

* The clip contract against the live `ZogratisPose`: that `cast` is a loop that a talking NPC may play for many seconds, that `attack` is entered 0.2 s into the clip (as the source says), that the
  transformation clock is split in halves at the model swap. Read from the source, not observed.
* `ArcNpcRenderer.talkingRule` (a player within 3.2 blocks) is an assumption; the core builder may replace it. Whether a name tag should always show or only near the player is a taste.
* The look of the characters: Heath's eye colours, the frost on his shoulders and the orbiting ice crystal are additions of the game, not canon; Yami's wrapped forearms are an addition (the brief asks
  for bandages or a scarf; no source confirms either), and the source text for his hair says brown while the brief and the usual depiction say black (the brief was followed). The pocket watch is
  from the wiki summary, the colour of its glow is a choice. Heath's source pages could not all be opened; the facts come from the search summaries.
* Face detail is painted on a 32 x 32 canvas; the face is **static** (no blink, no mouth movement).
* The preview shows the model in the harness light; very dark outfits (black on black) read darker there than they may in a lit hall.

What the kit cannot do yet:

* Only the **humanoid** body (seven builds). No quadrupeds, slimes, snakes, dragons as the whole body, no centaurs; a creature's parts (wings, tail, horns, ears, halo) can be added to a humanoid.
* No hand-keyed clips beyond `post(kit, rig)`; no per-character timing; no extra clips beyond `spawn` and `chest_gem_reveal` (the renderers know only those names).
* Fingers: hands are one block; a weapon is held in a fist, not gripped finger by finger.
* Two hands on a shaft are exact at rest; in a violent swing the off hand lets go and the kit keeps the arms outside the torso rather than the hand on the weapon. Broad and huge builds hold spears and
  tridents in one hand. The arm rules allow 1.6 px of arm inside the torso.
* Cloth is a hard box with follow-through, not a simulation; a long cape can show a corner through a leg in a hard turn (the kit tilts it back to clear the legs, not the arms).
* A model with more than about 250 cubes is slow to draw for thirty bosses at once; a character with many extra parts should keep an eye on `faces_per_pass` in the manifest (the two reference
  models draw 475 and 508 faces).
