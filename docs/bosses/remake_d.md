# Tier List remake, group d (style v3, hand-painted)

Characters: `axe_hand_morgan`, `bertholdt_hoover`, `shou_tucker`, `light_yagami`. Code: `tools/tier_bosses/remake/<id>.py` (+ private helper `lib_d.py`), loaded by `gen_tier_looks.py`.
All four are original pixel art painted face by face (shaded ramps, clustered light/mid/shadow, darker outlines), no traced skins.
Previews: `docs/previews/tier_style_v3/d_front_back.png`, `d_close.png`.

**Sources (rule 10).** English wiki pages (attackontitan / onepiece / fma / deathnote fandom, en.wikipedia) and the search tools could not be fetched: the session blocks those hosts (DNS failure,
so the allowed-domain list would need them). No Japanese wiki was reachable either. Every look below is therefore **from memory** of the series, not checked against a wiki page.

## axe_hand_morgan (One Piece, Axe-Hand Morgan)
- Colours: deep-navy Marine captain coat, cream-white collar/lapels/cuffs, gold buttons and piping, steel epaulettes with gold fringe, grey-blue trousers, black boots with steel cuffs.
- Pieces: huge epaulettes, big cream cuffs, double belt with gold buckle, navy cap band on the hat layer (top face empty), the right arm ends in a steel axe gauntlet with a bright cutting edge.
- Face: square slab jaw, heavy brows, small hard steel-blue eyes, stubble, wide mouth. Hair: `anime_short`, brown. Wide arms.
- Weaker than the references: the axe reads as a steel gauntlet only (the arm is a box); the hair silhouette is the kit's plain crop (a flat-top block would be better).

## bertholdt_hoover (Attack on Titan, Bertholdt Hoover)
- Colours: brown jacket, dark leather harness and double belt with silver buckles, cream trousers with thigh straps, tan-topped brown boots, green Scout cloak.
- Pieces: cloak shoulder pieces on both arms, leather bracers, white cravat, on the back the green cloak with an original V of two feathered wings (white and blue, a Wings-of-Freedom-style motif).
- Face: sad tired green-grey eyes with catch-lights, small downturned mouth. Hair: `anime_short_open`, near-black brown. Wide arms. No glow.
- Weaker: the wing emblem is small (8x12 back), hair is a kit style (his long parted bangs are only approximated).

## shou_tucker (Fullmetal Alchemist, Shou Tucker)
- Colours: warm-white lab coat, red-brown waistcoat, cream shirt, green tie, brown trousers and shoes.
- Pieces: open coat with a pen-filled breast pocket and a green stain, sleeves rolled to the elbow (bare forearms with a small stain), silver watch band, thin round glasses on the hat layer (top rim, bridge, temple arms; opaque pixels only).
- Face: half-shut tired amber eyes, eye bags, stubble, a small kind-tired smile. Hair: `anime_short`, brown. Slim arms.
- Weaker: the glasses are a one-row rim (the face is only 8 px wide); colours are quiet compared with the references.

## light_yagami (Death Note, Light Yagami)
- Colours: dark navy blazer with lighter lapel edges and silver buttons, white shirt, charcoal tie, dark trousers with a crease, polished black shoes. Hair auburn brown (`anime_swept`), amber eyes that glow (glow map on the irises). Slim arms.
- Kept: the notebook pocket and the teal cuff line are drawn by `gen_tier_looks.light_post`, called at the end of the module (3.36.0 motif). The suit stays dark and has no red, so `LightYagamiDataTest` passes.
- Weaker: a plain school suit has few "bulky" pieces; the identity is the hair, the eyes and the notebook.

## Requests / limitations
- Hair: a big spiky/flat-top silhouette (Morgan) and long parted bangs (Bertholdt) are not in the kit; closest styles used. Request: a `anime_flattop` and an `anime_bangs_long` style.
- Cost: texture only (64x64 per character), no new entities, packets or per-tick work.
