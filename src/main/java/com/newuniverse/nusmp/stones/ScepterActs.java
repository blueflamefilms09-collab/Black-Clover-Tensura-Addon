package com.newuniverse.nusmp.stones;

import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: Loki's Scepter holds the Mind Stone only. Its own two acts are the energy blast (one bolt at the foe in sight, through the balance law as a GOD-class hit, players capped at 30%) and the barrier
 * (a few absorption hearts for ten seconds). The Mind Stone's other four acts (Psionic Lance, Psionic Reach, Telekinesis, Mind Link) are cast through the scepter by {@link CarrierKit}. No mind
 * control: the touch control of the comics' scepter has no act here (owner decision). Server side; one target, nothing spawned.
 */
final class ScepterActs {
    private ScepterActs() {}

    static void blast(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity t = StoneActs.aim(p, CarrierRules.BLAST_RANGE);
        if (t == null) {
            Fx.say(p, "The scepter finds nothing in sight.");
            return;
        }
        StoneActs.hit(p, t, (float) (CarrierRules.BLAST_DAMAGE * StoneSlot.scale(p)));
        Vec3 from = p.getEyePosition();
        Vec3 to = t.getBoundingBox().getCenter();
        for (int i = 1; i <= 4; i++) StoneActs.flash(p, InfinityStone.MIND, from.add(to.subtract(from).scale(i / 4.0)), 0.8);
    }

    static void barrier(ActCtx c) {
        ServerPlayer p = c.player();
        Fx.shield(p, (float) (CarrierRules.BARRIER_ABSORB * StoneSlot.scale(p)), CarrierRules.BARRIER_TICKS);
        StoneActs.flash(p, InfinityStone.MIND, p.position().add(0, 1.0, 0), 2.5);
        Fx.say(p, "A pale barrier closes round you.");
    }
}
