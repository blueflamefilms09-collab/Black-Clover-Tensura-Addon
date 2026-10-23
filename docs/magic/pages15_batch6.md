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

## Research sources

(English wiki first, Japanese wiki as the fallback; list what could not be fetched.)

## Worst case of the batch (rule 11)

(Entities, packets per second, quads per frame, zones, targets: the figure per page is in `docs/magic/pages15_recipe.md`; state any page that goes beyond it and why it is still bounded.)

## New and replaced (rule 5)

(All pages of this batch are NEW; nothing is replaced. List any exception.)
