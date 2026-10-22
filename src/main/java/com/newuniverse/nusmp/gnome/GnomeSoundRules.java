package com.newuniverse.nusmp.gnome;

import com.newuniverse.nusmp.audio.voice.VoiceLimiter;

import java.util.List;
import java.util.UUID;

/**
 * Why: the Gnome has a sound set of its own (docs/magic/gnome.md section 6): not the basic Tensura spirit sounds, but a deep rumble, stone grinding and crashing, and a low resonant earth tone,
 * synthesised from nothing by tools/gen_gnome_sounds.py. This pure catalogue says which cues exist, how loud and how far each carries, and how often one may play ({@link Gate}, the pattern of
 * {@code audio.voice.VoiceLimiter}: a cooldown per cue per owner, a per-owner rate, and a server-wide rate), so the registry ({@link GnomeSounds}), {@code sounds.json}, the files and the tests cannot
 * drift apart and a crowd of earth mages cannot flood the sound engine. Everything is server driven: the game plays a cue to the players in range only.
 */
public final class GnomeSoundRules {
    private GnomeSoundRules() {}

    /**
     * One cue: id, number of variants, volume, the cooldown (ticks) between two plays of it for one owner, pitch range, what the subtitle says. How far it carries is the game's rule for a
     * variable range event: 16 blocks, and 16 times the volume when the volume is above 1 ({@link #range}); the server sends it to the players inside that range only.
     */
    public record Cue(String id, int variants, float volume, int cooldown, float pitchLow, float pitchHigh, String subtitle) {
        public int range() { return Math.round(16f * Math.max(1f, volume)); }

        public String event() { return "gnome." + id; }
        public String subtitleKey() { return "subtitles.nusmp.gnome." + id; }
        public float pitch(double unit) { return (float) (pitchLow + (pitchHigh - pitchLow) * Math.max(0.0, Math.min(1.0, unit))); }
    }

    /** The seven cues. Order is the registry order (append only). */
    public static final List<Cue> CUES = List.of(
            new Cue("link", 3, 0.9f, 20, 0.92f, 1.04f, "The Gnome rises beside its master"),
            new Cue("unlink", 2, 0.8f, 20, 0.92f, 1.04f, "The Gnome sinks into the earth"),
            new Cue("idle", 3, 0.55f, 160, 0.90f, 1.10f, "The Gnome grinds and rumbles"),
            new Cue("cast", 3, 1.0f, 6, 0.92f, 1.08f, "The Gnome grinds stone"),
            new Cue("rise", 3, 2.0f, 40, 0.94f, 1.04f, "The Gnome heaves out of the ground"),
            new Cue("slam", 3, 2.2f, 6, 0.92f, 1.06f, "The Gnome slams the earth"),
            new Cue("sink", 2, 1.6f, 40, 0.94f, 1.04f, "The Gnome sinks into the ground"));

    public static Cue cue(String id) {
        for (Cue c : CUES) if (c.id().equals(id)) return c;
        return null;
    }

    /** Most gnome sounds one owner may start in a second, and the whole server in a second. */
    public static final int PER_OWNER_PER_SECOND = 3, SERVER_PER_SECOND = 12;

    /**
     * The gate every server-driven Gnome sound goes through: {@link #allow} says yes only when the cue's own cooldown for this owner is over, the owner has not started too many this second, and
     * the whole server has not either. Time is a tick number the caller passes in, so tests drive it. Owner state is dropped by {@link #forget} (logout), everything by {@link #clear}.
     */
    public static final class Gate {
        private final VoiceLimiter owners = new VoiceLimiter(0);
        private long windowStart = Long.MIN_VALUE / 2;
        private int inWindow;

        public boolean allow(UUID owner, String cueId, long now) {
            Cue c = cue(cueId);
            if (c == null || owner == null) return false;
            if (now - windowStart >= VoiceLimiter.SECOND) { windowStart = now; inWindow = 0; }
            if (inWindow >= SERVER_PER_SECOND) return false;
            if (!owners.tryAcquire(owner, cueId, now, PER_OWNER_PER_SECOND, c.cooldown())) return false;
            inWindow++;
            return true;
        }

        public void forget(UUID owner) { owners.forget(owner); }

        public void clear() { owners.clear(); windowStart = Long.MIN_VALUE / 2; inWindow = 0; }

        public int tracked() { return owners.tracked(); }
    }
}
