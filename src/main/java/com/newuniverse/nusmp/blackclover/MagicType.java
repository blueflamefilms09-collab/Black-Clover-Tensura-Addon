package com.newuniverse.nusmp.blackclover;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import com.newuniverse.nusmp.core.magic.profile.MagicKeys;
import com.newuniverse.nusmp.core.magic.profile.MagicProfile;
import com.newuniverse.nusmp.core.magic.profile.MagicProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Black Clover magic attributes, grouped by TR Nightmare soul type.
 * word = the prefix used for spell names, e.g. "Flame" -> "Flame Bullet".
 */
public enum MagicType {
    // FLAME soul
    FLAME("Flame Magic", "Flame", "FLAME", HitEffect.BURN, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.FIRE_RESISTANCE),
    EXPLOSION("Explosion Magic", "Blast", "FLAME", HitEffect.PUSH, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_BOOST),
    MAGMA("Magma Magic", "Magma", "FLAME", HitEffect.BURN, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.FIRE_RESISTANCE),
    // WATER soul
    WATER("Water Magic", "Sea Dragon", "WATER", HitEffect.PUSH, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.WATER_BREATHING),
    ICE("Ice Magic", "Frost", "WATER", HitEffect.FREEZE, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_RESISTANCE),
    MERCURY("Mercury Magic", "Silver", "WATER", HitEffect.SLOW, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), () -> MobEffects.DAMAGE_RESISTANCE),
    MIST("Mist Magic", "Mist", "WATER", HitEffect.BLIND, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.INVISIBILITY),
    // WIND soul
    WIND("Wind Magic", "Gale", "WIND", HitEffect.LEVITATE, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.MOVEMENT_SPEED),
    STAR("Star Magic", "Star", "WIND", HitEffect.PIERCE, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.MOVEMENT_SPEED),
    STORM("Storm Magic", "Tempest", "WIND", HitEffect.SHOCK, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.SLOW_FALLING),
    // EARTH soul
    EARTH("Earth Magic", "Stone", "EARTH", HitEffect.SLOW, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_RESISTANCE),
    PLANT("Plant Magic", "Thorn", "EARTH", HitEffect.POISON, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.REGENERATION),
    SAND("Sand Magic", "Sand", "EARTH", HitEffect.BLIND, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), () -> MobEffects.DAMAGE_RESISTANCE),
    // LIGHT soul
    LIGHT("Light Magic", "Light", "LIGHT", HitEffect.PIERCE, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.MOVEMENT_SPEED),
    LIGHTNING("Lightning Magic", "Thunder", "LIGHT", HitEffect.SHOCK, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.MOVEMENT_SPEED),
    SWORD("Sword Magic", "Blade", "LIGHT", HitEffect.PIERCE, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_BOOST),
    // DARKNESS soul
    DARK("Dark Magic", "Dark", "DARKNESS", HitEffect.WITHER, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), () -> MobEffects.DAMAGE_BOOST),
    SHADOW("Shadow Magic", "Shadow", "DARKNESS", HitEffect.BLIND, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), () -> MobEffects.INVISIBILITY),
    POISON("Poison Magic", "Venom", "DARKNESS", HitEffect.POISON, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), () -> MobEffects.REGENERATION),
    // SPACE soul
    SPATIAL("Spatial Magic", "Rift", "SPACE", HitEffect.PULL, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), () -> MobEffects.MOVEMENT_SPEED),
    MIRROR("Mirror Magic", "Mirror", "SPACE", HitEffect.PIERCE, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.INVISIBILITY),
    GRAVITY("Gravity Magic", "Gravity", "SPACE", HitEffect.SLOW, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), () -> MobEffects.DAMAGE_RESISTANCE),
    // TIME soul
    TIME("Time Magic", "Chrono", "TIME", HitEffect.SLOW, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DIG_SPEED),
    SEALING("Sealing Magic", "Seal", "TIME", HitEffect.WEAKEN, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_RESISTANCE),
    // BATTLE soul
    REINFORCEMENT("Reinforcement Magic", "Iron Fist", "BATTLE", HitEffect.PUSH, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_BOOST),
    BEAST("Beast Magic", "Beast", "BATTLE", HitEffect.PIERCE, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.MOVEMENT_SPEED),
    BONE("Bone Magic", "Bone", "BATTLE", HitEffect.PIERCE, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), () -> MobEffects.DAMAGE_RESISTANCE),
    BLOOD("Blood Magic", "Crimson", "BATTLE", HitEffect.DRAIN, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), () -> MobEffects.REGENERATION),
    // FANTASY soul
    CREATION("Creation Magic", "Creation", "FANTASY", HitEffect.PUSH, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.ABSORPTION),
    COPY("Copy Magic", "Copy", "FANTASY", HitEffect.PIERCE, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_BOOST),
    ILLUSION("Illusion Magic", "Illusion", "FANTASY", HitEffect.BLIND, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), () -> MobEffects.INVISIBILITY),
    DREAM("Dream Magic", "Dream", "FANTASY", HitEffect.SLOW, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.REGENERATION),
    // Special: only an EMPTY soul with a five-leaf grimoire (the devil Liebe)
    ANTI_MAGIC("Anti-Magic", "Black", "EMPTY", HitEffect.NULLIFY, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), () -> MobEffects.DAMAGE_BOOST),
    // Added later: keep at the END (item model variants use the ordinal)
    STEEL("Steel Magic", "Steel", "BATTLE", HitEffect.PIERCE, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_RESISTANCE),
    THREAD("Thread Magic", "Thread", "FANTASY", HitEffect.SLOW, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_RESISTANCE),
    // 0.34: attributes from the wiki (Grey, Zora, Charmy, Henry); appended so every ordinal above stays put
    TRANSMUTATION("Transmutation Magic", "Alchemy", "EARTH", HitEffect.WEAKEN, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_RESISTANCE),
    ASH("Ash Magic", "Ash", "FLAME", HitEffect.WITHER, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), () -> MobEffects.DAMAGE_RESISTANCE),
    COTTON("Cotton Magic", "Cotton", "FANTASY", HitEffect.SLOW, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), () -> MobEffects.REGENERATION),
    RECOMBINATION("Recombination Magic", "Bull", "FANTASY", HitEffect.PUSH, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.ABSORPTION),
    // 0.40: Painting Magic (Rill Boismortier, Lira), appended
    PAINTING("Painting Magic", "Canvas", "FANTASY", HitEffect.SLOW, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.ABSORPTION),
    // 0.45: World Tree (William Vangeance), Dice (Baval, David Swallow), Slash (Jack the Ripper), Compass (Letoile Becquerel); appended
    WORLD_TREE("World Tree Magic", "Yggdrasil", "EARTH", HitEffect.SLOW, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.REGENERATION),
    DICE("Dice Magic", "Fortune", "FANTASY", HitEffect.PUSH, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.LUCK),
    SLASH("Slash Magic", "Ripper", "BATTLE", HitEffect.PIERCE, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_BOOST),
    COMPASS("Compass Magic", "Bearing", "SPACE", HitEffect.PULL, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.MOVEMENT_SPEED),
    // 0.47: Kotodama (Word Soul) Magic, Zagred's - God-class, creative only (never rolled; see book.KotodamaWords); appended
    KOTODAMA("Kotodama Magic", "Word Soul", "EMPTY", HitEffect.NULLIFY, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), () -> MobEffects.DAMAGE_RESISTANCE),
    // 0.49: Game Magic (Gifso, Spade Kingdom); appended
    GAME("Game Magic", "Gamemaster", "FANTASY", HitEffect.SLOW, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.LUCK),
    // 0.53: the Black Clover Magic and VFX expansion (29 attributes); appended. Restricted ones are never rolled for a soul.
    DEMON_BEAST("Demon Beast Magic", "Underworld Beast", "DARKNESS", HitEffect.PIERCE, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), () -> MobEffects.DAMAGE_RESISTANCE),
    BODY("Body Magic", "Titan", "BATTLE", HitEffect.PUSH, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_BOOST),
    EYE("Eye Magic", "Eye", "FANTASY", HitEffect.BLIND, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.NIGHT_VISION),
    EYEBALL("Eyeball Magic", "Eyeball", "DARKNESS", HitEffect.BLIND, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), () -> MobEffects.NIGHT_VISION),
    CURSE("Curse Magic", "Curse", "DARKNESS", HitEffect.WITHER, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), () -> MobEffects.DAMAGE_BOOST),
    CURSE_WARDING("Curse-Warding Magic", "Ward", "TIME", HitEffect.WEAKEN, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_RESISTANCE),
    DEMON_FIRE("Demon Fire Magic", "Demon Flame", "DARKNESS", HitEffect.BURN, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), () -> MobEffects.FIRE_RESISTANCE),
    DEMON_ICE("Demon Ice Magic", "Demon Frost", "DARKNESS", HitEffect.FREEZE, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), () -> MobEffects.DAMAGE_RESISTANCE),
    DEMON_LIGHT("Demon Light Magic", "Demon Light", "DARKNESS", HitEffect.PIERCE, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), () -> MobEffects.MOVEMENT_SPEED),
    DEMON_WATER("Demon Water Magic", "Demon Tide", "DARKNESS", HitEffect.PUSH, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), () -> MobEffects.WATER_BREATHING),
    BARRIER("Barrier Magic", "Barrier", "SPACE", HitEffect.PUSH, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), () -> MobEffects.DAMAGE_RESISTANCE),
    KEY("Key Magic", "Key", "SPACE", HitEffect.PULL, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.MOVEMENT_SPEED),
    CHAIN("Chain Magic", "Chain", "BATTLE", HitEffect.SLOW, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_RESISTANCE),
    BUTOH("Butoh Magic", "Dance", "FANTASY", HitEffect.PUSH, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.MOVEMENT_SPEED),
    BRIAR("Briar Magic", "Briar", "EARTH", HitEffect.POISON, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.REGENERATION),
    CHERRY_BLOSSOM("Cherry Blossom Magic", "Sakura", "EARTH", HitEffect.BLIND, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.REGENERATION),
    FUNGUS("Fungus Magic", "Spore", "EARTH", HitEffect.POISON, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.REGENERATION),
    FOOD("Food Magic", "Feast", "FANTASY", HitEffect.DRAIN, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.REGENERATION),
    CRYSTAL("Crystal Magic", "Crystal", "EARTH", HitEffect.PIERCE, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_RESISTANCE),
    CORUNDUM("Corundum Magic", "Corundum", "EARTH", HitEffect.WEAKEN, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_RESISTANCE),
    BRONZE("Bronze Magic", "Bronze", "EARTH", HitEffect.PUSH, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_RESISTANCE),
    COPPER("Copper Magic", "Copper", "EARTH", HitEffect.SHOCK, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_RESISTANCE),
    IRON("Iron Magic", "Iron", "BATTLE", HitEffect.PUSH, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_RESISTANCE),
    BLACK_OIL("Black Oil Magic", "Black Oil", "DARKNESS", HitEffect.SLOW, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), () -> MobEffects.DAMAGE_RESISTANCE),
    GEL("Gel Magic", "Gel", "WATER", HitEffect.SLOW, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_RESISTANCE),
    GLASS("Glass Magic", "Glass", "EARTH", HitEffect.PIERCE, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_RESISTANCE),
    BUBBLE("Bubble Magic", "Bubble", "WATER", HitEffect.LEVITATE, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.SLOW_FALLING),
    ICE_WEDGE("Ice Wedge Magic", "Wedge", "WATER", HitEffect.FREEZE, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_RESISTANCE),
    LEGION("Legion Magic", "Legion", "FANTASY", HitEffect.PUSH, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.ABSORPTION),
    // 0.89: remaining standalone attributes in the Black Clover Magic Attributes category.
    AIR("Air Magic", "Air", "WIND", HitEffect.PUSH, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.MOVEMENT_SPEED),
    HAIR("Hair Magic", "Hair", "BATTLE", HitEffect.SLOW, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_RESISTANCE),
    MEMORY("Memory Magic", "Memory", "FANTASY", HitEffect.BLIND, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.NIGHT_VISION),
    MINERAL("Mineral Magic", "Mineral", "EARTH", HitEffect.PIERCE, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_RESISTANCE),
    MODIFICATION("Modification Magic", "Modify", "FANTASY", HitEffect.WEAKEN, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_RESISTANCE),
    MUCUS("Mucus Magic", "Mucus", "WATER", HitEffect.SLOW, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_RESISTANCE),
    MUD("Mud Magic", "Mud", "EARTH", HitEffect.SLOW, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), () -> MobEffects.DAMAGE_RESISTANCE),
    NAIL("Nail Magic", "Nail", "BATTLE", HitEffect.PIERCE, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_BOOST),
    PERMEATION("Permeation Magic", "Permeate", "SPACE", HitEffect.PIERCE, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), () -> MobEffects.MOVEMENT_SPEED),
    POISON_PLANT("Poison Plant Magic", "Toxic Thorn", "EARTH", HitEffect.POISON, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.REGENERATION),
    RED_OCHRE("Red Ochre Magic", "Ochre", "EARTH", HitEffect.WEAKEN, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_RESISTANCE),
    ROCK("Rock Magic", "Rock", "EARTH", HitEffect.PUSH, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_RESISTANCE),
    SANDSTONE("Sandstone Magic", "Sandstone", "EARTH", HitEffect.PUSH, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), () -> MobEffects.DAMAGE_RESISTANCE),
    SCALE("Scale Magic", "Scale", "BATTLE", HitEffect.PIERCE, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_RESISTANCE),
    SHAKUDO("Shakudo Magic", "Shakudo", "EARTH", HitEffect.PIERCE, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_RESISTANCE),
    SKIN("Skin Magic", "Skin", "BATTLE", HitEffect.WEAKEN, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_RESISTANCE),
    SMOKE("Smoke Magic", "Smoke", "DARKNESS", HitEffect.BLIND, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), () -> MobEffects.INVISIBILITY),
    SNOW("Snow Magic", "Snow", "WATER", HitEffect.FREEZE, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_RESISTANCE),
    SONG("Song Magic", "Song", "FANTASY", HitEffect.WEAKEN, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.REGENERATION),
    SOUL_CORPSE("Soul Corpse Magic", "Soul Corpse", "DARKNESS", HitEffect.WITHER, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), () -> MobEffects.DAMAGE_RESISTANCE),
    SOUL("Soul Magic", "Soul", "DARKNESS", HitEffect.DRAIN, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), () -> MobEffects.REGENERATION),
    SOUND("Sound Magic", "Sound", "WIND", HitEffect.SHOCK, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.MOVEMENT_SPEED),
    SPIKE("Spike Magic", "Spike", "EARTH", HitEffect.PIERCE, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_RESISTANCE),
    SWITCHING("Switching Magic", "Switch", "SPACE", HitEffect.PULL, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.dark(), () -> MobEffects.MOVEMENT_SPEED),
    TONGUE("Tongue Magic", "Tongue", "BATTLE", HitEffect.PULL, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_BOOST),
    TREE("Tree Magic", "Tree", "EARTH", HitEffect.SLOW, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.REGENERATION),
    STONE("Stone Magic", "Stone", "EARTH", HitEffect.SLOW, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.DAMAGE_RESISTANCE),
    VINE("Vine Magic", "Vine", "EARTH", HitEffect.SLOW, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.REGENERATION),
    VORTEX("Vortex Magic", "Vortex", "WIND", HitEffect.PULL, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.MOVEMENT_SPEED),
    WING("Wing Magic", "Wing", "WIND", HitEffect.LEVITATE, () -> com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), () -> MobEffects.SLOW_FALLING);

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

    /**
     * 0.53: never rolled for a soul (devil, command or admin only), like Anti-Magic and Kotodama.
     * 1.27.0: by NAME ({@link MagicKeys#RESTRICTED}) instead of a switch over the constants, so appending constants can never shift it.
     * Answers exactly as before for every existing constant.
     */
    public boolean restricted() { return profile().restricted(); }

    /** The particle of this magic; GLOW if the supplier is missing or throws (never null). An override registered in MagicProfiles wins. */
    public SimpleParticleType particle() { return profile().particle(); }
    /** The guard effect of this magic; Resistance if the supplier is missing or throws (never null). An override registered in MagicProfiles wins. */
    public Holder<MobEffect> guardEffect() { return profile().guardEffect(); }

    /** The enum's own particle supplier, null if it is missing or throws. Used by the profile; prefer {@link #particle()}. */
    public SimpleParticleType rawParticle() {
        try { return particle == null ? null : particle.get(); } catch (RuntimeException | LinkageError e) { return null; }
    }

    /** The enum's own guard-effect supplier, null if it is missing or throws. Used by the profile; prefer {@link #guardEffect()}. */
    public Holder<MobEffect> rawGuardEffect() {
        try { return guardEffect == null ? null : guardEffect.get(); } catch (RuntimeException | LinkageError e) { return null; }
    }

    /** 1.27.0: the complete, never-null profile of this magic (core.magic.profile): everything a magic needs, with safe defaults. */
    public MagicProfile profile() { return MagicProfiles.of(this); }

    // 1.27.0: accessors that go through the profile (same values as the fields unless an override is registered)
    public String displayName() { return profile().displayName(); }
    public String word() { return profile().word(); }
    public String soulType() { return profile().soulType(); }
    public HitEffect hitEffect() { return profile().hit(); }

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

    /** 1.27.0: like {@link #byName} but null for an unknown name (byName turns it into Flame, which hides a typo or a merged id). */
    public static MagicType lookup(String name) {
        if (name == null) return null;
        try { return valueOf(MagicKeys.enumKey(name)); } catch (IllegalArgumentException e) { return null; }
    }

    /** What a spell does to whatever it hits. */
    public enum HitEffect { BURN, FREEZE, SHOCK, SLOW, POISON, WITHER, BLIND, LEVITATE, PULL, PUSH, WEAKEN, PIERCE, DRAIN, NULLIFY }
}
