# Shield fix (1.110.0): every shield is used and held like a vanilla shield

Owner request: "fix shield relics/weapons to be used and facing the same way as a basic Minecraft shield".

## What was wrong

* **Facing / pose.** The seven shield models carried display transforms made for a sprite on the sword diagonal. Third person: the Magebane and Legendary shields were turned
  `[-90, 0, 180]`, the five relic shields had no left hand at all and an unturned `[0, 0, 0]`; first person: `[0, -90, 25]` / `[0, -25, 8]`. Computed through the game's hand chain
  (`tools/item_preview/preview_shields.py` prints it), the emblem face of the old models pointed up-forward or **toward the camera** in first person, where vanilla's points away
  from you. Only the Magebane and Legendary shields had a `_blocking` model; the other five never changed pose when raised. The `blocking` item property was registered for those two only.
* **Use.** `RelicWeaponItem.use` raised a shield only when it had no act and was attuned, so the Hero's Shield and the Calamity Wave Aegis (acts) and every unattuned relic shield answered a
  click with a technique or a refusal. On the client `use` returned PASS (the hand was started only by the server's sync). The hero Legendary Shield refused the off hand.

## What it is now

| Part | Vanilla contract | Where |
|---|---|---|
| use | `UseAnim.BLOCK`, use duration 72000, `SHIELD_BLOCK`, `use()` starts the hand it was given on both sides, either hand | `MagicWeaponItem` (kept, was right), `HeroWeaponItem` (off hand added), `RelicWeaponItem` + `ShieldUseRules` (plain click always raises) |
| block, reduction, disabling | `LivingEntity#isBlocking`, `LivingShieldBlockEvent` (the hero shield's own handler stays), axes: `canDisableShield` puts the used shield on the 100-tick cooldown | `item/ShieldBoot` |
| arm pose | vanilla chooses `ArmPose.BLOCK` from `UseAnim.BLOCK`; nothing to change | (vanilla) |
| display | one shared preset = `minecraft:item/shield` and `shield_blocking` 1.21.1 | `visual/shields/ShieldPreset`, written to the jsons by `tools/gen_shield_models.py` |
| mesh frame | mesh drawn on the frame vanilla's `ShieldModel` uses (origin of the item pose, plate centred, emblem toward +z, handle toward -z, up +y), so the same display values turn it the same way | `visual/shields/ShieldFrame`, `client/WeaponRenderer` (`Mesh.shield`) |
| model swap | `blocking` override in every shield model + `blocking` property for every id | `models/item/<id>.json`, `client/ShieldClient` |

The mesh is scaled uniformly to fit 1.3 x 1.2 blocks (a vanilla plate is 0.75 x 1.375; the wider limit is for the winged Celestial Support), centred on the plate's centre, with the front
of the face on the plate's front plane (z = 2/16), so the face stands exactly where vanilla's emblem does and the straps sit where its handle does. In the inventory (and
the display `gui`) the flat sprite icon stays, as before: the preset's `gui` is an identity (vanilla's would tilt the flat icon); `head` is the identity (vanilla has none).

Sneak + use on a shield relic that has acts (Hero's Shield: Shield Throw; Calamity Wave Aegis: the three acts) is still the act, as is the Use Worn Relic key; sneak + use on an
unattuned shield is still the five-second attunement ritual. Without sneaking the click raises the shield and an unattuned one tells you it needs attunement. A shield with no act blocks
while sneaking too.

## The shield-class items

| Item | Class | Use | Display / blocking model | Mesh | Status |
|---|---|---|---|---|---|
| `magebane_shield` | `MagicWeaponItem` (Kind.MAGEBANE_SHIELD) | was right; kept | preset (REPLACED the old display; `_blocking` REPLACED) | `weapon_meshes/magebane_shield.txt` on the shield frame | KEPT use, REPLACED model display |
| `legendary_shield` (hero, + curse form mesh) | `HeroWeaponItem` (HeroStyle.BLOCK) | off hand NEW | preset (REPLACED; `_blocking` REPLACED) | `legendary_shield`, `legendary_shield_curse` on the frame | REPLACED display, NEW off hand |
| `aegis_of_the_abyss` | `RelicWeaponItem` (Look.SHIELD) | plain click raises: REPLACED | preset REPLACED; `_blocking` model NEW | on the frame | REPLACED + NEW |
| `celestial_support` | `RelicWeaponItem` | plain click raises (unattuned too): REPLACED | preset REPLACED; `_blocking` NEW | on the frame | REPLACED + NEW |
| `heros_shield` | `RelicWeaponItem` (has an act) | plain click raises, sneak + use = Shield Throw: REPLACED | preset REPLACED; `_blocking` NEW | on the frame | REPLACED + NEW |
| `thunder_shield` | `RelicWeaponItem` | plain click raises: REPLACED | preset REPLACED; `_blocking` NEW | on the frame | REPLACED + NEW |
| `calamity_wave_aegis` (+ upgraded `up/` mesh and its lod) | `RelicWeaponItem` (3 acts) | plain click raises: REPLACED | preset REPLACED; `_blocking` NEW | normal, upgraded and lod meshes on the frame | REPLACED + NEW |

NEW as files: the five `<id>_blocking.json` models (aegis, celestial, hero's, thunder, calamity), `visual/shields/*`, `item/ShieldBoot`, `client/ShieldClient`, `relics/ShieldUseRules`, the tests,
`tools/gen_shield_models.py`, `tools/item_preview/preview_shields.py`, `tools/snapshot_shield_models.py`, `docs/previews/shields/`, `docs/visual_upgrade/shield_fix_before.json` (the old display blocks).
KEPT: every item id, `_icon` models, meshes, skins, glows, engravings, the relic acts and passives, the hero shield's skills and `HeroCombat.onShieldBlock`, the old Magebane `blocking` property line.

Not shields on purpose: `eisens_shield_strap` and `heathcliffs_holy_sword_shield_strap` (worn on the belt / back, never held); `vassal_mirror` (a cast mirror; the class table files it under
"shield" for its size only, `ShieldSet.CLASS_ONLY`); the Vassal Carriage. No Tensura shield is re-skinned by the mod (none found in the registries, the item json or the class table); no
Djinn or Cardinal / Vassal shield exists besides the list above. If one is added, `ShieldModelsTest.theClassTableAndTheSetAgree` and `ShieldClassesTest.everyRelicOfTheShieldLookIsInTheSet`
fail until it joins `ShieldSet`.

## Before / after (computed, not seen in game)

`python3 -B tools/item_preview/preview_shields.py` draws vanilla / before / after for every shield in third person (right and left hand, lowered and raised) and first person, through the
game's own chain (`LivingEntityRenderer`, the humanoid arm with the ITEM / BLOCK pose, `ItemInHandLayer`'s turn to the grip, `ItemTransform#apply`, `applyItemArmTransform`), into
`docs/previews/shields/<id>.png` and `shields_sheet.png`. It also prints the world direction of the face. Example (Magebane Shield, player facing +z):

| context | vanilla face | before | after |
|---|---|---|---|
| third person right, lowered | (-1.00, 0, 0) outward | (0, +0.31, +0.95) forward | (-1.00, 0, 0) |
| third person right, raised | (-0.26, +0.11, +0.96) forward | (+0.29, +0.81, +0.51) up | (-0.26, +0.11, +0.96) |
| first person right | (0, 0, -1) away from the camera | (-0.42, 0, +0.91) toward the camera | (0, 0, -1) |

## Numbers

Display values (rotation degrees, translation 1/16 block, scale), identical in `ShieldPreset.java`, `tools/gen_shield_models.py` and every shield model json:

| context | lowered | raised (`_blocking`) |
|---|---|---|
| thirdperson_righthand | [0,90,0] [10,6,-4] 1.0 | [45,135,0] [3.51,11,-2] 1.0 |
| thirdperson_lefthand | [0,90,0] [10,6,12] 1.0 | [45,135,0] [13.51,3,5] 1.0 |
| firstperson_righthand | [0,180,5] [-10,2,0] 1.25 | [0,180,-5] [-15,5,-11] 1.25 |
| firstperson_lefthand | [0,180,5] [10,0,0] 1.25 | [0,180,-5] [5,5,-11] 1.25 |
| fixed | [0,180,0] [-2,4,-5] 0.5 | same |
| ground | [0,0,0] [4,4,2] 0.25 | same |
| gui, head | identity | identity |

Tests: `ShieldModelsTest` (every shield json has the preset within 0.01, the override, the icon, the mesh; class table and set agree), `ShieldFrameTest` (every shield mesh, incl. curse and
upgraded, fits the frame), `ShieldClassesTest` (source scan: BLOCK / 72000 / SHIELD_BLOCK / starting the hand, relic look set, client predicate, no Better Combat attributes on a shield, axe rule wired),
`ShieldUseRulesTest`. In-game self-tests for the CI server smoke: `visual.shields.items`, `visual.shields.use` (each shield raised from the main and the off hand by a stand-in player), `visual.shields.models`.
Kept passing: `WeaponMeshesTest`, `WeaponClassesTest`, `BowDisplayGuardTest`, `WeaponAttributesTest` (Better Combat), `HeroAssetsTest`, `ServerSafetyTest`, `RelicRenderCoverageTest`.

Better Combat: shields have no `weapon_attributes` file (the existing rule, now pinned by a test), so Better Combat treats them as plain items; its off-hand hiding is only for weapons whose
attributes say `two_handed`, so a shield stays usable beside a one-handed weapon. Nothing of Better Combat was touched.

## Multiplayer / server

All blocking, disabling and cooldowns are the server's (vanilla's blocking plus `ShieldBoot`); the client only draws and registers the `blocking` property. No `Minecraft.getInstance()` outside `client/`;
no static player; no packet; no config. `ShieldClient` and the renderer branch are client-only.

## Could not be verified (nothing visual can be run here)

* The vanilla display numbers are written from knowledge of Minecraft 1.21.1's `shield.json` / `shield_blocking.json`; no game or client jar was available to diff them. The `ground` translation
  (`[4,4,2]`) is the one I am least sure of. If one differs, change `ShieldPreset.java` and `tools/gen_shield_models.py`, run the generator.
* The "face outward" claim rests on `ShieldModel`'s plate being at z -2..-1 with the emblem on its -z side and `BlockEntityWithoutLevelRenderer` flipping it with `scale(1,-1,-1)`; and on every
  shield mesh being built face +z (the generator docstring says so for the Magebane Shield; the others were judged from their z extents, straps at -z, and the previews' texture side, not seen in game).
* The preview's hand chain is my reading of the game's code, not a capture; third-person arm angles for other animations (walking, swinging) are not simulated.
* The mesh size (1.3 x 1.2 block box) is a choice, not vanilla's 0.75 x 1.375; the winged Celestial Support comes out small in the body because its wings set the width. The LOD of the Calamity Wave Aegis
  is fitted to its own bounds, so it may shift a few hundredths of a block when it swaps in at 12 blocks.
* `visual.shields.use` drives `use()` with a stand-in player; the 5-tick raise of `isBlocking` is vanilla's and is not ticked there. `ShieldBoot`'s axe rule duplicates what NeoForge's
  `Player#disableShield` is believed to do for a modded shield (I could not read it); it is idempotent.
* The first-person oversize guard no longer applies to a shield (vanilla's first-person size 1.25 is the standard); a very large shield in first person fills the view as the vanilla one does.
* Right-click now raises the Hero's Shield / Calamity Wave Aegis instead of firing their first act; the acts moved to sneak + use and the worn-relic key. The Hero's Shield tooltip text change is in the lang
  fragment `tools/lang/shield_fix.json` (key `item.nusmp.heros_shield.desc`, overrides the `en_us.json` entry; the `Wave23.java` fallback text matches it).
