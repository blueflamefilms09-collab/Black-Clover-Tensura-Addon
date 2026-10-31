# Tier boss skins, style v3 remake: group b

Hand-painted skins (`tools/tier_bosses/remake/<id>.py`, shared helper `lib_b.py`: ASCII pixel maps stamped face by face, mirrored arms). Original pixel art, painted from memory.
Regenerate with `python3 -B tools/tier_bosses/gen_tier_looks.py mr_2_bon_clay malty_melromarc rachel jugram_haschwalth`.
Previews: `docs/previews/tier_style_v3/b_front_back.png`, `b_close.png`.

**Sources (rule 10):** onepiece.fandom.com, shieldhero.fandom.com, tensura.fandom.com and en.wikipedia.org could not be fetched (hosts not allowed in this session's network), and the Japanese wikis were not tried after that. All four looks are therefore **from memory** plus the colours of the existing sheets in `tier_specs.py`. Rachel's series (Tower of God) is an assumption from the sheet's tier neighbours; if she is another Rachel the outfit colours still follow the sheet.

| id | hair (kit style, colour) | arms |
|---|---|---|
| mr_2_bon_clay | anime_afro, #2a2a3e | wide |
| malty_melromarc | anime_long_open, #c8141f (tiara) | slim |
| rachel | anime_long_open, #7a4528 | slim |
| jugram_haschwalth | anime_long_open, #fff8e0 | wide |

## Mr. 2 Bon Clay (One Piece)
Colours: hot pink / white / black afro / blue-violet makeup. Face: thick blue-violet eyeshadow, big purple eyes with lashes, pink cheeks, wide red-lipped grin with white teeth. Pieces: white feather boa collar and
feather epaulettes with pink tips, hot-pink V-neck leotard with a white heart, magenta sash, scalloped white-and-pink tutu over body and hips, pink armbands, long white gloves with pink feather cuffs, white tights,
white ballet boots with criss-cross violet ribbons, pink feather tops and pink soles.

## Malty S. Melromarc (Shield Hero)
Colours: crimson / cream / gold / maroon. Gold tiara with a ruby on the hat layer (row 2), crimson royal dress with a cream tabard edged in gold and a ruby clasp, gold belt, flared short skirt with gold hem,
crimson mantle on the back with a cream fur collar and gold scalloped hem, gold-rimmed puff shoulders over cream sleeves with gold cuffs, wine thigh-high stockings with gold garters, maroon boots with gold cuffs.
Face: sharp red eyes, long lashes, thin brows, a smirk in red lipstick.

## Rachel (Tower of God, from memory)
Colours: teal-cyan / white / charcoal / chestnut. White ribbon with a side bow on the hat layer, white high collar and a cascading bow, white tabard with bright cyan edges, dark belt with white clasp, scalloped white
capelet, cyan sleeves with big white ruffle cuffs, flared skirt with a white hem, charcoal leggings with a cyan side line, slate boots with white folded cuffs. Face: amber-brown round eyes, lashes, flat cold brows, straight mouth, no blush.

## Jugram Haschwalth (Bleach)
Colours: white / black / steel / one blue accent. White knee-length coat worn open over a black vest with a blue cross emblem, blue piping on the open edge and hems, raised blue collar, steel pauldrons and bulky
steel gauntlets with a blue cuff row, steel belt with a blue gem, white coat tails over charcoal trousers, flared steel boots. Long pale hair (parted, no centre fringe), pale blue sharp eyes, thin pale brows, cold mouth.

## Honest limits
- The hair silhouette is the kit's anime_* geometry; the references' huge spiky masses are not reachable. Bon Clay would want a bigger, rounder afro (request: an `anime_afro_big` style, wider and taller than anime_afro); Jugram's hair is longer than canon (anime_long hangs to the waist).
- The preview renderer lights flatter than the game, so colour contrast is judged from the textures; none of this was seen in game.
- Rachel is the least certain canon-wise and the lowest-contrast skin of the four.
- Not done: glow maps (none of the four needs glowing parts).
