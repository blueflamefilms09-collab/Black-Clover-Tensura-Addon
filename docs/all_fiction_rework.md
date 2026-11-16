# All Fiction (God class) and Book Maker lead-in: rework

Owner request: "Bookmaker from Medaka Box should be fully reworked to actually be overpowered as like a god class ultimate skill."
Follow-up instructions used here: study how the Tensura Reincarnated Nightmares ultimates scale from Lord to Dragon class and use that curve; reference mods are read-only data, never a dependency.

Nothing here was seen in a game. No Gradle in the authoring session: CI is the compiler.

## 1. Canon (Medaka Box, Kumagawa Misogi): read from the wiki

**Update (canon pass):** the first version of this doc was written from general knowledge because no wiki could be opened. The owner then supplied the Medaka Box wiki (Fandom, rule 10: English wiki first; the Japanese wiki was not needed). Sources are page-level, in the wikitext saved under `/tmp/claude-0/wikis/medaka/` and fetched through the wiki's MediaWiki `api.php`, plus the owner's own summary `/tmp/claude-0/wikis/SERIES_ABILITY_SOURCES.md` section 3. Section 7 is the canon audit and what changed. The guess-era text of this section is kept below, marked superseded, so nothing is lost.

Pages read: `Misogi_Kumagawa` (Abilities, Minuses), `Najimi_Ajimu` (Hundred Gauntlets, Lip Service, Unskilled, Life Zero), `Abnormality` (list of Minuses), `Kumagawa_Incident_Arc`, `Good_Loser_Kumagawa`, `Good_Loser_Kumagawa_Gaiden`, `Mukae_Emukae`, `Shibuki_Shibushi`, `Fude_Ezumachi`, `Real_Eater` and the chapter pages. Not fetched: the Japanese wiki, and the wiki has no separate page called "All Fiction" or "Book Maker" (searches return the character pages; the abilities are sections of `Misogi_Kumagawa`).

<details><summary>Superseded guess-era text (kept)</summary>


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


</details>

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

- (Superseded by section 7.) Canon was first not checked against any wiki; it now is, against the English Medaka Box wiki; the limits that could not be expressed are listed in section 7.4.
- `silence` is Tensura's effect found by name (`MirrorWorks.tensuraEffect`), used by other skills of this mod the same way; whether it blocks a particular skill depends on Tensura. "Deny Skill" is therefore silence plus strip, not a hard per-skill seal.
- `instance.setCoolDown(0, mode)` is assumed to clear a mode cooldown (it is the same call `NUSkill.cooldown` makes with a longer value).
- The effect look reuses the existing screw / ring effects (no new model, no new VFX; nothing was made in Blender because no 3D model was added). A dedicated Deny / Rule visual is not included.
- Not included from the Nightmares curve: a server-wide ownership limit for God-class skills (All Fiction is already single-path through the Book Maker evolution), an EP / mastered-skill acquisition gate, a resource bar.
- Everything written without a compiler: CI is the check. The owner decides whether Mass Fiction's erase of every ordinary foe within 14 blocks (no drops) is acceptable on a public server; the setting turns it off with the rest of the god modes.

## 7. Canon pass (wiki-sourced): what the wiki says, what was invented, what changed

Server setting `ultimates_fiction_god.allFictionCanon` (default on; needs `allFictionGod`). Off = the sixteen-mode All Fiction and four-mode Book Maker of the first rework, exactly as before (Pin damage 10 / 16, no level-down, no Minus rule, no new modes). Source tags: W:<page> = English wiki page (Fandom), S = the owner's summary file. Where the wiki is silent the guess is marked DESIGN and is never presented as canon.

### 7.1 Canon facts (per page)

| Fact | Source |
|---|---|
| All Fiction (大嘘憑き, "The Great Liar"): makes anything "nothing", denies aspects of reality; undoes damage, so he is careless about destruction; undoes his own death and others' | W:Misogi_Kumagawa > Minuses; S |
| Erased things (wounds, sight (Zenkichi blinded), memories, his own aura "Lack of Presence", people (Zenkichi made to disappear), colours: the world turned black and white) | W:Misogi_Kumagawa (Abilities, Minuses), W:Kumagawa_Incident_Arc, W:Good_Loser_Kumagawa_Gaiden |
| Drawback: once removed it cannot be returned (it became fiction); he cannot control it fully, careless use could erase the whole world | W:Misogi_Kumagawa > All Fiction; W:Kumagawa_Incident_Arc |
| Limit: it cannot erase another Minus (Emukae's Raff-Rafflesia: only the unwanted part, the missing off switch, was erased; Emukae then controlled it) | W:Misogi_Kumagawa (ch. 84), W:Mukae_Emukae |
| Limit: no actual damage means All Fiction does not work (Ezumachi made the body believe it was bruised) | W:Fude_Ezumachi, W:Kumagawa_Incident_Arc |
| Drawback: without Hundred Gauntlets it deteriorated (could not erase what has strong emotional value, e.g. wounds one is proud of; could not heal some wounds); Ajimu's Hundred Gauntlets restored full potency | W:Misogi_Kumagawa, W:Najimi_Ajimu |
| Hundred Gauntlets (手のひら孵し, "Palm Hatcher"): a skill that reverses causality; All Fiction was born from it. Ajimu: All Fiction interacts with the law of causality and he cannot die because of it | W:Najimi_Ajimu |
| April Fiction (安心大嘘憑き): All Fiction mixed with Unskilled, flat-end-thread screws; the erase lasts at most three minutes, then returns to normal | W:Misogi_Kumagawa (ch. 174, 176) |
| Non Fiction (虚数嘘憑き): All Fiction upgraded to undo what was already undone; "never seen in use" | W:Misogi_Kumagawa (Final Chapter) |
| Unskilled (実力勝負): "don't use skills" skill, nullifies and blocks the user's skills for at most three minutes; Ajimu left it to Kumagawa | W:Najimi_Ajimu (ch. 168, 189) |
| Life Zero (Ajimu, not Kumagawa): nullify any skill, even The End | W:Najimi_Ajimu |
| Book Maker (却本作り, "Refuse the Book-Maker"): his original Minus (lost to Ajimu, regained in the dream), needs a slotted (-) screw, not his usual Phillips (+) screws; the screw stretches to sword length | W:Misogi_Kumagawa > Book Maker; S |
| Book Maker effect: the stabbed target is brought down to Kumagawa's level in body, spirit, technique, intellect and talent (made a Minus); damage nearly zero, "only soils the heart"; hair turns white; it reverts when the effect wears off and the screw dissolves | W:Misogi_Kumagawa; W:Abnormality ("Turns the target into a Minus by bringing that person to Kumagawa's level") |
| Book Maker proved ineffective against Medaka (the screw crumbled) | W:Kumagawa_Incident_Arc, S |
| Book Maker seals Ajimu's skills ("sealed by Book Maker and All Fiction", "cannot use the totality of her skills"; the seal fades) | W:Najimi_Ajimu |
| Leg Eating Forest (足喰の森): flat-end screws all over the floor, both he and the foe must balance on them; in truth all are hidden Book Maker screws | W:Misogi_Kumagawa (ch. 175-176) |
| He can use both Book Maker and All Fiction after Munakata kills him; before that it was one or the other (All Fiction came from Ajimu, losing the original Minus) | W:Misogi_Kumagawa |
| His ordinary screws are many large Phillips screws he conjures and throws with great accuracy "regardless of what Minus he has"; he also uses weak-point combat | W:Misogi_Kumagawa > Abilities |

### 7.2 Our modes against the wiki

| Mode | Verdict | Basis |
|---|---|---|
| Undo, Annul Damage, Share the Fiction | canon-based | undoing wounds, erasing damage (W:Misogi_Kumagawa; W:Execute_the_Student_Council: heals everyone) |
| Fiction (erase) | canon-based | people made to disappear. Now also canon-limited: cannot erase a Minus; a boss is not erased (the mod's guard, S "boss guard") |
| Erase Effects | canon-based | Emukae's Minus erased; effects removed. Now refuses when there is nothing to erase |
| Stand Up / the death passive | canon-based | undoes his own death; "cannot die" (W:Najimi_Ajimu). The 15-minute cooldown is DESIGN |
| World Fiction, Mass Fiction | loosely canon | "changes the world to black and white", "could erase the whole world" (W:Good_Loser_Kumagawa_Gaiden). The radius, the 24 cap and mastered-only are DESIGN (the careful use the wiki says he lacks) |
| Rewrite Time | DESIGN, renamed in lang to "Hundred Gauntlets" | Hundred Gauntlets "reverses causality"; resetting cooldowns is our expression of that |
| Deny Skill | DESIGN, renamed in lang to "Unskilled" | Unskilled / Life Zero block skills; the wiki gives Unskilled to Kumagawa. The balance law caps the seal to 1 s boss, 3 s player, 5 s mob, so the canon three minutes is not reachable (by design) |
| Rewind Step, Nullify Strike, Unmake (projectiles), Annul pool numbers | DESIGN | extrapolations of "make it not have happened"; no wiki use shown |
| Overwrite Rule | DESIGN, not Kumagawa | the wiki gives rule-adding to Shiranui (W:Real_Eater, Condition Bon Bon in W:Abnormality), not to All Fiction. Kept (rule 2), flagged |
| Reversal (writes harm back at the striker), Retcon passive, Nothing Sticks | DESIGN | no wiki basis; kept |
| Pin | canon-based | his conjured screws (W:Misogi_Kumagawa > Abilities); damage was invented (10 / 16), canon-pass: 1 |
| Barrage | canon-based | he attacks a whole class with screws and undoes the damage (W:Good_Loser_Kumagawa) |
| Equalize | the canon Book Maker | "bring down to your level", seals (W:Najimi_Ajimu) |
| Glimpse of the Fiction | DESIGN | the wiki has him use both Minuses only after Munakata; "a little of the harm undone" is ours |
| The evolution Book Maker -> All Fiction | DESIGN | the wiki has All Fiction come from Hundred Gauntlets and Book Maker is the original Minus lost and regained. Kept (rule 2: ids and evolution stay); owner decision whether to change it |

### 7.3 What changed (canon pass)

NEW (appended; all behind `allFictionCanon`):
- All Fiction mode 16 **Erase Presence** (W:Misogi_Kumagawa "Lack of Presence"): invisible 20 s, non-boss creatures hunting you lose you (at most 24). 3,000 flat / 60 s.
- All Fiction mode 17 **April Fiction** (W:Misogi_Kumagawa): the look foe loses its effects and its strength is erased (Weakness III) for a time that ends by itself: creature 3 minutes (canon), player 30 s, boss 20 s (DESIGN, the balance law's spirit). 5,000 / 120 s. Not on a Minus.
- All Fiction rule: **cannot erase another Minus**: a player holding Book Maker or any Ultimate skill (the "Minus equivalent", DESIGN) only loses its effects (Fiction, April Fiction refuses, Mass Fiction strips and hits instead of erasing). A boss was already guarded.
- All Fiction rule: **nothing to erase, nothing happens**: Undo (no missing health, no harmful effect) and Erase Effects refuse without paying.
- Book Maker mode 4 **Leg Eating Forest** (mastered only): every foe within 8 blocks (at most 24) gets a hidden Book Maker screw (the pin at 0.6x). 2,400 / 30 s.
- Book Maker **level-down**: Pin, Barrage, Forest and Equalize now pull the target's attack and armour down toward yours (transient attribute modifiers: the gap above your value times 60 % base, 100 % mastered, half on a boss; Pin does half of Equalize's share; 12 s base, 20 s mastered, 5 s boss), speed through the existing crawl (balance-law control caps), then it reverts by itself (one delayed task, `SpellRuntime.later`). Pin damage is 1 raw ("almost zero"). Not on another Minus ("failed against Medaka"): Equalize refuses for free, the others skip the level-down.
- A white ring of effect on a levelled target (stand-in for the white hair).
- Pure rules in `FictionRules` (appended), setting `NUConfig.ALL_FICTION_CANON` / `allFictionCanon()`, tests `FictionGodRulesTest` (four new tests), `FictionGodSourceScanTest` (one new test), self-test `fictiongod.canon`, lang `tools/lang/all_fiction_canon.json`.

REPLACED (display text only; ids, keys and save data unchanged): the lang names of mode `fiction.deny` Deny Skill -> "Unskilled", `fiction.rewritetime` Rewrite Time -> "Hundred Gauntlets"; the two skill descriptions; Pin's damage in the canon pass (10 / 16 -> 1; the setting restores it); the first-version "Canon was not checked" caveat. `UltimatePairSelfTests.fiction` and `FictionGodSelfTests` now ask for the three-argument wheel; `FictionGodSourceScanTest` asserts `FictionRules.wheel(rebuilt(), god(), canon())`.

KEPT: every id, the evolution, all sixteen modes with their numbers, Glimpse, Overwrite Rule, Reversal, Retcon, Nothing Sticks, the settings `allFictionRebuilt` and `allFictionGod`, the old two-argument `wheel`, `bookMakerModes(boolean)`, `erases`, the Balance Law caps (boss per-second 6 % max HP, player hit cap 40 %, boss control 1 s; every new silence or slow still goes through `BalanceLaw.controlTicks`; April Fiction and the level-down are not control and not damage), no Time Magic VFX touched.

### 7.4 What the wiki did not give, and what is not expressed

- Not stated: any numbers (ranges, durations other than April Fiction's three minutes and Unskilled's three-minute maximum), All Fiction's cost, or how many things at once. All numbers in the mod are DESIGN.
- Not expressed: the "cannot return what was erased" drawback for ordinary creatures is real (Fiction discards, nothing restores it) but the wiki's late "restore erased things" (Non Fiction) is not built because the wiki says it is never seen in use. April Fiction's "erased things come back after three minutes" is expressed as an effect that wears off, not as removing and respawning a creature (that would need storing the creature's data; unbounded, so not built).
- Not expressed: the deterioration (strong emotional attachments) and Hundred Gauntlets restoring it; the "could erase the whole world" accident (our caps are the careful use); Kumagawa's personality change; the white hair as a model or skin change (needs a client render change and a Blender model, not made), and the screw's sword-length stretch is only the existing screw effect. The manga look is carried only by the existing screw shapes and rings plus the white ring; no new VFX was built, nothing made in Blender.
- Stats: the wiki says body, spirit, technique, intellect and talent fall to his level. The mod has no intellect or talent stat; attack, armour and (through the crawl) speed stand in for them. Not stated: what exactly counts as his level; ours is the caster's own attribute value, never below it.

### 7.5 Worst-case cost of the canon pass (rule 11)

- Per press, Book Maker: Pin one target; Barrage and Forest at most 24 targets (the `SkillTargets` cap), each with two attribute reads and writes, one delayed task and one ring; at most 24 queued delayed tasks per press (each lives at most 20 s), cooldown at least 12 s per player. The list is the existing `SpellRuntime` queue (shared, not per-player; each entry is one closure over the target).
- Per press, All Fiction: Erase Presence one capped foe query and at most 24 target clears; April Fiction one look, two effect writes; the Minus check is one loop over the target's learned skills (a few dozen) per cast and only for players; Undo and Erase Effects checks read the active effects once.
- Per tick: nothing new (no new tick work; the level-down is removed by its delayed task).
- Network: no new packet; the attribute changes sync as the vanilla attribute update to nearby trackers (two small updates per target per press and per removal).
- Client: no client code, no new draw cost beyond the existing ring (24 at most).
- State: transient modifiers (never saved) and one `long` in the target's persistent data (stale value harmless); no static player map; nothing needs cleanup on logout, death or dimension change (modifiers do not survive a reload; the delayed task skips a removed entity).
- Not seen in a game; no compiler in the authoring session, CI is the check.
