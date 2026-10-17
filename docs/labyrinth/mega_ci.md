# The server smoke test and the in-game self-tests (CI)

Why this exists: the Mega-Dungeon is built from datapack registries (dimension, template pools, processor lists, structure sets), a world generator that places
hundreds of Jigsaw pieces, saved data that must survive a restart, and a dozen systems (rest, encounters, factions, the Guardian, the Crucible) that only
make sense in a running world. Unit tests cannot reach any of that and nobody can play the game while the code is being written, so the CI boots the real mod
in a real headless dedicated server, drives it over the console and fails the build when the game itself says something is wrong. It is the only place the game
runs; everything it does not check is listed at the end.

## How a push triggers it

| Token in the head commit message | Effect |
| --- | --- |
| (none) | `build.yml`: compile, unit tests, jar, GitHub pre-release `v<version>-b<run>`. No server boot. |
| `[smoke]` | the normal build AND `smoke.yml` (boot the server and run the checks) in parallel |
| `[smoke-only]` | `smoke.yml` only: `build.yml` skips its job, so no pre-release is published for a commit that only wants the smoke result |
| `[strict]` (with `[smoke]` or `[smoke-only]`) | a missing pool, structure or command is a FAILURE instead of a skip (use it on the integrated build) |
| `[skip ci]` | GitHub skips every workflow (used for work-in-progress commits so they never publish a release) |

The workflow cannot be started from outside by `workflow_dispatch` or `workflow_run` unless the file is on the default branch, which is why the trigger is the
push itself and the token is read from the commit message. The message reaches the script through an environment variable, never pasted into the script text.

## What `tools/ci/server_smoke.py` does

It starts `gradlew runServer` (NeoForge dev server, `online-mode=false`, RCON on, `max-tick-time=-1` so a heavy generation step is not killed by the watchdog,
small view distance, no mob or animal spawning), waits for `Done`, logs into RCON and runs, in order:

1. the boot reaches "Done" (every registry of the mod initialised, every datapack JSON decoded);
2. no ERROR or exception that names this mod in the boot log (other mods' noise is ignored on purpose: the script fails on what this repo owns);
3. the dimension `nusmp:mega_dungeon` exists and takes blocks;
4. `/locate structure` answers for the old arches (`nusmp:dungeon_gate`) and the singular Gateway (`nusmp:mega_gateway`);
5. the vanilla Jigsaw machinery places each of our pools with its processors (`/place jigsaw nusmp:mega/<pool> nusmp:mega_door 3 x 64 z`);
6. the generator: `/multiverse mega expand` creates sector 0, `status` reaches ACTIVE, `audit 0` is OK; `unseal k` grows sector k+1 up to `--sectors`
   (default 3 in CI), every audit is OK, regions are never closer than the 12 block region gap; `regen 0` reproduces the same plan (determinism);
7. the in-game self-tests below (`/multiverse mega selftest`);
8. with `--restart`: stop, start again, and `status` lists the same sectors in the same states (saved data, resume of an unfinished sector);
9. the final log again has no mod-related error.

It prints one line per check, writes GitHub `::notice` / `::error` annotations (readable without logging in through the check-runs API, which is how the
author reads the result), dumps the tail of the server log when something failed, and uploads the logs as an artifact.
A check that needs a feature that is not in the build yet is SKIPPED and reported as skipped, never silently passed; `--strict` turns every skip into a failure.
`--cmd "python3 tools/ci/mock_server.py"` runs the same script against a stand-in server that speaks the same protocol, which is how the script itself is
tested without Minecraft (`python3 tools/ci/server_smoke.py --cmd "python3 tools/ci/mock_server.py" --no-deps`).

## The self-tests (`labyrinth/mega/SelfTests.java`, `SelfTestCommand.java`)

Why: a unit test needs pure logic with no Minecraft import; the interesting behaviour of the safe zones, the Dungeon Master, the turf wars, the Guardian and
the Crucible lives in code that touches the world. So each system registers a TEST in its registry's `init`:

    SelfTests.register("safe.rest_short", server -> {
        ServerLevel mega = SelfTests.mega(server);
        FakePlayer p = SelfTests.player(mega, "rester", x, y, z);   // a real ServerPlayer with no connection
        ...drive the system through the same public methods its event handlers call, ticking in a loop...
        return SelfTests.Result.ok("rested 3 times, hearth cooldown honoured");   // or throw / Result.fail(..)
    });

`/multiverse mega selftest [key]` (permission 2, usable from the console) runs them on the server thread and prints one line per test in a fixed format
(`selftest <key> OK <detail>` or `selftest <key> FAIL: <detail>`, then `selftest summary ok=<n> fail=<m>`) which the script turns into annotations. The
registry catches every exception, so a broken system reports FAIL with the reason and the other tests still run. Rules for a test: under a few seconds, work
through the system's PUBLIC entry points (never a test-only back door), clean up what it spawned, and do not depend on another test's side effects except
through the saved Mega data. A test proves "this code path ran in the real game and its assertions held"; it does not prove the feel of the game.

## What the smoke test does NOT prove

Rendering, sound, lighting quality, balance (spawn rates, drop numbers, difficulty), what a real player's connection does (a `FakePlayer` has none, so
`changeDimension` travel is checked through the server-side arrival method, not through a live client), the compat with Farmer's Delight, MrCrayfish Furniture
Refurbished and Ice and Fire (none of them is in the CI run, so the processor's "mod absent" path and the optional entity ids are what is exercised; Tensura
Reincarnated, ManasCore, GeckoLib, Architectury, SmartBrainLib and TerraBlender ARE loaded), and a world created before 1.54.0 (the Gateway of the new layout is a structure set, so a chunk that was generated earlier never gets one; the old
arches and `/multiverse labyrinth enter` still lead in).
