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
