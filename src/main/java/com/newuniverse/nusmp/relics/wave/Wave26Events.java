package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.RelicFlags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Why: the game-bus handlers of wave 26, registered once by {@link Wave26#hooks}. Each leaves at once for anyone the wave does not concern
 * (the flag {@link Wave26Rules#F_ANY} is one cached lookup), nothing scans the world on a tick, and the work of a second is staggered by entity id.
 */
final class Wave26Events {
    private Wave26Events() {}

    private static final String ANGRY = "nusmp_w26_angry";

    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isAlive()) return;
        int phase = (p.tickCount + p.getId()) % 20;
        if (phase == 3) Wave26Wear.pass(p);
        else if ((phase == 8 || phase == 18) && RelicFlags.has(p, Wave26Rules.F_JELLY)) Wave26Wear.jelly(p);
    }

    static void onServerTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 20 == 7) Wave26Mind.drive(e.getServer());
    }

    /** Frank's disadvantage and Foresight on blows, the jellyfish's wrath, and the guardians the jellyfish cap befriends. */
    static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        if (e.getSource().getEntity() instanceof ServerPlayer p && victim != p) {
            float f = Wave26Frank.outFactor(p);
            if (f != 1.0f) e.setAmount(e.getAmount() * f);
            if (victim instanceof Guardian g && RelicFlags.has(p, Wave26Rules.F_JELLY)) g.getPersistentData().putBoolean(ANGRY, true);
        }
        if (victim instanceof ServerPlayer v) {
            float f = Wave26Frank.inFactor(v);
            if (f != 1.0f) e.setAmount(e.getAmount() * f);
            if (RelicFlags.has(v, Wave26Rules.F_JELLY) && e.getSource().getEntity() instanceof LivingEntity attacker) Wave26Wear.wrath(v, attacker);
        }
    }

    /** Dull News: the creatures the Mask of Indifference's wearer strikes do not hold it against him. */
    static void onDamagePost(LivingDamageEvent.Post e) {
        if (e.getSource().getEntity() instanceof ServerPlayer p && RelicFlags.has(p, Wave26Rules.F_INDIFF)) Wave26Wear.dullNews(e.getEntity());
    }

    /** A charmed creature never turns on its charmer; a guardian that was not harmed does not choose a friend of the jellyfish. */
    static void onChangeTarget(LivingChangeTargetEvent e) {
        LivingEntity target = e.getNewAboutToBeSetTarget();
        if (target == null) return;
        LivingEntity mob = e.getEntity();
        if (Wave26Mind.charmedBy(mob, target)) {
            e.setCanceled(true);
            return;
        }
        if (mob instanceof Guardian g && target instanceof ServerPlayer p && !g.getPersistentData().getBoolean(ANGRY) && RelicFlags.has(p, Wave26Rules.F_JELLY)) e.setCanceled(true);
    }

    static void onAttack(AttackEntityEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) Wave26Frank.onStrike(p, e.getTarget());
    }

    static void onToss(ItemTossEvent e) {
        if (e.getPlayer() instanceof ServerPlayer p) Wave26Frank.onToss(p);
    }

    static void onUseFinish(LivingEntityUseItemEvent.Finish e) {
        if (e.getEntity() instanceof ServerPlayer p) Wave26Frank.onEat(p, e.getItem());
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            Wave26Mind.forget(p.getServer(), p.getUUID());
            Wave26Frank.forget(p);
            Wave26Wear.forget(p);
        }
    }

    static void onStopping(ServerStoppingEvent e) {
        Wave26Mind.clearAll(e.getServer());
        Wave26Frank.clearAll(e.getServer());
        Wave26Wear.clearAll();
    }
}
