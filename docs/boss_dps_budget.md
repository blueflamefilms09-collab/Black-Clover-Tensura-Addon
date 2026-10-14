# Boss per-second damage budget (DPS cap)

Every boss of this mod can lose only a fraction of its max health per second (default 6%). Hits, true damage, mod effects and
`/kill` all draw from the same budget, so nothing one-shots a boss; it still dies through the normal path, just spread over seconds.
Healing, the phase logic and the existing per-hit clamps (`PowerCurve.clampDamage`, mitigation, Zagred's act thresholds) are untouched.

Covered: every `ZogratisBoss` (Dante, Vanica, Zenon, Lucius, Devil Avatar, Goliath, Charybdis Spawn, the God of Order) and `ZagredBossEntity`.
Never covered: vanilla or Tensura entities, and the Monarch's shadow soldiers and the Juggernaut (Tensura mobs, not classes of this mod); only classes that implement `BudgetedBoss` are budgeted.

Config (`world/serverconfig/nusmp-boss-budget-server.toml`, section `bosses`): `dpsCapEnabled` (true), `dpsCapFraction` (0.06),
`dpsCapMinimum` (20 hp/s), `dpsCapFinalPhaseScale` (1.5), `dpsCapIgnoreCreative` (true).

## Components, and why each exists

**`DamageBudget` (pure).** Holds the health still allowed in the current 20-tick window plus its own tick counter. `allow(amount)` returns
the amount, the rest of the budget when the amount is larger, or 0. Why: one tiny class with one rule is testable without Minecraft (mutation
checked), and truncating (not rejecting) means a huge blow still does its share instead of nothing. Why one instance per entity: two bosses
at once, or a boss and its clone, must not drain each other; and the counter lives in the instance so each boss's second starts when it spawned.
Why `setCap` never refills: otherwise a phase change would be a free refill of the window.

**`BudgetPolicy` (pure).** The cap formula, the phase scale and `settle`. Why a fraction of max health: a cap in plain numbers would be wrong
for every boss whose health is tuned by buffs or config; a fraction scales with them. Why a minimum: a test boss of a few hearts would
otherwise be a long fight at 6%; the minimum is also clipped to max health so the cap is never bigger than the bar. Why a phase scale: the
last phase (Gates open / act 4) already has its own pressure, and a flat cap there would make the end of the fight drag; the cap grows
half a step one phase before, and the full step in the final phase, and never shrinks. Why `settle`: the one place that decides what health a
boss ends on, so a fall that fits lands exactly on the requested value (a kill reaches 0 and `die()` runs, no 0.01 limbo from float drift), a fall that does
not fit is cut, and a rise is never touched.

**`BossBudgetConfig`.** Own spec file so the budget can be tuned or switched off without touching other configs. Reads fall back to the defaults
before the config is loaded (the same pattern as the Charter config).

**`BudgetedBoss`.** One required method (`budget()`), two optional phase methods. Why an interface: any future boss gets the whole system by
implementing it and forwarding three hooks; the shared code never needs to know the boss class.

**`BossBudget`.** The Minecraft-facing half, one per entity. `tick` (from `baseTick`) keeps the cap current (max health or phase may change)
and resets the window every 20 ticks. `hurt` truncates the FINAL amount after the boss's own mitigation and clamps, so a hit is reduced once, not
twice, then runs vanilla `hurt` with the budget bypassed. `setHealth` catches the falls that never pass `hurt` (true damage, other mods, the
last step of `/kill`). Why the bypass: vanilla `hurt` ends in `setHealth(health - damage)`; without the bypass that fall would be charged a second
time. The same bypass wraps loading (`readAdditionalSaveData`), because loading the saved health is a fall from the full-health default. A
fall to or above max health is also passed (attribute clamps). Why creative attackers pass whole (config): creative is the tester's tool and
the instant clear; the cap is a survival rule. Why it does nothing until the first tick: until then no cap exists, so letting everything through is safer
than blocking everything.

**Hooks in the entities** (`ZogratisBoss`, `ZagredBossEntity`): `baseTick`, `setHealth`, the final line(s) of `hurt` (also the
invulnerability-bypass early return, so `/kill` and the void are capped) and the load bypass. Stage: Zogratis `PowerCurve.stage(power())` of 0..4
(Gates = final); Zagred `phase() - 1` of 0..3.

## Adding a boss
Implement `BudgetedBoss`, keep `private final BossBudget dpsBudget = new BossBudget();`, then forward: `baseTick -> dpsBudget.tick(this, this)`,
`setHealth -> super.setHealth(dpsBudget == null ? h : dpsBudget.setHealth(this, h))`, `hurt -> dpsBudget.hurt(this, src, finalAmount, x -> super.hurt(src, x))`,
and wrap `super.readAdditionalSaveData` in `dpsBudget.bypass(...)`. The `== null` check is needed because `LivingEntity`'s constructor calls `setHealth`
before the subclass fields exist.
