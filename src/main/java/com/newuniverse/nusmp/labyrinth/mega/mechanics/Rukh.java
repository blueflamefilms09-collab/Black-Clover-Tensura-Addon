package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import com.newuniverse.nusmp.particle.rukh.RukhFx;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the owner's rule is that the dungeon spawns only Light and Dark Rukh particles (vanilla particles are purged). Every visual of a mechanic goes through this one helper,
 * which picks the alignment (Light on the Holy road, Dark on the Abyssal one, or as the data says) and the shape (a burst, a ring, a column, an aura on the players), and which
 * inherits the per-tick particle budget of {@link RukhFx}, so a thousand mechanics can never flood the packet stream.
 */
final class Rukh {
    private Rukh() {}

    static RukhFx.Alignment align(Instance in, String how) {
        if ("light".equals(how)) return RukhFx.Alignment.LIGHT;
        if ("dark".equals(how)) return RukhFx.Alignment.DARK;
        return in.floor < 0 ? RukhFx.Alignment.DARK : RukhFx.Alignment.LIGHT;
    }

    static void burst(Instance in, String align, String shape, int count) {
        at(in, in.anchor, align, shape, count);
    }

    static void at(Instance in, BlockPos pos, String align, String shape, int count) {
        RukhFx.Alignment a = align(in, align);
        int n = Math.max(1, Math.min(12, count));
        Vec3 c = Vec3.atCenterOf(pos).add(0, 0.5, 0);
        switch (shape) {
            case "ring" -> {
                for (int i = 0; i < 6; i++) {
                    double ang = i * Math.PI / 3.0;
                    RukhFx.burst(in.level, c.add(Math.cos(ang) * 3.0, 0.2, Math.sin(ang) * 3.0), a, Math.max(1, n / 5));
                }
            }
            case "column" -> {
                for (int i = 0; i < 4; i++) RukhFx.burst(in.level, c.add(0, i * 1.2, 0), a, Math.max(1, n / 4));
            }
            case "aura" -> {
                for (ServerPlayer p : in.inside) if (Safety.affects(p)) RukhFx.aura(in.level, p, a);
            }
            default -> RukhFx.burst(in.level, c, a, n);
        }
    }
}
