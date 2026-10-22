# Soul Corpse summons: why none of them could be cast, and the fix

Owner report on 2.1.0: "None of the new Soul Corpse summon spells work." Nothing here was run in a game. The cause was found by reading the code from the grimoire page to the zombie, then proved with a model of the unlock loop (`SoulCorpseAccessTest`, which runs the old rules and the new rules side by side). Type-checked against the API stubs (only the known stub artefacts remain, negative control done); the pure tests and the source-scan test run; the new CI self-test `magic.soul_corpse_cast` needs a server.

## Root causes (plain words)

The cast itself, the zombie entity and the summon code are fine: I traced `GrimoireBook.castPage` -> `SafeCast.run` -> the spirit and fire wrappers -> `SoulCorpseBook` -> `SoulZombies.summon` -> `addFreshEntity` and found no defect (the three wrappers hand a Soul Corpse page straight to `page.cast()`; the entity type, its attributes, its renderer, the hooks, the server config and the caps are all registered). The summon pages could not be **chosen**: a grimoire page only opens through `MasteryPages.tryUnlock`, and its three rules asked for more than a player can give.

1. **Mastery.** A page opens when mastery reaches the larger of its place in the book (page k of n) and its spell cost (cost % / 35). The summons were appended at the END of a 15 page book: Marionette is page 10 (67 % of the mastery bar), David page 13 (87 %), Necropolis, Legion, True Soul Corpse and Michael Caesal are signature pages (35 % cost = the full bar). The 1.46.0 fix ("Soul Corpse Magic summons nothing") only opened Reanimation by hand; none of the later summons got the same treatment.
2. **Rank.** A signature page also needs the Magic Knight rank Senior (`multiverse.gateSignature`). Necropolis, Legion, True Soul Corpse Magic and Michael Caesal therefore stayed shut for anyone below Senior even at full mastery.
3. **Page slots.** A grimoire has 6 page slots, growing to 12 with mastery (16 with a contract), and opens pages in book order. A Soul Corpse holder with the rank and the full bar uses all 12 slots on the earlier pages, so **David and Michael Caesal (the newest pages) are never reached**, whoever you are. A 15 page book cannot fit into 12 slots.
4. **Silent cast.** The Marionette and Legion returned "success" even when no zombie stood up (the server cap was full or the world refused the body), so the player saw a cast animation, paid mana and got a cooldown with nothing to show.
5. **Test gap.** `magic.rades_undead` summons by calling `SoulZombies.summon` and `Corpses.summonNamed` directly, so it stayed green while no player could reach a page. (Also: the page text of `true_soul_corpse` was missing in the lang file.)

What I checked and found fine (so the fix is not in these): the Rades lock lines in `CharacterSpells` (only `true_soul_corpse`, `corpse_jimmy`, `corpse_david`, `corpse_alfredo`, `corpse_carl`, `corpse_michael_caesal`; `corpse_reanimate`, Marionette, Swap, Legion and the generic five were never locked); the entity registration and default attributes; `Zombie` overrides (`isSunSensitive`, `isSunBurnTick`, `convertsInWater`, `shouldDespawnInPeaceful`); the goals, owner tagging, `LivingChangeTargetEvent` guard; the per-player/global caps and the cleanup on logout/death/dimension (`Corpses.sweep`, `crumbleAllOf`); prop kinds `SOUL_CORPSE_1/2` (appended in order, behaviours registered, painter registered); `VfxShape.SOUL_CORPSE_FX1..3`; the config default `corpse.zombiesInsteadOfPuppets = true`; the Armour policy (Soul Corpse has `kind: NONE`, no page is an armour page); `SafeCast` (it catches exceptions and plays the fail clip, but I found nothing in the Soul Corpse code that throws).

## The fix

One pure rule class, `corpse/SoulCorpseAccess`, and three small hooks.

| page | before | now |
|---|---|---|
| Reanimation (`corpse_reanimate`) | opened for every holder | same (starter) |
| Marionette (`corpse_marionette`) | 67 % of the bar + a free slot | **opens at once for every holder** (starter page) |
| Necropolis (`attribute_domain`) | full bar + Senior | **30 % of the bar, no rank** |
| Legion (`corpse_legion`) | full bar + Senior | **50 % of the bar, no rank** |
| David (`corpse_david`, Rades only) | 87 % + a free slot (never reached) | **60 %, no slot needed** |
| True Soul Corpse (`true_soul_corpse`, Rades only) | full bar + Senior | **70 %, no rank, no slot needed** |
| Michael Caesal (`corpse_michael_caesal`, Rades only) | full bar + Senior + slot (never reached) | **80 %, no rank, no slot needed** |
| Jimmy, Alfredo, Carl (Rades only), Swap, the generic five | book rule | unchanged |

* The capped pages are scaled by the server's `multiverse.masteryScale` as before, and a rank written for a page in `multiverse.pageGates` still wins over the waiver.
* The starter and capped pages take no page slot and are not stopped when the slots are full (`SoulCorpseAccess.slotFree`); other books are untouched (every rule is keyed on the book path `book_soul_corpse`).
* Gating decision: the generic summons (Reanimation, Marionette, Necropolis, Legion) are open to any Soul Corpse holder; only the five numbered undead and True Soul Corpse Magic are Rades's, exactly as the lock lines in `CharacterSpells` already say (kept, not edited; a test pins that the lock lines and `SoulCorpseAccess.RADES_ONLY` agree).
* The Marionette and Legion now say why when nothing rose ("Too many risen dead on this server right now." / "The dead cannot rise here.") and return false, so there is no cooldown for a cast that did nothing.
* `SoulZombies.summon` logs at debug level when it makes none, when the world refuses to create the entity and when `addFreshEntity` refuses it (no log at info level, no spam).
* The codex (client) shows the mastery the page really asks (`MasteryPages.needed`).
* Lang: the missing page text for `true_soul_corpse` (`tools/lang/soul_corpse_fix.json`).

Unchanged on purpose: every page id, order and unlock bit, magicule costs and cooldowns (Legion 35 % and 90 s, Necropolis 35 % and 45 s, Marionette 12 % and 13 s), the zombie caps (6 per player, 60 per server, numbered undead 3 per player / 30 per server), no hunger anywhere, `corpse.zombiesInsteadOfPuppets` (default true, false = the old puppet props), the old puppet path, the named undead.

## Cost (rules 9 and 11)

* No new entities, no new packets, no new per-tick work. The only new server work is in the unlock tick (every 2 s per player): one pass over the 15 pages with string comparisons.
* Entities in the worst case are the old ones: per player 6 zombies + 3 numbered undead, server-wide 60 + 30 (config caps), never saved, gone on logout, death, dimension change and server stop.
* The self-test runs only in CI on the throw-away world.
* All state is decided on the server; the only client change is a number shown in the codex.

## Tests

* `SoulCorpseAccessTest` (pure, 7): the table, who may open what, the old rule vs the new rule for every summon (Marionette 67 %, Necropolis/Legion 100 % + rank, David 87 %), a model of the unlock loop with and without the fix (the old rules leave the Marionette shut at 60 %, Necropolis/Legion shut without Senior, and a Senior Rades at full mastery loses David, Michael Caesal and Legion to the 12-slot limit; the new rules open everything but the rank-gated Grave Field), the server scale, the caps untouched.
* `SoulCorpsePagesSourceScanTest` (source scan, 7): every page id has a handler that exists, the lock lines are exactly Rades's six and no generic summon is locked, `MasteryPages.tryUnlock` goes through `needed` / `rankWaived` / `slotFree` (and the server config rank still wins), the starter pages are opened on the player tick, the Marionette and Legion say why when nothing rose, the registry/attribute/renderer/hook/config wiring and the caps, the self-test goes through `GrimoireBook.castFromStyle`, every page has a text.
* CI self-test **`magic.soul_corpse_cast`** (`corpse/SoulCorpseCastSelfTests`, registered from `CorpseHooks.register`; the lead adds the prefix to `SMOKE_EXPECT` in `.github/workflows/smoke.yml`): on a stand-in player, 1) the book's page table against `SoulCorpseAccess` (order, costs, locks), 2) the stand-in learns the Soul Corpse grimoire, the starters open, with full mastery the real `MasteryPages.tryUnlock` opens Necropolis and Legion with no rank and keeps Rades's six shut, 3) with the grimoire summoned, the real cast path `castFromStyle` (usable, holding the book, cooldown, mana, `SafeCast`, the wrappers, cooldown, mastery, hooks) is run for Marionette, Necropolis, Legion and Reanimation and the zombies must stand, owned and tagged, under the caps (and Legion cast again and again never passes them), 4) a sealed Rades page (David) does nothing for a non-Rades caster, then with Rades's canon bound all six pages open and the cast of Jimmy, David, Alfredo, Carl and Michael Caesal raises each one within the cap of 3.
* Ran: the whole corpse test package, `PerfSourceScanTest`, `ServerSafetyTest`, `ConfigFileNamesTest`, `ScreenBlurGuardTest`, `ParticleThemesTest`, `ArmourPolicyTest`, `CanonNamingTest`, `FlatPropsSourceScanTest`, `WikiStylesTest`, `MagicRebuildRulesTest`, `MagicRebuildAssetsTest`, `AlignmentDataTest`, `SpatialPageOrderTest`, `IsolationTest`: all green. `MagicEnumAuditTest` and two `GrimoireDesignGeneratorTest` cases do not load in the offline shim (they need Minecraft classes), as before.

## NEW / REPLACED / KEPT

| what | status |
|---|---|
| `corpse/SoulCorpseAccess` (who may open which Soul Corpse page, starters, caps, slot rule) | NEW |
| `MasteryPages.needed`, the rank waiver in `MasteryPages.gate`, the slot exemption in `MasteryPages.tryUnlock` (Soul Corpse book only) | NEW (small hooks in a shared file) |
| `Corpses.openStarterPage`: one starter page (Reanimation) | REPLACED by every page of `SoulCorpseAccess.STARTER` (Reanimation + Marionette) |
| Unlock requirements of Marionette, Necropolis, Legion, David, True Soul Corpse, Michael Caesal | REPLACED (table above) |
| Marionette and Legion: silent success when nothing rose | REPLACED by a message and no cooldown |
| `SoulZombies.refusal`, debug log lines | NEW |
| `magic.soul_corpse_cast`, `SoulCorpseAccessTest`, `SoulCorpsePagesSourceScanTest`, `tools/lang/soul_corpse_fix.json` | NEW |
| All page ids, positions, costs, cooldowns, Rades lock lines, caps, config switch, old puppets, named undead, `magic.rades_undead` | KEPT |

## Not verified (honest list)

* Nothing was run in a game. The diagnosis is by reading: it is certain that the unlock rules sealed these pages (the model reproduces it with the numbers of the code), but I cannot rule out a second, runtime-only fault (an exception thrown inside a cast would be swallowed by `SafeCast` and look the same); I found none in the Soul Corpse code. If a page still "does nothing" in game, look for an `[nusmp]` error line from `SafeCast` in the server log (it logs the first fault of each magic) and for the new debug lines of `SoulZombies`.
* The self-test is new and untested in CI. It uses a stand-in player (no connection) for `learnSkill`, `GrimoireSummon.summon`, `MasteryPages.tryUnlock` and `castFromStyle`; a hook after the cast that cannot cope with a stand-in is reported in the result text (the zombie count decides pass or fail). If it is red the first time, read the detail line: it names the page and the step.
* The self-test does not cover: the key press and hold timing (`onPressed`/`onHeld`/`onRelease`, the 4 to 7 tick wind-up and `SpellRuntime.later`), the client (what the page list and the codex look like, the unlock overlay), two casters at once, the dimension change and logout (shared with `magic.rades_undead`), the look of the zombies and numbered undead, and the 40-tick scheduling of the starter and mastery ticks (they are called directly).
* The mastery numbers (30 / 50 / 60 / 70 / 80 %) are a design choice of mine to make the strong summons reachable but still earned by casting; the owner may want them higher or lower (one table in `SoulCorpseAccess.MASTERY_CAP`), or to open Necropolis and Legion from the start like the Marionette.
* A player who already had Necropolis or Legion sealed gets them at the next unlock tick; a server that sets `multiverse.pageGates` for these pages keeps its own rank.
