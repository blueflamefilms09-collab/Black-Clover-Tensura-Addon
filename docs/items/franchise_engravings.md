# Franchise engravings (1.111.0)

The owner's request: *"also add custom engravements based on the animes the relic, magic and etc come from"*.

**Nothing here has been seen in a game.** It was written without a running game; the pure rules and the data are unit-tested, the handlers are type-checked and a CI self-test (`item.franchise_engravings`) exercises the real registry and a real damage event, but no engraving has been felt in play (see the last section).

## What it is

Tensura Reincarnated's engravings are enchantments (the tag `#tensura:engraving`: severance, barrier piercing, holy weapon ...). This release adds the mod's OWN engravings, themed by franchise: **33 franchises, 71 engravings** (2 or 3 each). Every one is a real 1.21.1 datapack enchantment `nusmp:fr_<name>` (`data/nusmp/enchantment/`), listed in `data/tensura/tags/enchantment/engraving.json` (an additions tag, `replace: false`; the two older entries stay) so Tensura's engraving screens and the grindstone rules treat them like its own.

What an engraving DOES is code, driven by the numbers of `data/nusmp/franchise/engravings.json`: the datapack `effects` of each enchantment are `{}` (the same shape as the two older `nusmp:` engravings). Vanilla enchantment effect components were not used on purpose: the effects are stateful (cooldowns, stacks, charges, per-player memory), cannot be tested offline, and a malformed component would fail the whole datapack load; the code path is capped, tested and cannot break a world.

An item of the mod that has a franchise carries that franchise's engravings, put on once by `franchise/FranchiseEngravings` (called from the once-a-second pass of `WeaponEngravings`; own marker `NUFranchise`, so the older marker `NUEngraved` and the older engravings are untouched). Level: a relic's grade (1 up to rare, 2 unique and legend, 3 god and genesis), level 1 for every other item. Food, potions and spawn eggs are never engraved. The unified tooltip shows a line `Franchise: <name>` (in the franchise colour) under the engraving list.

## The franchises, with counts

Derived from: the relic waves (the class comment of a generated wave says what it is inspired by; waves 0 to 30 are the Dungeons and Dragons magic-item cards; wave 71 and the weapon rosters name a source per weapon), the item groups of the mod (Black Clover robes, grimoires and swords, the Magi Djinn, the Fairy Tail keys and Requip, the Shield Hero weapons), the labyrinth foundry (mob roster; it names a work per mob), the loot roster (`data/nusmp/loot/roster.json`: dungeon materials and food, no franchise) and the magic enum (all 105 `MagicType` entries are Black Clover attributes).

| franchise | mapped items | foundry mobs | engravings |
|---|---:|---:|---|
| Black Clover | 79 | 6 | Grimoire Surge, Anti-Magic Edge, Wizard King's Reserve |
| That Time I Got Reincarnated as a Slime | 3 | 6 | Great Sage's Insight, Predator's Harvest |
| Magi: The Labyrinth of Magic | 108 | 4 | Magoi Overflow, Wings of the Rukh, Solomon's Wisdom |
| Solo Leveling | 19 | 4 | Shadow Extraction, Rank Breaker |
| DanMachi | 3 | 1 | Falna, Argonaut |
| Fairy Tail | 32 | 6 | Guild Bond, Dragon Force, Celestial Contract |
| The Rising of the Shield Hero | 41 | 1 | Vassal Bond, Spirit Tortoise Ward, Waves Hunter |
| The Seven Deadly Sins | 3 | 3 | Full Counter, Sunshine |
| Dungeons and Dragons | 354 | 178 | Natural Twenty, Sneak Attack, Saving Throw |
| Re:Zero | 20 | 7 | Return by Death, Unseen Hand |
| Fate | 19 | 12 | Noble Phantasm, Command Seal |
| Overlord | 20 | 11 | Aura of Despair, Supreme Magic |
| KonoSuba | 19 | 5 | Explosion!, Steal |
| Mushoku Tensei | 19 | 3 | Unrivalled Mage, Swordplay Rhythm |
| The Eminence in Shadow | 19 | 0 | Cloak of Night, Shadow Garden |
| Tower of God | 18 | 1 | Shinsu Flow, Floor Climber |
| Fullmetal Alchemist | 8 | 6 | Equivalent Exchange, Automail Plating |
| Berserk | 11 | 16 | Dragonslayer's Weight, Struggler's Will |
| Sword Art Online and Log Horizon | 27 | 1 | Sword Skill, Raid Chant |
| Jujutsu Kaisen | 17 | 5 | Black Flash, Domain Pressure |
| Frieren: Beyond Journey's End | 19 | 15 | Zoltraak, Mana Suppression |
| Delicious in Dungeon | 20 | 6 | Cook's Harvest, Hearty Party |
| Made in Abyss | 21 | 11 | Delver's Resolve, Hunter's Whistle |
| Tomb Raider King | 18 | 0 | Tomb Raider's Eye, Relic Hunter |
| Yu-Gi-Oh! | 1 | 0 | Heart of the Cards, Trap Card |
| Naruto | 0 | 3 | Chakra Control, Substitution Jutsu |
| Bleach | 0 | 4 | Bankai, Soul Burial |
| One Piece | 0 | 5 | Armament Haki, Observation Haki |
| Dragon Ball | 0 | 0 | Zenkai, Ki Control |
| Hunter x Hunter | 0 | 3 | Nen Vow, Gyo |
| JoJo's Bizarre Adventure | 0 | 0 | Stand Rush, Life Giver |
| The Exiled Heavy Knight Knows How to Game the System | 0 | 0 | Heavy Plate Discipline, Game the System |
| Reincarnated as a Sword | 1 | 0 | Teacher's Insight, Devoured Power |

Total mapped items: 919 in 26 franchises (the map is `data/nusmp/relics/franchise_map.json`).

**Franchises with engravings but no mapped item yet (7):** Naruto, Bleach, One Piece, Dragon Ball, Hunter x Hunter, JoJo's Bizarre Adventure, The Exiled Heavy Knight Knows How to Game the System. They appear in the owner's list and in skills / the labyrinth foundry, but no relic, weapon or armour of the mod comes from them today, so their engravings exist (anvil, Tensura's engraving screen, commands) and are put on automatically the day an item is mapped (add it to `tools/relics/franchise_overrides.json` or a wave that says `inspired by <work>`, then re-run the tool).

**Works seen only as labyrinth mobs (no item, no engraving):** Akame ga Kill (1), Attack on Titan (7), Bloodborne (3), Chainsaw Man (1), Dark Souls (11), Demon Slayer (1), Dragon Quest (7), Elden Ring (4), Final Fantasy (2), Fire Force (7), Goblin Slayer (3), Monster Hunter (3), My Hero Academia (1), Princess Mononoke (7), Record of Ragnarok (5), Saint Seiya (4), Soul Eater (1), Spirited Away (5), Zelda (3). Items of unknown origin get no franchise.

Dungeons and Dragons: waves 0 to 30 are the cards of the D&D magic-item sheets (the Infinity Stones / Gauntlet of wave 30 are in that sheet too, so they count as D&D here, not as a separate franchise). The mobs of the foundry marked `D&D: <monster>` are 178 in all.

## Every engraving

`main` = works while the item is in the main hand; `held` = either hand; `any` = held, worn as armour or worn as a Curio (re-read once a second). Percentages are a share of the hit that was going to land (never a flat number, never a share of the target's maximum health). All numbers are for level 1; level 2 is +25%, level 3 +50% on shares, chances and heals (never on a cooldown, never past a cap).

### Black Clover

| engraving | scope | effect |
|---|---|---|
| **Grimoire Surge** (`nusmp:fr_grimoire_surge`) | held | A grimoire answers a clear call. A spell cast returns 1.2% of your maximum magicule (once every 5 s). +5% damage on what your spell hits within 3 s of the cast. |
| **Anti-Magic Edge** (`nusmp:fr_anti_magic_edge`) | main | A blade that does not care how well a foe is warded. +8% damage against foes that carry a protective effect (resistance, absorption, regeneration). |
| **Wizard King's Reserve** (`nusmp:fr_wizard_kings_reserve`) | any | A deep well of mana that fills itself. Returns 0.4% of your maximum magicule every 5 seconds. |

### That Time I Got Reincarnated as a Slime

| engraving | scope | effect |
|---|---|---|
| **Great Sage's Insight** (`nusmp:fr_great_sage_insight`) | main | The first look at a foe is already an analysis. +7% damage on the first hit against a foe you have not hit for 10 s. |
| **Predator's Harvest** (`nusmp:fr_predator_harvest`) | main | What is defeated feeds the one who defeated it. A kill heals 1.5 (once every 3 s). |

### Magi: The Labyrinth of Magic

| engraving | scope | effect |
|---|---|---|
| **Magoi Overflow** (`nusmp:fr_magoi_overflow`) | held | Magoi returns to the one who spent it well. A spell cast returns 1% of your maximum magicule (once every 6 s). |
| **Wings of the Rukh** (`nusmp:fr_rukh_wings`) | any | The Rukh carry you out of the way. 5% chance to take no damage from a hit (once every 20 s). |
| **Solomon's Wisdom** (`nusmp:fr_solomons_wisdom`) | held | The wisdom of the king lends weight to spells. +6% damage on what your spell hits within 3 s of the cast. |

### Solo Leveling

| engraving | scope | effect |
|---|---|---|
| **Shadow Extraction** (`nusmp:fr_shadow_extraction`) | main | A fallen foe may rise to serve for a short while. 5% chance on a kill (once every 45 s) to leave a short-lived ally for 20 s. |
| **Rank Breaker** (`nusmp:fr_rank_breaker`) | main | Strong foes are the ones that make a hunter grow. +7% damage on your hits against big foes (60 or more health). |

### DanMachi

| engraving | scope | effect |
|---|---|---|
| **Falna** (`nusmp:fr_falna`) | main | Each victory is carved into the back as growth. Each kill adds +1.5% damage, up to 5 times; the growth fades 60 s after the last kill. |
| **Argonaut** (`nusmp:fr_argonaut`) | main | Hold the charge, then release it in one blow. Every hit charges the weapon; the 6th hit deals +10% damage and resets it. |

### Fairy Tail

| engraving | scope | effect |
|---|---|---|
| **Guild Bond** (`nusmp:fr_guild_bond`) | any | Comrades fight better beside each other. Other players within 8 blocks gain a faint regeneration (4 s, refreshed every 2 s). |
| **Dragon Force** (`nusmp:fr_dragon_force`) | main | Pain is only fuel for a mage of the guild. +8% damage on your hits while you are at or under half health. |
| **Celestial Contract** (`nusmp:fr_celestial_contract`) | held | The spirits repay a kept promise. A spell cast returns 0.8% of your maximum magicule (once every 7 s). |

### The Rising of the Shield Hero

| engraving | scope | effect |
|---|---|---|
| **Vassal Bond** (`nusmp:fr_vassal_bond`) | any | A bearer who raises the shield is hard to move. -6% damage taken while you block or hold the shield. |
| **Spirit Tortoise Ward** (`nusmp:fr_tortoise_ward`) | any | The turtle's shell turns magic aside. -6% damage taken from magic. |
| **Waves Hunter** (`nusmp:fr_waves_hunter`) | main | Born to answer the Waves: giants fall harder. +7% damage on your hits against big foes (60 or more health). |

### The Seven Deadly Sins

| engraving | scope | effect |
|---|---|---|
| **Full Counter** (`nusmp:fr_full_counter`) | main | A blow taken is a blow returned. After you take a hit, your next hit within 3 s deals +12% damage. |
| **Sunshine** (`nusmp:fr_sunshine`) | main | At noon the strongest man alive steps forward. +8% damage on your hits in daylight. |

### Dungeons and Dragons

| engraving | scope | effect |
|---|---|---|
| **Natural Twenty** (`nusmp:fr_natural_twenty`) | main | Now and then the dice just fall your way. 5% chance on a hit (once every 5 s) of a bonus: +15% damage on that hit. |
| **Sneak Attack** (`nusmp:fr_sneak_attack`) | main | A rogue strikes best from the shadows. +10% damage on your hits while sneaking. |
| **Saving Throw** (`nusmp:fr_saving_throw`) | any | A lucky roll turns a blow aside. 5% chance to take no damage from a hit (once every 20 s). |

### Re:Zero

| engraving | scope | effect |
|---|---|---|
| **Return by Death** (`nusmp:fr_return_by_death`) | any | At the edge, the world is set back a little. When a hit leaves you at or under 25% health, restores 2 health (once every 180 s). |
| **Unseen Hand** (`nusmp:fr_unseen_hand`) | main | Something unseen takes hold of a foe's step. 12% chance on a hit (once every 5 s) to inflict slowness for 2 s. |

### Fate

| engraving | scope | effect |
|---|---|---|
| **Noble Phantasm** (`nusmp:fr_noble_phantasm`) | main | Blows build the legend, then the legend is released. Every hit charges the weapon; the 8th hit deals +12% damage and resets it. |
| **Command Seal** (`nusmp:fr_command_seal`) | held | One spent seal returns a breath of mana. A spell cast returns 1.5% of your maximum magicule (once every 30 s). |

### Overlord

| engraving | scope | effect |
|---|---|---|
| **Aura of Despair** (`nusmp:fr_aura_of_despair`) | main | The sight of a supreme being saps the will. 15% chance on a hit (once every 5 s) to inflict weakness for 3 s. |
| **Supreme Magic** (`nusmp:fr_supreme_magic`) | held | Spells cast by the highest ranks carry more weight. +6% damage on what your spell hits within 3 s of the cast. |

### KonoSuba

| engraving | scope | effect |
|---|---|---|
| **Explosion!** (`nusmp:fr_explosion`) | main | One grand blow, once the chant is over. Every hit charges the weapon; the 10th hit deals +15% damage and resets it. |
| **Steal** (`nusmp:fr_steal_skill`) | main | Take a little of what you cut. Heals 4% of the damage you deal, at most 1.5 per hit (once every 1 s). |

### Mushoku Tensei

| engraving | scope | effect |
|---|---|---|
| **Unrivalled Mage** (`nusmp:fr_unrivalled_mage`) | held | Spells without a chant, a little sharper. +6% damage on what your spell hits within 3 s of the cast. |
| **Swordplay Rhythm** (`nusmp:fr_swordplay_rhythm`) | main | Strike in time and each cut flows into the next. Hits within 3 s of each other stack +2% damage, up to 4 times. |

### The Eminence in Shadow

| engraving | scope | effect |
|---|---|---|
| **Cloak of Night** (`nusmp:fr_night_veil`) | main | The shadow works best in the dark. +8% damage on your hits at night. |
| **Shadow Garden** (`nusmp:fr_shadow_garden`) | any | Nobody saw where the blade went. 4% chance to take no damage from a hit (once every 20 s). |

### Tower of God

| engraving | scope | effect |
|---|---|---|
| **Shinsu Flow** (`nusmp:fr_shinsu_flow`) | any | Shinsu gathers around those who climb. Returns 0.4% of your maximum magicule every 5 seconds. |
| **Floor Climber** (`nusmp:fr_floor_climber`) | main | Each fallen guardian is a step up. A kill grants 4 s of Speed (once every 10 s). |

### Fullmetal Alchemist

| engraving | scope | effect |
|---|---|---|
| **Equivalent Exchange** (`nusmp:fr_equivalent_exchange`) | main | To gain something, something of equal worth is lost. Each hit costs you 1 health (never below 8) and deals +12% damage. |
| **Automail Plating** (`nusmp:fr_automail_plating`) | any | Steel limbs turn arrows and bolts. -6% damage taken from arrows and bolts. |

### Berserk

| engraving | scope | effect |
|---|---|---|
| **Dragonslayer's Weight** (`nusmp:fr_dragonslayer_weight`) | main | A blade too big to be called a sword fells big things. +8% damage on your hits against big foes (60 or more health). |
| **Struggler's Will** (`nusmp:fr_strugglers_will`) | any | Refuses to fall at the last step. When a hit leaves you at or under 20% health, restores 2 health (once every 180 s). |

### Sword Art Online and Log Horizon

| engraving | scope | effect |
|---|---|---|
| **Sword Skill** (`nusmp:fr_sword_skill`) | main | One skill leads straight into the next. Hits within 2.5 s of each other stack +2.5% damage, up to 4 times. |
| **Raid Chant** (`nusmp:fr_raid_chant`) | held | A chanted spell lands a little harder. +5% damage on what your spell hits within 3 s of the cast. |

### Jujutsu Kaisen

| engraving | scope | effect |
|---|---|---|
| **Black Flash** (`nusmp:fr_black_flash`) | main | Sometimes the strike lands on the exact instant. 4% chance on a hit (once every 6 s) of a bonus: +15% damage on that hit. |
| **Domain Pressure** (`nusmp:fr_domain_pressure`) | main | The air weighs on whoever you face. 10% chance on a hit (once every 6 s) to inflict slowness for 2 s. |

### Frieren: Beyond Journey's End

| engraving | scope | effect |
|---|---|---|
| **Zoltraak** (`nusmp:fr_zoltraak`) | held | The common spell of the demon kind, honed over a thousand years. +6% damage on what your spell hits within 3 s of the cast. |
| **Mana Suppression** (`nusmp:fr_mana_suppression`) | any | A hidden mana pool is a hard one to read. -5% damage taken from magic. |

### Delicious in Dungeon

| engraving | scope | effect |
|---|---|---|
| **Cook's Harvest** (`nusmp:fr_cooks_harvest`) | main | The dungeon always gives back something. A kill heals 1.5 (once every 3 s). |
| **Hearty Party** (`nusmp:fr_hearty_party`) | any | A good table keeps a party going. Other players within 6 blocks gain a faint regeneration (4 s, refreshed every 3 s). |

### Made in Abyss

| engraving | scope | effect |
|---|---|---|
| **Delver's Resolve** (`nusmp:fr_delvers_resolve`) | any | Those who go down learn how to land. -20% damage taken from falls. |
| **Hunter's Whistle** (`nusmp:fr_hunters_whistle`) | main | A whistle of the deepest rank carries weight against giants. +7% damage on your hits against big foes (60 or more health). |

### Tomb Raider King

| engraving | scope | effect |
|---|---|---|
| **Tomb Raider's Eye** (`nusmp:fr_tomb_raiders_eye`) | main | A sharp eye finds the lucky moment. 5% chance on a hit (once every 6 s) of a bonus: +15% damage on that hit. |
| **Relic Hunter** (`nusmp:fr_relic_hunter`) | main | First contact is the cleanest one. +7% damage on the first hit against a foe you have not hit for 10 s. |

### Yu-Gi-Oh!

| engraving | scope | effect |
|---|---|---|
| **Heart of the Cards** (`nusmp:fr_heart_of_the_cards`) | main | Believe in the deck and a card is drawn: a bonus effect. 7% chance on a hit (once every 5 s) of a bonus: +15% damage on that hit, heals 1, 3 s of Speed or returns 1% magicule. |
| **Trap Card** (`nusmp:fr_trap_card`) | any | A face-down card turns a blow aside. 5% chance to take no damage from a hit (once every 15 s). |

### Naruto

| engraving | scope | effect |
|---|---|---|
| **Chakra Control** (`nusmp:fr_chakra_control`) | any | Well-kept chakra returns a little at a time. Returns 0.4% of your maximum magicule every 5 seconds. |
| **Substitution Jutsu** (`nusmp:fr_substitution_jutsu`) | any | A log is hit where you were. 6% chance to take no damage from a hit (once every 30 s). |

### Bleach

| engraving | scope | effect |
|---|---|---|
| **Bankai** (`nusmp:fr_bankai`) | main | The final release, after enough blows were traded. Every hit charges the weapon; the 7th hit deals +12% damage and resets it. |
| **Soul Burial** (`nusmp:fr_soul_burial`) | main | A soul reaper sends the dead on. +12% damage on your hits against the undead. |

### One Piece

| engraving | scope | effect |
|---|---|---|
| **Armament Haki** (`nusmp:fr_armament_haki`) | any | Haki hardens the body against blows. -6% damage taken from melee blows. |
| **Observation Haki** (`nusmp:fr_observation_haki`) | any | The next blow is felt before it lands. 5% chance to take no damage from a hit (once every 20 s). |

### Dragon Ball

| engraving | scope | effect |
|---|---|---|
| **Zenkai** (`nusmp:fr_zenkai`) | main | Blows taken make the next one stronger. After you take a hit, your next hit within 5 s deals +10% damage. |
| **Ki Control** (`nusmp:fr_ki_control`) | any | Ki that is well controlled is not wasted. Returns 0.4% of your maximum magicule every 5 seconds. |

### Hunter x Hunter

| engraving | scope | effect |
|---|---|---|
| **Nen Vow** (`nusmp:fr_nen_vow`) | main | A vow with a price makes the blow stronger. Each hit costs you 1 health (never below 10) and deals +10% damage. |
| **Gyo** (`nusmp:fr_gyo`) | main | Eyes full of aura see through a ward. +8% damage against foes that carry a protective effect (resistance, absorption, regeneration). |

### JoJo's Bizarre Adventure

| engraving | scope | effect |
|---|---|---|
| **Stand Rush** (`nusmp:fr_stand_rush`) | main | The barrage builds the longer it lasts. Hits within 2 s of each other stack +3% damage, up to 4 times. |
| **Life Giver** (`nusmp:fr_life_giver`) | main | A little life flows back from the cut. Heals 3% of the damage you deal, at most 1.5 per hit (once every 1 s). |

### The Exiled Heavy Knight Knows How to Game the System

| engraving | scope | effect |
|---|---|---|
| **Heavy Plate Discipline** (`nusmp:fr_heavy_plate_discipline`) | any | Blows glance off a well-built knight. -7% damage taken from melee blows. |
| **Game the System** (`nusmp:fr_game_the_system`) | main | Found the exploit: the first hit is the cheap one. +7% damage on the first hit against a foe you have not hit for 10 s. |

### Reincarnated as a Sword

| engraving | scope | effect |
|---|---|---|
| **Teacher's Insight** (`nusmp:fr_teachers_insight`) | main | A sword that appraises its foe sees the first opening. +8% damage on the first hit against a foe you have not hit for 10 s. |
| **Devoured Power** (`nusmp:fr_devoured_power`) | main | What the sword defeats makes it stronger for a while. Each kill adds +1.2% damage, up to 6 times; the growth fades 60 s after the last kill. |

## Balance

The weapon damage is already set by `relics/GradeDamage` (normal 20-30, special 35-55, rare 60-85, unique 90-115, legend 130-175, god 190-240, genesis 260-350 per hit by weapon type). An engraving is a flavour bonus, not a second weapon:

* every attack bonus is a SHARE of the hit (so it scales with the table and never makes a dagger out-hit a greatsword); all franchise attack bonuses of one hit together are capped at **15%** (server config `attackCapPercent`, 0 to 30) and count **half against a player**. A legend sword hit of 175 gains at most 26; a genesis hit of 350 at most 52.
* every damage reduction (shield guard, wards) together is capped at **20%**.
* every heal is at most **2** per proc; lifesteal and kill heals have their own cooldowns.
* mana: a spell refund is a share of the maximum magicule, at most **5% per minute** in all; a trickle pulse (every 5 seconds) at most **0.6%**.
* every chance is at most **25%** whatever the level; every proc has a cooldown (20 ticks to 3 minutes); a dodge happens at most once per 15 to 30 seconds; a second wind is once per 3 minutes and never saves a dead bearer; Equivalent Exchange / Nen Vow cost a flat 1 health and stop at 8 / 10 health, so they can never kill.
* the short-lived ally (Shadow Extraction) is at most one per 45 seconds, 5% chance, 20 seconds, and shares the 6-allies-per-player limit and the driver of the relic summons (`relics/Fx.summon`).
* debuffs are Slowness or Weakness for at most 5 seconds. **No engraving touches hunger, saturation or exhaustion** (`FranchiseRules.effectAllowed` refuses those effects; the data test refuses the words).
* effects of mob effects are applied without particles; the proc puffs use the mod's custom particles through `particle/fx/Fx` (the franchise's theme: for a generated wave its own `relic.wave.<n>` theme), so they go through the perf budgets.

## How items map

`tools/relics/franchise_map.py` (deterministic, re-runnable, tolerates a def without a description, a wave without a header, an unreadable file) writes `franchise_map.json`; the order of authority:

1. `tools/relics/franchise_overrides.json` (the manual layer: exact ids, id prefixes such as `charter_grimoire_` or `djinn_weapon_`, the D&D wave range, `null` to force none);
2. a roster that names a weapon's source (`tools/relics/new197_roster.py`, wave 71);
3. the wave: `inspired by <work>` in the class comment of a generated wave (an alias of the franchise table, whole words); the cards of waves 0 to 30 are D&D;
4. for items that are no relic: the franchise whose words occur most in the item's name and lang text (a tie gives none);
5. otherwise none (the pocket-tomb weapons and relics are original, the myth weapons Gungnir, Kusanagi and Gae Bolg are not an anime; consumables are never engraved).

The run prints how many items each rule decided (`--report --why` lists every item with the rule). The lead re-runs it after waves 101 to 103 are merged: `python3 -B tools/relics/franchise_map.py`; `--check` exits 1 when the file on disk is out of date. A pair's left half follows its right half; at run time `FranchiseMap` also resolves by longest prefix, so a new `charter_grimoire_*` or `djinn_weapon_*` item is engraved without a re-run.

| item class | gets | what the engraving does |
|---|---|---|
| melee weapons (swords, spears, axes, katanas, hammers, Djinn weapons ...) | its franchise's engravings | the `main` hit kinds: bonus damage by condition, combos, charges, kill stacks, lifesteal, debuffs |
| grimoires, magic books, staves, keys, Charter grimoires (mapped) | the engravings as a display tag | the `held` spell kinds (`spell_refund`, `spell_power`) for a spell cast from a grimoire (see the limit below) |
| armour, robes, mantles, Curios relics (mapped) | its franchise's engravings | the `any` kinds: wards, shield guard, dodge, second wind, mana trickle, ally regeneration |
| shields | `shield_guard` / `ward` engravings | the reduction while blocking or holding a shield |

**Limit, honestly:** the spell hook is one line in `book/GrimoireBook` after a finished cast, so `spell_refund` and `spell_power` work for grimoire casts only; the Fairy Tail, Celestial, Djinn and hero-weapon casters use their own cast paths and do not call it (their engravings still act through the melee, guard and aura kinds). Spell damage is recognised as a non-melee hit whose attacker is the caster within the engraving's window (3 seconds) of a cast; a spell that damages without naming its caster (vanilla `magic` damage with no entity) cannot be told from the caster and gets no bonus.

## Files

* `src/main/java/com/newuniverse/nusmp/franchise/`: `FranchiseData` (table, pure), `FranchiseMap` (item map, pure), `FranchiseRules` (the numbers and caps, pure), `FranchiseEngravings` (puts them on, tooltip line), `FranchiseEvents` (handlers, per-player state keyed by UUID, cleared on logout and server stop), `FranchiseMana`, `FranchiseConfig` (SERVER config `nusmp-franchise-server.toml`: `enabled`, `allies`, `aura`, `attackCapPercent`), `FranchiseBoot`, `FranchiseSelfTests`.
* data: `data/nusmp/enchantment/fr_*.json` (71), `data/tensura/tags/enchantment/engraving.json` (additions appended), `data/nusmp/franchise/engravings.json`, `data/nusmp/relics/franchise_map.json`.
* tools: `tools/franchise/gen_engravings.py` (the table; writes the enchantments, the tag additions, the data file and the lang fragment), `tools/franchise/gen_doc.py` (this page), `tools/relics/franchise_map.py` and `tools/relics/franchise_overrides.json`.
* lang fragment: `tools/lang/franchise_engravings.json` (names, descriptions `.desc`, `franchise.nusmp.<id>`, `tooltip.nusmp.franchise`).
* tests: `FranchiseRulesTest` (every effect with its caps), `FranchiseDataTest` (every json parses with the fields 1.21.1 requires, lang name and description, in the tag, every franchise of the map has 2 or 3 engravings, the numbers are inside their caps, nothing mentions hunger, the older `WeaponEngravings` entries are unchanged against a baseline taken before this release), CI self-test `item.franchise_engravings`.

Append-only edits to shared files: `NUSMP.java` (one line: `FranchiseBoot.init`), `item/WeaponEngravings.java` (one line in the once-a-second pass: `FranchiseEngravings.engrave`), `book/GrimoireBook.java` (one line after `ManaLaw.onCast`: `FranchiseEvents.onSpellCast`), `client/ui/tooltip/UnifiedTooltip.java` (two lines: the franchise line), `data/tensura/tags/enchantment/engraving.json` (values appended).

## NEW / REPLACED / KEPT

| part | status |
|---|---|
| 71 franchise engravings and their enchantments, tag entries, lang | **NEW** (nothing like them existed; the two older `nusmp:` engravings are a different thing) |
| franchise item map, its tool and overrides | **NEW** |
| franchise tooltip line | **NEW** (added under the engraving list) |
| `WeaponEngravings` table, `RelicEngravings`, `ArmorEngravings`, the `NUEngraved` marker, `nusmp:anti_magic`, `nusmp:legacy_of_the_wizard_kings`, every Tensura engraving on every weapon | **KEPT** unchanged |
| REPLACED | nothing |

## What could not be verified

* Nothing was seen or felt in a game. The handlers were type-checked against API stubs and the CI self-test was not run here; the rules and the data are unit-tested only.
* The datapack enchantments follow the shape of the two older `nusmp:` enchantments (known to load); the `supported_items` tag `#minecraft:enchantable/durability` and `slots: ["any"]` were not loaded by a real server here. If a version rejects them the fix is in `enchantment_json` of the generator.
* `LivingIncomingDamageEvent` / `LivingDamageEvent.Post` are used as in NeoForge 1.21.1; whether other mods' handlers at normal priority change the amount before or after the engraving was not checked. Better Combat's attacks are assumed to arrive as `player_attack` damage; a weapon whose hits arrive as another damage type gets no melee bonus.
* Day / night uses `Level.isDay()`: in a dimension with a fixed time (the Nether, perhaps the mega dimensions) it reads as night, so Sunshine never works there and Cloak of Night always does.
* `Fx.summon` allies are vanilla skeletons driven by the relic-summon sweep; their behaviour was not watched.
* Items worn only in a Curios slot are engraved when they sit in the inventory (as every older engraving): an item that never touched an inventory after this release stays unengraved until it does.
* The map is as good as the wave headers, the roster `src` fields and the overrides: the D&D default for waves 0 to 30 is the documented origin of those cards, not checked card by card; the manual overrides (the Black Clover swords, the Fairy Tail keys, Wave 45's two works) are the author's reading of the names. Wrong ones are fixed in `franchise_overrides.json`.
* 7 franchises have no mapped item (above), so their engravings are untested on a real item.
* Balance numbers are first estimates against the grade tables; nothing was played.

