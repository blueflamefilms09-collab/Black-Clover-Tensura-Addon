package com.newuniverse.nusmp.meteor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Which abilities drop a real meteor and which meteor-like names were left alone (docs/meteor_fall.md). Pure table: {@link #spec} is what a caller starts from, so the look and the size
 * of every wired ability are in one place, and the tests check that every entry has a caller in the source and every left-alone entry has a reason.
 * Ids are {@code <system>.<ability>}; a new ability is added at the END of {@link #WIRED}.
 */
public final class MeteorWiring {
    private MeteorWiring() {}

    /** One wired ability: its look, the generic radius, the model scale, the usual fall and the source file (under src/main/java/com/newuniverse/nusmp) that calls {@code MeteorWiring.spec(id)}. */
    public record Entry(String id, MeteorStyle style, double radius, float size, int fall, String file) {}

    public static final Map<String, Entry> WIRED = new LinkedHashMap<>();

    private static void add(String id, MeteorStyle style, double radius, float size, int fall, String file) { WIRED.put(id, new Entry(id, style, radius, size, fall, file)); }

    static {
        add("sword.meteor", MeteorStyle.STAR, 1.8, 0.8f, 14, "heroweapon/weapon/sword/SwordOps.java");
        add("gambler.meteor_of_bones", MeteorStyle.DARK, 5.5, 1.5f, 30, "dungeon/nexus/relic/GamblerPage.java");
        add("star.meteor_swarm", MeteorStyle.STAR, 2.6, 1.0f, 18, "book/pages15/Batch1.java");
        add("visionary.meteor", MeteorStyle.STAR, 6.0, 2.4f, 36, "skill/TheVisionarySkill.java");
        add("merlin.starfall", MeteorStyle.STAR, 3.0, 1.2f, 20, "arc/sds/MerlinMoves.java");
        add("demon_ice.black_comet", MeteorStyle.ICE, 5.0, 1.8f, 20, "book/pages15/Batch2.java");
        add("bow.meteor_bow", MeteorStyle.FIRE, 4.0, 1.4f, 24, "heroweapon/weapon/bow/BowRain.java");
    }

    /** What was looked at and not wired, with the reason (the doc prints this table). Values start with the file the name is in. */
    public static final Map<String, String> LEFT_ALONE = new LinkedHashMap<>();

    static {
        LEFT_ALONE.put("saiyan.meteor_combination", "saiyan/SaiyanTechniques.java: a rush of melee strikes on one foe, nothing falls");
        LEFT_ALONE.put("saiyan.meteor_strike", "saiyan/SaiyanTechniques4.java: a dash into one foe (a punch), nothing falls");
        LEFT_ALONE.put("goku.meteor_combo", "arc/goku/GokuMoves.java: the boss's version of the Meteor Combination, melee");
        LEFT_ALONE.put("antimagic.black_meteorite", "book/CanonSpells.java + item/DemonSlayerSwordItem.java: a horizontal dash and slash (Demon-Slayer sword art), not a fall; its own look stays");
        LEFT_ALONE.put("greed.piercing_meteor", "greed/GreedForms.java: a flat beam from the Black Bow");
        LEFT_ALONE.put("mirror.mirrors_meteor_dragon", "book/pages15/Batch6.java: a line (a ray) with a splash at its end, nothing falls");
        LEFT_ALONE.put("mirror.mirrors_meteorite", "book/MirrorWorks.java: 6 to 14 small mirror blades from 9 blocks up (a shard rain, each already a falling prop); owner decision whether the storm should become meteors");
        LEFT_ALONE.put("spear.meteor_spear", "heroweapon/weapon/spear/SpearStrikes.java: a lance thrown along a line; owner decision (the name says falling star, the move is horizontal)");
        LEFT_ALONE.put("diane.rising_meteor", "arc/sds/DianeMoves.java: pillars of stone RISE from the ground, the opposite of a fall");
        LEFT_ALONE.put("dream.falling_stars", "book/DreamWorld.java: six dream sparks over the dreamers, an illusion with a seven point hit each");
        LEFT_ALONE.put("ultimate.star_stellar_rain_of_ruin", "ultimate/UltimateDefs1.java: one shared Ultimate engine (wind-up cast, burst, echoes) for 40 ultimates; changing it for two defs is a follow-up");
        LEFT_ALONE.put("ultimate.iron_meteor_anvil", "ultimate/UltimateDefs3.java: same Ultimate engine as above");
        LEFT_ALONE.put("tier.falling_star", "arc/tier/TierRosterD.java: runs on the shared marked-strike engine of every tier boss (EarlyMoves.batchStrike); follow-up");
        LEFT_ALONE.put("relics.meteor_arrow", "relics/wave/Wave71.java, Wave102.java: bow acts (arrows on a line); bow weapons belong to the bow worker");
        LEFT_ALONE.put("relics.starfall_arrows", "relics/wave/Wave71.java: an arrow storm (Wave71Acts.storm), bow act");
        LEFT_ALONE.put("elementbook.star_comet", "book/ElementBooks.java: Comet is a bolt shot along the aim, Starfall a ground field; neither is a falling body");
        LEFT_ALONE.put("sword.skyfall_cleave", "heroweapon/weapon/sword: a leap and a slam of the wielder, not a meteor");
        LEFT_ALONE.put("djinn.bifron_skyfall", "relics/djinn/Djinn.java: pillars of storm");
        LEFT_ALONE.put("lucius.judgment_of_light", "entity/zogratis/LuciusBoss.java: columns of light, not rocks");
        LEFT_ALONE.put("shield.shooting_star", "heroweapon/weapon/shield: a comet of glass flown along a line; shield code is not touched");
        LEFT_ALONE.put("text.meteor_names", "SolomonRules / ChunibyoNames / DreamWorld keywords / lang text: a word, no ability");
    }

    /** True when the ability id drops a real meteor. */
    public static boolean wired(String id) { return WIRED.containsKey(id); }

    /** The starting spec of a wired ability (radius, size, look, usual fall); the caller adds its own landing and, if it has one, the timing. Unknown ids are a bug. */
    public static MeteorSpec spec(String id) {
        Entry e = WIRED.get(id);
        if (e == null) throw new IllegalArgumentException("no meteor is wired for " + id);
        return MeteorSpec.of(e.style()).radius(e.radius()).size(e.size()).fall(e.fall());
    }
}
