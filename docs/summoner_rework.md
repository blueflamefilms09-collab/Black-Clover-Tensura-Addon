# Summoner rework (owner request: "Any skills based off of the Summoner from Allen from the Hell Mode anime ... should all just be into that one skill as passives, actives, or mastery based ... change Summoner Lord of Summons to Summoner of Beginnings.")

**Read the "Canon pass" section at the end first: the Hell Mode wiki was read afterwards and corrects the guesses in the research table below (Hawk Eye and Spirit Card are not Allen's skills; the unlock order and Strengthening changed).**

Nothing here was compiled with Gradle (no Gradle in the session; CI is the compiler) or seen in a game. The Minecraft-free rules (`SummonerRules`, `AuraRules`, `Tier`, `SkillMath`) were compiled with plain `javac` and their key assertions run; the Minecraft-facing classes were only re-read.

## Allen's Summoner in Hell Mode (research)
Sources and what could be fetched:
- English wiki (hell-mode.fandom.com, isekai.fandom.com), en.wikipedia.org, ja.wikipedia.org, mystiqora.com, shapes.inc: **could not be fetched** (the session's network refuses these hosts, `ENOTFOUND`). The Japanese wiki fallback also could not be fetched.
- What was used instead: **web-search result summaries only** of the pages above (search snippets of hell-mode.fandom.com `Allen`, `Allen/Status`, `Summoner`; the Wikipedia `Hell Mode` article; WebNovel chapters 4, 18, 67; the Mystiqora season-two summon guides; ABEMA's episode 3 synopsis). Facts below marked (search) come from those summaries; facts marked (author) are the author's memory of the series and are **unverified**.

Allen's abilities as the sources describe them:
| Ability | What it does | Source |
|---|---|---|
| Summoning | calls a card's creature | search (fan wiki) |
| Creation | makes a summon card with MP; the level opens higher ranks (level 1 rank H, level 7 up to rank B) | search (fan wiki) |
| Deletion | removes a card / summon | search (fan wiki skill list) |
| Synthesis | combines two cards of the same rank into a higher-rank card (5 MP; from rank E a magic stone is needed) | search (fan wiki) |
| Strengthening | from Summoning level 3; 10 MP, +10 to two stats of a summon | search (WebNovel chapter text) |
| Grimoire / Storage / Sharing | the grimoire holds the cards (10 slots at the start); Storage and Sharing come later | search (fan wiki, early chapter summary) |
| Families | Insect, Beast, Bird, Grass, Stone (ranks H up to D at the start) | search (a status snapshot of a novel chapter) |
| Quick Summon | deploy a card almost at once for more mana | search (season two blog) |
| Stat reflection | Allen gains part of the attributes of summons that stay active or stored | search (season two blog) |
| Season two summons | Cerby (rank 6 beast that draws attention), Maria (support), a mole that reshapes terrain, Mirror | search (season two blog, fan-written, may be imprecise) |
| Hawk Eye (a bird card sight) and Spirit Card (healing) | the old mod skills are named after Allen's Bird and Spirit cards | author, unverified: the searches did not confirm these two card names |

## What the Summoner is now
`summoner` is the one skill. Mode indexes are append-only (0..4 as before; 3 and 4 only for the Summoner of Beginnings):

| Mode | What | Opens at (mastery) | Anime source |
|---|---|---|---|
| 0 Beast Card | a tamed wolf (unchanged) | start | Creation / Summoning, Beast family |
| 1 Stone Card | an iron golem guardian (unchanged) | start | Stone family |
| 2 Recall | dismiss all your summons (unchanged) | start | Deletion |
| 5 Bird Card | TOGGLE, the old Hawk Eye: night vision, hostile creatures glow (48 at most, 32 blocks, 30 magicules a second) | 15 % | Bird family (author) |
| 7 Strengthening | all your summons within 48 blocks get strength and resistance for 30 s (60 s mastered, 120 s Ultimate; level II as Ultimate); 400 ticks cooldown | 30 % | Strengthening |
| 8 Synthesis | two summons of the same card and rank become ONE of the next rank (health and damage x1.5 per rank; ranks up to 1 / 2 / 3 for Unique / mastered / Ultimate); 200 ticks cooldown | 45 % | Synthesis |
| 6 Spirit Card | TOGGLE, the old Spirit Card: a healing aura (8 blocks, 2 hearts per 2 s) | 60 % | Spirit family (author) |
| passive | stat reflection: while you keep summons, a short effect per card kind (speed for wolves, resistance for golems, slow fall for flying spirits); a level higher with four or more as the Ultimate | 75 % | stat reflection |
| 3 Legion, 4 Wraith Card | unchanged (Ultimate only) | Ultimate | |

Mastery: with the rework a cast earns `summonerMasteryPercent` (0.5 %) of the maximum per weight point so the gates are reachable in about 200 casts; the Bird / Spirit toggles add a slow trickle (a fifth of a cast per 10 s on). Summons are still capped (config 3, mastered 6, Ultimate 12; hard 64), follow and defend their owner, expire, vanish when the owner dies or leaves, and Recall removes them.

Summoner of Beginnings (`lord_of_summons`, class `LordOfSummonsSkill`; registry id kept; evolution `SUMMONER -> LORD_OF_SUMMONS` untouched and the Summoner's tag, with the toggles and carry-over flags, is merged into it): every mode is open from the start, twice the cap, Legion and Wraith Card, Synthesis up to rank 3, the reflection one level higher. Renamed in code (`LordOfSummonsSkill.LORD_TEXT`), in `avatar_skills/lord_of_summons.json`, and by lang (`tools/lang/summoner_rework.json` overrides `anime_multiverse.skill.lord_of_summons`, `.description` and `.msg.summoner.lord_only`; the coordinator's merge must let the fragment's values win over the existing keys).

## Hawk Eye and Spirit Card
Chosen: **inert registered skills plus a migration** (like `LegacyMigration`, but from the player tick because they can be granted at any time). With the rework on, `HawkEyeSkill` and `SpiritCardSkill` cannot be used, toggled or ticked; `SummonerMigration.check` (every 100 ticks per player, two registry look-ups) gives the holder the Summoner (or keeps the Summoner of Beginnings), sets the force flag (`NuSummonerForce`) so the mode is open at once, keeps a toggle that was on, keeps the higher mastery share, and forgets the old skill. Ids stay registered, so old worlds load.

## Setting that restores the old behaviour
SERVER config `summonerRework.summonerRework` (default true). false: three modes (five for the Ultimate), one mastery point a cast, the old Hawk Eye / Spirit Card work again as their own toggles, no migration. `summonerMasteryPercent` (0..5, default 0.5).

## NEW / REPLACED / KEPT
- NEW: modes Bird Card, Spirit Card, Strengthening, Synthesis, stat reflection, synthesis ranks (`nusmp_summon_rank` in the summon's data), `SummonerRules`, `SummonerMigration`, `SummonerSelfTests` (`summoner.rules`, `summoner.skills`), settings, tests `SummonerReworkRulesTest`, `SummonerReworkSourceScanTest`, `tools/lang/summoner_rework.json`.
- REPLACED (owner asked): Hawk Eye and Spirit Card as separate skills (now modes; the ids remain inert); the display name Lord of Summons -> Summoner of Beginnings; the old mastery gain of one point a cast (with the rework on).
- KEPT: all registry ids, the evolution path, mode indexes 0..4, the Card enum and `SummonRules` numbers, the `nusmp_summon` / `nusmp_owner` tags, the old VFX (`SkillAnimeFx2` hawk, spirit, summon), `HawkEyeSkill` / `SpiritCardSkill` pulse code (now shared static helpers).
- Shared files only appended to: `NUConfig` (two settings, two accessors), `NUSMP` (one register line), `smoke.yml` (`summoner` before `scaling`; tail unchanged), `SkillEvents` (one migration line). No `CHANGELOG.md`, `en_us.json` or version change.

## Not included (honestly)
Creating cards as items, Storage / Sharing (the grimoire inventory and party sharing), Quick Summon, the season two summons (Cerby, Maria, mole, Mirror), more families (Insect, Grass), contracts. These are not mod features here; the cards are modes. No new 3D model or texture was made (no Blender work; the Bird and Spirit cards keep their old VFX and icons).

## Worst-case cost
- Server per player: a holder's summons upkeep is the existing once-a-second query; the reflection adds at most 3 short effects a second; the Bird Card pulse is the old one entity query of radius 32/48 once a second (at most 48 glows); the Spirit pulse is one ally query every 2 s; migration is two look-ups every 5 s for everybody and work only for an old holder. Strengthening touches at most the cap (64 hard) summons per press, Synthesis removes two and adds one (no net spawn), no chunk loads, no new entities beyond the cap, all cleaned up as before on death, logout and expiry.
- Network: no new packets; the existing Bird / Spirit / summon VFX throttles stay (Bird 1 per 3 s, Spirit 1 per 3 s).
- Client: no new draw code.

## Unverified
Compilation, all in-game behaviour (modes, toggles through ManasCore's `canTick`/`onTick` for a non-toggled skill, the mode scroll skipping Ultimate-only cards, migration, evolution carrying the tag), balance numbers, and every anime detail marked above. Hawk Eye and Spirit Card as Allen's Bird / Spirit cards are not confirmed by any source reached.

---

# Canon pass (Hell Mode wiki read; supersedes the guesses above where they differ)

The sections above were written without wiki access. The owner then supplied the Hell Mode wiki (hell-mode.fandom.com, wikitext fetched through its public `api.php`). This pass compares the 4.5.0 Summoner with it and corrects it. Setting: SERVER `summonerRework.summonerCanon` (default true, needs `summonerRework`); false = exactly the 4.5.0 behaviour described above (nine modes, the guessed unlock order, speed from wolves, the 75 % reflection gate, timed Strengthening, the old card names).

## Sources per fact (English wiki; the Japanese wiki was not needed, nothing was blocked)
| Fact | Wiki page |
|---|---|
| Starting skills Summoning, Creation, Deletion; the Grimoire holds the cards, 10 slots at the start | `Summoner` |
| Creation: ranks H (Lv 1) to S (Lv 9), costs 2 / 5 / 10 MP, rank E+ needs a magic stone of that rank in Storage | `Summoner` |
| Summoning levels up when Creation, Synthesis and Strengthening reach the same level | `Summoner` |
| Synthesis: opens at Summoning/Creation 2, two same-rank cards into a higher rank, 5 MP, a stone from rank E | `Summoner` |
| Expansion: Summoning 2, +10 slots per level | `Summoner` |
| Strengthening: Summoning 3, 10 MP, two stats, +10 / +20 / +50 / +100 / +200 / +500, stays when the card is summoned again, lost in Synthesis, does not touch Allen's own buffs | `Summoner` |
| Storage: Summoning 4, no levels, 30 x 30 cm opening, time stands still inside, stones are taken from it | `Summoner` |
| Sharing: Summoning 5, free, sight and sound of a summon within 50 m, one more per 200 INT, a summon under 100 INT cannot take orders | `Summoner` |
| Awakening: Summoning 6, 100 MP, a summon's awakened skill, once a day (24 h cooldown), 6 h Soldier, 3 h General, 1 h King | `Summoner`, `Summons` |
| Deputize: after Allen's level 65 (seal broken), free, General +5,000, Recruit +2,500 | `Summoner` |
| The nine types, their order, recipes, creation MP and roles; ranks H..S; type buffs +1 / +2 / +5 / +10 / +20 | `Summons` |
| Soldier +2,500, General +5,000 (three per species), King +10,000 (one per species, Kingnification), awakened cooldown 6 h / 3 h / 1 h, up to 30 days summoned | `Summons` |
| When each skill and type opened (Summoning 1: Insect/Beast H; 2: Bird G; 3: Grass F; 4: Stone E; 5: Fish D; 6: Spirit C; 7: Dragon B; Quick Summoning, Deputize listed at Allen's level 55) | `Allen/Status` (class and summons lists per chapter) |
| Title "Summoner of Beginning", talent Summoner, 8 stars | `Allen` |
| Named summons and their abilities: Appo (Grass F: Aroma, Herb), Chappy (Bird G: Voice Mimic, Warble), Hawk (Bird E: Hawk Eye, Farsight), Wally (Stone E: Harden, Explode), Maria (Spirit C: Psychokinesis), Ellie (Spirit B: Gravity, Black Hole), Okiyosan (Spirit A), Merus (Angel A: Attribute Granting, Angel's Halo, Thunder of Judgement), Tsubame (Bird A), Dradra (Dragon B: Spitting Fire, The Fire of Rage), Denka (Insect H: Swarm), Mousey, Moley, Hayate (Beast), Steely (Stone C: Substitute, Sacrifice), Mira (Stone B: Reflect, Total Reflect) | `Appo`, `Chappy`, `Hawk`, `Wally`, `Maria`, `Ellie`, `Okiyosan`, `Merus`, `Tsubame`, `Dradra`, `Denka`, `Mousey`, `Moley`, `Hayate`, `Steely`, `Mira` |
| Stat buffs a summon gives Allen (the "+" in his status) | `Allen/Status`, `Summons`, each summon page |

## What the wiki says that changes the guess (and what changed)
| 4.5.0 guess | Wiki | Change |
|---|---|---|
| "Hawk Eye = Allen's Bird card" | **Hawk** is a named Bird E summon; Hawk Eye / Farsight are *its* abilities. Bird is a summon **type** (intelligence, information gathering), not a class skill. | Mode 5 stays (sight toggle) and is now the Bird type's role, shown in Awakening as Farsight (the Hawk's awakened skill). The old Hawk Eye skill is not Allen's own: it is kept inert + migrated (below). |
| "Spirit Card = healing" | Nothing named Spirit Card exists. **Spirit** is a type (rank C+) whose role is *long-range attack* (Maria, Ellie, Okiyosan). **Healing is the Grass type** (Appo's Aroma/Herb: MP refills, sleep). | Mode 6 (index and toggle kept) is now shown as **Grass Card** and opens at Summoning 3 (Grass F). A real **Spirit Card** cast was added (mode 11). The old Spirit Card skill is migrated into mode 6. |
| unlock order Bird 15 %, Strengthening 30 %, Synthesis 45 %, Spirit 60 %, reflection 75 % | the Summoning level order above | one Summoning level = 1/7 of the mastery: Beast, Insect, Recall 1; Synthesis, Bird 2; Strengthening, Grass 3; Stone, Storage 4; Fish 5; Awakening, Spirit 6; Dragon, Deputize 7 (percent 0 / 15 / 29 / 43 / 58 / 72 / 86). **The Stone Card is no longer open at the start.** |
| Strengthening: timed boost on all summons | permanent on the card, two stats, six steps with ratios 1 : 2 : 5 : 10 : 20 : 50, cost 2 x Synthesis | **permanent for the life of the summon** (+2 / 4 / 10 / 20 / 40 / 100 % health and damage, level = Summoning level - 2, the Ultimate has 6); lost when the summon is Synthesised (the new one is plain). A card is not kept here, so "survives unsummoning" is not expressible. Cost 1 x card price (was 0.4); Synthesis stays 0.5 x, so the 2 : 1 ratio holds. |
| stat reflection from 75 % mastery, wolves give speed | Allen's status always shows the summons' buffs (Hawk: Agility+Intelligence, Wally: HP+Endurance, Appo: MP+Luck); the type decides the stats | always on; Beast and Legion summons give **attack** (Strength), Stone gives endurance (Resistance), as before slow fall for Wraith (design); a level higher with four or more as the Ultimate (design). |
| cards named "Beast Card", "Stone Card" | Allen names summons by type and rank ("Beast H", "Bird A"), and gives individuals names | Beast summons are called **Beast H** (a letter further per Synthesis: G, F, E); Stone summons are **Wally** (Stone E), Stone D, **Steely** (C), **Mira** (B). Beast individuals (Mousey, Moley, Hayate) are a mouse, a mole and a wolf-like A-rank; our Beast is a wolf, so only the letter is used. |
| Ultimate: "Summoner of Beginnings" | the wiki alias is "Summoner of Beginning" (singular) | the owner's spelling is kept; noted. |

## Modes now (indexes 0-8 unchanged, 9-16 appended; Ultimate-only: 3, 4, 16)
| Mode | What | Opens (Summoning level / mastery) | Source |
|---|---|---|---|
| 0 Beast Card, 1 Stone Card, 2 Recall, 3 Legion, 4 Wraith | as before (Stone moves to level 4; Legion is the mod's own, Wraith is Spirit-like, neither is on the wiki) | 1 / 1 / 1 / Ultimate | Summons, Deletion |
| 5 Bird Card | sight toggle (Hawk Eye), kept | 2 (15 %) | `Hawk`, `Summons` (Bird = information) |
| 6 Grass Card | healing pulse toggle (was "Spirit Card"), kept | 3 (29 %) | `Appo`, `Summons` (Grass = recovery) |
| 7 Strengthening | permanent two-stat boost | 3 (29 %) | `Summoner` |
| 8 Synthesis | two same card and rank become one of the next rank (ceiling 1 / 2 / 3 by form), same ids | 2 (15 %) | `Summoner` |
| 9 Insect Card | poison, slowness, weakness on foes around you (range 10 / 14 / 20, 8-20 s) | 1 | `Summons` (Insect = debuffs), `Denka` (Swarm) |
| 10 Fish Card | speed, haste, jump for you and allies within 16 | 5 (58 %) | `Summons` (Fish = buffs) |
| 11 Spirit Card | Maria's Psychokinesis: damage and a lift on the foe you look at (24 blocks); at level 7 or as the Ultimate Ellie's Gravity also slows foes around it | 6 (72 %) | `Maria`, `Ellie`, `Summons` |
| 12 Dragon Card | Dradra's Spitting Fire: fire damage in a cone (8 / 12 / 16) | 7 (86 %) | `Dradra`, `Summons` (Dragon = area attack) |
| 13 Awakening | each summon uses its awakened skill: Beast/Legion strength+speed, Stone resistance (and for your party: Steely's Sacrifice), Wraith speed; with the Bird mode open Farsight marks hostiles within 64 (48 at most); with Grass open Herb regenerates allies (30 / 45 / 90 s). Price 10 card prices (100 MP : 10 MP ratio), wait one day (24000 ticks), a quarter with a Soldier, an eighth with a General, a 24th with a King among your summons | 6 (72 %) | `Summoner`, `Summons`, `Hawk`, `Steely`, `Appo` |
| 14 Deputize | free; look at one of your summons: General (+50 % health and damage; three per kind; the other summons of that kind near you become Soldiers, +25 %); again with Kingnification (the Ultimate): King (+100 %, one per kind); again: plain. Ratio 1 : 2 : 4 as in the wiki | 7 (86 %) | `Summoner`, `Summons` |
| 15 Storage | opens your ender chest (private, unlimited by the wiki, here 27 slots; the contents are away from the world) | 4 (43 %) | `Summoner` |
| 16 Angel Card | Merus' Thunder of Judgement: lightning (visual only, no fire) on the eight nearest foes, damage and paralysis (long slowness); Ultimate only; price 10 card prices | Ultimate | `Merus` |

Prices (multiples of the card price, which is the existing `magiculeCost`): Insect 1, Fish 3, Spirit 3.8, Dragon 14 (a fifth of the wiki's 1 / 15 / 19 / 70 MP, never below one card; **design scale, the wiki ratios are kept in order only**). Cooldown = the existing `cooldown` x 3 / 6 / 4 / 8 (Angel x 20), design.

## KEPT / REPLACED / NEW
- NEW: modes 9-16, `SummonerCanon`, type table (`SummonerRules.Family`), the wiki names (`canonName`), permanent Strengthening and the command tiers in `Summons`, setting `summonerCanon`, lang `tools/lang/summoner_canon.json`, tests `SummonerCanonRulesTest`, `SummonerCanonSourceScanTest`, self-test `summoner.canon` (inside the smoke prefix `summoner`).
- REPLACED (only with the setting on): the unlock order, Stone Card gated, Strengthening (timed -> permanent, price), the wolf reflection (speed -> attack) and its gate, the card names, the display name of mode 6 (Spirit Card -> Grass Card), the Ultimate's and the Summoner's descriptions, the message shown when an old Hawk Eye / Spirit Card is carried over.
- KEPT: every registry id (`summoner`, `lord_of_summons`, `hawk_eye`, `spirit_card`), mode indexes 0-8 and their ids, all old toggles and VFX, the `nusmp_summon*` tags, evolution, the cap (3 / 6 / 12, hard 64), Recall, the 4.5.0 behaviour behind `summonerCanon=false`.
- Hawk Eye and Spirit Card skills: **not Allen's skills** (Hawk is a summon, Spirit is a type). They stay registered, inert, and are still carried into the Summoner by `SummonerMigration` (holders keep the Bird sight and the mode-6 healing at once); only the shown names changed (the Bird card's sight is the Hawk's Hawk Eye, mode 6 is the Grass card).

## What the wiki does not give (and what was done)
- Effects, colours, motifs and animation of any move: the pages carry only images. No wiki colour was used; the existing card-flip rune circle (gold, violet for spirits) and the new casts use the mod's own colours (insect green, fish blue, spirit violet, dragon red, Angel gold-white lightning): **design**. Merus' lightning and paralysis, Dradra's fire and Ellie's gravity are from the ability names.
- Season-by-season anime content, Quick Summoning (only listed by name in Allen's status), the Angel's recipe, Magic Stone item mapping, Cerby, Mirror and the season-two mole are not on the fetched pages; **not implemented**.
- Not expressed: Expansion (the grimoire's slots; summons here have no cards, only the cap), Sharing (the camera of a summon, client side; the Bird sight stands in), Creation's rank ladder and magic stones (no card item exists), the 30 x 30 cm opening of Storage, summons living up to 30 days (summons here last 10 / 20 / 40 minutes by form, unchanged), XP from a summon's fights, the Soldier/General/King 10 km / 100 km ranges, Appo's change into a tree, Wally's Explode, per-summon abilities beyond the Awakening list.
- Numbers of the wiki (flat +2,500 / +5,000 / +10,000, 100 MP) are scaled: only the ratios are kept.

## Worst-case cost of the canon pass
- Server: every cast is one press, no per-tick work added. Insect / Dragon / Angel / Spirit use `SkillTargets` queries (24 entities at most, the Angel 8 plus 8 visual bolts), Fish touches at most 24 allies, Awakening touches the cap's summons (hard 64), at most 24 allies and 48 marked creatures once per day (or per 1 h with a King). Strengthening and Deputize touch the cap's summons (modifiers, no entities). No new entities except visual-only lightning (8 per Angel cast, one cast per 100 s). Storage opens a menu. Reflection: the same three short effects a second.
- Network: no new packets; effects use the existing VFX throttles. Client: no new draw code.
- Unverified: compilation (CI only), in-game behaviour of every new mode, balance numbers, ManasCore's mode list with 17 entries.
