# Noelle Silva's armour forms: Valkyrie / Saint Valkyrie Dress, Mermaid, Sea Dragon God, Black Dragon

The owner asked for the Spirit Dives and the Valkyrie Dress to be remade, with effects "10x cooler than the non-Character-Grimoire users". The reference was the Noelle Silva "Variations"
table (Mermaid Form, Saint Valkyrie Dress, Dragon Form, Black Dragon Form). This document covers the Water armour forms; the Spirit Dive look itself belongs to the Spirit Dive builder
(`SpiritDiveForm`, `SpiritDivePieces`, `SpiritDiveAura`), which this work did not touch.

Nothing here was run in a game. Every picture below is a render of the preview harness (`tools/render_preview`: the real painter classes compiled against stub renderers), not an in-game screenshot.

## NEW / REPLACED / KEPT

| What | Status | Notes |
|---|---|---|
| Valkyrie Dress look (style 1): pearl-white enamel armour, layered feather wings, halo | REPLACED (look only) | the page, its id, order, cost and buffs are KEPT; the first look (the 1.93.0 water armour set, `client/aura/WaterAura`) stays and comes back with the client switch `noelleForms.classicLook` |
| Saint Valkyrie Dress (style 17): second pair of wings, aureole, gold sash, hovering lance, gyro rings | NEW | the character tier of the same page: only Noelle Silva's grimoire gets it |
| Mermaid Form (page `mermaid_form`, styles 2 / 18) | NEW | appended at the end of the Water book; open to every Water mage |
| Sea Dragon God Form (page `sea_dragon_god_form`, styles 3 / 19) | NEW | appended; locked to Noelle's grimoire (`CharacterSpells`) |
| Black Dragon Form (page `black_dragon_form`, styles 4 / 20) | NEW | appended; locked to Noelle's grimoire; limited, see below |
| Sea Dragon's Scale Mail (style 0) | KEPT | untouched, still drawn by `WaterAura` |
| `Aura.WATER`, `ModeArmor.Mode.VALKYRIE` and its overlay, all old pages | KEPT | nothing removed, no enum constant added (the forms ride on the style byte of the existing Water layer) |
| 62 new geo models (800 cubes), 18 sprite / skin textures | NEW | all re-runnable from `tools/` |
| Particle themes `form.noelle.*` (4) | NEW | appended to `tools/particles/gen_themes.py`, JSON regenerated (only additions) |

## Canon and sources (rule 10)

* English first. `blackclover.fandom.com` could not be fetched (host not reachable from the build session). The search results (English) gave: Valkyrie Dress = water armour with wings and a spiral water
  lance; Mermaid Form = a variation for underwater fighting, the armour becomes a fish-like tail, less coverage, the lance stays; Saint Valkyrie Dress = after the contract with Undine (Wikipedia, chapter
  295 coverage on CBR); Dragon Form = after the contract with the sea god Leviathan, "Valkyrie Armor: Dragon Form", first seen in chapter 359 against her mother.
* Japanese fallback. `ja.wikipedia.org` could not be fetched either; the Japanese search results (ciatr.jp, syntelligence.jp) confirm 海神戦乙女の羽衣鎧 (Valkyrie Dress), 精霊同化 with the water spirit Undine
  giving the 聖 (Saint) form, and 海竜神形態 (Dragon Form) from the contract with Leviathan.
* Black Dragon Form: **no text was found in either language**. Its look follows the owner's reference picture only (dark, scaled, winged, devil-like); its rules (limit, exhaustion, devil gate) are this
  mod's own design, not canon. Say so before relying on it.
* Not fetched at all: blackclover.fandom.com (all pages), ja.wikipedia.org, the Japanese fandom wiki.

## How a form is entered and read

1. The form is a page of the Water book. The armour policy (`data/nusmp/armour_policy.json`, WATER entry) lists the page as an ARMOUR page with a style: `valkyrie_dress` 1, `mermaid_form` 2,
   `sea_dragon_god_form` 3, `black_dragon_form` 4 (`scale_mail` stays 0). Casting it puts the armour layer on (`ArmourCast` / `ArmourState`, unchanged); casting it again takes it off, free.
2. The policy entry has `style_by: "character"` (new, appended to the allowed values): `ArmourRules.style(..., character)` adds the character bit (16) when the caster's bound grimoire is Noelle
   Silva's (`CharacterSpells.characterOf == NOELLE`). So the style byte is `form + 16 * tier`: 1 / 17, 2 / 18, 3 / 19, 4 / 20. Everybody else gets the standard tier of the same silhouette.
3. The tier and form are decided on the server and travel in the existing `PlayerAuraPayload` (nothing new on the wire). The client picks the look in `NoelleFormsAura` (called first by `WaterAura`).
4. `NoelleFormState` (server, registered from `NUSMP`) adds what the look needs: a once-a-second refresh of the form's buffs as short hidden effects, the limits, four motes of particles.

| Page | Tier of the page | Cost / cooldown | Open to | Buffs while worn (refreshed each second) | Limit |
|---|---|---|---|---|---|
| Valkyrie Dress | signature (kept) | 35 %, cd 1200 (kept) | every Water mage | the page's own 30 s of Speed II, Strength II, Resistance II, Jump II, Fire Resistance (kept) | none |
| Mermaid Form | zone | 20 %, cd 600 | every Water mage (canon: Undine's gift to the water mage) | Water Breathing; Dolphin's Grace II and Regeneration in water; Slowness I on land | none |
| Sea Dragon God Form | signature | 35 %, cd 2400 | Noelle's grimoire | Strength II, Resistance II, Speed I, Water Breathing; entry roar: 12 damage within 6 blocks (x size up to 1.5), knock back | 5 minutes |
| Black Dragon Form | signature | 35 %, cd 6000 | Noelle's grimoire | Strength III, Resistance III, Speed II, Night Vision; entry shockwave 18 within 8 blocks | 90 s, then 3 minutes of exhaustion (Slowness II + Weakness I; never hunger); refused during the exhaustion, during Devil Union (`GrimoireBook.inDevilState`) and during the Devil Union aftermath (`DevilFormLimit.refuse`) |

Every armour page also pays the policy's upkeep (0.4 % of the pool per second, `ArmourState`) and the policy's bonus (12 % less damage, +5 % attack, +8 % speed, +10 % knockback resistance). Damage goes
through `GrimoireBook.hurt` (balance law); at most 24 foes are hit by an entry wave. Nothing in the forms touches the food bar.

## The looks (what is drawn)

Pieces are GeoLite models in whole-body coordinates anchored on the player's own head, body, arms and legs, forged in and shattered out by `EquipAnimation` (the Djinn standard); wings open out from
the spine while they forge (`unfold`). Passes: skin shell (translucent), pieces (cutout / translucent) + their glow maps (additive), haze (additive), then the dynamic parts, all additive through
`AuraRender` / `SafeAdditive`: ground seal with three ripples, a pillar of rising water for the first 28 ticks, winding ribbons (water; ink for the Black Dragon: a dark translucent under-ribbon and a blue
flame on top), flat ring strips (halo and gyro rings), orbiting feathers / bubbles / sparks / embers, the great seal behind the back, glowing eyes (dragon forms; red for the black form).

| Form | Standard tier pieces | Character tier adds | Built after |
|---|---|---|---|
| Valkyrie / Saint Valkyrie (`nvl`) | helm (gold circlet, crest, gem, swept blades), cuirass (pearl enamel, seigaiha engraving, gold border, blue gem), skirt of water cloth, layered pauldrons, vambraces with feather fans, gauntlets, greaves with ankle wings, skeletal wings (gold arm, 10 primaries, 8 secondaries, 7 coverts per side), gold halo | second lower pair of wings, aureole (ring of plates and a sunburst), gold sash with feathers, drill lance of water, three gyro rings, big back seal | Saint Valkyrie still (large angelic wings, ornate armour, halo) |
| Mermaid (`nml`) | circlet with shell and fin ears, twin tails of hair (two strands each, nine segments), shell top, scaled leggings on each leg that read as one tail, tail train ending in a fluke on the ground, hip fins, forearm fins, trident of ice-water with a turning spiral | dorsal sail, two water scarves, shell tiara, more bubbles, second ring | anime still (twin tails, tail, trident) |
| Sea Dragon God (`ndg`) | horned helm (chained horns), scale cuirass with belly plates and gem, spiked pauldrons, clawed vambraces, taloned greaves, scale skirt, dragon wings (arm, 4 finger spars, membrane gores), long tail with a water fin, dragon-head cannon on the right forearm | spine ridge, second horn crown, lower wings, second cannon, three rune rings, serpent coil | manga: horned, winged, dragon-scale armour |
| Black Dragon (`nbk`) | the same silhouette in obsidian scales with blue-flame seams, black horns with red glow, torn wings with blue-flame veins, jagged tail blades | as above, ink tendrils, red eyes at every level | reference still (dark, devil-like) |

Animation: wings beat (faster in the air), hair, scarves, skirts, tail and horns sway with a lag along their length, halo and aureole turn, the lance and trident bob and their spirals turn, the cannon's jaw works.

## Cost and worst cases (rule 11)

Server: one persistent-data read per player per tick (absent for nearly everybody, then it stops); for a wearer of one of the forms once a second: up to four effect refreshes and one `Fx.send` of 4 motes
(through the particle budgets). Entry: at most 24 targets hit, one puff (24) and one ring (24 points) of particles through the budgets. No entity is spawned, no world scan, no new packet (the armour policy's
lease of one aura packet per 100 ticks to the players that track the wearer is the only traffic). State is per player (persistent data), cleared by the armour engine on death, logout (forms), respawn.

Client, per frame (`NoelleFormRules`): level of detail by distance: up to 24 blocks the full tier (character pieces only for the character tier); 24 to 64 blocks only the silhouette pieces, no glow pass,
no skin, no ribbons, rings or drops; beyond 64 nothing. A crowd cap per frame: 2 players at the character level, 4 at standard or above, 6 distant ones, everyone else not drawn. Sprite quads per player
are at most `QUAD_CAP` = 400 (the worst, Black Dragon character tier, is below that; the unit test holds every form at every level under it).

Vertices per player per frame (preview harness, both passes of every piece and the shells included; "budget note" in the tool is 6000 per layer, the Djinn set and the Flame armour are above it too):

| Form | standard, near | character, near | silhouette at 30 blocks |
|---|---|---|---|
| Valkyrie | 8696 | 12984 | 2352 |
| Mermaid | 6968 | 8624 | 1680 |
| Sea Dragon God | 8136 | 10712 | 1800 |
| Black Dragon | 8768 | 11304 | 1928 |

Worst frame with a crowd: 2 x 12984 + 2 x 8768 + 6 x 2352 = about 57 600 vertices for all form wearers together; the common case is one or two players. Allocation per frame: none beyond the
rotation of a moving part (as the shared rig).

Assets: 3.2 MB (models, textures, glow maps, animations, sprites), test-limited to 8 MB.

## Tests

* `NoelleFormRulesTest` (pure): style byte, bad bytes, enum only appended, page gates, level of detail, crowd budget, quad cap per form and level, limits and exhaustion, buffs (no hunger), entry bounds, character rule.
* `NoelleFormAssetsTest` (reads the tree): every piece has model, texture, glow map and animation (bones exist, uvs inside), glow maps carry no colour under alpha 0, painter lists exactly the pieces of the rules,
  sprites fade at their border, asset budget, generators and lang present, policy / book / lock agree, classic switch is a guarded client config, server classes hold no client class and no food-bar call.
* Existing tests adjusted for the three appended pages (lists only): `WaterRebuildTest`, `UndineSourceScanTest`, self-test `magic.water_rebuild`.
* Self-test `magic.noelle_forms` (CI server smoke): pages appended and cast-able, locks, policy styles for plain and character tier, every model / texture on the classpath, budgets, and a stand-in player puts on,
  keeps, takes off and is read as the Mermaid / Sea Dragon forms, and the Black Dragon exhaustion refuses a new form.
* Run in the shim: GeoAssetsTest, ArmourPolicyTest, WaterRebuildTest, ConfigFileNamesTest, ServerSafetyTest, ParticleThemesTest, SafeAdditiveSourceScanTest, SafeAdditiveTextureScanTest, PerfSourceScanTest,
  FxCodemodMapTest, MagicRebuildAssetsTest, UndineSourceScanTest (all green).

## Iteration against the reference (preview rounds)

1. First armour and wings as flat fans of feather cards: read as a peacock tail. 2. Skeletal wings (gold arm, wrist, three feather rows; dragon wings with finger spars and membrane gores), forms 2 to 4 added.
3. Own materials (enamel, dragon scales, membrane, horn, ink): the first scale pattern was a dither, redone as shields with lit edges. 4. Horns as bone chains instead of floating cubes, cannon head turned
forward, lance and trident rebuilt, aureole closed into a ring. 5. Ink under-ribbons, glow passes cleaned for the safe-additive rule, crowd budget and silhouette level of detail.
Pictures: `docs/previews/noelle_forms/` (`reference_comparison.png` puts the owner's table next to the four forms).

## Files

New: `book/NoelleFormRules`, `NoelleFormState`, `NoelleFormsSelfTests`; `client/aura/NoelleFormsAura`, `NoelleFormRig`, `NoelleFormsOptions`, `NoelleFormsClientConfig`; tests above;
`tools/gen_noelle_forms_geo.py` (+ `_val`, `_mer`, `_drg`, `_blk`), `tools/noelle_dragon_kit.py`, `tools/noelle_forms_lib.py`, `tools/gen_noelle_forms_textures.py`,
`tools/render_preview/scenes.d/noelle_forms.json` + `gen_noelle_scenes.py`, `tools/lang/noelle_forms.json`, `docs/previews/noelle_forms/*`, assets `geo/entity/n{vl,ml,dg,bk}_arm_*`, `textures/entity/...`,
`animations/entity/...`, `textures/aura/noelle_*`.
Edited (additions only): `WaterBook` (3 pages), `WaterArts` (3 spells, particle helper), `CharacterSpells` (2 locks), `ArmourRules` / `ArmourPolicy` / `ArmourCast` (the character style rule),
`WaterAura` (one line), `NUSMP` (3 lines), `armour_policy.json` + `tools/gen_armour_policy.py` (Water pages and `style_by`), `tools/particles/gen_themes.py` + `particle_themes.json`.

## Not verified

* Nothing was seen in a game: real lighting, translucency sorting of the wings against terrain and water, frame cost, the look of the forge animation with real timing, first person.
* The Black Dragon Form has no canon text found; its numbers and limits are this mod's design. The Mermaid Form being open to every Water mage is a design decision (canon ties it to Undine's gift to Noelle).
* The ground seal, ribbons and rings are textures made by script and only judged in the preview harness; the sound of the forms is the old Water sound (none added).
* The Mermaid tail is built as scaled leggings plus a tail train, because the legs of the player keep their own animation; a true single tail was not attempted.
* The crowd budget draws the first players the renderer hands over, not the nearest first.
* `docs/rebuild/armour_policy.md` was not regenerated (the policy JSON has other builders' hand additions the generator does not know); the Water row there still lists two armour pages.
* The page order shifts the mode number of Spirit Dive and the universal pages of the Water book by three (as every earlier appended page did); the unlock bits of the family pages themselves do not move.
