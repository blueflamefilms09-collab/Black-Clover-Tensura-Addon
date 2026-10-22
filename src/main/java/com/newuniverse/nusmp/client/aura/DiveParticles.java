package com.newuniverse.nusmp.client.aura;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.book.SpiritDivePieces;
import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDiveTier;
import com.newuniverse.nusmp.book.SpiritDiveTier.Grade;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.perf.PerfProbe;
import com.newuniverse.nusmp.particle.fx.FxSlot;
import com.newuniverse.nusmp.particle.fx.FxTone;
import com.newuniverse.nusmp.particle.fx.ThemeKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.List;

/**
 * Spirit Dive remake: the particle trail of a dive, spawned on the CLIENT from the lease the server already sent (no packet of its own): the custom particles of the theme of the dive
 * ({@code spirit_dive.<wind|fire|water|earth|yuno|fuegoleon|noelle>} in data/nusmp/particle_themes.json, written by tools/particles/gen_themes.py), thrown round the body with the motion of the
 * element (Wind swirls, Fire rises, Water drifts out, Earth lifts as dust). Bounded three times over: only dives within {@link SpiritDiveTier#TRAIL_RANGE} blocks, at most
 * {@link SpiritDiveTier#TRAIL_PLAYERS} dives per tick ({@link SpiritDiveTier#trailCount}: one every third tick for the standard tier, two a tick for the character tier), and every particle goes
 * through {@code Fx} (the client particle budget {@code PerfBudgets.clientParticles}, the effect quality and the distance bands). On the tick after a dive begins a burst of
 * {@link SpiritDiveTier#entryBurst} particles marks the entry. The viewer can switch it off ({@code spiritDive.particles}). No allocation but the particles.
 */
public final class DiveParticles {
    private DiveParticles() {}

    private static final ThemeKey[] STANDARD = {ThemeKey.of("spirit_dive.wind"), ThemeKey.of("spirit_dive.water"), ThemeKey.of("spirit_dive.fire"), ThemeKey.of("spirit_dive.earth")};
    private static final PerfProbe.Section SECTION = PerfProbe.section("c.spiritdive.particles");   // timed (/multiverse perf)
    private static final ThemeKey YUNO = ThemeKey.of("spirit_dive.yuno"), FUEGOLEON = ThemeKey.of("spirit_dive.fuegoleon"), NOELLE = ThemeKey.of("spirit_dive.noelle");

    private static ThemeKey theme(Element el, Grade g) {
        return switch (g) {
            case YUNO -> YUNO;
            case FUEGOLEON -> FUEGOLEON;
            case NOELLE -> NOELLE;
            case STANDARD -> STANDARD[el.ordinal()];
        };
    }

    /** Game bus, client: once per tick. */
    public static void onClientTick(ClientTickEvent.Post e) {
        if (!SpiritDiveLook.particles()) return;
        long t0 = PerfProbe.begin();
        try {
            trail();
        } finally {
            PerfProbe.end(SECTION, t0);
        }
    }

    private static void trail() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused()) return;
        List<AbstractClientPlayer> players = level.players();
        if (players.isEmpty()) return;
        long now = level.getGameTime();
        double range2 = SpiritDiveTier.TRAIL_RANGE * SpiritDiveTier.TRAIL_RANGE;
        int served = 0;
        for (int i = 0, n = players.size(); i < n && served < SpiritDiveTier.TRAIL_PLAYERS; i++) {
            AbstractClientPlayer p = players.get(i);
            var running = PlayerAuraClient.running(p.getId());
            long[] w = running == null ? null : running.get(Aura.SPIRIT_DIVE);
            if (w == null || now >= w[1] || p.isSpectator() || p.distanceToSqr(mc.player) > range2) continue;
            served++;
            int style = (int) w[2];
            Element el = SpiritDivePieces.element(style);
            Grade g = SpiritDivePieces.grade(style);
            boolean character = SpiritDiveTier.character(el, g) && SpiritDiveLook.characterEffects();
            ThemeKey key = theme(el, character ? g : Grade.STANDARD);
            if (now - w[0] == 1L) Fx.burst(level, key, p.getX(), p.getY() + 1.0, p.getZ(), FxTone.LIGHT, SpiritDiveTier.entryBurst(character));
            int count = SpiritDiveTier.trailCount(character, now, p.getId());
            RandomSource r = level.getRandom();
            for (int k = 0; k < count; k++) spawn(level, key, el, p, r, k);
        }
    }

    private static void spawn(ClientLevel level, ThemeKey key, Element el, AbstractClientPlayer p, RandomSource r, int k) {
        float a = r.nextFloat() * Mth.TWO_PI;
        double rad = 0.3 + r.nextDouble() * 0.5, h = r.nextDouble() * 1.9;
        double x = p.getX() + Mth.cos(a) * rad, y = p.getY() + h, z = p.getZ() + Mth.sin(a) * rad;
        double vx, vy, vz;
        switch (el) {
            case WIND -> { vx = -Mth.sin(a) * 0.07; vz = Mth.cos(a) * 0.07; vy = 0.03 + r.nextDouble() * 0.03; }                 // swirls round the body
            case FIRE -> { vx = Mth.cos(a) * 0.012; vz = Mth.sin(a) * 0.012; vy = 0.05 + r.nextDouble() * 0.06; }                // rises
            case WATER -> { vx = Mth.cos(a) * 0.03; vz = Mth.sin(a) * 0.03; vy = 0.02 + r.nextDouble() * 0.03; }                 // drifts out
            default -> { vx = Mth.cos(a) * 0.01; vz = Mth.sin(a) * 0.01; vy = 0.02 + r.nextDouble() * 0.02; }                    // earth: dust lifts
        }
        Fx.spray(level, key, (k + r.nextInt(3)) % 3 == 0 ? FxSlot.ACCENT : FxSlot.MOTE, x, y, z, 1, vx, vy, vz, 1.0);
    }
}
