# Rades's numbered undead: "just magic circles" (fix 3.7.1 candidate)

Owner report: the Number Summons of Rades (No.4 Jimmy, No.3 David, No.2 Alfredo, No.1 Carl, No.0 Michael Caesal) are "just magic circles, not the model/entity I asked for", and basic Soul Corpse
magic should raise zombies that do not burn in daylight. Nothing here was played in a game (see Caveats); every claim below is either a code reading, a pure test, or a server self-test that CI runs.

## What was built, how it is read, what is not included

* Built: a fix of the real cause, a loud diagnosis, a safe fallback body, a real-grimoire self-test and sun-safety checks.
* Read: server log lines starting `[nusmp] numbered undead` (server) and `[nusmp] numbered undead renderer` / `look` (client, `latest.log`); the self-test `magic.soul_corpse_cast` in the CI smoke test.
* Not included: no new models, textures or spells; no change of any number (health, damage, caps, cooldowns); David and Michael were not found broken by reading (see Caveats).

## Root causes (traced end to end)

| # | Where | What |
|---|-------|------|
| 1 (certain, found by reading) | `NUSMP.java:186` (old LOWEST listener of `CorpseClient::registerLooks`) vs `NUSMP.java:265` (`PlayerModelRegistry.init`, also LOWEST) | Listeners of one priority run in the order they were added. The new-look listener was added first, the dungeon player-model kit's listener second, so the kit's renderer for Jimmy, Alfredo and Carl (the old player-model puppet, `client/playermodel/DungeonRenderers`) ran last and stayed. The body existed on the server, but the client drew the old puppet (or, if that puppet's model did not draw, only the cast circle and the name). |
| 2 (client robustness) | `client/corpse/RadesUndeadRenderer.render` (geo draw returns at once for an empty model) | If the geo model or texture failed to load, nothing was drawn and no line said which file. |
| 3 (diagnosis and test gap) | `corpse/Corpses.summonNamed`, `corpse/SoulCorpseSpells.named`, `corpse/SoulCorpseCastSelfTests` | A refused summon or a body that vanished within seconds left no reason anywhere, and the earlier self-tests never proved that a cast body persists. |

No server-side defect was found in the spawn path (caps, `findSpot`, `addFreshEntity`, attributes, sweep, tracking, hooks).

## What changed

NEW (added; the old stays):

* `PlayerModelRegistry.extend(CorpseClient::registerLooks)` (`NUSMP.java:266`): the new looks are registered inside the kit's own listener after its dungeon puppets, so the order is by construction. The old LOWEST listener at line 186 stays (a second registration of the same renderer is harmless). `corpse.classicLooks` still restores the old looks.
* `corpse/CorpseLookRules` (pure): what a renderer draws (`GEO`, `FALLBACK`, `NOTHING`) and which registration wins.
* `client/corpse/RadesUndeadRenderer`: decides once per kind (again after a resource reload), logs the result, and draws a tinted zombie stand-in (vanilla humanoid, name tag kept) when the model or texture is missing. Client setting `corpse.fallbackLook` (default true); false = the old behaviour (nothing).
* `client/corpse/CorpseClient.report`: one client-log line per numbered kind naming the renderer that really stayed (info when it is the new look, error when another registration won).
* `corpse/RadesCastDiagnosis` (pure): refusal reasons, log lines, "vanished early" rule (under 100 ticks, not a chunk or dimension move), per-player rate limit (one line per second).
* `Corpses.summonNamedChecked` (returns body or the refusal), `SoulCorpseSpells.named` now shows the specific reason to the player and writes it to the server log. Server setting `corpse.diagnose` (default true).
* `NamedCorpseEntity.remove` logs an early, non-dismissed removal; `dismissed()` marks the sweep, cap and logout dismissals so they stay silent.
* Sun safety: `NamedCorpseEntity.isSunBurnTick()` and the new `burnsInDaylight()` answer false (it is a `PathfinderMob`, which never reads daylight; this makes it explicit and testable). `Corpses` sweep puts out a creature raised the old way when it has Fire Resistance, behind server setting `corpse.noSunBurn` (default true; best effort).
* Tests: `CorpseLookRulesTest`, `RadesCastDiagnosisTest`, `RadesCastSourceScanTest`; `SoulCorpseCastSelfTests` section 5 and `RadesUndeadSelfTests` sun rule.

REPLACED: nothing. KEPT: every entity, model, texture, animation, page, spell, number and the old renderers (`NamedCorpseRenderer`, the dungeon puppets) and the old LOWEST listener.

## Sun burning (basic Soul Corpse magic)

* Soul Zombies (`SoulZombieEntity`): `isSunSensitive()` returns `SoulZombieRules.SUN_BURNS` (false) and `isSunBurnTick()` is false, so vanilla `Zombie.aiStep` never ignites them. Already true before this change; now asserted in `SoulZombieRulesTest`, `RadesUndeadSelfTests`, `SoulCorpseCastSelfTests` (also `isOnFire()` after a ticked daylight cast) and the source scan.
* Numbered undead: plain `PathfinderMob`, no daylight code; now explicit and asserted.
* Marionette / Reanimation / Necropolis / Legion creatures raised the older way (`Corpses.raise`) are vanilla zombies and skeletons with a Fire Resistance effect of lifetime + 40 ticks (1.46.0); the sweep now also clears fire while that effect is present. Caveat: fire from a source that is not daylight is also cleared while the effect lasts, which is the same protection the effect already gives against damage.

## Self-test (CI smoke test, prefix `magic.soul_corpse_cast`, no new prefix)

For each of the five numbered pages the test opens the real grimoire page cast (the same handler the key press reaches) with the stand-in player, then asserts a `NamedCorpseEntity` of that kind exists in the level's entity list, is alive, owned by the caster, not invisible, carries the corpse marker and is listed in the roster; then it ticks the body 40 ticks (calling `tick()` itself because game time does not advance in the test) with four owner sweeps (the sweep is given the stand-in as the online owner through `Corpses.sweepAll`) and asserts all of that again. A second part casts the zombie page in daylight and asserts no zombie or numbered undead burns.

## Settings

| Setting | Side | Default | Effect |
|---------|------|---------|--------|
| `corpse.diagnose` | server | true | log lines for refused casts and early vanishing |
| `corpse.noSunBurn` | server | true | sweep puts out fire on old-path raised undead with Fire Resistance |
| `corpse.fallbackLook` | client | true | zombie stand-in instead of drawing nothing |
| `corpse.classicLooks` | client | unchanged | restores the old looks of Jimmy, Alfredo and Carl |

Lang: `tools/lang/rades_cast.json` (setting labels and tooltips only).

## Worst-case cost (rule 11)

* Server: no new entity, no new scan. The diagnosis adds at most one `Map` lookup per refused cast or early removal; log output is at most 1 line per player per second (bounded by online players, a held key cannot flood). The sun clear is one boolean read plus `clearFire()` per old-path body inside the existing sweep (already capped by `corpse.maxActive` per owner); numbered bodies are capped at 3 per owner and `namedGlobalCap` per server. `DIAG_AT` holds one `long` per owner who ever triggered a line (a few bytes each).
* Network: no new packet.
* Client: the decision (`CorpseLookRules.draw`) is one cached enum per kind (5 entries), recomputed only when the model object changes; the stand-in model is built once per renderer; the stand-in draws one humanoid (about 6 cubes, under 40 quads) only when the geo look failed, and the existing distance culling and budgets apply as before. The report runs once at renderer setup (5 lines).

## Caveats

* Not seen in a game. Root cause 1 is certain from the listener ordering in the code; whether a given player's missing body was also another cause could not be proved without a game. The new log lines will show it on the next run.
* David and Michael use `RadesUndeadRenderer` in the first place and were never found broken statically; if they still show only a circle, the client log line `numbered undead look: ... EMPTY or missing` and the server line `made no body: ...` name the cause.
* The type checker used offline reports MC stub artifacts ("abstract; cannot be instantiated") only; Gradle was not run.
* Sources: no wiki lookup was needed (no new canon detail).
