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

It is **not** the basic Tensura Spirit Lord. It is a lightweight prop (`PropKind.FIRECAST_1` for ball/spiral/small form, `FIRECAST_2` for the big form), with its own renderer (`client/prop/FireCastPropPainter` + `FireCastDraw`) and its own skin, built from the repo's geo tools: `tools/gen_salamander_geo.py` (models and animations), `tools/gen_salamander_art.py` (deterministic texture painter) and `tools/firecast_geo_lib.py` (the geo kit: frames, boxes laid between joint points, flat cards, named texture windows). The server keeps the exclusive `ElementalSpiritLordEntity` untouched (kept, not replaced); the Salamander borrows the Spirit Lord's job (follows its owner, is the casting emitter, the spirit link) but not its AI, because a per-player AI mob per caster would cost far more than a prop. Spirit commands are limited to what this prop supports: link toggle, aura, emitter.

### 4.0 The redo after the four reference stills (owner request: "make the salamander more like the references")

The first version only approximated the references (its own gap list: "the silhouette is still more crocodile-like, the small form crouches instead of clinging, the big form is not upright or muscular, the heads are boxy"). Both forms were rebuilt in place (same ids, same file names); the old models, sheets and clips are kept next to them as `firecast_salamander_v1.*` and `firecast_dragon_v1.*` (geo, animation, texture), unused by the game. Regenerate everything with `python3 -B tools/gen_salamander_geo.py` (`--count` prints the quads, `--sync` writes them into `firecast/FireCastLod.java`).

What each reference gave, and what was built from it:

| Reference still | What it shows | What was built |
|---|---|---|
| 115 (small red form on the shoulder) | a scaled lizard-dragon CLINGING: forelimbs gripping the edge with long clawed fingers, arched back, a thick plated tail that curls up over the back and ends in a hooked spike, a short wide wedge head with a brow ridge, jagged jaw, swept crest and cheek spines, big yellow slit eyes, small bat wings with finger bones, spikes down the spine, dark maroon shadows with lit plate edges | arched spine (three boxes laid along an arched curve, deep chest), forelimbs that reach over the shoulder's front edge (3 two-segment clawed fingers each), hind legs braced on the back edge, a 12-bone tail curve of boxes with a plate spike and two side spines on each segment and a barb on the tip, a short wide head (blunt two-step snout, cheeks, slanted brow bars with a spike each, two cheek frill fans, jagged jaw line, fangs, 4-spike swept crest), two rows of spine spikes, wings with an arm bone, four finger bones (a claw on each) and four membrane panels, flat-cel plates (dark outlines, lit upper edges, darker plates on every top face) |
| 116 (big form, front) | an upright muscular torso (pecs, sternum, ribs, abs, obliques), broad shoulders, a long thick neck, a horned head with a spiny frill and an open fanged mouth, huge raised bat wings with veined membranes, big hands with red claws, smooth golden-orange skin with thin crease lines | a stack of seven torso slices (narrow waist, broad chest) wearing ONE painted front (`torso_front`: pec borders, sternum, ribs, six-pack, V of the obliques, soft shadows, outlines), cut into windows per slice; real relief on top (two pectoral boxes and six ab blocks standing out, each with its own window of the same picture); deltoid and trapezius boxes; a 4-bone neck on an S-curve (thick at the base); a big head (the small head scaled 2.4 with a long jaw, swept horns, cheek frills, crest, fangs) whose jaw is open at rest; wings raised 46 degrees and tilted so the membrane faces the viewer; hands with four two-segment fingers and red claws |
| 117 (golden dragon, side) | long neck, spotted scales with red rings, a spiked tail that curls up at the end, big wings with long finger bones and veined membrane, fire licking from wings and tail | red ring spots (thin dark outline, orange centre, some open rosettes), a 14-bone tail that trails down and curls up with a plate spike and two side spines per segment, neck spines, four long finger bones per wing, flame cards of the glow-only model on the wing tips and the tail's barb |
| 118 (giant spirit hovering, breathing fire) | legs tucked, arms forward with open hands, wings spread wide, leopard spots, fire blast | thighs forward and shins down (tucked), forearms forward, four-fingered hands, the fly clip beats the wings about the body axis; the breath comes out of the open jaw, whose mouth furnace glows |

**How the shapes are built (and why it stays cheap).** Silhouette first, texture for the rest: the head, the neck, the wing fingers, the claws and the spikes are geometry; the skin detail is painted. Every limb and the tail are boxes laid between joint points (`Model.limb`), every bone has its pivot at a joint, so the one animation file drives all three levels of detail. Shapes that are not boxes are FLAT CARDS (one quad each, alpha-cut by the picture, nothing is culled): spikes, claws, fangs, the cheek frills, the eyes (a round slit eye with an outline), and the four membrane panels of each wing. A panel is cut to the wing's real polygon (scalloped edge between two finger tips, veins, bone lines) and rides on its own finger bone, so the finger bones flap with a lag and the membrane follows. The painted sheets are the anime cel look: flat colours, one shadow tone, one light tone, dark outlines drawn into the texture (small form: overlapping shield plates, red with darker plates on every top face and a lighter belly; big form: smooth golden skin with crease lines and ring spots, a sculpted torso front, red claws). Glow-only models keep the eyes, the furnace of the mouth and the flame licks, drawn additive from a half-size glow sheet (safe additive rule: nothing under a transparent texel; the scan reports 0 offending texels).

**Animations** (`animations/entity/firecast_*.animation.json`, generated by the same script; rotations are added to the bones' rest rotations, wings and tail move by bone rotation only): `idle` (breathing, head turn, tail sway, wing twitch), `perch` (small form clinging: the hands tighten, the head looks about, the tail sways, the wings flutter; the big form's slower hover), `fly` (the wings beat about the body axis with the four finger bones fanning with a lag, the body bobs, the legs dangle or tuck, the tail streams), `breathe` (plays once: head draws back, jaw opens wide, chest swells, the wings spread, the strike forward, the tail flicks).

### 4.1 Levels of detail and the exact quads per frame

**Owner-approved raised limits** (the redo asked for more silhouette, so the limits of the first version were raised by the owner): a full model may take up to 700 quads (small) / 900 (dragon), a half model up to 300 / 380, a low model up to 120; the shared frame cap is 3200 quads (was 2400); the cull distances are 64 blocks (small) and 112 blocks (dragon) (were 56 and 96). Unchanged: three levels, one dragon per player and 6 on the server, 48 spirits on the server, the 60-tick dragon, nearby-only packets, nothing allocated per frame, the shared frame budget stepping a form down to a lower level when it is over budget. NEW with the raise: the CASTER'S OWN Salamander is never dropped by the budget (when nothing fits the frame it is drawn at the low level anyway, a bounded overshoot of one low form). The numbers below are counted from the model files; `FireCastLodTest` reads the files and the doc and fails if they disagree.

| Form | LOD | When (distance) | Body quads | Glow-only model quads | Face plates (full level only, the largest face) | Quads per frame |
|---|---|---|---|---|---|---|
| Small (shoulder) | full | up to 14 blocks | 486 | 13 | 3 | 502 |
| Small | half | up to 30 | 278 | 13 | 0 | 291 |
| Small | low | up to 64 | 98 | 0 | 0 | 98 |
| Small | culled | beyond 64 | 0 | 0 | 0 | 0 |
| Big dragon | full | up to 30 | 618 | 17 | 3 | 638 |
| Big dragon | half | up to 60 | 314 | 17 | 0 | 331 |
| Big dragon | low | up to 112 | 91 | 0 | 0 | 91 |
| Big dragon | culled | beyond 112 | 0 | 0 | 0 | 0 |

The face plates are NEW with the moods (4.2): a face (not the calm one) draws at most 3 more quads on the full level, so a full small form is at most 502 quads and a full dragon at most 638, still under the owner-approved ceilings (700 / 900); the cost of a full form in the frame budget always includes them. The body and glow-only models, the three levels and their numbers are exactly as before.

(Before the redo: small 240 / 170 / 86 quads, dragon 254 / 184 / 86, frame cap 2400, ranges 14 / 30 / 56 and 30 / 60 / 96. The ranges are the distances at the best effect quality; a weaker machine gets the lower levels sooner, down to half the distance.)

The glow-only model is drawn additive with its own half-size glow sheet. No billboards. Wing and tail animation is by part rotation. Textures: the colour sheet per form (small 512 x 512, big 1024 x 1024), `_lod1` half size, `_lod2` quarter size, `_fx` the glow sheet at half size.

A **shared frame budget** `FireCastLod.FRAME_QUADS = 3200` quads for all Salamanders, spirals and balls in one frame: the nearest are drawn at the level their distance asks for, then each is stepped down a level until it fits (`QuadBudget`, `FireCastLod.fit`), then culled; the caster's own form is forced in at the low level when the budget is spent (`fit(..., own)`, `QuadBudget.force`). Nothing is allocated per frame (a static scratch array, static tables). The painter decides `own` from the entity's owner (`e.target() == mc.player`), client side only.

### 4.2 Moods: poses, faces and creature sounds (the Salamander does not speak)

Owner request: let the spirits show different poses, faces and voice lines where that makes sense. The Salamander is a fire lizard-dragon and does NOT speak, so it has poses (new clips on both forms), faces (plate swaps on the head) and creature sounds (a trill, a hiss, a roar; no word anywhere). The server picks them from what happens; the clients only play what the synced param says. Everything below is behind two SERVER switches in `nusmp-firecast-server.toml`: `magic.salamanderMoods` (poses and faces, default true) and `magic.salamanderSounds` (creature sounds, default true; false also gives the big form's summon its plain Ravager roar back). Both false = the Salamander perches, flies and breathes exactly as before.

**Poses (new animation clips; the four old ones, `idle`, `perch`, `fly`, `breathe`, stay first and unchanged; all written by `tools/gen_salamander_poses.py`):**

| Form | Clip | Length | What it shows |
|---|---|---|---|
| small | `grip_tighten` | 1.4 s | the hands clench the shoulder's edge, the body sinks, the wings fold, the tail curls tight |
| small | `head_tilt` | 1.8 s | a curious look: the head turns and rolls to one side |
| small | `tail_flick` | 1.2 s | the tail curls aside and snaps back |
| small | `wing_stretch` | 2.2 s | the wings open flat and wide with the fingers fanned, the chest arches |
| small | `yawn` | 2.4 s | head tipped back, the jaw wide and held, the chest filling |
| small | `flinch` | 0.7 s | a hurt jerk: body snapped back, head thrown up, jaw gaping, wings flared |
| small | `puff_up` | 1.8 s | proud: the chest swells, the head lifts, the wings open a little |
| small | `sleepy_curl` | 4 s, loops | settled low on the shoulder, head tucked down and aside, wings folded, tail curled, slow breathing |
| small | `breathe` (existing) | 1.6 s | the fire breath of a cast |
| big | `hover` | 2.8 s, loops | the new base: slow strong wing beats, a calm bob, legs tucked |
| big | `roar` | 1.6 s | head thrown back, jaw gaping, chest swelling, arms open, wings spread |
| big | `wing_display` | 1.6 s | the wings open to their full span with the fingers fanned |
| big | `claw_swipe` | 0.9 s | the right arm winds back and rakes across, the body twists |
| big | `hurt` | 0.8 s | the body jerks back, the head flings up, the jaw gapes, the wings flare |
| big | `descend` | 0.4 s | drops in from above with the wings flared, one beat to brake |
| big | `land` | 0.6 s | the wings flare to brake, the legs reach down, the body sinks and the wings fold |
| big | `breathe` (existing) | 2.0 s | the fire breath |

**Faces (plate swaps on the head, `tools/gen_salamander_faces.py`).** The model's own slit eyes are the calm face. Every other face is a tiny model per form (`firecast_<form>_fx_face_<face>`: only the bones from the root to the head and the jaw, the same pivots and rest turns as the full model, so the one animation file poses it exactly like the body) with two flat eye plates, one quad each, standing 0.25 units proud of the model's own eye cards and painted opaque so they cover them: a lidded eye socket with the eye of the mood (alert: a wide round eye with a thin pupil; angry: a slanted lid and a heavy brow; sleepy: a heavy flat lid with a thin crescent of eye; smug: a half lid with a raised lower lid and a sideways look; roaring: a squeezed slanted eye with a heavy brow and crinkles, plus a mouth plate on the jaw, the glowing inside of an open mouth). Each face has its own small sheet and a glow sheet of the same size (the iris, drawn additive). A face costs at most 3 quads (2 eyes, +1 mouth for the roaring face), drawn on the full level of detail only, before the glow-only model so the plates cover its eyes; the half and low levels keep the calm eyes. The owner's limit was +20 quads: the real number is +3.

**Creature sounds (`tools/gen_salamander_sounds.py`, 11 cues, 25 variants, about 370 KB of mono Vorbis; every file is synthesised from nothing with numpy, seeded from its own name, so a re-run writes the same audio):** `trill` (3), `chirp` (3), `growl` (2), `hiss` (2), `whoosh` (a small flame whoosh, 2), `purr` (a content purr-rumble, 2), `yawn` (2), `whimper` (2), `flap` (small leathery wings, 2), `roar` (the big form, 3), `dragon_flap` (heavy wing beats, 2). Sound events `nusmp:salamander.<cue>` are registered from the catalogue (`SalamanderSoundSet`), the entries are appended at the END of `sounds.json`, the subtitles are in `tools/lang/salamander_moods.json` (a lang fragment, merged by the owner's lang merge). They are played by the SERVER only to players within 32 blocks, as a sound packet to each allowed listener, through a per listener gate built on the voice limiter (`audio/voice/VoiceLimiter`): at most one Salamander sound per 3 seconds for a listener and no cue inside its own cooldown (trill 5 s, chirp 5 s, growl 4 s, hiss 4 s, whoosh 2 s, purr 10 s, yawn 20 s, whimper 10 s, flap 6 s, roar 5 s, dragon flap 5 s).

**Which situation gives what (`SalamanderMoodRules`, pure and tested; priority decides what may interrupt what):**

| Situation | Small form: pose, face, sound | Big form: pose, face, sound | Cooldown |
|---|---|---|---|
| link (the key) | `grip_tighten`, alert, trill | n/a | 2 s |
| unlink (the key) | no pose (the prop goes at once), whoosh | n/a | 2 s |
| cast, light page (under 8 % mana) | `breathe`, alert, whoosh | `breathe`, roaring | 0.6 s |
| cast, mid page (8 to 20 %) | `breathe`, angry, growl | `breathe`, roaring | 0.6 s |
| cast, heavy page (20 % and up; these call the big form) | `breathe`, roaring, growl (the big form is out, so the dragon breathes) | the summon: `descend` while it arrives, then `breathe`, roaring face, and its roar | 0.6 s |
| owner took damage | `flinch`, angry, hiss | `hurt`, angry, roar | 2.5 s |
| idle for 10 s | `head_tilt` (alert, trill), `tail_flick`, `wing_stretch` (smug, flap), `yawn` (sleepy, yawn), in turn, a gap of 12 to 18 s between | none (it only lives 3 s) | by the idle clock |
| idle for 90 s | `sleepy_curl` (20 s), sleepy, purr; any event wakes it | none | 90 s |
| owner on low health (under 30 %) | `grip_tighten`, alert, trill | `wing_display`, angry, dragon flap | 20 s |
| owner killed something | `puff_up`, smug, chirp | `roar`, roaring, roar | 5 s |
| owner struck with their hand or weapon | none | `claw_swipe`, angry | 2 s |
| owner died | sound only, whimper (the props are removed at once, so only the whimper is heard) | the same | 5 s |
| the big form leaves | the last 12 ticks: `land` (no packet: the clients read it from the dragon's life) | | |

Nothing flickers: a pose is held until it ends unless something of a higher priority comes (hurt 6 and the owner's death 7 outrank a cast and a kill 4 and a link 3, which outrank the idle poses 1), two poses are at least 1 second apart, every situation has its cooldown, and one Salamander makes at most one sound per 3 seconds (a pose that is not allowed now may still let its sound through if the gap allows; a hit that kills does not make it hiss, so the whimper is heard). The pose ends by itself: the server writes a pose id and a start tick into the prop's one synced param (bits 4 to 7 the pose, bits 9 to 11 the face, the old stamp bits the start tick; the old encoding is still read the old way) and sends nothing when it ends; each client works out the clip, its time and the face from the clock (`SalamanderMoodRules.look`, allocation free). The dragon's descent and landing are read from its own life, no packet.

Server side, how it hooks in: `FireCastEvents` (a `LivingDamageEvent.Post` listener for the owner's hurt and strike, the death listener for a kill and for the owner's death), `FireCastProps` (the Salamander prop's own tick, once every 5 ticks, runs the idle clock and the low health check), `Salamander` (link, unlink, cast, summon), `FireCasting.State` (one pure `Tracker` per player, a dozen numbers, gone with the player's state on logout, death, respawn and dimension change). `SalamanderSounds.forget` drops a listener's gate record on logout.

**Worst-case cost of the moods (rule 11).** Server: no new entity; no new packet type; one entity-data update of the prop's param per pose change, nearby players only (the vanilla tracking range), at most one per second per Salamander (the minimum pose gap) and in practice one per few seconds; the idle and low health checks are a few integer compares on a tick that already ran (once per 5 ticks per Salamander, the same as before); the hit, kill and death hooks cost one map lookup for a player without a Salamander; sounds: at most one packet per listener per 3 seconds, to listeners within 32 blocks, so even 48 Salamanders in earshot send one listener at most 20 sound packets a minute. Client: at most 3 more quads for a face on the full level (a full small form 502, a full dragon 638 quads, both included in the frame budget of 3200), one more small texture pair per face used, no allocation per frame (a static table of face specs, primitive math for the pose), the pose clips are bone rotations like the old ones.

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

Client per frame: see 4.1 (at most 3200 quads shared, plus at most one forced low form of the caster's own: 3200 + 98 = 3298 quads in the worst case; the busiest legal frame is six full dragons at 635 quads, which the budget steps down to half and low levels and culls beyond the cap). A ball costs at most 14 quads (`BALL_QUADS`), within 64 blocks. A spiral at most 26 sprites (`SPIRAL_MAX_POINTS`: `10 + 4 x level`, thinned with distance), within 48 blocks. Embers are spawned once per tick by custom particles through `Fx`, within the particle budget.

## 6. NEW / REPLACED / KEPT

| | What |
|---|---|
| NEW | the whole `firecast` package (rules, ledgers, LOD maths, server config, net, events, self-test), Magna's Soul Bat (item, 3D model, glow, engravings, Better Combat attributes, club class, grip, creative tab, Magna canon-grimoire grant), the Salamander (both forms, 4 LODs each, skin scripts, animations, renderer, key), the Leopold spiral prop and renderer, the Mereoleona punch combo, six player animation clips, lang fragment, previews, tests. With the redo: the geo kit helpers (`tools/firecast_geo_lib.py`: frames, limbs, cards, texture windows), the owner-approved raised LOD limits and the never-dropped own form (`FireCastLod.fit(..., own)`, `QuadBudget.force`), `--sync` of the quad table, the `_v1` copies of the first version, new asset tests |
| REPLACED (owner asked for a closer likeness, rule 5) | the geometry, sheets (colour, glow, LODs) and animation files of both Salamander forms (`firecast_salamander*`, `firecast_dragon*`: same ids and file names, built in place); the quad table, ranges and frame cap of `FireCastLod`; the placement numbers of the big form in `FireCastDraw` (its model origin is now the pelvis); the preview scenes' views and the three pictures in `docs/previews/fire_casting/` (`salamander_small.png`, `salamander_dragon.png`, `salamander_vs_reference.png`) |
| KEPT | all Fire page ids and gameplay numbers (the style only changes who releases the page, from where, and how the old multipliers are fed), the old flame props and effects, the exclusive `ElementalSpiritLordEntity`, every other caster's generic casting (and everybody's, with `magic.fireCasting=false`), the first version of the Salamander models as `*_v1` copies (unused), the clip names (`idle`, `perch`, `fly`, `breathe`) and the bone names `body`, `head`, `jaw`, `wing_*`, `wingout_*`, `t0`.., `tail_tip`, `arm_*`, `fore_*`, `hand_*`, `leg_*`, `shin_*`, `foot_*` (the old single `neck` bones became the chain `neck1`..`neck4`, new are `chest`, `wf1_*`..`wf4_*`), the server side of the Salamander (rules, emitter positions, ledgers) |

**With the moods (round 1.99):**

| | What |
|---|---|
| NEW | 15 pose clips (8 small, 7 big) and the pose table, 5 face models per form with their sheets, 11 creature sound cues (25 files), the sound events and their gate, the mood rules and tracker, the two server switches `magic.salamanderMoods` and `magic.salamanderSounds`, the self-test `magic.salamander_moods`, the lang fragment `tools/lang/salamander_moods.json`, the scene and preview tools, two pictures, tests |
| REPLACED | only what the new poses take over in the game's choice of clip: the dragon's base clip is now `hover` (it was `fly`), and with the creature sounds on, the dragon's summon plays the new `roar` (to nearby players, through the gate) instead of the plain Ravager roar (`magic.salamanderSounds=false` gives the Ravager roar back); the quad table of the doc and `FireCastLod.cost` now include the face plates |
| KEPT | the four old clips of both forms (`idle`, `perch`, `fly`, `breathe`, unchanged and first in the files), every model, level of detail, sheet and quad number of the body and the glow-only models, the old encoding of the param (still read the old way), all server rules, ledgers and ids, the vanilla fire-charge sound at the link, the `*_v1` copies |

Existing files touched (hooks and append-only one-liners): `book/GrimoireBook` (style hooks in `onPressed`, `onHeld`, `onRelease`, `castReady` split into `castReady` + `castReadyBase`, `castPage` refuse gate and `FireCasting.run`, `size()` / `hurtAs()` multipliers, `vfx()` origin), `book/SpellRuntime` (bolt start origin + homing), `book/TensuraShots` (spawn origin), `anim/CastAnim` (constants), `prop/PropKind` (+`FIRECAST_1`, `FIRECAST_2`, appended), `prop/PropRegistry`, `client/prop/PropPainterRegistry`, `NUSMP` (boot lines), `NUCreativeTab`, `client/WeaponRenderer`, `item/WeaponEngravings`, `blackclover/GrimoireItem` (Magna canon grant).

## 7. Weapon: Magna's Soul Bat

Real 3D weapon in hand (`tools/gen_magna_bat_model.py` through the hook in `tools/gen_weapon_models.py`, drawn by `client/WeaponRenderer`; mesh `weapon_meshes/magnas_soul_bat.txt`, grip entry `gripxyz`), the old sprite is the inventory icon, glow texture is safe additive only. Club class (`weapon_classes.json`), Better Combat attributes (parent `bettercombat:mace`). Tensura engravings (rule 6): crushing 2, elemental boost 2, magic weapon 1, sturdy 1. Attack +6, speed -2.8. Preview: `docs/previews/fire_casting/magnas_soul_bat.png`.

## 8. Tests

* `firecast.FireCastRulesTest`, `FireCastLodTest`, `FireCastAssetsTest`, `FireCastSourceScanTest` (pure JUnit, data and asset checks, and source scans: the server decides, no client class from common code, no per-frame allocation in the painter, a unique config file name);
* self-test key `magic.fire_casting` (`FireCastSelfTests`, in the CI server smoke test);
* the guards that scan it: `FxSourceScanTest`, `PerfSourceScanTest`, `ServerSafetyTest`, `ConfigFileNamesTest`, `WeaponClassesTest`, `WeaponMeshesTest`, `GripTableTest`, `WeaponSizesTest`, `GeoAssetsTest`, `CastAnimAssetsTest`, the safe additive texture scan (scope `entity` for the two `_fx` sheets), `WeaponAttributesTest`, `FranchiseDataTest`.
* the redo added: `FireCastLodTest` (the exact quads of the files equal the table and the doc, the owner-approved ceilings, the own form never dropped, the frame budget), `FireCastAssetsTest` (the `_v1` copies, the rig with four finger bones per wing and the tail chain, the silhouette cards, the glow-only models, the level models keep the silhouette bones); `SafeAdditiveTextureScanTest` covers the two glow sheets (`scan_textures.py --fix` reports 0 offending texels);
* the moods added: `SalamanderMoodRulesTest` (the table event to pose, face and sound for both forms, the cast tiers, the cooldowns and priorities, the minimum pose gap, one sound per 3 seconds, the idle clock, a long random day that never flickers, the param word, the client's reading of it, the per listener gate), `SalamanderSoundsTest` (the catalogue against `sounds.json`, the files, the subtitles and the size), `FireCastLodTest` (the face quads and the table with its face column), `FireCastAssetsTest` (the new clips come after the four old ones, the face models and sheets, the plates cover the model's own eye cards), `FireCastSourceScanTest` (the moods are server decisions behind their switches), self-test key `magic.salamander_moods`;
* previews: `python tools/render_preview/preview.py firecast_...` (scenes in `tools/render_preview/scenes.d/firecast_*.json`), the punch in `tools/vfx_preview/scenes.d/firecast_punch.json`; pictures in `docs/previews/fire_casting/` (`salamander_poses.png`, `salamander_faces.png` for the moods; scenes in `tools/render_preview/scenes.d/firecast_salamander_moods.json`, written by `tools/gen_salamander_scenes.py`).

## 9. Not verified (honest list)

* **Nothing was run in a game or on a server.** The type check was against API stubs and the JUnit run in a shim without Minecraft; tests that need the game classes (for example `MagicEnumAuditTest`) did not run in the shim and are left to CI.
* **The look is closer, not identical.** Compared side by side with the four stills (`salamander_vs_reference.png`, rendered by the project's headless rasteriser) the redo now has the right pose and proportions: the small form clings with an arched back, a short wide head, a curled plated tail and small finger-boned wings; the big form is upright and muscular with a sculpted chest, ribs and abs, a long S-curved neck, a horned head with an open fanged mouth, huge raised veined wings and red-clawed hands, legs tucked, a tail that curls up. What is still different: the models are built from boxes and flat cards, so curves (the neck, the pectorals, the head, the limbs) are stepped, not smooth like the drawing; the anime's ink outline round the whole silhouette is only painted into the textures, not drawn round the model; the big form's head is still the blockiest part; there are no feathery flame effects on the body (only the licks on the wing tips and the tail's barb and the mouth glow); the glow of the eyes only shows through the additive pass (the geo-only previews show the eyes without it, so they look duller than in the prop previews). All of it was judged on headless previews only.
* Geometry and clip rotation signs were checked only in the headless preview; the in-game player animation (bat swing, punches, charge pose) was never seen, so the contact frames may need a retime.
* The default key for the Salamander toggle (backslash) is a guess and may clash with a pack's keys; it is rebindable.
* Balance feel (ball speed, finisher counts, charge costs, dragon cooldown) is untested.
* Mereoleona's held-punch variant and Fuegoleon's flame forearm are not built. The spirit commands are only link toggle, aura and emitter (no AI follow, no attack commands).
* The Salamander's `ElementalSpiritLordEntity` behaviours (tamed follow, commands) are not shared: this is a prop, deliberately cheaper.

* The moods (4.2): nothing was heard or seen in a game. The pictures are the headless rasteriser's (the pose sheet and the face sheet); how the poses read in motion, whether the face plates z-fight with the model's own eye cards at the far end of the full level (14 and 30 blocks) and how the synthesised sounds actually sound (they were only checked as spectrograms and by size, length, peak and mono) all wait for a game. The sounds are procedurally synthesised creature calls, not recordings: expect them to be rough. The sleepy curl and the idle poses are timed by the clock only (not by whether the owner moves), so a Salamander may yawn while its owner is fighting if nothing hit them for 10 seconds; the tuning numbers (10 s, 90 s, 30 % health, the cooldowns) are guesses. The owner's death and the unlink have no visible pose: the prop is removed at once (a graceful exit would need the prop to outlive its link), so only their sound is heard. The listener gate does not count a refused cue against the 3 second gap, but a sound that is allowed by the gate and then lost on the network is simply lost.
* The face plates cover the model's own eyes by depth (0.25 units in front): they only appear on the full level (within 14 blocks for the small form, 30 for the dragon); at the half level the calm eyes stay whatever the mood.

## 10. Sources

Rule 10 (English wiki first, Japanese wiki fallback): both `https://blackclover.fandom.com/wiki/Salamander` (English) and the Japanese Black Clover wiki page for the Salamander (サラマンダー) **could not be fetched in this session** (`getaddrinfo ENOTFOUND blackclover.fandom.com`: no network route to fandom.com). The Salamander's design was therefore taken from the four reference images the owner supplied (small red form on the shoulder, big orange muscular form, golden-orange spotted form with red rings and veined wings, giant spirit above the caster breathing a fire blast), not from the wiki text (for the redo of the models the four stills were read again, image by image, and the wiki was not retried). The four casting styles are taken from the owner's brief file (the owner's description of how Magna, Mereoleona, Fuegoleon and Leopold fight), not from wiki spell lists; page names and numbers are this mod's own and unchanged.

## Update 3.0.0: controls moved to the spirit control plane

The Salamander key (the backslash) is gone. Summon and release the Salamander with the Fire grimoire's "Summon Spirit / Release Spirit" page, or `/multiverse spirit summon salamander` / `release`; its big form is the page "Salamander Dragon" or `/multiverse spirit ability dragon`; "Spirit Combat Mode" lets it cast fire bolts with its mana and yours. Only the key was removed: the link, the caps, the looks and the sounds are as above. Full description, numbers and costs: `docs/magic/spirit_control.md`.
