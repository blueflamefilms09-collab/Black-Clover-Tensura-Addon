package com.newuniverse.nusmp.client.relics.worn;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: whether an armour relic was really drawn in a frame is a fact of the render thread that the plan ({@link WornPlan}) cannot know. Two places record it: the item's armour-model hook
 * ({@code ArmorSlotClient}) says "the vanilla armour layer was asked for this piece and answered with the painted layer" ({@link #markPainted}), and the worn layer says what the model library and
 * the own renderers did and what the failsafe did ({@link #rec}). {@link ArmorVisible} turns the facts into a decision; {@code /multiverse relic debug} prints the last record per slot.
 * No Minecraft types. Client render thread only.
 */
public final class ArmorTrace {
    private ArmorTrace() {}

    /** One armour relic of one wearer, last frame. */
    public static final class Rec {
        public String id = "", slot = "";
        public WornPlan.Kind kind = WornPlan.Kind.SKIP;
        public WornPlan.Why why = WornPlan.Why.DRAWN;
        public ArmorSlotPlan.Layer layer = ArmorSlotPlan.Layer.PAINTED;
        public int modelPieces;
        public boolean customRan, paintedAsked, textureOk, failsafeDrawn;
        public ArmorVisible.Outcome outcome = ArmorVisible.Outcome.FAILSAFE;
        public long tick;
    }

    private static final Map<UUID, Integer> PAINTED = new HashMap<>();
    private static final Map<UUID, Map<String, Rec>> RECS = new HashMap<>();
    private static final Map<String, Boolean> WARNED = new LinkedHashMap<>();

    private static int bit(String slot) { return "HEAD".equals(slot) ? 1 : "CHEST".equals(slot) ? 2 : 4; }

    /** The vanilla armour layer was asked about a relic in {@code slot} of this wearer and the answer was "draw the painted layer". */
    public static void markPainted(UUID wearer, String slot) {
        if (PAINTED.size() > 512) PAINTED.clear();
        PAINTED.merge(wearer, bit(slot), (a, b) -> a | b);
    }

    /** Reads and clears the mark of {@link #markPainted}. */
    public static boolean takePainted(UUID wearer, String slot) {
        Integer m = PAINTED.get(wearer);
        if (m == null) return false;
        int b = bit(slot);
        if ((m & b) == 0) return false;
        PAINTED.put(wearer, m & ~b);
        return true;
    }

    /** The record of {@code slot} of this wearer (created on demand). */
    public static Rec rec(UUID wearer, String slot) {
        if (RECS.size() > 256) RECS.clear();
        return RECS.computeIfAbsent(wearer, k -> new HashMap<>(4)).computeIfAbsent(slot, k -> new Rec());
    }

    /** The last record of {@code slot} of this wearer, or null. */
    public static Rec of(UUID wearer, String slot) {
        Map<String, Rec> m = RECS.get(wearer);
        return m == null ? null : m.get(slot);
    }

    /** True the first time it is asked for this relic id in this session (the failsafe logs one warning per relic). */
    public static boolean firstWarning(String relicId) {
        if (WARNED.size() > 1024) WARNED.clear();
        return WARNED.put(relicId, Boolean.TRUE) == null;
    }

    public static void clear() {
        PAINTED.clear();
        RECS.clear();
        WARNED.clear();
    }
}
