package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicFlags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import com.newuniverse.nusmp.particle.fx.FxRelic;

/**
 * Wave 4: the game-bus listeners of the neck relics. All cheap: each begins with a type check or an empty-table check.
 * <ul>
 *   <li>Death: the Pendant of Echoed Souls turns a lethal blow into 1 hit point and calls its spectral warriors (once a game day).</li>
 *   <li>Incoming damage: nothing hurts a wearer guarded by their warriors; a foe under the Weakening Gaze takes 30 percent more; the
 *       Arcane Glass's armed d6 changes the wearer's next blow.</li>
 *   <li>Heal: the beetle's blessing makes every heal 50 percent larger.</li>
 *   <li>Target change: warriors never turn on a player, charmed humanoids pick no player, a guarded wearer is not targeted at all.</li>
 *   <li>Server tick: the once-a-second upkeep ({@link Wave4Upkeep}).</li>
 *   <li>Logout, dimension change, respawn, server stop: the wave's state is dropped.</li>
 * </ul>
 */
final class Wave4Handlers {
    private Wave4Handlers() {}

    static final String FLAG_ECHO = "w4_echo";
    static final String ENTITY_WARRIOR = "minecraft:vex";
    static final float WARRIOR_HEALTH = 10f;
    static final double WARRIOR_ATTACK = 5.0;
    static final int BONE = 0xD9D2BC;

    // ---------------------------------------------------------------- Pendant of Echoed Souls
    static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !RelicFlags.has(p, FLAG_ECHO)) return;
        DamageSource src = e.getSource();
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;               // the void and /kill take what they take
        ServerLevel lv = p.serverLevel();
        long now = lv.getGameTime();
        CompoundTag kept = Wave4Store.kept(p);
        long last = kept.contains(Wave4Store.K_ECHO, 4) ? kept.getLong(Wave4Store.K_ECHO) : -1L;
        if (!Wave4Echo.ready(last, now)) return;
        e.setCanceled(true);
        kept.putLong(Wave4Store.K_ECHO, now);
        p.setHealth(1f);
        p.clearFire();
        Set<UUID> warriors = new HashSet<>();
        int n = Wave4Echo.count(lv.random.nextInt(4) + 1);
        for (int i = 0; i < n; i++) {
            LivingEntity w = Fx.summon(p, ENTITY_WARRIOR, Wave4Echo.LIFE);
            if (w == null) continue;
            shape(w);
            warriors.add(w.getUUID());
        }
        Wave4Echo.start(p.getUUID(), now, warriors);
        Wave4Upkeep.shakeSkillDebuffs(p);                                          // no spell effect of Tensura's holds on you either
        if (warriors.isEmpty()) Fx.shield(p, 6f, 200);                              // no warrior could answer: a shell of bone for five seconds
        FxRelic.ring(lv, p.position().add(0, 0.3, 0), 2.5, BONE);
        FxRelic.burst(lv, p.position().add(0, 1.0, 0), BONE);
        Fx.say(p, warriors.isEmpty() ? "The pendant cracks the blow into a shell of bone: you hang on at 1 health."
                : "The pendant echoes: you hang on at 1 health and " + warriors.size() + " spectral " + (warriors.size() == 1 ? "warrior rises" : "warriors rise")
                + ". Nothing can touch you until they fall (at most a minute). It will not answer again for a day.");
    }

    /** A warrior of the pendant: 10 health, a blow of about 5. */
    private static void shape(LivingEntity w) {
        AttributeInstance hp = w.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(WARRIOR_HEALTH);
        w.setHealth(WARRIOR_HEALTH);
        AttributeInstance hit = w.getAttribute(Attributes.ATTACK_DAMAGE);
        if (hit != null) hit.setBaseValue(WARRIOR_ATTACK);
    }

    // ---------------------------------------------------------------- incoming damage
    static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        DamageSource src = e.getSource();
        long now = victim.level().getGameTime();
        if (victim instanceof ServerPlayer vp && Wave4Echo.has(vp.getUUID()) && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            ServerLevel lv = vp.serverLevel();
            if (Wave4Echo.guarded(vp.getUUID(), now, w -> Wave4Upkeep.warriorAlive(lv, w))) {
                e.setCanceled(true);
                return;
            }
        }
        if (src.getEntity() instanceof ServerPlayer ap && Wave4Glass.armed(ap.getUUID(), now)) {
            int kind = Wave4Glass.take(ap.getUUID(), now);
            if (kind != 0) e.setAmount(e.getAmount() * Wave4Glass.multiplier(kind));
        }
        if (Wave4State.GAZE.size() > 0 && Wave4State.GAZE.active(victim.getUUID(), now)) e.setAmount(e.getAmount() * Wave4CovenActs.GAZE_EXTRA);
    }

    // ---------------------------------------------------------------- the beetle
    static void onHeal(LivingHealEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Wave4Forest s = Wave4State.FOREST.get(p.getUUID());
        if (s != null) e.setAmount(e.getAmount() * Wave4Forest.healMultiplier(s));
    }

    // ---------------------------------------------------------------- who may be targeted
    static void onChangeTarget(LivingChangeTargetEvent e) {
        if (!(e.getNewAboutToBeSetTarget() instanceof Player target)) return;
        LivingEntity me = e.getEntity();
        long now = me.level().getGameTime();
        if (Wave4Echo.isWarrior(me.getUUID())) { e.setCanceled(true); return; }                   // the pendant's warriors never turn on a player
        if (Wave4State.CHARM.size() > 0 && Wave4State.CHARM.active(me.getUUID(), now)) { e.setCanceled(true); return; }
        if (target instanceof ServerPlayer sp && Wave4Echo.has(sp.getUUID())) {
            ServerLevel lv = sp.serverLevel();
            if (Wave4Echo.guarded(sp.getUUID(), now, w -> Wave4Upkeep.warriorAlive(lv, w))) e.setCanceled(true);
        }
    }

    // ---------------------------------------------------------------- upkeep and cleanup
    static void onServerTick(ServerTickEvent.Post e) { Wave4Upkeep.run(e.getServer()); }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { Wave4State.forget(e.getEntity().getUUID()); }

    static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent e) { Wave4Echo.end(e.getEntity().getUUID()); }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) { Wave4State.forget(e.getEntity().getUUID()); }

    static void onServerStopping(ServerStoppingEvent e) { Wave4State.clearAll(); }
}
