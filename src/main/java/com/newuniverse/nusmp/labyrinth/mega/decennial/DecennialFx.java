package com.newuniverse.nusmp.labyrinth.mega.decennial;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.ArenaFrame;
import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the lair actions, the signature mechanics and the fight bookkeeping all need the same few small things (who is a legitimate target, how hard a share of the boss's attack really hits once the
 * player's hit cap has had its say, how each of the closed list of effects is put on a person, which game particle a table name means), and writing them once is what keeps the promises of the brief
 * true in one place: only survival players are ever hurt, every hit passes {@code BalanceLaw.cap}, blindness is two seconds at most, a root holds a person for a second and a half at most and is
 * halved when it comes again soon, and the closed list of effects has no hunger entry. The effect names are the table's own ({@code DecennialVocab.Effect}); nothing outside that list can be applied.
 */
final class DecennialFx {
    private DecennialFx() {}

    /** Test seam, set only by the in-game self-tests: while it is not null these are the only people the fight can see (a stand-in without a connection is in no level's player list). */
    static volatile List<Player> testPlayers;

    /** The key of the player's persistent data that remembers when a root last held them (the diminishing-returns rule). */
    private static final String ROOT_KEY = "nusmp_dec_root";

    static boolean target(Player p) { return p != null && p.isAlive() && !p.isSpectator() && !p.isCreative(); }

    /** Survival players inside the hall of {@code f} (the footprint and the volume above the floor). */
    static List<Player> inHall(ServerLevel sl, ArenaFrame f, int height) {
        List<Player> out = new ArrayList<>();
        List<Player> seen = testPlayers;
        if (seen == null) {
            int h = f.size() / 2 + 1;
            AABB box = new AABB(f.cx() - h, f.y0() - 2, f.cz() - h, f.cx() + h + 1, f.y0() + height, f.cz() + h + 1);
            seen = new ArrayList<>(sl.getEntitiesOfClass(Player.class, box, DecennialFx::target));
        }
        for (Player p : seen) if (target(p) && f.contains((int) Math.floor(p.getX()), (int) Math.floor(p.getY()), (int) Math.floor(p.getZ()), height)) out.add(p);
        return out;
    }

    /** Survival players within {@code r} blocks of a point (horizontal and vertical), for a boss with no hall. */
    static List<Player> near(ServerLevel sl, double x, double y, double z, double r) {
        List<Player> out = new ArrayList<>();
        List<Player> seen = testPlayers;
        if (seen == null) seen = new ArrayList<>(sl.getEntitiesOfClass(Player.class, new AABB(x - r, y - r, z - r, x + r, y + r, z + r), DecennialFx::target));
        for (Player p : seen) if (target(p) && p.distanceToSqr(x, y, z) <= r * r) out.add(p);
        return out;
    }

    // ================================================================ damage
    /** The base attack of the boss (without the phase multiplier: the lair power carries that) times a share of it. */
    static double baseAttack(LabyrinthGuardianEntity g) {
        var a = g.getAttribute(Attributes.ATTACK_DAMAGE);
        return a == null ? 8.0 : a.getBaseValue();
    }

    /** Hurts a player for {@code amount}, after the player's hit cap. Magic ignores armour like a spell; the rest is a blow of the boss. Returns whether it landed. */
    static boolean hurt(LabyrinthGuardianEntity g, Player p, double amount, boolean magic) {
        if (!target(p) || amount <= 0.0) return false;
        float capped = BalanceLaw.cap(p, (float) amount);
        return p.hurt(magic ? g.damageSources().indirectMagic(g, g) : g.damageSources().mobAttack(g), capped);
    }

    // ================================================================ effects (a closed list: no hunger)
    /** Puts a table effect on a player. {@code cx, cz} is the point the push comes from (for knock-back). */
    static void effect(LabyrinthGuardianEntity g, ServerLevel sl, Player p, DecennialVocab.Effect e, int ticks, double cx, double cz) {
        if (!target(p)) return;
        switch (e) {
            case SLOW -> p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Math.max(20, ticks), 0));
            case WEAKNESS -> p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Math.max(40, ticks), 0));
            case POISON -> p.addEffect(new MobEffectInstance(MobEffects.POISON, Math.max(40, Math.min(160, ticks)), 0));
            case IGNITE -> p.igniteForSeconds(Math.max(2, Math.min(5, ticks / 20)));
            case FREEZE -> {
                p.setTicksFrozen(Math.max(p.getTicksFrozen(), ticks + 60));
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0));
            }
            case KNOCKBACK -> push(p, cx, cz, 0.9, 0.35);
            case LAUNCH -> {
                p.setDeltaMovement(p.getDeltaMovement().x, 0.95, p.getDeltaMovement().z);
                p.hurtMarked = true;
            }
            case ROOT -> root(sl, p, ticks);
            case CONFUSE -> p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, Math.max(40, Math.min(80, ticks)), 0));
            case BLIND -> p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Math.min(40, Math.max(10, ticks)), 0, false, true));    // two seconds at most
            case DRAIN -> drain(p, 0.03);
            default -> { }
        }
    }

    /** A root holds a person for a second and a half at most; if one held them in the last six seconds it holds half as long. */
    private static void root(ServerLevel sl, Player p, int ticks) {
        long now = sl.getGameTime();
        long last = p.getPersistentData().getLong(ROOT_KEY);
        int hold = Math.min(30, Math.max(10, ticks));
        if (now - last < 120) hold = Math.max(8, hold / 2);
        p.getPersistentData().putLong(ROOT_KEY, now);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, hold, 5, false, true));
    }

    static void push(Player p, double fromX, double fromZ, double strength, double lift) {
        double dx = p.getX() - fromX, dz = p.getZ() - fromZ, d = Math.max(0.5, Math.hypot(dx, dz));
        p.setDeltaMovement(p.getDeltaMovement().add(dx / d * strength, lift, dz / d * strength));
        p.hurtMarked = true;
    }

    /** The magicules of a person drained by a share of their maximum (the same drain the Guardian's own legendary actions use). */
    static void drain(Player p, double fraction) {
        try {
            var ex = TensuraStorages.getExistenceFrom(p);
            if (ex == null) return;
            double max = EnergyHelper.getMaxMagicule(p);
            ex.setMagicule(Math.max(0.0, ex.getMagicule() - max * fraction));
            ex.markDirty();
        } catch (RuntimeException ignored) {
            // a person without a Tensura record has no magicules to lose
        }
    }

    // ================================================================ particles
    /** The game particle a table name means (the closed list of {@code DecennialVocab.PARTICLES}). */
    static ParticleOptions particle(String id) {
        return switch (id == null ? "" : id) {
            case "soul" -> ParticleTypes.SOUL;
            case "soul_fire_flame" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "flame" -> ParticleTypes.FLAME;
            case "end_rod" -> ParticleTypes.END_ROD;
            case "crit" -> ParticleTypes.CRIT;
            case "portal" -> ParticleTypes.PORTAL;
            case "witch" -> ParticleTypes.WITCH;
            case "squid_ink" -> ParticleTypes.SQUID_INK;
            case "sculk_soul" -> ParticleTypes.SCULK_SOUL;
            case "cloud" -> ParticleTypes.CLOUD;
            case "electric_spark" -> ParticleTypes.ELECTRIC_SPARK;
            case "snowflake" -> ParticleTypes.SNOWFLAKE;
            case "happy_villager" -> ParticleTypes.HAPPY_VILLAGER;
            case "enchant" -> ParticleTypes.ENCHANT;
            case "crimson_spore" -> ParticleTypes.CRIMSON_SPORE;
            default -> ParticleTypes.SMOKE;
        };
    }

    static void ring(ServerLevel sl, ParticleOptions type, double x, double y, double z, double r, int n) {
        int count = Math.max(8, Math.min(64, n));
        for (int i = 0; i < count; i++) {
            double a = i * Math.PI * 2.0 / count;
            sl.sendParticles(type, x + Math.cos(a) * r, y, z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    static void line(ServerLevel sl, ParticleOptions type, double ax, double ay, double az, double bx, double by, double bz, int n) {
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            sl.sendParticles(type, ax + (bx - ax) * t, ay + (by - ay) * t, az + (bz - az) * t, 1, 0, 0, 0, 0);
        }
    }
}
