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

**Ready-made jar:** every push is built on GitHub (`.github/workflows/build.yml`): open the repository's **Releases** page and download `multiverse-of-anime-<version>.jar` from the newest "Multiverse <version> (build N)". Put it in `mods` and delete any older `multiverse-of-anime-*.jar` there (an old jar is why a game can still show the flat pre-0.21 books).

1. Install **JDK 21** (Eclipse Adoptium Temurin 21).
2. Open a terminal in this folder and run:
   - Windows: `gradlew.bat build`
   - Mac/Linux: `./gradlew build`
   The first build downloads Minecraft, NeoForge and Tensura, so it takes a while.
3. The mod jar is in `build/libs/multiverse-of-anime-0.29.0.jar`.

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
- Pages open by mastery only (0.24: the old kill rolls are gone; see "0.24" below).
- Time Magic pages: Chrono Stasis (starter), Chrono Stasis Grigora, Time Acceleration, Time Reversal, Stolen Time.
- Admin: `/multiverse grimoire ...` (the old `/nusmp grimoire setcover|page|info|give|roll|reset` stays as a hidden admin alias).

## Grimoires as Tensura Unique skills (0.13)
- Every ported family is ONE Unique skill (`nusmp:book_<family>`), pages are modes. Summon the grimoire (0.24: it must be out), hold the skill key to chant (1 s, 0.4 s mastered), release to cast.
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
Replaced in 0.21 by the art-based grimoire below (the procedural look, its 33 textures, the 420 old 2D variant models and the `/nusmp grimoire look` / `lookseed` commands were removed at the owner's request). Old worlds still load: the old `nusmp:appearance` data is accepted and dropped the first time the server sees the stack.

## 0.21 - grimoires after the Black Clover art
Every grimoire is a 3D book built after the reference art: thick leather boards, a cream page block, a banded spine, a raised double gold frame, a corner / border ornament stamped on both covers, a raised medallion with the emblem (clover or suit) on the front and a rosette on the back. Pages and emblem glow only while the book is held.
- **Look** comes from the grimoire's own data (cover, magic, optional canon book, owner), so every old grimoire gets the new look automatically. Cover colour follows the magic, trim is gold / silver / bronze / dark, the emblem colour follows the cover. Each owner's book is shaded slightly differently (config `[black_clover] uniqueLooks`).
- **Ornaments by kingdom**: Clover three-leaf = filigree, four/five-leaf = ornate, Spade = wheels, Heart = floral, Diamond = lattice, Star magic = stars; Asta's anti-magic five-leaf is tattered. A devil's five-leaf / triple-spade is darker.
- **Canon books** (31): Fuegoleon, Mereoleona, Leopold, Yuno, Asta, Noelle, Nozel, Yami, Julius, Mimosa, Charmy, Finral, Vanessa, Luck, Gauche, Magna, Zora, Klaus, Kirsch, Karna, William, Langris, Lemiel, Zenon, Vanica, Dante, Lucius, Floga, Gadjah, Lolopechka, Mars. They are in the creative tab; using one (creative / op, no grimoire yet) binds it and keeps the look.
- **Admin**: `/multiverse grimoire canon <player> <book>` binds a canon book to a player who has no grimoire; add `copy` to hand out an unbound copy instead.
- **Code**: `grimoire/BookLook`, `BookPalette`, `BookMotif`, `CanonBook`, `GrimoireBookPlan` (pure, tested by `GrimoireBookPlanTest`); `client/grimoire/GrimoireBakedModel` bakes and caches one model per emblem x ornament x held. Tint layers: 0 cover, 1 trim, 2 emblem.
- **Textures**: `textures/item/grimoire_book/` (27 files), regenerated with `PYTHONHASHSEED=0 python tools/gen_grimoire_book_textures.py` (Pillow + numpy). Preview without the game: `python tools/grimoire_preview/preview.py` -> `build/grimoire_preview/books.png`.

## Grimoire shelf (client)
- `/grimoireshelf` toggles a floating shelf in front of you: the grimoires in your inventory, or the canon books if you carry none. `/grimoireshelf close` closes it; there is also a "Grimoire Shelf" key (unbound by default, under Controls > Multiverse of Anime). It closes when you walk 6 blocks away, change dimension or log out.
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

## Summon Grimoire (base ability of every grimoire)
- Every grimoire book has a new last mode, **Summon Grimoire**: free, instant (no chant). Press the ability key while your grimoire is in its **Grimoire Slot**: it floats up from your right hip to the front of your right hand (12-tick ease, a small arc), glows, and starts its idle bob. It counts as held, so you cast with your hands free.
- Put it away (0.22): press the ability key **while sneaking (shift)**; it floats back down to your hip. It also goes away on death, dimension change, logout or if it leaves the slot. Other players see all of it.
- Once it arrives it **opens into a V** like the Blender reference renders (three quick steps): spine and covers towards everyone else, the glowing pages towards you (in first person you look into the open book). On a stow it closes in front of your hand, then floats home (`GrimoireBookPlan.buildOpen`, `GrimoireCarry.openStep`; preview: `python tools/grimoire_preview/preview.py` -> `build/grimoire_preview/open.png`).
- Switching spells while it is out plays a **page flip** (3 pages, ~10 ticks) inside the open book, from the right-hand page block over to the left: parchment for most books; anti-magic / forbidden books have torn, soot-dark pages that tremble and throw red-black sparks; Flame-soul books singe with embers, Water-soul books shed droplets, Wind-soul books puff air, Earth-soul books shed dust.

## Grimoire Slot (0.22)
- A bound grimoire lives in a dedicated **Grimoire Slot**, not the hotbar: open it with the "Grimoire Slot" key (unbound by default), the button on the Multiverse status screen, or `/multiverse slot`. Only your own bound grimoire fits.
- While the slot is filled the book hangs **dormant at your right hip** (everyone sees it); an empty slot means no grimoire is carried. Summon Grimoire works straight from the slot; casting needs the book summoned (0.24).
- Any grimoire the mod gives you lands in the slot by itself (an owned grimoire anywhere in the inventory, hotbar or offhand moves in when the slot is empty). The slot is kept through death.
- **HUD cleanup**: names, titles and any other text floating above players' heads are hidden (client config `[hud] hideHeadText`, default on).

## Skill icons (0.23)
- `python tools/gen_skill_icons.py` builds three 32x32 icons for **every** grimoire magic (all 35 plus Forbidden), after the "Black Clover Skill Icon Generator" brief (dark fantasy anime spell icon, glossy and clean-lined, embossed rune borders, glowing magic colour, particles):
  - **Active / attack** `textures/skill/grimoire/<magic>.png` (the book's skill icon): midnight-blue aura, energy streaks, gold frame with clover-rune corners.
  - **Buff / rune** `textures/skill/icons/<magic>_buff.png`: ancient parchment, glowing rune circle, chain links, silver stone frame.
  - **Ultimate / forbidden** `textures/skill/icons/<magic>_ultimate.png`: black void, radial burst, layered magic circles with a star seal, black-iron frame lit in the magic colour.
  - Layers `bg_aura` / `bg_ancient` / `bg_void`, `sym_<magic>`, the archetype badges `mod_offense` (crosshair) / `mod_defense` (shield) / `mod_buff` (up-arrow) / `mod_debuff` (down-arrow), and `sheet.png` with everything.
- The floating spell card shown while chanting picks the icon by spell: Ultimate for signature-tier spells, Buff for defensive / buff / debuff spells, Active otherwise, with the archetype badge in its corner (`core/magic/grimoire/SpellArchetype`).
- `docs/skill_icon_prompts.md`: the brief's three image-generator prompts (Midjourney / DALL-E) for every magic, each naming the file it would replace, if you want hand-painted icons instead.
- Sneaking alone does not stow it (only sneak + the ability key), so sneaking (and Grigora's sneak-to-spare-players) still works while it floats.

## 0.24 - the brief: art-pack covers, slot-only grimoire, summon to cast, mastery-only pages
- **Covers from the art pack.** Every grimoire's covers now wear one of the owner's cover designs (`tools/art/covers`, `tools/gen_cover_art_textures.py`): the design's background shading tinted to the magic's colour, its ornament in the art's own colours, glowing while the book is held. Three-Leaf (blue, gold filigree), Four-Leaf (green, gilded vines), Five-Leaf (royal flourishes), Spade (crown of spears), Triple Spade (arcane wheels), Heart (cloud swirls), Two-Heart (sea serpent), Diamond (stained crystal), and the two new top tiers **Black Magic** (black book, blood-red burst, blood-red metal) and **God-Tier** (ivory and gold, golden sunburst, white-gold glow). Only Asta's tattered book and Karna's straps keep their own look. The procedural 0.21 ornaments (filigree, ornate, wheels, lattice, floral, stars, plain) are gone.
- **Rarity ladder** at the ceremony / altar: Three-Leaf -> Four-Leaf / kingdom covers -> Five-Leaf -> Black Magic (Anti-Magic, the devil Liebe; `weightBlack` 0.5) / God-Tier (`weightGod` 0.1).
- **Starter attributes**: a grimoire chooses Fire, Water, Wind or Earth (config `starterMagics`, narrowed to your soul family when it can be); Anti-Magic comes with the Black Magic cover or an empty soul. Every other magic stays in the mod (canon books, admin grants).
- **Grimoire Slot only.** A bound grimoire always lives in the Grimoire Slot (anything bound in your inventory, hotbar or offhand moves in by itself); it hangs dormant at your hip. Holding it or selecting it on the hotbar does nothing.
- **Cast only when summoned.** Press **G** ("Summon / Stow Grimoire", rebindable) or use the Summon Grimoire page: the book floats from your hip to your right hand and bobs; **shift + G** stows it. Every page needs the book out. *Anti-Magic exception:* the Anti-Magic Lord's Black Form toggle and the demon swords work without a summoned book (they are the swords drawn from it); the Anti-Magic book's own pages still need it out.
- **Pages by mastery only.** Kill rolls, the kill counter, `/nusmp grimoire addkills` and their configs were removed; Liaris Freese no longer feeds grimoire mastery from kills. A page opens when mastery >= its cost (the larger of its place in the book and its spell cost: cost% / 35), its rank / race gate passes and a slot is free (slots 6 -> 12, +4 with a spirit or devil contract, cap 16).
- **Anti-Magic kit**: new pages **Black Hurricane** (a whirling field that drags foes in and strips their magic) and **Black Form** (30 s of strength, speed, resistance and jump, with an anti-magic burst); mastering the Anti-Magic Lord now also grants the **Demon-Slasher** katana next to Demon-Dweller and Demon-Destroyer.
- **Altar**: a bound mage prays or trains with the grimoire in their slot (no need to hold it). A player who is not eligible no longer gets the old random roll there.
- **Player text**: no message tells players to use `/nusmp`, none names another mod; the creative tab is called "Multiverse". Squads without the team mod say so plainly ("team support is not installed").

## 0.29 - bigger grimoire
- The summoned book is **2x bigger** (in third person and in first person), and so is the dormant book on the hip. Both sit a little further out so the larger book clears the leg, the body and the crosshair.

## 0.28 - Black Clover weapons from the art pack; Anti Magic, Sword Magic and Yami's Dark Magic after the wiki
- **Weapons (replacements).** Demon-Slayer, Demon-Dweller, Demon-Destroyer, Demon-Slasher Katana and Yami's Katana (id kept: `miasma_infused_katana`) are redrawn from the weapons art pack (`tools/gen_weapon_textures.py`): one 128x128 sprite each, extruded to 3D in game, held large. Canon details: the Demon-Slayer's pointed tip, inward-angled base and fuller; the Demon-Dweller's four-sided guard, spiral grip, sphere pommel and markings; the Demon-Destroyer's wide blade end with the clover. The old layered element models (`textures/item/demon_swords/`, `demon_sword_display.json`) are gone.
- **New weapons.** Licht's white **Demon-Dweller** and **Demon-Destroyer** swords, drawn from a Sword Magic grimoire for a minute (they fade by themselves).
- **Sword techniques (right-click), renamed per canon:** Demon-Slayer **Black Divider** (bats spells back, huge sweep), Demon-Dweller **Black Slash** (flying slash, knockback), Demon-Destroyer **Causality Break** (cleanses allies, erases spells, strips foes), Demon-Slasher **Infinite Slash** (20-block anti-magic line), Yami's Katana **Dark Cloaked Dimension Slash** (16 blocks, darkness instead of wither), Licht's Dweller **Conquering Eon**, Licht's Destroyer **Causality Break**.
- **Anti Magic book:** Black Slash, Black Divider, Black Meteorite (charge the target), Black Hurricane (nullifies every spell nearby), Black Asta (replaces Black Form: twice a day, a third costs you), plus new **Bull Thrust**, **Infinite Slash** and **Infinite Slash Equinox** (spares allies).
- **Sword Magic book (Licht):** Origin Flash, Origin Flash Barrage, Demon-Dweller Sword: Conquering Eon (grows with allies near you, heals), plus new pages to draw Licht's Demon-Dweller / Demon-Destroyer Sword.
- **Dark Magic book (Yami):** Avidya Slash now flies, Black Hole swallows spells within 5 m and paralyses their casters, Dimension Slash cuts 18 blocks, Death Thrust needs Black Moon, plus new **Black Blade** (+2 reach), **Avidya Wild Slash**, **Black Moon** (Mana Zone: enemy spells vanish, allies' pass), **Iai Slash** and **Dimension Slash: Equinox**.
- Page ids are unchanged where a page was replaced, so unlocked pages and mastery carry over. Concept spec for every weapon (classification, visuals, abilities, Blender breakdown): `docs/weapons_spec.md`.

## Creative grimoires no longer overwrite yours
- Right-clicking an unbound (creative-tab) grimoire when you already have one now does nothing and keeps the item; reset first with `/nusmp grimoire reset <player>`. Binding also keeps the clicked book's cover (it used to fall back to the default cover).

## Fire, Water, Wind and Earth spell visuals (after the anime / wiki)
Layers in `vfx/client/layer/` (`FireSpellLayer`, `WaterSpellLayer`, `WindSpellLayer`, `EarthSpellLayer`, shared `ElementFx`), textures from `tools/gen_element_vfx_textures.py`. Old effects are kept; these books now use the new ones:
- **Fire**: Sol Linea = a spiralling flame spear; Calderos = Ignis Columna flame pillars; hits burst into flame. New page **Leo Rugiens** (appended last): a lion of flame bounds up to 20 blocks, burning what it runs through, and roars out in a blast where it lands.
- **Water**: Sea Dragon's Roar = a water dragon with a sinuous body flying to the target, splash crown on impact; Sea Dragon's Cradle = a whirling water sphere ringed by watery globs that follows you.
- **Wind**: Gust Lane = Swallow's Gale (swallows made of wind, streaks, rolling rings); Spirit Storm = a green-white tornado around you.
- **Earth**: Earth Spikes = 3D stone spikes erupting one after another; Earth Wall = cracks, dust and rubble along the wall; Mother Earth Split = a racing crack with stone slabs heaving up.
- Test any of them: `/nusmp vfx fire_lion|fire_spear|fire_pillar|fire_burst|water_dragon|water_cradle|water_burst|wind_tornado|wind_gale|stone_spikes|earth_rise|earth_fissure [power]`.
- Preview without the game: `python tools/vfx_preview/preview.py [scene...]` compiles the real layer code against small stubs and renders the frames to `build/vfx_preview/` (scenes in `tools/vfx_preview/scenes.json`).

## Multiverse systems (commands under `/multiverse`)
Server config: `world/serverconfig/nusmp-multiverse-server.toml`; client config: `config/nusmp-multiverse-client.toml`. The old `/nusmp` commands still work.

**Grimoire Acceptance Ceremony**
- Once a year (real-world March by default, or `/multiverse ceremony start|stop`) grimoires choose every eligible player who comes within 48 blocks of a Grimoire Tower. Eligible = no grimoire yet.
- Missed it? Pray empty-handed at any Grimoire Altar (top floor of every tower): a grimoire always answers there.
- Cover rarity when chosen: Three-Leaf / basic suit 80, Four-Leaf / Double Spade / Two-Heart / Five-Sided 16, Five-Leaf 3.5, Black (Anti-Magic five-leaf) 0.5 (config weights).
- Grants are transactional: the new grimoire item gets its own `GrimoireId` UUID and is handed over before anything is consumed (creative copies are used up only after binding worked).
- The old soul-type auto roll is kept behind `legacySoulAutoRoll` (off).

**Pages unlock by mastery only**
- Page k of n opens at k/n of the mastery bar, if its rank/race gate passes and a slot is free. Slots: 6, growing to 12 with mastery, +4 with a spirit or devil contract, max 16.
- Gates: zone pages need Intermediate 5th, signature pages Senior 5th; per-page overrides in `pageGates` (Chrono Anastasis Senior 3rd, Leo Rugiens Senior 5th by default).
- Mastery from casting (as before), altar training (sneak + use your grimoire on an altar, costs 10% magicule, 5 min cooldown), Spirit Channeling time, and gold stars (missions). Kill-based page rolls are kept behind `pagesFromKills` (off).

**Magic Knight rank, stars, social class, race**
- Everyone starts as a 5th Class Junior Magic Knight. Junior/Intermediate/Senior have 5th-1st classes; Grand Magic Knight and Wizard King are set by admins.
- Gold stars (merit) and black stars (demerit): with `autoPromote` every 10 net stars moves you up one class, up to 1st Class Senior.
- Social class (Royalty / Noble / Commoner / Peasant) is flavour. **Race (0.27)** is the player's **Tensura race**; the mod only adds a small buff on top of it by family and uses the family for page gates:
  Human (+5% attack, +1 luck), Elf (+8% speed, +2 health), Dwarf (+2 armor toughness, +15% mining speed), Beastfolk (+10% speed, higher jump), Ogre / Oni (+10% attack, +2 health), Goblin (+5% speed, +1 luck), Giant (+4 health, +20% knockback resistance), Orc (+4 health, +5% attack), Lizardman / Dragonewt (+2 armor, +5% attack), Merfolk (faster swimming, more air), Slime (half fall damage, +2 armor), Ghoul / Vampire (+5% attack and speed), Wight / Skeleton (+2 armor, +20% knockback resistance), Daemon (+8% attack, +5% speed).
  **Devil** is the mod's only own race and is **command only** for now: `/multiverse profile race <player> devil` (+10% attack, +2 armor, +5% speed), `... none` to return them to their Tensura race. The old Spirit-bonded and Hybrid races are gone.

**Magic Knight Squads (need FTB Teams)**
- A squad is an FTB Teams party flagged as a squad. Captain = party owner, Vice-Captain chosen by the captain, everyone else Member. Invites, leaving and ownership go through FTB Teams' own commands.
- Create needs Intermediate 5th, join Junior 5th, max 10 knights. Squad score = members' gold minus black stars (offline members count with their last known stars); `/multiverse squad standings` lists the Star Awards.
- Without FTB Teams, squad commands and the panel say "requires FTB Teams".

**Status panel (Tensura menu)**
- In the Tensura status / magic menu, the "Coming Soon" placeholder is replaced by the Multiverse panel (rank, stars, squad, grimoire, spirit, ceremony). Click it for the full status screen (rank, stars, squad, social class, race, grimoire pages/slots/mastery, spirit lord, anti-magic, ceremony).
- If the placeholder isn't a widget, a small "Multiverse" button opens the screen instead; `panelRect` in the client config places the panel exactly. There is also a "Multiverse Status" key (unbound by default).

**Spirit Lord (no grimoire needed)**
- The Spirit Lord Skill item (or `/multiverse spirit bond`) binds you to Salamander, Undine, Sylph, Gnome (one mage each) or the Anti-Magic lord and teaches the Spirit Lord skill: 30% less physical damage, no fall damage, Spirit Channeling 0-100% with Overdrive (25%), Nova (50%) and Cataclysm (100%), and Call Spirit once it is incarnated.
- Spirit Lord companions are now a small floating orb in the spirit's colour (the full Tensura body renderer is kept for later).

**Commands**
```
/multiverse grimoire give|page|awaken_anti <player> ...
/multiverse ceremony start|stop|status | accept|reset|legacy_roll <player>
/multiverse spirit bond <player> <type> | incarnate <player> <name> | info [player]
/multiverse antimode <player> <0-3>        (Dormant, Black Arm, Black Form, Devil Union)
/multiverse rank set <player> <rank> [class] | rank info [player]
/multiverse stars add <player> <amount> | stars info [player]   (negative amount = black stars)
/multiverse profile class|race|eligible <player> <value>
/multiverse squad create <name> | invite <player> | leave | info | standings | setcaptain <player> | setvice <player>
/multiverse locate tower|ruins
```
