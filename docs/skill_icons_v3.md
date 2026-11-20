# Skill icons v3: pixel-art redesign

Owner request: the skill icons "look like a 4-year-old drew them"; bring them to the quality and layout of the other Tensura mods, and do the same for the animated ones (Tensura No Index has good ones). Measurements behind it: `docs/skill_icons_v3/study.md`.

## What it is

Every skill icon is repainted on the native 32 px grid (the old pipeline painted smooth at 128 / 256 px and box-downsampled, giving 590 to 840 colours of mush per icon; now median 58 to 83, max 124). Technique: hard mask (threshold 0.45), 4 flat tones lit from the top-left, selective 1 px outline in a darkened neighbour colour, separation lines between parts, rim light, banded backgrounds (no gradient), 3 px frames with a 1 px bright ring, rank pips in the Grade colours (`relics/Grade.java`), star sparkles. Ultimates and root Uniques animate with the No Index kind of loop (glint, twinkle, pulse, embers).

Generator: `tools/icon_forge_v3/` (`pxr.py` painter and compositor, `grim.py` magic glyphs, `misc_symbols.py`, `veldanava_v3.py`, `forge.py` driver and sheets, `check.py` pure-Python mirror of the Java invariants). Run `python tools/icon_forge_v3/forge.py` to regenerate, `python tools/icon_forge_v3/check.py` to check. List of rewritten files: `tools/icon_forge_v3/icons_v4.txt` (469 lines).

## NEW / REPLACED / KEPT (rule 5)

REPLACED (same id, same path, same size; old picture kept, see below):
- 121 root icons (37 root + 54 Beast Will + 30 Dragon Ball), `textures/skill/<id>.png`.
- 105 grimoire icons `textures/skill/grimoire/*.png`.
- 75 buff and 75 ultimate icons `textures/skill/icons/*_buff.png`, `*_ultimate.png`.
- 6 symbol icons (5 sin lords, Saiyan Prodigy).
- 76 animated strips `textures/skill/anim/<id>_anim.png` (Ultimates were 8 frames, now 12; frametime 4).
- 5 Veldanava 32 px pictures `veldanava/<id>.png` and `<id>_anim.png` (32 frames, frametime 2 as before; `AnimatedIconsTest` unchanged).

NEW:
- 20 animated strips for the root Uniques (they had none). Index now lists 76 animated.
- `textures/skill_old/` (438 files): the pre-redesign pictures, byte-identical to HEAD.
- `skill_icons_old.json` (list of replaced stills, 382 paths).
- Client option `skillIcons.classic` (below), `skillicons.old` self-test, `SkillIconsForgeTest`.

KEPT:
- Every skill id, file path, size and the game code that reads them. The 4.4.0 `textures/skill/classic/v440` set and the `newStyle` switch work as before.
- The 128 px Veldanava `_hd` strips (Will of the Creator card art) and the `_classic.png` files.
- Time Magic icons and VFX (rule 3): not touched (`time` is excluded in the generator; `SkillIconsForgeTest.timeMagicIsNotTouched`).
- Weapon-skill `ws_*` icons and the legacy icons of removed skills.

## Switching back (rule 2)

Client config `config/nusmp-skillicons-client.toml`:
- `skillIcons.classic = true` shows every replaced icon as it was before this redesign (from `textures/skill_old`), including the old animation of the Ultimates and the old Veldanava pictures. Default false.
- `skillIcons.newStyle = false` still shows the 4.4.0 icons and wins over `classic`.
- `skillIcons.animate = false` shows still frames only.
The swap reuses the existing dynamic-texture mechanism of `SkillIconSwap` (checked once a second, see below). Both options only change looks, so they are CLIENT configs (rule 9).

## Server safety and cost (rules 9 and 11)

- No server logic changed. Server side there is only `SkillIconIndex` (parsing, bounds, path validation by pattern, no `..`, lower case only, duplicates rejected) and the `skillicons.index` / `.bounds` / `.old` self-tests in the CI smoke run, which read jar resources only.
- Client: textures are swapped by `SkillIconSwap` at most once a second, bounded by `MAX_STYLED` (256 lookups) plus `MAX_OLD` (640 lookups) per second; no per-frame allocation, no RenderSystem state of its own.
- Worst case animation: 76 animated icons x (20 ticks / 4 ticks per frame) = 380 texture uploads a second, each one 32x32 RGBA = 4 KiB, about 1.5 MiB/s total (limit asserted at 400 uploads/s in `SkillIconsV3Test` and `skillicons.bounds`). This is the ceiling, assuming every animated icon is live at once; it was not profiled in a game.
- 5 Veldanava medallions: 32 frames at 2 ticks, same mechanism as before.
- Download size: 76 strips of 32x384 about 49 KB each (about 1 MB on disk compressed), plus `skill_old` about 2.7 MB in the jar.
- No packets, no entities, no ticks on the server.

## Tests

Updated only to keep their invariants true: `SkillIconsV3Test` (animated set is Ultimates plus root Uniques, 12 frames, MAX_FRAMES 16, index frames 12, 17 frames rejected). Added: `SkillIconsForgeTest` (every rewritten path keeps path and size, has an old copy, at most 170 colours, Time Magic untouched, the classic list matches the forge list, bad old lists parse to empty, the client option and swap exist). `SkillIconsV2Test`, `SkillAssetsTest`, `AnimatedIconsTest` unchanged and still true by the Python mirror.

Verified here: `python tools/icon_forge_v3/check.py` prints OK (121 manifest icons, 387 forge stills, colours median 68 / max 124); `SkillIconIndex` compiled with javac and run against the shipped resources (121 styled, 76 animated, 12 frames, 382 old paths, all live and old files present, bad inputs rejected).

## Previews

`docs/skill_icons_v3/`: `<set>_before.png` / `<set>_after.png` for root, beast, dragonball, extras, grimoire, buff, ultimate; `animated_frames_1..8.png` (all frames of the strips).

## Hand-made versus procedural (honest count)

- Procedural, from the old glyph masks, re-rendered by the new painter: all 255 magic icons (105 grimoire, 75 buff, 75 ultimate).
- Re-rendered from the existing hand-built symbol lists (same geometry, new painter): all 121 root icons. Their silhouettes are therefore not new designs.
- Newly drawn by hand for 32 px: 6 symbol icons (5 sin lords, Saiyan Prodigy) and 5 Veldanava emblems (Hercules fist in laurel, Israfil trumpet, Jibril scales and wings, Sandalphon sword and wing guards, Solomon hexagram).
- All animation is procedural.

## Still weak

- Dragon Ball busts still share one bust template.
- Beast Will Unique and Ultimate pairs differ mainly in frame, ring and animation.
- Magic `stone` is a plain slab.
- Veldanava 32 px versions are simplified redraws, not the ornate 128 px art.
- Contrast and saturation are still below Tensura No Index.
- Rank mods (`mod_*` badges) were not touched.

## Not verified

- Nothing was seen in a running game: no screenshot of the skill list, the card screen or the tooltip at GUI scale 2 to 4.
- The Java edits (`SkillIconIndex`, `SkillIconOptions`, `SkillIconSwap`, `SkillIconSelfTests`, the two test classes) were not built with Gradle (offline); only `SkillIconIndex` was compiled alone. CI is the compiler for the rest.
- `skillIcons.classic` was not exercised on a client.

## Rules not applicable or not done

- Rule 14 (Blender): these are 2D icons, no 3D model was made.
- Rule 10 (wiki research): no canon lookup was needed; glyph meanings come from the existing icon code.
- Rules 4 and 8: no version bump, no changelog, no push or release (the brief for this session said not to).
- Reference mods were used for style only; none of their pixels or file names are in the repo.
