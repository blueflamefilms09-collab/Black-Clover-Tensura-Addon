# HD outfits, group d (Axe-Hand Morgan, Bertholdt Hoover, Shou Tucker, Light Yagami)

Owner request: "rework the hairs it don't look right ... the spiky hair sucks ... fix how the hair is floating ... also make the outfits HD".
Group d is painted with the HD pipeline of `docs/bosses/hd_outfits.md` (256x256 skin plus glow map, every model pixel a 4x4 block of texels) and the new hair
styles of the hair rework (`docs/bosses/anime_style_v2.md`). The colour blocking and the silhouette of the 64x64 remake (`docs/bosses/remake_d.md`) stay, HD adds
the detail (folds, weave, ribs, trims, stitches, rivets, glasses, a notebook) and a strong readable outline.

Regenerate: `python3 -B tools/tier_bosses/gen_tier_looks.py axe_hand_morgan bertholdt_hoover shou_tucker light_yagami` (about 1.4 s per character, offline, no game).
Previews (software renderer, not the game): `docs/previews/tier_style_v4/d_front_back.png` (front and back of all four), `d_close.png` (head and chest, front, 3/4 and back).

## New or replacement (CLAUDE.md rule 5)

| item | status |
|---|---|
| `tools/tier_bosses/hd/axe_hand_morgan.py`, `bertholdt_hoover.py`, `shou_tucker.py`, `light_yagami.py` | NEW |
| `tools/tier_bosses/hd/_dlib.py` | NEW, shared helpers of group d only (hair painted on the base head, rivets, buckle, leather, ribs, slash pocket). The leading underscore keeps it out of `test_hdlib.HD_IDS` |
| `axe_hand_morgan.png`, `bertholdt_hoover.png`, `shou_tucker.png`, `light_yagami.png` (256x256) | REPLACE the 64x64 PNGs (the owner asked for HD); the 64 painters stay in `remake/` and `--size 64` still reaches them |
| `light_yagami_glow.png` (256x256) | REPLACES the 64x64 glow map, same places lit (the two irises), finer |
| `looks_tier.json`: `hair` and `hair_color` of the four | CHANGED by the generator (see below) |
| `tools/tier_bosses/remake/*` (the four 64 painters, `light_post`) | KEPT, `light_post` is no longer on the HD path (the HD module draws the notebook and the teal cuff itself) but stays in `gen_tier_looks.py` for `--size 64` |
| `hd_core.py`, `hd_detail.py`, `hd_bridge.py`, `hdlib.py`, hair files, tests | KEPT, not touched |

## Hair choice per character (checked in the preview with the real new hair, front, back, three-quarter)

| id | hair style | colour (stored) | why |
|---|---|---|---|
| axe_hand_morgan | `anime_flattop` | `#604226` | his canon flat-top (square block above a square jaw). Reads as a brown block cap with ear-flap side locks, which suits the military silhouette; no strand texture on the 3D block, so the strands are painted on the base head for the far LOD |
| bertholdt_hoover | `anime_bangs_long` | `#342822` | parted fringe over the brow plus side locks down to the jaw; the back hair stops at the nape so the two cloak wings stay visible. `anime_long_open` was tried: the back hair reaches the hips and hides the wings, the one thing that reads at a distance |
| shou_tucker | `anime_short_open` | `#563e2e` | messy short hair, the forehead and the glasses stay free. `anime_swept` was tried: its fringe block covers the eyes and the glasses |
| light_yagami | `anime_short_open` | `#76523a` | forehead and eyes free (the glowing irises are his whole face). `anime_swept` hid the eyes, `anime_short` is the same tuft ring. The contract of `LightYagamiDataTest` needs an `anime_` style |

The hair tint multiplies a grey sheet and renders darker and more orange than the hex, so all four colours are dark and a little desaturated.

## Design per character

All four use the HD helpers (`H.cloth`, `H.folds`, `H.sheen`, `H.bevel`, `H.trim_*`, `H.metal_paint`, `H.eye`, ...), light from the top left, an outline shadow where cloth lies over cloth.
Detail is drawn on BOTH layers where it must survive the LOD drop (face, collar, emblem, cuffs), the overlay carries the cutout parts. Every base island is fully opaque, the base head carries painted hair, ears, sideburns and the nape for the case where the 3D hair is dropped (24 blocks).

### Axe-Hand Morgan (One Piece, Marine captain), wide arms
- Face: heavy scowl brows, steel-blue eyes, broad nose, jaw shading, stubble speckle, a flat-top hairline with sideburns.
- Coat: navy double-breasted with narrow cream lapels with gold piping, white cravat, two columns of gold buttons, a gold badge, pocket flaps, gold hem, a double belt with a big gold anchor buckle.
- Shoulders: steel-blue ribbed epaulettes with gold ribs and gold fringe.
- Back: gold wings with an anchor crest, a vent in the coat, belt and buckle.
- Arms: the left sleeve ends in a cream cuff with gold buttons and a bare fist; the right arm carries the axe-hand, a steel socket ring with rivets and a crescent blade (a bright cutting edge, a wedge profile on the side faces). The axe is painted on a box arm, the arm is not longer or wider.
- Legs: pale steel-blue trousers with straight folds, a crease, a slash pocket, knee wrinkles, a gold line above steel greaves with rivets, black boots with a strap and buckle.

### Bertholdt Hoover (Attack on Titan, Scout), wide arms
- Face: a sad parted look, sad brows, green-grey eyes with bags, a small downturned mouth.
- Jacket: brown with leather harness straps over the chest, a small cravat, a chest pocket, a double belt with two buckles, a hip gear case on each side.
- Cloak: green Scout cloak on the shoulders, ragged scallops on the arms and the hem, a gathered neck cord.
- Back: the Wings of Freedom, two crossed feathered wings (six feathers each, blue and white), drawn on top of belt and cloak so nothing cuts them. Cloak tail on the back of both legs (ragged, the rest of the back overlay is erased so the legs stay clear).
- Arms: leather bracers and gloves with one buckle each. Legs: white trousers with a thigh strap and buckle, tan boot cuffs, boots.

### Shou Tucker (Fullmetal Alchemist), slim arms
- Face: tired amber eyes with bags, a stubble beard, a mild smile. Round glasses on the overlay (a thin steel rim and a glint), a soft shadow ring on the base, bridge and temple arms on both side faces.
- Lab coat: cream, open panels with lapels and stitches, a breast pocket with three pens, a hip flap pocket, a green stain; a red-brown waistcoat with four buttons over a cream shirt and an olive tie, belt buckle.
- Back: a chalk transmutation circle, a stitched patch, a centre vent, a frayed hem.
- Arms: rolled sleeves with a cuff roll and stitch, bare forearms with tendon lines, a silver watch on the left wrist.
- Legs: creased brown trousers with a slash pocket and knee bags, worn laced shoes with a scuffed toe.

### Light Yagami (Death Note), slim arms, glow
- Face: narrow sharp amber eyes with a flare, thin slightly severe brows, a faint smirk, a dim glow on both irises (so they do not blow out).
- Blazer: dark navy painted first, the white shirt V and a woven charcoal tie with a knot on top, notch lapels with stitching, two silver buttons, flap pockets, a welt breast pocket.
- Notebook: the teal Death Note in the breast pocket (cover, spine, pages, ribbon, label ring) on both layers, inside the body-front columns 5..6 rows 2..5 that `LightYagamiDataTest` checks.
- Left cuff: the teal page-edge line (3.36.0 motif) round all four faces on both layers, white shirt cuffs with sleeve buttons on both arms.
- Legs: dark charcoal trousers with a crease and a slash pocket, polished shoes with laces and a sheen.

## Look rounds

At least four rounds per character, each against the owner references (images 154/155/156 and the feedback shots 157/158) in the software preview (front, back, 3/4, side, head close):
- Morgan 5: first full paint; epaulettes, axe and greaves were too white (darker blue steel, crescent axe); trousers were wavy (straight folds); hair tint check; slash pocket.
- Bertholdt 4: the cravat read as a beard (smaller); the wings were small and low (bigger, higher); the hair read orange (darker, less saturated tint); the back cloak tail and the `anime_long_open` check.
- Tucker 4: `anime_swept` hid the glasses (short_open); the glasses rim was too bright (darker); the base ring softened; trousers and slash pocket.
- Light 5: `anime_swept` hid the eyes; the eye glow blew out (dim glow colour); a white strip under the tie (blazer painted first, shirt V on top); a pinstripe look (shallower folds); the trouser top was too bright (cloth top 1.28 to 1.1).

## Sources (CLAUDE.md rule 10)

The English and Japanese wikis could not be fetched in this session (no network from the sandbox), so every design above is FROM MEMORY of the series (One Piece, Attack on Titan, Fullmetal Alchemist, Death Note) and of the character notes in `remake_d.md`. Nothing could be verified against a wiki page. The colours and the clothing layout come from `remake_d.md` and `tier_specs.py`, the HD pass only adds detail. The axe-hand shape, the Wings of Freedom feather count and the chalk circle are my reading from memory.

## Cost (CLAUDE.md rules 9 and 11)

- No runtime code changed, no packet, no entity, no tick work. Pure data: four 256x256 skins (40 to 46 KB each as PNG) and one glow map (under 1 KB), loaded once per model like any skin.
- Draw cost on the client: the same player model (6 boxes plus overlay boxes) and the same hair mesh as before, so the same quad count; the only change is a bigger texture (256 KB of RGBA per skin in VRAM, four skins plus the Light glow map 1.25 MB at most, shared by all instances of the id). Far players already drop overlay and hair beyond 24 blocks (PlayerLod), the base head carries painted hair for that case.
- Tool cost: about 1.4 s per character on the generator, `test_hdlib` about 49 s for all HD modules, offline.
- Server: no change, the skin tables are the same JSON as before.

## Honest caveats

- Nothing was seen in a game. The previews come from the repo's software renderer, which has no real lighting, no glow bloom and no tint pipeline of the real hair shader; the hair colours in game may look a little darker or duller than the preview. Check the four hair colours first.
- Hair (not mine to edit): `anime_bangs_long`, `anime_short_open` and `anime_short` carry a ring of crown tufts that still reads slightly spiky from the front and the 3/4 view (Bertholdt, Tucker, Light). `anime_swept` hides the eyes (Light, Tucker), `anime_long_open` hides the back (Bertholdt). `anime_flattop` reads like a brown hat block with ear-flap side locks and has no strand texture (Morgan). I did not change any hair file.
- Morgan: the axe-hand is painted on a box arm, so it is a flat drawing, not a real blade.
- Bertholdt and Morgan have no glow map (nothing in the outfit emits light), Tucker neither; only Light has one (the irises).
- The glasses of Tucker sit on the overlay and vanish beyond 24 blocks, the base head keeps a faint shadow ring.
