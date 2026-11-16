# All Fiction (God class) and Book Maker lead-in: rework

Owner request: "Bookmaker from Medaka Box should be fully reworked to actually be overpowered as like a god class ultimate skill."
Follow-up instructions used here: study how the Tensura Reincarnated Nightmares ultimates scale from Lord to Dragon class and use that curve; reference mods are read-only data, never a dependency.

Nothing here was seen in a game. No Gradle in the authoring session: CI is the compiler.

## 1. Canon (Medaka Box, Kumagawa Misogi)

Sources, in the order rule 10 asks for:

| Source | Result |
|---|---|
| English wiki, `medaka-box.fandom.com` / `medakabox.fandom.com` (WebFetch) | NOT fetched: host does not resolve in this session (not in the allowed domains) |
| English Wikipedia (`en.wikipedia.org`) | NOT fetched: same |
| Japanese wiki / ja.wikipedia (`ja.wikipedia.org`) | NOT fetched: same |
| Web search (secondary snippets only: fan wikis, a VS-battles thread, profile pages) | Fetched, quality low and partly contradictory; used only for the two facts marked (S) below |
| Author's knowledge of the manga | The rest; marked (K) |

What the canon abilities are:

- **Book Maker** (却本作り, a Minus "skill" of Kumagawa's): a giant screw is driven into the target; it "brings the target down to Kumagawa's level" (strips or equalises their strength, powers and confidence) (S, K). This is what the existing Pin / Barrage express.
- **All Fiction** (大嘘憑き, "Everything Is Fiction"): the Book Maker mutated into a power that makes a thing "not have happened". Used on wounds, damage, effects, abilities, blows, even Kumagawa's own death, and offensively on others (S, K). In the manga it is used to undo injuries and attacks, to negate other people's abilities and to erase what has been done; its limits (it cannot undo some things, it has a cost to Kumagawa's own hold on reality) were not verified here and are not modelled.
- Not modelled: Kumagawa's Minus traits (the "Minus" personality, cooperation with Medaka and Zenkichi), and the later forms of the power. Not verified: the exact in-series limits and the exact names of every use.

## 2. The class curve (Tensura Reincarnated Nightmares), and where All Fiction sits

Sources: the Nightmares wiki pages saved by the coordinator in `/tmp/claude-0/tn_wiki/pages/` (tier folders unique 71 / lord 50 / king 11 / god 6 / dragon 4 pages; read for header fields and passive / active counts, wikitext not copied); the mod jar `tensura-reincarnated-nightmares.jar` (lang + config defaults read as data: `NightmareUltSkillConfig$Abaddon / $Azathoth / $AkashicRecords / $Astaroth / $Lucifer / $Nodens`). `tensuranightmares.wiki.gg` itself was not fetched by me (the coordinator's saved copies were). Several God / King pages are marked work in progress on the wiki, and many cost fields are empty ("?" below). The jar and wiki sometimes disagree (Abaddon 500k MP on the wiki, 750k in the jar config). Everything here is structure and scale only.

| Class | Obtain cost | Points to master | Passives | Actives | Biggest listed number | What the class is "about" |
|---|---|---|---|---|---|---|
| Unique | 50-300K MP | 1,000 | median 0 (max 3) | median 2 (max 8) | about 100 damage | one trick, one theme |
| Lord ultimate | 340K - 5M MP (jar: Lucifer 1.7M) | 5,000 (a few 15,000 - 25,000) | median 2 (max 7) | median 4 (max 8) | median 200, one 5,000 | a themed kit; passives start to matter |
| King ultimate | 0.5 - 3.9M MP (jar: Abaddon 750k, Astaroth 3.9M) | 5,000 (some 1,500) | median 3 (2-4) | median 3 (1-6) | about 500 | a resource bar and a power with a price (Abaddon: nihility bar 25k, Rapture 120 s, 600 s cooldown, 1,800 s backlash) |
| God ultimate | 2.5M MP up to 30M MP / 35M EP (jar: Azathoth 20M MP and 40-60M EP, 100 mastered skills, 100 named subordinates; Akashic 20M MP / 60M EP; Nodens 3.4M MP / 40M EP / 150 mastered) | 5,000 | 2-3 | 3-7 | about 650 (Yog-Sotohort), plus rule-level effects | rule-breaking utility: seal movement, skill use and healing (Yog-Sotohort Imprison, 300 s), block-all (Absolute), steal or null defences (Azathoth Null: 50 % dodge negate), create skills (Akashic); a nihility bar of 100,000; ownership limited server-wide ("Truly God Class") |
| Dragon ultimate | 1M MP | 5,000 | 1 | 2 | 1,000 damage at 1.5M MP (Storm Break) | few abilities, one huge number |

Our own classes (`skill/rules/UltimateClass`, docs/ultimate_classes.md), for comparison:

| Class | Damage ceiling | Cost | Max mastery | Notes |
|---|---|---|---|---|
| LORD | x8 | 2 % of max MP (ref 2,000) | 10,000 | |
| KING | x10 | 3 % (ref 3,000) | 25,000 | |
| GOD | x12 (combined cap x36) | 5 % (ref 4,000) | 50,000 | All Fiction is here; the highest class the system has except DRAGON (which is a Black Clover devil track, same cap) |
| DRAGON | x12 | 4 % (ref 3,000) | 15,000 | unused |

Reading of the curve for All Fiction (what changes class by class, and what we took):

1. **Lord to King:** more passives and a "price" mode. All Fiction keeps its price (a 15 minute death refusal, big magicule costs, long cooldowns).
2. **King to God:** the kit stops being damage and becomes **rule-level utility** (seal skill use, block everything, null defences, write a rule) and a **large mode list (3-7 actives)** with **2-3 passives**. Damage barely grows (about 500 to 650) while the utility reaches whole areas. All Fiction's new modes follow that: Deny Skill (seal magic and strip effects), Overwrite Rule (a rule written into an area), Reversal (an absolute block that is written back), Mass Fiction (area erase), Nothing Sticks and Retcon (passives), Rewrite Time (cooldown rewrite). Their damage numbers are small (40, 60, a 1.5x write-back capped at 60 raw) and go through the class-capped balance law.
3. **Mastery gate:** the God pages gate their strongest uses behind mastery and long cooldowns; Mass Fiction is mastered-only and the dearest mode (9,000 flat).
4. **Dragon:** one huge number, few abilities. Not used: All Fiction is a God-class, many-mode skill.

## 3. What is built

Ids are unchanged: `anime_multiverse:book_maker` (unique) evolves into `anime_multiverse:all_fiction` (ultimate, GOD class). Server setting `ultimates_fiction_god.allFictionGod` (default on). Off: All Fiction is the ten-mode 3.46.0 skill, Book Maker the two-mode one. `ultimates_3_46.allFictionRebuilt = false` still gives the old two modes and wins.

### All Fiction (GOD) - sixteen modes (indexes 0-9 unchanged)

| # | Mode | Canon idea | What it does | Cost / cooldown (flat, ticks) |
|---|---|---|---|---|
| 0-9 | Undo, Fiction, Annul Damage, Rewind Step, Erase Effects, Nullify Strike, Unmake, Share the Fiction, Stand Up, World Fiction | unchanged (docs/skills/nen_allfiction.md) | unchanged | unchanged |
| 10 | **Deny Skill** (new) | negate another's ability | the foe you look at loses every effect, its magic is silenced (Tensura `silence`, time through `BalanceLaw.controlTicks`: boss 1 s, player 3 s, mob 5 s, halved on repeats) and it takes a 40-raw hit | 4,000 / 600 |
| 11 | **Overwrite Rule** (new) | write a rule into an area | 12 s zone (10 blocks) where you stood; once a second foes (at most 24) are weakened and silenced (control caps), allies (at most 8) cleansed, hostile projectiles (at most 16) erased; you take half damage inside | 7,000 / 3,600 |
| 12 | **Reversal** (new) | "it never happened", returned | 8 s, the next 3 hits are made not to have happened (taken as 0) and are written back at the striker: 1.5x the hit, capped at 60 raw, through `SkillDamage` (so the player hit cap 40 % and the boss caps apply) | 5,000 / 1,800 |
| 13 | **Mass Fiction** (new, mastered only) | erase what is around | every foe within 14 blocks (at most 24): ordinary creatures are erased (no drops); bosses and players lose their effects, are silenced and take a 60-raw hit; never erases a player or a boss | 9,000 / 2,400 |
| 14 | **Nothing Sticks** (new) | negate an effect | 20 s: harmful effects are cleansed every second | 4,000 / 2,400 |
| 15 | **Rewrite Time** (new) | negate time spent | the cooldowns of the lesser modes (Undo, Annul, Rewind, Erase Effects, Nullify, Unmake, Share, Deny, Reversal, Nothing Sticks) are gone; Fiction, World, Mass, Overwrite Rule and itself keep theirs | 8,000 / 6,000 |
| passive (new) **Retcon** | negate an effect | once a second one harmful effect of yours (the shortest) is removed | free |
| passive (kept) | refuse death | once per 15 min your death is made fiction (`DeathReadyAt`) | free |

Price: every cast goes through `NUSkill.pay`, so the GOD class price applies (5 % of max magicule scaled by flat/4,000, never below the flat price, x0.8 from 75 % mastery). Damage: GOD class scale (cap x36). Milestones (25 / 50 / 75 / 100 %) apply as for every GOD skill.

### Book Maker (unique, the lead-in) - four modes (indexes 0-1 unchanged)

| # | Mode | What it does | Mastery |
|---|---|---|---|
| 0 | Pin (kept) | screw, damage, crawl, weaken, drag down | base |
| 1 | Barrage (kept) | the pin on every foe in a 10-block cone | mastered |
| 2 | **Equalize** (new) | "bring them down to your level": up to 6 boons torn off the target, Weakness III (II base), plus a 5 s silence when mastered | base, silence mastered |
| 3 | **Glimpse of the Fiction** (new) | 5 s window in which a quarter of your max health of harm is made not to have happened, the first taste of All Fiction | mastered |

Equalize 900 / 160 ticks, Glimpse 1,800 / 1,200 ticks. Book Maker still evolves into All Fiction through `SkillEvolution` (entry untouched).

## 4. NEW / REPLACED / KEPT (rule 5)

- NEW: All Fiction modes 10-15 and the Retcon passive; Book Maker modes 2-3; `FictionRules` god-class section; `NUConfig.ALL_FICTION_GOD` / `allFictionGod()`; self-tests `fictiongod.allfiction`, `fictiongod.bookmaker` (`skill/FictionGodSelfTests`, registered in `NUSMP`, prefix `fictiongod` in `smoke.yml` before `scaling`); tests `FictionGodRulesTest`, `FictionGodSourceScanTest`; lang `tools/lang/all_fiction_god.json`; this doc.
- REPLACED: nothing removed. The skill descriptions (All Fiction, Book Maker) are re-worded (new text in the lang file and the code fallback); `UltimatePairSelfTests.fiction` now asks for `wheel(rebuilt, god)` instead of `wheel(rebuilt)` because the default wheel is now sixteen.
- KEPT: ids, the evolution, icon, modes 0-9 with their numbers and lang keys, the health and position memories, the death passive, `FictionRules.wheel(boolean)` and `modes()` (the old counts), the setting `allFictionRebuilt`, every shared file only appended to.

## 5. Worst-case cost (rule 11)

- Per press, All Fiction: Deny one look and one hit; Overwrite Rule one pulse (see below); Reversal tag writes only; Mass Fiction at most 24 creatures (the `SkillTargets` cap): at most 24 sprays of 8 particles through `SkillFx` (nearby players only, inside the particle budgets), at most 24 discards or hits; Nothing Sticks and Rewrite Time tag writes and 10 cooldown writes.
- Per tick (once a second per owner, the tick already existed): Overwrite Rule open: one foe query (at most 24 creatures, two effect applications each), one ally query (at most 8 cleansed) and one projectile query (at most 16 erased); closed: two tag reads. Nothing Sticks: one cleanse. Retcon: a loop over the owner's active effects and one removal. No entity or block spawn anywhere; no chunk load; no per-tick world scan outside a window.
- Damage hooks: a few tag reads per hit received. Reversal writes back at most 3 hits per 8 s. Book Maker's Glimpse hook reads one tag value per hit.
- Network: no new packet. A press marks the skill dirty once (one skill sync).
- State: only in the owner's skill tag (windows as game times, `RuleX/Y/Z/Dim`, `ReversalLeft`, `GlimpseUntil/Pool`); nothing static, nothing keyed by a player held; logout, death and dimension change need no cleanup (windows are game times; Overwrite Rule closes when you leave its dimension).
- Client: no client code added.

## 6. Unverified / not seen in a game / honest caveats

- Canon was not checked against the English or Japanese wiki (not reachable here); All Fiction's exact limits are not modelled.
- `silence` is Tensura's effect found by name (`MirrorWorks.tensuraEffect`), used by other skills of this mod the same way; whether it blocks a particular skill depends on Tensura. "Deny Skill" is therefore silence plus strip, not a hard per-skill seal.
- `instance.setCoolDown(0, mode)` is assumed to clear a mode cooldown (it is the same call `NUSkill.cooldown` makes with a longer value).
- The effect look reuses the existing screw / ring effects (no new model, no new VFX; nothing was made in Blender because no 3D model was added). A dedicated Deny / Rule visual is not included.
- Not included from the Nightmares curve: a server-wide ownership limit for God-class skills (All Fiction is already single-path through the Book Maker evolution), an EP / mastered-skill acquisition gate, a resource bar.
- Everything written without a compiler: CI is the check. The owner decides whether Mass Fiction's erase of every ordinary foe within 14 blocks (no drops) is acceptable on a public server; the setting turns it off with the rest of the god modes.
