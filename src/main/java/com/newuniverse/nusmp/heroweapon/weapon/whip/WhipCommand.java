package com.newuniverse.nusmp.heroweapon.weapon.whip;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.weapon.strike.Strike;
import com.newuniverse.nusmp.heroweapon.weapon.strike.StrikeFx;
import com.newuniverse.nusmp.song.SongTargets;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the canon special ability of the Vassal Whip is to control the power of monsters. In the game that is the Command: up to three hostile monsters near the wielder stop
 * hunting the wielder and their friends and fight the nearest other monster for six seconds, thin violet strings from the wielder's hand to each of them, and then go back to
 * what they were. It is the most dangerous kind of technique to write (a mob's own AI keeps trying to take its target back, and a blow from a mob the wielder "owns" must never
 * land on the wielder), so every guard is here: only a plain hostile monster qualifies (never a boss, a player, a pet or a summon; never a creeper, ghast or blaze, whose blows
 * would break blocks or light fires), the wielder needs the shared control lock, the mark on the monster is two saved numbers that expire by the clock (so a wielder who dies or
 * leaves lets them go within six seconds, with nothing left in a table), and two event listeners make the order stick: a commanded monster cannot be given the wielder, an ally of the
 * wielder or another commanded monster as a target, and a blow from one never hurts the wielder, an ally or any player. Also here: the marks the other whip techniques leave on
 * a foe (bound by a bind or a pull, charged by a lash).
 */
final class WhipCommand {
    private WhipCommand() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final String OWNER = "nusmp_wh_cmd_by", UNTIL = "nusmp_wh_cmd_until", BOUND = "nusmp_wh_bound", CHARGE = "nusmp_wh_charge";
    /** The violet of the gem and of the strings, and the gold of the rings. */
    static final int VIOLET = 0x9B5CFF, GOLD = 0xC9A24A;
    private static boolean registered;

    /** Attaches the two listeners that make the Command stick; once per game. */
    static synchronized void register() {
        if (registered) return;
        registered = true;
        NeoForge.EVENT_BUS.addListener(WhipCommand::onChangeTarget);
        NeoForge.EVENT_BUS.addListener(WhipCommand::onIncomingDamage);
    }

    // ------------------------------------------------------------------ the marks
    /** True while a monster is under a Command. A time further ahead than a Command can last is a clock that went backwards and counts as over. */
    static boolean isCommanded(Entity e) {
        CompoundTag d = e.getPersistentData();
        if (!d.contains(UNTIL)) return false;
        long left = d.getLong(UNTIL) - e.level().getGameTime();
        return left > 0 && left <= WhipNumbers.COMMAND_TICKS + 20;
    }

    static void markBound(LivingEntity t, int ticks) { t.getPersistentData().putLong(BOUND, t.level().getGameTime() + ticks); }

    static boolean isBound(LivingEntity t) {
        long left = t.getPersistentData().getLong(BOUND) - t.level().getGameTime();
        return left > 0 && left <= 400;
    }

    static void markCharged(LivingEntity t, int ticks) { t.getPersistentData().putLong(CHARGE, t.level().getGameTime() + ticks); }

    /** True once if the foe carries a charge, and the charge is used up. */
    static boolean takeCharge(LivingEntity t) {
        CompoundTag d = t.getPersistentData();
        long left = d.getLong(CHARGE) - t.level().getGameTime();
        boolean charged = left > 0 && left <= 400;
        if (d.contains(CHARGE)) d.remove(CHARGE);
        return charged;
    }

    private static ServerPlayer ownerOf(Mob m) {
        CompoundTag d = m.getPersistentData();
        if (!d.hasUUID(OWNER)) return null;
        Player p = m.level().getPlayerByUUID(d.getUUID(OWNER));
        return p instanceof ServerPlayer sp ? sp : null;
    }

    // ------------------------------------------------------------------ choosing and commanding
    /** True for a monster the Command may take: a plain hostile mob that is not a boss, a pet, a summon, already commanded, or one whose blows break blocks or light fires. */
    static boolean eligible(LivingEntity e) {
        if (!(e instanceof Mob m) || !(e instanceof Enemy) || !m.isAlive() || m.isNoAi() || BalanceLaw.isBoss(m) || isCommanded(m)) return false;
        if (m instanceof Creeper || m instanceof Ghast || m instanceof Blaze) return false;
        return !(m instanceof OwnableEntity own && own.getOwnerUUID() != null);
    }

    /** The nearest eligible monsters within {@code range} of the wielder, at most {@code max}. */
    static List<Mob> candidates(ServerPlayer p, double range, int max) {
        List<Mob> out = new ArrayList<>();
        for (LivingEntity t : Strike.foesAround(p, range, 24)) if (out.size() < max && eligible(t)) out.add((Mob) t);
        return out;
    }

    /** Takes the monsters: marks them, clears their targets, draws the strings and starts the steering. The caller has already passed the control lock. */
    static void begin(HeroCtx c, List<Mob> mobs) {
        ServerPlayer p = c.player;
        long until = c.level.getGameTime() + WhipNumbers.COMMAND_TICKS;
        boolean strong = c.form.has(WhipNumbers.TASKMASTER);
        for (Mob m : mobs) {
            CompoundTag d = m.getPersistentData();
            d.putUUID(OWNER, p.getUUID());
            d.putLong(UNTIL, until);
            m.setTarget(null);
            m.getNavigation().stop();
            if (strong) Strike.effect(m, "minecraft:strength", WhipNumbers.COMMAND_TICKS, 0);
        }
        strings(c, mobs);
        Strike.zone(p, WhipNumbers.COMMAND_TICKS, WhipNumbers.COMMAND_STEER, (cx, age) -> steer(cx, mobs, age));
    }

    private static void steer(HeroCtx c, List<Mob> mobs, int age) {
        boolean end = age + WhipNumbers.COMMAND_STEER >= WhipNumbers.COMMAND_TICKS;
        for (Mob m : mobs) {
            if (!m.isAlive()) continue;
            if (end) {
                release(m);
                continue;
            }
            LivingEntity foe = nearestFoe(c.player, m);
            if (m.getTarget() != foe) m.setTarget(foe);
        }
        if (!end && age > 0 && age % 20 == 0) strings(c, mobs);
    }

    /** Gives a monster back to itself. */
    static void release(Mob m) {
        CompoundTag d = m.getPersistentData();
        d.remove(UNTIL);
        d.remove(OWNER);
        m.setTarget(null);
    }

    /** The nearest other monster the wielder could hurt that a commanded monster may attack: never a player, an ally, another commanded monster or itself. */
    private static LivingEntity nearestFoe(ServerPlayer owner, Mob m) {
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : owner.serverLevel().getEntitiesOfClass(LivingEntity.class, m.getBoundingBox().inflate(WhipNumbers.COMMAND_SIGHT),
                x -> x != m && x.isAlive() && !(x instanceof Player) && !isCommanded(x) && SongTargets.isFoe(owner, x))) {
            double d = e.distanceToSqr(m);
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    private static void strings(HeroCtx c, List<Mob> mobs) {
        Vec3 hand = c.player.getEyePosition().add(Strike.flatLook(c.player).scale(0.6)).add(0, -0.4, 0);
        for (Mob m : mobs) if (m.isAlive()) StrikeFx.shape(c.level, VfxShape.THREAD_STRINGS, hand, Strike.center(m), VIOLET, 24, 1.0f);
    }

    // ------------------------------------------------------------------ the two listeners
    /** A commanded monster is not given a player, the wielder, an ally or a fellow commanded monster as a target; the order to forget a target always goes through. */
    static void onChangeTarget(LivingChangeTargetEvent e) {
        try {
            if (!(e.getEntity() instanceof Mob m) || !isCommanded(m)) return;
            LivingEntity next = e.getNewAboutToBeSetTarget();
            if (next == null) return;
            ServerPlayer owner = ownerOf(m);
            if (next instanceof Player || owner == null || next == owner || SongTargets.isAlly(owner, next) || isCommanded(next)) e.setCanceled(true);
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] the whip's command could not judge a new target", ex);
        }
    }

    /** A blow from a commanded monster never lands on a player, the wielder or an ally of the wielder. */
    static void onIncomingDamage(LivingIncomingDamageEvent e) {
        try {
            if (!(e.getSource().getEntity() instanceof Mob m) || !isCommanded(m)) return;
            LivingEntity victim = e.getEntity();
            ServerPlayer owner = ownerOf(m);
            if (victim instanceof Player || owner == null || victim == owner || SongTargets.isAlly(owner, victim)) e.setCanceled(true);
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] the whip's command could not judge a blow", ex);
        }
    }
}
