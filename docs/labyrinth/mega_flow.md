# Mega-Dungeon: the Rukh flow of the Holy floors and the decennial Metal Vessel (`mega.flow`, 1.55.0, builder FLOW)

Package `com.newuniverse.nusmp.labyrinth.mega.flow`. **Nothing here was run in game.** The pure classes are unit tested (JUnit, 62 tests, every rule mutation
checked, section 11), the game-side classes were type-checked against the project's API stubs (which are incomplete: section 11), the classification
table is checked by `AlignmentDataTest` against the REAL list of magic families and the REAL page ids of the sources, and five IN-GAME self-tests (`flow.*`,
section 10) are registered with the shared `SelfTests` registry so the CI server smoke test exercises the real thing. CI is the first real compile.

The owner's brief (system 4, "Holy Ascensions: the Magi Rukh ecosystem"): the air of the Holy floors is dense with Light Rukh; a player who casts magic that
matches the flow has their damage "massively multiplied"; opposing magic (Dark, Demonic) is "violently rejected by the environment, draining their health";
and the Holy boss of every tenth floor drops a guaranteed Metal Vessel, "a unique artifact granting elemental flight and massive magic buffs via a Djinn equip".

## 1. New, replacement, kept (owner rule 5)

| Part | Status | Notes |
|---|---|---|
| Magic alignment table (`alignments.json`, `MagicAlignments`, `AlignmentReload`) | NEW | Nothing classified a cast as Light or Dark before. |
| Flow rules, cast tracker, damage multiplier, rejection, ambient Rukh | NEW | Per road of the Mega-Dungeon. |
| Decennial Metal Vessel (`DecennialReward`, `FlowData`, `VesselRules`, `RewardLog`) | NEW drop of an EXISTING item class | The vessel relics of 1.24 (Legend and above, flagged `vessel()`); nothing new is registered. The one owner-requested exception to "Unique-or-higher relics come only from Crucibles". |
| `/multiverse mega flow` (`status`, `classify`, `vessel list`, `vessel give`) | NEW | The lead attaches it. |
| Rukh particles (`nusmp:light_rukh`, `nusmp:dark_rukh`, `RukhFx`, the 731-file purge and its build test) | KEPT, untouched | Only CALLED through `RukhFx`, which budgets the density. No vanilla particle, no new particle type, no texture. |
| The 1.36.0 `EconomyDrops`, the Guardian's own empty loot, `RelicLoot`, `UniqueLedger`, the Djinn system | KEPT, untouched | Used through their public methods. |
| Two lines in `GrimoireBook.castPage` | SHARED FILE, for the lead | Section 8. |
| Nothing was replaced and nothing was removed. | | |

There are no blocks, items, entities, textures, models or recipes in this module: every effect is code, one JSON file, and particles that already exist.

## 2. What it does, in one page

* **Where is the flow.** A place has the flow of its road (`MegaDungeon.polarityAt`): the Holy road is LIGHT (config `holyFlow`), the Abyssal road has NONE
  (config `abyssalFlow`, default `none`; `dark` mirrors the rule), Floor 0 and everything outside a sector have none.
* **What is a cast.** Every grimoire page that finishes successfully, and every other ManasCore skill (Tensura skills, relic-granted skills) that a player
  activates. The id of the page, the family, or the skill is looked up in the table and the cast is LIGHT, DARK or NEUTRAL. The cast is remembered per player
  with its game time.
* **A matching cast boosts.** Damage a player (or their summon, or their arrow) deals in a place with a flow, within `castWindowTicks` (30) of a cast that
  matches it, is multiplied by `holyMultiplier` (2.5). A few Rukh of the flow's colour gather on the target.
* **An opposing cast is rejected.** At the moment of the cast, in a place whose flow is the opposite colour, the player loses `rejectPercent` (8 percent) of their
  maximum health, at least `rejectMinHealth` (2 HP), at most once per `rejectCooldownTicks` (20), and by default never below 1 HP. A line on the action bar, the
  caster's Rukh scattering, the place's Rukh closing in, two sounds. Optionally the cast's damage is cancelled as well (`rejectCancelsDamage`, default off).
* **A neutral cast does nothing,** except that it replaces the last cast, so a plain spell ends the window of an earlier Light one.
* **Ambient.** Every 2 seconds a few Rukh of the flow's colour drift around each player who stands in a place that has a flow.
* **The Metal Vessel.** When the Guardian of a HOLY sector dies (its tenth floor), one relic that can hold a Djinn and that nobody owns yet drops at its corpse,
  stamped through the Order's ledger, once per sector.

The two pictures `flow_matrix.png` (what a cast does on each road, with the numbers) and `flow_families.png` (every magic family by alignment) are drawn from
the real files by `tools/labyrinth/mega_flow_table.py`; see section 12.

## 3. The numbers

| What | Default | Range | Config key (`nusmp-labyrinth-mega-flow-server.toml`, `[flow]`) |
|---|---|---|---|
| Matching cast multiplier | x2.5 | 1 to 10 | `holyMultiplier` |
| A hit counts as coming from a cast for | 30 ticks | 1 to 600 | `castWindowTicks` |
| Rejection | 8 percent of maximum health | 0 to 100 | `rejectPercent` |
| ... but at least | 2 HP (one heart) | 0 to 100 | `rejectMinHealth` |
| Rejection cooldown per player | 20 ticks | 0 to 1200 | `rejectCooldownTicks` |
| Can a rejection kill | no (stops at 1 HP) | | `nonLethal` |
| Does a rejected cast's damage vanish | no | | `rejectCancelsDamage` |
| Holy road's flow / Abyssal road's flow | `light` / `none` | `light`, `dark`, `none` | `holyFlow`, `abyssalFlow` |
| Ambient Rukh on / count per puff | on / 3 | 0 to 12 | `ambient`, `ambientCount` |
| Read non-grimoire skills as casts | on | | `skillHook` |
| The Guardian drops the Metal Vessel | on | | `vesselEnabled` |
| The whole feature | on | | `enabled` |

Worked examples: 10 damage becomes 25. A 20 HP player loses 2 HP per rejection (8 percent is 1.6, raised to the 2 HP floor), a 100 HP player 8 HP, a 1000 HP
player 80 HP. At 4 HP a rejection takes 2; at 2 HP it takes 1; at 1 HP it takes nothing.

## 4. The WHY of the decisions

* **Why the answer is data, with keywords and exact entries.** The mod has 105 magic families, 632 distinct spell-page ids, 362 Tensura skills and magics
  and relics added by many hands over many versions; no `switch` over any of them survives the next update. The rules are three, applied in this order:
  (1) an exact entry for an id (a family such as `lightning`, a page such as `devil_union`, a Tensura skill) is final, because only a person can know that
  Curse-Warding is a ward and not a curse; (2) otherwise the id is cut into WHOLE words and a word of the `dark` or `light` list decides, so `lightning` and
  `lighten` are not "light"; (3) anything else is NEUTRAL. A datapack can retune all of it.
* **Why DARK beats LIGHT when both match.** `fallen_angel_gate` has the Light word `angel` and the Dark word `fallen`; `veil_of_black_light` has `light` and
  `black`. A half-demonic spell must not slip through the holy air as light, so the safer reading wins.
* **Why NEUTRAL is the default, and the empty or broken table.** A cast that nobody classified must not be boosted or punished by accident. A missing or
  unreadable file installs the empty table, which classifies everything NEUTRAL: the feature then does nothing instead of doing something wrong.
* **Why the first id with an entry decides alone.** A cast is asked as "page, then family" (`devil_union`, `light`). A page entry must beat its family
  (Devil Union is a devil's pact whichever grimoire it is written in) and a family entry must beat the keywords of its pages (a Light-family page called
  `dark_prison` stays Light, a Dark-family page called `light_prison`, which is Demon Light Magic, stays Dark).
* **Why the table follows the mod's own soul groups.** The mod already groups the families by soul (`MagicType.soulType`): the LIGHT soul (Light, Lightning,
  Sword) and the DARKNESS soul (Dark, Shadow, Poison, Eyeball, Curse, the four Demon magics, Demon Beast, Black Oil, Smoke, Soul, Soul Corpse). Those 3 and 14
  families are the table's base. Blood Magic (a Battle-soul family that is the devils' blood art) and Anti-Magic (the devil Liebe's magic, restricted by the
  mod to devils and admins) are added as Dark by entry. The families that DRAW Dark Rukh particles (Mercury, Sand, Spatial, Gravity, Bone, Illusion, Ash, Cotton,
  Barrier, Mud, Permeation, Sandstone, Switching, Kotodama) are NOT Dark: that is how they look, not what they are, and rejecting a Gravity mage for the colour
  of the sparks would make half of the mod's casters unwelcome in the Holy floors. The picture marks them with a dot.
* **Why Lightning and Sword are Light.** They share the Light soul with Light Magic, and a player whose soul is Light can awaken any of the three; if only Light
  Magic were boosted, two thirds of the Light-soul players would get nothing from the floors made for them. Say the word and each is one line of the file.
* **Why the keyword lists are short and whole-word.** Read against all 632 page ids and the 362 Tensura ids, the keywords touch exactly 79 page names
  (almost all inside families that decide by entry anyway) and 38 Tensura ids. Reading them found two false hits that are now entries: the Raging Black Bull
  (the Black Bulls are a squad, so the word `black` must not touch it) and Tensura's physical Poison toggle (which shares a path with the Darkness-soul Poison
  Magic). `undead` is deliberately NOT a keyword, so a future "Turn Undead" is not Dark; Tensura's two undead-summoning spells are Dark by entry instead.
  `AlignmentDataTest` pins the 79 names: a new page named after light or darkness fails the build with its name until somebody decides.
* **Why the multiplier is applied in `LivingIncomingDamageEvent`.** It is the first of NeoForge 1.21's three damage events and carries the RAW hit before armour
  or resistance, so "the target's own reductions still apply after the boost" costs no code, and a shield, ward or cap that the mod already has sees the boosted
  number and can answer it. It runs at HIGH priority so those handlers (default priority) are behind it.
* **Why the window, and why two steps.** A damage event does not say what caused it, so the flow remembers the last cast of each player and treats a hit as
  "from that cast" for 30 ticks. A spell that hurts instantly deals its damage INSIDE the call that casts it, before anything can report the cast finished, so a cast
  has two steps: `beforeCast` (just before the page runs; it colours this tick's damage only and opens no window) and `onCast` (after the page succeeded; opens the
  window and may reject). A page that fails therefore never costs a rejection. The limits are honest: a spell that keeps hurting for six seconds is boosted for the
  first one and a half, and any damage the player deals inside the window (a sword swing, an arrow) is boosted, as the brief says "their damage".
* **Why bosses keep their damage budget, and how (a correction of the brief).** The brief assumed the boss's per-second budget (`BossBudget`) cuts the damage after
  this event. The code says the opposite: a budgeted boss's own `hurt` draws the ORIGINAL hit from its budget and then calls `super.hurt`, which fires the event, with the
  budget bypassed. So a boost would slip past the cap. `FlowEvents` therefore draws only the EXTRA (boosted minus original) from the same budget (`FlowRules.chargeExtra`):
  an empty budget leaves the hit as it was, and a boost can never push a boss past its cap. A boss with no budget (the Floor Warden family) takes the full multiplier.
  A boosted hit on a player is held under the share of health that the balance law's one-hit cap allows (`BalanceLaw.cap`, players only: for a mob it would apply the mob
  damage multiplier a second time).
* **Why the attacker is the root cause.** The owner of an arrow or of a summon is credited (`source.getEntity()` and `OwnableEntity`), the target must not be the caster,
  an ally, or a creature the caster owns, and the caster must stand in a place with a flow.
* **Why a rejection takes health directly, and why it is ABSOLUTE.** Setting the health leaves no damage event, so armour, the Resistance effect, Totems and the **Rare
  grade's magic resistance of the gear module** (which reduces vanilla `magic` and Tensura's spell types by up to 40 percent) cannot shrink it. The place's rejection is not a
  spell, and a price that a full Rare set halves would stop being a price. The only case that uses a damage event is a drain that would really kill, possible only with
  `nonLethal = false`: it goes through the normal death path with `generic_kill`, a type that vanilla's own tags list in `bypasses_armor`, `bypasses_resistance` and
  `bypasses_invulnerability` (checked in the 1.21.1 data) and that the gear rule does not count as magic. A drain therefore costs exactly what `FlowRules.rejectionDrain` says.
* **Why non-lethal by default.** "Violently rejects" is a price, not an execution: a cast is not suicide, and a server owner who wants a harsher place sets `nonLethal = false`.
  Creative and spectator players are exempt, and the cooldown stops five Dark spells in a row from costing five rejections.
* **Why the Abyssal road is neutral by default.** The brief defines only the Holy side. `abyssalFlow = "dark"` mirrors the rule in one line (Dark magic boosted, Light
  rejected) for an owner who wants the Descent to favour Dark magic; it is the open design question of this module (section 13).
* **Why the Metal Vessel is an existing relic issued through the ledger.** The mod already has the item class: a relic of Legend grade or above that the catalog
  flagged `vessel()`, which a Djinn Seal can be bound into (`VesselAccess.isVessel`). Making a new item would duplicate it. Every relic is one of a kind in the world,
  guarded by the Order's ledger, so the reward picks among the vessels the ledger says nobody has (`UniqueLedger.liveIds`), makes a stack with `RelicCatalog.newStack`
  and stamps it with `UniqueLedger.issue`, exactly as `RelicLoot.rollUnissued` does for chests and Mimics. `RelicLoot` has no filter parameter, so the same two calls are used
  with the vessel filter; nothing of the ledger is re-implemented. The draw is weighted by the mod's own loot weights of the grades (Legend 4, God 1.5, Genesis 0.5).
* **Why once per sector, in its own saved data.** A Guardian can die twice as far as the server can tell (a re-forge at its altar, an admin command, a replayed save), and a
  second vessel from one sector would be a broken promise. `FlowData` (`data/nusmp_flow.dat` in the overworld's storage, the same always-loaded place as the Order's
  ledger) keeps the sector ids that paid and the relic each one paid. `Sector` is not touched (its saved format belongs to the generator).
* **Why the reward ignores the `doMobLoot` game rule.** It is a guaranteed boss prize, not mob loot: the Guardian drops nothing else (its own loot table and custom loot are empty,
  the Mana Core comes from its altar). The death event fires whatever the rule says.
* **Why a test Guardian pays nothing.** The Guard builder writes `nusmp_sector = -1` on a Guardian that belongs to no sector (an admin's test). A Guardian that carries the key
  belongs to exactly that sector; only a Guardian WITHOUT the key falls back to the sector whose region holds the corpse.
* **Why the item is a glowing, invulnerable ground drop.** It cannot burn, drown in lava or be blown up, never despawns, and glows so it can be found in the dark of the arena. It is
  not restricted to the killer: a party decides, and the last hit should not own the prize.
* **Why the second door is added when the server has started.** ManasCore calls its skill listeners in the order they were added; the ones that refuse an activation (the spell
  seal, the boss rules) are added while the mod is constructed. A listener added after the server started runs after them, so a refused activation is never counted.

## 5. Classes (the WHY of each)

| Class | Pure? | Why it exists |
|---|---|---|
| `Alignment` | pure | The three words (LIGHT, DARK, NEUTRAL) every part speaks; separate from `RukhFx.Alignment`, which has only the two particle colours. |
| `MagicAlignments` | pure | The reader of the table: exact entry, whole-word keyword, dark-wins, neutral; merged magics part by part; `explain` says which rule spoke; `parse` never throws and returns the problems. |
| `AlignmentRegistry` | | The one volatile reference to the table the last reload installed (the empty table before the first). |
| `AlignmentReload` | | The `ResourceManagerReloadListener` that reads `data/nusmp/mega/flow/alignments.json`, hands the tree to the parser, logs every complaint and installs the table in one step. |
| `FlowRules` | pure | Every number and decision: flow of a road, outcome of a cast against a flow, the multiplier and its clamp, the boss-budget charge, the player cap, the drain and its non-lethal floor, the cooldown. |
| `FlowTracker` | pure | Per player: the last cast and its tick, the running spell of this tick, and the stamps that keep the effects from nagging. Cleared on logout and server stop. |
| `FlowCasts` | | The two doors that hear casts (the grimoire hook and the ManasCore skill listener), the classification, the tracker and the rejection. |
| `FlowEvents` | | The incoming-damage handler and `adjust`, the whole decision with the road passed in (so the self-tests drive it), boss budget and player cap, the Rukh on a boosted hit. |
| `Rejection` | | The one place that applies a rejection: absolute drain, flinch, action bar, Rukh, sounds. |
| `FlowAir` | | The 40-tick ambient puff of Rukh around players in a place with a flow. |
| `FlowConfig` | | The `[flow]` config, plain `define` / `defineInRange`, with `get()` answering the default while the spec is not loaded. |
| `DecennialReward` | | The Metal Vessel: recognises the Guardian by its tag, chooses and issues the vessel, drops it, announces it, fires `mega.vessel`. |
| `VesselRules` | pure | What may be a vessel (four facts) and the weighted draw. |
| `RewardLog` / `FlowData` | pure / | The once-per-sector memory and its saved-data wrapper. |
| `FlowCommand` | | `status`, `classify`, `vessel list`, `vessel give`. |
| `FlowSelfTests` | | The in-game self-tests. |
| `FlowRegistry` | | `init` (self-tests), `register` (game-bus handlers), `command()`. |

## 6. The data file `data/nusmp/mega/flow/alignments.json`

Why a file: the owner can retune a family, add a word or move a magic to neutral without a mod update, and a datapack that ships the same path replaces the whole file. It
carries a `why` string (JSON has no comments).

```json
{ "why": "...",
  "light": ["light", "holy", "sacred", "solar", "divine", "radiant", "angel", "angelic", "seraph", "saint", "paladin"],
  "dark":  ["dark", "darkness", "shadow", "demon", "demonic", "devil", "curse", "cursed", "hell", "hellfire", "death", "blood", "black", "necro", "necromancy",
            "evil", "abyss", "abyssal", "fallen", "yami"],
  "exact": { "light": "light", "lightning": "light", "sword": "light", "dark": "dark", "...": "...", "curse_warding": "neutral", "tensura:poison": "neutral" } }
```

Keywords are ONE word of letters and digits (a bad one is skipped and reported); an `exact` key is an id (lower-case letters, digits, `_ : . / +`), its value `light`, `dark`, `neutral`
or `none`. An id is tried whole first, then without its namespace, so `light` also names `tensura:light`; a namespaced entry (`tensura:poison`) is tried first and overrides it.
Ids are normalised (lower case, spaces and dashes as underscores), and a merged magic (`merged:flame+poison`) is asked part by part, the strongest part (DARK, then LIGHT) speaking.

**The result over the real mod** (read by `AlignmentDataTest` and drawn in `flow_families.png`):

* **105 families: 3 Light** (Light, Lightning, Sword), **16 Dark** (Dark, Shadow, Poison, Blood, Anti-Magic, Demon Beast, Eyeball, Curse, Demon Fire, Demon Ice, Demon Light, Demon Water,
  Black Oil, Smoke, Soul Corpse, Soul) and **86 Neutral**, each listed by name in the test.
* Pages: outside the Light and Dark families only `devil_union` (every grimoire), `black_lightning_battle_fiend` and six pages with a dark word in the name (`evil_eye`, `demon_king`,
  `death_scythe`, `fallen_angel_gate`, `myriad_black`, `arresting_dark_weave`) are Dark; no page outside the Light families is Light; `curse_warding`'s pages and `raging_black_bull` are kept Neutral by entry.
* Tensura's 362 skill and magic ids (the owner's 2.0.1.3 reference): 14 Light, 24 Dark, 324 Neutral. Examples are in the picture. Passive resistance skills (`tensura:holy_attack_resistance`) match a word but are never activated, so they never reach the hook.

## 7. The command (`FlowRegistry.command()`, permission 2; the lead attaches it under `/multiverse mega`)

* `flow status [player]`: the config in one line; the sector, road and floor the player stands on and the flow there; their last cast, its alignment, its age against the window and what it is worth here; the table's size.
* `flow classify <ids...>`: the alignment of a cast named by one or more ids, most specific first (`flow classify devil_union light`), AND which rule decided. The way to tune the file.
* `flow vessel list`: how many relics can be handed out as a Metal Vessel by grade, which are free, which are already in the world, and which sectors have paid.
* `flow vessel give <player>`: runs the reward logic for the sector the sender stands in and gives the stack to the player. It skips the once-per-sector memory and the switch and never
  writes to the memory: an admin hand-over, for tests and for making good a lost vessel.

All text goes through `message.nusmp.mega.mfw.*` keys with an English fallback in the call, so the command reads fine before the lang fragment is merged.

## 8. What hears a cast (the hook coverage), and the two lines for the lead

`GrimoireBook.castPage` is the ONE place a grimoire page is run (private, no subclass overrides it; `QuestHooks.onCast`, which reports `spell.cast`, is called from the same spot, and
`QuestHooks.onCast` is called from nowhere else). Every grimoire book extends `GrimoireBook`: the 75 books registered in `NUSkills` and the 30 standalone attributes of
`WikiBooks.newAttributes()` cover all 105 families, plus every merged book (`MergedBook`, 64 slots). So **all 105 families reach the hook** (read from the sources, not run).

Not covered, and why: (1) Copy Magic runs another book's page inside its own cast: it is classified as Copy (neutral), so copying a Dark page escapes the rejection; (2) the Avatar's body
double casts through its own `SimulatedPlayerCaster`; (3) relic actives and Djinn moves are not casts of either kind; (4) a ManasCore skill whose activation is refused by a veto listener
is not counted (the listener is added after the vetoes), but a skill that is accepted and then fails inside is counted. The Tensura skills and the mod's own non-grimoire skills reach the
second door (`SkillEvents.ACTIVATE_SKILL`); only Light and Dark ones are remembered, because a utility skill or a toggle is not a spell and must not end a Light window.

**Lines for `book/GrimoireBook.java` (a shared file), inside `castPage`:**

```java
// 1) immediately BEFORE the line that begins:  if (!com.newuniverse.nusmp.core.magic.profile.SafeCast.run(magicId(), gameTime, player, () -> p.cast().cast(this, instance, player, mode))) {
        com.newuniverse.nusmp.labyrinth.mega.flow.FlowCasts.beforeCast(player, magicId(), p.id());      // 1.55.0: the Rukh flow: this tick's damage comes from this page

// 2) immediately AFTER the line:  com.newuniverse.nusmp.quests.QuestHooks.onCast(player, String.valueOf(castMagic), p.id(), p.costPercent());
        com.newuniverse.nusmp.labyrinth.mega.flow.FlowCasts.onCast(player, magicId(), p.id(), p.costPercent());      // 1.55.0: the Rukh flow: a finished cast is Light, Dark or neutral; an opposing one is rejected in a Holy place
```

`magicId()` (not the dominant parent that `QuestHooks.onCast` gets) so a merged magic is classified part by part. Both calls return at once outside the Mega-Dungeon and never throw.
Without these lines only the second door works (non-grimoire skills); the status command says so ("the two FlowCasts lines in GrimoireBook.castPage are not wired").

## 9. The Metal Vessel

* **Who drops it.** A living entity in the Mega-Dungeon carrying the scoreboard tag `nusmp_sector_guardian` (the Guard builder's contract: tag `nusmp_sector_guardian` and persistent int
  `nusmp_sector`, read in `LabyrinthGuardianEntity`), whose sector is HOLY and `FloorMap.isDecennial(sector.lastFloor(), sector.perRegion)` (always true for a sector's last floor, kept so the rule
  reads as the brief does), with a player credited for the kill (the attacker, the owner of the summon that struck, the kill credit, else the nearest living player within 64 blocks).
* **What it drops.** One relic chosen among the candidates the ledger does not hold: a relic that is one of a kind (`isUnique`: not a consumable), flagged `vessel()`, of a grade that
  `djinnCapable()`, and not half of a pair. **68 relics qualify by the sources** (52 Legend, 14 God, 2 Genesis; the two flagged consumables `devil_contract` and `scroll_of_absorption` do not: the ledger cannot track
  them). By the mod's grade weights a draw is Legend 90.4 percent, God 9.1 percent, Genesis 0.4 percent. The runtime count is authoritative (`flow vessel list`, self-test `flow.vessel`).
  The candidates: aegis_of_the_abyss, amulet_of_dragon_rage, amulet_of_the_coven, amulet_of_the_forest, armor_of_plot, armor_of_the_lionheart, astralaria, bags_of_transferrence, bloodbound_katana, brooch_of_poeur,
  cape_of_nine_lives, cloak_of_levitation, coat_of_the_duelist, crest_of_foresight, dante_gravity_crown, delphynes_cursed_armor, divine_mercy, dragons_fire_gauntlet, eschaton, flamecutter, frostfang, gold_circlet,
  hellfire_gauntlet, helm_of_whispers, hemlocks_amulet, heros_shield, holy_handgrenade, horn_of_leroy, hunger, leviathan_axe, mind_stone, needler, null_blade, palladion, ring_of_power, scepter_of_the_double_moon,
  silver_circlet, skycrystal_armor, steel_circlet, suit_of_the_astral_overseer, suit_of_the_golden_shadow, sunheart_medallion, the_amulet_of_lost_souls, the_bifrost, the_forgotten_ring, time_hammer, tripolar_sword,
  wallet_of_the_slayer, windcutter, witchers_medallion, zar_sancuis_plate_of_the_red_prophet, zenon_spatial_ring (Legend); deck_of_many_things, eye_of_vecna, godking_crown, greaves_of_the_wind_walker, hand_of_vecna,
  paradox, rose_petal, the_mind_stone, the_power_stone, the_reality_stone, the_soul_stone, the_space_stone_or_the_tesseract, the_time_stone_or_the_eye_of_agamotto, warrens_key (God); astralis_the_eye_of_the_universe,
  the_infinity_gauntlet (Genesis).
* **If none is left,** nothing drops and the killer is told ("every relic that can carry a Djinn is already in the world"); never an exception. The sector does NOT record a payment then.
* **Flight and the buffs.** The Metal Vessel is a container, as in the lore: the Djinn's power sits in the wearer's personal item (sword, ring, bracelet, necklace). The elemental flight and
  the magic buffs come from the EXISTING Djinn system once a Djinn Seal is bound into the vessel: the Full Djinn Equip (Maso) of `DjinnEngine` gives the attribute boosts and "a bounded, safe flight".
  This module does not add a Djinn Seal to the drop (the brief does not ask for one); that is a one-line follow-up if the owner wants the Guardian to hand over a Seal too.
* **Quest key `mega.vessel`** (arg = relic id) is fired for the receiver of a real reward (not for an admin hand-over).
* **The one exception.** Unique-or-higher relics come from Crucibles only; this drop is the single, owner-requested exception, and it still goes through the ledger so a vessel can never exist twice.

## 10. Self-tests (registered by `FlowRegistry.init`, run by `/multiverse mega selftest`)

| Key | What it drives |
|---|---|
| `flow.classify` | The data reload installed the table; known ids classify as this doc says; the 105 real families split into light, dark and neutral. |
| `flow.multiplier` | A FakePlayer: no cast, a stale cast and a neutral cast change nothing; a matching cast multiplies by the configured factor; an opposing cast is a rejection; an ally is never boosted. With sector 0 generated and HOLY: a genuine `LivingIncomingDamageEvent` through the real handler is multiplied; the real cast hook remembers casts, a matching cast costs nothing, an opposing cast takes the configured share once (the cooldown holds), and at 1.5 HP it never kills. The player's health is restored. |
| `flow.neutral_outside` | No road (Floor 0, the overworld), the Abyssal road at its configured flow: damage is as the rules say (unchanged by default). |
| `flow.vessel` | The candidate list is non-empty and every one is `djinnCapable`; the free ones are exactly those the ledger does not hold; the draw is valid at both ends. **No vessel is issued.** |
| `flow.reward_log` | The once-per-sector memory survives NBT; the reward's gate answers for made-up sectors (Holy, Abyssal, none) without touching saved data. |

A test whose switch is off in the config reports an OK "skipped", never a failure. A FakePlayer is not in `level.players()`, so the tests pass it explicitly.

## 11. What was checked, and what was not

Checked here: the 62 pure JUnit tests pass with the local JUnit shim (`MagicAlignmentsTest`, `AlignmentDataTest`, `FlowRulesTest`, `FlowTrackerTest`, `VesselRulesTest`). **Mutation check:** 19 deliberate
breakages of the rules and the data (dark no longer beating light, exact entries ignored, keywords matching substrings, a match becoming a rejection, the drain able to kill, the cooldown off by one,
a multiplier below 1, the boost cap lowering the hit, the boss budget ignored, the window one tick short, a logout keeping the last cast, a stale running spell counting, a pair half becoming a vessel,
the draw boundary, a sector paying twice, and four edits of the data file) were each caught by at least one test. The type-check of the whole package against the API stubs shows only the three known
stub artifacts (`new BlockPos(int,int,int)` twice and `new ItemEntity(...)` reported as abstract); a deliberately wrong file (wrong arguments to `RukhFx.burst`, a missing method on the damage event, a wrong
`DamageContainer` constructor, wrong argument types) is rejected by the same check, so it does look at these calls. The Python port of the rules in the picture script was compared with the Java class on 1111
verdicts (every family, every page with its family, every Tensura id, merged ids): no difference. No vanilla particle API is used anywhere in the package (searched), so the Rukh purge rule is respected; that build test itself was not run.

NOT checked (nothing ran in game): that the two hook lines exist in the build (they are the lead's); the real damage numbers against the mod's many mitigation handlers; the feel of the rejection and the Rukh;
that `AddReloadListenerEvent` fires before the first cast of a world (it does at server start by the NeoForge contract); the FakePlayer's behaviour in the self-tests beyond what its source says; the three
Guard-builder keys on a real Guardian (read in the Guard builder's worktree, not run); the vessel list at runtime (counted from the sources by a script that reads `RelicDef.of(...)` chains).

## 12. References and what is still a guess

* The pictures: `python3 tools/labyrinth/mega_flow_table.py --out <dir>` writes `flow_matrix.png` and `flow_families.png` from the shipped JSON, `MagicType.java`, `FlowRules.java` and the page ids of the sources.
* Verified online (by search results; the fan wiki pages themselves could not be opened from this build machine): Magi fan wiki, "Rukh" and "Metal Vessel" (https://magi.fandom.com/wiki/Metal_Vessel,
  https://magi.fandom.com/wiki/Djinn): white Rukh is the life essence that flows through all living things, black Rukh is its opposite and feeds on hatred and malice; Metal Vessels use the white Rukh as food and
  the Dark Metal Vessels use dark Rukh; a conquered Djinn resides in the conqueror's metallic accessory, and Djinn Equip is a full-body transformation (https://evil.fandom.com/wiki/Black_Rukh,
  https://en.wikipedia.org/wiki/Magi:_The_Labyrinth_of_Magic). The owner's own `magi_djinn_system_guide.md` says the same. This is why LIGHT is the "white Rukh" side of the table and DARK the "black Rukh" side.
* Verified online by reading the source: NeoForge 1.21.1 `FakePlayer` (https://raw.githubusercontent.com/neoforged/NeoForge/1.21.1/src/main/java/net/neoforged/neoforge/common/util/FakePlayer.java: `displayClientMessage` is a no-op, `isInvulnerableTo` is true, it has
  a dummy connection), `DamageContainer` and `LivingIncomingDamageEvent` (same repository, `common/damagesource/DamageContainer.java`, `event/entity/living/LivingIncomingDamageEvent.java`: public constructors, so a self-test can build a real event) and vanilla's damage-type tags
  (https://raw.githubusercontent.com/misode/mcmeta/1.21.1-data/data/minecraft/tags/damage_type/bypasses_armor.json and the `bypasses_resistance`, `bypasses_invulnerability` files: `generic_kill` is in all three).
* Still a guess: that Lightning and Sword Magic should count as Light (the soul group says so; the lore of the owner's brief says only "Light/Holy"); that the Abyssal road should have no flow; the multiplier 2.5 and the rejection 8 percent (the brief says "massively" and "violently"; both are one config line).

## 13. For the owner and the lead

* **Decision for the owner:** the Abyssal road has no flow by default (the brief defines only the Holy side). `abyssalFlow = "dark"` makes Dark magic x2.5 and Light magic rejected there.
* **Lead integration lines** are in section 8 (the two lines in `GrimoireBook.castPage`) and in the report: config registration, the command, the lang fragment `$S/mega/frag/mfw.lang.json`, the `flow` prefix for `SMOKE_EXPECT` in `.github/workflows/smoke.yml`, the quest key `mega.vessel`.
* **Interplay with the gear module** (read in its `MagicDamage` and `GearEvents`): the Rare grade's magic resistance reduces the damage of a boosted hit on a PLAYER target (a PvP case) but never the rejection, which is absolute (section 4).
