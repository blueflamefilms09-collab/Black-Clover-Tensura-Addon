# HD arc characters, group bc6: the Eye of the Midnight Sun and its friends (hand-painted 256x256)

Group bc6 is six characters, ten looks: Heath Grice, Lotus Whomalt, Morris Libardirt, Patolli, Rades Spirito (each with the second form where the looks table has one: Heath, Morris, Patolli,
Rades) and Revchi Salik (one form). The painters are `tools/arc_hd/chars/<look>.py` (`paint(spec, cid, masks) -> (skin 256, glow 256 or None, hair style id, hair rgb, slim)`), the form-2 modules
`<id>_form2.py` are one line each: they load the painter of the base look (`K.sibling`) and the painter picks the second palette from the id. The private kit is `tools/arc_hd/chars/_bc6.py` (a
copy of the bc4 kit plus a sibling loader, a chain painter along a path, bandage wraps, a stripe, eye bags, an eye-glow ring that keeps the pupil, and a crystal shard; the underscore keeps it out of
the generator and the module test). The pipeline is the one of `docs/arc/hd_arc.md`: the 64x64 sheet is the blueprint (layout, colour blocking and the second-layer coverage stay), the front of the
head and every body island are redrawn at 4x detail on both layers (the game drops the second layer and the hair beyond 24 blocks, `PlayerLod`), the other faces of the head are repainted clean
as skin and scalp. The hair sheets stay 64x64 (owner: no HD hair sheets). The old 64x64 skins are REPLACED under the same file names
(`src/main/resources/assets/nusmp/textures/entity/playermodel/<look>.png` and `_glow.png`), the generator keeps its 64 mode (`--size 64`).

Preview: `docs/previews/arc_hd/bc6_front_back.png` (front and back of all ten), `docs/previews/arc_hd/bc6_close.png` (the bust of all ten).

## Source (rule 10)

English wiki first, Japanese wiki as the fallback: neither could be fetched in this session (the proxy answers 403 for `blackclover.fandom.com`, the Japanese fandom and `ja.wikipedia.org`). So every
look is **from memory of the series** plus what the repo already records (`docs/arc/<id>.md`, which cite the wiki pages, and the `tools/arc/char_<id>.py` specs): the cut and the colours are the sheet's
choice, the canon points used are listed per character. Not fetched at all: the six English wiki pages and their six Japanese counterparts. The owner should allow `blackclover.fandom.com` if a
wiki check is wanted; the two details that are least sure are marked below (which eye of Rades is covered; the colour of Revchi's clothes).

## What is new and what is replaced (rule 5)

REPLACED (same file names): the ten 64x64 skins and the glow maps (`heath_grice`, `heath_grice_form2`, `morris_libardirt_form2`, `patolli`, `patolli_form2`, `rades_spirito_form2` existed; four are new
glow maps: `lotus_whomalt_glow`, `morris_libardirt_glow`, `rades_spirito_glow`, `revchi_salik_glow`), now 256x256; the look fragments `tools/arc_hd/looks/<look>.json` (size, hair, hair colour, `glow`; the
extras are kept as they were). NEW: the ten painter modules, the kit `_bc6.py`, this document, the two previews, four small glow maps (see Glow). KEPT: every extra (wings, halo, orb, smoke, horns,
capes, chains, thorns, props), the slim and wide arms, the height scale, the hair sheets, `looks.json` (the lead merges the fragments), the classic `braid` hair of Patolli.

Cost (rule 11): textures only. A 256x256 RGBA skin is 256 KiB of video memory, so the ten skins and the ten glow maps are about 5 MiB at most; no new quads, no new entities, no per-tick or per-frame
work, no packets. The distance culling is the existing `PlayerLod` (the second layer and the hair are dropped beyond 24 blocks, which is why every crisp detail is on both layers).

## Heath Grice (and form 2)

Design: a cold, hard young man: pale slanted half-lidded blue-white eyes with dark rings under them, thin flat brows, a hard mouth, a diagonal pink scar from the right brow across the nose bridge to the
left cheek with three stitch marks; a black military jumpsuit `#2a2a3c` with lit chest panels, a placket with steel piping and five buttons, a stand collar with two buckled steel straps round the neck, steel
shoulder boards with rivets, a double belt `#34343f` with a steel buckle, a watch chain from the second button to the belt, long sleeves with steel cuffs, black gloves with a steel knuckle line, trousers with
knee creases and boots with a steel cuff and strap; a snowflake emblem on the back (form 1). Slim.
Colours: hair `#cdd2e6`, steel `#8e9ab4`, strap `#8c98b2`, iris `#3a5a9a`/`#8aa6e0`, glow `#a6e4ff`. Glow: the eyes, a row of frost shards on both shoulders and a fan on the left forearm, the snowflake.
Hair: `anime_short_open`, a silver cut combed to one side with an open forehead (hair_map says the classic `slick`, which is a flat cap on top: this one has the volume of the character's cut).
Form 2 (the Purified): white coat `#e8ecf8` with gold trim `#cfa646`, gold eyes with a glowing ring, hair `#f6f6fc`, golden light lines on the face and a golden glowing piping line on each sleeve, white gloves
and boots with gold cuffs. The angel wings, the halo, the ice orb and the pocket watch are extras: the back between the shoulders is plain, the front of the belt is free of any watch drawing.

## Lotus Whomalt

Design: a tall, slouching, tired general: half-lidded grey-violet eyes under heavy flat brows with dark rings, stubble round the mouth and jaw, a brown pipe at the corner of the mouth (the viewer's right) with a glowing
ember, one grey streak in the fringe; a calf-long smoky violet coat `#443d58` with lit panels, lilac piping `#aaa6bc` along the opening, the hem and the cuffs, flared collar flaps over a violet cravat, board
shoulders with studs, side seams, a back vent and the lilac diamond of the Diamond Kingdom on the back; a pale shirt strip with a placket and buttons; a belt with a lilac buckle; a bare slim hand; dark
trousers and boots with a lilac cuff. Slim.
Colours: coat `#443d58`, trim `#aaa6bc`, shirt `#cbc5d8`, cravat `#8e82ae`, diamond `#cdc4e4`, hair `#4c4540`. Glow: only the pipe's ember (`#ff8a30`), a few texels (the sheet had none).
Hair: `anime_short` (as in hair_map): a messy short cut of dark locks that leaves the face and the pipe free. The smoke plumes on the shoulders, the wisp from the pipe, the low wisp behind the hem and the
open grimoire are extras: the shoulders are plain boards, the back hem is free, the left hand is bare.

## Morris Libardirt (and form 2)

Design: a tall, slim, blind scholar: round black tinted glasses with a glint and a thick frame on both layers (the closed eyes behind are not drawn), thin blond brows, a smirk, a sharp nose, a fringe of
thick blond clumps with a dark root edge; a calf-long cream coat `#dad2c0` with lit panels, an orange stand collar and orange piping on the opening, the cuffs and the hem, an orange cravat, a dark shirt in
the opening, a brown belt with an orange buckle and three notebooks, board shoulders, the orange rune of Modification Magic on the back with its ring and glyph strokes (faint orange glow), dark trousers
and low black shoes with a lit toe cap. Slim.
Colours: hair `#ffe680` (tint `#ffce1e`: the pipeline renders pale yellow as khaki, so the 3D hair tint is a more saturated gold), orange `#f28e44`, cream coat `#dad2c0`, dark shirt `#33303e`.
Hair: `anime_short_open` (hair_map says `anime_spiky`, whose long thin spikes read as fingers; the open cut has thick clumps and leaves the brows free).
Form 2 (Lucifero surfaces): no glasses, violet slit eyes that glow, a dark sigil on the forehead, thin veins on the cheeks that glow violet, black sleeves `#1a1722` with violet cuffs and black gloves
whose veins glow; the coat stays cream. The horns, the bat wings and the orb are extras: the top of the head and the upper back are plain. Glow: eyes, sigil, veins, the rune (form 1).

## Patolli (and form 2)

Design: a tall, slim elf: narrow gold eyes with a red wing of eyeshadow and two short red tattoo lines under each eye, thin white brows, a cold smirk, two long face-framing locks; a gold circlet with a
gem on the brow (both layers, round the whole head); a floor-long white robe `#f0ebe0` edged in black with a thin gold line inside the edge, the dark inner shirt `#30303c` with four gold buttons in the
opening, a black stand collar edged in gold, a black sash with a gold buckle and a hanging tail, wide sleeves with black cuffs and a gold line, long bare hands, a hem in black with a gold line. The
mantle on the back, the four-leaf grimoire and (form 2) the broken halo are extras: the back of the robe is plain under the mantle, the left hand is bare.
Colours: robe `#f0ebe0`, black `#26262f`, gold `#d8ae4a`, red paint `#c02a2a`, iris `#a06c14`/`#ecc452`. Glow: the eyes and the gem.
Hair: the classic `braid` (kept, as hair_map). **Hair problem:** no `anime_*` cut is a braid, and the classic `braid` is a flat cap on the top with the plait behind, so the front reads like a hat; the
face frame (the fringe and the two locks) and the circlet are painted so the cap reads as hair. Patolli with `anime_long` would lose the long plait that is the character's signature.
Form 2 (the Dark Elf): dark violet skin `#4a3c5c`, black eyes ringed with violet fire, violet war paint that glows, a night robe `#32204a` with violet trim `#a24ae4`, a violet circlet and sash.
Glow: eyes, the paint, the gem.

## Rades Spirito (and form 2)

Design: a wide-armed man with long messy light-purple hair, one blue eye (the character's right, the viewer's left; narrow, angry, under a hard brow with a dark ring under it), dark red bandages in
turns round the forehead and over the character's left eye (the viewer's right) with a loose end across the cheek and a light-green gem sewn on it, a raised scar with stitches on the character's right
cheek, a hard frown; bandages wound tight round the neck with a hanging end; a dark red sleeveless top `#6a1620` with black piping at the armholes, panel seams with stitching and a torn hem; a wide
dark belt with studs, a steel buckle and a hanging strap; bare muscled arms, the right forearm wrapped in dark red bandages, the left arm in a long black glove with a dark red strap and a studded wrist
strap; dark red trousers `#5a1220` and calf boots with a light cuff and one light stripe each. The tattered cape, the grimoire and (form 2) the ring of bone thorns are extras: the back is plain.
Colours: hair `#c8aaff` (tint `#ac70ff`: lighter colours render grey on the hair sheet), bandage `#8a1c2c`, gem `#9be8b0`, iris `#1e48a8`/`#4a82e8`.
Glow: the gem (both forms), a thread of light on the glove strap, in form 2 the eye and the veins.
Hair: `anime_long_open` (hair_map says `anime_short`, too short for the long hair of the doc), form 2 `anime_mane` (as hair_map).
Form 2 (the Betrayed): ashen skin `#8a8694`, the visible eye burns green with a glowing ring and a wider, madder shape, a thin grin with teeth, glowing green veins on the face and both arms, a redder
top `#8a1c2c` and trousers `#6a1620`, hair `#e4d2ff` (tint `#cda0ff`) as a mane.
Which eye: the doc text says "the left eye"; the old sheet drew the patch on the viewer's left; this paint follows the text (the character's left, the viewer's right) and the glove is on the same side.
Change `eyepatch()` and the mirrored x values if the wiki shows the other side.

## Revchi Salik

Design: a slim man in his thirties with a greedy, calculating look: a half-lidded sharp eye with a dark ring under it and a thin brow (the viewer's left), the left side of the face (the viewer's right)
taken by the big burn scar, a raised pink mass over the cheek, the brow and the eyelid with a dark rim, mottled blotches, shiny streaks and a slit eye cut into it; stubble on the jaw and a lopsided
smirk; a dark thief's coat `#2c2a36` with steel trim `#7a8498` along the opening, the lapels, the shoulder plates (with rivets) and the cuffs, a torn hem, a pale laced shirt `#cfc8bc` in the opening, a
purple scarf `#5a4470` in two turns with a hanging end, a short chain across the chest with a lock plate at the shoulder (an addition of this paint, made of the same links as the extras), a double
belt with two pouches and a steel buckle, fingerless gloves with bare fingertips, dark trousers and knee boots with a steel turn-down and two straps. Slim, no second form.
Glow: the sheet's glow lives in the steel of the chains (extras); here a faint cold glow `#bcd0ff` in the painted chain only.
Hair: `anime_swept` (hair_map says `anime_short`; the doc says long droopy hair, and `anime_bangs_long` is a spiky pom, `anime_long_open` reads like a bob): a droopy cut with a heavy fringe over one
brow. The chains on the forearms, the chain with the kunai blade at the back and the purple cape are extras: the cuffs and gloves are drawn clear of the rings, the back is plain.

## Rounds

Every look was looked at in front, back and a close three-quarter bust against the old 64x64 look at least four times (the last ones are the two previews): the first paint, the face and hair pass (eyes,
scars, fringe, hair style), the cloth and metal pass (piping, plates, straps, belts, boots), and the fix pass (eye glow rings, the scar routes, the frost shards instead of a polyline, the pipe, the glasses
and the sigil placement, the war paint size, the boot stripes, the V insert that read as a hole and was dropped, the hair tints).

## Hair problems (for the lead)

* `anime_short`, `anime_short_open` (Lotus, Heath, Morris) and `anime_swept` (Revchi) still have a few thin spike tips on the crown; the clumps are thick, but the tips can read as fingers at 3 blocks.
* `anime_swept` hides the eye on the viewer's left of Revchi under its fringe; with `anime_long_open` both eyes are free but the cut reads feminine.
* Patolli keeps the flat-capped `braid` (see above).
* The preview renderer turns pale hair colours grey, so the fragment's hair colour is a tint chosen to render right (Morris gold, Rades purple), not the hex of the scalp.
