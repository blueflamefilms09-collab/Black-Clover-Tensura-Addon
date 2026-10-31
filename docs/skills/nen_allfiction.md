# Nen (new) and All Fiction (rebuilt) — 3.46.0

Owner: "Nen (Hunter x Hunter) as a new Ultimate", "All Fiction is a yes but we are fully rebuilding it", "i want custom skill based animes", Ultimates "2 at a time", "let me know as you make them". This is pair 1.

## What is built, how it is read

Both are Ultimates in the skill wheel (the mode key cycles, the skill key presses the current mode). Windows are stored as the game time they end at in the skill tag of the one player who owns the skill, so nothing ticks for them and nothing has to be cleaned up on logout, death or a dimension change.

### Nen (`nusmp:nen`, NEW) — 10 modes
| # | Mode | What it does |
|---|------|--------------|
| 0 | Ten | 45 s: aura round the body, hits do 0.8x. A second press ends it. |
| 1 | Zetsu | 20 s: invisible, foes within 16 blocks that were hunting you forget you; striking ends it. A second press ends it. |
| 2 | Ren | 20 s: Strength II, Speed I. A second press ends it. |
| 3 | Gyo | Night Vision for a minute; invisible creatures within 24 blocks (at most 16) glow for 8 s. |
| 4 | En | Everything hostile within 16 blocks (at most 24) glows for 10 s; you are told how many. |
| 5 | Ken | 8 s stance: hits do 0.4x, Slowness II. A second press ends it. |
| 6 | Ko | 10 s: your next hit is followed by a heavy strike (30), and you take 1.2x while it waits. |
| 7 | Hatsu: Enhancement | One empowered blow (14, knockback) at 4.5 blocks. |
| 8 | Hatsu: Emission | An aura bolt, 24 blocks, pierces up to 3 (12 each). |
| 9 | Vow and Limitation | 30 s: no Ten/Zetsu/Ken, 1.25x damage taken, hatsu (Enhancement, Emission, Ko) hit 1.75x. A second press ends it. |

Costs (Magicule) 600 / 500 / 1500 / 400 / 1000 / 1200 / 2000 / 1500 / 1800 / 0. Numbers: `skill/rules/NenRules.java`. All damage goes through `SkillDamage.hit` (the balance law: a player is never one-shot). Obtain: `/tensura edit <player> ability grant nusmp:nen`. Switch: server setting `ultimates_3_46.nenEnabled` (off: the modes refuse and the hooks do nothing).

### All Fiction (`nusmp:all_fiction`, REPLACED by a rebuild) — 10 modes
| # | Mode | What it does |
|---|------|--------------|
| 0 | Undo (kept) | Health back to what it was 5 s ago, harmful effects gone. |
| 1 | Fiction (kept) | Erase the creature you look at (a boss, or a protected player, loses its effects and a quarter of its health). |
| 2 | Annul Damage | 6 s: hits are made not to have happened until half your maximum health has been absorbed. |
| 3 | Rewind Step | You stand where you stood up to 5 s ago, if it is in this world, loaded, free, and 1.5 to 48 blocks away. |
| 4 | Erase Effects | A foe you look at loses every effect; an ally (or you) every harmful one. |
| 5 | Nullify Strike | 10 s: the next hit never happens. |
| 6 | Unmake | Hostile projectiles within 10 blocks (at most 32) never existed. Yours, your allies' and your summons' are left alone. |
| 7 | Share the Fiction | Allies within 8 blocks (you included, at most 8) are cleansed and healed 25 % (through `BalanceLaw.heal`). |
| 8 | Stand Up | Tells you when your death can be made fiction again. Free. |
| 9 | World Fiction | 6 s zone round where you stood: once a second allies in it are cleansed (at most 8) and hostile projectiles in it erased (at most 16). Closes if you leave the dimension. |

Passive (kept, unchanged): once every 15 minutes your own death becomes fiction. Numbers: `skill/rules/FictionRules.java` (the old constants are all still there; Book Maker still uses `modes() = 2`). Switch: server setting `ultimates_3_46.allFictionRebuilt` (off: the wheel is the old two modes and the new hooks do nothing).

## NEW / REPLACED / KEPT
- NEW: Nen (skill, rules, icon, lang, config, tests, self-tests); All Fiction modes 2-9 (Annul Damage, Rewind Step, Erase Effects, Nullify Strike, Unmake, Share the Fiction, Stand Up, World Fiction); the five-second position memory; settings `nenEnabled`, `allFictionRebuilt`; self-tests `skill.nen.registry`, `skill.fiction.rebuild`.
- REPLACED: the All Fiction skill class (two modes -> ten), by the owner's word ("fully rebuilding it").
- KEPT: the id `nusmp:all_fiction`, its icon, Undo and Fiction with their ids, numbers and lang keys, the health memory (`H0..H4`), the death passive and `DeathReadyAt`, the evolution Book Maker -> All Fiction, every other skill. Shared files were only appended to.

## Worst-case cost (rule 11)
- Server, per press: Nen looks at no more than 24 creatures (`SkillTargets.MAX_FOES`) and hits at most 3; Gyo makes one box query with a fixed 24-block radius and reveals at most 16. All Fiction: Unmake erases at most 32 projectiles in one box query, Share touches at most 8 allies, Rewind is one teleport and one collision check, Erase Effects one look.
- Server, per tick: Nen has no tick. All Fiction's tick runs once a second per owner (it already did, for the health memory): it now also shifts 15 numbers in the tag and compares one dimension string. World Fiction adds one pulse a second for 6 s: one ally query (<= 24 entities, <= 8 cleansed) and one projectile query (<= 16 erased). The damage hooks are a few tag reads per hit received (no allocation but the boxed number).
- Network: no new packet type. A press marks the skill dirty once (one skill sync); effects go through `SkillFx` -> `Fx`, so they are sent to nearby players only and inside the per-tick particle budgets (a pulse is one ring plus at most 16 three-particle sprays).
- Client: no new client code; the Nen icon is one 32x32 texture.
- State: only in the skill tag of the owning player (no static map, no player held); expires by game time; the position memory resets on a dimension change.

## Sources (rule 10)
Not fetched: the English wiki (`hunterxhunter.fandom.com`, `medaka-box.fandom.com`) and the Japanese wiki (`ja.wikipedia.org`) did not resolve in this build session (not in the session's allowed domains). Every entry above therefore comes from the author's knowledge of the series (Ten/Zetsu/Ren/Hatsu, Gyo/En/Ken/Ko, Vow and Limitation; All Fiction's "make it never have happened"), not from a checked page. The exact in-series effects should be checked against the wiki before the numbers are treated as canon; the game numbers themselves are balance choices, not canon values.

## Not included
The four other Nen types' signature abilities (Transmutation, Conjuration, Manipulation, Specialization) and Shu/In/Ryu as separate modes; restoring blocks or broken terrain with All Fiction; per-mode animated icons; a model. Nothing here was seen in a game.
