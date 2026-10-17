package com.newuniverse.nusmp.client.avatar;

import com.newuniverse.nusmp.avatar.aptitude.Aptitude;
import com.newuniverse.nusmp.avatar.aptitude.ForgeContext;
import com.newuniverse.nusmp.avatar.aptitude.ForgeRules;
import com.newuniverse.nusmp.avatar.aptitude.ForgeSkill;
import com.newuniverse.nusmp.avatar.net.ForgeAptitudeRow;
import com.newuniverse.nusmp.avatar.net.ForgeSkillRow;
import com.newuniverse.nusmp.avatar.net.S2C_AptitudeRollPayload;
import com.newuniverse.nusmp.avatar.net.S2C_SoulForgeStatePayload;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Why: the Soul Forge screen opens a moment before, or after, the packets that fill it arrive (the rules and reserves, the two catalogs, the answer to a free
 * aptitude roll, a sample line from the chatter engine). This holds the latest of each with a version number the screen polls, exactly like
 * {@link AvatarClientData} does for the classic screen, so the screen never misses one and never has to be told. The catalogs are turned into the pure records
 * ({@link Aptitude}, {@link ForgeSkill}) once, here, so the screen's cost maths can use the very same classes the server's validator uses. Cleared on logout and
 * whenever a new state arrives for a new ritual. Client thread only (the net handlers hand over with {@code enqueueWork}).
 */
public final class SoulForgeClientData {
    private SoulForgeClientData() {}

    public static ForgeRules rules = ForgeRules.DEFAULT;
    public static ForgeContext reserves = new ForgeContext(0.0, 0.0, 0.0, 0.0, 0.0, 0, false, "", 0L, 0.0);
    public static int rollsLeft;
    public static List<ForgeAptitudeRow> aptitudes = List.of();
    public static List<ForgeSkillRow> skills = List.of();
    public static int stateVersion;

    /** The newest answer to the free roll; the screen reads it once per {@link #rollVersion}. */
    public static S2C_AptitudeRollPayload roll;
    public static int rollVersion;

    /** The newest sample line: the moment it is about (a ChatEvent name) and the words. */
    public static String barkEvent = "", barkLine = "";
    public static int barkVersion;

    private static final Map<String, ForgeAptitudeRow> APT_ROWS = new HashMap<>();
    private static final Map<String, Aptitude> APT = new HashMap<>();
    private static final Map<String, ForgeSkillRow> SKILL_ROWS = new HashMap<>();
    private static final Map<String, ForgeSkill> SKILL = new HashMap<>();

    public static void onState(S2C_SoulForgeStatePayload p) { load(p.rules(), p.reserves(), p.rollsLeft(), p.aptitudes(), p.skills()); }

    /** Takes a whole new state (the packet's content): the rules and reserves, the free rolls left, and the two catalogs. */
    public static void load(ForgeRules newRules, ForgeContext newReserves, int newRollsLeft, List<ForgeAptitudeRow> aptRows, List<ForgeSkillRow> skillRows) {
        rules = newRules;
        reserves = newReserves;
        rollsLeft = newRollsLeft;
        aptitudes = List.copyOf(aptRows);
        skills = List.copyOf(skillRows);
        APT_ROWS.clear();
        APT.clear();
        for (ForgeAptitudeRow r : aptitudes) {
            try {
                APT_ROWS.put(r.id(), r);
                APT.put(r.id(), r.toAptitude());
            } catch (RuntimeException badRow) {
                APT_ROWS.remove(r.id());                   // a row the pure record refuses is simply not offered
            }
        }
        SKILL_ROWS.clear();
        SKILL.clear();
        for (ForgeSkillRow r : skills) {
            try {
                SKILL_ROWS.put(r.id(), r);
                SKILL.put(r.id(), r.toSkill());
            } catch (RuntimeException badRow) {
                SKILL_ROWS.remove(r.id());
            }
        }
        stateVersion++;
    }

    public static void onRoll(S2C_AptitudeRollPayload p) {
        roll = p;
        rollsLeft = p.rollsLeft();
        rollVersion++;
    }

    /** The server's sample line arrives as {@code EVENT|words}. */
    public static void onBark(String message) {
        String m = message == null ? "" : message;
        int bar = m.indexOf('|');
        barkEvent = bar < 0 ? "" : m.substring(0, bar);
        barkLine = bar < 0 ? m : m.substring(bar + 1);
        barkVersion++;
    }

    public static ForgeAptitudeRow aptitudeRow(String id) { return id == null ? null : APT_ROWS.get(id.toLowerCase(Locale.ROOT)); }

    public static Aptitude aptitude(String id) { return id == null ? null : APT.get(id.toLowerCase(Locale.ROOT)); }

    public static ForgeSkillRow skillRow(String id) { return id == null ? null : SKILL_ROWS.get(id.toLowerCase(Locale.ROOT)); }

    public static ForgeSkill skill(String id) { return id == null ? null : SKILL.get(id.toLowerCase(Locale.ROOT)); }

    public static void clear() {
        rules = ForgeRules.DEFAULT;
        reserves = new ForgeContext(0.0, 0.0, 0.0, 0.0, 0.0, 0, false, "", 0L, 0.0);
        rollsLeft = 0;
        aptitudes = List.of();
        skills = List.of();
        APT_ROWS.clear();
        APT.clear();
        SKILL_ROWS.clear();
        SKILL.clear();
        roll = null;
        barkEvent = "";
        barkLine = "";
        stateVersion++;
        rollVersion++;
        barkVersion++;
    }
}
