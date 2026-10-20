package com.newuniverse.nusmp.labyrinth.mega.decennial;

import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * Why: every Decennial boss has ONE signature mechanic, the thing a party says about it afterwards ("the one that puts up a shield every twenty seconds", "the one that steps behind you"). Fourteen
 * kinds cover the roster ({@code DecennialVocab.SIGNATURE_KINDS}); the table says which kind a signature is and its two numbers (a power and a period), and this class is the implementation of all
 * fourteen, one small branch each, so a profile can never name a mechanic the code lacks. Each is bounded by the same rules as the lair actions: a hit is a share of the boss's own base attack
 * after the player's hit cap, a thing that arrives is shown first (a ring, a mark), and nothing here touches a food bar. The state is runtime only (a reload restarts the timers, which is the
 * kind direction to be wrong in).
 */
final class Signatures {
    private final DecennialVocab.Signature sig;
    private long nextAt = -1, armedAt = -1;
    private boolean enraged;
    private UUID armedTarget;
    private double armedX, armedY, armedZ;
    private final Set<UUID> far = new HashSet<>();
    private final List<long[]> echoes = new ArrayList<>();                 // {due tick}, with the target kept in the parallel list
    private final List<UUID> echoTargets = new ArrayList<>();

    Signatures(DecennialVocab.Signature sig) { this.sig = sig; }

    DecennialVocab.Signature def() { return sig; }

    /** The extra damage factor this signature gives (the enrage). */
    double damageBonus() { return enraged ? 1.0 + sig.power() : 1.0; }

    void reset() {
        nextAt = -1;
        armedAt = -1;
        enraged = false;
        armedTarget = null;
        far.clear();
        echoes.clear();
        echoTargets.clear();
    }

    // ================================================================ the tick
    /** Called every tick of a fight (and not during a phase shift). {@code fightTicks} is how long the fight has lasted. */
    void tick(LabyrinthGuardianEntity g, ServerLevel sl, long now, List<Player> players, int addsAlive, long fightTicks) {
        if (players.isEmpty()) return;
        long period = Math.max(40L, sig.period() * 20L);
        switch (sig.kind()) {
            case "shield_pulse" -> {
                if (due(now, period)) {
                    g.addShield((float) (g.getMaxHealth() * sig.power()));
                    DecennialFx.ring(sl, Fx.options(FxThemes.DUNGEON_DECENNIAL, FxOld.END_ROD), g.getX(), g.getY() + 1.0, g.getZ(), 1.6 * Math.max(1.0, g.getScale() * 0.5), 20);
                    sl.playSound(null, g.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.0f, 1.6f);
                }
            }
            case "enrage" -> {
                if (!enraged && fightTicks >= period) {
                    enraged = true;
                    notify(sl, g, "enrage");
                    sl.playSound(null, g.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.6f, 0.6f);
                }
            }
            case "blink_strike" -> blink(g, sl, now, players, period);
            case "frost_aura", "scorch_aura", "venom_aura" -> {
                if (due(now, Math.max(20L, sig.period() * 20L))) aura(g, sl, players);
            }
            case "gravity_well" -> gravityWell(g, sl, now, players, period);
            case "kite_ward" -> {
                if (due(now, period)) kiteWard(g, sl, players);
            }
            case "mana_leech" -> {
                if (due(now, Math.max(40L, sig.period() * 20L))) manaLeech(g, sl, players);
            }
            case "echo_strike" -> echoes(g, sl, now);
            default -> { }
        }
    }

    private boolean due(long now, long period) {
        if (nextAt < 0) nextAt = now + period / 2;
        if (now < nextAt) return false;
        nextAt = now + period;
        return true;
    }

    private static void notify(ServerLevel sl, LabyrinthGuardianEntity g, String key) {
        for (Player p : sl.getEntitiesOfClass(Player.class, g.getBoundingBox().inflate(48.0), Player::isAlive)) {
            p.displayClientMessage(Component.translatableWithFallback("message.nusmp.mega.decennial.signature." + key, key.equals("enrage") ? "%1$s is enraged!" : "", g.getDisplayName()), true);
        }
    }

    // ================================================================ blink: marked, then behind you
    private void blink(LabyrinthGuardianEntity g, ServerLevel sl, long now, List<Player> players, long period) {
        if (armedTarget != null) {
            if (now % 4 == 0) DecennialFx.ring(sl, Fx.options(FxThemes.DUNGEON_DECENNIAL, FxOld.PORTAL), armedX, armedY + 0.1, armedZ, 1.4, 14);
            if (now < armedAt) return;
            Player t = null;
            for (Player p : players) if (p.getUUID().equals(armedTarget)) t = p;
            armedTarget = null;
            if (t == null) return;
            double tx = armedX, ty = armedY, tz = armedZ;
            if (g.insideLeash(tx, tz) && sl.noCollision(g, g.getBoundingBox().move(tx - g.getX(), ty - g.getY(), tz - g.getZ()))) {
                DecennialFx.ring(sl, Fx.options(FxThemes.DUNGEON_DECENNIAL, FxOld.REVERSE_PORTAL), g.getX(), g.getY() + 1.0, g.getZ(), 1.2, 16);
                g.teleportTo(tx, ty, tz);
                g.getNavigation().stop();
                g.setTarget(t);
                sl.playSound(null, g.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.3f, 0.5f);
            }
            return;
        }
        if (!due(now, period)) return;
        Player t = players.get(sl.random.nextInt(players.size()));
        if (g.distanceToSqr(t) < 36.0) return;                          // already in its face: no need
        double dx = g.getX() - t.getX(), dz = g.getZ() - t.getZ(), d = Math.max(0.5, Math.hypot(dx, dz));
        armedTarget = t.getUUID();
        armedAt = now + 24;                                             // 1.2 s of warning, at the point it will land on
        armedX = t.getX() + dx / d * 2.0;
        armedY = t.getY();
        armedZ = t.getZ() + dz / d * 2.0;
    }

    // ================================================================ auras
    private void aura(LabyrinthGuardianEntity g, ServerLevel sl, List<Player> players) {
        double r = 6.0 * Math.max(1.0, g.getScale() * 0.6);
        var type = switch (sig.kind()) {
            case "frost_aura" -> Fx.options(FxThemes.DUNGEON_DECENNIAL, FxOld.SNOWFLAKE);
            case "scorch_aura" -> Fx.options(FxThemes.DUNGEON_DECENNIAL, FxOld.FLAME);
            default -> Fx.options(FxThemes.DUNGEON_DECENNIAL, FxOld.HAPPY_VILLAGER);
        };
        DecennialFx.ring(sl, type, g.getX(), g.getY() + 0.3, g.getZ(), r, 24);
        for (Player p : players) {
            if (g.distanceToSqr(p) > r * r) continue;
            switch (sig.kind()) {
                case "frost_aura" -> DecennialFx.effect(g, sl, p, DecennialVocab.Effect.FREEZE, 60, g.getX(), g.getZ());
                case "scorch_aura" -> {
                    DecennialFx.effect(g, sl, p, DecennialVocab.Effect.IGNITE, 60, g.getX(), g.getZ());
                    DecennialFx.hurt(g, p, DecennialFx.baseAttack(g) * 0.04, true);
                }
                default -> DecennialFx.effect(g, sl, p, DecennialVocab.Effect.POISON, 80, g.getX(), g.getZ());
            }
        }
    }

    // ================================================================ gravity well: shown, then everyone is dragged in
    private void gravityWell(LabyrinthGuardianEntity g, ServerLevel sl, long now, List<Player> players, long period) {
        if (armedAt >= 0) {
            if (now % 4 == 0) DecennialFx.ring(sl, Fx.options(FxThemes.DUNGEON_DECENNIAL, FxOld.PORTAL), g.getX(), g.getY() + 0.2, g.getZ(), 12.0, 40);
            if (now < armedAt) return;
            armedAt = -1;
            for (Player p : players) {
                if (g.distanceToSqr(p) > 144.0) continue;
                DecennialFx.push(p, g.getX(), g.getZ(), -0.95, 0.2);
                if (g.distanceToSqr(p) < 25.0) DecennialFx.hurt(g, p, DecennialFx.baseAttack(g) * 0.12, false);
            }
            sl.playSound(null, g.blockPosition(), SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 1.4f, 0.4f);
            return;
        }
        if (due(now, period)) armedAt = now + 20;
    }

    // ================================================================ the ward against kiting
    private void kiteWard(LabyrinthGuardianEntity g, ServerLevel sl, List<Player> players) {
        Set<UUID> now = new HashSet<>();
        for (Player p : players) {
            if (g.distanceToSqr(p) <= 14.0 * 14.0) continue;
            now.add(p.getUUID());
            if (!far.contains(p.getUUID())) continue;                   // the first time it is only a mark; the ward strikes someone who stays far
            DecennialFx.line(sl, Fx.options(FxThemes.DUNGEON_DECENNIAL, FxOld.ELECTRIC_SPARK), g.getX(), g.getY() + g.getBbHeight() * 0.7, g.getZ(), p.getX(), p.getY() + 1.0, p.getZ(), 14);
            DecennialFx.hurt(g, p, DecennialFx.baseAttack(g) * (0.15 + sig.power()), true);
            sl.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 0.6f, 1.8f);
        }
        far.clear();
        far.addAll(now);
    }

    // ================================================================ mana leech
    private void manaLeech(LabyrinthGuardianEntity g, ServerLevel sl, List<Player> players) {
        int n = 0;
        for (Player p : players) {
            if (g.distanceToSqr(p) > 100.0) continue;
            DecennialFx.drain(p, Math.max(0.01, sig.power()));
            n++;
        }
        if (n > 0) {
            g.addShield((float) (g.getMaxHealth() * 0.004 * n));
            DecennialFx.ring(sl, Fx.options(FxThemes.DUNGEON_DECENNIAL, FxOld.SCULK_SOUL), g.getX(), g.getY() + 1.0, g.getZ(), 10.0, 24);
        }
    }

    // ================================================================ echo strike
    void afterMelee(LabyrinthGuardianEntity g, ServerLevel sl, Entity target, long now) {
        if (!sig.kind().equals("echo_strike") || !(target instanceof Player p) || echoes.size() >= 6) return;
        echoes.add(new long[] {now + 16});
        echoTargets.add(p.getUUID());
        Fx.spray(sl, FxThemes.DUNGEON_DECENNIAL, FxOld.CRIT, p.getX(), p.getY() + 1.0, p.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
    }

    private void echoes(LabyrinthGuardianEntity g, ServerLevel sl, long now) {
        Iterator<long[]> it = echoes.iterator();
        int i = 0;
        while (it.hasNext()) {
            long[] due = it.next();
            UUID id = echoTargets.get(i);
            if (now < due[0]) { i++; continue; }
            it.remove();
            echoTargets.remove(i);
            Entity e = sl.getEntity(id);
            if (e instanceof Player p && DecennialFx.target(p) && g.distanceToSqr(p) < 36.0) {
                DecennialFx.hurt(g, p, DecennialFx.baseAttack(g) * sig.power(), false);
                Fx.spray(sl, FxThemes.DUNGEON_DECENNIAL, FxOld.CRIT, p.getX(), p.getY() + 1.0, p.getZ(), 14, 0.35, 0.5, 0.35, 0.1);
                sl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.0f, 0.7f);
            }
        }
    }

    // ================================================================ taking a blow
    /** The damage after the signature's say: the blood pact and the mirror guard. */
    float incoming(LabyrinthGuardianEntity g, ServerLevel sl, DamageSource source, float amount, int addsAlive) {
        switch (sig.kind()) {
            case "blood_pact" -> {
                double cover = sig.power() * Math.min(1.0, addsAlive / 4.0);
                return (float) (amount * (1.0 - cover));
            }
            case "mirror_guard" -> {
                if (source.is(DamageTypeTags.IS_PROJECTILE)) {
                    Entity shooter = source.getEntity();
                    if (shooter instanceof Player p && DecennialFx.target(p)) {
                        DecennialFx.hurt(g, p, Math.min(amount * sig.power(), DecennialFx.baseAttack(g) * 0.5), true);
                        DecennialFx.ring(sl, Fx.options(FxThemes.DUNGEON_DECENNIAL, FxOld.ELECTRIC_SPARK), g.getX(), g.getY() + 1.2, g.getZ(), 1.2, 12);
                    }
                    return (float) (amount * (1.0 - sig.power()));
                }
                return amount;
            }
            default -> {
                return amount;
            }
        }
    }

    /** After a blow landed: the thorns. */
    void afterHurt(LabyrinthGuardianEntity g, ServerLevel sl, DamageSource source, float amount) {
        if (!sig.kind().equals("thorns")) return;
        if (source.is(DamageTypeTags.IS_PROJECTILE) || !(source.getDirectEntity() instanceof Player p) || !DecennialFx.target(p) || g.distanceToSqr(p) > 49.0) return;
        double back = Math.min(amount * sig.power(), DecennialFx.baseAttack(g) * 0.5);
        if (back < 1.0) back = 1.0;
        DecennialFx.hurt(g, p, back, false);
        Fx.spray(sl, FxThemes.DUNGEON_DECENNIAL, FxOld.CRIT, p.getX(), p.getY() + 1.0, p.getZ(), 8, 0.3, 0.4, 0.3, 0.05);
    }

    /** One of the boss's adds died: the tithe is paid to the boss as a shield. */
    void addDied(LabyrinthGuardianEntity g) {
        if (sig.kind().equals("add_tithe")) g.addShield((float) (g.getMaxHealth() * sig.power()));
    }

    /** Is the target alive and a person (used by the layer to decide whom the boss may mark)? */
    static boolean live(LivingEntity e) { return e != null && e.isAlive(); }
}
