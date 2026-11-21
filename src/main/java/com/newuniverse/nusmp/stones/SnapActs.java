package com.newuniverse.nusmp.stones;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.boss.rules.BossIdentity;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Why: the reworked Snap of a gauntlet with all six stones socketed (the Wave 30 Snap of the old gauntlet is untouched). The wearer chooses a mode ({@link CarrierRules.SnapMode}) and holds the gesture for five
 * seconds; the server checks everything before anything is paid (all six socketed, the EP gate, the setting, a hand that is not held, a cooldown of seven in-game days per player) and then does one bounded
 * thing within 48 blocks to at most 40 targets: Restore (allies healed by at most half a bar and cleansed), Purge (cleansed), Grant (Resistance II and Strength I for five minutes) or, only when the server
 * allows it (off by default), Erase (weak hostile mobs turn to dust; a boss loses a tenth of its maximum health; never a player, a pet, a named mob or a mob that belongs to a relic). No world edit, no
 * movement, no spawn, nobody wiped world-wide. The toll falls on the wearer (Weakness and Mining Fatigue, a Burned Arm for an hour on the Nano Gauntlet); nothing kills the wearer (there is no Mark LXXXV
 * lethal option because the suit is not built). Every snap is announced to the server.
 */
final class SnapActs {
    private SnapActs() {}

    static final String READY_KEY = "nusmp_infb_snap_ready";

    static long readyAt(ServerPlayer p) { return p.getPersistentData().getLong(READY_KEY); }

    // ================================================================ choosing
    static void chooseMode(ActCtx c) {
        ServerPlayer p = c.player();
        CarrierRules.SnapMode next = CarrierRules.nextMode(CarrierData.mode(c.stack()), StoneConfig.snapErase());
        CarrierData.setMode(c.stack(), next);
        Fx.say(p, "Snap chosen: " + next.title + " (" + next.text + ")");
    }

    // ================================================================ the Snap
    static void snap(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        CarrierRules.SnapMode mode = CarrierData.mode(c.stack());
        boolean nano = CarrierRules.isNano(c.def().id());
        long now = level.getGameTime();
        int n = switch (mode) {
            case RESTORE -> restore(p);
            case PURGE -> purge(p);
            case GRANT -> grant(p);
            case ERASE -> erase(p);
        };
        p.getPersistentData().putLong(READY_KEY, CarrierRules.snapReadyAt(now, StoneConfig.snapCooldownDays()));
        for (InfinityStone s : InfinityStone.values()) StoneActs.flash(p, s, p.position().add(0, 1.0, 0), 6.0 + 2.0 * s.ordinal());
        if (StoneConfig.snapToll()) {
            Fx.addEffect(p, "minecraft:weakness", CarrierRules.tollTicks(), 1);
            Fx.addEffect(p, "minecraft:mining_fatigue", CarrierRules.tollTicks(), 1);
            if (nano) Fx.addEffect(p, "minecraft:weakness", CarrierRules.BURNED_ARM_TICKS, 0);          // the Burned Arm: weaker blows for an hour
        }
        if (p.getServer() != null) {
            p.getServer().getPlayerList().broadcastSystemMessage(Component.literal(p.getName().getString() + " snapped their fingers: " + mode.title + " (" + n
                    + (n == 1 ? " target)." : " targets).")).withStyle(ChatFormatting.GOLD), false);
        }
        Fx.say(p, "The toll falls on you" + (nano ? ", and your arm is burned for an hour." : ".") + " The stones go quiet for " + StoneConfig.snapCooldownDays() + " in-game days.");
    }

    // ================================================================ the modes
    private static int restore(ServerPlayer p) {
        int n = 0;
        for (LivingEntity t : CarrierActs.allies(p, CarrierRules.SNAP_RADIUS, CarrierRules.SNAP_TARGET_CAP)) {
            BalanceLaw.heal(t, t.getMaxHealth() * CarrierRules.HEAL_FRACTION);
            cleanse(t);
            n++;
        }
        return n;
    }

    private static int purge(ServerPlayer p) {
        int n = 0;
        for (LivingEntity t : CarrierActs.allies(p, CarrierRules.SNAP_RADIUS, CarrierRules.SNAP_TARGET_CAP)) {
            cleanse(t);
            n++;
        }
        return n;
    }

    private static int grant(ServerPlayer p) {
        int n = 0;
        for (LivingEntity t : CarrierActs.allies(p, CarrierRules.SNAP_RADIUS, CarrierRules.SNAP_TARGET_CAP)) {
            Fx.addEffect(t, "minecraft:resistance", CarrierRules.GRANT_TICKS, 1);
            Fx.addEffect(t, "minecraft:strength", CarrierRules.GRANT_TICKS, 0);
            n++;
        }
        return n;
    }

    /** Erase (only when the server allows it): at most forty hostile mobs, nearest first; dust for the weak, half the current health for a strong one, a tenth of the maximum for a boss. */
    private static int erase(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        double r = CarrierRules.SNAP_RADIUS;
        List<Mob> foes = new ArrayList<>(level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(r),
                m -> m.isAlive() && m instanceof Enemy && !m.isAlliedTo(p) && m.distanceToSqr(p) <= r * r));
        foes.sort(Comparator.comparingDouble(m -> m.distanceToSqr(p)));
        int n = 0;
        for (Mob m : foes) {
            if (n >= CarrierRules.SNAP_TARGET_CAP) break;
            CarrierRules.Fate fate = CarrierRules.fate(isBoss(m), false, spared(m), m.getMaxHealth());
            if (fate == CarrierRules.Fate.NONE) continue;
            float dmg = CarrierRules.eraseDamage(fate, m.getMaxHealth(), m.getHealth());
            if (dmg > 0f) m.hurt(p.damageSources().indirectMagic(p, p), dmg);
            Fx.burst(level, m.position().add(0, m.getBbHeight() * 0.5, 0), InfinityStone.POWER.rgb());
            n++;
        }
        return n;
    }

    static boolean isBoss(LivingEntity e) {
        try {
            return BalanceLaw.isBoss(e) || BossIdentity.isBoss(e);
        } catch (RuntimeException | LinkageError unreadable) {
            return true;          // when in doubt it is a boss: it only loses a tenth
        }
    }

    /** A mob the Snap never touches: named, owned (somebody's pet), or a summon of a relic. */
    static boolean spared(Mob m) {
        if (m.hasCustomName()) return true;
        if (m instanceof OwnableEntity o && o.getOwnerUUID() != null) return true;
        return m.getPersistentData().hasUUID("nusmp_relic_owner");
    }

    /** Removes every harmful potion effect (the Tensura ones that are harmful too). Returns how many. */
    static int cleanse(LivingEntity e) {
        List<Holder<MobEffect>> bad = new ArrayList<>();
        for (MobEffectInstance i : e.getActiveEffects()) if (i.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) bad.add(i.getEffect());
        for (Holder<MobEffect> h : bad) e.removeEffect(h);
        return bad.size();
    }
}
