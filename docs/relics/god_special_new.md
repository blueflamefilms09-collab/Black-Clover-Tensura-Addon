# God and Special grade relics, wave 102 (release 1.111.0): 9 God + 20 Special, all NEW

Owner request: "JUST ADD MORE TO balance IT OUT": the relics of the mod were top-heavy (a God total of 51 against a Normal total of 9), so this wave ADDS 9 God grade and 20 Special grade relics next to everything that exists. **Every relic here is NEW: nothing existing was regraded, replaced, moved or removed** (CLAUDE.md rules 2 and 5; the table of shared-file edits is at the end). The roster `tools/relics/new_wave102/roster102.py` is the source: edit it and rerun the tools, not the generated files.

## NEW / REPLACED / KEPT

| | |
|---|---|
| NEW | all 29 relics below (9 God, 20 Special), their items, 3D / worn models, sprites, lang fragment, tests, tools |
| REPLACED | nothing |
| KEPT | every existing relic, grade, id, model and table; relics of the existing waves that look misgraded are only listed in section 6, not moved |

## God grade (9 relics; weapon damage 190-240)

### Weapons

| id | name | franchise | type (class) | slot | damage / speed | passive | acts | Tensura engravings |
|---|---|---|---|---|---|---|---|---|
| `moonfang_zangetsu` | Moonfang Zangetsu | Bleach | greatsword (greatsword) | main hand | 240 / 1.1 | 25% more damage on every blow; slowness for 3 seconds on the foe | Moon Cleave (use, 20 s, 6%); Crescent Fang (sneak + use, 70 s, 8%); Final Moon (the worn-relic key, 4 min, 12%, 3/day) | severance L3, magic_weapon, barrier_piercing, elemental_boost |
| `ruyi_power_pole` | Ruyi Power Pole | Dragon Ball | staff (staff) | main hand | 212.5 / 1.5 | 20% more damage on every blow; glowing for 6 seconds on the foe | Pole Vault (use, 14 s, 5%); Grow Long (sneak + use, 60 s, 8%); Sky Tower Smash (the worn-relic key, 4 min, 11%, 3/day) | crushing L3, swift, sturdy, vigor |
| `ackerman_twin_blades` | Ackerman Twin Blades | Attack on Titan | pair of blades (dagger) | main hand | 190 / 2.5 | 70% more damage against a foe with 100 health or more | Twin Flurry (use, 8 s, 4%); Nape Strike (sneak + use, 40 s, 6%); Ackerman Awakening (the worn-relic key, 4 min, 9%, 3/day) | severance L3, swift, vigor, barrier_piercing |
| `arashs_stella_bow` | Arash's Stella Bow | Fate | bow (bow) | main hand | 215 / 1 | 35% more damage against a foe with 100 health or more | Farsight Shot (sneak + use, 25 s, 5%); Meteor Arrow (the worn-relic key, 70 s, 8%); Stella (the worn-relic key, 4 min, 12%, 3/day) | barrier_piercing L3, swift, magic_weapon, elemental_boost |
| `hylian_hero_shield` | Hylian Hero's Shield | The Legend of Zelda | shield (shield) | main hand | 200 / 1.2 | 10% more damage on every blow; weakness for 3 seconds on the foe | Parry Stance (use, 40 s, 6%); Hero's Rebound (sneak + use, 70 s, 8%); Triforce Ward (the worn-relic key, 4 min, 10%, 3/day) | sturdy L3, magic_protection, severance_protection, holy_coat |

### Gear

| id | name | franchise | slot (look) | passives | acts |
|---|---|---|---|---|---|
| `perfect_susanoo_plate` | Perfect Susanoo Plate | Naruto | CHEST (PLATE) | +7 armour, +4 armour toughness, +10 maximum health, 40% less magic damage, +0.5 knockback resistance | Susanoo Guard (use, 40 s, 5%); Totsuka Verdict (sneak + use, 5 min, 9%, 1/day) |
| `joyboys_straw_hat` | Joyboy's Straw Hat | One Piece | HEAD (HAT) | +8 maximum health, +3 luck, +8% movement speed, 50% less fall damage, the lent skill thought acceleration | Rubber Dash (use, 30 s, 4%); Sun God's Laughter (sneak + use, 5 min, 9%, 1/day) |
| `greed_island_binder` | Greed Island Binder | Hunter x Hunter | CHARM (TOME) | +4 luck, +8 maximum health, +15% maximum magicules, the lent skill magic sense | Detect and Mark (use, 60 s, 3%); Restricted Slot: Reflect (sneak + use, 4 min, 7%); Card Storm (the worn-relic key, 20 min, 12%, 1/day) |
| `requiem_ladybug_brooch` | Requiem Ladybug Brooch | JoJo's Bizarre Adventure | CHARM (AMULET) | +10 maximum health, the lent skill self regeneration, 50% less wither damage, +2 luck | Return to Zero (use, 2 min, 6%); Golden Experience (sneak + use, 15 min, 10%, 1/day) |

## Special grade (20 relics; weapon damage 35-55)

### Weapons

| id | name | franchise | type (class) | slot | damage / speed | passive | acts | Tensura engravings |
|---|---|---|---|---|---|---|---|---|
| `rems_morning_star` | Rem's Morning Star | Re:Zero | morning star (club) | main hand | 46 / 1.2 | 20% more damage against a foe with 100 health or more | Oni Swing (use, 20 s, 2%); Chain Pull (sneak + use, 60 s, 3%) | crushing, sturdy |
| `explosion_wizards_staff` | Explosion Wizard's Staff | KonoSuba | staff (staff) | main hand | 44 / 1.3 | 10% more damage on every blow; sets the foe alight for 3 seconds | Small Blast (use, 25 s, 2%); Explosion! (sneak + use, 90 s, 5%) | elemental_boost, magic_weapon |
| `anneal_blade` | Anneal Blade | Sword Art Online | short sword (shortsword) | main hand | 41 / 1.7 | 20% more damage while you are at full health | Vertical Arc (use, 12 s, 2%); Quest Reward (sneak + use, 90 s, 3%) | swift, sturdy |
| `thorn_princess_stiletto` | Thorn Princess Stiletto | Spy x Family | pair of blades (dagger) | main hand | 35 / 2.2 | 30% more damage while you sneak | Rose Thrust (use, 20 s, 2%); Silent Step (sneak + use, 40 s, 3%) | severance, swift |

### Gear

| id | name | franchise | slot (look) | passives | acts |
|---|---|---|---|---|---|
| `four_leaf_clover_locket` | Four-Leaf Clover Locket | Black Clover | NECK (LOCKET) | +2 luck, +2 maximum health | Lucky Draw (use, 3 min, 2%) |
| `revival_fluid_pendant` | Revival Fluid Pendant | Dr. Stone | NECK (PENDANT) | +3 maximum health, 20% less wither damage | Revival Dose (use, 150 s, 2%) |
| `butterfly_estate_haori` | Butterfly Estate Haori | Demon Slayer | BACK (CAPE) | +3% movement speed, 15% less wither damage, the lent skill poison resistance | Flutter Step (use, 2 min, 2%) |
| `boar_hat_tavern_cloak` | Boar Hat Tavern Cloak | The Seven Deadly Sins | BACK (CLOAK) | +3 maximum health, 20% less cold damage | Warm Welcome (use, 200 s, 2%) |
| `shinobi_tool_belt` | Shinobi Tool Belt | Naruto | BELT (BELT) | +0.1 attack speed, +3% movement speed | Kunai Volley (use, 100 s, 2%) |
| `adventurers_cooking_belt` | Adventurer's Cooking Belt | Delicious in Dungeon | BELT (BELT) | +3 maximum health, +1 armour | Hot Meal (use, 3 min, 2%) |
| `e_rank_hunter_gloves` | E-Rank Hunter Gloves | Solo Leveling | HANDS (GLOVES) | +0.5 attack damage, +2 maximum health | Hidden Strength (use, 150 s, 2%) |
| `transmutation_circle_gloves` | Transmutation Circle Gloves | Fullmetal Alchemist | HANDS (GLOVES) | +1 armour, +0.5 attack damage | Clap and Transmute (use, 2 min, 2%) |
| `kuriboh_plush_charm` | Kuriboh Plush Charm | Yu-Gi-Oh! | CHARM (CHARM) | +1 luck, +2 maximum health | Multiply! (use, 200 s, 2%) |
| `pochita_pull_cord_charm` | Pochita Pull-Cord Charm | Chainsaw Man | CHARM (POTION) | +0.5 attack damage, +0.1 attack speed | Chainsaw Rev (use, 150 s, 2%) |
| `flower_field_charm` | Flower-Field Charm | Frieren | CHARM (AMULET) | +2 maximum health, +1 luck | Petal Spell (use, 160 s, 2%) |
| `magician_turban_circlet` | Magician's Turban Circlet | Magi | HEAD (CIRCLET) | +5% maximum magicules, the lent skill magic sense | Magoi Surge (use, 3 min, 2%) |
| `kijin_festival_mask` | Kijin Festival Mask | That Time I Got Reincarnated as a Slime | HEAD (PLATE) | +2 maximum health, 20% less fire damage | Festival Drum (use, 3 min, 2%) |
| `black_bulls_robe` | Black Bull's Robe | Black Clover | CHEST (ROBE) | +3 maximum health, +1 armour, 15% less magic damage | Never Give Up (use, 4 min, 3%) |
| `hestia_familia_leathers` | Hestia Familia Leathers | DanMachi | CHEST (LEATHER) | +2 armour, +2% movement speed | Falna Awakening (use, 200 s, 2%) |
| `weighted_training_boots` | Weighted Training Boots | Dragon Ball | FEET (GREAVES) | +0.1 knockback resistance, +3% movement speed, +2 safe fall distance | Weights Off (use, 150 s, 2%) |

## 3. What each looks like

**Weapons** (`tools/relics/new_wave102/weapons102.py`, the 1.97.0 weapon kit `new197_*.py`): each is a real baked mesh with a painted skin and a second additive glow skin, a 16 x 16 sprite rendered from the mesh for the inventory, and the item json that hands the drawing to the weapon renderer (the bow has its draw states, the shield is framed like vanilla's).

* **Moonfang Zangetsu** (`moonfang_zangetsu`): A huge black cleaver-sword: a wide single-edged blade with a pale moon-blue edge line, a crescent guard, a long bandage-wrapped hilt with a trailing chain and cloth.
* **Ruyi Power Pole** (`ruyi_power_pole`): A long red lacquered pole with gold end caps, a ring of gold bands, small rune marks along the shaft and glowing caps.
* **Ackerman Twin Blades** (`ackerman_twin_blades`): Two ultrahard steel blades crossed behind one grip: slim, slightly curved, each with a ring of the guard's crest, a spring-loaded hilt and a taut blue cord.
* **Arash's Stella Bow** (`arashs_stella_bow`): A tall bow of pale wood and white-gold, the limbs inlaid with sky-blue stars, a red-gold wrapped grip, a thin star-white string and a flame-tipped arrow.
* **Hylian Hero's Shield** (`hylian_hero_shield`): A heater shield of blue steel with a gold border, a red field, a gold three-triangle crest under a bird, a central sapphire and a strap behind.
* **Rem's Morning Star** (`rems_morning_star`): A short iron haft with a leather grip, a chain of heavy links and a spiked iron ball with pale blue gleam in its studs.
* **Explosion Wizard's Staff** (`explosion_wizards_staff`): A crooked dark-wood staff with gold bands and a large glowing crimson stone in a claw head.
* **Anneal Blade** (`anneal_blade`): A straight blue-grey single-edged short sword with a simple cross guard, a brown wrapped grip and a plain round pommel.
* **Thorn Princess Stiletto** (`thorn_princess_stiletto`): A long, very thin black stiletto with a red rose-bud guard, a thorn-wrapped grip and a small ruby pommel.

**God gear** (`worn102.py`, the worn relic library): each has its own multi-part signature model with emissive and pulsing glow parts and animated groups, a table of nine ambient particle layers and a visual for each act (`assets/nusmp/visual_upgrade/<id>.json`, `fx102.py`; every particle is one of the mod's own).

* **Perfect Susanoo Plate** (`perfect_susanoo_plate`): a violet plated cuirass with a glowing ribcage over chest and back, a spiked gorget with a sigil, tassets, horned layered pauldrons and chakra motes circling (3 pieces, 2600 vertices).
* **Joyboy's Straw Hat** (`joyboys_straw_hat`): a woven wide-brim straw hat with a red band, knot and cord, and behind it a sun halo with twelve turning rays and sparks drifting round the head.
* **Greed Island Binder** (`greed_island_binder`): a book on a chain at the hip: covers with gilt corners, a latch and a glowing emblem, six cards circling it.
* **Requiem Ladybug Brooch** (`requiem_ladybug_brooch`): a gold ladybug on the chest: split shell, six black spots, head and antennae, a glowing eye, an arrow-shaped pin, a turning rune ring and circling motes.

**Special gear** (16): the look of its slot (the shared worn model for the slot and look) plus its own small ornament `uq_<id>` of the generated waves (`gen_worn_unique.py`: two or three motifs picked and sized by the id), its own palette and its own sprite, so no two look alike. Held, the relic is shown as its worn model (the same route the generated waves 31 to 79 use).

## 4. How they work

* Weapons: the acts are the shapes of `Wave71Acts` (cone, line, nova, rush, blink strike, execution, ward, rally, pull, volley, storm, bolster), run inside `SafeCast.tick`, take foes only from `Fx.foesAround`, deal every blow through `Fx.damage` / `Fx.damageHeavy` (the balance law) and are multiples of the weapon's own base damage (`DMG_<ID> * k`). Each weapon's passive is a `Wave71Rules.Rule` in `Wave102Passives`, applied by `Wave102Events` on the blow being dealt (the arrow for the bow), only while the weapon is in the main hand and its flag is on (attuned for God). The shield raises like vanilla's (`ShieldSet`, `<id>_blocking.json`).
* Gear: attribute / resistance / lent-skill passives and acts from the vocabulary of `Wave31Acts` (heal, regeneration, buff, shield, area damage, bolt, dash, blink, cleanse, allies, mark, second wind, gale, pull). God gear needs attunement; Special never does (`AttuneRules`). Nothing here touches hunger or exhaustion.
* Loot: by grade through the existing relic pools (vault, pedestals, the Crucible, the God of Order's ledger), so the new relics enter every source that rolls God and Special. Each God relic is also placed on a dungeon road in `data/nusmp/mega/relic_themes.json`.
* Better Combat: a file in `data/nusmp/weapon_attributes/<id>.json` for each melee weapon (the bow and the shield have none).
* Everything is server-side logic (rule 9): the client only draws; no new packet; per-player state is the engine's own; effects go through the existing particle and effect budgets.

## 5. Tests and tools

JUnit `relics/wave/Wave102DefsTest` (exactly 9 God and 20 Special, none of the ids in another wave, every weapon's damage inside its grade's range, engravings from the Tensura list, acts, the lang fragment, the files of every relic, the tables of the God gear) and the existing `RelicRenderCoverageTest`, `RelicHandCoverageTest`, `WornCoverageTest`, `GradeTest`, `RelicDefTest`, `NoHungerDrainTest`, `ShieldClassesTest`, `WeaponMeshesTest` ... pass with the new relics. In-game self-test `relic.god_special_new` (registered by `Wave102.hooks`).

```
python3 -B tools/relics/new_wave102/gen102.py          # Wave102.java, Wave102Passives.java, tools/lang/god_special_new.json, class rows, theme rows, this page
python3 -B tools/relics/new_wave102/build102.py all    # the 9 weapon meshes / skins / sprites / item models, the 20 gear sprites / item models
python3 -B tools/relics/new_wave102/fx102.py           # the four effect tables of the God gear
python3 -B tools/relics/gen_worn_all.py                # the worn models + catalog (calls worn102.py through two hook lines)
python3 -B tools/relics/new_wave102/presets102.py      # the first-person display of the long weapons
python3 -B tools/relics/bow_vanilla.py && python3 -B tools/gen_shield_models.py && python3 -B tools/bettercombat/inventory.py && python3 -B tools/bettercombat/gen_attributes.py && python3 -B tools/relics/gen_curios_tags.py
```

## 6. Existing relics that look misgraded (reported, NOT moved)

None were moved or regraded (the owner's rule: do not move relics around unless they clearly belong in another rank). A quick pass over the 653 existing relics found only two that the owner may want to look at; both are opinions, neither was touched: `the_mad_kings_crown` (Special: a ruler's crown whose lore is God-flavoured, with the effects of its grade) and `mind_stone` (Legend: one of the six cosmic stones, the other stones of that set are graded by their own power).

Previews (a software rasteriser, not the game): `docs/previews/god_special_new/sprites.png` (the 29 sprites), `models/<id>.png` (the nine weapon meshes), `worn_*.png` (the four God gear models beside a plain figure).

## 7. Edits to files that already existed (all additions)

* `relics/RelicCatalog.java`: one `guard("Wave102", ...)` line in `init()` and one in `hooks()` (plus the import).
* `visual/shields/ShieldSet.java` (the id of the new shield appended), `tools/gen_shield_models.py` (the same id).
* `assets/nusmp/weapon_classes.json`, `tools/relics/weapon_classes.json`: 9 class rows appended. `data/nusmp/mega/relic_themes.json`: 9 God rows appended.
* `tools/relics/gen_worn_all.py`: two append-only hook lines (`worn102.register`, `worn102.catalog`); `assets/nusmp/relic_worn/catalog.txt`: 24 lines appended by that generator.
* `data/curios/tags/item/{necklace,back,belt,hands,charm}.json`: the new Curios relics appended by `gen_curios_tags.py`.
* `tools/bettercombat/manifest.json` and the Better Combat attribute files, `tools/relics/display_presets_manifest.json`: rows for the new melee weapons.
* Not touched, as asked: the version, the changelog, the README, LIBRARIES, `en_us.json` (the lead merges `tools/lang/god_special_new.json`), `.github`, the Time Magic VFX.

## 8. What could not be verified

Nothing can be run in a game here. Unseen in the game: every model, skin, glow map and sprite (the previews are a software rasteriser), the held pose and size of each weapon, the bow's draw pose, the shield's raised pose, the worn models on a real body (position, clipping, the ensemble of the Susanoo plate sits exactly on its 2600-vertex cap), the particle layers and act visuals, the act numbers against a real boss, the passive hooks, the loot rolls and the dungeon roads. The Java was type-checked against API stubs (no real compile) and the JUnit tests ran in a shim without Minecraft. The Special gear is held as its worn model (no separate hand-only model). The shared worn atlas was not regenerated (this machine's image libraries paint it slightly differently), the committed atlas is used as it is.
