package com.newuniverse.nusmp.arc.lines;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Why: the own spoken lines of the Goku and the Sinbad boss (docs/boss_redesign_goku_sinbad.md, "Voice"). Each boss has his OWN set in his own manner, not the shared boss lines ({@code boss_<family>_<event>}):
 * Goku speaks like the anime hero (greetings, battle cries, technique names, friendly taunts), Sinbad like the king of Magi (his Djinn Equip calls, spell names, easy confidence). The manifests are
 * {@code tools/audio/goku_lines.json} and {@code tools/audio/sinbad_lines.json}; {@code tools/audio/boss_lines_gen.py} writes them and the generated {@link BossLinesData}. This class is PURE (no Minecraft
 * type): it answers which ids a boss has, the group and words of a line, which line to say next, how long a group rests, and which ids are REGISTERED, that is, have a voice file. A line without a file is
 * shown as the boss's chat text only; a registered line is also sent as a sound with its subtitle ({@code audio.voice.VoiceCatalog} appends {@link #registeredIds()}). The registered list is rewritten by
 * {@code tools/audio/boss_lines_gen.py --register} between the two marker comments, so it cannot drift from the files; {@code BossLinesTest} checks it.
 */
public final class BossLines {
    private BossLines() {}

    public static final String GOKU = "goku", SINBAD = "sinbad";

    // BEGIN REGISTERED (rewritten by tools/audio/boss_lines_gen.py --register)
    private static final String[] REGISTERED = {};
    // END REGISTERED

    private static final List<String> REG = Collections.unmodifiableList(java.util.Arrays.asList(REGISTERED));

    private static String[] idsArr(String boss) { return GOKU.equals(boss) ? BossLinesData.GOKU_IDS : SINBAD.equals(boss) ? BossLinesData.SINBAD_IDS : new String[0]; }

    private static String[] groupsArr(String boss) { return GOKU.equals(boss) ? BossLinesData.GOKU_GROUPS : SINBAD.equals(boss) ? BossLinesData.SINBAD_GROUPS : new String[0]; }

    private static String[] textsArr(String boss) { return GOKU.equals(boss) ? BossLinesData.GOKU_TEXTS : SINBAD.equals(boss) ? BossLinesData.SINBAD_TEXTS : new String[0]; }

    /** Every line id of a boss, recorded or not, in manifest order. */
    public static List<String> ids(String boss) { return List.of(idsArr(boss)); }

    /** The boss a line id belongs to ({@code goku}, {@code sinbad}), or null. */
    public static String bossOf(String id) {
        if (id == null) return null;
        if (id.startsWith("goku_")) return GOKU;
        if (id.startsWith("sinbad_")) return SINBAD;
        return null;
    }

    /** The group (the moment) of a line, or null for an unknown id. */
    public static String groupOf(String id) {
        String boss = bossOf(id);
        if (boss == null) return null;
        String[] ids = idsArr(boss);
        for (int i = 0; i < ids.length; i++) if (ids[i].equals(id)) return groupsArr(boss)[i];
        return null;
    }

    /** The words of a line (the chat text and the subtitle); the id itself when unknown. */
    public static String text(String id) {
        String boss = bossOf(id);
        if (boss == null) return String.valueOf(id);
        String[] ids = idsArr(boss);
        for (int i = 0; i < ids.length; i++) if (ids[i].equals(id)) return textsArr(boss)[i];
        return id;
    }

    /** The distinct groups of a boss, in manifest order. */
    public static List<String> groups(String boss) {
        List<String> out = new ArrayList<>();
        for (String g : groupsArr(boss)) if (!out.contains(g)) out.add(g);
        return out;
    }

    /** The line ids of one group of a boss. */
    public static List<String> idsOf(String boss, String group) {
        List<String> out = new ArrayList<>();
        String[] ids = idsArr(boss), gs = groupsArr(boss);
        for (int i = 0; i < ids.length; i++) if (gs[i].equals(group)) out.add(ids[i]);
        return out;
    }

    public static boolean hasGroup(String boss, String group) { return !idsOf(boss, group).isEmpty(); }

    /** The {@code counter}-th line of a group, round and round (null when the boss has no such group). */
    public static String pick(String boss, String group, int counter) {
        List<String> l = idsOf(boss, group);
        return l.isEmpty() ? null : l.get(Math.floorMod(counter, l.size()));
    }

    /** Ticks a group rests before the same boss says it again. */
    public static long cooldown(String group) {
        for (int i = 0; i < BossLinesData.COOLDOWN_GROUPS.length; i++) if (BossLinesData.COOLDOWN_GROUPS[i].equals(group)) return BossLinesData.COOLDOWN_TICKS[i];
        return BossLinesData.DEFAULT_COOLDOWN;
    }

    /** A line that is never held back by the gap between two lines: the fight start, the transformations and the end. */
    public static boolean priority(String group) {
        return "intro".equals(group) || "defeat".equals(group) || "victory".equals(group) || group.startsWith("form_") || group.startsWith("equip_");
    }

    /** The group a boss calls out when it starts to cast a move, or null when that move is silent. */
    public static String calloutGroup(String boss, String moveId) {
        String[] moves = GOKU.equals(boss) ? BossLinesData.GOKU_MOVES : SINBAD.equals(boss) ? BossLinesData.SINBAD_MOVES : new String[0];
        String[] groups = GOKU.equals(boss) ? BossLinesData.GOKU_MOVE_GROUPS : SINBAD.equals(boss) ? BossLinesData.SINBAD_MOVE_GROUPS : new String[0];
        for (int i = 0; i < moves.length; i++) if (moves[i].equals(moveId)) return groups[i];
        return null;
    }

    /** The move ids a boss calls out, in table order. */
    public static List<String> calloutMoves(String boss) {
        return List.of(GOKU.equals(boss) ? BossLinesData.GOKU_MOVES : SINBAD.equals(boss) ? BossLinesData.SINBAD_MOVES : new String[0]);
    }

    /** The registered ids (the ones that have a voice file); empty until the audio is generated and converted. */
    public static List<String> registeredIds() { return REG; }

    public static boolean isRegistered(String id) { return REG.contains(id); }

    /** The lang key of the words of a line, used as the subtitle of its sound event. */
    public static String subtitleKey(String id) { return "subtitles.anime_multiverse.voice." + id; }
}
