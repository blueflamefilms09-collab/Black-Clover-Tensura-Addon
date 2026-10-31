# Tier boss skin remake, group a (style v3, hand-painted)

Owner request: "remake them" (the 17 style-v2 skins were generic and flat). Group a: charlos, zetsu, yammy_llargo, sogou_nobuyuki, kurozumi_orochi.
Each has a hand-authored painter in `tools/tier_bosses/remake/<id>.py` (shared helper `lib_ga.py`: ASCII panels in texture space, banded cloth shading, anime eyes). Original pixel art, nothing traced.
Regenerate: `python3 -B tools/tier_bosses/gen_tier_looks.py charlos zetsu yammy_llargo sogou_nobuyuki kurozumi_orochi`.

**Sources (rule 10):** the English wikis (onepiece/bleach/swordartonline/naruto fandom), en.wikipedia.org and ja.wikipedia.org could NOT be fetched (the session cannot resolve those hosts; they must be allowed in the
network settings). Every look below is **from memory** and has not been checked against a wiki. Uncertain details are marked (?).

## charlos (One Piece, Celestial Dragon)
Hair `anime_short_open` golden. Cream-white suit with a huge ruffled collar and a gold neck ring (the bubble-helmet rim, painted on the hat layer chin row), gold circlet with a glowing ruby, gold epaulettes with
fringe, puffed ruffle cuffs with a gold band, crimson waistcoat with gold buttons and gold edges, crimson sash with a gold buckle, crimson cape on the back with a gold sun crest, white trousers with a gold side stripe,
crimson boots with gold cuffs. Colour blocking: white / gold / crimson. Glow: gold studs, buckle, crest, ruby. (?) no moustache/helmet glass (a translucent bubble is not possible on the skin).

## zetsu (Naruto)
Hair `anime_hood` lime green = the Venus-flytrap clamp. Face split down the middle (pale left, black right), big glowing yellow eyes with slit pupils, sinister grin. Black Akatsuki cloak with white-outlined red
clouds (body, back, both sleeves), tall red-lined collar, red cuffs, one white hand and one black hand, black trousers with white shin tape and dark sandals. Glow: both eyes. (?) exact cloud layout, tape.

## yammy_llargo (Bleach, Espada 10)
Hair `anime_spiky` black. Tan skin, small fierce eyes with orange glowing iris, broad nose, heavy frown, crack scar, bone mask fragment at the right temple (?) side, white open jacket with a high collar and wide white
shoulders, bare muscular chest with the numeral 10 on the belly (orange, glowing), black sash, black wrist guards with white tape, wide white hakama, black tabi and wooden geta. (?) place of the mask fragment,
numeral colour (canon is a dark tattoo; orange chosen for the glow map).

## sogou_nobuyuki (SAO, Oberon / Nobuyuki Sugou)
Hair `anime_swept` golden (the kit has no wavy mid-length cut; `anime_long*` would hide the back). Gold crown with a glowing red gem, violet glowing eyes, thin cruel smile, deep-violet long coat with gold piping, tall gold
collar, white jabot with a gem, gold pauldrons with gems, gold gauntlets over white gloves, black belt with a gold buckle, black trousers with a gold stripe, tall black boots with gold cuffs, translucent pale-blue
dragonfly wings drawn on the back (glowing). Glow: eyes, gems, wings.

## kurozumi_orochi (One Piece, Shogun of Wano)
Hair `anime_topknot` black. Heavy brows, slit eyes, drooping moustache and goatee. Deep-purple kimono with a subtle snake-scale (uroko) pattern, crossed white collar with a lavender lapel edge, winged indigo shoulders
(kataginu) with lavender edge, wide lavender obi with a gold cord and a big knot on the back, wide hanging sleeves with lilac lining, gold-and-white eight-snake crest on the back, indigo pleated hakama, white tabi, wooden
geta. Glow: the obi row. (?) crest and pattern are stylised.

## Cost and limits (rule 11)
Offline pixel art: no code in the game, no new packets; each skin is one 64x64 PNG (+ glow map). Worst case in game: the usual 1 humanoid model per boss (6 parts x 2 layers), nothing new.

## Still weaker than the three references
- The hair is the kit's geometry tinted by one colour: no 3-4 tone clumps or highlight pixels (references paint those). The tint renders darker than the chosen RGB in the preview, so golds look olive.
- No kit style gives a huge spiky mass (Yammy, Zetsu's spikes) or wavy mid-length hair (Sogou). Request: an `anime_wavy` mid-length cut and a taller `anime_spiky_big`.
- Small faces: the fringe hides the brows; eyes are 2x3 pixels. Fabric shading is banded, not hand-clustered everywhere (sleeves, legs).
