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
| Wynn Fimbulvetr (magic) | `nusmp:wynn_fimbulvetr` | Instant ice blizzard at target |
| Rea Laevateinn (magic) | `nusmp:rea_laevateinn` | Instant flame pillars around you |
| Arcs Ray (magic) | `nusmp:arcs_ray` | Instant piercing light beam |
| Futsunomitama (magic) | `nusmp:futsunomitama` | Instant gravity field |
| Uchide no Kozuchi (magic) | `nusmp:uchide_no_kozuchi` | Instant Level Boost on an ally |

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

Magic spells fire when the skill key is released; holding the key does not trigger them early.

## Build

**Ready-made jar:** every push is built on GitHub (`.github/workflows/build.yml`): open the repository's **Releases** page and download `multiverse-of-anime-<version>.jar` from the newest "Multiverse <version> (build N)". Put it in `mods` and delete any older `multiverse-of-anime-*.jar` there (an old jar is why a game can still show the flat pre-0.21 books).

1. Install **JDK 21** (Eclipse Adoptium Temurin 21).
2. Open a terminal in this folder and run:
   - Windows: `gradlew.bat build`
   - Mac/Linux: `./gradlew build`
   The first build downloads Minecraft, NeoForge and Tensura, so it takes a while.
3. The mod jar is in `build/libs/multiverse-of-anime-0.97.0.jar`.

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
- Time Magic pages: Chrono Stasis (starter), Chrono Stasis Grigora, Time Acceleration, Time Reversal, Stolen Time, Chrono Anastasis, Hourglass Step, Borrowed Future, Pendulum Ward, Age Breaker, Moment Reprise and Hourglass Storm.
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

`node tools/gen_grimoire_icons.mjs` creates the distinct skill-menu icons for the newer wiki attributes using Node.js only; existing art is preserved unless `--force` is passed.

## 0.90.0 - Legion render crash and player-sized Zagred

- Fixed the Legion chessboard and chess-piece models reusing render buffers after switching render types, which caused a client crash while rendering the board.
- Scaled Zagred's existing detailed model to player height while keeping his horns, wings, claws, and tail; reduced the oversized shadow.

## 0.91.0 - Wizard King's legacy blade and grimoire finish

- Added Elsdocia as a modeled God-Tier sword: it absorbs magic on hit, stores up to 1,000 charge, and releases it in a spatial rift; the sneak technique requires a summoned Key Magic grimoire and drains more magic.
- Added the `Legacy of the Wizard Kings` Tensura engraving, applied with Elsdocia's other engravings when the sword enters a player's inventory.
- Added magic-specific grimoire cover sigils for the newly added wiki attributes and textured both sides of loose page flips.
- Added held spell buildup to grimoire magic; releasing the skill starts the cast windup, and a fully charged spell gains up to 20% size and damage.
- Brought Zagred close to player height and shortened his arm/forearm and leg proportions while preserving his horns, wings, claws, and tail.

## 0.95.0 - Riven Remake raised ceiling

- Riven now has four health phases, 1,200 base health plus 400 per additional arena player (capped at four), without phase-change healing. Movement speed rises 10% per phase; lethal casts have 0.7–1.4 second telegraphs and cast speed rises by 8% per phase.
- Riven's hits use the requested damage bands: phases I–IV normal 12–18 / 22–34 / 34–48 / 48–64 and signature 22 / 42 / 62 / 85. New signature entries: Page Tear, Severance Aria, Island Fall, Maw of the Rift and Final Page; their tells and hit effects use the existing VFX layers. Phase IV signature hits are separated by at least four seconds.
- Phase transition invulnerability lasts 1.5 seconds; anti-magic interrupts casting, staggers for 0.8 seconds and shortens a live null window. Added named phase II physical/magic null windows, phase III spatial-source null and phase IV rotating physical/magic null.
- Tensura EP is initialized at 300,000,000 through EnergyHelper / ExistenceStorage, its normal magicule pool is kept topped up, and Tensura's `no_max_ep_plunder` entity tag excludes Riven from EP plunder rewards. His existence is marked to skip EP drops.
- The boredom portal from 0.94.0 remains enabled.
- Not implemented or verified in-game: the full 15-move attack list, partial resistance bypass, Gold Ring null, Unwritten Ending, Rewrite Round, the exact 60-second rift pull and all requested shader-specific boss visuals. EP display, magicule scaling, and no-plunder behavior use Tensura APIs/tags but have not been exercised in a running game.

## 0.97.0 - Riven signature/VFX audit

- Added executable Eldritch Verse (three bolts with the third eight ticks after the first), phase-II Hexblade Waltz (three timed swings and a one-point reduction of Tensura's multilayer-barrier modifier), and made Shadow Step's afterimage repeat Page Tear at half signature damage.
- Signature casts now layer a dark arc-rune ground tell with Riven's violet-tinted Zagred-aura shader pass; signature hits use the Dark Slash shader and Barrier impact layer. Maw of the Rift uses the existing Demon Void shader on a floor disk, with the existing textured vortex fallback when that optional shader is unavailable.
- Soul Note now records damage actually removed from Riven's health/absorption, rather than the pre-mitigation incoming amount.
- Partial resistance bypass remains blocked: Tensura 2.0.1.0's `ResistSkill.isResistanceBypass` treats `tensura$getResistanceBypassLevel()` as a boolean threshold (`>= 1.0`), while `onTakenDamage` applies the configured resistance multiplier as a binary result. No supported partial reduction hook was found; using full bypass would violate the null/resistance rules.
- `nusmp:story_rift` is not registered in the worktree or reachable history, so the existing arena-local pull is retained. Rewrite Round still replays Riven's last cast because this combat path does not expose dependable player codex-skill attribution. Riven's current Bard relic drop remains; the historical `RivenPassives` implementation is not compatible with the current boss architecture.

## 0.96.0 - Riven attack roster continuation

- Added executable native attacks for Bull Ward (absorption shield and break shockwave), Soul Note (returns damage dealt by its marked player after five seconds), Legion Knight (construct spawn and position swap), Discord (42 damage plus two-second silence if its tell completes), Gold Ring (three-second magic null and delayed arena strike), Two Moons (second sky strike delayed ten ticks), Doom Gate (pull then close slash), Unwritten Ending (1.5-second untargetable window and 85-damage reappearance strike), Audience Collapse (25 story charge and 48 + 8 per additional living player), Crown Break (four non-tracking, block-occluded normal-damage shard rays), and one-use Rewrite Round.
- Phase II's Discord unlock is phase-gated. Phase IV signatures are separated by 80 ticks; Audience Collapse and Final Page are separated by 120 ticks. Anti-magic can shorten/cancel active invulnerability and cancels a pending Rewrite Round startup. Rewrite use persists when the boss is saved and reloaded.
- Fixed an Audience Collapse charge double-spend and kept Crown Break shards on the normal damage band. Added focused checks for the attack roster, phase unlocks, party damage formula, signature spacing, and normal Crown Break classification.
- Incomplete/unverified: Rewrite Round currently replays Riven's last cast, not a player spell that hit him; the damage-source API does not expose a reliable codex skill identity in this combat path. The 60-second pull is centered on the local arena, not an actual `nusmp:story_rift` dimension (no such dimension is registered in the current repo/history). The historical `RivenPassives` suite from commit `848f9de` is not integrated in this newer boss architecture; current boss-bound Tensura marker skills are granted and stripped on death, but are not themselves active passives. Signature resistance bypass and shader-specific boss VFX remain unimplemented; resistance bypass was not added without a safe Tensura API path. Runtime/in-game behavior has not been exercised.

## 0.94.0 - Riven Remake, the Black Bulls' Bard; generated grimoire designs; wiki VFX scale fixes

- **New boss: Riven Remake** (`/nusmp riven summon`, op level 2; spawn egg in the creative tab). An adaptive bard boss: every `replanTicks` (40) he scans his target (gear, armour, anti-magic / Nihility, grimoire magic, Tensura barrier and immunity skills, flying, mounted) and scores every skill of the Anime Skill Codex against it, commits to the best plan for up to four seconds and replans at once when the target changes gear, raises a barrier or pops anti-magic. Damage that does nothing teaches him to stop using that kind. Plans are logged at debug. Phases: I The Black Bulls' Bard, II Fictional Remake (70%, Story Manifestation constructs), III Final Form (30%, lethal skills x1.5, a tier up, two-skill chains). The quirk: a player with anti-magic, Nihility or a raised guard resists his rewrite (silence, slow, soul bond, song debuffs) unless he spends story charge. Health scales with players (`baseHealth` 600, `perPlayerHealth` 200), constructs +1 per extra player up to 4.
- **Anime Skill Codex** (`data/nusmp/skills/codex/*.json`, 38 skills: his own bard kit plus Black Clover, Tensura, DanMachi, Fire Force, Jujutsu Kaisen, JoJo and a generic pool). A skill is data only: a name, numbers and sandbox primitives (`SkillSandbox`); anything unknown is rejected at load. `/nusmp riven research <anime> <ability>` (off by default, `enableResearch`) runs off-thread against a configured https endpoint, never a private address, and only writes a candidate JSON into `config/nusmp/codex-inbox/`; `/nusmp riven accept <file>` moves it into the live codex. With no endpoint it just tells the op to drop a JSON file in the inbox. `config/nusmp-riven.toml` holds the settings.
- **Model, animations and boss bar.** A player-proportioned geo model (messy black hair, violet-blue eyes, black high-collar coat with silver filigree and the Black Bull skull, coat tails) with 18 clips (idle, walk, talk, cast_grimoire, cast_song, eldritch_blast, shadow_step, manifest weapon / shield, soul_bond, three sword combos, hit, stagger, phase2, final_form, death); Final Form swaps to a model of the same entity with page-wings and a bull-skull crown. His five-leaf-styled grimoire floats on his left. The boss bar is a custom overlay (crown, name, phase subtitle, violet-blue fill with a travelling lightning shimmer, a Black Bull skull that cracks at 70% and 30%, the casting skill typing on, a story-charge pip row from phase II, HP number and phase numeral); vanilla's bar for him is hidden. Assets come from `java tools/GenRiven.java`.
- **Generated grimoire designs.** Magics without a hand-made palette (all 30 wiki attributes fell back to one brown cover, gold trim and white glow) now get a cover, glow, metal and frame-stud pattern generated from their name (`GrimoireDesignGenerator`): stable per magic, visibly distinct, hand-made books unchanged.
- **Wiki-magic VFX fixes.** Fields and bursts were drawn at a third of their real radius (callers pass radius / 3); bolts drew a beam from the caster to the tip instead of a short tail and never finished for short effects; the guard aura was sized for the old scale. Bone and Sand no longer auto-spawn the shared construct wall props on every construct-shaped spell (Recombination keeps its walls).

## 0.92.0

- Version bump with a first `GrimoireDesignGenerator` stub (superseded by 0.94.0).

## 0.89.0 - Missing Black Clover attributes and Unique-skill icons

- Added standalone grimoires for Air, Hair, Memory, Mineral, Modification, Mucus, Mud, Nail, Permeation, Poison Plant, Red Ochre, Rock, Sandstone, Scale, Shakudo, Skin, Smoke, Snow, Song, Soul Corpse, Soul, Sound, Spike, Switching, Tongue, Tree, Stone, Vine, Vortex, and Wing Magic. Each has a distinct palette, five typed spell pages, and a Unique-skill menu entry.
- Added a shared layered VFX renderer for those attributes with visual families for wind, vines, stone/crystal, fluid, soul/space, snow, sound, and transformation effects.
- Added a dependency-free Node.js generator for their distinct Tensura Unique-skill icons.
- Imitation Magic is already represented by Copy Magic; the wiki's True-element attributes are upgrades of existing elemental magics and were not duplicated as standalone types.

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

## 0.62.0 - Instant casting, identity VFX, and stored Key Magic

- Changed: grimoire spells and crossover chant skills resolve when the ability key is released; holding the key does not cast early. Existing grimoire page ids remain in place.
- New: Star, Sand, and Mist have dedicated layered VFX; Slash has distinct cast, impact, and reaping effects. Bone and Blood spells now route through their existing attribute layers.
- New: Key Magic collects physical Magic Keys for Janus Abigail's extra gates; legacy stored-charge data is still read. Key pages use a key-turn cast pose.
- New: Legion has an appended chessboard toggle page, and Dice/Game grimoires use a looping dice-fiddle idle pose while summoned.
- Fixed: cast poses no longer rotate the torso root independently of the limbs, avoiding the visible body split.
- Existing: Key gate and Recombination construct visuals continue using their dedicated layers and summon implementations.
- The repository has no standalone Steal Magic type; stealing remains part of Key Magic.
- Build status: `gradlew.bat build` succeeded; in-game visual checks are still pending.

## 0.82.0 - Modeled magic constructs and robe upgrades

- New: Bone Spear/Ossuary, Sand Spear/Sandstorm, Slash Wave/Death Scythe, Recombination constructs, and Legion's soldiers/Gehenna Game board now use 3D model props and layered materials.
- Improved: junior and senior Magic Knight robes have a raised collar, shoulder mantle, tailored waist trim, and split coat tails; all existing robe items and set effects are preserved.
- Fixed: Legion's toggleable board refreshes independently of slow upkeep, its modeled pieces follow their soldier positions, and the board prop appears with Gehenna Game.
- Existing: Star, Bone, Blood, Sand, Mist, Recombination, Slash, and Key rune designs are generated with magic-specific palettes and glyphs.
- Note: there is no standalone Steal Magic attribute; stealing remains a Key Magic mechanic. In-game appearance and multiplayer rendering still need runtime verification.
- Build status: `gradlew.bat build` succeeded; in-game visual verification is still pending.

## 0.83.0 - Key textures, safe prop rendering, and release casting

- Fixed modeled magic props acquiring render buffers in an order that could crash the client when rendering Sand Magic.
- Added the physical Magic Key texture to the item atlas, and changed orbiting Key Magic visuals to smaller, fully textured Great Keys.
- Grimoire spells, crossover chant spells, and Firebolt now fire when the ability key is released rather than on press.

## 0.84.0 - Legion skill-menu toggle
- Legion Magic can now be toggled from the Tensura Unique-skill menu; the existing appended grimoire page uses the same toggle callbacks.
- The Legion render-buffer fix is included; the supplied crash log shows the game was still loading the older `0.82.0` jar.

## 0.85.0 - Zagred true-form detail and voice
- Added layered rib and spine anatomy, facial contours, horn and arm spurs, wing spars, and a defined tail spade to Zagred's existing model.
- Added processed synthetic voice lines for spell words, combat attacks, healing, Overwrite, and phase changes, with subtitles.

## 0.86.0 - Bone Magic textures and armor
- Fixed Bone Magic modeled props using a nonexistent vanilla bone-block texture; props now use the bone block's actual side texture.
- Added Bone Armor as a timed player render layer with textured rib plates, a spine, arm guards, shin plates, and a subtle magic glow.

## 0.88.0 - Zagred's louder, expanded battle voice

- Added over 50 original context-specific Zagred battle lines for Kotodama spells, phase changes, magic casts, adaptation, barrier breaks, lances, and defeat; the existing synthetic vocal clips accompany the lines.
- Increased Zagred's voice mix from 1.35x to 2.4x while retaining slight pitch variation and the existing sound cooldown to avoid constant overlapping playback.
- Rescaled Zagred's detailed devil body to player height and hitbox while retaining his horns, wings, claws, ribs, and tail; his open five-leaf grimoire now hovers and moves with his casting animation.

## 0.87.0 - Legion chessboard and Anti-Magic weapon passives
- Rebuilt Legion's board toggle as a following 3D chessboard and Gehenna Game as a circular, crimson-rimmed platform with a full modeled army; pieces distinguish pawns, rooks, knights, bishops, queens, and kings.
- Reduced Legion's repeat/ambient VFX while retaining spell impacts and queen signatures.
- Added a custom Anti-Magic Tensura engraving to the Anti-Magic swords. Anti-Magic-only passives now include projectile reflection, Demon-Dweller magic absorption and charged release, and the Demon-Destroyer / Slasher enchantment-stripping effects.
- Black Divider briefly enlarges the Demon-Slayer and extends reach for Anti-Magic users.
- Added a short post-release casting wind-up (4 ticks when mastered, 7 otherwise); spells still only begin after key release. Suppressed incidental Bone/Sand construct props on non-construct casts; Recombination constructs remain unchanged.

## 0.61.0 - Julius Time Magic expansion and client crash fix

- New: six appended Time Magic pages, preserving all previous page indices; the added spells use the existing balance, time-stop and VFX systems.
- Fixed: floating grimoire page rendering now reacquires the active render buffer when switching textures, preventing the reported `BufferBuilder: Not building!` client crash.

## 0.58 - Every new magic has a real grimoire; Ice and Dark VFX, sounds, summons

- New: all 29 new grimoires now hold about 12 themed pages each (Eye, Eyeball, Body and Legion may still be one page if their build did not finish; see the build notes), with page descriptions.
- Replaced: Ice Magic's water-looking effects are real ice effects; Yami's Black Hole, Black Moon, Death Thrust and Iai Slash have their own dark effects.
- New: sounds for the new effects (ice cracks, metal rings, gel squelches, beasts roar); summoned models and render layers for Beast, Key, Glass, Barrier, Mercury and the metal magics.
- Not verified: nothing was run in game.

## 0.57 - Real grimoires for the new magics, bug fixes

- Fixed: the new magics' skills showed raw ids (language entries were never merged); the red "Coming Soon" tooltip over the Four Kingdoms slot; the Spirit Lord skill stayed after losing the spirit or the Anti-Magic magic (it is now checked every 10 seconds).
- New: the new grimoires now hold about 12 pages each (themed: gel slows and holds, ice slows and freezes, ...), with page descriptions in the read-page menu. Done so far: Gel, Demon Ice, Ice Wedge, Bubble, Glass, Crystal, Demon Fire, Demon Water, Demon Light, Demon Beast, Curse, Curse Warding, Barrier, Key, Chain, Butoh, Cherry Blossom, Corundum, Bronze. Still one page: Iron, Copper, Food, Fungus, Black Oil, Briar, Eye, Eyeball, Body, Legion.
- Replaced: the new magics' cast and impact effects draw three times bigger (zones 1.5x).
- Not verified: nothing was run in game.

## 0.56 - Yami's dark slashes, Dimension Slash shader, Sealing amp, Secre's Seal Magic

- Replaced: the Yami slash spells (Dimension Slash, Equinox, Black Blade, Avidya, wild slashes, Death Thrust, Iai) now use new dark slash VFX instead of the generic wind slash. Sealing Chains and Trinity Seal are amped up; the Sealing magic has its own cast / field / impact effects.
- New: the Dimension Slash spatial-fracture shader (`rendertype_dimension_slash`, falls back to a sprite ribbon if it fails to load).
- New: basic Sealing "Sigil Combo" (random shape / tint / orb count) and Secre Swallowtail's grimoire spells (Branching Array, Eternal Prison, Wound Sealing, Orbital Bind), seal circle / cube props and the orbital aura.
- Not verified: nothing was run in game; the shader has never been loaded.

## 0.55 - New VFX for the new magics (built to the owner's art)

- Replaced: the one-billboard placeholder effects of the new magics now have three real effects each (cast / zone / impact shapes) with their own textures: Beast, Blood, Bone, Briar, Black Oil, Eye, Eyeball, Body, Demon Beast, Demon Water, Demon Fire, Demon Ice, Demon Light, Crystal, Curse, Chain, Gel, Barrier, Key, Cherry Blossom, Corundum, Butoh, Food, Copper, Iron, Ice Wedge, Bronze, Glass, Bubble, Fungus, Legion, Curse Warding (those that finished; see the build notes). Reference art is in `docs/attributes/art_reference/`.
- Not verified: only headless previews were rendered; nothing was run in game.
- Not done yet: the Sealing amp, the Sylph boss model, spells and auras of the new magics.

## 0.54 - Casting body animations, Yami's draw, store a weapon in the grimoire

- New: when you cast, the player raises a hand out, up or to the side (chant pose while the key is held, a release by page, a finisher for signature spells, a flinch when a spell fizzles). `docs/cast_animation_guide.md`.
- New: Yami's Grimoire gives the sword draw animation and Yami's katana, like Anti-Magic and Sword Magic.
- New: Store Weapon and Draw Weapon pages in every grimoire. `docs/grimoire_weapon_store.md`.
- Not done yet: the new VFX of the 29 new magics and the Sealing amp (blocked by the usage limit).

## 0.53 - Zagred nerf and summons that hurt him; the groundwork of the 37-attribute expansion
- **Replacement: Zagred's balance** (details in `docs/zagred_boss_gdd.md`, section 6).
  - **Summons can hurt him now.** A summon (Tensura summon, tamed or owner-bound creature, daemon, Real Double, painted construct) no longer bounces off Physical Attack Nullification: its blows count as arcane, wear the kinetic barrier layers, are never sidestepped, and credit their owner for the loot. His Banish only shoves summons now; it no longer sends them away.
  - **Softer defences:** barrier layers 4.5% of his health (was 6%) refreshing every 11 s for 35%; Thought Acceleration tokens 3 / 4 / 5 / 6 per act, 40% slower regeneration, dodges 60% of the time (was 75%); elemental resistances 0.65 / 0.75; adaptation leaves 35%; the act 4 shield lets 30% through; out-of-thought +35%.
  - **Softer attacks:** his word and letter damage is 20% lower and the counter-strike after a sidestep does 3.
- **New (groundwork, the spells come next):** the 29 new Black Clover attributes are registered with a book, a grimoire cover, a skill icon (placeholder symbol), three VFX shapes and a one-spell placeholder page each: Demon Beast, Body, Eye, Eyeball, Curse, Curse-Warding, Demon Fire / Ice / Light / Water, Barrier, Key, Chain, Butoh, Briar, Cherry Blossom, Fungus, Food, Crystal, Corundum, Bronze, Copper, Iron, Black Oil, Gel, Glass, Bubble, Ice Wedge, Legion. Curse, the Demon magics and Demon Beast are command or admin only, like Anti-Magic. Eight existing magics (Beast, Blood, Bone, Gravity, Sealing, Imitation, Ice, Light) get their new pages appended in the next build; nothing of them changes now.
- **New: two generic render systems for those attributes.** `PlayerAuras` draws a custom layer over a posed player (additive spectral auras, translucent ethereal bodies, entity-cutout muscle or bone overlays, bubbles), synced to everyone nearby; `MagicProps` is a networked 3D entity (never saved) with server behaviours and client painters built from boxes, spheres, cylinders, tori, crystals and tubes (beast bodies, eyes, keys, chains, crystals, shields, gel, bubbles, constructs). Authoring guide: `docs/attribute_authoring_guide.md`; the table of the 37: `tools/attribute_table.py`.
- **Tools:** `tools/gen_attribute_icons.py` (icons for the new magics only, never touches the old set), per-attribute preview scenes in `tools/vfx_preview/scenes.d/`.
- **Not verified:** nothing was run in game. Whether the summons' damage numbers feel right against him, the nerf values, and that the 29 placeholder magics register and roll cleanly are the things to check; the real spells, effects and render layers of the 37 attributes arrive in the next build.

## 0.52 - Zagred as designed, better weapon abilities, Cotton sheep and cloud, the grimoire sword draw, fixes
- **New: Zagred's defences and acts** (design: `docs/zagred_boss_gdd.md`).
  - **Nullifications:** plain physical blows, harmful effects and spiritual attacks do nothing (a one-line hint tells attackers). Mythical weapons (this mod's swords, conceptual damage, very high EP), magic and anti-magic get through. Holy and demonic damage is halved, the natural four are at 60%.
  - **Multilayer Barrier:** 3 layers in phases 1 and 2, 4 in phase 3, 5 in phase 4. Each layer takes full damage from its own grain and 40% from the other; anti-magic pierces at 150%. Layers refresh on their own. When all are gone he rewrites them for 3 s and takes 150%. Shown on the boss bar.
  - **Thought Acceleration:** he sidesteps projectiles and single blows with a counter, spending tokens (shown on the bar). Two threats at once, bait, anti-magic or a far hit beat it; at zero tokens he takes 20% more for 2 s.
  - **Acts:** think speed, wind-ups, cooldowns and move speed change at 50% and 25%, with a 3 s invulnerable transition; one burst cannot skip an act; a 40-block leash; later acts target the strongest caster, then the player who hurt the barrier most.
- **New: Redact and Overwrite.** Redact writes a sentence of glyph letters on the floor that detonates in reading order (an anti-magic swing on a letter cuts the rest); players can speak it too (creative or earned). Overwrite writes one rule on the arena for 12 s (No fire, ice, lightning, flying, standing still or healing); destroying three of the five rule stones cuts it. Halt is now a 12-block ring for 1.5 s; Fall now ends in a low crushing ring you can jump.
- **New: Grimoire Daemons** (lesser, greater, arch) with their own model and skins. Arch Daemons arrive at 50% and 25% and anchor his barrier.
- **New: Zagred's drops.** Heart of Words for everyone who hurt him, plus one of Last Word (quill-blade, Barrier Piercing II and Severance I), Shroud of Margins and Circlet of Quickened Thought, with no repeats until all three have dropped.
- **Replacement: the other swords' abilities.** Every Black Clover sword, the trident and Last Word get what the Demon-Slayer has: a full-swing strike on top of the normal blow, and a sneak + right-click second technique on its own cooldown (Black Dash, Dark Cloaked Eclipse, Spatial Barrage, Severing Quake, Black Hurricane Cut, Causality Collapse, Eon Burst, Light Verdict, Absolute Zero, Long Sentence, Void Rain). The right-click techniques are unchanged. Tooltips list them.
- **Replacement: Cotton Magic's sheep.** Sleeping Sheep Strike, Sheep Cook and Sheep Bondage now summon real sheep entities (tall, fluffy and upright like the anime; cooks wear a chef's hat and a blue neckerchief) and a new **cotton cloud** entity instead of the billboard sheep VFX. Cotton Cloud is a cloud you ride (walk to steer, sneak to step off, up to four riders). The old VFX shape stays registered, unused.
- **Replacement: the Grimoire Sword Draw.** Sword Magic's sword pages and the Anti-Magic sword awakenings now play a 1.2 s draw: the grimoire opens at the hip, the arm reaches in and pulls the sword out; it lands in the hand at tick 12. Guide and keyframe table: `docs/grimoire_sword_draw.md`.
- **Fixes (your list):** the two katanas' blades were facing the wrong way in hand (mirrored); the Otherworld Trident is 65% bigger in hand; the Artist's Palette now faces the player in the off hand.
- **Not verified:** none of it was run in game. The sword-draw numbers, the cloud ride feel, the new fight's balance, which Tensura engravings exist in your version, and whether the mixin applied are the things to check first.

## 0.51 - Zagred boss design document
- **New: `docs/zagred_boss_gdd.md`** (design only, nothing in the game changes). It expands Zagred into acts, with four Word Soul attacks (Halt, Redact, Fall, Overwrite), the 50% and 25% phase changes, three drops with engravings, and the code logic for the Multilayer Barrier and Thought Acceleration. It also lists what is new and what already exists.

## 0.50 - every weapon and relic in 3D (the Demon-Slayer standard), Tensura engravings
- **Replacement: the in-hand look of 12 weapons and 11 relics.** Each is now a real 3D model like the Genesis Demon-Slayer; the inventory keeps its old icon.
  - **Weapons:** Demon-Slasher Katana, Miasma-Infused Katana (Yami), Spell-Forged Rapier, Severing Greatsword, Demon-Dweller, Demon-Destroyer, Licht's Demon-Dweller and Demon-Destroyer, Rimeheart Runeblade, Otherworld Trident, Magic Tool Sword and Spear.
  - **Relics:** Communication Magic Device, Mana-Method Rune Stone, Spirit Charm, Bond Thread, Fortune Die, Grimoire Chain, Anti-Bird Charm, Recovery Salve, Written Consent, Devil Contract, Gauche's Hand Mirror.
  - **Shapes:** bevelled blades with a ridge, curved katanas with a hamon or a jagged red edge, clover cross guards, tsuba, extruded pixel guards, wrapped grips.
  - **Effects:** glowing runes, edges and gems that pulse; Asta's demon swords show the crimson void through their notches and broken patches.
  - **Guide:** built by `tools/gen_weapon_models.py`; preview with `python tools/item_preview/preview_weapons.py`.
- **New: Tensura engravings on every weapon** (`item/WeaponEngravings.java`). They are applied once, the first time the weapon is in a player's inventory, and capped at each engraving's max level:
  - Demon-Slasher: Barrier Piercing, Swift.
  - Miasma Katana: Severance, Enervation.
  - Rapier: Swift, Magic Weapon.
  - Severing Greatsword: Severance, Crushing.
  - Demon-Slayer: Barrier Piercing, Magic Interference.
  - Demon-Dweller: Barrier Piercing, Energy Steal.
  - Demon-Destroyer: Magic Interference, Sturdy.
  - Licht's swords: Holy Weapon, plus Elemental Boost (Dweller) or Barrier Piercing (Destroyer).
  - Rimeheart: Elemental Boost, Magicule Absorption.
  - Otherworld Trident: Soul Eater, Elemental Boost.
  - Magic Tool sword, spear and bow: Magic Weapon.
- **Fix:** Black Meteorite (shift + use on the Demon-Slayer) is no longer spammable in creative; it keeps a 30 s creative cooldown.

## 0.49 - Zagred after the reference art, Game Magic, mob skill casting, smarter summons, more spells
- **Replacement: Zagred's look, reshaped to the owner's two images.**
  - **Body:** very tall and thin and black, with a pale long face, red eyes with dark streaks, long ears, and messy black hair with two horns.
  - **Chest:** a pale neck and ribcage in a V.
  - **Limbs:** long thin arms with long curved claws, very long legs on clawed feet.
  - **Wings and tail:** huge dark wings and a whip tail curling round to the front.
  - **Effects:** black flakes and smoke shed from the shoulders, and a crimson aura (no more violet).
  - **Hitbox:** now 3.6 blocks tall.
- **New: mobs cast Tensura's own magic** (`entity/TensuraCaster.java`).
  - Zagred learns up to five dark, void or elemental Tensura spells and casts them between its words. Each cast is telegraphed.
  - A Tensura skill that can't be cast by a mob is dropped from its kit with a log line.
- **New: smarter summons** (painted constructs, mirror doubles, spirit lords).
  - **Targeting:** they pick targets by threat: whoever attacks their owner first.
  - **Casting:** they cast Tensura spells or a mana bolt. Mirror doubles hold casting range.
  - **Healing:** they heal their owner below 40% health.
- **New: Game Magic (Gifso).** Pages: Game Board, Monster Toy, Temple Shuffle, Initiative, Dungeon Master's Verdict, Trap Squares. D&D d20 rolls throughout. Guide: `docs/game_magic_spec.md`.
- **New: random painted beasts.** Each Painted Menagerie or Monster Toy beast is a random wolf, panther, bear, boar, griffin, dragon or lion, or a chimera mixing heads, builds, wings, manes, spikes and tails. Stats and on-hit effects match what it was drawn with.
- **Replacement: Rouge.** Rouge is now a red-thread cat model sitting on her summoner's head, seen by everyone nearby, instead of the old VFX.
- **New: 26 spells for the thinnest magics:** Ice, Gravity, Steel, Magma, Star, Sand, Reinforcement, Beast, Bone, Blood, Copy, Illusion. They are appended, so existing pages are unchanged.
- **Replacement: the anti-magic slash VFX** (every demon sword): a black crescent, a coloured rim, afterimages, torn flakes, a shock streak and an end crack.
- **Fix:** the Time Magic passive (Reversal, Deceleration) now ticks every tick. The Time VFX are untouched.

## 0.48 - Genesis Demon-Slayer, Word Soul counter-words, Zagred's true form, global buff, gamerules
- **Replacement: the Demon-Slayer Sword is now Genesis-grade** (same item id; Black Divider kept). Guide: `docs/demon_slayer_genesis_spec.md`.
  - **Conceptual Severance:** a full swing does EP-scaled true damage that ignores armour, enchantments and hit cooldowns, plus spiritual damage.
  - **Unbreakable.**
  - **Nullification field (15 blocks, while held):** foes' Ultimate skills jam, their magicules bleed away, and you can't be time-stopped.
  - **Black Meteorite — Void Severance (sneak + right-click):** locks on up to 100 blocks, charges as a meteor, cuts twice, and leaves a Nihility zone that shatters barriers.
    - Costs 80% aura, with a 2-minute cooldown.
    - Its max-EP cut is temporary for players.
  - **New look:** a 3D model in hand (the inventory icon is unchanged), with crimson void parallax in its split and spots (a custom shader), a five-leaf pommel bloom that pulses near strong beings, and a shard fracture during Black Meteorite.
  - **New VFX:** meteor and Nihility zone.
- **New: ten Word Soul counter-words:** Seal, Reject, Fall, Reveal, Sleep, Petrify, Cower, Banish, Reverse, Drain.
  - All of them wear off, so none is a lasting nerf to a player.
  - The old words and their look are unchanged.
- **Replacement: Zagred's boss look and AI.**
  - **Look:** a cuboid true-form devil (wings, horns, claws, whip tail) with a violet-to-crimson fresnel aura shader, a glow pass, a target reticle and embers.
  - **Sync:** a state packet.
  - **AI:** a utility AI that reads the fight and telegraphs each word.
- **Buff (the owner's call): addon magic now stands with or above Tensura's.**
  - **Damage:** spell and weapon damage scales with the caster's EP.
  - **PvP cap:** one hit takes at most 40% of a player's max health (`pvpHitCapPercent`).
  - **Cooldowns:** 40% shorter (`spellCooldownPercent` = 60).
- **Replacement: Anti-Magic Spirit Lord merged into the Anti-Magic grimoire.**
  - Old Lords migrate automatically.
  - Anti-Magic is now event only: an op gives it with `/multiverse event antimagic <player>`.
  - **New:** a new Anti-Magic icon.
- **New: gamerules** for the per-world switches: `/gamerule nusmp...`. Examples: `nusmpZagredBoss`, `nusmpAntiMagicEventOnly`, `nusmpPvpHitCapPercent`, `nusmpSpellCooldownPercent`.
- **Fixes:**
  - Charmy's Cotton/Food pages no longer stay sealed.
  - World Tree trees are taken back fully, even after a restart.
  - The Four Kingdoms label no longer covers the preset slots.
  - Grimoire upkeep no longer depends on skill ticks.

## 0.47 - Kotodama Magic (Word Soul), the Zagred boss
- **Replacement: Zagred's devil power.** Zagred's old kneel aura (Devil Union) is gone. In Devil Union, Zagred now lends three Kotodama words: **Halt**, **Shatter** and **Heal**, at reduced power. The `ZAGRED` devil entry stays.
- **New: Kotodama Magic (Word Soul)**, Zagred's magic, a God-class (Tensura Ultimate) grimoire.
  - **Creative only.** It is never rolled in survival, and only binds or answers in creative or for someone who has beaten Zagred.
  - **Speaking:** type a command word alone in chat ("Halt!", "Shatter.") or cast its page. The word flashes in mid-air as demonic glyphs.
  - **Cost:** an astronomical share of max magicules with a very high floor; Aura pays whatever magicules can't. Free in creative.
  - **Scaling:** EP drives the radius, damage and counts.
  - **Words:** Halt / Bind (mass Absolute Paralysis + Magic Jamming), Shatter / Return (enemy shots break back into magicules, some refunded to you), Heal (health and Aura to full, debuffs cleared, allies too), Devour (spreading underworld sludge), Trident (an otherworldly trident for 60 s), Swords (a demon sword storm).
  - **Effects:** Tensura paralysis and silence, spiritual damage, and Soul Annihilation when a target's spirit is driven to its floor.
  - **Underworld sludge** devours the ground it spreads over, hurts and drains whoever wades in it, and puts every block back when it ends (unless `griefBlocks` is on).
  - **Summoned grimoire:** a purple-black smoke aura and a glowing five-leaf emblem.
  - **Zagred** joins as a canon five-leaf grimoire (creative tab).
- **New: the Zagred boss (off by default).** Turn on `zagredBossEnabled` in the server config, then an op runs `/multiverse boss zagred`.
  - Four phases: words, then elemental adaptation, then void flooding, then only anti-magic or three elements at once can wound it.
  - Set `kotodamaBossReward = true` to give Kotodama to everyone who fought it; it is off by default.
- **New VFX:** spoken glyphs, the grimoire aura, a Halt / Heal burst, shatter, demon swords, the trident and sludge. Previews are in `docs/vfx_previews/koto_*.png`.
- **New Blender script:** `tools/blender/build_zagred_grimoire.py` builds Zagred's grimoire: rigged, etched starbursts, the black clover, spine panels, a pulsing shader and smoke. Preview: `docs/blender/previews/zagred_grimoire.png`.
- Guide: `docs/kotodama_spec.md`.

## 0.46 - Four Kingdoms menu, real World Tree trees
- **New: the Four Kingdoms menu.** The grey "?" slot in Tensura's Magic tab is now a Four Kingdoms logo button that opens the mod's own menu.
  - The menu has the logo, your grimoire's summary and a Status button.
  - Your unlocked grimoire pages appear as an open book: each page shows its name, cost, cooldown and what it does.
  - Turning pages plays a page-flip animation with sound.
  - Guide: `docs/four_kingdoms_menu.md`.
- **Replacement: World Tree Magic grows real Minecraft trees.**
  - Mistilteinn Seed grows a small tree where it lands.
  - Magic Tree Descent grows a great tree: fancy oak, then dark oak or mega spruce, then mega jungle as your EP grows.
  - Budding of Yggdrasil raises a ring of trees with a giant in front.
  - The trees are taken back when the spell ends, block by block, only where they're still the tree's own. Set `worldTreeTreesStay = true` in the server config to keep them.
  - The drawn tree effect stays registered but is unused.

## 0.45 - Light, World Tree, Dice, Slash, Compass, Mercury
- **Replacements:**
  - **Light Magic** is the fastest magic in the mod. Light hits ignore armour and cut the spirit; only an Aura barrier halves them. Its two pages were remade, and three were added: Healing Ray, Lamp of Avior Gloria and Light Speed.
  - **Mercury Magic** switches between liquid and hyper-dense forms. Silver Guardian is a liquid dome that pours back into spears when it ends, and fire melts through it. Silver Blade, Silver Rain and the new Silver Eagle all scale hard with EP.
  - **Canon books:** William Vangeance now uses World Tree Magic and Jack the Ripper uses Slash Magic. Their old pages stay registered.
- **New magic:**
  - **World Tree:** grows real Minecraft trees, from saplings up to mega jungle trees as your EP rises. They are taken back when the spell ends; set `worldTreeTreesStay` in the server config to keep them. They bind foes, catch shots in mid-air and drain magicules, passing them to you and your allies.
  - **Dice:** Elemental Dice rolls 2d6 etched with elements. The Fate Die plays by D&D rules: a natural 1 fumbles and silences you, a natural 20 is a critical hit. Also Gambler's Fallacy and Loaded Dice.
  - **Slash:** adaptive cuts permanently mark defended foes until their armour and barriers stop working. Also Forearm Blades and Ripper Dash (both use aura) and Death Scythe.
  - **Compass:** brass compasses whose needles lock onto the strongest magicule signature. Willful Compass sends enemy shots back; Another Atlas marks a foe; Compass Rose locks every foe nearby.
  - **Letoile Becquerel** joins as a canon Compass grimoire.
- **New VFX (16 effects):**
  - **Light:** blades with real lens-flare ghosts.
  - **World Tree:** erupting roots, a descending world tree and a mana drain stream.
  - **Dice:** 3D resin dice that roll and land on the result.
  - **Slash:** forearm blades, crescent slashes and the scythe.
  - **Compass:** the compass array, homing needles and a lock sigil.
  - **Mercury:** a liquid-silver dome, spears and the eagle.
- **New Blender script:** the dice and the compass, with a preview at `docs/blender/previews/dice_compass.png`. Guide: `docs/extended_attributes_spec.md`.

## 0.44 - Painting Magic remake (palette & brush)
- **Replacement: Painting Magic.** Page ids are kept; the full guide is `docs/painting_palette_spec.md`.
  - **Palette & brush:** summoning a Painting grimoire now puts a real wooden thumb-hole **palette** in your off hand and a **mana brush** in your main hand.
    - They are 3D models with glowing, animated pools of paint and a new model for each paint. They dissolve when you drop them or stow the grimoire, and they can't be duplicated.
    - **Palette:** right-click changes the paint (ink, fire, water, ice, wind, earth, lightning). Sneak + right-click picks the paint that counters the last element that hit you. While held, it softens that element by 40%.
    - **Brush:** right-click paints a stroke. It uses **Aura**, falling back to magicules.
  - **Elements:** each paint does something different, including Tensura's silence (magic jamming), fragility (resistance shred), energy drain and spiritual damage.
  - **Mood:** joy (landing hits, kills, food, friends nearby) boosts your imagination up to +30%; frustration (getting hurt, low health, fear) stifles it to −30%.
  - **EP scaling:** EP and gear scale every painting.
  - **Page changes:**
    - Brushstroke, Painted Menagerie and Master of Valhalla were remade. The Menagerie and the einherjar are now living painted constructs.
    - New pages: **Living Illustration** and **Counter Palette**.
- **New: living illustrations.** The drawing is painted flat on the ground, stands up, and steps out as a solid painted beast, einherjar or giant with a glowing wet outline.
- **New VFX** at the Time standard: palette manifest, glossy brush trail, living illustration and element shift (`tools/gen_paint_studio_textures.py`).
- **Shaders and Blender:** labPBR specular maps let Photon shine the wet paint. A Blender builder for the palette & brush (`tools/blender/build_palette_brush.py`) has a preview at `docs/blender/previews/palette_brush.png`.

## 0.43 - Better Combat compatibility
- **New: Better Combat support.** With [Better Combat](https://modrinth.com/mod/better-combat) installed, every Black Clover sword gets combo swings, attack hitboxes and dual wielding. The files are in `data/nusmp/weapon_attributes`. Without Better Combat nothing changes: the files are just ignored, and the dependency is optional.
  - **Asta's and Licht's blades** (Demon-Slasher, Demon-Slayer, Demon-Dweller, Demon-Destroyer and Licht's two) are one-handed, so you can dual-wield them as in canon. The big blades reach further.
  - **Rimeheart Runeblade** is one-handed, so its "second magic sword in the off hand" bonus works with Better Combat's dual wielding.
  - **Other weapons:** the Miasma-Infused Katana uses the two-handed katana set (Yami's style), the Spell-Forged Rapier the rapier set, the Severing Greatsword the claymore set, and the magic tool sword and spear the sword and spear sets.
  - **Unchanged:** right-click weapon skills, grimoire casting, and the hit effects (crit bursts, anti-magic stripping) all work as before, because Better Combat still lands hits through the normal player attack.

## 0.42 - Mirror Magic overhaul (Gauche Adlai)
- **Real mirrors:** every Mirror spell now summons an ornate silver mirror. Frames come in five shapes (oval, arch, round, gothic crest and diamond), with glass, a flickering violet mana corona, distortion ripples and an edge glow.
- **Replacements:** Reflect Refrain, Real Double, Reflect Ray and Full Reflection (page ids are kept).
  - **Real Double:** doubles are now real, living mirror clones (`nusmp:mirror_double`). Each wears your mirrored skin and holds your weapon.
    - **Cost:** each double costs a tenth of your max magicules, with aura making up any shortfall.
    - **Combat:** doubles fight beside you, echo every spell you cast and pull enemies off you. 35% of the blows aimed at you land on a double.
    - **Trading places:** sneak-cast to swap places with a double.
  - **Reflect Refrain / Full Reflection:** the shots they catch go back to the shooter, who is silenced. Full Reflection also returns 60% of melee blows.
  - **Reflect Ray:** fires a volley that wounds the spirit.
- **New pages:** Mirror Array (orbiting mirrors that catch shots and shatter in place of a killing blow), Mirror Step (glass-to-glass travel), Mirrors Slash and Mirrors Meteorite.
- **Scaling:** Mirror power grows with your Tensura EP and your armour.
- **Tensura effects:** silence (magic jamming), fragility and spiritual damage.
- **New VFX:** mirror frame, array, shatter and step-out (`tools/gen_mirror_vfx_textures.py`).
- Guide with Blender and shader notes: `docs/mirror_spec.md`.

## 0.41 - Dream Magic's pocket dimension, Painting Magic
- **Replacement: Dream World and Glamour World** no longer only debuff a field. They pull their targets into a real pocket dimension, `nusmp:dream`: a pastel arena under an iridescent dome with a starry sky.
  - **Manifestation:** inside, the caster's words become solid (bear, feast, fire, ice, cage, stars, wall, sleep). Dorothy also reads minds: what dreamers say can be turned against them.
  - **Exhaustion:** a dream load wears each dreamer's mind down until it breaks.
  - **Breakout:** heavy combined damage, teleporting, or casting Spatial, Time, Anti-Magic or Dream Magic shatters the dream.
  - **Returning:** everyone goes back where they were. Page ids are kept.
- **New page:** Imagination Manifestation (Dream).
- **New attribute: Painting Magic** (from the wiki), with nine pages: Brushstroke, Spring of Restriction, Camouflage, Souterrain Giant's Strong Arm, Deux Tempêtes of Fire and Ice, God's Game, Elemental Quintet, Master of Valhalla and Painted Menagerie. It has sticky ink that dries to lacquer and roots, camouflage that breaks when you strike, and paintings that come to life.
  - **Replacement:** Rill Boismortier's canon book is Painting Magic (was Creation); his Creation page stays.
- **New VFX** at the Time standard: dream transition, dome, manifestation and shatter; paint stroke, sticky splat, painting-to-life and camouflage (`tools/gen_dream_paint_vfx_textures.py`). The Painting skill icons are new too.
- Guide: `docs/dream_painting_spec.md`.

## 0.40 - crash fix
- **Fix (crash):** the transformation armour overlays (Wind Spirit Dive and the others) crashed the client with "Not building!": the layer kept drawing into a buffer after asking Minecraft for one of another render type, which closes the first. Each piece now asks for its buffer right before it draws. The headless preview (`tools/mode_preview`) now catches this.
- **Fix:** the grimoire guard's hook into Tensura's plunder / learning events failed to attach (Java access to Architectury's event class), so only the 5 s check was protecting grimoires. It now registers through the public `Event` interface.

## 0.39 - The Convergence
- **New (core system):** the world starts as pure Tensura and the Black Clover world slowly bleeds in. Full guide: `docs/convergence.md`.
  - **Origin roll:** each account gets one server-side roll the first time it joins. About 5% are **anomalies**, Black Clover-origin mages; everyone else is Tensura-origin.
  - **Pity rule:** after 20 Tensura rolls in a row, the next player is an anomaly.
  - **Kingdom roll:** each anomaly also rolls a kingdom (50 Clover / 20 Diamond / 20 Heart / 10 Spade), and the kingdom picks the grimoire's covers.
  - **Awakening:** after some play time, an anomaly sees **[Unknown Magic Detected]**, then the world chooses them ("You have been chosen by another world's magic."). Everyone else only sees an anonymous "anomalous magical disturbance".
  - **Stages:** SIGNS → FIRST_GRIMOIRE → CLOVER → DIAMOND → HEART → SPADE. Each comes with rumours and announcements, ending in **❄ THE SPADE KINGDOM HAS ARRIVED**. Stages advance by real days or with `/multiverse convergence stage`.
  - **Hidden until revealed:** Grimoire Towers only generate from the CLOVER stage, and Tensura-origin players see no Black Clover status until then.
  - **Tensura skills:** Black Clover players keep and use all their Tensura races, evolutions and skills.
- **New: secret side quests for anomalies.** There are 15 (four of them, one per kingdom, only for that kingdom's anomalies), rewarding gold stars and grimoire mastery.
  - They show up in chat, the Multiverse status panel and `/multiverse quests`.
  - With FTB Quests installed, a hidden "The Convergence" chapter is written. Its quests complete through hidden advancements, so only anomalies ever see it.
- **New: the grimoire skill can't be copied, plundered or bestowed.** Tensura's plunder and learning events are refused for grimoire skills, and a catch-all takes back any grimoire skill nobody gave. Only the world or an admin can give one.
- **Replacements:**
  - The Acceptance Ceremony and altars choose only anomalies: "everyone eligible" is replaced while the Convergence is on, and `enabled = false` restores it.
  - A grimoire grant is announced anonymously instead of by name.
  - Players who already had a grimoire keep it and become anomalies of their cover's kingdom.

## 0.38 - third-person book fix, Julius-only drum, transformation armour
- **Fix (third person):** the summoned book no longer looks like a flat card stuck to your hand and facing the camera. The 0.36 pose turned the pages outward, away from you, and the 1.1-block book sat on your right arm. Now it hovers in front of your right shoulder, clear of the body, turned in toward you and leaning back like a lectern: the same view as first person, which is unchanged (`tools/grimoire_preview/third_person.py` checks it from behind, the front and the side).
- **Replacement:** the page drum is Julius's canon grimoire only again. Every other Time grimoire is a normal book. His drum no longer ripples or flips pages on a spell switch.
- **New: transformation armour overlays.** These are temporary and take no armour slot. They appear on the instant the mode is cast and fade when it ends.
  - Wind Spirit Dive (Yuno): fur-collared coat, floating star-blade crown, wind wings. Turned on by Spirit Dive on the Wind book or by Spirit of Zephyr.
  - Fire Spirit Dive (Salamander): clawed gauntlets, dragon wing frame, flame hair.
  - Anti-Magic Demon Mode (Asta): one horn, one tattered left wing, black arm, dark pixel wisps. Turned on by Black Asta or by the Spirit Lord's Black Form.
  - Lightning God Mode (Luck): runic chest and shoulder plates, a crackling bolt crown. Turned on by Thunder Fiend or Black Lightning Battle Fiend.
  - Valkyrie Dress (Noelle): crystal water armour, avian water wings, a spinning drill lance.
  - Synced to everyone nearby. Textures come from `tools/gen_mode_armor_textures.py`; preview with `python tools/mode_preview/preview.py`.
- Blender blueprint for the overlays (topology and sockets, per-mode breakdowns, shaders and scrolling textures, Minecraft integration): `docs/mode_armor_spec.md`.

## 0.37 - the Time grimoire's page drum
- **Replacement:** the Time grimoire now looks like the owner's screenshot. It is a solid upright drum of about 96 cream pages packed edge to edge, with a ribbed band of page edges round the outside and a radial fan of page tops: no covers, no spine. It is about 0.8 blocks across and stands off the leg at the hip. Summoned, it stays upright beside you, glows, turns, flutters, fans out and ripples on a spell switch. This replaces 0.35's small hollow translucent ring and now applies to **every Time Magic grimoire**, not only Julius's canon book (`tools/gen_time_drum_texture.py`). The Time Magic VFX are untouched.
- The blueprint's Julius section is updated to the solid drum (`docs/grimoire_harness_julius_rouge_spec.md`).

## 0.36 - low-profile floating grimoire
- **Replacement:** the grimoire is ultra-thin (2.8 units board to board, was 5), and summoned it opens almost flat (14 degrees, was a 34-degree V). It now floats low beside your right side, lying open at a steep tilt with the pages facing up at you, after the floating-grimoire references; first person matches the tilt. The summon trip, hip harness, page flip and Julius's drum are unchanged.
- **New:** glowing spell runes on the open pages while the book is held (full-bright, in the book's trim metal; `tools/gen_page_runes.py`).
- Blender blueprint for the low-profile floating grimoire (thin geometry, tilt pivots and hover rig, variant architecture, slot-free binding, stylised shading and emissive runes): `docs/floating_grimoire_spec.md`.

## 0.35 - grimoire holster harness, Julius's coverless grimoire
- **New: holster harness.** Every player with a bound grimoire wears a leather belt with a brass buckle. A strap hangs from the right hip and wraps the dormant book: down the front cover to a brass tip, over the top and down the back, with a keeper loop. It is drawn over the player like a cosmetic layer, so it takes no armour slot (`tools/gen_harness_textures.py`).
- **Replacement: Julius Novachrono's grimoire** (his canon book) is no longer drawn as a covered book but, as in the anime, as a cylinder of loose pages with no front or back cover. It turns slowly at the hip; summoned it glows, turns faster and flutters, fans wider as it opens, and ripples on a spell switch. The Time Magic VFX are untouched.
- Combined Blender blueprint for the harness, Julius's grimoire and Rouge (modelling, accessory rig, Geometry Nodes, shaders, compositing): `docs/grimoire_harness_julius_rouge_spec.md`.

## 0.34 - every other magic after the wiki, captains' character spells, Rouge
Time, Fire, Water, Earth and Wind keep their spells and effects exactly as they were. Everything below is new (appended pages) unless marked *replaced*; replaced pages keep their ids, so open pages and mastery carry over.
- **New effects** (20, `ArcaneSpellLayer` / `ArcaneSpellLayer2`, textures from `tools/gen_arcane_vfx_textures.py`, all checked in `tools/vfx_preview` under the 400-vertex budget): thread web and puppet strings, seal chains and the trinity seal crystal, Thunder Fiend arcs and the Rising Salim pillar, the transmutation circle, the poison curtain and breath, the dimensional gate and the Red Room cube, the ash formation, reflect ray and mirror doubles, cotton sheep, the glutton's maw, shadow pools with hands, the Unite aura, the Raging Black Bull construct and Rouge.
- **Thread:** Arachne's Web, Dancing Doll (the foe walks where you look; a monster turns on its own side), Mending. Red Thread now shows puppet strings. *Replaced:* Rouge is now drawn as a woven cat on your head that unravels when she saves you, and she can also dodge a blow for you (12%).
- **Seal:** Sealing Chains, Trinity Seal Magic, Barrier. *Replaced:* Seal / Grand Seal now show chains.
- **Lightning:** Thunder Fiend and Black Lightning Battle Fiend (Luck only; blows arc to more foes), Pulsaranta, God of Lightning Rising Salim, Thunderbird Cavalry.
- **Spatial:** Unopening Red Room, Myriad Black, Door of Fate (a gate that takes you and your allies to your spawn point). *Replaced:* the Fallen Angel Gate is now a real portal; Spatial Mana Domination is renamed Sacred Mana Domination.
- **Mirror:** Reflect Ray, Large Reflect Ray, Full Reflection. *Replaced:* Real Double now spawns three mirror doubles (Tensura-style body doubles): blows can shatter a double instead of you, the doubles echo your blows, and monsters lose track of you. **Gauche's Hand Mirror** (new relic): sneak-use to leave a mirror, use to step back through it.
- **Poison** (*replaced*, same ids): Aufwachen Dachs, Violett Schirm, Basilisk's Breath, plus Curse-Worker's Neighbor (Gordon only).
- **Shadow** (*replaced*, same ids): Dark Garden Invitation, Kids' Playground, Shadow Realm, plus Heaven's Shadow Second Sight and Unite Mode: Canis / Gallus / Canis x Felis (Nacht only).
- **New attributes** (new grimoires, icons and cover colours): **Transmutation** (Grey: Iron Spikes, Magic Convert, Quagmire, Grand Transmutation), **Ash** (Zora: Ash Bullets, Ash Cloud, Ash Absorbing Formation, Revelation of the Cowardly), **Cotton / Food** (Charmy: rolls Cotton or Food 50/50 when bound, Charmy's own book has both: Sleeping Sheep Strike, Sheep Cook, Sheep Bondage, Cotton Cloud / Gourmet's Bite, Glutton's Banquet), **Recombination** (Henry: Mana Corkscrew, Bulwark, Room Swap, The Raging Black Bull).
- **Character spells**: some pages only open in a named character's grimoire (`book/CharacterSpells.java`; the character is the canon book bound with the creative tab or `/multiverse grimoire canon <player> <book>`). Already-open pages stay open.

  | Character | Spells |
  |---|---|
  | Yami Sukehiro | Dark Cloaked Dimension Slash: Equinox |
  | Fuegoleon / Leopold Vermillion | Leo Rugiens |
  | Mereoleona Vermillion | Calidos Brachium |
  | Yuno | Spirit of Zephyr |
  | William Vangeance | World Tree Magic: Yggdrasil |
  | Charlotte Roselei | Briar Magic: Briar Prison |
  | Jack the Ripper | Slash Magic: Ripper Cut |
  | Rill Boismortier | Painting Magic: Painted Menagerie |
  | Dorothy Unsworth | Glamour World |
  | Kaiser Granvorka | Vortex Magic: Vortex Shield |
  | Nozel Silva | Mercury Magic: Mercury Rain |
  | Nacht Faust | Unite Mode: Canis, Gallus, Canis x Felis |
  | Luck Voltia | Thunder Fiend, Black Lightning Battle Fiend |
  | Gordon Agrippa | Curse-Worker's Neighbor |
  | Zora Ideale | Revelation of the Cowardly |
  | Henry Legolant | The Raging Black Bull |
- **New canon grimoires:** the captains Charlotte, Jack, Rill, Dorothy, Kaiser and Nacht, and the Black Bulls Grey, Gordon and Henry. Charmy's and Zora's canon books now carry Cotton and Ash.
- Rouge's Blender build blueprint (topology, woven fibres, Geometry Nodes threads, the unravel rig, shaders, compositing): `docs/rouge_spec.md`.

## 0.33 - every sword redrawn as pixel art
- **Replacement:** the Demon-Slayer, Demon-Dweller, Demon-Destroyer, Demon-Slasher Katana, Yami's Katana, Licht's Demon-Dweller and Demon-Destroyer, the Spell-Forged Rapier and the Severing Greatsword are redrawn in the same pixel-art style as the Rimeheart Runeblade and the reference sheet (`tools/gen_pixel_swords.py`, replacing the 0.28 smooth 128 px sprites and `tools/gen_weapon_textures.py`). Each is a 32x32 sprite with 4-step palette ramps lit from the top-left and canon shapes (the Slayer's chipped edges and pointed tip, the Dweller's four-sided guard and spiral grip, the Destroyer's bite and flared clover end, round tsubas and diamond-wrapped katana grips, the rapier's swept hilt).
- Mana glows: Asta's katana has a crimson anti-magic edge, Licht's swords have gold light markings and clover, the rapier has blue runes and the greatsword a blue gem. These sit on a full-bright layer (`<sword>_glow.png`), like the runeblade.
- Item ids, stats and abilities are unchanged. The plain Magic Tool sword and spear keep their vanilla-style 16 px look.

## 0.32 - Rimeheart Runeblade
- New sword: **Rimeheart Runeblade**, a navy-steel pixel-art runeblade with a hooked silver guard and cyan mana runes (32x32 sprite, `tools/gen_runeblade_texture.py`). Its runes and gems sit on a separate layer drawn full-bright, so they glow in the dark (and shader packs bloom them).
- 10 attack damage, 1.6 speed, +0.25 knockback resistance in the main hand, Netherite durability.
- Right-click **Glacial Matrix Burst**: erases spells within 5 blocks, then freezes, slows, hurts and knocks back everything caught. Critical hits shatter a smaller burst round the target. With another magic sword in the off hand the burst recharges 40% faster and hits freeze longer.
- Full spec (lore and mana circuitry, voxel grid and shading, palette hex codes, emissive layering, stats): `docs/runeblade_spec.md`.

## 0.31 - Fire, Water, Earth and Wind spells from the wiki
New pages (appended; Time Magic VFX untouched, new effects built to the same standard and checked in `tools/vfx_preview`):
- **Fire:** **Spiral Flame** (a flame vortex drills forward, piercing), **Wild Bursting Flame** (three waves of flame out of you in every direction), **Ignis Columna** (a towering column of flame that keeps burning and lifts).
- **Water:** **Aqua Javelin** (high-pressure piercing lance), **Sea Dragon's Nest** (water dome: enemy spells swallowed, foes slowed, allies heal), **Valkyrie Dress** (water armour, 30 s of speed, strength, resistance and jump).
- **Earth:** **Witch Hunter Claws** (stone claws clamp shut on the target and hold it), **Rampaging Mother Earth** (a heaving wave of stone rolling 18 blocks); Earth Wall is now called **Mud Wall Partition** and Mother Earth Split **Divided Mother Earth** (same spells, canon names).
- **Wind:** **Towering Tornado** (a huge tornado where you aim), **Slicing Wind Emperor** (a vast crescent of many wind blades), **Spirit of Zephyr** (30 s of wind-spirit speed and lightness; enemy spells blown away).
- Nine new effects (FIRE_SPIRAL, FIRE_WILD, WATER_JAVELIN, WATER_NEST, WATER_DRESS, WIND_EMPEROR, WIND_ZEPHYR, EARTH_CLAWS, EARTH_RAMPAGE); Ignis Columna and Towering Tornado use the existing column and tornado effects, scaled up.
- Already in the mod from earlier versions: the Summon Grimoire ability with the float / open / stow animation, the creative grimoire fix, and Time-quality Fire / Water / Wind / Earth effects.

## 0.30 - addon logo
- The Four Kingdoms / Tensura emblem is the mod's logo (`src/main/resources/logo.png`, 512x512, shown in the in-game Mods list). The same file works as the CurseForge project icon.

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
