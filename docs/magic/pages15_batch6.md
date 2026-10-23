# Pages 15, batch 6: Space, Time, Game and Mirror

Owner goal: every grimoire has 15 spells. This batch owns these books (and no other): `book_compass`, `book_copy`, `book_dice`, `book_slash`, `book_gravity`, `book_game`, `book_spatial`, `book_mirror`, `book_time`, `book_legion`, `book_key`, `book_kotodama`.

How to build a page: `docs/magic/pages15_recipe.md`. Files this batch edits: `src/main/java/com/newuniverse/nusmp/book/pages15/Batch6.java`, `tools/lang/pages15_batch6.json`,
`src/test/resources/armour_policy/pages15_batch6_new_pages.json` and this file.

## Pages

One table row per page, exactly this shape (the test `Pages15SourceScanTest` reads it): book, id, name, tier (starter / mid / zone / signature), origin (`original`, or
`canon: <source>` naming the English wiki page, the Japanese wiki page, or the chapter / episode; say which wiki each came from), then free columns (what it does, the shape and effect it reuses, worst case).

| Book | Id | Name | Tier | Origin | What it does | Reuses | Worst case |
|---|---|---|---|---|---|---|---|
| `book_compass` | `needle_flick` | Needle Flick | starter | original | Flicks one golden compass needle straight ahead; the first foe it touches takes a light hit. | bolt + COMPASS_NEEDLE | 1 bolt, 1 target, 2 packets |
| `book_compass` | `bearing_step` | Bearing Step | starter | original | Takes a bearing and steps up to 8 blocks forward along it, a short move without a hit. | dash + COMPASS_LOCK | no target, 2 packets |
| `book_compass` | `magnetic_pull` | Magnetic Pull | starter | original | Draws the foe under your crosshair in towards you with a magnetic tug and hits it lightly on the way. | custom (pull) + COMPASS_NEEDLE, COMPASS_LOCK | 1 target, 3 packets |
| `book_compass` | `true_heading` | True Heading | starter | original | The compass settles on true north: you run faster for ten seconds and your melee hits carry a little extra weight. | empower + COMPASS_FX2 | self, 1 follow packet |
| `book_compass` | `lodestone_snare` | Lodestone Snare | mid | original | A lodestone needle pins one foe in place: heavy slowness for a few seconds and a small hit (shares the control cooldown). | bind + COMPASS_NEEDLE | 1 target, 2 packets |
| `book_compass` | `cardinal_volley` | Cardinal Volley | mid | original | Four homing needles, one for each cardinal point, each seeking a different foe. | volley + COMPASS_NEEDLE | 4 bolts, up to 6 targets by hit rules |
| `book_compass` | `declination_wave` | Declination Wave | mid | original | A wave of bent magnetic force in front of you that hurts and shoves foes far off their line. | cone + COMPASS_FX1 | 6 targets, 2 packets |
| `book_compass` | `dead_reckoning` | Dead Reckoning | mid | original | Plots a straight course and drives a long needle along it, hitting everything on the line. | line + COMPASS_NEEDLE | 6 targets, 2 packets |
| `book_compass` | `safe_bearing` | Safe Bearing | mid | original | The compass points the way to safety: you and your allies nearby regain a part of their health. | healAllies | allies in 6 blocks, 1 packet |
| `book_compass` | `magnetic_storm` | Magnetic Storm | zone | original | A storm of scattered needles at the aim point for four seconds: foes inside are hit every half second and dragged slow. | field + COMPASS_FX3 | 8 targets, 8 pulses, 9 packets |
| `book_compass` | `meridian_lance` | Meridian Lance | zone | original | A huge needle along the meridian: a long, wide line that hits hard and shoves foes along it. | line + COMPASS_NEEDLE | 8 targets, 2 packets |
| `book_compass` | `cardinal_burst` | Cardinal Burst | zone | original | All four points of the compass fire at once in a ring around you, hurting and knocking back everything close. | nova + COMPASS_FX2 | 8 targets, 2 packets |
| `book_copy` | `borrowed_strike` | Borrowed Strike | starter | original | A copied swing: a short narrow sweep in front of you that hurts and nudges foes back. | cone + IMITATION_FX1 | 4 targets, 1 packet |
| `book_copy` | `echo_step` | Echo Step | starter | original | You step 7 blocks forward and leave an echo of yourself behind; a plain move, no hit. | dash + IMITATION_FX2 | no target, 1 packet |
| `book_copy` | `touch_and_learn` | Touch and Learn | starter | original | Touching foes close to you copies a little of their vigour: a small burst that hurts them and heals you slightly. | nova + IMITATION_FX1, leech | 4 targets, 1 packet |
| `book_copy` | `returned_blow` | Returned Blow | starter | original | For six seconds you copy every blow aimed at you: attackers take a little of it back and your own hits are stronger. | empower(thorns) + IMITATION_FX2 | self, 1 follow packet |
| `book_copy` | `delayed_echo` | Delayed Echo | mid | original | Hits one foe, then the copy of the strike lands again about a second later for a smaller hit. | custom (hit + later) + IMITATION_FX1, FX3 | 1 target, 2 hits, 4 packets |
| `book_copy` | `mimic_fan` | Mimic Fan | mid | original | A fan of copied force in front of you; foes it touches are hurt and weakened for four seconds. | cone + IMITATION_FX3 | 6 targets, 1 packet |
| `book_copy` | `imitated_fetters` | Imitated Fetters | mid | original | Copies the bindings of other mages and wraps one foe in them: heavy slowness and a small hit (shares the control cooldown). | bind + IMITATION_FX2 | 1 target, 1 packet |
| `book_copy` | `copycat_pounce` | Copycat Pounce | mid | original | You blink behind the foe you look at and strike it in the back as you pass. | dash(behind) + IMITATION_FX1 | 1 target, 1 packet |
| `book_copy` | `hall_of_echoes` | Hall of Echoes | zone | original | Echoes of every blow fill the aimed spot for four seconds: foes inside are hit every half second and slowed. | field + IMITATION_FX3 | 8 targets, 8 pulses, 9 packets |
| `book_copy` | `chorus_of_copies` | Chorus of Copies | zone | original | A choir of your copies sings at the aimed spot and blasts everything round it away. | nova + IMITATION_FX3 | 8 targets, 2 packets |
| `book_copy` | `perfect_reproduction` | Perfect Reproduction | signature | original | The strongest copy: a huge burst around you that shoves foes away and strips one beneficial effect from each, copying it for the moment. | nova + IMITATION_FX3, knock, strip | 10 targets, 2 packets |
| `book_dice` | `snake_eyes` | Snake Eyes | starter | original | Throws a pair of dice that burst in a small blast where they land and slow the foes they hit. | bolt + DICE_D6, DICE_FX1 | 4 targets, 2 packets |
| `book_dice` | `pip_toss` | Pip Toss | starter | original | Three dice tumble out in a fan, each stinging the first foe it meets. | volley + DICE_D6 | 3 bolts |
| `book_dice` | `lucky_seven` | Lucky Seven | starter | original | The dice come up seven: ten seconds of speed and luck, and your blows hit a little harder. | empower + DICE_FX2, luck, speed | self, 1 follow packet |
| `book_dice` | `dice_cup_shake` | Dice Cup Shake | starter | original | Shakes a cup of dice in front of you and spills them: a short cone that hurts and knocks foes back. | cone + DICE_FX3 | 4 targets, 1 packet |
| `book_dice` | `critical_roll` | Critical Roll | mid | original | Rolls a d20 against the foe you look at: a natural 20 hits for half again as much, a natural 1 barely scratches, anything else scales with the roll. | custom d20 + DICE_FX2 | 1 target, 3 packets |
| `book_dice` | `double_or_nothing` | Double or Nothing | mid | original | Rolls two dice: seven or more and a burst round you hits hard, otherwise it is only a small hit. | custom 2d6 + DICE_FX3 | 6 targets, 2 packets |
| `book_dice` | `bowling_roll` | Bowling Roll | mid | original | Rolls a heavy die along the ground in a line that hits everything on it and knocks foes aside. | line + DICE_D6 | 6 targets, 1 packet |
| `book_dice` | `snare_roll` | Snare Roll | mid | original | Throws a d20 at one foe and pins it where it lands: heavy slowness for a few seconds (shares the control cooldown). | bind + DICE_D20 | 1 target, 1 packet |
| `book_dice` | `reroll` | Reroll | mid | original | Throws the bad result away: clears up to four harmful effects from you and gives a few seconds of regeneration. | custom (cleanse) + DICE_FX1 | self, 2 packets |
| `book_dice` | `craps_table` | Craps Table | zone | original | A table of tumbling dice at the aimed spot for four seconds: foes inside are hit every half second and slowed. | field + DICE_FX2 | 8 targets, 8 pulses, 9 packets |
| `book_dice` | `rain_of_dice` | Rain of Dice | zone | original | Five homing dice rain out over your foes and knock them about. | volley + DICE_D6 | 5 bolts |
| `book_slash` | `ripper_cut` | Ripper Cut | starter | canon: en-wiki: Jack the Ripper (character page, listed as "Slash Magic: Ripper Cut" in character_spells/jack.json) | A quick narrow cut of mana blades in front of you, the Ripper's opening stroke. | cone + SLASH_WAVE | 4 targets, 1 packet |
| `book_slash` | `cross_slash` | Cross Slash | starter | original | Two crescents fly out one after the other, crossing in front of you like an X. | volley(2) + SLASH_WAVE | 2 bolts |
| `book_slash` | `blade_flurry` | Blade Flurry | starter | original | A spinning flurry of short cuts all round you that hits every foe within arm's reach. | nova + SLASH_FX2 | 4 targets, 1 packet |
| `book_slash` | `edge_guard` | Edge Guard | starter | original | Blades along your forearms turn every blow aimed at you: for six seconds attackers are cut back and your own hits are stronger. | empower(thorns) + SLASH_BLADES | self, 1 follow packet |
| `book_slash` | `crescent_barrage` | Crescent Barrage | mid | original | Five projected crescents fan out in front of you. | volley(5) + SLASH_WAVE | 5 bolts |
| `book_slash` | `severing_line` | Severing Line | mid | original | One long straight cut that slices everything on a 14 block line. | line + SLASH_WAVE | 6 targets, 1 packet |
| `book_slash` | `blade_cyclone` | Blade Cyclone | mid | original | A cyclone of blades round you that cuts nearby foes and lifts them off the ground. | nova + SLASH_FX3, lift | 6 targets, 1 packet |
| `book_slash` | `hamstring_cut` | Hamstring Cut | mid | original | A low slash that flies straight and cuts the legs of the foe it hits: heavy slowness for four seconds. | bolt + SLASH_WAVE, SLASH_FX1 | 1 target, 2 packets |
| `book_slash` | `parrying_blades` | Parrying Blades | mid | original | For three seconds your blades cut arrows and magic bolts out of the air before they reach you (at most three per step). | custom zone (small box) + SLASH_FX2 | self, 15 steps, 1 packet |
| `book_slash` | `final_slash` | Final Slash | zone | canon: ja-search: ジャック・ザ・リッパー ファイナル・スラッシュ (search summary of the Japanese pages; not a wiki spell page) | The last cut: a long, wide slash that severs everything on its line and throws foes aside. | line + SLASH_WAVE | 8 targets, 1 packet |
| `book_slash` | `reaper_field` | Reaper Field | zone | original | Blades dance round you for three seconds, hitting every foe that comes close, every half second. | field + SLASH_FX2 | 8 targets, 6 pulses, 7 packets |

## Research sources

(English wiki first, Japanese wiki as the fallback; list what could not be fetched.)

## Worst case of the batch (rule 11)

(Entities, packets per second, quads per frame, zones, targets: the figure per page is in `docs/magic/pages15_recipe.md`; state any page that goes beyond it and why it is still bounded.)

## New and replaced (rule 5)

(All pages of this batch are NEW; nothing is replaced. List any exception.)
