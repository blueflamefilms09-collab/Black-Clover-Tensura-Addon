package com.newuniverse.nusmp.compat.create;

/**
 * Why: the six kinds of Create trap the dungeon can arm. The family decides the machinery that is drawn (see {@link TrapBlueprints}), the kind of hurt its bands do, how
 * deep in the dungeon it starts to appear and the timing of its cycle. Entries are only ever appended (the id, not the ordinal, is what is saved). Pure.
 */
public enum TrapFamily {
    /** Mechanical saws in both walls of a corridor, lashing across it one after the other. */
    SAW_RUN("saw_run", 1, HazardKind.SLASH, 44, 12, 10, 14, 1.0),
    /** Mechanical pistons in the walls and the rams they stand for, closing on the lane. */
    PRESS("press", 4, HazardKind.CRUSH, 60, 20, 12, 20, 1.0),
    /** Drills fed from a shaft line by gearboxes, lunging out of both walls. */
    DRILL_WALL("drill_wall", 7, HazardKind.BORE, 52, 16, 10, 17, 1.0),
    /** A conveyor belt that carries whoever stands on it toward a press. */
    TREADMILL("treadmill", 9, HazardKind.CRUSH, 56, 18, 14, 0, 1.0),
    /** Spouts under a fluid pipe, pouring molten metal. */
    POUR("pour", 3, HazardKind.SCALD, 64, 20, 22, 21, 1.0),
    /** Nixie tubes that light in order, and the discharge that follows them down the corridor. */
    SEQUENCE("sequence", 12, HazardKind.SHOCK, 48, 14, 8, 12, 1.0);

    private final String id;
    private final int minDepth, period, telegraph, active, stagger;
    private final HazardKind kind;
    private final double weight;

    TrapFamily(String id, int minDepth, HazardKind kind, int period, int telegraph, int active, int stagger, double weight) {
        this.id = id;
        this.minDepth = minDepth;
        this.kind = kind;
        this.period = period;
        this.telegraph = telegraph;
        this.active = active;
        this.stagger = stagger;
        this.weight = weight;
    }

    public String id() { return id; }

    /** The shallowest floor (absolute number) this family appears on. */
    public int minDepth() { return minDepth; }

    public HazardKind kind() { return kind; }

    /** Ticks of one cycle of a band. */
    public int period() { return period; }

    /** Ticks of the warning at the start of a cycle (sound, particles, the machinery winding up): nothing hurts yet. */
    public int telegraph() { return telegraph; }

    /** Ticks a band hurts, after its warning. */
    public int active() { return active; }

    /** Ticks between the start of one band's cycle and the next band's. */
    public int stagger() { return stagger; }

    /** The base share of this family in the draw before theme, depth and room kind are applied. */
    public double weight() { return weight; }

    /** The family with this id, or null. */
    public static TrapFamily byId(String id) {
        if (id == null) return null;
        for (TrapFamily f : values()) if (f.id.equalsIgnoreCase(id)) return f;
        return null;
    }
}
