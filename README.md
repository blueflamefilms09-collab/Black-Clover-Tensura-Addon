# Multiverse of Anime in Tensura

(internal mod id: `nusmp` - kept so existing skills and saves keep working)

Custom Tensura: Reincarnated skills for NeoForge 1.21.1.

| Skill | ID | Modes |
|---|---|---|
| Summoner (Allen / Hell Mode) | `nusmp:summoner` | Beast Card, Stone Card, Recall |
| Faith (skill bestowal) | `nusmp:faith` | Bestow, Pray, Check Faith |
| Firebolt (Bell, DanMachi) | `nusmp:firebolt` | Instant lightning-fire bolt |
| Argonaut (Bell, DanMachi) | `nusmp:argonaut` | HOLD to charge (bell chimes, up to 5), release to strike |
| Liaris Freese (Bell, DanMachi) | `nusmp:liaris_freese` | Passive: kills can raise mastery of all your skills |
| Airiel (Ais, DanMachi) | `nusmp:airiel` | Tempest (wind buff), Wind Dash |
| Hestia (god) | `nusmp:hestia` | Heal + bless nearby allies |
| Freya (god) | `nusmp:freya` | Charm: mobs drop aggro, enemies dazed |
| Hephaestus (god) | `nusmp:hephaestus` | Repair held item (+ armor when mastered) |
| Loki (god) | `nusmp:loki` | Vanish + blink forward |
| Wynn Fimbulvetr (magic) | `nusmp:wynn_fimbulvetr` | 3-chant ice blizzard at target |
| Rea Laevateinn (magic) | `nusmp:rea_laevateinn` | 3-chant flame pillars around you |
| Arcs Ray (magic) | `nusmp:arcs_ray` | 1-chant piercing light beam |
| Futsunomitama (magic) | `nusmp:futsunomitama` | 2-chant gravity field |
| Uchide no Kozuchi (magic) | `nusmp:uchide_no_kozuchi` | 3-chant Level Boost on an ally |

| Hell Walker | `nusmp:hell_walker` | Toggle: more damage the lower your health |
| Hawk Eye (Allen) | `nusmp:hawk_eye` | Toggle: night vision + hostile mobs glow |
| Spirit Card (Allen) | `nusmp:spirit_card` | Toggle: healing aura for nearby players |
| Unbreakable Will | `nusmp:unbreakable_will` | Passive: survive a killing blow (long cooldown) |
| Hati (Bete, DanMachi) | `nusmp:hati` | Passive: absorb fire/lightning/magic into Strength |

## Ultimate evolutions
Master the Unique skill and it evolves automatically (checked every 5 seconds). Faith points carry over.

| Unique | Evolves into | Ultimate ID |
|---|---|---|
| Summoner | Lord of Summons | `nusmp:lord_of_summons` |
| Faith | Goddess of Faith | `nusmp:goddess_of_faith` |
| Argonaut | Hero's Argonaut | `nusmp:heros_argonaut` |
| Hell Walker | Hell Conqueror | `nusmp:hell_conqueror` |
| Unbreakable Will | Immortal Will | `nusmp:immortal_will` |
| Devil's Footprints (Fire Force) | Adolla Burst | `nusmp:adolla_burst` |
| Book Maker (Medaka Box) | All Fiction | `nusmp:all_fiction` |
| Six Eyes (JJK) | Limitless | `nusmp:limitless` |
| Gold Experience (JoJo) | Gold Experience Requiem | `nusmp:gold_experience_requiem` |
| Vector Manipulation (Railgun) | Black Wings | `nusmp:black_wings` |

Standalone Ultimates (grant only): `nusmp:instant_death`, `nusmp:the_almighty`, `nusmp:the_visionary`.
God-tier balance: config `[god_tier]` (affectsPlayers, bossHealthThreshold, instantDeathBossShare, almightyDodgeChance).

Config `[ultimate_evolution]`: enabled, minimumEP, replacesBase.

Chanted magics: use **Chant** mode until the chant is complete, then switch to **Cast**. Mastery shortens the chant by one line.

## Build
1. Install **JDK 21** (Eclipse Adoptium Temurin 21).
2. Open a terminal in this folder and run:
   - Windows: `gradlew.bat build`
   - Mac/Linux: `./gradlew build`
   The first build downloads Minecraft, NeoForge and Tensura, so it takes a while.
3. The mod jar is in `build/libs/multiverse-of-anime-0.20.0.jar`.

## Install
Put the jar in the `mods` folder of the **server and every player's client**.

## Use
- Give skills: `/tensura edit <player> ability grant nusmp:summoner` (or `nusmp:faith`)
- Faith admin: `/nusmp faith get|set|add <player> [amount]`
- Settings: `world/serverconfig/nusmp-server.toml` (costs, cooldowns, max summons, bestowable skill list)

## Notes
- Hook signatures verified against Tensura 2.0.1.3 and ManasCore source (1.21.1).
- Written against Tensura 2.0.1.0 / ManasCore 4.0.0.2. Not yet compiled or tested: send build errors back to fix.
- Cooldown values are passed straight to Tensura's skill cooldown system; verify the unit in game and adjust the config.

## Black Clover grimoires
- After TR Nightmare gives a player a soul type, they roll ONCE: 1% five-leaf (+devil), 2% four-leaf, 10% three-leaf (config `[black_clover]`).
- Magic comes from the soul type (e.g. FLAME -> Flame/Explosion/Magma, WATER -> Water/Ice/Mercury/Mist). EMPTY souls can get any magic; an EMPTY five-leaf is Anti-Magic with Liebe.
- The grimoire is bound to its owner; if lost, using Grimoire Magic summons it back.
- Skill modes: Bullet, Burst, Ward, Cataclysm (four-leaf or mastered), Devil Union (five-leaf).
- Despair: a four-leaf owner has a small chance on death for their grimoire to darken into a five-leaf.
- Admin: `/nusmp grimoire info|roll|give|reset <player>` (give takes 3, 4 or 5 leaves).

## Look & creative tab
- Grimoires use Tensura's 3D grimoire model in your hand, and a custom icon (cover color per magic type, clover per leaf count) in inventories, on the ground and in item frames.
- Creative tab "Multiverse of Anime in Tensura" holds unbound grimoires of every type. Right-clicking one binds it to you (creative or operator only).

## Black Clover VFX (client)
Custom immediate-mode renderer (no vanilla particles): `com.newuniverse.nusmp.vfx`.
- Server: `VfxSpawn.send(level, VfxShape.X, from, to, color, duration, power)` (or `sendFollowing` to stick to an entity).
- Client: `VfxManager` + one layer per magic family (`MagicCircleLayer`, `FlameMagicLayer`, `WindMagicLayer`).
- Max 400 vertices per effect. Particles: All = full, Decreased = half detail, Minimal = vanilla dust only.
- Defaults per effect: `assets/nusmp/vfx/effects.json`. Textures: `assets/nusmp/textures/vfx/`.
- Test: `/nusmp vfx <shape> [power]` (ops).
- Adding a magic type: add a shape at the END of VfxShape, write a layer extending AbstractVfxLayer, register it in VfxManager.

## Grimoires: kingdoms, covers, pages (0.11)
- Acceptance rolls a kingdom (Clover 40 / Spade 20 / Heart 20 / Diamond 20) and a tier; the kingdom decides the cover.
- Covers: three/four/five-leaf, spade/double/triple spade, heart/two-heart/cracked heart, diamond/five-sided/cracked diamond. Each changes spell power and cost.
- Pages: every 100 kills (bosses count 10) roll 35% for a new page (+15% rare covers, -10% cracked). No pages left -> mastery instead.
- Time Magic pages: Chrono Stasis (starter), Chrono Stasis Grigora, Time Acceleration, Time Reversal, Stolen Time.
- Admin: `/nusmp grimoire setcover|addkills|page|info|give|roll|reset ...`

## Grimoires as Tensura Unique skills (0.13)
- Every ported family is ONE Unique skill (`nusmp:book_<family>`), pages are modes. Hold the grimoire, hold the skill key to chant (1 s, 0.4 s mastered), release to cast.
- Families: time, fire, water, wind, earth, light, dark, spatial, lightning, steel, mirror, thread, plant, sealing, gravity, ice, mercury.
- Cooldown tiers: starter 4 s, mid 8 s, zone 15 s, signature 30 s, daily one in-game day. Cooldown shows in the mode name.
- Spirit Dive (fire/water/wind/earth, Tensura Spirit Lord + free spirit slot), Devil Union (five-leaf/triple spade).
- Admin: `/nusmp grimoire spirit release <Salamander|Undine|Sylph|Gnome>`.
- Unported magic types (explosion, magma, mist, star, storm, sand, sword, shadow, poison, reinforcement, beast, bone, blood, creation, copy, illusion, dream, anti-magic) still use the legacy grimoire skill.

## 0.14
- Cooldowns are in SECONDS (ManasCore ticks them once per second) - everything was 20x too long before; fixed.
- Learning any grimoire book (acceptance, /nusmp, /tensura grant) now hands over the matching grimoire; if another mod deletes the book skill while you own the grimoire, it is re-taught.
- All 35 magic types are Unique books now. Mana rules: Mana Zone (chant twice as long), Mana Skin, exhaustion at low mana, Magic Stages, mana sensing for wind-derived mages, Mana Method for Heart.
- New VFX: Spirit Dive aura, mana charge, spell cards, weapon constructs.
- Magic gear: 11 robes, 10 relics, magic tool sword/spear/bow. Forbidden Magic via the Devil Contract (`/devilpact accept <maxhp|seal|tax>`).

## 0.16
- Robes are real 3D GeckoLib armor (my own models): robe / mantle / coat / simple / devil shapes with hoods, hats, horns, robe skirts and a swaying cape. Each set is 4 pieces (hood, robe, leggings, boots); the robe carries the bonus, the full set adds +50%.
- Iron's-style shaded icons for all gear.
- Swords: Demon Slasher Katana, Miasma-Infused Katana, Spell-Forged Rapier, Severing Greatsword (right-click techniques).
- Anti-Magic Spirit Lord (Unique): AMP pool, physique, Black Form; Demon-Slayer / Dweller / Destroyer swords bound to the owner. Awakened by mastering the Anti-Magic grimoire or `/nusmp grimoire awaken_anti <player>`.

## Spirit Lord companions
- Once a spirit chooses you (Spirit Dive), sneak-use a Spirit Charm to call your Spirit Lord: Salamander, Undine, Sylph or Gnome. Anti-Magic Spirit Lords call the Black Devil.
- Uses Tensura's own spirit models/animations by reference (`tensura:geo/entity/...`); nothing from Tensura is copied into this jar.
- Admin: `/nusmp grimoire spirit_lord <player> <salamander|undine|sylph|gnome|black_devil>` (run again to dismiss).

## 0.17
- Grimoire Altar block; super-rare Grimoire Towers (altar + loot chest at the top) and rare Library Ruins in the overworld.
- Spirit bond: 50 personality traits, Trust 0-100 from what you do, daily Over-Pull strain, Incarnation at 100 Trust (rename a Spirit Charm = True Name).
- Spirit Channeling page on Fire/Water/Wind/Earth books (hold: 25% Overdrive, 50% Spirit Nova, 100% Cataclysm).
- Updated 3D demon sword models.
- Admin: `/nusmp grimoire spirit_info <player>`, `/nusmp grimoire spirit_trust <player> <amount>`.

## 0.18 - spec-driven VFX (vfx.fx)
- Effects are pure-Java specs (phases in ticks, pieces, 3-6 colour palette, cues), built once and animated; rendered with the nusmp:fx shader.
- `gradle build` runs `EffectRulesTest`: budget 400/200 vertices, whole quads, life/phase containment, soft in/out, palette, vocabulary, jitter/stagger, 3D bursts, <=64 draws, flash<=1.8, flicker<=0.35, light shake, cues on phase starts. A failing effect blocks the jar.
- First batch: lightning_arc (Chain Lightning), fire_eruption (Calderos), water_crash (Sea Dragon's Roar), wind_gust (Gust Lane), earth_spikes (Earth Spikes).

## 0.19
- Spirit Channeling effects as rule-checked specs: spirit_aura (hold), spirit_overdrive (25%), spirit_nova (50%), spirit_cataclysm (100%), each in fire / water / wind / earth (payload colour = element).
- New Grimoire Altar pedestal model and top texture.

## 0.20 - procedural 3D grimoires
Every grimoire is now a real 3D book assembled at runtime from 32 greyscale textures + colour, instead of a flat per-variant icon. The old 2D model is kept as `models/item/grimoire_legacy.json` and every old texture is still in the jar.
- **Look data** lives in the `nusmp:appearance` data component (`GrimoireAppearance`): kingdom, crest, leaf count, cover material, trim metal, primary / trim / aura colours, page thickness, clasp. 16 bytes on the wire. Old grimoires get one automatically the first time the server sees them in an inventory.
- **Layers** (tintindex): 0 cover primary, 1 trim metal, 2 crest, 3 emissive aura. Crests: three/four/five-leaf, heart / two / tiered / cracked, spade / double / triple, diamond / dual / five-sided / cracked, Boundless (coverless, time), Forbidden Runes.
- **Geometry** (`GrimoireModelPlan`, baked by `GrimoireBakedModel`): thin / standard / tome / single-page bodies, corner brackets and spine bands per metal style, and clasps: open, single buckle, dual chains, padlock + key. Quads are built once per `GrimoireRenderKey` and kept in a 512-entry LRU; colours are not in the key, so recolours share geometry. Aura glow is full-bright.
- **Generator**: `GrimoireAppearance.fromGrimoireId(0..102999)` (or `fromSeed(long)`) maps an id to a unique crest / cover / metal / thickness / clasp / affinity combination (107,520 combinations, bijective scramble, so no two ids collide).
- **In game**: each owner's grimoire gets its own cosmetic variation (cover material, metal, thickness, clasp, colour drift) inside what suits its rarity; same owner + cover + magic always gives the same look. Config `[black_clover] uniqueLooks` (default true) turns this off.
- Creative tab: extra showcase grimoires for the crests / thicknesses / clasps no regular grimoire has.
- Admin: `/nusmp grimoire look <player> <0-102999>` and `/nusmp grimoire lookseed <player> <seed>` give an unbound grimoire with that look (its magic follows the aura affinity).
- Textures are regenerated with `python tools/gen_grimoire_textures.py` (needs Pillow + numpy). `gradle build` runs `GrimoireAppearanceTest` (103,000 unique ids, codecs, rarity rules) and `GrimoireModelPlanTest` (every geometry wound outward, inside the item cube, tints, texture budget <= 40). `GRIMOIRE_PREVIEW_DIR=build/preview gradle test --tests '*GrimoirePreviewTest'` renders contact sheets with a software rasteriser.
- **Canon look (Black Clover):** the kingdom insignia sits at the centre of the front cover and the binding reflects the owner's magic (so the cover colour is the magic's). Three-leaf = common: plain leather, simple border, gold clover. Four-leaf = rare: gold clover plus intricate gilded ornaments around the border (a wind four-leaf is Yuno's gold-and-green). A four-leaf that turns into a devil's five-leaf gets a darker cover, dark ornaments and a black clover; an Anti-Magic five-leaf is Asta's: black clover on filthy, tattered leather. Grimoires have no straps, chains, locks or corner caps, and a grimoire only glows (slightly) while it is held out, never in the inventory, on the ground or in a frame. Julius' coverless book is the Boundless crest. Spade / heart / diamond books use the same construction with their suit; the double / triple / cracked / five-sided variants are this mod's own inventions, not series canon. Clasps, chains, padlocks, dragon hide, runed trim and the full aura are kept for `/nusmp grimoire look` ids and the creative showcase.

## Grimoire shelf (client)
- `/grimoireshelf` toggles a floating shelf in front of you: the grimoires in your inventory, or the creative-tab showcase looks if you carry none. `/grimoireshelf close` closes it; there is also a "Grimoire Shelf" key (unbound by default, under Controls > Multiverse of Anime). It closes when you walk 6 blocks away, change dimension or log out.
- Client only: no entity, nothing sent over the network. Every frame `GrimoireShelfClient.onRender` asks `GrimoireShelfLayout.open(i, n, age)` (`core/magic/grimoire/`, pure maths) for each book's pose at `age = (game time - open time) + partial tick` and draws the book item there.
- Bob: `0.04 * sin(age * 0.12 + i * 7) * e` blocks (about 2.5 cm, one bob every ~52 ticks / 2.6 s, each book out of step), where `e` is the 0-1 opening ease, so a book only bobs once it has arrived. Height: `-0.6 + (rest.up + 0.6) * e + bob`.
- Books turn to face you (`Axis.YP.rotation(atan2(dx, dz))`); the one you look at tilts back 8 degrees and grows 22%.

## Time Magic look and passive
Drawn after the anime by `vfx/client/layer/TimeMagicLayer.java` (textures from `tools/gen_time_vfx_textures.py`, `textures/particle/time_*.png`).
- **Chrono Stasis**: a pale-blue glass sphere around the target, a white ribbon of Roman numerals orbiting it at a tilt, four-pointed sparkles inside. It follows the frozen target and lasts exactly as long as the freeze.
- **Chrono Stasis Grigora**: one stasis sphere on every frozen target (up to 16), plus the gold circle under you.
- **Chrono Anastasis** (new page, appended after Stolen Time so the other pages keep their numbers): a blue-violet clock face 12 blocks wide (16 mastered) opens over the spot you look at, with its hands sweeping backwards and gold light raining down. Under it, broken blocks from the last minute are rewound, you and your allies (every player when nobody is on a team) heal 25% of max health and are put out if burning, and enemy projectiles in the air are uncast. Cooldown 60 s.
- **Time Acceleration**: a dial at your feet with racing hands and two numeral ribbons whirling forwards. **Time Reversal**: a dial whose hands and ribbon run backwards.
- **Passive, Time Sense** (always on with the Time book): enemy projectiles within 4 blocks (6 mastered) that are flying at you get caught in a small stasis bubble and lose 60% of their speed, once each, at most 3 every 2 ticks. 3 s after you are hurt, 20% (35% mastered) of the damage taken in that window is rewound (healed, at most 4 hearts), shown by a rewinding dial at your feet; then it rests for 10 s.
- Test: `/nusmp vfx time_stasis|time_clock|time_rewind|time_accel [power]`.
