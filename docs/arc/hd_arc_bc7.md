# HD arc skins, group bc7: Rhya, Mars, Lucifero, Udaeus, Vetto, Zagred, Zagred (legacy), the Dark Disciples, the Devil Avatars

Part of `docs/arc/hd_arc.md` (the HD pipeline: 256x256 skins, attached 3D hair, the second layer a cutout inside the 64x64 blueprint coverage, glow maps premultiplied and as big as the skin).
Painters: `tools/arc_hd/chars/<look>.py` (23 looks), the kit copy `tools/arc_hd/chars/_bc7.py` (a private copy of `_bc6.py` plus the crystal, crack, facet, flow-mark and plate helpers). Previews:
`docs/previews/arc_hd/bc7_front_back.png` (front and back of all 23) and `docs/previews/arc_hd/bc7_close.png` (a bust of all 23).

## New or replacement (rule 5)

* REPLACED: the generic HD bridge output of these 23 looks (the baseline made by `arc_hdlib.bridge`) is replaced by hand-painted skins and glow maps (`textures/entity/playermodel/<look>.png` and `_glow.png`)
  and their fragments (`tools/arc_hd/looks/<look>.json`). The 64x64 sheets are KEPT: `gen_arc_hd.py --size 64` still reproduces them and the 64 mode of the setting is unchanged (rule 2).
* NEW: the 23 painter modules, `_bc7.py`, this document, the two preview sheets. Nothing was deleted; no hair file, no shared tool and no test was touched.
* Not changed: the extras (wings, horns, weapons, the grimoire, the cloaks), the height scales, slim or wide arms, the hair styles of `hair_map.py`, the glow semantics.

## What every look follows (the owner's style)

A strong silhouette from the attached hair, hard colour blocking (skin, main cloth, dark trim, light metal or bone, each its own value), bulky pauldrons and bracers, cuffs and collars, boots with
cuffs, shaded anime eyes (lid shadow, iris gradient, highlight, glow where the sheet glows), cloth with folds, lit edges and piping, and crisp details on BOTH layers (the second layer and the hair are
dropped beyond 24 blocks). Lines are never thinner than 0.2 of a model pixel. The 3D extras stay free: nothing is painted where a wing, a horn, a weapon or a glyph box attaches, or it is painted as a plain
backing that the extra covers. The second layer is clipped to the blueprint coverage (`K.finish`, and `extra_covers` for the head mask of Udaeus and the Disciples).

Sources (rule 10): the English wiki (blackclover.fandom.com, danmachi.fandom.com) and the Japanese pages (ja.fandom, ja.wikipedia) all answered "CONNECT tunnel failed, 403" from this session, so NOTHING could be
fetched. Every look is "from memory of the series" plus the repo's own docs (`docs/arc/rhya.md`, `mars.md`, `lucifero.md`, `vetto.md`, `zagred.md`, `udaeus.md`, which cite the wiki pages) and the 64x64 sheet sources
(`tools/arc/char_*.py`). The wiki pages that could not be fetched: Rhya, Mars, Lucifero, Vetto, Zagred (blackclover.fandom.com), Udaeus (danmachi.fandom.com), and the Japanese pages of all of them.
Where the series gives no outfit (Rhya's coat, Mars's armor colours, the Disciples, the Devil Avatar, the legacy Zagred) the look is the mod's own, as the repo docs say.

Rounds: every look went through at least four look rounds with `preview_arc.py` (front, back, close) against the owner's references and the old 64 look: (1) the first paint, (2) colour blocking (Lucifero's dark
tank, leggings and plates with a lit violet stripe; Mars's saturated crystal; Rhya's deeper cream coat; Udaeus's lit ribs and stone; Zagred's armband and darker ashen skin), (3) a 6x crop of every head and torso
to check the faces, eyes, marks and trims on the base layer, (4) the whole set front, back and close, checked for floating hair and for anything painted over an extra.

## Rhya (`rhya`, `rhya_form2`)

* Design: the light hooded robe of the Eye of the Midnight Sun, cream coat (`#cfc9b4`, lit `#efe9d6`, shade `#8c8672`) with dark piping and a stand collar over a dark shirt, long sleeves with stitched dark cuffs,
  a double belt with a pale buckle, dark trousers, calf boots with a cuff and strap, slim arms. Half-lidded black eyes with a violet sheen, a one-sided smirk, the red tattoo on his left cheek.
* Hair: `anime_short` (a messy black cut that keeps the eyes free), black `#1c1c28`; form 2 `anime_spiky` white `#eceef8` (clumps, not fingers). The fringe is drawn to match; no floating strands.
* Form 2 (the elf awake): gold glowing eyes, the tattoo reaching over the eye and down the jaw, gold cheek lines, the third eye open on the forehead at y 3.0 to 3.4 (below the fringe), gold piping.
* Extras kept free: the hood and the cloak behind the neck, the grimoire. Source: from memory plus `docs/arc/rhya.md`.

## Mars (`mars`, `mars_form2`)

* Design: dark navy cloth with lavender trim, a chest plate frame under the stone box, pauldrons, bracers and greaves of faceted crystal (violet `#8c74e0`, light `#d4c6ff`, dark `#3e2c92`), a big faceted diamond on
  the back, forehead gem and cracks, gloves with knuckles, boots with a cuff.
* Hair: `anime_spiky` pale `#dcd8ee`; form 2 `anime_spike_tall` warm white `#fff0d8`. Thick layered clumps.
* Form 2 (the Gates, the fire of Fana): the crystal turns amber (`#f0b45c`), the stones and cracks burn orange, the eyes are amber, the hair warm.
* Extras kept free: the chest stone box, the forearm stone, the crystal sword and the orbiting crystal. Source: from memory plus `docs/arc/mars.md`.

## Lucifero (`lucifero`, `lucifero_form2`)

* Design: the pale ash-grey King of Devils with white hair, a pale third eye on the brow, black tribal patterns on the face and arms; a black-violet tank (`#322e42`) with lit violet rune strokes, leggings with a
  glowing violet stripe, dark bone pauldrons and bracers with a glowing trim, a dark sash with a lit buckle, bare feet. Colour blocking: pale skin, black cloth, violet light.
* Hair: `anime_long_open` white `#ece8f6`. This DEVIATES from `hair_map.py` (`anime_long`): the closed long style would cover the third eye; the open style leaves it free. Its back is a flat slab (see the hair
  problems below).
* Form 2 (full power): a brighter white third eye, white eyes, face cracks, bone thorns on the shoulders and elbows (glowing tips).
* Extras kept free: the two wing pairs and the horns (the back of the body is plain tank). Source: from memory plus `docs/arc/lucifero.md`.

## Udaeus (`udaeus`, `udaeus_form2`)

* Design: the DanMachi skeleton giant. A bald skull with a brow ridge, deep sockets with a lit pupil each (blue, form 2 violet), a nose hole, cheekbones, a tooth row, sutures and hairline cracks (the mask over the
  lower face is the second layer, painted the same); a tattered dark cloak torso with a dark cavity, lit ribs and a violet magic stone behind the breastbone, collar bones; a belt with a bone buckle, a pelvis with a
  loincloth; arm bones with elbow knobs, two forearm bones and finger bones with joints; bone pauldrons with a violet rim; kneecaps, shin greaves with a violet rim, foot bones.
* Hair: none (bald, as `hair_map.py`).
* Form 2 (the leveled giant): grey ash-bone, a grey cloak, violet eye lights, violet cracks glowing through the bone and the cloak.
* Extras kept free: the black sword, the violet halo (form 2); the painted ribs and spine are a backing under the 3D rib cage and spine. Source: from memory plus `docs/arc/udaeus.md` (danmachi.fandom.com not fetched).

## Vetto (`vetto`, `vetto_form2`)

* Design: the wild Spade Kingdom brute. A red war-paint band over the eyes, glowing red eyes, a beard and teeth, a fang necklace, a bare chest with pecs, abs and scars, a brown vest, a red sash with a gold buckle,
  wrapped forearms, a left-shoulder fur pelt, baggy dark trousers, boots.
* Hair: `anime_mane` gold `#dcb44c` (a thick mane that falls behind the shoulders); form 2 white `#f2f0ea`.
* Form 2 (the Mythical Beast): white hair and beard, burning red eyes and paint, glowing red beast marks on the chest and arms.
* Extras kept free: the claws, the horns, the tail. Source: from memory plus `docs/arc/vetto.md`.

## Zagred (`zagred`, `zagred_form2`)

* Design: the Word Soul host. A long black coat (`#1c1626`) with violet trim, lit lapel panels, a stitched rune pattern and a high collar with a lit rim, left open over a bare ashen chest; bare ashen arms (grey
  `#868898`) with a violet armband, black gloves with a lit cuff; a double belt with a glowing buckle; the coat skirt to the ankles with a glowing violet hem; knee boots with a violet cuff; a big glowing ringed rune
  emblem on the back. Narrow angry red eyes, a smirk, the rune on the brow (the lower half shows below the fringe).
* Hair: `anime_spike_tall` black `#16121e` (tall dark spikes in clumps, as `hair_map.py`).
* Form 2 (the Word Soul devil): grey skin, slit red eyes with fangs and face cracks, a crimson trim, red glowing runes and cracks.
* Extras kept free: the chest and arm glyph boxes (the chest of the skin is plain), the grimoire, horns, wings, tail, trident; the wings sit lower and wider than the emblem. Source: from memory plus `docs/arc/zagred.md`.

## Zagred, legacy (`zagred_legacy`, `zagred_legacy_form2`)

* Design: the first Zagred boss, a lanky black-violet devil: a red rune on the brow, slit red eyes with fangs, red cheek cracks and veins, a bare chest with a lit bone ribcage over a dark cavity (red cracks between the
  ribs), a red glowing necklace, a dark sash with a glowing red buckle, black trousers with a bold glowing red stripe, bone shoulder plates with two thorns and bone bracers with a red edge, black hands with pale
  claws, bare clawed feet. The back keeps the spine and the shoulder blades.
* Hair: `spiky_long` black `#14101c` (the classic style kept, as `hair_map.py`).
* Form 2 (phase 4): redder and hotter: more cracks, white-hot cores, lit knuckles.
* Extras kept free: the curved horns, the bat wings, the whip tail, the trident (phase 4). Source: from memory plus `docs/arc/zagred.md` and `tools/arc/char_zagred_legacy.py`.

## Dark Disciple (`dark_disciple`, `_astaroth`, `_beelzebub`, `_lucifero`, `_megicula`)

* Design: one painter, the devil read from the id (the tint is the cloth, trim, runes and glow). A hooded acolyte: an ashen face with a rune on the brow under a bone skull mask over the lower face (cracked plate,
  brow ridge, two almond eye holes with a lit eye, a nose slit, a glowing rune line on each cheek, a tooth row, dark straps), a ragged long robe with a dark inner panel and trim piping, lit runes, a high collar, a ragged
  hem with a trim line, a big lit skull emblem on the back, wide sleeves with a trim cuff, bone wraps on the hands, dark trousers, ankle boots with a trim cuff, a dark sash with a trim buckle.
* Tints: grey `#5a566a`, Lucifero violet `#4a3a80`, Megicula crimson `#7a2c3c`, Beelzebub indigo `#34408a`, Astaroth gold `#806a30`.
* Hair: `anime_hood`, the hood in the cloth colour.
* Extras kept free: the hood and the hooded cloak, the ritual dagger, the floating orb. Source: the mod's own mob (`tools/arc/char_dark_disciple.py`); the devils' colours from memory.

## Devil Avatar (`devil_avatar`, `_form2`, `_megicula`, `_megicula_form2`)

* Design: the Underworld boss. A gaunt violet (or crimson) devil, bald, narrow slit eyes with a lit core, fangs, veins at the temples, cracks on the cheeks; a ragged plate-coloured tabard with a bare V at the neck, a
  wide lit collar, a lit spade emblem in a ring, runes and trim piping, a dark belt with a glowing gem, a ragged hem with a lit line; bone pauldrons and bracers with a lit rim, black gauntlets with a lit rim (the claws
  are the model's), trousers with runes, knee boots with a trim cuff.
* Hair: none (bald, as `hair_map.py`); the top of the head is plain skin with a few cracks (the horns stand on it).
* Form 2 (the Gates open): a forehead sigil, lit cheek lines, bone thorns on the shoulders and elbows, more cracks.
* Extras kept free: the horns (a second pair in the Gates), the claws, the tattered cloak, the tail, the halo, the sigil tablets.

## Cost (rule 11), multiplayer (rule 9)

* Server: nothing. The painters run in the build tools (no game code); the server only holds the look ids. No new packet, no new entity, no tick work, no config.
* Network: none.
* Client: 23 skins of 256x256 (256 KiB each as RGBA, 44 of the 46 files with a glow map: about 11 MiB of texture memory if all were loaded at once; only the looks in view are loaded, and the game's `PlayerLod`
  drops the second layer and the hair beyond 24 blocks). The model quads per frame are unchanged (the same player model and extras as before); the 64 mode of the setting still serves the old sheets.
* Determinism: the painters use `H.hsh` only (no random, no clock); the same input gives the same PNG.

## Weaker than the references, and hair problems (honest)

* Weaker than the owner's references: the renderer is flat-shaded, so cloth folds and the fine shading read less than in the owner's images; with the fringe hiding the top 3 to 4 face rows of the `anime_*` styles,
  Rhya, Mars, Lucifero, Vetto and Zagred have about 5 visible face rows for detail; the grey base Dark Disciple (grey hood on a grey robe) has the least colour blocking; Zagred's bare arms are plain ash-grey blocks by
  the sheet's design (only an armband and veins break them); the back views of Lucifero, Zagred and the legacy Zagred are mostly hidden by hair and wings, so their back detail is mostly not seen.
* Hair problems: Lucifero's `anime_long_open` falls behind as a flat grey slab (no clumps) in the back view; Zagred's and the legacy Zagred's tall dark spikes are wide and read as a fan of thick clumps from the front;
  Lucifero uses `anime_long_open` instead of the table's `anime_long` (the third eye would be covered).
* Not seen in a game: everything here is judged from the software preview only; the lit glow strengths, the extras' fit to the painted backing (the Udaeus ribs, the Mars chest stone, the Zagred glyph boxes) and the in-game
  light are unverified.
