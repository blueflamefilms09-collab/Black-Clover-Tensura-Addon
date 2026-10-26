# DanMachi research, batch C (release 3.28.0): Lefiya Viridis (`lefiya_viridis_mage`)

Rule 10: English wiki first, Japanese wiki as the fallback.

## What could NOT be fetched
* `danmachi.fandom.com` and `danmachien.miraheze.org` (Lefiya Viridis, /Abilities): hosts unreachable (getaddrinfo ENOTFOUND), WebFetch failed.
* Japanese wikis (atwiki / wikiwiki "レフィーヤ・ウィリディス"): found by search, but only the search summary was read, the pages themselves could not be opened.
* Everything below comes from WebSearch summaries (en: danmachi fandom, miraheze, baidu; ja: atwiki / wikiwiki summaries). No page was read in full.

## Existing DanMachi lines checked (no duplicate)
`liaris_freese` (Bell's growth), `airiel_wind_maiden` (Aiz's wind), `ais_sword_princess` (Aiz, Evolution batch B), `rea_laevateinn` and `wynn_fimbulvetr` (Riveria's fire and ice spells), `luminous_wind` / `ryuu_astrea_blade` (Ryuu), `haruhime_uchide`. Lefiya is a different character. Her line borrows Riveria's frost chant only through Elf Ring (canon: she cast Wynn Fimbulvetr via Elf Ring) and uses none of the Riveria / Aiz kind sets: art kinds {barrage, buff_self, chain_hit, cone_strike, meteor_drop, ray_shot, shield, stun_ring} (Riveria rea: has ring_burst, ground_wave; wynn: has debuff_cone, pull; Aiz: has blink, execute, mark, uppercut), passive kinds {aura, crit, debt_store, dmg_when, mending, regen_when, resist, xp_bonus} (no other DanMachi line has this exact set; the generator reports no identical-set warning for it).

## Lefiya Viridis: canon used

| Used in the line | Canon basis | Source |
|---|---|---|
| Arcs Ray (chain_hit) and Arcs Ray: Shift (ray_shot): homing arrows of light, short chant | Arcs Ray: a homing blast with a short chant; ja summary "光の矢" | en wiki (miraheze) search summary, ja summary |
| Fusillade Fallarica (barrage) and Fusillade Fallarica: Shift (meteor_drop): wide-area fire spheres | wide-area fire magic, ja "ヒュゼレイド・ファラーリカ: 広域の炎攻撃魔法" | ja search summary, en summary |
| Elf Ring: Borrowed Winter (stun_ring), Elf Ring: Borrowed Ward (shield): casts another elf's magic if she knows the chant and understands the effect | Elf Ring, summoning-type magic, only elves' spells, needs full knowledge of chant and effect; alias "Thousand Elf"; in Sword Oratoria episode 3 she casts Wynn Fimbulvetr with it | en wiki search summary, ja search summary |
| Fairy Canon (buff_self) | skill Fairy Canon (フェアリー・カノン / 妖精追奏): magic amplification, doubled for attack magic | en and ja search summaries |
| Double Canon (stance 2, held "Long Chant"; the debt passive "Held Circle") | skill Double Canon (ダブル・カノン): keeps the magic circle of a finished chant on her arm as a ring, one spell at a time, costs mental energy | en and ja search summaries |
| Elf Dust Short Staff (cone_strike) | Twin Staff "Elf Dust": short staff for close combat, long staff for artillery, gems resonate | en search summary |
| Thousand Elf (xp passive, Ultimate "Ultimate Skill, Elf Ring: Thousand Elf") | alias "Thousand Elf" | en search summary |

## UNVERIFIED / INVENTED
* **Argo Vesta**: the search query named it but no summary confirmed it. UNVERIFIED, not used in any name.
* **Via Shirga**: not confirmed, not used.
* Elf Ring: Borrowed Ward is a shield art inspired by Elf Ring's idea (borrowing an elf's spell); the real spell it borrows is not named. INVENTED.
* The numbers, the "Chant Posture" crouch bonus (sneaking bonus), "Elven Ward" aura, "Backlash" crit etc.: the mod's own design in the spirit of a chant mage.
* A Wistoria wiki also has a Lefiya Viridis; it is another character (Dio Grail), not used.
