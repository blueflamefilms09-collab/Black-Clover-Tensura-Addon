# Kotodama Magic (Word Soul) and the Zagred boss (0.47)

**Replacement:** Zagred's devil power. The old Devil Union kneel aura (slowness + weakness) is removed. Zagred now lends Kotodama words in Devil Union. The `Devil.ZAGRED` entry stays, so old grimoires load.

**New:**
- Kotodama Magic (`MagicType.KOTODAMA`, book `book_kotodama`);
- the Zagred boss;
- underworld sludge (block `underworld_matter`);
- the otherworldly trident (item `otherworld_trident`);
- six VFX shapes;
- the canon book `ZAGRED`;
- the Blender grimoire script.

## Kotodama Magic (`book/KotodamaWords.java`, `book/KotodamaBook.java`)

### Class and access
- **God-class:** the book is a Tensura **ULTIMATE** skill (every other grimoire is Unique).
- **Creative only:**
  - it is never rolled (`MagicType.forSoul` and `starterPool` skip it);
  - an unbound Kotodama grimoire binds only in creative;
  - the grimoire tier answers only in creative, or for a player who has beaten Zagred. The earned flag is kept through death.

### Speaking
- Type a command word **alone** in chat, punctuation allowed: "Halt!", "Shatter.", "Heal". Or cast its page.
- Chat and pages share the same cost and cooldowns.
- The word shows as demonic glyphs in mid-air (`KOTO_WORDS`).

### Tiers
| Tier | When | Words | Power |
|---|---|---|---|
| Grimoire | the Kotodama grimoire is summoned | all six | EP power × 1.5 |
| Devil | a grimoire with Zagred as its devil, in Devil Union | Halt, Shatter, Heal | EP power × 0.6, cooldowns × 1.5 |
| Boss | the Zagred boss | all but Trident (it throws its own) | 1.6 + 0.3 × phase |

### Pools and scaling
- **Cost:** a share of max magicules with a very high floor (the "astronomical" cost). For example, Halt is 30% with a floor of 40,000 and Swords is 40% with a floor of 80,000.
- **Aura bridging:** what magicules can't cover is taken from Aura. If both together can't pay, the word fails.
- **Creative** is free, and its cooldowns are a quarter as long.
- **EP scaling:** `EnergyBridge.power` (EP log + armour) sets radius, damage and counts.

### Words
| Word (aliases) | Cooldown | Effect |
|---|---|---|
| **Halt** (bind, kneel, stop, freeze) | 30 s | **Absolute Paralysis** in a radius of 12 + 8 × power: Tensura `paralysis` plus a velocity lock (held in place). **Magic Jamming:** Tensura `silence` for twice as long. Spiritual damage. Durations follow the balance law's control caps (players 3 s, bosses 1 s, diminishing returns). |
| **Shatter** (return, vanish, break) | 15 s | Every enemy projectile in a radius of 16 + 8 × power breaks apart, as do other casters' spell bolts. Each one refunds 1% of max magicules. |
| **Heal** (mend, restore) | 45 s | Health and Aura to full, harmful effects cleared. With the grimoire, allies within 8 blocks are healed too. The devil heals half health and 30% aura; the boss heals a quarter, once. |
| **Devour** (underworld, sludge, drown) | 60 s | Underworld sludge floods out where you look (radius 5 + 4 × power, 15 s). |
| **Trident** (spear, pierce) | 60 s | An otherworldly trident in your hand for 60 s. |
| **Swords** (blades, storm) | 45 s | A storm of 8 – 20 demon swords falls on the aimed area. Each is a physical blow plus spiritual damage. |

### Effects
- **Spiritual damage:** takes Tensura spiritual health.
- **Soul Annihilation:** when a target's spiritual health is already at its floor, the next spiritual hit strikes the soul:
  - a heavy blow (60% of max health, 10% for bosses);
  - every magicule torn out (and given to the speaker);
  - Tensura `fear`.

  It can trigger once per 5 s per target.
- **Damage:** goes through the speaker's grimoire, so the balance-law caps apply to players. It is darkness-element damage.

### Grimoire look
- Summoned, the book trails a purple-black smoke aura with a glowing five-leaf emblem turning on the ground (`KOTO_AURA`), renewed every 3 s.
- The floating book uses the corrupted (anti-magic) style.

## Underworld sludge (`book/UnderworldMatter.java`, `block/UnderworldMatterBlock.java`)

**Spread:** a flood fill over surface blocks (solid, with room above), a few blocks a tick, out to its radius.
- It swallows each block into `underworld_matter` and eats small plants on top.
- It skips air, fluids, block entities and unbreakable blocks.

**The block:**
- unbreakable and unpushable while it lasts;
- slows anything wading through it and lowers its jumps;
- glows faintly, with full-bright veins (an animated texture).

**Contact (every 0.5 s, not the owner or allies):** darkness damage, 1 spiritual damage, and a 2% magicule drain that goes to the owner.

**End:** when the time is up or the owner is gone, every devoured block is put back exactly as it was.
- With `griefBlocks` on, the ground stays eaten.
- A server stop reverts everything.
- Matter that no session owns (left behind by a crash) crumbles to dirt on a random tick.

## Otherworldly trident (`item/OtherworldTridentItem.java`)

- Netherite-tier damage (+14) and unbreakable; every melee hit also does spiritual damage.
- **Right-click:** a piercing void bolt (14 damage + spiritual), 1.5 s cooldown.
- **Bound:** it fades after 60 s, when dropped, or in anyone else's hands.
- It is not in the creative tab. Like Material Creation, it is only spoken into being.

## The Zagred boss (`entity/ZagredBossEntity.java`, `client/ZagredRenderer.java`)

**Off by default:**
- `zagredBossEnabled = false` (server config). When on, an op runs `/multiverse boss zagred`; the arena centre is where it is summoned.
- `kotodamaBossReward = false`. When on, everyone who damaged it earns Kotodama and gets Zagred's five-leaf grimoire.

**Stats:**
- 600 HP, armour 12 (toughness 6), full knockback resistance, fire immune.
- Melee does 14.
- A purple notched boss bar shows the phase and its adaptation.

| Phase | Health | What it does |
|---|---|---|
| 1 | 100–75% | **Halt** every 14 s on anyone within 20 blocks. **Shatter** whenever something is fired at it. |
| 2 | 75–50% | **Elemental adaptation:** every 10 s it adapts to the element that hurt it most and takes only 20% from it. Keep changing elements. **Void lances** are piercing line throws. |
| 3 | 50–25% | **Void flooding:** underworld sludge floods the arena from its centre every 30 s. **Demon sword storms** fall on its target. |
| 4 | 25–0% | It takes **15% of any damage** unless the hit is **anti-magic** (a demon sword, the Anti-Magic Lord, a summoned Anti-Magic grimoire) or it is **exposed**. Three different elements within 5 s expose it for 5 s. It speaks **Heal** once below 10%, throws triple lances, and holds its trident. |

**The fight needs coordination:**
- Phase 2 punishes a party that sticks to one element.
- Phase 4 needs anti-magic, or three elemental attackers hitting together.

**On death or removal:** the arena's sludge is put back.

**Look:** a humanoid devil at ×1.4 with two-segment swept horns, a tail with a spade tip, ink-black skin with violet rune lines, and glowing eyes and runes that pulse faster each phase. It carries the trident in phase 4.

## VFX (`vfx/client/layer/KotodamaLayer.java`)

Textures come from `tools/gen_kotodama_textures.py`. Previews are in `docs/vfx_previews/koto_*.png`:
```
python tools/vfx_preview/preview.py koto_words koto_aura koto_halt koto_shatter koto_swords koto_trident koto_sludge
```

| Shape | Look |
|---|---|
| `KOTO_WORDS` | The spoken word as demonic glyphs: they burn in one by one (white flash, then violet) on a slab of black smoke with a rune ring pulse, shiver, then burn away upward. The seed holds the word and the tier (the devil's loan is fainter). |
| `KOTO_AURA` | Long: the grimoire aura, purple-black smoke clinging and billowing round the body, plus a glowing five-leaf emblem on the ground. Short: a rune-and-smoke ring rushing out (Halt, Heal). |
| `KOTO_SHATTER` | A crack star, black-glass shards, and freed magicules streaming back to the speaker. |
| `KOTO_SWORDS` | A demon sword falls point-first with a black wake and sticks in the ground with a violet ring. |
| `KOTO_TRIDENT` | Thrown: the trident with a violet trail and shed smoke. In the hand: glyph sparks drawn in. |
| `KOTO_SLUDGE` | A black-violet pool welling out, with a glowing rim, swelling and popping bubbles, and black wisps. |

All of them stay under 210 of the 400-vertex budget.

## Blender: Zagred's grimoire (`tools/blender/build_zagred_grimoire.py`)

It is procedural (no image files) and targets Blender 4.0+. It was tested headless with the Blender 5.2 `bpy` module in Cycles. Preview: `docs/blender/previews/zagred_grimoire.png`.

- **bmesh open hardcover:** one mesh.
  - Vertex groups: `front_cover`, `back_cover`, `spine`, `pages`, plus `page_edges` (the smoke emitter) and the deform groups `Cover.Front`, `Cover.Back`, `Spine`.
  - The rounded spine and the page gutter blend between the covers.
- **Open/close:**
  - The armature `ZagredRig` (Root > Spine > Cover.Front / Cover.Back) is hinged on the spine edges.
  - Action `OpenClose`: closed at frame 1, open by 40, held to 100, closed again by 140.
  - A shape key `Closed` holds the same closed pose. Use one or the other.
- **Etched starbursts on both covers:** the covers' outer faces are gridded at 1.5 mm and pushed in where the pattern runs. The pattern is a 16-ray burst with alternating long and short rays and a ring, five small 6–8-ray stars, and a border line. An `etch` attribute marks the grooves.
- **Black five-leaf clover:** a separate bevelled mesh on the front cover (the right cover seen from outside), bone-parented so it closes with the cover. The devil's leaf is a little longer.
- **Spine:** three raised pewter panels following the spine's curve, with ridged ends and an S-scroll of swirls on each.
- **Cover shader:** indigo leather (noise grain and bump). The etched grooves glow violet through the `etch` attribute. The glow strength is a Value node driven by `#frame`: `3.0 + 2.0*sin(frame*0.12)`.
- **Smoke:**
  - **Volume box:** a Principled Volume driven by 4D noise animated by `#frame`. It is masked by a signed-distance falloff to the page blocks' outline and fades with height, so it hugs the page edges.
  - **Particle system:** emits from the `page_edges` vertex group and instances dark purple-black volumetric puffs that drift up.

Run it:
```
blender --background --python tools/blender/build_zagred_grimoire.py -- --render open.png --frame 70 --view open --engine cycles
blender --background --python tools/blender/build_zagred_grimoire.py -- --render closed.png --frame 140 --view closed --engine cycles
```
Other options: `--glb`, `--save`, `--res`, `--samples`.

## Not verified

- **Compile:** the Java has not been compiled in this session (no Gradle offline here). CI compiles it on push.
- **Untested in game:**
  - none of 0.47 has been played;
  - the boss fight, the sludge spread/restore, the chat trigger and the Tensura effect ids `paralysis`, `silence` and `fear` are untested in game;
  - the block model's full-bright overlay (`neoforge_data`) is untested in game.
- **Trnightmare** effects are not on the build path, so none are used.
