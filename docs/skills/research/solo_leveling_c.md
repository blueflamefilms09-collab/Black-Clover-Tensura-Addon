# Solo Leveling canon research, batch C: Ashborn (`ashborn_shadow_monarch`) and Go Gun-hee (`go_gunhee_chairman`)

Rule 10 applied: English wiki first, Japanese as the fallback.

## How the data was obtained (honest caveats)
- WebFetch could not open any page: solo-leveling.fandom.com, solo-leveling-arise.fandom.com, sportskeeda.com and breezewiki all failed with `getaddrinfo ENOTFOUND`.
- Only WebSearch (model-written summaries, not page text) worked. English searches were tried first; a Japanese search was the fallback and returned only a few generic points. Each entry is tagged `search (en)` or `search (ja)`. Nothing is a wiki quote.
- Anything not confirmed is marked UNVERIFIED or INVENTED and was not presented as canon. All numbers are this mod's own design.

## Ashborn (`ashborn_shadow_monarch`, the original Shadow Monarch)

| Used in the line | Canon basis | Source |
|---|---|---|
| Titles: Monarch of Shadows, King of the Dead, Greatest Fragment of Brilliant Light (book/line text, ultimate "Monarch of Shadows", art "Black Ray of the Fragment", Fragment of Brilliant Light stat) | Heroes wiki (fandom) summary gives these three titles | search (en) |
| Betrayal: left for dead by his allies, then found the hidden power; book lore, "Grudge of the Betrayed", "Fall of the Betrayed King", stance "Throne of the Dead King" | Former Ruler, betrayed, became the Shadow Monarch | search (en): Sportskeeda, deltiasgaming summaries |
| Killed Baran after the ambush by Rakan and Baran, his army nearly wiped out; art "Baran's Bane", passive "Slayer of Monarchs" | Monarchs wiki page summary | search (en) |
| Dark, shadow and undead theme: ash/grave vfx, "Mantle of the Dead", "Touch of Death", "Rise from the Fallen" | Shadow Monarch / King of the Dead identity; the specific effects are the mod's | search (en) + invented |
| Passing his power to Jin-Woo through the System | Sportskeeda / soapcentral summaries | search (en), only flavour in the description |

Deliberately NOT used so the line stays apart from the existing lines: Shadow Extraction ("Arise"), Shadow Exchange (teleports), Monarch's Domain (stun/pull/aura field) and Ruler's Authority (telekinesis) are Jin-Woo's skills and each already has its own line. This line uses none of their names, none of their art kinds in the same order (they use drain_bite/ring_burst/chain_hit/debuff_cone/pull/mark/ward_reflect, blink/smoke/execute..., stun_ring/pull/buff_aura/shield..., pull/ray_shot/barrage/uppercut/chain_hit...; Ashborn uses ray_shot, barrage, whirlwind, ground_wave, execute, uppercut, heal_pulse, shield), a Wither/Wither-resist and undead-king passive set, and the vfx ASH_FORMATION / SPIRIT_AURA / STONE_SPIKES / LIGHTNING_FIEND on a holy background.

INVENTED (no canon technique names found): every art name and effect, all passives and both finishers other than the title "Monarch of Shadows". Whether Ashborn used daggers or a sword: UNVERIFIED, not used.

## Go Gun-hee (`go_gunhee_chairman`, Hunters Association chairman, S-rank)

Canon is thin; only the following was found, and everything else is INVENTED.

| Used in the line | Canon basis | Source |
|---|---|---|
| S-rank hunter, chairman of the Korean Hunters Association; description, stance names, stat "Weight of Office" | role and rank | search (en) + search (ja) |
| Retired early because his physical abilities declined with age; later looked for healing magic to reverse aging and failed; passive "Healer's Patience", "Retirement Postponed", stat "Old Soldier's Constitution" | background | search (en) |
| Immense strength and speed; arts "Veteran's Straight", "Veteran's Charge: Shift", "Iron Fist", "Iron Fist: Shift", stats | ability summary | search (en) + search (ja) |
| Fights the Frost Monarch in melee and dies; ultimate "Chairman's Last Stand" (name is the mod's own, a nod to that fight) | story summary | search (en) + search (ja) |
| Light energy: art "Chairman's Will" (the name of an ARISE game skill, "gathers the Power of Brilliant Light") and "Brilliant Light: Shift" | Solo Leveling: ARISE wiki summary (game spin-off, not the main story) | search (en); the ARISE name is a game-only skill |

UNVERIFIED and not used by name: one English summary lists "Ruler's Authority (telekinesis)" and "Spiritual Body Manifestation" for him; the sources were hero-wiki style summaries and one claims he held a Ruler's power but could not use it fully due to age. Not used, since both are already in other lines and are not reliably his. His Japanese name appears as 後藤清臣 (Gō Kiyoomi) in the Japanese edition per a search summary (ja), UNVERIFIED, not used. The "Fan SS-rank" ranking is a fan reading, not used.

INVENTED: "Hunter's Directive", "Rally the Hunters: Shift", "Chairman's Order: Hold the Line" (the team buff, on the normal finisher, Resistance), "Weight of Experience" (the Debt), "Monster Hunter of Forty Years" (forty is the mod's number), "Chairman's Presence", "Association Spoils", "Heavy Fists", "Veteran's Composure", both stances.

## Could NOT be fetched
- solo-leveling.fandom.com (Ashborn, Go Gun-hee, Monarchs), solo-leveling-arise.fandom.com, sportskeeda.com, namu.wiki, ja fandom, ja.wikipedia: no page text at all.
- Exact canon description of any Ashborn technique; any named Go Gun-hee technique from the main story.

## Server and lag cost
Data only, as in docs/skills/evolution_ten.md: counters O(1) per event, at most 20 Debt foes per player, 10 foes per burst, cooldown 12000 to 18000 ticks, no entities, no per-tick scans, no new packets.
