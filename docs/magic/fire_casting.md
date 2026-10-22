# Fire casting (Black Clover): four canon casters, four ways to cast the same Fire pages (key `fire_casting`)

Owner request: rework HOW the existing Fire grimoire pages are cast for Magna, Mereoleona, Fuegoleon and Leopold when the caster's bound canon grimoire (`CharacterSpells.characterOf(player)`) is theirs. **No new skills, no new spell trees, no page ids changed, no gameplay number raised past its old cap.** Every other caster keeps the generic casting.

Nothing here was seen in a game. Every picture is the project's headless rasteriser (see section 9).

The whole feature is one **server** config switch: `magic.fireCasting` in `nusmp-firecast-server.toml` (default `true`; `false` gives back the old generic casting for everybody).

## 1. The four styles at a glance

| Caster | Style (`firecast.FireStyle`) | How a Fire page is cast now |
|---|---|---|
| Magna | `MAGNA` | Only with **Magna's Soul Bat** (`nusmp:magnas_soul_bat`) in the main hand. The page spawns a hovering fire ball in front of you; the bat swing hits it at the impact tick and sends it where you look. A missed swing makes the ball drop and burst weakly. |
| Mereoleona | `MEREOLEONA` | Key casting is refused while the style is on. Landed unarmed punches build a combo; the 3rd (cheap pages) or 5th (costly pages) punch is the finisher and releases the armed page from the fist. |
| Fuegoleon | `FUEGOLEON` | A small **Salamander** spirit sits on the right shoulder (toggled with its own key). The page leaves its mouth. A short, cooldown-bound **big dragon form** hovers over you and casts from its jaws. |
| Leopold | `LEOPOLD` | Hold the cast key to wind a spiral up (4 levels); each charge step draws mana through the mana law; damage and size grow; release throws it. |

## 2. Step-by-step flows (pseudo-code; the server decides, the client draws)

Common entry (`book/GrimoireBook`, hooks marked `FC`):

```
onPressed(player, page):                       # FC: style hooks
    style = FireCasting.styleOf(player)        # NONE unless config on + canon grimoire is a Fire caster
    if style == NONE: old path
    FireCasting.held(...)                      # Leopold starts charging; Mereoleona reads "armed page"
onHeld(player):    FireCasting.held(...)       # Leopold: charge ticks; every CHARGE_STEP ticks drawMana()
onRelease(player): FireCasting.release(...)    # Leopold throws, Mereoleona arms the page, Magna/Fuegoleon cast
castPage(player, page):
    reason = FireCasting.refuse(player, page)  # Magna without the bat, Mereoleona key cast, ...
    if reason: say(reason); return             # short message, no cost, no cooldown
    ... old checks (cooldown, mana via EnergyHelper, locks) ...
    FireCasting.run(player, page)              # the style's flow, wrapped by SafeCast.run
    ... old cooldown / anim ...
```

### 2.1 Magna Swing

```
refuse: style==MAGNA and main hand is not magnas_soul_bat -> "needs the Soul Bat"
run:
    if not CapLedger BALLS.acquire(player, perPlayer 1, global maxBalls): refuse
    if not gapOk(now, lastSwing, MIN_SWING_GAP=24): refuse
    ball = spawn FIRECAST_1 prop at ballPosition(eye, look, BALL_DISTANCE 1.9)   # hovering
    play clip fire_bat_swing (16 ticks) to nearby players
    schedule at tick SWING_IMPACT_TICK (8):
        if player dead/offline/changed dimension/ball gone: release, stop
        hits = swingHits(player pose, SWING_REACH 3.6, SWING_HALF_ANGLE 55) vs the BALL   # tested ONCE
        if hit:
            aim = look vector; target = homing search ONCE (cone 9 deg, 24 blocks) or none
            launch the page's own shot from the ball position (emitter override)
        else:
            ball drops BALL_DROP 0.25; weak burst (2 damage, r 2, at most 4 targets)
        ball ends (BALL_LIFE 16 ticks at most); BALLS.release
```

### 2.2 Mereoleona's fists

```
key press cast of a Fire page: refused with "your fists are the spell" (message gap 40 ticks)
page chosen -> on key release the page is "armed" (stored per player UUID, ARM_TICKS 600)
LivingDamageEvent.Post (attacker is a player, style MEREOLEONA, main hand empty, melee):
    r = PunchCombo.land(now, mana zone?, costPercent of armed page, armed)
        # timeout COMBO_TIMEOUT 30 ticks (+12 in an open Mana Zone), cap COMBO_CAP 8
        # finisher at the 3rd hit (cost < 20%) or the 5th (cost >= 20%), one less in a Mana Zone, never below 2
    if r == FINISHER: castFromStyle(player, armed page) with the origin moved to the fist (fistPosition)
    play clip fire_punch_a / fire_punch_b / fire_punch_finisher
```

### 2.3 Fuegoleon's Salamander

```
toggle key (client sends Key(0), 10 tick gap) -> server toggleSpirit(player):
    link:  SPIRITS.acquire(player, 1, maxSpirits 48); state.linked = true; SPIRIT_AURA effect
    unlink: SPIRITS.release; the prop ends
upkeep (every 4th tick, only for players in LIVE):
    not Fuegoleon / dead / dimension change / logout -> unlink (forget)
    every 60 ticks: re-send the spirit glow effect; Fire Resistance aura every 100 ticks (FireCastProps)
cast while linked:   emitter = shoulderMouth(player)           # right shoulder, 0.34 forward, 0.10 up
big dragon (summonDragon):
    needs cooldown over (DRAGON_COOLDOWN 400), DRAGONS.acquire(player, 1, maxDragons 6), extra cost 20 %
    state: small form hidden (HIDDEN bit), dragon prop for DRAGON_TICKS 60
    emitter = dragonMouth(player)                              # 1.8 behind, 3.3 up, mouth 2.6 forward, 0.9 up
    breath at tick DRAGON_BREATH_TICK 8 (the page's own shot, at the mouth)
    end: dragon released, small form back
```

### 2.4 Leopold's spiral

```
onHeld each tick: state.hold++ ; level = chargeLevel(hold)   # LEVEL_AT {20,40,65,90} -> levels 1..4, hard cap MAX_HOLD 90
    every CHARGE_STEP (5) ticks past CHARGE_FREE_TICKS (20): draw CHARGE_STEP_COST 0.05 of the page cost
        through ManaLaw cost rules (chargeCostScale server config); not enough mana -> stop growing, keep what is wound
    FIRECAST_2 prop (spiral) follows the hand, level in the prop param
onRelease: throw: damage x {1, 1, 1.15, 1.3, 1.5}[level], size x {1, 1, 1.12, 1.25, 1.4}[level]; clip fire_charge_release
interrupt: death, logout, dimension change, page swap, style off -> prop ends, SPIRALS.release, nothing is cast
```

The damage and size multipliers are applied on the page's own numbers through `GrimoireBook.hurtAs()` and `size()`, after which the old caps (`BalanceLaw`, PvP hit cap) still apply: the style never lifts a cap.

## 3. Animation integration

The server clock decides, the client only plays. Clips are in `assets/nusmp/animations/player/cast.animation.json` (only insertions, generated by `tools/gen_cast_animations.py --append-fire`), their names are constants in `anim/CastAnim` (`FIRE_BAT_SWING`, `FIRE_PUNCH_A`, `FIRE_PUNCH_B`, `FIRE_PUNCH_FINISHER`, `FIRE_CHARGE`, `FIRE_CHARGE_RELEASE`).

| Clip | Length | Contact (impact) | What happens on the server at the contact tick |
|---|---|---|---|
| `fire_bat_swing` | 16 ticks | tick 8 (`SWING_IMPACT_TICK`) | the swing hitbox is tested once and the ball is launched or dropped |
| `fire_punch_a`, `fire_punch_b` | short | at the keyed fist-forward frame | nothing extra; the damage event already counted the hit |
| `fire_punch_finisher` | longer | the fist-forward frame | the page leaves the fist (the server already knows the finisher at the damage event, the clip is played with it) |
| `fire_charge` | looped | none | none; a pose held while the key is down |
| `fire_charge_release` | short | the throw frame | the spiral is thrown on key release |

Advice for anyone retiming a clip: keep the **contact keyframe at the same tick** as the constant in `FireCastRules` (`SWING_IMPACT_TICK`, `SWING_CLIP_TICKS`). The server never waits for the client: if a player has the animation mod off or lags, the spell still happens at tick 8; only the picture is late. Never put gameplay on a client animation event, and never read the client's pose on the server (the hitbox uses the server's own eye position and look).

## 4. The Salamander (Fuegoleon's Spirit Lord)

It is **not** the basic Tensura Spirit Lord. It is a lightweight prop (`PropKind.FIRECAST_1` for ball/spiral/small form, `FIRECAST_2` for the big form), with its own renderer (`client/prop/FireCastPropPainter` + `FireCastDraw`) and its own skin, built from the repo's geo tools: `tools/gen_salamander_geo.py` (models and animations) and `tools/gen_salamander_art.py` (deterministic texture painter). The server keeps the exclusive `ElementalSpiritLordEntity` untouched (kept, not replaced); the Salamander borrows the Spirit Lord's job (follows its owner, is the casting emitter, the spirit link) but not its AI, because a per-player AI mob per caster would cost far more than a prop. Spirit commands are limited to what this prop supports: link toggle, aura, emitter.

Design, from the four reference images: red-orange scales with spotted rings (hex lattice, ring spots), bat wings with veined membranes, a spiny head crest, a long segmented spiked tail that curls up, yellow slit eyes with a safe additive glow, clawed hands, fire licking off the wing tips and the tail barb. Animations (`animations/entity/firecast_*.animation.json`): `idle` (breathing, tail sway), `perch` (small form, folded wings), `fly` (wing flaps), `breathe` (head back, jaw open). Wings and tail move by bone rotation only (no per-frame geometry).

### 4.1 Levels of detail and the exact quads per frame

Quads are counted from the model files (`FireCastLodTest` reads them, so these numbers cannot drift from the files).

| Form | LOD | When (distance) | Body quads | Glow-only model quads | Quads per frame |
|---|---|---|---|---|---|
| Small (shoulder) | full | up to 14 blocks | 240 | 23 | 263 |
| Small | half | up to 30 | 170 | 23 | 193 |
| Small | low | up to 56 | 86 | 0 | 86 |
| Small | culled | beyond 56 | 0 | 0 | 0 |
| Big dragon | full | up to 30 | 254 | 23 | 277 |
| Big dragon | half | up to 60 | 184 | 23 | 207 |
| Big dragon | low | up to 96 | 86 | 0 | 86 |
| Big dragon | culled | beyond 96 | 0 | 0 | 0 |

The glow-only model (eyes, mouth, flame licks) is drawn additive with its own half-size glow sheet. No billboards. Wing and tail animation is by part rotation. Textures: the colour sheet per form, `_lod1` half size, `_lod2` quarter size, `_fx` the glow sheet at half size.

A **shared frame budget** `FireCastLod.FRAME_QUADS = 2400` quads for all Salamanders, spirals and balls in one frame: the nearest are drawn at the level their distance asks for, then each is stepped down a level until it fits (`QuadBudget`, `FireCastLod.fit`), then culled. Nothing is allocated per frame (a static scratch array, static tables).

## 5. Worst-case cost (rule 11)

Server side (per server, defaults of `FireCastConfig`):

| Thing | Per caster | Server-wide cap | Lifetime | Notes |
|---|---|---|---|---|
| Magna balls (prop entities) | 1 | 32 | 16 ticks | swing gap 24 ticks; one swing test, one homing search |
| Leopold spirals (prop entities) | 1 | 48 | at most 90 ticks of charge, then thrown | mana drawn every 5 ticks |
| Small Salamander (prop entities) | 1 | 48 | while linked | position nudge every 5 ticks |
| Big dragon (prop entities) | 1 | 6 | 60 ticks, then 400 ticks cooldown | the small form is hidden meanwhile, so one body per caster |
| Mereoleona combo state | one small record | not applicable | cleaned on logout/death | hit registration in the damage event, nothing per tick |

Worst case entities in the world from this feature: the sum of the caps, 32 + 48 + 48 + 6 = 134 prop entities. That is an upper bound only: a caster has one canon style, so a caster owns balls (Magna), a spiral (Leopold) or a spirit and its dragon (Fuegoleon), never several kinds, and the caps are server config values the owner can lower.

Packets per second per caster (all nearby-only, within `FX_RADIUS` 48 blocks, through the perf budgets):

* client to server: 1 byte `Key(action)`, at most one per `PACKET_GAP` 10 ticks, validated (action in range, sender is a player, style linked), 2 per second at most;
* animation clips: at most one per swing / punch / charge start or release;
* prop position nudge: every 5 ticks per live prop (4 per second);
* spirit glow effect: every 60 ticks per linked caster (1 per 3 seconds);
* flame/spark effects of the old pages: unchanged, still through `Fx.send` and its budget.

Server per tick: the upkeep runs every 4th tick and only for players in the `LIVE` set; no per-tick scan of worlds or entities; at most one homing search per swing (cone 9 degrees, 24 blocks), at most 4 targets in the weak burst.

Client per frame: see 4.1 (at most 2400 quads shared). A ball costs at most 14 quads (`BALL_QUADS`), within 64 blocks. A spiral at most 26 sprites (`SPIRAL_MAX_POINTS`: `10 + 4 x level`, thinned with distance), within 48 blocks. Embers are spawned once per tick by custom particles through `Fx`, within the particle budget.

## 6. NEW / REPLACED / KEPT

| | What |
|---|---|
| NEW | the whole `firecast` package (rules, ledgers, LOD maths, server config, net, events, self-test), Magna's Soul Bat (item, 3D model, glow, engravings, Better Combat attributes, club class, grip, creative tab, Magna canon-grimoire grant), the Salamander (both forms, 4 LODs each, skin scripts, animations, renderer, key), the Leopold spiral prop and renderer, the Mereoleona punch combo, six player animation clips, lang fragment, previews, tests |
| REPLACED | nothing. No page, skill, id, model, texture or number was removed or replaced |
| KEPT | all Fire page ids and gameplay numbers (the style only changes who releases the page, from where, and how the old multipliers are fed), the old flame props and effects, the exclusive `ElementalSpiritLordEntity`, every other caster's generic casting (and everybody's, with `magic.fireCasting=false`) |

Existing files touched (hooks and append-only one-liners): `book/GrimoireBook` (style hooks in `onPressed`, `onHeld`, `onRelease`, `castReady` split into `castReady` + `castReadyBase`, `castPage` refuse gate and `FireCasting.run`, `size()` / `hurtAs()` multipliers, `vfx()` origin), `book/SpellRuntime` (bolt start origin + homing), `book/TensuraShots` (spawn origin), `anim/CastAnim` (constants), `prop/PropKind` (+`FIRECAST_1`, `FIRECAST_2`, appended), `prop/PropRegistry`, `client/prop/PropPainterRegistry`, `NUSMP` (boot lines), `NUCreativeTab`, `client/WeaponRenderer`, `item/WeaponEngravings`, `blackclover/GrimoireItem` (Magna canon grant).

## 7. Weapon: Magna's Soul Bat

Real 3D weapon in hand (`tools/gen_magna_bat_model.py` through the hook in `tools/gen_weapon_models.py`, drawn by `client/WeaponRenderer`; mesh `weapon_meshes/magnas_soul_bat.txt`, grip entry `gripxyz`), the old sprite is the inventory icon, glow texture is safe additive only. Club class (`weapon_classes.json`), Better Combat attributes (parent `bettercombat:mace`). Tensura engravings (rule 6): crushing 2, elemental boost 2, magic weapon 1, sturdy 1. Attack +6, speed -2.8. Preview: `docs/previews/fire_casting/magnas_soul_bat.png`.

## 8. Tests

* `firecast.FireCastRulesTest`, `FireCastLodTest`, `FireCastAssetsTest`, `FireCastSourceScanTest` (pure JUnit, data and asset checks, and source scans: the server decides, no client class from common code, no per-frame allocation in the painter, a unique config file name);
* self-test key `magic.fire_casting` (`FireCastSelfTests`, in the CI server smoke test);
* the guards that scan it: `FxSourceScanTest`, `PerfSourceScanTest`, `ServerSafetyTest`, `ConfigFileNamesTest`, `WeaponClassesTest`, `WeaponMeshesTest`, `GripTableTest`, `WeaponSizesTest`, `GeoAssetsTest`, `CastAnimAssetsTest`, the safe additive texture scan (scope `entity` for the two `_fx` sheets), `WeaponAttributesTest`, `FranchiseDataTest`.
* previews: `python tools/render_preview/preview.py firecast_...` (scenes in `tools/render_preview/scenes.d/firecast_*.json`), the punch in `tools/vfx_preview/scenes.d/firecast_punch.json`; pictures in `docs/previews/fire_casting/`.

## 9. Not verified (honest list)

* **Nothing was run in a game or on a server.** The type check was against API stubs and the JUnit run in a shim without Minecraft; tests that need the game classes (for example `MagicEnumAuditTest`) did not run in the shim and are left to CI.
* **The look only approximates the references.** The colours, spotted rings, yellow slit eyes, spiny crest, veined bat wings and the curling spiked tail match reasonably in the preview, but the silhouette is still more crocodile-like than the anime design: the small form crouches rather than clings, the big form is not upright or muscular, the heads are boxy. The side-by-side sheet `salamander_vs_reference.png` shows the gap plainly.
* Geometry and clip rotation signs were checked only in the headless preview; the in-game player animation (bat swing, punches, charge pose) was never seen, so the contact frames may need a retime.
* The default key for the Salamander toggle (backslash) is a guess and may clash with a pack's keys; it is rebindable.
* Balance feel (ball speed, finisher counts, charge costs, dragon cooldown) is untested.
* Mereoleona's held-punch variant and Fuegoleon's flame forearm are not built. The spirit commands are only link toggle, aura and emitter (no AI follow, no attack commands).
* The Salamander's `ElementalSpiritLordEntity` behaviours (tamed follow, commands) are not shared: this is a prop, deliberately cheaper.

## 10. Sources

Rule 10 (English wiki first, Japanese wiki fallback): both `https://blackclover.fandom.com/wiki/Salamander` (English) and the Japanese Black Clover wiki page for the Salamander (サラマンダー) **could not be fetched in this session** (`getaddrinfo ENOTFOUND blackclover.fandom.com`: no network route to fandom.com). The Salamander's design was therefore taken from the four reference images the owner supplied (small red form on the shoulder, big orange muscular form, golden-orange spotted form with red rings and veined wings, giant spirit above the caster breathing a fire blast), not from the wiki text. The four casting styles are taken from the owner's brief file (the owner's description of how Magna, Mereoleona, Fuegoleon and Leopold fight), not from wiki spell lists; page names and numbers are this mod's own and unchanged.
