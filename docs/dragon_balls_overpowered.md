# Dragon Balls, overpowered wishes

Owner request: "make it so the wishes are actually overpowered, so like players can change their race, pick any of the existing races in the game, pick skills ... the minute a wish is made, all of the Dragon Balls basically teleport around the world, just like the Dragon Ball anime." Plus: Super Shenron grants Unique and Ultimate skills, and sees every skill of every Tensura mod that is installed.

Everything here sits behind ONE server setting, `dragonBalls.overpowered`, default **true**. Set it to **false** and the old wishes, caps and scatter timing come back exactly as they were (see "Old mode" below). Read `docs/dragon_balls.md` first for the ritual itself (summoning, phrases, radar, lock).

## What each wish does (overpowered on)

You type the wish in chat, or click it, or use `/dragonwish <wish> <name>` (the command goes through exactly the same code as typing it, so an admin and a player get the same answer). Names are matched loosely: capitals, spaces and underscores do not matter, a namespace is optional (`tensura:high_orc`, `high orc`, `High_Orc`, `highorc`), and a partial name is enough when only one thing fits ("dem lord"). If two things fit equally, the dragon lists up to 8 and asks which one; nothing is used up. A name it does not know is refused politely and the wish is NOT consumed.

| Wish | What it does | Earth (Shenron) | Super (Super Shenron) |
|---|---|---|---|
| Race | Become ANY race registered in the game right now (every Tensura race and evolution, every race from other mods, the Saiyan, this mod's Devil). Read live from the registry. Same call Tensura's own `/tensura race set` uses (no spawn teleport, evolution progress reset). | yes | yes |
| Rate (rank) | Raise your Magic Knight rank. See "What 'rate' means" below. | up to Senior Magic Knight 1st class | up to Grand Magic Knight |
| Skill | Learn a skill by its name or registry id. Uses the existing skill grant helper and the existing "already known" check. | 1 skill, up to the Earth tier (default Extra) | up to 3 skills (`overpoweredSkillsPerWish`), up to Unique and Ultimate |
| Item | Any item in the game, one stack, at most 64 ("diamond", "64 diamonds", "netherite ingot"). | yes | yes |
| Weapon | Any relic up to God grade ("ruyi power pole", or any other relic by name). Genesis and Akashic only if `overpoweredGenesisRelics` is on. Once per player per real week. With no name: the Ruyi Power Pole. | yes (new) | yes |
| Power | Permanent power levels (+1 heart, +0.5 attack each). | +2 per wish | +4 per wish |
| Coins | Tensura coins. | 100 gold coins | 1000 gold coins |
| Revive | Bring back dead players. The time window is gone: only the 256 recorded deaths limit it. | yes | yes |
| Saiyan | As before (still behind `saiyanWish`). | yes | yes |

Wishes per summoning: Earth **3**, Super **5** (range 1 to 10).

Power can reach level 20 in total (the hard maximum) instead of the old `powerCapLevels`.

### Skills

* The skill list is **read at run time from the live skill registry**, so it holds every skill of Tensura and of every addon or other mod that adds Tensura skills. Nothing is copied into this mod, so a new mod's skills appear without any change here.
* Matched by display name (English, as translated by the language file) AND by registry id. Tab completion offers the namespaced id (`mod:skill`); the clickable list shows the display name.
* The list is cached the first time it is needed, and thrown away when the server starts, stops, or `/reload` runs. Nothing scans the registry per tick.
* **The tier of every skill** comes from Tensura's own skill type (resistance, common, extra, unique, ultimate) through one pure function (`SkillRules.tierOf`, unit tested with stand-ins). A skill with no tier is never offered.
* Ceilings: Super Shenron reaches Unique and Ultimate while `overpoweredUltimateSkills` is true (default); with it off Super stops at Unique. Shenron stops at `overpoweredEarthSkillTier` (default EXTRA, not Unique or Ultimate; it may be raised to ULTIMATE). A skill above the ceiling is answered with "beyond this dragon" and is not consumed.
* **How grantability was decided.** A skill is offered only when ALL of these hold: it is a typed Tensura `Skill` (so it has a tier) and is not INTRINSIC (those belong to a race); it is not an inert placeholder class of a removed or retired skill; its id is not one this mod removed or retired (`LegacyIds`), soul-tied (`ResetRules.soulTied`: grimoires, pages, spirit lord and so on), a weapon skill (`ws_`) or one that a service of this mod grants and takes back (Shishou, the sins' lords, Wizard King, Seifort). Then, at grant time, the final veto is the grant helper itself (`Skills.learnSkill`) which returns false when Tensura or any guard refuses; the player is told and the wish is not consumed. Tensura's own `canLearnSkill` only rejects spectators, so there is no further API answer to ask.

### What 'rate' means (ambiguous)

"Change their rate" was ambiguous. It was built as **rank**: the Magic Knight rank (Junior to Senior to Grand, steps 0 to 16) that this mod already has and from which the Mage Stage is derived. With no rank named an Earth wish climbs one class group (5 steps), a Super wish goes straight to its ceiling; you can name a rank (`/dragonwish rank senior magic knight 1st class`). It never lowers a rank and never grants the Wizard King. The words "rank", "rate", "promote", "knight" and "stage" all select this wish. Evolved Tensura forms (a higher tier of a race) are reachable through the Race wish, because they are races in the registry. NOT done: changing the grimoire cover tier.

## The anime scatter

The moment the **first wish is granted** (a refused wish does not count), the whole set of balls flashes where the summoner stands and flies apart to new places in the world, like the anime:

* It uses the existing scatter rules (the generation number goes up, every ball becomes a pending row, balls of an older generation stop counting) and the existing stone lock afterwards: the balls are stone for `overpoweredLockMinutes` (default 60 real minutes), then wake up and are findable with the radar again.
* It happens **once** per ritual: a guard (`scattered` on the session, `WishRules.scatterNow`) stops a second scatter at the second, third wish or at the end of the ritual, and the ritual is marked not active at that moment, so a server restart in the middle of the remaining wishes cannot scatter twice.
* The remaining wishes of that summoning still work; the summoner keeps talking to the dragon.
* Why not "search the world and chests for the balls": the balls are used up at the summoning, so there is nothing to search for; stray copies of old generations are dead by the generation rule.
* Effect: up to 16 particles per ball (12 sparks, 1 flash, 3 glows), sent through the perf particle budget to players near the summoner only, plus one sound and one chat line to nearby players. No chunk is loaded and no world is scanned.

## What is refused (unchanged)

Every forbidden-word refusal stays exactly: immortality, mass death, world edits, mind control, deleting or stealing others' things ("immortal", "invincible", "kill everyone", "destroy", "erase", "delete", "wipe", "control", "enslave", "world", "universe", "planet", "server", "ban", "steal" and so on, see `WishRules.FORBIDDEN`). Consequence worth knowing: a skill or race whose name contains one of those words (for example one with "World" or "Control" in its name) is refused by that check, and cannot be wished for by name.

The item wish never gives: operator and creative-only items (command blocks, barrier, light, structure blocks, jigsaw, debug stick, spawners, bedrock, spawn eggs, written books, player heads and so on), anything tagged hidden from recipe viewers, anything gated by the game master block, the Dragon Balls themselves, or a relic of this mod (those come from the weapon wish). One stack at most, never more than 64.

## Settings (server config, `anime_multiverse-server.toml`, section `dragonBalls`)

TOML cannot hold a key and a table with the same name, so the sub-settings of `overpowered` are flat keys named `overpoweredXxx` (this differs from a `overpowered.xxx` layout).

| Key | Default | Meaning |
|---|---|---|
| `overpowered` | true | Master switch. false = old wishes, caps and scatter timing. |
| `overpoweredEarthWishes` | 3 | Wishes per Shenron summoning (1 to 10). |
| `overpoweredSuperWishes` | 5 | Wishes per Super Shenron summoning (1 to 10). |
| `overpoweredSkillsPerWish` | 3 | Skills one Super skill wish teaches (1 to 5). Earth teaches 1. |
| `overpoweredUltimateSkills` | true | Super may teach Ultimate skills (it always reaches Unique). |
| `overpoweredEarthSkillTier` | EXTRA | Highest skill tier Shenron teaches (RESISTANCE, COMMON, EXTRA, UNIQUE, ULTIMATE). |
| `overpoweredGenesisRelics` | false | The weapon wish may give Genesis and Akashic relics. |
| `overpoweredLockMinutes` | 60 | Real minutes the balls stay stone after the first wish (0 = no lock). |

The old `earthWishes`, `superWishes`, `lockMinutes`, `powerCapLevels` stay and are used when `overpowered` is false.

## Old mode (`overpowered` false)

Exactly the old behaviour: the old wish table (no Race, no Rank, weapon only from Super), 1 / 2 wishes (as set), the revive window in minutes, `powerCapLevels`, 20 / 100 gold coins, the curated item and skill lists, and the scatter at the end of the ritual. The old code paths are kept as they were (`DbWishes.itemClassic`, `skillClassic`); the new ones are added next to them.

## For the voice worker

`WishRules.spokenVocabulary()` returns the single words of every wish keyword plus the words of the race, skill, item and relic names in the game right now (read from the live lists, lower case, de-duplicated, at most 2000; the wish words come first so a cap never cuts them).

## Worst-case cost (rule 11)

* Server per tick: nothing. All work happens when a player says or clicks a wish, once per wish.
* Per wish: one pass over one cached list (a few thousand entries at most, a sentence cut at 512 characters); one `learnSkill` per skill (at most 5); one item stack; one race change. Lists are built lazily once and rebuilt only after a server start, stop or `/reload`.
* Anime scatter: once per ritual, `n` balls (at most 8) rescattered by the existing scatter (a bounded number of pending rows, no chunk load, balls spawn later through the existing capped sweep). Particles: at most 8 x 16 = 128 per scatter, sent to players within the ball network range only, through the perf particle budget. Packets: no new packet type, one chat line per nearby player.
* Tab completion: at most 100 suggestions, only for a player with a running ritual.
* Clickable lists: one chat line with at most 90 (race) or 24 (skill) buttons.
* Client: no new client code.
* Per-player state is keyed by player UUID (the weekly weapon stamp lives in the player's own persistent data).

## Tests

* Pure JUnit: `WishNamesTest` (fuzzy matching), `SkillRulesTest` (tiers with stand-ins, ceilings, blocks), `WishRulesOverpoweredTest` (table, caps, counts, blocklist, scatter guard, weapon week, rank target, spoken vocabulary), `WishRulesTest` and `DbFilesTest` updated.
* Server self-tests (prefix `dbwish.`, registered next to the other Dragon Ball self-tests, no extra mod needed): `dbwish.rules`, `dbwish.registry` (non-empty race and skill lists from the live registry, every race and skill name resolves back to itself, the CI log line carries the live skill count per tier), `dbwish.apply` (a stand-in player gets a skill, a race and a rank by wish, refusals are not consumed), `dbwish.scatter` (the first granted wish scatters once, later wishes and the end of the ritual do not).

## NOT done, and not seen in a game

* Nothing here was run in a real game or on a dedicated server. The pure tests and the source-scan tests were run; the `dbwish.*` self-tests compile but only run in the CI smoke test.
* Tensura Magic and Battle Will entries that are not `Skill` classes are not offered by the skill wish.
* The grimoire cover tier is not changed by the rank wish.
* The settings are flat `overpoweredXxx` keys (see above).
* A race or skill with a forbidden word in its name cannot be wished for by name.
* The Devil race is a flag on the Magic Knight profile (not a Tensura race); choosing another race clears the flag.
