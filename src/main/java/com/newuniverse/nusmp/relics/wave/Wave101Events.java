package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.particle.fx.FxRelic;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicFlags;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Why: the passives of wave 101 that need a game event. One damage listener (the Necklace's foresight cut and the Puzzle's second soul on the wearer; the marks and the weapon rules on
 * the attacker) and one player tick (once a second, staggered by entity id: the set's ambient gifts, the Key's shield against the mind, the Ring's needles). Every handler runs under {@link SafeCast#tick};
 * the per-player timers live in the player's persistent data (game time, so they survive relog and death) and the only table in memory, {@link Wave101Marks}, is bounded and cleared when the server stops.
 * The blows of an act are the act's own ({@link Wave101Acts#running}): no mark or weapon rule is added on top of them.
 */
final class Wave101Events {
    private Wave101Events() {}

    static final String SOUL = "nusmp_w101_soul", FORESIGHT = "nusmp_w101_foresight";

    /** 1.112.0 (lag audit): the cost of this wave's two handlers (off by default: a flag read). */
    private static final com.newuniverse.nusmp.perf.PerfProbe.Section PROBE = com.newuniverse.nusmp.perf.PerfProbe.section("s.relic.w101");

    static void register() {
        NeoForge.EVENT_BUS.addListener(Wave101Events::onIncoming);
        NeoForge.EVENT_BUS.addListener(Wave101Events::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Wave101Events::onStopping);
    }

    // ================================================================ damage
    /** The flag key of each weapon with a rule, built once ("w101_" + id was a string concatenation on every swing of such a weapon). */
    private static final java.util.Map<String, String> FLAGS = new java.util.HashMap<>();

    static {
        for (String id : Wave101Passives.RULES.keySet()) FLAGS.put(id, "w101_" + id);
    }

    static void onIncoming(LivingIncomingDamageEvent e) {
        long t0 = com.newuniverse.nusmp.perf.PerfProbe.begin();
        try {
            onIncoming0(e);
        } finally {
            com.newuniverse.nusmp.perf.PerfProbe.end(PROBE, t0);
        }
    }

    private static void onIncoming0(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        // 1.112.0 (lag audit): this listener sees every hit of every mob in the world; only a hit that involves a player is worth a guard and a closure
        if (!(victim instanceof ServerPlayer) && !(e.getSource().getEntity() instanceof ServerPlayer)) return;
        if (victim.level().isClientSide()) return;
        SafeCast.tick("relic.w101.events", () -> {
            if (victim instanceof ServerPlayer p) wearer(e, p);
            DamageSource src = e.getSource();
            if (src.getEntity() instanceof ServerPlayer atk && !e.isCanceled()) striker(e, atk, victim, src);
        });
    }

    /** The wearer takes a blow: the Necklace foresees it, and the Puzzle's second soul takes the deadly one. */
    private static void wearer(LivingIncomingDamageEvent e, ServerPlayer p) {
        if (Relics.loadout(p).flags().isEmpty()) return;                                          // no relic with a flag works for this player: neither the Necklace nor the Puzzle
        long now = p.level().getGameTime();
        CompoundTag data = p.getPersistentData();
        if (RelicFlags.has(p, "w101_necklace") && now >= data.getLong(FORESIGHT) && e.getAmount() >= Wave101Rules.FORESIGHT_MIN) {
            e.setAmount(Wave101Rules.foreseen(e.getAmount()));
            data.putLong(FORESIGHT, now + Wave101Rules.FORESIGHT_COOLDOWN);
            FxRelic.ring(p.serverLevel(), p.position(), 1.6, 0xE8C050);
        }
        if (RelicFlags.has(p, "w101_puzzle") && now >= data.getLong(SOUL) && Wave101Rules.fatal(e.getAmount(), p.getHealth(), p.getAbsorptionAmount())) {
            e.setCanceled(true);
            data.putLong(SOUL, now + Wave101Rules.SOUL_COOLDOWN);
            p.setHealth(Wave101Rules.soulHealth(p.getMaxHealth(), p.getHealth()));
            Fx.addEffect(p, "minecraft:resistance", 60, 3);
            Fx.shield(p, 40f, 100);
            Wave101Acts.clearMind(p);
            ServerLevel l = p.serverLevel();
            FxRelic.ring(l, p.position(), 4.0, 0xE8C050);
            FxRelic.burst(l, p.position().add(0, 1, 0), 0x6A8CFF);
            Fx.say(p, "The other soul took the blow for you. It will not do it again for five minutes.");
        }
    }

    /** The attacker strikes: the marks they left, and the rule of the weapon in their hand. */
    private static void striker(LivingIncomingDamageEvent e, ServerPlayer atk, LivingEntity victim, DamageSource src) {
        if (Wave101Acts.running() || Wave71Acts.running()) return;
        long now = atk.level().getGameTime();
        double marks = Wave101Marks.bonus(victim, atk.getUUID(), now);
        if (marks > 0.0) e.setAmount((float) (e.getAmount() * (1.0 + marks)));
        String id = Wave20Util.heldId(atk);
        Wave71Rules.Rule rule = Wave101Passives.RULES.get(id);
        if (rule == null || !RelicFlags.has(atk, FLAGS.get(id))) return;
        boolean melee = src.getDirectEntity() == atk && !src.is(DamageTypeTags.IS_PROJECTILE);
        if (!melee) return;
        Wave71Rules.Facts f = Wave71Events.facts(atk, victim, atk.serverLevel());
        double roll = atk.getRandom().nextDouble();
        double factor = Wave71Rules.factor(rule, f, roll);
        if (factor != 1.0) e.setAmount((float) (e.getAmount() * factor));
        if (Wave71Rules.lands(rule, f, roll) && !rule.effect().isEmpty() && rule.secs() > 0) Fx.addEffect(victim, rule.effect(), rule.secs() * 20, rule.amp());
        float back = Wave71Rules.steal(rule, e.getAmount());
        if (back > 0f) Fx.heal(atk, back);
    }

    // ================================================================ the player tick
    private static void onPlayerTick(PlayerTickEvent.Post e) {
        long t0 = com.newuniverse.nusmp.perf.PerfProbe.begin();
        try {
            onPlayerTick0(e);
        } finally {
            com.newuniverse.nusmp.perf.PerfProbe.end(PROBE, t0);
        }
    }

    private static void onPlayerTick0(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isAlive() || p.isSpectator()) return;
        if ((p.tickCount + p.getId()) % 20 != 0) return;
        // 1.112.0 (lag audit): a player with nothing that works for them costs the cached loadout look and nothing else (no closure, no guard, no set count)
        if (Relics.loadout(p).active().isEmpty()) {
            if (p.tickCount % 200 < 20) Wave101Marks.purge(p.level().getGameTime());
            return;
        }
        SafeCast.tick("relic.w101.tick", () -> {
            Wave101Set.tick(p);
            if (RelicFlags.has(p, "w101_key")) Wave101Acts.clearMind(p);
            if (RelicFlags.has(p, "w101_ring")) needles(p);
            if (p.tickCount % 200 < 20) Wave101Marks.purge(p.level().getGameTime());
        });
    }

    /** The Ring's needles: a monster that hunts the wearer glows for a few seconds, through walls, so it is never a surprise. */
    private static void needles(ServerPlayer p) {
        int n = 0;
        for (LivingEntity t : Fx.foesAround(p, 24.0)) {
            if (n >= 8) break;
            if (t instanceof Mob m && m.getTarget() == p && !(t instanceof Player)) {
                Fx.addEffect(t, "minecraft:glowing", 60, 0);
                n++;
            }
        }
    }

    private static void onStopping(ServerStoppingEvent e) { Wave101Marks.clear(); }
}
