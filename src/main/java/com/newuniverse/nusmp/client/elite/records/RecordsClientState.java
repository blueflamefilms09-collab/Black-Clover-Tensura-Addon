package com.newuniverse.nusmp.client.elite.records;

import com.newuniverse.nusmp.elite.records.RecordsNet;
import com.newuniverse.nusmp.elite.records.RecordsNet.Prog;
import com.newuniverse.nusmp.elite.records.RecordsNet.RecordInfo;
import com.newuniverse.nusmp.elite.records.RecordsNet.SynergyInfo;
import com.newuniverse.nusmp.elite.records.RecordsNet.TitleInfo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the client's copy of what the server told it: the definitions it may see, the player's state and the toasts waiting to be shown. Plain statics and plain collections, no Minecraft client class, so
 * the packet handlers (registered in common code) can fill it on any side and the screen and the toast overlay only read it, never the server. Until the first state arrives {@link #haveState()} is false and
 * the screen says it is waiting. Everything is dropped on leaving the world. Client thread only.
 */
public final class RecordsClientState {
    private RecordsClientState() {}

    private static final Map<String, RecordInfo> RECORDS = new LinkedHashMap<>();
    private static final Map<String, TitleInfo> TITLES = new LinkedHashMap<>();
    private static final Map<String, SynergyInfo> SYNERGIES = new LinkedHashMap<>();
    private static RecordsNet.State state;
    private static final Map<String, Prog> RECORD_PROGRESS = new HashMap<>();
    private static final Map<String, List<Prog>> TITLE_PROGRESS = new HashMap<>();
    private static final Map<String, int[]> SYNERGY_PROGRESS = new HashMap<>();
    private static final ArrayDeque<RecordsNet.Toast> TOASTS = new ArrayDeque<>();
    private static int version;

    // ------------------------------------------------------------------ receiving
    public static void receiveDefs(RecordsNet.Defs d) {
        if (d.replace()) {
            RECORDS.clear();
            TITLES.clear();
            SYNERGIES.clear();
        }
        for (RecordInfo r : d.records()) RECORDS.put(r.id(), r);
        for (TitleInfo t : d.titles()) TITLES.put(t.id(), t);
        for (SynergyInfo s : d.synergies()) SYNERGIES.put(s.id(), s);
        version++;
    }

    public static void receiveState(RecordsNet.State s) {
        state = s;
        RECORD_PROGRESS.clear();
        TITLE_PROGRESS.clear();
        SYNERGY_PROGRESS.clear();
        for (int i = 0; i < s.recordIds().size() && i < s.recordProgress().size(); i++) RECORD_PROGRESS.put(s.recordIds().get(i), s.recordProgress().get(i));
        for (int i = 0; i < s.titleIds().size() && i < s.titleProgress().size(); i++) TITLE_PROGRESS.put(s.titleIds().get(i), s.titleProgress().get(i));
        for (int i = 0; i < s.synergyIds().size() && i < s.synergyProgress().size(); i++) SYNERGY_PROGRESS.put(s.synergyIds().get(i), s.synergyProgress().get(i));
        version++;
    }

    public static void receiveToast(RecordsNet.Toast t) { TOASTS.add(t); }

    public static void reset() {
        RECORDS.clear();
        TITLES.clear();
        SYNERGIES.clear();
        RECORD_PROGRESS.clear();
        TITLE_PROGRESS.clear();
        SYNERGY_PROGRESS.clear();
        TOASTS.clear();
        state = null;
        version++;
    }

    // ------------------------------------------------------------------ reading
    public static boolean haveState() { return state != null; }

    /** Changes whenever new data arrived (the screen re-lays itself out). */
    public static int version() { return version; }

    public static List<RecordInfo> records() { return Collections.unmodifiableList(new ArrayList<>(RECORDS.values())); }

    public static List<TitleInfo> titles() { return Collections.unmodifiableList(new ArrayList<>(TITLES.values())); }

    public static List<SynergyInfo> synergies() { return Collections.unmodifiableList(new ArrayList<>(SYNERGIES.values())); }

    public static TitleInfo title(String id) { return TITLES.get(id); }

    public static boolean unlocked(String recordId) { return state != null && state.unlocked().containsKey(recordId); }

    /** Unix second of the unlock, 0 when locked. */
    public static long unlockedAt(String recordId) { return state == null ? 0L : state.unlocked().getOrDefault(recordId, 0L); }

    public static boolean held(String titleId) { return state != null && state.held().containsKey(titleId); }

    public static String active() { return state == null ? "" : state.active(); }

    public static int points() { return state == null ? 0 : state.points(); }

    public static int totalPoints() { return state == null ? 0 : state.totalPoints(); }

    public static int unlockedCount() { return state == null ? 0 : state.unlocked().size(); }

    public static int heldCount() { return state == null ? 0 : state.held().size(); }

    /** Progress of a LOCKED record (null for an unlocked one, or one the server did not report). */
    public static Prog recordProgress(String id) { return RECORD_PROGRESS.get(id); }

    /** Progress of each requirement of a title not yet held (empty once held). */
    public static List<Prog> titleProgress(String id) { return TITLE_PROGRESS.getOrDefault(id, List.of()); }

    /** {have, need} of a synergy; {0, 0} when unknown. */
    public static int[] synergyProgress(String id) { return SYNERGY_PROGRESS.getOrDefault(id, new int[]{0, 0}); }

    public static boolean synergyActive(String id) {
        int[] p = synergyProgress(id);
        return p[1] > 0 && p[0] >= p[1];
    }

    public static List<String> bonusLines() { return state == null ? List.of() : state.bonusLines(); }

    /** The next toast to show, or null. */
    public static RecordsNet.Toast pollToast() { return TOASTS.poll(); }

    public static boolean hasToast() { return !TOASTS.isEmpty(); }
}
