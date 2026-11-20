# Technique durations (heroweapon.durationScale)

Owner request: "a lot of moves have very short durations for abilities or etc. Fix that." This page lists every duration constant of the hero and vassal
weapons, what was done to it, and why. `DurationTableTest` fails when a duration constant of a `*Numbers.java` file is missing from the table below, or when a
value under 40 ticks has no status.

## Mechanism

* One SERVER setting, `heroweapon.durationScale` in `nusmp-heroweapon-server.toml` (`[heroweapon]`): default **2.0**, range 0.25 to 6.0. **1.0 gives the old
  numbers exactly** (the old constants are all kept; nothing was deleted or renamed).
* One helper, `heroweapon/skill/Dur.java` (pure, no game types): `scale(ticks, factor)` clamps the factor to the range, rounds, keeps at least 1 tick (zero or less
  is returned as it is) and holds every result to a **hard cap of 20 minutes** (24000 ticks). `Dur.t` scales a length, `Dur.task` scales and then holds a
  repeating task (zone, aura) to the **400-tick cap** the weapons already had (`Safe.MAX_TICKS`, `SwordNumbers.MAX_TASK_TICKS`, `ShieldNumbers.MAX_TASK_TICKS`),
  `Dur.seconds` scales fire seconds. Until `HeroCore.init` installs the config reader, the factor is 1.0, so unit tests and early code see the old numbers.
* The Weapon Book shows the real numbers: `DurText` (pure, client display only) rewrites exactly the listed base seconds of a skill or passive text; the lang
  files and `FormText`/`*Text` keep the base numbers, so tests and a scale of 1.0 are unchanged.
* **Never scaled twice.** A length is scaled at exactly one place, shown in the Mechanism column:
  * `HELPER`: inside `Strike.effect`, `Status.add`, `Status.slow`, `Status.weaken` (the call sites pass the base number). The unscaled twins `Strike.hold`,
    `Status.addRaw`, `Status.slowRaw` are used where the length is already final (a wind-up hold, a boss slow, a length that went through the law).
  * `SITE`: `Dur.t(...)` / `Dur.seconds(...)` at the one call site that applies the length.
  * `TASK`: `Dur.task(...)` for the repeating task (and `Dur.t` for the state that goes with it).
  * `LAW`: the hard-control binds are scaled first and then pass `BalanceLaw.controlTicks`, so **the control law stays the cap**: 3 s on players, 5 s on mobs,
    1 s on bosses, with its diminishing returns on re-application. A scale of 6.0 therefore cannot lengthen a bind past the law.
* Boss rules are untouched: bosses keep their old short slows (`Strike.hold`, `Status.slowRaw`), and the law caps binds on them at 1 s.

## What is not scaled

Statuses LEFT in the table: flight lives, wind-up and tell delays, pulse and step intervals (`MINUTE_STEP_TICKS`), parry / riposte / evade / combo / tempo windows,
the cast times, resource fades, guard-break locks, self-inflicted prices (the Blood Sacrifice curse, Sloth weight on the caster), and the damage-pulse fields
(Tornado Throw / Axe Tornado). **Damage pulses, damage-over-time and bleed lengths are deliberately not lengthened**: their length multiplies damage, which the
SkillRow / HitCap budgets were balanced for. The Dirge poison is kept for the same reason (its weakness is scaled). Making those longer is an owner decision.
Cooldowns are not scaled.

## Cooldown review

The cooldown of a technique is unchanged. A technique that could now run on top of itself was checked one by one; every zone or state skill has an existing
guard (a replace-or-count guard such as `MirrorNumbers.PANE_MAX = 2`, the `castNumber` of the float blades, one active state per player, or a task that ends the
older one), so none can stack unboundedly, and **no cooldown was raised**. The concurrent same-skill tasks one player can run at scale 2.0 (task length divided
by the effective cooldown: base times 0.5 for the buff factor, minimum 20 ticks) are at most:

| Skill | Task at 2.0 | Effective cooldown | Same-skill tasks at once |
|---|---|---|---|
| shield Provoke | 160 | 80 | 2 |
| instrument March / War Song | 400 / 320 | 120 / 150 | 4 / 3 |
| mirror Glass Shield | 240 | 60 | 2 (PANE_MAX) |
| mirror Stardust Mirror | 320 | 80 | 4 |
| mirror Ressentiment | 160 | 140 | 2 |
| sewing Stitched Barrier | 240 | 90 | 3 |
| staff Float Mirror | 160 and up | 160 | 2 |
| whip Command | 240 | 200 | 2 |
| sword Float Blades | 320 | 200 | 1 (castNumber ends the older) |

Worst case server work per player: each of these tasks ran at most half as long before, so the number of concurrent tasks per player is at most doubled (the
table above is the most), each task does the same bounded work per tick as before (its interval, radius and target caps are unchanged), and the 400-tick cap
bounds every task. **Packets are unchanged**: no new packet; a follow effect sends its (longer) lifetime once at the start. The aggregate caps stay as they were
(zone `MAX_TICKS`, per-player and perf budgets, `PerfSourceScanTest`). No global cap on zones exists in `SpellRuntime`; the bound is the per-skill cooldown
times the 400-tick cap, as in the table.

## The table

Counts: 106 raised, 33 left alone, 7 bow entries that would change (listed only, the bow package is not touched), plus the literal sites below.
"New at 2.0" is the value before the law or the cap of the mechanism (a `LAW` value is then capped by `BalanceLaw.controlTicks`; a `TASK` value is held to 400).
Status: RAISED, LEFT, BOW.

| Weapon | Constant | Skill or use | Old (ticks) | New at 2.0 | Status | Mechanism | Reason |
|---|---|---|---|---|---|---|---|
| axe | `AxeNumbers.POWER_WEAK_TICKS` | axe Power Strike weakness | 100 | 200 | RAISED | HELPER Strike.effect | debuff on the foe |
| axe | `AxeNumbers.POWER_SLOW_TICKS` | axe Power Strike slow | 60 | 120 | RAISED | HELPER Strike.effect | debuff on the foe |
| axe | `AxeNumbers.TORNADO_TICKS` | axe Tornado (damage pulses) | 24 | 24 | LEFT | NONE | damage-pulse zone: lengthening it multiplies the damage the SkillRow budget fixed |
| book | `BookNumbers.WALL_TICKS` | book Wall of Pages | 200 | 400 | RAISED | SITE Dur.t (BookPages) | barrier |
| book | `BookNumbers.CAGE_ROOT` | book Page Cage root | 40 | 80 | RAISED | LAW Dur.t then BalanceLaw.controlTicks | hard control: scaled, then the law caps it (3 s player, 5 s mob, 1 s boss) |
| book | `BookNumbers.REST_LIFE` | book Life Force Burst rest | 140 | 140 | LEFT | NONE | the rest gap of a heavy, a cooldown-like gate |
| bow | `BowNumbers.ARMED_TICKS` | bow armed state | 200 | 200 | BOW | NONE | bow package is not touched; would be Dur.t (10 s to 20 s) |
| bow | `BowNumbers.NET_ROOT` | bow Net Arrow root | 30 | 30 | BOW | NONE | bow package not touched; would be LAW (scaled, then the control law) |
| bow | `BowNumbers.MARK_TICKS` | bow Mark Arrow mark and glow | 160 | 160 | BOW | NONE | bow package not touched; would be Dur.t at BowShots 186-187 (8 s to 16 s) |
| bow | `BowNumbers.SEEK_LIFE` | bow Tracking Shot seek | 40 | 40 | BOW | NONE | flight life of a seeking arrow, mechanical; would stay |
| bow | `BowNumbers.FROZEN_SLOW_TICKS` | bow Frozen Rain slow | 30 | 30 | BOW | NONE | bow package not touched; would be Dur.t |
| bow | `BowNumbers.LAW_TICKS` | bow Law Fanatic buff | 200 | 200 | BOW | NONE | bow package not touched; would be Dur.t at BowDirect 133-135 (10 s to 20 s) |
| bow | `BowNumbers.BULL_ROOT` | bow Bull of Phalaris root | 30 | 30 | BOW | NONE | bow package not touched; would be LAW |
| carriage | `CarriageNumbers.BOOST_TICKS` | carriage Boost | 50 | 100 | RAISED | SITE Dur.t (CarriageSpells) | speed state |
| carriage | `CarriageNumbers.DRIFT_SLOW_TICKS` | carriage Drift slow | 30 | 60 | RAISED | HELPER Status.slow | debuff |
| carriage | `CarriageNumbers.BARRIER_TICKS` | carriage Cabin Barrier | 100 | 200 | RAISED | SITE Dur.t (CarriageSpells) | barrier |
| carriage | `CarriageNumbers.HAIKUIKKU_TICKS` | carriage Haikuikku haste | 160 | 320 | RAISED | SITE Dur.t (CarriageSpells) | buff |
| carriage | `CarriageNumbers.CHARGE_TICKS` | carriage Crash Charge run | 44 | 44 | LEFT | NONE | the charge run itself, a mechanical timing |
| carriage | `CarriageNumbers.CHARGE_STUN_TICKS` | carriage Crash Charge stun | 20 | 40 | RAISED | LAW Dur.t then controlTicks, Status.slowRaw | hard control, capped by the law |
| chain | `ChainNumbers.PULL_TICKS` | chain Pull / Grapple | 14 | 14 | LEFT | NONE | the pull motion, a mechanical timing |
| chain | `ChainNumbers.BINDING_TICKS` | chain Chain Binding | 60 | 120 | RAISED | LAW Dur.t then controlTicks | hard control, capped by the law |
| chain | `ChainNumbers.DEFENCE_TICKS` | chain Chain Defence | 120 | 240 | RAISED | SITE Dur.t (ChainSkills) | barrier buff |
| chain | `ChainNumbers.OROCHI_BIND_TICKS` | chain Orochi bind | 80 | 160 | RAISED | LAW Dur.t then controlTicks | hard control |
| chain | `ChainNumbers.WEIGHT_SLOW_TICKS` | chain weight slow | 40 | 80 | RAISED | SITE Dur.t (ChainSkills) | debuff |
| chain | `ChainNumbers.SLOTH_TICKS` | chain Sloth Weight | 120 | 120 | LEFT | NONE | already 6 s, capped by the control law, and also the caster's own slow; scaling would lengthen the self-slow |
| chain | `ChainNumbers.DEBT_TICKS` | chain Debt Comes Due | 120 | 120 | LEFT | NONE | the delay until the bill is paid (a mechanical timing) and the law-capped bind |
| chain | `ChainNumbers.DEBT_STAGGER` | chain Debt stagger | 20 | 40 | RAISED | LAW Dur.t then controlTicks | stagger, capped by the law |
| claw | `ClawNumbers.RUSH_ARMOR_TICKS` | claw Rush armour shred | 100 | 200 | RAISED | SITE Dur.t (ClawSkills) | debuff |
| claw | `ClawNumbers.INSTINCT_TICKS` | claw Instinct buffs | 160 | 320 | RAISED | HELPER Strike.effect | buff |
| claw | `ClawNumbers.MADNESS_CURSE_TICKS` | claw Madness curse | 160 | 320 | RAISED | HELPER Strike.effect | debuff |
| fan | `FanNumbers.DANCE_SLOW_FALL_TICKS` | fan Wind Dance | 80 | 160 | RAISED | SITE Dur.t (FanSkills) | float state |
| fan | `FanNumbers.VEIL_TICKS` | fan Mist Veil | 80 | 160 | RAISED | SITE Dur.t (FanSkills) | invisibility state |
| fan | `FanNumbers.PLUM_TICKS` | fan Snow Plum haste | 160 | 320 | RAISED | SITE Dur.t (FanSkills) | buff |
| fan | `FanNumbers.GLIDE_TICKS` | fan glide refresh | 12 | 12 | LEFT | NONE | a refreshed glide tick, mechanical |
| gauntlet | `GauntletNumbers.FIST_SLOW_TICKS` | gauntlet Fist slow | 20 | 40 | RAISED | HELPER Status.slow | debuff |
| gauntlet | `GauntletNumbers.CRUSH_WEAK_TICKS` | gauntlet Crush weakness | 60 | 120 | RAISED | HELPER Status.weaken | debuff |
| gauntlet | `GauntletNumbers.GUARD_TICKS` | gauntlet Iron Guard | 60 | 120 | RAISED | SITE Dur.t (GauntletSpells) | guard state |
| hammer | `HammerNumbers.AEGIS_TICKS` | hammer Aegis | 160 | 320 | RAISED | SITE Dur.t (ShieldOps via the shield kit) | buff |
| hammer | `HammerNumbers.CRUSH_TICKS` | hammer Crush armour crack | 100 | 200 | RAISED | SITE Dur.t (HammerPoise.crackArmor) | debuff |
| hammer | `HammerNumbers.CRACK_TICKS` | hammer Crack armour | 160 | 320 | RAISED | SITE Dur.t (HammerPoise.crackArmor) | debuff |
| hammer | `HammerNumbers.BREAK_WEAKNESS_TICKS` | hammer poise break weakness | 100 | 200 | RAISED | HELPER Strike.effect | debuff |
| hammer | `HammerNumbers.BREAK_FRAGILITY_TICKS` | hammer poise break fragility | 60 | 120 | RAISED | HELPER Strike.effect | debuff |
| hammer | `HammerNumbers.BREAK_ROOT_BASE` | hammer poise break root | 30 | 60 | RAISED | LAW Dur.t then controlTicks | hard control |
| hammer | `PoiseMath.BREAK_TICKS` | hammer poise-broken state | 50 | 50 | LEFT | NONE | the broken-state window of the poise meter, mechanical |
| harpoon | `HarpoonNumbers.REEL_TICKS` | harpoon reel | 14 | 14 | LEFT | NONE | the reel motion, mechanical |
| harpoon | `HarpoonNumbers.WHALE_STAGGER` | harpoon Whale stagger | 20 | 40 | RAISED | LAW Dur.t then controlTicks | stagger, capped by the law |
| harpoon | `HarpoonNumbers.CRUCIFY_TICKS` | harpoon Crucifixion | 60 | 120 | RAISED | LAW Dur.t then controlTicks | hard control |
| harpoon | `HarpoonNumbers.FISH_STAGGER` | harpoon Fish stagger | 20 | 40 | RAISED | LAW Dur.t then controlTicks | stagger, capped by the law |
| instrument | `InstrumentNumbers.STUN_CONTROL_TICKS` | instrument Stun Beat | 40 | 80 | RAISED | LAW Dur.t then controlTicks | hard control |
| instrument | `InstrumentNumbers.STUN_BLIND_TICKS` | instrument Stun Beat blind | 20 | 40 | RAISED | SITE Dur.t (InstrumentSkills) | debuff |
| instrument | `InstrumentNumbers.EFFECT_TICKS` | instrument song buff refresh | 60 | 60 | LEFT | NONE | re-applied on every pulse while the (scaled) song lasts; scaling it would only lengthen the tail |
| instrument | `InstrumentNumbers.MARCH_TICKS` | instrument March | 200 | 400 | RAISED | SITE Dur.t (InstrumentSkills.sing) | field; held to the 400-tick task cap by Safe.MAX_TICKS |
| instrument | `InstrumentNumbers.WAR_TICKS` | instrument War Song | 160 | 320 | RAISED | SITE Dur.t (InstrumentSkills.sing) | field; held to the 400-tick task cap |
| instrument | `InstrumentNumbers.LULLABY_TICKS` | instrument Lullaby sleep | 120 | 120 | LEFT | NONE | hard control already at the law cap, scaling changes nothing |
| instrument | `InstrumentNumbers.DIRGE_TICKS` | instrument Dirge poison | 120 | 120 | LEFT | NONE | budgeted poison (a damage-over-time), the weakness it carries is scaled |
| instrument | `InstrumentNumbers.DIRGE_EXTRA_TICKS` | instrument Dirge strong harmony | 40 | 40 | LEFT | NONE | same as Dirge poison |
| katana | `KatanaNumbers.PARRY_TICKS` | katana Counter Stance | 14 | 14 | LEFT | NONE | parry window, mechanical |
| katana | `KatanaNumbers.EVADE_TICKS` | katana Release Evade | 8 | 8 | LEFT | NONE | evade window, mechanical |
| katana | `KatanaNumbers.DOUBLE_TICKS` | katana Double Sword | 120 | 240 | RAISED | SITE Dur.t (KatanaSkills) | state |
| katana | `KatanaNumbers.RIPOSTE_TICKS` | katana Riposte passive | 60 | 60 | LEFT | NONE | riposte window, mechanical |
| mirror | `MirrorNumbers.BASH_STAGGER_TICKS` | mirror Bash stagger | 12 | 24 | RAISED | SITE Dur.t (MirrorSkills) | stagger |
| mirror | `MirrorNumbers.PANE_LIFE` | mirror Glass Shield pane | 120 | 240 | RAISED | TASK Dur.task | barrier; the pane is a repeating task, held to 400 |
| mirror | `MirrorNumbers.STARDUST_TICKS` | mirror Stardust Mirror | 160 | 320 | RAISED | TASK Dur.t (absorption), Dur.task (zone) | buff and field |
| mirror | `MirrorNumbers.REBOUND_TICKS` | mirror Release Rebound window | 80 | 160 | RAISED | SITE Dur.t (MirrorSkills) | state |
| mirror | `MirrorNumbers.REBOUND_RETURN_TICKS` | mirror rebound returned effects | 120 | 240 | RAISED | SITE Dur.t (MirrorSkills) | effects sent back to a foe |
| mirror | `MirrorNumbers.IMAGE_LIFE` | mirror Movement Mirror image | 240 | 480 | RAISED | SITE Dur.t (MirrorSkills) | summon |
| mirror | `MirrorNumbers.CHAIN_BIND_TICKS` | mirror Chain Binding | 60 | 120 | RAISED | LAW Dur.t then controlTicks | hard control |
| mirror | `MirrorNumbers.COVET_TICKS` | mirror Covet | 200 | 400 | RAISED | SITE Dur.t (MirrorSkills) | stolen effect length |
| mirror | `MirrorNumbers.RESSENT_TICKS` | mirror Ressentiment | 80 | 160 | RAISED | SITE Dur.t (Safe.zone) | field (damage once per interval, bounded by SkillRow) |
| projectile | `ProjectileNumbers.MARK_TICKS` | projectile Mark Prey | 160 | 320 | RAISED | SITE Dur.t (ProjectileSpells) | mark |
| projectile | `ProjectileNumbers.TORNADO_TICKS` | projectile Tornado Throw (damage pulses) | 20 | 20 | LEFT | NONE | damage-pulse zone, see AxeNumbers.TORNADO_TICKS |
| projectile | `ProjectileNumbers.TORNADO_SLOW` | projectile Tornado slow | 40 | 80 | RAISED | HELPER Status.slow | debuff |
| projectile | `ProjectileNumbers.TORNADO_HEAVY_SLOW` | projectile Heavy Hatchet tornado slow | 40 | 80 | RAISED | HELPER Status.slow | debuff |
| projectile | `ProjectileNumbers.BOLAS_SLOW` | projectile Bolas slow | 50 | 100 | RAISED | HELPER Status.slow | debuff |
| projectile | `ProjectileNumbers.DRITTE_STAGGER` | projectile Dritte gap between throws | 4 | 4 | LEFT | NONE | a gap between throws, a step interval |
| projectile | `ProjectileNumbers.BIND_TICKS` | projectile Shadow Bind | 50 | 100 | RAISED | LAW Dur.t then controlTicks | hard control |
| scythe | `ScytheNumbers.SOUL_FADE_TICKS` | scythe souls fade | 2400 | 2400 | LEFT | NONE | the souls already last 120 s; a resource that fades, not an effect |
| sewingset | `SewingNumbers.FOE_MARK_TICKS` | sewing needle mark on a foe | 160 | 320 | RAISED | SITE Dur.t (NeedleBoard.mark) | mark |
| sewingset | `SewingNumbers.ALLY_MARK_TICKS` | sewing needle mark on an ally | 400 | 800 | RAISED | SITE Dur.t (NeedleBoard.mark) | mark |
| sewingset | `SewingNumbers.PLACE_TICKS` | sewing planted needle | 400 | 400 | LEFT | NONE | already 20 s, the task cap |
| sewingset | `SewingNumbers.BIND_TICKS` | sewing Bind Wire | 60 | 120 | RAISED | LAW Dur.t then controlTicks | hard control |
| sewingset | `SewingNumbers.BARRIER_TICKS` | sewing Stitched Barrier | 120 | 240 | RAISED | SITE Dur.t (Safe.zone) | barrier field |
| sewingset | `SewingNumbers.BARRIER_SLOW_TICKS` | sewing barrier slow | 40 | 80 | RAISED | SITE Dur.t (SewingSkills) | debuff |
| sewingset | `SewingNumbers.NET_TICKS` | sewing Spider Net | 80 | 160 | RAISED | LAW Dur.t then controlTicks | hard control |
| sewingset | `SewingNumbers.ARIADNE_GLOW` | sewing Ariadne's Thread glow | 160 | 320 | RAISED | SITE Dur.t (SewingSkills) | mark (glowing) |
| shield | `ShieldNumbers.MAX_TASK_TICKS` | shield task cap | 400 | 400 | LEFT | NONE | the aggregate cap, kept |
| shield | `ShieldNumbers.BREAK_TICKS` | shield guard-broken lock | 60 | 60 | LEFT | NONE | the guard-break lock, a penalty and mechanical; the blocking code is not touched |
| shield | `ShieldNumbers.VENOM_TICKS` | shield Venom Fang poison | 80 | 160 | RAISED | SITE Dur.t (ShieldLogic) | poison on the attacker; the damage per tick is unchanged |
| shield | `ShieldNumbers.COVER_TICKS` | shield Cover | 160 | 320 | RAISED | SITE Dur.t (ShieldOps) | barrier |
| shield | `ShieldNumbers.WARD_TICKS` | shield Second Shield ward | 200 | 400 | RAISED | SITE Dur.t (ShieldOps) | barrier |
| shield | `ShieldNumbers.HERB_REGEN_TICKS` | shield Herb Mend | 100 | 200 | RAISED | SITE Dur.t (ShieldOps) | regeneration |
| shield | `ShieldNumbers.AEGIS_TICKS` | shield Soul Aegis | 240 | 480 | RAISED | SITE Dur.t (ShieldOps) | absorption |
| shield | `ShieldNumbers.PANEL_TICKS` | shield panel | 160 | 320 | RAISED | TASK Dur.task | barrier field, a repeating task |
| shield | `ShieldNumbers.PROVOKE_TICKS` | shield Provoke | 80 | 160 | RAISED | TASK Dur.t (resistance), Dur.task (field) | buff and field |
| shield | `ShieldNumbers.PRISON_TICKS` | shield Prison | 80 | 160 | RAISED | LAW Dur.t then controlTicks | hard control |
| shield | `ShieldNumbers.UNDEAD_TICKS` | shield Undead Control | 120 | 240 | RAISED | TASK Dur.task | field |
| shield | `ShieldNumbers.CAGE_MARK_TICKS` | shield cage mark sanity bound | 100 | 100 | LEFT | NONE | only a sanity bound on the stored mark, the mark itself ends with the law-capped bind |
| shield | `ShieldNumbers.STAR_LIFE` | shield star bolt flight | 14 | 14 | LEFT | NONE | flight life of a bolt, mechanical |
| shield | `ShieldNumbers.DEFENCE_TICKS` | shield Absolute Defence | 100 | 200 | RAISED | TASK Dur.task | field |
| shield | `ShieldNumbers.RAGE_BASH_FIRE_SECONDS` | shield Rage Bash fire (seconds) | 3 | 6 | RAISED | SITE Dur.seconds (ShieldOps) | fire |
| shield | `ShieldNumbers.ROAR_SLOW_TICKS` | shield Rage Roar slow | 60 | 120 | RAISED | LAW Dur.t then controlTicks | hard control |
| shield | `ShieldNumbers.ROAR_FRENZY_TICKS` | shield Rage Roar frenzy | 120 | 240 | RAISED | SITE Dur.t (ShieldOps) | buff |
| shield | `ShieldNumbers.BLOOD_ROOT_TICKS` | shield Blood Sacrifice root | 30 | 60 | RAISED | LAW Dur.t then controlTicks | hard control |
| shield | `ShieldNumbers.BLOOD_CURSE_TICKS` | shield Blood Sacrifice curse | 300 | 300 | LEFT | NONE | the caster's own price for the skill; lengthening it would punish the wielder |
| shield | `ShieldNumbers.FURY_TICKS` | shield Rage Fury | 80 | 160 | RAISED | SITE Dur.t (ShieldLogic) | buff |
| shield | `ShieldNumbers.ENDURANCE_REGEN_TICKS` | shield Endurance regeneration | 100 | 200 | RAISED | SITE Dur.t (ShieldLogic) | regeneration |
| spear | `SpearNumbers.VAULT_SLOW_FALL` | spear Vault | 60 | 120 | RAISED | SITE Dur.t (SpearMotion) | float state |
| spear | `SpearNumbers.JAVELIN_LIFE` | spear Javelin flight | 14 | 14 | LEFT | NONE | flight life, mechanical |
| spear | `SpearNumbers.MINUTE_STEP_TICKS` | spear Minute Hand steps | 2 | 2 | LEFT | NONE | step interval, mechanical (named in the task as left alone) |
| spear | `SpearNumbers.PORTAL_ECHO_TICKS` | spear Portal Echo speed | 60 | 120 | RAISED | SITE Dur.t (SpearMotion) | buff |
| spear | `SpearNumbers.RUSH_TICKS` | spear Filo Rush | 50 | 100 | RAISED | TASK Dur.t (speed), Dur.task (zone, each foe is hit once) | buff and rush |
| spear | `SpearNumbers.RIPOSTE_TICKS` | spear Riposte stance | 24 | 24 | LEFT | NONE | riposte window, mechanical |
| spear | `SpearNumbers.FERVOR_TICKS` | spear Lust Fervor | 120 | 240 | RAISED | SITE Dur.t (SpearLust) | buff |
| staff | `StaffNumbers.MARK_TICKS` | staff Analyze mark | 200 | 400 | RAISED | SITE Dur.t (StaffSpells) | mark |
| staff | `StaffNumbers.MARK_EXTRA_TICKS` | staff Star Mark extra | 100 | 200 | RAISED | SITE Dur.t (StaffSpells) | mark |
| staff | `StaffNumbers.HASTE_TICKS` | staff Quick Chant haste | 160 | 320 | RAISED | SITE Dur.t (StaffSpells) | buff |
| staff | `StaffNumbers.RUNE_SLOW_TICKS` | staff Rune Field slow | 30 | 60 | RAISED | HELPER Status.slow | debuff |
| staff | `StaffNumbers.PRISM_TICKS` | staff Magic Prism | 160 | 320 | RAISED | SITE Dur.t (StaffSpells) | barrier |
| staff | `StaffNumbers.PRISM_WIDE_TICKS` | staff Wide Prism extra | 80 | 160 | RAISED | SITE Dur.t (StaffSpells) | barrier |
| staff | `StaffNumbers.MIRROR_TICKS` | staff Float Mirror | 80 | 160 | RAISED | TASK Dur.task | float state and field |
| staff | `StaffNumbers.MIRROR_DEEP_TICKS` | staff Deep Mirror extra | 40 | 80 | RAISED | TASK Dur.task | field |
| sword | `SwordNumbers.MAX_TASK_TICKS` | sword task cap | 400 | 400 | LEFT | NONE | the aggregate cap, kept |
| sword | `SwordNumbers.SPEED_TICKS` | sword tempo speed refresh | 30 | 30 | LEFT | NONE | re-applied on every hit of the tempo; lengthening it would only lengthen the tail |
| sword | `SwordNumbers.GHOST_TICKS` | sword Afterimage ghost | 60 | 120 | RAISED | SITE Dur.t (HitLedger.armGhost) | state; the guard in ghostArmed is scale-aware |
| sword | `SwordNumbers.GRAVITY_EDGE_TICKS` | sword Gravity Edge slow | 30 | 60 | RAISED | SITE Dur.t (SwordLogic) | debuff |
| sword | `SwordNumbers.EMBER_FIRE_SECONDS` | sword Ember Edge fire | 2 | 4 | RAISED | SITE Dur.seconds (SwordLogic) | fire |
| sword | `SwordNumbers.GILDED_TICKS` | sword Gold Rebellion mark | 120 | 240 | RAISED | SITE Dur.t (MarkBook) | mark; the book's sanity bounds are scale-aware |
| sword | `SwordNumbers.STEP_SPEED_TICKS` | sword Flashing step speed | 60 | 120 | RAISED | SITE Dur.t (SwordOps) | buff |
| sword | `SwordNumbers.STEP_GUARD_TICKS` | sword Flashing step guard | 20 | 40 | RAISED | SITE Dur.t (SwordOps) | buff |
| sword | `SwordNumbers.FLASH_BLIND_TICKS` | sword Flash blind | 40 | 80 | RAISED | SITE Dur.t (SwordOps) | debuff |
| sword | `SwordNumbers.FLASH_GLOW_TICKS` | sword Flash glow | 60 | 120 | RAISED | SITE Dur.t (SwordOps) | mark (glowing) |
| sword | `SwordNumbers.FLASH_SPEED_TICKS` | sword Flash speed | 60 | 120 | RAISED | SITE Dur.t (SwordOps) | buff |
| sword | `SwordNumbers.CUTTER_LIFE` | sword cutter flight | 8 | 8 | LEFT | NONE | flight life, mechanical |
| sword | `SwordNumbers.GRAVITY_SLOW_TICKS` | sword Gravity zone slow | 25 | 25 | LEFT | NONE | held per foe under the control law while the zone runs |
| sword | `SwordNumbers.PHOENIX_BIRD_LIFE` | sword Phoenix bird flight | 11 | 11 | LEFT | NONE | flight life, mechanical |
| sword | `SwordNumbers.PHOENIX_FIRE_SECONDS` | sword Phoenix Gale fire | 4 | 8 | RAISED | SITE Dur.seconds (SwordOps) | fire |
| sword | `SwordNumbers.FLOAT_TICKS` | sword Float Blades | 160 | 320 | RAISED | TASK Dur.task | state and task, held to 400 (the float blades keep their castNumber guard) |
| sword | `SwordNumbers.DECLINE_SLOW_TICKS` | sword Strong Decline slow | 40 | 80 | RAISED | SITE Dur.t (SwordOps) | debuff |
| whip | `WhipNumbers.COIL_TICKS` | whip Coil slow | 20 | 40 | RAISED | HELPER Strike.effect | debuff |
| whip | `WhipNumbers.BIND_TICKS` | whip Bind | 40 | 80 | RAISED | LAW Dur.t then controlTicks (WhipSkills.hold) | hard control |
| whip | `WhipNumbers.BIND_SLOW_TICKS` | whip Bind slow | 40 | 80 | RAISED | HELPER Strike.effect | debuff |
| whip | `WhipNumbers.TETHER_SOFT_TICKS` | whip Tether soft landing | 40 | 80 | RAISED | HELPER Strike.effect | float state |
| whip | `WhipNumbers.CHARGE_TICKS` | whip Static Charge mark | 120 | 240 | RAISED | SITE Dur.t (WhipCommand.markCharged) | mark |
| whip | `WhipNumbers.COMMAND_TICKS` | whip Command | 120 | 240 | RAISED | TASK Dur.task | field and mark; held to 400 |
| whip | `WhipNumbers.BOUND_TICKS` | whip bound mark | 100 | 200 | RAISED | SITE Dur.t (WhipCommand.markBound) | mark |
| whip | `WhipNumbers.DIMENSION_BIND_TICKS` | whip Dimension bind | 50 | 100 | RAISED | LAW Dur.t then controlTicks (WhipSkills.hold) | hard control |

### Literal sites (no constant)

| Where | Old | New at 2.0 | Mechanism | Reason |
|---|---|---|---|---|
| `ShieldLogic` Rotting Endurance resistance (literal 60) | 60 | 120 | SITE Dur.t | buff |
| `ShieldOps` Megiddo ignite (literal 4 s) | 4 s | 8 s | SITE Dur.seconds | fire |
| `InstrumentSkills` Dirge weakness (`ticks` of the poison) | 120 / 160 | 240 / 320 | SITE Dur.t | debuff; the poison itself is kept |
| `HammerPoise.crackArmor` (`ticks` argument) | CRUSH 100, CRACK 160 | 200, 320 | SITE Dur.t | armour crack; see HammerNumbers rows |
| `MarkBook`, `HitLedger.ghostArmed`, `WhipCommand.isCommanded / markBound / markCharged` | sanity guards | scale-aware | SITE Dur.t | the old guards compared a stored end time with the base constant and would have dropped the longer marks |
| `NeedleBoard.mark` | per call | 2x | SITE Dur.t | one place for every mark of the sewing set |

### Bow package (not changed, listed only)

`heroweapon/weapon/bow` is outside this change. At the same scale these would change: `BowNumbers.ARMED_TICKS` (10 s), `MARK_TICKS` (8 s mark and glow,
`BowShots` 186-187), `LAW_TICKS` (10 s buff, `BowDirect` 133-135), `FROZEN_SLOW_TICKS`; as LAW-capped binds `NET_ROOT`, `BULL_ROOT`. `SEEK_LIFE` is a flight life and
would stay.

## Other systems with very short durations (listed, not changed)

* grimoire books `book/*` (about 418 effect sites of 140 ticks or less): the slows of 20 to 50 ticks in `IceBook`, `TimeBook` (Time Magic is protected, do not
  touch), `SpatialWorks`, `AirBook`, `BriarArts`, `GravityBook`, `CharacterArtsBulls`, `ModificationArts`; `SoulCorpseBook` invisibility 40.
* Saiyan techniques: `SaiyanTechniques3` line 219, a 20-tick hold refreshed every 10 ticks.
* devil: `devil/phase2/Phase2` darkness 100 ticks.
* Greed forms: `GreedService` 528-557, night vision 60, speed 40, slow 40 (twice).
* relics: `OrderRework` slow 60, `DjinnTraits` foresight / quicken buffs of 40 to 60, `DjinnMagic` regeneration 140, `SocketActs` 60 to 160, `EarthKit` and
  `SmokeBook` riders.
* `ManaLaw` wither 40.

## Owner decisions

1. The default scale (2.0). Everything can go back with `heroweapon.durationScale = 1.0`.
2. Whether damage-pulse fields, damage-over-time and bleed should also get longer (they are not, because they scale damage).
3. Whether the 400-tick task cap should rise (it holds a field to 20 s even at scale 6).
4. The bow, and the other systems above, in a later change.
