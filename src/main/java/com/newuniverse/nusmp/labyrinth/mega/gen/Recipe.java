package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.RoomKind;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the Sector Recipe is the list of promises every sector makes to the player, kept as DATA ({@code data/nusmp/mega/recipe/default.json}) so the
 * owner can retune the pacing without a new jar: how far the Guardian's hall must be from the Landing, how deep the far-side band that is kept free
 * for the boss chain is, how many extra safe rooms, trials and treasure closets must exist, how many set pieces may exist, and the thresholds the audit
 * enforces (rooms, loops). The class is pure (a plain bag of numbers with the defaults of the design document), the game-side loader only copies the
 * JSON into it with {@link #fromMap}, and a key it does not know is reported rather than ignored, because a typo in a recipe would otherwise silently
 * keep the default and the owner would tune a number that does nothing.
 *
 * <p>Distances are measured in MODULES (9 blocks) in the horizontal plane between box centres, because the designer thinks in rooms, not blocks.
 */
public final class Recipe {
    /** Longest chain of doors from the Landing (vanilla Jigsaw's {@code max_depth}); deeper sockets only get door caps. */
    public int maxDepth = 24;
    /** The audit fails a plan with fewer rooms (door caps and the tunnel to the Landing excluded). */
    public int minRooms = 40;
    /** The audit fails a plan with fewer loops (two unused doorways that face each other across a shared wall). */
    public int minLoops = 3;
    /** The Guardian's hall hangs from a doorway at least this many modules from the Landing's centre. */
    public double arenaMinDistance = 8.0;
    /** ... and at least this many doors deep in the placement tree. */
    public int arenaMinDepth = 5;
    /** Depth, in modules, of the full-width band at the far side of the region that the first fill leaves free for the boss chain; 0 = measure the chain pools and size it automatically. */
    public int reserveDepth = 0;
    /** How many extra SAFE rooms (besides the Landing) must exist at a distance of {@link #safeMinDistance} to {@link #safeMaxDistance} modules from the Landing. */
    public int extraSafe = 1;
    public double safeMinDistance = 5.0, safeMaxDistance = 12.0;
    /** How many candidate doorways the search tries for the Guardian's hall before it gives up on a seed. */
    public int chainSockets = 120;
    /** How many complete boss chains the search looks at before installing the one whose Way Onward the policy rates best. */
    public int chainLeaves = 10;
    /** Upper bound on attach attempts in the boss-chain and extras search (a runaway guard, not a tuning knob). */
    public long stepBudget = 600_000L;
    /** Pieces kept free of the first fill for the boss chain, the extras and the door caps. */
    public int capReserve = 60;
    /** Does the audit demand the landmark markers (ARRIVAL, GUARDIAN_SPAWN, GATEKEEPER, VAULT_CENTER, EXPANSION_SOCKET)? */
    public boolean requireMarkers = true;

    private final EnumMap<RoomKind, Integer> min = new EnumMap<>(RoomKind.class);
    private final EnumMap<RoomKind, Integer> max = new EnumMap<>(RoomKind.class);
    private final Map<String, Integer> shapeMax = new LinkedHashMap<>();
    private final List<String> unknown = new ArrayList<>();

    /**
     * The recipe of the design document: at least one trial, two treasure closets, at most one set piece. Since 1.55.0 it also carries the notes of the room builders:
     * the trap, puzzle and trial rooms (the dark room is a trap) are meant to be about one per sector, so at most two of each kind and the dark room once; the great hall
     * (3 x 5 modules) and the cathedral rotunda (5 x 5) are landmarks that must not repeat inside one sector.
     */
    public Recipe() {
        min.put(RoomKind.TRIAL, 1);
        min.put(RoomKind.TREASURE, 2);
        max.put(RoomKind.SET_PIECE, 1);
        max.put(RoomKind.TRIAL, 2);
        max.put(RoomKind.TRAP, 2);
        max.put(RoomKind.PUZZLE, 2);
        shapeMax.put("nusmp:mega/hall/great_hall", 1);
        shapeMax.put("nusmp:mega/set/cathedral_rotunda", 1);
        shapeMax.put("nusmp:mega/trap/dark_room", 1);
    }

    /** The least number of rooms of this kind a plan must hold (0 when the recipe asks for none). */
    public int min(RoomKind k) { return min.getOrDefault(k, 0); }

    /** The most rooms of this kind a plan may hold ({@link Integer#MAX_VALUE} when unlimited). */
    public int max(RoomKind k) { return max.getOrDefault(k, Integer.MAX_VALUE); }

    /** The most times the template with this id may appear in one sector (0 = unlimited). */
    public int shapeMax(String shapeId) { return shapeMax.getOrDefault(shapeId, 0); }

    public Recipe setMin(RoomKind k, int n) { min.put(k, Math.max(0, n)); return this; }

    public Recipe setMax(RoomKind k, int n) { max.put(k, Math.max(0, n)); return this; }

    public Recipe setShapeMax(String id, int n) { shapeMax.put(id, Math.max(0, n)); return this; }

    /** Keys the last {@link #fromMap} did not recognise (for the loader to log). */
    public List<String> unknownKeys() { return List.copyOf(unknown); }

    /** A copy with the same numbers. */
    public Recipe copy() {
        Recipe r = new Recipe();
        r.maxDepth = maxDepth;
        r.minRooms = minRooms;
        r.minLoops = minLoops;
        r.arenaMinDistance = arenaMinDistance;
        r.arenaMinDepth = arenaMinDepth;
        r.reserveDepth = reserveDepth;
        r.extraSafe = extraSafe;
        r.safeMinDistance = safeMinDistance;
        r.safeMaxDistance = safeMaxDistance;
        r.chainSockets = chainSockets;
        r.chainLeaves = chainLeaves;
        r.stepBudget = stepBudget;
        r.capReserve = capReserve;
        r.requireMarkers = requireMarkers;
        r.min.clear();
        r.min.putAll(min);
        r.max.clear();
        r.max.putAll(max);
        r.shapeMax.putAll(shapeMax);
        return r;
    }

    /**
     * The recipe a JSON object describes, laid over the defaults. {@code values} is the parsed JSON as plain Java (objects as {@code Map<String,Object>},
     * numbers as {@code Number}, booleans as {@code Boolean}); the schema is documented in {@code docs/labyrinth/mega_core.md}. Keys it does not know are
     * collected in {@link #unknownKeys}; a value of the wrong type or outside its sane range is clamped or skipped and also reported there.
     */
    public static Recipe fromMap(Map<String, Object> values) {
        Recipe r = new Recipe();
        if (values == null) return r;
        for (Map.Entry<String, Object> e : values.entrySet()) {
            String k = e.getKey();
            Object v = e.getValue();
            switch (k) {
                case "maxDepth" -> r.maxDepth = clamp(num(v, r.maxDepth), 2, 64);
                case "minRooms" -> r.minRooms = clamp(num(v, r.minRooms), 1, 1000);
                case "minLoops" -> r.minLoops = clamp(num(v, r.minLoops), 0, 1000);
                case "arenaMinDistance" -> r.arenaMinDistance = Math.max(0.0, dbl(v, r.arenaMinDistance));
                case "arenaMinDepth" -> r.arenaMinDepth = clamp(num(v, r.arenaMinDepth), 0, 64);
                case "reserveDepth" -> r.reserveDepth = clamp(num(v, r.reserveDepth), 0, 15);
                case "extraSafe" -> r.extraSafe = clamp(num(v, r.extraSafe), 0, 10);
                case "safeMinDistance" -> r.safeMinDistance = Math.max(0.0, dbl(v, r.safeMinDistance));
                case "safeMaxDistance" -> r.safeMaxDistance = Math.max(0.0, dbl(v, r.safeMaxDistance));
                case "chainSockets" -> r.chainSockets = clamp(num(v, r.chainSockets), 1, 5000);
                case "chainLeaves" -> r.chainLeaves = clamp(num(v, r.chainLeaves), 1, 200);
                case "stepBudget" -> r.stepBudget = Math.max(1000L, (long) dbl(v, r.stepBudget));
                case "capReserve" -> r.capReserve = clamp(num(v, r.capReserve), 0, 1000);
                case "requireMarkers" -> r.requireMarkers = v instanceof Boolean b ? b : r.requireMarkers;
                case "min" -> readKinds(v, r.min, r.unknown, "min");
                case "max" -> readKinds(v, r.max, r.unknown, "max");
                case "shapeMax" -> {
                    if (v instanceof Map<?, ?> m) {
                        for (Map.Entry<?, ?> s : m.entrySet()) r.shapeMax.put(String.valueOf(s.getKey()), clamp(num(s.getValue(), 0), 0, 1000));
                    } else r.unknown.add("shapeMax (not an object)");
                }
                default -> {
                    if (!k.startsWith("_")) r.unknown.add(k);          // keys starting with an underscore are comments
                }
            }
        }
        if (r.safeMaxDistance < r.safeMinDistance) r.safeMaxDistance = r.safeMinDistance;
        return r;
    }

    private static void readKinds(Object v, EnumMap<RoomKind, Integer> into, List<String> unknown, String what) {
        if (!(v instanceof Map<?, ?> m)) {
            unknown.add(what + " (not an object)");
            return;
        }
        for (Map.Entry<?, ?> e : m.entrySet()) {
            RoomKind kind = null;
            String key = String.valueOf(e.getKey());
            for (RoomKind rk : RoomKind.values()) if (!rk.folder().isEmpty() && rk.folder().equals(key)) kind = rk;
            if (kind == null) unknown.add(what + "." + key);
            else into.put(kind, clamp(num(e.getValue(), 0), 0, 1000));
        }
    }

    private static int num(Object v, int def) { return v instanceof Number n ? n.intValue() : def; }

    private static double dbl(Object v, double def) { return v instanceof Number n ? n.doubleValue() : def; }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
