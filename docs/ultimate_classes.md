# Ultimate classes (Lord / King / God / Dragon) and the NUSkill cost fix

Source: the owner's master build guide (Part 5 §5.2, Part 6 A.1-A.8 and C.3, Part 4 §5 P1 #1 and #4). Guide steps §5.6 1-3 for these items.
Not in this change (another worker): `SkillDamage.source` (Tensura damage sources), grimoire lang, `SpellRuntime`.

## 1. The class table (`skill/rules/UltimateClass`, pure)

| Class | epCap (damage ceiling of the EP scaling) | costPercent | costReference | maxMastery | killBelow | shredBelow |
|---|---|---|---|---|---|---|
| LORD | x8 | 2 % | 2,000 | 10,000 | 0 | 0 |
| KING | x10 | 3 % | 3,000 | 25,000 | 0 | 0.70 |
| GOD | x12 | 5 % | 4,000 | 50,000 | 0.60 | 0.80 |
| DRAGON (Black Clover: Devil track) | x12 | 4 % | 3,000 | 15,000 | 0 | 0 |

Starting points, not tested balance. Every column is a SERVER setting (below) and passes a clamp (epCap 1-12, costPercent 0-50, costReference >= 1,
maxMastery 100-1,000,000, ratios 0-0.95). Enum entries are append only. `killBelow` / `shredBelow` are only data for now: no skill reads them yet (the
guide's EP-ratio mechanics are per-skill work, §5.6 steps 4-5), so no instakill was added anywhere.

**Deviation from the guide, on purpose:** the guide's `costFloor` (50k / 150k / 400k / 300k) used as a `BalanceLaw.cost` floor would make every
Ultimate cost at least 50,000 MP. Ultimates evolve from a mastered Unique with `ultimate_evolution.minimumEP = 0` by default, so a fresh Ultimate holder
can have a few thousand MP and could never cast. The column is a **costReference** instead: "a cast whose old flat price is costReference costs
costPercent of max magicule", and every cast keeps its own flat price as the floor.

## 2. Classification (guide Part 6 C.3 table; the owner can move any skill)

| Class | Skills (class file) |
|---|---|
| GOD | The Almighty `TheAlmightySkill`, The Visionary `TheVisionarySkill`, All Fiction `AllFictionSkill` |
| KING | Gate of Babylon `GateOfBabylonSkill`, Hell Conqueror `HellConquerorSkill`, Lord of Summons `LordOfSummonsSkill` |
| LORD | Adolla Burst `AdollaBurstSkill`, Hero's Argonaut `HerosArgonautSkill`, Immortal Will `ImmortalWillSkill`, Ki `KiSkill`, Nen `NenSkill`, Shishou `ShishouUltimateSkill`, Lord of Gluttonous Greed `LordOfGluttonousGreedSkill` (not in the guide's table: Lord by its name and kit), the five Berserk weapon skills `SinWeaponSkill` (Lord of Slothful Weight, Lord of Green-Eyed Envy, Lord of Empty Vanity, Lord of the Twin Rings, Seifort's Holy Judgement: one class, set up like Greed's), the 15 Dragon Ball Ultimates `DbSkill` and the 27 Beast Will Ultimates `BeastSkill` (their Unique halves stay unclassified) |
| DRAGON | none yet. Candidate for the owner: the Dragon Sovereign Beast Ultimate (`bw_dragon`), kept LORD with the other Beast Ultimates as the guide's table says |
| not an NUSkill, recorded only | the Veldanava Ultimates (`entity/veldanava/skills/VeldanavaSkillBase`, guide: GOD), Nullification, Kotodama (a GrimoireBook): they extend Tensura's `Skill` / `GrimoireBook`, so they keep their own costs and mastery |

Each classified skill says it with one line, `@Override public UltimateClass ultimateClass() { return UltimateClass.X; }`. `NUSkill.ultimateClass()` is
null for everything else, which keeps the old `boolean ultimate` path (`Tier.of(mastered, ultimate)`).

## 3. What the class changes

### Cost (P1 #1): `NUSkill.pay` / `charge` / `price`
The 58 NUSkill-family call sites of `SkillUtil.spendMagicules` (which wrote the magicule pool directly, had no creative bypass and showed Tensura a cost of
0) now call `pay(instance, player, mode, flat)` (or, where a failed effect refunds, `price(...)` then `charge(...)` so the refund gives back exactly what
was taken). `charge` puts the price where `getMagiculeCost(entity, instance, mode)` answers it and calls `EnergyHelper.isOutOfEnergy(player, instance, mode)`
once (check and take; creative pays nothing; Tensura's own "not enough magicule" message). The size is `BalanceLaw.classCost(caster, cls, flat)` =
`max(flat, maxMagicule x costPercent / 100 x flat / costReference)`; unclassified skills use `plainCostPercent` 1 % / `plainCostReference` 1,000.
The flat price already carries `costMultiplier` (DMUtil) at its call site, so it is not applied twice. From the 75 % milestone: x0.8.

| Cast (old flat price) | 10k max MP | 400k max MP (guide "late game") | 10M max MP |
|---|---|---|---|
| Meteor, The Visionary (GOD, 4,000) | 4,000 / 4,000 | 4,000 -> **20,000** (5 %) | 4,000 -> **500,000** (5 %) |
| Ea, Gate of Babylon (KING, 8,000) | 8,000 / 8,000 | 8,000 -> **32,000** (8 %) | 8,000 -> **800,000** |
| Ryu, Nen (LORD, 1,200) | 1,200 / 1,200 | 1,200 -> **4,800** (1.2 %) | 1,200 -> **120,000** |
| Ki Blast, Ki (LORD, 400) | 400 / 400 | 400 -> **1,600** | 400 -> **40,000** |
| Firebolt (unclassified Extra, 100) | 100 / 100 | 100 -> **400** (0.1 %) | 100 -> **10,000** |

Early game nothing changes (the flat price wins); a big pool can no longer spam a x12-damage Ultimate for under 1 % of itself (guide A.8 #1).

**Converted later (guide P2, `LegacyPay`; the text below is what this change left):** `GrimoirePageSkill` (1) and `GrimoireMagicSkill` (5). They extend Tensura's `Skill`, not `NUSkill` (the legacy
grimoire skills the guide's P2 #8 plans to retire), so the helper does not reach them; they still pay flat through `spendMagicules`.
`getMagiculeCost` answers only during a payment (0 otherwise, as before), so Tensura's UI still shows no price for these skills.

### Mastery: `getMaxMastery()` from the class
LORD 10,000 (= Tensura's Ultimate default, unchanged), KING 25,000, GOD 50,000, DRAGON 15,000. **Existing mastery is kept:** at login (`NUSkill.onLogin`,
one pass over the player's own skills) a classified instance's mastery is moved to the same fraction of the new max (a mastered Gate of Babylon at
10,000 / 10,000 becomes 25,000 / 25,000, still mastered), and the instance remembers the max it is measured against (tag `NuMasteryMax`), so a later config
change rescales again and the move never runs twice. A newly learned classified skill is stamped in `onLearnSkill`.

### Tier (P1 #4): `Tier.ULTIMATE_MASTERED`
Appended to `Tier`. A mastered classified Ultimate reads it (`Tier.of(mastered, ultimate, split)`). It reads like `ULTIMATE` everywhere: every
`== Tier.ULTIMATE` in `skill/rules` and the skills became `tier.ultimate()`, `pick(a, b, c)` gives the Ultimate value, and the summon switch takes both. New:
`pick(base, mastered, ultimate, ultimateMastered)` for mastered values and `masteryScale` = x1.2 for it (was only reached through `isMastered`). No skill
has new mastered numbers yet: that is the per-skill conversion (guide §5.6 steps 4-5).

### Milestones (§5.2): one implementation in `NUSkill`
`milestone = (int)(masteryFrac x 4)`, classified skills only: 25 % `milestoneRange(instance)` x1.2 (a hook the skill multiplies its range by), 50 %
`milestoneSecondEffect(instance)` (hook), 75 % cost x0.8 (in `price`) and cooldown x0.75 (in `NUSkill.cooldown`, for every cooldown set through it; the few
direct `instance.setCoolDown` calls in the skills do not get it), 100 % mastered (ULTIMATE_MASTERED). Announced once per milestone in chat (gold, lang keys
`anime_multiverse.skill.msg.milestone.1..4`), the highest announced kept in the instance tag `NuMilestone`; checked after a successful payment.
No skill reads the range / second-effect hooks yet.

### Damage: `BalanceLaw.epScale(caster, cls)` and the combined cap
`epScale(caster, cls)` = the old curve capped at the class epCap; `epScale(caster)` stays for every unclassified caller.
`classScale(caster, cls)` = `min(epScale(caster, cls) x lateGameFactor, epCap x lateGameShare)` and `damage(caster, target, raw, mastery, cls)` uses it
instead of `epScale x lateGameScale` (the late-game factor is still applied once per hit: a hit takes one path or the other).

| Damage scaling (EP / max magicule) | unclassified (today) | LORD | KING | GOD / DRAGON |
|---|---|---|---|---|
| 1k EP / small pool | x1 | x1 | x1 | x1 |
| 800k EP / 400k MP | 8.26 x 3.00 = x24.8 | x24.0 | x24.8 | x24.8 |
| 25M EP / 10M MP | 12 x 4.75 = x57 | **x24** (cap) | **x30** (cap) | **x36** (cap) |
| top (EP >= 25M, MP >= 100M) | 12 x 6 = **x72** | x24 | x30 | x36 |

**Combined cap: epCap x lateGameShare (default 3) = x24 / x30 / x36 / x36, never above x72** (12 x 6, the unclassified top), whatever the settings.
**Live since the guide P2 change (docs/guide_fixes_p2.md): `SkillDamage.hit` passes the skill's class.** (Old text: `SkillDamage.hit` (owned by the damage-source worker this round) still calls the 4-argument `BalanceLaw.damage`, so classified hits
today scale as before. The hookup is one line there once `hit` has the skill instance (which the `createSource(instance, ...)` change needs anyway):
`BalanceLaw.damage(caster, target, scaled, masteryFrac, instance.getSkill() instanceof NUSkill s ? s.ultimateClass() : null)`.)

### Attunement (docs/attunement.md)
Its "King class" target (60-150 raw per hit) is a raw-damage band; the KING class carries no raw band (it caps the EP scaling, prices casts and sets mastery),
so the attunement factors stay as they are. Noted in docs/attunement.md; no code change.

## 4. Settings (SERVER, `serverconfig/nusmp-server.toml`, `[ultimateClasses]`)

| Setting | Default | Old behaviour |
|---|---|---|
| `flatCosts` | false | true = the old flat price written straight to the pool (and the old refunds) |
| `classMastery` | true | false = Tensura's mastery (instances are moved back at the next login) |
| `masteredTier` | true | false = a mastered Ultimate stays `ULTIMATE` |
| `milestones` | true | false = no milestone effects or messages |
| `classDamage` | true | false = classified hits use the plain law (only matters once SkillDamage passes the class) |
| `lateGameShare` | 3.0 (1-6) | 6 = epCap x 6 (Lord x48, God x72) |
| `plainCostPercent`, `plainCostReference` | 1.0, 1,000 | percent 0 = unclassified skills pay flat |
| `[ultimateClasses.<lord/king/god/dragon>]` `epCap`, `costPercent`, `costReference`, `maxMastery`, `killBelow`, `shredBelow` | the table | |

## 5. Worst-case cost (rule 11)

- Per cast: one ThreadLocal set / remove, the one `isOutOfEnergy` call (what the grimoires already do), a few config reads and one instance-tag read;
  once per milestone ever, one tag write, one instance sync (ManasCore's normal one) and one chat message to the caster. No entities, no extra packets,
  no allocation beyond a boxed Double.
- Per login: one pass over the player's own learned skills; at most one tag write and sync per classified skill, once (then only after a config change).
- Per tick: nothing new (Toggle Aura upkeep keeps its own pulse interval).

## 6. Tests

- `UltimateClassTest` (pure): columns and order, clamps, class-capped epScale, combined cap (x24 / x30 / x36, never above x72), price (flat floor,
  proportional, NaN safe), milestone math, mastery rescale, the ULTIMATE_MASTERED tier keeps every Ultimate perk in the rules.
- `UltimateClassSourceScanTest`: no NUSkill-family file calls `spendMagicules`, the helper charges once through `getMagiculeCost` + `isOutOfEnergy`, the
  classification matches this doc, the settings are SERVER settings, the class math is pure, `ultclass` is in the smoke list after `greed`.
- Self-tests `ultclass.rules`, `ultclass.skills` (`skill/UltimateClassSelfTests`); prefix `ultclass` in `SMOKE_EXPECT` after `,greed,`.

## NEW / REPLACED / KEPT

| | |
|---|---|
| NEW | `UltimateClass`, `Tier.ULTIMATE_MASTERED` (appended) + `ultimate()` + 4-value `pick`, `NUSkill.ultimateClass / pay / charge / price / milestone*`, `BalanceLaw.epScale(caster, cls) / classScale / damage(..., cls) / classCost`, settings `[ultimateClasses]`, login mastery rescale, milestone messages, self-tests `ultclass.*`, two tests, this doc |
| REPLACED | the NUSkill family's direct magicule writes (58 sites) by the Tensura payment (old path: `flatCosts = true`); `== Tier.ULTIMATE` checks by `tier.ultimate()` (same result for every old tier) |
| KEPT | `SkillUtil.spendMagicules` (fallback + legacy grimoire skills), `epScale(caster)`, the 4-argument `damage`, `Tier.of(mastered, ultimate)`, the `boolean ultimate` path for unclassified skills, every flat price as the early-game price, every skill id and mode |

## Caveats (nothing here was seen in a game)

- `Skill.getMaxMastery()` is overridden on the assumption that Tensura leaves it overridable (the guide shows Magic subclasses overriding it); CI is the check.
- Creative players now pay nothing for these skills (Tensura's rule) and get no refunds.
- The KING and GOD mastery raise (x2.5 / x5) slows mastering those skills to match; already mastered skills stay mastered.
- Class damage is wired in `BalanceLaw` but not yet called (see Damage above); `killBelow` / `shredBelow` and the range / second-effect hooks are data and hooks only.
