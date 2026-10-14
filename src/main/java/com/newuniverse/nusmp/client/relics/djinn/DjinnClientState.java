package com.newuniverse.nusmp.client.relics.djinn;

import com.newuniverse.nusmp.relics.djinn.Djinn;
import com.newuniverse.nusmp.relics.djinn.DjinnNet;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;

/**
 * What this client knows of everyone's Djinn: the last {@link DjinnNet.Sync} per player entity, with the game time it arrived. A running phase is
 * refreshed every second by the server; one not heard from for three seconds is treated as over, so a player who left the range or logged out never
 * stays transformed. The HUD, the equip layer and the screen effects all read here.
 */
public final class DjinnClientState {
    private DjinnClientState() {}

    /** One player's Djinn at the moment a packet arrived. {@code since} is when the phase last changed and {@code prev} what it was before (the armour forges in and shatters out from these). */
    public record Snap(int phase, int prev, Djinn djinn, long at, long since, int left, int magoi, int assim, int trust, int lock, int cooldown, long pulseAt, int flags) {
        public boolean lent() { return (flags & 1) != 0; }
        public boolean marked() { return (flags & 2) != 0; }
        /** The Dark Djinn: flag bit 3 (value 8) of the synced state; the equip is drawn black and gold. */
        public boolean dark() { return (flags & 8) != 0; }

        /** Ticks left of the timed phase as of {@code now} (never below 0). */
        public int leftNow(long now) { return Math.max(0, left - (int) (now - at)); }
        public int lockNow(long now) { return Math.max(0, lock - (int) (now - at)); }
        public int cooldownNow(long now) { return Math.max(0, cooldown - (int) (now - at)); }
        /** 1 right after an Extreme Magic strikes, fading to 0 over 20 ticks. */
        public float pulse(long now, float partial) { return pulseAt < 0 ? 0f : Math.max(0f, 1f - (now + partial - pulseAt) / 20f); }
    }

    private static final Snap NONE = new Snap(0, 0, null, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0);
    private static final Map<Integer, Snap> BY_ENTITY = new HashMap<>();
    private static final long STALE = 70;

    /** Network handler (client thread). */
    public static void receive(DjinnNet.Sync p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        Snap old = BY_ENTITY.get(p.entity());
        Djinn dj = Djinn.byOrdinal(p.djinn() - 1);
        boolean same = old != null && old.phase() == p.phase() && old.djinn() == dj;
        long since = same ? old.since() : now;
        int prev = same ? old.prev() : old == null ? 0 : old.phase();
        long pulseAt = p.pulse() > 0 ? now - (20 - Math.min(20, p.pulse())) : (old != null && now - old.pulseAt() < 20 ? old.pulseAt() : -1);
        BY_ENTITY.put(p.entity(), new Snap(p.phase(), prev, dj, now, since, p.left(), p.magoi(), p.assim(), p.trust(), p.lock(), p.cooldown(), pulseAt, p.flags()));
        if (!same && dj != null && p.phase() != prev && Math.max(p.phase(), prev) >= 2 && !(p.phase() >= 3 && prev >= 3)) DjinnClient.burst(p.entity(), dj, p.phase() > prev, (p.flags() & 8) != 0);
    }

    /** The player's Djinn state; phase 0 when nothing is known or the last word is stale. */
    public static Snap of(int entityId) {
        Snap s = BY_ENTITY.get(entityId);
        if (s == null) return NONE;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && s.phase() > 0 && mc.level.getGameTime() - s.at() > STALE) {
            Snap off = new Snap(0, s.phase(), s.djinn(), s.at(), mc.level.getGameTime(), 0, s.magoi(), s.assim(), s.trust(), s.lock(), s.cooldown(), -1, s.flags());
            BY_ENTITY.put(entityId, off);
            return off;
        }
        return s;
    }

    /** The local player's state. */
    public static Snap local() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? NONE : of(mc.player.getId());
    }

    /** The glow colour of the player's Djinn while it is in Half Equip or higher, else 0: for the glow of a held vessel. */
    public static int glowRgb(int entityId) {
        Snap s = of(entityId);
        return s.djinn() != null && s.phase() >= 2 ? s.djinn().glow() : 0;
    }

    public static void clear() { BY_ENTITY.clear(); }
}
