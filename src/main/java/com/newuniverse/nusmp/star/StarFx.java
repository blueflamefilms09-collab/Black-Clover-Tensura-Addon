package com.newuniverse.nusmp.star;

import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Star Magic nodes: every effect packet of this feature goes through here (docs/vfx_remake/star_ingame.md). One packet is the usual {@code VfxPayload} (about 60 bytes) sent through {@link VfxSpawn#send}, so it
 * reaches players within 96 blocks only and counts against the per-tick effect-packet budget of the server ({@code PerfBudgets.EFFECT_PACKETS}). A cast also has its own ceiling, {@link #CAST_PACKETS}, held by
 * a {@link Budget} the cast creates. The {@code flag} rides in the payload's seed (the layer reads it: a mode, a step number or a segment index).
 */
public final class StarFx {
    private StarFx() {}

    /** Star Magic's own colour (the book's pale gold); the layer draws from its own palette and only uses the alpha and the hue to tint the glows. */
    public static final int COLOR = 0xFFFFF0A0;
    public static final int CAST_PACKETS = 10;

    /** The effect packets one cast may still send. */
    public static final class Budget {
        private int left = CAST_PACKETS;

        public int left() { return left; }

        boolean take() { if (left <= 0) return false; left--; return true; }
    }

    /** The shapes this feature adds (the layer registers for exactly these). */
    public static final List<VfxShape> SHAPES = com.newuniverse.nusmp.vfx.StarV2.SHAPES;

    /** Sends one effect outside a cast budget (a pane hit): still through the per-tick server budget. */
    public static void send(ServerLevel level, VfxShape shape, Vec3 from, Vec3 to, int flag, int ticks, float power) {
        VfxSpawn.send(level, new VfxPayload(shape.ordinal(), from, to, COLOR, Math.max(4, Math.min(ticks, 120)), Math.max(0.2f, Math.min(power, 4f)), -1, flag));
    }

    /** Sends one effect of a cast; false when the cast's ten packets are spent. */
    public static boolean send(Budget budget, ServerLevel level, VfxShape shape, Vec3 from, Vec3 to, int flag, int ticks, float power) {
        if (!budget.take()) return false;
        send(level, shape, from, to, flag, ticks, power);
        return true;
    }
}
