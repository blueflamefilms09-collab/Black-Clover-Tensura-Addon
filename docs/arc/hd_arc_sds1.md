# HD arc, group sds1: Meliodas, King, Diane, Ban (and their second forms)

Eight looks hand-painted at 256x256 (every model pixel is a 4x4 texel block), each with the glow map the look already had. The painters are
`tools/arc_hd/chars/{meliodas,king,diane,ban}.py` plus the thin `*_form2.py` loaders, with the shared helpers in `tools/arc_hd/chars/_sds1_kit.py`
(the underscore keeps the kit out of the generator and out of the module test). Output: `src/main/resources/assets/nusmp/textures/entity/playermodel/<id>.png`
(+ `_glow.png`), and `tools/arc_hd/looks/<id>.json` for each id. `looks.json` is merged by the lead (`merge_looks.py`), not here.

Previews: `docs/previews/arc_hd/sds1_front_back.png` (front and back of all eight) and `docs/previews/arc_hd/sds1_close.png` (bust of all eight).

## What is the same for all four

- **64x64 stays** (rule 2): the 64x64 sheets and the baseline bridge are untouched; HD is an added mode. Each painter starts from the 64x64 blueprint
  (`A.blueprint`, `A.bridge`), so the colour blocking and the layout of the sheet carry over, then redraws by hand what has to be crisp.
- **Both layers.** The game drops the second layer and the hair beyond 24 blocks (`PlayerLod`), so every detail (face, trim, buckles, seams) is on the base
  layer too, and the second layer only holds cloth where the sheet has cloth (`all_covers` / `hold_cover`: the overlay is clipped to the blueprint coverage).
- **Extras are not painted over.** Wings, weapons, the cape and the horns are separate models of the look (`extras`), so the skin only carries the yoke of
  the mantle (King's back) and leaves the weapon grips alone. Slim/wide arms, height `scale`, `hold_r` and the glow semantics are kept as the old look had them.
- **Hair is the kit's attached 3D hair** (single colour `hair_color`); the skin only paints the scalp and the visible fringe, as thick layered clumps (`K.lock`,
  `K.spike`, `K.fringe_mass`), never fingers, and never a floating strand: every lock starts on the forehead mass.
- **Colours are mixed with `K.lit` / `K.cloth`**, not `H.tone` above 1.0 (that whitens about 45% at 1.3 and turned the dark trousers and the jacket grey).
- Deterministic code (numpy + Pillow only): fixed seeds, no `hash()`, the same bytes on every run.

## Sources (rule 10)

The wiki pages could not be fetched in this environment (the fandom host was not resolvable, English and Japanese alike), so every entry below is
**from memory of the series plus the earlier research in `docs/arc/<id>.md`**, which cites the wikis. Nothing is traced. Entries marked "added in the HD pass"
are details that the 64x64 sheet did not have.

## Meliodas, the Dragon's Sin of Wrath (`meliodas`, `meliodas_form2`)

- **Design.** Golden-blond boy with emerald eyes, thick blond brows, a wide grin with one fang. Dark vest with a brown yoke and a stand collar over a cream
  shirt (buttons, placket, stitching), short cream sleeves with stitched brown cuffs, the wrapped right hand, a double belt with a gold buckle, dark
  trousers with creases, calf boots with straps, welt and polished toe. The sword (Lostvayne) is the `pmx_meliodas` extra and is not painted.
- **Colours.** Hair #f6d044 (the sheet's #e8c860 read khaki after the hair tint), vest #322a29, yoke #94713c, piping #e0c27a, shirt #e4dac2, trousers
  #2e2a36, boots #43332c, gold #e0b43e, mark #5c1519 / #9a2e2a.
- **Hair: `anime_spike_tall`.** The pilot's `anime_spike_swept` read as thin fingers; the tall spikes give thick layered clumps like owner reference 154.
- **Form 2 (Demon King).** Same code with `FORM2 = True`: vest and trousers #18141a, yoke #7a1432, piping #d8486a, black lock at the left of the fringe
  (x 2.9, length 2.1, shortened after round 2 when it read as a bar), slit red glowing eyes and angry brows, the demon sigil on the forehead (4.7, 2.75),
  fangs, cracks on the face, glowing cracks and veins on the left arm. Glow map: eyes, sigil, cracks (none for the base form).
- **Added in the HD pass:** the Dragon's Sin mark on the left forearm (the sources disagree on the exact place, here below the sleeve so it is seen); the sigil
  and cracks of form 2.
- **Source:** from memory + `docs/arc/meliodas.md`.

## King, the Grizzly's Sin of Sloth (`king`, `king_form2`)

- **Design.** Sleepy boyish fairy: half-lidded amber eyes with bags under them (base form), light brows set low, a parted brown mop with two long side locks,
  a gold circlet with beads and the pink flower on all four sides of the head overlay. Thigh-length cream tunic with a green stand collar, gold piping and
  stitching, the Sacred Tree on the chest (leaves and roots, glows), green tabard borders, green sash with a knot and a gold buckle, short sleeves with green
  cuffs, bare legs, small brown shoes. The back carries the yoke of the pale-green mantle; the cape, the wings and the spear are extras.
- **Colours.** Hair #c4702e, tunic #eae2aa, trim #4fa04a / #244226, gold #e8d070, sash #4fa04a, mantle #9ad07a, glow #b8ff80, eyes #6e3408 / #dc8a28 / #ffd98a.
- **Hair: `anime_short`**, a short mop that matches the sheet's silhouette (the owner references show a short messy cut); locks are painted in the same hue.
- **Form 2 (Fairy King Harlequin).** White-gold tunic #fff4c0, gold trim #e8d070, hair #f0a040, gold glowing eyes #ffd040 (first tried near white: it read as blank rings,
  so the glow colour is saturated), leaf glow #d8ffa0. Glow map: eyes and the tree emblem (the base form also has a glow map for the tree).
- **Added in the HD pass:** circlet beads, the tabard borders, the sash knot.
- **Decision for the owner:** the palette follows the 64x64 sheet (cream, green, gold). Owner reference 156 shows navy, teal and gold. Changing it is a palette edit
  in `BASE` / `FORM2` of `king.py`; it was not done without a decision (rule 2/5).
- **Source:** from memory + `docs/arc/king.md`.

## Diane, the Serpent's Sin of Envy (`diane`, `diane_form2`)

- **Design.** Cheerful young giant: big shaded purple eyes with lashes, blush, open smile, layered bangs and side locks. Orange sleeveless one-piece with princess
  seams, a scoop neckline with cream-peach trim, a brown belt with a gold buckle, a zipper on the back; studded grey gauntlets (plates, rivets, gold rings) on rows 6 to
  12 of both arms; very short orange shorts with a gold hem; knee boots with straps, buckles, a gold cuff, welt and sole. Hammer (Gideon) is the `pmx_diane` extra.
- **Colours.** Hair #8a5434, suit #e8782a, trim #f8c070, belt/boots #6a4426, gold #f8c070, metal #8a8a98, eyes #3c1670 / #9a58d8 / #e6c8ff.
- **Hair: `twin_tails`** (the kit's classic), matching her canon twin tails.
- **Form 2 (Drole's Dance).** Sunrise suit #f09a3a with pale-gold trim, a glowing sun rune on the chest, scaled gold pauldrons, bracers and greaves with glowing
  gems (scallops), gold glowing eyes #ffc040, light lines, hair #c47a3a. Glow map: eyes, rune, gems (none for the base form).
- **Added in the HD pass:** the back zipper, the sun rune and the crystal plates of form 2.
- **Source:** from memory + `docs/arc/diane.md`.

## Ban, the Fox's Sin of Greed (`ban`, `ban_form2`)

- **Design.** Tall, lean, muscled man with silver spiky hair and scarlet slit eyes: thick pointed bangs, sharp tilted brows (tilt 0.34), a one-sided smirk with a fang,
  the scar on the left cheek with stitch marks, stubble (a jaw shade plus 16 short strokes). Open dark-red jacket over a bare chest (pecs, collar bones, abs, obliques),
  a dark fur collar, lapels and folds, a double belt with pouches and a steel buckle, fingerless gloves, sleeves with elbow creases and rolled cuffs, black trousers, boots with a
  lighter cuff and straps. His staff (the `pmx_ban` extra) is not painted.
- **Colours.** Hair #d6dae6, jacket #5a1c24 / #2a1014, gloves and boots #241c1a, belt #3c281c, steel #9aa0aa, trousers #1e1c22, eyes #5a0810 / #c83a3a / #ff9a8a.
- **Hair: `anime_spiky`.** Pale spikes in thick clumps like the owner references; its tall boxy spikes are the weakest part (see caveats).
- **Form 2 (the last life).** Jacket #42161d, torn at the hem into a zigzag (`ragged_hem`, fixed seeds per face), sleeves cut off at the forearm with a torn overlay edge,
  white hair #eef2f8, eyes and cheek and brow lines glow Fountain green #7be0a4. Glow map: eyes and face lines (none for the base form).
- **Added in the HD pass:** the Fox's Sin mark low on the left of the stomach (`docs/arc/ban.md` lists it as canon), the trouser waist under the belt (the sheet left bare skin).
- **Source:** from memory + `docs/arc/ban.md`.

## NEW / REPLACED / KEPT (rule 5)

- **NEW files:** `chars/ban.py`, `chars/ban_form2.py`, `chars/meliodas_form2.py` (before this the form 2 of Meliodas and both Ban looks were baseline bridges).
  Look fragments for `ban`, `ban_form2`, `meliodas_form2` now say source "hand".
- **REPLACED (reworked in place):** `chars/meliodas.py` (hair style, face split into human and demon, trouser/boot colours), `chars/king.py`, `chars/diane.py`,
  `chars/_sds1_kit.py` (these existed from the wip commit 530d1b5; the textures of `king`, `king_form2`, `diane`, `diane_form2`, `meliodas`, `meliodas_form2` are regenerated).
  Meliodas hair changed `anime_spike_swept` -> `anime_spike_tall`; King's `hair_color` is now #c4702e (merge writes both into `looks.json`).
- **KEPT:** the 64x64 sheets and `tools/arc/char_*.py`, all `looks.json` entries until the merge, the `extras`, slim/wide arms, `scale`, `hold_r`, glow on/off per look,
  hair sheets (stay 64x64), the `_form2` ids, and the Time Magic VFX (not touched).

## Cost (rule 11)

Skins are client assets only: no server work, no packets, no ticks, no per-player state. Worst case: 13 PNGs of 256x256 (8 skins + 5 glow maps), 279 KB on disk in total; each
loaded look costs about 341 KiB of GPU memory (256x256 RGBA with mips) and one extra for its glow map; only the looks within draw distance are loaded, and the 24-block LOD
drops the second layer and the hair as before. Per-frame draw cost is unchanged (same quads as the 64x64 model); there is no per-frame allocation in the painters (they run at tool time).

## Review rounds (at least four per character, front / back / close with `tools/arc_hd/preview_arc.py`)

1. Baseline bridge vs. the 64x64 look: faces and hair silhouettes redrawn; King's tree emblem filled the chest (size 4.6 -> 2.7, tabard borders added).
2. Colour blocking: `H.tone` above 1.0 washed the fringe, trousers and boots grey -> `K.lit`, `K.cloth`, `hair_palette`; Ban's stubble was blotchy -> jaw shade + 16 strokes; Meliodas's black lock looked like a bar -> shortened.
3. Detail and glow: form 2 eye glow too white -> saturated gold; torn hems rewritten as a clean zigzag; non-deterministic seed (`hash`) replaced by fixed seeds.
4. Base-layer-only check of all eight (what is seen beyond 24 blocks) and a final front / back / close pass against owner references 154 to 156 and the old 64x64 look.

## Honest caveats

- Software renderer only (`tools/playermodel`, flat shading): nothing here was seen in the game, so the in-game light, the hair engine and the extras at their real size are unconfirmed.
- Weaker than the references 154 to 156: no truly bulky pauldrons or collars (the overlay geometry is one pixel thick), softer colour blocking, and the head sides, top and back
  keep the baseline bridged hair texture.
- Hair-engine matters (not skin): `anime_spike_tall` on Meliodas looks like a flat star-shaped crown from behind; Ban's `anime_spiky` spikes are tall and boxy; Diane's `twin_tails` leaves a pale nape patch.
- `test_fragments_match_looks_json` in `tools/arc_hd/test_arc_hd.py` fails until the lead runs `merge_looks.py` (the fragments differ from `looks.json` by King's `hair_color` and Meliodas's hair style).
