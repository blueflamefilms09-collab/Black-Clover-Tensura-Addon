# Extended attributes (0.45): Light, World Tree, Dice, Slash, Compass, Mercury

**Replacements:** Light Magic and Mercury Magic (their page ids are kept).

**New attributes:** World Tree, Dice, Slash and Compass. They are appended to `MagicType`, have their own books `book_world_tree`, `book_dice`, `book_slash` and `book_compass`, and get icons, lang entries and cover colours.

**Canon books:**
- William Vangeance now uses World Tree Magic (was Plant). His old Plant page `yggdrasil` stays registered.
- Jack the Ripper now uses Slash Magic (was Sword). His old Sword page `ripper_cut` stays registered.
- Letoile Becquerel is added as a new Compass canon book.

Wiki facts come from the CI wiki fetch (Light, World Tree, Dice, Slash, Compass, Mercury Magic). The wiki has no page for "Vuhu": Dice Magic's wiki users are Baval and David Swallow.

## Shared Tensura bridge (`book/EnergyBridge.java`)

- **Pools:** pages spend magicules through Tensura. Physical enhancement and high-speed movement draw on **Aura** (falling back to magicules): Light Speed, Ripper Dash and Forearm Blades (held per second).
- **EP scaling:**
  - `power`: EP log + armour, ×0.8 … ×2.4 (shared with Mirror).
  - `scale`: ×0.9 … ×1.5.
  - `modifier`: a D&D-style +0 … +4.
  - Mercury uses the full `power` curve ("scales aggressively"). The others use `scale`.
  - EP also drives projectile speed (Light), construct and terrain size (World Tree, Mercury's eagle and dome) and VFX size.
- **Effects:**
  - Tensura `silence` = magic jamming.
  - `fragility` = resistance shredding.
  - **Energy Drain** with transfer: `drain(from, to, frac)`.
  - **Spiritual damage**: `spirit(t, n)`.
  - **Aura barrier**: a target whose aura is ≥ 30% full.
  - **Armour bypass**: a damage multiplier that cancels a fraction of the target's armour (`CombatRules`).
- **Trnightmare:** not on the build path, so its effects are not ported. Any registry id can be passed to `EnergyBridge.effect` later.

## Light Magic (replacement)

**Fastest projectiles in the mod:** 6 blocks/tick × (0.85 + 0.25 × EP power).

**Every light hit:**
- goes through Tensura's light element;
- ignores armour;
- takes 1 spiritual health.

**Only an Aura barrier holds it:** the hit is halved and burns 5% of the target's aura.

| Page | |
|---|---|
| Light Sword of Judgment (`judgment`, kept) | Three geometric light swords cross the field at light speed, one homing. |
| Light Shaft of Divine Punishment (`divine_punishment`, kept) | 4–8 shafts (by EP) fall on the marked foe in quick succession. |
| Healing Ray of Light (new) | Heals an ally you look at (or yourself) and removes one affliction. |
| Lamp of Avior Gloria (new) | A lamp overhead rains light swords on every foe in reach for 3 s. |
| Light Speed (new) | Moves you up to 24 blocks instantly, cutting through what's in between. Costs Aura. |

## Mercury Magic (replacement)

**Two states:**
- free-flowing: the dome, and the liquid tails of every construct;
- hyper-dense: spears, the eagle.

The dome flows back into spears when it ends. Fire melts straight through the dome (the wiki weakness).

| Page | |
|---|---|
| Silver Blade (`silver_blade`, kept) | Mercury pours out and hardens into three razor spears. |
| Silver Guardian (`mercury_shield`, kept) | A liquid dome for 8 s: it catches shots and halves blows. At the end it becomes up to 6 homing spears. |
| Silver Rain (`mercury_rain`, kept; Nozel) | A storm of needle spears falls where you aim. |
| Silver Eagle (new) | A giant mercury eagle swoops along your sight line. Its wingspan grows with EP. |

## World Tree Magic (new; William)

**Real trees (the owner's call):** the spells grow real Minecraft trees with the vanilla tree generators.
- **Sizes:**
  - small: oak, birch, azalea (Mistilteinn Seed);
  - medium: fancy oak, jungle, spruce (the Yggdrasil ring);
  - large: fancy oak at low EP, dark oak or mega spruce at mid EP, mega jungle at high EP (Magic Tree Descent and the Yggdrasil centre).
- **Mangrove-root sculptures** are also grown into open space only.
- **Taking them back:** each tree's blocks are snapshotted. When the spell ends, every block the tree changed that is still the tree's is put back, so nothing built in the meantime is touched.
- **Keeping them:** server config `worldTreeTreesStay = true` keeps the trees. (A server restart before a spell ends also leaves its tree standing.)

**Energy Drain:**
- Bound foes lose magicules (Tensura).
- What they lose flows to you (and, under the tree, to your allies).

| Page | |
|---|---|
| Mistilteinn Seed (wiki) | A seed bolt: roots burst from the target, bind it and drain 3%. |
| Root Bind | Binds the foe for 2.5 s, drains it, then drags it to you. |
| Magic Tree Descent (wiki) | A real great tree (sized by EP) grows where you aim for 20 s. It binds and drains everything around it and shares what it drinks with you and your allies, who also regenerate. |
| Budding of Yggdrasil (wiki) | A ring of real trees and great roots, with a giant tree in front. Every foe inside is bound and drained for 5 s, and enemy shots are caught in mid-air. |

## Dice Magic (new)

**Elemental Dice (2d6):**
- The faces are 1 fire, 2 earth, 3 water, 4 wind, 5 lightning, 6 light.
- The higher face picks the element. It goes through Tensura's elemental damage types, so Tensura's elemental resistances and weaknesses react to it.
- The power is (sum / 7). Doubles resonate (×1.5).
- Each element adds a rider:
  - fire: burn;
  - earth: fragility;
  - water: energy drain to the caster;
  - wind: knockback;
  - lightning: stun-slow;
  - light: spiritual damage.

**Fate Die (d20), D&D rules:**
- **Roll:** d20 + modifier (EP: +0 … +4).
- **Natural 1, critical fumble:** the spell fails, and the caster is silenced (magic jamming) and slowed.
- **Natural 20, critical hit:**
  - maximum EP scale;
  - armour fully bypassed;
  - delivered as light (past every elemental resistance);
  - −4 spiritual health.
- **Anything else:** below 10 the spell glances (×0.5); above 10 it scales up to ×1.5 at 24.

**Other pages:**
- **Gambler's Fallacy** (wiki): three d20s in a row. Each roll under 10 adds +3 to the rolls after it. A 1 busts the streak.
- **Loaded Dice:** 30 s of advantage, and a natural 1 is rerolled once.

## Slash Magic (new; Jack)

**Adaptive cutting:**
- **Reading the defence:** every Slash hit on a defended foe reads it and adds a **permanent mark**, stored on the target, up to 10. "Defended" means armour 8+, toughness, an Aura barrier, Resistance or absorption.
- **What a mark does:** each one adds +5% damage and ignores another tenth of the target's armour.
- **At 10 marks:** the defence is "severed", and fragility II is applied.

| Page | |
|---|---|
| Slash Wave | A projected jagged crescent that pierces a 24-block line. |
| Forearm Blades | Jagged blades along both forearms for 20 s. Every melee blow gets +5 × scale and adapts. Held with Aura each second. |
| Ripper Dash | A 10-block Aura dash that cuts everyone on the path. |
| Death Scythe (wiki) | A full-circle reaping arc. |

## Compass Magic (new; Letoile)

**The array:**
- 3–5 brass compasses (by EP) hover in an arc behind you with a golden aura.
- Each has a green dial, a rose, glass, and a gold needle that swings to the target, overshoots and settles.

**Locking:**
- The needles lock onto the **strongest magicule signature** in sight (the highest Tensura max EP), or onto the Another Atlas mark.
- They home on it.

| Page | |
|---|---|
| Useless North (wiki) | Homing needles from the array. |
| Willful Compass (wiki) | For 10 s, every enemy shot within 7 blocks is wrenched round and sent back at its shooter, and becomes yours. |
| Another Atlas (wiki) | Charts a foe for 15 s: it glows, every lock finds it, and the array fires on it by itself every 1.5 s. |
| Compass Rose | Every foe within 20 blocks is locked and gets two homing needles. |

## VFX (Time Magic standard)

**Layers:** `LightTreeLayer`, `DiceLayer` and `SlashCompassMercuryLayer`. Textures come from `tools/gen_attr_vfx_textures.py`.

**Preview:**
```
python tools/vfx_preview/preview.py light_blade light_flare tree_roots tree_canopy tree_drain dice_d6 dice_d20 slash_blades slash_wave slash_scythe compass_array compass_lock mercury_dome mercury_spear mercury_eagle
```

- **Light:**
  - A geometric sword crosses the field in a few ticks, with a streak behind it.
  - Its **lens flare** rides the head and its ghosts are mirrored through the screen centre, as a real lens flare's are.
  - A six-ray star bursts on impact.
- **World Tree:**
  - Bark-textured roots arch out of the ground with emerald buds.
  - `TREE_CANOPY` (a drawn tree) stays registered but is no longer used: the spells grow real trees.
  - Drained mana flows in a sinuous emerald stream.
- **Dice:** real 3D dice built from quads.
  - Translucent resin faces in element colours, with gold glyph and number engravings.
  - A d20 icosahedron with a swirling galaxy core; opposite faces sum to 21.
  - They tumble on a bouncing arc and settle with the rolled face toward the viewer.
  - A natural 20 bursts gold; a natural 1 cracks with red light.
- **Slash:** jagged forearm blades follow the caster's facing; crescents leave afterimages; Death Scythe's arc carries a blade at its head.
- **Compass:** the array (rim, dial, rose, needle, glass); gilt homing needles; a lock sigil that snaps shut.
- **Mercury:**
  - A liquid chrome dome with a sliding specular highlight and ripples.
  - Chrome spears with liquid tails.
  - An eagle that turns its wings to the viewer, with a silver wake and falling drops.

## Blender

`tools/blender/build_dice_compass.py` builds three props. Preview: `docs/blender/previews/dice_compass.png`.

- **Elemental d6:** a bevelled resin cube (transmission 0.85, clear coat) with gold-filled engraved numbers.
- **Fate d20:** a violet resin icosahedron with an emissive noise galaxy core and gold numbers on all 20 faces (opposites sum to 21).
- **Brass compass:**
  - a bevelled case and bezel (metallic brass);
  - a green dial with an eight-point rose;
  - an emissive gold needle;
  - a glass cover (transmission 1).

Run it the same way as the palette builder (`--render`, `--glb`, `--engine cycles`).

**Photon / shader packs:** resin wants a low-roughness, high-transmission look. If you make block or item variants, give them labPBR `_s` maps the same way as the 0.44 paint: high smoothness, dielectric F0, and emission for the engraving and the galaxy core.
