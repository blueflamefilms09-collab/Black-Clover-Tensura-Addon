# Gate of Babylon (new) and Ki (new) — 3.47.0

Owner: "i want custom skill based animes", Ultimates "2 at a time", "let me know as you make them", then "go". This is pair 2 (pair 1 was Nen and All Fiction, 3.46.0).

## What is built, how it is read

Both are Ultimates in the skill wheel (the mode key cycles, the skill key presses the current mode). Windows are stored as the game time they end at in the skill tag of the one player who owns the skill, so nothing ticks for them and nothing has to be cleaned up on logout, death or a dimension change. A second press ends the windows marked "ends".

### Gate of Babylon (`nusmp:gate_of_babylon`, NEW) — 10 modes
| # | Mode | What it does | Cost / cooldown (s) |
|---|------|--------------|---------------------|
| 0 | Gate | A gold gate opens above the creature you look at (32 blocks) and a weapon falls: 16. | 1200 / 6 |
| 1 | Volley | Up to six creatures in a cone in front of you (24 blocks) are each struck: 11. | 2000 / 15 |
| 2 | Rain | A place you look at (32 blocks), radius 6; up to twelve creatures there are struck: 9. | 3000 / 30 |
| 3 | Chains of Heaven | The creature you look at (24 blocks) cannot move (Slowness VII) for 5 s and is weakened for 10 s. A boss is held 0.4x as long, a player 0.5x. | 1800 / 25 |
| 4 | Golden Armour | 20 s: Absorption II and every hit does 0.7x. A second press ends it. | 2000 / 40 |
| 5 | Vimana | A forward leap (1.8) with lift, and Slow Falling for 15 s. A second press ends the Slow Falling. | 1500 / 30 |
| 6 | King's Aura | Allies within 12 blocks (you included, at most 24, never a player you could hurt) gain Strength I and Resistance I for 20 s; foes within 12 blocks (at most 24) Weakness I for 10 s. | 2500 / 45 |
| 7 | Gae Bolg | A spear down a line (24 blocks, width 1): up to three creatures, 24 each. | 2200 / 13 |
| 8 | Ea | One great line (40 blocks, width 2.2): everything in it you may hurt (at most 24) is struck for 48. | 8000 / 120 |
| 9 | Elixir | Heals 30 % of your maximum health (through `BalanceLaw.heal`), cleanses, Regeneration for 10 s. | 2500 / 60 |

Numbers: `skill/rules/GateRules.java`. All damage goes through `SkillDamage.hit` (the balance law: a player is never one-shot; the PvP hit cap applies to Ea too). Obtain: `/tensura edit <player> ability grant nusmp:gate_of_babylon`. Switch: server setting `ultimates_3_47.gateEnabled`.

### Ki (`nusmp:ki`, NEW) — 10 modes
| # | Mode | What it does | Cost / cooldown (s) |
|---|------|--------------|---------------------|
| 0 | Ki Blast | A quick ball down your look (32 blocks): the first creature in its path takes 10. | 400 / 2 |
| 1 | Kamehameha | A beam, 28 blocks, width 1.6: up to six creatures, 22 each. | 2500 / 20 |
| 2 | Instant Transmission | A blink along your look up to 32 blocks to the nearest safe spot (room, ground, no lava; the shared blink helper). A spot closer than 1.5 blocks is no blink and costs nothing. | 1200 / 10 |
| 3 | Solar Flare | Foes in a cone in front of you (12 blocks, at most 24) are blinded 4 s and slowed 2 s (boss 0.4x as long, player 0.5x). | 800 / 20 |
| 4 | Afterimage | 4 s: the next blow does 0.2x, and the window closes. | 900 / 20 |
| 5 | Super Saiyan | 30 s: Strength II, Speed I, every hit does 0.85x. A second press ends it. | 3000 / 60 |
| 6 | Spirit Bomb | Where you look (32 blocks), radius 8: 24 + 4 per living ally within 24 blocks lending energy (at most eight) to every foe there (at most 24). | 6000 / 90 |
| 7 | Kaioken | 10 s: Strength III, Speed II, every hit does 1.25x. Costs 15 % of your maximum health (never below two points; refused at four health points or fewer). A second press ends it. | 1500 / 45 |
| 8 | Ultra Instinct | 8 s: one blow a second is dodged outright, the others do 0.5x. A second press ends it. | 4000 / 90 |
| 9 | Ki Sense | Every hostile creature within 32 blocks (at most 24) glows for 10 s; you are told how many. | 600 / 15 |

Numbers: `skill/rules/KiRules.java`. The holds of Solar Flare and Chains share `skill/rules/ControlRules.controlTicks`. Obtain: `/tensura edit <player> ability grant nusmp:ki`. Switch: server setting `ultimates_3_47.kiEnabled`.

## NEW / REPLACED / KEPT
- NEW: Gate of Babylon and Ki (skills, rules, icons, lang, config, tests, self-tests `skill.gate.registry` and `skill.ki.registry`), `ControlRules`.
- REPLACED: nothing. (The relic item "Gate of Babylon Crest" is a different thing and is unchanged.)
- KEPT: every other skill; Nen and All Fiction as in 3.46.0. Shared files were only appended to.

## Worst-case cost (rule 11)
- Server, per press: no more than 24 creatures looked at (`SkillTargets.MAX_FOES`), each press makes at most one box query through the `SkillTargets` helpers. Hits per press: Gate 1, Volley 6, Rain 12, Gae Bolg 3, Ea 24 (120-second cooldown), Ki Blast 1, Kamehameha 6, Spirit Bomb 24 (90-second cooldown). Effects applied: King's Aura at most 24 allies and 24 foes. Instant Transmission is one blink (the shared helper tries at most 64 candidate spots).
- Server, per tick: neither skill has a tick. The damage hooks are a handful of tag reads (a long per window) per hit received and allocate nothing but the boxed number.
- Network: no new packet type. A press marks the skill dirty once (one skill sync). Effects go through `SkillFx` -> `Fx`/`VfxSpawn`, so they are sent to nearby players only and inside the per-tick budgets: at most three gate shapes and six beams (Gate of Babylon), one beam and one shape (Kamehameha, Ea), one ring and one shape (Spirit Bomb, Rain).
- Client: no new client code; each icon is one 32x32 texture.
- State: only in the skill tag of the owning player (no static map, no player held); expires by game time; nothing needs cleaning up. Kaioken's health cost cannot kill.

## Sources (rule 10)
Not fetched: the English wiki (`typemoon.fandom.com`, `dragonball.fandom.com`) and the Japanese wiki (`ja.wikipedia.org`, ja fandom) did not resolve in this build session (not in the session's allowed domains). Every entry above therefore comes from the author's knowledge of the series (Gilgamesh's Gate of Babylon, Enkidu, Vimana, Gae Bolg, Ea; Kamehameha, Instant Transmission, Solar Flare, Afterimage, Super Saiyan, Spirit Bomb, Kaioken, Ultra Instinct), not from a checked page. The in-series details should be checked against the wiki before the modes are treated as canon; the game numbers are balance choices, not canon values.

## Not included
Gilgamesh's other Noble Phantasms as items; Dragon Ball fusion, Super Saiyan God/Blue and the other transformations; per-mode animated icons; models. Nothing here was seen in a game.
