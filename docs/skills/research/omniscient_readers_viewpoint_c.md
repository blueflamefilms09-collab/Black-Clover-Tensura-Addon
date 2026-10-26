# Omniscient Reader's Viewpoint canon research, batch C: Lee Seolhwa (`lee_seolhwa_sword`) and Shin Yoosung (`shin_yoosung_child`)

Rule 10 applied: English wiki first, Japanese as the fallback.

## How the data was obtained (honest caveats)
- WebFetch could not open any page: omniscient-readers-viewpoint.fandom.com, en.wikipedia.org and en.namu.wiki all failed with `getaddrinfo ENOTFOUND`.
- The only working channel was WebSearch, which returns short model-written summaries of the English fandom wiki and NamuWiki pages, not page text. Every entry is tagged `search (en)`. A Japanese search (全知的な読者の視点から イ・ソルファ) returned nothing usable (the name did not appear in any result).
- Anything not confirmed by a search summary is marked UNVERIFIED and was not turned into a name or number. All numbers in the lines are this mod's own design.

## IMPORTANT finding: Lee Seolhwa is not a swordswoman in the sources found
The design doc (`evolution_ten_c.md`) calls her "Sword of the Constellation party" and fixes the id `lee_seolhwa_sword`. The search summaries of the fandom wiki and NamuWiki describe her instead as a **doctor and poison specialist** (white hair, runs the sickbay of the Company). No source found gave her a sword. The id, requirement (700 kills), rarity and colour are kept as fixed, but the line is built as "a doctor with a thin blade and poison": the blade (cuts, dash, execute) is INVENTED, the poison and medicine are from the search summaries. The lead may want to correct "Sword" in the design doc.
It does not overlap `jihye_twin_blades` (dual-blade duelist), `heewon_righteous_knight` (righteous knight), `sangah_war_room` (strategist) or `beast_communion` (animal communion).

## Lee Seolhwa (line `lee_seolhwa_sword`, "Lee Seolhwa, Needle and Blade")

| Used in the line | Canon basis | Source |
|---|---|---|
| Identity: white-haired doctor of the Company, runs the sickbay, medical skill "extraordinary" | Fandom wiki and NamuWiki summaries: rimless glasses, white hair, mostly runs the sickbay | search (en) |
| Art "Deadly Poison Drop", passive "Poison Master" (poison on hit) | Her skill list includes Poison Master (Rare) and "Deadly Poison Drop"; NamuWiki also: "white-haired woman who uses a deadly poison skill". Two summaries agree on the poison; the exact skill names come from one summary, so treat the names as UNVERIFIED in detail | search (en) |
| Stance "Sickbay Vigil", arts "Emergency Treatment", "Anaesthetic Mist: Shift", passives "Self-Prescribed Rest", "Spoils of the Ward", "Slow Dose" (the debt_store), "Built-up Tolerance" | INVENTED in the spirit of a doctor-poisoner | invented |
| Stance "Poisoner's Poise", arts "Scalpel Cut", "Scalpel Cut: Shift", "Silver Needles", "Mark the Wound", "Mercy Stroke: Shift", passives "Swordswoman's Steadiness", "Exact Incision", "Spotting the Weakness", finisher "Triage Sweep" | INVENTED (the blade and the needles; no sword confirmed) | invented |
| Ultimate "Final Dose" | INVENTED name, not a canon skill | invented |

Not used because not confirmed: a sponsor constellation (UNVERIFIED), any stigma name, the "Star Counting Night" orphanage (one summary said she founded it; another search found no mention, so it was left out), her relationship to Yoo Jung-hyuk (conflicting summaries), a possession by a constellation (UNVERIFIED), exact skill ranks.

## Shin Yoosung (line `shin_yoosung_child`, "Shin Yoosung, Monster Gate")
Distinct from `beast_communion` (Lee Gilyoung's talk-to-animals and pack-howl line): this one is the Beast Lord's gate, guard and flood, with a buff and heal focus for the `ally_buffs` counter.

| Used in the line | Canon basis | Source |
|---|---|---|
| Identity: youngest member of the Company, Kim Dokja's sponsored incarnation | Fandom wiki summary | search (en) |
| Art "Monster Gate" (+ Shift, finisher "Monster Gate: Wide Open"), "Rush of the Gate" | Skill Monster Gate from the Beast Lord attribute (Legend): summons any monster she has domesticated, even from other dimensions. The buff form of the arts is the mod's own mapping | search (en), fandom wiki summary |
| Stance "Beast King's Sensitivity" | Defensive skill that blocks all attacks / grants invincibility for a large magic cost per the wiki summary; here only a heavy-guard stance, not invincibility | search (en) |
| Art "Beast King's Breath" (+ Shift) | The wiki summary only PRESUMES an area breath attack; effect UNVERIFIED, the ray and ring are invented | search (en), UNVERIFIED |
| Ultimate "Disaster of Floods" | Canon title of her adult 41st-round self, who used Monster Gate to flood Nodeulseom with monsters in Main Scenario 5 (the burst is the mod's own design) | search (en) |
| Arts "Tamed Pack's Mending", "Shield of the Pack", "Calm the Wild", all passives (incl. "Flood's Toll", the debt_store), stats | INVENTED in the spirit of a child commander of tamed beasts | invented |

Not used because not confirmed: the "Beast Lord" attribute as a stat name beyond the flavour, the ether-condensed punch (appears in a chapter summary, exact skill name UNVERIFIED), a Dokkaebi link of the 41st-round self (single summary), stigma names.

## Could NOT be fetched
- omniscient-readers-viewpoint.fandom.com (Lee Seolhwa, Shin Yoosung, Shin Yoosung (41st round)), en.namu.wiki, en.wikipedia.org: no page text at all, only search summaries.
- Any Japanese wiki page for either character.
