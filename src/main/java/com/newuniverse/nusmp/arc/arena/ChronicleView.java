package com.newuniverse.nusmp.arc.arena;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.arc.story.StoryData;
import com.newuniverse.nusmp.arc.story.StoryRules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Why: the lectern of the Chronicle must show one player's story as a list (saga, boss, locked or open, defeated) and the sigil must know which of those a
 * player may be carried to. Both are the same table, so the table is built once, here, from the roster and the player's story: one {@link Row} per boss in roster
 * order with its {@link State}, grouped into one {@link Page} per saga in the story's own order (only sagas that have a boss: a saga of friends is not on the list).
 * A boss is DEFEATED when the story says so, OPEN when every boss it follows has fallen (or the story gate is off), and LOCKED otherwise with the names of what is
 * still missing, so the page can say "after Dante and Vanica". Pure: the lectern only turns rows into chat lines.
 */
public final class ChronicleView {
    private ChronicleView() {}

    public enum State { DEFEATED, OPEN, LOCKED }

    /** One boss on the list; {@code missing} holds the names of the characters it still waits for (empty unless LOCKED). */
    public record Row(String id, String name, String title, ArcSaga saga, int tier, State state, List<String> missing) {
        /** May the party be carried to this boss now? A defeated one only when replays are allowed. */
        public boolean selectable(boolean replay) { return state == State.OPEN || (state == State.DEFEATED && replay); }
    }

    /** One saga's page: its rows and how many of them are defeated. */
    public record Page(ArcSaga saga, List<Row> rows, int defeated) {
        public boolean cleared() { return !rows.isEmpty() && defeated == rows.size(); }
    }

    /**
     * The rows of every boss of the roster for this story.
     *
     * @param gateOn the story gate ({@code arc.storyGate}): with it off every boss that is not defeated is OPEN
     */
    public static List<Row> rows(Collection<ArcCharacter> roster, StoryData d, boolean gateOn) {
        Map<String, ArcCharacter> byId = new LinkedHashMap<>();
        for (ArcCharacter c : roster) byId.put(c.id(), c);
        Predicate<String> exists = byId::containsKey;
        Function<String, String> nameOf = id -> byId.containsKey(id) ? byId.get(id).name() : id;
        List<Row> out = new ArrayList<>();
        for (ArcCharacter c : roster) {
            if (!c.isBoss()) continue;
            State state;
            List<String> missing = List.of();
            if (d.hasDefeated(c.id())) {
                state = State.DEFEATED;
            } else {
                List<String> ids = StoryRules.missing(d, c, exists);
                if (!gateOn || ids.isEmpty()) {
                    state = State.OPEN;
                } else {
                    state = State.LOCKED;
                    missing = new ArrayList<>();
                    for (String id : ids) missing.add(nameOf.apply(id));
                }
            }
            out.add(new Row(c.id(), c.name(), c.title(), c.saga(), c.tier(), state, missing));
        }
        return out;
    }

    /** The pages: one per saga that has a boss, in the story's order, each with its rows in roster order. */
    public static List<Page> pages(List<Row> rows) {
        List<Page> out = new ArrayList<>();
        for (ArcSaga saga : ArcSaga.values()) {
            List<Row> in = new ArrayList<>();
            int done = 0;
            for (Row r : rows) {
                if (r.saga() != saga) continue;
                in.add(r);
                if (r.state() == State.DEFEATED) done++;
            }
            if (!in.isEmpty()) out.add(new Page(saga, in, done));
        }
        return out;
    }

    /** The index of the page of this saga, or -1. */
    public static int pageIndex(List<Page> pages, ArcSaga saga) {
        for (int i = 0; i < pages.size(); i++) if (pages.get(i).saga() == saga) return i;
        return -1;
    }

    /** The page a player should see first: their own saga's, else the first. */
    public static int firstPage(List<Page> pages, ArcSaga current) {
        int i = pageIndex(pages, current);
        return i >= 0 ? i : 0;
    }

    /** The next page after {@code index}, wrapping round (0 for an empty list). */
    public static int nextPage(List<Page> pages, int index) { return pages.isEmpty() ? 0 : (index + 1) % pages.size(); }
}
