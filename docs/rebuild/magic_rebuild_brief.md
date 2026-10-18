# Magic rebuild brief (1.93.0 and later): the shared rules for every builder of the rebuild

Owner request: "rebuild the magics (list) ... remake the listed magic based on the Black Clover anime: custom summon models, models, render layer temp armor (it should use the same equip animation the
Djinn full equip has), VFX effects, and mechanics." The merge generator ("custom magic") was removed in 1.93.0; two attributes now stay two attributes.

## What "rebuild" means for ONE magic (all five parts, in this order of priority if time is short: armour, summon model, VFX, mechanics, docs)
1. **Canon first.** The Black Clover anime / wiki decides the spells' names, behaviour and look. Use WebSearch / WebFetch for the Black Clover wiki (blackclover.fandom.com, page names like "Soul Magic") if the
   network allows; the repo also has `.github/wiki/*.txt` for some magics. If you cannot reach the wiki, say so in your doc and work from what you know. Where the anime has a named user (a captain, a
   character), say so in the doc. Where a spell cannot be reproduced 1:1, build the nearest honest version and say so.
2. **Temporary armour (render layer).** Rebuild the magic's `client/aura/<Pascal>Aura.java` painter (the 5 second armour after a cast; read `client/aura/AuraPainter`, `AuraContext`, `AuraRender`, an existing
   good one) so that it is a real SET OF PIECES on the player's model (helm / crown, chest, pauldrons, vambraces, gauntlets, greaves, plus magic-specific pieces: wings, mantle, tail, a halo, a piece behind
   the back ...), each piece a geo model of the magic's own (see `docs/geo_models.md`, `tools/geo_builder.py`, `tools/geo_check.py`) with an emissive `_glow` pass, and that it FORGES IN AND SHATTERS OUT
   EXACTLY LIKE THE DJINN FULL EQUIP: use `client/aura/EquipAnimation` (written for this job from `client/relics/djinn/DjinnArmorLayer`: pieces anchored on the model part they were built for, staggered by
   a per-piece delay of 0 to 12 ticks, flying in from their own side while fading up and growing 85 to 100 percent over 8 ticks, and shattering out over the last 18 ticks). READ `DjinnArmorLayer.part(..)` and
   `EquipAnimation` first. Explicit render layers on every pass (`AuraRender.cutout / translucent / additive / emissive`; solid overlays >= 2 percent larger than the skin under them, drawn first; translucent
   next; additive last). Never put anything in an inventory or armour slot. The temporary armour must also look different per magic (silhouette, material, glow colour) and fit slim and wide arms.
3. **Summon models.** Whatever the magic summons or constructs (a creature, a golem, a familiar, a weapon, a construct, a projectile body) gets a real custom 3D model: geo model + animation + glow texture,
   drawn through the existing prop system (`client/prop/<Pascal>PropPainter`, `prop/<Pascal>Props`, `MagicProps`) or the way the Painting / Mirror / Cotton / World Tree magics already do it. Do NOT register
   new entity types unless there is no other way (then append one line to the registry, say so in the report). Animated where the thing moves (idle, attack, vanish).
4. **VFX.** Upgrade the magic's effect layers (`vfx/client/layer/<Layer>.java`, its `VfxShape` `<NAME>_FX1..3` if any, its particle textures via a `tools/gen_<key>_*.py`) toward the quality of the Time Magic VFX
   (read `TimeMagicLayer.java` as the bar; **never edit it**, its textures, its generator, or the Time calls in TimeBook). Preview without the game: `python3 -B tools/vfx_preview/preview.py` (scenes.d/<key>.json)
   and `tools/render_preview` (section 9 of the authoring guide).
5. **Mechanics.** The spells match the canon behaviour, scale with the caster's EP (`BalanceLaw.epScale`, `EnergyBridge`), cost magicules or aura like the other books, use the status helpers (`EnergyBridge`,
   `Nullification`, `GrimoireBook.control`) and the safe wrappers (`SafeCast`). KEEP every existing page id, its order and its mastery bit (saves and mastery depend on them): existing pages may be
   re-implemented, new pages are APPENDED. Bosses get shortened control and capped damage (`BalanceLaw.isBoss`).
6. **Docs.** `docs/attributes/<key>_rebuild.md` per magic with a NEW / REPLACED / KEPT table, the canon sources used, what the armour pieces and summon models are, the spells and their numbers, and an honest list
   of what could not be verified (nothing can be run in a game; the preview tools are re-implementations).

## Read first
`CLAUDE.md` (the owner's rules), `docs/attribute_authoring_guide.md` (the pipeline of the 0.54 expansion: sections 0 to 10 apply: where files live, the status helpers, the render-layer engine, GeoLite, the three
local verification tools), `docs/magic_framework.md`, `docs/geo_models.md`, `docs/cast_animation_guide.md`, `docs/mode_armor_spec.md`, `client/relics/djinn/DjinnArmorLayer.java`, `client/aura/*`.

## Rules that are always in force
* Add the new, keep the old. Rebuilding replaces YOUR magics' old armour / effects / spell code (the owner asked for the remake), but never a page id, an enum constant, a registered id, a texture another system uses.
* Do NOT edit `MagicType`, `NUSkills`, `VfxShape` ordering (append only), the Time Magic VFX, or another magic's files. You may add NEW files whose names start with your magic's Pascal / key name. Shared
  registries (`AuraRegistry`, `VfxManager`, `MagicProps`, `VfxShape`, `Aura`): append-only one-liners, listed in your report. No version bump, no CHANGELOG / README / LIBRARIES / gradle.properties /
  neoforge.mods.toml / `en_us.json` edit: lang goes in a flat fragment `tools/lang/<key>_rebuild.json` and is read with `translatableWithFallback` where text is built in code.
* Commit in YOUR worktree (never push, no PR) with the trailers `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>` and `Claude-Session: https://claude.ai/code/session_0174t393fJ2EgwV92FW9SroE`.
* Checking (nothing can be run in a game; Gradle is not available offline): type-check with `WT=$PWD KEY=<k> bash /tmp/claude-0/-home-user-Black-Clover-Tensura-Addon/2c8ee7c0-aec7-5d43-b651-54cec528bb71/scratchpad/tc_wt.sh '<regex of your paths>' <absolute java paths>`
  (stub artefacts "X is abstract; cannot be instantiated" and "does not override deserializeNBT" are known; anything else in your files is real; do one deliberate wrong call as a negative control) and JUnit with
  `WT=$PWD KEY=<k> bash /tmp/claude-0/-home-user-Black-Clover-Tensura-Addon/2c8ee7c0-aec7-5d43-b651-54cec528bb71/scratchpad/msf_jt.sh <fully.qualified.TestClass>` (the shim lacks assertArrayEquals(int[]); tests that need
  Minecraft classes cannot run in it, keep decisions in pure classes and test those). Existing tests must keep passing (ScreenBlurGuardTest, the asset tests, MagicEnumAuditTest ...). Run python as `python3 -B`.
* Every geo model: box UV, pivots, emissive glow map, checked with `tools/geo_check.py`. Textures are generated by scripts (so they can be redone), at the resolution of the existing assets or better.
* No model or assistant names in code, comments, docs or lang text.
* Honest report at the end: what is built per magic, exact edits to existing files (keep them additions), lang fragment paths, new self-tests if any (register with
  `com.newuniverse.nusmp.labyrinth.mega.SelfTests.register("magic.<key>_rebuild", ...)`; the CI server smoke test runs them), and what could NOT be verified (everything visual).
