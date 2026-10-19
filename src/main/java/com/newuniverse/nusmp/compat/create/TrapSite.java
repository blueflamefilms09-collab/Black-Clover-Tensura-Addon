package com.newuniverse.nusmp.compat.create;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

/**
 * Why: one Create trap as the world remembers it. A site is first PLANNED (a room and a family, decided when the sector was generated, nothing in the world yet), then BUILT
 * when its chunks are loaded and a choke was found in the room (the origin cell, the direction of the lane and the headroom are saved), or DROPPED when the room has no choke
 * or the build was refused. The drawing itself is never saved: it is re-derived from (family, core, headroom, trigger, seed), so a change to a drawing in a later version
 * never disagrees with a site that stores only its inputs. Mutable on purpose: the director moves a site through its states.
 */
public final class TrapSite {
    public enum State { PLANNED, BUILT, DROPPED }

    public final String id;
    public final int sector, room;
    public final TrapFamily family;
    public final TriggerKind trigger;
    public final int core;
    public final long seed;
    public State state = State.PLANNED;
    /** The world cell of local (0, 0, 0) and the direction of the lane (BUILT only). */
    public BlockPos origin;
    public Direction forward = Direction.SOUTH;
    public int headroom = TrapRules.MIN_HEADROOM;
    /** The depth (absolute floor) and sector threat the damage is scaled with, fixed at planning. */
    public int depth = 1, threat = 1;
    /** Why a site was dropped (a short word for the status command). */
    public String note = "";

    public TrapSite(String id, int sector, int room, TrapFamily family, TriggerKind trigger, int core, long seed) {
        this.id = id;
        this.sector = sector;
        this.room = room;
        this.family = family;
        this.trigger = trigger;
        this.core = core;
        this.seed = seed;
    }

    public static String idOf(int sector, int room) { return "s" + sector + "r" + room; }

    /** The drawing of this site, re-derived (never null). */
    public Blueprint blueprint() { return TrapBlueprints.build(family, core, headroom, trigger, seed); }

    public Frame frame() { return new Frame(origin, forward); }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("id", id);
        t.putInt("sector", sector);
        t.putInt("room", room);
        t.putString("family", family.id());
        t.putString("trigger", trigger.id());
        t.putInt("core", core);
        t.putLong("seed", seed);
        t.putString("state", state.name());
        if (origin != null) t.putIntArray("origin", new int[] {origin.getX(), origin.getY(), origin.getZ()});
        t.putString("forward", forward.getName());
        t.putInt("headroom", headroom);
        t.putInt("depth", depth);
        t.putInt("threat", threat);
        if (!note.isEmpty()) t.putString("note", note);
        return t;
    }

    /** Reads a site; null when the family is unknown (a site of a later version is skipped, not guessed). */
    public static TrapSite load(CompoundTag t) {
        TrapFamily f = TrapFamily.byId(t.getString("family"));
        if (f == null) return null;
        TrapSite s = new TrapSite(t.getString("id"), t.getInt("sector"), t.getInt("room"), f, TriggerKind.byId(t.getString("trigger")),
                Math.max(TrapRules.MIN_CORE, Math.min(TrapRules.MAX_CORE, t.getInt("core"))), t.getLong("seed"));
        try {
            s.state = State.valueOf(t.getString("state"));
        } catch (IllegalArgumentException e) {
            s.state = State.DROPPED;
        }
        int[] o = t.getIntArray("origin");
        if (o.length == 3) s.origin = new BlockPos(o[0], o[1], o[2]);
        Direction d = Direction.byName(t.getString("forward"));
        if (d != null && d.getAxis().isHorizontal()) s.forward = d;
        s.headroom = Math.max(TrapRules.MIN_HEADROOM, Math.min(TrapRules.MAX_HEADROOM, t.getInt("headroom")));
        s.depth = Math.max(1, t.getInt("depth"));
        s.threat = Math.max(1, t.getInt("threat"));
        s.note = t.getString("note");
        if (s.state == State.BUILT && s.origin == null) s.state = State.DROPPED;
        return s;
    }
}
