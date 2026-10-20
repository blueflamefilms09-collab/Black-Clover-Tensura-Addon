"""Writes docs/tiny_djinn.md: the prose below plus the table of all Djinn read from specs.json and the cube counts read from the generated geo files, so the page
cannot drift from the data. Run: python3 -B tools/tiny_djinn/make_doc.py (after tools/gen_tiny_djinn.py)."""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, ".."))
import tdparts  # noqa: E402
ROOT = os.path.join(HERE, "..", "..")
GEO = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "geo", "entity")

HEAD = {
    "bolt_crown": "three forked bolts for a crown", "flame_crown": "a flame crown", "fin_tiara": "a fin tiara", "wind_plume": "a swept wind plume and temple wings",
    "antlers_leaves": "antlers hung with oak leaves", "great_helm": "a great helm", "bell_crown": "a crown of bells", "wolf_cowl": "a wolf cowl", "hood": "a hood",
    "storm_visor": "a storm visor and forked antlers", "crescent_circlet": "a crescent circlet", "bone_mask": "a bone mask", "ice_crown": "a crown of ice",
    "scholar_cap": "a scholar's cap with faces at the temples", "skull_mask": "a skull mask with drumsticks", "beak_hood": "a crow's beak hood and a ruff of feathers",
    "carapace_cap": "a carapace cap with a star, eyes on stalks, pincers", "lion_brow": "a brow plate with two lion's eyes", "third_eye_hood": "a hood with a third eye and a serpent",
    "coral_crown": "a coral crown and temple shells", "dove_wings": "a dove's wings above the brow", "hound_skull": "a hound's skull, a spiked collar, swept horns",
    "goggles": "goggles and a nose ring", "sallet": "an open sallet with a frozen plume and a long beard", "three_faces": "three small faces on the brow, lyre horns, a flame crown",
    "sun_crown": "a crown of sunrays", "horse_plate": "a steed's face plate, horse ears, a flaming mane", "hollow_crown": "a crown of three floating hoops, hooped horns",
    "pane_crown": "standing glass panes and a mirror on the brow", "aigrette": "an aigrette of three stalks", "band": "a brow band", "horns_crescent": "crescent horns",
    "horns_ram": "ram horns", "horns_bull": "bull horns with bronze caps", "horns_stag": "antlers", "horns_spike": "spike horns", "horns_swept": "swept-back horns",
    "horns_lyre": "lyre horns", "horns_spiral": "a spiral unicorn horn", "horns_ice": "crystal horns", "ram_horns_dark": "black ram horns", "bolt_temples": "a bolt at each temple",
    "croc_snout": "a crocodile snout across the brow, eyes on the crown", "wheel_crown": "a small wheel on the crown and herb sprigs", "spire_crown": "a crown of spires with an orb",
}
WEAPON = {
    "lance": "thunder lance", "pennon_lance": "cavalry lance with a pennon", "star_lance": "star lance with eyed plumes", "greatsword": "greatsword", "firesword": "flaming greatsword",
    "claymore": "claymore", "rapier": "rapier", "mirror_rapier": "rapier with a mirror guard", "horn_rapier": "rapier with a spiral horn blade", "sabre": "swift sabre",
    "trident": "trident", "flame_trident": "flaming trident with faces", "glaive": "glaive", "storm_glaive": "storm glaive", "moon_glaive": "moon glaive", "bloom_staff": "blooming staff",
    "wheel_staff": "staff with a wheel and a serpent", "tome_staff": "staff with a chained tome", "crystal_staff": "crystal staff", "quill_staff": "quill staff with a crow",
    "orb_staff": "orb staff", "axe": "war axe", "horn": "war horn", "claws": "claws", "bow": "bow", "antler_bow": "recurve bow with antler tips", "scythe": "scythe",
    "harpoon": "harpoon", "daggers": "crossed daggers", "anchor": "anchor", "fans": "war fans", "halberd": "halberd", "spear_eye": "spear with an eye", "cleaver": "bronze cleaver",
    "pincers": "crab pincers", "drum_mace": "drum mace", "hollow_sword": "hollow outline greatsword", "lion_sword": "lion-guard longsword", "crown_claymore": "crown claymore",
}
ACC = {
    "none": "none", "sun_disc": "the sun of a mane (a disc of rays) behind", "throne": "the high back of a throne behind", "peacock": "a peacock fan of eyed plumes behind", "wheel": "a great wheel behind",
    "moon": "a crescent moon behind", "horseshoe": "a horseshoe with glowing nails behind", "maw": "a ring of fangs behind", "jaws": "open jaws behind", "tome": "floating open books",
    "drum": "a bass drum with a skull behind, small drums orbiting", "crows": "three crows circling", "flask": "an alembic flask in the free hand and behind", "mirror": "a floating tide mirror",
    "masks": "three masks orbiting", "shield": "a kite shield and crossed lances", "crab_legs": "crab legs and a pentagram ring behind", "keys": "three stolen keys orbiting",
    "quiver": "a quiver of arrows", "snowflake": "a snowflake behind", "pinwheel": "a pinwheel of wind blades behind", "eye_ring": "a ring of watching eyes behind", "doves": "three doves orbiting",
    "star": "stars orbiting", "forks": "a tuning fork at each side",
}
for k, v in {"sparks": "sparks", "embers": "embers", "bubbles": "bubbles", "rings": "rings of wind", "petals": "petals", "boulders": "boulders", "suns": "small suns", "skulls": "skulls",
             "runes": "runes", "leaves": "leaves", "notes": "notes", "shards": "shards", "feathers": "feathers", "orbs": "orbs"}.items():
    ACC["orbit_" + k] = v + " in orbit"
TAIL = {"smoke": "smoke", "flame": "flame", "water": "water with a fluke", "wind": "wind rings", "bolt": "lightning", "ice": "ice", "blade": "a blade", "vine": "vine and leaves",
        "tendril": "a curling tendril", "feather": "feathers"}


def cubes(i):
    with open(os.path.join(GEO, "tinydjinn_%s.geo.json" % i)) as f:
        g = json.load(f)["minecraft:geometry"][0]
    return sum(len(b["cubes"]) for b in g["bones"])


def main():
    with open(os.path.join(HERE, "specs.json")) as f:
        specs = json.load(f)["djinn"]
    counts = [cubes(s["id"]) for s in specs]
    rows = []
    for s, n in zip(specs, counts):
        head = ", ".join(HEAD[h] for h in s["head"])
        hair = "" if s["hair"] in ("none",) else s["hair"].replace("_", " ") + " hair; "
        rows.append("| %s | #%s / #%s | %s%s | %s | %s | %d |" % (s["name"], s["color"], s["glow"], hair, head, WEAPON[s["weapon"]], TAIL[s["tail"]] + "; " + ACC[s["acc"]], n))
    recipes = "%d headgear, %d hairs, %d ears, %d capes, %d weapons, 10 tail styles, %d accessories" % (len(tdparts.HEAD), len(tdparts.HAIR), len(tdparts.EARS), len(tdparts.CAPE), len(tdparts.WEAPON), len(tdparts.ACC))
    doc = TEMPLATE.replace("@RECIPES@", recipes).replace("@TABLE@", "\n".join(rows)).replace("@MIN@", str(min(counts))).replace("@MAX@", str(max(counts))).replace("@AVG@", str(round(sum(counts) / len(counts))))
    path = os.path.join(ROOT, "docs", "tiny_djinn.md")
    with open(path, "w") as f:
        f.write(doc)
    print("wrote", path)


TEMPLATE = """# The Tiny Djinn: the Lesser Summon as a real little Djinn (1.105.0)

Owner request: *make the lesser summon for djinns an actual tiny djinn model.* Until now a Lesser Summon was the panel plus a translucent flat bust above the right
shoulder (`DjinnArmorLayer.bust`) and a bolt every two seconds. It is now a **3D chibi figure of the bearer's own Djinn**, 0.45 block tall, hovering at the right
shoulder, with a wisp for a tail where its legs would be, its own crown, hair, horns, ears, cloak, weapon, tail and accessory, a blink, a cast pose, a dash at the foe, a
happy hop after a kill, a poof when it arrives and a spiral into the vessel when it leaves. It is **not an entity**: it is drawn on the client from the Djinn state the
engine already syncs, for everyone in range.

## NEW / REPLACED / KEPT (owner rule 5)

| Part | Status | What |
|---|---|---|
| 36 tiny Djinn models `geo/entity/tinydjinn_<id>.geo.json`, their light and Dark Djinn textures and glow maps (`tinydjinn_<id>.png`, `_glow`, `_dark`, `_dark_glow`) | NEW | one per Djinn of the enum (12 + 24 = 36), 25 shared bones, @MIN@ to @MAX@ cubes (@AVG@ on average), 64 x 64 |
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
shared building blocks (@RECIPES@) combined per Djinn by the spec table.

## The 36 tiny Djinn

Colours are the enum's body and glow colour. "Cubes" is the count of the generated model.

| Djinn | Body / glow | Hair and headgear | Weapon in hand | Tail; accessory | Cubes |
|---|---|---|---|---|---|
@TABLE@

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
"""

if __name__ == "__main__":
    main()
