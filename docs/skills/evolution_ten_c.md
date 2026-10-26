# Ten more Evolution Unique skills, third batch (3.28.0)

All ten are NEW (rule 5: no line of this mod covers these characters; the lines that exist for the same series are other characters or generic classes. Builders must still check the existing line names/descriptions of their franchise for overlap and tell the lead if a character is already covered). Same format, same engine and same mapping table as `docs/skills/evolution_ten.md` / `evolution_ten_b.md` (no engine change).
The characters were picked by the lead because the owner did not name any ("make the next set"). Fairy Tail has only 5 lines, so it gets four. Canon is researched by the builders (rule 10: English wiki first, Japanese wiki as the fallback; say which source, list what could not be fetched).

| # | id | Character / ability | Franchise | Evolution requirement | Rarity | Colour |
|---|---|---|---|---|---|---|
| 1 | `gajeel_iron_dragon` | Gajeel Redfox, Iron Dragon Slayer | fairy_tail | 4000 `damage_taken` (damage_kind any) | rare | #8a96a8 |
| 2 | `mirajane_satan_soul` | Mirajane Strauss, Take Over: Satan Soul | fairy_tail | 600 `arts_cast` | epic | #b0305a |
| 3 | `gildarts_crash` | Gildarts Clive, Crash Magic | fairy_tail | 120000 `damage_dealt` | mythic | #b86bff |
| 4 | `juvia_water_body` | Juvia Lockser, Water Magic / Water Body | fairy_tail | 12 `lethal_survived` | rare | #3f8fe0 |
| 5 | `ashborn_shadow_monarch` | Ashborn, the original Shadow Monarch | solo_leveling | 12 `boss_kills` | mythic | #5b2a9d |
| 6 | `go_gunhee_chairman` | Go Gun-hee, Hunters Association chairman (S-rank) | solo_leveling | 40 `elite_kills` | epic | #d4a64a |
| 7 | `regis_leywin_devour` | Regis Leywin, Asura lance-beast (Devour, Shadow) | tbate | 900 `kills` | epic | #6a5acd |
| 8 | `lee_seolhwa_sword` | Lee Seolhwa, the Company's doctor and poison specialist (found by the builder: not a swordswoman; the id stays) | omniscient_readers_viewpoint | 700 `kills` | rare | #c9e0ff |
| 9 | `shin_yoosung_child` | Shin Yoosung, the child of the party | omniscient_readers_viewpoint | 350 `ally_buffs` | rare | #ffb3d1 |
| 10 | `lefiya_viridis_mage` | Lefiya Viridis, Elf mage of the Loki Familia | danmachi | 500 `arts_cast` | epic | #4ccf9a |

Spread: 2 mythic, 4 epic, 4 rare. The ultimate name of each skill is chosen by its builder from canon ("Ultimate Skill, <canon name>"); the builder may adjust a skill NAME inside the line if the research shows a better one, but the id, requirement, count, rarity and colour above are fixed. If a character's canon is too thin or unverifiable (Go Gun-hee, Shin Yoosung, Lee Seolhwa), design the arts in the spirit of the character, mark them INVENTED in the research file and never present them as canon.
Each skill follows the line rules (2 stances, 3-4 stats, 6-8 arts incl. "<Art>: Shift" variants, 6-8 passives with one `debt_store` and 2-3 `evolved` overrides, 2 finishers, the last one the Ultimate Evolution `debt_burst`, cooldown 12000-18000).

## Weapon engravings (rule 6)
No weapon items are added, so none are needed.

## Server and lag cost (rule 11), worst case per player
Identical to `docs/skills/evolution_ten.md`: counters O(1) per event; at most 20 Debt foes per player, at most 16 foes per burst, cooldown 10 to 15 minutes, no entities, no per-tick scans, no new packet type. Data only.

## Final (3.28.0)
Released as 3.28.0. Skill names in game: Iron Dragon Slayer, Take Over: Satan Soul, Crash Magic, Water Body, Ashborn the Shadow Monarch, Chairman Go's Fist, Regis Leywin the Devouring Shadow, Lee Seolhwa Needle and Blade, Shin Yoosung Monster Gate, Lefiya Viridis the Thousand Elf. Juvia's art "Water Body" is "Water Body Form" (id `water_body` belongs to a removed magic). Test sweep: no new failure against 3.27.0.
