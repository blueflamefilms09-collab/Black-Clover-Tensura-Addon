# The eight Faith gods

Owner request: a better Mitama Faith with the 4.3.0 faith system, where players can become gods, "some skills will basically give them the abilities of a god", eight god identities with real names from anime characters who become gods or fight gods, each based on an anime in the mod (plus some the owner wants). Later instruction: the god-form Ultimates follow the Tensura Reincarnated Nightmares Ultimate scaling from Lord up to Dragon class. Nothing could be compiled or run in the session (CI is the compiler); everything below is from reading code, wikis and the reference jar.

## 1. The eight

| # | Unique | God form (Ultimate) | Franchise | Class |
|---|---|---|---|---|
| 1 | Faith of Femto | Femto, God of Incarnation | Berserk | GOD |
| 2 | Faith of Lucifero | Lucifero, God of Devils | Black Clover | DRAGON |
| 3 | Faith of Luminus | Luminus, Goddess of Holy Light | Tensura | KING |
| 4 | Faith of Mavis | Mavis, Goddess of Fairy Law | Fairy Tail | LORD |
| 5 | Faith of Ill Ilah | Ill Ilah, God of the Rukh | Magi | KING |
| 6 | Faith of Super Saiyan God | Super Saiyan Blue, God of Divine Ki | Dragon Ball | GOD |
| 7 | Faith of Ra | Ra, God of the Sun | Yu-Gi-Oh! | KING |
| 8 | Faith of Aqua | Aqua, Goddess of Water | Konosuba | LORD |

Registry ids: `faith_of_<femto|lucifero|luminus|mavis|ill_ilah|super_saiyan_god|ra|aqua>` (Unique) and `femto_god_of_incarnation`, `lucifero_god_of_devils`, `luminus_goddess_of_light`, `mavis_goddess_of_fairy_law`, `ill_ilah_god_of_rukh`, `ssj_blue_god_of_divine_ki`, `ra_god_of_the_sun`, `aqua_goddess_of_water` (god form). `faith` and `goddess_of_faith` are untouched.

## 2. Franchises the mod covers (step 1) and the choice

Read from `data/anime_multiverse/relics/franchise_map.json`, `franchise/FranchiseTabs` (tabs: Black Clover, Tensura, Berserk, Fairy Tail, Magi, hero weapons / Shield Hero, anime worlds) and the skill packages: Black Clover, Tensura, Berserk, Fairy Tail, Magi, Dragon Ball (Ki, Saiyan), Hunter x Hunter (Nen), Fate (Gate of Babylon), Fire Force, Medaka Box, DanMachi, Shield Hero, SAO / Log Horizon, Konosuba, Overlord, Re:Zero, Mushoku Tensei, Frieren, Jujutsu Kaisen, Solo Leveling, Eminence in Shadow, Made in Abyss, Delicious in Dungeon, Tower of God, Fullmetal Alchemist, Seven Deadly Sins, Yu-Gi-Oh!, Tomb Raider King, plus D&D relics.

Eight gods from eight franchises: the five franchise tabs (Black Clover, Tensura, Berserk, Fairy Tail, Magi), Dragon Ball (Ki is one of the biggest systems), Yu-Gi-Oh! (it has the Egyptian God Cards) and Konosuba (a goddess worshipped by a cult, also what the reference mods build a Faith for). **Kept as ready candidates** (one table row plus ids appended at the end; none were dropped for lack of canon): Fate (Gilgamesh, two thirds god, and Enkidu, the Chains of Heaven made to bind a god), Hunter x Hunter (Netero's Guanyin Bodhisattva, built from daily prayers of gratitude; wiki confirmed), Fullmetal Alchemist (Truth, "a pantheistic godlike being" that regulates alchemy, and Father; wiki confirmed), Overlord (Ainz and Nazarick), Mushoku Tensei (Orsted the Dragon God against Hitogami), Solo Leveling (the Shadow Monarch against the Rulers), Seven Deadly Sins (the Goddess Clan). **Missing on purpose:** DanMachi is not an extra line: its gods (Hestia, Freya, Loki, Hephaestus) already exist as skills and are the "hearth, trickster, forge" roles the owner called weak names; Fire Force (Shinra becomes the Sun God) and Medaka Box belong to the parallel rework of those skills and are not touched.

## 3. Sources (rule 10: English wiki first, Japanese fallback)

| Fact | Source | Result |
|---|---|---|
| Femto: Griffith sacrifices his followers to become Femto of the God Hand, Incarnation Ceremony, Apostles, Falconia, causality and spatial manipulation | en.wikipedia.org "List of Berserk characters" | fetched |
| Super Saiyan God: Goku has fellow Saiyans perform a ritual, form beyond it is Super Saiyan Blue, Beerus God of Destruction sought him | en.wikipedia.org "Dragon Ball Super" | fetched |
| Ill Ilah the deity sealed by King Solomon, corrupted by David who seeks godhood, fought by Aladdin and the Magi | en.wikipedia.org "Magi: The Labyrinth of Magic" | fetched |
| Netero's Guanyin (candidate), Truth (candidate) | en.wikipedia.org HxH characters, Fullmetal Alchemist | fetched, not used in the eight |
| Konosuba, Fairy Tail, Black Clover | en.wikipedia.org series pages | fetched but no character detail (Lucifero, Zagred, Licht: no match) |
| Lucifero as the devils' king | the mod's own boss roster (`docs/black_clover_bosses.md`) | not verified on a wiki |
| Luminus (worshipped as a goddess), Mavis (Fairy Sphere, Fairy Law, Fairy Glitter), Ra (sacrifice of three monsters), Aqua (goddess of water, Axis Church) | my own knowledge of the series | **not fetched** |
| Everything on fandom (blackclover, fairytail, ...) | fandom.com | HTTP 403 for every try |
| Japanese wiki fallback (ja.wikipedia / ja fandom) | | **not completed**: the page reached with HTTP 200 once, the API and later pages answered 429 (Wikimedia rate limit on the shared proxy); Yu-Gi-Oh! and Konosuba character pages hit the same 429 |

**Honest flags:** Mavis is a guild guardian, not a god, in canon (weakest fit; alternative Acnologia, the Dragon King, or the Celestial Spirit King); Lucifero is a devil king whom devils serve, not a god of the heavens; Luminus, Ra and Aqua rest on memory until the wikis can be reached.

## 4. Class table (the Nightmares scaling, Lord up to Dragon)

Source: the Tensura Reincarnated Nightmares wiki (tensuranightmares.wiki.gg, wikitext saved by the coordinator in `/tmp/claude-0/tn_wiki/pages/<tier>__<Name>.wiki`, counted by a script: lines starting `*[Passive` / `*[Active`), the Nightmares jar lang (23 Lord skills average 4.4 modes, 4 King skills 5.5) and `Mechanics/Faith`. Reference only: no text or class copied, no dependency.

| Class | Wiki pages | Passives (wiki) | Actives (wiki) | Obtain cost (wiki) | EP asked (wiki) | Here: actives / passives | Strength | EP gate to evolve | Mastery (existing `UltimateClass`) |
|---|---|---|---|---|---|---|---|---|---|
| LORD | 50 | avg 2.6, 0 to 7 | avg 4.4, 0 to 8 | median 1.5M MP (300k up) | about 2M to 3M | 4 / 2 | x1.00 | 2,000,000 | 10,000 |
| KING | 11 | avg 2.7, 0 to 4 | avg 3.4, 0 to 6 | median 2.4M MP | median about 15M (0.1M to 30M, 2 samples) | 4 / 3 | x1.25 | 15,000,000 | 25,000 |
| GOD | 6 | 2 to 3 | 3 to 7 | 2.5M MP up to 35M EP | 10M to 30M | 5 / 3 | x1.60 | 30,000,000 | 50,000 |
| DRAGON | 4 (2 with data) | 1 | 2 | 1M MP | none on the wiki | 3 / 2 | x1.35 | 5,000,000 (my choice) | 15,000 |

The Unique half of every line is 3 actives and 1 passive at x0.6, unclassified. Counts round the wiki averages up so a god form never has fewer abilities than its Unique. The wiki's MP "obtain cost" is not charged here (the evolution is mastery plus the EP gate, like every other Ultimate pair); it is listed so the owner can decide. Class placement: Aqua and Mavis LORD (a healer and a guardian), Luminus, Ill Ilah and Ra KING (rulers of a faith, of the Rukh, of the sun), Femto and Super Saiyan God GOD (the two who literally ascend), Lucifero DRAGON (the repo's Dragon track is the Black Clover devil track, `docs/ultimate_classes.md`).

## 5. What being a god gives (3 to 4 abilities in the Unique, up to 8 in the god form)

Each line has a pool of 5 actives and 3 passives; the Unique uses the first 3 and 1, the god form the first `actives` and `passives` of its class. Every active has a Faith price (1 to 3), a magicule price (through `NUSkill.pay`, so the class cost law applies), a cooldown and a reach. The verbs reuse existing code: BLESS and PURGE are the Goddess of Faith's Blessing and Miracle, BLINK is Loki's relic blink, hits go through `SkillDamage` (BalanceLaw) and heals through `BalanceLaw.heal`.

| God | Actives (pool order) | Passives (one potion effect each) | Ritual offering |
|---|---|---|---|
| Femto | Eclipse (domain), Spatial Step (blink), Causality Lance (beam), Falconia (ward), Incarnation (surge) | Falcon of Light (sight), God Hand Vigor (regen), Apostle Might (strength) | 8 ender eyes |
| Lucifero | Devil's Dominion (domain), Soul Rend (drain), Underworld Maw (beam), Blood Pact (surge), Infernal Veil (ward) | Devil's Hide (resist), Dread Sight, Hellfire Blood (fire) | wither skeleton skull |
| Luminus | Divine Judgment (smite), Blessing of Luminus (bless), Holy Cleansing (purge), Eternal Night (bind), Sacred Mending (heal) | Holy Aura (regen), Vampiric Vitality (absorb), Night Sovereign (sight) | 3 golden apples |
| Mavis | Fairy Sphere (ward), Fairy Law (smite), Fairy Glitter (beam), Tactician's Call (bless), Guild Bond (purge) | Fairy Heart (regen), Tactician's Eye (sight), Guild Haste | 16 emeralds |
| Ill Ilah | Rukh Storm (smite), Wings of Rukh (ward), Destiny Binding (bind), Rukh Blessing (bless), Rukh Beam (beam) | Rukh Flow (speed), Rukh Sight, Rukh Vitality (regen) | 16 amethyst shards |
| Super Saiyan God | God Ki Blast (beam), Divine Dash (blink), Red Aura (surge), Ritual of Six (bless), Destruction Wave (smite) | Divine Ki Vitality (regen), Divine Strength, God Ki Sense (sight) | 6 redstone blocks (the six Saiyans) |
| Ra | God Blaze Cannon (beam), Phoenix Flames (smite), Mighty Sacrifice (drain), Sun's Blessing (heal), Wings of Ra (surge) | Solar Vigor (regen), Sun's Fire (fire), Radiance (sight) | 32 gold ingots |
| Aqua | Sacred Cure (heal), Purify (purge), Turn Undead (smite), Goddess's Blessing (bless), Water Goddess's Decree (beam) | Divine Protection (resist), Water's Grace (water breathing), Goddess's Cure (regen) | 16 prismarine crystals |

Passives work while the skill is switched ON (the usual toggle). The first passive is shared with the god's online worshippers within 32 blocks (at most 16 of them), the Nightmares "the faithful gain a Secret of Faith" idea.

### How worshippers and offerings feed it (the 4.3.0 system)
- **Faith is the fuel.** Every active burns Faith from the player's Faith / Goddess of Faith skill (or from the god skill itself when an admin gave it without Faith). Worshippers' `/faith offer` and Pray fill it; the worshippers' slow trickle refills it.
- **Devotion.** Each worshipper adds 4 % to the strength of every active (cap x2.0), set by `devotionPercent`. From 8 worshippers the passives are one level stronger.
- **Ritual feed.** Two of the four ritual proofs are the worshipper count and the Faith they have offered.

## 6. How to become a god (survival)

`/faith gods` lists them; `/faith ascend <id>` runs the ritual, all of it checked on the server: you hold Faith or Goddess of Faith, you stand on a **gold block** (the shrine), you carry the god's **offering** (table above, consumed), the ritual's price in Faith (40) is paid, and **three of four proofs** hold: you own an Ultimate skill, you have 4 worshippers, they have offered 20 Faith, your Faith is mastered. This is the Nightmares "become a god" rule (three quarters of the requirements) mapped to our system. One god line at a time (`maxGodLines`), one ritual per 5 minutes. You then learn "Faith of X". Master it and, once your EP reaches the class gate, it evolves into the god form (the same 5-second check as the other Ultimate pairs; the Faith tag and worshippers carry over). Admin: `/anime_multiverse faithgod give <player> <id> [ultimate]`.

## 7. Settings (SERVER, section `faithGods`)
`enabled` true (false: the god skills do nothing, the ritual refuses, nothing evolves, Faith is exactly 4.3.0), `ascendFaithCost` 40, `minWorshippers` 4, `minOffered` 20, `devotionPercent` 4, `powerPercent` 100, `evolutionEpGatePercent` 100 (0 = mastery alone), `maxGodLines` 1, `ascendCooldown` 6000 ticks.

## 8. NEW / REPLACED / KEPT
- **NEW:** 16 skills (8 Uniques, 8 god forms), `FaithGodRules` (table, class table, ritual, devotion), `FaithGodSkill`, `FaithGodMoves`, `FaithGods` (registry, evolution, ritual, commands), `FaithGodSelfTests` (prefix `gods`), 9 server settings, 16 icons (`tools/faith_gods/gen_lang_icons.py`, procedural 32x32), `tools/lang/faith_gods.json` (131 keys), `FaithGodRulesTest`.
- **REPLACED:** nothing. Appended: one line in `NUSkills`, `SkillEvolution`, `NUConfig` (fields, section, accessors), three in `NUSMP`, `gods` in `smoke.yml` before `scaling`.
- **KEPT:** `faith`, `goddess_of_faith`, every 4.3.0 mode, command and setting, the old evolution table (not one `m.put` added), the Time Magic VFX.
- No 3D model, weapon or engraving is involved (rules 6, 7, 13, 14 do not apply); the looks are the existing sigil, ring, beam and spray effects in each god's colour.

## 9. Worst-case cost (rule 11)
- **Per cast:** at most 24 foes and 24 allies (the `SkillTargets` caps, radius at most 28.8 after the milestone), so at most 24 hits or 24 x 4 potion effects; effect packets: one sigil, one ring, a beam adds two, at most 8 ally sprays, the cast circle; all through the existing budgets to players within 96 blocks. Cooldowns are 200 to 1200 ticks per mode.
- **Per second per god with the skill ON:** at most 3 potion effects on the holder and at most 16 UUID lookups and effects on worshippers (`PlayerList.getPlayer`, no world scan, no chunk load). Skill ticking only runs for toggled-on holders.
- **Ritual:** once per 6000 ticks per player: a 41-slot inventory scan twice, one block read, 16 skill lookups. `/faith gods` is 9 chat lines.
- **State:** the Faith and worshipper list already on the skill tag; one rate-limit map keyed by player UUID, at most 256 entries, cleared on logout and server stop. No entity is spawned, no new packet exists.
- **Client draw:** none added.

## 10. Not verified
- Nothing was compiled or run; the API calls were read from existing code in the same package (`DbSkill`, `GoddessOfFaithSkill`, `LokiSkill`). The CI self-test `gods.rules` / `gods.skills` checks the table, the 16 registrations and their mode counts, and that `/faith gods`, `/faith ascend` and the old worship commands all exist after the two command trees merge.
- Never seen in a game: the numbers (damage 8 to 14 raw before the balance law, heal 8 to 10, durations 100 to 600 ticks) are first values, not tested balance; the 30M EP gate may be out of reach on a given server (turn `evolutionEpGatePercent` down).
- The icons are procedural glyphs, not hand-painted art; the effects reuse the existing sigil styles (Faith, Goddess, Will, Hell, Gravity, Fire Force), no new shape was added.
- Canon facts marked "from memory" above are unverified; the Black Clover wiki and the Japanese wikis could not be read.
- The Nightmares class numbers come from a small sample for King (11 pages) and Dragon (2 pages with data).
- Decisions for the owner: keep Mavis or swap to Acnologia or the Celestial Spirit King; whether the wiki's MP obtain cost should be charged at evolution; whether to add the candidate lines (Fate, Hunter x Hunter, Fullmetal Alchemist).

## 11. Canon pass

Owner request: "Rework, set gens to be based off the animes, or ask me for the wikis you need." Sections 1 to 10 were written without wiki access (fandom answered 403, see section 3). This pass redoes the eight lines against the English fandom wikis (wikitext saved by the coordinator under `/tmp/claude-0/wikis/<series>/`, plus the `api.php` JSON for the Tensura pages that had no wikitext file). Nothing was compiled or run here; CI is the compiler.

**How it is wired.** A second table, `FaithGodRules.CANON_GODS`, holds the eight rows (same ids, franchises, classes and pool sizes as `GODS`). `FaithGodRules.pick(god, canon)` chooses between them at run time, so the registered skills never change (ids, mode counts, save data, the `faith` and `goddess_of_faith` skills, the commands). The SERVER setting `faithGods.canon` (default **true**; needs `faithGods.enabled`) selects the table: **false gives the 4.6.0 behaviour exactly** (same moves, strengths, colours, offerings, no unlocks, no toll). Skill names and descriptions are static registry text and do not change with the setting (lang fragment `tools/lang/faith_gods_canon.json`, which the lead merges; its keys override the 4.6.0 keys of moves that share an id).

### 11.1 What is new in the rules (one place, used by every god)

| Rule | Behaviour | Basis |
|---|---|---|
| Unlock order | The Unique's three moves open at 0 / 25 / 50 percent mastery of the skill; the god form has every move. A locked move says when it opens (`gods.locked`). The canon Unique earns max(1, 0.5 percent of its maximum) mastery per cast so the thresholds are reachable. | Design (the wiki has no mastery system; the order of moves follows how the series introduces each power) |
| Worshipper minimum | A move can need N worshippers (`gods.need_worshippers`). | Design, from each god's page (see 11.3) |
| Strength base and devotion | Per god: base factor (Aqua 0.7) and devotion multiplier (Luminus 1.5, Aqua 2.0); Lucifero uses gates instead of the linear devotion: x0.7 under 3 worshippers, x1.0 from 3, x1.3 from 6. Devotion stays capped at +100 percent, the server power percent still applies. | Aqua page ("her power correlates to the devotion of her followers", weakened on the mortal plane); Luminous page (Secret of Faith and Grace); Devil page (a host wields what the gate lets through). Numbers are design |
| Health price | Fairy Law (4 percent of max health per foe beyond the first, at most 24 percent) and Point-to-Point Transfer (30 percent of max health for up to x1.6 damage). Taken only after the move worked, never under one heart, nothing in creative, and the damage bonus is bought only with health really paid. | Mavis page (Fairy Law) and the Ra / Winged Dragon of Ra pages (the life-point cannon). The percentages are design |
| Holy factor | Aqua's Turn Undead and Sacred Exorcism hit undead x1.6 and everything else x0.6 (`EntityTypeTags.UNDEAD`, the tag other skills of the mod already use). | Aqua page (Turn Undead; holy water and aura work on the undead and demons). Factors are design |
| Ritual worshippers | Super Saiyan God refuses the ritual with fewer than 5 worshippers (`gods.need_ritual_worshippers`). | Super Saiyan God page (six righteous Saiyans; the target is the sixth). Five is the count of the other Saiyans |
| Unmastered toll | The Super Saiyan God Unique keeps hunger on the holder (once a second, one effect) until the skill is mastered. | Super Saiyan God page (the form drains the body and ends quickly until mastered). The time limit is modelled as hunger, design |
| Effect colours | The god form may have its own colour (Super Saiyan Blue is blue). | Canon colours where a page names them, otherwise **design** (marked below) |
| Five new verbs | WELL (pull, slow, hit), PRESS (force down, slow, weaken, hit), HOLY, LAW (foes only, life price), LIFE_BEAM; appended after DOMAIN. | New code in `FaithGodCanonMoves` |

### 11.2 Sources and what could not be fetched (rule 10)

English fandom wikis, all read as saved wikitext: Berserk (Griffith, God Hand, Idea of Evil, Beherit, Causality, Void, Band of the Falcon, Endless Eclipse), Black Clover (Lucifero, Devil, Zagred, Unite, Magic Knights vs. Lucifero), Tensura (Luminous Twilight Valentine and its abilities page, Holy magic, Holy Field, Sanctuary, Western Holy Church, plus api.php JSON for Asmodeus, Ahura Mazda, Embrace Drain, Memory End Requiem, Day-Night Inversion), Fairy Tail (Mavis Vermillion, Fairy Sphere, Fairy Law, Fairy Glitter, Fairy Heart, Fairy Heart Form, Fairy Tail Guild), Magi (Ill Ilah, Rukh, Al-Thamen, Dark Metal Vessel, Wisdom of Solomon, Solomon Jehoahaz Abraham, Alma Torran), Dragon Ball (Super Saiyan God, Super Saiyan Blue, Saiyan beyond God, Super Saiyan God race page, The Divine Power), Yu-Gi-Oh! (The Winged Dragon of Ra original, character and Sphere Mode pages, The Sun Dragon Ra, Egyptian God, Ra), Konosuba (Aqua, Axis Order, Magic, Skills). The Japanese wikis were not needed and not used.

**Not fetched or not found:** the Tensura "Ruler of the Dark" has no page on the English wiki (Luminus' title is read from Ahura Mazda, "Lord of Radiance"); Black Clover has no separate page for Lucifero's Gravity Magic (read from the Lucifero page); no wiki gives any damage, cooldown, cost, radius or Faith number (all numbers are design); the Japanese wikis were not consulted; the Nightmares class table (section 4) was not re-read.

### 11.3 The eight gods

Each move row: `slot. Move (verb) - opens at - Faith cost - source page - canon or design`. "Canon" = the move or power exists on that page; the numbers behind it are always design.

**1. Femto (Berserk, class GOD kept).** Pages: Griffith, God Hand, Idea of Evil, Beherit, Causality, Void, Band of the Falcon, Endless Eclipse.
- Moves: 1. Band of the Falcon (BLESS) - open - 2 - Band of the Falcon, Griffith - canon name and role. 2. Eclipse (DOMAIN) - 25 percent, needs 2 worshippers - 4 - Griffith, God Hand, Endless Eclipse - canon event; the "sacrifice needs followers" minimum is design. 3. Gravity Well (WELL) - 50 percent - 2 - Griffith (space and gravity) - canon power. 4. Spatial Distortion (BLINK) - god form - 1 - Griffith (spatial manipulation) - canon. 5. Falconia (WARD) - god form - 2 - Griffith, God Hand - canon.
- Passives: Wings of Darkness (leap), Nigh-Absoluteness (resistance), Causal Awareness (night vision) - Griffith, Causality - canon names, effects design.
- Lore text: the White Falcon wills the Band's sacrifice and is reborn as Femto, fifth of the God Hand; god form name "Femto, Falcon of Light" (canon title). Offering: 1 heart of the sea (design stand-in for the Behelit, which is not an item here). Colours: Unique white 0xE6E6F5, god form blood red 0x9A1030 (the Eclipse sky) - **design**.
- Wiki says: nigh-absolute in the physical world, but not against those outside his story (not modelled).
- 4.6.0 got wrong: Incarnation as a combat surge (it is a one-time ceremony), the invented Causality Lance and Spatial Step, 8 ender eyes, "God of Incarnation".
- NEW: Band of the Falcon, Gravity Well, Spatial Distortion, Wings of Darkness, Nigh-Absoluteness, Causal Awareness. REPLACED: Spatial Step, Causality Lance, Incarnation, the three old passives, the offering. KEPT: Eclipse, Falconia, ids, class, sigil.

**2. Lucifero (Black Clover, class DRAGON kept).** Pages: Lucifero, Devil, Zagred, Unite, Magic Knights vs. Lucifero.
- Moves: 1. Gravity Prostration (PRESS) - open - 3 - Lucifero (Gravity Magic) - canon. 2. Space Warp (BLINK) - 25 percent - 1 - Lucifero (Space Magic) - canon. 3. Half Manifestation (SURGE) - 50 percent - 3 - Lucifero - canon. 4. Crushing Fist (SMITE) - god form - 2 - Lucifero (brute strength) - canon. 5. Heart Seizure (DRAIN) - god form - 2 - Lucifero - canon.
- Passives: Immense Durability, Immense Strength, Hellfire Hide - Lucifero, Devil - canon.
- Faith rule: the gate. A devil cannot enter the living world without a gate and a host wields only what the gate lets through, so worshippers open gates (x0.7 / x1.0 / x1.3 at 0 / 3 / 6) - Devil page, numbers design. Wiki says Anti Magic harms him (not modelled).
- Class: highest-ranking devil and ruler of the lowest underworld; the repo's Dragon track is the Black Clover devil track (`docs/ultimate_classes.md`), so the class stays. **Dragon has 3 actives and 2 passives, so Crushing Fist, Heart Seizure and Hellfire Hide stay in the table but are unreachable until a class change** (flagged for the owner).
- God form "Lucifero, King of Devils" (canon title). Offering: 1 wither skeleton skull (kept). Colour: crimson 0xB01030 for both forms - **design** (the wiki gives no aura colour).
- 4.6.0 got wrong: "God of Devils" (he is the King of Devils, not a god), Devil's Dominion and Blood Pact and Underworld Maw (invented), linear devotion. It was also flagged unverified; now read from the wiki.
- NEW: Gravity Prostration, Space Warp, Half Manifestation, Crushing Fist, Heart Seizure, the gates. REPLACED: all five old moves and passives. KEPT: ids, class, sigil, offering.

**3. Luminus (Tensura, class KING kept).** Pages: Luminous Twilight Valentine (abilities and gear), Holy magic, Holy Field, Sanctuary, Western Holy Church, Asmodeus, Ahura Mazda, Embrace Drain, Memory End Requiem, Day-Night Inversion.
- Moves: 1. Holy Field (DOMAIN) - open - 2 - Holy Field - canon. 2. High Heal (HEAL) - 25 percent - 2 - Holy magic - canon. 3. Energy Drain (DRAIN) - 50 percent - 2 - Embrace Drain - canon. 4. Sanctuary Disintegration (SMITE) - god form, needs 3 worshippers, 4 Faith, cooldown 90 s - Sanctuary (borrows the followers' minds) - canon; the numbers are design. 5. Memory End Requiem (BEAM) - god form - 2 - Memory End Requiem - canon.
- Passives: Godspeed Regeneration, Day-Night Inversion, Dimension Fault - Luminous abilities page, Day-Night Inversion - canon.
- Faith rule: Secret of Faith and Grace; each worshipper counts 1.5 times (design number). Limit: Sanctuary needs the followers and long preparation. Wiki says Holy Field does nothing against holy beings (not modelled).
- Class: Demon Lord with 20,000,000+ EP (between this mod's King gate 15M and God gate 30M) who rules a faith but did not create it: King kept. God form "Luminus, Lord of Radiance" (Ahura Mazda). Offering 3 golden apples (kept). Colours: crimson 0xC2294E, god form gold 0xFFF3B0 - **design**.
- 4.6.0 got wrong: the invented "Goddess of Holy Light" title, Divine Judgment, Eternal Night, Vampiric Vitality, no Sanctuary at all; the 4.6.0 note "from memory" is replaced.
- NEW: Holy Field, High Heal, Energy Drain, Sanctuary Disintegration, Memory End Requiem, Godspeed Regeneration, Day-Night Inversion, Dimension Fault. REPLACED: Divine Judgment, Blessing of Luminus, Holy Cleansing, Eternal Night, Sacred Mending and the old passives. KEPT: ids, class, offering.

**4. Mavis (Fairy Tail, class LORD kept). Owner decision still open: keep, swap for Acnologia, or drop.** Pages: Mavis Vermillion, Fairy Sphere, Fairy Law, Fairy Glitter, Fairy Heart, Fairy Heart Form, Fairy Tail (Guild).
- Moves: 1. Tenrou Illusion (BIND) - open - 2 - Mavis Vermillion - canon. 2. Fairy Sphere (WARD) - 25 percent, needs 2 worshippers - 3 - Fairy Sphere (turns members' bonds and faith into power) - canon; the minimum is design. 3. Fairy Law (LAW) - 50 percent - 3 - Fairy Law (strikes only those the caster truly sees as enemies) - canon; the life price is design. 4. Fairy Glitter (BEAM) - god form - 3 - Fairy Glitter - canon. 5. Fairy Heart Form (SURGE) - god form - 3 - Fairy Heart Form - canon.
- Passives: Fairy Heart, Fairy Tactician, Guild Bond - Fairy Heart, Mavis Vermillion, Fairy Tail (Guild) - names canon, haste is design.
- Class: she is the guild's founder and guardian, not a god, so LORD (a guardian class) is the honest fit; god form "Mavis, the Fairy Tactician" (canon title). Offering 16 emeralds (kept). Colours: cream 0xF5E6A8, god form pale green 0xE8FFD8 - **design**.
- 4.6.0 got wrong: "Goddess of Fairy Law" (not canon), Fairy Law as a plain smite on everything near (canon: foes only), the invented Tactician's Call and Guild Bond moves. The wiki gives no cost for Fairy Law; the life price is design.
- NEW: Tenrou Illusion, Fairy Heart Form, the Fairy Law foe filter and life price. REPLACED: Tactician's Call, Guild Bond (move), Fairy Sphere text. KEPT: Fairy Sphere, Fairy Law, Fairy Glitter ids, class, offering.

**5. Ill Ilah (Magi, class KING kept).** Pages: Ill Ilah, Rukh, Al-Thamen, Dark Metal Vessel, Wisdom of Solomon, Solomon Jehoahaz Abraham, Alma Torran.
- Moves: 1. Rukh's Guidance (BIND) - open - 2 - Rukh, Ill Ilah - canon. 2. Gift of Magic (BLESS) - 25 percent - 2 - Ill Ilah (it gave humans magic) - canon. 3. Magoi Drain (DRAIN) - 50 percent - 2 - Ill Ilah (drains magoi from everything alive) - canon. 4. Black Spot (WELL) - god form, 4 Faith - 1000 MP - Al-Thamen, Rukh (drawn down through the Black Spot) - canon. 5. Black Rukh (SMITE) - god form - 2 - Rukh, Al-Thamen - canon.
- Passives: Great Flow, Source of Fate, Black Rukh Veil - Rukh, Ill Ilah - canon names.
- Faith rule: Al-Thamen worship it and feed black rukh; the god has no will, so plain devotion (design). Wiki says it can be sealed again (not modelled).
- Class: creator of Alma Torran but a will-less lump of power: a King, not a God. **Owner decision open: the page would also support God class.** God form "Ill Ilah, the Black God" (canon). Offering 16 amethyst shards (kept). Colours: white 0xF2F2FF, god form deep violet 0x5B2A86 (black rukh) - **design**.
- 4.6.0 got wrong: a "deity corrupted by David" reading from an encyclopedia summary, Rukh Storm and Wings of Rukh and Destiny Binding (invented).
- NEW: Rukh's Guidance, Gift of Magic, Magoi Drain, Black Spot, Black Rukh. REPLACED: all five old moves and passives. KEPT: ids, class, offering.

**6. Super Saiyan God (Dragon Ball, class GOD kept).** Pages: Super Saiyan God, Super Saiyan Blue, Saiyan beyond God, Super Saiyan God (race), The Divine Power.
- Moves: 1. Ritual of Six (BLESS) - open, needs 2 worshippers - 3 - Super Saiyan God (the six righteous Saiyans) - canon. 2. Invisible Palm Blast (BEAM) - 25 percent - 2 - Super Saiyan God - canon. 3. Aura of a God (BIND) - 50 percent - 2 - Super Saiyan God (godly ki paralyses) - canon. 4. Absorb and Nullify (WARD) - god form - 2 - Super Saiyan Blue - canon. 5. Limitbreaker Kamehameha (BEAM) - god form - 3 - Super Saiyan Blue - canon.
- Passives: Divine Ki, Divine Speed, Godly Ki Sense - The Divine Power, Super Saiyan God - canon names.
- Faith rule: six righteous Saiyans; the ritual refuses under 5 worshippers. Limit: until mastered the form drains the body (hunger), design model of the time limit.
- Class: the form matches a God of Destruction (Beerus used 70 percent of his power against it) yet a mortal reaches it, so GOD kept. The Unique is Super Saiyan God and the god form is Super Saiyan Blue, which is the series' own order. Offering 6 redstone blocks (the six, kept). Colours: Unique red 0xFF4A2A, god form blue 0x38B8FF - canon colours of the forms.
- 4.6.0 got wrong: one red colour for both forms, God Ki Blast, Divine Dash and Destruction Wave (hakai is Beerus'), no ritual rule.
- NEW: Invisible Palm Blast, Aura of a God, Absorb and Nullify, Limitbreaker Kamehameha, the worshipper rule, the toll, the blue god form. REPLACED: God Ki Blast, Divine Dash, Red Aura, Destruction Wave, passive names. KEPT: Ritual of Six, ids, class, offering.

**7. Ra (Yu-Gi-Oh!, class KING kept).** Pages: The Winged Dragon of Ra (original, character, Sphere Mode), The Sun Dragon Ra, Egyptian God, Ra.
- Moves: 1. Sphere Mode (WARD) - open - 2 - Sphere Mode page - canon. 2. God Blaze Cannon (BEAM) - 25 percent - 2 - The Winged Dragon of Ra - canon. 3. Point-to-Point Transfer (LIFE_BEAM) - 50 percent - 3 - Winged Dragon of Ra (the summoner's life points feed the cannon) - canon; 30 percent of max health for up to x1.6 is design. 4. Egyptian God Phoenix (SMITE) - god form - 3 - Winged Dragon of Ra (Phoenix Mode) - canon. 5. Wrath of Ra (SMITE) - god form - 3 - Ra, Egyptian God - canon.
- Passives: Divine Immunity (no spell or trap can pick it out), Winged God Dragon, Blaze of the Sun - Egyptian God, Winged Dragon of Ra - canon.
- Faith rule: three Tributes call Ra, so the ritual offering is 3 gold blocks; the god's power is paid in life points. Class: the strongest of the Egyptian God Cards yet a summoned spirit: KING. God form "Ra, the Winged Dragon". Colour gold 0xFFC830 for both - **design**.
- 4.6.0 got wrong: Mighty Sacrifice as a drain, Sun's Blessing as a heal and Wings of Ra (not canon moves), 32 gold ingots instead of the three Tributes, "God of the Sun".
- NEW: Sphere Mode, Point-to-Point Transfer, Egyptian God Phoenix, Wrath of Ra, Divine Immunity, Winged God Dragon, Blaze of the Sun. REPLACED: Phoenix Flames, Mighty Sacrifice, Sun's Blessing, Wings of Ra, the offering count and item. KEPT: God Blaze Cannon, ids, class.

**8. Aqua (Konosuba, class LORD kept).** Pages: Aqua, Axis Order (plus Magic and Skills for the spell list).
- Moves: 1. Heal (HEAL) - open - 1 - Aqua - canon. 2. Purification (PURGE) - 25 percent - 1 - Aqua - canon. 3. Turn Undead (HOLY) - 50 percent - 2 - Aqua (sends the undead to the afterlife) - canon; the x1.6 and x0.6 are design. 4. Resurrection (HEAL 18) - god form - 3 - Aqua - canon spell, **a heal stand-in: it does not revive the dead here**. 5. Sacred Exorcism (HOLY) - god form - 3 - Aqua - canon.
- Passives: Divine Protection, Goddess of Water (water breathing; she walks on water), Enhanced Vision - Aqua - canon names.
- Faith rule: her power follows the devotion of her followers (each counts double); weakened after her fall, so base x0.7. Limit: only the undead take full damage. Class: a lowest-rank goddess weakened on the mortal plane, healer-specialised: LORD kept. God form "Aqua, Goddess of Water" (kept, canon). Offering 16 prismarine crystals (kept). Colour light blue 0x60C8FF - design.
- 4.6.0 got wrong: Turn Undead as a normal smite on every foe, Water Goddess's Decree and Goddess's Blessing (not canon), "my own knowledge" is replaced by the wiki page.
- NEW: Heal, Purification, Resurrection, Sacred Exorcism, Goddess of Water, Enhanced Vision, the holy factor. REPLACED: Sacred Cure, Purify, Goddess's Blessing, Water Goddess's Decree, Water's Grace, Goddess's Cure. KEPT: Turn Undead (name; now holy), Divine Protection, ids, class, offering.

### 11.4 NEW / REPLACED / KEPT (whole pass)
- **NEW:** `FaithGodRules.CANON_GODS`, `Canon` record, five verbs and the pure rules (`pick`, `rgbOf`, `unlocked`, `masteryGain`, `enoughWorshippers`, `ritualWorshippersOk`, `gateFactor`, `holyFactor`, `lawCostFrac`, `hpPaid`, `lifeBeamFactor`, `tollApplies`), `FaithGodCanonMoves`, the SERVER setting `faithGods.canon`, `tools/lang/faith_gods_canon.json` (107 keys, English), the self-test `gods.canon` (under the `gods` prefix the smoke list already runs), `FaithGodCanonRulesTest`, `FaithGodCanonSourceScanTest`, this section.
- **REPLACED (only while `canon` is on):** the moves, passives, lore text, source line, colours, offerings and strength factors of the eight rows as listed above; the dynamic wheel in `FaithGodSkill`.
- **KEPT:** every registry id, class, franchise, the `faith` and `goddess_of_faith` skills, commands, save data, the whole 4.6.0 table (`GODS`, untouched, selected by `canon=false`), the old lang keys, the Time Magic VFX.
- No weapon, 3D model or engraving is involved (rules 6, 7, 13, 14 do not apply).

### 11.5 Worst case cost (rule 11)
- **Per cast of a new verb:** at most 24 foes (the `SkillTargets` cap). WELL and PRESS: 24 hits, 24 or 48 potion effects and at most 24 vanilla entity-velocity packets (sent by the game to players tracking those foes); Black Spot cools down 1200 ticks, Gravity Prostration 900, Gravity Well 300. LAW and HOLY: 24 hits, one health write for Law. LIFE_BEAM: the same as a beam (24 hits, two effect packets), one health write. The look of a cast is unchanged (one sigil, one ring).
- **Per second:** the Super Saiyan God toll is one effect on the holder; nothing else ticks. No scan, no chunk load.
- **State and packets:** nothing stored (health is read and written on the server at the cast), no new packet type, no new entity. The self-test `gods.canon` reads tables and the registry only.
- **Client draw:** none added.

### 11.6 Caveats and what the owner still decides
- Nothing was compiled, run or seen in a game. The numbers (damage, cooldowns, unlock fractions, health prices, factors) are design; the wiki gives none.
- Names and descriptions are static lang: turning `canon` off restores the behaviour but the skill's displayed description keeps the canon wording.
- Dragon class reaches only 3 actives and 2 passives, so two Lucifero moves and one passive are unreachable; Resurrection is a heal stand-in.
- Owner decisions: Mavis (keep, swap for Acnologia, or drop); Ill Ilah as KING or GOD; the unlock thresholds; whether `ritualWorshippers` 5 is too strict for small servers.
