# Libraries and how to rebuild

Nothing below is bundled (together they are several hundred MB). Gradle downloads every one of them automatically the first
time you build, from the repositories named in `source/build.gradle` and `source/settings.gradle`.

## Needed to BUILD (`source/`)
- JDK 21 (Eclipse Temurin 21)
- Gradle 9.2.1: the wrapper in `source/gradle/wrapper` downloads it for you
- NeoForge 21.1.234 (Minecraft 1.21.1) via the NeoGradle userdev plugin 7.1.38
- Compile-time mods (CurseMaven, `curse.maven:<slug>-<project>:<file>`):
  - ManasCore: `manascore-619025:8022425` (its skill / storage / network modules are unpacked from the jar by the build)
  - Architectury API: `architectury-api-419699:5786327`
  - GeckoLib: `geckolib-388172:8350073`
  - SmartBrainLib: `smartbrainlib-661293:7055149`
  - Tensura: Reincarnated: `tensura-reincarnated-643695:8098859` (2.0.1.0; runs on 2.0.1.3)
- Tests: JUnit Jupiter 5.10.2 (Maven Central)
- Texture regeneration only (optional): Python 3 with Pillow and numpy -> `python tools/gen_grimoire_book_textures.py`

Build:  `gradlew.bat build`  (Mac/Linux `./gradlew build`)  ->  `build/libs/multiverse-of-anime-0.84.0.jar`

Magic rune textures: Node.js (no external packages) -> `node tools/gen_magic_runes.mjs`; optional high-detail ring and band textures use Pillow -> `python tools/gen_magic_runes.py src/main/resources/assets/nusmp/textures/particle/magic_runes`

## Needed to RUN the mod (put the jar from `mod/` in the game's `mods` folder)
NeoForge 21.1.x for Minecraft 1.21.1 plus: Tensura: Reincarnated 2.0.1.0+, ManasCore 4.0.0.2+, Architectury, GeckoLib,
SmartBrainLib, and TerraBlender 4.1.0.0+ (a Tensura dependency).

## Dev client note
`gradlew runClient` also needs a TerraBlender NeoForge 1.21.1 jar in `source/run/client/mods/` (the project does not declare it).
