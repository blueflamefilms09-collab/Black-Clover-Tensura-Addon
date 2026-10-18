package com.newuniverse.nusmp.heroweapon.weapon.staff;

import com.newuniverse.nusmp.heroweapon.weapon.kit.Fmt;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.newuniverse.nusmp.heroweapon.weapon.staff.StaffNumbers.*;

/**
 * Why: the names and descriptions of the staff's techniques and passives are shown in the Weapon Book and the tooltip, and they quote the staff's numbers. They
 * are built here from {@link StaffNumbers} (never typed twice), kept free of game classes so a test can read them, and the lang fragment the lead merges is
 * written from the same strings. Plain ASCII English.
 */
public final class StaffText {
    private StaffText() {}

    /** Skill id to {name, description}, in the order of {@link StaffNumbers#ALL}. */
    public static final Map<String, String[]> SKILLS = new LinkedHashMap<>();
    /** Passive id to {name, description}. */
    public static final Map<String, String[]> PASSIVES = new LinkedHashMap<>();

    static {
        SKILLS.put(ARCANE_BOLT.id(), new String[]{"Arcane Bolt",
                "A bolt of mana along your look line (" + Fmt.num(BOLT_RANGE) + " blocks) that pierces " + BOLT_PIERCE + " foes for " + Fmt.num(ARCANE_BOLT.rawBase())
                        + " damage. A marked foe takes " + Fmt.pct(MARK_BONUS) + " percent more."});
        SKILLS.put(CHAIN_SPARK.id(), new String[]{"Chain Spark",
                "A spark leaps from the foe you aim at to up to " + CHAIN_JUMPS + " more within " + Fmt.num(CHAIN_JUMP) + " blocks of each other, " + Fmt.num(CHAIN_SPARK.rawBase())
                        + " damage each."});
        SKILLS.put(RUNE_FIELD.id(), new String[]{"Rune Field",
                "Draws a rune circle (" + Fmt.num(RUNE_RADIUS) + " blocks) where you aim. After " + Fmt.secs(RUNE_TELL) + " s it pulses " + RUNE_PULSES + " times for "
                        + Fmt.num(RUNE_FIELD.rawBase()) + " damage and slows whoever stands in it."});
        SKILLS.put(ARCANE_BURST.id(), new String[]{"Arcane Burst",
                "A ring of force " + Fmt.num(BURST_RADIUS) + " blocks wide round you: " + Fmt.num(ARCANE_BURST.rawBase()) + " damage and a shove for every foe in it, a moment after the ring shows."});
        SKILLS.put(MAGIC_PRISM.id(), new String[]{"Magic Prism",
                "Hangs a prism where you aim for " + Fmt.secs(PRISM_TICKS) + " s. An Arcane Bolt that passes through it splits into " + PRISM_COPIES
                        + " more bolts at foes near the prism (" + PRISM_CHARGES + " splits)."});
        SKILLS.put(FLOAT_MIRROR.id(), new String[]{"Float Mirror",
                "A mirror floats before you for " + Fmt.secs(MIRROR_TICKS) + " s: shots flying at you are destroyed (up to " + MIRROR_NEGATE + ") and up to "
                        + MIRROR_RETURNS + " are sent back at their shooters for " + Fmt.num(FLOAT_MIRROR.rawBase()) + " damage."});
        SKILLS.put(FENRIR_ROD.id(), new String[]{"Fenrir Rod",
                "Quickens your chant for " + Fmt.secs(HASTE_TICKS) + " s: every staff spell you cast waits " + Fmt.pct(1.0 - HASTE_FACTOR) + " percent less before it can be cast again."});
        SKILLS.put(ANALYZE.id(), new String[]{"Analyze Magic",
                "Marks up to " + ANALYZE.maxTargets() + " foes in front of you for " + Fmt.secs(MARK_TICKS) + " s. A marked foe takes " + Fmt.pct(MARK_BONUS)
                        + " percent more from your staff spells."});
        SKILLS.put(HEALING_LIGHT.id(), new String[]{"Healing Light",
                "Heals you and allies within " + Fmt.num(HEAL_RADIUS) + " blocks for " + Fmt.pct(HEALING_LIGHT.heal()) + " percent of their health and washes away one harmful effect each."});
        SKILLS.put(BOMBARDMENT.id(), new String[]{"Perpetual Bombardment",
                "Marks a field " + Fmt.num(BOMB_RADIUS * 2) + " blocks wide where you aim. After " + Fmt.secs(BOMB_TELL) + " s " + BOMB_SALVOS + " salvos of arcane bolts rain on it, "
                        + Fmt.num(BOMBARDMENT.rawBase()) + " damage each."});

        PASSIVES.put("quick_chant", new String[]{"Quick Chant",
                "Spells cast one after another (each within " + Fmt.secs(RHYTHM_WINDOW) + " s of the last) wait " + Fmt.pct(RHYTHM_STEP) + " percent less per earlier spell, up to "
                        + Fmt.pct(RHYTHM_STEP * RHYTHM_MAX) + " percent. A full staff swing keeps the chain going."});
        PASSIVES.put("sharp_focus", new String[]{"Sharp Focus",
                "Arcane Bolt deals " + Fmt.num(SHARP_BONUS) + " more damage and pierces one more foe."});
        PASSIVES.put("wide_prism", new String[]{"Wide Prism",
                "Magic Prism lasts " + Fmt.secs(PRISM_WIDE_TICKS) + " s longer and splits a bolt into " + PRISM_COPIES_WIDE + "."});
        PASSIVES.put("deep_mirror", new String[]{"Deep Mirror",
                "Float Mirror lasts " + Fmt.secs(MIRROR_DEEP_TICKS) + " s longer and can destroy " + (MIRROR_DEEP_NEGATE - MIRROR_NEGATE) + " more shots."});
        PASSIVES.put("star_mark", new String[]{"Star Mark",
                "Marks from Analyze Magic last " + Fmt.secs(MARK_EXTRA_TICKS) + " s longer."});
    }

    public static String name(String skillId) { return SKILLS.get(skillId)[0]; }

    public static String desc(String skillId) { return SKILLS.get(skillId)[1]; }
}
