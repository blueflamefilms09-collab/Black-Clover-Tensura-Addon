package com.newuniverse.nusmp.client.relics;

import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.joml.Vector3f;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * The grade you can see in the world: a relic of GOD or GENESIS grade lying on the ground gives off a column of coloured motes, and whoever wears or
 * holds one shimmers softly. Everything is client-side, rare and bounded: a dropped relic spawns a particle every few ticks, and a wearer is looked at
 * only every eight ticks, within 48 blocks, at most a dozen players, through the cached look at what they wear.
 */
public final class RelicGradeFx {
    private RelicGradeFx() {}

    private static int budget;

    /** Game bus: a relic item entity on the ground (client side). */
    public static void onEntityTick(EntityTickEvent.Post e) {
        if (!(e.getEntity() instanceof ItemEntity item) || !item.level().isClientSide() || item.tickCount % 3 != 0) return;
        RelicDef def = Relics.defOf(item.getItem());
        if (def == null || !def.grade().atLeast(Grade.GOD) || budget >= 24) return;
        budget++;
        ClientLevel level = (ClientLevel) item.level();
        float t = item.tickCount * 0.35f;
        double r = 0.35 + 0.1 * Math.sin(t * 0.7);
        Fx.spray(level, FxThemes.RELIC, FxTone.LIGHT, item.getX() + Math.cos(t) * r, item.getY() + 0.2 + (item.tickCount % 24) * 0.05, item.getZ() + Math.sin(t) * r, 1, 0, 0.02, 0, 1.0);
        if (item.tickCount % 12 == 0) Fx.spray(level, FxThemes.RELIC, FxTone.LIGHT, item.getX(), item.getY() + 0.3, item.getZ(), 1, 0, 0.04, 0, 1.0);
    }

    /** Game bus, client tick: the shimmer of the people who carry one. */
    public static void onClientTick(ClientTickEvent.Post e) {
        budget = 0;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused() || level.getGameTime() % 8 != 0) return;
        int looked = 0;
        for (AbstractClientPlayer p : level.players()) {
            if (p.isSpectator() || p.isInvisible() || p.distanceToSqr(mc.player) > 48 * 48 || ++looked > 12) continue;
            Grade top = null;
            for (Relics.Worn w : Relics.wornList(p)) top = Grade.max(top, w.def().grade());
            if (top == null || !top.atLeast(Grade.GOD)) continue;
            double a = level.random.nextDouble() * Math.PI * 2;
            Fx.spray(level, FxThemes.RELIC, FxTone.LIGHT, p.getX() + Math.cos(a) * 0.5, p.getY() + 0.2 + level.random.nextDouble() * 1.6, p.getZ() + Math.sin(a) * 0.5, 1, 0, 0.015, 0, 1.0);
            if (top == Grade.GENESIS && level.random.nextInt(3) == 0) Fx.spray(level, FxThemes.RELIC, FxTone.LIGHT, p.getX() + Math.cos(a) * 0.6, p.getY() + 1.0 + level.random.nextDouble(), p.getZ() + Math.sin(a) * 0.6, 1, 0, 0.02, 0, 1.0);
        }
    }

}
