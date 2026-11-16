# Skill icon study: the Tensura house style and why ours looked childish

Sources (read only, extracted to a scratch folder): `tensura.jar` (base mod), `tensura-no-index.jar`, `tensura-reincarnated-nightmares.jar`, `tensura-ascensions.jar`. Everything below was measured on the extracted PNGs and looked at on contact sheets.

## What the reference mods do

| Mod | Canvas | Look | Frames / classes | Animation |
|---|---|---|---|---|
| **Tensura** (base) | 32 x 32, 214 icons in `skill/{unique,extra,common,intrinsic,resistance}` | hand-placed pixel art; a vignetted gradient background (dark edge, lit centre), one shaded symbol, a 1 px dark outline, small sparkle pixels, often the slime mascot | the **frame colour is the class**: Unique = cyan with white corner studs, Extra = gold / orange, Common = silver, Intrinsic = bronze, Resistance = a shield-shaped badge | none (no `.mcmeta` for skill icons) |
| **Tensura: No Index** | 76 x 76 for most (67 Ultimates, 65 Uniques), 32 x 32 for the rest | same recipe at higher resolution: dark background, thin coloured border per skill, centred emblem with a dark outline and soft highlights, a few star pixels | `skill/ultimate`, `skill/unique`, `skill/intrinsic`; the border colour is chosen per skill (accent of the emblem) | none for skills (the `.mcmeta` files are for blocks, items and `blur` flags) |
| **Reincarnated Nightmares** | 32 x 32 (a minority are 500-1300 px raw art, unscaled) | mixed: some pixel art in the Tensura frame, some flat or blurred | class folders as above | none |
| **Ascensions** | 64 x 64 | high-resolution art, either painted portraits in a coloured square or flat symbols in a round badge | class folders | none |

Take-aways: (1) every icon is **one clear emblem on a dark, lit-centre background**; (2) the **class is read from the frame**, not from the picture; (3) shading is **lit from the top left** with a dark outline, a bright rim and a few highlight pixels; (4) nobody uses `.mcmeta` for skill icons: a skill icon is blitted by its path, which only vanilla atlases can animate.

## Why ours looked like a 7-year-old drew them

Looked at the 128 root / Beast Will / Dragon Ball icons (before sheets in `docs/skill_icons/*_before.png`):

* **One identical yellow frame for everything**: no class information, the frame is the loudest thing in the picture.
* **Flat fills, hard outline, no light**: a coloured blob with a black edge; nothing is bevelled, nothing has a highlight or a shadow.
* **No background depth**: a flat or two-tone square instead of a vignette, so the symbol floats.
* **Tiny, generic symbols**: the 54 Beast Will icons were a 10 px blob in a circle (a spiral for three different beasts, a triangle for the Sphinx); the 30 Dragon Ball icons were the same dragon ball with the stars moved. Several skills (Gluttonous Greed, Shishou) were a few pixels of colour.
* **No Ultimate / Unique distinction** besides frame tint, no animation for the 56 Ultimates of those families.

## Our existing icon machinery (what was reused, what was not)

* Skills name their icon as `textures/skill/<id>.png` (`NUSkill`, `DbSkill`, `BeastSkill`); ManasCore draws it with a plain texture blit, so `.mcmeta` animation does not apply (it only works for atlas textures).
* The five Veldanava Ultimates already animate (3.17.0): `client/veldanava/AnimatedIcons` registers an `AnimatedIconTexture` (a dynamic texture that shows one frame of a vertical strip and swaps it on a game tick) **under the skill's own icon path**. That is the technique reused here (`AnimatedIconTexture` and `IconStrip` are used as they are; nothing in `client/veldanava/` was changed). Those five icons are already painted at 128 px and are not touched.
* Older passes: `tools/skills/gen_icons_v2.py` (grimoire placeholders, reviewed fine), `tools/icon_forge.py` / `icon_polish.py` (3.86.0 polish of the root icons, still flat), `textures/skill/classic/` (the 3.86 originals, unused by code).
