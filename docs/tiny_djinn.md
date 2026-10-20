# The Tiny Djinn: the Lesser Summon as a real little Djinn (1.105.0)

Owner request: *make the lesser summon for djinns an actual tiny djinn model.* Until now a Lesser Summon was the panel plus a translucent flat bust above the right
shoulder (`DjinnArmorLayer.bust`) and a bolt every two seconds. It is now a **3D chibi figure of the bearer's own Djinn**, 0.45 block tall, hovering at the right
shoulder, with a wisp for a tail where its legs would be, its own crown, hair, horns, ears, cloak, weapon, tail and accessory, a blink, a cast pose, a dash at the foe, a
happy hop after a kill, a poof when it arrives and a spiral into the vessel when it leaves. It is **not an entity**: it is drawn on the client from the Djinn state the
engine already syncs, for everyone in range.

## NEW / REPLACED / KEPT (owner rule 5)

| Part | Status | What |
|---|---|---|
| 36 tiny Djinn models `geo/entity/tinydjinn_<id>.geo.json`, their light and Dark Djinn textures and glow maps (`tinydjinn_<id>.png`, `_glow`, `_dark`, `_dark_glow`) | NEW | one per Djinn of the enum (12 + 24 = 36), 25 shared bones, 61 to 130 cubes (87 on average), 64 x 64 |
| One shared animation file `animations/entity/tinydjinn_anim.animation.json` | NEW | idle, cast, attack, hop, poof, exit |
| `tools/gen_tiny_djinn.py` and `tools/tiny_djinn/` (`specs.json`, `tdlib`, `tdparts`, `tdpaint`, `tdanim`, `preview`, `make_specs`, `make_doc`) | NEW | the deterministic generator, the hand-curated spec table, a software preview that repeats `GeoDraw`'s maths |
| `client/relics/djinn/tiny/` (`TinyDjinnLayer`, `TinyDjinnActor`, `TinyDjinnMath`, `TinyDjinnAssets`, `TinyDjinnConfig`, `TinyDjinnFx`, `TinyDjinnPortrait`, `TinyDjinnClient`) | NEW | the renderer, the state machine and spring, the pure numbers, the config, the particles, the panel head |
| `DjinnNet.Cue` packet, `relics/djinn/TinyDjinnCue`, `TinyDjinnSelfTest` (`djinn.tiny`) | NEW | the server's "I am about to strike this foe" cue, additive; the self-test |
| Config `nusmp-djinn-tiny-client.toml` (`[djinn] tinyModel, tinyMax, tinyNameTag, tinyTimerBar, tinyPortrait`) | NEW | client options |
| Tests `TinyDjinnMathTest`, `TinyDjinnActorTest`, `TinyDjinnAssetsTest`, a branch in `GeoAssetsTest` | NEW | see Tests |
| The translucent bust (`DjinnArmorLayer.bust`, `DjinnMeshes.bust`, the `*_bust.png` textures) | KEPT | still in the repo and drawn again when `djinn.tinyModel = false` or a Djinn has no tiny model |
| The panel's silhouette emblem (`GuiMedallion.emblem`) | KEPT | the head portrait is drawn over it only while a Lesser Summon runs and `tinyPortrait` is on; any failure leaves the emblem |
| The Lesser Summon's bolt (`DjinnMagic.lesserBolt`: the nearest mob within 12 blocks, 2 damage, every 40 ticks), its price, its 20 seconds, its cooldown | KEPT | not one number of the gameplay changed |
| The Sync packet and the Djinn state | KEPT | no field added to them; the cue is a separate small packet |
| Anything REPLACED | one visual | the translucent bust is no longer drawn by default (see above); nothing was deleted |

Roster note: the brief said "~60 Djinn". The enum holds **36** (the first twelve and the 24 of 1.93.0). Every one has a tiny model, and each has a light and a Dark Djinn
texture, so 72 skins exist; no Djinn is missing.

## What the player sees

* **Entrance (poof, 0.5 s).** When a Lesser Summon starts (phase 1 of the synced state) a flash, a ring of smoke and a burst of the Djinn's light appear at the shoulder, the
  figure pops out of nothing (scale 0.05, overshoot to 1.16, settle) with a half turn, and a soft chime plays.
* **Idle (6 s loop).** It floats and bobs (4 cm, each figure out of step), the wisp tail and the cape sway, the arms hang out a little (the weapon leans clear of the head), the
  ears twitch, the lids blink every six seconds, the orbiting accessory (crows, books, orbs) sways. It **faces the camera** (so everyone sees its face; in first person that
  is the owner). Its name floats over it within 16 blocks with a thin bar of the seconds left.
* **Cast (0.6 s).** Every two seconds the Lesser Summon has a foe, the figure turns to it, winds up and points its weapon; at 0.25 s a burst of sparks and a ring of its light
  stand at the weapon hand.
* **Attack (0.6 s).** It dashes to a point 0.65 block short of the foe in 4 ticks, hangs there for 2 (sparks and crits where it lands: this is when the bolt lands) and
  returns to the shoulder in 6.
* **Hop (0.8 s).** If the foe is dead or dying 3 ticks after the dash, it jumps for joy: a squash, a leap with both arms up, a spin of the head, a squinted blink, glitter.
* **Exit (0.6 s).** Six ticks before the summon ends (or at once when the phase changes: Half Equip, Full Equip, the vessel put away) it turns twice, shrinks and flies into the
  owner's chest in a spiral of its light; a last puff and a chime.
* **First person.** The owner's own figure sits low and out to the right in front of the view (0.95 block ahead, 0.62 to the right, at 0.67 of the body height), drawn at 78 %
  size, with a stiffer spring so it keeps up when the view turns, and without a name.
* **Dark Djinn.** The same model with the Dark Djinn's palette (black metal, gold trim, ash skin, the Djinn's own colour in the eyes and the glow), and a thorn crown and an
  eye crest on the brow that exist in every model but are painted only in the dark texture (the light texture leaves those strips clear, which the shader discards).
* **Djinn panel.** During a Lesser Summon the circle of the panel shows the figure's head (hair, crown, horns, ears; it turns gently and blinks) on a dark disc with a rim in the
  Djinn's colour, instead of the silhouette.

## How it works

```
server (DjinnEngine tick, every 4 ticks)                 client
  t % 40 == 24  TinyDjinnCue.lesserCue: the foe the        TinyDjinnClient.cue(owner, foe) -> TinyDjinnLayer.cue -> actor.cue (CAST, 12 ticks)
                bolt would pick -> DjinnNet.Cue ---------> CAST 12 ticks, ATTACK: dash out 4, hold 2, back 6 (the dash arrives at tick 16)
  t % 40 == 0   DjinnMagic.lesserBolt (unchanged)  ......  the bolt lands as the figure arrives
  Sync (existing, every second): phase 1, which Djinn, ticks left, the Dark flag ---> TinyDjinnLayer.onTick: spawn / exit / replace a figure per player in the level
```

* **Per tick** (`TinyDjinnLayer.onTick`): for every player in the client level, read `DjinnClientState.of(id)`; phase 1 with a Djinn and more than 6 ticks left means a figure
  (spawn it if there is none, replace it if the Djinn or the Dark flag changed); anything else starts its exit. The kill check (`killCheckDue`) and the removal of finished
  figures happen here.
* **Per frame** (`RenderLevelStageEvent.AFTER_PARTICLES`, the stage the floating grimoire uses): with no figure out the hook returns at its first line. Otherwise each figure
  is advanced (anchor from the owner's interpolated position and yaw, the foe's interpolated position, the state machine, a spring-damper), its events become particles and
  sounds, the nearest `tinyMax` are kept (`TinyDjinnMath.cap`), and each is drawn by `GeoDraw.paint` (translucent body pass, then the additive glow pass).
* **No allocation per frame**: the `PoseStack`, the `Quaternionf`, the arrays, the mutable `BlockPos`, the `GeoSpec`s and the holders' arrays are reused or made once.
* **LOD**: not drawn beyond 48 blocks, no additive glow beyond 24, no name beyond 16; at most `tinyMax` (default 16, 0 to 64) figures per frame.
* **State machine** (`TinyDjinnActor`, pure): GONE, ENTER (10 ticks), IDLE, CAST (12), ATTACK (12), HOP (16), EXIT (12). Events (`POOF_IN`, `CAST`, `STRIKE`, `HOP`,
  `SPIRAL`, `POOF_OUT`) are read once by the renderer.
* **Spring** (`TinyDjinnMath.spring`): implicit Euler in steps of at most 1/90 s, omega 9, zeta 0.78 floating, omega 30 in the dash; a gap over 0.5 s snaps (a figure that was
  not drawn does not fly across the world).

### The model (tools/gen_tiny_djinn.py)

One skeleton for all 36, so one animation file moves them all: `root > body > head (lids, hair, crown, brow, ear_l, ear_r, horn_l, horn_r, dark), arm_l > hand_l, arm_r >
hand_r > weapon, cape, scarf, back, acc, tail1 > tail2 > tail3 > tail4`. The figure is 33 px tall in model units (the tail tip to the crown's base), drawn 0.45 block tall. The
**texture is 64 x 64**: the top 56 rows hold the painted boxes (head with its face, torso, the four tail segments, an arm, a hand, a cape: shaded, noisy, edge-darkened, with
eyes, brows, mouth, blush, marks and beard), the bottom 8 rows are **32 swatch strips** (2 x 8 px light-to-dark gradients of one material: skin, hair, cloth, trim, metal, fire,
water, ice, leaf, bone, gold, gem, bolt, ...). Every small ornament (a spike, a feather, a horn tooth, a rune) is a cube whose six faces map onto one strip, which is how a model of
100 cubes fits a 64 x 64 sheet. The glow map holds the eyes, the emblem, the gems, the runes, the flame cores and the lit strips. The recipes (`tdparts.py`) are
shared building blocks (46 headgear, 12 hairs, 11 ears, 10 capes, 39 weapons, 10 tail styles, 39 accessories) combined per Djinn by the spec table.

## The 36 tiny Djinn

Colours are the enum's body and glow colour. "Cubes" is the count of the generated model.

| Djinn | Body / glow | Hair and headgear | Weapon in hand | Tail; accessory | Cubes |
|---|---|---|---|---|---|
| Baal | #3C8CFF / #BFE4FF | spiky hair; three forked bolts for a crown | thunder lance | lightning; sparks in orbit | 71 |
| Amon | #FF5A14 / #FFD27A | flame hair; a flame crown, ram horns | flaming greatsword | flame; embers in orbit | 80 |
| Vinea | #2D9CDB / #B5F0FF | long hair; a fin tiara | trident | water with a fluke; bubbles in orbit | 64 |
| Focalor | #62D6A0 / #E6FFF1 | short hair; a swept wind plume and temple wings | glaive | wind rings; rings of wind in orbit | 84 |
| Zagan | #58C75A / #E4FFB8 | moss hair; antlers hung with oak leaves | blooming staff | vine and leaves; petals in orbit | 87 |
| Zepar | #D43A3A / #FFB9A0 | a great helm, black ram horns | war axe | smoke; boulders in orbit | 61 |
| Furfur | #B77BFF / #F1DDFF | short hair; a crown of bells | war horn | wind rings; rings of wind in orbit | 90 |
| Marchosias | #E23A22 / #FFA878 | a wolf cowl | claws | flame; embers in orbit | 62 |
| Sitri | #FFE070 / #FFFFFF | bun hair; a crown of sunrays | bow | smoke; small suns in orbit | 78 |
| Bifron | #7B4DFF / #D2C2FF | a storm visor and forked antlers | storm glaive | lightning; sparks in orbit | 75 |
| Paimon | #4FC3D6 / #DFFAFF | white mane hair; a crescent circlet | moon glaive | wind rings; a crescent moon behind | 94 |
| Vassago | #2F55D8 / #B8CCFF | a bone mask, ram horns | scythe | water with a fluke; skulls in orbit | 89 |
| Bael | #C9A227 / #FFF0A0 | short hair; a crown of spires with an orb | claymore | smoke; the high back of a throne behind | 83 |
| Agares | #1FA37A / #B8FFE0 | a crocodile snout across the brow, eyes on the crown | harpoon | water with a fluke; open jaws behind | 84 |
| Valefor | #5A3DB5 / #E0D2FF | a hood, a bolt at each temple | crossed daggers | lightning; three stolen keys orbiting | 90 |
| Barbatos | #2E7D32 / #CFFFB0 | moss hair; antlers hung with oak leaves | recurve bow with antler tips | vine and leaves; a quiver of arrows | 99 |
| Gusion | #3D7EAA / #D6F2FF | short hair; standing glass panes and a mirror on the brow | rapier with a mirror guard | water with a fluke; a floating tide mirror | 82 |
| Vepar | #00A6B8 / #BFFCFF | long hair; a coral crown and temple shells | anchor | water with a fluke; bubbles in orbit | 89 |
| Shax | #8E8EA8 / #F0F0FF | short hair; a dove's wings above the brow | war fans | feathers; three doves orbiting | 81 |
| Buer | #9CCC3B / #F4FFC0 | braids hair; a small wheel on the crown and herb sprigs | staff with a wheel and a serpent | vine and leaves; a great wheel behind | 120 |
| Andrealphus | #2F6FDB / #A8D4FF | short hair; an aigrette of three stalks | star lance with eyed plumes | feathers; a peacock fan of eyed plumes behind | 106 |
| Kimaris | #3A2A3F / #FF6A8A | short hair; a steed's face plate, horse ears, a flaming mane | cavalry lance with a pennon | flame; a horseshoe with glowing nails behind | 80 |
| Amdusias | #D58AF0 / #FFE3FF | long hair; a spiral unicorn horn | rapier with a spiral horn blade | smoke; a tuning fork at each side | 76 |
| Belial | #7A0F2E / #FF7096 | spiky hair; a crown of three floating hoops, hooped horns | hollow outline greatsword | flame; runes in orbit | 109 |
| Decarabia | #E0B45A / #FFF4C8 | a carapace cap with a star, eyes on stalks, pincers | crab pincers | smoke; crab legs and a pentagram ring behind | 98 |
| Seere | #E8E8C0 / #FFFFFF | short hair; a swept wind plume and temple wings | swift sabre | wind rings; a pinwheel of wind blades behind | 81 |
| Dantalion | #C0A0FF / #F2E8FF | short hair; a scholar's cap with faces at the temples | staff with a chained tome | smoke; floating open books | 67 |
| Andromalius | #2B2B6B / #9CA8FF | a hood with a third eye and a serpent | spear with an eye | a curling tendril; a ring of watching eyes behind | 81 |
| Glasya-Labolas | #A00000 / #FF9090 | short hair; a hound's skull, a spiked collar, swept horns | claws | a curling tendril; a ring of fangs behind | 103 |
| Haagenti | #B87333 / #FFD9A0 | short hair; goggles and a nose ring, bull horns with bronze caps | bronze cleaver | smoke; an alembic flask in the free hand and behind | 103 |
| Crocell | #9ADCF0 / #FFFFFF | short hair; a crown of ice, crystal horns | crystal staff | ice; a snowflake behind | 83 |
| Furcas | #AAB7C8 / #FFFFFF | an open sallet with a frozen plume and a long beard | halberd | ice; a kite shield and crossed lances | 70 |
| Balam | #D1622A / #FFE0A0 | three small faces on the brow, lyre horns, a flame crown | flaming trident with faces | flame; embers in orbit | 94 |
| Alloces | #E8892B / #FFF0B0 | mane hair; a brow plate with two lion's eyes | lion-guard longsword | flame; the sun of a mane (a disc of rays) behind | 108 |
| Camio | #2A3340 / #A0C8FF | a crow's beak hood and a ruff of feathers | quill staff with a crow | feathers; three crows circling | 86 |
| Murmur | #6B3FA0 / #E8C8FF | dreads hair; a skull mask with drumsticks | drum mace | wind rings; a bass drum with a skull behind, small drums orbiting | 130 |

## Previews

`docs/previews/tiny_djinn/`: `contact_front_side_cast_1.png` and `_2.png` (all 36: front, side, cast pose), `contact_dark.png` (all 36 as the Dark Djinn), `alloces_scale.png`
(Alloces at a player's right shoulder to scale, with a close-up), `clips_alloces.png` (idle, cast, attack, hop, poof, exit), `one_<id>.png`. Made by
`python3 -B tools/tiny_djinn/preview.py`, a software renderer that repeats `GeoDraw`'s bone, cube, uv and rotation maths with the model's own textures and the additive glow map:
the shapes, colours and poses are the game's, the lighting is not.

## Tests and checks

* `TinyDjinnMathTest` (15): size (0.45 block), the 48 / 24 / 16 limits, the cap (nearest kept, ties, 0, a crowd), the shoulder anchor for every yaw, sneak and first person, the
  spring (critical, underdamped, huge frames, gaps), the short-way turns, the dash curve, the strike point, the spiral.
* `TinyDjinnActorTest` (14): entrance, idle follow, cue accepted / refused, cast then dash then strike, the kill check once and the hop, no hop without a kill, a kill noticed
  while idle, the exit, an exit in mid attack, vanish, the six clips all reachable, the glow, the tick budget the server relies on (12 + 4 = 16).
* `TinyDjinnAssetsTest` (8): a spec per Djinn with the enum's colours, all looks different, every geo model (25 shared bones, cube count, uv inside the texture, a weapon, the dark
  crest), every texture and glow (light and dark: sizes, lit pixels, the dark-only strips clear in the light texture and opaque in the dark one, the dark palette differs, no
  two Djinn share a texture), the animation file (six clips, real bones, clip lengths equal the actor's tick counts, the idle loop closes, every clip keeps the arm stance and the
  lids), the names and paths.
* `GeoAssetsTest`: a branch for the shared `tinydjinn_anim` file (every bone must exist in every tiny model).
* `python3 -B tools/gen_tiny_djinn.py --check` and `python3 -B tools/geo_assets_check.py --prefix tinydjinn_` (36 models, 0 problems), `tools/geo_check.py`.
* Self-test `djinn.tiny` (the CI server smoke test): the 36 models, textures and glows on the class path with 25 bones and a sane cube count, the six clips, the `Cue` packet's
  codec, the cap, the anchor, and the actor's walk through cast, dash, kill, hop and exit.

## Edits to existing files (all additions, one line unless said)

* `client/relics/djinn/DjinnClient.java`: `TinyDjinnClient.init(modBus)` after `DjinnWeaponClient.init`.
* `relics/djinn/DjinnNet.java`: the `Cue` record (about ten lines) and its `playToClient` registration (two lines).
* `relics/djinn/DjinnEngine.java`: `if (... LESSER && t % 40 == 24) TinyDjinnCue.lesserCue(p);` after the line that calls `lesserBolt`.
* `relics/djinn/DjinnSelfTests.java`: `TinyDjinnSelfTest.register();`.
* `client/relics/djinn/DjinnArmorLayer.java`: the bust is skipped while the figure is shown (`&& !TinyDjinnClient.replacesBust()`).
* `client/relics/djinn/gui/DjinnGui.java`: `TinyDjinnPortrait.draw(...)` after the painter, during phase 1.
* `src/test/.../GeoAssetsTest.java`: the branch for the shared animation file.
* `tools/lang/tiny_djinn.json`: the config screen names.

## Not verified (nothing visual can be run here)

* **Nothing was run in the game.** The Java was type-checked against the API stubs (no error in these files except the known stub artefacts) and the pure logic is covered by
  JUnit in the shim; the renderer, the particles, the sounds and the packet were never executed.
* How the figure **looks in the game's lighting**: the previews use my own simple shading. The translucent body pass relies on `entityTranslucent` discarding the clear texels
  (the dark-only strips, the cut-outs of a ragged cape) and drawing opaque ones solid; overlapping translucent pieces (the wisp tail) are not depth-sorted.
* The **feel** of the spring (omega 9, zeta 0.78; 30 and 0.9 in the dash; 2.4 times stiffer in first person), the 4 cm bob, the first-person spot (0.95 ahead, 0.62 right, 0.67
  of the body height: about 33 degrees out and 25 down from the eye, which may touch the held item on some field-of-view settings) and the 78 % size.
* **Timing against the server**: the cue is sent 16 ticks before the bolt and the dash arrives at tick 16 of the client's own clock; network delay shifts it a few ticks. A cue
  sent for one foe while the bolt picks another (a nearer mob appears) points at the wrong one. A client without the packet (the channel is optional) never casts.
* The **timer bar** is two quads in `RenderType.textBackground` drawn from both sides (I could not confirm which side that render type culls); the **name tag** copies the
  vanilla name tag's setup at a smaller scale.
* The **panel head**: the dark disc, the model through `GuiGraphics.bufferSource()` and a `flush()`, with the GUI's default entity lighting; if the disc or the head misdraw, set
  `tinyPortrait = false` and the old silhouette is back. The head's scale (40 GUI px per block) was chosen so a crown of normal height stays inside the 60 px circle; the tall
  antlers and plumes may poke out of it.
* A Djinn lent to a household member also shows its figure (the sync names it); a player in a vehicle or crawling uses the same anchor (no special pose).
* **Cost** was not profiled. A figure is 60 to 130 cubes (about 400 to 800 vertices per pass), two passes within 24 blocks, one beyond, none beyond 48, 16 at most.
* The **Better Combat**, shader packs and the VFX engine fix in progress were not considered; the figure uses only the vanilla entity render types through `GeoDraw` and
  `AuraRender` (read only).
* Looks are my reading of `docs/djinn_roster_36.md`, `docs/djinn_roster_36_art.md`, the enum's `Look` rows and the tools' palette tables; no reference image of the owner's was
  available for the first twelve beyond those tables.

## Regenerate

```
python3 -B tools/tiny_djinn/make_specs.py          # specs.json from the table in that file (edit the table, run this)
python3 -B tools/gen_tiny_djinn.py                 # geo, textures, glow maps, animation (all 36, about 20 s; deterministic: two runs write byte-identical files) or: ... alloces baal
python3 -B tools/gen_tiny_djinn.py --check
python3 -B tools/tiny_djinn/preview.py             # docs/previews/tiny_djinn/
python3 -B tools/tiny_djinn/make_doc.py            # this page
```
