package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.entity.veldanava.skills.JudgmentRules.Kind;
import com.newuniverse.nusmp.entity.veldanava.skills.JudgmentRules.Mode;

import java.util.List;
import java.util.Locale;

/**
 * Why: the numbers of Jibril, Lord of Rigor (docs/veldanava_skills.md) in one place that names no Minecraft type, so the skill, the events, the unit tests and the CI self-test read the same
 * table. Ten modes like every Veldanava skill: four toggles (Divine Rigor, Heavenly Presence, Strict Governance, Wings of Revelation), five actives (Absolute Command, Judgment of Heaven, Heavenly
 * Purge, Holy Shackles, Word of Law) and the held mastery active Kingdom of God. Costs are in Magicule; Kingdom of God's cost is per second held.
 */
public final class JibrilRules {
    private JibrilRules() {}

    public static final JudgmentRules.Spec JIBRIL = new JudgmentRules.Spec("jibril", "Jibril", "Lord of Rigor", List.of(
            new Mode(0, "divine_rigor", "Divine Rigor", Kind.TOGGLE, 0, 0, 0, false),
            new Mode(1, "heavenly_presence", "Heavenly Presence", Kind.TOGGLE, 0, 0, 0, false),
            new Mode(2, "strict_governance", "Strict Governance", Kind.TOGGLE, 0, 0, 0, false),
            new Mode(3, "wings_of_revelation", "Wings of Revelation", Kind.TOGGLE, 0, 0, 0, false),
            new Mode(4, "absolute_command", "Absolute Command", Kind.PRESS, 5000, 3500, 25, 15, false),
            new Mode(5, "judgment_of_heaven", "Judgment of Heaven", Kind.CROUCH_PRESS, 8500, 6000, 30, 20, false),
            new Mode(6, "heavenly_purge", "Heavenly Purge", Kind.PRESS, 7000, 5000, 40, 25, false),
            new Mode(7, "holy_shackles", "Holy Shackles", Kind.CROUCH_PRESS, 4500, 3000, 20, 12, false),
            new Mode(8, "word_of_law", "Word of Law", Kind.PRESS, 6000, 4000, 18, 10, false),
            new Mode(9, "kingdom_of_god", "Kingdom of God", Kind.HELD, 4000, 2500, 90, 60, true)));

    /** Jibril's skill code in the player's mask (bits 5..7), see {@link JudgmentRules#code}. */
    public static final int CODE = 3;

    // ---------------------------------------------------------------- Divine Rigor (toggle 0)
    /** How far a mastered Divine Rigor reaches its allies (blocks) and the share of the resistance they get (their debuffs last this much of their length: one half). */
    public static final double RIGOR_ALLY_RADIUS = 16.0, RIGOR_ALLY_SHARE = 0.5;
    /** The most harmful effects one second of Divine Rigor cleanses from its owner. */
    public static final int RIGOR_CLEANSE_MAX = 8;

    // ---------------------------------------------------------------- Heavenly Presence (toggle 1)
    /** The holy burn: reach, base damage a second, extra damage per stack of Temporal Debt (and its cap), the creatures it burns at once. */
    public static final double PRESENCE_RADIUS = 16.0, PRESENCE_DAMAGE = 14.0, PRESENCE_PER_DEBT = 0.5, PRESENCE_DEBT_CAP = 20.0;
    public static final int PRESENCE_MAX_TARGETS = 12;

    // ---------------------------------------------------------------- Strict Governance (toggle 2)
    /** The reach of the law (blocks), the enemy players it holds at once, the skills of one player it walks and the modes of one skill. */
    public static final double GOVERNANCE_RADIUS = 30.0;
    public static final int GOVERNANCE_MAX_PLAYERS = 16, GOVERNANCE_MAX_SKILLS = 24, GOVERNANCE_MAX_MODES = 16;

    // ---------------------------------------------------------------- Wings of Revelation (toggle 3)
    /** Creative flight speed of the wings (the game's own is 0.05), the chance (percent) that a blow taken in the air is dodged (normal / mastered), and how often the wings are drawn again (ticks). */
    public static final float WINGS_FLY_SPEED = 0.16f;
    public static final int WINGS_DODGE_PERCENT = 70, WINGS_DODGE_PERCENT_MASTERED = 85, WINGS_FX_EVERY_TICKS = 80;

    // ---------------------------------------------------------------- the actives
    /** Absolute Command: reach, base length of the halt (ticks; the Balance Law limits it per target). */
    public static final double COMMAND_RANGE = 40.0;
    public static final int COMMAND_TICKS = 100;
    /** Judgment of Heaven: reach, base damage, pillar radius, the radius of the splash, its share of the damage and the most creatures it touches besides the target; the good effects stripped from the target. */
    public static final double JUDGMENT_RANGE = 64.0, JUDGMENT_DAMAGE = 220.0, JUDGMENT_RADIUS = 2.5, JUDGMENT_SPLASH_RADIUS = 3.0, JUDGMENT_SPLASH_SHARE = 0.5;
    public static final int JUDGMENT_MAX_SPLASH = 6;
    /** Heavenly Purge: radius, the lingering clouds it clears, the allied players it cleanses. */
    public static final double PURGE_RADIUS = 50.0;
    public static final int PURGE_MAX_CLOUDS = 64, PURGE_MAX_ALLIES = 24, PURGE_CLEANSE_MAX = 8;
    /** Holy Shackles: reach, base length of the Movement Interference (ticks), the good effects stolen at most, and the longest a stolen effect lasts on the thief (ticks). */
    public static final double SHACKLE_RANGE = 40.0;
    public static final int SHACKLE_TICKS = 120, SHACKLE_STEAL_MAX = 8, SHACKLE_BUFF_MAX_TICKS = 6000;
    /** Word of Law: the parry window (ticks; normal / mastered) and the power the blow comes back with. */
    public static final int PARRY_TICKS = 12, PARRY_TICKS_MASTERED = 20;
    public static final double REFLECT_MULTIPLIER = 3.0;
    /** Kingdom of God: the radius, the longest it stands (ticks), the second between pulses, what a pulse does to one enemy (good effects stripped, crush length) and how many enemies one pulse touches. */
    public static final double KINGDOM_RADIUS = 50.0, KINGDOM_FLOOR_HP = 1.0;
    public static final int KINGDOM_MAX_TICKS = 1200, KINGDOM_PULSE_TICKS = 20, KINGDOM_STRIP = 4, KINGDOM_CRUSH_TICKS = 30, KINGDOM_MAX_ENEMIES = 24, KINGDOM_FX_EVERY_TICKS = 80;
    /** How long the sanctuary keeps protecting after its last pulse (ticks): two pulses' worth, so a late tick never opens a gap. */
    public static final int KINGDOM_GRACE_TICKS = 40;
    /** The hotbar of a commanded player stays on its slot this long at most (the Balance Law's limit applies first). */
    public static final int HOTBAR_MAX_TICKS = 200;

    // ---------------------------------------------------------------- pure helpers
    /** The length of a debuff that lands on an ally under a mastered Divine Rigor: half of it (never below one tick; an endless effect is left as it is). */
    public static int rigorAllyDuration(int duration) {
        if (duration < 0) return duration;
        return Math.max(1, (int) Math.round(duration * RIGOR_ALLY_SHARE));
    }

    /** True when a creature type path names a demon: one of its words (split on anything that is not a letter) starts with a demon word, or is "imp" itself ("chimp" and "simple" are not demons). */
    public static boolean demonic(String typePath) {
        if (typePath == null) return false;
        for (String word : typePath.toLowerCase(Locale.ROOT).split("[^a-z]+")) {
            if (word.equals("imp") || word.equals("imps")) return true;
            for (String key : DEMON_WORDS) if (word.startsWith(key)) return true;
        }
        return false;
    }

    private static final String[] DEMON_WORDS = {"demon", "devil", "daemon", "fiend", "succub", "incub", "infernal", "hellhound", "hellspawn"};

    /** The holy damage a second of Heavenly Presence deals to a creature with this much Temporal Debt. */
    public static double presenceDamage(int debt) {
        return PRESENCE_DAMAGE + Math.min(PRESENCE_DEBT_CAP, Math.max(0, debt) * PRESENCE_PER_DEBT);
    }

    /** True when Strict Governance stretches the cooldowns this second: every other second, so a cooldown that counts down one a second counts down one every two (twice as long). */
    public static boolean governanceSecond(long gameTime) { return (gameTime / 20L) % 2L == 0L; }

    /** The chance (percent) of a dodge in the air. */
    public static int dodgePercent(boolean mastered) { return mastered ? WINGS_DODGE_PERCENT_MASTERED : WINGS_DODGE_PERCENT; }

    /** True when a roll of 0..99 is a dodge. */
    public static boolean dodges(int roll, boolean mastered) { return roll >= 0 && roll < dodgePercent(mastered); }

    /** The parry window in ticks. */
    public static int parryTicks(boolean mastered) { return mastered ? PARRY_TICKS_MASTERED : PARRY_TICKS; }

    /** The damage a parried blow comes back with: the blow times three, never negative. */
    public static double reflected(double blow) { return Math.max(0.0, blow) * REFLECT_MULTIPLIER; }

    /**
     * What is left of a blow inside the Kingdom of God: the blow, but never more than what leaves the ally on at least {@link #KINGDOM_FLOOR_HP} (health plus absorption minus the floor).
     * An ally on 1 HP or less takes nothing.
     */
    public static double kingdomLimited(double blow, double health, double absorption) {
        double room = Math.max(0.0, health + Math.max(0.0, absorption) - KINGDOM_FLOOR_HP);
        return Math.max(0.0, Math.min(blow, room));
    }

    /** How long a stolen effect lasts on the thief: what was left, at most {@link #SHACKLE_BUFF_MAX_TICKS}; an endless effect becomes the cap. */
    public static int stolenTicks(int remaining) {
        return remaining < 0 ? SHACKLE_BUFF_MAX_TICKS : Math.min(SHACKLE_BUFF_MAX_TICKS, Math.max(1, remaining));
    }

    /** The length the hotbar of a commanded player is held: the control time, never more than {@link #HOTBAR_MAX_TICKS}. */
    public static int hotbarTicks(int controlTicks) { return Math.max(0, Math.min(HOTBAR_MAX_TICKS, controlTicks)); }
}
