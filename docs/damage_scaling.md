# Damage scaling and the grimoire buff (3.90)

Owner requests: "buff grimoire damage because it seems like it's bad" and "buff the damage for a lot of the things in my game so that
they're actually good for late game as well, or make them scale with the player".

## 1. How addon damage is computed today (audit)

- **The choke point** is `balance/BalanceLaw.damage(caster, target, raw, mastery)`:
  `raw x epScale(caster) x (0.85 + 0.3 mastery) x gamerule/config spellDamagePercent x ManaLaw.powerMult(caster)`, plus a small
  `DamageSliver` (spell-scale raws on non-bosses, at most 2% of max health), then `cap()`: players take at most `pvpHitCapPercent`
  (40%) of their max health per hit, mobs get `x mobDamageMultiplier` (1.0).
  `epScale` is the old EP scaling: x1 at 1k EP, x3.5 at 10k, x6 at 100k, x8.5 at 1M, x11 at 10M (cap x12).
- **Grimoire spells** go through `GrimoireBook.hurt / hurtAs / hurtDirect` (about 507 call sites in `book/`, the Pages15 batches through
  `Pages15Kit`, which first clamps raw to the tier's `maxHit`). Raw numbers are small: 3 to 18 per hit (Fire: 6 / 10 / 12 / 14 / 18),
  times the cover `damage` (0.97 to 1.15), Devil Union x1.1, build-up up to x1.2, fire casting, worn armour and Magic Gear, Dark vs devils
  x1.25, then `BalanceLaw.damage`.
- **Other systems through `BalanceLaw.damage(caster, ...)`**: weapon arts in `item/` (Rot Doom raw 24 to 48, Bolt, Pin / Hand of Providence,
  Resurgence, Magebane), hero weapons (`heroweapon/weapon/chain|whip|harpoon|mirror|shield`, curse, skill), props (`prop/`), skills through
  `skill/SkillDamage.hit` (which multiplies `DM_DAMAGE_MULT` first: Nen, Adolla, Arc's Ray, ...) and `DbMoves`, Saiyan (`SaiyanAbilities`),
  Djinn weapon acts, Beast Will, Mana Core actives, Mana Method runes, spirit control, Shadow Unite, Storm staff, Veldanava ultimates,
  dungeon nexus, avatar goals.
- **Own numbers, only `cap()` or `epScale()`**: about 29 `epScale(p) x raw` sites (13 in `book/`, `prop/`, Spirit Space, Shadow), and
  `cap()`-only sites (Veldanava cap rules, corpse, labyrinth mega, relic scaling, Zagred, sword logic). `NUConfig.damageMultiplier`
  (`DM_DAMAGE_MULT`) is read by `SkillDamage`, `DMUtil` (god skills) and the legacy `GrimoireMagicSkill` / `GrimoirePageSkill`.

## 2. Late-game scaling (new)

`balance/LateGameScaling.factor(enabled, maxMagicule, baseline, cap)` (pure):
`1 + 1.25 x log10(maxMagicule / baseline)`, x1 at or below the baseline, capped. Defaults: baseline 10,000, cap x6.

| max magicule | 10k | 100k | 1M | 10M | 100M+ |
|---|---|---|---|---|---|
| factor | x1 | x2.25 | x3.5 | x4.75 | x6 |

- Applied **once**, in `BalanceLaw.damage(caster, ...)` only (`raw * epScale * lateGameScale * ...`), before `cap()`: PvP still ends at
  the pvp hit cap. **Players only**: a mob or boss caster is x1 (the Black Clover boss stats are untouched).
- Read from `EnergyHelper.getMaxMagicule` (the same call `BalanceLaw.cost` uses), guarded; NaN / negative / zero give x1.
- SERVER config `nusmp-server.toml` `[black_clover]`: `lateGameScaling = true` (false = the old damage), `lateGameScalingBaseline`,
  `lateGameScalingCap` (1 = off).

**Covered** (every `BalanceLaw.damage(caster, ...)` hit by a player): all grimoire pages through `GrimoireBook.hurt*`, the 22 `book/` files
with direct caster calls, item weapon arts, hero weapon skills, props, `SkillDamage` / `DbMoves` skills, Saiyan, Mana Core, Mana Method, spirit
control, Shadow, Storm, Veldanava, nexus, avatar goals. Beast Will and Djinn weapon acts already route through it, so they get the scaling
without any edit at their call sites (another worker owns their multipliers).
**Not covered**: the caster-less `BalanceLaw.damage(target, raw, m)` (book passive "empower" bonuses), the ~29 `epScale x raw` + `cap()`
sites, `cap()`-only sites, the legacy `GrimoireMagicSkill`, `DMUtil` god skills, Devil and Spirit-own damage that does not call
`BalanceLaw.damage`, and Time Magic (rule 3, untouched).

## 3. Grimoire buff (new)

SERVER config `grimoireDamageMultiplier = 1.75` (1.0 = the old damage), `r *= BalanceLaw.grimoireMult()` once in `GrimoireBook.hurtAs` and
once in `hurtDirect`, before `BalanceLaw.damage` (so the late-game scaling and caps follow).
Why 1.75: a mid grimoire page hits raw 10 to 14 (x cover ~1.05), while weapon arts at the same tier hit raw 24 to 36 (Rot Doom) and hero
weapon / relic techniques more. x1.75 brings a page to raw 18 to 25: level with a weapon art, still under weapon finishers, which is fair for
ranged, area spells with shorter cooldowns. Pages15 clamps raw to the tier `maxHit` before the buff, so those pages can exceed their old
`maxHit` by the multiplier. Raw x1.75 also lifts the small `DamageSliver` a little (still at most 2% of max health, never on bosses).

## 4. Grimoire VFX (not done here, another stream)

Weakest-looking attributes found while reading (pages drawn only through the shared `WikiSpells` / `ElementBook` helpers, no own VFX layer;
judged from code, not seen in a game): **Cotton, Mirror, Plant, Sealing, Thread**.

## 5. Cost

A few multiplications and one Tensura attachment read per hit (`getMaxMagicule`), no packets, no entities, no per-tick work, no allocation.
Worst case = the existing hit rate of the balance law; nothing new per tick.

## 6. Tests

- `LateGameScalingTest` (pure): off = 1, baseline = 1, monotonic, capped, NaN / negative / infinity safe.
- `LateGameScalingSourceScanTest`: the factor is multiplied once in `BalanceLaw.damage` and nowhere else, the grimoire buff once per hit
  path, the settings are in the SERVER config, the curve is pure, the self-test prefix is in the smoke list.
- Self-tests `scaling.rules`, `scaling.live` (`balance/ScalingSelfTests`), prefix `scaling` in `SMOKE_EXPECT` before `boss.stats`.

## NEW / REPLACED / KEPT

| | |
|---|---|
| NEW | `LateGameScaling` curve, `BalanceLaw.lateGameScale` / `grimoireMult`, settings `lateGameScaling`, `lateGameScalingBaseline`, `lateGameScalingCap`, `grimoireDamageMultiplier`, self-tests `scaling.*`, two tests, this doc |
| REPLACED | nothing |
| KEPT | `epScale`, the pvp hit cap, `mobDamageMultiplier`, `spellDamagePercent`, `damageMultiplier`, every raw number; both new behaviours off = the old damage |
