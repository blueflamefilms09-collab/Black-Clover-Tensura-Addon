package com.newuniverse.nusmp.skill.modes;

/**
 * One mode of a multi-mode skill, as plain data (no game type, so it is unit-tested). A mode has a permanent lowercase {@code id} (saved in the skill's mode id and in cooldowns, so it is never renamed or
 * reordered: new modes are only appended), an English name (the lang key is {@code anime_multiverse.skill.mode.<skill>.<id>}), a {@link CostClass} that says how it is paid and rate limited, and the
 * {@code action} the skill sends to its owner when the key is pressed. {@code sneakAction} (0 = none) is what a press while sneaking does instead; {@code releaseAction} (0 = none) makes the mode a HOLD
 * (press starts, the release sent when the key is let go). {@code need} is a skill specific unlock code (0 = always open) that {@link ModeBundle#next} skips while it is not met.
 */
public record ModeSpec(String id, String name, CostClass costClass, int action, int sneakAction, int releaseAction, int need) {
    /** How a mode is paid and how often it may be asked for (the skill that owns the table decides the numbers; this is only the class). */
    public enum CostClass { FREE, LIGHT, MEDIUM, HEAVY, ULTIMATE }

    public ModeSpec {
        if (id == null || !id.matches("[a-z][a-z0-9_]*")) throw new IllegalArgumentException("bad mode id: " + id);
        if (name == null || name.isBlank()) throw new IllegalArgumentException("no name for " + id);
        if (costClass == null) throw new IllegalArgumentException("no cost class for " + id);
        if (need < 0) throw new IllegalArgumentException("negative need for " + id);
    }

    public boolean isHold() { return releaseAction != 0; }

    public static ModeSpec of(String id, String name, CostClass c, int action) { return new ModeSpec(id, name, c, action, 0, 0, 0); }

    public ModeSpec sneak(int a) { return new ModeSpec(id, name, costClass, action, a, releaseAction, need); }

    public ModeSpec hold(int release) { return new ModeSpec(id, name, costClass, action, sneakAction, release, need); }

    public ModeSpec needs(int code) { return new ModeSpec(id, name, costClass, action, sneakAction, releaseAction, code); }
}
