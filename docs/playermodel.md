# The player-model kit (1.94.0, phase 1)

Owner request: "remake all models to use the player model, fix the hair to look better". Scope: every humanoid NPC and boss. This is phase 1: the shared kit, the hair system, and the
story / arc / ally / avatar / Zogratis humanoids. Phase 2 (the dungeon's humanoids) uses this kit; the list of what phase 1 left is at the end.

Nothing here could be run in a game. What was checked: the type check against the API stubs, the JUnit tests of the pure parts and the asset audit, and software-rendered previews
(`docs/previews/playermodel/`, made by `tools/playermodel/preview.py`, a re-implementation of what the kit draws: colours and light differ from the game).

## 1. Survey: how every entity type is drawn today, and which are people

"Effective" is the renderer that wins at run time (`client/remodel/RemodelRenderers` registers last and replaces the original renderer, which stays in the tree as legacy).

| Entity id | Effective renderer (legacy renderer) | Built on | Person? | Phase 1 |
|---|---|---|---|---|
| `arc_npc` (40 characters) | `ArcNpcRenderer` | GeoLite `arc_<id>` | yes | **converted** |
| arc bosses: heath_grice, vetto, lotus_whomalt, mars, revchi_salik, meliodas, merlin, king, escanor, ban, diane, gowther, fana, rhya, rades_spirito, patolli, lucifero, morris_libardirt, zagred, udaeus | `ArcBossRenderer` | GeoLite `arc_<id>` (+`_form2`) | yes | **converted** |
| arc bosses black_goliath, amphisbaena, minotaur | `ArcBossRenderer` | GeoLite | no (monsters; the minotaur has a bull's head) | kept |
| `dante_zogratis`, `vanica_zogratis`, `zenon_zogratis`, `lucius_zogratis` | `KitBossRenderer` (`ZogratisBossRenderer`) | GeoLite | yes | **converted** |
| `dark_disciple`, `zogratis_clone` | `KitMobRenderer` (`ZogratisDiscipleRenderer`, `ZogratisCloneRenderer`) | GeoLite | yes | **converted** |
| `blood_beast` | `KitMobRenderer` (`ZogratisBeastRenderer`) | GeoLite | no | kept |
| `devil_avatar` | `KitBossRenderer` (`AvatarRenderer`) | GeoLite | yes (a 2.8 block devil) | **converted** |
| `player_avatar` | `PlayerAvatarRenderer` | vanilla `PlayerModel`, composed skin, 16 GeoLite hair models | yes | **converted** (hair replaced) |
| `zagred_legacy` | `KitZagredRenderer` (`ZagredRenderer`) | GeoLite | yes | **converted** (aura and reticle kept) |
| `god_of_order` | `KitBossRenderer` (`GodOfOrderRenderer`) | GeoLite | yes | **converted** |
| `quest_giver_npc`, `merchant_npc` | `HubNpcRenderer` | vanilla `VillagerModel` | yes | **converted** |
| `tower_wizard` | `KitMobRenderer` (`TowerWizardRenderer`) | GeoLite | yes | **converted** |
| `painted_construct` | `KitMobRenderer` (`PaintedConstructRenderer`) | GeoLite | knight yes, hound and giant no | knight **converted**, others kept |
| `mirror_double` | `MirrorDoubleRenderer` | vanilla `PlayerModel` with the owner's skin | yes, already the player model | left (meant to look like the player) |
| `spirit_lord`, `elemental_spirit_lord`, `wind_spirit_lord` | `KitMobRenderer` (`SpiritLordRenderer`, `ElementalSpiritLordRenderer`, `WindSpiritLordRenderer`) | GeoLite `arc_spirit_*` | no (elementals) | kept |
| `grimoire_daemon`, `cotton_sheep`, `cotton_cloud`, `rouge`, `thread_trap` | `KitMobRenderer`, `KitCloudRenderer`, `ThreadTrapRenderer` | GeoLite | no | kept |
| `magic_prop`, `carriage` | `MagicPropRenderer`, `CarriageRenderer` | props, vehicle | no | kept |
| `jimmy`, `alfredo`, `carl` (corpses) | `NamedCorpseRenderer` | GeoLite `corpse_*`, 4 decay skins each | yes | **NOT converted** (phase 2, see below) |
| `living_armor`, `floor_warden`, `labyrinth_guardian` | `KitMobRenderer` with `KitProbes.ARMOUR` (`LivingArmorRenderer`, `FloorWardenRenderer`) | GeoLite `arc_living_armor` / `arc_floor_warden` | yes (armoured bodies) | **NOT converted** |
| `goliath` | `KitBossRenderer` (`GoliathRenderer`) | GeoLite, 6 blocks | yes (a giant) | **NOT converted** |
| `foundry_mob` (about 900 monsters) | `FoundryRenderer` | GeoLite `foundry_<id>` with shared `foundry_anim_*` | many | **NOT converted** |
| `dungeon_direwolf`, `basilisk`, `dungeon_slime`, `charybdis_spawn`, `magicule_scale` | `KitMobRenderer`, `KitBossRenderer`, `MagiculeScaleRenderer` | GeoLite | no | kept |

`shadow` (`ShadowPackPainter`), `soul` (`PaladinLayer`), `heroweapon` (HUD, keys, carriage) are not entities: they are render layers and overlays on the player, which already is the player model.

### Humanoids NOT converted (for the phase 2 builder) and the class that draws each

| Entity id | Draws it now | Notes |
|---|---|---|
| `jimmy`, `alfredo`, `carl` | `client/corpse/NamedCorpseRenderer` | three bodies (1.95, 2.55 and 1.9 blocks), a decay stage 0..3 from the Soul Corpse mark (`CorpseDecay.stageOf`), a throwing arm for Carl (`castLeft()`); need 12 skins: write them as `MANUAL` sheets in `tools/playermodel/characters.py` with a stage variant |
| `living_armor` | `client/remodel/KitMobRenderer` + `KitProbes.ARMOUR` (legacy `client/dungeon/LivingArmorRenderer`) | an armoured person: paint the armour on the overlay layers (`skin_lib.paint_armor`) |
| `floor_warden`, `labyrinth_guardian` | same, model `floor_warden` (legacy `client/labyrinth/seal/FloorWardenRenderer`) | the Living Armor grown huge; scale by the `size` attribute (`PlayerBody.Dynamic.size`) |
| `goliath` | `KitBossRenderer` with `KitBossHooks.GOLIATH` (legacy `client/floor/goliath/GoliathRenderer`) | rises out of the floor (`rise`); a 6 block giant: `PlayerLook.scale` 3.2 |
| `foundry_mob` | `client/labyrinth/foundry/FoundryRenderer` | one entity type, the monster is a data id; the humanoid ones (paladins, mages, rogues, class servants ...) need a look per id in `looks.json` and a `PlayerBody` that reads that id; the creatures stay |
| `minotaur`, `black_goliath`, `amphisbaena` (arc bosses) | `client/arc/ArcBossRenderer` | monsters; the minotaur and black goliath are the only ones that could be a person with a custom head |

## 2. The kit (`client/playermodel/**`)

Public API (small and stable; everything else is internal):

* `PlayerBody<T extends LivingEntity>`: the one interface an entity implements. `look(e)` returns the `PlayerLook` worn now; all the rest is optional: `skin(e)` (a composed texture),
  `dynamic(e, partial, Dynamic)` (fade, tint, glow level and colour, size, squash, hover, cast, transformation, flash), `pose(e, partial, PoseState)` (the repo's own pose over the vanilla
  animation), `visible`, `cullGrow`, `showName`, `name`, `layers(Body, ctx)` (add your own render layers), `decorate(e, partial, Decoration)` (draw at the feet after the body).
* `PlayerModelRenderer<T>`: `new PlayerModelRenderer<>(ctx, body)`. It owns a wide and a slim `Body` (a `LivingEntityRenderer<T, PlayerModel<T>>`) and dispatches by the look's `slim`.
  It draws the vanilla `PlayerModel` (64x64 skin with jacket, sleeves, pants and hat layers) scaled by 15/16 times `look.scale` times `dynamic.size`, with the vanilla walk, swing,
  bow / crossbow / spyglass / block poses (`ArmPoses`, the player's own rule), sneak, riding, swimming and death animations, the vanilla armour layer (wide and slim models), held items, head
  items, elytra, then `HairLayer`, `ExtrasLayer`, `KitCapeLayer` (cape texture of the vanilla layout), `GlowLayer` (emissive skin pass + the power-up flash). Fade and tint go through `TintedBuffers`
  (a `MultiBufferSource` wrapper that multiplies every vertex colour, so armour, items and hair fade with the body); a fade needs the translucent render type, chosen automatically.
* `PlayerLook` (record, plain values, parsed from `assets/nusmp/playermodel/looks.json`): `skin`, `slim`, `glow` (+`glowRgb`), `hair` (+`hairRgb`), `scale` (1 = 1.875 blocks, `scaleForHeight`),
  `alpha`, `extras` (`Extra{model, texture, tint, glow}`), `cape`, `holdR` / `holdL` (an arm raised while it holds an extra). `PlayerAssets.look(id)` resolves an id (default look on a miss, logged once).
* Skins: `SkinPath.resolve("asta")` = `textures/entity/playermodel/asta.png`; a path under the assets is kept; anything that could leave the folder is refused. The glow map is `<skin>_glow.png`. A body
  can instead return a texture composed at run time from `skin(e)` (the Avatar).
* Pose: `PoseState` (per part `blend` / `target` / `add`, plus `sitting`, `lift`, `lean`; applied after the vanilla animation by `KitPlayerModel`) and `PosePatterns` (`cast`, `talk`, `slump`,
  `powerUp`, `read`, `point`, `bow`, `hover`).
* Extras: a GeoLite file whose root bones are named `x_head`, `x_body`, `x_arm_r`, `x_arm_l` (`x_leg_r`, `x_leg_l`); each root is drawn under that model part with the file's absolute humanoid coordinates
  (the technique of `client/aura/RebuildSetKit`: translateAndRotate, undo the part's offset, `GeoDraw.Space.PLAYER`), solid or translucent, then the glow map additive; the clip comes from the state
  (`ExtraClip`: idle, walk, run, attack, cast, cast_big, transform, death, with fall-backs). `GeoModelData.view(...)` (new) shares one parsed model between the anchors.
* Registration: `PlayerModelRegistry.init(modBus)` (called once from `NUSMP`, after `RemodelRenderers.init`, same LOWEST priority, so the kit's renderer is the last registered and wins),
  `PlayerModelRegistry.put(e, type, body)` and `PlayerModelRegistry.extend(consumer)` (phase 2 appends its conversions from client init code; each conversion is guarded so one bad type cannot stop the rest),
  `PlayerModelRegistry.Split` (an entity type with person and non-person kinds). To convert an entity: write a `PlayerBody`, add a look to the table, add one `put` line.

### Hair (`HairStyleData`, `HairDraw`, `HairLayer`, `HairSpring`, `HairPhysics`, `HairPalette`, `HairState`, `LegacyHair`)

* 23 styles in `assets/nusmp/playermodel/hair_styles.json`, painted sheets `textures/entity/playermodel/hair/<style>.png` (both written by `tools/playermodel/gen_hair.py`): `bald`, `short`, `slick`,
  `curtains`, `swept`, `messy`, `spiky`, `spiky_long`, `mohawk`, `bob`, `long`, `layered`, `hime`, `ponytail`, `high_ponytail`, `twin_tails`, `drills`, `braid`, `bun`, `pompadour`, `ahoge`, `mane`, `hood`.
* Geometry: layered boxes with the vanilla box UV on a 64x64 sheet. The scalp cap is the 8x8x8 box at the hat layer's UV (u 32, v 0) inflated 0.7 (the hat layer is 0.5), and every layer on top
  of it has its own inflate (0.85 and up), so no two surfaces share a plane (a test checks the cap and equal boxes). The haircut is carved by the sheet's alpha mask; the sheets are grey strands,
  tinted at draw time. A fringe is a second layer in front of the cap (single-segment chains). `HairPalette.of(rgb)` gives base, highlight and shade (black hair still shines); boxes pick a tone.
* Hanging locks are chains of 1 to 4 segments (`HairSpring`: damped springs, softer down the chain so the tip whips). The upper segments hang from the head, the lower ones from the body, so the
  back hair follows the neck. `HairPhysics` pulls the lock from: wind (forward speed), inertia (acceleration, sideways acceleration, turning), gravity (the lock counter-rotates the head's nod and tilt,
  or the body's lean), a standing idle sway, and kicks on landing and take-off; `HairState` integrates by real elapsed time (frame-rate independent, clamped, substepped).
* Helmets: any item in the head slot hides the cap and the pieces marked `hide` and keeps the segments that end below the helmet's lower edge (`HairStyleData.segmentShownUnderHelmet`).
* Wide and slim models differ only in the arms; the hair never reaches the arms (side locks sit in front of them).
* Replaces: the 16 geo hair models of the avatar creator. They stay in the assets (a test and the saved data need their ids); `AvatarParts.draw` no longer draws them, `AvatarParts.drawHair` (new) and
  the world renderer draw the new hair. The saved appearance is unchanged: `AppearanceData.hairId` 0..15 maps to a haircut and its colour through `LegacyHair` (0 short, 1 spiky, 2 long, 3 ponytail,
  4 twin tails, 5 bob, 6 bun, 7 mohawk, 8 messy, 9 swept, 10 braid, 11 ahoge, 12 drills, 13 pompadour, 14 layered, 15 hime).

## 3. What was converted, and how

The art kit's character sheets (`tools/arc/char_*.py`) already described every character (skin tone, face, hair, outfit, armour, accessories, glow). `tools/playermodel/` reads those sheets:

* `characters.py` loads them (drops the creatures) and adds the two Hub NPCs, `skin_lib.py` paints the 64x64 skin and glow map (face, hair scalp, outfit, sleeves, trousers, boots, gloves, belt,
  armour plates, accessories on the overlay layers, wide or slim arms), `gen_skins.py` writes `textures/entity/playermodel/<id>.png` (+`_glow`) and `playermodel/looks.json` (101 looks: 68 characters,
  their second forms, the four Dark Disciple devils, the Devil Avatar's two skins, the Hub NPCs).
* `extract_extras.py` cuts every non-body bone (wings, horns, tails, halos, capes, scarves, floating weapons, hats, props in hand) out of the arc model, anchors it on the player model part it hung from
  (`p' = V + k (p - A)`, `k = 2 / height`), writes `geo/entity/pmx_<id>.geo.json` + animation (extras' tracks only) and copies the texture as `pmx_<id>.png` (88 models). Held props are tilted up
  (`PROP_TILT`) and the holding arm raised (`hold_r` / `hold_l`) because the player model's arm hangs.
* Hair colour and style come from the sheet (`CH.HAIR_MAP`); the skin's own scalp is painted in the hair colour under the cap.

Converted entities (`PlayerModelTable`): see the survey. Behaviour kept: form rules of each boss (second look from the form, or from the first frame of the transformation for the Triad), glow curve
(`ZogratisGlow`), fade of a dying body, flash over the swap, Lucius' hover and Julius glow, clone fade and gold mark, the Disciple's devil colours, the God of Order's appear / read / beam / bow,
Zagred's fresnel aura and target reticle, the painted knight's tint and rise, NPC talk and name tag, the Avatar's composed skin, aura ring, glowing iris, horns, wings and outfit pieces.
Attributes, AI, sounds and triggers are untouched (client-only change).

## 4. Edits to existing files (all additions or one-word changes)

* `NUSMP.java`: two lines (self-test registration; `PlayerModelRegistry.init`).
* `client/geo/GeoModelData.java`: `view(Predicate<Bone>)`.
* `client/avatar/AvatarParts.java`: `draw` no longer draws hair; `drawHair` added. `AvatarPreview.java`: calls `drawHair`.
* `client/remodel/RemodelRenderers.java`: `paintedOther(ctx)` added. `client/remodel/KitZagredRenderer.java`: `reticle` made `public`.
* New: `client/playermodel/**`, `client/avatar/AvatarPlayerBody.java`, `tools/playermodel/**`, `tools/lang/playermodel.json`, assets under `playermodel/`, `textures/entity/playermodel/`, `geo|animations|textures/entity/pmx_*`,
  tests `client/playermodel/HairPhysicsTest`, `PlayerModelDataTest`, self-test `render.playermodel` (data only, runs on the dedicated server; add `render` to `SMOKE_EXPECT` in `.github/workflows/smoke.yml`).

## 5. Tools

`python3 -B tools/playermodel/gen_hair.py` (hair JSON + sheets), `extract_extras.py`, `gen_skins.py` (skins, glow, looks.json; run after the extras), `preview.py [ids] | --hair | --hair-motion | --sheet`.

## 6. Not verified

Nothing was run in a game. In particular: the exact look of every skin and of the extras on the vanilla rig at game lighting; the draw order of the layers against vanilla's armour; `TintedBuffers`
with NeoForge's vertex consumers; the hair springs' tuning (the numbers are reasoned, not played); slim-arm fit of arm-anchored props; the translucent fade of armour (armour stays solid while the body fades);
the CI smoke list. The type check reports only the stub artefacts (abstract classes the stubs cannot instantiate, `setupAnim` of the stubbed `HumanoidModel`).

## 7. Phase 2 (1.99.0)

The humanoids that section 1 lists as NOT converted (`living_armor`, `floor_warden`, `labyrinth_guardian` with the abyssal bosses, `goliath`, the `foundry_mob` people, the corpses Jimmy, Alfredo and Carl) are converted in
phase 2: `docs/playermodel_phase2.md` (what was built, the flag `client.playermodel.dungeon` that gives the old renderers back, the 385 new looks, the skin generator `tools/gen_dungeon_skins.py`, the level of detail).
