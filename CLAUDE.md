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

Build: `gradlew.bat build` (JDK 21) -> `build/libs/multiverse-of-anime-<version>.jar`.
Preview VFX without the game: `python tools/vfx_preview/preview.py`.
