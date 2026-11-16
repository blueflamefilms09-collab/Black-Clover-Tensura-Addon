package com.newuniverse.nusmp.dungeon.rework2;

import java.util.Arrays;
import java.util.List;

/**
 * Why: the dungeon bosses and creatures have a sound set of their own (docs/dungeon_bosses_rework2.md, part 2): a rising whoosh for a telegraph, a heavy slam, a chain rattle, a roar, a howl, a hiss, a gong
 * for a phase change, an ultimate's charge and blast, a bell toll, a metal clang, a spit, a shimmer, a crackle, a vanishing step and a growl, synthesised from nothing by tools/gen_dungeon_boss_sounds.py. This
 * pure catalogue says which cues exist, how many variants each has, how loud and how far each carries and how often one may play ({@link Limiter}: one instance per boss, so there is no static state), so the
 * registry ({@link DungeonBossSounds}), sounds.json, the files and the tests cannot drift apart ({@code DungeonBossSoundsTest}) and one boss cannot flood the sound engine. Nothing here knows Minecraft.
 */
public final class DungeonBossSoundRules {
    private DungeonBossSoundRules() {}

    /** One cue: id, variants, volume, the cooldown (ticks) between two plays of it by one boss, the pitch range, and what the subtitle says. The range is the game's rule for a variable-range event. */
    public record Cue(String id, int variants, float volume, int cooldown, float pitchLow, float pitchHigh, String subtitle) {
        /** The distance in blocks the sound reaches: 16, and 16 times the volume when that is above 1 (the server sends it to the players inside that range only). */
        public int range() { return Math.round(16f * Math.max(1f, volume)); }

        public String event() { return "dboss." + id; }

        public String subtitleKey() { return "subtitles.anime_multiverse.dboss." + id; }

        public float pitch(double unit) { return (float) (pitchLow + (pitchHigh - pitchLow) * Math.max(0.0, Math.min(1.0, unit))); }
    }

    /** The sixteen cues, in registry order (append only). */
    public static final List<Cue> CUES = List.of(
            new Cue("rise", 3, 1.2f, 8, 0.90f, 1.10f, "A boss gathers its strength"),
            new Cue("slam", 3, 1.8f, 6, 0.90f, 1.05f, "A boss slams down"),
            new Cue("chain", 3, 1.3f, 10, 0.92f, 1.08f, "Chains rattle"),
            new Cue("roar", 3, 2.0f, 40, 0.88f, 1.05f, "A boss roars"),
            new Cue("howl", 2, 1.8f, 100, 0.92f, 1.06f, "A beast howls"),
            new Cue("hiss", 3, 1.2f, 20, 0.92f, 1.10f, "A serpent hisses"),
            new Cue("gong", 2, 2.2f, 60, 0.92f, 1.04f, "A deep gong tolls"),
            new Cue("charge", 2, 1.8f, 60, 0.95f, 1.05f, "Power gathers"),
            new Cue("blast", 2, 2.4f, 60, 0.92f, 1.05f, "A great blast"),
            new Cue("toll", 2, 2.0f, 15, 0.94f, 1.04f, "A bell tolls"),
            new Cue("clang", 3, 1.2f, 6, 0.90f, 1.10f, "Metal clangs"),
            new Cue("spit", 2, 1.0f, 10, 0.92f, 1.10f, "Something spits venom"),
            new Cue("shimmer", 3, 1.2f, 8, 0.92f, 1.08f, "Magic shimmers"),
            new Cue("crackle", 3, 1.2f, 8, 0.92f, 1.08f, "Energy crackles"),
            new Cue("step", 2, 1.3f, 20, 0.92f, 1.08f, "A boss vanishes"),
            new Cue("growl", 3, 1.0f, 20, 0.90f, 1.08f, "A beast growls"));

    /** The cue with this id, or null. */
    public static Cue cue(String id) {
        for (Cue c : CUES) if (c.id().equals(id)) return c;
        return null;
    }

    /** The index of a cue in {@link #CUES}, or -1. */
    public static int indexOf(String id) {
        for (int i = 0; i < CUES.size(); i++) if (CUES.get(i).id().equals(id)) return i;
        return -1;
    }

    /** The most cues ONE boss may start in a second. */
    public static final int PER_BOSS_PER_SECOND = 4;

    /**
     * The gate one boss (or creature) puts its cues through: a cue plays only when its own cooldown is over and fewer than {@link #PER_BOSS_PER_SECOND} cues started in the last second. Time is a tick
     * number the caller passes in, so tests drive it. One instance per boss, a few longs: nothing static, nothing to clean up (it dies with the entity).
     */
    public static final class Limiter {
        private final long[] last = new long[CUES.size()];
        private long windowStart = Long.MIN_VALUE / 2;
        private int inWindow;

        public Limiter() { Arrays.fill(last, Long.MIN_VALUE / 2); }

        public boolean allow(String cueId, long now) {
            int i = indexOf(cueId);
            if (i < 0) return false;
            if (now - windowStart >= 20) { windowStart = now; inWindow = 0; }
            if (inWindow >= PER_BOSS_PER_SECOND || now - last[i] < CUES.get(i).cooldown()) return false;
            last[i] = now;
            inWindow++;
            return true;
        }
    }
}
