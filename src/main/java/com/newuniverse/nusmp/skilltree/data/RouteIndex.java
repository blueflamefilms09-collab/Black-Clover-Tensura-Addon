package com.newuniverse.nusmp.skilltree.data;

import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Why: the registry has to answer "which route has this id / this book / this class / this skill?" thousands of times (every cast gate, every tooltip, every packet) and has to be swapped as a
 * WHOLE when the datapacks reload, so a reader never sees half of a reload. An immutable snapshot built once is the simplest structure that gives both: the maps never change after construction, so
 * the server thread, the client thread and a reload thread can all read the same object without a lock, and a reload just replaces the reference. Building it is also where the rules that need
 * ALL the files live (two routes may not share a book or a class id or a skill, the number and the total size of the routes are capped so the sync packet always fits, see {@link RouteWire}): a
 * route that breaks one of them is the one that loses, the LATER one in id order, deterministically, with a sentence saying who it clashed with. The report of every file (loaded or not, its
 * problems and warnings) travels with the snapshot, which is what {@code /multiverse tree routes} prints and what the self-test {@code tree.core.routes} asserts on. Pure (Gson and the records
 * only), so all of it is unit-tested.
 */
public final class RouteIndex {
    public static final RouteIndex EMPTY = new RouteIndex(List.of(), List.of(), 0);

    /**
     * Why: a server owner needs to see, per file, whether it loaded and exactly why not, without reading a log; this is that line of the report ({@code /multiverse tree routes}, the self-test).
     * It records loaded or not, how many nodes, and every problem and warning found in the file.
     */
    public record Entry(String id, boolean loaded, int nodes, List<String> problems, List<String> warnings) {
        /** Loaded with nothing wrong at all: what a shipped file must be. */
        public boolean clean() { return loaded && problems.isEmpty(); }
    }

    private final List<SkillRoute> routes;
    private final List<Entry> report;
    private final int bytes;
    private final Map<String, SkillRoute> byId = new HashMap<>(), byBook = new HashMap<>(), byClass = new HashMap<>();
    private final Map<String, String[]> bySkill = new HashMap<>();

    private RouteIndex(List<SkillRoute> routes, List<Entry> report, int bytes) {
        this.routes = Collections.unmodifiableList(new ArrayList<>(routes));
        this.report = Collections.unmodifiableList(new ArrayList<>(report));
        this.bytes = bytes;
        for (SkillRoute r : routes) {
            byId.putIfAbsent(r.id(), r);
            byBook.putIfAbsent(r.book(), r);
            byClass.putIfAbsent(r.classId(), r);
            for (SkillNode n : r.nodes()) if (n.grantsSkill()) bySkill.putIfAbsent(n.skill(), new String[]{r.id(), n.id()});
        }
    }

    // ---------------------------------------------------------------- lookups
    /** Every route, in id order. */
    public List<SkillRoute> all() { return routes; }

    public SkillRoute byId(String routeId) { return routeId == null ? null : byId.get(routeId); }

    public SkillRoute byBook(String bookId) { return bookId == null ? null : byBook.get(bookId); }

    public SkillRoute byClass(String classId) { return classId == null ? null : byClass.get(classId); }

    /** The route and node that grant a skill id, as a fresh {@code {route, node}} array, or null. */
    public String[] bySkill(String skillId) {
        String[] hit = skillId == null ? null : bySkill.get(skillId);
        return hit == null ? null : hit.clone();
    }

    public boolean isEmpty() { return routes.isEmpty(); }

    /** One entry per file that was looked at, in id order. */
    public List<Entry> report() { return report; }

    /** How many files had at least one problem (a refused route, or a route with skipped nodes). */
    public int problemFiles() {
        int n = 0;
        for (Entry e : report) if (!e.problems().isEmpty()) n++;
        return n;
    }

    /** The total encoded size of the loaded routes in bytes (what the sync packet carries). */
    public int bytes() { return bytes; }

    // ---------------------------------------------------------------- builders
    /**
     * A snapshot of routes that are already known to be good (the copy a client receives from its server). Duplicates are dropped silently, first one wins; nothing is re-validated, because the
     * server already did and this side only displays.
     */
    public static RouteIndex of(List<SkillRoute> routes) {
        List<Entry> report = new ArrayList<>();
        for (SkillRoute r : routes) report.add(new Entry(r.id(), true, r.nodes().size(), List.of(), List.of()));
        return new RouteIndex(routes, report, 0);
    }

    /**
     * Builds the snapshot from the parsed JSON files of a reload. {@code files} maps {@code <namespace>:<file name>} to the file's JSON; {@code unreadable} lists ids of files that exist but
     * could not be parsed as JSON at all (they are reported, not loaded). Files are taken in id order, so which of two clashing routes survives never depends on the order the game happened to list them.
     */
    public static RouteIndex build(Map<String, JsonElement> files, Collection<String> unreadable) {
        TreeSet<String> ids = new TreeSet<>(files.keySet());
        if (unreadable != null) ids.addAll(unreadable);
        List<SkillRoute> accepted = new ArrayList<>();
        List<Entry> report = new ArrayList<>();
        Map<String, String> books = new HashMap<>(), classes = new HashMap<>(), skills = new HashMap<>();
        int total = 0;
        for (String id : ids) {
            JsonElement json = files.get(id);
            if (json == null) {
                report.add(new Entry(id, false, 0, List.of("the file is not valid JSON (the server log has the parse error)"), List.of()));
                continue;
            }
            RouteSchema.Parsed parsed = RouteSchema.parse(id, json);
            List<String> problems = new ArrayList<>(parsed.problems());
            SkillRoute route = parsed.route();
            int size = 0;
            if (route != null) {
                String refusal = null;
                try {
                    size = RouteWire.size(route);
                } catch (IllegalArgumentException tooLong) {
                    refusal = "cannot be sent to players: " + tooLong.getMessage();
                }
                if (refusal == null && accepted.size() >= RouteWire.MAX_ROUTES) refusal = "more than " + RouteWire.MAX_ROUTES + " routes are installed, this one is not loaded";
                if (refusal == null && size > RouteWire.MAX_ROUTE_BYTES) refusal = "is too big to send to players: " + size + " bytes, the limit is " + RouteWire.MAX_ROUTE_BYTES;
                if (refusal == null && total + size > RouteWire.MAX_TOTAL_BYTES) refusal = "would make all routes together " + (total + size) + " bytes, the limit is " + RouteWire.MAX_TOTAL_BYTES;
                if (refusal == null && books.containsKey(route.book())) refusal = "its book '" + route.book() + "' is already the book of route '" + books.get(route.book()) + "'";
                if (refusal == null && classes.containsKey(route.classId())) refusal = "its class_id '" + route.classId() + "' is already used by route '" + classes.get(route.classId()) + "'";
                if (refusal == null) {
                    for (SkillNode n : route.nodes()) {
                        if (n.grantsSkill() && skills.containsKey(n.skill())) {
                            refusal = "node '" + n.id() + "' grants '" + n.skill() + "', which route '" + skills.get(n.skill()) + "' already grants";
                            break;
                        }
                    }
                }
                if (refusal != null) {
                    problems.add(refusal);
                    route = null;
                }
            }
            if (route != null) {
                accepted.add(route);
                books.put(route.book(), route.id());
                classes.put(route.classId(), route.id());
                for (SkillNode n : route.nodes()) if (n.grantsSkill()) skills.put(n.skill(), route.id());
                total += size;
            }
            report.add(new Entry(id, route != null, route == null ? 0 : route.nodes().size(), Collections.unmodifiableList(problems), Collections.unmodifiableList(new ArrayList<>(parsed.warnings()))));
        }
        return new RouteIndex(accepted, report, total);
    }
}
