package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.mana.method.RuneCharge.Charge;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.perf.PerfParticles;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Dash Rune: a velocity push along the look vector, working on the ground and mid-air, its strength scaled by how long the page was held ({@link MobilityRules#dashSpeed}). The rules and
 * the maths are {@link MobilityRules}. The velocity is set on the server and sent with {@code hurtMarked}, which makes the server tell the player's own client the new motion (without it
 * the push would only exist on the server and rubber-band). A cooldown of {@link MobilityRules#DASH_COOLDOWN} ticks per player (on top of the page's own), and the fall so far is forgiven
 * by {@link MobilityRules#FALL_GRACE} blocks only, never reset.
 *
 * <p>Cost: one velocity set, one packet (the vanilla motion packet), a handful of particles through the particle budget, per cast; one map entry per player who has dashed, dropped when
 * they log out.
 */
public final class Dash {
    private Dash() {}

    private static final Map<UUID, Long> LAST = new HashMap<>();
    private static final double[] V = new double[3];

    /** Dashes {@code p} at tick {@code now}; false, with a message, when the cooldown has not run out. */
    public static boolean cast(ServerPlayer p, Charge charge, long now) {
        Long last = LAST.get(p.getUUID());
        if (last != null && !MobilityRules.dashReady(now, last)) {
            GrimoireBook.fail(p, "The Dash Rune is still glowing.");
            return false;
        }
        if (p.isPassenger()) {
            GrimoireBook.fail(p, "You cannot dash from here.");
            return false;
        }
        double speed = MobilityRules.dashSpeed(ManaMethodConfig.dashStrength(), charge, ManaMethodConfig.maxHold());
        Vec3 look = p.getLookAngle();
        MobilityRules.dashVelocity(look.x, look.y, look.z, speed, p.onGround(), V);
        p.setDeltaMovement(V[0], V[1], V[2]);
        p.hurtMarked = true;                                                               // the server sends the new motion to the player's own client
        p.fallDistance = MobilityRules.fallAfterDash(p.fallDistance);
        LAST.put(p.getUUID(), now);
        p.level().playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.8f, 1.6f);
        PerfParticles.send(p.serverLevel(), Fx.options(FxThemes.WORLD, FxOld.CLOUD), p.getX(), p.getY() + 0.9, p.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
        return true;
    }

    public static int tracked() { return LAST.size(); }

    public static void cancelOwner(UUID owner) { LAST.remove(owner); }

    public static void clearAll() { LAST.clear(); }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!LAST.isEmpty() && e.getEntity() instanceof ServerPlayer p) LAST.remove(p.getUUID());
    }
}
