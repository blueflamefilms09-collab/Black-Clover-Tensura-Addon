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
| `book_compass` | `magnet_draw` | Magnet Draw | starter | original | Draws the foe under your crosshair in towards you with a magnetic tug and hits it lightly on the way. | custom (pull) + COMPASS_NEEDLE, COMPASS_LOCK | 1 target, 3 packets |
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
| `book_slash` | `rending_cut` | Rending Cut | starter | original | A quick narrow cut of mana blades in front of you that rends whatever it meets. | cone + SLASH_WAVE | 4 targets, 1 packet |
| `book_slash` | `cross_slash` | Cross Slash | starter | original | Two crescents fly out one after the other, crossing in front of you like an X. | volley(2) + SLASH_WAVE | 2 bolts |
| `book_slash` | `razor_flurry` | Razor Flurry | starter | original | A spinning flurry of short cuts all round you that hits every foe within arm's reach. | nova + SLASH_FX2 | 4 targets, 1 packet |
| `book_slash` | `edge_guard` | Edge Guard | starter | original | Blades along your forearms turn every blow aimed at you: for six seconds attackers are cut back and your own hits are stronger. | empower(thorns) + SLASH_BLADES | self, 1 follow packet |
| `book_slash` | `crescent_salvo` | Crescent Salvo | mid | original | Five projected crescents fan out in front of you. | volley(5) + SLASH_WAVE | 5 bolts |
| `book_slash` | `severing_line` | Severing Line | mid | original | One long straight cut that slices everything on a 14 block line. | line + SLASH_WAVE | 6 targets, 1 packet |
| `book_slash` | `blade_cyclone` | Blade Cyclone | mid | original | A cyclone of blades round you that cuts nearby foes and lifts them off the ground. | nova + SLASH_FX3, lift | 6 targets, 1 packet |
| `book_slash` | `hamstring_cut` | Hamstring Cut | mid | original | A low slash that flies straight and cuts the legs of the foe it hits: heavy slowness for four seconds. | bolt + SLASH_WAVE, SLASH_FX1 | 1 target, 2 packets |
| `book_slash` | `parrying_blades` | Parrying Blades | mid | original | For three seconds your blades cut arrows and magic bolts out of the air before they reach you (at most three per step). | custom zone (small box) + SLASH_FX2 | self, 15 steps, 1 packet |
| `book_slash` | `final_slash` | Final Slash | zone | canon: ja-search: ジャック・ザ・リッパー ファイナル・スラッシュ (search summary of the Japanese pages; not a wiki spell page) | The last cut: a long, wide slash that severs everything on its line and throws foes aside. | line + SLASH_WAVE | 8 targets, 1 packet |
| `book_slash` | `reaper_field` | Reaper Field | zone | original | Blades dance round you for three seconds, hitting every foe that comes close, every half second. | field + SLASH_FX2 | 8 targets, 6 pulses, 7 packets |
| `book_gravity` | `gravity_jab` | Gravity Jab | starter | original | A short heavy jab in front of you: foes it hits are hurt and weighed down, slowed for two seconds. | cone + GRAVITY_FX1 | 4 targets, 1 packet |
| `book_gravity` | `ground_slam` | Ground Slam | starter | original | You bring your own weight down on the ground: a burst of crushing force that hurts and slows foes within reach. | nova + GRAVITY_IMPACT | 4 targets, 1 packet |
| `book_gravity` | `featherfall` | Featherfall | starter | original | Lets go of your own weight for ten seconds: no fall damage and a higher jump, and your blows stay a little heavier. | empower + GRAVITY_AURA, slow falling, jump | self, 1 follow packet |
| `book_gravity` | `pressure_lane` | Pressure Lane | mid | original | Presses a lane of heavy air along the ground in front of you: foes on it are hit and pinned down by slowness for three seconds. | line + GRAVITY_LANES | 6 targets, 1 packet |
| `book_gravity` | `inward_pull` | Inward Pull | mid | original | Draws every foe around the aimed point into a tight knot and hits them for half a blow. | custom (pull) + GRAVITY_WELL | 6 targets, 2 packets |
| `book_gravity` | `anchor_weight` | Anchor Weight | mid | original | Hangs an anchor of weight on the foe you look at: heavy slowness for a few seconds (shares the control cooldown). | bind + GRAVITY_LANES | 1 target, 1 packet |
| `book_gravity` | `weightless_burst` | Weightless Burst | mid | original | Takes the weight from foes round you for a moment: they are hit and thrown up into the air. | nova + GRAVITY_FX2, lift | 6 targets, 1 packet |
| `book_gravity` | `gravity_singularity` | Gravity Singularity | zone | canon: en-wiki: Gravity Singularity (repo copy .github/wiki/spell_gravity_singularity.txt) | A small black hole at the aimed point drags foes in for two seconds, then collapses and crushes everything it holds. | custom (zone pull + later) + GRAVITY_SINGULARITY | 8 targets, 10 pull steps, 1 collapse, 3 packets |
| `book_gravity` | `falling_weight` | Falling Weight | zone | original | Drops a huge weight on the aimed spot: foes under it are hit, shoved away and slowed for three seconds. | nova + GRAVITY_IMPACT | 8 targets, 2 packets |
| `book_game` | `health_potion` | Health Potion | starter | original | Drinks a potion and shares it: you and your allies close to you regain a part of their health. | healAllies | allies in 6 blocks, 1 packet |
| `book_game` | `warp_tile` | Warp Tile | starter | original | A warp tile under your feet throws you 8 blocks forward; a plain move, no hit. | dash + GAME_BOARD | no target, 1 packet |
| `book_game` | `button_mash` | Button Mash | starter | original | Four quick d20 shots in a row, each stinging the first foe it meets. | volley(4) + DICE_D20 | 4 bolts |
| `book_game` | `skip_turn` | Skip Turn | mid | original | The foe you look at loses its turn: frozen in place for a few seconds (shares the control cooldown). | bind(freeze) + WIKI_MAGIC_CAST | 1 target, 1 packet |
| `book_game` | `fog_of_war` | Fog of War | mid | original | Covers the map round you in fog: foes close to you are hit and blinded for three seconds. | nova + WIKI_MAGIC_BURST | 6 targets, 1 packet |
| `book_game` | `combo_chain` | Combo Chain | mid | original | A rush of hits as you move 9 blocks forward: everything on the way is struck. | dash + WIKI_MAGIC_CAST | 6 targets, 1 packet |
| `book_game` | `loot_drop` | Loot Drop | mid | original | Foes close to you drop their loot in a burst: they are hurt and you regain a little health. | nova + WIKI_MAGIC_BURST, leech | 6 targets, 1 packet |
| `book_game` | `boss_arena` | Boss Arena | zone | original | Locks an arena round the aimed spot for five seconds: foes inside are hit every half second and slowed. | field + WIKI_MAGIC_FIELD | 8 targets, 10 pulses, 11 packets |
| `book_game` | `random_encounter` | Random Encounter | zone | original | A game board opens under the foes at the aimed spot and each rolls its own d20: low rolls take a heavy hit, high rolls nearly dodge it. | custom (d20 per foe) + GAME_BOARD | 8 targets, 3 packets |
| `book_spatial` | `archangel_shootdown` | Archangel Shootdown | starter | canon: en-wiki: Archangel Shootdown (Spatial Magic navigation; Langris's spheres that erase what they touch) | Launches a sphere of spatial force that bursts where it lands and erases one beneficial effect from every foe it catches. | bolt + SPATIAL_FX1, FX2, strip | 4 targets, 2 packets |
| `book_spatial` | `rift_step` | Rift Step | starter | original | Steps through a rift 8 blocks ahead; a plain move, no hit. | dash + SPATIAL_RIFT | no target, 1 packet |
| `book_spatial` | `crevice_men` | Crevice Men | mid | canon: en-wiki: Spatial Magic navigation (name listed under offensive spells; the effect is the mod's) | Opens a crack in space along a line in front of you: foes on it are hit and slowed for two seconds. | line + SPATIAL_FX1 | 6 targets, 1 packet |
| `book_spatial` | `blackout` | Blackout | mid | canon: en-wiki: Blackout (marks areas and carries targets between them through black matter; here a group transport) | Black matter carries you and up to three allies within five blocks to the point you aim at (20 blocks at most). | custom (teleport) + SPATIAL_RIFT | up to 4 allies, 2 packets |
| `book_spatial` | `rift_burst` | Rift Burst | mid | original | Tears space open at the aimed point: a burst that hurts and throws foes away (the wiki's Spatial Rupture is already a page of the Bone book, so this one has its own name). | nova + SPATIAL_FX3 | 6 targets, 2 packets |
| `book_spatial` | `archangel_destruction` | Archangel Destruction | zone | canon: en-wiki: Archangel Destruction (Spatial Magic navigation; the stronger form of Shootdown) | Five homing spheres of erasing force seek your foes and strip a beneficial effect from each. | volley(5) + SPATIAL_FX1, strip | 5 bolts |
| `book_spatial` | `different_dimension_sphere` | Different Dimension Sphere | zone | canon: en-wiki: Different Dimension Sphere (Spatial Magic navigation; the effect is the mod's) | Seals one foe in a sphere of another dimension: held still for a few seconds (shares the control cooldown). | bind(freeze) + SPATIAL_FX2 | 1 target, 1 packet |
| `book_mirror` | `shard_shot` | Shard Shot | starter | original | A shard of mirror glass flies straight and shatters on the first foe it meets. | bolt + MIRROR_RAY, MIRROR_SHATTER | 1 target, 2 packets |
| `book_mirror` | `glint_flash` | Glint Flash | starter | original | A blinding flash off a mirror round you: foes close to you are hurt and blinded for two seconds. | nova + MIRROR_FX1 | 4 targets, 1 packet |
| `book_mirror` | `silver_backing` | Silver Backing | mid | original | A silver backing on your skin for eight seconds: damage taken is reduced and attackers are hit back. | empower(thorns) + MIRROR_FX2, resistance | self, 1 follow packet |
| `book_mirror` | `prism_split` | Prism Split | mid | original | A beam is split by a prism into three homing rays. | volley(3) + MIRROR_RAY | 3 bolts |
| `book_mirror` | `large_reflect_refrain` | Large Reflect Refrain | zone | canon: en-wiki: Large Reflect Refrain (Black Clover wiki, found through WebSearch) | Large mirrors bounce magical light between them and destroy what is in the way: a wide, long beam that shoves foes aside. | line + MIRROR_RAY | 8 targets, 2 packets |
| `book_mirror` | `hall_of_mirrors` | Hall of Mirrors | zone | original | A hall of mirrors at the aimed spot for four seconds: foes inside are hit every half second and slowed as they lose their way. | field + MIRROR_FX3 | 8 targets, 8 pulses, 9 packets |
| `book_mirror` | `mirrors_meteor_dragon` | Mirrors Meteor Dragon | signature | canon: en-wiki: Mirrors Meteor Dragon (Mirror Magic navigation, union spells; the effect is the mod's) | A dragon of mirror light runs 22 blocks along your aim and lands as a meteor at its end: everything on the line is hit, foes at the end take half a blow more. | custom (line + landing) + MIRROR_RAY, MIRROR_SHATTER | 10 targets, 4 packets |
| `book_time` | `stopwatch_strike` | Stopwatch Strike | starter | original | Starts a stopwatch on the foe you look at: a quick hit and a second of slowness as its time runs back. | custom + TIME_REWIND (existing shape) | 1 target, 2 packets |
| `book_time` | `borrowed_second` | Borrowed Second | starter | original | Borrows a second from tomorrow: you heal a little and move faster for three seconds. | custom (leech) + TIME_ACCEL (existing shape) | self, 2 packets |
| `book_time` | `frozen_instant` | Frozen Instant | mid | original | Catches the foe you look at in a frozen instant: a hit, then two seconds in which it can hardly move or strike. | custom + TIME_STASIS (existing shape) | 1 target, 2 packets |
| `book_time` | `dial_back` | Dial Back | mid | original | Turns the dial back on everything round you: foes are hit, pushed back where they stood a moment ago and slowed. | nova + TIME_REWIND (existing shape) | 6 targets, 2 packets |
| `book_time` | `slow_hour` | Slow Hour | zone | original | Five slow hours over the aimed spot: a clock face hangs there and foes inside are dragged slow and hurt every half second. | custom zone + TIME_CLOCK (existing shape) | 8 targets, 10 pulses, 2 packets |
| `book_legion` | `bishops_reach` | Bishop's Reach | starter | original | The bishop strikes along a long straight line: everything on it is hit. | line + LEGION_FX1 | 4 targets, 2 packets |
| `book_legion` | `queens_charge` | Queen's Charge | mid | original | The queen charges 9 blocks forward, striking and knocking back everything on her path. | dash + LEGION_FX2 | 6 targets, 1 packet |
| `book_legion` | `checkmate_field` | Checkmate Field | zone | original | A board of checks at the aimed spot for four seconds: foes inside are hit every half second and kept from running. | field + LEGION_FX3 | 8 targets, 8 pulses, 9 packets |
| `book_key` | `lock_pick` | Lock Pick | starter | original | A thin pick flies straight and springs the lock on the first foe it meets: it takes a hit and loses one beneficial effect. | bolt + KEY_FX1, KEY_FX2, strip | 1 target, 2 packets |

## Research sources

Order of rule 10: the English wiki first, the Japanese wiki as the fallback. In this sandbox the fandom pages cannot be fetched, so every look-up was a `WebSearch` summary; the repo copies of the English attribute pages (`.github/wiki/*.txt`) were read directly.

| Book | English wiki (what was found) | Japanese fallback | Result |
|---|---|---|---|
| Compass | repo copy `compass_magic.txt` lists only Useless North, Willful Compass, Another Atlas (all already pages) | none needed | 12 original pages |
| Copy (Imitation) | repo copy `imitation_magic.txt`: no spell pages listed | none found | 11 original pages (1 signature) |
| Dice | repo copy `dice_magic.txt`: only Gambler's Fallacy (already a page) | none found | 11 original pages |
| Slash | repo copy `slash_magic.txt`; WebSearch of the Slash Magic page: no spell list rendered. "Ripper Cut" is already a page of the Sword book (locked to Jack), so it is not made again | WebSearch (ja): Jack's technique "Final Slash" (ファイナル・スラッシュ), a summary only, no wiki page | 1 canon (Final Slash, from a Japanese search summary), 10 original |
| Gravity | repo copy `gravity_magic.txt` + `spell_gravity_singularity.txt` (Gravity Singularity: a small black hole that sucks in and destroys; controllable, grows as it moves away) | WebSearch (ja) gave no list of Dante's spells | 1 canon (Gravity Singularity), 8 original |
| Game | WebSearch: no Game Magic page or spell list found (only Painting Magic's God's Game / Sirène's Game turned up) | none found | 9 original pages, in the D&D style the book already uses |
| Spatial | WebSearch of the Spatial Magic navigation: Offensive: Myriad Black, Crevice Men, Archangel Shootdown, Archangel Destruction, Spatial Rupture; Supplementary: Fallen Angel Gate, Blackout, Different Dimension Sphere, Unopening Red Room, Fallen Angel Flapping, Sacred Mana Domination. Shootdown and Blackout have descriptions; Crevice Men, Archangel Destruction and Different Dimension Sphere were found as NAMES only (the effect is the mod's). Spatial Rupture is already a page of the Bone book (Zenon), so the burst here is called Rift Burst | none found | 5 canon (by name), 2 original |
| Mirror | WebSearch of the Mirror Magic navigation: Offensive: Reflect Ray, Reflect Refrain, Double Reflect Refrain, Large Reflect Ray, Large Reflect Refrain; Defensive: Full Reflection; Supplementary: Real Double, Mirrors Shift, Mirrors Brigade; Union: Mirrors Slash, Mirrors Meteorite, Mirrors Meteor Dragon. Large Reflect Refrain has a description; Mirrors Shift is what Mirror Step already does | none found | 2 canon (Large Reflect Refrain, Mirrors Meteor Dragon), 5 original |
| Time | WebSearch: Chrono Stasis, Grigora, Anastasis (all pages already); no further Time spells | WebSearch (ja): only an overview of Julius's time magic | 5 original pages (existing Time shapes only) |
| Legion | repo copy `legion_magic.txt`: all covered | none found | 3 original pages |
| Key | repo copy `key_magic.txt`: all covered | none found | 1 original page |
| Kotodama | already 16 pages, nothing to add | | 0 |

Could not be fetched at all: every fandom page directly (blackclover.fandom.com), the Japanese fandom / Wikipedia pages (only search summaries), the Game Magic page (no hit), the Slash Magic spell list (the navigation did not render), the Dante, Jack, Julius and Letoile spell lists in full. The canon marks above therefore name what the search summaries gave; spells whose effect was not described are marked in the table as "name only".

Totals: 86 pages, 9 canon (Archangel Shootdown, Crevice Men, Blackout, Archangel Destruction, Different Dimension Sphere, Gravity Singularity, Large Reflect Refrain, Mirrors Meteor Dragon, Final Slash) and 77 original.

## Character grimoires extended (`character_spells/*.json`, entries appended, no old entry changed)

| Character | Book | Visible before | Entries added | Visible now |
|---|---|---|---|---|
| Letoile | Compass | 3 | 12 (all new pages) | 15 |
| Jack | Slash | 5 | 10 (9 new pages and Final Slash) | 15 |
| Dante | Gravity | 8 | 7 (6 new pages and Gravity Singularity) | 15 |
| Finral | Spatial | 4 | 11 (7 new pages and the older Gate Swap, Gate Strike, Myriad Black, Dimensional Armour) | 15 |
| Langris | Spatial | 4 | 11 (7 new pages and the older Spatial Cut, Myriad Black, Unopening Red Room, Dimension Collapse) | 15 |
| Gauche | Mirror | 9 | 7 (all new pages) | 16 |
| Julius | Time | 6 | 9 (5 new pages and the older Hourglass Step, Borrowed Future, Pendulum Ward, Age Breaker) | 15 |

Every new entry is non-exclusive (the pages stay in the basic books), opens by mastery or by using pages of the set, never needs a rank beyond the tier's own page gate, and names its source (`design` where the mod chose the fit, `en_wiki` where the wiki names the spell). The older pages added to Finral, Langris and Julius are the mod's own pages: no source names them for these characters, which the note of each entry says.

## Worst case of the batch (rule 11)

Every page is one server cast of the kit: at most the tier's targets (4 / 6 / 8 / 10), at most 10 effect packets to nearby players, nothing per tick once it ends. Nothing in this batch is above the table in `docs/magic/pages15_recipe.md`. The custom casts and what bounds them:

* Magnetic Pull, Delayed Echo, Critical Roll, Double or Nothing, Reroll, Inward Pull, Stopwatch Strike, Second Wind, Frozen Instant, Random Encounter, Mirrors Meteor Dragon: one `Ctx` call each, targets from `ctx.target / foes / foesAlong` (capped by the tier), damage only through `ctx.hit` (clamped to 1.5 hits), at most 4 packets. Delayed Echo has one `later(14)` task (does nothing if the caster left or died).
* Parrying Blades: one `ctx.zone(60, 4)`: 15 steps, each scans a box of 3.5 blocks around the caster for projectiles and discards at most 3; the zone slot cap (2 per player) applies.
* Gravity Singularity: one `ctx.zone(40, 4)` (10 pulls, each `foes(at, 7)` capped by the tier) and one `later(44)` collapse; 3 packets.
* Slow Hour: one `ctx.zone(100, 10)` (10 pulses, `foes(at, 5)` capped, slowness 30 ticks, the zone pulse damage); 2 packets.
* Blackout: moves the caster and at most 3 allied players within 5 blocks to one point at most 20 blocks away (a no-collision check, same dimension); 2 packets.
* The 7 kit fields (`field`) run at most 100 ticks with a 10 tick step (at most 11 packets with the field circle), at most 2 kit zones per player.
* No entity is spawned, no block placed, no chunk loaded, no static state: nothing needs cleanup on logout.

Client draw cost is that of the shapes reused (they are already budgeted by `perf`). Nothing new was drawn: no new shape, prop, texture, packet or entity.

## New and replaced (rule 5)

All 86 pages are NEW; nothing is replaced and nothing is removed. The 7 character sets only had entries appended. Two ideas were dropped because the page already exists: "Ripper Cut" (Sword book, Jack) and "Spatial Rupture" (Bone book, Zenon); the pages made instead are Rending Cut and Rift Burst.

Time Magic: `TimeMagicLayer`, `textures/particle/time_*.png`, `tools/gen_time_vfx_textures.py` and `TimeBook.java` are untouched (rule 3); the five Time pages only pass the existing `TIME_REWIND`, `TIME_ACCEL`, `TIME_STASIS` and `TIME_CLOCK` shapes to the existing vfx call.

## Caveats

* Nothing was played in a game: the pages are type-checked against the API stubs and the pure tests pass, but no cast, animation, balance number or effect was seen.
* The Gravity Singularity pull moves foes with `setDeltaMovement`; Blackout teleports allied players: both are server-side and bounded, but untested on a real server.
* The dice pages do not show the rolled face on the die (the kit's effect call has no seed); the rolled number is shown in the action bar instead.
