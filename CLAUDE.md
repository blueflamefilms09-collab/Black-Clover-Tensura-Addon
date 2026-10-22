# Rules for working on this repo (from the owner)

1. **Always hand over the new file after every change.** When a change is committed and pushed, also send the owner the
   updated build: the mod jar if it can be built in the session, otherwise a zip of the updated source
   (`git archive --format=zip --prefix=nusmp-source/ -o nusmp-source-<what-changed>.zip HEAD -- . ':!dist'`), and say which
   one it is. Do this every time, without being asked.
2. **Never delete or remove anything unless the owner asks for it; add the new, keep the old.** Old models, textures, effects,
   shapes, pages and skills stay registered; new ones are added (enum entries only appended at the end, new pages appended last).
   When the owner explicitly asks to replace something, the old version may be removed (keep save compatibility, e.g. leave old
   data component ids registered so old worlds load).
3. **Do not touch the Time Magic VFX** (`vfx/client/layer/TimeMagicLayer.java`, `textures/particle/time_*.png`,
   `tools/gen_time_vfx_textures.py`, the Time VFX calls in `book/TimeBook.java`). They are the quality standard the other
   effects are built to.
4. **Bump the version on every change: +0.1.0** (e.g. 0.21.0 -> 0.22.0) in `gradle.properties` (`mod_version`) and
   `src/main/resources/META-INF/neoforge.mods.toml` (`version`), and update the jar name in README / LIBRARIES.
5. **Always check whether a request is fully new or a replacement.** Before building anything from a new spec or file, compare
   it with what the mod already has: if it covers something that exists (same feature, same asset, same mechanic), it is a
   replacement, so swap the old one out (rule 2 exception); if nothing like it exists, it is new, so add it next to everything
   else. When handing over the build, list which parts were new and which were replacements.
6. **Every weapon gets Tensura engravings.** Tensura Reincarnated's engravings are its enchantments (the `#tensura:engraving`
   tag: severance, barrier_piercing, holy_weapon, magic_interference, ...). Any new or reworked weapon (sword, spear, bow,
   trident, staff, ...) gets the engravings that fit its lore, listed in `item/WeaponEngravings.java` (applied once to the stack).
7. **The Genesis Demon-Slayer is the visual standard for weapons and items.** New or reworked weapons and items are real 3D
   models in hand (built in `tools/gen_weapon_models.py`, drawn by `client/WeaponRenderer.java`, previewed with
   `python tools/item_preview/preview_weapons.py`), keep their old sprite as the inventory icon, and get glow / void / effects
   that fit them.
8. **Always publish completed updates and fixes.** Commit and push the completed change to the current working branch, then
   publish its build as a GitHub pre-release with the mod JAR attached. Do not push directly to `main` or `master` unless
   the owner explicitly asks; if build or release publication is blocked, report the blocker clearly.

Build: `gradlew.bat build` (JDK 21) -> `build/libs/multiverse-of-anime-<version>.jar`.
Preview VFX without the game: `python tools/vfx_preview/preview.py`.
9. **Always keep the mod multiplayer- and server-friendly.** Everything must work on a dedicated server and with several players at once: all game state and rules are decided on the server
   (client code only draws and asks); never touch client-only classes (`net.minecraft.client`, `com.mojang.blaze3d`, `com.newuniverse.nusmp.client`) from common code except behind a side check or a
   client-only holder class; per-player state is keyed by player UUID, never a static "current player"; no `Minecraft.getInstance()` outside `client/`; settings that change gameplay are SERVER
   configs, settings that only change looks are CLIENT configs; every new packet validates its data and its sender on the server; effects are sent to nearby players only and go through the perf budgets;
   new work is covered by the CI server smoke test (`SelfTests`). `ServerSafetyTest` guards the client-class rule (allow-list `docs/server_safe_allow.txt`).
10. **Wiki research: English wiki first, Japanese wiki as the fallback.** When a spell, character, weapon or other canon detail has to be looked up, try the English wiki first (Black Clover, Fairy Tail, ... fandom pages). If it is blocked or too thin, fall back to the Japanese wiki of the same series (ja fandom / ja.wikipedia) for names, effects and users. Say in the doc or report which source each entry came from, and list what could not be fetched at all.
11. **No lag on a public server, ever.** Every new or changed feature must stay cheap with many players online: server work per tick is bounded (per-player and global caps, cooldowns, budgets; no unbounded entity spawns, no per-tick world scans or chunk loads, no big allocations per tick; scheduled/cleaned up on logout, death, dimension change); network traffic is bounded (few small packets, sent to nearby players only, through the perf budgets); client draw cost is bounded (mesh/quad budgets per effect, distance culling, LOD, no per-frame allocation, effects of far players skipped). Use the existing `perf` package budgets and `PerfProbe`, extend `PerfSourceScanTest` where a new pattern appears, and state the worst-case cost (entities, packets per second, quads per frame) in the doc of every change. A change that cannot be shown to be bounded does not ship.
12. **Building and shipping standard (upgrades rules 1, 4 and 8, which stay).** Before starting any change, tell the owner in plain words what will be built, how it is read and what is not included, then start (the owner has asked for this every time). While building: new work is checked for new vs replacement (rule 5), kept behind a setting that restores the old behaviour, and covered by pure tests plus a self-test; shared files are only appended to. Before every release: run the whole test sweep (all test classes that read the repo's files, the source-scan tests, ServerSafetyTest, ConfigFileNamesTest, the particle/texture/lang tests), fix every real failure, refresh generated files (lang merge, franchise map, grip golden, Curios tags, particle themes), and add the new self-test prefixes to the smoke list. Send the owner a preview of every new visual as soon as its builder finishes. Release only when the version is bumped, the changelog is written and the commit carries `[smoke] [strict]`; after pushing, watch CI, and if it is red read the failed logs, fix, and push again until both the build and the server smoke test are green. Only then report the pre-release link with the jar, the NEW / REPLACED / KEPT lists, the honest caveats (what was not seen in a game), and any decision the owner still has to make. Never say a build is done before the release exists.
