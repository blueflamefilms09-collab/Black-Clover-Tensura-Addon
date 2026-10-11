# Spell-casting body animations: production guide

How to give every grimoire spell a full-body cast (wind-up, magic circle, release, recoil or a floating hold) in the style of
Tensura's casting, from Blockbench to in-game. Written for **this** mod: NeoForge 1.21.1, Tensura Reincarnated, GeckoLib 4
(already a dependency), the existing chant / Mana Zone / release flow in `book/GrimoireBook.java`, and the VFX layers that
already draw the magic circles.

> Status (0.59): built, without Player Animator. Clips live in `assets/nusmp/animations/player/cast.animation.json` (made by `tools/gen_cast_animations.py`), the server picks them in `anim/CastAnim`, `client/CastAnimClient` plays them from `PlayerModelMixin`. Chant and Mana Zone loop; releases include thrust, side, up, sweep, slam, the signature finisher, the fail flinch, sealing's two-handed crush, and weapon-specific motions. A page can pick its own with `BookPage.withAnim(...)`; `BookPage.withWeapon(...)` gates it on a held tagged weapon and overrides that ordinary release with the matching weapon clip.

---

## 0. Pick the right tool for each body

| What moves | Tool | Why |
|---|---|---|
| **The player** (casting, slashing, charging, floating) | **Player Animator** (KosmX's library) | It plays Blockbench keyframes on the vanilla player model, blends them over walking and looking, and fades in and out. It also works with the GeckoLib robes and the mode-armor overlays: they hang on the player's model parts, so they follow the pose. |
| **Mobs and summons** (Spirit Lords, painted beasts, future captains) | **GeckoLib** (already used: `SpiritLordEntity`, `RobeItem`) | They are custom models anyway, and `triggerAnim` is already wired (see `SpiritLordEntity.customServerAiStep`). |
| **Magic circles, slashes, rings, auras** | The existing **VFX layers** (`VfxShape.MAGIC_CIRCLE`, `MANA_CHARGE`, `WIND_RING`…) | Keep circles as VFX, **not** as Blockbench models or entities: no entity, no hitbox, and they already scale to the 400-vertex budget. The animation only decides *when* they spawn. |
| **Real Double clones** (`MirrorDoubleEntity`) | Not Player Animator (it only animates real players) | Either convert the double to a GeckoLib player-shaped model with the same keyframes, or pose its `PlayerModel` by hand in `MirrorDoubleRenderer`. |
| **Trailers** | Blockbench → glTF → Blender | Section 5. |

**About MCreator:** it can't import into this hand-written NeoForge codebase. Use it only to prototype a pose in a throwaway workspace; everything that ships goes through the code below.

**What Tensura itself does:** we inspected its jar (`.github/inspect-request.txt`, Oct 2026). It bundles no animation library; it poses the player with its own render mixins (`MixinPlayerRenderer`, `MixinItemInHandLayer`, `MixinAbstractClientPlayer`). The *feel* of its casting comes from short, sharp poses timed with circles and particles. That is the target here, with better tooling.

---

## 1. Anatomy of a Tensura-style cast

Every cast has five beats. The mod already has the server-side moments for each one; the animation just has to land on them.

| Beat | What the body does | Where it happens in the mod | Ticks (20/s) |
|---|---|---|---|
| **1. Anticipation (wind-up)** | It moves *away* from the release: the casting arm pulls back, the torso twists against it, and the weight sinks. | `onHeld`, `heldTicks == 1` (the `MANA_CHARGE` VFX starts here) | 0-6 |
| **2. Chant hold** | Gathering: a slow breathing loop, the off hand raised and open, small sway. The magic circle spins under you. | `onHeld` until `castTicks` (20, or 8 when mastered) | 6-20 |
| **3. Overcharge (Mana Zone)** | The body lifts and opens: arms spread, chest up, heels off the ground (visual hover). | `onHeld`, `heldTicks == need * 2` (the `WIND_RING` VFX fires) | 40+ |
| **4. Release (the contact frame)** | The fastest motion of the cast: the thrust, sweep or slam. The spell VFX spawns on this exact frame. | `onRelease`, after `p.cast().cast(...)` succeeds | 0-3 after release |
| **5. Recoil and recovery** | Overshoot past the end pose, kick back from the spell's force, then settle into idle. Heavier spells mean bigger recoil and a longer settle. | Client only (no server event needed) | 3-16 after release |

**The key rule: no new input lag.** The chant already makes the player wait 20 ticks, so do the wind-up inside the chant and put the
release animation's contact frame at **0-2 ticks**. Never delay the damage to wait for the animation. If a pose needs a longer
release, start it *before* release, in the chant's last 4 ticks.

The generated attribute-specific rune overlays are stored under `assets/nusmp/textures/particle/magic_runes/`; regenerate them after
adding a `MagicType` with `node tools/gen_magic_runes.mjs`. High-detail ring and band textures are also available for selected attributes;
generate them with `python tools/gen_magic_runes.py <output-directory>` (requires Pillow). Open-book page flips prefer the high-detail ring
when one is available and keep the base overlay for the other attributes.

### 1.1 Keyframing principles (Blockbench)

1. **Set Blockbench to 20 fps** (Animation panel → snapping **20**). Then every keyframe lands on a game tick: 0.05 s = 1 tick. Ticks are what the server sends, so the timing lines up exactly.
2. **Pose-to-pose, then breakdowns.**
   - Key the 3-4 *story poses* first (rest → wind-up → contact → recoil → rest).
   - Play it. Only then add breakdowns between them.
3. **Snappy in, soft out.** This is what makes anime casting read as powerful:
   - Wind-up: ease **in** (slow start, fast end), *Bezier*, or *Linear* with an extra key close to the end.
   - Contact: **1-2 frames** from wind-up to the full extension. Fast is the point; don't smooth it.
   - Recoil: overshoot 10-20 % past the contact pose, then *ease-out* back over 6-12 frames.
4. **Drive from the hips.** The body moves first, the arm follows 1 frame later, and the hand/head follow 1 more frame later (follow-through). On the vanilla model that means keying `body` one frame before `right_arm`.
5. **Hold the contact pose for 2-3 frames.** The eye needs a still frame to read the shape while the VFX flash plays.
6. **Hit symmetric poses with asymmetric timing.** Two-handed spells look stiff when both arms arrive together; offset one arm by 1 frame.
7. **Head stays on target.** Don't key the head's yaw in cast animations; let the player's look direction drive it, so the aim stays readable in PvP. Only key head *pitch* for dramatic chin-up or chin-down moments.

### 1.2 The core animation set

Angles are Blockbench degrees on the player template (X = pitch, Y = yaw, Z = roll). **X −90 on an arm points it straight
forward.** Check the sign of each axis once in the preview before keying a whole set.

The summoned grimoire floats at the **right hand**, so the **left arm does the gestures** and the right arm stays within about 30° of rest.
Otherwise the hand drifts away from the book (see 4.1).

#### `nusmp:cast_chant` (loop, 1.0 s / 20 ticks): every page while the key is held
| t (s) | body | left_arm | right_arm | notes |
|---|---|---|---|---|
| 0.00 | X 0 | X −20, Z 15 | X −25 | from rest |
| 0.15 | X 6 (lean in), Y 10 | X −110, Z −10 | X −30 | wind-up: left palm raised over the book |
| 0.50 | X 4, Y 8 | X −100, Z −14 | X −28 | breathing: ±4° drift |
| 1.00 | X 6, Y 10 | X −110, Z −10 | X −30 | loop point = 0.15 pose (set "loop", start the loop at 0.15) |

Legs: knees bent with `left_leg` X −8 and `right_leg` X 6 (a combat stance). Add a body **position** Y of −0.5 px to sink the weight.

#### `nusmp:cast_release_thrust` (0.8 s / 16 ticks): bolts, rays, spears (starter and mid pages)
| t (s) | body | left_arm | notes |
|---|---|---|---|
| 0.00 | X 6, Y 25 | X −110, Z −10 | the end of the chant, twisted away |
| 0.05 | X −4, Y −15 | X −95, Y −10 | **contact** (1 tick): the body unwinds, the arm shoots forward |
| 0.15 | X −6, Y −20 | X −98 | hold (VFX flash) |
| 0.25 | X 4, Y −8 | X −70, Z 10 | **recoil**: the arm kicks up and back |
| 0.80 | X 0, Y 0 | X 0 | settle (ease-out) |

#### `nusmp:cast_release_sweep` (0.9 s): slashes and arcs (Mirrors Slash, Wind Blade, anti-magic slashes)
- Wind-up: the left arm goes across the chest (Y 60, X −80) and the body twists Y +30.
- Contact (2 ticks): the arm sweeps to Y −70 and the body to Y −25.
- After the contact, hold for 3 ticks, then recoil and settle.
- If a slash spell sends a VFX arc, spawn it at the contact frame with its start angle matching the wind-up side.

#### `nusmp:cast_release_slam` (1.1 s): zone pages (meteorites, pillars, domes)
- Both arms go up (X −170), the body arches back (X −12), and it **rises 2 px** (body position Y +2).
- Contact: both arms drive down to X −40, the body to X 20 and position Y −2 (a crouch). Spawn `MAGIC_CIRCLE_EXPLOSION` on this frame.
- Recoil is small; the weight stays down for 6 ticks, then the body rises.

#### `nusmp:cast_mana_zone` (loop, 1.6 s): the overcharge hold (sustained floating stance)
- Arms spread (Z ±70, X −30), palms open. The chest lifts (body X −8). Legs hang: X 10 / 4, toes pointing down.
- **Visual hover:** a body position Y bob between +1 and +3 px, a sine wave over the 1.6 s loop.
- A real hover (moving the hitbox off the ground) must be done on the server, e.g. a short `slow_falling` + small upward velocity in `onHeld`. Never fake gameplay height with the animation alone.

#### `nusmp:cast_signature` (1.6 s): signature pages (Real Double, Full Reflection, captain spells)
- Pattern: chant pose → a 4-tick "breath in" (body X −10, arms half-open) → a 2-tick explosive spread (arms Z ±95) → a 6-tick hold → a slow 12-tick settle.
- The longer hold is what makes it feel like a finisher. Pair it with the page's own big VFX.

#### `nusmp:cast_fail` (0.5 s): the chant broke off, sealed, or out of magicules
- The off hand drops sharply (2 ticks), the body flinches back (X −6), then a slow recovery. It tells the player the cast fizzled without a chat message.

### 1.3 Which page plays which release

`BookPage` has no animation field yet. Derive it from the existing tier (cost percent) and let a page override it:

| Page factory | `costPercent` | Default release |
|---|---|---|
| `starter` | 8 | `cast_release_thrust` (fast, small recoil) |
| `mid` | 12 | `cast_release_thrust`, or `sweep` for slash spells |
| `zone` / `daily` | 20 | `cast_release_slam` |
| `signature` | 35 | `cast_signature` |

Add the override as a new `BookPage` method (e.g. `.withAnim("sweep")`), next to `withCooldown`, so no existing call changes.

### 1.4 Syncing the magic circle and the VFX to the animation

The circle is already spawned by the server (`castCircle`, `MANA_CHARGE`, `MAGIC_CIRCLE_EXPLOSION`). To make it feel *summoned by the gesture*:
- **Chant:** spawn the circle at `heldTicks == 3` instead of 1, which is the moment the left palm finishes rising in `cast_chant`.
- **Release:** keep the spell VFX on the contact frame (tick 0-1). The animation was built around that, so nothing moves on the server.
- **Mana Zone:** the `WIND_RING` burst already fires on the same tick as `cast_mana_zone` starts. Use a 2-tick fade-in on the animation, not more, so the arms open *with* the ring.

---

## 2. Blockbench: building and rigging

### 2.1 For the player (Player Animator)
1. Use the **player template** that ships with Player Animator / Emotecraft (see their wiki).
   - **Don't rename bones, move pivots or add bones.** The library maps the bone names to the vanilla model parts, so a renamed bone just doesn't move.
   - Pivots are already at the shoulders, hips and neck.
2. Format: animate in the template's format and **export the animation as JSON** (Animation → Export Animations). Player Animator reads Blockbench/GeckoLib-style animation JSON. Put the files at
   `src/main/resources/assets/nusmp/player_animation/<name>.json` (one animation per file is easiest to manage).
3. **Elbows and knees:** the vanilla player has no elbow or knee bones. Player Animator can *bend* limbs only with its optional bend add-on, and armor and overlays won't bend with it. **Recommendation: don't depend on bends.** Build the poses from shoulder, hip and torso rotation, which is how Tensura's poses read too.
4. Name animations `nusmp.cast_chant` and so on, so they are easy to grep, and keep one Blockbench project (`tools/blockbench/cast_animations.bbmodel`) for the whole set so poses stay consistent.

### 2.2 For GeckoLib mobs and summons (Spirit Lords, beasts, a future GeckoLib Real Double)
Follow the existing layout: `assets/nusmp/geo/<group>/<name>.geo.json` and `assets/nusmp/animations/<group>/<name>.animation.json` (like `geo/armor/robe.geo.json` and `RobeModel`).

1. **Bone tree:** `root` → `body` → (`head`, `arm_left` → `hand_left` → `grimoire`, `arm_right` → …, `leg_left`, `leg_right`), plus a separate `fx` bone for glow cards.
   - Parent everything under `root`, so one key can lift the whole mob into a float.
2. **Pivots on the joint:**
   - shoulder pivots at the top-centre of the arm cube (y = 22 for a player-sized rig);
   - hips at the top of the leg (y = 12);
   - the elbow (if you add one) at the cube's joint face, not its centre.
   - A wrong pivot is the #1 cause of limbs that "slide" out of the body.
3. **The grimoire as a child of the hand,** so it follows every gesture. Its rest rotation goes on the `grimoire` bone, not on the hand.
4. Keep the depth at 4 levels or fewer and the bone count at 25 or fewer per mob (each bone is a matrix push per frame).
5. Use the same animation names as the player set (`cast_chant`, `cast_release_thrust`…), so summons can mimic their owner by name.

---

## 3. Implementation in this mod

### 3.1 Dependency
This project pulls mods from CurseMaven (see `build.gradle`). Add Player Animator the same way and pin the 1.21.1 NeoForge file:
```groovy
// build.gradle (dependencies)
implementation "curse.maven:playeranimator-658587:<fileId of the 1.21.1 NeoForge build>"
```
Then add a dependency block for it in `META-INF/neoforge.mods.toml` (`type = "required"`, `side = "BOTH"`).
Its mod id is `playeranimator`. Better Combat (supported since 0.43) already requires it, so servers running Better Combat
already have it installed.

Check the project id and the file id on the CurseForge page before committing. CI (`build.yml`) will fail fast if they're wrong.

Optional dependency: to keep the mod working without the library, make it `type = "optional"` and only touch its classes from a
class that's loaded behind `ModList.get().isLoaded("playeranimator")`. That's the same isolation trick `GrimoireGuard` uses for Tensura's events.

### 3.2 Network: the server decides, every client plays
The cast phases happen on the server (`onHeld` / `onRelease`), and *other* players must see your cast. So the server sends a tiny payload whenever the phase changes, **never every tick**:

```java
// book/CastAnim.java (new)
public record CastAnimPayload(int entityId, String anim, float speed) implements CustomPacketPayload { ... }

public static void play(ServerPlayer p, String anim, float speed) {
    PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new CastAnimPayload(p.getId(), anim, speed));
}
```
Register it next to the others (`r.playToClient(...)`, as in `GrimoireSummon.registerPayloads`). Its hooks in `GrimoireBook`:

| Where | Call |
|---|---|
| `onHeld`, `heldTicks == 1` | `CastAnim.play(p, "cast_chant", 20f / need)`: mastered chants (8 ticks) play 2.5× faster, so the wind-up still finishes on time |
| `onHeld`, Mana Zone reached | `CastAnim.play(p, "cast_mana_zone", 1f)` |
| `onRelease`, after a successful `cast(...)` | `CastAnim.play(p, releaseAnimFor(page), 1f)` |
| every `fail(...)` path in `onRelease`, and stowing the book | `CastAnim.play(p, "cast_fail", 1f)` (or `"stop"` for a 3-tick fade to idle) |

### 3.3 Client: one animation layer per player
```java
// client/CastAnimClient.java (new, client only)
static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath("nusmp", "cast");

static void init(FMLClientSetupEvent e) {
    // priority 42: above Better Combat's held-weapon poses (layers 1-4), below its attack swings (2000), so a swing always wins
    PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 42, player -> new ModifierLayer<>());
}

static void handle(CastAnimPayload msg, IPayloadContext ctx) {
    if (!(Minecraft.getInstance().level.getEntity(msg.entityId()) instanceof AbstractClientPlayer pl)) return;
    @SuppressWarnings("unchecked")
    var layer = (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(pl).get(LAYER);
    if (layer == null) return;
    if (msg.anim().equals("stop")) { layer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(3, Ease.INOUTSINE), null); return; }
    var anim = PlayerAnimationRegistry.getAnimation(ResourceLocation.fromNamespaceAndPath("nusmp", msg.anim()));
    if (anim == null) return;
    var player = new KeyframeAnimationPlayer(anim);
    layer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(msg.anim().startsWith("cast_release") ? 1 : 3, Ease.INOUTSINE),
            player);
    // speed: wrap with a SpeedModifier(msg.speed()) when != 1
}
```
- **Fades:** use a 1-tick fade into a release (the snap is the point) and a 3-tick fade everywhere else.
- **Left-handed players:** add a `MirrorModifier` when `pl.getMainArm() == HumanoidArm.LEFT`.
- **First person:** by default these animations show only in third person. For the release, turn on the library's first-person mode (`setFirstPersonMode(...)` on the animation player, in recent versions), so you see your own arm thrust. Leave it off for the chant loop; it fills the screen.
- **Your own client:** to remove the round-trip delay on the local player, start `cast_chant` the moment the key goes down (client side), and ignore the echoed chant payload for yourself. Keep releases server-driven; they only play if the cast succeeded.

### 3.4 GeckoLib summons: the same pattern the mod already uses
`SpiritLordEntity` already shows it:
- a triggerable `"cast"` controller (`cast.triggerableAnim(...)`) fired from the server with `triggerAnim("cast", name)`;
- a separate `"main"` controller for idle and movement.

For any new caster mob:
- Keep the **two-controller split** (locomotion and cast). Casting then never cancels walking, and the cast controller blends over it.
- Give the cast controller a transition length of 0-2 ticks (it's `2` in `SpiritLordEntity`), so releases stay sharp.
- Have the server spawn the spell VFX on the same tick as `triggerAnim`, matching the player rule in section 1.

---

## 4. Best practices

### 4.1 Clipping
- **The floating grimoire:** the book floats at the right hand from the player's position, not from the arm bone.
  - Keep the right arm within about 30° of rest in every animation, so the hand stays on the book.
  - If you want a two-handed gesture, move the book instead (`GrimoireSummon` already sends a float state; add an offset for "raised").
- **Arm into torso:** arms rotated past Z ±15° *inward* sink into the body. When an arm crosses the chest, put it **in front** with X −60…−90 first, then rotate Y.
- **Legs during the visual hover:** lift the body position, not the legs. Legs rotated past X ±25° stick out of trousers and robes.
- **Robes, mode armor and capes:** the GeckoLib robes and `ModeArmorLayer` follow the vanilla parts, so they move with the pose.
  - Check them in the **robe** and **devil** styles, which have long skirts.
  - Body pitch past X ±15° makes the skirt cut into the legs.
- **Head and hat:** don't roll the head (Z) past ±10°; tall hats and horns from Tensura races clip into the shoulders.

### 4.2 Hierarchy and performance
- **Player:** Player Animator touches only the vanilla parts, so it's cheap. The cost is the number of *playing* layers: use **one** cast layer per player and replace its animation; don't stack new layers.
- **GeckoLib:**
  - 25 bones or fewer, depth 4 or less.
  - No scale keyframes on deep bones (they force the whole subtree to re-scale).
  - Bake long loops into 1-2 s.
  - Keep animation names constant and build `RawAnimation`s once, as static fields (as `RobeItem.IDLE` does).
- **Network:** one payload per phase change (4-5 per cast at most), sent only to tracking players. Never send per-tick poses.
- **Resource lookup:** look up `PlayerAnimationRegistry.getAnimation(...)` once and cache the results in a map keyed by name. Don't allocate `ResourceLocation`s in render code.

### 4.3 Server and client timing
- **Ticks are truth.** The server runs at 20 tps. The client renders at any fps and interpolates with partial ticks; both libraries do this for you. Author at 20 fps (1.1) and every key lands on a tick.
- **Gameplay never waits for the client.** Damage, cooldowns and VFX stay on the server exactly where they are now. The animation is decoration timed to them, never the other way round.
- **Lag:**
  - The other players see your cast one round-trip late. That's fine, because the chant hides it.
  - Don't try to "catch up" by skipping frames; a late release still reads well.
- **Interruptions:** send `cast_fail` / `stop` on every early exit, including these:
  - chant broken, cooldown, page sealed, out of magicules (the `fail(...)` paths);
  - book stowed (`GrimoireSummon.toggle`);
  - death, dimension change (`DreamWorld` sends people across dimensions);
  - logout.

  A loop with nobody to stop it is the classic "stuck in a T-pose forever" bug.
- **Joining mid-cast:** a player who starts tracking you halfway through a 1 s animation simply doesn't see it. That's acceptable; don't sync animation state on tracking start.
- **Testing:** test with two clients and `/tick rate 5` (vanilla 1.21 command). Slow motion shows timing errors between the contact frame and the VFX.

---

## 5. Optional: cinematic trailers in Blender

1. Blockbench → **File → Export → glTF** (with animations ticked). Use the GeckoLib summon models, or a player model built from the template plus a skin.
2. Blender:
   - **File → Import → glTF**, and set the scene to **20 fps** so the keys keep their timing; or retime to 24 fps with the NLA strip's scale (1.2).
   - The grimoire model from `tools/blender/build_grimoire.py` can be parented to the hand bone.
3. The look:
   - **EEVEE** with Bloom (in 4.2+, a compositor **Glare** node, Fog Glow).
   - Emission shaders on the circle and VFX cards in the magic colour.
   - A low-key rim light behind the caster.
   - One camera push-in timed to the wind-up and a 2-frame camera shake on the contact frame.
4. Magic circles for the trailer: reuse the in-game circle textures (`assets/nusmp/textures/particle/*`) on emissive planes, so the trailer matches what players see.

---

## 6. Build order (when we implement it)

1. Add the Player Animator dependency (pinned), the payload, the client layer and the `CastAnim.play` calls. Ship it with **one** animation (`cast_chant`) to prove the pipeline in multiplayer.
2. Add `cast_release_thrust`, `cast_fail`, and the `stop` paths for every interruption.
3. Add `sweep`, `slam`, `mana_zone` and `signature`, plus the `BookPage.withAnim(...)` override for slash and zone spells.
4. First-person release, left-hand mirroring, and local-player prediction for the chant.
5. Optional:
   - convert `MirrorDoubleEntity` to GeckoLib, so Real Doubles mimic their owner's casts by animation name;
   - Spirit Lords and other summons reuse the same names.

Every step is additive. Existing pages, VFX and timings stay as they are; the animations only attach to the moments that already exist.
