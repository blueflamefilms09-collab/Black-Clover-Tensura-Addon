# HD outfits, group a (Zetsu, Yammy Llargo, Sogou Nobuyuki, Kurozumi Orochi, Charlos check)

Owner request: "rework the hairs it don't look right ... the spiky hair sucks ... fix how the hair is floating ... also make the outfits HD".
Group a is painted with the HD pipeline of `docs/bosses/hd_outfits.md` (256x256 skin plus glow map, every model pixel a 4x4 block of texels) and the new hair
styles of the hair rework (`docs/bosses/anime_style_v2.md`). The colour blocking and the silhouette of the 64x64 remake (`docs/bosses/remake_a.md`) stay, HD adds
the detail (folds, weave, trims, jewels, scales, stitches) and a strong readable outline.

Regenerate: `python3 -B tools/tier_bosses/gen_tier_looks.py charlos zetsu yammy_llargo sogou_nobuyuki kurozumi_orochi` (about 4 s for all five, offline, no game).
Previews (software renderer, not the game): `docs/previews/tier_style_v4/a_front_back.png` (front and back of all five), `a_close.png` (head and chest, front and 3/4).

## New or replacement (CLAUDE.md rule 5)

| item | status |
|---|---|
| `tools/tier_bosses/hd/zetsu.py`, `yammy_llargo.py`, `sogou_nobuyuki.py`, `kurozumi_orochi.py` | NEW |
| `zetsu.png`, `yammy_llargo.png`, `sogou_nobuyuki.png`, `kurozumi_orochi.png` (256x256) | REPLACE the 64x64 PNGs (the owner asked for HD); the 64 painters stay in `remake/` and `--size 64` still reaches them |
| `*_glow.png` of the four (256x256) | REPLACE the 64x64 glow maps, same places lit (eyes, the Yammy "10", the Sogou gems and wings, the Orochi gold crest and obi) plus finer rims |
| `looks_tier.json`: `hair` and `hair_color` of the four | CHANGED by the generator (see below) |
| `charlos.py` (pilot) | KEPT, one tweak: `hair_color` `#ffe040` -> `#ffd008` (see below) |
| `tools/tier_bosses/hd/hdlib.py`, `hd_core.py`, `hd_detail.py`, `hd_bridge.py`, hair files, tests | KEPT, not touched |

## Hair choice per character (checked in the preview with the real new hair, front, back, three-quarter)

| id | hair style | colour | why |
|---|---|---|---|
| zetsu | `anime_mane` | `#46b83a` | the green Venus-flytrap hood: a big leaf crown on top, side curtains down to the shoulders, a heavy back piece. `anime_hood`, `anime_spike_tall` and `anime_spike_swept` were tried; the mane is the only one that touches the head all round and gives the trap silhouette. The base head also carries a toothed leaf rim painted on both layers, so the far LOD (overlay and hair dropped beyond 24 blocks) still shows the leaf frame |
| yammy_llargo | `anime_spike_tall` | `#2a2a3e` | the dark wild spiky mop, one big tall silhouette. The new spike hair is attached (the roots cover the whole scalp), the base head has painted hair strands under it |
| sogou_nobuyuki | `anime_short_open` | `#ffd000` | short golden hair, the forehead shows the gem band of the crown. `anime_long_open`, `anime_long`, `anime_swept` and `anime_bangs_long` were tried: the long ones hide the back wings, the one thing that reads at a distance. Trade-off: his long canon hair is shorter here |
| kurozumi_orochi | `anime_topknot` | `#26263c` | the samurai top knot (chonmage) on the shaved sides, sits on the head, no gap |
| charlos (check) | `anime_short_open` | `#ffd008` | fits: attached, forehead tiara visible. The hair tint multiplies a grey sheet, so pale colours render khaki and dull; the more saturated gold keeps the blond |

The tint lesson matters for the other groups too: pick saturated hair colours (`#ffd000` instead of `#ffe040`), the pipeline darkens them.

## Design per character

All four use the HD helpers (`H.cloth`, `H.folds`, `H.sheen`, `H.bevel`, `H.trim_*`, `H.metal_paint`, ...), light from the top left, an outline shadow where cloth lies over cloth.
Detail is drawn on BOTH layers where it must survive the LOD drop (face, collar, the main emblem), the overlay carries the cutout parts (jacket, sleeves, coat tails).

### Zetsu (Naruto, Akatsuki)
- Face: split in half, left white, right black, two yellow slit eyes (dim glow so they do not blow out to white), a toothed grin stitched across, a toothed leaf rim round the face (both layers).
- Cloak: black high-collar Akatsuki cloak with weave and folds, red V collar lining, a red roll at each cuff, red lining at the hem.
- Clouds: the red cumulus with a white outline, a bumpy underside and a swirl in each lobe, on the chest, the sleeves, the sides and a big one on the back. Edge bevel and a soft shadow so they stand off the cloth.
- Legs: black trousers, white spiral shin wraps, black sandals.

### Yammy Llargo (Bleach, Espada 10)
- Face: dark tan, heavy angry brows, glowing orange eyes with a heavy lid, stubble, a scar, nasolabial lines, a frown; a bone mask fragment on the viewer's right temple (front and east side).
- Chest: bare, pecs, sternum, collarbones, abs blocks and ribs; the dark ink "10" with an orange core and glow on the stomach.
- Jacket: white, open lapels (kept narrow so the chest stays visible), black sash with knot, back tails and a back plate; white sleeves with a black rim, bare forearms with a tendon line, black wrist guards with white tape, fists with knuckles.
- Legs: white pleated hakama, tabi and geta.

### Sogou Nobuyuki (Sword Art Online, Oberon, king of the fairies)
- Face: violet glowing sharp eyes, a thin smirk, a crown band with a ruby on both layers.
- Coat: deep violet with a gold vine (arabesque) pattern, a white jabot with three ruffle rows and a ruby brooch, gold piping, buckle belt, an opening skirt.
- Shoulders and arms: gold pauldrons with a gem, a segmented gold gauntlet, a white glove.
- Back: two pairs of pale-blue veined dragonfly (fairy) wings with a glow, drawn on top of belt and skirt edge so nothing cuts them.
- Legs: violet trousers with a side stripe, gold scalloped boot cuff, buckle strap.

### Kurozumi Orochi (One Piece, Shogun of Wano)
- Face: heavy brows, slit eyes with bags, a narrow drooping moustache and a goatee.
- Kimono: purple with a triangle scale (uroko) lattice, a pale under-kimono V with two diagonal collar bands (an earlier white V read as a beard), a lavender brocade obi with gold trim and a flat gold knot with tassels (glows).
- Kataginu: stiff indigo winged shoulders with a lavender piped edge; lilac sleeve lining.
- Back: the eight-necked snake crest, a white disc in a gold ring with eight gold necks (glows), a lavender musubi bow.
- Legs: indigo pleated hakama, lavender hem, tabi, geta with red thongs.

## Look rounds

At least four rounds per character, each against the owner references (images 154/155/156 and the feedback shots 157/158) in the software preview (front, back, 3/4, side, head close, glow check):
Zetsu 7 (hair choice, split face, the cloud shapes three times), Sogou 8 (gold too bright, glow blown out, long hair hid the wings), Yammy 4 plus back and side checks (lapels covered the chest, the numeral clashes slightly with the abs), Orochi 4 plus back and side checks (the white V read as a beard, the hakama was recoloured bluer), Charlos 2 (hair colour).

## Sources (CLAUDE.md rule 10)

The English and Japanese wikis could not be fetched in this session (no network from the sandbox), so every design above is FROM MEMORY of the series (Naruto, Bleach, Sword Art Online, One Piece) and of the character notes in `remake_a.md`. Nothing could be verified against a wiki page. Colours and the clothing layout come from `docs/bosses/remake_a.md` and `tier_specs.py`, the HD pass only adds detail.

## Cost (CLAUDE.md rules 9 and 11)

- No runtime code changed, no packet, no entity, no tick work. Pure data: five 256x256 skins (36 to 52 KB each) and five glow maps (1 to 5 KB), loaded once per model like any skin.
- Draw cost on the client: the same player model (6 boxes plus overlay boxes) and the same hair mesh as before, so the same quad count; the only change is a bigger texture (256 KB of RGBA per skin in VRAM, five of them at most 1.3 MB, shared by all instances of the id). Far players already drop overlay and hair beyond 24 blocks (PlayerLod).
- Tool cost: about 1 s per character on the generator, offline.
- Server: no change, the skin tables are the same JSON as before.

## Honest caveats

- Nothing was seen in a game. The previews come from the repo's software renderer, which has no real lighting, no glow bloom and no tint pipeline of the real hair shader; the hair colours in game may look a little darker or duller than the preview. Check `#ffd000` (Sogou, Charlos) and `#46b83a` (Zetsu) first.
- Sogou: the short hair costs his long canon hair, in exchange the wings stay visible.
- Zetsu: the cloak is dark and flat apart from the clouds and the lining (it is a plain black cloak in canon).
- Yammy: the "10" is lit but small; at range it reads as a glowing dot.
- Orochi: the face still reads slightly sad (the droop moustache); the topknot reads like a small top hat from the 3/4 view because the hair sheet is boxy.
- The cloud shapes and the leaf rim are my reading of the series from memory.

## Shared tool problems found (not fixed, not mine to edit)

- `hd_detail.py` `H.mouth_smile(..., open_h=...)` raises `TypeError: ufunc 'hypot' not supported` when `open_h` is set and the width is not a binary-exact number, because a tiny negative base (about -1e-16) reaches a fractional power and becomes complex. Worked around in `zetsu.py` (own grin code with `max(0.0, ...)`) and `yammy_llargo.py` (width 3.5). A clamp `np.maximum(base, 0.0)` inside the helper fixes it for everybody.
- `test_hdlib.py` `test_every_hd_module_has_its_hd_png` currently fails only for `ao`, a module of another group whose PNG is still 64x64 (not part of group a).
