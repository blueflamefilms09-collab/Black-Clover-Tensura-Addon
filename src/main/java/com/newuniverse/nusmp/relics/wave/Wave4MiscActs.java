package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Wave 4: the acts of the two simpler amulets.
 * <ul>
 *   <li><b>Voice of the Mountain, Rousing Call</b>: a note on the horn. You, your tamed creatures and friendly players (same team) within
 *       24 blocks get Speed and Haste for 20 s, and each player there draws 3 percent of their maximum magicules back (Tensura stat). (The card widens a bard's reach by 25 feet; there are no bards here, so the horn carries
 *       the bard's inspiration itself, at the widened reach.)</li>
 *   <li><b>Amulet of the Sea of Stars, Star-Eyed</b>: Truesight for five minutes: invisible creatures within 24 blocks are outlined,
 *       blindness and darkness cannot hold you, and Tensura's Magic Sense and Sage are lent to you for the length of it. Once per game day.</li>
 * </ul>
 */
final class Wave4MiscActs {
    private Wave4MiscActs() {}

    static final double ROUSE_RADIUS = 24.0;
    static final int ROUSE_TICKS = 400;
    static final int ROUSE_MAX = 16;
    static final int EMERALD = 0x2EC27E;
    static final int AMETHYST = 0xA855F7;
    static final int SIGHT_TICKS = 6000;
    /** The share of maximum magicules the horn gives back to each player it rouses. */
    static final double INSPIRE = 0.03;

    static void rouse(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel lv = p.serverLevel();
        List<LivingEntity> near = lv.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(ROUSE_RADIUS), e -> e.isAlive() && ally(p, e));
        int n = 0;
        for (LivingEntity e : near) {
            if (n++ >= ROUSE_MAX) break;
            Fx.addEffect(e, "minecraft:speed", ROUSE_TICKS, 0);
            Fx.addEffect(e, "minecraft:haste", ROUSE_TICKS, 0);
            if (e instanceof ServerPlayer ally) inspire(ally);
        }
        Fx.ring(lv, p.position().add(0, 0.3, 0), ROUSE_RADIUS * 0.5, EMERALD);
        Fx.burst(lv, p.position().add(0, 1.6, 0), EMERALD);
        Fx.say(p, "The horn sounds: " + Math.min(n, ROUSE_MAX) + (n == 1 ? " ally is" : " allies are") + " roused (Speed and Haste for 20 seconds; players also draw " + (int) (INSPIRE * 100) + "% of their magicules back).");
    }

    /** The bard's inspiration, Tensura style: a share of the player's maximum magicules flows back into their pool. */
    static void inspire(ServerPlayer p) {
        try {
            var ex = TensuraStorages.getExistenceFrom(p);
            if (ex == null) return;
            double max = EnergyHelper.getMaxMagicule(p);
            if (max <= 0) return;
            ex.setMagicule(Math.min(max, ex.getMagicule() + max * INSPIRE));
            ex.markDirty();
        } catch (RuntimeException | LinkageError noTensura) {
            // no Tensura storage on this player: the Speed and Haste still came
        }
    }

    static boolean ally(ServerPlayer p, LivingEntity e) {
        if (e == p) return true;
        if (e instanceof OwnableEntity own && p.getUUID().equals(own.getOwnerUUID())) return true;
        return e instanceof Player pl && !pl.isSpectator() && pl.isAlliedTo(p);
    }

    static void starEyed(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel lv = p.serverLevel();
        long now = lv.getGameTime();
        Wave4State.SIGHT.put(p.getUUID(), now + SIGHT_TICKS, now);
        Wave4Upkeep.sight(p, now, true);
        Fx.ring(lv, p.position().add(0, 1.6, 0), 1.0, AMETHYST);
        Fx.say(p, "Star-Eyed: Truesight for five minutes. What hides from the dark cannot hide from you.");
    }
}
