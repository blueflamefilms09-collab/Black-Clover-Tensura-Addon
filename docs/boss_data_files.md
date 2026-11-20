# Data-pack boss numbers: `data/anime_multiverse/arc_bosses/<boss id>.json` (Leon study fix 9)

Source: `docs/boss_reference_leon.md` G14 and fix 9, approved by the owner. An operator or a data pack can retune a boss without a rebuild. The Java numbers stay the DEFAULT and the fallback. **The mod ships no override file**, so installing this changes no number.

## File

`<boss id>` is the boss's entity path (`meliodas`, `heath_grice`, `lucius`, ...), snake_case, no sub-folder. All keys are optional, at least one is required:

```json
{ "health": 3600, "attack": 26, "speed": 0.30, "armor": 12, "move_cooldown_scale": 0.9 }
```

An inert example is `docs/boss_data_example.json` (it is not under `data/`, so it is never loaded). Keys that start with `_` are comments.

| key | meaning | clamped to |
|---|---|---|
| `health` | max health (base) | 1 .. 1,000,000 |
| `attack` | melee attack damage (base) | 0 .. 1,000 |
| `speed` | movement speed (base) | 0.01 .. 1.0 |
| `armor` | armor (base) | 0 .. 30 |
| `move_cooldown_scale` | factor on every move's cooldown (below 1 = faster moves) | 0.25 .. 4.0 |

Rules: a number outside its range is clamped. A non-number, a non-finite number, an unknown key, an empty file, a file that is not an object or a file name that is not a boss id makes the **whole file invalid: no change** (one warning per file in the log, at most 8 per reload, then one summary line).

## How it is applied

* Loaded like `BossDropData`: a `SimpleJsonResourceReloadListener` (directory `arc_bosses`) swaps one immutable map in on every data reload.
* When a registered boss joins the world (`EntityJoinLevelEvent`, server side) each named attribute gets one permanent multiplied-total modifier `anime_multiverse:boss_data/<key>` of (file value / Java default) - 1. So the file's number is the boss's base number and the Black Clover x3, the floor scale and the party scale (fix 5) still multiply with it. The bar keeps its share. The modifier is saved with the boss; if the file or the setting later goes away, the modifier is taken off when the boss next joins. `move_cooldown_scale` is a transient field read in the move scheduler (and Lucius's combo planner).
* `/reload` changes the loaded files, not bosses already standing in the world (they pick it up when they next join: a restart or a new summon).
* Server key `bossRules.bossData` (`nusmp-boss-rules-server.toml`, default **true**): false ignores every file and takes existing overrides off.

## NEW / REPLACED / KEPT

NEW: `boss/data/BossDataTable` (pure parse/validate/clamp), `boss/data/BossData` (loader and applier), `bossRules.bossData`, `ZogratisBoss.moveCooldownScale`, self-test `boss.leon3`, tests `BossDataTableTest` and the scan `LeonFixesSourceScanTest`, `docs/boss_data_example.json`.
REPLACED: nothing.
KEPT: every Java number and `ArcCharacter` row, the Decennial and Tier List data rows, `BossDropData`.

## Worst-case cost (rule 11)

No per-tick work, no packets, no scan, no spawn. A reload parses at most the files in the pack (each a handful of numbers). Each entity join does one registry lookup; a boss with a file does at most four attribute-modifier writes and one health write, once. Log output is bounded (8 lines per reload).

## Limits and decisions

* A Java default of 0 (for example a boss with 0 armor) cannot be raised, because the override is a factor of the default.
* Overrides apply to bosses registered in the boss registry; Tier List rows can be overridden too if a file is named after them (the party scale is separate and stays off for them).
* Lucius's own numbers (arena scaling, stage tables) read his base health attribute, so `health` moves his bar like any other.

## Not checked

* Nothing was played and no pack was installed in a game; `boss.leon3` (CI) installs a test override, applies it, replaces it, and takes it off.
* Whether a boss that sets its own base attribute at runtime (a few disciples and clones do) keeps the ratio was not audited; the registered bosses use the framework defaults.
* The behaviour of `/reload` with a very large pack (hundreds of files) was not measured; the work is linear in files with no I/O beyond the vanilla reader.
