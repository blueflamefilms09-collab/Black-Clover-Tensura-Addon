# Light Magic and Bone Magic, remade after the Black Clover wiki (`magic.lightBoneJa`)

The owner asked for Light Magic and Bone Magic to be remade after the Black Clover wiki, English wiki first and the Japanese wiki as the fallback. This is what was built, what each spell is
based on, what each one costs the server and the client, and what could not be verified. Nothing here has been seen in a running game: only the preview tool, the type check and the pure
JUnit tests were run.

## Sources (and what could not be fetched)

* `blackclover.fandom.com` (English wiki) and `ja.wikipedia.org` could NOT be fetched from the build session: the fetch tool answered `ENOTFOUND` and the agent proxy answered `403` to the
  CONNECT. The repo's own wiki folder in the scratchpad was empty and `wiki_spells.json` only has five Light names and two Bone names.
* What was read instead are search extracts of the real pages (the search tool returns a summary of the page it found), so every wiki fact below is second hand:
  English wiki pages Light Magic, Light Sword of Judgment, Bright Judgment Whip, Lamp of Avior, Healing Ray of Light, Demon Light Magic, Bone Magic, Eternal Fangs, Bone Bullets, Demon Sword
  Dainsleif, Spatial Rupture, Zenon Zogratis, Lucius Zogratis; Japanese ja.wikipedia (Black Clover), the pixiv encyclopedia (Lumiere, Patolli, Zenon), the 必殺技辞典 spell dictionary and
  namu.wiki's Japanese spell list.
* The owner pasted the Light Magic spell list with the visual descriptions (offensive: Light Sword of Judgment, Bright Judgment Whip, Heaven-Splitting Flash, Light Shaft of Divine Punishment,
  Arrows of Judgment, Lamp of Avior, Lamp of Avior Gloria; supplementary: Healing Ray of Light, Absolute Monarch of Divine Brilliance). That list is the source of truth for the Light book.
* The wiki's Bone Magic list (from the search extract of the Bone Magic page): offensive Eternal Fangs and Bone Bullets, union Demon Sword Dainsleif and Spatial Rupture. Users: Zenon
  Zogratis (Eternal Fangs, Dainsleif, Spatial Rupture), Lucius Zogratis (Bone Bullets, and every Zogratis attribute he absorbed).
* The compound Light spells of the wiki (Elemental Quintet, Storm of Light Imperial Swords of Conviction, Demon-Dweller Sword: Protecting Light) need other magics' books and are NOT built.

## The spells

`Status`: NEW = added at the end of the book; REPLACED = the old page of the same spell keeps its page id and its place and now casts the wiki spell (the old code stays and is shown again with
`magic.lightBoneJa = false`); KEPT = the old page, unchanged. `Japanese` is shown in the page tooltip (`tools/lang/light_bone_ja.json`). A Japanese name marked `?` could not be read from a
fetched page (a back-translation).

### Light Magic (ten pages, mode order)

| # | page id | English | Japanese | romaji | source | status |
|---|---|---|---|---|---|---|
| 0 | `judgment` | Light Sword of Judgment | 審判の光剣 ? | Shinpan no Kōken | English wiki (extract: shards of light with four lines through each, thrown; users Patolli, Rhya); owner list | REPLACED (was three swords) |
| 1 | `divine_punishment` | Light Shaft of Divine Punishment | 神罰の光芒 | Shinbatsu no Kōbō | English wiki + ja.wikipedia extract (Patolli's strongest); owner list | REPLACED (was falling shafts on a marked foe) |
| 2 | `healing_ray` | Healing Ray of Light | 癒やしの光粒 | Iyashi no Kōryū | English wiki + ja.wikipedia extract; owner list (sphere of white light with a halo) | REPLACED |
| 3 | `lamp_of_avior` | Lamp of Avior Gloria | アヴィオールの灯・栄光 | Aviōru no Tomoshibi: Gloria | English wiki + pixiv encyclopedia extract (Lumiere); owner list (black-and-white manga burst) | REPLACED (was a lamp raining swords) |
| 4 | `light_speed` | Light Speed | 光速移動 ? | Kōsoku Idō | the Light Magic page text (speed is the attribute's trait); no spell page of that name | KEPT |
| 5 | `bright_judgment_whip` | Bright Judgment Whip | 裁きの光鞭 | Sabaki no Kōben | English wiki (a light Creation Magic spell) + pixiv encyclopedia extract; owner list | NEW |
| 6 | `heaven_splitting_flash` | Heaven-Splitting Flash | 裂天の閃光 | Retten no Senkō | English wiki (Rhya's copy list) + namu.wiki Japanese list; owner list | NEW |
| 7 | `arrows_of_judgment` | Arrows of Judgment | 審判の矢 | Shinpan no Ya | English wiki list + ja.wikipedia extract; owner list | NEW |
| 8 | `avior_lamp` | Lamp of Avior | アヴィオールの灯 | Aviōru no Tomoshibi | English wiki (the user surrounds themselves with light and launches it) + pixiv extract; owner list | NEW |
| 9 | `absolute_monarch` | Absolute Monarch of Divine Brilliance | 神聖なる光輝の絶対君主 ? | Shinsei naru Kōki no Zettai Kunshu | owner list only; namu.wiki calls a spell of Augustus 神聖な光の絶対王, so the kanji is a back-translation | NEW |

### Bone Magic (seven pages, mode order)

| # | page id | English | Japanese | romaji | source | status |
|---|---|---|---|---|---|---|
| 0 | `bone_spear` | Bone Bullets | 骨弾 ? | Kotsudan | English wiki (palm hole, spinning fragments, user Lucius); no Japanese page found | REPLACED (was Bone Spear) |
| 1 | `bone_armor` | Bone Armor | 骨の鎧 ? | Hone no Yoroi | ja.wikipedia extract (Zenon's bones block almost every physical blow); English wiki Zenon page (armour at full devil power); not a spell page | KEPT |
| 2 | `ossuary` | Eternal Fangs | 無間骨牙 | Mugen Kotsuga | English wiki + ja.wikipedia + 必殺技辞典 (chapter 235) | REPLACED (was the Ossuary line of spikes) |
| 3 | `rib_cage` | Rib Cage | 肋骨の檻 ? | Rokkotsu no Ori | old book, not a wiki spell | KEPT |
| 4 | `bone_rain` | Bone Rain | 骨雨 ? | Kotsuu | old book, not a wiki spell | KEPT |
| 5 | `demon_sword_dainsleif` | Demon Sword Dainsleif | 魔剣ダーインスレイヴ | Maken Dāinsureivu | English wiki (bone sword coated in offensive Spatial Magic) + 必殺技辞典 (chapter 310) | NEW |
| 6 | `spatial_rupture` | Spatial Rupture | 空間断裂 ? | Kūkan Danretsu | English wiki (flying slashes of Spatial Magic from the Dainsleif); the kanji is a back-translation of 空間を断裂する in the 必殺技辞典 text | NEW |

Judgment calls on the Bone replacements (rule 5): Bone Bullets (a volley of small bone fragments from the palm) replaces the Bone Spear (one thrown spear, the starter slot) and Eternal Fangs
(bones out of the body in every direction) replaces the Ossuary (a line of bone spikes, the signature slot). Both are the same kind of attack in the same slot, so they are replacements; the old
casts are one config switch away. Bone Armor, Rib Cage and Bone Rain are not wiki spells and are kept as they were.

## What each spell does (server decides everything; numbers are in `book/LightBoneJa`, all capped)

| spell | cast | hits |
|---|---|---|
| Light Sword of Judgment | 4 to 8 shards (by EP modifier) rise in an arc, one more thrown every tick at 6 to 9 blocks per tick (the fastest thing in the mod); at most 4 cross-slash props | 5 raw light damage each; a light hit ignores armour, cuts the spirit, only an Aura barrier halves it |
| Light Shaft of Divine Punishment | ring on the ground (follows a marked foe for 14 ticks), then a column, 3 pulses 6 ticks apart; radius 2.6 to 4.2 | 8.5 raw per pulse, the nearest 12 foes, lifts them |
| Healing Ray of Light | sphere on the ally you look at (or you), 3 pulses 12 ticks apart | 5 percent of max health per pulse through the heal law (50 percent cap and decay), one affliction burnt, up to 5 allies within 3.5 blocks share half |
| Lamp of Avior Gloria | 26 ticks of charge over you, then a burst of 14 blocks and a second weaker pulse 6 ticks later | 13 raw (falls off with distance), blindness 3 s, push; nearest 12 foes |
| Bright Judgment Whip | 3 lashes 4 ticks apart in a fan of -24, 0, +24 degrees, 13 blocks | 6.5 raw each, nearest 8 along the lash, drags non-bosses a little |
| Heaven-Splitting Flash | 8 ticks of charge, a beam of 28 to 46 blocks stopped by a wall | 15 raw to the nearest 12 along it, push |
| Arrows of Judgment | 12 to 20 arrows over a disc of radius 8 around the aimed point, landing over 20 ticks | 4.2 raw each in a 1.7 block blast; at most 24 foes touched per cast |
| Lamp of Avior | 8 ticks gathering in the hand, an orb at 2.6 blocks per tick (up to 40 blocks), a starburst of 6 blocks | 11 raw, blindness 2.5 s, push; nearest 12 foes |
| Absolute Monarch of Divine Brilliance | a giant behind you for 12 s (one per caster) | every second the allies within 9 blocks (up to 16) get resistance I and 1 percent healing; every 1.5 s the nearest foe within 14 takes 8 raw and a push |
| Bone Bullets | 3 to 5 bullets 2 ticks apart, 2.6 blocks per tick, 47 blocks, 5 degrees of spread | 4.5 raw each, half the armour ignored |
| Eternal Fangs | 16 to 24 fangs out of the body, three waves 6 ticks apart, 9 blocks | 5.5 raw per wave to the nearest 12 foes in line of sight, 40 percent of armour ignored, push |
| Demon Sword Dainsleif | the sword for 44 ticks, three arcs 6 ticks apart, 4.6 blocks, 110 degrees | 7.5 raw each, 90 percent of armour ignored (space is cut) |
| Spatial Rupture | 1 to 3 slashes in a fan, 1.9 blocks per tick, 23 blocks, piercing | 8.5 raw each, 90 percent of armour ignored |

Mana, mastery and cooldown come from the page tiers (`BookPage.starter/mid/zone/signature`) and the old laws (`BalanceLaw`, `MagicGear`, the mana law): the replaced pages keep the tier and
cooldown of the page they replace, the new pages are mid or zone (whip, lamp, flash, arrows, rupture) or signature (the monarch, Dainsleif) with cooldowns of 160 to 1800 ticks. No spell
touches the food bar (a source scan pins it).

## Cost on a public server (owner rule 11)

Caps (all in `LightBoneJa`, pinned by `LightBoneJaTest`):

* props (real entities) per caster: 40 at once, both magics together; a new prop past the cap ends that caster's oldest first. On the whole server: 160; past it a new prop is not made (the
  spell still hits, only the model is missing). Cleanup on death, logout and dimension change (`LightBoneJaBudget`); followers also end when their owner is gone.
* props one cast makes: swords 8 (+4 cross), arrows 20, fangs 24, bullets 5, slashes 3, whip 3, everything else 1 to 3.
* foes touched: 12 per pulse (8 per whip lash and Dainsleif swing, 6 per arrow landing, 24 per Arrows cast), always the nearest ones.
* target searches: once per pulse or per landing tick, never per tick and never per prop: shaft 3, healing 3 (+3 ally searches), Gloria 2, whip 3, flash 1, Arrows at most 9 (the landings of
  one tick share one search), fangs 3 (+ a line-of-sight ray per foe), Dainsleif 3, monarch one ally search a second and one foe search every 1.5 s. Flying parts are `SpellRuntime` bolts (the
  shared engine: one box query per bolt per tick, at most 8 swords for 9 ticks, 5 bullets for 18, 3 slashes for 12).
* delayed parts check the caster is alive, in the world and in the dimension the cast began in; every cast and delayed part is timed by the probe sections `s.book.light_ja` and
  `s.book.bone_ja`.
* network: no prop is moved by the server (flying ones are drawn by the client from their age and the speed in the param), so a prop costs one spawn packet and one remove packet; only the
  followers (the healing sphere, Gloria's lamp, the gathering lamp, the giant, the Dainsleif) send a movement packet in a tick when they moved. Effect packets (`VfxSpawn`) and the custom
  particles (`Fx`) go through the perf budgets (`maxEffectPacketsPerTick`, the particle budget) and to nearby players only. Worst case per cast: about 12 to 20 effect packets spread over 10 to 30
  ticks, 1 to 24 spawn packets.
* client: a prop draws at most 568 quads at full detail (the Absolute Monarch, the biggest; the preview report): fang 75, whip 235, healing sphere 420, Gloria 104, starburst 218, column
  144, beam 66, sword 19, arrow 16, Dainsleif 78, slash 10, bullet 18. The most quads of one busy cast near the camera: 24 fangs 1800, 20 arrows 320, 3 whips 705. The painters scale their
  segment and spike counts by distance (`LightBoneJa.lod`: full to 24 blocks, 0.7 to 48, 0.45 to 96, 0.25 to 128, nothing beyond) and by the video quality, allocate nothing while painting
  (static scratch arrays, textures looked up once, a source scan pins it) and ask for glow only through `SafeAdditive`. The effect that is big on purpose (the Gloria burst, the column) has a
  fixed quad count: Gloria at most 26 spikes of 4 quads plus two jagged flashes of 22 triangles, the column 3 cylinders of 24 sides and at most 2 rings.

None of these numbers was measured in a game; they are the worst cases that can be derived from the code and the preview report.

## Tests and checks

* `LightBoneJaTest` (pure): the two page tables (old ids first and unchanged, new ones appended, one row per page with a Japanese name and a source), every cap for every EP modifier, the
  prop budget decision, arrow landings and fang directions, the whip curve (no allocation), the speed in the param, the level of detail.
* `LightBoneJaDataTest` (reads the real files): the lang fragment names every page in English, kanji and romaji; every texture a painter asks for exists; the preview scenes cover every variant;
  the config switch and the armour policy's appended pages exist; painters allocate nothing and ask for no render type directly; every delayed part has a guard; no prop behaviour scans the
  world; nothing touches the food bar or a client class from common code; the books are rebuilt on the config switch.
* Self-test for the CI server smoke test: `magic.light_bone_ja` (`LightBoneJaSelfTests`, started from `LightJaProps.init`): the two registered books against the table, then every prop variant
  of both kinds on the real server, then three times the per-caster cap in props, which must stay under the cap, and every prop must end.
* Existing guards re-run in the pure shim and green: `ArmourPolicyTest` (it needed `new_pages` for the appended pages), `FxSourceScanTest`, `PerfSourceScanTest`, `ServerSafetyTest`,
  `SafeAdditiveSourceScanTest`, `SafeAdditiveTextureScanTest`, `ConfigFileNamesTest` (no new config file), `WaterRebuildTest`, `MagicRebuildRulesTest`. `tools/safe_additive/scan_textures.py`
  reports 0 offenders (no glow texture was added: the three new textures are opaque cutout textures in `textures/prop`, outside its scope).
* The type check (`tc_wt.sh`) reports no error in the new files, with a deliberate wrong call as the negative control.
* Previews: `docs/previews/light_bone_ja/*.png`, made with `python3 -B tools/render_preview/preview.py ljb_sword ljb_cross ...` (scenes in `tools/render_preview/scenes.d/light_bone_ja.json`).
  What they show: the golden-white shard with four lines and a trail, the X of two diamond blades, a long curling whip with a loop at the tip, the three-layer beam with bright points, the
  ring and the column with a pool of light, the falling arrow, the spiky lamp gathering and exploding in a starburst, the white healing sphere with halo rings and motes, the golden giant with a
  yellow rim, a crown, wings of light feathers and a punching fist, the black-and-white manga burst of Gloria, the bone fang with knuckles and barbs, the spinning bone bullet, the Dainsleif with
  a near-black violet blade, a violet glow and three swing arcs, and the black crescent of cut space with a violet edge.

## Files

New: `book/LightBoneJa` (pure tables, caps, math), `book/LightBoneJaKit` (props, projectiles, probes, particle themes), `book/LightBoneJaBudget` (prop caps and cleanup),
`book/LightJaArts`, `book/BoneJaArts` (the casts), `book/BoneBook` (Bone Magic's book class), `book/LightBoneJaSelfTests`, `prop/LightJaProps`, `prop/BoneJaProps`,
`prop/LightJaVariant`, `prop/BoneJaVariant`, `client/prop/LightJaPropPainter`, `client/prop/BoneJaPropPainter`, textures `prop/bone_ja_bone`, `prop/bone_ja_space`, `prop/light_ja_plate`
(`tools/gen_light_bone_ja_textures.py`), `tools/lang/light_bone_ja.json`, `tools/render_preview/scenes.d/light_bone_ja.json`, the two tests, this document and the previews.

Edited (additive): `NUConfig` (server config `magic.lightBoneJa`, default true), `PropKind` (two kinds appended), `PropRegistry` and `PropPainterRegistry` (two lines each), `LightBook` (the
remake list beside the old one, switched by the config), `ElementBooks` (`bone()` now builds `BoneBook`; the old page list moved into `legacyBonePages()` unchanged),
`data/nusmp/armour_policy.json` (`new_pages` of LIGHT and BONE), and the preview tool (`tools/render_preview/preview.py`: the pure perf classes added to the compiled set, two stub classes,
`Preview.java`: an `xRot` scene field, `EntityRenderDispatcher` stub: `distanceToSqr`; the tool no longer compiled without them after the perf and safe-additive changes).

Nothing was deleted: every old page, its code and its id stay; `LightBook.lightHit` and `lightSpeed` are shared by both lists.

## Not verified (nothing was run in a game)

* None of the visuals was seen in the game. The preview tool is a software rasteriser of the real painter classes against stubs: no world, no depth against blocks, its additive blend is the
  plain eyes type (it shows hard edges on soft glow cards that the safe additive type softens in the game), and the arrow scene is far from the camera.
* Every Japanese name marked `?` above is a back-translation or a guess, not read from a fetched page; the wiki facts are from search extracts, not the pages. The owner's list was followed
  for the Light book; the Bone book's list is the wiki's four spells plus the old pages.
* Mana costs, cooldowns, damage and radii are first numbers from the existing tiers, not balanced by play.
* Flying props are moved by the client from the prop's age; a lag spike moves them against the server's hitbox for a moment (the hit and its effects stay the server's).
* The page tooltips are the remake's text for the page ids; with `magic.lightBoneJa = false` the replaced pages show the remake's tooltip over the old behaviour (a lang key cannot follow a
  config). The appended pages sit before the universal pages (Spirit Dive, Devil Union, Summon, Store, Draw, Mana Strike ...), so those pages' mode numbers move by 5 (Light) or 2 (Bone) when the
  switch is on, as they did when earlier books were extended; a player who has one of them selected when the server flips the switch may find another page selected.
* The Absolute Monarch's exact effect in the story is not described in the extracts read: it is built as the owner's visual description says (a golden giant) with a shelter and a strike.
* `MagicEnumAuditTest`, `MagicAuditTest` and the other tests that need the real Minecraft classes cannot run in the pure shim; they run in CI.
