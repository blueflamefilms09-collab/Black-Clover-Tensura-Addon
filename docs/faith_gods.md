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
