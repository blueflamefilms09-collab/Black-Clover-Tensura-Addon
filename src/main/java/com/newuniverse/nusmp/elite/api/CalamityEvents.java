package com.newuniverse.nusmp.elite.api;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

import java.util.UUID;

/**
 * Why: the public announcement channel of the calamity system. Other subsystems (the voice announcers, the chronicles, the records) must be able to react when a calamity is announced, rises,
 * changes phase or falls WITHOUT importing the world engine's internals: they listen to these NeoForge events on the GAME bus ({@code NeoForge.EVENT_BUS.addListener(CalamityEvents.Omen.class ...)}
 * or, to hear everything, {@code CalamityEvents.Base}). The classes live in {@code elite/api} (the neutral package) on purpose.
 *
 * <p>All events are fired on the server thread, none can be cancelled, and none is fired when the world engine or the calamity system is switched off in its config.
 * {@link Base#text} is the English line of the calamity's data file for this moment (it may be empty); the in-game chat uses the lang key
 * {@code nusmp.elite.calamity.<id>.<kind>} instead, so a voice system should speak {@code text} or its own translation.
 *
 * <p>Order of a calamity's life: {@link Omen} (the warning, {@code warnSeconds} before the rise) then {@link Rise} (the boss exists) then any number of {@link PhaseChange} then {@link Defeat},
 * or {@link Vanish} when the boss leaves without being defeated (removed by an operator, a cleanup, a failed rise).
 */
public final class CalamityEvents {
    private CalamityEvents() {}

    /** Posts an event on the game bus. Never throws into the caller: a listener's error is logged by the bus. */
    public static <T extends Base> T post(T event) { return NeoForge.EVENT_BUS.post(event); }

    /** The fields every calamity announcement has. */
    public abstract static class Base extends Event {
        private final String calamityId;
        private final String displayName;
        private final ServerLevel level;
        private final Vec3 pos;
        private final String text;

        protected Base(String calamityId, String displayName, ServerLevel level, Vec3 pos, String text) {
            this.calamityId = calamityId;
            this.displayName = displayName;
            this.level = level;
            this.pos = pos;
            this.text = text == null ? "" : text;
        }

        /** The calamity's id from its data file ({@code gravebound_colossus}, {@code lich_calamity}). */
        public String calamityId() { return calamityId; }

        /** Its display name in English ("Gravebound Colossus"). */
        public String displayName() { return displayName; }

        public ServerLevel level() { return level; }

        /** Where it happens (the place of the rise, or the boss's position). */
        public Vec3 pos() { return pos; }

        /** The English announcement line for this moment (may be empty). */
        public String text() { return text; }
    }

    /** A calamity is about to rise: the warning. {@code secondsLeft} is the time until the rise; {@code reason} is the trigger (sustained_magicule, mother_rocks, night, ritual, command). */
    public static final class Omen extends Base {
        private final int secondsLeft;
        private final String reason;

        public Omen(String id, String name, ServerLevel level, Vec3 pos, String text, int secondsLeft, String reason) {
            super(id, name, level, pos, text);
            this.secondsLeft = secondsLeft;
            this.reason = reason == null ? "" : reason;
        }

        public int secondsLeft() { return secondsLeft; }

        public String reason() { return reason; }
    }

    /** The boss exists now. {@code boss} is its entity's UUID. */
    public static final class Rise extends Base {
        private final UUID boss;

        public Rise(String id, String name, ServerLevel level, Vec3 pos, String text, UUID boss) {
            super(id, name, level, pos, text);
            this.boss = boss;
        }

        public UUID boss() { return boss; }
    }

    /** The boss entered phase {@code phase} (0-based; the first change is 1) of {@code phaseCount}. */
    public static final class PhaseChange extends Base {
        private final UUID boss;
        private final int phase, phaseCount;

        public PhaseChange(String id, String name, ServerLevel level, Vec3 pos, String text, UUID boss, int phase, int phaseCount) {
            super(id, name, level, pos, text);
            this.boss = boss;
            this.phase = phase;
            this.phaseCount = phaseCount;
        }

        public UUID boss() { return boss; }

        public int phase() { return phase; }

        public int phaseCount() { return phaseCount; }
    }

    /** The boss was defeated. {@code fighters} are the players who damaged it (their UUIDs). */
    public static final class Defeat extends Base {
        private final UUID boss;
        private final java.util.Set<UUID> fighters;
        private final long durationTicks;

        public Defeat(String id, String name, ServerLevel level, Vec3 pos, String text, UUID boss, java.util.Set<UUID> fighters, long durationTicks) {
            super(id, name, level, pos, text);
            this.boss = boss;
            this.fighters = java.util.Set.copyOf(fighters);
            this.durationTicks = durationTicks;
        }

        public UUID boss() { return boss; }

        public java.util.Set<UUID> fighters() { return fighters; }

        public long durationTicks() { return durationTicks; }
    }

    /** The boss is gone without being defeated (discarded, or the rise failed). */
    public static final class Vanish extends Base {
        private final String why;

        public Vanish(String id, String name, ServerLevel level, Vec3 pos, String text, String why) {
            super(id, name, level, pos, text);
            this.why = why == null ? "" : why;
        }

        public String why() { return why; }
    }
}
