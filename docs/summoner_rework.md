# Summoner rework (owner request: "Any skills based off of the Summoner from Allen from the Hell Mode anime ... should all just be into that one skill as passives, actives, or mastery based ... change Summoner Lord of Summons to Summoner of Beginnings.")

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
