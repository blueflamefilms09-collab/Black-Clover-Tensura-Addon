# The abyssal bosses: every Decennial boss looks like the creature it is (1.94.0)

Owner's request: "fix the abyssal bosses like the lich to actually look like a lich from D&D and etc". Until now every boss of the D&D layer (`docs/labyrinth/dnd_boss.md`) was the Labyrinth Guardian,
and the Guardian was drawn as the Floor Warden: one suit of dark armour, whether the profile said Acererak, a Beholder or a five-headed dragon. The profiles already named the creature
(`name`, `dnd`, `forms`); the renderer never looked at them.

Status: **written, built, previewed with the repo's preview harness (a software rasteriser of the real `GeoDraw`, not the game), type-checked against the API stubs, the pure tests run through the
local JUnit shim. Nothing here has run in the game.** Section 9 lists what could not be verified.

## 1. What is NEW and what is REPLACED (rule 5)

| part | NEW / REPLACED / KEPT |
|---|---|
| 22 looks (creature models), three phases each, with clips, glow layers and previews | NEW (nothing like them existed; the Guardian had one body) |
| the `look` field of a boss profile; `AbyssalLooks`, the table `assets/nusmp/abyssal/looks.json`; the humanoids as `assets/nusmp/playermodel/looks_abyssal.json` for the player-model kit (skins, extras) | NEW |
| `AbyssalRenderer`, the Guardian's renderer | NEW class; it REPLACES the Floor Warden body **only for a Guardian that has a look**. A Guardian with no look is drawn by the very code it was before (`AbyssalRenderer extends KitMobRenderer` and calls it), so the old look is KEPT for every profile without a `look` |
| the twenty ordinary profiles | KEPT, except `evil_t1a` (the Umbral Catacomb Lich-Djinn), which now says `look: lich` (the owner asked for "a generic lich") |
| the 17 famous profiles | each gains `look` (an addition to its file, the generator `tools/labyrinth/gen_dnd_famous.py` writes it) |
| old models, textures and generators (`floor_warden`, `tools/arc/char_floor_warden.py` ...) | KEPT |

## 2. How the Guardian was drawn, and how it is now

* Before: `RemodelRenderers` registered `KitMobRenderer(ctx, "floor_warden", Size.fit(), KitProbes.ARMOUR)` for `nusmp:labyrinth_guardian` (the art-kit body `arc_floor_warden`).
* Now: the same `case` of `RemodelRenderers` registers `AbyssalRenderer` (`client/remodel/AbyssalRenderer`). It reads three synced values of the entity (`LabyrinthGuardianEntity`: the look id, the
  phase 1 to 3, the game time and kind of the last cast), chooses `abyssal_<look>_p<phase>` from `AbyssalLooks`, plays the clip the pose clock (`ZogratisPose`) picks (idle, walk and run paced by the ground
  covered, attack on the swing, cast on the boss's cast cue, hurt, death), scales it so the model's reference height is the hitbox height (which grows with the Guardian's size attribute) times the look's
  boost, and drives the glow layer by phase (0.75, 0.9, 1.0) and the cast. An undead look crumbles to ash and soul fire while it dies (`dust`).
* No new entity type: the look is data on the one entity that already is every Decennial boss.

## 3. The data

* A profile (`data/nusmp/mega/dnd_bosses/**`) may say `"look": "<id>"` (lower-case words). No `look`: the Guardian keeps the look it had, except an **evil boss of a demon sector** (`abyss`, `blood`,
  `flame`, `infernal_foundry`: the sectors where `data/nusmp/mega/factions/demon.json` weights the demon faction at 5) which wears the demon of its tier: quasit, vrock, hezrou, balor, balor.
  (`AbyssalLooks.resolve`, the table `theme_defaults`.) A holy boss, and a boss of any other sector, is never a demon.
* The table `assets/nusmp/abyssal/looks.json` (written by `tools/abyssal/gen_abyssal.py` next to the models it describes) holds, per look: name, kind (`humanoid` or `geo`), the reference height, the
  ground a walk and a run cycle cover, the shadow, the culling box, the boost, the glow colour, `dust`, the three phases (model, caption, and for a humanoid its skin, its extras model and the extras),
  the clips file, and for a humanoid `slim` and `part_scale`.
* `data/.../famous/*.json` and `evil_t1a.json` are written by the generators `tools/labyrinth/gen_dnd_famous.py` / `gen_dnd_bosses.py` (they now carry the look map), so a re-run keeps the field.

## 4. The looks

Humanoid = the vanilla **player body** with a real 64x64 player skin (box UV, a glow skin next to it) plus separate GeoLite **extras** keyed by the look id (the owner's rule for human-shaped bosses).
Geo = a full GeoLite body (the non-humanoids). Every look has three phases: the model of phase 2 and 3 differs from phase 1 (glow, extra parts, a cracked or broken piece), and each change of phase is
announced above the hotbar with the caption of that look (`message.nusmp.mega.dnd.look.<id>.p2` / `.p3`, lang fragment `tools/lang/abyssal_bosses.json`) and plays the cast clip once.

| look | worn by | built as | phase 1 | phase 2 adds | phase 3 adds |
|---|---|---|---|---|---|
| `lich` | `evil_t1a` (a generic lich) | humanoid | skeleton in a hooded violet robe, spiked gold crown, a phylactery on the belt, green-violet soul fire in the sockets, a bone staff with a skull that floats at its side, ghostly wisps | the phylactery cracks (a glowing seam), the eye flames and the ribs' soul fire swell, more wisps | the phylactery is broken open and leaks a beam of light with shards circling it, the skull cracks, a full aura |
| `acererak` | Acererak | humanoid | bare gem-studded skull, tall gold crown, indigo and gold robe with a fan collar, cyan fire, a floating open tome, three orbiting skulls | the gems blaze, five skulls | seven skulls, bigger eyes, cracked skull |
| `vecna` | Vecna | humanoid | one red eye and a sewn socket, a short left arm ending in a bound stump (`part_scale`), the **Hand of Vecna** floating beyond it, the **Eye of Vecna** floating at his shoulder, a ragged web-trimmed cape | the Hand ignites (flames on the fingers), the Eye sends a ray | three rays, a bigger Eye, more smoke |
| `strahd` | Strahd | humanoid | pale noble, slicked black hair, red eyes and fangs, black and crimson coat, stand collar, a cape lined in blood red, four bats circling | the cape opens into bat wings, seven bats, eyes blaze | bigger wings, ten bats, a red mist along the floor |
| `zariel` | Zariel | humanoid (slim arms) | fallen angel in burnt plate with glowing seams, a broken halo of fire, charred wings with burning tips, a flame sword, fire on the pauldrons | bigger wings, more flames | a second pair of wings, the halo closes into a ring |
| `asmodeus` | Asmodeus | humanoid | red skin, a crown of seven curved horns, gold eyes, black and gold robe with a mantle of spikes, leathery wings, a spaded tail, the Ruby Rod | wings spread to full span, hellfire on the shoulders | taller horns, wings veined with gold, a ring of hellfire behind the head |
| `orcus` | Orcus | humanoid | bloated grey-green demon prince, a great gut hung with a skull chain, ram horns, skull pauldrons, torn wings, the Wand of Orcus (skull, green fire) | the belly splits and soul fire shows | ribs break out of the gut, bigger horns, a crowd of green souls |
| `lich_king` | the Lich King | humanoid | skeleton in frost-steel plate under a horned great helm (visor open on the skull), fur collar, cape, spiked pauldrons, a huge runeblade | ice crystals grow on the shoulders | a ring of rune light opens under his feet |
| `solar` | the Solar | humanoid | gold skin, white-gold hair, blind gold eyes, white and gold plate, a halo, feathered wings, a sunburst, a bow of light | a second pair of wings, an arrow of light | six wings, a larger halo |
| `demogorgon` | Demogorgon | geo | two mandrill heads (red nose stripe, blue ridged cheeks, orange eyes, fangs, a purple mane) on two necks, tentacle arms, purple-blue scaled body, serpent tail | eyes swell, barbs of the tentacles glow | both heads crowned in horns, a whirl of purple light |
| `beholder` | Beholder | geo | round mauve body, one huge central eye with heavy lids, a toothed maw, ten eyestalks each ending in an eye of its own colour | the stalk eyes swell and burn, the central eye blazes | four more tendrils, cracks of light, the cone of the anti-magic gaze |
| `elder_brain` | Elder Brain | geo | a brain in a jar of green ooze (brass rim and posts), eight crawling tentacles and two long ones | the glass cracks, the brain swells | the jar bursts: shards of glass hang in the air round a bare, glowing brain ringed with psionic light |
| `kraken` | Kraken | geo | a squid on eight arms: a tall slanted mantle with fins, two slit-pupil eyes, a beak, two long feeding tentacles with clubs, bioluminescent spots | the lights blaze, four more arms | lightning crawls over the mantle |
| `tiamat` | Tiamat | geo | one body, five necks fanning from the shoulders, a head of each chromatic colour (red the highest), leathery wings, a barbed tail | every mouth burns with its breath, crests | bigger wings, glowing back, a ring of the five colours |
| `bahamut` | Bahamut | geo | sleek platinum dragon, swept horns, a crest, four whiskers, feathered wings edged in blue light | a halo of platinum light behind the head | a corona of rays from the wings, the mouth glows |
| `dracolich` | the Dracolich | geo | a dragon of bare bone: spine, rib cage with soul fire, torn teal wings on bone fingers | the fire in the ribs swells, the skull ignites | the phylactery gem floats over the back |
| `tarrasque` | the Tarrasque | geo | wingless, squat, plated: a carapace of spiked plates, two huge forward-curving horns, a spiked club tail | the reflective carapace shimmers | the carapace blazes, bigger horns |
| `lolth` | Lolth | geo | a drider: a white-haired dark elf queen with a crown of spider-leg spikes on a giant spider (thorax, spotted abdomen with a glowing hourglass, eight jointed legs), a scourge | four legs rise behind her like a crown | a second pair of arms with blades, eight burning eyes, a blazing hourglass |
| `quasit` | evil boss of a demon sector, tier 1 | geo | small pot-bellied green imp, swept ears, horns, fanged grin, bat wings, barbed tail | bigger wings, the tail tip burns | longer horns and claws, a corona of fire |
| `vrock` | tier 2 | geo | the vulture-demon: bald red head, hooked beak, a ruff of feathers, long taloned arms, bird legs, ragged wings | spores rise | violet fire on the wing tips, bigger eyes |
| `hezrou` | tier 3 | geo | the toad-demon: a hulking green-brown body, a mouth from ear to ear, bulging yellow eyes, webbed arms, a ridge of spines, a stench cloud | the spines glow, acid drips | taller crest, a thicker stench |
| `balor` | tiers 4 and 5 | geo | giant, lava-cracked black-red body, a horned head in a mane of flame, huge bat wings, hooved legs, a jagged lightning sword and a whip of fire | wider cracks, the wings burn along the veins | ablaze from horn to hoof |

All clips are the names the pose clock plays: `idle walk run attack cast cast_big hurt death` (one shared file per look, `animations/entity/abyssal_<look>.animation.json`). The lich raises its arms and
floats its staff forward in `cast`; every look's `death` falls or collapses and the renderer fades it (an undead look in ash and soul fire).

## 5. For the player-model renderer (humanoids)

The owner decided that human-shaped bosses are player-model characters (`docs/playermodel.md`, `client/playermodel/**`). The nine humanoids are delivered in the kit's own format:

* **`assets/nusmp/playermodel/looks_abyssal.json`**: the same entry format as `playermodel/looks.json` (fields `skin`, `slim`, `glow`, `hair`, `hair_color`, `scale`, `extras`, `hold_r` / `hold_l`; `alpha`, `cape` and
  `glow_color` are left out: not needed). One entry per look and phase, id `abyssal_<look>_p<n>` (27 entries). `scale` is the look's height over 1.875 (a boss's real size comes from the entity, the table
  only says how tall the model was drawn). `hold_r` / `hold_l` -0.42 raises the arm that holds a weapon (Zariel's flame sword, Asmodeus' Ruby Rod, Orcus' Wand, the Lich King's runeblade, the Solar's bow).
* **Skins**: `textures/entity/playermodel/abyssal_<look>_p<n>.png` + `_glow.png`: real 64x64 classic skins (`slim` for Zariel). The robes, hoods, helms and plate are on the second layer, the base layer is
  opaque. Strahd, Zariel and the Solar use the kit's hair (`slick` black, `long` dark, `long` white-gold): their skin only has the scalp painted in the hair colour; the others are hooded, helmed, bald or horned.
* **Extras**: `geo/entity/pmx_abyssal_<look>_p<n>.geo.json`, its atlas `textures/entity/pmx_abyssal_<look>_p<n>.png` + `_glow.png`, its clips `animations/entity/pmx_abyssal_<look>_p<n>.animation.json` (only the
  extras' own tracks). The roots are `x_head`, `x_body`, `x_arm_r`, `x_arm_l` (pivots at the player model's own pivots; the file's coordinates are absolute humanoid ones, y up, 24 px = the neck line); every extra
  is a child of the root of the part it hangs on (crown, eye flames, horns, halo -> head; skirt, cape, wings, phylactery, collar -> body; pauldrons, weapons -> arms). Things that float beside the body (the
  lich's staff, the wisps, Strahd's bats, Acererak's orbiting skulls, the Hand and the Eye of Vecna, the rune ring) hang under `x_body`, so they follow it.
* `looks.json` (the table of `AbyssalLooks`) holds the same facts per phase (`skin`, `extras_model`, `extras[]` with each extra's id, part and bones) plus `humanoid {slim, part_scale, hair, hair_color, hold_r, hold_l}`.
* Vecna's short left arm is `part_scale` in the composite only (the kit has no per-part scale); in the kit table the missing hand is a black bound cap over the end of the arm (an extra on `x_arm_l`).
* Until the kit's loader reads `looks_abyssal.json`, `AbyssalRenderer` draws the composite `abyssal_<look>_p<n>` (the player body as GeoLite bones with the same skin and the same extras), so the humanoids
  already stand in the world; the kit replaces it.

## 6. How it is built (tools/abyssal)

`python3 -B tools/abyssal/gen_abyssal.py [id ...]` rebuilds the looks (deterministic; ~30 s for all), the table, the lang fragment and the preview scenes. `lib.py` is the toolkit (a painted box-UV
atlas packer with materials and a glow map, the player body and 64x64 skin, shapes: beam, horn, spike, bat wing, feather wing, rings; the clip writer with sway); `beasts.py` and `dragons.py` the shared
bodies; `undead.py` and `humans.py` the skin painters; `look_<id>.py` one file per creature. `python3 -B tools/abyssal/preview_sheets.py [id ...] [--clips attack cast death id]` renders with the
repo's preview harness (`tools/render_preview`) into `docs/previews/abyssal/` (`<id>_phases.png`: phase 1, 2 and 3 next to each other).
`python3 -B tools/abyssal/gen_abyssal.py --check` checks the files of the table (bones linked, uv inside the sheet, texture and glow present, every clip bone exists).

## 7. Code (all additions; no existing behaviour changed for a boss with no look)

* `labyrinth/mega/guard/AbyssalLooks` (new, pure): the table, `resolve`, the file paths, `problems`.
* `DndProfile`: a 24th component `look` (plus the 23-argument constructor kept, so every caller and every file without it works), parsed from `look`.
* `DndProfiles.all()`; `LabyrinthGuardianEntity`: synced data (look, phase, cast time and kind), `lookId() lookPhase() castAt() castKind()`, `syncLook()` after the profile is set and after a load,
  the phase and its caption in `refreshDndName`; `DndBoss`: `g.noteCast(1)` in `legendary()` and `lair()`.
* `client/remodel/AbyssalRenderer` (new) and one `case` of `RemodelRenderers` pointing at it.
* Lang: `tools/lang/abyssal_bosses.json` (the look names `entity.nusmp.abyssal.<id>` and the captions); the captions are read with `translatableWithFallback`, so the English text of the table shows without the merge.

## 8. Tests

* `AbyssalLooksTest` (pure): 22 looks of 3 phases and distinct models; the humanoid table (anchors, skin path, slim, part scale); every famous profile names its own look; every shipped profile parses;
  `evil_t1a` is a lich and `holy_t1a` keeps the old look; the demon of a tier on a demon sector, never for a holy boss; a bad look id is refused; every look is worn by some boss.
* `AbyssalAssetsTest` also checks `playermodel/looks_abyssal.json` (27 entries, only keys the kit knows, skins and extras exist, roots are `x_<part>`). It reads: every look's files exist (`AbyssalLooks.problems`), models are linked and their faces inside their sheets, atlas and glow have the model's size, skins are 64x64, the clips
  exist and move no bone that no phase has, the phases' textures differ, the glow lights something.
* Self-test `guard.abyssal_looks` (`/multiverse mega selftest guard.abyssal_looks`, the CI smoke test runs it): the table's files are on the class path; every loaded profile makes a real Guardian wear
  its named look (or none); an evil boss of a demon sector (Abyss) wears a demon; phases 2 and 3 reach the synced data with a cast cue; a saved Guardian wears the same look in the same phase.

## 9. What could not be verified

* **Nothing was run in the game.** The previews are the harness's re-implementation: its light and colours differ from the game's. A look may read darker in a dim hall (the lich and the Lich King most).
* How the renderer feels live: the cast cue, the phase change swapping the model, the death fade with the ash, the scale of small looks (the quasit is drawn at about four times its model size; the
  boost is a table number to tune).
* The hitbox is the Guardian's (1.4 x 3.2 times its size), whatever the look: the Tiamat's wings and the Kraken's arms reach far past it.
* Draw cost: the biggest looks are 340 cubes (Tiamat, about 15 000 vertices a frame with the glow pass); one boss at a time, but weak machines may notice.
* The humanoid extras in the player-model renderer (the converter was not available): only the composite is drawn today.
* The blocky look of the large faces (the body sheets are one texel per model pixel, two for big cubes): taste.
