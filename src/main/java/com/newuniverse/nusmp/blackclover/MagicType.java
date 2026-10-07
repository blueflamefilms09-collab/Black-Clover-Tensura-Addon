package com.newuniverse.nusmp.blackclover;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Black Clover magic attributes, grouped by TR Nightmare soul type.
 * word = the prefix used for spell names, e.g. "Flame" -> "Flame Bullet".
 */
public enum MagicType {
    // FLAME soul
    FLAME("Flame Magic", "Flame", "FLAME", HitEffect.BURN, () -> ParticleTypes.FLAME, () -> MobEffects.FIRE_RESISTANCE),
    EXPLOSION("Explosion Magic", "Blast", "FLAME", HitEffect.PUSH, () -> ParticleTypes.EXPLOSION, () -> MobEffects.DAMAGE_BOOST),
    MAGMA("Magma Magic", "Magma", "FLAME", HitEffect.BURN, () -> ParticleTypes.LAVA, () -> MobEffects.FIRE_RESISTANCE),
    // WATER soul
    WATER("Water Magic", "Sea Dragon", "WATER", HitEffect.PUSH, () -> ParticleTypes.SPLASH, () -> MobEffects.WATER_BREATHING),
    ICE("Ice Magic", "Frost", "WATER", HitEffect.FREEZE, () -> ParticleTypes.SNOWFLAKE, () -> MobEffects.DAMAGE_RESISTANCE),
    MERCURY("Mercury Magic", "Silver", "WATER", HitEffect.SLOW, () -> ParticleTypes.WHITE_ASH, () -> MobEffects.DAMAGE_RESISTANCE),
    MIST("Mist Magic", "Mist", "WATER", HitEffect.BLIND, () -> ParticleTypes.CLOUD, () -> MobEffects.INVISIBILITY),
    // WIND soul
    WIND("Wind Magic", "Gale", "WIND", HitEffect.LEVITATE, () -> ParticleTypes.GUST, () -> MobEffects.MOVEMENT_SPEED),
    STAR("Star Magic", "Star", "WIND", HitEffect.PIERCE, () -> ParticleTypes.END_ROD, () -> MobEffects.MOVEMENT_SPEED),
    STORM("Storm Magic", "Tempest", "WIND", HitEffect.SHOCK, () -> ParticleTypes.ELECTRIC_SPARK, () -> MobEffects.SLOW_FALLING),
    // EARTH soul
    EARTH("Earth Magic", "Stone", "EARTH", HitEffect.SLOW, () -> ParticleTypes.CRIT, () -> MobEffects.DAMAGE_RESISTANCE),
    PLANT("Plant Magic", "Thorn", "EARTH", HitEffect.POISON, () -> ParticleTypes.HAPPY_VILLAGER, () -> MobEffects.REGENERATION),
    SAND("Sand Magic", "Sand", "EARTH", HitEffect.BLIND, () -> ParticleTypes.WHITE_ASH, () -> MobEffects.DAMAGE_RESISTANCE),
    // LIGHT soul
    LIGHT("Light Magic", "Light", "LIGHT", HitEffect.PIERCE, () -> ParticleTypes.END_ROD, () -> MobEffects.MOVEMENT_SPEED),
    LIGHTNING("Lightning Magic", "Thunder", "LIGHT", HitEffect.SHOCK, () -> ParticleTypes.ELECTRIC_SPARK, () -> MobEffects.MOVEMENT_SPEED),
    SWORD("Sword Magic", "Blade", "LIGHT", HitEffect.PIERCE, () -> ParticleTypes.SWEEP_ATTACK, () -> MobEffects.DAMAGE_BOOST),
    // DARKNESS soul
    DARK("Dark Magic", "Dark", "DARKNESS", HitEffect.WITHER, () -> ParticleTypes.SQUID_INK, () -> MobEffects.DAMAGE_BOOST),
    SHADOW("Shadow Magic", "Shadow", "DARKNESS", HitEffect.BLIND, () -> ParticleTypes.SMOKE, () -> MobEffects.INVISIBILITY),
    POISON("Poison Magic", "Venom", "DARKNESS", HitEffect.POISON, () -> ParticleTypes.WITCH, () -> MobEffects.REGENERATION),
    // SPACE soul
    SPATIAL("Spatial Magic", "Rift", "SPACE", HitEffect.PULL, () -> ParticleTypes.PORTAL, () -> MobEffects.MOVEMENT_SPEED),
    MIRROR("Mirror Magic", "Mirror", "SPACE", HitEffect.PIERCE, () -> ParticleTypes.END_ROD, () -> MobEffects.INVISIBILITY),
    GRAVITY("Gravity Magic", "Gravity", "SPACE", HitEffect.SLOW, () -> ParticleTypes.REVERSE_PORTAL, () -> MobEffects.DAMAGE_RESISTANCE),
    // TIME soul
    TIME("Time Magic", "Chrono", "TIME", HitEffect.SLOW, () -> ParticleTypes.ENCHANT, () -> MobEffects.DIG_SPEED),
    SEALING("Sealing Magic", "Seal", "TIME", HitEffect.WEAKEN, () -> ParticleTypes.ENCHANT, () -> MobEffects.DAMAGE_RESISTANCE),
    // BATTLE soul
    REINFORCEMENT("Reinforcement Magic", "Iron Fist", "BATTLE", HitEffect.PUSH, () -> ParticleTypes.CRIT, () -> MobEffects.DAMAGE_BOOST),
    BEAST("Beast Magic", "Beast", "BATTLE", HitEffect.PIERCE, () -> ParticleTypes.CRIT, () -> MobEffects.MOVEMENT_SPEED),
    BONE("Bone Magic", "Bone", "BATTLE", HitEffect.PIERCE, () -> ParticleTypes.WHITE_ASH, () -> MobEffects.DAMAGE_RESISTANCE),
    BLOOD("Blood Magic", "Crimson", "BATTLE", HitEffect.DRAIN, () -> ParticleTypes.DAMAGE_INDICATOR, () -> MobEffects.REGENERATION),
    // FANTASY soul
    CREATION("Creation Magic", "Creation", "FANTASY", HitEffect.PUSH, () -> ParticleTypes.TOTEM_OF_UNDYING, () -> MobEffects.ABSORPTION),
    COPY("Copy Magic", "Copy", "FANTASY", HitEffect.PIERCE, () -> ParticleTypes.ENCHANT, () -> MobEffects.DAMAGE_BOOST),
    ILLUSION("Illusion Magic", "Illusion", "FANTASY", HitEffect.BLIND, () -> ParticleTypes.WITCH, () -> MobEffects.INVISIBILITY),
    DREAM("Dream Magic", "Dream", "FANTASY", HitEffect.SLOW, () -> ParticleTypes.CHERRY_LEAVES, () -> MobEffects.REGENERATION),
    // Special: only an EMPTY soul with a five-leaf grimoire (the devil Liebe)
    ANTI_MAGIC("Anti-Magic", "Black", "EMPTY", HitEffect.NULLIFY, () -> ParticleTypes.SQUID_INK, () -> MobEffects.DAMAGE_BOOST),
    // Added later: keep at the END (item model variants use the ordinal)
    STEEL("Steel Magic", "Steel", "BATTLE", HitEffect.PIERCE, () -> ParticleTypes.CRIT, () -> MobEffects.DAMAGE_RESISTANCE),
    THREAD("Thread Magic", "Thread", "FANTASY", HitEffect.SLOW, () -> ParticleTypes.ENCHANT, () -> MobEffects.DAMAGE_RESISTANCE),
    // 0.34: attributes from the wiki (Grey, Zora, Charmy, Henry); appended so every ordinal above stays put
    TRANSMUTATION("Transmutation Magic", "Alchemy", "EARTH", HitEffect.WEAKEN, () -> ParticleTypes.ENCHANT, () -> MobEffects.DAMAGE_RESISTANCE),
    ASH("Ash Magic", "Ash", "FLAME", HitEffect.WITHER, () -> ParticleTypes.ASH, () -> MobEffects.DAMAGE_RESISTANCE),
    COTTON("Cotton Magic", "Cotton", "FANTASY", HitEffect.SLOW, () -> ParticleTypes.WHITE_ASH, () -> MobEffects.REGENERATION),
    RECOMBINATION("Recombination Magic", "Bull", "FANTASY", HitEffect.PUSH, () -> ParticleTypes.CRIT, () -> MobEffects.ABSORPTION),
    // 0.40: Painting Magic (Rill Boismortier, Lira), appended
    PAINTING("Painting Magic", "Canvas", "FANTASY", HitEffect.SLOW, () -> ParticleTypes.DRIPPING_WATER, () -> MobEffects.ABSORPTION),
    // 0.45: World Tree (William Vangeance), Dice (Baval, David Swallow), Slash (Jack the Ripper), Compass (Letoile Becquerel); appended
    WORLD_TREE("World Tree Magic", "Yggdrasil", "EARTH", HitEffect.SLOW, () -> ParticleTypes.HAPPY_VILLAGER, () -> MobEffects.REGENERATION),
    DICE("Dice Magic", "Fortune", "FANTASY", HitEffect.PUSH, () -> ParticleTypes.ENCHANT, () -> MobEffects.LUCK),
    SLASH("Slash Magic", "Ripper", "BATTLE", HitEffect.PIERCE, () -> ParticleTypes.SWEEP_ATTACK, () -> MobEffects.DAMAGE_BOOST),
    COMPASS("Compass Magic", "Bearing", "SPACE", HitEffect.PULL, () -> ParticleTypes.WAX_ON, () -> MobEffects.MOVEMENT_SPEED),
    // 0.47: Kotodama (Word Soul) Magic, Zagred's - God-class, creative only (never rolled; see book.KotodamaWords); appended
    KOTODAMA("Kotodama Magic", "Word Soul", "EMPTY", HitEffect.NULLIFY, () -> ParticleTypes.SQUID_INK, () -> MobEffects.DAMAGE_RESISTANCE),
    // 0.49: Game Magic (Gifso, Spade Kingdom); appended
    GAME("Game Magic", "Gamemaster", "FANTASY", HitEffect.SLOW, () -> ParticleTypes.ENCHANT, () -> MobEffects.LUCK),
    // 0.53: the Black Clover Magic and VFX expansion (29 attributes); appended. Restricted ones are never rolled for a soul.
    DEMON_BEAST("Demon Beast Magic", "Underworld Beast", "DARKNESS", HitEffect.PIERCE, () -> ParticleTypes.SOUL_FIRE_FLAME, () -> MobEffects.DAMAGE_RESISTANCE),
    BODY("Body Magic", "Titan", "BATTLE", HitEffect.PUSH, () -> ParticleTypes.CRIT, () -> MobEffects.DAMAGE_BOOST),
    EYE("Eye Magic", "Eye", "FANTASY", HitEffect.BLIND, () -> ParticleTypes.ENCHANT, () -> MobEffects.NIGHT_VISION),
    EYEBALL("Eyeball Magic", "Eyeball", "DARKNESS", HitEffect.BLIND, () -> ParticleTypes.WITCH, () -> MobEffects.NIGHT_VISION),
    CURSE("Curse Magic", "Curse", "DARKNESS", HitEffect.WITHER, () -> ParticleTypes.SQUID_INK, () -> MobEffects.DAMAGE_BOOST),
    CURSE_WARDING("Curse-Warding Magic", "Ward", "TIME", HitEffect.WEAKEN, () -> ParticleTypes.ENCHANT, () -> MobEffects.DAMAGE_RESISTANCE),
    DEMON_FIRE("Demon Fire Magic", "Demon Flame", "DARKNESS", HitEffect.BURN, () -> ParticleTypes.SOUL_FIRE_FLAME, () -> MobEffects.FIRE_RESISTANCE),
    DEMON_ICE("Demon Ice Magic", "Demon Frost", "DARKNESS", HitEffect.FREEZE, () -> ParticleTypes.SNOWFLAKE, () -> MobEffects.DAMAGE_RESISTANCE),
    DEMON_LIGHT("Demon Light Magic", "Demon Light", "DARKNESS", HitEffect.PIERCE, () -> ParticleTypes.END_ROD, () -> MobEffects.MOVEMENT_SPEED),
    DEMON_WATER("Demon Water Magic", "Demon Tide", "DARKNESS", HitEffect.PUSH, () -> ParticleTypes.SPLASH, () -> MobEffects.WATER_BREATHING),
    BARRIER("Barrier Magic", "Barrier", "SPACE", HitEffect.PUSH, () -> ParticleTypes.PORTAL, () -> MobEffects.DAMAGE_RESISTANCE),
    KEY("Key Magic", "Key", "SPACE", HitEffect.PULL, () -> ParticleTypes.ENCHANT, () -> MobEffects.MOVEMENT_SPEED),
    CHAIN("Chain Magic", "Chain", "BATTLE", HitEffect.SLOW, () -> ParticleTypes.CRIT, () -> MobEffects.DAMAGE_RESISTANCE),
    BUTOH("Butoh Magic", "Dance", "FANTASY", HitEffect.PUSH, () -> ParticleTypes.NOTE, () -> MobEffects.MOVEMENT_SPEED),
    BRIAR("Briar Magic", "Briar", "EARTH", HitEffect.POISON, () -> ParticleTypes.COMPOSTER, () -> MobEffects.REGENERATION),
    CHERRY_BLOSSOM("Cherry Blossom Magic", "Sakura", "EARTH", HitEffect.BLIND, () -> ParticleTypes.CHERRY_LEAVES, () -> MobEffects.REGENERATION),
    FUNGUS("Fungus Magic", "Spore", "EARTH", HitEffect.POISON, () -> ParticleTypes.SPORE_BLOSSOM_AIR, () -> MobEffects.REGENERATION),
    FOOD("Food Magic", "Feast", "FANTASY", HitEffect.DRAIN, () -> ParticleTypes.COMPOSTER, () -> MobEffects.REGENERATION),
    CRYSTAL("Crystal Magic", "Crystal", "EARTH", HitEffect.PIERCE, () -> ParticleTypes.END_ROD, () -> MobEffects.DAMAGE_RESISTANCE),
    CORUNDUM("Corundum Magic", "Corundum", "EARTH", HitEffect.WEAKEN, () -> ParticleTypes.CRIT, () -> MobEffects.DAMAGE_RESISTANCE),
    BRONZE("Bronze Magic", "Bronze", "EARTH", HitEffect.PUSH, () -> ParticleTypes.CRIT, () -> MobEffects.DAMAGE_RESISTANCE),
    COPPER("Copper Magic", "Copper", "EARTH", HitEffect.SHOCK, () -> ParticleTypes.ELECTRIC_SPARK, () -> MobEffects.DAMAGE_RESISTANCE),
    IRON("Iron Magic", "Iron", "BATTLE", HitEffect.PUSH, () -> ParticleTypes.CRIT, () -> MobEffects.DAMAGE_RESISTANCE),
    BLACK_OIL("Black Oil Magic", "Black Oil", "DARKNESS", HitEffect.SLOW, () -> ParticleTypes.SQUID_INK, () -> MobEffects.DAMAGE_RESISTANCE),
    GEL("Gel Magic", "Gel", "WATER", HitEffect.SLOW, () -> ParticleTypes.SPLASH, () -> MobEffects.DAMAGE_RESISTANCE),
    GLASS("Glass Magic", "Glass", "EARTH", HitEffect.PIERCE, () -> ParticleTypes.END_ROD, () -> MobEffects.DAMAGE_RESISTANCE),
    BUBBLE("Bubble Magic", "Bubble", "WATER", HitEffect.LEVITATE, () -> ParticleTypes.BUBBLE, () -> MobEffects.SLOW_FALLING),
    ICE_WEDGE("Ice Wedge Magic", "Wedge", "WATER", HitEffect.FREEZE, () -> ParticleTypes.SNOWFLAKE, () -> MobEffects.DAMAGE_RESISTANCE),
    LEGION("Legion Magic", "Legion", "FANTASY", HitEffect.PUSH, () -> ParticleTypes.ENCHANT, () -> MobEffects.ABSORPTION);

    public final String displayName;
    public final String word;
    public final String soulType;
    public final HitEffect hit;
    private final Supplier<SimpleParticleType> particle;
    private final Supplier<Holder<MobEffect>> guardEffect;

    MagicType(String displayName, String word, String soulType, HitEffect hit,
              Supplier<SimpleParticleType> particle, Supplier<Holder<MobEffect>> guardEffect) {
        this.displayName = displayName;
        this.word = word;
        this.soulType = soulType;
        this.hit = hit;
        this.particle = particle;
        this.guardEffect = guardEffect;
    }

    /** 0.53: never rolled for a soul (devil, command or admin only), like Anti-Magic and Kotodama. */
    public boolean restricted() {
        return switch (this) { case ANTI_MAGIC, KOTODAMA, DEMON_BEAST, CURSE, DEMON_FIRE, DEMON_ICE, DEMON_LIGHT, DEMON_WATER -> true; default -> false; };
    }

    public SimpleParticleType particle() { return particle.get(); }
    public Holder<MobEffect> guardEffect() { return guardEffect.get(); }

    /** Magic types available to a soul type. EMPTY (or no Nightmare) can awaken any normal magic. */
    public static List<MagicType> forSoul(String soul) {
        List<MagicType> list = new ArrayList<>();
        for (MagicType t : values()) {
            if (t != ANTI_MAGIC && t != KOTODAMA && t.soulType.equals(soul)) list.add(t);
        }
        if (list.isEmpty()) {
            for (MagicType t : values()) if (!t.restricted()) list.add(t);
        }
        return list;
    }

    public static MagicType byName(String name) {
        try { return valueOf(name); } catch (Exception e) { return FLAME; }
    }

    /** What a spell does to whatever it hits. */
    public enum HitEffect { BURN, FREEZE, SHOCK, SLOW, POISON, WITHER, BLIND, LEVITATE, PULL, PUSH, WEAKEN, PIERCE, DRAIN, NULLIFY }
}
