# The Genesis Demon-Slayer Sword (0.48)

**Replacement:** the 0.28 Demon-Slayer Sword. The item id `nusmp:demon_slayer_sword` is unchanged, so swords already drawn keep working. Black Divider stays on right-click. The old 32×32 sprite is still the inventory icon, kept as the model `item/demon_slayer_sword_icon`.

## Lore
Asta's first sword, drawn from a five-leaf grimoire. It is a crude, dirty greatsword with a split running up from the guard, and it was forged around nothing. Where other blades cut flesh, this one cuts the *concept* of an existence. Wherever magic wants to be, the blade answers with an absence: the dark spots on its face are holes into that absence, and when it strikes at full force it shatters into the void it came from.

## Mechanics (`item/DemonSlayerSwordItem.java`)

### Bound and durability
- **Bound:** only its owner can use it, as before.
- **Unbreakable:** existing wear mends.

### Conceptual Severance (a full swing, 90% charge or more)
- **Damage:** `max(50, EP^0.6 / 2) × affinity × (1 + 0.25 × magic deficiency) × spellDamagePercent`.
  - **Affinity:** 1.15 with Anti-Magic, 1.4 as the Anti-Magic Lord, 1.75 in Black Form.
  - **Magic deficiency:** how empty your magicule pool is.
- **True damage:** it goes through the damage type `nusmp:conceptual_severance`. That type is tagged to bypass armour, enchantments, effects, the hit cooldown (i-frames) and shields.
- **Spiritual damage:** a quarter of the hit, also dealt as spiritual damage.
- **Caps:** a player loses at most the PvP hit cap of their max health per hit (default 40%). A boss loses at most 15%.

### Conceptual Nullification Field (15 blocks, while held, once a second)
- **Ultimate interference:** every other second, each Tensura **Ultimate** skill of each foe has a 35% chance to get a 3 s cooldown. If it is toggled on, it has a 20% chance to switch off (a misfire). Players get a warning line.
- **EP bleed:** 1% of max magicules a second, taken from the current pool only. Each drain gives the wielder +1 AMP.
- **TimeStop immunity:** the wielder and allies can't be caught in stopped time, and a freeze already on them breaks.

### Black Meteorite — Void Severance (sneak + right-click)
1. **Lock-on:** the target is what the crosshair is on, up to 100 blocks. Failing that, it is the strongest energy signature in a 30° cone; hostiles and players are picked first. Energy is sensed through walls.
2. **The charge:** the wielder crosses the gap as an anti-magic meteor. On the way, spells in the air are cut, and anything in the path loses a buff and takes a half severance.
3. **The cut:** two severances at ×1.5 each (the second 0.3 s later), plus a soul strike: 30% of max spiritual health and Tensura fear.
4. **Void Severance:** 10% of the target's max EP is cut away.
   - **Players, their pets and Tensura summons:** a transient −10% modifier on max magicules and max aura that lasts 60 s and also ends on death or relog. **Never permanent.**
   - **Wild mobs:** they lose it for good.
5. **Nihility zone:** radius 12, for 10 s, at the target.
   - **Twice a second:** every Tensura barrier, jail and area spell (boss arena walls are spared), and every spell projectile shatters. Foes are silenced.
   - **Once a second:** every active skill of a foe inside gets a 3 s cooldown, and one buff is stripped.
6. **Cost:** 80% of max aura (stamina).
7. **Cooldown:** 2 minutes at the default cooldown setting. It scales with `spellCooldownPercent`.

### Resonance
The server writes the stack's `Resonance` value (0..4) from the strongest EP within the field:
- 0 below 10k EP;
- 1 from 10k, 2 from 100k, 3 from 1M, 4 from 10M.

The tooltip shows it as dots. A data change does not replay the hand's draw animation.

## Rendering
- **Model:** `client/DemonSlayerMesh.java` is plain geometry with no Minecraft classes. `client/DemonSlayerRenderer.java` draws it through a BEWLR.
  - **Body:** a broad, chipped, slightly tapering blade with a slanted tip, a riveted crossguard with chunky ends, a cloth-wrapped grip, and a pommel with a crimson stone.
  - **Texture:** `textures/entity/demon_slayer_sword.png`, 64×64, from `tools/gen_demon_slayer_textures.py`.
- **Dimensional parallax:** the split (a slot through the blade, with walls) and the dark spots on both faces use the core shader `nusmp:rendertype_demon_void`. It is the end portal's parallax star layers, recoloured crimson and black, drifting faster and pulsing.
  - If the shader fails to load, vanilla's end-portal look is used.
- **Five-leaf bloom:** a near-black clover on both pommel faces. It grows with resonance. A crimson glow behind it pulses between crimson and black, faster and brighter near stronger beings.
- **Fracture:** for 1.5 s of Black Meteorite (`MeteorUntil`), the blade breaks into four shards. They drift apart, tilt and shiver, with crimson light between them, then re-form.

## VFX (`vfx/client/layer/DemonSlayerLayer.java`)

| Shape | Look |
|---|---|
| `DEMON_METEOR` | A black meteor with a crimson rim crosses the path with torn void streaks. Shards and sparks fall off it. A shock ring marks the landing, then a smouldering scar. |
| `NIHILITY_ZONE` | A black wall rises round the zone's edge with crimson cracks crawling up it, over a cracked ground disc. A pulse ring sweeps out every second, and magic motes drift in and are snuffed out. |

Previews: `docs/vfx_previews/demon_meteor.png`, `nihility_zone.png` and `demon_slayer_sword_3d.png`. To regenerate them:
- `python tools/vfx_preview/preview.py demon_meteor nihility_zone`
- `python tools/item_preview/preview_demon_slayer.py`

## Not verified
- **Compile:** CI compiles. Neither the game nor the shaders were run.
- **GLSL:** both shaders compiled cleanly as GLSL ES 3.00 in headless Chromium (WebGL2). That is not the game's GLSL 150 driver.
- **Untested in game:**
  - the BEWLR display transforms;
  - the standalone icon model;
  - the Tensura skill cooldown and toggle calls on other players' Ultimates;
  - the transient max-EP modifiers on `tensura:max_magicule` / `max_aura`;
  - which entities in `io.github.manasmods.tensura.entity.magic.*` count as constructs.
