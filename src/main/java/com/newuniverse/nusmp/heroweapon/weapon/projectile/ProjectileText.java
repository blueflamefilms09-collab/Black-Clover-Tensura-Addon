package com.newuniverse.nusmp.heroweapon.weapon.projectile;

import com.newuniverse.nusmp.heroweapon.weapon.kit.Fmt;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.newuniverse.nusmp.heroweapon.weapon.projectile.ProjectileNumbers.*;

/**
 * Why: the names and descriptions of the projectile's techniques and passives, built from {@link ProjectileNumbers} so the numbers in the text are the numbers
 * in the table, free of game classes so a test can read them, and the source of the lang fragment the lead merges. Plain ASCII English.
 */
public final class ProjectileText {
    private ProjectileText() {}

    /** Skill id to {name, description}. */
    public static final Map<String, String[]> SKILLS = new LinkedHashMap<>();
    /** Passive id to {name, description}. */
    public static final Map<String, String[]> PASSIVES = new LinkedHashMap<>();

    static {
        SKILLS.put(INSTANT_THROW.id(), new String[]{"Air Strike Throw",
                "Throw a blade along your look line (" + Fmt.num(THROW_RANGE) + " blocks) through up to " + THROW_PIERCE + " foes for " + Fmt.num(INSTANT_THROW.rawBase())
                        + " damage each. Every foe it hits is marked for " + Fmt.secs(MARK_TICKS) + " s."});
        SKILLS.put(BOOMERANG.id(), new String[]{"Returning Edge",
                "The blade flies out " + Fmt.num(BOOMERANG_RANGE) + " blocks, then comes back to you by a curved path, striking foes on both ways for " + Fmt.num(BOOMERANG.rawBase())
                        + " damage each time."});
        SKILLS.put(RICOCHET.id(), new String[]{"Ricochet Throw",
                "The blade hits the foe you aim at, or glances off a wall, and jumps to up to " + RICOCHET_JUMPS + " more foes within " + Fmt.num(RICOCHET_JUMP) + " blocks of each other for "
                        + Fmt.num(RICOCHET.rawBase()) + " damage each."});
        SKILLS.put(KNIFE_RAIN.id(), new String[]{"Knife Rain",
                "One knife becomes " + KNIFE_COUNT + ", fanned over " + Fmt.num(KNIFE_SPREAD) + " degrees. The fan shows for " + Fmt.secs(KNIFE_DELAY) + " s, then each knife deals " + Fmt.num(KNIFE_RAIN.rawBase()) + " damage to the first foe it meets (a foe takes at most "
                        + KNIFE_CAP + ")."});
        SKILLS.put(TORNADO_THROW.id(), new String[]{"Tornado Throw",
                "Three blades whirl round the foe you aim at, " + Fmt.num(TORNADO_THROW.rawBase()) + " damage each, and slow it."});
        SKILLS.put(BOLAS_BLAST.id(), new String[]{"Bolas Blast",
                "Bolas wrap the foe you aim at and slow it; after " + Fmt.secs(BOLAS_DELAY) + " s they burst in flame for " + Fmt.num(BOLAS_BLAST.rawBase()) + " damage to everything within "
                        + Fmt.num(BOLAS_RADIUS) + " blocks."});
        SKILLS.put(DRITTE_THROW.id(), new String[]{"Dritte Throw",
                "Three blades in quick turn, each at a different foe in front of you (the same one if there is only one), " + Fmt.num(DRITTE_THROW.rawBase()) + " damage each."});
        SKILLS.put(RECALL.id(), new String[]{"Recall Blades",
                "Every marked foe within " + Fmt.num(RECALL_RANGE) + " blocks is struck by a blade that flies back to you, " + Fmt.num(RECALL.rawBase()) + " damage each (up to "
                        + RECALL.maxTargets() + "). The marks are used up."});
        SKILLS.put(SHADOW_BIND.id(), new String[]{"Shadow Bind",
                "Pin the foe you aim at by throwing a blade into its shadow: it cannot move for about " + Fmt.secs(BIND_TICKS) + " s (less on a player or a boss, and only once in a while)."});
        SKILLS.put(THOUSAND_BLADES.id(), new String[]{"Thousand Blade Rain",
                "Marks a field " + Fmt.num(BLADES_RADIUS * 2) + " blocks wide where you aim. After " + Fmt.secs(BLADES_TELL) + " s blades pour down on it in " + BLADES_VOLLEYS
                        + " volleys, " + Fmt.num(THOUSAND_BLADES.rawBase()) + " damage each."});

        PASSIVES.put("mark_prey", new String[]{"Marked Prey",
                "Every foe your blades or your knife hit is marked for " + Fmt.secs(MARK_TICKS) + " s; Recall Blades strikes the marked."});
        PASSIVES.put("second_throw", new String[]{"Second Throw",
                "Air Strike Throw throws a second blade " + Fmt.secs(SECOND_DELAY) + " s after the first."});
        PASSIVES.put("wide_ricochet", new String[]{"Wide Ricochet",
                "Ricochet Throw jumps to one more foe and " + Fmt.num(RICOCHET_WIDE_JUMP) + " blocks farther each time."});
        PASSIVES.put("heavy_hatchet", new String[]{"Heavy Hatchet",
                "Bolas Blast bursts " + Fmt.num(BOLAS_HEAVY_RADIUS) + " block wider and Tornado Throw slows its foe " + Fmt.secs(TORNADO_HEAVY_SLOW) + " s longer."});
        PASSIVES.put("over_custom", new String[]{"Over Custom",
                "Every technique can be used again " + Fmt.pct(1.0 - CUSTOM_COOLDOWN) + " percent sooner, the customisation paid for with coin."});
    }

    public static String name(String skillId) { return SKILLS.get(skillId)[0]; }

    public static String desc(String skillId) { return SKILLS.get(skillId)[1]; }
}
