# Ten Evolution Unique skills (3.26.0)

All ten are NEW (rule 5: nothing like them exists; Fairy Tail has no lines at all yet, the other four franchises get two lines each next to their 25).
They are normal skill lines (`tools/lines/rows/<franchise>.py`, generated to `data/nusmp/skill_trees/<id>.json`, one book each) with the Evolution extras from `docs/skillbooks_3_26.md` section 4.

## How the Gemini "Evolution" format maps onto the line engine

| Gemini section | What it becomes in the line |
|---|---|
| Information (name, source, evolution requirement) | `Line` desc / book lore, plus `evolution={requirement, count, text, ultimate_name}` |
| Passive-Toggle | stance 1 (the line has exactly 2 stances) |
| Passive-Combat (with a Debt) | the `debt_store` passive (blows store delayed damage on the foe) plus 1-2 normal passives |
| Active Press (Q) | arts (the line's normal modes) |
| Active Crouch (Shift+Q) | a separate art per crouch variant, named "<Art>: Shift". The engine has no hold/crouch variants, so each one is its own mode (10 modes per skill) |
| Active Held | stance 2 (a held posture is a stance in the engine) |
| Ultimate Evolution (cost, cooldown) | the LAST finisher, flagged `ultimate=True`, kind `debt_burst`; cooldown 12 000 to 18 000 ticks (10 to 15 minutes), mana cost 5 to 10 % of the pool; locked until the evolution counter is met AND the skill is mastered |

Counters (`catalog.EVOLUTION`): kills, elite_kills, boss_kills, damage_dealt, damage_taken (optionally one damage kind), lethal_survived, arts_cast, ally_buffs.
Once evolved, passives with an `evolved` block use those stronger numbers (top level), and the Ultimate art unlocks.
Every skill keeps the line rules: 2 stances, 3-4 stats, 6-8 arts, 6-8 passives (one is `debt_store`), 2 finishers (the second is the ultimate), variety guard (5 distinct art kinds, 5 distinct passive kinds), finisher cooldown at least 900 ticks.

## The ten

Ids follow each franchise's own prefix. "Source" says where the canon came from (rule 10): every entry was researched through WebSearch summaries restricted to the English fandom wiki of the series; the actual wiki pages could not be opened in this environment (host unreachable), and the Japanese fallback returned nothing usable. Details marked UNVERIFIED in `docs/skills/research/*.md` were not used for numbers.

| # | id | Name (character, ability) | Franchise | Evolution requirement | Ultimate name | Rarity | Colour / background |
|---|---|---|---|---|---|---|---|
| 1 | `tog_rose_shower` | Rose Shower (Ha Yuri Zahard, Shinsu Flower / Red Velvet Sword / Rose Shower) | tower_of_god | 25 `elite_kills` | Ultimate Skill, Crimson Rose of Zahard | epic | #d6204a |
| 2 | `tog_true_flame` | True Flame (Yeon Yihwa, Yeon family flame) | tower_of_god | 2000 `damage_taken` kind `fire` | Ultimate Skill, Flame That Does Not Burn the Self | rare | #ff7a1a |
| 3 | `dragon_king_antares` | Dragon Monarch's Breath (Antares, Monarch of Destruction) | solo_leveling | 10 `boss_kills` | Ultimate Skill, Monarch of Destruction | mythic | #ff4a2b |
| 4 | `white_tiger_baek` | Tiger's Claws (Baek Yoonho, White Tiger Guild master) | solo_leveling | 15 `lethal_survived` | Ultimate Skill, White Tiger Awakened | rare | #e8e8f0 |
| 5 | `haruhime_uchide` | Uchide no Kozuchi (Sanjouno Haruhime, Level Boost) | danmachi | 300 `ally_buffs` | Ultimate Skill, Uchide no Kozuchi: Full Moon | rare | #ffb6e0 |
| 6 | `ryuu_astrea_blade` | Astraea's Wooden Sword (Ryuu Lion, the elf blade dancer) | danmachi | 400 `arts_cast` | Ultimate Skill, Luminous Wind: Aria of Justice | rare | #66d9a0 |
| 7 | `gray_ice_make` | Ice-Make (Gray Fullbuster, Static Ice-Make and Ice Devil Slayer) | fairy_tail | 50 000 `damage_dealt` | Ultimate Skill, Iced Shell: Absolute Zero | epic | #8fd8ff |
| 8 | `erza_requip_knight` | Requip: The Knight (Erza Scarlet, armour and weapon swap) | fairy_tail | 1000 `kills` | Ultimate Skill, Armadura Fairy: Heaven's Wheel | epic | #c8203c |
| 9 | `goh_haetae_jade` | Haetae to Jade Emperor (Han Daewi, water Charyeok to the Wisdom of the Sage) | god_of_high_school | 3000 `damage_taken` kind `any` | Ultimate Skill, Wisdom of the Sage | epic | #3fb6ff |
| 10 | `goh_lu_bu_moonlight` | Yeo-Po Bong-Seon (Yoo Mira, Moonlight Sword Style and the Lu Bu Charyeok) | god_of_high_school | 666 `kills` | Ultimate Skill, Sky Piercer Red Hare | rare | #f0e6a0 |

Spread: 1 mythic, 4 epic, 5 rare. The Ultimate of every skill is a `debt_burst` finisher (the Debt mechanic is the shared engine of the ten, so the owner's Gemini format is kept) but each has its own vfx, radius and multiplier.

## Per-skill outline (the ROWS builders fill in names, numbers and icons within the catalogue ranges)

For each: Toggle (stance 1) / Held (stance 2) / Press arts / Shift arts / Debt passive / Ultimate.

1. **Rose Shower (Ha Yuri).** Toggle: Shinsu Flower stance (thorns reflect, armour). Held: Shinsu Bubble guard (damage reduction, slow). Press: Red Velvet Sword slash, Rose Shower (rain of petals, ring burst), shinsu pull, mark. Shift: Rose Shower: Shift (wide cone of petals), Red Velvet: Shift (dash strike). Debt: wounds bloom, ticking bleed that the Ultimate detonates. Ultimate: every rose on every foe in range blooms at once.
2. **True Flame (Yeon Yihwa).** Toggle: Yeon Flame stance (damage when burning, fire resist). Held: Flame Veil guard (damage reduction and a burn aura). Press: flame bolt, fire pillar, flame wall, ignite mark. Shift: Hwayeomsa: Shift (big fire ring), flame step. Debt: burns accumulate as stored fire damage. Ultimate: True Flame: the stored fire erupts, no self harm.
3. **Dragon Monarch's Breath (Antares).** Toggle: Dragon Monarch's Might (damage up, armour down). Held: Dragon Hide (armour, knockback resistance). Press: dragon breath cone, tail sweep, roar (fear / weakness), claw strike. Shift: Breath: Shift (long beam), Wing Dive (leap and slam). Debt: Dragon's curse stores a fraction of every blow. Ultimate: the Monarch of Destruction, the biggest burst in the pack.
4. **Tiger's Claws (Baek Yoonho).** Toggle: Tiger Form (speed and damage). Held: Prowl (speed, evasion, no armour). Press: claw slash, pounce, roar, rending bleed. Shift: Claw Combo: Shift, Pounce: Shift (long leap). Debt: claw wounds stored as bleed. Ultimate: White Tiger Awakened: massive pounce and burst. Counter is lethal blows survived, so the skill pays for taking risks.
5. **Uchide no Kozuchi (Haruhime).** Toggle: Fox Prayer stance (healing allies, ally buffs). Held: Chanting (slow, allies near you grow strong). Press: level boost on allies (`buff_aura`), heal pulse, Sacred Sword blessing, fox-fire bolt. Shift: Boost: Shift (bigger radius, shorter duration), Fox-Fire: Shift. Debt: blessings borrow strength, then ask it back as stored damage. Ultimate: Full Moon, a single strong team buff plus the burst (one `buff_aura` effect only; a second effect would break existing rows).
6. **Astraea's Wooden Sword (Ryuu).** Toggle: Elf Poise stance (speed, parry). Held: Wind Guard (reflect). Press: wooden-sword combo, Luminous Wind arrows (projectile), sweep, bleed. Shift: Dash Strike: Shift, Aria of Justice (charged). Debt: fighting in silence stores justice as delayed damage. Ultimate: Luminous Wind: Aria of Justice.
7. **Ice-Make (Gray).** Toggle: Static Ice-Make stance (frost, slows foes). Held: Ice Shield (armour). Press: Ice-Make Lance, Floor (freeze ring), Hammer, Saucer. Shift: Lance: Shift (multi), Cannon: Shift. Debt: frostbite stores cold damage on the foe, ticks while it waits. Ultimate: Iced Shell: Absolute Zero (freeze plus a very large Debt burst).
8. **Requip: The Knight (Erza).** Toggle: Heart Kreuz stance (armour). Held: Adamantine guard (damage reduction, slow). Press: sword combo, Circle Sword (ring), Blumenblatt volley, armour swap buff. Shift: Sword: Shift (long cone), Requip Rush. Debt: each swap stores a blow. Ultimate: Armadura Fairy: Heaven's Wheel (rain of swords as a Debt burst).
9. **Haetae to Jade Emperor (Han Daewi).** Toggle: Haetae's Tide stance (water regen, mobility). Held: Hard Water barrier (damage reduction). Press: water arrows, wave, bubble lift, healing mist. Shift: Water Wave: Shift, Haetae's Breath (direct contract breath). Debt: pressure stores damage on foes. Ultimate: Wisdom of the Sage (gravity crush plus Debt burst).
10. **Yeo-Po Bong-Seon (Yoo Mira).** Toggle: Moonlight stance (crits, speed). Held: Mountain Stance (armour, counter). Press: moonlight slash, Sky Piercer lunge, Red Hare charge (dash), return-the-blade reflect. Shift: Slash: Shift (long), Lunge: Shift. Debt: each cut stores a fraction. Ultimate: Sky Piercer on Red Hare (a long charge ending in the burst).

## Weapon engravings (rule 6)

These skills carry no new weapon items, so no engravings are needed. If a future release gives them a weapon, `WeaponEngravings.java` is the place.

## Server and lag cost (rule 11), worst case per player

* Counters: one integer per kind per skill, stored in the player's persistent data, bumped on server events only (kill, damage); a bump costs O(1), no scanning.
* Debt: one small map on the foe (at most 20 foes tracked per player, one entry per foe, pruned after `hold_seconds` and on death, logout and dimension change), ticked at most once every `tick_seconds`; at most 20 foes per player (`targets` cap of `debt_burst` is 20) per burst.
* Ultimate: cooldown of 10 to 15 minutes, one burst per cast, a single packet to nearby players.
* No new entities, no new per-tick world scans.

## Not fetched (rule 10)

None of the fandom or wikipedia pages could be opened (host unreachable, proxy 403). Everything came from WebSearch summaries. Entries are canon-faithful by name and idea, all numbers are the mod's own. The unverified bits (listed in the research files) were not used.
