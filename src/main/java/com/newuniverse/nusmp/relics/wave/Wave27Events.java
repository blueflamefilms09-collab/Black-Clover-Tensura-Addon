package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.RelicFlags;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Why: the game-bus handlers of wave 27, registered once by {@link Wave27#hooks}. Each leaves at once for anyone the wave does not concern (the
 * flag {@link Wave27Rules#F_ANY} is one cached lookup) and nothing here scans the world on a tick.
 */
final class Wave27Events {
    private Wave27Events() {}

    /** Once a second per player (staggered by entity id): the mask clock for everyone, then the wearer's upkeep. */
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isAlive()) return;
        int phase = (p.tickCount + p.getId()) % 20;
        if (phase == 6) Wave27Head.clocks(p);
        if (phase != 11 || !RelicFlags.has(p, Wave27Rules.F_ANY)) return;
        Wave27Head.pass(p);
        if (RelicFlags.has(p, Wave27Rules.F_STORM)) Wave27Hands.stormPass(p);
    }

    /** Before armour: the wearer's bare-handed blows get their bonuses; the dragon gauntlet saves from blasts; the gloves turn a fumble into a theft. */
    static void onIncoming(LivingIncomingDamageEvent e) {
        DamageSource src = e.getSource();
        LivingEntity victim = e.getEntity();
        if (src.getEntity() instanceof ServerPlayer a && RelicFlags.has(a, Wave27Rules.F_ANY)) Wave27Hands.bareBlow(a, victim, src, e);
        if (!(victim instanceof ServerPlayer v) || !RelicFlags.has(v, Wave27Rules.F_ANY)) return;
        if (RelicFlags.has(v, Wave27Rules.F_DRAGON)) Wave27Hands.dragonSave(v, e);
        if (!e.isCanceled() && RelicFlags.has(v, Wave27Rules.F_OPPORTUNITY)) Wave27Hands.opportunity(v, e);
    }

    /** The undead do not pick out a masked wearer: they see one of their own. */
    static void onChangeTarget(LivingChangeTargetEvent e) {
        LivingEntity t = e.getNewAboutToBeSetTarget();
        if (t instanceof ServerPlayer p && e.getEntity() instanceof Mob m && Wave27Head.maskHides(m, p)) e.setCanceled(true);
    }

    static void onWake(PlayerWakeUpEvent e) {
        if (e.wakeImmediately() || !(e.getEntity() instanceof ServerPlayer p) || !RelicFlags.has(p, Wave27Rules.F_CROWN)) return;
        Wave27Head.crownWake(p);
    }

    /** A compliment in chat that names the wearer of the Glove of Appreciation (read on the server thread). */
    static void onChat(ServerChatEvent e) {
        ServerPlayer from = e.getPlayer();
        String line = e.getRawText();
        if (from == null || from.getServer() == null || line == null || line.length() > 256) return;
        from.getServer().execute(() -> {
            for (ServerPlayer w : from.getServer().getPlayerList().getPlayers()) {
                if (w == from || !w.isAlive() || !RelicFlags.has(w, Wave27Rules.F_GLOVE)) continue;
                if (Wave27Rules.compliment(line, w.getGameProfile().getName())) Wave27Hands.appreciate(w, from.getUUID(), from.getName().getString() + " paid you a compliment.");
            }
        });
    }

    /** Winning an animal over also counts as being appreciated (each animal once per gap). */
    static void onTame(AnimalTameEvent e) {
        if (e.getTamer() instanceof ServerPlayer p && RelicFlags.has(p, Wave27Rules.F_GLOVE)) Wave27Hands.appreciate(p, e.getAnimal().getUUID(), "The " + e.getAnimal().getName().getString() + " trusts you.");
    }
}
