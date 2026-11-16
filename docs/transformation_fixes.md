# Transformation fixes: forms and death, respawn, relog

Owner's report: "The transformations, you need to fix them because they sometimes bug when you die or whatever."

This pass read every transformation / form-change / mode-armour system of the mod against one checklist (death with and without keepInventory,
respawn, End-portal return, dimension change, logout/login, restart, clone, attribute modifiers left behind, effects and flight left on, render
state not cleared, stale cooldowns, static maps keyed by something other than the player UUID, a form applied to a dead player, a form applied
twice). It could be done by reading the code only: **nothing in this document was seen in a running game** (see "What cannot be verified").

## The policy (documented, one place: `core/xform/TransformPolicy`)

| Moment | What the guard does | Who decides the rest |
|---|---|---|
| Death (the dying body, `LivingDeathEvent`, lowest priority = after every system's own handler) | ends every form: hooks, form flags stripped, form modifiers removed by id, every render layer cleared | the systems' own death handlers still run first |
| Clone after a death (`PlayerEvent.Clone`, `isWasDeath`, highest priority) | strips the form flags that were copied to the new body | |
| Respawn after a death (`PlayerRespawnEvent`, not End-conquered, highest priority = before any system rebuilds what should persist) | the same sweep on the new body | the systems then re-apply only what persists (racial bonuses, passives, permanent prices) |
| Return from the End (`isEndConquered`) | nothing: it is not a death | each system (a toggled armour comes back like at a login) |
| Dimension change, logout, login | nothing | each system (a Saiyan form ends on a dimension change, a toggled armour survives a relog) |

Default: **forms end on death and nothing resumes.** Setting `transformations.resetOnDeath` (SERVER, `NUConfig`, default `true`); `false` = the guard does nothing at all and every
system behaves exactly as before this change (the dead-body guards below are bug fixes and stay on).

## Bugs found (file:line are after the change)

| # | Bug | Where | Fix |
|---|---|---|---|
| 1 | **Black Form survived death.** Its on/off switch is a flag (`BlackForm`) in the Anti-Magic grimoire's skill tag (and `isToggled()` on the older Anti-Magic Lord skill); skill data is copied to the new body, so after the respawn the upkeep found the form still on, re-drew the demon armour, re-added the +25% attack modifier and the speed effect. Worse, the time limit (`nusmp_devil_form_since`) could be gone, so the 5-minute limit of Devil Union never fired. | `book/AntiMagicBook.java:52,96` (flag), `antimagic/AntiMagicLordSkill.java:99` (toggle) | `FormHooks.endBlackForm` / `endLordForm` (hook `anti_magic_black_form`) switch both off on death and again on respawn |
| 2 | **Devil Union / Noelle aftermath state not cleared on death.** `DevilFormLimit`'s own comment says "neither survives death, which clears the debuffs too" but nothing cleared `nusmp_devil_form_since` / `nusmp_devil_form_after_until` / `nusmp_noelle_black_after` (persistent data; whether NeoForge copies it into the new body is not something I could check). | `book/DevilFormLimit.java:16,21`, `book/NoelleFormState.java:31` | flags registered with the guard (constants made `public`), stripped from the dying body, from the clone and from the respawned body |
| 3 | **Devil Union / Devil Dive flag of the element books (`nusmp_devil_until`, `nusmp_devil_src`)** is never removed on death (8 s long, but it carries the HP drain and the +10% pierce into the next body if the data is copied). | `book/GrimoireBook.java:441-446` | registered with the guard |
| 4 | **Mode-armour overlay flags (`nusmp_mode_armor*`) and Unite flags (`nusmp_unite_*`)** are only reset through their own death handler; a path that skips it (a cancelled/late death, a copied tag) leaves the form on the next body. | `blackclover/ModeArmor.java:34`, `shadow/UniteState.java:23` | registered with the guard as the net under the systems' own handlers |
| 5 | **Render layers outlive the body.** `PlayerAuras` kept every lease (the Saiyan form look 60 ticks, armours, Spirit Dive, Beast Will...) in its server table by UUID, so the dead body kept being drawn, and a viewer who started tracking the respawned body (same entity id, same UUID) was *replayed* the stale lease by `onStartTracking`. | `aura/PlayerAuras.java` | new `PlayerAuras.clearAll(player)` (`:51`): drops the table and sends one clear packet per still-running layer to the **tracking players only**; called by the guard on death/respawn |
| 6 | **Saiyan form look left running after the form was dropped silently** by a change of dimension, a logout, a death or a race removal: `RUN.remove` dropped the form with no `lookEnd`, so other players saw the form for up to 3 s (and the shatter never played). | `saiyan/SaiyanManager.java:331,368,375` | `endLook(p, RUN.remove(uuid))` on `onEnter` and `cleanRuntime` |
| 7 | **A dead body was upkept and could start forms.** `SaiyanManager.tick` and `BeastWillManager.tick` ran for the dead player: the runtime record dropped at death was rebuilt, the racial / passive modifiers re-applied to the corpse, sync packets sent; and a form key sent from the death screen was accepted (the net handlers never looked at `isAlive`), so a form (and its attribute modifiers, Ki payment, charge) could be started on the dead body. | `saiyan/SaiyanManager.java:92`, `saiyan/SaiyanNet.java:118`, `beastwill/BeastWillManager.java:72`, `beastwill/BeastWillNet.java:158` | `!p.isAlive()` return |

Checked and found correct (no change): Saiyan forms (transient fixed-id modifiers, `SaiyanAttributes.clear` on death/logout/race removal, flight given back, `Run` keyed by UUID and
dropped at death/logout/dimension/server stop), Beast Will (`BeastWillAttributes.clear` + aura off + cancel pending + `phase=OFF` on death and at every login/respawn/dimension),
`ArmourState` (ends on death with no animation, a form never survives a logout, `onRespawn` drops it, End return restores like a login), Spirit Dive (`stop` zeroes `DiveUntil` and the overlay),
Noelle forms (ride on `ArmourState`; hidden 3 s effects expire alone), Devil Contract (live part reset in `onClone`, permanent prices re-applied idempotently by id, flight modifier removed),
Ultimate casts (`UltimateManager` cancels on death/dimension/logout), Hero weapon forms (loadout refreshed on respawn/dimension, per-UUID maps `forget`-ed at logout), Greed forms (cleaned at logout/respawn).
All static per-player maps found in these systems are keyed by UUID and have a logout or clear hook.

## What changed (files)

New (`core/xform/`): `TransformPolicy` (pure policy), `TransformGuard` (registry + the three listeners + `end()`), `FormHooks` (what each system registers), `XformGuardSelfTests`.
Changed: `NUConfig` (`transformations.resetOnDeath`), `NUSMP` (registration), `aura/PlayerAuras` (`clearAll`), `saiyan/SaiyanManager`, `saiyan/SaiyanNet`, `beastwill/BeastWillManager`,
`beastwill/BeastWillNet`, and visibility only (`private`/package to `public`) of the flag constants in `DevilFormLimit`, `NoelleFormState`, `ArmourState`, `ModeArmor`, `UniteState`.
No lang keys, no packet type, no entity. The client already understood the existing `PlayerAuraPayload` (ticks 0 = ends) and `ModePayload`, so the "resync" is those small payloads, sent to
tracking players only.

New vs replacement (rule 5): everything is **new**; nothing was replaced or removed. The old behaviour is restored by `resetOnDeath=false` (bugs 1-5); bugs 6-7 are plain fixes.

## Per-system table

| System | State lives in | Ends on death | Ends/kept on dimension | Logout / login | Guard coverage |
|---|---|---|---|---|---|
| Saiyan forms, Ki flight, techniques | `SaiyanManager.Run` (memory, by UUID), transient modifiers `saiyan/*` | yes (own handler) | form ends (own) | `Run` dropped | modifiers `saiyan/form_*`, `saiyan/tech_*` swept; look cleared (6); dead-body guards (7) |
| Beast Will forms | `BeastWillState.phase` (attachment), `Run`, modifiers `bw/*` | yes (own) | form cut to OFF (own) | cut to OFF at login (own) | auras cleared; dead-body guards (7) |
| Black Form (Anti-Magic book / Lord skill) | skill tag `BlackForm`, `isToggled()`, persistent `nusmp_devil_form_*` | **no -> fixed (1, 2)** | kept | kept (intended) | hook + flags + modifier `black_form` |
| Devil Union / Devil Dive (element books) | persistent `nusmp_devil_until/src` | no -> fixed (3) | kept | kept | flags |
| Armour / Spirit Dive forms, Noelle forms | persistent `nusmp_armour`, auras | yes (own) | kept, re-synced | toggled armour kept, forms dropped (own) | flag + modifiers `armour_*` as a net |
| Mode armours (Wind/Fire dive, Demon, Lightning God, Valkyrie) | persistent `nusmp_mode_armor*`, `ModePayload` | stop (own) | kept | re-sent at login (own) | flags (4) |
| Devil Unite modes (Shadow) | persistent `nusmp_unite_*`, modifiers `unite_*` | stop (own) | kept | refreshed at login (own) | flags + modifiers (4) |
| Devil Contract union / flight | attachment + modifiers | union ends (own) | stats re-applied (own) | union ends (own) | not a guard system |
| Ultimate casts (Genesis etc.) | `UltimateManager.SESSIONS` by UUID | cancelled (own) | cancelled (own) | cancelled (own) | not a guard system |
| Hero weapon forms, Greed forms | bond attachment / per-UUID maps | n/a (weapon forms) | refreshed (own) | maps forgotten (own) | not a guard system |
| Render layers (all `Aura`) | `PlayerAuras` table by UUID + client table by entity id | **no -> fixed (5)** | kept | server table dropped | `clearAll` |
| Avatar / Zogratis / Veldanava forms | entities or boss AI, not a player body | n/a | n/a | n/a | out of scope (no player form state found) |

## Cost (rule 11)

Nothing runs per tick. A death, a clone and a respawn each do ~10 flag lookups, ~16 modifier lookups and, at most, one 5-byte clear packet per layer that was still running, to the
players tracking the body (a handful; expired leases cost none). The guard keeps no per-player map. Worst case: a mass death of N players = N x (a few dozen map reads + <= ~5 small packets to nearby
players). The two dead-body guards remove per-tick work (a dead player is no longer upkept and no longer sends sync packets).

## Server safety (rule 9)

All logic is server-side; no client class is touched from common code; the settings are SERVER config; the net handlers validate the sender (`ServerPlayer`) and now the body's liveness.

## Tests

* `TransformPolicyTest` (pure): the policy table, the clone rule, the own-namespace rules.
* `TransformGuardSourceScanTest`: every swept modifier id and every stripped flag really exists in a system, permanent prices/racial bonuses are never swept, the listeners run at the intended
  priorities, the guard has no tick work and no client class, the setting exists with default `true`, the dead-body guards stay, `clearAll` never broadcasts to everyone.
* Server self-test, prefix **`xformguard`** (CI smoke): `xformguard.rules`, `xformguard.lifecycle` (a stand-in player wears a form: death and respawn end it and keep the unrelated flag and
  modifier, the setting off keeps all, the End/dimension/login/logout keep all, the clone strip, the Black Form hook).

## What cannot be verified without a game

* Whether NeoForge 1.21.1 copies a player's `getPersistentData()` into the respawned body. The code base assumes it does not (`ArmourState` javadoc); the guard is written to be correct either way
  (it strips on the dying body, on the clone and on the respawn), but bugs 2-4 only matter in the "it is copied" world.
* Whether ManasCore copies the skill tag (`BlackForm`) at death exactly as read here; the hook ends the flag on the dying body and again on the new one, so either way is covered.
* The self-test stand-in has no network connection, so the clear *packets* of `PlayerAuras.clearAll` are not exercised by it (the table and the sweep are).
* Nothing was compiled here (no local Gradle): CI is the first compile.

## Remaining risks / not changed

* `PlayerAuraClient.ACTIVE` (client, keyed by entity id) is never pruned except at disconnect; entries only expire by their lease, so a stale entry for a removed entity is harmless but not freed.
* A death cancelled by another mod after our lowest-priority handler would still have had its forms ended (the guard runs on the event, not on the confirmed death).
* Forms that keep their state in a third-party mod's data (Tensura skills toggled by Tensura itself) are not seen by the guard.
* Aftermath penalties (Devil Union muscle pain, Black Dragon exhaustion) are cleared by death, as the class comment always said; if the owner wants dying to NOT cancel a penalty,
  remove those two keys from `FormHooks`.
* Logout/login and dimension change are deliberately left to each system (policy table above); a system that wants its form to end there must do it itself.
