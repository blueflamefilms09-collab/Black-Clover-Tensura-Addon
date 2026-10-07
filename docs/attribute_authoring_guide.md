# Attribute authoring guide (0.54: the Black Clover Magic and VFX expansion)

One document for whoever builds ONE of the 44 attributes: 29 NEW magics (their own grimoire book), 8 UPGRADES (new pages appended to a magic that
exists: Beast, Blood, Bone, Gravity, Seal, Imitation, Ice, Light) and 7 AUDITS (core magics that already exist and are checked against the owner's
master framework: Painting, Kotodama, World Tree, Dice, Slash, Compass, Mercury; Light is also audited). The scaffold is in the repo and compiles in CI:
every attribute has stub files that you REPLACE with the real thing. Other authors build the other attributes at the same time, so touch only the
files this guide gives you.

Where things are:
* `tools/attribute_table.py`: the table of all 44 (key, class names, wiki title, the owner's spec text for your attribute).
* `.github/wiki/<slug>.txt` (the magic's wiki page: description + spell list) and `.github/wiki/spell_<slug>.txt` (each spell page): the canon reference.
* `.github/api/api.tar.gz`: the dependency API (Minecraft 1.21.1 Mojang names, NeoForge, ManasCore, Tensura) as javap signatures:
  `mkdir -p S/api && tar xzf .github/api/api.tar.gz -C S/api`, then grep (`net_minecraft_world_entity.txt` holds every class of `net.minecraft.world.entity.*`).
* `docs/geo_models.md` + `tools/geo_builder.py` + `tools/geo_check.py`: how models are made (GeoLite, section 5c).
* Three local verification tools: `tools/vfx_preview` (section 4), `tools/render_preview` (section 9), `tools/typecheck` (section 10).

---------------------------------------------------------------------------------------------------------------------------------
## 0. Owner rules (CLAUDE.md and the master framework, always in force)

* **Non-destructive.** Never delete, strip, simplify or overwrite any attribute specification, item logic or mechanic that exists: add the new, keep the old.
  Enum entries are only appended; the existing pages of a book keep their ids, order and behaviour; old textures, models and effects stay registered.
  (The stub files named in section 2 are the scaffold: yours to replace.)
* **Do not touch the Time Magic VFX** (`TimeMagicLayer.java`, `textures/particle/time_*.png`, `tools/gen_time_vfx_textures.py`, the Time calls in
  `TimeBook.java`): they are the quality standard your effects are built to. READ them.
* **Wiki first (Dynamic Wiki Retrieval Protocol).** Canon spell names, behaviour and looks come from the Black Clover wiki pages in `.github/wiki/`; for anything the
  owner spec does not define, the wiki decides. Synthesis pipeline: (1) translate each canon spell into exact gameplay mechanics scaled by the Tensura EP system;
  (2) map its elemental properties onto Tensura's elemental damage types and reactions; (3) design the matching Blender 3D asset spec and Photon shader pipeline spec
  for it (written in your doc, section 8); (4) assign explicit render layers (translucent / cutout / additive) to every visual manifestation.
* **Models (Custom Model Creation directive).** Whenever an ability needs a visual model (a creature, summon, construct, weapon, tool, key, chain, eye, dice,
  shield, projectile body, armour or overlay piece), generate the model and its textures yourself, to the standard of the existing assets or better:
  GeckoLib / Blockbench-style `.geo.json` + `.animation.json` with emissive `_glow.png` layers, box UV and pivots like `assets/nusmp/geo/armor/*.geo.json`. If plain
  boxes are not enough for the ability, build a real animated geo structure with a glow map. Section 5c: GeoLite + `tools/geo_builder.py`.
* **Weapons.** A real weapon ITEM needs Tensura engravings (`item/WeaponEngravings.java`) and a 3D model: **do not add items** in this expansion. Weapons in
  spells (blood blades, bone spears, iron swords, bronze shields) are PROP CONSTRUCTS (section 5b) with geo models.
* **Parallel working.** Git is READ-ONLY for authors (status / log / diff / show; never add / commit / checkout / switch / stash / reset / clean / rebase / pull / push).
  Edit only your own files. Never run `tools/gen_skill_icons.py` (it wipes the icon folder) or another attribute's generators. Run python as `python3 -B`. Previews,
  API extracts and temporary files go to your scratch folder, never into the repo.
* No model or assistant names in code, comments, docs or lang text.

## 1. What the owner asked for (every attribute)

* **Resources.** Magicules project and manifest spells (the page cost, `costPercent` of the maximum magicule pool); Aura powers physical enhancement and reinforcement
  (self buffs, body hardening, blade / brush work, constructs of physical matter: `EnergyBridge.aura(p, frac, floor)` drains it). High-tier spells cost magicules;
  physical constructs and body enhancement cost aura.
* **EP scaling.** Construct durability, projectile count and speed, AOE radius and VFX intensity scale with the caster's Existence Points (EP), physical and
  equipment points: `EnergyBridge.power(e)` / `scale(e)` / `modifier(e)`, `BalanceLaw.epScale(caster)`, `GrimoireBook.size(instance, player)` (grimoire cover and
  Spirit Dive size). Feed the scale into the VFX `power` and the props' `scale`.
* **Status conditions** (use them wherever they fit the wiki behaviour; name them in the contract):
  * Magic Jamming = `EnergyBridge.effect(t, "silence", ticks, 0)` or `Nullification.interfere(t, seconds, chance, misfire)` / `Nullification.jamAll(t, seconds, misfire)`;
  * Spiritual Damage = `EnergyBridge.spirit(t, amount)` (bypasses armour and elemental resistance; at the spiritual floor it becomes Soul Annihilation, see `KotodamaWords.spirit`);
  * Absolute Paralysis = `EnergyBridge.effect(t, "paralysis", BalanceLaw.controlTicks(t, ticks), 0)`, after the `GrimoireBook.control(p)` gate;
  * Energy Drain = `EnergyBridge.drain(from, to, frac)`, `EnergyBridge.burnAura(t, frac)`;
  * resistance shredding = `Nullification.shatterBarriers(level, centre, radius, caster)`, `Nullification.bleed(t, frac)`, `EnergyBridge.armourBypass(t, source, raw, bypass)`,
    and per-target stacking counters you keep in the target's persistent data.
  Look at the usage in `book/*Book.java`, `book/WikiSpells.java`, `book/KotodamaWords.java` before inventing a call. Bosses (`BalanceLaw.isBoss`) get shortened control and capped damage.
* **Custom Render Layering Engine.** Whenever an attribute creates an aura, a physical mutation, a transparent barrier or an entity overlay, assign an EXPLICIT render layer to
  every pass (`AuraRender.cutout` = entityCutoutNoCull, `AuraRender.translucent` = entityTranslucent, `AuraRender.additive` = RenderType.eyes, `AuraRender.emissive`) so nothing
  z-fights with the base character mesh and depth sorting is right: solid overlays are scaled slightly larger than the surface under them (>= 2 % or inflate >= 0.3 px) and
  drawn first; translucent passes next, back to front (larger scale first); additive passes last. A sentence of your owner spec that starts with RENDER LAYER is binding: the
  named render type is the one you use.
* The grimoire summoning, the creative safeguard, the bosses and the Kotodama / Zagred systems already exist: do not change them.
* **Exact wiki behaviour.** Use the exact spell names, behaviour, incantations and look of the wiki files given to you. The owner will compare. When a spell cannot be
  reproduced 1:1, build the nearest honest version and say so in your doc.

## 2. Your files (replace the stubs, create the rest)

`<Pascal>` = the `pascal` column of the table; `<Layer>` its `layer` column (`<Pascal>Layer`, except `DiceFxLayer` and `KotodamaFxLayer`); `<key>` the lower-case key;
`<KEY>` the upper-case key. The roles of one attribute split the files so that nobody edits the same file:

| Role | Files |
|---|---|
| SPELLS | new magic: `book/<Pascal>Book.java`; upgrade / audit: `book/ext/<Pascal>Ext.java` (`pages()` = the NEW pages, APPENDED after the existing ones); `prop/<Pascal>Props.java` (server behaviours + AttributeEvents hooks); helpers `book/<Pascal>*.java`, `prop/<Pascal>*.java`; an audit may also make ADDITIVE edits inside the existing book / helper classes of its magic that the table row names |
| VFX | `vfx/client/layer/<Layer>.java` (+ helpers whose names start with the layer's stem next to it); `tools/gen_<key>_textures.py` -> `textures/particle/<key>_*.png`; `tools/vfx_preview/scenes.d/<key>.json` |
| AURA (player layer) | `client/aura/<Pascal>Aura.java` (+ helpers); `tools/gen_<key>_aura_textures.py` -> `textures/aura/<key>_*.png`; `tools/gen_<key>_aura_geo.py` -> its geo models; `tools/render_preview/scenes.d/<key>_aura.json` |
| PROPS | `client/prop/<Pascal>PropPainter.java` (+ helpers); `tools/gen_<key>_prop_textures.py` -> `textures/prop/<key>_*.png`; `tools/gen_<key>_geo.py` -> geo models; `tools/render_preview/scenes.d/<key>_props.json` |
| geo models (AURA or PROPS) | `assets/nusmp/geo/entity/<key>_<name>.geo.json`, `animations/entity/<key>_<name>.animation.json`, `textures/entity/<key>_<name>.png` + `_glow.png` |
| ICON (new magics only) | `tools/icons/g_<key>.py` |
| REVIEW | `tools/lang/<key>.json`, `docs/attributes/<key>.md` (the designer writes the first half), and fixes anywhere in the attribute's files |

You may add NEW files whose names start with your `<Pascal>` / `<key>`. Do NOT edit any other existing file. In particular NOT: `MagicType`, `NUSkills`, `VfxShape`, `VfxManager`,
`Aura`, `PropKind`, the three registries, `BookPalette`, `NUSMP`, `NUEntities`, `NUItems`, `en_us.json`, `ElementBook(s)`, `GrimoireBook`, `SpellRuntime`, `GeoDraw` and the other
`client/geo` classes, the shared layers (`SlashCompassMercuryLayer`, `ElementShapesLayer`...), the preview tools, the other attributes' files. If you need a change there, say so
in `needsSharedChange` and work around it.

What the scaffold already gives you for your key: `MagicType.<KEY>` (new magics) with soul bias, colours and `BookPalette` cover, `NUSkills.BOOK_<KEY>`, the legacy skill
`grimoire_<key>`; three VFX shapes `VfxShape.<KEY>_FX1 / _FX2 / _FX3` bound to your layer class (all your effects are variants of these three: switch on `inst.shape` and on
`inst.power` / `from` / `to` / `color`); one player render layer `Aura.<KEY>` (variants by an int `style`); two prop kinds `PropKind.<KEY>_1`, `PropKind.<KEY>_2` (variants by an int
`param`); your stub files, already wired: `<Pascal>Aura.register()`, `<Pascal>Props.init()`, `<Pascal>PropPainter.register()` are called by the generated registries (keep those
names and signatures). The three-shape / one-aura / two-prop budget is a hard limit: vary by `power`, `color`, `style`, `param`.

## 3. The spell side (book)

`GrimoireBook` (abstract) -> `ElementBook` (shared shapes) -> your `<Pascal>Book extends GrimoireBook` (new magic) with
`familyPages()` and `damageType()`. Copy the structure of `book/ThreadBook.java`, `book/SealingBook.java`, `book/TimeBook.java`,
`book/WikiSpells.java` (compact wiki spells) and `book/ElementBooks.java`. The stub shows the minimum.

```java
BookPage.starter("id", "Name", cast)          // 8 % max magicule, floor 150, cooldown 80 t   (page 0 should be a starter)
BookPage.mid(...)  .zone(...)  .signature(...) // 12 % / 20 % / 35 %, cooldown 160 / 300 / 600 t
BookPage.daily(...)                            // once a day style (cooldown set by you with .withCooldown(ticks))
page.withCooldown(ticks)
Cast = (GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) -> boolean   // return false = nothing happened, nothing spent
```

**Page limit: a book may hold at most 24 family pages in TOTAL (existing + yours).** Unlocks are bits of an int (`1 << mode`) and the book appends four special pages after the family pages
(spirit dive, spirit channeling, devil union, summon grimoire), so a page number above 31 would alias another page. For upgrades and audits count the existing pages first
(`ElementBooks.java` / `<X>Book.java` / `PaintingBook.java`) and add at most `24 - existing`.

Page ids are lower-case snake_case and unique in the book; they are save keys: **never rename an id you wrote**. Lang key per page:
`nusmp.page.book_<key>.<id>`: one sentence, what it does and what scales with EP.

Helpers you can rely on (read their source; all are in this repo):
* `GrimoireBook`: `hurt(i, p, target, mode, raw)` and `hurtAs(i, p, target, mode, raw, damageTypeKey)` (the ONLY way to damage: it goes through
  the balance law), `target(p, range)`, `aim(p, range)` (hit position), `around(p, centre, r)`, `along(p, a, b, width)`,
  `control(p)` (the shared control cooldown: call it before a hard control), `vfx(p, shape, from, to, ticks, power)`,
  `castCircle(p, power)`, `impact(p, at, power)`, `size(i, p)`, `fail(p, "message")`, `level(p)`.
* `ElementBook` shapes (each returns a `Cast`): `bolt(dmg, speed, radius, life, pierce, aoe, trail, burst, rider)`, `volley`, `cone`,
  `nova`, `field`, `dash`, `empower`, `line`, `bind`, `constructs`, `wall`, `decoy`, `healAllies`, `copy`; riders `NONE`, `effect(...)`, `ignite`, `knock`,
  `lift`, `leech`, `strip`, `all(...)`. Use them for the simple pages and write your own `Cast` lambdas for the signature behaviour.
* `SpellRuntime`: `bolt(p, start, vel, radius, life, pierce, homingTarget, onHit, onEnd)` (a flying hitbox), `zone(level, ticks, interval, step)` (a repeating server
  effect), `later(level, delay, runnable)`, `tempBlock(level, pos, state, ticks)` (self-restoring block, for walls and platforms),
  `dissolveBolts`.
* `damageType()` returns a `TensuraDamageTypes.*` key: `MAGIC_GENERIC`, `FIRE_ELEMENTAL`, `WATER_ELEMENTAL`, `EARTH_ELEMENTAL`, `WIND_ELEMENTAL`,
  `LIGHTNING_ELEMENTAL`, `LIGHT_ELEMENTAL`, `DARKNESS_ELEMENTAL`, `SPACE_ELEMENTAL`, `GRAVITY_ELEMENTAL`, `ICE_ELEMENTAL`, `CURSE`, `BLOOD_RAY`. Use only these (they exist).
* Persistent per-caster state (stances, charges): `i.getOrCreateTag()` (the skill instance NBT) and `p.getPersistentData()`.
  Timed buffs: store an end game-time and read it in an `AttributeEvents` hook (3.1) or a controller prop (3.2); a new BOOK may also override `GrimoireBook`'s `onTakenDamage` / `onDamageEntity`
  (see `ElementBook` and `TimeBook`). Never register your own event listeners on the buses: NUSMP is a shared file.
* Server-side only: casts run on the server. Never touch client classes (`net.minecraft.client`, `client.*`) from the book, props or the
  spell code. The only bridges to the client are `VfxSpawn.send(...)` (a VFX shape), `PlayerAuras.set(...)` (a render layer on a player) and
  `MagicProps.spawn(...)` (a prop entity).

Safety:
* Never `kill` a player outright, never break non-temporary blocks (use `tempBlock`), never remove items, never target the caster's own side
  (allies: `GrimoireBook` friend checks live in `Nullification.friendly` / the helpers above). Respect creative/spectator players (skip them as targets).
* A cast must be safe to fire in a crowd (cap the number of entities you touch, cap `ticks * entities`), safe on a dedicated server (no client calls),
  and safe at the world edge / unloaded chunks (use `level.isLoaded(pos)` before blocks).
* **Use only Minecraft, NeoForge, Tensura and ManasCore API that you confirmed**: in the dependency API dump (`.github/api/api.tar.gz`, see the top of this guide) or in code that already exists in this
  repo (copy its form: generic types, argument order). CI is the compiler and a single wrong signature fails the whole build for everyone; the local type check (section 10) catches it before.
  No reflection tricks, no mixins, no new access transformers.

### 3.1 Hooks: passives, upkeep, damage rewrites (`book.ext.AttributeEvents`)
Register once, from your `<Pascal>Props.init()` (it runs at mod construction):
```java
AttributeEvents.incoming(e -> {                                   // before damage is applied: e.setAmount / e.setCanceled, e.getSource(), e.getEntity() = the victim
    if (e.getEntity() instanceof ServerPlayer p && p.getPersistentData().getLong("nusmp_bone_armor") > p.level().getGameTime()) e.setAmount(e.getAmount() * 0.6f);
});
AttributeEvents.playerTick(p -> { if (p.getPersistentData().getLong("nusmp_x_until") < p.level().getGameTime()) return; /* upkeep */ });
AttributeEvents.serverTick(server -> { /* once per server tick */ });
AttributeEvents.damaged(e -> { /* after damage: e.getNewDamage() */ });
AttributeEvents.death(e -> { /* e.getEntity(), e.getSource() */ });
AttributeEvents.logout(p -> STATE.remove(p.getUUID()));          // clean static maps keyed by player UUID
```
Hooks run for EVERYBODY, ALL the time: return at once when your flag (an end game-time in the caster's persistent data, or a static map keyed by UUID) is not set. A throwing hook
is caught and logged, but write them so they do not throw.

### 3.2 Controller props: timed effects without a listener
For an effect that needs per-tick work while it lasts (a buff upkeep, a trail emitter, an orbiting ring, a leech field) spawn a prop (its painter draws nothing, or the visible thing)
with `MagicProps.spawn(sl, PropKind.<KEY>_2, at, yaw, scale, lifeTicks, param, owner)`. Its `Behavior.tick(e, sl)` follows the owner (`e.owner()`, `e.setPos(...)`), applies the effect,
and ends when the owner dies, logs out or the life runs out.

### 3.3 Upgrades and audits: adding to what exists
* The existing book's pages keep their ids, order and behaviour. List its page ids first (`book/ElementBooks.java`, `book/<X>Book.java`, `book/CanonSpells.java`, `book/WikiSpells.java`,
  `book/CharacterSpells.java`, `book/PaintStudio.java`...) and never duplicate one.
* AUDIT PROTOCOL: split the owner spec into statements; for each, find the evidence in the existing code (file, method) and classify it OK / PARTIAL / MISSING (the designer's
  `auditGaps`). Implement only PARTIAL / MISSING. Additive edits in the magic's own existing classes are allowed (a new method, branch or field); never remove or rewrite existing
  behaviour, never rename an existing page id, never change the order of the existing pages. If everything is OK, still add what the new framework asks of every attribute and that is
  missing here: geo models with glow maps, explicit render layers, Photon-grade VFX shapes, the Blender / Photon specs.

## 4. The VFX side (Time Magic quality)

Read `vfx/client/layer/TimeMagicLayer.java` first, then `ElementFx.java`, `FireSpellLayer.java`, `DemonSlayerLayer.java`, `MirrorLayer.java`, `GravityLayer.java`.

* **THE SHAPE PROTOCOL (every attribute, so that spells and VFX meet without negotiation).** Your three shapes mean the same thing in every attribute:
  * `<KEY>_FX1` = CAST / PROJECTILE: `from` = origin (caster hand / eye), `to` = target point, `power` = size scale (1.0 normal; 0.6 to 3.0), duration = flight ticks (default 16). A charge flash at `from` in the first quarter, then the projectile / beam / trail from `from` to `to` with an afterimage, ending in a small flash at `to`.
  * `<KEY>_FX2` = ZONE / FIELD / DOME: `from` = centre on the ground, `power` = RADIUS in blocks (the effect is drawn to that radius), duration = life ticks (default 80). Ground sigil / ring, rising motes, a rim or wall, a pulse; fades in over 8 ticks and out over the last 12.
  * `<KEY>_FX3` = IMPACT / BURST / SIGNATURE: `from` = centre, `to` = optional direction hint (`to - from`, may be zero), `power` = scale, duration = life ticks (default 28). Flash, expanding rings, the magic's debris flying out, a lingering afterglow.
  Variants come from `power`, `color` (`inst.color`, mixed with the magic's own palette) and the length of `to - from`. Spells map pages onto them: a bolt page = FX1 trail + FX3 at the impact; a zone page = FX2; a buff = FX3 at the caster (+ aura); an ultimate = FX2 + a big FX3.
* Your layer extends `AbstractVfxLayer`; it already exists as `<Pascal>Layer` with the three shapes. `render(inst, ctx, buf)` is called every frame for each live
  instance; everything is computed from the age (`inst.ageTicks(ctx.partialTick)`, `inst.progress(...)`) and the seed (`inst.random()`), never from state.
  Positions: `inst.from(ctx)`, `inst.to(ctx)` (world) -> `ctx.rel(...)` (camera relative). `inst.power` is your size/intensity knob (the spell passes it), `inst.color` the tint.
* **At most 400 vertices per activation** (`VfxVertexBuffer.MAX_VERTICES_PER_ACTIVATION`, 4 per quad: at most ~100 quads). Budget with `buf.hasBudget(n)`; use
  `ctx.seg(full, min)` for segment counts. Use few, big, textured, layered quads (billboards, rings, beams, arcs, planes) instead of many small ones; the quality comes from
  the textures, the layering order (back to front) and the motion (anticipation, impact flash, fade), as in the Time layers.
* Buffer calls: `billboard(ctx, tex, blend, centre, size, rotation, argb)`, `plane(tex, blend, pose, halfSize, argb)`, `ring(tex, blend, pose, inner, outer, segments, ...)`,
  `beam(ctx, tex, blend, a, b, ...)`, `arc(tex, blend, pose, radius, width, ...)`, `quad(...)`; colour helpers `withAlpha`, `lerpColor`, `whiten`. Blends: `VfxBlend.ADD`
  (glows, light, energy), `ALPHA` (solid-ish, dark, smoke, petals), `WATER` (tinted translucent), `NEGATIVE` (inverted palettes). `VfxPose` / `VfxAnim` helpers give you rotation and easing.
* Textures: `VfxTextures.byName("<key>_something")` -> `assets/nusmp/textures/particle/<key>_something.png`. **Write them with your own `tools/gen_<key>_textures.py`**
  (Pillow + numpy, deterministic seeds; copy the style of `tools/gen_time_vfx_textures.py` / `tools/gen_element_vfx_textures.py` / `tools/gen_attr_vfx_textures.py`; white or
  grey-scale with alpha so the vertex colour tints them, except where the colour is the point). Power-of-two sizes (64/128/256), at least 4 distinct textures per attribute
  (not just glow blobs: shaped sprites such as claw marks, runes, petals, shards, spores, keys, eyes).
  Run your generator and commit its PNG output.
* Preview without the game (the verification you can actually run):
  `python tools/vfx_preview/preview.py <scene>` for scenes in `tools/vfx_preview/scenes.json` AND in `tools/vfx_preview/scenes.d/*.json` (put yours in `scenes.d/<key>.json`,
  same schema as `scenes.json`: `{"<scene>": {"layer": "<Pascal>Layer", "shape": "<KEY>_FX1", "from": [x,y,z], "to": [x,y,z], "power": 2.0, "duration": 40,
  "cam": [x,y,z], "target": [x,y,z], "ages": [3, 14, 30], "figures": [[0,0,0,1.8]]}}`). Write one scene per shape at least (3), look at `build/vfx_preview/<scene>.png`,
  and iterate until each frame reads clearly and prettily. The run also reports the vertex budget (it exits non-zero over 400).
  Needs JDK 21 + numpy + Pillow (installed). Do not edit `preview.py` or `scenes.json`.
  **Always** run it with your own build folder and with scene names: `VFX_PREVIEW_BUILD=<scratch>/vfx python3 -B tools/vfx_preview/preview.py <scene> <scene>` (many authors run it at the
  same time; without names it renders every scene of everybody). Helper classes of a layer must be named `<Stem>*.java` next to `<Stem>Layer.java` (the preview copies them along).

## 5. The two render-layer systems (what the owner called "custom render layers")

### 5a. Player render layer = `Aura` (draws over the posed player model; Beast, Demon Beast, Body, Bone armour, Bubble skin, Gel skin, Seal domes ...)

Server: `PlayerAuras.set(ServerPlayer p, Aura.<KEY>, int ticks[, int style])`, `PlayerAuras.clear(p, aura)`, `PlayerAuras.active(entity, aura)`, `PlayerAuras.style(entity, aura)`.
It is synced to everybody who tracks the player; you never write a payload. `style` is any small int your spell wants to hand to the painter (which beast, which phase).

Client: in your `client/aura/<Pascal>Aura.register()` call `PlayerAuraClient.register(Aura.<KEY>, c -> { ... })`. The painter is called once per frame per affected player with an
`AuraContext c` (player, the posed `PlayerModel` `c.model()`, `c.pose()`, `c.buffers()`, `c.light()`, `c.age()` (animation clock), `c.t()` / `c.left()` / `c.total()`, `c.style()`,
`c.fade()` 0..1, limb swing, head angles). Helpers in `AuraRender`: `tex("aura/<key>_x")` -> `textures/aura/<key>_x.png`, render types `additive(tex)` (RenderType.eyes: glows, spectral
auras), `translucent(tex)` (entityTranslucent: ethereal bodies, glass, bubbles), `emissive(tex)`, `cutout(tex)` (entityCutoutNoCull: solid overlays such as muscle and bone), and
`model(c, renderType, sx, sy, sz, argb)` / `modelBright(...)` which re-draw the player's posed model scaled about the body centre (1.08 = 8 % larger; the texture is laid out like a
64x64 player skin: write it so). You may also draw extra geometry around the player with `PropDraw` (below) in `c.pose()` space (body centre at y = 0.75 blocks, **y points DOWN
in layer space**; the helpers' doc says which way each one faces). Draw 2 to 4 passes at slightly different scales for depth (inner solid, outer translucent, additive rim).
Rules: the layer must NOT hide the hitbox or the player's own skin (keep alpha, keep scales within about 0.9 to 1.6, never draw opaque over the face of the local player in first
person: use `Minecraft.getInstance().options.getCameraType().isFirstPerson()` and `c.player() == Minecraft.getInstance().player` to skip the head pass), and must cost at most a few
hundred vertices. `AuraRender.alpha(argb, k)` scales an alpha.

### 5b. Prop entity = `MagicPropEntity` (a real, networked, never-saved 3D thing in the world: Beast and Demon-Beast bodies, eyes, keys, chains, crystals, bronze shields, gel blobs,
bubbles, legion constructs, black holes, bone armour pieces, food, cloud of spores ...)

Server (`prop/<Pascal>Props.init()`): `MagicProps.register(PropKind.<KEY>_1, behavior)` where `Behavior` has `init(e, sl)`, `tick(e, sl)` (required), `end(e, sl)`, `hurt(e, source, amount)`.
Spawn from a spell: `MagicProps.spawn(ServerLevel, PropKind, Vec3 at, float yaw, float scale, int lifeTicks, int param, Entity owner)`. On the entity: `e.kind()`, `e.scale()`, `e.life()`, `e.maxLife()`,
`e.lifeFrac()`, `e.param()/setParam(int)`, `e.seed()`, `e.owner()`, `e.setTarget(LivingEntity)/target()`, `e.setSize(w, h)`, `e.setSolid(bool)`, `e.setHittable(health)`, `e.setGravity(bool)`,
`e.expire()`. Move it with `e.setPos/setDeltaMovement/move`; hurt things through the book's `hurt(...)` helper (keep the book/instance in a captured lambda or in the owner's data), not directly.
Props never save: they die with the chunk, which is the intended behaviour (always set a finite life).

Client (`client/prop/<Pascal>PropPainter.register()`): `PropPainters.register(PropKind.<KEY>_1, (e, partial, age, pose, buffers, light) -> { ... })`. The pose is at the prop's feet, turned by its yaw, y up,
units are blocks. Build the model with `PropDraw`: `box`, `boxUv`, `sphere`, `ellipsoid`, `cylinder` (cone / frustum, caps), `torus`, `crystal`, `tube` (a swept tube through points: tentacles, chains, vines,
claws, bones), `quad`, all outward-wound and lit with the given `light`; pass `PropDraw.FULL_BRIGHT` for glows. Render types from `AuraRender` (see above) on `buffers.getBuffer(...)`, textures from
`textures/prop/<key>_*.png` (your generator). Make props read as modelled objects: use several primitives per prop, a second translucent or additive shell, animation from `age`, and the `param`/`seed` for variety.
Two kinds per attribute are registered; if you need more variants, switch on `e.param()`.

### 5c. Geo models: GeoLite (Blockbench / GeckoLib files, animated, with glow maps)

The mod draws `.geo.json` models (Bedrock format 1.12.0, the files Blockbench and GeckoLib use) with their `.animation.json` keyframes and a `_glow.png` emissive layer, through
`client/geo/GeoDraw` (read `GeoModelData`, `GeoAnim`, `GeoSpec`, `GeoDraw`, `docs/geo_models.md`). It works everywhere a pose stack exists, so the same model can be a prop, an aura piece
on the player, or a boss. Build models with `tools/geo_builder.py` (cubes, bones, automatic box-UV layout, texture painting helpers, animation keyframes, `tools/geo_check.py` validator).
Files, by the convention of `GeoSpec.of("<key>", "<name>")`:
`assets/nusmp/geo/entity/<key>_<name>.geo.json`, `animations/entity/<key>_<name>.animation.json`, `textures/entity/<key>_<name>.png` (the size the model's texture_width / texture_height says)
and `textures/entity/<key>_<name>_glow.png` (same size; only the glowing parts). A CI unit test (`GeoAssetsTest`) fails the whole build for every model with a missing parent bone, uvs
outside the texture, a missing or differently sized texture / glow map, or an animation that names a bone the model does not have: run `python3 -B tools/geo_check.py` before you finish.
```java
private static final GeoSpec SHIELD = GeoSpec.of("bronze", "shield");
// in the prop painter (pose at the prop's feet, y up; the model stands on it, front along the prop's forward):
pose.pushPose();
pose.scale(e.scale(), e.scale(), e.scale());
GeoDraw.paint(pose, buffers, SHIELD, "idle", age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, AuraRender.alpha(0xFFFFB060, 0.6f + 0.4f * (float) Math.sin(age * 0.2f)));
pose.popPose();
// in an aura painter (layer space of the player; the model stands on the player's feet, front where the face is):
GeoDraw.paint(c.pose(), c.buffers(), ARMOR, "idle", c.age() / 20f, GeoDraw.Space.PLAYER, GeoDraw.Layer.TRANSLUCENT, c.light(), AuraRender.alpha(0xFFFFFFFF, c.fade()), 0);
```
* `Layer.CUTOUT` = solid with alpha test (entityCutoutNoCull: no fading), `TRANSLUCENT` = alpha blended (fading, glass, ghosts), `ADDITIVE` = light. The glow map is drawn additive and full bright
  in the colour you pass as `glowArgb` (alpha 0 = no glow pass); pulse it with a sine for living glow. One model = one render type: split a model that needs two (a solid body and glass wings)
  into two geo files drawn with two calls.
* Space.PLAYER models are authored to the humanoid's proportions (feet at y 0, shoulders at y 24, head origin y 24..32, arms at x +-5..8; `assets/nusmp/geo/armor/robe.geo.json` is a
  working example) and are laid over the player's own body: give overlay cubes `inflate` >= 0.3 so they do not z-fight with the skin.
* Animation clocks are seconds: `age / 20f`. Loops close (first keyframe == last). Bones that an animation moves must exist in the model.
* Budget: a prop model 30 to 150 cubes, a creature up to ~250, texture 64 to 256 px. A model that is a creature, construct, weapon, tool, key, chain, eye, dice, shield, projectile body,
  armour or overlay piece is a geo model; shockwaves, spores, petals, mists and fields are VFX layers or PropDraw geometry.

### 5d. Explicit render layers (the owner's Custom Render Layering Engine) in practice
* Write down, for every pass of every aura style and prop, its render type and why (the doc table in section 8). Never leave a pass on an implicit type.
* Order of calls = draw order: opaque / cutout passes first, then translucent (back to front), then additive. Never draw two coplanar surfaces of the same render type: offset them by scale.
* Additive and emissive passes use `PropDraw.FULL_BRIGHT` light; solid and translucent passes use the light you are given.
* Refraction (bubbles, glass, gravity lensing): there is no screen-copy shader in this mod; approximate it with fresnel rims (a brighter rim where the surface turns away: scale + additive shell), iridescent
  hue bands that drift (two or three additive skins cross-faded by sine phases), rim lensing rings, and distortion rings in a negative-blend VFX layer.

## 6. Icon and lang

* Icon: `tools/icons/g_<key>.py` (new magics only). `ENTRY = (display, glow RGB, body RGB, colour words, signature spell)` and `glyph(d)` which draws a bold, readable, centred symbol of the
  magic (key, eye, chain, crystal, bubble, bronze shield, ...) with `d` an `ImageDraw` on a 256 px "L" canvas: `d.polygon(G.P([(x, y), ...]), fill=255)` with `G.P` in glyph units (origin at
  the centre, y down, roughly +-11), 255 = body, ~150 = facet detail. Look at `g_*` in `tools/gen_skill_icons.py` for the style. Preview with `python3 -B tools/gen_attribute_icons.py <key>`
  (it writes the four PNGs), look at `src/main/resources/assets/nusmp/textures/skill/grimoire/<key>.png` (32 px; view it enlarged) and iterate.
* Lang: `tools/lang/<key>.json`, a flat JSON object `{"key": "text"}`, English only, plain ASCII quotes, no `\n`. New magics: `nusmp.skill.book_<key>` (name) and `.description` (2 to 4 sentences:
  what the magic is, its signature look and mechanic), `nusmp.skill.grimoire_<key>` (+ `.description`), and one `nusmp.page.book_<key>.<pageId>` per page. Upgrades and audits: only the new
  pages' keys, `nusmp.page.book_<magic>.<pageId>` where `<magic>` is the lower-case key of the EXISTING book (look at how that magic's pages are keyed in `en_us.json`: Seal is `sealing`,
  Imitation is `copy`, Ice is `ice` ...). One or two sentences per page: what it does, what scales with EP, which status condition.

## 7. Verification you must run (CI compiles later; there are no Minecraft jars here)

1. API: `mkdir -p <scratch>/api && tar xzf .github/api/api.tar.gz -C <scratch>/api`, then grep the exact signature of every Minecraft / NeoForge / Tensura / ManasCore call you are not sure of
   (or copy a call that already exists in this repo). The dump is `javap -protected` output: parameter names are not shown.
2. Type check: `python3 -B tools/typecheck/typecheck.py --files <your java files>` (section 10: real javac over the mod's sources against signature stubs of the whole API; it reports every unknown
   symbol, wrong argument type, missing import, wrong generic, unhandled exception). Fix every error it reports in YOUR files. Also parse-check with
   `javac -proc:none -XDshould-stop.ifNoError=PARSE -XDshould-stop.ifError=PARSE -d <scratch dir> <files>` if the type check tool is not there yet, and READ your code once more as a compiler would.
3. Run your texture generators, the geo generators (`python3 -B tools/geo_check.py`), the icon module (`python3 -B tools/gen_attribute_icons.py <key>`), and the preview scenes (all of them, all
   frames looked at: the PNGs are the only eyes you have). Fix what looks wrong; do not hand over a flat blob.
4. Validate JSON (`python3 -B -c "import json; json.load(open('tools/lang/<key>.json'))"`, the scenes files, the geo files).
5. Write your part of `docs/attributes/<key>.md` (section 8) and **what you did not verify**.

## 8. Docs and report format

`docs/attributes/<key>.md` has these sections (designer: 1-4 and 6; reviewer: the rest): 1 Summary; 2 Spec coverage (each owner requirement and where it is implemented); 3 Pages (table: id, name,
tier, what it does, cost tier, what scales with EP, status condition, VFX shape / aura style / prop); 4 VFX shapes (what each of the three looks like, the from / to / power / colour encoding);
5 Render layers (table: every pass of every aura style and prop: what, render type, scale / order, why); 6 Models (every geo model: file, bones, animations, textures, render layer, cube count);
7 BLENDER ASSET SPEC (how to rebuild every model as a high-poly Blender asset: meshes, rig / bones, materials, UV, export to geo/fbx) and PHOTON PIPELINE SPEC (for every effect: emitters, textures,
blend mode, lifetime curves, the shader / distortion / refraction recipe that the in-game version approximates); 8 Files; 9 How to test in game (page order, `nusmp:book_<key>` skill, commands);
10 NOT VERIFIED (honest).

Your final message is machine read: return the JSON the workflow asks for (files written, what you verified and how, what you could not verify, `needsSharedChange` = anything required in a file you
may not edit; empty if none).

