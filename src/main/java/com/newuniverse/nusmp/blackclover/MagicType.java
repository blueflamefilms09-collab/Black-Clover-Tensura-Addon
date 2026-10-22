package com.newuniverse.nusmp.blackclover;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import com.newuniverse.nusmp.core.magic.profile.MagicKeys;
import com.newuniverse.nusmp.core.magic.profile.MagicProfile;
import com.newuniverse.nusmp.core.magic.profile.MagicProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxTone;
import com.newuniverse.nusmp.particle.fx.ThemeKey;

/**
 * Black Clover magic attributes, grouped by TR Nightmare soul type.
 * word = the prefix used for spell names, e.g. "Flame" -> "Flame Bullet".
 */
public enum MagicType {
    // FLAME soul
    FLAME("Flame Magic", "Flame", "FLAME", HitEffect.BURN, () -> Fx.options(ThemeKey.of("magic.FLAME"), FxTone.LIGHT), () -> MobEffects.FIRE_RESISTANCE),
    EXPLOSION("Explosion Magic", "Blast", "FLAME", HitEffect.PUSH, () -> Fx.options(ThemeKey.of("magic.EXPLOSION"), FxTone.LIGHT), () -> MobEffects.DAMAGE_BOOST),
    MAGMA("Magma Magic", "Magma", "FLAME", HitEffect.BURN, () -> Fx.options(ThemeKey.of("magic.MAGMA"), FxTone.LIGHT), () -> MobEffects.FIRE_RESISTANCE),
    // WATER soul
    WATER("Water Magic", "Sea Dragon", "WATER", HitEffect.PUSH, () -> Fx.options(ThemeKey.of("magic.WATER"), FxTone.LIGHT), () -> MobEffects.WATER_BREATHING),
    ICE("Ice Magic", "Frost", "WATER", HitEffect.FREEZE, () -> Fx.options(ThemeKey.of("magic.ICE"), FxTone.LIGHT), () -> MobEffects.DAMAGE_RESISTANCE),
    MERCURY("Mercury Magic", "Silver", "WATER", HitEffect.SLOW, () -> Fx.options(ThemeKey.of("magic.MERCURY"), FxTone.DARK), () -> MobEffects.DAMAGE_RESISTANCE),
    MIST("Mist Magic", "Mist", "WATER", HitEffect.BLIND, () -> Fx.options(ThemeKey.of("magic.MIST"), FxTone.LIGHT), () -> MobEffects.INVISIBILITY),
    // WIND soul
    WIND("Wind Magic", "Gale", "WIND", HitEffect.LEVITATE, () -> Fx.options(ThemeKey.of("magic.WIND"), FxTone.LIGHT), () -> MobEffects.MOVEMENT_SPEED),
    STAR("Star Magic", "Star", "WIND", HitEffect.PIERCE, () -> Fx.options(ThemeKey.of("magic.STAR"), FxTone.LIGHT), () -> MobEffects.MOVEMENT_SPEED),
    STORM("Storm Magic", "Tempest", "WIND", HitEffect.SHOCK, () -> Fx.options(ThemeKey.of("magic.STORM"), FxTone.LIGHT), () -> MobEffects.SLOW_FALLING),
    // EARTH soul
    EARTH("Earth Magic", "Stone", "EARTH", HitEffect.SLOW, () -> Fx.options(ThemeKey.of("magic.EARTH"), FxTone.LIGHT), () -> MobEffects.DAMAGE_RESISTANCE),
    PLANT("Plant Magic", "Thorn", "EARTH", HitEffect.POISON, () -> Fx.options(ThemeKey.of("magic.PLANT"), FxTone.LIGHT), () -> MobEffects.REGENERATION),
    SAND("Sand Magic", "Sand", "EARTH", HitEffect.BLIND, () -> Fx.options(ThemeKey.of("magic.SAND"), FxTone.DARK), () -> MobEffects.DAMAGE_RESISTANCE),
    // LIGHT soul
    LIGHT("Light Magic", "Light", "LIGHT", HitEffect.PIERCE, () -> Fx.options(ThemeKey.of("magic.LIGHT"), FxTone.LIGHT), () -> MobEffects.MOVEMENT_SPEED),
    LIGHTNING("Lightning Magic", "Thunder", "LIGHT", HitEffect.SHOCK, () -> Fx.options(ThemeKey.of("magic.LIGHTNING"), FxTone.LIGHT), () -> MobEffects.MOVEMENT_SPEED),
    SWORD("Sword Magic", "Blade", "LIGHT", HitEffect.PIERCE, () -> Fx.options(ThemeKey.of("magic.SWORD"), FxTone.LIGHT), () -> MobEffects.DAMAGE_BOOST),
    // DARKNESS soul
    DARK("Dark Magic", "Dark", "DARKNESS", HitEffect.WITHER, () -> Fx.options(ThemeKey.of("magic.DARK"), FxTone.DARK), () -> MobEffects.DAMAGE_BOOST),
    SHADOW("Shadow Magic", "Shadow", "DARKNESS", HitEffect.BLIND, () -> Fx.options(ThemeKey.of("magic.SHADOW"), FxTone.DARK), () -> MobEffects.INVISIBILITY),
    POISON("Poison Magic", "Venom", "DARKNESS", HitEffect.POISON, () -> Fx.options(ThemeKey.of("magic.POISON"), FxTone.DARK), () -> MobEffects.REGENERATION),
    // SPACE soul
    SPATIAL("Spatial Magic", "Rift", "SPACE", HitEffect.PULL, () -> Fx.options(ThemeKey.of("magic.SPATIAL"), FxTone.DARK), () -> MobEffects.MOVEMENT_SPEED),
    MIRROR("Mirror Magic", "Mirror", "SPACE", HitEffect.PIERCE, () -> Fx.options(ThemeKey.of("magic.MIRROR"), FxTone.LIGHT), () -> MobEffects.INVISIBILITY),
    GRAVITY("Gravity Magic", "Gravity", "SPACE", HitEffect.SLOW, () -> Fx.options(ThemeKey.of("magic.GRAVITY"), FxTone.DARK), () -> MobEffects.DAMAGE_RESISTANCE),
    // TIME soul
    TIME("Time Magic", "Chrono", "TIME", HitEffect.SLOW, () -> Fx.options(ThemeKey.of("magic.TIME"), FxTone.LIGHT), () -> MobEffects.DIG_SPEED),
    SEALING("Sealing Magic", "Seal", "TIME", HitEffect.WEAKEN, () -> Fx.options(ThemeKey.of("magic.SEALING"), FxTone.LIGHT), () -> MobEffects.DAMAGE_RESISTANCE),
    // BATTLE soul
    REINFORCEMENT("Reinforcement Magic", "Iron Fist", "BATTLE", HitEffect.PUSH, () -> Fx.options(ThemeKey.of("magic.REINFORCEMENT"), FxTone.LIGHT), () -> MobEffects.DAMAGE_BOOST),
    BEAST("Beast Magic", "Beast", "BATTLE", HitEffect.PIERCE, () -> Fx.options(ThemeKey.of("magic.BEAST"), FxTone.LIGHT), () -> MobEffects.MOVEMENT_SPEED),
    BONE("Bone Magic", "Bone", "BATTLE", HitEffect.PIERCE, () -> Fx.options(ThemeKey.of("magic.BONE"), FxTone.DARK), () -> MobEffects.DAMAGE_RESISTANCE),
    BLOOD("Blood Magic", "Crimson", "BATTLE", HitEffect.DRAIN, () -> Fx.options(ThemeKey.of("magic.BLOOD"), FxTone.DARK), () -> MobEffects.REGENERATION),
    // FANTASY soul
    CREATION("Creation Magic", "Creation", "FANTASY", HitEffect.PUSH, () -> Fx.options(ThemeKey.of("magic.CREATION"), FxTone.LIGHT), () -> MobEffects.ABSORPTION),
    COPY("Copy Magic", "Copy", "FANTASY", HitEffect.PIERCE, () -> Fx.options(ThemeKey.of("magic.COPY"), FxTone.LIGHT), () -> MobEffects.DAMAGE_BOOST),
    ILLUSION("Illusion Magic", "Illusion", "FANTASY", HitEffect.BLIND, () -> Fx.options(ThemeKey.of("magic.ILLUSION"), FxTone.DARK), () -> MobEffects.INVISIBILITY),
    DREAM("Dream Magic", "Dream", "FANTASY", HitEffect.SLOW, () -> Fx.options(ThemeKey.of("magic.DREAM"), FxTone.LIGHT), () -> MobEffects.REGENERATION),
    // Special: only an EMPTY soul with a five-leaf grimoire (the devil Liebe)
    ANTI_MAGIC("Anti-Magic", "Black", "EMPTY", HitEffect.NULLIFY, () -> Fx.options(ThemeKey.of("magic.ANTI_MAGIC"), FxTone.DARK), () -> MobEffects.DAMAGE_BOOST),
    // Added later: keep at the END (item model variants use the ordinal)
    STEEL("Steel Magic", "Steel", "BATTLE", HitEffect.PIERCE, () -> Fx.options(ThemeKey.of("magic.STEEL"), FxTone.LIGHT), () -> MobEffects.DAMAGE_RESISTANCE),
    THREAD("Thread Magic", "Thread", "FANTASY", HitEffect.SLOW, () -> Fx.options(ThemeKey.of("magic.THREAD"), FxTone.LIGHT), () -> MobEffects.DAMAGE_RESISTANCE),
    // 0.34: attributes from the wiki (Grey, Zora, Charmy, Henry); appended so every ordinal above stays put
    TRANSMUTATION("Transmutation Magic", "Alchemy", "EARTH", HitEffect.WEAKEN, () -> Fx.options(ThemeKey.of("magic.TRANSMUTATION"), FxTone.LIGHT), () -> MobEffects.DAMAGE_RESISTANCE),
    ASH("Ash Magic", "Ash", "FLAME", HitEffect.WITHER, () -> Fx.options(ThemeKey.of("magic.ASH"), FxTone.DARK), () -> MobEffects.DAMAGE_RESISTANCE),
    COTTON("Cotton Magic", "Cotton", "FANTASY", HitEffect.SLOW, () -> Fx.options(ThemeKey.of("magic.COTTON"), FxTone.DARK), () -> MobEffects.REGENERATION),
    RECOMBINATION("Recombination Magic", "Bull", "FANTASY", HitEffect.PUSH, () -> Fx.options(ThemeKey.of("magic.RECOMBINATION"), FxTone.LIGHT), () -> MobEffects.ABSORPTION),
    // 0.40: Painting Magic (Rill Boismortier, Lira), appended
    PAINTING("Painting Magic", "Canvas", "FANTASY", HitEffect.SLOW, () -> Fx.options(ThemeKey.of("magic.PAINTING"), FxTone.LIGHT), () -> MobEffects.ABSORPTION),
    // 0.45: World Tree (William Vangeance), Dice (Baval, David Swallow), Slash (Jack the Ripper), Compass (Letoile Becquerel); appended
    WORLD_TREE("World Tree Magic", "Yggdrasil", "EARTH", HitEffect.SLOW, () -> Fx.options(ThemeKey.of("magic.WORLD_TREE"), FxTone.LIGHT), () -> MobEffects.REGENERATION),
    DICE("Dice Magic", "Fortune", "FANTASY", HitEffect.PUSH, () -> Fx.options(ThemeKey.of("magic.DICE"), FxTone.LIGHT), () -> MobEffects.LUCK),
    SLASH("Slash Magic", "Ripper", "BATTLE", HitEffect.PIERCE, () -> Fx.options(ThemeKey.of("magic.SLASH"), FxTone.LIGHT), () -> MobEffects.DAMAGE_BOOST),
    COMPASS("Compass Magic", "Bearing", "SPACE", HitEffect.PULL, () -> Fx.options(ThemeKey.of("magic.COMPASS"), FxTone.LIGHT), () -> MobEffects.MOVEMENT_SPEED),
    // 0.47: Kotodama (Word Soul) Magic, Zagred's - God-class, creative only (never rolled; see book.KotodamaWords); appended
    KOTODAMA("Kotodama Magic", "Word Soul", "EMPTY", HitEffect.NULLIFY, () -> Fx.options(ThemeKey.of("magic.KOTODAMA"), FxTone.DARK), () -> MobEffects.DAMAGE_RESISTANCE),
    // 0.49: Game Magic (Gifso, Spade Kingdom); appended
    GAME("Game Magic", "Gamemaster", "FANTASY", HitEffect.SLOW, () -> Fx.options(ThemeKey.of("magic.GAME"), FxTone.LIGHT), () -> MobEffects.LUCK),
    // 0.53: the Black Clover Magic and VFX expansion (29 attributes); appended. Restricted ones are never rolled for a soul.
    DEMON_BEAST("Demon Beast Magic", "Underworld Beast", "DARKNESS", HitEffect.PIERCE, () -> Fx.options(ThemeKey.of("magic.DEMON_BEAST"), FxTone.DARK), () -> MobEffects.DAMAGE_RESISTANCE),
    BODY("Body Magic", "Titan", "BATTLE", HitEffect.PUSH, () -> Fx.options(ThemeKey.of("magic.BODY"), FxTone.LIGHT), () -> MobEffects.DAMAGE_BOOST),
    EYE("Eye Magic", "Eye", "FANTASY", HitEffect.BLIND, () -> Fx.options(ThemeKey.of("magic.EYE"), FxTone.LIGHT), () -> MobEffects.NIGHT_VISION),
    EYEBALL("Eyeball Magic", "Eyeball", "DARKNESS", HitEffect.BLIND, () -> Fx.options(ThemeKey.of("magic.EYEBALL"), FxTone.DARK), () -> MobEffects.NIGHT_VISION),
    CURSE("Curse Magic", "Curse", "DARKNESS", HitEffect.WITHER, () -> Fx.options(ThemeKey.of("magic.CURSE"), FxTone.DARK), () -> MobEffects.DAMAGE_BOOST),
    CURSE_WARDING("Curse-Warding Magic", "Ward", "TIME", HitEffect.WEAKEN, () -> Fx.options(ThemeKey.of("magic.CURSE_WARDING"), FxTone.LIGHT), () -> MobEffects.DAMAGE_RESISTANCE),
    DEMON_FIRE("Demon Fire Magic", "Demon Flame", "DARKNESS", HitEffect.BURN, () -> Fx.options(ThemeKey.of("magic.DEMON_FIRE"), FxTone.DARK), () -> MobEffects.FIRE_RESISTANCE),
    DEMON_ICE("Demon Ice Magic", "Demon Frost", "DARKNESS", HitEffect.FREEZE, () -> Fx.options(ThemeKey.of("magic.DEMON_ICE"), FxTone.DARK), () -> MobEffects.DAMAGE_RESISTANCE),
    DEMON_LIGHT("Demon Light Magic", "Demon Light", "DARKNESS", HitEffect.PIERCE, () -> Fx.options(ThemeKey.of("magic.DEMON_LIGHT"), FxTone.DARK), () -> MobEffects.MOVEMENT_SPEED),
    DEMON_WATER("Demon Water Magic", "Demon Tide", "DARKNESS", HitEffect.PUSH, () -> Fx.options(ThemeKey.of("magic.DEMON_WATER"), FxTone.DARK), () -> MobEffects.WATER_BREATHING),
    BARRIER("Barrier Magic", "Barrier", "SPACE", HitEffect.PUSH, () -> Fx.options(ThemeKey.of("magic.BARRIER"), FxTone.DARK), () -> MobEffects.DAMAGE_RESISTANCE),
    KEY("Key Magic", "Key", "SPACE", HitEffect.PULL, () -> Fx.options(ThemeKey.of("magic.KEY"), FxTone.LIGHT), () -> MobEffects.MOVEMENT_SPEED),
    CHAIN("Chain Magic", "Chain", "BATTLE", HitEffect.SLOW, () -> Fx.options(ThemeKey.of("magic.CHAIN"), FxTone.LIGHT), () -> MobEffects.DAMAGE_RESISTANCE),
    BUTOH("Butoh Magic", "Dance", "FANTASY", HitEffect.PUSH, () -> Fx.options(ThemeKey.of("magic.BUTOH"), FxTone.LIGHT), () -> MobEffects.MOVEMENT_SPEED),
    BRIAR("Briar Magic", "Briar", "EARTH", HitEffect.POISON, () -> Fx.options(ThemeKey.of("magic.BRIAR"), FxTone.LIGHT), () -> MobEffects.REGENERATION),
    CHERRY_BLOSSOM("Cherry Blossom Magic", "Sakura", "EARTH", HitEffect.BLIND, () -> Fx.options(ThemeKey.of("magic.CHERRY_BLOSSOM"), FxTone.LIGHT), () -> MobEffects.REGENERATION),
    FUNGUS("Fungus Magic", "Spore", "EARTH", HitEffect.POISON, () -> Fx.options(ThemeKey.of("magic.FUNGUS"), FxTone.LIGHT), () -> MobEffects.REGENERATION),
    FOOD("Food Magic", "Feast", "FANTASY", HitEffect.DRAIN, () -> Fx.options(ThemeKey.of("magic.FOOD"), FxTone.LIGHT), () -> MobEffects.REGENERATION),
    CRYSTAL("Crystal Magic", "Crystal", "EARTH", HitEffect.PIERCE, () -> Fx.options(ThemeKey.of("magic.CRYSTAL"), FxTone.LIGHT), () -> MobEffects.DAMAGE_RESISTANCE),
    CORUNDUM("Corundum Magic", "Corundum", "EARTH", HitEffect.WEAKEN, () -> Fx.options(ThemeKey.of("magic.CORUNDUM"), FxTone.LIGHT), () -> MobEffects.DAMAGE_RESISTANCE),
    BRONZE("Bronze Magic", "Bronze", "EARTH", HitEffect.PUSH, () -> Fx.options(ThemeKey.of("magic.BRONZE"), FxTone.LIGHT), () -> MobEffects.DAMAGE_RESISTANCE),
    COPPER("Copper Magic", "Copper", "EARTH", HitEffect.SHOCK, () -> Fx.options(ThemeKey.of("magic.COPPER"), FxTone.LIGHT), () -> MobEffects.DAMAGE_RESISTANCE),
    IRON("Iron Magic", "Iron", "BATTLE", HitEffect.PUSH, () -> Fx.options(ThemeKey.of("magic.IRON"), FxTone.LIGHT), () -> MobEffects.DAMAGE_RESISTANCE),
    BLACK_OIL("Black Oil Magic", "Black Oil", "DARKNESS", HitEffect.SLOW, () -> Fx.options(ThemeKey.of("magic.BLACK_OIL"), FxTone.DARK), () -> MobEffects.DAMAGE_RESISTANCE),
    GEL("Gel Magic", "Gel", "WATER", HitEffect.SLOW, () -> Fx.options(ThemeKey.of("magic.GEL"), FxTone.LIGHT), () -> MobEffects.DAMAGE_RESISTANCE),
    GLASS("Glass Magic", "Glass", "EARTH", HitEffect.PIERCE, () -> Fx.options(ThemeKey.of("magic.GLASS"), FxTone.LIGHT), () -> MobEffects.DAMAGE_RESISTANCE),
    BUBBLE("Bubble Magic", "Bubble", "WATER", HitEffect.LEVITATE, () -> Fx.options(ThemeKey.of("magic.BUBBLE"), FxTone.LIGHT), () -> MobEffects.SLOW_FALLING),
    ICE_WEDGE("Ice Wedge Magic", "Wedge", "WATER", HitEffect.FREEZE, () -> Fx.options(ThemeKey.of("magic.ICE_WEDGE"), FxTone.LIGHT), () -> MobEffects.DAMAGE_RESISTANCE),
    LEGION("Legion Magic", "Legion", "FANTASY", HitEffect.PUSH, () -> Fx.options(ThemeKey.of("magic.LEGION"), FxTone.LIGHT), () -> MobEffects.ABSORPTION),
    // 0.89: remaining standalone attributes in the Black Clover Magic Attributes category.
    AIR("Air Magic", "Air", "WIND", HitEffect.PUSH, () -> Fx.options(ThemeKey.of("magic.AIR"), FxTone.LIGHT), () -> MobEffects.MOVEMENT_SPEED),
    HAIR("Hair Magic", "Hair", "BATTLE", HitEffect.SLOW, () -> Fx.options(ThemeKey.of("magic.HAIR"), FxTone.LIGHT), () -> MobEffects.DAMAGE_RESISTANCE),
    MEMORY("Memory Magic", "Memory", "FANTASY", HitEffect.BLIND, () -> Fx.options(ThemeKey.of("magic.MEMORY"), FxTone.LIGHT), () -> MobEffects.NIGHT_VISION),
    MINERAL("Mineral Magic", "Mineral", "EARTH", HitEffect.PIERCE, () -> Fx.options(ThemeKey.of("magic.MINERAL"), FxTone.LIGHT), () -> MobEffects.DAMAGE_RESISTANCE),
    MODIFICATION("Modification Magic", "Modify", "FANTASY", HitEffect.WEAKEN, () -> Fx.options(ThemeKey.of("magic.MODIFICATION"), FxTone.LIGHT), () -> MobEffects.DAMAGE_RESISTANCE),
    MUCUS("Mucus Magic", "Mucus", "WATER", HitEffect.SLOW, () -> Fx.options(ThemeKey.of("magic.MUCUS"), FxTone.LIGHT), () -> MobEffects.DAMAGE_RESISTANCE),
    MUD("Mud Magic", "Mud", "EARTH", HitEffect.SLOW, () -> Fx.options(ThemeKey.of("magic.MUD"), FxTone.DARK), () -> MobEffects.DAMAGE_RESISTANCE),
    NAIL("Nail Magic", "Nail", "BATTLE", HitEffect.PIERCE, () -> Fx.options(ThemeKey.of("magic.NAIL"), FxTone.LIGHT), () -> MobEffects.DAMAGE_BOOST),
    PERMEATION("Permeation Magic", "Permeate", "SPACE", HitEffect.PIERCE, () -> Fx.options(ThemeKey.of("magic.PERMEATION"), FxTone.DARK), () -> MobEffects.MOVEMENT_SPEED),
    POISON_PLANT("Poison Plant Magic", "Toxic Thorn", "EARTH", HitEffect.POISON, () -> Fx.options(ThemeKey.of("magic.POISON_PLANT"), FxTone.LIGHT), () -> MobEffects.REGENERATION),
    RED_OCHRE("Red Ochre Magic", "Ochre", "EARTH", HitEffect.WEAKEN, () -> Fx.options(ThemeKey.of("magic.RED_OCHRE"), FxTone.LIGHT), () -> MobEffects.DAMAGE_RESISTANCE),
    ROCK("Rock Magic", "Rock", "EARTH", HitEffect.PUSH, () -> Fx.options(ThemeKey.of("magic.ROCK"), FxTone.LIGHT), () -> MobEffects.DAMAGE_RESISTANCE),
    SANDSTONE("Sandstone Magic", "Sandstone", "EARTH", HitEffect.PUSH, () -> Fx.options(ThemeKey.of("magic.SANDSTONE"), FxTone.DARK), () -> MobEffects.DAMAGE_RESISTANCE),
    SCALE("Scale Magic", "Scale", "BATTLE", HitEffect.PIERCE, () -> Fx.options(ThemeKey.of("magic.SCALE"), FxTone.LIGHT), () -> MobEffects.DAMAGE_RESISTANCE),
    SHAKUDO("Shakudo Magic", "Shakudo", "EARTH", HitEffect.PIERCE, () -> Fx.options(ThemeKey.of("magic.SHAKUDO"), FxTone.LIGHT), () -> MobEffects.DAMAGE_RESISTANCE),
    SKIN("Skin Magic", "Skin", "BATTLE", HitEffect.WEAKEN, () -> Fx.options(ThemeKey.of("magic.SKIN"), FxTone.LIGHT), () -> MobEffects.DAMAGE_RESISTANCE),
    SMOKE("Smoke Magic", "Smoke", "DARKNESS", HitEffect.BLIND, () -> Fx.options(ThemeKey.of("magic.SMOKE"), FxTone.DARK), () -> MobEffects.INVISIBILITY),
    SNOW("Snow Magic", "Snow", "WATER", HitEffect.FREEZE, () -> Fx.options(ThemeKey.of("magic.SNOW"), FxTone.LIGHT), () -> MobEffects.DAMAGE_RESISTANCE),
    SONG("Song Magic", "Song", "FANTASY", HitEffect.WEAKEN, () -> Fx.options(ThemeKey.of("magic.SONG"), FxTone.LIGHT), () -> MobEffects.REGENERATION),
    SOUL_CORPSE("Soul Corpse Magic", "Soul Corpse", "DARKNESS", HitEffect.WITHER, () -> Fx.options(ThemeKey.of("magic.SOUL_CORPSE"), FxTone.DARK), () -> MobEffects.DAMAGE_RESISTANCE),
    SOUL("Soul Magic", "Soul", "DARKNESS", HitEffect.DRAIN, () -> Fx.options(ThemeKey.of("magic.SOUL"), FxTone.DARK), () -> MobEffects.REGENERATION),
    SOUND("Sound Magic", "Sound", "WIND", HitEffect.SHOCK, () -> Fx.options(ThemeKey.of("magic.SOUND"), FxTone.LIGHT), () -> MobEffects.MOVEMENT_SPEED),
    SPIKE("Spike Magic", "Spike", "EARTH", HitEffect.PIERCE, () -> Fx.options(ThemeKey.of("magic.SPIKE"), FxTone.LIGHT), () -> MobEffects.DAMAGE_RESISTANCE),
    SWITCHING("Switching Magic", "Switch", "SPACE", HitEffect.PULL, () -> Fx.options(ThemeKey.of("magic.SWITCHING"), FxTone.DARK), () -> MobEffects.MOVEMENT_SPEED),
    TONGUE("Tongue Magic", "Tongue", "BATTLE", HitEffect.PULL, () -> Fx.options(ThemeKey.of("magic.TONGUE"), FxTone.LIGHT), () -> MobEffects.DAMAGE_BOOST),
    TREE("Tree Magic", "Tree", "EARTH", HitEffect.SLOW, () -> Fx.options(ThemeKey.of("magic.TREE"), FxTone.LIGHT), () -> MobEffects.REGENERATION),
    STONE("Stone Magic", "Stone", "EARTH", HitEffect.SLOW, () -> Fx.options(ThemeKey.of("magic.STONE"), FxTone.LIGHT), () -> MobEffects.DAMAGE_RESISTANCE),
    VINE("Vine Magic", "Vine", "EARTH", HitEffect.SLOW, () -> Fx.options(ThemeKey.of("magic.VINE"), FxTone.LIGHT), () -> MobEffects.REGENERATION),
    VORTEX("Vortex Magic", "Vortex", "WIND", HitEffect.PULL, () -> Fx.options(ThemeKey.of("magic.VORTEX"), FxTone.LIGHT), () -> MobEffects.MOVEMENT_SPEED),
    WING("Wing Magic", "Wing", "WIND", HitEffect.LEVITATE, () -> Fx.options(ThemeKey.of("magic.WING"), FxTone.LIGHT), () -> MobEffects.SLOW_FALLING);

    public final String displayName;
    public final String word;
    public final String soulType;
    public final HitEffect hit;
    private final Supplier<ParticleOptions> particle;
    private final Supplier<Holder<MobEffect>> guardEffect;

    MagicType(String displayName, String word, String soulType, HitEffect hit,
              Supplier<ParticleOptions> particle, Supplier<Holder<MobEffect>> guardEffect) {
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
    public ParticleOptions particle() { return profile().particle(); }
    /** The guard effect of this magic; Resistance if the supplier is missing or throws (never null). An override registered in MagicProfiles wins. */
    public Holder<MobEffect> guardEffect() { return profile().guardEffect(); }

    /** The enum's own particle supplier, null if it is missing or throws. Used by the profile; prefer {@link #particle()}. */
    public ParticleOptions rawParticle() {
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
            if (t != ANTI_MAGIC && t != KOTODAMA && t.soulType.equals(soul) && !com.newuniverse.nusmp.fairytail.storm.StormGate.hidden(t)) list.add(t);   // Storm Magic is a Fairy Tail staff magic (config storm.staffOnly)
        }
        if (list.isEmpty()) {
            for (MagicType t : values()) if (!t.restricted() && !com.newuniverse.nusmp.fairytail.storm.StormGate.hidden(t)) list.add(t);
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
