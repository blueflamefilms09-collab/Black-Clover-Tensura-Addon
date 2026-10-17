package com.newuniverse.nusmp.arc.arena;

import java.util.List;

/**
 * Why: a player chooses WHICH boss the Chronicle Sigil carries them to by clicking a name on the lectern, and everybody forgets what they chose: the pick lives in
 * the player's own data as a plain id, and the sigil must still do something sensible when it is stale (the boss was removed from the roster, the story
 * was reset and it is locked again, the boss was defeated and replays are off). The rules: a stored pick that can still be fought is kept; otherwise the sigil
 * offers the NEXT boss of the story (the first open one that is not defeated); otherwise, if replays are allowed, the first defeated one; otherwise nothing. And
 * a player who wants another can cycle through what can be fought, wrapping round. Pure.
 */
public final class ChroniclePick {
    private ChroniclePick() {}

    /** The id the sigil carries the party to, or null when nobody can be fought at all. */
    public static String choose(List<ChronicleView.Row> rows, String stored, boolean replay) {
        if (stored != null && !stored.isEmpty()) {
            for (ChronicleView.Row r : rows) if (r.id().equals(stored) && r.selectable(replay)) return r.id();
        }
        for (ChronicleView.Row r : rows) if (r.state() == ChronicleView.State.OPEN) return r.id();
        if (replay) for (ChronicleView.Row r : rows) if (r.state() == ChronicleView.State.DEFEATED) return r.id();
        return null;
    }

    /** The boss after {@code current} among those that can be fought, in roster order, wrapping round; the first one when {@code current} is not on the list; null when none can. */
    public static String cycle(List<ChronicleView.Row> rows, String current, boolean replay) {
        int from = -1;
        for (int i = 0; i < rows.size(); i++) if (rows.get(i).id().equals(current)) from = i;
        for (int step = 1; step <= rows.size(); step++) {
            ChronicleView.Row r = rows.get((from + step + rows.size()) % rows.size());
            if (r.selectable(replay)) return r.id();
        }
        return null;
    }

    /** May this id be picked right now? (The lectern's click command asks.) */
    public static boolean pickable(List<ChronicleView.Row> rows, String id, boolean replay) {
        for (ChronicleView.Row r : rows) if (r.id().equals(id)) return r.selectable(replay);
        return false;
    }
}
