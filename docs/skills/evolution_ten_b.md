# Ten more Evolution Unique skills (3.27.0)

All ten are NEW (rule 5: no line of this mod covers these characters; existing lines of the same series are other characters or generic classes). Same format, same engine and same mapping table as `docs/skills/evolution_ten.md` (Gemini section -> line engine). No engine change.
The characters were picked by the lead because the owner did not name any ("another Evolution batch"). Canon is researched by the builders (rule 10: English wiki first, Japanese wiki as the fallback; each research file says which source it used and what could not be fetched).

| # | id | Character / ability | Franchise | Evolution requirement | Rarity | Colour |
|---|---|---|---|---|---|---|
| 1 | `natsu_fire_dragon` | Natsu Dragneel, Fire Dragon Slayer | fairy_tail | 150 `elite_kills` | epic | #ff5a1f |
| 2 | `laxus_lightning_dragon` | Laxus Dreyar, Lightning Dragon Slayer | fairy_tail | 8 `boss_kills` | epic | #ffe14a |
| 3 | `wendy_sky_dragon` | Wendy Marvell, Sky Dragon Slayer (healing, buffs) | fairy_tail | 400 `ally_buffs` | rare | #7fd6ff |
| 4 | `ais_sword_princess` | Ais Wallenstein, Sword Princess (Aria, Tempest) | danmachi | 800 `kills` | epic | #f2d56b |
| 5 | `cha_hae_in_sword` | Cha Hae-In, S-rank sword hunter | solo_leveling | 30 `elite_kills` | epic | #c2e6ff |
| 6 | `baran_demon_monarch` | Baran, Monarch of Demons | solo_leveling | 80000 `damage_dealt` | mythic | #9b2d5e |
| 7 | `tog_anak_zahard` | Anak Zahard, Zahard's princess | tower_of_god | 60000 `damage_dealt` | epic | #5fd0c0 |
| 8 | `tog_rak_wraithraiser` | Rak Wraithraiser, Spear Bearer | tower_of_god | 700 `arts_cast` | rare | #44aaff |
| 9 | `goh_ilpyo_sword` | Park Ilpyo, sword of the Park family | god_of_high_school | 1500 `kills` | rare | #d0d8e0 |
| 10 | `goh_taejin_charyeok` | Jin Taejin, Charyeok user | god_of_high_school | 10 `lethal_survived` | epic | #e8a23a |

Spread: 1 mythic, 6 epic, 3 rare. The ultimate name of each skill is chosen by its builder from canon ("Ultimate Skill, <canon name>"); the builder may adjust a character's skill NAME inside the line if the canon research shows a better one, but the id, requirement, count, rarity and colour above are fixed.
Each skill follows the line rules (2 stances, 3-4 stats, 6-8 arts incl. "<Art>: Shift" variants, 6-8 passives with one `debt_store` and 2-3 `evolved` overrides, 2 finishers, the last one the Ultimate Evolution `debt_burst`, cooldown 12000-18000).

## Weapon engravings (rule 6)
No weapon items are added, so none are needed.

## Server and lag cost (rule 11), worst case per player
Identical to `docs/skills/evolution_ten.md`: counters O(1) per event; at most 20 Debt foes per player, at most 16 foes per burst, cooldown 10 to 15 minutes, no entities, no per-tick scans, no new packet type. Data only.
