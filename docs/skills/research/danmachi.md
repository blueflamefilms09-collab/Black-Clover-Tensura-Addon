# DanMachi canon research: five Unique-Skill candidates

Rule 10 applied: English wiki first, Japanese fallback.

## READ THIS FIRST: how the data was obtained (honest caveats)
- WebFetch could NOT open any page: every call to danmachi.fandom.com failed with `getaddrinfo ENOTFOUND`.
- Direct curl through the session proxy was refused with `403 CONNECT` (policy denial) for danmachi.fandom.com, the JA fandom, ja.wikipedia.org, en.wikipedia.org, wikiwiki.jp/danmachi and fandom.com. This was not worked around.
- The ONLY working channel was WebSearch, which returns short model-written summaries of pages, not the page text. Nothing below is a direct quote of a wiki page. Every entry is therefore "as summarised by search result for URL X".
- Summaries sometimes contradicted each other or the series (examples flagged below). Anything that is not confirmed by at least two independent results is marked UNVERIFIED.
- No exact chant texts were obtained (the search tool declined to reproduce them). Do not invent chants from this file.
- Source tags: [EN-F] danmachi.fandom.com, [EN-M] danmachien.miraheze.org (English fan wiki, used as second English source), [JA] Japanese web pages found by search (wikiwiki.jp/danmachi was listed but not openable; the JA pages used are blogs/guides, not the official ja fandom wiki), [EXT] other (sportskeeda, baidu baike).

## Candidates (summary)
1. Sanjouno Haruhime: Level Boost support caster (Uchide no Kozuchi + Kokonoe). Best evidence, clear level-up growth.
2. Ryuu Lion: Astraea Record (borrowed magic of ten dead comrades). NOTE overlap: her Luminous Wind is already in the game; propose only Astraea Record.
3. Asfi Al Andromeda: "Perseus" all-rounder, skills that temporarily manifest development abilities.
4. Zald: Level 7 whose skill Deus Ambrosia lets him permanently grow by eating (Overeat).
5. Fels: the Sage, healing magic Dia Panakeia and resurrection magic Dia Orpheus (succeeded once in 800 years).

Not chosen / too thin: Wiene (only physical traits and wings found; Luminous Wind claim is UNVERIFIED), Tione (overlaps the Amazon Berserker skill already in game, and a search summary called her Level 6, which looks wrong, UNVERIFIED), Hermes (mostly divine abilities; Arcanum sealed), Kaguya/Alfia/Naaza/Aria/Eina/Lyd/Gros (not retrievable in the searches made).

---

## 1. Sanjouno Haruhime (Hestia Familia, ex-Ishtar Familia)
**Role:** Support mage, Renard (fox-person). Level 1 at introduction, shown as Level 2 by the faction war.
**Sources:** [EN-F] https://danmachi.fandom.com/wiki/Sanjouno_Haruhime ; [EN-M] https://danmachien.miraheze.org/wiki/Sanjouno_Haruhime ; [EN-F forum] https://danmachi.fandom.com/f/p/3416036590223505734 ; [EN-F] https://danmachi.fandom.com/wiki/The_Hero_and_the_Prostitute ; [JA] https://anigala-rew.jp/danmachi-haruhime/ , https://ameblo.jp/izumikudo/entry-12692758005.html , https://detail.chiebukuro.yahoo.co.jp/qa/question_detail/q14178521404 (chant text lives here, not copied).

**Magic**
- **Uchide no Kozuchi** (JA: ウチデノコヅチ, Level Boost / 階位昇華). Temporary rank-up of one target: every ability moves to the same tier of the NEXT level (example: a Level 3 Aisha near level-up fought like a high Level 4). Cannot be cast on herself. Needs a chant and an interval before the next cast. Drawbacks: boosted targets gain less than half normal experience; first-time targets can struggle to control the new power. Rank B per one fan wiki; one fan wiki says "30% of hidden status total", which conflicts with the one-level description (UNVERIFIED, prefer the one-level version). Sources: [EN-F] Sanjouno_Haruhime page, [JA] anigala-rew.jp.
- **Kokonoe** (JA: ココノエ). Special enchant/chain-cast magic (rank D per fan wiki). Makes glowing fox tails; each tail can be loaded with one Uchide no Kozuchi, so several people can be boosted at once. Limit: up to nine tails in theory; five at introduction; six at Level 2. Boost interval still applies. Sources: [EN-F]/[EN-M] Haruhime pages, [JA] ameblo blog (4-person example, "limit 5, past that she collapses from mind drain").
- Her skills/development abilities (e.g. her own passive skills): NOT retrieved, UNVERIFIED.

**Growth moment (canon)**
- Level 1: boost lasts up to about 15 minutes (does not end if she faints, per JA), interval about 10 minutes, 5 tails ([EN-F], [JA]).
- Level 2 (shown at the War Game / faction war against the Freya Familia; one summary says she ranked up after Volume 14 without knowing): duration 20 minutes, interval 9 minutes, Kokonoe up to 6 simultaneous boosts ([EN-F] summary, Baidu baike https://baike.baidu.com/en/item/Haruhime%20Sanj%C5%8Dno/1477652). Two sources give exact 20/9/6; the JA results only confirmed the Level 1 numbers (15/10), not the Level 2 ones.
- Story use: Uchide no Kozuchi plus Hedin's enchantment let Bell briefly match Ottar ([JA] anigala-rew.jp). A forum claim of hour-long boosts is unsourced and should be ignored.
- No Level 3 information found.

**Why it fits Unique Skill -> Ultimate:** A single-target "one tier up" buff that grows by level (longer, shorter cooldown, more simultaneous targets) maps directly to Unique -> Ultimate: Ultimate adds multi-target tails and a bigger boost. Strong multiplayer hook, but keep it server-side bounded (cap targets, cooldown).

---

## 2. Ryuu Lion (Hestia Familia, ex-Astraea Familia)
**Role:** Elf support/all-round fighter, "Gale" (Level 4 at introduction, later Level 6).
**Sources:** [EN-F] https://danmachi.fandom.com/wiki/Ryuu_Lion ; [EN-M] https://danmachien.miraheze.org/wiki/Ryuu_Lion/Abilities ; [EN-M] https://danmachien.miraheze.org/wiki/Astraea_Familia ; [EN-F] https://danmachi.fandom.com/wiki/DanMachi_~Memoria_Freese~/Astraea_Record ; [EXT] https://sportskeeda.com/anime/what-astraea-record-everything-know-ryu-lion-s-new-ability-danmachi-season-5 ; [EXT] https://www.sportskeeda.com/anime/danmachi-season-5-episode-12-ryu-s-astraea-record-brings-back-alise-s-flames-hogni ; [EXT] https://baike.baidu.com/en/item/Ry%C5%AB%20Rion/1440344 ; [JA] fan wiki text via search: https://wikiwiki.jp/danmachi/ (listed, not opened).

**Magic (JA result for Level 6: Luminous Wind, Noa Heal, Astraea Record)**
- **Luminous Wind**: wide-area wind/light attack, summons green star dust. ALREADY IN GAME, do not use.
- **Noa Heal**: listed by the JA summary only, effect not retrieved. UNVERIFIED.
- **Astraea Record** (subtitle "Justice Inheritance"). Third magic. Uses Astraea's ichor remaining in her Falna plus her bond to ten dead Astraea Familia comrades to reproduce THEIR magic: more than ten offensive, defensive, healing and support spells; includes a light-crystal barrier (5 m radius) that blocks any attack. Drawbacks: strength depends on Ryuu's mental state and focus on her comrades; she is less skilled with each spell than its original owner and will need a long time to match them. Anime S5 ep. 12: she reproduces Alise's flames against Hogni; Hogni broke the barrier too late because her chant was already finished. Sources: [EN-F], [EN-M], [EXT] sportskeeda.

**Skills (conflict between sources, UNVERIFIED which names are exact)**
- [EN-M summary] Fairy Serenade, Mind Load, Aero Mana, and a passive "Astrae Varmas" (always on, buffs people near her including herself, effect/range scale with level; said to be named after Astraea and Adi Varma). A second summary called Astrae Varmas her "fourth skill" and also said it amplifies skills and raises magic and mind for same-familia members; treat as UNVERIFIED.
- [JA summary, Lv.6] 妖精星唱 (Fairy Star Song), 精神装填 (Mind Load), 疾風迅雷 (lightning gale). 
- Development abilities: Hunter, Abnormal Resistance, Magic Defense (Lv.4, ranks G/G/I per JA) plus Mage (魔導) added at Lv.6 (JA).
- Techniques: Iai-style "Gokou" tachi learned from Kaguya; Concurrent Chanting (skilled, better than Riveria per Lefiya).

**Growth moment (canon)**
- Level 4 to Level 6 before the faction war: "first consecutive rank-ups in history"; one EN summary says she sat at Level 5 for about three days (UNVERIFIED, not confirmed elsewhere); a second source says Astraea staged it (unlock to 5, train, unlock to 6) to avoid damage; sportskeeda says she went straight to 6. Sources disagree, treat the exact path as UNVERIFIED. Weapon: Alvs Justitia in the anime (a wooden sword "Alvs Lumina" claim in my query was not confirmed).

**Why it fits:** "Inherit the powers of fallen allies" is a natural Unique Skill that starts with one or two borrowed spells and evolves into the full ten-spell Ultimate, with the mental-state drawback as a design hook. Overlap warning: the repo already has Luminous Wind, so name the new skill Astraea Record only.

---

## 3. Asfi Al Andromeda (Hermes Familia captain, alias Perseus / "Versatile")
**Role:** Item maker and all-round captain. Level 4 (publicly reported as Level 2 by familia policy per [JA] pixiv; the Hermes Familia deliberately does not report rank-ups to the Guild per [EN-F]).
**Sources:** [EN-F] https://danmachi.fandom.com/wiki/Asfi_Al_Andromeda ; [EN-M] https://danmachien.miraheze.org/wiki/Asfi_Al_Andromeda ; [EN-F] https://danmachi.fandom.com/wiki/Hermes_Familia ; [EN-F] https://danmachi.fandom.com/wiki/Skills ; [EN-M] https://danmachien.miraheze.org/wiki/Development_Abilities ; [JA] https://dic.pixiv.net/a/アスフィ・アル・アンドロメダ , https://manga-tantei.com/danmachiusfi-14046 .

**Skills (EN summaries; names as given by the search summary)**
- **Trismegistus**: effects of items she wields are strengthened; when she crafts, multiple effects apply and she temporarily manifests Mystery, Alchemy, Mixing and Oathcraft (JA: 神秘, 錬金, 調合, 誓造) with strengthened effects. Drawbacks not found, UNVERIFIED.
- **Special General** (name uncertain): in battle she temporarily manifests Abnormal Resistance, Hunter and Magic Defense (EN: "Abnormal Resistance, Hunter, Magic Defense").
- **Argeiphontes**: high resistance to sleep effects; can be activated deliberately for more resistance at the cost of mind.
- **Andromedas**: weakens her magic in naval battles but sharpens senses near the sea; one wiki labels it "cancelled". UNVERIFIED.
- Development abilities at Level 4: Mystery F, Mixing G, Engraving H (JA: 神秘F, 調合G, 彫金H); Mystery is rare (fewer than five owners in Orario per JA).
- Signature items: a frenzy device that makes monsters attack each other; flying boots (Hermes-style, name UNVERIFIED).
- Magic: none confirmed. (A JA summary placed the temporary-ability effect under "magic", while EN lists it as skills; UNVERIFIED which.)

**Growth moment:** No level-up event found. Only the static fact that development abilities are normally permanent (from Level 2 on) while Asfi's skills "borrow" them temporarily, which is the design-relevant contrast.

**Why it fits:** "Temporarily manifest development abilities" gives a clean Unique (a few borrowed abilities while crafting or fighting) -> Ultimate (all of them at once, stronger). Weaker canon growth story than the others.

---

## 4. Zald (Zeus Familia, Level 7)
**Role:** Former Zeus Familia Level 7 front-liner, "Black Dragon Slayer" era; later villain who fought Ottar. Deceased.
**Sources:** [EN-F] https://danmachi.fandom.com/wiki/Zald ; [EN-M] https://danmachien.miraheze.org/wiki/Zald/Abilities ; [EN-F] https://danmachi.fandom.com/wiki/Zeus_Familia ; [EN-F] https://danmachi.fandom.com/wiki/Ottar ; [EXT] https://grokipedia.com/page/Zald_and_Alfia (not a wiki, low trust) ; [EXT] https://www.dualshockers.com/danmachi-strongest-characters/ .
NOTE: no "Berserker" skill found for Zald; he is Zeus Familia, not Ares Familia.

**Skills / Magic**
- **Deus Ambrosia** (skill; the "Overeat" part): while active he can eat beasts, people and monsters and permanently gains status; gain depends on the meal, usually small, a big gain is hard; the skill has a limit (he could not use Level 10 power at Level 7). Result: physical stats permanently above a normal Level 7 (his last Level 7 listing shows Strength and Endurance S999 per [EN-M]). He can eat inorganic matter and corpses without ill effect.
- **Rea Ambrosia**: flame magic coating his weapon in intense fire (from one EXT-style summary, UNVERIFIED name/effect).
- **Al Questa** (JA 冒力冒身 "Risk Power Risk Body") and **Niggle Gourmet** (JA 肉蹂肉塊 "Meat Trample Meat Lump"): other skills, names only, effects not retrieved, UNVERIFIED.
- Development abilities (one source): Hunter, Abnormal Resistance (E), Crush, Strong Body, Supreme Attack. UNVERIFIED (single source).
- Drawback event: he was permanently weakened by Behemoth poison after eating its flesh; he still survived a hit from a beastified Ottar, Hildis Vini, though it knocked him out.

**Growth moment:** Eating raises his status beyond what Falna updates give: that is the "evolution" built into the skill. Story: Ottar became Level 7 by defeating Zald (Great Feud); first fight Zald won.

**Why it fits:** A "grow permanently by consuming" Unique Skill is a textbook evolve-to-Ultimate; the poison drawback gives a built-in cost. Design caution: a permanent-stat gain must be capped per player and saved per UUID, and the "eat people" flavour should be restricted to monsters.

---

## 5. Fels (the Sage, Ouranos's agent)
**Role:** Immortal Level 4 wizard (800 years old, Philosopher's Stone), patron of the Xenos. "Fool" alias; true identity is a sage of the old kingdom of Altena.
**Sources:** [EN-F] https://danmachi.fandom.com/wiki/Fels ; [EN-M] https://danmachien.miraheze.org/wiki/Fels ; [EN-F] https://danmachi.fandom.com/wiki/Magic ; [JA] https://manga-tantei.com/danmachifells-16073 , https://x.com/danmachi_anime/status/1319672920633991168 .

**Magic**
- **Dia Panakeia**: wide-area healing, coloured light spheres; restores stamina, heals wounds, removes status effects and curses ([EN-F]).
- **Dia Orpheus**: resurrection magic; consumes the caster's entire mind (full Mind), tiny success chance, had failed for 800 years; first success was reviving Wiene ([EN-F]). Based on the Orpheus myth.
- Other spells: not retrieved, UNVERIFIED.
**Skills / abilities**
- Concurrent Chanting (casting while fighting or moving, high difficulty).
- Development abilities: Mage (stronger magic, wider range, efficient Mind use) and Mystery (item crafting; some of his inventions Asfi cannot copy) ([EN-F]); JA confirms 神秘 and 魔導 and says he is the sage who mastered Mystery.
- Items: shockwave gloves, Occulus twin crystals ([EN-F]).
- His Status does not update (he lost the Falna with his back flesh during the Stone ritual per JA), so he never levels up.

**Growth moment:** Dia Orpheus succeeding for the first time in 800 years (Wiene). Not a level-up; a story-event evolution.

**Why it fits:** A heal Unique (Dia Panakeia) -> Ultimate resurrection (Dia Orpheus with a mind-drain and low success chance) is a ready-made evolution path with a strong cost. Caution: his "does not level" fact means the evolution should be tied to the story/boss event, not to a level.

---

## Could not be fetched at all
- Every danmachi.fandom.com page (DNS failure in WebFetch; 403 policy block for curl). Only the search-engine summary of each page was available.
- Japanese wiki (ja fandom / ja.wikipedia / wikiwiki.jp/danmachi): 403, not opened; JA data above came from blogs and guide sites surfaced by search.
- Exact chant texts for all magic.
- Details for: Haruhime's skills and Level 3; Ryuu's skills (names conflict) and Noa Heal; Zald's Al Questa and Niggle Gourmet effects; Asfi's skill drawbacks; Wiene, Kaguya, Alfia, Naaza, Aria, Lyd, Gros, Eina (not reached).
