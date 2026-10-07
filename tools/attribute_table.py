"""0.53: the table of the 37 attributes of the Black Clover Magic and VFX expansion (29 new magics, 8 upgrades of existing ones).

Used by tools/gen_attribute_icons.py and by the scaffold of the expansion. One row per attribute:

  key        the MagicType name (new) or the existing MagicType it extends (upgrade)
  pascal     class-name stem: book/<Pascal>Book.java, vfx/client/layer/<Pascal>Layer.java, client/aura/<Pascal>Aura.java,
             prop/<Pascal>Props.java, client/prop/<Pascal>PropPainter.java (upgrades: book/ext/<Pascal>Ext.java)
  new        True = a new MagicType and Book; False = an upgrade: new spells appended to the existing book through its Ext class
  display, word, soul, hit, particle, guard   the MagicType constructor arguments (new ones)
  cover, glow, trim   grimoire cover RGB, glow RGB and metal (GOLD / SILVER / BRONZE / DARK) for BookPalette
  restricted True = never rolled for a soul (command, devil or admin only), like Anti-Magic and Kotodama
  wiki       the Black Clover wiki page title
  spec       what the owner asked for
"""

COMMON = ("All magic draws on magicules (to project and manifest spells) and aura (physical enhancement and reinforcement). Construct "
          "durability, projectile count and AOE radius scale with the caster's EP (BalanceLaw / EnergyBridge power). Use Tensura's "
          "status conditions freely (Magic Jamming = the 'silence' effect, Spiritual Damage = EnergyBridge.spirit, Absolute Paralysis "
          "= the 'paralysis' effect, Energy Drain = EnergyBridge.drain, resistance shredding).")

# key, pascal, new, display, word, soul, hit, particle, guard, cover, glow, trim, restricted, wiki, spec
_ROWS = [
    # ---- 3.1 Biological, anatomical and beast manifestations (render-layer heavy)
    ("BEAST", "Beast", False, "Beast Magic", "Beast", "BATTLE", "PIERCE", "CRIT", "MOVEMENT_SPEED", 0xB89466, 0xFFB060, "BRONZE", False, "Beast Magic",
     "UPGRADE. Animalistic physical traits and extreme Aura-based physical enhancement. RENDER LAYER: an ADDITIVE foreground layer projects a "
     "spectral, roaring animal aura over the caster's model (Aura.BEAST, styles for different beasts) and fast movement leaves spectral "
     "claw-mark trails (a VFX shape)."),
    ("DEMON_BEAST", "DemonBeast", True, "Demon Beast Magic", "Underworld Beast", "DARKNESS", "PIERCE", "SOUL_FIRE_FLAME", "DAMAGE_RESISTANCE", 0x3A0F2E, 0xB02A6A, "DARK", True, "Demon Beast Magic",
     "Summons massive, spectral underworld beasts. RENDER LAYER: high-poly (for Minecraft: finely built) assets use a custom translucent / "
     "ethereal layer that visually engulfs the caster WITHOUT obscuring their hitbox (Aura.DEMON_BEAST, plus prop kinds for the beasts)."),
    ("BODY", "Body", True, "Body Magic", "Titan", "BATTLE", "PUSH", "CRIT", "DAMAGE_BOOST", 0xA05A3A, 0xFF8A4A, "BRONZE", False, "Body Magic",
     "Extreme physical enhancement shown as pulsing muscle expansion. RENDER LAYER: a custom ENTITY CUTOUT layer overlays the expanded muscle "
     "mesh seamlessly onto the player model (Aura.BODY, the posed player model drawn again slightly larger with a muscle texture laid out "
     "like a player skin), with rapid steam emission VFX."),
    ("BLOOD", "Blood", False, "Blood Magic", "Crimson", "BATTLE", "DRAIN", "DAMAGE_INDICATOR", "REGENERATION", 0x6E0A14, 0xE0182C, "GOLD", False, "Blood Magic",
     "UPGRADE. Advanced fluid look mimicking viscous crimson liquid. Can drain target HP / magicules to form hyper-dense coagulated weapons "
     "(blood weapons as props or auras)."),
    ("BONE", "Bone", False, "Bone Magic", "Bone", "BATTLE", "PIERCE", "WHITE_ASH", "DAMAGE_RESISTANCE", 0x8E8E96, 0xF0EAD0, "BRONZE", False, "Bone Magic",
     "UPGRADE. Calcified, high-detail skeletal spikes and armour. Exceptional physical durability that scales with Aura (a bone-armour aura layer)."),
    ("EYE", "Eye", True, "Eye Magic", "Eye", "FANTASY", "BLIND", "ENCHANT", "NIGHT_VISION", 0x6A2A4A, 0xFF4A9A, "GOLD", False, "Eye Magic",
     "Summons floating, 3D-modelled eyes. Tracks enemy magicule signatures; breaking line of sight is required to avoid homing attacks or "
     "localized Magic Jamming (prop kinds for the eyes with real tracking behaviour)."),
    ("EYEBALL", "Eyeball", True, "Eyeball Magic", "Eyeball", "DARKNESS", "BLIND", "WITCH", "NIGHT_VISION", 0x5A1A2A, 0xFF3A3A, "DARK", False, "Eyeball Magic",
     "Floating fleshy or ethereal eyeballs (a different flavour from Eye Magic: fleshy, with veins and a roving pupil). Tracks enemy magicule "
     "signatures; breaking line of sight is required to avoid homing attacks or localized Magic Jamming."),
    # ---- 3.2 Demonic and forbidden arts
    ("CURSE", "Curse", True, "Curse Magic", "Curse", "DARKNESS", "WITHER", "SQUID_INK", "DAMAGE_BOOST", 0x1E0A24, 0x9A2AFF, "DARK", True, "Curse Magic",
     "Summons glowing, jagged black runes. Applies permanent-feeling (long, stacking) max-HP reduction and heavy Spiritual Damage. Corrupted "
     "shaders: deep purple / black emissions, high-contrast bloom."),
    ("CURSE_WARDING", "CurseWarding", True, "Curse-Warding Magic", "Ward", "TIME", "WEAKEN", "ENCHANT", "DAMAGE_RESISTANCE", 0x3A2A5A, 0xC08AFF, "SILVER", False, "Curse-Warding Magic",
     "The counter to curses: it can hijack or re-route existing status effects (move a debuff from an ally to a foe, reflect a curse back). "
     "Pale-violet warding runes."),
    ("DEMON_FIRE", "DemonFire", True, "Demon Fire Magic", "Demon Flame", "DARKNESS", "BURN", "SOUL_FIRE_FLAME", "FIRE_RESISTANCE", 0x1A0A1A, 0x8A2AD0, "DARK", True, "Demon Fire Magic",
     "Corrupted fire: INVERTED / blackened colour palette. Completely bypasses standard elemental immunities and barriers, dealing "
     "unmitigated Spiritual Damage."),
    ("DEMON_ICE", "DemonIce", True, "Demon Ice Magic", "Demon Frost", "DARKNESS", "FREEZE", "SNOWFLAKE", "DAMAGE_RESISTANCE", 0x10182A, 0x3A6AFF, "DARK", True, "Demon Ice Magic",
     "Corrupted ice: inverted / blackened palette (black ice with violet-blue light). Bypasses elemental immunities and barriers, dealing "
     "unmitigated Spiritual Damage."),
    ("DEMON_LIGHT", "DemonLight", True, "Demon Light Magic", "Demon Light", "DARKNESS", "PIERCE", "END_ROD", "MOVEMENT_SPEED", 0x1A1A0A, 0xFF3AD0, "DARK", True, "Demon Light Magic",
     "Corrupted light: black beams with a magenta bloom. Bypasses elemental immunities and barriers, dealing unmitigated Spiritual Damage."),
    ("DEMON_WATER", "DemonWater", True, "Demon Water Magic", "Demon Tide", "DARKNESS", "PUSH", "SPLASH", "WATER_BREATHING", 0x0A141A, 0x20D0A0, "DARK", True, "Demon Water Magic",
     "Corrupted water: ink-black water with teal-green light. Bypasses elemental immunities and barriers, dealing unmitigated Spiritual Damage."),
    # ---- 3.3 Spatiotemporal, conceptual and utility
    ("GRAVITY", "Gravity", False, "Gravity Magic", "Gravity", "SPACE", "SLOW", "REVERSE_PORTAL", "DAMAGE_RESISTANCE", 0x3D2462, 0x8A4CD0, "GOLD", False, "Gravity Magic",
     "UPGRADE. Heavy screen-space distortion (as far as the VFX layer system can fake it) and localized black-hole looks. Alters the gravity of "
     "targeted entities, pinning them with Absolute Paralysis or crushing their defences."),
    ("SEALING", "Seal", False, "Seal Magic", "Seal", "TIME", "WEAKEN", "ENCHANT", "DAMAGE_RESISTANCE", 0x2C4FA8, 0xFF5A78, "SILVER", False, "Seal Magic",
     "UPGRADE (the book is SEALING). High-complexity 3D geometric arrays. Magic Jamming / sealing of what is inside."),
    ("BARRIER", "Barrier", True, "Barrier Magic", "Barrier", "SPACE", "PUSH", "PORTAL", "DAMAGE_RESISTANCE", 0x2A6A8A, 0x6AD0FF, "SILVER", False, "Barrier Magic (attribute)",
     "Refractive glass domes. RENDER LAYER: strict TRANSLUCENT rendering so entities trapped inside stay perfectly visible while it is up. "
     "Nullifies incoming attacks strictly by EP checks; applies Magic Jamming to trapped targets."),
    ("IMITATION", "Imitation", False, "Copy Magic", "Copy", "FANTASY", "PIERCE", "ENCHANT", "DAMAGE_BOOST", 0x5F6A80, 0xA0B0D0, "GOLD", False, "Imitation Magic",
     "UPGRADE (the book is COPY; the wiki's Imitation Magic is the same idea). Scanning VFX (sweeping light grids). Temporarily copies the "
     "target's elemental attribute and spells at the cost of immense magicule consumption."),
    ("KEY", "Key", True, "Key Magic", "Key", "SPACE", "PULL", "ENCHANT", "MOVEMENT_SPEED", 0x8A6A1A, 0xFFD04A, "GOLD", False, "Key Magic",
     "Summons ornate 3D golden keys used to tear open spatial rifts (portals) or lock / seal magical abilities (Magic Jamming)."),
    ("CHAIN", "Chain", True, "Chain Magic", "Chain", "BATTLE", "SLOW", "CRIT", "DAMAGE_RESISTANCE", 0x4A4A52, 0xC0C8E0, "SILVER", False, "Chain Magic",
     "Dynamic 3D physics chains with metallic PBR-style textures. Restricts movement and leeches Aura from bound targets."),
    ("BUTOH", "Butoh", True, "Butoh Magic", "Dance", "FANTASY", "PUSH", "NOTE", "MOVEMENT_SPEED", 0x6A1A3A, 0xFF5A8A, "GOLD", False, "Butoh Magic",
     "Rhythmic, animation-driven magic. The caster emits rhythmic shockwaves that buff allied Aura output and disrupt enemy casting timings."),
    # ---- 3.4 Flora, fungi and organic generation
    ("BRIAR", "Briar", True, "Briar Magic", "Briar", "EARTH", "POISON", "COMPOSTER", "REGENERATION", 0x3A5A2A, 0x8AFF4A, "BRONZE", False, "Briar Magic",
     "Procedural thorny vines that apply Energy Drain."),
    ("CHERRY_BLOSSOM", "CherryBlossom", True, "Cherry Blossom Magic", "Sakura", "EARTH", "BLIND", "CHERRY_LEAVES", "REGENERATION", 0xD87AA0, 0xFFB0D0, "SILVER", False, "Cherry Blossom Magic",
     "GPU-instanced-style particle swarms of swirling pink petals that blind enemies and deal rapid, thousand-cut chip damage."),
    ("FUNGUS", "Fungus", True, "Fungus Magic", "Spore", "EARTH", "POISON", "SPORE_BLOSSOM_AIR", "REGENERATION", 0x6A5A3A, 0xC0A04A, "BRONZE", False, "Fungus Magic",
     "Dense, volumetric spore clouds. Applies localized poison and siphons magicules from anything within the area."),
    ("FOOD", "Food", True, "Food Magic", "Feast", "FANTASY", "DRAIN", "COMPOSTER", "REGENERATION", 0xC8803A, 0xFFC04A, "GOLD", False, "Food Magic",
     "Summons 3D-modelled culinary assets (props). Consuming them restores Aura / Magicules and applies temporary EP buffs to allies. "
     "(Charmy's Cotton / Food grimoire already has Food pages; this is the standalone attribute.)"),
    # ---- 3.5 Elemental, mineral and state alteration
    ("CRYSTAL", "Crystal", True, "Crystal Magic", "Crystal", "EARTH", "PIERCE", "END_ROD", "DAMAGE_RESISTANCE", 0x6A9AD0, 0xA0E0FF, "SILVER", False, "Crystal Magic",
     "Highly refractive, multi-faceted gem look. Can redirect light-based attacks."),
    ("CORUNDUM", "Corundum", True, "Corundum Magic", "Corundum", "EARTH", "WEAKEN", "CRIT", "DAMAGE_RESISTANCE", 0xB02A3A, 0xFF6A7A, "SILVER", False, "Corundum Magic",
     "Ruby / sapphire-like gem shaders. Constructs unbreakable EP-scaled fortresses."),
    ("BRONZE", "Bronze", True, "Bronze Magic", "Bronze", "EARTH", "PUSH", "CRIT", "DAMAGE_RESISTANCE", 0x8A5A2A, 0xD09A4A, "BRONZE", False, "Bronze Magic",
     "Metal Magic (Bronze): a tarnished green-and-brown metal look. Summons heavy physical weaponry and shields that rely on physical "
     "penetration rather than magical damage."),
    ("COPPER", "Copper", True, "Copper Magic", "Copper", "EARTH", "SHOCK", "ELECTRIC_SPARK", "DAMAGE_RESISTANCE", 0xB0602A, 0xFF9A4A, "BRONZE", False, "Copper Magic",
     "Metal Magic (Copper): a reflective orange metal look. Summons heavy physical weaponry and shields that rely on physical penetration."),
    ("IRON", "Iron", True, "Iron Magic", "Iron", "BATTLE", "PUSH", "CRIT", "DAMAGE_RESISTANCE", 0x5A5E66, 0xB0B8C8, "SILVER", False, "Iron Magic",
     "Metal Magic (Iron): a dull grey metal look. Summons heavy physical weaponry and shields that rely on physical penetration."),
    ("BLACK_OIL", "BlackOil", True, "Black Oil Magic", "Black Oil", "DARKNESS", "SLOW", "SQUID_INK", "DAMAGE_RESISTANCE", 0x101018, 0x40E0A0, "DARK", False, "Black Oil Magic",
     "An iridescent, viscous fluid look that drastically reduces entity movement speed and is highly explosive."),
    ("GEL", "Gel", True, "Gel Magic", "Gel", "WATER", "SLOW", "SPLASH", "DAMAGE_RESISTANCE", 0x4AA0A0, 0x7AFFD8, "SILVER", False, "Gel Magic",
     "Semi-translucent jiggle-physics gel that absorbs and nullifies blunt kinetic force."),
    ("GLASS", "Glass", True, "Glass Magic", "Glass", "EARTH", "PIERCE", "END_ROD", "DAMAGE_RESISTANCE", 0x8AC0D0, 0xE0FFFF, "SILVER", False, "Glass Magic",
     "Translucent, hyper-sharp shards that apply stacking bleed."),
    ("BUBBLE", "Bubble", True, "Bubble Magic", "Bubble", "WATER", "LEVITATE", "BUBBLE", "SLOW_FALLING", 0x6A9AE0, 0xA0D8FF, "SILVER", False, "Bubble Magic",
     "Iridescent, soapy refraction look. RENDER LAYER: bubbles use a custom refractive layer that distorts the background behind the sphere "
     "(as far as possible without a screen-copy shader: fresnel rims, iridescent bands, rim lensing), while trapping targets in zero gravity."),
    ("ICE", "Ice", False, "Ice Magic", "Frost", "WATER", "FREEZE", "SNOWFLAKE", "DAMAGE_RESISTANCE", 0x8CC6E6, 0x9FE8FF, "SILVER", False, "Ice Magic",
     "UPGRADE. Dense frost particle emission and sub-surface frozen textures. Shards act as physical projectiles, while freezing AOEs apply "
     "Absolute Paralysis."),
    ("ICE_WEDGE", "IceWedge", True, "Ice Wedge Magic", "Wedge", "WATER", "FREEZE", "SNOWFLAKE", "DAMAGE_RESISTANCE", 0x6AB0D8, 0xC0F0FF, "SILVER", False, "Ice Wedge Magic",
     "Ice wedges: driven-in wedge-shaped ice that splits and freezes (a different shape of ice from Ice Magic: wedges, splitting)."),
    # ---- 3.6 Swarm and light
    ("LEGION", "Legion", True, "Legion Magic", "Legion", "FANTASY", "PUSH", "ENCHANT", "ABSORPTION", 0x6A5A3A, 0xD0C080, "BRONZE", False, "Legion Magic",
     "Summons swarms of low-poly, faceless humanoid constructs (prop kinds) to overwhelm targets. Scales in quantity with the caster's total "
     "magicule pool."),
    ("LIGHT", "Light", False, "Light Magic", "Light", "LIGHT", "PIERCE", "END_ROD", "MOVEMENT_SPEED", 0xDDBE58, 0xFFE680, "GOLD", False, "Light Magic",
     "UPGRADE. The highest-velocity travel and projectile speed in the framework, high-bloom photon geometry, inflicting Spiritual Damage."),
]

ATTRS = [dict(zip(("key", "pascal", "new", "display", "word", "soul", "hit", "particle", "guard", "cover", "glow", "trim", "restricted", "wiki", "spec"), r)) for r in _ROWS]
NEW = [a for a in ATTRS if a["new"]]
UPGRADES = [a for a in ATTRS if not a["new"]]


def by_key(k):
    return next(a for a in ATTRS if a["key"] == k)
