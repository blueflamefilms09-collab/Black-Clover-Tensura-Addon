package com.newuniverse.nusmp.boss.bars;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: the one value that travels from the server to the client for each custom boss bar (pure: no Minecraft type, so the diff and the byte layout are unit tested). The health itself is NOT in
 * it: the vanilla boss event packets already carry the name and the progress, and the client reads the fill from them. What the vanilla bar cannot say is here: which style, which phase of how
 * many and where the phases begin, the shield, the invulnerable / enraged / healing / dying flags, the lair action that is about to fire, the adds left, the move being wound up, the number of
 * fighters. The diff ({@link #differs}) is what keeps the traffic small: a state is sent only when something a viewer can see has changed, and the server asks at most five times a second.
 *
 * @param bar          the id of the vanilla {@code ServerBossEvent} this state decorates (the client keys everything by it)
 * @param entityId     the boss's entity id (-1 when unknown), so the client can draw its portrait
 * @param name         the boss's own name for the plate ("" = use the name of the vanilla boss event); a text starting with '@' is a lang key
 * @param style        one of {@link BarStyleIds#ALL}
 * @param title        the line under the name ("Lich of the Weeping Graves"), may be empty
 * @param phaseName    the name of the current phase, may be empty
 * @param phase        the current phase, 1-based
 * @param phaseCount   how many phases the boss has (1 = no phase display)
 * @param thresholds   the percent of health at which phase 2, 3, ... begin (descending, at most {@value #MAX_THRESHOLDS})
 * @param shield       absorption as a percent of the maximum health, 0..100
 * @param flags        {@link #INVULNERABLE} | {@link #ENRAGED} | {@link #HEALING} | {@link #DEAD} | {@link #PREVIEW}
 * @param lairId       the lair action about to fire ("" = none); the client turns the id into a lang key
 * @param lairTicks    ticks until it fires at the moment of sending (-1 = none)
 * @param lairTotal    the length of its warning window in ticks (for the ring), 0 = none
 * @param adds         how many adds are alive (-1 = no counter)
 * @param castName     the move being wound up ("" = none)
 * @param castPercent  how far the wind-up has got, 0..100 (-1 = none)
 * @param fighters     how many players are in the fight (-1 = unknown, 0 = none)
 */
public record BossBarState(UUID bar, int entityId, String name, String style, String title, String phaseName, int phase, int phaseCount, List<Integer> thresholds, int shield, int flags, String lairId,
                           int lairTicks, int lairTotal, int adds, String castName, int castPercent, int fighters) {
    public static final int INVULNERABLE = 1, ENRAGED = 2, HEALING = 4, DEAD = 8, PREVIEW = 16;
    public static final int MAX_THRESHOLDS = 6;
    /** A lair action is drawn as a warning for this long before it fires (5 s). */
    public static final int LAIR_WARNING_TICKS = 100;

    public BossBarState {
        name = name == null ? "" : name;
        style = style == null ? BarStyleIds.EVIL : style;
        title = title == null ? "" : title;
        phaseName = phaseName == null ? "" : phaseName;
        phaseCount = Math.max(1, Math.min(9, phaseCount));
        phase = Math.max(1, Math.min(phaseCount, phase));
        List<Integer> t = new ArrayList<>();
        if (thresholds != null) for (Integer v : thresholds) if (v != null && v > 0 && v < 100 && t.size() < MAX_THRESHOLDS) t.add(v);
        thresholds = List.copyOf(t);
        shield = Math.max(0, Math.min(100, shield));
        lairId = lairId == null ? "" : lairId;
        castName = castName == null ? "" : castName;
        castPercent = castPercent < 0 ? -1 : Math.min(100, castPercent);
        lairTicks = lairId.isEmpty() ? -1 : Math.max(-1, Math.min(32000, lairTicks));
        lairTotal = Math.max(0, Math.min(32000, lairTotal));
        fighters = Math.max(-1, Math.min(120, fighters));
    }

    public boolean has(int flag) { return (flags & flag) != 0; }

    public boolean invulnerable() { return has(INVULNERABLE); }

    public boolean enraged() { return has(ENRAGED); }

    public boolean healing() { return has(HEALING); }

    public boolean dead() { return has(DEAD); }

    public boolean casting() { return castPercent >= 0 && !castName.isEmpty(); }

    public boolean lairPending() { return !lairId.isEmpty() && lairTicks >= 0; }

    // ================================================================ the diff
    /** How much the wind-up must move before it is worth a packet (percent). */
    public static final int CAST_STEP = 5;

    /**
     * Has anything changed that a viewer can see? {@code prevTick} and {@code nextTick} are the server ticks the two states were read at: a lair countdown that merely ran down (the same
     * firing time, within 3 ticks) is not a change, and a wind-up that moved less than {@link #CAST_STEP} percent is not one either.
     */
    public static boolean differs(BossBarState prev, long prevTick, BossBarState next, long nextTick) {
        if (prev == null) return true;
        if (prev.entityId != next.entityId || prev.phase != next.phase || prev.phaseCount != next.phaseCount || prev.shield != next.shield || prev.flags != next.flags
                || prev.adds != next.adds || prev.fighters != next.fighters || prev.lairTotal != next.lairTotal) return true;
        if (!prev.name.equals(next.name) || !prev.style.equals(next.style) || !prev.title.equals(next.title) || !prev.phaseName.equals(next.phaseName) || !prev.thresholds.equals(next.thresholds)) return true;
        if (!prev.lairId.equals(next.lairId)) return true;
        if (!prev.lairId.isEmpty() && Math.abs((prevTick + prev.lairTicks) - (nextTick + next.lairTicks)) > 3) return true;
        if (!prev.castName.equals(next.castName)) return true;
        if ((prev.castPercent < 0) != (next.castPercent < 0)) return true;
        return prev.castPercent >= 0 && Math.abs(prev.castPercent - next.castPercent) >= CAST_STEP;
    }

    // ================================================================ the byte layout (the packet wraps these)
    /** A byte sink: the game's {@code FriendlyByteBuf} and the tests' {@code DataOutputStream} both fit behind it. */
    public interface Out {
        void writeByte(int v);
        void writeShort(int v);
        void writeInt(int v);
        void writeLong(long v);
        void writeUtf(String s, int maxChars);
    }

    public interface In {
        int readByte();
        int readShort();
        int readInt();
        long readLong();
        String readUtf(int maxChars);
    }

    public static final int MAX_STYLE = 32, MAX_TITLE = 96, MAX_NAME = 64, MAX_ID = 48;

    public void write(Out o) {
        o.writeLong(bar.getMostSignificantBits());
        o.writeLong(bar.getLeastSignificantBits());
        o.writeInt(entityId);
        o.writeUtf(name, MAX_TITLE);
        o.writeUtf(style, MAX_STYLE);
        o.writeUtf(title, MAX_TITLE);
        o.writeUtf(phaseName, MAX_NAME);
        o.writeByte(phase);
        o.writeByte(phaseCount);
        o.writeByte(thresholds.size());
        for (int t : thresholds) o.writeByte(t);
        o.writeByte(shield);
        o.writeByte(flags);
        o.writeUtf(lairId, MAX_ID);
        o.writeShort(lairTicks);
        o.writeShort(lairTotal);
        o.writeShort(adds);
        o.writeUtf(castName, MAX_NAME);
        o.writeByte(castPercent);
        o.writeByte(fighters);
    }

    public static BossBarState read(In in) {
        long hi = in.readLong(), lo = in.readLong();
        int entity = in.readInt();
        String name = in.readUtf(MAX_TITLE), style = in.readUtf(MAX_STYLE), title = in.readUtf(MAX_TITLE), phaseName = in.readUtf(MAX_NAME);
        int phase = in.readByte() & 0xFF, count = in.readByte() & 0xFF;
        int n = Math.min(MAX_THRESHOLDS, in.readByte() & 0xFF);
        List<Integer> th = new ArrayList<>(n);
        for (int i = 0; i < n; i++) th.add(in.readByte() & 0xFF);
        int shield = in.readByte() & 0xFF, flags = in.readByte() & 0xFF;
        String lair = in.readUtf(MAX_ID);
        int lairTicks = in.readShort(), lairTotal = in.readShort(), adds = in.readShort();
        String cast = in.readUtf(MAX_NAME);
        int castPct = in.readByte(), fighters = in.readByte();
        return new BossBarState(new UUID(hi, lo), entity, name, style, title, phaseName, phase, count, th, shield, flags, lair, lairTicks, lairTotal, adds, cast, castPct, fighters);
    }
}
