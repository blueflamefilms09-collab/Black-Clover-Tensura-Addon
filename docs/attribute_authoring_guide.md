# Attribute authoring guide (0.53: the Black Clover Magic and VFX expansion)

One page for whoever builds ONE attribute of the 37 (29 new magics + 8 upgrades of existing ones). The scaffold is already in the
repo and compiles in CI: every attribute has stub files that you REPLACE with the real thing. Other authors work on the other
attributes at the same time, so **only touch the files listed under "Your files"**.

The table of all 37 (key, class names, wiki title, the owner's spec text) is `tools/attribute_table.py`.
The wiki text of each attribute (when fetched) is in the wiki folder you are given.

---------------------------------------------------------------------------------------------------------------------------------
## 0. Owner rules (CLAUDE.md, always in force)

* Never delete or remove anything that exists; add the new, keep the old. Enum entries are only appended. Existing pages of a book
  stay, in order. (The stubs listed under "Your files" are yours to replace: that is the scaffold, not the old mod.)
* Do not touch the Time Magic VFX (`TimeMagicLayer.java`, `textures/particle/time_*.png`, `tools/gen_time_vfx_textures.py`,
  `book/TimeBook.java`). READ them: they are the quality bar your effects are built to.
* Weapons: any real weapon ITEM needs Tensura engravings in `item/WeaponEngravings.java` and a 3D model. **Do not add items.**
  Weapons in your spells (blood blades, bone spears, iron swords, bronze shields...) are PROP CONSTRUCTS (section 5) or VFX shapes.
* No model names, no "Claude", no tool names in code comments or docs.
* The owner reads the spells in game, so every page needs a real, distinct effect. No two pages may be the same bolt with a new
  name. A page is cheap to write, so write enough: **new magics 8 to 12 pages, upgrades 5 to 8 appended pages**.

## 1. What the owner asked for (every attribute)

* Magicules project and manifest spells (cost, `costPercent`), aura is physical enhancement (self buffs, `EnergyBridge.aura`).
* The EP of the caster scales durability of constructs, number of projectiles, area of effect: use `EnergyBridge.power(caster)` /
  `EnergyBridge.scale(caster)` / `BalanceLaw.epScale(caster)` and `GrimoireBook.size(instance, player)` (the grimoire cover size).
* Use Tensura status conditions where they fit (wiki behaviour first):
  * **Magic Jamming** = `EnergyBridge.effect(t, "silence", ticks, 0)` or `Nullification.interfere(t, seconds, chance, misfire)` /
    `Nullification.jamAll(t, seconds, misfire)`;
  * **Spiritual Damage** = `EnergyBridge.spirit(t, amount)` (bypasses armour and elemental resistances);
  * **Absolute Paralysis** = `EnergyBridge.effect(t, "paralysis", ticks, 0)` (always through `BalanceLaw.controlTicks(target, ticks)`);
  * **Energy Drain** = `EnergyBridge.drain(from, to, frac)`; `EnergyBridge.burnAura(t, frac)`;
  * **resistance shredding** = `Nullification.shatterBarriers(level, centre, radius, caster)`, `Nullification.bleed(t, frac)`,
    `EnergyBridge.armourBypass(t, source, raw, bypass)`.
  Look at the usage in `book/*Book.java` and `book/WikiSpells.java` before inventing a new call.
* Bosses: `BalanceLaw.isBoss(t)` targets get shortened control (`controlTicks`) and capped damage (`BalanceLaw.damage/cap`).
* **Always use the exact spell names, behaviour, incantations and look from the attribute's wiki page** (given to you). The owner
  will compare. When the wiki lists a spell you cannot reproduce 1:1 in Minecraft, build the nearest honest version and say so in the doc.

## 2. Your files (replace the stubs, create the rest)

`<Pascal>` = the `pascal` column of the table, `<key>` = the lower-case key, `<KEY>` the upper-case key.

| New magic (`new = True`) | Upgrade (`new = False`) |
|---|---|
| `book/<Pascal>Book.java` (the whole book) | `book/ext/<Pascal>Ext.java` (`pages()`: the pages APPENDED to the existing book) |
| `vfx/client/layer/<Pascal>Layer.java` | `vfx/client/layer/<Pascal>Layer.java` |
| `client/aura/<Pascal>Aura.java` | `client/aura/<Pascal>Aura.java` |
| `prop/<Pascal>Props.java` + `client/prop/<Pascal>PropPainter.java` | `prop/<Pascal>Props.java` + `client/prop/<Pascal>PropPainter.java` |
| `tools/gen_<key>_textures.py` (all your textures) | `tools/gen_<key>_textures.py` |
| `tools/icons/g_<key>.py` (the skill icon symbol) | (the icon already exists: leave it) |
| `tools/lang/<key>.json` (all your lang keys) | `tools/lang/<key>.json` |
| `tools/vfx_preview/scenes.d/<key>.json` (preview scenes) | same |
| `docs/attributes/<key>.md` (what you built) | same |

You may add NEW files whose names start with your `<Pascal>` (for example `book/BronzeArms.java`, `client/aura/BronzeSkin.java`) and
new textures named `<key>_*.png`. Do NOT edit any other existing file. In particular NOT: `MagicType`, `NUSkills`, `VfxShape`,
`VfxManager`, `Aura`, `PropKind`, the three registries, `BookPalette`, `NUSMP`, `NUEntities`, `en_us.json`, `ElementBook(s)`,
`GrimoireBook`, the other attributes' files. If you need a change in one of them, say so in your report (field `needs_shared_change`)
and work around it.

What the scaffold already gives you for your key:
* `MagicType.<KEY>` (new magics) with soul bias, colours, `BookPalette` cover; `NUSkills.BOOK_<KEY>`; the legacy skill `grimoire_<key>`.
* Three VFX shapes `VfxShape.<KEY>_FX1`, `_FX2`, `_FX3` bound to your `<Pascal>Layer` (all your effects are variants of these three:
  switch on `inst.shape` and on `inst.power` / `from` / `to`).
* One player render layer `Aura.<KEY>`, two prop kinds `PropKind.<KEY>_1`, `PropKind.<KEY>_2`.
* Your stub files, already wired: `<Pascal>Aura.register()`, `<Pascal>Props.init()`, `<Pascal>PropPainter.register()` are called by the
  generated registries; keep those method names and signatures.

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
  Timed buffs: store an end game-time and check it in `onPlayerTick`-style hooks that `GrimoireBook` already offers (`onTakenDamage`,
  `onDamageEntity`, `onTick`: see `ElementBook` and `TimeBook` for overrides). Do not register new event listeners outside your own files.
* Server-side only: casts run on the server. Never touch client classes (`net.minecraft.client`, `client.*`) from the book, props or the
  spell code. The only bridges to the client are `VfxSpawn.send(...)` (a VFX shape), `PlayerAuras.set(...)` (a render layer on a player) and
  `MagicProps.spawn(...)` (a prop entity).

Safety:
* Never `kill` a player outright, never break non-temporary blocks (use `tempBlock`), never remove items, never target the caster's own side
  (allies: `GrimoireBook` friend checks live in `Nullification.friendly` / the helpers above). Respect creative/spectator players (skip them as targets).
* A cast must be safe to fire in a crowd (cap the number of entities you touch, cap `ticks * entities`), safe on a dedicated server (no client calls),
  and safe at the world edge / unloaded chunks (use `level.isLoaded(pos)` before blocks).
* **The only Minecraft, NeoForge, Tensura and ManasCore API you may use is API that already appears somewhere in this repo**: grep for the exact method
  before you call it and copy its form (generic types, argument order). CI is the compiler and a single wrong signature fails the whole build for everyone.
  No reflection tricks, no mixins, no new access transformers.

## 4. The VFX side (Time Magic quality)

Read `vfx/client/layer/TimeMagicLayer.java` first, then `ElementFx.java`, `FireSpellLayer.java`, `DemonSlayerLayer.java`, `MirrorLayer.java`, `GravityLayer.java`.

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

## 6. Icon and lang

* Icon: `tools/icons/g_<key>.py` (new magics only). `ENTRY = (display, glow RGB, body RGB, colour words, signature spell)` and `glyph(d)` which draws a bold, readable, centred symbol of the magic
  (key, eye, chain, crystal, bubble, bronze shield, ...) with `d` an `ImageDraw` on a 256 px "L" canvas: `d.polygon(G.P([(x, y), ...]), fill=255)` with `G.P` in glyph units (origin at the centre, y down,
  roughly +-11), 255 = body, ~150 = facet detail. Look at `g_*` in `tools/gen_skill_icons.py` for the style. Preview with `python tools/gen_attribute_icons.py <key>` (it writes the four PNGs), look at
  `src/main/resources/assets/nusmp/textures/skill/grimoire/<key>.png` (32 px; view it enlarged) and iterate.
* Lang: `tools/lang/<key>.json`, a flat JSON object `{"key": "text"}`. Required keys: `nusmp.skill.book_<key>` (name) and `.description` (2 to 4 sentences: what the magic is, its signature look and mechanic),
  `nusmp.skill.grimoire_<key>` (+ `.description`), and one `nusmp.page.book_<key>.<pageId>` per page. For upgrades only the new pages' keys. English only, plain ASCII quotes, no `\n`.

## 7. Verification you must run (CI compiles later; there are no Minecraft jars here)

1. Parse-check every Java file you wrote:
   `javac -proc:none -XDshould-stop.ifNoError=PARSE -XDshould-stop.ifError=PARSE -d <scratch dir> <files>` (syntax only). Then READ your own code once more as a compiler would: imports, generics, checked
   exceptions, lambda captures (effectively final), switch exhaustiveness, `int`/`float`/`double` narrowing, every method you call exists with that exact signature (grep the repo).
2. Run your texture generator, your icon module (`python tools/gen_attribute_icons.py <key>`), and the preview scenes (all of them, all frames looked at). Fix what looks wrong; do not hand over a flat blob.
3. Validate JSON (`python -c "import json; json.load(open('tools/lang/<key>.json'))"`, same for the scenes file).
4. Write `docs/attributes/<key>.md`: the wiki spells you implemented (page ids, what each does, cost tier, what scales with EP, which status condition), what the render layers/props look like, what you
   could not reproduce and why, how to test it in game (commands: `/tensura ...` grant of the book skill `nusmp:book_<key>` or via the creative grimoire), and **what you did not verify**.

## 8. Report format (your final message, machine read)

Return the JSON the workflow asks for: files written, pages (id + one line), shapes used, aura styles, prop kinds, textures written, scenes run with their vertex counts, parse-check result, the things you did NOT
verify, and `needs_shared_change` (anything required in a file you may not edit; empty if none).
